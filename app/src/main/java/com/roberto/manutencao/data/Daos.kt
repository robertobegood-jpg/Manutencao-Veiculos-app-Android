package com.roberto.manutencao.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

const val SQL_ITENS_COM_ULTIMA_TROCA = """
    SELECT i.*,
        (SELECT r.km FROM registros_manutencao r
            INNER JOIN registro_itens ri ON ri.registroId = r.id
            WHERE ri.itemId = i.id
            ORDER BY r.data DESC, r.km DESC LIMIT 1) AS registroKm,
        (SELECT r.data FROM registros_manutencao r
            INNER JOIN registro_itens ri ON ri.registroId = r.id
            WHERE ri.itemId = i.id
            ORDER BY r.data DESC, r.km DESC LIMIT 1) AS registroData
    FROM itens_manutencao i
    WHERE i.veiculoId = :veiculoId
    ORDER BY i.ordem, i.nome
"""

@Dao
interface VeiculoDao {
    @Query("SELECT * FROM veiculos ORDER BY apelido COLLATE NOCASE")
    fun observarTodos(): Flow<List<Veiculo>>

    @Query("SELECT * FROM veiculos WHERE id = :id")
    fun observarPorId(id: Long): Flow<Veiculo?>

    @Query("SELECT * FROM veiculos ORDER BY apelido COLLATE NOCASE")
    suspend fun todos(): List<Veiculo>

    @Query("SELECT * FROM veiculos WHERE id = :id")
    suspend fun buscarPorId(id: Long): Veiculo?

    @Insert
    suspend fun inserir(veiculo: Veiculo): Long

    @Update
    suspend fun atualizar(veiculo: Veiculo)

    @Delete
    suspend fun excluir(veiculo: Veiculo)

    @Query("UPDATE veiculos SET kmAtual = :km WHERE id = :id")
    suspend fun atualizarKm(id: Long, km: Int)
}

@Dao
interface LeituraKmDao {
    @Insert
    suspend fun inserir(leitura: LeituraKm): Long

    @Query("SELECT * FROM leituras_km WHERE veiculoId = :veiculoId ORDER BY data DESC")
    fun observarPorVeiculo(veiculoId: Long): Flow<List<LeituraKm>>

    /** Usado ao corrigir um km digitado errado: tira as leituras acima do valor certo. */
    @Query("DELETE FROM leituras_km WHERE veiculoId = :veiculoId AND km > :km")
    suspend fun apagarAcimaDe(veiculoId: Long, km: Int)

    @Query("SELECT MAX(data) FROM leituras_km WHERE veiculoId = :veiculoId")
    suspend fun ultimaLeitura(veiculoId: Long): Long?
}

@Dao
interface ManutencaoDao {

    /** Itens do veículo com o km e a data da troca mais recente registrada. */
    @Query(SQL_ITENS_COM_ULTIMA_TROCA)
    fun observarItens(veiculoId: Long): Flow<List<ItemComUltimaTroca>>

    /** O mesmo, numa consulta só (usado pela verificação em segundo plano). */
    @Query(SQL_ITENS_COM_ULTIMA_TROCA)
    suspend fun itensComUltimaTroca(veiculoId: Long): List<ItemComUltimaTroca>

    @Query("SELECT * FROM itens_manutencao WHERE id = :id")
    suspend fun buscarItem(id: Long): ItemManutencao?

    @Query("SELECT * FROM itens_manutencao WHERE veiculoId = :veiculoId AND ativo = 1 ORDER BY ordem, nome")
    suspend fun itensAtivos(veiculoId: Long): List<ItemManutencao>

    @Query("SELECT nome FROM itens_manutencao WHERE veiculoId = :veiculoId")
    suspend fun nomesItens(veiculoId: Long): List<String>

    @Query("SELECT COALESCE(MAX(ordem), 0) FROM itens_manutencao WHERE veiculoId = :veiculoId")
    suspend fun maiorOrdem(veiculoId: Long): Int

    @Insert
    suspend fun inserirItem(item: ItemManutencao): Long

