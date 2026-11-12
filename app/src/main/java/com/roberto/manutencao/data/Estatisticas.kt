package com.roberto.manutencao.data

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class Periodo(val rotulo: String, val meses: Long?) {
    MES("Mês", 1),
    SEMESTRE("6 meses", 6),
    ANO("Ano", 12),
    TUDO("Tudo", null)
}

/** Tamanho de cada barra do gráfico de gastos. */
enum class Escala(val rotulo: String) {
    MES("Por mês"),
    TRIMESTRE("Por trimestre"),
    ANO("Por ano")
}

/**
 * Uma barra do gráfico: gastos de um mês, trimestre ou ano, em centavos.
 * [rotulo] vai embaixo da barra ("NOV", "3T", "2025"); [ano] marca a virada de ano
 * na linha de baixo; [titulo] aparece no topo quando a barra é tocada.
 */
data class BarraGasto(
    val rotulo: String,
    val ano: String?,
    val titulo: String,
    val manutencao: Long,
    val combustivel: Long,
    val despesas: Long
) {
    val total: Long get() = manutencao + combustivel + despesas
}

/**
 * Uma fatia do gráfico por categoria.
 * [indice] é fixo por categoria, para a cor não mudar quando o período muda.
 */
data class FatiaGasto(val rotulo: String, val centavos: Long, val indice: Int)

data class PontoConsumo(val data: LocalDate, val kmL: Double)

data class ResumoPainel(
    val periodo: Periodo,
    val gastoTotal: Long,
    val kmRodados: Int?,
    /** Centavos por km rodado. */
    val custoPorKm: Double?,
    val consumoMedio: Double?,
    val gastoMedioMensal: Long?,
    val escala: Escala,
    val barras: List<BarraGasto>,
    val fatias: List<FatiaGasto>,
    val consumo: List<PontoConsumo>
)

object Estatisticas {

    /** Primeiro dia do período (null = desde sempre). */
    fun inicio(periodo: Periodo, hoje: LocalDate): LocalDate? =
        periodo.meses?.let { YearMonth.from(hoje).minusMonths(it - 1).atDay(1) }

