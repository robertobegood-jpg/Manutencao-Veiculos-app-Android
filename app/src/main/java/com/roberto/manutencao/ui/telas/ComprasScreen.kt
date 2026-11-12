package com.roberto.manutencao.ui.telas

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.roberto.manutencao.data.ItemCompra
import com.roberto.manutencao.data.Veiculo
import com.roberto.manutencao.data.subtotalCentavos
import com.roberto.manutencao.ui.componentes.BotaoPrimario
import com.roberto.manutencao.ui.componentes.BotaoSecundario
import com.roberto.manutencao.ui.componentes.BotaoTexto
import com.roberto.manutencao.ui.componentes.CaixaMarcacao
import com.roberto.manutencao.ui.componentes.DialogoOficina
import com.roberto.manutencao.ui.componentes.Eyebrow
import com.roberto.manutencao.ui.componentes.Hud
import com.roberto.manutencao.ui.componentes.Linha
import com.roberto.manutencao.ui.componentes.Segmentado
import com.roberto.manutencao.ui.componentes.TituloDisplay
import com.roberto.manutencao.ui.descreverQuantidade
import com.roberto.manutencao.ui.formatarDinheiro
import com.roberto.manutencao.ui.plural
import com.roberto.manutencao.ui.textoListaCompras
import com.roberto.manutencao.ui.theme.Oficina

private enum class AbaCompras(val rotulo: String) {
    PENDENTES("A comprar"),
    COMPRADOS("Comprados")
}