    @Insert
    suspend fun inserirItens(itens: List<ItemManutencao>)

    @Update
    suspend fun atualizarItem(item: ItemManutencao)

    @Delete
    suspend fun excluirItem(item: ItemManutencao)

    @Transaction
    @Query("SELECT * FROM registros_manutencao WHERE veiculoId = :veiculoId ORDER BY data DESC, km DESC")
    fun observarRegistros(veiculoId: Long): Flow<List<RegistroComItens>>

    @Transaction
    @Query("SELECT * FROM registros_manutencao WHERE id = :id")
    suspend fun buscarRegistro(id: Long): RegistroComItens?

    @Insert
    suspend fun inserirRegistro(registro: RegistroManutencao): Long

    @Update
    suspend fun atualizarRegistro(registro: RegistroManutencao)

    @Query("DELETE FROM registros_manutencao WHERE id = :id")
    suspend fun excluirRegistro(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun inserirRegistroItens(itens: List<RegistroItem>)

    @Query("DELETE FROM registro_itens WHERE registroId = :registroId")
    suspend fun limparItensDoRegistro(registroId: Long)

    @Transaction
    @Query("SELECT * FROM registros_manutencao WHERE veiculoId = :veiculoId ORDER BY data DESC, km DESC")
    suspend fun registrosDoVeiculo(veiculoId: Long): List<RegistroComItens>

    @Query("SELECT arquivo FROM anexos WHERE registroId = :registroId")
    suspend fun arquivosDoRegistro(registroId: Long): List<String>

    @Query("DELETE FROM anexos WHERE registroId = :registroId")
    suspend fun limparAnexosDoRegistro(registroId: Long)

    @Insert
    suspend fun inserirAnexos(anexos: List<Anexo>)

    @Query("SELECT arquivo FROM anexos")
    suspend fun todosOsArquivos(): List<String>
}

/** Leitura e escrita de todas as tabelas, para backup e restauração. */
@Dao
interface BackupDao {
    @Query("SELECT * FROM veiculos") suspend fun veiculos(): List<Veiculo>
    @Query("SELECT * FROM leituras_km") suspend fun leituras(): List<LeituraKm>
    @Query("SELECT * FROM itens_manutencao") suspend fun itens(): List<ItemManutencao>
    @Query("SELECT * FROM registros_manutencao") suspend fun registros(): List<RegistroManutencao>
    @Query("SELECT * FROM registro_itens") suspend fun registroItens(): List<RegistroItem>
    @Query("SELECT * FROM itens_compra") suspend fun compras(): List<ItemCompra>
    @Query("SELECT * FROM abastecimentos") suspend fun abastecimentos(): List<Abastecimento>
    @Query("SELECT * FROM despesas") suspend fun despesas(): List<Despesa>
    @Query("SELECT * FROM anexos") suspend fun anexos(): List<Anexo>

    @Query("DELETE FROM anexos") suspend fun limparAnexos()
    @Query("DELETE FROM registro_itens") suspend fun limparRegistroItens()
    @Query("DELETE FROM itens_compra") suspend fun limparCompras()
    @Query("DELETE FROM registros_manutencao") suspend fun limparRegistros()
    @Query("DELETE FROM itens_manutencao") suspend fun limparItens()
    @Query("DELETE FROM abastecimentos") suspend fun limparAbastecimentos()
    @Query("DELETE FROM despesas") suspend fun limparDespesas()
    @Query("DELETE FROM leituras_km") suspend fun limparLeituras()
    @Query("DELETE FROM veiculos") suspend fun limparVeiculos()

