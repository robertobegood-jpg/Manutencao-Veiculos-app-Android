package com.roberto.manutencao.ui.telas

import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.saveable.rememberSaveable
import com.roberto.manutencao.data.AbastecimentoComConsumo
import com.roberto.manutencao.data.Despesa
import com.roberto.manutencao.data.Estatisticas
import com.roberto.manutencao.data.LeituraKm
import com.roberto.manutencao.data.Periodo
import com.roberto.manutencao.data.RegistroComItens
import com.roberto.manutencao.ui.componentes.GradeIndicadores
import com.roberto.manutencao.ui.componentes.GraficoCategorias
import com.roberto.manutencao.ui.componentes.GraficoConsumo
import com.roberto.manutencao.ui.componentes.GraficoGastos
import com.roberto.manutencao.ui.componentes.Segmentado
import com.roberto.manutencao.ui.formatarKm
import java.time.LocalDate
import com.roberto.manutencao.data.SituacaoItem
import com.roberto.manutencao.data.StatusItem
import com.roberto.manutencao.data.Veiculo
import com.roberto.manutencao.ui.componentes.AtualizarKmDialog
import com.roberto.manutencao.ui.componentes.BotaoPrimario
import com.roberto.manutencao.ui.componentes.BotaoSecundario
import com.roberto.manutencao.ui.componentes.BotaoTexto
import com.roberto.manutencao.ui.componentes.Carregando
import com.roberto.manutencao.ui.componentes.Eyebrow
import com.roberto.manutencao.ui.componentes.Hud
import com.roberto.manutencao.ui.componentes.Linha
import com.roberto.manutencao.ui.componentes.LinhaSituacao
import com.roberto.manutencao.ui.componentes.TituloDisplay
import com.roberto.manutencao.ui.componentes.marcasDeCanto
import com.roberto.manutencao.ui.descreverLeitura
import com.roberto.manutencao.ui.descricao
import com.roberto.manutencao.ui.diasDesde
import com.roberto.manutencao.ui.formatarConsumo
import com.roberto.manutencao.ui.formatarDinheiro
import com.roberto.manutencao.ui.formatarNumero
import com.roberto.manutencao.ui.plural
import com.roberto.manutencao.ui.theme.Oficina

@Composable
fun PainelScreen(
    veiculos: List<Veiculo>?,
    selecionado: Veiculo?,
    situacoes: List<SituacaoItem>?,
    ultimaLeitura: Long?,
    lembreteKmDias: Int,
    registros: List<RegistroComItens>,
    abastecimentos: List<AbastecimentoComConsumo>,
    despesas: List<Despesa>,
    leituras: List<LeituraKm>,
    onCadastrar: () -> Unit,
    onAtualizarKm: (Long, Int) -> Unit,
    onVerTodos: () -> Unit,
    onAbrirItem: (Long) -> Unit,
    onRegistrar: () -> Unit
) {
    if (veiculos == null) {
        Carregando()
        return
    }
    if (selecionado == null) {
        PrimeiroVeiculo(onCadastrar)
        return
    }

    val c = Oficina.cores
    val t = Oficina.tipos
    var mostrarDialogo by remember { mutableStateOf(false) }

    val ativos = situacoes.orEmpty().filter { it.status != StatusItem.INATIVO }
    val vencidos = ativos.count { it.status == StatusItem.VENCIDO }
    val proximos = ativos.count { it.status == StatusItem.PROXIMO }
    val emDia = ativos.count { it.status == StatusItem.EM_DIA }
    val semDados = ativos.count { it.status == StatusItem.SEM_HISTORICO }

    val titulo = when {
        situacoes == null -> " "
        ativos.isEmpty() -> "Sem itens *cadastrados*."
        vencidos > 0 -> "Hora da *troca*."
        proximos > 0 -> "Quase na *hora*."
        semDados == ativos.size -> "Falta o *histórico*."
        else -> "Tudo em *dia*."
    }
    val resumo = listOfNotNull(
        vencidos.takeIf { it > 0 }?.let { plural(it, "vencido", "vencidos") },
        proximos.takeIf { it > 0 }?.let { plural(it, "próximo", "próximos") },
        emDia.takeIf { it > 0 }?.let { "$it em dia" },
        semDados.takeIf { it > 0 }?.let { "$it sem dados" }
    ).joinToString(" · ")

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Column(Modifier.padding(16.dp)) {
            Eyebrow("01", "Painel · ${selecionado.tipo.rotulo}")
            Spacer(Modifier.height(16.dp))
            TituloDisplay(titulo)
            val linhaApoio = listOf(selecionado.descricao(), resumo).filter { it.isNotBlank() }.joinToString(". ")
            if (linhaApoio.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text("$linhaApoio.", style = t.bodyLg, color = c.muted)
            }
            Spacer(Modifier.height(24.dp))

            // Hodômetro emoldurado com marcas de canto.
            Column(
                Modifier
                    .fillMaxWidth()
                    .border(1.dp, c.line)
                    .marcasDeCanto(c.lineStrong)
                    .padding(24.dp)
            ) {
                Hud(if (selecionado.placa.isBlank()) "Hodômetro" else "Hodômetro · ${selecionado.placa}")
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(formatarNumero(selecionado.kmAtual), style = t.displayLg, color = c.inkStrong)
                    Spacer(Modifier.width(8.dp))
                    Hud("Km", Modifier.padding(bottom = 8.dp))
                }
                if (ultimaLeitura != null) {
                    val atrasado = lembreteKmDias > 0 && diasDesde(ultimaLeitura) >= lembreteKmDias
                    Spacer(Modifier.height(4.dp))
                    Hud(descreverLeitura(ultimaLeitura), cor = if (atrasado) c.ambarInk else c.muted)
                }
                Spacer(Modifier.height(16.dp))
                BotaoSecundario("Atualizar km", onClick = { mostrarDialogo = true }, modifier = Modifier.fillMaxWidth())
            }
        }

        Spacer(Modifier.height(16.dp))
        Eyebrow("02", "Próximas trocas", Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(8.dp))
        if (situacoes != null && ativos.isEmpty()) {
            Text(
                "Nenhum item com alerta.",
                style = t.body,
                color = c.muted,
                modifier = Modifier.padding(16.dp)
            )
        }
        ativos.take(4).forEach { situacao ->
            Linha()
            LinhaSituacao(situacao) { onAbrirItem(situacao.item.id) }
        }
        Linha()
        Column(Modifier.padding(16.dp)) {
            BotaoPrimario("Registrar manutenção", onClick = onRegistrar, modifier = Modifier.fillMaxWidth(), seta = true)
            if (ativos.size > 4) {
                BotaoTexto("Ver todos os ${ativos.size} itens  →", onClick = onVerTodos)
            }
        }

        Spacer(Modifier.height(8.dp))
        SecaoGastos(
            veiculo = selecionado,
            registros = registros,
            abastecimentos = abastecimentos,
            despesas = despesas,
            leituras = leituras
        )
        Spacer(Modifier.height(32.dp))
    }

    if (mostrarDialogo) {
        AtualizarKmDialog(
            kmAtual = selecionado.kmAtual,
            onDismiss = { mostrarDialogo = false },
            onConfirmar = { km ->
                onAtualizarKm(selecionado.id, km)
                mostrarDialogo = false
            }
        )
    }
}