@Composable
fun ComprasScreen(
    selecionado: Veiculo?,
    compras: List<ItemCompra>?,
    trocasPendentes: Int,
    onNovaPeca: () -> Unit,
    onAbrir: (Long) -> Unit,
    onAlternar: (ItemCompra) -> Unit,
    onAdicionarTrocasPendentes: () -> Unit,
    onRegistrarCompras: () -> Unit,
    onLimparComprados: () -> Unit,
    /** Abrir já em "Comprados" (usado nos prints da revisão). */
    compradosInicial: Boolean = false
) {
    val c = Oficina.cores
    val t = Oficina.tipos
    val contexto = LocalContext.current

    if (selecionado == null) {
        EmBreveScreen(
            numero = "03",
            rotulo = "Compras",
            titulo = "Primeiro, o *veículo*.",
            texto = "Cadastre um carro ou moto para montar a lista de peças."
        )
        return
    }

    var aba by rememberSaveable { mutableStateOf(if (compradosInicial) AbaCompras.COMPRADOS else AbaCompras.PENDENTES) }
    var confirmarLimpeza by remember { mutableStateOf(false) }

    val pendentes = compras.orEmpty().filter { !it.comprado }
    val comprados = compras.orEmpty().filter { it.comprado }
    val semRegistro = comprados.count { it.registroId == null }
    val totalPendente = pendentes.sumOf { it.subtotalCentavos() }
    val totalComprado = comprados.sumOf { it.subtotalCentavos() }

    fun compartilhar() {
        val envio = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, textoListaCompras(selecionado, pendentes))
        contexto.startActivity(Intent.createChooser(envio, "Enviar lista de compras"))
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f)) {
            item {
                Column(Modifier.padding(16.dp)) {
                    Eyebrow("03", "Compras · ${selecionado.apelido}")
                    Spacer(Modifier.height(16.dp))
                    TituloDisplay("Lista de *compras*.")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        when {
                            compras == null -> " "
                            pendentes.isEmpty() -> "Nada para comprar agora."
                            totalPendente > 0 ->
                                "${plural(pendentes.size, "peça", "peças")} a comprar · ${formatarDinheiro(totalPendente)} estimado."
                            else -> "${plural(pendentes.size, "peça", "peças")} a comprar."
                        },
                        style = t.bodyLg,
                        color = c.muted
                    )
                    Spacer(Modifier.height(16.dp))
                    Segmentado(
                        opcoes = AbaCompras.entries.toList(),
                        selecionada = aba,
                        rotulo = { if (it == AbaCompras.COMPRADOS && comprados.isNotEmpty()) "${it.rotulo} · ${comprados.size}" else it.rotulo },
                        onSelecionar = { aba = it }
                    )
                }
            }

            when (aba) {
                AbaCompras.PENDENTES -> {
                    items(pendentes, key = { it.id }) { compra ->
                        Linha()
                        LinhaCompra(compra, onAlternar = { onAlternar(compra) }, onClick = { onAbrir(compra.id) })
                    }
                    item {
                        if (pendentes.isNotEmpty()) Linha()
                        Column(Modifier.padding(horizontal = 4.dp)) {
                            if (trocasPendentes > 0) {
                                BotaoTexto(
                                    "+  Trocas pendentes · $trocasPendentes",
                                    onClick = onAdicionarTrocasPendentes
                                )
                            }
                            if (pendentes.isNotEmpty()) {
                                BotaoTexto("Enviar lista pelo WhatsApp  →", onClick = ::compartilhar)
                            }
                        }
                    }
                }

                AbaCompras.COMPRADOS -> {
                    if (comprados.isEmpty()) {
                        item {
                            Text(
                                "Marque as peças como compradas e elas aparecem aqui.",
                                style = t.body,
                                color = c.muted,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        item {
                            Text(
                                "${formatarDinheiro(totalComprado)} em peças compradas.",
                                style = t.bodySm,
                                color = c.muted,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                            )
                        }
                        items(comprados, key = { it.id }) { compra ->
                            Linha()
                            LinhaCompra(
                                compra,
                                onAlternar = { onAlternar(compra) },
                                onClick = { onAbrir(compra.id) },
                                mostrarRegistro = true
                            )
                        }
                        item {
                            Linha()
                            Column(Modifier.padding(16.dp)) {
                                if (semRegistro > 0) {
                                    BotaoSecundario(
                                        "Registrar como manutenção · $semRegistro",
                                        onClick = onRegistrarCompras,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Cria o registro com as peças e o valor já preenchidos.",
                                        style = t.bodySm,
                                        color = c.muted
                                    )
                                }
                                BotaoTexto(
                                    "Limpar comprados",
                                    onClick = { confirmarLimpeza = true },
                                    cor = c.muted
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }

        Linha()
        BotaoPrimario(
            "Adicionar peça",
            onClick = onNovaPeca,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            seta = true
        )
    }

    if (confirmarLimpeza) {
        DialogoOficina(
            eyebrow = "Limpar",
            titulo = "Tirar os comprados da lista?",
            onDismiss = { confirmarLimpeza = false },
            acoes = {
                BotaoTexto("Cancelar", onClick = { confirmarLimpeza = false }, cor = c.muted)
                BotaoSecundario("Limpar", onClick = {
                    confirmarLimpeza = false
                    onLimparComprados()
                }, perigo = true)
            }
        ) {
            Text(
                if (semRegistro > 0) "Há $semRegistro sem registro de manutenção. Os registros já feitos continuam no histórico."
                else "Os registros de manutenção continuam no histórico.",
                style = t.body,
                color = c.ink
            )
        }
    }
}

@Composable
private fun LinhaCompra(
    compra: ItemCompra,
    onAlternar: () -> Unit,
    onClick: () -> Unit,
    mostrarRegistro: Boolean = false
) {
    val c = Oficina.cores
    val t = Oficina.tipos
    val subtotal = compra.subtotalCentavos()

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(end = 16.dp, top = 6.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Área de toque de 48dp só para marcar como comprado.
        Box(
            Modifier
                .size(48.dp)
                .toggleable(value = compra.comprado, role = Role.Checkbox, onValueChange = { onAlternar() }),
            contentAlignment = Alignment.Center
        ) {
            CaixaMarcacao(compra.comprado)
        }
        Column(
            Modifier
                .weight(1f)
                .padding(top = 6.dp)
        ) {
            Text(
                compra.nome,
                style = t.body,
                color = if (compra.comprado) c.muted else c.ink,
                textDecoration = if (compra.comprado) TextDecoration.LineThrough else null
            )
            // Referência em linha própria e inteira: é o que identifica a peça na loja.
            if (compra.referencia.isNotBlank()) {
                Hud("Ref. ${compra.referencia}", cor = if (compra.comprado) c.faint else c.accentInk, maxLinhas = 2)
            }
            if (compra.modelo.isNotBlank()) {
                Text(compra.modelo, style = t.bodySm, color = c.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(
                descreverQuantidade(compra) +
                    if (compra.precoUnitarioCentavos > 0) " × ${formatarDinheiro(compra.precoUnitarioCentavos)}" else "",
                style = t.bodySm,
                color = c.muted
            )
            if (compra.loja.isNotBlank()) {
                Text(compra.loja, style = t.bodySm, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (mostrarRegistro) {
                Hud(
                    if (compra.registroId != null) "Registrado" else "Sem registro",
                    cor = if (compra.registroId != null) c.verdeInk else c.ambarInk
                )
            }
        }
        if (subtotal > 0) {
            Spacer(Modifier.width(12.dp))
            Text(
                formatarDinheiro(subtotal),
                style = t.body,
                color = if (compra.comprado) c.muted else c.inkStrong,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
