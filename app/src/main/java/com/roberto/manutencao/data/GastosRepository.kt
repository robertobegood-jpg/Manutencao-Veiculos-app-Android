package com.roberto.manutencao.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class GastosRepository(private val db: AppDatabase) {
    private val dao = db.gastosDao()
    private val veiculoDao = db.veiculoDao()
    private val leituraDao = db.leituraKmDao()

    fun observarAbastecimentos(veiculoId: Long): Flow<List<Abastecimento>> = dao.observarAbastecimentos(veiculoId)

    fun observarDespesas(veiculoId: Long): Flow<List<Despesa>> = dao.observarDespesas(veiculoId)

    suspend fun buscarAbastecimento(id: Long): Abastecimento? = dao.buscarAbastecimento(id)

    suspend fun ultimoAbastecimento(veiculoId: Long): Abastecimento? = dao.ultimoAbastecimento(veiculoId)

    suspend fun buscarDespesa(id: Long): Despesa? = dao.buscarDespesa(id)

    /** Salva o abastecimento e, se o km for maior que o do veículo, atualiza o hodômetro. */
    suspend fun salvarAbastecimento(abastecimento: Abastecimento) {
        db.withTransaction {
            if (abastecimento.id == 0L) dao.inserirAbastecimento(abastecimento)
            else dao.atualizarAbastecimento(abastecimento)

            val veiculo = veiculoDao.buscarPorId(abastecimento.veiculoId)
            if (veiculo != null && abastecimento.km > veiculo.kmAtual) {
                veiculoDao.atualizarKm(veiculo.id, abastecimento.km)
                leituraDao.inserir(LeituraKm(veiculoId = veiculo.id, km = abastecimento.km))
            }
        }
    }

    suspend fun excluirAbastecimento(abastecimento: Abastecimento) = dao.excluirAbastecimento(abastecimento)

    suspend fun salvarDespesa(despesa: Despesa) {
        if (despesa.id == 0L) dao.inserirDespesa(despesa) else dao.atualizarDespesa(despesa)
    }

    suspend fun excluirDespesa(despesa: Despesa) = dao.excluirDespesa(despesa)
}
