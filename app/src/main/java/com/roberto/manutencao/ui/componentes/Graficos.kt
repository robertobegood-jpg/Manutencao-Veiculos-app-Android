package com.roberto.manutencao.ui.componentes

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roberto.manutencao.data.FatiaGasto
import com.roberto.manutencao.data.BarraGasto
import com.roberto.manutencao.data.PontoConsumo
import com.roberto.manutencao.ui.formatarConsumo
import com.roberto.manutencao.ui.formatarDinheiro
import com.roberto.manutencao.ui.theme.Oficina
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

private val PT_BR = Locale("pt", "BR")
private val FORMATO_DIA = DateTimeFormatter.ofPattern("dd/MM", PT_BR)

/** Entrada das réguas do sistema: cúbico ease-out, 0,9 s. */
private val EASING_REVELAR = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1f)

/** Teto "redondo" para o eixo, dividido em 4 linhas de grade. */
private fun tetoRedondo(maximoCentavos: Long): Long {
    val reais = (maximoCentavos / 100.0).coerceAtLeast(1.0)
    val magnitude = 10.0.pow(floor(log10(reais)))
    val passo = listOf(1.0, 2.0, 4.0, 5.0, 8.0, 10.0).first { it * magnitude >= reais }
    return (passo * magnitude * 100).toLong()
}

/** "1,5 mil", "800" */
private fun dinheiroCompacto(centavos: Long): String {
    val reais = centavos / 100.0
    return if (reais >= 1000) String.format(PT_BR, "%.1f mil", reais / 1000).replace(",0 mil", " mil")
    else String.format(PT_BR, "%.0f", reais)
}

/* ───────────── Indicadores ───────────── */

