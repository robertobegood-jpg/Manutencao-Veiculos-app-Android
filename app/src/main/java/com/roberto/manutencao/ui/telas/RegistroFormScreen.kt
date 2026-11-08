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
import androidx.compose.ui.platform.LocalContext
import com.roberto.manutencao.arquivos.Anexos
import com.roberto.manutencao.data.Datas
import com.roberto.manutencao.data.ItemManutencao
import com.roberto.manutencao.data.RegistroManutencao
import com.roberto.manutencao.data.subtotalCentavos
import com.roberto.manutencao.ui.ComprasViewModel
import com.roberto.manutencao.ui.ManutencaoViewModel
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
import com.roberto.manutencao.ui.componentes.Linha
import com.roberto.manutencao.ui.componentes.LinhaMarcavel
import com.roberto.manutencao.ui.componentes.SecaoNotaFiscal
import com.roberto.manutencao.ui.descreverIntervalo
import com.roberto.manutencao.ui.descreverPeca
import com.roberto.manutencao.ui.descreverQuantidade
import com.roberto.manutencao.ui.formatarDinheiro
import com.roberto.manutencao.ui.formatarKm
import com.roberto.manutencao.ui.theme.Oficina

@Composable
fun RegistroFormScreen(
    veiculoId: Long,
    registroId: Long,
    itemInicialId: Long,
    viewModel: ManutencaoViewModel,
    veiculosViewModel: VeiculosViewModel,
    onVoltar: () -> Unit,
    deCompras: Boolean = false,
    comprasViewModel: ComprasViewModel? = null
) {
    val c = Oficina.cores
    val t = Oficina.tipos
    val contexto = LocalContext.current
    val editando = registroId != 0L

    var itensDisponiveis by remember { mutableStateOf<List<ItemManutencao>>(emptyList()) }
    var original by remember { mutableStateOf<RegistroManutencao?>(null) }
    var kmVeiculo by remember { mutableStateOf<Int?>(null) }
    var carregou by remember { mutableStateOf(false) }

    var preenchido by rememberSaveable { mutableStateOf(false) }
    var data by rememberSaveable { mutableStateOf(Datas.hoje()) }
    var km by rememberSaveable { mutableStateOf("") }
    var selecionados by rememberSaveable { mutableStateOf(longArrayOf()) }
    var local by rememberSaveable { mutableStateOf("") }
    var feitoPorMim by rememberSaveable { mutableStateOf(false) }
    var pecas by rememberSaveable { mutableStateOf(0L) }
    var maoDeObra by rememberSaveable { mutableStateOf(0L) }
    var observacoes by rememberSaveable { mutableStateOf("") }
    /** Compras que vão ficar ligadas a este registro. */
    var comprasVinculadas by rememberSaveable { mutableStateOf(longArrayOf()) }
    /** Fotos da nota: nomes de arquivo separados por "|", para sobreviver a girar a tela. */
    var anexosTexto by rememberSaveable { mutableStateOf("") }
    var anexosOriginais by rememberSaveable { mutableStateOf("") }
    val anexos = anexosTexto.split('|').filter { it.isNotBlank() }
    var tentouSalvar by rememberSaveable { mutableStateOf(false) }
    var confirmarExclusao by remember { mutableStateOf(false) }

    LaunchedEffect(registroId) {
        val ativos = viewModel.itensAtivos(veiculoId)
        val veiculo = veiculosViewModel.buscar(veiculoId)
        kmVeiculo = veiculo?.kmAtual
        if (editando) {
            val r = viewModel.buscarRegistro(registroId)
            original = r?.registro
            // Inclui itens pausados que já estavam neste registro.
            itensDisponiveis = (ativos + r?.itens.orEmpty()).distinctBy { it.id }.sortedBy { it.ordem }
            if (r != null && !preenchido) {
                data = r.registro.data
                km = r.registro.km.toString()
                selecionados = r.itens.map { it.id }.toLongArray()
                local = r.registro.local
                feitoPorMim = r.registro.feitoPorMim
                pecas = r.registro.valorPecasCentavos
                maoDeObra = r.registro.valorMaoDeObraCentavos
                observacoes = r.registro.observacoes
                anexosTexto = r.anexos.joinToString("|") { it.arquivo }
                anexosOriginais = anexosTexto
                preenchido = true
            }
        } else {
            itensDisponiveis = ativos
            if (!preenchido) {
                km = veiculo?.kmAtual?.toString().orEmpty()
                if (itemInicialId != 0L) selecionados = longArrayOf(itemInicialId)
                if (deCompras && comprasViewModel != null) {
                    // Preenche com as peças compradas que ainda não viraram registro.
                    val compras = comprasViewModel.compradosSemRegistro(veiculoId)
                    comprasVinculadas = compras.map { it.id }.toLongArray()
                    val disponiveis = ativos.map { it.id }.toSet()
                    selecionados = compras.mapNotNull { it.itemManutencaoId }
                        .filter { it in disponiveis }
                        .distinct()
                        .toLongArray()
                    pecas = compras.sumOf { it.subtotalCentavos() }
                    local = compras.map { it.loja.trim() }.filter { it.isNotBlank() }.distinct().singleOrNull().orEmpty()
                    observacoes = compras.joinToString("\n") { compra ->
                        listOf(compra.nome, descreverPeca(compra.modelo, compra.referencia), descreverQuantidade(compra))
                            .filter { it.isNotBlank() }
                            .joinToString(" · ")
                    }
                    compras.mapNotNull { it.compradoEm }.maxOrNull()?.let { data = Datas.doInstante(it) }
                }
                preenchido = true
            }
        }
        carregou = true
    }

    val kmNumero = km.toIntOrNull()
    val erroKm = if (tentouSalvar && kmNumero == null) "Informe o km." else null
    val avisoKm = kmVeiculo?.let { atual ->
        if (kmNumero != null && kmNumero > atual) "O hodômetro vai para ${kmNumero.formatarKm()}." else null
    }
    val erroItens = tentouSalvar && selecionados.isEmpty()

    fun alternar(id: Long) {
        selecionados = if (id in selecionados) selecionados.filter { it != id }.toLongArray() else selecionados + id
    }

    fun salvar() {
        tentouSalvar = true
        val k = kmNumero ?: return
        if (selecionados.isEmpty()) return
        val base = original
        if (editando && base == null) return
        val registro = (base ?: RegistroManutencao(veiculoId = veiculoId, data = data, km = k)).copy(
            data = data,
            km = k,
            local = if (feitoPorMim) "" else local.trim(),
            feitoPorMim = feitoPorMim,
            valorPecasCentavos = pecas,
            valorMaoDeObraCentavos = maoDeObra,
            observacoes = observacoes.trim()
        )
        viewModel.salvarRegistro(
            registro = registro,
            itemIds = selecionados.toSet(),
            comprasIds = comprasVinculadas.toList(),
            anexos = anexos,
            anexosRemovidos = anexosOriginais.split('|').filter { it.isNotBlank() && it !in anexos },
            aoConcluir = onVoltar
        )
    }

    Column(Modifier.fillMaxSize()) {
        CabecalhoInterno(
            eyebrow = "Registro",
            titulo = when {
                editando -> "Editar *registro*."
                deCompras -> "Registrar *compras*."
                else -> "Nova *manutenção*."
            },
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
            if (comprasVinculadas.isNotEmpty()) {
                Text(
                    "Preenchido com ${comprasVinculadas.size} ${if (comprasVinculadas.size == 1) "peça comprada" else "peças compradas"}. Confira os itens e o km.",
                    style = t.bodySm,
                    color = c.accentInk
                )
            }
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
                    rotulo = "Km",
                    erro = erroKm,
                    ajuda = avisoKm,
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(4.dp))
            Hud("Itens trocados · ${selecionados.size}", cor = if (erroItens) c.danger else c.muted)
            if (erroItens) {
                Text("Marque pelo menos um item.", style = t.bodySm, color = c.danger)
            }
            if (carregou && itensDisponiveis.isEmpty()) {
                Text(
                    "Nenhum item ativo. Cadastre itens na aba Serviços.",
                    style = t.bodySm,
                    color = c.muted
                )
            } else {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .border(1.dp, c.line)
                ) {
                    itensDisponiveis.forEachIndexed { i, item ->
                        if (i > 0) Linha()
                        LinhaMarcavel(
                            texto = item.nome,
                            marcado = item.id in selecionados,
                            onAlternar = { alternar(item.id) },
                            subtitulo = descreverIntervalo(item),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Hud("Onde foi feito")
            LinhaMarcavel(
                texto = "Feito por mim",
                marcado = feitoPorMim,
                onAlternar = { feitoPorMim = !feitoPorMim }
            )
            if (!feitoPorMim) {
                CampoTexto(
                    valor = local,
                    onValor = { local = it.take(60) },
                    rotulo = "Oficina ou loja",
                    teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
            }

            Spacer(Modifier.height(4.dp))
            Hud("Valores")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoDinheiro(pecas, { pecas = it }, "Peças", Modifier.weight(1f))
                CampoDinheiro(maoDeObra, { maoDeObra = it }, "Mão de obra", Modifier.weight(1f))
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Hud("Total", Modifier.weight(1f))
                Text(formatarDinheiro(pecas + maoDeObra), style = t.heading, color = c.inkStrong)
            }

            Spacer(Modifier.height(4.dp))
            SecaoNotaFiscal(
                anexos = anexos,
                onAdicionar = { novos -> anexosTexto = (anexos + novos).joinToString("|") },
                onRemover = { nome ->
                    anexosTexto = anexos.filter { it != nome }.joinToString("|")
                    // Foto que ainda não estava salva pode ser apagada na hora.
                    if (nome !in anexosOriginais.split('|')) Anexos.excluir(contexto, listOf(nome))
                }
            )

            CampoTexto(
                valor = observacoes,
                onValor = { observacoes = it.take(500) },
                rotulo = "Observações",
                linhaUnica = false,
                minLinhas = 3,
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            Spacer(Modifier.height(4.dp))
            BotaoPrimario("Salvar registro", onClick = ::salvar, modifier = Modifier.fillMaxWidth())
            if (editando) {
                BotaoSecundario(
                    "Excluir registro",
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
            titulo = "Excluir este registro?",
            onDismiss = { confirmarExclusao = false },
            acoes = {
                BotaoTexto("Cancelar", onClick = { confirmarExclusao = false }, cor = c.muted)
                BotaoSecundario("Excluir", onClick = {
                    confirmarExclusao = false
                    viewModel.excluirRegistro(registroId, onVoltar)
                }, perigo = true)
            }
        ) {
            Text(
                "Os vencimentos dos itens voltam a contar a partir da troca anterior.",
                style = t.body,
                color = c.ink
            )
        }
    }
}
