package com.roberto.manutencao.ui.telas

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.roberto.manutencao.data.TipoVeiculo
import com.roberto.manutencao.data.Veiculo
import com.roberto.manutencao.ui.VeiculosViewModel
import com.roberto.manutencao.ui.componentes.BotaoPrimario
import com.roberto.manutencao.ui.componentes.CabecalhoInterno
import com.roberto.manutencao.ui.componentes.CampoTexto
import com.roberto.manutencao.ui.componentes.Hud
import com.roberto.manutencao.ui.componentes.Segmentado
import java.util.Calendar

@Composable
fun VeiculoFormScreen(
    veiculoId: Long,
    viewModel: VeiculosViewModel,
    onVoltar: () -> Unit
) {
    val editando = veiculoId != 0L
    var original by remember { mutableStateOf<Veiculo?>(null) }
    var preenchido by rememberSaveable { mutableStateOf(false) }

    var tipo by rememberSaveable { mutableStateOf(TipoVeiculo.CARRO) }
    var apelido by rememberSaveable { mutableStateOf("") }
    var marca by rememberSaveable { mutableStateOf("") }
    var modelo by rememberSaveable { mutableStateOf("") }
    var ano by rememberSaveable { mutableStateOf("") }
    var placa by rememberSaveable { mutableStateOf("") }
    var km by rememberSaveable { mutableStateOf("") }
    var tentouSalvar by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(veiculoId) {
        if (editando) {
            val v = viewModel.buscar(veiculoId)
            original = v
            if (v != null && !preenchido) {
                tipo = v.tipo
                apelido = v.apelido
                marca = v.marca
                modelo = v.modelo
                ano = v.ano?.toString().orEmpty()
                placa = v.placa
                km = v.kmAtual.toString()
                preenchido = true
            }
        }
    }

    val anoMaximo = Calendar.getInstance().get(Calendar.YEAR) + 1
    val anoNumero = ano.toIntOrNull()
    val erroApelido = if (tentouSalvar && apelido.isBlank()) "Dê um nome, como \"Meu Gol\"." else null
    val erroAno = if (ano.isNotEmpty() && (anoNumero == null || anoNumero !in 1950..anoMaximo)) "Ano inválido." else null
    val erroKm = if (!editando && tentouSalvar && km.isBlank()) "Informe o km do painel." else null

    fun salvar() {
        tentouSalvar = true
        if (apelido.isBlank() || erroAno != null || (!editando && km.isBlank())) return
        val base = original
        if (editando && base == null) return
        val veiculo = (base ?: Veiculo(tipo = tipo, apelido = apelido)).copy(
            tipo = tipo,
            apelido = apelido,
            marca = marca,
            modelo = modelo,
            ano = anoNumero,
            placa = placa,
            // Ao editar, o campo serve para corrigir um km digitado errado.
            kmAtual = km.toIntOrNull() ?: base?.kmAtual ?: 0
        )
        viewModel.salvar(veiculo, onVoltar)
    }

    Column(Modifier.fillMaxSize()) {
        CabecalhoInterno(
            eyebrow = "Veículo",
            titulo = if (editando) "Editar *veículo*." else "Novo *veículo*.",
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
            Hud("Tipo")
            Segmentado(
                opcoes = TipoVeiculo.entries.toList(),
                selecionada = tipo,
                rotulo = { it.rotulo },
                onSelecionar = { tipo = it }
            )
            Spacer(Modifier.height(4.dp))
            CampoTexto(
                valor = apelido,
                onValor = { apelido = it.take(40) },
                rotulo = "Apelido",
                placeholder = "Meu Gol, Moto do trabalho",
                erro = erroApelido,
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )
            CampoTexto(
                valor = marca,
                onValor = { marca = it.take(30) },
                rotulo = "Marca",
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )
            CampoTexto(
                valor = modelo,
                onValor = { modelo = it.take(40) },
                rotulo = "Modelo",
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoTexto(
                    valor = ano,
                    onValor = { novo -> ano = novo.filter(Char::isDigit).take(4) },
                    rotulo = "Ano",
                    erro = erroAno,
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                CampoTexto(
                    valor = placa,
                    onValor = { novo -> placa = novo.filter(Char::isLetterOrDigit).uppercase().take(7) },
                    rotulo = "Placa",
                    teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.weight(1f)
                )
            }
            CampoTexto(
                valor = km,
                onValor = { novo -> km = novo.filter(Char::isDigit).take(7) },
                rotulo = "Km atual",
                erro = erroKm,
                ajuda = if (editando) "Só para corrigir um erro de digitação. No dia a dia, use \"Atualizar km\" no painel."
                else "O número que aparece no painel.",
                teclado = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(Modifier.height(8.dp))
            BotaoPrimario("Salvar veículo", onClick = ::salvar, modifier = Modifier.fillMaxWidth())
        }
    }
}