/** Grade de indicadores 2 × N com hairlines, no estilo .of-grid. */
@Composable
fun GradeIndicadores(itens: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    val c = Oficina.cores
    Column(
        modifier
            .fillMaxWidth()
            .border(1.dp, c.line)
    ) {
        itens.chunked(2).forEachIndexed { linha, par ->
            if (linha > 0) Linha()
            Row(Modifier.height(IntrinsicSize.Min)) {
                par.forEachIndexed { i, (rotulo, valor) ->
                    if (i > 0) {
                        Box(
                            Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(c.line)
                        )
                    }
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(16.dp)
                    ) {
                        Hud(rotulo)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            valor,
                            style = Oficina.tipos.heading.copy(fontSize = 20.sp, lineHeight = 26.sp),
                            color = if (valor == "—") c.faint else c.inkStrong,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (par.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Legenda com pontos de cor. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Legenda(itens: List<Pair<String, Color>>, modifier: Modifier = Modifier) {
    FlowRow(modifier.fillMaxWidth()) {
        itens.forEach { (rotulo, cor) ->
            Row(
                Modifier.padding(end = 16.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PontoStatus(cor)
                Spacer(Modifier.width(6.dp))
                Hud(rotulo)
            }
        }
    }
}

/* ───────────── Barras por período ───────────── */

/**
 * Barras empilhadas: manutenção, combustível e outras despesas.
 * Cada barra é um mês, trimestre ou ano (até 12 barras). Os rótulos são
 * espaçados conforme a largura disponível, e o ano aparece numa linha
 * própria na virada, para os meses nunca se embolarem.
 * Tocar numa barra mostra o detalhe dela no topo.
 */
@Composable
fun GraficoGastos(barras: List<BarraGasto>, modifier: Modifier = Modifier) {
    if (barras.isEmpty()) return
    val c = Oficina.cores
    val t = Oficina.tipos
    val medidor = rememberTextMeasurer()
    val cores = listOf(c.hues[0], c.hues[2], c.hues[3])

    var selecionado by remember(barras.size, barras.firstOrNull()?.titulo) { mutableIntStateOf(barras.lastIndex) }
    val progresso = remember(barras) { Animatable(0f) }
    LaunchedEffect(barras) { progresso.animateTo(1f, tween(900, easing = EASING_REVELAR)) }

    val teto = tetoRedondo(barras.maxOf { it.total })
    val estiloEixo = t.hud.copy(color = c.faint)
    val estiloRotulo = t.hud.copy(color = c.muted)
    val estiloAtivo = t.hud.copy(color = c.ink)
    val estiloAno = t.hud.copy(color = c.faint)

    Column(modifier.fillMaxWidth()) {
        val atual = barras[selecionado.coerceIn(0, barras.lastIndex)]
        Row(verticalAlignment = Alignment.Bottom) {
            Hud(atual.titulo, Modifier.weight(1f))
            Text(formatarDinheiro(atual.total), style = t.heading, color = c.inkStrong)
        }
        Text(
            "Manutenção ${formatarDinheiro(atual.manutencao)} · Combustível ${formatarDinheiro(atual.combustivel)} · Outras ${formatarDinheiro(atual.despesas)}",
            style = t.bodySm,
            color = c.muted
        )
        Spacer(Modifier.height(16.dp))

        Canvas(
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .pointerInput(barras.size) {
                    detectTapGestures { posicao ->
                        val faixa = size.width.toFloat() / barras.size
                        selecionado = (posicao.x / faixa).toInt().coerceIn(0, barras.lastIndex)
                    }
                }
        ) {
            val alturaRotulos = 38.dp.toPx()
            val alturaUtil = size.height - alturaRotulos
            val fio = 1.dp.toPx()
            val folga = 8.dp.toPx()

            // Grade: 4 divisões, valor no canto esquerdo de cada linha.
            for (i in 0..4) {
                val y = alturaUtil * (1f - i / 4f)
                drawLine(if (i == 0) c.lineStrong else c.line, Offset(0f, y), Offset(size.width, y), fio)
                if (i > 0) {
                    val rotulo = medidor.measure(dinheiroCompacto(teto * i / 4), estiloEixo)
                    drawText(rotulo, topLeft = Offset(0f, y - rotulo.size.height - 2.dp.toPx()))
                }
            }

            val faixa = size.width / barras.size
            val larguraBarra = (faixa * 0.56f).coerceAtMost(28.dp.toPx())
            fun centro(i: Int) = faixa * i + faixa / 2

            // De quantas em quantas barras cabe um rótulo, contando a partir da mais recente.
            val maiorRotulo = barras.maxOf { medidor.measure(it.rotulo, estiloRotulo).size.width }
            val passo = listOf(1, 2, 3, 4, 6).firstOrNull { faixa * it >= maiorRotulo + folga } ?: 12

            barras.forEachIndexed { i, b ->
                val x = centro(i) - larguraBarra / 2
                val alfa = if (i == selecionado) 1f else 0.45f
                var base = alturaUtil
                listOf(b.manutencao, b.combustivel, b.despesas).forEachIndexed { serie, valor ->
                    if (valor > 0) {
                        val h = alturaUtil * (valor.toFloat() / teto) * progresso.value
                        drawRect(
                            color = cores[serie].copy(alpha = alfa),
                            topLeft = Offset(x, base - h),
                            size = Size(larguraBarra, (h - fio).coerceAtLeast(0f))
                        )
                        base -= h
                    }
                }
                // Rótulo só nas barras espaçadas; o título da selecionada já aparece no topo.
                if ((barras.lastIndex - i) % passo == 0) {
                    val r = medidor.measure(b.rotulo, if (i == selecionado) estiloAtivo else estiloRotulo)
                    val esquerda = (centro(i) - r.size.width / 2).coerceIn(0f, size.width - r.size.width)
                    drawText(r, topLeft = Offset(esquerda, alturaUtil + 6.dp.toPx()))
                }
            }

            // Linha do ano: um traço e o ano em cada virada. Percorre da direita para a
            // esquerda, para que as viradas reais tenham prioridade sobre o ano da 1ª barra.
            var inicioProximo = Float.POSITIVE_INFINITY
            val yAno = alturaUtil + 22.dp.toPx()
            for (i in barras.lastIndex downTo 0) {
                val ano = barras[i].ano ?: continue
                val r = medidor.measure(ano, estiloAno)
                val esquerda = if (i == 0) 0f else centro(i) - faixa / 2
                val recuo = if (i == 0) 0f else 4.dp.toPx()
                if (esquerda + recuo + r.size.width + folga > inicioProximo) continue
                if (i > 0) drawLine(c.lineStrong, Offset(esquerda, alturaUtil), Offset(esquerda, yAno + r.size.height), fio)
                drawText(r, topLeft = Offset(esquerda + recuo, yAno))
                inicioProximo = esquerda
            }
        }
        Spacer(Modifier.height(8.dp))
        Legenda(listOf("Manutenção" to cores[0], "Combustível" to cores[1], "Outras" to cores[2]))
    }
}

/* ───────────── Proporção por categoria ───────────── */

/** Régua 100% empilhada e a lista de categorias com valor e porcentagem. */
@Composable
fun GraficoCategorias(fatias: List<FatiaGasto>, modifier: Modifier = Modifier) {
    if (fatias.isEmpty()) return
    val c = Oficina.cores
    val t = Oficina.tipos
    val total = fatias.sumOf { it.centavos }.coerceAtLeast(1)
    fun cor(f: FatiaGasto) = c.hues[f.indice % c.hues.size]

    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(12.dp)
        ) {
            fatias.forEachIndexed { i, f ->
                if (i > 0) Spacer(Modifier.width(2.dp))
                Box(
                    Modifier
                        .weight(f.centavos.toFloat())
                        .fillMaxHeight()
                        .background(cor(f))
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        fatias.forEach { f ->
            Linha()
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PontoStatus(cor(f))
                Spacer(Modifier.width(10.dp))
                Text(f.rotulo, style = t.body, color = c.ink, modifier = Modifier.weight(1f))
                Hud("${Math.round(f.centavos * 100.0 / total)}%")
                Spacer(Modifier.width(12.dp))
                Text(formatarDinheiro(f.centavos), style = t.body, color = c.inkStrong)
            }
        }
        Linha()
    }
}

/* ───────────── Linha de consumo ───────────── */

/** Consumo (km/l) de cada trecho medido, com a média tracejada. */
@Composable
fun GraficoConsumo(pontos: List<PontoConsumo>, media: Double?, modifier: Modifier = Modifier) {
    if (pontos.size < 2) return
    val c = Oficina.cores
    val t = Oficina.tipos
    val medidor = rememberTextMeasurer()
    val progresso = remember(pontos) { Animatable(0f) }
    LaunchedEffect(pontos) { progresso.animateTo(1f, tween(1100, easing = EASING_REVELAR)) }

    val valores = pontos.map { it.kmL } + listOfNotNull(media)
    val minimo = floor(valores.min() - 1).coerceAtLeast(0.0)
    val maximo = ceil(valores.max() + 1)
    val faixaY = (maximo - minimo).coerceAtLeast(1.0)
    val estiloEixo = t.hud.copy(color = c.faint)
    val estiloMedia = t.hud.copy(color = c.muted)
    val estiloValor = t.hud.copy(color = c.accentInk)

    Column(modifier.fillMaxWidth()) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            val alturaRotulo = 20.dp.toPx()
            val alturaUtil = size.height - alturaRotulo
            val fio = 1.dp.toPx()
            val margem = 8.dp.toPx()
            val largura = size.width - margem * 2
            fun y(v: Double) = (alturaUtil * (1 - (v - minimo) / faixaY)).toFloat()
            fun x(i: Int) = margem + largura * i / (pontos.size - 1)

            // Grade com 3 divisões.
            for (i in 0..3) {
                val v = minimo + faixaY * i / 3
                val yy = y(v)
                drawLine(if (i == 0) c.lineStrong else c.line, Offset(0f, yy), Offset(size.width, yy), fio)
                if (i > 0) {
                    val r = medidor.measure(String.format(PT_BR, "%.0f", v), estiloEixo)
                    drawText(r, topLeft = Offset(0f, yy - r.size.height - 2.dp.toPx()))
                }
            }

            // Média tracejada.
            if (media != null) {
                val ym = y(media)
                drawLine(
                    c.lineStrong, Offset(0f, ym), Offset(size.width, ym), fio,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
                )
                val r = medidor.measure("MÉDIA ${formatarConsumo(media).uppercase(PT_BR)}", estiloMedia)
                drawText(r, topLeft = Offset(size.width - r.size.width, ym - r.size.height - 2.dp.toPx()))
            }

            // Linha, revelada da esquerda para a direita.
            clipRect(right = size.width * progresso.value) {
                val caminho = Path()
                pontos.forEachIndexed { i, p ->
                    if (i == 0) caminho.moveTo(x(i), y(p.kmL)) else caminho.lineTo(x(i), y(p.kmL))
                }
                drawPath(caminho, c.accent, style = Stroke(width = 2.dp.toPx()))
                val lado = 6.dp.toPx()
                pontos.forEachIndexed { i, p ->
                    drawRect(c.accent, Offset(x(i) - lado / 2, y(p.kmL) - lado / 2), Size(lado, lado))
                }
            }

            // Valor do último ponto e datas das pontas.
            val ultimo = pontos.last()
            val rValor = medidor.measure(String.format(PT_BR, "%.1f", ultimo.kmL), estiloValor)
            drawText(
                rValor,
                topLeft = Offset(
                    (x(pontos.lastIndex) - rValor.size.width).coerceAtLeast(0f),
                    (y(ultimo.kmL) - rValor.size.height - 6.dp.toPx()).coerceAtLeast(0f)
                )
            )
            val rIni = medidor.measure(pontos.first().data.format(FORMATO_DIA), estiloEixo)
            val rFim = medidor.measure(ultimo.data.format(FORMATO_DIA), estiloEixo)
            drawText(rIni, topLeft = Offset(0f, alturaUtil + 6.dp.toPx()))
            drawText(rFim, topLeft = Offset(size.width - rFim.size.width, alturaUtil + 6.dp.toPx()))
        }
    }
}
