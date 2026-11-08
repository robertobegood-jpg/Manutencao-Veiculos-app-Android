package com.roberto.manutencao.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class StatusItem {
    VENCIDO,
    PROXIMO,
    EM_DIA,
    SEM_HISTORICO,
    INATIVO
}

/** Onde cada item está em relação à próxima troca. */
data class SituacaoItem(
    val item: ItemManutencao,
    val ultimoKm: Int?,
    val ultimaData: Long?,
    val proximoKm: Int?,
    val proximaData: LocalDate?,
    val kmRestante: Int?,
    val diasRestantes: Long?,
    /** 0 = acabou de trocar, 1 = chegou a hora. Pode passar de 1. */
    val progresso: Float,
    val status: StatusItem
)

object CalculoVencimento {

    /** A partir de quanto do intervalo usado o item fica "próximo" (90% = faltam 10%). */
    private const val LIMITE_PROXIMO = 0.9f

    fun calcular(dado: ItemComUltimaTroca, kmAtual: Int, hoje: LocalDate = LocalDate.now()): SituacaoItem {
        val item = dado.item
        val intervaloKm = item.intervaloKm
        val intervaloMeses = item.intervaloMeses

        // A troca mais recente: registro no app ou a informada à mão.
        val ultimoKm = listOfNotNull(dado.registroKm, item.baseKm).maxOrNull()
        val ultimaData = listOfNotNull(dado.registroData, item.baseData).maxOrNull()

        val proximoKm = if (ultimoKm != null && intervaloKm != null) ultimoKm + intervaloKm else null
        val dataUltima = ultimaData?.let(Datas::paraLocalDate)
        val proximaData = if (dataUltima != null && intervaloMeses != null) {
            dataUltima.plusMonths(intervaloMeses.toLong())
        } else null

        val kmRestante = proximoKm?.let { it - kmAtual }
        val diasRestantes = proximaData?.let { ChronoUnit.DAYS.between(hoje, it) }

        val usoKm = if (kmRestante != null && intervaloKm != null && intervaloKm > 0) {
            1f - kmRestante.toFloat() / intervaloKm
        } else null
        val usoTempo = if (diasRestantes != null && dataUltima != null && proximaData != null) {
            val total = ChronoUnit.DAYS.between(dataUltima, proximaData).coerceAtLeast(1)
            1f - diasRestantes.toFloat() / total
        } else null
        val progresso = listOfNotNull(usoKm, usoTempo).maxOrNull() ?: 0f

        val status = when {
            !item.ativo -> StatusItem.INATIVO
            proximoKm == null && proximaData == null -> StatusItem.SEM_HISTORICO
            (kmRestante != null && kmRestante <= 0) || (diasRestantes != null && diasRestantes <= 0) -> StatusItem.VENCIDO
            progresso >= LIMITE_PROXIMO -> StatusItem.PROXIMO
            else -> StatusItem.EM_DIA
        }

        return SituacaoItem(
            item = item,
            ultimoKm = ultimoKm,
            ultimaData = ultimaData,
            proximoKm = proximoKm,
            proximaData = proximaData,
            kmRestante = kmRestante,
            diasRestantes = diasRestantes,
            progresso = progresso,
            status = status
        )
    }

    /** Mais urgentes primeiro. */
    fun ordenar(lista: List<SituacaoItem>): List<SituacaoItem> =
        lista.sortedWith(compareBy<SituacaoItem> { it.status.ordinal }.thenByDescending { it.progresso })
}
