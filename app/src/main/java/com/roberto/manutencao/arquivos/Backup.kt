package com.roberto.manutencao.arquivos

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.roberto.manutencao.data.Abastecimento
import com.roberto.manutencao.data.Anexo
import com.roberto.manutencao.data.AppDatabase
import com.roberto.manutencao.data.CategoriaDespesa
import com.roberto.manutencao.data.Combustivel
import com.roberto.manutencao.data.Despesa
import com.roberto.manutencao.data.ItemCompra
import com.roberto.manutencao.data.ItemManutencao
import com.roberto.manutencao.data.LeituraKm
import com.roberto.manutencao.data.RegistroItem
import com.roberto.manutencao.data.RegistroManutencao
import com.roberto.manutencao.data.TipoVeiculo
import com.roberto.manutencao.data.Veiculo
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Backup completo num .zip: dados.json com todas as tabelas e a pasta anexos/ com as fotos.
 * Restaurar substitui tudo o que está no celular pelo conteúdo do arquivo.
 */
object Backup {
    private const val FORMATO = 1
    private const val ARQUIVO_DADOS = "dados.json"
    private const val PASTA_ANEXOS = "anexos/"

    data class Resumo(val veiculos: Int, val registros: Int, val fotos: Int)

    fun nomeSugerido(): String = "manutencao-backup-${java.time.LocalDate.now()}.zip"

    suspend fun exportar(context: Context, db: AppDatabase, destino: Uri): Resumo {
        val dao = db.backupDao()
        val anexos = dao.anexos()
        val json = JSONObject().apply {
            put("formato", FORMATO)
            put("app", "manutencao-veiculos")
            put("criadoEm", System.currentTimeMillis())
            put("veiculos", lista(dao.veiculos()) { it.json() })
            put("leituras", lista(dao.leituras()) { it.json() })
            put("itens", lista(dao.itens()) { it.json() })
            put("registros", lista(dao.registros()) { it.json() })
            put("registroItens", lista(dao.registroItens()) { it.json() })
            put("compras", lista(dao.compras()) { it.json() })
            put("abastecimentos", lista(dao.abastecimentos()) { it.json() })
            put("despesas", lista(dao.despesas()) { it.json() })
            put("anexos", lista(anexos) { it.json() })
        }

        var fotos = 0
        val saida = context.contentResolver.openOutputStream(destino, "wt")
            ?: throw IllegalStateException("Não foi possível abrir o arquivo de destino.")
        ZipOutputStream(saida.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(ARQUIVO_DADOS))
            zip.write(json.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            anexos.map { it.arquivo }.distinct().forEach { nome ->
                val f = Anexos.arquivo(context, nome)
                if (f.exists()) {
                    zip.putNextEntry(ZipEntry(PASTA_ANEXOS + nome))
                    f.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                    fotos++
                }
            }
        }
        return Resumo(json.getJSONArray("veiculos").length(), json.getJSONArray("registros").length(), fotos)
    }

    suspend fun importar(context: Context, db: AppDatabase, origem: Uri): Resumo {
        // 1. Lê tudo para uma pasta temporária antes de mexer nos dados atuais.
        val temporaria = File(context.cacheDir, "restauracao").apply { deleteRecursively(); mkdirs() }
        var texto: String? = null
        val fluxo = context.contentResolver.openInputStream(origem)
            ?: throw IllegalStateException("Não foi possível abrir o arquivo.")
        ZipInputStream(fluxo.buffered()).use { zip ->
            var entrada = zip.nextEntry
            while (entrada != null) {
                val nome = entrada.name
                when {
                    nome == ARQUIVO_DADOS -> texto = zip.readBytes().toString(Charsets.UTF_8)
                    nome.startsWith(PASTA_ANEXOS) && !entrada.isDirectory -> {
                        val simples = File(nome).name // evita caminhos fora da pasta
                        File(temporaria, simples).outputStream().use { zip.copyTo(it) }
                    }
                }
                zip.closeEntry()
                entrada = zip.nextEntry
            }
        }
        val json = JSONObject(texto ?: throw IllegalArgumentException("Este arquivo não é um backup do app."))
        require(json.optString("app") == "manutencao-veiculos") { "Este arquivo não é um backup do app." }
        require(json.optInt("formato") <= FORMATO) { "Backup feito numa versão mais nova do app." }

        val veiculos = ler(json, "veiculos") { veiculo(it) }
        val leituras = ler(json, "leituras") { leitura(it) }
        val itens = ler(json, "itens") { item(it) }
        val registros = ler(json, "registros") { registro(it) }
        val registroItens = ler(json, "registroItens") { RegistroItem(it.getLong("registroId"), it.getLong("itemId")) }
        val compras = ler(json, "compras") { compra(it) }
        val abastecimentos = ler(json, "abastecimentos") { abastecimento(it) }
        val despesas = ler(json, "despesas") { despesa(it) }
        val anexos = ler(json, "anexos") { Anexo(it.getLong("id"), it.getLong("registroId"), it.getString("arquivo"), it.getLong("criadoEm")) }

        // 2. Substitui o banco inteiro numa transação: se algo falhar, nada muda.
        val dao = db.backupDao()
        db.withTransaction {
            dao.limparAnexos()
            dao.limparRegistroItens()
            dao.limparCompras()
            dao.limparRegistros()
            dao.limparItens()
            dao.limparAbastecimentos()
            dao.limparDespesas()
            dao.limparLeituras()
            dao.limparVeiculos()
            dao.inserirVeiculos(veiculos)
            dao.inserirLeituras(leituras)
            dao.inserirItens(itens)
            dao.inserirRegistros(registros)
            dao.inserirRegistroItens(registroItens)
            dao.inserirCompras(compras)
            dao.inserirAbastecimentos(abastecimentos)
            dao.inserirDespesas(despesas)
            dao.inserirAnexos(anexos)
        }

        // 3. Troca as fotos.
        val pasta = Anexos.pasta(context)
        pasta.listFiles()?.forEach { it.delete() }
        var fotos = 0
        temporaria.listFiles()?.forEach { f ->
            f.copyTo(File(pasta, f.name), overwrite = true)
            fotos++
        }
        temporaria.deleteRecursively()
        return Resumo(veiculos.size, registros.size, fotos)
    }

