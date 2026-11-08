package com.roberto.manutencao.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class ManutencaoRepository(private val db: AppDatabase) {
    private val dao = db.manutencaoDao()
    private val veiculoDao = db.veiculoDao()
    private val leituraDao = db.leituraKmDao()

    fun observarItens(veiculoId: Long): Flow<List<ItemComUltimaTroca>> = dao.observarItens(veiculoId)

    fun observarRegistros(veiculoId: Long): Flow<List<RegistroComItens>> = dao.observarRegistros(veiculoId)

    suspend fun buscarItem(id: Long): ItemManutencao? = dao.buscarItem(id)

    suspend fun buscarRegistro(id: Long): RegistroComItens? = dao.buscarRegistro(id)

    suspend fun itensAtivos(veiculoId: Long): List<ItemManutencao> = dao.itensAtivos(veiculoId)

    suspend fun salvarItem(item: ItemManutencao) {
        db.withTransaction {
            if (item.id == 0L) {
                dao.inserirItem(item.copy(ordem = dao.maiorOrdem(item.veiculoId) + 1))
            } else {
                dao.atualizarItem(item)
            }
        }
    }

    suspend fun excluirItem(item: ItemManutencao) = dao.excluirItem(item)

    /** Guarda no item o modelo e a referência informados na compra. */
    suspend fun atualizarPeca(itemId: Long, modelo: String, referencia: String) {
        val item = dao.buscarItem(itemId) ?: return
        dao.atualizarItem(item.copy(modelo = modelo, referencia = referencia))
    }

    /** Adiciona os itens sugeridos que o veículo ainda não tem. Devolve quantos entraram. */
    suspend fun adicionarSugeridos(veiculoId: Long, tipo: TipoVeiculo): Int = db.withTransaction {
        val existentes = dao.nomesItens(veiculoId).map { it.trim().lowercase() }.toSet()
        val novos = ModelosManutencao.criarItens(veiculoId, tipo, ordemInicial = dao.maiorOrdem(veiculoId) + 1)
            .filter { it.nome.lowercase() !in existentes }
        if (novos.isNotEmpty()) dao.inserirItens(novos)
        novos.size
    }

    /**
     * Salva o registro e os itens trocados. Se o km for maior que o do veículo,
     * o hodômetro é atualizado junto.
     */
    suspend fun salvarRegistro(
        registro: RegistroManutencao,
        itemIds: Set<Long>,
        comprasIds: List<Long> = emptyList(),
        anexos: List<String>? = null
    ): Long = db.withTransaction {
        val id = if (registro.id == 0L) {
            dao.inserirRegistro(registro)
        } else {
            dao.atualizarRegistro(registro)
            dao.limparItensDoRegistro(registro.id)
            registro.id
        }
        dao.inserirRegistroItens(itemIds.map { RegistroItem(registroId = id, itemId = it) })
        // Fotos: substitui o conjunto inteiro pelo que está na tela.
        if (anexos != null) {
            dao.limparAnexosDoRegistro(id)
            dao.inserirAnexos(anexos.map { Anexo(registroId = id, arquivo = it) })
        }
        // Compras que viraram este registro não aparecem de novo para registrar.
        if (comprasIds.isNotEmpty()) db.compraDao().marcarRegistrados(comprasIds, id)

        val veiculo = veiculoDao.buscarPorId(registro.veiculoId)
        if (veiculo != null && registro.km > veiculo.kmAtual) {
            veiculoDao.atualizarKm(veiculo.id, registro.km)
            leituraDao.inserir(LeituraKm(veiculoId = veiculo.id, km = registro.km))
        }
        id
    }

    suspend fun excluirRegistro(id: Long) = dao.excluirRegistro(id)
}
