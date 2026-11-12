package com.roberto.manutencao.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.roberto.manutencao.data.Veiculo
import com.roberto.manutencao.ui.corTipo
import com.roberto.manutencao.ui.theme.Oficina

data class AbaNav(val rota: String, val rotulo: String, val icone: ImageVector)

/** Topo das abas principais: seletor de veículo à esquerda, marca à direita. */
@Composable
fun BarraSuperior(
    veiculos: List<Veiculo>,
    selecionado: Veiculo?,
    onSelecionar: (Long) -> Unit,
    onGerenciar: () -> Unit
) {
    val c = Oficina.cores
    Column(
        Modifier
            .fillMaxWidth()
            .background(c.bg)
            .statusBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SeletorVeiculo(veiculos, selecionado, onSelecionar, onGerenciar, Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            // Marca do app: Archivo expandido com a palavra de emoção em serif itálico.
            TituloDisplay(
                "Em *dia*",
                estilo = Oficina.tipos.heading.copy(fontSize = 18.sp, lineHeight = 20.sp, letterSpacing = (-0.01).em),
                cor = c.ink
            )
        }
        Linha()
    }
}

/** Mostra o veículo atual; ao tocar, abre a lista. Trocar aqui atualiza todas as telas. */
@Composable
fun SeletorVeiculo(
    veiculos: List<Veiculo>,
    selecionado: Veiculo?,
    onSelecionar: (Long) -> Unit,
    onGerenciar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = Oficina.cores
    val t = Oficina.tipos
    var aberto by remember { mutableStateOf(false) }

    Box(modifier) {
        Column(
            Modifier
                .clickable(role = Role.DropdownList) { aberto = true }
                .padding(vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selecionado != null) {
                    PontoStatus(corTipo(selecionado.tipo), tamanho = 6.dp)
                    Spacer(Modifier.width(6.dp))
                    Hud("Veículo · ${selecionado.tipo.rotulo}")
                } else {
                    Hud("Veículo")
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    selecionado?.apelido ?: "Nenhum",
                    style = t.heading.copy(fontSize = 20.sp, lineHeight = 26.sp),
                    color = c.inkStrong,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(8.dp))
                Text("▾", style = t.body, color = c.muted)
            }
        }

        DropdownMenu(
            expanded = aberto,
            onDismissRequest = { aberto = false },
            shape = RectangleShape,
            containerColor = c.bgRaised,
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, c.line)
        ) {
            veiculos.forEach { veiculo ->
                DropdownMenuItem(
                    text = { Text(veiculo.apelido, style = t.body, color = c.ink) },
                    leadingIcon = { PontoStatus(corTipo(veiculo.tipo)) },
                    trailingIcon = {
                        if (veiculo.id == selecionado?.id) Text("✓", style = t.body, color = c.ink)
                    },
                    onClick = {
                        onSelecionar(veiculo.id)
                        aberto = false
                    }
                )
            }
            if (veiculos.isNotEmpty()) Linha()
            DropdownMenuItem(
                text = { Hud("Gerenciar veículos  →", cor = c.accentInk) },
                onClick = {
                    aberto = false
                    onGerenciar()
                }
            )
        }
    }
}

/**
 * Menu inferior: a aba ativa vira bloco invertido.
 * [marcas] coloca um ponto de status no ícone de uma aba (rota → cor).
 */
@Composable
fun BarraInferior(
    abas: List<AbaNav>,
    rotaAtual: String?,
    onAba: (String) -> Unit,
    marcas: Map<String, Color> = emptyMap()
) {
    val c = Oficina.cores
    Column(
        Modifier
            .fillMaxWidth()
            .background(c.bg)
    ) {
        Linha()
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp)
        ) {
            abas.forEach { aba ->
                val ativa = aba.rota == rotaAtual
                val frente = if (ativa) c.invertFg else c.muted
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (ativa) c.invertBg else Color.Transparent)
                        .clickable(role = Role.Tab) { onAba(aba.rota) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box {
                        Icon(aba.icone, contentDescription = null, modifier = Modifier.size(20.dp), tint = frente)
                        marcas[aba.rota]?.let { cor ->
                            PontoStatus(
                                cor,
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-2).dp),
                                tamanho = 7.dp
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Hud(aba.rotulo, cor = frente, estilo = Oficina.tipos.hud.copy(letterSpacing = 0.08.em))
                }
            }
        }
    }
}
