package com.roberto.manutencao

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityOptionsCompat
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.roberto.manutencao.DadosDeTeste.carro
import com.roberto.manutencao.DadosDeTeste.motoLonga
import com.roberto.manutencao.data.Periodo
import com.roberto.manutencao.data.Tema
import com.roberto.manutencao.data.Veiculo
import com.roberto.manutencao.ui.Rotas
import com.roberto.manutencao.ui.abas
import com.roberto.manutencao.ui.componentes.BarraInferior
import com.roberto.manutencao.ui.componentes.BarraSuperior
import com.roberto.manutencao.ui.componentes.Hud
import com.roberto.manutencao.ui.componentes.TituloDisplay
import com.roberto.manutencao.ui.telas.AjustesAlertas
import com.roberto.manutencao.ui.telas.CombustivelScreen
import com.roberto.manutencao.ui.telas.ComprasScreen
import com.roberto.manutencao.ui.telas.DespesasScreen
import com.roberto.manutencao.ui.telas.MaisScreen
import com.roberto.manutencao.ui.telas.ManutencaoScreen
import com.roberto.manutencao.ui.telas.PainelScreen
import com.roberto.manutencao.ui.telas.SecaoGastos
import com.roberto.manutencao.ui.telas.VeiculosScreen
import com.roberto.manutencao.ui.theme.FontesOficina
import com.roberto.manutencao.ui.theme.ManutencaoTheme
import com.roberto.manutencao.ui.theme.Oficina
import org.junit.Rule
import org.junit.Test

/**
 * Prints automáticos para a revisão antes de cada versão.
 * Celular estreito (360 dp de largura) com dados extremos: nomes longos, valores altos
 * e históricos de anos. Cada tela é desenhada inteira e fotografada em fatias de 500 dp
 * (o Paparazzi reduz imagens maiores que 1000 px, o que deixaria o texto ilegível).
 * Gerados com: ./gradlew recordPaparazziDebug  (saem em app/src/test/snapshots/images)
 */
class TelasTest {

