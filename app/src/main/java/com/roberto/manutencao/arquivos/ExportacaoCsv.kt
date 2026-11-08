package com.roberto.manutencao.arquivos

import android.content.Context
import com.roberto.manutencao.data.Datas
import com.roberto.manutencao.data.totalCentavos
import com.roberto.manutencao.ui.descreverIntervalo
import com.roberto.manutencao.ui.rotuloStatus
import java.io.File
import java.time.LocalDate
import java.util.Locale

/**
 * Planilhas em CSV no padrão brasileiro do Excel:
 * separador ";", vírgula decimal e UTF-8 com BOM (para acentos abrirem certo).
 */
object ExportacaoCsv {
    private val PT_BR = Locale("pt", "BR")

    fun gerar(context: Context, dados: DadosVeiculo): List<File> {
        val pasta = Compartilhar.pastaExportacoes(context)
        val prefixo = "${slug(dados.veiculo.apelido)}-${LocalDate.now()}"

        val registros = escrever(
            File(pasta, "$prefixo-manutencoes.csv"),
            listOf("Data", "Km", "Itens", "Local", "Feito por mim", "Peças (R$)", "Mão de obra (R$)", "Total (R$)", "Observações"),
            dados.registros.sortedBy { it.registro.data }.map { r ->
                listOf(
                    Datas.formatar(r.registro.data),
                    r.registro.km.toString(),
                    r.itens.joinToString(", ") { it.nome },
                    r.registro.local,
                    if (r.registro.feitoPorMim) "Sim" else "Não",
                    reais(r.registro.valorPecasCentavos),
                    reais(r.registro.valorMaoDeObraCentavos),
                    reais(r.totalCentavos()),
                    r.registro.observacoes
                )
            }
        )

        val abastecimentos = escrever(
            File(pasta, "$prefixo-abastecimentos.csv"),
            listOf("Data", "Km", "Combustível", "Litros", "Preço/L (R$)", "Total (R$)", "Tanque cheio", "Km/l do trecho", "Posto"),
            dados.abastecimentos.sortedBy { it.abastecimento.km }.map { a ->
                val x = a.abastecimento
                listOf(
                    Datas.formatar(x.data),
                    x.km.toString(),
                    x.combustivel.rotulo,
                    numero(x.litros, 2),
                    numero(x.precoLitroMilesimos / 1000.0, 3),
                    reais(x.totalCentavos),
                    if (x.tanqueCheio) "Sim" else "Não",
                    a.consumoKmL?.let { numero(it, 2) }.orEmpty(),
                    x.posto
                )
            }
        )

        val despesas = escrever(
            File(pasta, "$prefixo-despesas.csv"),
            listOf("Data", "Categoria", "Descrição", "Valor (R$)", "Observação"),
            dados.despesas.sortedBy { it.data }.map { d ->
                listOf(Datas.formatar(d.data), d.categoria.rotulo, d.descricao, reais(d.valorCentavos), d.observacao)
            }
        )

        val itens = escrever(
            File(pasta, "$prefixo-itens.csv"),
            listOf("Item", "Modelo", "Referência", "Intervalo", "Última troca (km)", "Última troca (data)", "Próxima (km)", "Próxima (data)", "Situação"),
            dados.situacoes.map { s ->
                listOf(
                    s.item.nome,
                    s.item.modelo,
                    s.item.referencia,
                    descreverIntervalo(s.item),
                    s.ultimoKm?.toString().orEmpty(),
                    s.ultimaData?.let { Datas.formatar(it) }.orEmpty(),
                    s.proximoKm?.toString().orEmpty(),
                    s.proximaData?.let { Datas.formatar(it) }.orEmpty(),
                    rotuloStatus(s.status)
                )
            }
        )

        return listOf(registros, abastecimentos, despesas, itens)
    }

    private fun reais(centavos: Long): String = numero(centavos / 100.0, 2)

    private fun numero(valor: Double, casas: Int): String = String.format(PT_BR, "%.${casas}f", valor)

    private fun celula(texto: String): String {
        val limpo = texto.replace("\r", " ").replace("\n", " ")
        return if (limpo.any { it == ';' || it == '"' }) "\"" + limpo.replace("\"", "\"\"") + "\"" else limpo
    }

    private fun escrever(arquivo: File, cabecalho: List<String>, linhas: List<List<String>>): File {
        arquivo.bufferedWriter(Charsets.UTF_8).use { w ->
            w.write("﻿")
            w.write(cabecalho.joinToString(";") { celula(it) })
            w.write("\r\n")
            linhas.forEach { linha ->
                w.write(linha.joinToString(";") { celula(it) })
                w.write("\r\n")
            }
        }
        return arquivo
    }
}
