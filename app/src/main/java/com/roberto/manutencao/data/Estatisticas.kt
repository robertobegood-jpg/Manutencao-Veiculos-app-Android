package com.roberto.manutencao.data

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class Periodo(val rotulo: String, val meses: Long?) {
    MES("Mês", 1),
    SEMESTRE("6 meses", 6),
    ANO("Ano", 12),
    TUDO("Tudo", null)
}

/** Gastos de um mês, em centavos, separados nas três séries do gráfico. */
data class GastoMes(
    val mes: YearMonth,
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
    val meses: List<GastoMes>,
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

        // Janela de meses do gráfico de barras (mínimo 6, máximo 24).
        val primeiraData = listOfNotNull(
            registros.minOfOrNull { it.registro.data },
            abastecimentos.minOfOrNull { it.abastecimento.data },
            despesas.minOfOrNull { it.data }
        ).minOrNull()?.let(Datas::paraLocalDate)
        val ultimoMes = YearMonth.from(hoje)
        val mesesDesdeInicio = primeiraData?.let {
            ChronoUnit.MONTHS.between(YearMonth.from(it), ultimoMes).toInt() + 1
        } ?: 1
        val quantidadeBarras = when (periodo) {
            Periodo.MES, Periodo.SEMESTRE -> 6
            Periodo.ANO -> 12
            Periodo.TUDO -> mesesDesdeInicio.coerceIn(6, 24)
        }
        val meses = (quantidadeBarras - 1 downTo 0).map { atras ->
            val mes = ultimoMes.minusMonths(atras.toLong())
            fun doMes(millis: Long) = YearMonth.from(Datas.paraLocalDate(millis)) == mes
            GastoMes(
                mes = mes,
                manutencao = registros.filter { doMes(it.registro.data) }
                    .sumOf { it.registro.valorPecasCentavos + it.registro.valorMaoDeObraCentavos },
                combustivel = abastecimentos.filter { doMes(it.abastecimento.data) }
                    .sumOf { it.abastecimento.totalCentavos },
                despesas = despesas.filter { doMes(it.data) }.sumOf { it.valorCentavos }
            )
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
            meses = meses,
            fatias = fatias,
            consumo = consumo
        )
    }

    private fun doInstante(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
}
