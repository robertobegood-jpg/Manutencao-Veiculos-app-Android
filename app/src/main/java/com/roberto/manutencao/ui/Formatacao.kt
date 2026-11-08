package com.roberto.manutencao.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.roberto.manutencao.data.Datas
import com.roberto.manutencao.data.ItemCompra
import com.roberto.manutencao.data.ItemManutencao
import com.roberto.manutencao.data.SituacaoItem
import com.roberto.manutencao.data.StatusItem
import com.roberto.manutencao.data.TipoVeiculo
import com.roberto.manutencao.data.Veiculo
import com.roberto.manutencao.data.subtotalCentavos
import com.roberto.manutencao.ui.theme.Oficina
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale

private val PT_BR = Locale("pt", "BR")
private val formatoNumero: NumberFormat = NumberFormat.getIntegerInstance(PT_BR)
private val formatoDinheiro: NumberFormat = NumberFormat.getCurrencyInstance(PT_BR)

/** 12345 → "12.345" */
fun formatarNumero(valor: Int): String = formatoNumero.format(valor)

/** 12345 → "12.345 km" */
fun Int.formatarKm(): String = "${formatarNumero(this)} km"

/** 123456 centavos → "R$ 1.234,56" */
fun formatarDinheiro(centavos: Long): String = formatoDinheiro.format(centavos / 100.0)

/** Valor com N casas decimais: 5899 com 3 casas → "R$ 5,899" (preço do litro). */
fun formatarDinheiro(valor: Long, casas: Int): String {
    if (casas == 2) return formatarDinheiro(valor)
    val formato = NumberFormat.getCurrencyInstance(PT_BR).apply {
        minimumFractionDigits = casas
        maximumFractionDigits = casas
    }
    var divisor = 1.0
    repeat(casas) { divisor *= 10 }
    return formato.format(valor / divisor)
}

/** 11.83 → "11,8 km/l" */
fun formatarConsumo(kmL: Double): String = String.format(PT_BR, "%.1f km/l", kmL)

/** 40.5 → "40,5 L" (até 2 casas) */
fun formatarLitros(litros: Double): String = "${formatarQuantidade(litros)} L"

/** "novembro de 2026" */
fun nomeDoMes(data: LocalDate): String =
    data.format(java.time.format.DateTimeFormatter.ofPattern("MMMM 'de' yyyy", PT_BR))

fun plural(n: Int, singular: String, plural: String): String = "$n ${if (n == 1) singular else plural}"

/** Dias de calendário entre um instante (millis) e hoje. */
fun diasDesde(millis: Long): Long =
    ChronoUnit.DAYS.between(
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate(),
        LocalDate.now()
    ).coerceAtLeast(0)

/** "Atualizado hoje", "Atualizado ontem", "Atualizado há 12 dias" */
fun descreverLeitura(millis: Long): String = when (val d = diasDesde(millis)) {
    0L -> "Atualizado hoje"
    1L -> "Atualizado ontem"
    else -> "Atualizado há $d dias"
}

/** "Honda CG 160 2022" */
fun Veiculo.descricao(): String =
    listOf(marca, modelo, ano?.toString().orEmpty())
        .filter { it.isNotBlank() }
        .joinToString(" ")

/** Matiz que identifica o tipo de veículo nos pontos de cor. */
@Composable
@ReadOnlyComposable
fun corTipo(tipo: TipoVeiculo): Color = when (tipo) {
    TipoVeiculo.CARRO -> Oficina.cores.azul
    TipoVeiculo.MOTO -> Oficina.cores.vermelhao
}

/** Cor do ponto e cor do texto para cada situação. */
@Composable
@ReadOnlyComposable
fun coresStatus(status: StatusItem): Pair<Color, Color> {
    val c = Oficina.cores
    return when (status) {
        StatusItem.VENCIDO -> c.danger to c.danger
        StatusItem.PROXIMO -> c.ambar to c.ambarInk
        StatusItem.EM_DIA -> c.verde to c.verdeInk
        StatusItem.SEM_HISTORICO -> c.lineStrong to c.muted
        StatusItem.INATIVO -> c.line to c.faint
    }
}

fun rotuloStatus(status: StatusItem): String = when (status) {
    StatusItem.VENCIDO -> "Vencido"
    StatusItem.PROXIMO -> "Próximo"
    StatusItem.EM_DIA -> "Em dia"
    StatusItem.SEM_HISTORICO -> "Sem dados"
    StatusItem.INATIVO -> "Pausado"
}

