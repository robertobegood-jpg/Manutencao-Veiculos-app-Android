package com.roberto.manutencao

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roberto.manutencao.alertas.Notificacoes
import com.roberto.manutencao.data.Tema
import com.roberto.manutencao.ui.AppRoot
import com.roberto.manutencao.ui.theme.ManutencaoTheme
import kotlinx.coroutines.flow.MutableStateFlow

/** Para onde ir quando o app é aberto por uma notificação. */
data class DestinoNotificacao(val veiculoId: Long, val destino: String)

class MainActivity : ComponentActivity() {

    private val destino = MutableStateFlow<DestinoNotificacao?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as ManutencaoApp).container
        lerDestino(intent)

        setContent {
            val tema by container.temaStore.tema.collectAsStateWithLifecycle()
            val destinoAtual by destino.collectAsStateWithLifecycle()
            val escuro = when (tema) {
                Tema.ESCURO -> true
                Tema.CLARO -> false
                Tema.SISTEMA -> isSystemInDarkTheme()
            }

            // Ícones da barra de status acompanham o tema.
            DisposableEffect(escuro) {
                val estilo = if (escuro) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = estilo, navigationBarStyle = estilo)
                onDispose { }
            }

            ManutencaoTheme(escuro = escuro, fontes = container.fontes) {
                AppRoot(
                    tema = tema,
                    onTema = container.temaStore::definir,
                    fontesFaltando = container.fontes.faltando,
                    destino = destinoAtual,
                    onDestinoConsumido = { destino.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        lerDestino(intent)
    }

    private fun lerDestino(intent: Intent?) {
        val alvo = intent?.getStringExtra(Notificacoes.EXTRA_DESTINO) ?: return
        destino.value = DestinoNotificacao(intent.getLongExtra(Notificacoes.EXTRA_VEICULO, 0L), alvo)
        // Evita repetir a navegação se a tela for recriada (ex.: girar o celular).
        intent.removeExtra(Notificacoes.EXTRA_DESTINO)
    }
}
