package com.roberto.manutencao.alertas

import android.content.Context
import com.roberto.manutencao.data.AlertasStore
import com.roberto.manutencao.data.AppDatabase
import com.roberto.manutencao.data.CalculoVencimento
import com.roberto.manutencao.data.StatusItem
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Olha todos os veículos e decide o que avisar.
 *
 * Regras para não incomodar:
 * - Avisa quando um item entra em "próximo" ou "vencido" pela primeira vez.
 * - Se ainda houver item vencido, lembra de novo a cada 7 dias.
 * - O lembrete de km sai no máximo uma vez por período escolhido.
 */
class VerificadorAlertas(
    private val context: Context,
    private val db: AppDatabase,
    private val store: AlertasStore
) {
    suspend fun verificar(forcar: Boolean) {
        if (!store.ativos.value && !forcar) return

        val hoje = LocalDate.now()
        val agora = System.currentTimeMillis()
        val veiculos = db.veiculoDao().todos()
        var avisou = false

        veiculos.forEach { veiculo ->
            // Trocas vencidas e próximas.
            val pendentes = CalculoVencimento.ordenar(
                db.manutencaoDao().itensComUltimaTroca(veiculo.id)
                    .map { CalculoVencimento.calcular(it, veiculo.kmAtual, hoje) }
            ).filter { it.status == StatusItem.VENCIDO || it.status == StatusItem.PROXIMO }

            val atual = pendentes.map { "${it.item.id}:${it.status.name}" }.toSet()
            val novos = atual - store.conjuntoAvisado(veiculo.id)
            val temVencido = pendentes.any { it.status == StatusItem.VENCIDO }
            val diasDesdeAviso = dias(agora - store.dataAviso(veiculo.id))

            val notificar = pendentes.isNotEmpty() &&
                (forcar || novos.isNotEmpty() || (temVencido && diasDesdeAviso >= 7))

            if (notificar) {
                Notificacoes.resumo(context, veiculo, pendentes)
                avisou = true
            } else if (pendentes.isEmpty()) {
                Notificacoes.cancelar(context, Notificacoes.idResumo(veiculo.id))
            }
            store.salvarAviso(veiculo.id, atual, notificou = notificar)

            // Lembrete de atualizar o hodômetro.
            val periodo = store.lembreteKmDias.value
            if (periodo > 0) {
                val ultimaLeitura = db.leituraKmDao().ultimaLeitura(veiculo.id) ?: veiculo.criadoEm
                val semLeitura = dias(agora - ultimaLeitura)
                val desdeLembrete = dias(agora - store.dataLembreteKm(veiculo.id))
                if (semLeitura >= periodo && (forcar || desdeLembrete >= periodo)) {
                    Notificacoes.lembreteKm(context, veiculo, semLeitura)
                    store.salvarLembreteKm(veiculo.id)
                    avisou = true
                }
            }
        }

        if (forcar && !avisou) Notificacoes.tudoEmDia(context, semVeiculos = veiculos.isEmpty())
    }

    private fun dias(millis: Long): Long = TimeUnit.MILLISECONDS.toDays(millis.coerceAtLeast(0))
}