@Composable
private fun SecaoGastos(
    veiculo: Veiculo,
    registros: List<RegistroComItens>,
    abastecimentos: List<AbastecimentoComConsumo>,
    despesas: List<Despesa>,
    leituras: List<LeituraKm>
) {
    val c = Oficina.cores
    val t = Oficina.tipos
    var periodo by rememberSaveable { mutableStateOf(Periodo.MES) }
    val resumo = remember(periodo, registros, abastecimentos, despesas, leituras, veiculo.kmAtual) {
        Estatisticas.resumir(periodo, LocalDate.now(), registros, abastecimentos, despesas, leituras, veiculo.kmAtual)
    }
    val semDados = resumo.barras.all { it.total == 0L } && resumo.gastoTotal == 0L

    Column(Modifier.padding(horizontal = 16.dp)) {
        Eyebrow("03", "Gastos")
        Spacer(Modifier.height(12.dp))
        Segmentado(
            opcoes = Periodo.entries.toList(),
            selecionada = periodo,
            rotulo = { it.rotulo },
            onSelecionar = { periodo = it }
        )
        Spacer(Modifier.height(12.dp))
        GradeIndicadores(
            listOf(
                (if (periodo == Periodo.MES) "Gasto no mês" else "Total gasto") to
                    (if (resumo.gastoTotal > 0) formatarDinheiro(resumo.gastoTotal) else "—"),
                "Custo por km" to (resumo.custoPorKm?.let { "${formatarDinheiro(Math.round(it))}/km" } ?: "—"),
                "Km rodados" to (resumo.kmRodados?.formatarKm() ?: "—"),
                "Consumo médio" to (resumo.consumoMedio?.let { formatarConsumo(it) } ?: "—")
            )
        )
        resumo.gastoMedioMensal?.let {
            Spacer(Modifier.height(8.dp))
            Text("Média de ${formatarDinheiro(it)} por mês.", style = t.bodySm, color = c.muted)
        }

        if (semDados) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Registre manutenções, abastecimentos e despesas para ver os gráficos.",
                style = t.body,
                color = c.muted
            )
        } else {
            Spacer(Modifier.height(24.dp))
            Hud(resumo.escala.rotulo)
            Spacer(Modifier.height(8.dp))
            GraficoGastos(resumo.barras)

            if (resumo.fatias.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Hud("Por categoria · ${resumo.periodo.rotulo}")
                Spacer(Modifier.height(12.dp))
                GraficoCategorias(resumo.fatias)
            }

            if (resumo.consumo.size >= 2) {
                Spacer(Modifier.height(24.dp))
                Hud("Consumo por trecho · km/l")
                Spacer(Modifier.height(12.dp))
                GraficoConsumo(resumo.consumo, resumo.consumoMedio)
            }
        }
    }
}

@Composable
private fun PrimeiroVeiculo(onCadastrar: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Spacer(Modifier.height(32.dp))
        Eyebrow("01", "Começar")
        Spacer(Modifier.height(16.dp))
        TituloDisplay("Cadastre seu primeiro *veículo*.")
        Spacer(Modifier.height(8.dp))
        Text(
            "Carro ou moto. Os itens de manutenção já vêm prontos para você ajustar.",
            style = Oficina.tipos.bodyLg,
            color = Oficina.cores.muted
        )
        Spacer(Modifier.height(24.dp))
        BotaoPrimario("Cadastrar veículo", onClick = onCadastrar, modifier = Modifier.fillMaxWidth(), seta = true)
    }
}