    @Insert suspend fun inserirVeiculos(lista: List<Veiculo>)
    @Insert suspend fun inserirLeituras(lista: List<LeituraKm>)
    @Insert suspend fun inserirItens(lista: List<ItemManutencao>)
    @Insert suspend fun inserirRegistros(lista: List<RegistroManutencao>)
    @Insert suspend fun inserirRegistroItens(lista: List<RegistroItem>)
    @Insert suspend fun inserirCompras(lista: List<ItemCompra>)
    @Insert suspend fun inserirAbastecimentos(lista: List<Abastecimento>)
    @Insert suspend fun inserirDespesas(lista: List<Despesa>)
    @Insert suspend fun inserirAnexos(lista: List<Anexo>)
}

@Dao
interface GastosDao {
    @Query("SELECT * FROM abastecimentos WHERE veiculoId = :veiculoId ORDER BY km DESC, data DESC, id DESC")
    fun observarAbastecimentos(veiculoId: Long): Flow<List<Abastecimento>>

    @Query("SELECT * FROM abastecimentos WHERE veiculoId = :veiculoId ORDER BY km DESC, data DESC, id DESC")
    suspend fun abastecimentosDoVeiculo(veiculoId: Long): List<Abastecimento>

    @Query("SELECT * FROM despesas WHERE veiculoId = :veiculoId ORDER BY data DESC, id DESC")
    suspend fun despesasDoVeiculo(veiculoId: Long): List<Despesa>

    @Query("SELECT * FROM abastecimentos WHERE id = :id")
    suspend fun buscarAbastecimento(id: Long): Abastecimento?

    @Query("SELECT * FROM abastecimentos WHERE veiculoId = :veiculoId ORDER BY km DESC, data DESC LIMIT 1")
    suspend fun ultimoAbastecimento(veiculoId: Long): Abastecimento?

    @Insert
    suspend fun inserirAbastecimento(abastecimento: Abastecimento): Long

    @Update
    suspend fun atualizarAbastecimento(abastecimento: Abastecimento)

    @Delete
    suspend fun excluirAbastecimento(abastecimento: Abastecimento)

    @Query("SELECT * FROM despesas WHERE veiculoId = :veiculoId ORDER BY data DESC, id DESC")
    fun observarDespesas(veiculoId: Long): Flow<List<Despesa>>

    @Query("SELECT * FROM despesas WHERE id = :id")
    suspend fun buscarDespesa(id: Long): Despesa?

    @Insert
    suspend fun inserirDespesa(despesa: Despesa): Long

    @Update
    suspend fun atualizarDespesa(despesa: Despesa)

    @Delete
    suspend fun excluirDespesa(despesa: Despesa)
}

@Dao
interface CompraDao {
    /** A comprar primeiro (na ordem em que entraram), depois os comprados (mais recentes primeiro). */
    @Query(
        """
        SELECT * FROM itens_compra WHERE veiculoId = :veiculoId
        ORDER BY comprado, CASE WHEN comprado = 1 THEN -IFNULL(compradoEm, 0) ELSE criadoEm END
        """
    )
    fun observarPorVeiculo(veiculoId: Long): Flow<List<ItemCompra>>

    @Query("SELECT * FROM itens_compra WHERE id = :id")
    suspend fun buscar(id: Long): ItemCompra?

    @Insert
    suspend fun inserir(item: ItemCompra): Long

    @Insert
    suspend fun inserirVarios(itens: List<ItemCompra>)

    @Update
    suspend fun atualizar(item: ItemCompra)

    @Delete
    suspend fun excluir(item: ItemCompra)

    @Query(
        "SELECT itemManutencaoId FROM itens_compra " +
            "WHERE veiculoId = :veiculoId AND comprado = 0 AND itemManutencaoId IS NOT NULL"
    )
    suspend fun itensPendentesVinculados(veiculoId: Long): List<Long>

    @Query(
        "SELECT * FROM itens_compra " +
            "WHERE veiculoId = :veiculoId AND comprado = 1 AND registroId IS NULL ORDER BY compradoEm"
    )
    suspend fun compradosSemRegistro(veiculoId: Long): List<ItemCompra>

    @Query("UPDATE itens_compra SET registroId = :registroId WHERE id IN (:ids)")
    suspend fun marcarRegistrados(ids: List<Long>, registroId: Long)

    @Query("DELETE FROM itens_compra WHERE veiculoId = :veiculoId AND comprado = 1")
    suspend fun excluirComprados(veiculoId: Long)
}
