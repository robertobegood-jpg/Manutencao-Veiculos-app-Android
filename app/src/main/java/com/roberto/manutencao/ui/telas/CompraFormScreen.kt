package com.roberto.manutencao.ui.telas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.roberto.manutencao.data.ItemCompra
import com.roberto.manutencao.data.ItemManutencao
import com.roberto.manutencao.ui.ComprasViewModel
import com.roberto.manutencao.ui.ManutencaoViewModel
import com.roberto.manutencao.ui.componentes.BotaoPrimario
import com.roberto.manutencao.ui.componentes.BotaoSecundario
import com.roberto.manutencao.ui.componentes.BotaoTexto
import com.roberto.manutencao.ui.componentes.CabecalhoInterno
import com.roberto.manutencao.ui.componentes.CampoDinheiro
import com.roberto.manutencao.ui.componentes.CampoTexto
import com.roberto.manutencao.ui.componentes.DialogoOficina
import com.roberto.manutencao.ui.componentes.Hud
import com.roberto.manutencao.ui.componentes.Linha
import com.roberto.manutencao.ui.componentes.LinhaMarcavel
import com.roberto.manutencao.ui.componentes.Segmentado
import com.roberto.manutencao.ui.formatarDinheiro
import com.roberto.manutencao.ui.formatarQuantidade
import com.roberto.manutencao.ui.theme.Oficina

private val UNIDADES = listOf("un", "L", "kit", "jogo")

