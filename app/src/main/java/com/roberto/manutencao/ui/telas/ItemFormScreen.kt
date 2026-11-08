package com.roberto.manutencao.ui.telas

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.roberto.manutencao.data.Datas
import com.roberto.manutencao.data.ItemManutencao
import com.roberto.manutencao.data.SituacaoItem
import com.roberto.manutencao.ui.ManutencaoViewModel
import com.roberto.manutencao.ui.componentes.BotaoPrimario
import com.roberto.manutencao.ui.componentes.BotaoSecundario
import com.roberto.manutencao.ui.componentes.BotaoTexto
import com.roberto.manutencao.ui.componentes.CabecalhoInterno
import com.roberto.manutencao.ui.componentes.CampoData
import com.roberto.manutencao.ui.componentes.CampoTexto
import com.roberto.manutencao.ui.componentes.DialogoOficina
import com.roberto.manutencao.ui.componentes.Hud
import com.roberto.manutencao.ui.componentes.LinhaMarcavel
import com.roberto.manutencao.ui.componentes.PontoStatus
import com.roberto.manutencao.ui.componentes.marcasDeCanto
import com.roberto.manutencao.ui.coresStatus
import com.roberto.manutencao.ui.descreverProxima
import com.roberto.manutencao.ui.descreverSituacao
import com.roberto.manutencao.ui.formatarKm
import com.roberto.manutencao.ui.rotuloStatus
import com.roberto.manutencao.ui.theme.Oficina

