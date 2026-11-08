@file:OptIn(ExperimentalMaterial3Api::class)

package com.roberto.manutencao.ui.componentes

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.roberto.manutencao.data.Datas
import com.roberto.manutencao.ui.formatarDinheiro
import com.roberto.manutencao.ui.theme.Oficina
import java.util.Locale

private val PT_BR = Locale("pt", "BR")

/* ───────────── Texto ───────────── */

/** Rótulo de instrumento: mono, 11sp, sempre em maiúsculas. */
@Composable
fun Hud(
    texto: String,
    modifier: Modifier = Modifier,
    cor: Color = Oficina.cores.muted,
    estilo: TextStyle = Oficina.tipos.hud
) {
    Text(
        text = texto.uppercase(PT_BR),
        modifier = modifier,
        color = cor,
        style = estilo,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

/** Eyebrow numerado com régua: "02/SERVIÇOS ────────". */
@Composable
fun Eyebrow(numero: String, rotulo: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Hud("$numero/$rotulo")
        Spacer(Modifier.width(12.dp))
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(Oficina.cores.line)
        )
    }
}

/**
 * Título display. A palavra entre asteriscos sai em serif itálico:
 * "Tudo em *dia*."
 */
@Composable
fun TituloDisplay(
    texto: String,
    modifier: Modifier = Modifier,
    estilo: TextStyle = Oficina.tipos.displayMd,
    cor: Color = Oficina.cores.inkStrong
) {
    val serif = Oficina.tipos.serif
    val anotado = remember(texto, serif) {
        buildAnnotatedString {
            texto.split('*').forEachIndexed { i, parte ->
                if (i % 2 == 1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = serif,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Normal
                        )
                    ) { append(parte) }
                } else {
                    append(parte)
                }
            }
        }
    }
    Text(anotado, modifier = modifier, color = cor, style = estilo)
}

/* ───────────── Botões ───────────── */

/** Ação principal: bloco invertido, rótulo mono. */
@Composable
fun BotaoPrimario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    seta: Boolean = false
) {
    val cores = Oficina.cores
    val interacao = remember { MutableInteractionSource() }
    val pressionado by interacao.collectIsPressedAsState()
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .alpha(if (!habilitado) 0.4f else if (pressionado) 0.8f else 1f)
            .background(cores.invertBg)
            .clickable(
                interactionSource = interacao,
                indication = null,
                enabled = habilitado,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Hud(
            if (seta) "$texto  →" else texto,
            cor = cores.invertFg,
            estilo = Oficina.tipos.buttonLabel
        )
    }
}

/** Ação secundária: contorno de 1px; ao tocar, inverte. */
@Composable
fun BotaoSecundario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    perigo: Boolean = false
) {
    val cores = Oficina.cores
    val interacao = remember { MutableInteractionSource() }
    val pressionado by interacao.collectIsPressedAsState()
    val corBase = if (perigo) cores.danger else cores.ink
    val fundo by animateColorAsState(
        if (pressionado) (if (perigo) cores.danger else cores.invertBg) else Color.Transparent,
        animationSpec = tween(120, easing = LinearEasing),
        label = "fundo"
    )
    val frente by animateColorAsState(
        if (pressionado) cores.invertFg else corBase,
        animationSpec = tween(120, easing = LinearEasing),
        label = "frente"
    )
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .alpha(if (habilitado) 1f else 0.4f)
            .background(fundo)
            .border(1.dp, if (perigo) cores.danger else cores.lineStrong)
            .clickable(
                interactionSource = interacao,
                indication = null,
                enabled = habilitado,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Hud(texto, cor = frente, estilo = Oficina.tipos.buttonLabel)
    }
}

/** Ação de texto (link HUD). */
@Composable
fun BotaoTexto(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cor: Color = Oficina.cores.accentInk
) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Hud(texto, cor = cor, estilo = Oficina.tipos.buttonLabel)
    }
}

/* ───────────── Estrutura ───────────── */

/** Hairline horizontal de 1px. */
@Composable
fun Linha(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Oficina.cores.line)
    )
}

/** Marcas de canto (crosshairs) para emoldurar um bloco-chave. */
fun Modifier.marcasDeCanto(cor: Color, tamanho: Dp = 11.dp): Modifier = drawWithContent {
    drawContent()
    val t = tamanho.toPx() / 2
    val espessura = 1.dp.toPx()
    listOf(
        Offset(0f, 0f),
        Offset(size.width, 0f),
        Offset(0f, size.height),
        Offset(size.width, size.height)
    ).forEach { c ->
        drawLine(cor, Offset(c.x - t, c.y), Offset(c.x + t, c.y), espessura)
        drawLine(cor, Offset(c.x, c.y - t), Offset(c.x, c.y + t), espessura)
    }
}