@Composable
fun CompraFormScreen(
    veiculoId: Long,
    compraId: Long,
    itemInicialId: Long,
    comprasViewModel: ComprasViewModel,
    manutencaoViewModel: ManutencaoViewModel,
    itensDoVeiculo: List<ItemManutencao>,
    onVoltar: () -> Unit
) {
    val c = Oficina.cores
    val t = Oficina.tipos
    val editando = compraId != 0L

    var original by remember { mutableStateOf<ItemCompra?>(null) }
    var preenchido by rememberSaveable { mutableStateOf(false) }
    var nome by rememberSaveable { mutableStateOf("") }
    var modelo by rememberSaveable { mutableStateOf("") }
    var referencia by rememberSaveable { mutableStateOf("") }
    var quantidade by rememberSaveable { mutableStateOf("1") }
    var unidade by rememberSaveable { mutableStateOf("un") }
    var preco by rememberSaveable { mutableStateOf(0L) }
    var loja by rememberSaveable { mutableStateOf("") }
    var observacao by rememberSaveable { mutableStateOf("") }
    var comprado by rememberSaveable { mutableStateOf(false) }
    var itemVinculado by rememberSaveable { mutableStateOf<Long?>(null) }
    var guardarNoItem by rememberSaveable { mutableStateOf(true) }
    var tentouSalvar by rememberSaveable { mutableStateOf(false) }
    var menuItens by remember { mutableStateOf(false) }
    var confirmarExclusao by remember { mutableStateOf(false) }

    LaunchedEffect(compraId, itemInicialId) {
        if (preenchido) return@LaunchedEffect
        if (editando) {
            val compra = comprasViewModel.buscar(compraId)
            original = compra
            if (compra != null) {
                nome = compra.nome
                modelo = compra.modelo
                referencia = compra.referencia
                quantidade = formatarQuantidade(compra.quantidade)
                unidade = compra.unidade
                preco = compra.precoUnitarioCentavos
                loja = compra.loja
                observacao = compra.observacao
                comprado = compra.comprado
                itemVinculado = compra.itemManutencaoId
                preenchido = true
            }
        } else {
            if (itemInicialId != 0L) {
                manutencaoViewModel.buscarItem(itemInicialId)?.let { item ->
                    nome = item.nome
                    modelo = item.modelo
                    referencia = item.referencia
                    itemVinculado = item.id
                }
            }
            preenchido = true
        }
    }
    // Ao editar, recupera o original mesmo depois de girar a tela.
    LaunchedEffect(compraId) {
        if (editando && original == null) original = comprasViewModel.buscar(compraId)
    }

    val itemSelecionado = itensDoVeiculo.firstOrNull { it.id == itemVinculado }
    val qtdNumero = quantidade.replace(',', '.').toDoubleOrNull()
    val subtotal = if (qtdNumero != null) Math.round(qtdNumero * preco) else 0L
    val erroNome = if (tentouSalvar && nome.isBlank()) "Dê um nome à peça." else null
    val erroQtd = if (tentouSalvar && (qtdNumero == null || qtdNumero <= 0.0)) "Quantidade inválida." else null
    val pecaMudou = itemSelecionado != null &&
        (modelo.trim() != itemSelecionado.modelo || referencia.trim() != itemSelecionado.referencia) &&
        (modelo.isNotBlank() || referencia.isNotBlank())

    fun escolherItem(item: ItemManutencao?) {
        itemVinculado = item?.id
        if (item != null) {
            if (nome.isBlank()) nome = item.nome
            if (modelo.isBlank()) modelo = item.modelo
            if (referencia.isBlank()) referencia = item.referencia
        }
        menuItens = false
    }

    fun salvar() {
        tentouSalvar = true
        val q = qtdNumero
        if (nome.isBlank() || q == null || q <= 0.0) return
        val base = original
        if (editando && base == null) return
        val agora = System.currentTimeMillis()
        val compra = (base ?: ItemCompra(veiculoId = veiculoId, nome = nome)).copy(
            nome = nome.trim(),
            modelo = modelo.trim(),
            referencia = referencia.trim(),
            quantidade = q,
            unidade = unidade,
            precoUnitarioCentavos = preco,
            loja = loja.trim(),
            observacao = observacao.trim(),
            itemManutencaoId = itemVinculado,
            comprado = comprado,
            compradoEm = when {
                !comprado -> null
                base?.comprado == true -> base?.compradoEm
                else -> agora
            }
        )
        val vinculado = itemSelecionado
        if (vinculado != null && pecaMudou && guardarNoItem) {
            manutencaoViewModel.atualizarPecaDoItem(vinculado.id, modelo.trim(), referencia.trim())
        }
        comprasViewModel.salvar(compra, onVoltar)
    }

    Column(Modifier.fillMaxSize()) {
        CabecalhoInterno(
            eyebrow = "Compra",
            titulo = if (editando) "Editar *peça*." else "Nova *peça*.",
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
            // Vínculo com um item de manutenção.
            Hud("Para qual item")
            Box {
                CampoTexto(
                    valor = itemSelecionado?.nome ?: "Peça avulsa",
                    onValor = {},
                    rotulo = "Item de manutenção",
                    somenteLeitura = true,
                    trailing = { Text("▾", style = t.body, color = c.muted) }
                )
                Box(
                    Modifier
                        .matchParentSize()
                        .clickable(role = Role.DropdownList) { menuItens = true }
                )
                DropdownMenu(
                    expanded = menuItens,
                    onDismissRequest = { menuItens = false },
                    shape = RectangleShape,
                    containerColor = c.bgRaised,
                    tonalElevation = 0.dp,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, c.line)
                ) {
                    DropdownMenuItem(
                        text = { Text("Peça avulsa", style = t.body, color = c.ink) },
                        onClick = { escolherItem(null) }
                    )
                    if (itensDoVeiculo.isNotEmpty()) Linha()
                    itensDoVeiculo.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item.nome, style = t.body, color = c.ink) },
                            trailingIcon = {
                                if (item.id == itemVinculado) Text("✓", style = t.body, color = c.ink)
                            },
                            onClick = { escolherItem(item) }
                        )
                    }
                }
            }

            CampoTexto(
                valor = nome,
                onValor = { nome = it.take(60) },
                rotulo = "Peça",
                placeholder = "Óleo do motor",
                erro = erroNome,
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )
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
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
            )
            if (pecaMudou) {
                LinhaMarcavel(
                    texto = "Guardar no item ${itemSelecionado?.nome.orEmpty()}",
                    marcado = guardarNoItem,
                    onAlternar = { guardarNoItem = !guardarNoItem },
                    subtitulo = "Na próxima compra, modelo e referência já vêm preenchidos."
                )
            }

            Spacer(Modifier.height(4.dp))
            Hud("Quantidade")
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                CampoTexto(
                    valor = quantidade,
                    onValor = { novo ->
                        val limpo = novo.replace('.', ',').filter { it.isDigit() || it == ',' }
                        val virgula = limpo.indexOf(',')
                        quantidade = (if (virgula >= 0) {
                            limpo.substring(0, virgula + 1) + limpo.substring(virgula + 1).replace(",", "")
                        } else limpo).take(7)
                    },
                    rotulo = "Qtd.",
                    erro = erroQtd,
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                Segmentado(
                    opcoes = UNIDADES,
                    selecionada = unidade,
                    rotulo = { it },
                    onSelecionar = { unidade = it },
                    modifier = Modifier
                        .weight(2f)
                        .padding(top = 8.dp)
                )
            }

            Hud("Preço")
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CampoDinheiro(preco, { preco = it }, "Preço por $unidade", Modifier.weight(1f))
                Column(Modifier.weight(1f)) {
                    Hud("Subtotal")
                    Text(formatarDinheiro(subtotal), style = t.heading, color = c.inkStrong)
                }
            }
            CampoTexto(
                valor = loja,
                onValor = { loja = it.take(60) },
                rotulo = "Loja",
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )
            CampoTexto(
                valor = observacao,
                onValor = { observacao = it.take(200) },
                rotulo = "Observação",
                linhaUnica = false,
                minLinhas = 2,
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )
            LinhaMarcavel(
                texto = "Já comprei",
                marcado = comprado,
                onAlternar = { comprado = !comprado }
            )

            Spacer(Modifier.height(4.dp))
            BotaoPrimario("Salvar peça", onClick = ::salvar, modifier = Modifier.fillMaxWidth())
            if (editando) {
                BotaoSecundario(
                    "Remover da lista",
                    onClick = { confirmarExclusao = true },
                    modifier = Modifier.fillMaxWidth(),
                    perigo = true
                )
            }
        }
    }

    if (confirmarExclusao) {
        DialogoOficina(
            eyebrow = "Remover",
            titulo = "Remover ${nome.ifBlank { "a peça" }}?",
            onDismiss = { confirmarExclusao = false },
            acoes = {
                BotaoTexto("Cancelar", onClick = { confirmarExclusao = false }, cor = c.muted)
                BotaoSecundario("Remover", onClick = {
                    confirmarExclusao = false
                    original?.let { comprasViewModel.excluir(it, onVoltar) }
                }, perigo = true)
            }
        ) {
            Text("A peça sai da lista de compras.", style = t.body, color = c.ink)
        }
    }
}