@Composable
fun ItemFormScreen(
    veiculoId: Long,
    itemId: Long,
    viewModel: ManutencaoViewModel,
    situacoes: List<SituacaoItem>?,
    onVoltar: () -> Unit,
    onRegistrarTroca: (Long) -> Unit,
    onAdicionarCompra: (Long) -> Unit
) {
    val c = Oficina.cores
    val t = Oficina.tipos
    val editando = itemId != 0L

    var original by remember { mutableStateOf<ItemManutencao?>(null) }
    var preenchido by rememberSaveable { mutableStateOf(false) }
    var nome by rememberSaveable { mutableStateOf("") }
    var intervaloKm by rememberSaveable { mutableStateOf("") }
    var intervaloMeses by rememberSaveable { mutableStateOf("") }
    var baseKm by rememberSaveable { mutableStateOf("") }
    var baseData by rememberSaveable { mutableStateOf<Long?>(null) }
    var ativo by rememberSaveable { mutableStateOf(true) }
    var modelo by rememberSaveable { mutableStateOf("") }
    var referencia by rememberSaveable { mutableStateOf("") }
    var tentouSalvar by rememberSaveable { mutableStateOf(false) }
    var confirmarExclusao by remember { mutableStateOf(false) }

    LaunchedEffect(itemId) {
        if (editando) {
            val item = viewModel.buscarItem(itemId)
            original = item
            if (item != null && !preenchido) {
                nome = item.nome
                intervaloKm = item.intervaloKm?.toString().orEmpty()
                intervaloMeses = item.intervaloMeses?.toString().orEmpty()
                baseKm = item.baseKm?.toString().orEmpty()
                baseData = item.baseData
                ativo = item.ativo
                modelo = item.modelo
                referencia = item.referencia
                preenchido = true
            }
        }
    }

    val situacao = situacoes?.firstOrNull { it.item.id == itemId }
    val erroNome = if (tentouSalvar && nome.isBlank()) "Dê um nome ao item." else null
    val semIntervalo = intervaloKm.toIntOrNull() == null && intervaloMeses.toIntOrNull() == null
    val erroIntervalo = if (tentouSalvar && semIntervalo) "Informe km, meses ou os dois." else null

    fun salvar() {
        tentouSalvar = true
        if (nome.isBlank() || semIntervalo) return
        val base = original
        if (editando && base == null) return
        val item = (base ?: ItemManutencao(veiculoId = veiculoId, nome = nome)).copy(
            nome = nome.trim(),
            intervaloKm = intervaloKm.toIntOrNull()?.takeIf { it > 0 },
            intervaloMeses = intervaloMeses.toIntOrNull()?.takeIf { it > 0 },
            baseKm = baseKm.toIntOrNull(),
            baseData = baseData,
            ativo = ativo,
            modelo = modelo.trim(),
            referencia = referencia.trim()
        )
        viewModel.salvarItem(item, onVoltar)
    }

    Column(Modifier.fillMaxSize()) {
        CabecalhoInterno(
            eyebrow = "Item",
            titulo = if (editando) "Ajustar *item*." else "Novo *item*.",
            onVoltar = onVoltar
        )
        Column(
            Modifier
                .weight(1f)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Situação atual, emoldurada.
            if (editando && situacao != null) {
                val (corPonto, corTexto) = coresStatus(situacao.status)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .border(1.dp, c.line)
                        .marcasDeCanto(c.lineStrong)
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PontoStatus(corPonto)
                        Spacer(Modifier.width(8.dp))
                        Hud(rotuloStatus(situacao.status), cor = corTexto)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(descreverSituacao(situacao), style = t.body, color = c.ink)
                    descreverProxima(situacao)?.let { Text(it, style = t.bodySm, color = c.muted) }
                    val ultima = listOfNotNull(
                        situacao.ultimoKm?.formatarKm(),
                        situacao.ultimaData?.let { Datas.formatar(it) }
                    )
                    if (ultima.isNotEmpty()) {
                        Text("Última troca: ${ultima.joinToString(" · ")}.", style = t.bodySm, color = c.muted)
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            CampoTexto(
                valor = nome,
                onValor = { nome = it.take(40) },
                rotulo = "Nome do item",
                placeholder = "Óleo do motor",
                erro = erroNome,
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            Hud("Peça")
            CampoTexto(
                valor = modelo,
                onValor = { modelo = it.take(60) },
                rotulo = "Modelo ou especificação",
                placeholder = "5W30 sintético, Fram PH6017",
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )
            CampoTexto(
                valor = referencia,
                onValor = { referencia = it.take(40) },
                rotulo = "Referência",
                placeholder = "Código da peça no catálogo",
                ajuda = "Aparece na lista de compras para facilitar o pedido.",
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
            )

            Spacer(Modifier.height(4.dp))
            Hud("Intervalo · o que vier primeiro")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoTexto(
                    valor = intervaloKm,
                    onValor = { novo -> intervaloKm = novo.filter(Char::isDigit).take(6) },
                    rotulo = "A cada (km)",
                    erro = erroIntervalo,
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                CampoTexto(
                    valor = intervaloMeses,
                    onValor = { novo -> intervaloMeses = novo.filter(Char::isDigit).take(3) },
                    rotulo = "A cada (meses)",
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(4.dp))
            Hud("Última troca antes do app")
            Text(
                "Preencha se a troca foi feita antes de você começar a registrar aqui.",
                style = t.bodySm,
                color = c.muted
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoTexto(
                    valor = baseKm,
                    onValor = { novo -> baseKm = novo.filter(Char::isDigit).take(7) },
                    rotulo = "Km",
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                CampoData(
                    valor = baseData,
                    onValor = { baseData = it },
                    rotulo = "Data",
                    permiteLimpar = true,
                    modifier = Modifier.weight(1f)
                )
            }

            LinhaMarcavel(
                texto = "Alertar este item",
                marcado = ativo,
                onAlternar = { ativo = !ativo },
                subtitulo = "Desmarque para pausar os avisos."
            )

            Spacer(Modifier.height(4.dp))
            BotaoPrimario("Salvar item", onClick = ::salvar, modifier = Modifier.fillMaxWidth())
            if (editando) {
                BotaoSecundario(
                    "Adicionar à lista de compras",
                    onClick = { onAdicionarCompra(itemId) },
                    modifier = Modifier.fillMaxWidth()
                )
                BotaoSecundario(
                    "Registrar troca deste item",
                    onClick = { onRegistrarTroca(itemId) },
                    modifier = Modifier.fillMaxWidth()
                )
                BotaoSecundario(
                    "Excluir item",
                    onClick = { confirmarExclusao = true },
                    modifier = Modifier.fillMaxWidth(),
                    perigo = true
                )
            }
        }
    }

    if (confirmarExclusao) {
        val item = original
        DialogoOficina(
            eyebrow = "Excluir",
            titulo = "Excluir ${nome.ifBlank { "item" }}?",
            onDismiss = { confirmarExclusao = false },
            acoes = {
                BotaoTexto("Cancelar", onClick = { confirmarExclusao = false }, cor = c.muted)
                BotaoSecundario("Excluir", onClick = {
                    confirmarExclusao = false
                    if (item != null) viewModel.excluirItem(item, onVoltar)
                }, perigo = true)
            }
        ) {
            Text(
                "O item sai da lista de trocas. Os registros antigos continuam no histórico.",
                style = t.body,
                color = c.ink
            )
        }
    }
}
