package com.roberto.manutencao.arquivos

import com.roberto.manutencao.data.AbastecimentoComConsumo
import com.roberto.manutencao.data.AppDatabase
import com.roberto.manutencao.data.CalculoConsumo
import com.roberto.manutencao.data.CalculoVencimento
import com.roberto.manutencao.data.Despesa
import com.roberto.manutencao.data.RegistroComItens
import com.roberto.manutencao.data.SituacaoItem
import com.roberto.manutencao.data.Veiculo
import java.time.LocalDate

/** Tudo de um veículo, reunido para o relatório e para as planilhas. */
data class DadosVeiculo(
    val veiculo: Veiculo,
    val situacoes: List<SituacaoItem>,
    val registros: List<RegistroComItens>,
    val abastecimentos: List<AbastecimentoComConsumo>,
    val despesas: List<Despesa>
) {
    companion object {
        suspend fun coletar(db: AppDatabase, veiculoId: Long): DadosVeiculo? {
            val veiculo = db.veiculoDao().buscarPorId(veiculoId) ?: return null
            val hoje = LocalDate.now()
            val situacoes = db.manutencaoDao().itensComUltimaTroca(veiculoId)
                .map { CalculoVencimento.calcular(it, veiculo.kmAtual, hoje) }
            return DadosVeiculo(
                veiculo = veiculo,
                situacoes = situacoes.sortedBy { it.item.ordem },
                registros = db.manutencaoDao().registrosDoVeiculo(veiculoId),
                abastecimentos = CalculoConsumo.calcular(db.gastosDao().abastecimentosDoVeiculo(veiculoId)),
                despesas = db.gastosDao().despesasDoVeiculo(veiculoId)
            )
        }
    }
}

/** "Meu Gol" → "meu-gol", para nomes de arquivo. */
fun slug(texto: String): String =
    java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "")
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .ifBlank { "veiculo" }