    private val celular = DeviceConfig(
        screenWidth = 720, screenHeight = 1000, xdpi = 320, ydpi = 320,
        density = Density.XHIGH, softButtons = false
    )
    private val alturaFatia = 500.dp

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = celular, theme = "android:Theme.Material.NoActionBar")

    private fun fonteGrande() = paparazzi.unsafeUpdateConfig(deviceConfig = celular.copy(fontScale = 1.3f))

    private val semAlertas = AjustesAlertas(true, 15, true, {}, {}, {}, {})

    @Composable
    private fun ComTema(escuro: Boolean, conteudo: @Composable () -> Unit) {
        val fontes = FontesOficina(LocalContext.current)
        // Telas com seletores de arquivo/permissão precisam de um "dono" de resultados, como uma Activity.
        val dono = object : ActivityResultRegistryOwner {
            override val activityResultRegistry = object : ActivityResultRegistry() {
                override fun <I, O> onLaunch(
                    requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?
                ) = Unit
            }
        }
        CompositionLocalProvider(LocalActivityResultRegistryOwner provides dono) {
            ManutencaoTheme(escuro = escuro, fontes = fontes) {
                Box(Modifier.fillMaxSize().background(Oficina.cores.bg)) { conteudo() }
            }
        }
    }

    /** Desenha [conteudo] com [alturaTotal] e tira uma foto de cada fatia de 500 dp. */
    private fun fatias(nome: String, alturaTotal: Dp, escuro: Boolean = true, conteudo: @Composable () -> Unit) {
        val quantidade = ((alturaTotal.value + alturaFatia.value - 1) / alturaFatia.value).toInt()
        for (k in 0 until quantidade) {
            paparazzi.snapshot(name = "${nome}_${k + 1}") {
                ComTema(escuro) {
                    Box(Modifier.fillMaxSize().clipToBounds()) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(align = Alignment.Top, unbounded = true)
                                .requiredHeight(alturaTotal)
                                .offset(y = -alturaFatia * k)
                        ) { conteudo() }
                    }
                }
            }
        }
    }

    /** A tela com a barra de cima e o menu de baixo, como no celular. */
    @Composable
    private fun Celular(rota: String, veiculo: Veiculo = carro, conteudo: @Composable () -> Unit) {
        Column(Modifier.fillMaxSize().background(Oficina.cores.bg)) {
            BarraSuperior(DadosDeTeste.veiculos, veiculo, {}, {})
            Box(Modifier.weight(1f)) { conteudo() }
            BarraInferior(abas, rota, {})
        }
    }

    @Composable
    private fun Painel(veiculo: Veiculo = carro, anos: Int = 2) {
        Celular(Rotas.PAINEL, veiculo) {
            PainelScreen(
                veiculos = DadosDeTeste.veiculos, selecionado = veiculo,
                situacoes = DadosDeTeste.situacoesCarro(), ultimaLeitura = DadosDeTeste.instante(20), lembreteKmDias = 15,
                registros = DadosDeTeste.registros(anos), abastecimentos = DadosDeTeste.abastecimentos(anos * 12),
                despesas = DadosDeTeste.despesas(), leituras = DadosDeTeste.leituras(),
                onCadastrar = {}, onAtualizarKm = { _, _ -> }, onVerTodos = {}, onAbrirItem = {}, onRegistrar = {}
            )
        }
    }

    @Test fun painel() = fatias("painel", 2300.dp) { Painel() }
    @Test fun painel_claro() = fatias("painel_claro", 1000.dp, escuro = false) { Painel() }
    @Test fun painel_moto() = fatias("painel_moto", 1000.dp) { Painel(veiculo = motoLonga) }
    @Test fun painel_fonte_grande() { fonteGrande(); fatias("painel_fonte_grande", 2500.dp) { Painel() } }
    @Test fun painel_vazio() = fatias("painel_vazio", 500.dp) {
        Celular(Rotas.PAINEL) {
            PainelScreen(emptyList(), null, null, null, 15, emptyList(), emptyList(), emptyList(), emptyList(), {}, { _, _ -> }, {}, {}, {})
        }
    }

    private fun gastos(nome: String, periodo: Periodo, anos: Int) = fatias(nome, 1500.dp) {
        Column(Modifier.padding(vertical = 16.dp)) {
            SecaoGastos(
                carro, DadosDeTeste.registros(anos), DadosDeTeste.abastecimentos(anos * 12),
                DadosDeTeste.despesas(), DadosDeTeste.leituras(), periodoInicial = periodo
            )
        }
    }

    @Test fun gastos_mes() = gastos("gastos_mes", Periodo.MES, 2)
    @Test fun gastos_6meses() = gastos("gastos_6meses", Periodo.SEMESTRE, 2)
    @Test fun gastos_ano() = gastos("gastos_ano", Periodo.ANO, 2)
    @Test fun gastos_tudo_10meses() = gastos("gastos_tudo_10meses", Periodo.TUDO, 1)
    @Test fun gastos_tudo_2anos() = gastos("gastos_tudo_2anos", Periodo.TUDO, 2)
    @Test fun gastos_tudo_6anos() = gastos("gastos_tudo_6anos", Periodo.TUDO, 6)
    @Test fun gastos_fonte_grande() { fonteGrande(); gastos("gastos_fonte_grande", Periodo.TUDO, 2) }

    @Composable
    private fun Servicos() = Celular(Rotas.MANUTENCOES) {
        ManutencaoScreen(carro, DadosDeTeste.situacoesCarro(), DadosDeTeste.registros(1), {}, {}, {}, {}, {}, {})
    }

    @Test fun servicos() = fatias("servicos", 1500.dp) { Servicos() }
    @Test fun servicos_historico() = fatias("servicos_historico", 1500.dp) {
        Celular(Rotas.MANUTENCOES) {
            ManutencaoScreen(carro, DadosDeTeste.situacoesCarro(), DadosDeTeste.registros(1), {}, {}, {}, {}, {}, {}, historicoInicial = true)
        }
    }
    @Test fun servicos_fonte_grande() { fonteGrande(); fatias("servicos_fonte_grande", 1500.dp) { Servicos() } }

    @Composable
    private fun Compras() = Celular(Rotas.COMPRAS) {
        ComprasScreen(carro, DadosDeTeste.compras(), 3, {}, {}, {}, {}, {}, {})
    }

    @Test fun compras() = fatias("compras", 1500.dp) { Compras() }
    @Test fun compras_comprados() = fatias("compras_comprados", 1000.dp) {
        Celular(Rotas.COMPRAS) { ComprasScreen(carro, DadosDeTeste.compras(), 3, {}, {}, {}, {}, {}, {}, compradosInicial = true) }
    }
    @Test fun compras_fonte_grande() { fonteGrande(); fatias("compras_fonte_grande", 1500.dp) { Compras() } }

    @Test fun posto() = fatias("posto", 1500.dp) {
        Celular(Rotas.ABASTECIMENTOS) { CombustivelScreen(carro, DadosDeTeste.abastecimentos(18), {}, {}, {}) }
    }

    @Test fun posto_vazio() = fatias("posto_vazio", 1000.dp) {
        Celular(Rotas.ABASTECIMENTOS) { CombustivelScreen(carro, emptyList(), {}, {}, {}) }
    }

    @Test fun mais() = fatias("mais", 1500.dp) {
        Celular(Rotas.MAIS) { MaisScreen({}, {}, {}, {}, Tema.ESCURO, {}, semAlertas, emptyList()) }
    }

    @Test fun despesas() = fatias("despesas", 1000.dp) { DespesasScreen(carro, DadosDeTeste.despesas(), {}, {}, {}) }

    @Test fun veiculos() = fatias("veiculos", 1000.dp) { VeiculosScreen(DadosDeTeste.veiculos, carro.id, {}, {}, {}, {}, {}) }

    /** Confere se as fontes do Oficina carregaram no ambiente dos prints. */
    @Test fun fontes() = fatias("fontes", 500.dp) {
        val fontes = FontesOficina(LocalContext.current)
        Column(Modifier.padding(16.dp)) {
            Hud("Fontes faltando: ${fontes.faltando.ifEmpty { listOf("nenhuma") }.joinToString()}")
            TituloDisplay("Archivo expandido com *serif* itálico.")
        }
    }
}
