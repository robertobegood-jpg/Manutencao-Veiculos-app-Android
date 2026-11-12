package com.roberto.manutencao.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class VeiculoRepository(private val db: AppDatabase) {
    private val veiculoDao = db.veiculoDao()
    private val leituraDao = db.leituraKmDao()
    private val manutencaoDao = db.manutencaoDao()

    fun observarVeiculos(): Flow<List<Veiculo>> = veiculoDao.observarTodos()

    fun observarVeiculo(id: Long): Flow<Veiculo?> = veiculoDao.observarPorId(id)

    fun observarLeituras(veiculoId: Long): Flow<List<LeituraKm>> =
        leituraDao.observarPorVeiculo(veiculoId)

    suspend fun buscar(id: Long): Veiculo? = veiculoDao.buscarPorId(id)

    /**
     * Insere ou atualiza. Um veículo novo já ganha a primeira leitura do hodômetro
     * e os itens de manutenção sugeridos para o tipo dele.
     */
    suspend fun salvar(veiculo: Veiculo): Long = db.withTransaction {
        if (veiculo.id == 0L) {
            val id = veiculoDao.inserir(veiculo)
            leituraDao.inserir(LeituraKm(veiculoId = id, km = veiculo.kmAtual))
            manutencaoDao.inserirItens(ModelosManutencao.criarItens(id, veiculo.tipo))
            id
        } else {
            val antes = veiculoDao.buscarPorId(veiculo.id)
            veiculoDao.atualizar(veiculo)
            // Correção de km pelo cadastro (ex.: digitou 2.010.000 em vez de 201.000).
            if (antes != null && antes.kmAtual != veiculo.kmAtual) {
                if (veiculo.kmAtual < antes.kmAtual) leituraDao.apagarAcimaDe(veiculo.id, veiculo.kmAtual)
                leituraDao.inserir(LeituraKm(veiculoId = veiculo.id, km = veiculo.kmAtual))
            }
            veiculo.id
        }
    }

    suspend fun excluir(veiculo: Veiculo) = veiculoDao.excluir(veiculo)

    suspend fun atualizarKm(veiculoId: Long, novoKm: Int) {
        db.withTransaction {
            val veiculo = veiculoDao.buscarPorId(veiculoId)
                ?: throw IllegalArgumentException("Veículo não encontrado.")
            require(novoKm >= veiculo.kmAtual) {
                "A quilometragem não pode ser menor que a atual (${veiculo.kmAtual} km)."
            }
            veiculoDao.atualizarKm(veiculoId, novoKm)
            leituraDao.inserir(LeituraKm(veiculoId = veiculoId, km = novoKm))
        }
    }
}
