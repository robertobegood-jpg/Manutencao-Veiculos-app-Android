package com.roberto.manutencao.ui.telas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.roberto.manutencao.data.CategoriaDespesa
import com.roberto.manutencao.data.Datas
import com.roberto.manutencao.data.Despesa
import com.roberto.manutencao.ui.GastosViewModel
import com.roberto.manutencao.ui.componentes.BotaoPrimario
import com.roberto.manutencao.ui.componentes.BotaoSecundario
import com.roberto.manutencao.ui.componentes.BotaoTexto
import com.roberto.manutencao.ui.componentes.CabecalhoInterno
import com.roberto.manutencao.ui.componentes.CampoData
import com.roberto.manutencao.ui.componentes.CampoDinheiro
import com.roberto.manutencao.ui.componentes.CampoTexto
import com.roberto.manutencao.ui.componentes.DialogoOficina
import com.roberto.manutencao.ui.componentes.Hud
import com.roberto.manutencao.ui.componentes.Segmentado
import com.roberto.manutencao.ui.theme.Oficina

/** Categorias em pares, para caber em células de duas colunas. */
private val LINHAS_CATEGORIA = CategoriaDespesa.entries.chunked(2)

@Composable
fun DespesaFormScreen(
    veiculoId: Long,
    despesaId: Long,
    gastosViewModel: GastosViewModel,
    onVoltar: () -> Unit
) {
    val c = Oficina.cores
    val t = Oficina.tipos
    val editando = despesaId != 0L

    var original by remember { mutableStateOf<Despesa?>(null) }
    var preenchido by rememberSaveable { mutableStateOf(false) }
    var data by rememberSaveable { mutableStateOf(Datas.hoje()) }
    var categoria by rememberSaveable { mutableStateOf(CategoriaDespesa.IPVA) }
    var descricao by rememberSaveable { mutableStateOf("") }
    var valor by rememberSaveable { mutableStateOf(0L) }
    var observacao by rememberSaveable { mutableStateOf("") }
    var tentouSalvar by rememberSaveable { mutableStateOf(false) }
    var confirmarExclusao by remember { mutableStateOf(false) }

    LaunchedEffect(despesaId) {
        if (editando) {
            val d = gastosViewModel.buscarDespesa(despesaId)
            original = d
            if (d != null && !preenchido) {
                data = d.data
                categoria = d.categoria
                descricao = d.descricao
                valor = d.valorCentavos
                observacao = d.observacao
                preenchido = true
            }
        }
    }

    val erroValor = if (tentouSalvar && valor <= 0L) "Informe o valor." else null

    fun salvar() {
        tentouSalvar = true
        if (valor <= 0L) return
        val base = original
        if (editando && base == null) return
        val despesa = (base ?: Despesa(veiculoId = veiculoId, data = data, categoria = categoria, valorCentavos = valor)).copy(
            data = data,
            categoria = categoria,
            descricao = descricao.trim(),
            valorCentavos = valor,
            observacao = observacao.trim()
        )
        gastosViewModel.salvarDespesa(despesa, onVoltar)
    }

    Column(Modifier.fillMaxSize()) {
        CabecalhoInterno(
            eyebrow = "Despesa",
            titulo = if (editando) "Editar *despesa*." else "Nova *despesa*.",
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
            Hud("Categoria")
            Column {
                LINHAS_CATEGORIA.forEachIndexed { i, linha ->
                    Segmentado(
                        opcoes = linha,
                        selecionada = categoria,
                        rotulo = { it.rotulo },
                        onSelecionar = { categoria = it },
                        // Sobrepõe 1px para as bordas das linhas virarem uma só.
                        modifier = Modifier.offset(y = (-i).dp)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            CampoData(
                valor = data,
                onValor = { novo -> if (novo != null) data = novo },
                rotulo = "Data",
                modifier = Modifier.fillMaxWidth()
            )
            CampoTexto(
                valor = descricao,
                onValor = { descricao = it.take(60) },
                rotulo = "Descrição",
                placeholder = "IPVA 2027, parcela 1 de 3",
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )
            CampoDinheiro(
                centavos = valor,
                onCentavos = { valor = it },
                rotulo = "Valor",
                erro = erroValor,
                modifier = Modifier.fillMaxWidth()
            )
            CampoTexto(
                valor = observacao,
                onValor = { observacao = it.take(200) },
                rotulo = "Observação",
                linhaUnica = false,
                minLinhas = 2,
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            Spacer(Modifier.height(4.dp))
            BotaoPrimario("Salvar despesa", onClick = ::salvar, modifier = Modifier.fillMaxWidth())
            if (editando) {
                BotaoSecundario(
                    "Excluir despesa",
                    onClick = { confirmarExclusao = true },
                    modifier = Modifier.fillMaxWidth(),
                    perigo = true
                )
            }
        }
    }

    if (confirmarExclusao) {
        DialogoOficina(
            eyebrow = "Excluir",
            titulo = "Excluir esta despesa?",
            onDismiss = { confirmarExclusao = false },
            acoes = {
                BotaoTexto("Cancelar", onClick = { confirmarExclusao = false }, cor = c.muted)
                BotaoSecundario("Excluir", onClick = {
                    confirmarExclusao = false
                    original?.let { gastosViewModel.excluirDespesa(it, onVoltar) }
                }, perigo = true)
            }
        ) {
            Text("Ela sai do histórico de gastos deste veículo.", style = t.body, color = c.ink)
        }
    }
}
