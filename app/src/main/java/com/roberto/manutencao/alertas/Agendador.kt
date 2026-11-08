package com.roberto.manutencao.alertas

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.roberto.manutencao.ManutencaoApp
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** Tarefa que o Android executa em segundo plano, mesmo com o app fechado. */
class VerificacaoWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as ManutencaoApp
        val forcar = inputData.getBoolean(CHAVE_FORCAR, false)
        return try {
            app.container.verificadorAlertas.verificar(forcar)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val CHAVE_FORCAR = "forcar"
    }
}

object Agendador {
    private const val NOME_DIARIO = "verificacao_diaria"
    private const val NOME_AGORA = "verificacao_agora"
    private const val HORA_DO_AVISO = 9

    /** Agenda uma verificação por dia, começando no próximo horário das 9h. */
    fun agendar(context: Context, substituir: Boolean = false) {
        val agora = LocalDateTime.now()
        var alvo = agora.toLocalDate().atTime(HORA_DO_AVISO, 0)
        if (!alvo.isAfter(agora)) alvo = alvo.plusDays(1)
        val atrasoMinutos = Duration.between(agora, alvo).toMinutes()

        val pedido = PeriodicWorkRequestBuilder<VerificacaoWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(atrasoMinutos, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            NOME_DIARIO,
            if (substituir) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP,
            pedido
        )
    }

    fun cancelar(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(NOME_DIARIO)
    }

    /** Roda uma verificação agora e sempre mostra o resultado (para testar). */
    fun verificarAgora(context: Context) {
        val pedido = OneTimeWorkRequestBuilder<VerificacaoWorker>()
            .setInputData(workDataOf(VerificacaoWorker.CHAVE_FORCAR to true))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(NOME_AGORA, ExistingWorkPolicy.REPLACE, pedido)
    }
}
