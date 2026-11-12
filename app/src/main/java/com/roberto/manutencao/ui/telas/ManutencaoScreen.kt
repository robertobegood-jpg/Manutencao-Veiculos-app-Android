package com.roberto.manutencao.ui.telas

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.roberto.manutencao.data.Datas
import com.roberto.manutencao.data.RegistroComItens
import com.roberto.manutencao.data.SituacaoItem
import com.roberto.manutencao.data.Veiculo
import com.roberto.manutencao.data.totalCentavos
import com.roberto.manutencao.ui.componentes.BotaoPrimario
import com.roberto.manutencao.ui.componentes.BotaoSecundario
import com.roberto.manutencao.ui.componentes.BotaoTexto
import com.roberto.manutencao.ui.componentes.Eyebrow
import com.roberto.manutencao.ui.componentes.Hud
import com.roberto.manutencao.ui.componentes.Linha
import com.roberto.manutencao.ui.componentes.LinhaSituacao
import com.roberto.manutencao.ui.componentes.Segmentado
import com.roberto.manutencao.ui.componentes.TituloDisplay
import com.roberto.manutencao.ui.formatarDinheiro
import com.roberto.manutencao.ui.formatarKm
import com.roberto.manutencao.ui.plural
import com.roberto.manutencao.ui.theme.Oficina

private enum class AbaServicos(val rotulo: String) {
    ITENS("Itens"),
    HISTORICO("Histórico")
}

@Composable
fun ManutencaoScreen(
    selecionado: Veiculo?,
    situacoes: List<SituacaoItem>?,
    registros: List<RegistroComItens>?,
    onCadastrarVeiculo: () -> Unit,
    onAbrirItem: (Long) -> Unit,
    onNovoItem: () -> Unit,
    onAdicionarSugeridos: () -> Unit,
    onRegistrar: () -> Unit,
    onAbrirRegistro: (Long) -> Unit,
    /** Abrir já no histórico (usado nos prints da revisão). */
    historicoInicial: Boolean = false
) {
    val c = Oficina.cores
    val t = Oficina.tipos

    if (selecionado == null) {
        EmBreveScreen(
            numero = "02",
            rotulo = "Serviços",
            titulo = "Primeiro, o *veículo*.",
            texto = "Cadastre um carro ou moto para ver os itens de manutenção."
        )
        return
    }

    var aba by rememberSaveable { mutableStateOf(if (historicoInicial) AbaServicos.HISTORICO else AbaServicos.ITENS) }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f)) {
            item {
                Column(Modifier.padding(16.dp)) {
                    Eyebrow("02", "Serviços · ${selecionado.apelido}")
                    Spacer(Modifier.height(16.dp))
                    TituloDisplay("Trocas e *revisões*.")
                    Spacer(Modifier.height(16.dp))
                    Segmentado(
                        opcoes = AbaServicos.entries.toList(),
                        selecionada = aba,
                        rotulo = { it.rotulo },
                        onSelecionar = { aba = it }
                    )
                }
            }

            when (aba) {
                AbaServicos.ITENS -> {
                    val lista = situacoes
                    if (lista != null && lista.isEmpty()) {
                        item {
                            Column(Modifier.padding(16.dp)) {
                                Text("Nenhum item cadastrado.", style = t.body, color = c.muted)
                                Spacer(Modifier.height(12.dp))
                                BotaoSecundario(
                                    "Adicionar itens sugeridos",
                                    onClick = onAdicionarSugeridos,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    if (!lista.isNullOrEmpty()) {
                        items(lista, key = { it.item.id }) { situacao ->
                            Linha()
                            LinhaSituacao(situacao) { onAbrirItem(situacao.item.id) }
                        }
                        item {
                            Linha()
                            Row(Modifier.padding(horizontal = 4.dp)) {
                                BotaoTexto("+  Novo item", onClick = onNovoItem)
                                BotaoTexto("Repor sugeridos", onClick = onAdicionarSugeridos, cor = c.muted)
                            }
                        }
                    }
                }

                AbaServicos.HISTORICO -> {
                    val lista = registros
                    if (lista != null && lista.isEmpty()) {
                        item {
                            Text(
                                "Nenhuma manutenção registrada ainda.",
                                style = t.body,
                                color = c.muted,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    if (!lista.isNullOrEmpty()) {
                        item {
                            val total = lista.sumOf { it.totalCentavos() }
                            Text(
                                "${plural(lista.size, "registro", "registros")} · ${formatarDinheiro(total)} no total.",
                                style = t.bodySm,
                                color = c.muted,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                            )
                        }
                        items(lista, key = { it.registro.id }) { registro ->
                            Linha()
                            LinhaRegistro(registro) { onAbrirRegistro(registro.registro.id) }
                        }
                        item { Linha() }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }

        Linha()
        BotaoPrimario(
            "Registrar manutenção",
            onClick = onRegistrar,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            seta = true
        )
    }
}

@Composable
private fun LinhaRegistro(registro: RegistroComItens, onClick: () -> Unit) {
    val c = Oficina.cores
    val t = Oficina.tipos
    val r = registro.registro
    val total = registro.totalCentavos()

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
                    Datas.formatar(r.data),
                    r.km.formatarKm(),
                    registro.anexos.size.takeIf { it > 0 }?.let { if (it == 1) "1 foto" else "$it fotos" }
                ).joinToString(" · ")
            )
            Spacer(Modifier.height(4.dp))
            Text(
                registro.itens.joinToString(", ") { it.nome }.ifBlank { "Sem itens marcados" },
                style = t.body,
                color = c.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val onde = if (r.feitoPorMim) "Feito por mim" else r.local
            if (onde.isNotBlank()) Text(onde, style = t.bodySm, color = c.muted)
        }
        if (total > 0) {
            Spacer(Modifier.width(12.dp))
            Text(formatarDinheiro(total), style = t.body, color = c.inkStrong)
        }
    }
}
