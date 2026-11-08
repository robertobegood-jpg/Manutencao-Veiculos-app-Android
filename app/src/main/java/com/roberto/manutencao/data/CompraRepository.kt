package com.roberto.manutencao.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class CompraRepository(private val db: AppDatabase) {
    private val dao = db.compraDao()

    fun observar(veiculoId: Long): Flow<List<ItemCompra>> = dao.observarPorVeiculo(veiculoId)

    suspend fun buscar(id: Long): ItemCompra? = dao.buscar(id)

    suspend fun salvar(item: ItemCompra) {
        if (item.id == 0L) dao.inserir(item) else dao.atualizar(item)
    }

    suspend fun excluir(item: ItemCompra) = dao.excluir(item)

    suspend fun alternarComprado(item: ItemCompra) {
        dao.atualizar(
            if (item.comprado) item.copy(comprado = false, compradoEm = null)
            else item.copy(comprado = true, compradoEm = System.currentTimeMillis())
        )
    }

    /**
     * Coloca na lista as peças dos itens informados, copiando modelo e referência.
     * Pula os que já estão na lista a comprar. Devolve quantos entraram.
     */
    suspend fun adicionarDeItens(veiculoId: Long, itens: List<ItemManutencao>): Int = db.withTransaction {
        val jaNaLista = dao.itensPendentesVinculados(veiculoId).toSet()
        val novos = itens
            .filter { it.id !in jaNaLista }
            .map {
                ItemCompra(
                    veiculoId = veiculoId,
                    itemManutencaoId = it.id,
                    nome = it.nome,
                    modelo = it.modelo,
                    referencia = it.referencia
                )
            }
        if (novos.isNotEmpty()) dao.inserirVarios(novos)
        novos.size
    }

    suspend fun compradosSemRegistro(veiculoId: Long): List<ItemCompra> = dao.compradosSemRegistro(veiculoId)

    suspend fun limparComprados(veiculoId: Long) = dao.excluirComprados(veiculoId)
}
