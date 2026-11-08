package com.roberto.manutencao.data

/** Abastecimento com o consumo do trecho que ele fecha (se fechar um). */
data class AbastecimentoComConsumo(
    val abastecimento: Abastecimento,
    /** km/l do trecho entre o tanque cheio anterior e este. null se não deu para medir. */
    val consumoKmL: Double?,
    val kmRodados: Int?,
    val litrosTrecho: Double?
)

/**
 * Método do tanque cheio: o consumo só é medido quando o tanque é completado.
 * Soma os litros de todos os abastecimentos desde o tanque cheio anterior
 * (inclusive os parciais) e divide pelos km rodados no trecho.
 */
object CalculoConsumo {

    /** Devolve na ordem de exibição: o mais recente primeiro. */
    fun calcular(lista: List<Abastecimento>): List<AbastecimentoComConsumo> {
        val ordem = lista.sortedWith(compareBy<Abastecimento>({ it.km }, { it.data }, { it.id }))
        val trechos = mutableMapOf<Long, Triple<Double, Int, Double>>()
        var ultimoCheio: Abastecimento? = null
        var litrosAcumulados = 0.0

        for (a in ordem) {
            litrosAcumulados += a.litros
            if (a.tanqueCheio) {
                val anterior = ultimoCheio
                if (anterior != null && a.km > anterior.km && litrosAcumulados > 0.0) {
                    val km = a.km - anterior.km
                    trechos[a.id] = Triple(km / litrosAcumulados, km, litrosAcumulados)
                }
                ultimoCheio = a
                litrosAcumulados = 0.0
            }
        }

        return ordem.reversed().map { a ->
            val t = trechos[a.id]
            AbastecimentoComConsumo(a, t?.first, t?.second, t?.third)
        }
    }

    /** Média ponderada de todos os trechos medidos: km totais ÷ litros totais. */
    fun media(lista: List<AbastecimentoComConsumo>): Double? {
        val medidos = lista.filter { it.kmRodados != null && it.litrosTrecho != null }
        val litros = medidos.sumOf { it.litrosTrecho ?: 0.0 }
        if (medidos.isEmpty() || litros <= 0.0) return null
        return medidos.sumOf { it.kmRodados ?: 0 } / litros
    }
}