    /* ───── JSON ───── */

    private fun <T> lista(itens: List<T>, conversor: (T) -> JSONObject) =
        JSONArray().apply { itens.forEach { put(conversor(it)) } }

    private fun <T> ler(json: JSONObject, chave: String, conversor: (JSONObject) -> T): List<T> {
        val array = json.optJSONArray(chave) ?: return emptyList()
        return (0 until array.length()).map { conversor(array.getJSONObject(it)) }
    }

    private fun JSONObject.textoOuNulo(chave: String): String? = if (isNull(chave)) null else optString(chave)
    private fun JSONObject.longOuNulo(chave: String): Long? = if (isNull(chave) || !has(chave)) null else getLong(chave)
    private fun JSONObject.intOuNulo(chave: String): Int? = if (isNull(chave) || !has(chave)) null else getInt(chave)
    private fun JSONObject.putNulavel(chave: String, valor: Any?) = put(chave, valor ?: JSONObject.NULL)

    private fun Veiculo.json() = JSONObject().apply {
        put("id", id); put("tipo", tipo.name); put("apelido", apelido); put("marca", marca); put("modelo", modelo)
        putNulavel("ano", ano); put("placa", placa); put("kmAtual", kmAtual); putNulavel("fotoUri", fotoUri)
        put("criadoEm", criadoEm)
    }

    private fun veiculo(o: JSONObject) = Veiculo(
        id = o.getLong("id"),
        tipo = TipoVeiculo.entries.firstOrNull { it.name == o.optString("tipo") } ?: TipoVeiculo.CARRO,
        apelido = o.getString("apelido"),
        marca = o.optString("marca"),
        modelo = o.optString("modelo"),
        ano = o.intOuNulo("ano"),
        placa = o.optString("placa"),
        kmAtual = o.optInt("kmAtual"),
        fotoUri = o.textoOuNulo("fotoUri"),
        criadoEm = o.optLong("criadoEm", System.currentTimeMillis())
    )

    private fun LeituraKm.json() = JSONObject().apply {
        put("id", id); put("veiculoId", veiculoId); put("km", km); put("data", data)
    }

    private fun leitura(o: JSONObject) = LeituraKm(o.getLong("id"), o.getLong("veiculoId"), o.getInt("km"), o.getLong("data"))

    private fun ItemManutencao.json() = JSONObject().apply {
        put("id", id); put("veiculoId", veiculoId); put("nome", nome)
        putNulavel("intervaloKm", intervaloKm); putNulavel("intervaloMeses", intervaloMeses)
        putNulavel("baseKm", baseKm); putNulavel("baseData", baseData)
        put("ativo", ativo); put("ordem", ordem); put("modelo", modelo); put("referencia", referencia)
    }

    private fun item(o: JSONObject) = ItemManutencao(
        id = o.getLong("id"),
        veiculoId = o.getLong("veiculoId"),
        nome = o.getString("nome"),
        intervaloKm = o.intOuNulo("intervaloKm"),
        intervaloMeses = o.intOuNulo("intervaloMeses"),
        baseKm = o.intOuNulo("baseKm"),
        baseData = o.longOuNulo("baseData"),
        ativo = o.optBoolean("ativo", true),
        ordem = o.optInt("ordem"),
        modelo = o.optString("modelo"),
        referencia = o.optString("referencia")
    )

