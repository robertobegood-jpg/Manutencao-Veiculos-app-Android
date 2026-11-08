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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.roberto.manutencao.data.Abastecimento
import com.roberto.manutencao.data.Combustivel
import com.roberto.manutencao.data.Datas
import com.roberto.manutencao.ui.GastosViewModel
import com.roberto.manutencao.ui.VeiculosViewModel
import com.roberto.manutencao.ui.componentes.BotaoPrimario
import com.roberto.manutencao.ui.componentes.BotaoSecundario
import com.roberto.manutencao.ui.componentes.BotaoTexto
import com.roberto.manutencao.ui.componentes.CabecalhoInterno
import com.roberto.manutencao.ui.componentes.CampoData
import com.roberto.manutencao.ui.componentes.CampoDinheiro
import com.roberto.manutencao.ui.componentes.CampoTexto
import com.roberto.manutencao.ui.componentes.DialogoOficina
import com.roberto.manutencao.ui.componentes.Hud
import com.roberto.manutencao.ui.componentes.LinhaMarcavel
import com.roberto.manutencao.ui.componentes.Segmentado
import com.roberto.manutencao.ui.formatarKm
import com.roberto.manutencao.ui.formatarQuantidade
import com.roberto.manutencao.ui.theme.Oficina
import kotlin.math.roundToLong

/** Mantém só dígitos e uma vírgula: "40.5" → "40,5". */
private fun limparDecimal(texto: String, max: Int = 7): String {
    val limpo = texto.replace('.', ',').filter { it.isDigit() || it == ',' }
    val virgula = limpo.indexOf(',')
    val final = if (virgula >= 0) {
        limpo.substring(0, virgula + 1) + limpo.substring(virgula + 1).replace(",", "")
    } else limpo
    return final.take(max)
}

private fun paraDouble(texto: String): Double? = texto.replace(',', '.').toDoubleOrNull()

