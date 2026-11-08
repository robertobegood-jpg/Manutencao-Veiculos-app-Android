package com.roberto.manutencao.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.roberto.manutencao.ManutencaoApp
import com.roberto.manutencao.arquivos.Anexos
import com.roberto.manutencao.data.CalculoVencimento
import com.roberto.manutencao.data.ItemManutencao
import com.roberto.manutencao.data.ManutencaoRepository
import com.roberto.manutencao.data.RegistroComItens
import com.roberto.manutencao.data.RegistroManutencao
import com.roberto.manutencao.data.SituacaoItem
import com.roberto.manutencao.data.Veiculo
import com.roberto.manutencao.data.VeiculoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ManutencaoViewModel(
    private val app: Application,
    private val repo: ManutencaoRepository,
    private val veiculoRepo: VeiculoRepository
) : ViewModel() {

    private val veiculoId = MutableStateFlow<Long?>(null)

    /** Chamado pela tela sempre que o veículo selecionado muda. */
    fun definirVeiculo(id: Long?) {
        veiculoId.value = id
    }

    /** Itens do veículo com a situação calculada, os mais urgentes primeiro. null = carregando. */
    val situacoes: StateFlow<List<SituacaoItem>?> = veiculoId
        .flatMapLatest<Long?, List<SituacaoItem>?> { id ->
            if (id == null) {
                flowOf(null)
            } else {
                combine(repo.observarItens(id), veiculoRepo.observarVeiculo(id)) { itens, veiculo ->
                    val km = veiculo?.kmAtual ?: 0
                    val hoje = LocalDate.now()
                    CalculoVencimento.ordenar(itens.map { CalculoVencimento.calcular(it, km, hoje) })
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val registros: StateFlow<List<RegistroComItens>?> = veiculoId
        .flatMapLatest<Long?, List<RegistroComItens>?> { id ->
            if (id == null) flowOf(null) else repo.observarRegistros(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _mensagens = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val mensagens: SharedFlow<String> = _mensagens.asSharedFlow()

    suspend fun buscarItem(id: Long): ItemManutencao? = repo.buscarItem(id)

    suspend fun buscarRegistro(id: Long): RegistroComItens? = repo.buscarRegistro(id)

    suspend fun itensAtivos(veiculoId: Long): List<ItemManutencao> = repo.itensAtivos(veiculoId)

    fun salvarItem(item: ItemManutencao, aoConcluir: () -> Unit) {
        viewModelScope.launch {
            repo.salvarItem(item)
            _mensagens.emit("Item salvo.")
            aoConcluir()
        }
    }

    fun excluirItem(item: ItemManutencao, aoConcluir: () -> Unit) {
        viewModelScope.launch {
            repo.excluirItem(item)
            _mensagens.emit("Item excluído.")
            aoConcluir()
        }
    }

    /** Atualiza modelo e referência do item sem mostrar mensagem. */
    fun atualizarPecaDoItem(itemId: Long, modelo: String, referencia: String) {
        viewModelScope.launch { repo.atualizarPeca(itemId, modelo, referencia) }
    }

    fun adicionarSugeridos(veiculo: Veiculo) {
        viewModelScope.launch {
            val quantidade = repo.adicionarSugeridos(veiculo.id, veiculo.tipo)
            _mensagens.emit(
                if (quantidade > 0) "${plural(quantidade, "item adicionado", "itens adicionados")}."
                else "Todos os itens sugeridos já estão na lista."
            )
        }
    }

    fun salvarRegistro(
        registro: RegistroManutencao,
        itemIds: Set<Long>,
        comprasIds: List<Long> = emptyList(),
        anexos: List<String>? = null,
        anexosRemovidos: List<String> = emptyList(),
        aoConcluir: () -> Unit
    ) {
        viewModelScope.launch {
            repo.salvarRegistro(registro, itemIds, comprasIds, anexos)
            if (anexosRemovidos.isNotEmpty()) {
                withContext(Dispatchers.IO) { Anexos.excluir(app, anexosRemovidos) }
            }
            _mensagens.emit("Manutenção registrada.")
            aoConcluir()
        }
    }

    fun excluirRegistro(id: Long, aoConcluir: () -> Unit) {
        viewModelScope.launch {
            repo.excluirRegistro(id)
            _mensagens.emit("Registro excluído.")
            aoConcluir()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ManutencaoApp
                ManutencaoViewModel(app, app.container.manutencaoRepository, app.container.veiculoRepository)
            }
        }
    }
}