    fun resumir(
        periodo: Periodo,
        hoje: LocalDate,
        registros: List<RegistroComItens>,
        abastecimentos: List<AbastecimentoComConsumo>,
        despesas: List<Despesa>,
        leituras: List<LeituraKm>,
        kmAtual: Int
    ): ResumoPainel {
        val ini = inicio(periodo, hoje)
        fun noPeriodo(d: LocalDate) = (ini == null || !d.isBefore(ini)) && !d.isAfter(hoje)

        val regs = registros.filter { noPeriodo(Datas.paraLocalDate(it.registro.data)) }
        val abs = abastecimentos.filter { noPeriodo(Datas.paraLocalDate(it.abastecimento.data)) }
        val desp = despesas.filter { noPeriodo(Datas.paraLocalDate(it.data)) }

        // Por categoria.
        val categorias = CategoriaDespesa.entries
        val fatias = buildList {
            add(FatiaGasto("Peças", regs.sumOf { it.registro.valorPecasCentavos }, 0))
            add(FatiaGasto("Mão de obra", regs.sumOf { it.registro.valorMaoDeObraCentavos }, 1))
            add(FatiaGasto("Combustível", abs.sumOf { it.abastecimento.totalCentavos }, 2))
            desp.groupBy { it.categoria }.forEach { (cat, lista) ->
                add(FatiaGasto(cat.rotulo, lista.sumOf { it.valorCentavos }, 3 + categorias.indexOf(cat)))
            }
        }.filter { it.centavos > 0 }.sortedByDescending { it.centavos }
        val total = fatias.sumOf { it.centavos }

        // Km rodados no período: do último km conhecido antes do início até o km atual.
        val pontosKm = buildList {
            leituras.forEach { add(doInstante(it.data) to it.km) }
            registros.forEach { add(Datas.paraLocalDate(it.registro.data) to it.registro.km) }
            abastecimentos.forEach { add(Datas.paraLocalDate(it.abastecimento.data) to it.abastecimento.km) }
            add(hoje to kmAtual)
        }
        val kmBase = if (ini == null) {
            pontosKm.minOfOrNull { it.second }
        } else {
            pontosKm.filter { it.first.isBefore(ini) }.maxOfOrNull { it.second }
                ?: pontosKm.filter { !it.first.isBefore(ini) }.minOfOrNull { it.second }
        }
        val kmRodados = kmBase?.let { (kmAtual - it).coerceAtLeast(0) }?.takeIf { it > 0 }
        val custoPorKm = if (kmRodados != null && total > 0) total.toDouble() / kmRodados else null

        val consumo = abs.mapNotNull { a ->
            a.consumoKmL?.let { PontoConsumo(Datas.paraLocalDate(a.abastecimento.data), it) }
        }.sortedBy { it.data }

        // Barras do gráfico. Períodos longos são agrupados em trimestres ou anos,
        // para nunca passar de 12 barras e os rótulos não se embolarem.
        val primeiraData = listOfNotNull(
            registros.minOfOrNull { it.registro.data },
            abastecimentos.minOfOrNull { it.abastecimento.data },
            despesas.minOfOrNull { it.data }
        ).minOrNull()?.let(Datas::paraLocalDate)
        val ultimoMes = YearMonth.from(hoje)
        val mesesDesdeInicio = primeiraData?.let {
            ChronoUnit.MONTHS.between(YearMonth.from(it), ultimoMes).toInt() + 1
        } ?: 1
        val escala = when {
            periodo != Periodo.TUDO || mesesDesdeInicio <= 12 -> Escala.MES
            mesesDesdeInicio <= 36 -> Escala.TRIMESTRE
            else -> Escala.ANO
        }
        val barras = when (escala) {
            Escala.MES -> {
                val quantidade = when (periodo) {
                    Periodo.MES, Periodo.SEMESTRE -> 6
                    Periodo.ANO -> 12
                    Periodo.TUDO -> mesesDesdeInicio.coerceIn(6, 12)
                }
                (quantidade - 1 downTo 0).mapIndexed { i, atras ->
                    val mes = ultimoMes.minusMonths(atras.toLong())
                    barra(
                        rotulo = mes.format(MES_CURTO).removeSuffix(".").uppercase(PT_BR),
                        ano = if (i == 0 || mes.monthValue == 1) mes.year.toString() else null,
                        titulo = mes.format(MES_ANO),
                        inicio = mes.atDay(1),
                        fim = mes.atEndOfMonth(),
                        registros, abastecimentos, despesas
                    )
                }
            }
            Escala.TRIMESTRE -> {
                fun trimestreDe(ym: YearMonth) = YearMonth.of(ym.year, (ym.monthValue - 1) / 3 * 3 + 1)
                val ultimo = trimestreDe(ultimoMes)
                val primeiro = trimestreDe(YearMonth.from(primeiraData ?: hoje))
                val quantidade = (ChronoUnit.MONTHS.between(primeiro, ultimo).toInt() / 3 + 1).coerceIn(4, 12)
                (quantidade - 1 downTo 0).mapIndexed { i, atras ->
                    val ini = ultimo.minusMonths(atras * 3L)
                    val numero = (ini.monthValue - 1) / 3 + 1
                    barra(
                        rotulo = "${numero}T",
                        ano = if (i == 0 || numero == 1) ini.year.toString() else null,
                        titulo = "${numero}º trimestre ${ini.year}",
                        inicio = ini.atDay(1),
                        fim = ini.plusMonths(2).atEndOfMonth(),
                        registros, abastecimentos, despesas
                    )
                }
            }
            Escala.ANO -> {
                val primeiroAno = (primeiraData ?: hoje).year
                val quantidade = (hoje.year - primeiroAno + 1).coerceIn(2, 12)
                (quantidade - 1 downTo 0).map { atras ->
                    val ano = hoje.year - atras
                    barra(
                        rotulo = ano.toString(),
                        ano = null,
                        titulo = ano.toString(),
                        inicio = LocalDate.of(ano, 1, 1),
                        fim = LocalDate.of(ano, 12, 31),
                        registros, abastecimentos, despesas
                    )
                }
            }
        }

        val mesesNoPeriodo = periodo.meses?.toInt() ?: mesesDesdeInicio
        val mediaMensal = if (total > 0 && mesesNoPeriodo > 1) total / mesesNoPeriodo else null

        return ResumoPainel(
            periodo = periodo,
            gastoTotal = total,
            kmRodados = kmRodados,
            custoPorKm = custoPorKm,
            consumoMedio = CalculoConsumo.media(abs),
            gastoMedioMensal = mediaMensal,
            escala = escala,
            barras = barras,
            fatias = fatias,
            consumo = consumo
        )
    }

    private val PT_BR = Locale("pt", "BR")
    private val MES_CURTO = DateTimeFormatter.ofPattern("MMM", PT_BR)
    private val MES_ANO = DateTimeFormatter.ofPattern("MMMM yyyy", PT_BR)

    /** Soma as três séries entre [inicio] e [fim], inclusive. */
    private fun barra(
        rotulo: String,
        ano: String?,
        titulo: String,
        inicio: LocalDate,
        fim: LocalDate,
        registros: List<RegistroComItens>,
        abastecimentos: List<AbastecimentoComConsumo>,
        despesas: List<Despesa>
    ): BarraGasto {
        fun dentro(millis: Long) = Datas.paraLocalDate(millis).let { !it.isBefore(inicio) && !it.isAfter(fim) }
        return BarraGasto(
            rotulo = rotulo,
            ano = ano,
            titulo = titulo,
            manutencao = registros.filter { dentro(it.registro.data) }
                .sumOf { it.registro.valorPecasCentavos + it.registro.valorMaoDeObraCentavos },
            combustivel = abastecimentos.filter { dentro(it.abastecimento.data) }
                .sumOf { it.abastecimento.totalCentavos },
            despesas = despesas.filter { dentro(it.data) }.sumOf { it.valorCentavos }
        )
    }

    private fun doInstante(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
}