/** Ponto de status, o único elemento redondo do sistema. */
@Composable
fun PontoStatus(cor: Color, modifier: Modifier = Modifier, tamanho: Dp = 8.dp) {
    Box(
        modifier
            .size(tamanho)
            .background(cor, CircleShape)
    )
}

/** Régua de progresso de 2px. */
@Composable
fun BarraProgresso(fracao: Float, cor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(Oficina.cores.line)
    ) {
        Box(
            Modifier
                .fillMaxWidth(fracao.coerceIn(0f, 1f))
                .height(2.dp)
                .background(cor)
        )
    }
}

/** Seletor segmentado em células: o item ativo vira bloco invertido. */
@Composable
fun <T> Segmentado(
    opcoes: List<T>,
    selecionada: T,
    rotulo: (T) -> String,
    onSelecionar: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val cores = Oficina.cores
    Row(
        modifier
            .fillMaxWidth()
            .border(1.dp, cores.line)
    ) {
        opcoes.forEachIndexed { i, opcao ->
            if (i > 0) {
                Box(
                    Modifier
                        .width(1.dp)
                        .height(44.dp)
                        .background(cores.line)
                )
            }
            val ativa = opcao == selecionada
            Box(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .background(if (ativa) cores.invertBg else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelecionar(opcao) },
                contentAlignment = Alignment.Center
            ) {
                Hud(rotulo(opcao), cor = if (ativa) cores.invertFg else cores.muted)
            }
        }
    }
}

/** Caixa de marcação quadrada de 20dp (só o desenho). */
@Composable
fun CaixaMarcacao(marcado: Boolean, modifier: Modifier = Modifier) {
    val cores = Oficina.cores
    Box(
        modifier
            .size(20.dp)
            .background(if (marcado) cores.invertBg else Color.Transparent)
            .border(1.dp, if (marcado) cores.invertBg else cores.lineStrong),
        contentAlignment = Alignment.Center
    ) {
        if (marcado) {
            Text(
                "✓",
                color = cores.invertFg,
                style = Oficina.tipos.buttonLabel.copy(fontSize = 12.sp, letterSpacing = 0.sp)
            )
        }
    }
}

/** Linha com caixa de marcação quadrada. */
@Composable
fun LinhaMarcavel(
    texto: String,
    marcado: Boolean,
    onAlternar: () -> Unit,
    modifier: Modifier = Modifier,
    subtitulo: String? = null
) {
    val cores = Oficina.cores
    Row(
        modifier
            .fillMaxWidth()
            .toggleable(value = marcado, role = Role.Checkbox, onValueChange = { onAlternar() })
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(20.dp)
                .background(if (marcado) cores.invertBg else Color.Transparent)
                .border(1.dp, if (marcado) cores.invertBg else cores.lineStrong),
            contentAlignment = Alignment.Center
        ) {
            if (marcado) {
                Text(
                    "✓",
                    color = cores.invertFg,
                    style = Oficina.tipos.buttonLabel.copy(fontSize = 12.sp, letterSpacing = 0.sp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(texto, style = Oficina.tipos.body, color = cores.ink)
            if (subtitulo != null) {
                Text(subtitulo, style = Oficina.tipos.bodySm, color = cores.muted)
            }
        }
    }
}

/** Cabeçalho das telas internas: voltar, eyebrow e título display. */
@Composable
fun CabecalhoInterno(
    eyebrow: String,
    titulo: String,
    onVoltar: () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .height(48.dp)
                .clickable(role = Role.Button, onClick = onVoltar)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Hud("←  Voltar", cor = Oficina.cores.ink)
        }
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)) {
            Hud(eyebrow)
            Spacer(Modifier.height(8.dp))
            TituloDisplay(titulo)
        }
        Linha()
    }
}

/** Indicador de carregamento discreto. */
@Composable
fun Carregando(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Hud("Carregando")
    }
}

/* ───────────── Diálogo ───────────── */

@Composable
fun DialogoOficina(
    titulo: String,
    onDismiss: () -> Unit,
    eyebrow: String? = null,
    acoes: @Composable RowScope.() -> Unit,
    conteudo: @Composable ColumnScope.() -> Unit
) {
    val cores = Oficina.cores
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(cores.bgRaised)
                .border(1.dp, cores.line)
                .padding(24.dp)
        ) {
            if (eyebrow != null) {
                Hud(eyebrow)
                Spacer(Modifier.height(8.dp))
            }
            Text(titulo, style = Oficina.tipos.heading, color = cores.inkStrong)
            Spacer(Modifier.height(16.dp))
            conteudo()
            Spacer(Modifier.height(24.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                content = acoes
            )
        }
    }
}

/* ───────────── Campos ───────────── */

