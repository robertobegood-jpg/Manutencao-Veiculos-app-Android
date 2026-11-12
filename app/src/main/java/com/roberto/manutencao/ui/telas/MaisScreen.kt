package com.roberto.manutencao.ui.telas

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.roberto.manutencao.Cafezinho
import com.roberto.manutencao.alertas.Notificacoes
import com.roberto.manutencao.data.Tema
import com.roberto.manutencao.ui.componentes.BotaoSecundario
import com.roberto.manutencao.ui.componentes.Eyebrow
import com.roberto.manutencao.ui.componentes.Hud
import com.roberto.manutencao.ui.componentes.Linha
import com.roberto.manutencao.ui.componentes.LinhaMarcavel
import com.roberto.manutencao.ui.componentes.Segmentado
import com.roberto.manutencao.ui.componentes.TituloDisplay
import com.roberto.manutencao.ui.theme.Oficina

/** Ajustes de alerta exibidos em "Mais". */
data class AjustesAlertas(
    val ativos: Boolean,
    val lembreteKmDias: Int,
    val pediuPermissao: Boolean,
    val onAtivos: (Boolean) -> Unit,
    val onLembreteKm: (Int) -> Unit,
    val onPermissaoPedida: () -> Unit,
    val onVerificarAgora: () -> Unit
)

private val OPCOES_LEMBRETE = listOf(7, 15, 30, 0)

@Composable
fun MaisScreen(
    onVeiculos: () -> Unit,
    onDespesas: () -> Unit,
    onDados: () -> Unit,
    onCafezinho: () -> Unit,
    tema: Tema,
    onTema: (Tema) -> Unit,
    alertas: AjustesAlertas,
    fontesFaltando: List<String>
) {
    val c = Oficina.cores
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Column(Modifier.padding(16.dp)) {
            Eyebrow("05", "Mais")
            Spacer(Modifier.height(16.dp))
            TituloDisplay("Ajustes e *dados*.")
        }

        Linha()
        ItemMenu("Meus veículos", "Cadastrar, editar e excluir.", onClick = onVeiculos)
        Linha()
        ItemMenu("Despesas avulsas", "IPVA, seguro, multas e pedágio.", onClick = onDespesas)
        Linha()
        ItemMenu("Relatórios", "PDF do veículo e planilhas CSV.", onClick = onDados)
        Linha()
        ItemMenu("Backup e restauração", "Todos os dados e fotos num arquivo.", onClick = onDados)
        Linha()
        if (Cafezinho.configurado) {
            ItemMenu("Pague um cafezinho", "Apoie quem fez este app, via Pix.", onClick = onCafezinho)
            Linha()
        }

        SecaoAlertas(alertas)
        Linha()

        Column(Modifier.padding(16.dp)) {
            Hud("Tema")
            Spacer(Modifier.height(8.dp))
            Segmentado(
                opcoes = Tema.entries.toList(),
                selecionada = tema,
                rotulo = { it.rotulo },
                onSelecionar = onTema
            )
        }
        Linha()

        Column(Modifier.padding(16.dp)) {
            Hud("Fontes Oficina")
            Spacer(Modifier.height(4.dp))
            Text(
                if (fontesFaltando.isEmpty()) "Instaladas."
                else "Faltam ${fontesFaltando.joinToString(", ")}. Veja assets/fonts/LEIA-ME.txt no projeto.",
                style = Oficina.tipos.bodySm,
                color = if (fontesFaltando.isEmpty()) c.verdeInk else c.muted
            )
        }
        Linha()
        val contexto = LocalContext.current
        val versao = remember {
            runCatching { contexto.packageManager.getPackageInfo(contexto.packageName, 0).versionName }.getOrNull()
        }
        Hud(
            "Em Dia${versao?.let { " · versão $it" }.orEmpty()} · código aberto no GitHub",
            Modifier.padding(16.dp),
            cor = c.faint
        )
    }
}

@Composable
private fun SecaoAlertas(alertas: AjustesAlertas) {
    val c = Oficina.cores
    val t = Oficina.tipos
    val contexto = LocalContext.current

    var permitido by remember { mutableStateOf(Notificacoes.podeNotificar(contexto)) }
    // Revê a permissão sempre que você volta para o app (ex.: depois das configurações).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permitido = Notificacoes.podeNotificar(contexto)
    }
    val pedirPermissao = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permitido = Notificacoes.podeNotificar(contexto)
    }
    val podePerguntar = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !alertas.pediuPermissao

    fun liberarNotificacoes() {
        if (podePerguntar) {
            alertas.onPermissaoPedida()
            pedirPermissao.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            contexto.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, contexto.packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    Column(Modifier.padding(16.dp)) {
        Hud("Alertas")
        LinhaMarcavel(
            texto = "Avisar sobre trocas",
            marcado = alertas.ativos,
            onAlternar = { alertas.onAtivos(!alertas.ativos) },
            subtitulo = "Uma verificação por dia, por volta das 9h."
        )

        if (alertas.ativos && !permitido) {
            Text(
                "As notificações estão bloqueadas neste celular.",
                style = t.bodySm,
                color = c.ambarInk
            )
            Spacer(Modifier.height(8.dp))
            BotaoSecundario(
                if (podePerguntar) "Permitir notificações" else "Abrir configurações",
                onClick = ::liberarNotificacoes,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(8.dp))
        Hud("Lembrar de atualizar o km")
        Spacer(Modifier.height(8.dp))
        Segmentado(
            opcoes = OPCOES_LEMBRETE,
            selecionada = alertas.lembreteKmDias,
            rotulo = { if (it == 0) "Nunca" else "$it dias" },
            onSelecionar = alertas.onLembreteKm
        )

        Spacer(Modifier.height(12.dp))
        BotaoSecundario(
            "Verificar agora",
            onClick = alertas.onVerificarAgora,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Mostra uma notificação com a situação atual, para você testar.",
            style = t.bodySm,
            color = c.muted
        )
    }
}

@Composable
private fun ItemMenu(
    titulo: String,
    subtitulo: String,
    etiqueta: String? = null,
    onClick: (() -> Unit)? = null
) {
    val c = Oficina.cores
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(titulo, style = Oficina.tipos.body, color = if (onClick != null) c.ink else c.muted)
            Text(subtitulo, style = Oficina.tipos.bodySm, color = c.muted)
        }
        Spacer(Modifier.width(12.dp))
        if (etiqueta != null) Hud(etiqueta, cor = c.faint) else Text("→", style = Oficina.tipos.body, color = c.ink)
    }
}

/** Tela provisória para as abas que ainda vão ser construídas. */
@Composable
fun EmBreveScreen(numero: String, rotulo: String, titulo: String, texto: String) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Eyebrow(numero, rotulo)
        Spacer(Modifier.height(16.dp))
        TituloDisplay(titulo)
        Spacer(Modifier.height(8.dp))
        Text(texto, style = Oficina.tipos.bodyLg, color = Oficina.cores.muted)
    }
}
