package com.roberto.manutencao.alertas

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.roberto.manutencao.MainActivity
import com.roberto.manutencao.R
import com.roberto.manutencao.data.SituacaoItem
import com.roberto.manutencao.data.StatusItem
import com.roberto.manutencao.data.Veiculo
import com.roberto.manutencao.ui.descreverSituacao
import com.roberto.manutencao.ui.plural

object Notificacoes {
    const val CANAL = "alertas_manutencao"

    const val EXTRA_VEICULO = "veiculo"
    const val EXTRA_DESTINO = "destino"
    const val DESTINO_SERVICOS = "servicos"
    const val DESTINO_PAINEL = "painel"

    private const val ID_TUDO_EM_DIA = 1
    private const val COR_ACENTO = 0xFF2D81FF.toInt()

    fun idResumo(veiculoId: Long): Int = (veiculoId * 10 + 1).toInt()
    private fun idKm(veiculoId: Long): Int = (veiculoId * 10 + 2).toInt()

    fun criarCanal(context: Context) {
        val canal = NotificationChannel(
            CANAL,
            "Alertas de manutenção",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Trocas vencidas, trocas próximas e lembretes de quilometragem."
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    /** As notificações estão liberadas para o app? */
    fun podeNotificar(context: Context): Boolean = runCatching {
        val permissao = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        permissao && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }.getOrDefault(true)

    fun cancelar(context: Context, id: Int) {
        NotificationManagerCompat.from(context).cancel(id)
    }

    /** Um resumo por veículo: quantos vencidos e próximos, com a lista dos itens. */
    fun resumo(context: Context, veiculo: Veiculo, pendentes: List<SituacaoItem>) {
        val vencidos = pendentes.count { it.status == StatusItem.VENCIDO }
        val proximos = pendentes.count { it.status == StatusItem.PROXIMO }
        val titulo = if (vencidos > 0) "${veiculo.apelido}: hora da troca." else "${veiculo.apelido}: troca chegando."
        val texto = listOfNotNull(
            vencidos.takeIf { it > 0 }?.let { plural(it, "item vencido", "itens vencidos") },
            proximos.takeIf { it > 0 }?.let { plural(it, "item próximo", "itens próximos") }
        ).joinToString(" · ") + "."

        val estilo = NotificationCompat.InboxStyle().setBigContentTitle(titulo)
        pendentes.take(6).forEach { s ->
            estilo.addLine("${s.item.nome} · ${descreverSituacao(s).removeSuffix(".")}")
        }
        if (pendentes.size > 6) estilo.setSummaryText("e mais ${pendentes.size - 6}")

        val notificacao = base(context, veiculo.id, DESTINO_SERVICOS, idResumo(veiculo.id))
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(estilo)
            .build()
        enviar(context, idResumo(veiculo.id), notificacao)
    }

    fun lembreteKm(context: Context, veiculo: Veiculo, diasSemLeitura: Long) {
        val notificacao = base(context, veiculo.id, DESTINO_PAINEL, idKm(veiculo.id))
            .setContentTitle("Atualize o km do ${veiculo.apelido}.")
            .setContentText("A última leitura foi há $diasSemLeitura dias. Os alertas dependem dela.")
            .build()
        enviar(context, idKm(veiculo.id), notificacao)
    }

    /** Usado pelo botão "Verificar agora" quando não há nada pendente. */
    fun tudoEmDia(context: Context, semVeiculos: Boolean) {
        val notificacao = base(context, 0L, DESTINO_PAINEL, ID_TUDO_EM_DIA)
            .setContentTitle(if (semVeiculos) "Nenhum veículo cadastrado." else "Tudo em dia.")
            .setContentText(
                if (semVeiculos) "Cadastre um veículo para receber alertas."
                else "Nenhuma troca vencida ou próxima. As notificações estão funcionando."
            )
            .build()
        enviar(context, ID_TUDO_EM_DIA, notificacao)
    }

    private fun base(context: Context, veiculoId: Long, destino: String, codigo: Int): NotificationCompat.Builder {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_VEICULO, veiculoId)
            putExtra(EXTRA_DESTINO, destino)
        }
        val pendente = PendingIntent.getActivity(
            context,
            codigo,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.ic_notificacao)
            .setColor(COR_ACENTO)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendente)
            .setAutoCancel(true)
    }

    @SuppressLint("MissingPermission")
    private fun enviar(context: Context, id: Int, notificacao: Notification) {
        if (podeNotificar(context)) {
            NotificationManagerCompat.from(context).notify(id, notificacao)
        }
    }
}