@Composable
fun coresCampo(): TextFieldColors {
    val c = Oficina.cores
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = c.ink,
        unfocusedTextColor = c.ink,
        disabledTextColor = c.muted,
        errorTextColor = c.ink,
        focusedContainerColor = c.bgSunken,
        unfocusedContainerColor = c.bgSunken,
        disabledContainerColor = c.bgSunken,
        errorContainerColor = c.bgSunken,
        cursorColor = c.accentInk,
        errorCursorColor = c.danger,
        focusedBorderColor = c.accentInk,
        unfocusedBorderColor = c.line,
        disabledBorderColor = c.line,
        errorBorderColor = c.danger,
        focusedLabelColor = c.accentInk,
        unfocusedLabelColor = c.muted,
        disabledLabelColor = c.faint,
        errorLabelColor = c.danger,
        focusedPlaceholderColor = c.faint,
        unfocusedPlaceholderColor = c.faint,
        focusedSupportingTextColor = c.muted,
        unfocusedSupportingTextColor = c.muted,
        disabledSupportingTextColor = c.faint,
        errorSupportingTextColor = c.danger
    )
}

@Composable
fun CampoTexto(
    valor: String,
    onValor: (String) -> Unit,
    rotulo: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    placeholder: String? = null,
    erro: String? = null,
    ajuda: String? = null,
    teclado: KeyboardOptions = KeyboardOptions.Default,
    linhaUnica: Boolean = true,
    minLinhas: Int = 1,
    habilitado: Boolean = true,
    somenteLeitura: Boolean = false,
    trailing: (@Composable () -> Unit)? = null
) {
    val apoio = erro ?: ajuda
    OutlinedTextField(
        value = valor,
        onValueChange = onValor,
        modifier = modifier,
        enabled = habilitado,
        readOnly = somenteLeitura,
        textStyle = Oficina.tipos.body,
        label = { Text(rotulo) },
        placeholder = placeholder?.let { p -> { Text(p) } },
        trailingIcon = trailing,
        supportingText = apoio?.let { a -> { Text(a) } },
        isError = erro != null,
        keyboardOptions = teclado,
        singleLine = linhaUnica,
        minLines = minLinhas,
        shape = RectangleShape,
        colors = coresCampo()
    )
}

/** Campo de valor em reais: você digita só os números, ele formata. */
@Composable
fun CampoDinheiro(
    centavos: Long,
    onCentavos: (Long) -> Unit,
    rotulo: String,
    modifier: Modifier = Modifier,
    casas: Int = 2,
    erro: String? = null
) {
    // "centavos" está na unidade de 10^-casas (com casas = 3, são milésimos).
    val texto = if (centavos == 0L) "" else formatarDinheiro(centavos, casas)
    OutlinedTextField(
        value = TextFieldValue(texto, TextRange(texto.length)),
        onValueChange = { novo ->
            onCentavos(novo.text.filter(Char::isDigit).take(10).toLongOrNull() ?: 0L)
        },
        modifier = modifier,
        textStyle = Oficina.tipos.body,
        label = { Text(rotulo) },
        placeholder = { Text(if (casas == 3) "R$ 0,000" else "R$ 0,00") },
        supportingText = erro?.let { e -> { Text(e) } },
        isError = erro != null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RectangleShape,
        colors = coresCampo()
    )
}

/** Campo de data que abre o calendário ao tocar. */
@Composable
fun CampoData(
    valor: Long?,
    onValor: (Long?) -> Unit,
    rotulo: String,
    modifier: Modifier = Modifier,
    permiteLimpar: Boolean = false
) {
    val cores = Oficina.cores
    var aberto by remember { mutableStateOf(false) }
    val mostrarLimpar = permiteLimpar && valor != null

    Box(modifier) {
        CampoTexto(
            valor = valor?.let(Datas::formatar).orEmpty(),
            onValor = {},
            rotulo = rotulo,
            modifier = Modifier.fillMaxWidth(),
            somenteLeitura = true,
            trailing = if (mostrarLimpar) {
                {
                    IconButton(onClick = { onValor(null) }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Limpar data", tint = cores.muted)
                    }
                }
            } else null
        )
        Box(
            Modifier
                .matchParentSize()
                .padding(end = if (mostrarLimpar) 48.dp else 0.dp)
                .clickable(role = Role.Button) { aberto = true }
        )
    }

    if (aberto) {
        val estado = rememberDatePickerState(initialSelectedDateMillis = valor ?: Datas.hoje())
        val coresCalendario = DatePickerDefaults.colors(containerColor = cores.bgRaised)
        DatePickerDialog(
            onDismissRequest = { aberto = false },
            confirmButton = {
                BotaoTexto("Confirmar", onClick = {
                    estado.selectedDateMillis?.let(onValor)
                    aberto = false
                })
            },
            dismissButton = {
                BotaoTexto("Cancelar", onClick = { aberto = false }, cor = cores.muted)
            },
            shape = RectangleShape,
            colors = coresCalendario
        ) {
            DatePicker(state = estado, colors = coresCalendario, showModeToggle = false)
        }
    }
}
