package com.roberto.manutencao.ui.telas

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.roberto.manutencao.data.AbastecimentoComConsumo
import com.roberto.manutencao.data.CalculoConsumo
import com.roberto.manutencao.data.Datas
import com.roberto.manutencao.data.PontoConsumo
import com.roberto.manutencao.data.Veiculo
import com.roberto.manutencao.ui.componentes.BotaoPrimario
import com.roberto.manutencao.ui.componentes.BotaoTexto
import com.roberto.manutencao.ui.componentes.Eyebrow
import com.roberto.manutencao.ui.componentes.GraficoConsumo
import com.roberto.manutencao.ui.componentes.Hud
import com.roberto.manutencao.ui.componentes.Linha
import com.roberto.manutencao.ui.componentes.TituloDisplay
import com.roberto.manutencao.ui.componentes.marcasDeCanto
import com.roberto.manutencao.ui.formatarConsumo
import com.roberto.manutencao.ui.formatarDinheiro
import com.roberto.manutencao.ui.formatarKm
import com.roberto.manutencao.ui.formatarLitros
import com.roberto.manutencao.ui.theme.Oficina
import java.time.LocalDate

@Composable
fun CombustivelScreen(
    selecionado: Veiculo?,
    abastecimentos: List<AbastecimentoComConsumo>?,
    onRegistrar: () -> Unit,
    onAbrir: (Long) -> Unit,
    onDespesas: () -> Unit
) {
    val c = Oficina.cores
    val t = Oficina.tipos

    if (selecionado == null) {
        EmBreveScreen(
            numero = "04",
            rotulo = "Posto",
            titulo = "Primeiro, o *veículo*.",
            texto = "Cadastre um carro ou moto para registrar abastecimentos."
        )
        return
    }

    val lista = abastecimentos.orEmpty()
    val media = CalculoConsumo.media(lista)
    val ultimoMedido = lista.firstOrNull { it.consumoKmL != null }?.consumoKmL
    val hoje = LocalDate.now()
    val doMes = lista.filter {
        val d = Datas.paraLocalDate(it.abastecimento.data)
        d.year == hoje.year && d.month == hoje.month
    }
    val gastoMes = doMes.sumOf { it.abastecimento.totalCentavos }
    val litrosMes = doMes.sumOf { it.abastecimento.litros }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f)) {
            item {
                Column(Modifier.padding(16.dp)) {
                    Eyebrow("04", "Posto · ${selecionado.apelido}")
                    Spacer(Modifier.height(16.dp))
                    TituloDisplay("Consumo e *abastecimentos*.")
                    Spacer(Modifier.height(24.dp))

                    // Painel de consumo emoldurado.
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .border(1.dp, c.line)
                            .marcasDeCanto(c.lineStrong)
                            .padding(24.dp)
                    ) {
                        Hud("Consumo médio")
                        Spacer(Modifier.height(8.dp))
                        if (media != null) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    String.format(java.util.Locale("pt", "BR"), "%.1f", media),
                                    style = t.displayLg,
                                    color = c.inkStrong
                                )
                                Spacer(Modifier.width(8.dp))
                                Hud("Km/l", Modifier.padding(bottom = 8.dp))
                            }
                            if (ultimoMedido != null) {
                                Hud("Último trecho · ${formatarConsumo(ultimoMedido)}")
                            }
                        } else {
                            Text(
                                "Complete o tanque em dois abastecimentos seguidos para medir.",
                                style = t.body,
                                color = c.muted
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Linha()
                        Spacer(Modifier.height(12.dp))
                        Row {
                            Column(Modifier.weight(1f)) {
                                Hud("Gasto no mês")
                                Text(formatarDinheiro(gastoMes), style = t.heading, color = c.inkStrong)
                            }
                            Column(Modifier.weight(1f)) {
                                Hud("Litros no mês")
                                Text(formatarLitros(litrosMes), style = t.heading, color = c.inkStrong)
                            }
                        }
                    }
                }
            }

            val pontos = lista.mapNotNull { a ->
                a.consumoKmL?.let { PontoConsumo(Datas.paraLocalDate(a.abastecimento.data), it) }
            }.sortedBy { it.data }
            if (pontos.size >= 2) {
                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Hud("Consumo por trecho · km/l")
                        Spacer(Modifier.height(12.dp))
                        GraficoConsumo(pontos.takeLast(20), media)
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
            if (abastecimentos != null && lista.isEmpty()) {
                item {
                    Text(
                        "Nenhum abastecimento registrado ainda.",
                        style = t.body,
                        color = c.muted,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            if (lista.isNotEmpty()) {
                item { Hud("Histórico", Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
                items(lista, key = { it.abastecimento.id }) { item ->
                    Linha()
                    LinhaAbastecimento(item) { onAbrir(item.abastecimento.id) }
                }
                item { Linha() }
            }
            item {
                BotaoTexto(
                    "IPVA, seguro e multas  →",
                    onClick = onDespesas,
                    modifier = Modifier.padding(horizontal = 4.dp),
                    cor = c.muted
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        Linha()
        BotaoPrimario(
            "Registrar abastecimento",
            onClick = onRegistrar,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            seta = true
        )
    }
}

@Composable
private fun LinhaAbastecimento(item: AbastecimentoComConsumo, onClick: () -> Unit) {
    val c = Oficina.cores
    val t = Oficina.tipos
    val a = item.abastecimento

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            Hud(
                listOfNotNull(
                    Datas.formatar(a.data),
                    a.km.formatarKm(),
                    if (a.tanqueCheio) "Cheio" else "Parcial"
                ).joinToString(" · ")
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${formatarLitros(a.litros)} de ${a.combustivel.rotulo.lowercase()} · ${formatarDinheiro(a.precoLitroMilesimos, 3)}/L",
                style = t.body,
                color = c.ink
            )
            val detalhe = listOfNotNull(
                item.consumoKmL?.let { "${formatarConsumo(it)} em ${item.kmRodados?.formatarKm()}" },
                a.posto.takeIf { it.isNotBlank() }
            ).joinToString(" · ")
            if (detalhe.isNotBlank()) {
                Text(detalhe, style = t.bodySm, color = if (item.consumoKmL != null) c.accentInk else c.muted)
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(formatarDinheiro(a.totalCentavos), style = t.body, color = c.inkStrong)
    }
}
