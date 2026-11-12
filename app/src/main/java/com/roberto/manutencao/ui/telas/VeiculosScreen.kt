package com.roberto.manutencao.ui.telas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roberto.manutencao.data.Veiculo
import com.roberto.manutencao.ui.componentes.BotaoPrimario
import com.roberto.manutencao.ui.componentes.BotaoSecundario
import com.roberto.manutencao.ui.componentes.BotaoTexto
import com.roberto.manutencao.ui.componentes.CabecalhoInterno
import com.roberto.manutencao.ui.componentes.DialogoOficina
import com.roberto.manutencao.ui.componentes.Hud
import com.roberto.manutencao.ui.componentes.Linha
import com.roberto.manutencao.ui.componentes.PontoStatus
import com.roberto.manutencao.ui.corTipo
import com.roberto.manutencao.ui.descricao
import com.roberto.manutencao.ui.formatarKm
import com.roberto.manutencao.ui.theme.Oficina

@Composable
fun VeiculosScreen(
    veiculos: List<Veiculo>,
    selecionadoId: Long?,
    onVoltar: () -> Unit,
    onNovo: () -> Unit,
    onEditar: (Veiculo) -> Unit,
    onExcluir: (Veiculo) -> Unit,
    onSelecionar: (Long) -> Unit
) {
    var paraExcluir by remember { mutableStateOf<Veiculo?>(null) }

    Column(Modifier.fillMaxSize()) {
        CabecalhoInterno(eyebrow = "Veículos", titulo = "Seus *veículos*.", onVoltar = onVoltar)

        if (veiculos.isEmpty()) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Nenhum veículo ainda.", style = Oficina.tipos.body, color = Oficina.cores.muted)
            }
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(veiculos, key = { it.id }) { veiculo ->
                    LinhaVeiculo(
                        veiculo = veiculo,
                        selecionado = veiculo.id == selecionadoId,
                        onClick = { onSelecionar(veiculo.id) },
                        onEditar = { onEditar(veiculo) },
                        onExcluir = { paraExcluir = veiculo }
                    )
                    Linha()
                }
            }
        }

        Linha()
        BotaoPrimario(
            "Cadastrar veículo",
            onClick = onNovo,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            seta = true
        )
    }

    paraExcluir?.let { veiculo ->
        DialogoOficina(
            eyebrow = "Excluir",
            titulo = "Excluir ${veiculo.apelido}?",
            onDismiss = { paraExcluir = null },
            acoes = {
                BotaoTexto("Cancelar", onClick = { paraExcluir = null }, cor = Oficina.cores.muted)
                BotaoSecundario("Excluir", onClick = {
                    onExcluir(veiculo)
                    paraExcluir = null
                }, perigo = true)
            }
        ) {
            Text(
                "Todo o histórico deste veículo será apagado. Isso não pode ser desfeito.",
                style = Oficina.tipos.body,
                color = Oficina.cores.ink
            )
        }
    }
}

@Composable
private fun LinhaVeiculo(
    veiculo: Veiculo,
    selecionado: Boolean,
    onClick: () -> Unit,
    onEditar: () -> Unit,
    onExcluir: () -> Unit
) {
    val c = Oficina.cores
    val t = Oficina.tipos
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PontoStatus(corTipo(veiculo.tipo))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Hud(
                listOf(veiculo.tipo.rotulo, veiculo.placa, veiculo.kmAtual.formatarKm())
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
                maxLinhas = 2
            )
            Text(veiculo.apelido, style = t.heading.copy(fontSize = 18.sp, lineHeight = 24.sp), color = c.inkStrong)
            val descricao = veiculo.descricao()
            if (descricao.isNotBlank()) Text(descricao, style = t.bodySm, color = c.muted)
            if (selecionado) Hud("Selecionado", cor = c.accentInk)
        }
        IconButton(onClick = onEditar) {
            Icon(Icons.Outlined.Edit, contentDescription = "Editar", modifier = Modifier.size(18.dp), tint = c.muted)
        }
        IconButton(onClick = onExcluir) {
            Icon(Icons.Outlined.Delete, contentDescription = "Excluir", modifier = Modifier.size(18.dp), tint = c.muted)
        }
    }
}
