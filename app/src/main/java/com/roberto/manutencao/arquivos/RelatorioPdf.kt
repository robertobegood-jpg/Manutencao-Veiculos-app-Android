package com.roberto.manutencao.arquivos

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.roberto.manutencao.data.CalculoConsumo
import com.roberto.manutencao.data.Datas
import com.roberto.manutencao.data.StatusItem
import com.roberto.manutencao.data.totalCentavos
import com.roberto.manutencao.ui.descreverPeca
import com.roberto.manutencao.ui.descricao
import com.roberto.manutencao.ui.formatarConsumo
import com.roberto.manutencao.ui.formatarDinheiro
import com.roberto.manutencao.ui.formatarKm
import com.roberto.manutencao.ui.formatarLitros
import com.roberto.manutencao.ui.rotuloStatus
import java.io.File
import java.time.LocalDate
import java.util.Locale

/**
 * Relatório do veículo em PDF (A4), no estilo Oficina em papel:
 * tinta quase preta, hairlines, rótulos em mono e título em Archivo expandido.
 */
object RelatorioPdf {
    private const val LARGURA = 595
    private const val ALTURA = 842
    private const val MARGEM = 40f
    private val PT_BR = Locale("pt", "BR")

    private val INK = 0xFF101014.toInt()
    private val MUTED = 0x9E101014.toInt()
    private val FAINT = 0x6B101014
    private val LINE = 0x29101014
    private val LINE_FORTE = 0x66101014
    private val VERMELHO = 0xFFB3261E.toInt()
    private val AMBAR = 0xFF8A5800.toInt()
    private val VERDE = 0xFF136938.toInt()

