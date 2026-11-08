package com.roberto.manutencao.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.roberto.manutencao.ManutencaoApp
import com.roberto.manutencao.data.CompraRepository
import com.roberto.manutencao.data.ItemCompra
import com.roberto.manutencao.data.ItemManutencao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ComprasViewModel(private val repo: CompraRepository) : ViewModel() {

    private val veiculoId = MutableStateFlow<Long?>(null)

    fun definirVeiculo(id: Long?) {
        veiculoId.value = id
    }

    /** Lista do veículo selecionado. null = carregando. */
    val compras: StateFlow<List<ItemCompra>?> = veiculoId
        .flatMapLatest<Long?, List<ItemCompra>?> { id ->
            if (id == null) flowOf(null) else repo.observar(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _mensagens = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val mensagens: SharedFlow<String> = _mensagens.asSharedFlow()

    suspend fun buscar(id: Long): ItemCompra? = repo.buscar(id)

    suspend fun compradosSemRegistro(veiculoId: Long): List<ItemCompra> = repo.compradosSemRegistro(veiculoId)

    fun salvar(item: ItemCompra, aoConcluir: () -> Unit) {
        viewModelScope.launch {
            repo.salvar(item)
            _mensagens.emit("Peça salva.")
            aoConcluir()
        }
    }

    fun excluir(item: ItemCompra, aoConcluir: () -> Unit = {}) {
        viewModelScope.launch {
            repo.excluir(item)
            _mensagens.emit("Peça removida.")
            aoConcluir()
        }
    }

    fun alternarComprado(item: ItemCompra) {
        viewModelScope.launch { repo.alternarComprado(item) }
    }

    fun adicionarDeItens(veiculoId: Long, itens: List<ItemManutencao>) {
        viewModelScope.launch {
            if (itens.isEmpty()) {
                _mensagens.emit("Nenhuma troca vencida ou próxima.")
                return@launch
            }
            val quantidade = repo.adicionarDeItens(veiculoId, itens)
            _mensagens.emit(
                if (quantidade > 0) "${plural(quantidade, "peça adicionada", "peças adicionadas")}."
                else "Essas peças já estão na lista."
            )
        }
    }

    fun limparComprados(veiculoId: Long) {
        viewModelScope.launch {
            repo.limparComprados(veiculoId)
            _mensagens.emit("Comprados removidos da lista.")
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ManutencaoApp
                ComprasViewModel(app.container.compraRepository)
            }
        }
    }
}
