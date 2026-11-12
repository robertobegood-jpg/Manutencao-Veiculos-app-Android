package com.roberto.manutencao

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.roberto.manutencao.DadosDeTeste.carro
import com.roberto.manutencao.DadosDeTeste.motoLonga
import com.roberto.manutencao.data.Periodo
import com.roberto.manutencao.data.Tema
import com.roberto.manutencao.ui.Rotas
import com.roberto.manutencao.ui.abas
import com.roberto.manutencao.ui.componentes.BarraInferior
import com.roberto.manutencao.ui.componentes.BarraSuperior
import com.roberto.manutencao.ui.componentes.Hud
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
 * Celular estreito (360 dp), alto o bastante para mostrar a tela inteira,
 * com dados extremos: nomes longos, valores altos e históricos de anos.
 * Gerados com: ./gradlew recordPaparazziDebug  (saem em app/src/test/snapshots/images)
 */
class TelasTest {

    private val estreito = DeviceConfig(
        screenWidth = 720, screenHeight = 4400, xdpi = 320, ydpi = 320,
        density = Density.XHIGH, softButtons = false
    )

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = estreito, theme = "android:Theme.Material.NoActionBar")

    private fun fonteGrande() = paparazzi.unsafeUpdateConfig(deviceConfig = estreito.copy(fontScale = 1.3f))

    private val semAlertas = AjustesAlertas(true, 15, true, {}, {}, {}, {})

    @Composable
    private fun ComTema(escuro: Boolean = true, conteudo: @Composable () -> Unit) {
        val fontes = FontesOficina(LocalContext.current)
        ManutencaoTheme(escuro = escuro, fontes = fontes) {
            Box(Modifier.fillMaxSize().background(Oficina.cores.bg)) { conteudo() }
        }
    }

    /** A tela com a barra de cima e o menu de baixo, como no celular. */
    @Composable
    private fun Celular(rota: String, escuro: Boolean = true, veiculo: com.roberto.manutencao.data.Veiculo = carro, conteudo: @Composable () -> Unit) {
        ComTema(escuro) {
            Column(Modifier.fillMaxSize()) {
                BarraSuperior(DadosDeTeste.veiculos, veiculo, {}, {})
                Box(Modifier.weight(1f)) { conteudo() }
                BarraInferior(abas, rota, {})
            }
        }
    }

    private fun painel(escuro: Boolean = true, veiculo: com.roberto.manutencao.data.Veiculo = carro, anos: Int = 2) = paparazzi.snapshot {
        Celular(Rotas.PAINEL, escuro, veiculo) {
            PainelScreen(
                veiculos = DadosDeTeste.veiculos, selecionado = veiculo,
                situacoes = DadosDeTeste.situacoesCarro(), ultimaLeitura = DadosDeTeste.instante(20), lembreteKmDias = 15,
                registros = DadosDeTeste.registros(anos), abastecimentos = DadosDeTeste.abastecimentos(anos * 12),
                despesas = DadosDeTeste.despesas(), leituras = DadosDeTeste.leituras(),
                onCadastrar = {}, onAtualizarKm = { _, _ -> }, onVerTodos = {}, onAbrirItem = {}, onRegistrar = {}
            )
        }
    }

    @Test fun painel_escuro() = painel()
    @Test fun painel_claro() = painel(escuro = false)
    @Test fun painel_moto_nome_longo() = painel(veiculo = motoLonga)
    @Test fun painel_fonte_grande() { fonteGrande(); painel() }
    @Test fun painel_vazio() = paparazzi.snapshot {
        Celular(Rotas.PAINEL) {
            PainelScreen(emptyList(), null, null, null, 15, emptyList(), emptyList(), emptyList(), emptyList(), {}, { _, _ -> }, {}, {}, {})
        }
    }

    private fun gastos(periodo: Periodo, anos: Int) = paparazzi.snapshot {
        ComTema {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 16.dp)) {
                SecaoGastos(
                    carro, DadosDeTeste.registros(anos), DadosDeTeste.abastecimentos(anos * 12),
                    DadosDeTeste.despesas(), DadosDeTeste.leituras(), periodoInicial = periodo
                )
            }
        }
    }

    @Test fun gastos_mes() = gastos(Periodo.MES, 2)
    @Test fun gastos_6meses() = gastos(Periodo.SEMESTRE, 2)
    @Test fun gastos_ano() = gastos(Periodo.ANO, 2)
    @Test fun gastos_tudo_2anos() = gastos(Periodo.TUDO, 2)
    @Test fun gastos_tudo_6anos() = gastos(Periodo.TUDO, 6)
    @Test fun gastos_tudo_10meses() = gastos(Periodo.TUDO, 1)
    @Test fun gastos_tudo_fonte_grande() { fonteGrande(); gastos(Periodo.TUDO, 2) }

    private fun telaServicos() = paparazzi.snapshot {
        Celular(Rotas.MANUTENCOES) {
            ManutencaoScreen(carro, DadosDeTeste.situacoesCarro(), DadosDeTeste.registros(1), {}, {}, {}, {}, {}, {})
        }
    }

    @Test fun servicos() = telaServicos()
    @Test fun servicos_fonte_grande() { fonteGrande(); telaServicos() }

    @Test fun compras() = paparazzi.snapshot {
        Celular(Rotas.COMPRAS) {
            ComprasScreen(carro, DadosDeTeste.compras(), 3, {}, {}, {}, {}, {}, {})
        }
    }

    @Test fun compras_fonte_grande() {
        fonteGrande()
        paparazzi.snapshot { Celular(Rotas.COMPRAS) { ComprasScreen(carro, DadosDeTeste.compras(), 3, {}, {}, {}, {}, {}, {}) } }
    }

    @Test fun posto() = paparazzi.snapshot {
        Celular(Rotas.ABASTECIMENTOS) { CombustivelScreen(carro, DadosDeTeste.abastecimentos(18), {}, {}, {}) }
    }

    @Test fun posto_vazio() = paparazzi.snapshot {
        Celular(Rotas.ABASTECIMENTOS) { CombustivelScreen(carro, emptyList(), {}, {}, {}) }
    }

    @Test fun mais() = paparazzi.snapshot {
        Celular(Rotas.MAIS) {
            MaisScreen({}, {}, {}, {}, Tema.ESCURO, {}, semAlertas, emptyList())
        }
    }

    @Test fun despesas() = paparazzi.snapshot {
        ComTema { DespesasScreen(carro, DadosDeTeste.despesas(), {}, {}, {}) }
    }

    @Test fun veiculos() = paparazzi.snapshot {
        ComTema { VeiculosScreen(DadosDeTeste.veiculos, carro.id, {}, {}, {}, {}, {}) }
    }

    /** Confere se as fontes do Oficina carregaram no ambiente dos prints. */
    @Test fun fontes() = paparazzi.snapshot {
        ComTema {
            val fontes = FontesOficina(LocalContext.current)
            Hud("Fontes faltando: ${fontes.faltando.ifEmpty { listOf("nenhuma") }.joinToString()}", Modifier.padding(16.dp))
        }
    }
}