    private class Fontes(context: Context) {
        val display = carregar(context, "archivo.ttf", 620, "'wdth' 118, 'wght' 620", Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD))
        val corpo = carregar(context, "inter.ttf", 400, "'wght' 400", Typeface.SANS_SERIF)
        val corpoMedio = carregar(context, "inter.ttf", 500, "'wght' 500", Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD))
        val mono = carregar(context, "geist_mono.ttf", 500, "'wght' 500", Typeface.MONOSPACE)

        private fun carregar(context: Context, arquivo: String, peso: Int, variacao: String, reserva: Typeface): Typeface =
            try {
                val existe = context.assets.list("fonts")?.contains(arquivo) == true
                if (!existe) reserva
                else Typeface.Builder(context.assets, "fonts/$arquivo")
                    .setWeight(peso)
                    .setFontVariationSettings(variacao)
                    .build() ?: reserva
            } catch (e: Exception) {
                reserva
            }
    }

    private fun pincel(fonte: Typeface, tamanho: Float, cor: Int, espacamento: Float = 0f) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = fonte
            textSize = tamanho
            color = cor
            letterSpacing = espacamento
        }

    /** Escreve página por página, quebrando quando o conteúdo não cabe. */
    private class Escritor(private val doc: PdfDocument, private val f: Fontes, private val rodape: String) {
        lateinit var canvas: Canvas
        private var pagina: PdfDocument.Page? = null
        private var numero = 0
        var y = MARGEM
        val largura = LARGURA - MARGEM * 2

        val hud = pincel(f.mono, 6.5f, MUTED, 0.15f)
        val corpo = pincel(f.corpo, 8.5f, INK)
        val corpoApoio = pincel(f.corpo, 8.5f, MUTED)
        val linha = Paint().apply { color = LINE; strokeWidth = 0.5f }
        val linhaForte = Paint().apply { color = LINE_FORTE; strokeWidth = 0.5f }

        fun novaPagina() {
            pagina?.let { fecharPagina(it) }
            numero++
            val p = doc.startPage(PdfDocument.PageInfo.Builder(LARGURA, ALTURA, numero).create())
            pagina = p
            canvas = p.canvas
            y = MARGEM
        }

        private fun fecharPagina(p: PdfDocument.Page) {
            val r = "${rodape.uppercase(PT_BR)} · PÁGINA $numero"
            canvas.drawLine(MARGEM, ALTURA - MARGEM + 6, LARGURA - MARGEM, ALTURA - MARGEM + 6, linha)
            canvas.drawText(r, MARGEM, ALTURA - MARGEM + 18, pincel(f.mono, 6f, FAINT, 0.15f))
            doc.finishPage(p)
        }

        fun terminar() {
            pagina?.let { fecharPagina(it) }
            pagina = null
        }

        fun garantir(altura: Float) {
            if (y + altura > ALTURA - MARGEM - 8) novaPagina()
        }

        fun quebrar(texto: String, paint: Paint, larguraMax: Float): List<String> {
            if (texto.isBlank()) return listOf("")
            val linhas = mutableListOf<String>()
            texto.split("\n").forEach { paragrafo ->
                var atual = ""
                paragrafo.split(" ").forEach { palavra ->
                    val tentativa = if (atual.isEmpty()) palavra else "$atual $palavra"
                    if (paint.measureText(tentativa) <= larguraMax) {
                        atual = tentativa
                    } else {
                        if (atual.isNotEmpty()) linhas += atual
                        // Palavra maior que a coluna: corta.
                        var resto = palavra
                        while (paint.measureText(resto) > larguraMax && resto.length > 1) {
                            val n = paint.breakText(resto, true, larguraMax, null).coerceAtLeast(1)
                            linhas += resto.substring(0, n)
                            resto = resto.substring(n)
                        }
                        atual = resto
                    }
                }
                linhas += atual
            }
            return linhas
        }

        /** Eyebrow numerado com régua: "02/HISTÓRICO ─────". */
        fun secao(numero: String, titulo: String) {
            garantir(60f)
            y += 18f
            val texto = "$numero/${titulo.uppercase(PT_BR)}"
            canvas.drawText(texto, MARGEM, y, hud)
            val fim = MARGEM + hud.measureText(texto) + 8
            canvas.drawLine(fim, y - 2.5f, LARGURA - MARGEM, y - 2.5f, linha)
            y += 12f
        }

        fun paragrafo(texto: String, paint: Paint = corpoApoio) {
            quebrar(texto, paint, largura).forEach { l ->
                garantir(12f)
                y += 11f
                canvas.drawText(l, MARGEM, y, paint)
            }
            y += 4f
        }

        /**
         * Tabela com cabeçalho em mono e hairlines entre as linhas.
         * [colunas]: título e fração da largura. [direita]: colunas alinhadas à direita.
         */
        fun tabela(
            colunas: List<Pair<String, Float>>,
            linhas: List<List<String>>,
            direita: Set<Int> = emptySet(),
            cor: (linha: Int, coluna: Int) -> Int? = { _, _ -> null }
        ) {
            val larguras = colunas.map { it.second * largura }
            val folga = 6f

            fun cabecalho() {
                garantir(30f)
                var x = MARGEM
                y += 10f
                colunas.forEachIndexed { i, (titulo, _) ->
                    val t = titulo.uppercase(PT_BR)
                    val px = if (i in direita) x + larguras[i] - hud.measureText(t) else x
                    canvas.drawText(t, px, y, hud)
                    x += larguras[i]
                }
                y += 5f
                canvas.drawLine(MARGEM, y, LARGURA - MARGEM, y, linhaForte)
            }

            cabecalho()
            linhas.forEachIndexed { li, celulas ->
                val quebradas = celulas.mapIndexed { i, texto -> quebrar(texto, corpo, larguras[i] - folga) }
                val altura = quebradas.maxOf { it.size } * 11f + 8f
                if (y + altura > ALTURA - MARGEM - 8) {
                    novaPagina()
                    cabecalho()
                }
                var x = MARGEM
                quebradas.forEachIndexed { i, partes ->
                    val paint = cor(li, i)?.let { c -> Paint(corpo).apply { color = c } } ?: corpo
                    partes.forEachIndexed { k, parte ->
                        val px = if (i in direita) x + larguras[i] - paint.measureText(parte) else x
                        canvas.drawText(parte, px, y + 12f + k * 11f, paint)
                    }
                    x += larguras[i]
                }
                y += altura
                canvas.drawLine(MARGEM, y, LARGURA - MARGEM, y, linha)
            }
        }

        /** Grade de indicadores com hairlines (3 colunas). */
        fun indicadores(itens: List<Pair<String, String>>) {
            val colunas = 3
            val celula = largura / colunas
            val alturaCelula = 46f
            val linhasGrade = (itens.size + colunas - 1) / colunas
            garantir(alturaCelula * linhasGrade + 10)
            val topo = y
            val valor = pincel(f.display, 14f, INK, -0.02f)
            itens.forEachIndexed { i, (rotulo, v) ->
                val cx = MARGEM + (i % colunas) * celula
                val cy = topo + (i / colunas) * alturaCelula
                canvas.drawText(rotulo.uppercase(PT_BR), cx + 10, cy + 16, hud)
                canvas.drawText(v, cx + 10, cy + 36, valor)
            }
            val fundo = topo + linhasGrade * alturaCelula
            canvas.drawRect(MARGEM, topo, LARGURA - MARGEM, fundo, Paint().apply {
                color = LINE; style = Paint.Style.STROKE; strokeWidth = 0.5f
            })
            for (c in 1 until colunas) {
                val x = MARGEM + c * celula
                canvas.drawLine(x, topo, x, fundo, linha)
            }
            for (l in 1 until linhasGrade) {
                val yy = topo + l * alturaCelula
                canvas.drawLine(MARGEM, yy, LARGURA - MARGEM, yy, linha)
            }
            y = fundo + 6f
        }
    }

    fun gerar(context: Context, dados: DadosVeiculo): File {
        val fontes = Fontes(context)
        val v = dados.veiculo
        val hoje = LocalDate.now()
        val doc = PdfDocument()
        val e = Escritor(doc, fontes, "Manutenção · ${v.apelido} · emitido em ${Datas.formatar(hoje)}")
        e.novaPagina()

        // Cabeçalho.
        e.y += 8f
        e.canvas.drawText("RELATÓRIO DO VEÍCULO · ${v.tipo.rotulo.uppercase(PT_BR)}", MARGEM, e.y, e.hud)
        e.y += 30f
        e.canvas.drawText(v.apelido, MARGEM, e.y, pincel(fontes.display, 28f, INK, -0.035f))
        val sub = listOf(v.descricao(), v.placa.takeIf { it.isNotBlank() }?.let { "Placa $it" }.orEmpty())
            .filter { it.isNotBlank() }.joinToString(" · ")
        if (sub.isNotBlank()) {
            e.y += 16f
            e.canvas.drawText(sub, MARGEM, e.y, pincel(fontes.corpo, 10f, MUTED))
        }
        e.y += 18f

        // Indicadores.
        val manutencao = dados.registros.sumOf { it.totalCentavos() }
        val combustivel = dados.abastecimentos.sumOf { it.abastecimento.totalCentavos }
        val outras = dados.despesas.sumOf { it.valorCentavos }
        e.indicadores(
            listOf(
                "Km atual" to v.kmAtual.formatarKm(),
                "Total gasto" to formatarDinheiro(manutencao + combustivel + outras),
                "Consumo médio" to (CalculoConsumo.media(dados.abastecimentos)?.let { formatarConsumo(it) } ?: "—"),
                "Manutenção" to formatarDinheiro(manutencao),
                "Combustível" to formatarDinheiro(combustivel),
                "Outras despesas" to formatarDinheiro(outras)
            )
        )

        // 01 · Situação dos itens.
        e.secao("01", "Situação dos itens")
        if (dados.situacoes.isEmpty()) {
            e.paragrafo("Nenhum item de manutenção cadastrado.")
        } else {
            e.tabela(
                colunas = listOf("Item" to 0.25f, "Peça" to 0.25f, "Última troca" to 0.17f, "Próxima" to 0.19f, "Situação" to 0.14f),
                linhas = dados.situacoes.map { s ->
                    listOf(
                        s.item.nome,
                        descreverPeca(s.item.modelo, s.item.referencia).ifBlank { "—" },
                        listOfNotNull(s.ultimoKm?.formatarKm(), s.ultimaData?.let { Datas.formatar(it) })
                            .joinToString("\n").ifBlank { "—" },
                        listOfNotNull(s.proximoKm?.formatarKm(), s.proximaData?.let { Datas.formatar(it) })
                            .joinToString("\n").ifBlank { "—" },
                        rotuloStatus(s.status)
                    )
                },
                cor = { linha, coluna ->
                    if (coluna != 4) null else when (dados.situacoes[linha].status) {
                        StatusItem.VENCIDO -> VERMELHO
                        StatusItem.PROXIMO -> AMBAR
                        StatusItem.EM_DIA -> VERDE
                        else -> MUTED
                    }
                }
            )
        }

        // 02 · Histórico de manutenção.
        e.secao("02", "Histórico de manutenção")
        if (dados.registros.isEmpty()) {
            e.paragrafo("Nenhuma manutenção registrada.")
        } else {
            e.tabela(
                colunas = listOf("Data" to 0.12f, "Km" to 0.12f, "Itens" to 0.38f, "Local" to 0.21f, "Valor" to 0.17f),
                linhas = dados.registros.map { r ->
                    listOf(
                        Datas.formatar(r.registro.data),
                        r.registro.km.formatarKm(),
                        r.itens.joinToString(", ") { it.nome }.ifBlank { "—" } +
                            if (r.registro.observacoes.isNotBlank()) "\n${r.registro.observacoes}" else "",
                        if (r.registro.feitoPorMim) "Feito pelo dono" else r.registro.local.ifBlank { "—" },
                        if (r.totalCentavos() > 0) formatarDinheiro(r.totalCentavos()) else "—"
                    )
                },
                direita = setOf(4)
            )
            e.paragrafo("${dados.registros.size} registros · ${formatarDinheiro(manutencao)} em manutenção.")
        }

        // 03 · Abastecimentos.
        e.secao("03", "Abastecimentos")
        if (dados.abastecimentos.isEmpty()) {
            e.paragrafo("Nenhum abastecimento registrado.")
        } else {
            val litros = dados.abastecimentos.sumOf { it.abastecimento.litros }
            e.paragrafo(
                "${dados.abastecimentos.size} abastecimentos · ${formatarLitros(litros)} · ${formatarDinheiro(combustivel)}."
            )
            e.tabela(
                colunas = listOf(
                    "Data" to 0.13f, "Km" to 0.14f, "Combustível" to 0.15f, "Litros" to 0.12f,
                    "Preço/L" to 0.14f, "Total" to 0.17f, "Km/l" to 0.15f
                ),
                linhas = dados.abastecimentos.map { a ->
                    val x = a.abastecimento
                    listOf(
                        Datas.formatar(x.data),
                        x.km.formatarKm(),
                        x.combustivel.rotulo,
                        formatarLitros(x.litros),
                        formatarDinheiro(x.precoLitroMilesimos, 3),
                        formatarDinheiro(x.totalCentavos),
                        a.consumoKmL?.let { String.format(PT_BR, "%.1f", it) } ?: (if (x.tanqueCheio) "—" else "parcial")
                    )
                },
                direita = setOf(3, 4, 5, 6)
            )
        }

        // 04 · Despesas.
        e.secao("04", "Despesas avulsas")
        if (dados.despesas.isEmpty()) {
            e.paragrafo("Nenhuma despesa registrada.")
        } else {
            e.tabela(
                colunas = listOf("Data" to 0.13f, "Categoria" to 0.2f, "Descrição" to 0.48f, "Valor" to 0.19f),
                linhas = dados.despesas.map { d ->
                    listOf(
                        Datas.formatar(d.data),
                        d.categoria.rotulo,
                        d.descricao.ifBlank { "—" },
                        formatarDinheiro(d.valorCentavos)
                    )
                },
                direita = setOf(3)
            )
        }

        e.terminar()
        val arquivo = File(Compartilhar.pastaExportacoes(context), "${slug(v.apelido)}-relatorio-$hoje.pdf")
        arquivo.outputStream().use { doc.writeTo(it) }
        doc.close()
        return arquivo
    }
}