private fun tempo(dias: Long): String = when {
    dias < 60 -> if (dias == 1L) "1 dia" else "$dias dias"
    dias < 730 -> (dias / 30).let { if (it == 1L) "1 mês" else "$it meses" }
    else -> (dias / 365).let { if (it == 1L) "1 ano" else "$it anos" }
}

/** "Faltam 2.300 km · faltam 4 meses" ou "Passou 300 km · venceu há 15 dias" */
fun descreverSituacao(s: SituacaoItem): String {
    if (s.status == StatusItem.INATIVO) return "Alertas pausados."
    if (s.status == StatusItem.SEM_HISTORICO) return "Informe a última troca para calcular."
    val partes = mutableListOf<String>()
    s.kmRestante?.let { km ->
        partes += when {
            km < 0 -> "passou ${(-km).formatarKm()}"
            km == 0 -> "chegou no km da troca"
            else -> "faltam ${km.formatarKm()}"
        }
    }
    s.diasRestantes?.let { dias ->
        partes += when {
            dias < 0 -> "venceu há ${tempo(-dias)}"
            dias == 0L -> "vence hoje"
            else -> "faltam ${tempo(dias)}"
        }
    }
    return partes.joinToString(" · ").replaceFirstChar { it.uppercase(PT_BR) } + "."
}

/** "A cada 10.000 km ou 12 meses" */
fun descreverIntervalo(item: ItemManutencao): String {
    val partes = listOfNotNull(
        item.intervaloKm?.let { it.formatarKm() },
        item.intervaloMeses?.let { if (it == 1) "1 mês" else "$it meses" }
    )
    return if (partes.isEmpty()) "Sem intervalo definido" else "A cada " + partes.joinToString(" ou ")
}

/** "Próxima troca: 45.000 km ou 12/03/2027." */
fun descreverProxima(s: SituacaoItem): String? {
    val partes = listOfNotNull(
        s.proximoKm?.formatarKm(),
        s.proximaData?.let { Datas.formatar(it) }
    )
    return if (partes.isEmpty()) null else "Próxima troca: " + partes.joinToString(" ou ") + "."
}

/** 4.0 → "4", 4.5 → "4,5", 0.75 → "0,75" */
fun formatarQuantidade(valor: Double): String =
    if (valor % 1.0 == 0.0) valor.toLong().toString()
    else String.format(PT_BR, "%.2f", valor).trimEnd('0').trimEnd(',')

/** "4 L", "1 un", "2 jogo" */
fun descreverQuantidade(item: ItemCompra): String = "${formatarQuantidade(item.quantidade)} ${item.unidade}"

/** "Mobil 5W30 · Ref. ABC123" */
fun descreverPeca(modelo: String, referencia: String): String =
    listOfNotNull(
        modelo.takeIf { it.isNotBlank() },
        referencia.takeIf { it.isNotBlank() }?.let { "Ref. $it" }
    ).joinToString(" · ")

/** Texto da lista para mandar pelo WhatsApp ou para a loja. */
fun textoListaCompras(veiculo: Veiculo, itens: List<ItemCompra>): String = buildString {
    val cabecalho = listOf(veiculo.apelido, veiculo.placa).filter { it.isNotBlank() }.joinToString(" · ")
    appendLine("Lista de compras · $cabecalho")
    val descricao = veiculo.descricao()
    if (descricao.isNotBlank()) appendLine(descricao)
    appendLine()
    itens.forEach { item ->
        append("• ${item.nome}")
        val peca = descreverPeca(item.modelo, item.referencia)
        if (peca.isNotBlank()) append(" — $peca")
        append(" — ${descreverQuantidade(item)}")
        if (item.precoUnitarioCentavos > 0) {
            append(" × ${formatarDinheiro(item.precoUnitarioCentavos)} = ${formatarDinheiro(item.subtotalCentavos())}")
        }
        appendLine()
        if (item.observacao.isNotBlank()) appendLine("   ${item.observacao}")
    }
    val total = itens.sumOf { it.subtotalCentavos() }
    if (total > 0) {
        appendLine()
        append("Total estimado: ${formatarDinheiro(total)}")
    }
}.trimEnd()