    private fun RegistroManutencao.json() = JSONObject().apply {
        put("id", id); put("veiculoId", veiculoId); put("data", data); put("km", km); put("local", local)
        put("feitoPorMim", feitoPorMim); put("valorPecasCentavos", valorPecasCentavos)
        put("valorMaoDeObraCentavos", valorMaoDeObraCentavos); put("observacoes", observacoes)
    }

    private fun registro(o: JSONObject) = RegistroManutencao(
        id = o.getLong("id"),
        veiculoId = o.getLong("veiculoId"),
        data = o.getLong("data"),
        km = o.getInt("km"),
        local = o.optString("local"),
        feitoPorMim = o.optBoolean("feitoPorMim"),
        valorPecasCentavos = o.optLong("valorPecasCentavos"),
        valorMaoDeObraCentavos = o.optLong("valorMaoDeObraCentavos"),
        observacoes = o.optString("observacoes")
    )

    private fun RegistroItem.json() = JSONObject().apply { put("registroId", registroId); put("itemId", itemId) }

    private fun ItemCompra.json() = JSONObject().apply {
        put("id", id); put("veiculoId", veiculoId); putNulavel("itemManutencaoId", itemManutencaoId)
        put("nome", nome); put("modelo", modelo); put("referencia", referencia); put("quantidade", quantidade)
        put("unidade", unidade); put("precoUnitarioCentavos", precoUnitarioCentavos); put("loja", loja)
        put("observacao", observacao); put("comprado", comprado); putNulavel("compradoEm", compradoEm)
        putNulavel("registroId", registroId); put("criadoEm", criadoEm)
    }

    private fun compra(o: JSONObject) = ItemCompra(
        id = o.getLong("id"),
        veiculoId = o.getLong("veiculoId"),
        itemManutencaoId = o.longOuNulo("itemManutencaoId"),
        nome = o.getString("nome"),
        modelo = o.optString("modelo"),
        referencia = o.optString("referencia"),
        quantidade = o.optDouble("quantidade", 1.0),
        unidade = o.optString("unidade", "un"),
        precoUnitarioCentavos = o.optLong("precoUnitarioCentavos"),
        loja = o.optString("loja"),
        observacao = o.optString("observacao"),
        comprado = o.optBoolean("comprado"),
        compradoEm = o.longOuNulo("compradoEm"),
        registroId = o.longOuNulo("registroId"),
        criadoEm = o.optLong("criadoEm", System.currentTimeMillis())
    )

    private fun Abastecimento.json() = JSONObject().apply {
        put("id", id); put("veiculoId", veiculoId); put("data", data); put("km", km); put("litros", litros)
        put("precoLitroMilesimos", precoLitroMilesimos); put("totalCentavos", totalCentavos)
        put("tanqueCheio", tanqueCheio); put("combustivel", combustivel.name); put("posto", posto)
        put("observacao", observacao)
    }

    private fun abastecimento(o: JSONObject) = Abastecimento(
        id = o.getLong("id"),
        veiculoId = o.getLong("veiculoId"),
        data = o.getLong("data"),
        km = o.getInt("km"),
        litros = o.getDouble("litros"),
        precoLitroMilesimos = o.optLong("precoLitroMilesimos"),
        totalCentavos = o.optLong("totalCentavos"),
        tanqueCheio = o.optBoolean("tanqueCheio", true),
        combustivel = Combustivel.entries.firstOrNull { it.name == o.optString("combustivel") } ?: Combustivel.GASOLINA,
        posto = o.optString("posto"),
        observacao = o.optString("observacao")
    )

    private fun Despesa.json() = JSONObject().apply {
        put("id", id); put("veiculoId", veiculoId); put("data", data); put("categoria", categoria.name)
        put("descricao", descricao); put("valorCentavos", valorCentavos); put("observacao", observacao)
    }

    private fun despesa(o: JSONObject) = Despesa(
        id = o.getLong("id"),
        veiculoId = o.getLong("veiculoId"),
        data = o.getLong("data"),
        categoria = CategoriaDespesa.entries.firstOrNull { it.name == o.optString("categoria") } ?: CategoriaDespesa.OUTRO,
        descricao = o.optString("descricao"),
        valorCentavos = o.optLong("valorCentavos"),
        observacao = o.optString("observacao")
    )

    private fun Anexo.json() = JSONObject().apply {
        put("id", id); put("registroId", registroId); put("arquivo", arquivo); put("criadoEm", criadoEm)
    }
}