@Composable
fun AbastecimentoFormScreen(
    veiculoId: Long,
    abastecimentoId: Long,
    gastosViewModel: GastosViewModel,
    veiculosViewModel: VeiculosViewModel,
    onVoltar: () -> Unit
) {
    val c = Oficina.cores
    val t = Oficina.tipos
    val editando = abastecimentoId != 0L

    var original by remember { mutableStateOf<Abastecimento?>(null) }
    var kmAnterior by remember { mutableStateOf<Int?>(null) }
    var preenchido by rememberSaveable { mutableStateOf(false) }
    var data by rememberSaveable { mutableStateOf(Datas.hoje()) }
    var km by rememberSaveable { mutableStateOf("") }
    var litros by rememberSaveable { mutableStateOf("") }
    var precoLitro by rememberSaveable { mutableStateOf(0L) }
    var total by rememberSaveable { mutableStateOf(0L) }
    var tanqueCheio by rememberSaveable { mutableStateOf(true) }
    var combustivel by rememberSaveable { mutableStateOf(Combustivel.GASOLINA) }
    var posto by rememberSaveable { mutableStateOf("") }
    var observacao by rememberSaveable { mutableStateOf("") }
    var tentouSalvar by rememberSaveable { mutableStateOf(false) }
    var confirmarExclusao by remember { mutableStateOf(false) }

    LaunchedEffect(abastecimentoId) {
        val ultimo = gastosViewModel.ultimoAbastecimento(veiculoId)
        if (editando) {
            val a = gastosViewModel.buscarAbastecimento(abastecimentoId)
            original = a
            if (a != null && !preenchido) {
                data = a.data
                km = a.km.toString()
                litros = formatarQuantidade(a.litros)
                precoLitro = a.precoLitroMilesimos
                total = a.totalCentavos
                tanqueCheio = a.tanqueCheio
                combustivel = a.combustivel
                posto = a.posto
                observacao = a.observacao
                preenchido = true
            }
        } else {
            kmAnterior = ultimo?.km
            if (!preenchido) {
                // Repete o combustível, o posto e o preço do último abastecimento.
                if (ultimo != null) {
                    combustivel = ultimo.combustivel
                    posto = ultimo.posto
                    precoLitro = ultimo.precoLitroMilesimos
                }
                km = veiculosViewModel.buscar(veiculoId)?.kmAtual?.toString().orEmpty()
                preenchido = true
            }
        }
    }

    // Os três valores se completam: litros × preço = total.
    fun recalcularTotal(novosLitros: String, novoPreco: Long) {
        val l = paraDouble(novosLitros)
        if (l != null && novoPreco > 0) total = (l * novoPreco / 10.0).roundToLong()
    }

    val kmNumero = km.toIntOrNull()
    val litrosNumero = paraDouble(litros)
    val erroKm = if (tentouSalvar && kmNumero == null) "Informe o km." else null
    val avisoKm = kmAnterior?.let { anterior ->
        when {
            kmNumero != null && kmNumero <= anterior -> "Menor que o último abastecimento (${anterior.formatarKm()})."
            else -> "Último abastecimento: ${anterior.formatarKm()}."
        }
    }
    val erroLitros = if (tentouSalvar && (litrosNumero == null || litrosNumero <= 0.0)) "Informe os litros." else null
    val erroTotal = if (tentouSalvar && total <= 0L) "Informe o valor pago." else null

    fun salvar() {
        tentouSalvar = true
        val k = kmNumero ?: return
        val l = litrosNumero ?: return
        if (l <= 0.0 || total <= 0L) return
        val base = original
        if (editando && base == null) return
        val preco = if (precoLitro > 0) precoLitro else (total * 10.0 / l).roundToLong()
        val abastecimento = (base ?: Abastecimento(
            veiculoId = veiculoId, data = data, km = k, litros = l,
            precoLitroMilesimos = preco, totalCentavos = total
        )).copy(
            data = data,
            km = k,
            litros = l,
            precoLitroMilesimos = preco,
            totalCentavos = total,
            tanqueCheio = tanqueCheio,
            combustivel = combustivel,
            posto = posto.trim(),
            observacao = observacao.trim()
        )
        gastosViewModel.salvarAbastecimento(abastecimento, onVoltar)
    }

    Column(Modifier.fillMaxSize()) {
        CabecalhoInterno(
            eyebrow = "Posto",
            titulo = if (editando) "Editar *abastecimento*." else "Novo *abastecimento*.",
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
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoData(
                    valor = data,
                    onValor = { novo -> if (novo != null) data = novo },
                    rotulo = "Data",
                    modifier = Modifier.weight(1f)
                )
                CampoTexto(
                    valor = km,
                    onValor = { novo -> km = novo.filter(Char::isDigit).take(7) },
                    rotulo = "Km no painel",
                    erro = erroKm,
                    ajuda = avisoKm,
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            Hud("Combustível")
            Segmentado(
                opcoes = Combustivel.entries.toList(),
                selecionada = combustivel,
                rotulo = { it.rotulo },
                onSelecionar = { combustivel = it }
            )

            Spacer(Modifier.height(4.dp))
            Hud("Bomba")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoTexto(
                    valor = litros,
                    onValor = { novo ->
                        litros = limparDecimal(novo)
                        recalcularTotal(litros, precoLitro)
                    },
                    rotulo = "Litros",
                    erro = erroLitros,
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                CampoDinheiro(
                    centavos = precoLitro,
                    onCentavos = { novo ->
                        precoLitro = novo
                        recalcularTotal(litros, novo)
                    },
                    rotulo = "Preço/L",
                    casas = 3,
                    modifier = Modifier.weight(1f)
                )
            }
            CampoDinheiro(
                centavos = total,
                onCentavos = { novo ->
                    total = novo
                    // Ajusta o preço por litro quando você digita o total direto.
                    val l = paraDouble(litros)
                    if (l != null && l > 0.0 && novo > 0) precoLitro = (novo * 10.0 / l).roundToLong()
                },
                rotulo = "Total pago",
                erro = erroTotal,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Preencha dois dos três valores. O terceiro é calculado.",
                style = t.bodySm,
                color = c.muted
            )

            LinhaMarcavel(
                texto = "Completei o tanque",
                marcado = tanqueCheio,
                onAlternar = { tanqueCheio = !tanqueCheio },
                subtitulo = "O consumo é medido entre dois tanques cheios."
            )
            CampoTexto(
                valor = posto,
                onValor = { posto = it.take(60) },
                rotulo = "Posto",
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

            Spacer(Modifier.height(4.dp))
            BotaoPrimario("Salvar abastecimento", onClick = ::salvar, modifier = Modifier.fillMaxWidth())
            if (editando) {
                BotaoSecundario(
                    "Excluir abastecimento",
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
            titulo = "Excluir este abastecimento?",
            onDismiss = { confirmarExclusao = false },
            acoes = {
                BotaoTexto("Cancelar", onClick = { confirmarExclusao = false }, cor = c.muted)
                BotaoSecundario("Excluir", onClick = {
                    confirmarExclusao = false
                    original?.let { gastosViewModel.excluirAbastecimento(it, onVoltar) }
                }, perigo = true)
            }
        ) {
            Text("O consumo dos trechos vizinhos é recalculado.", style = t.body, color = c.ink)
        }
    }
}
