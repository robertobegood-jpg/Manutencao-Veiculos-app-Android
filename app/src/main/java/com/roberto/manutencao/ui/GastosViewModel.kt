package com.roberto.manutencao.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.roberto.manutencao.ManutencaoApp
import com.roberto.manutencao.data.Abastecimento
import com.roberto.manutencao.data.AbastecimentoComConsumo
import com.roberto.manutencao.data.CalculoConsumo
import com.roberto.manutencao.data.Despesa
import com.roberto.manutencao.data.GastosRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class GastosViewModel(private val repo: GastosRepository) : ViewModel() {

    private val veiculoId = MutableStateFlow<Long?>(null)

    fun definirVeiculo(id: Long?) {
        veiculoId.value = id
    }

    /** Abastecimentos com o consumo de cada trecho, o mais recente primeiro. null = carregando. */
    val abastecimentos: StateFlow<List<AbastecimentoComConsumo>?> = veiculoId
        .flatMapLatest<Long?, List<AbastecimentoComConsumo>?> { id ->
            if (id == null) flowOf(null)
            else repo.observarAbastecimentos(id).map { CalculoConsumo.calcular(it) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val despesas: StateFlow<List<Despesa>?> = veiculoId
        .flatMapLatest<Long?, List<Despesa>?> { id ->
            if (id == null) flowOf(null) else repo.observarDespesas(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _mensagens = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val mensagens: SharedFlow<String> = _mensagens.asSharedFlow()

    suspend fun buscarAbastecimento(id: Long): Abastecimento? = repo.buscarAbastecimento(id)

    suspend fun ultimoAbastecimento(veiculoId: Long): Abastecimento? = repo.ultimoAbastecimento(veiculoId)

    suspend fun buscarDespesa(id: Long): Despesa? = repo.buscarDespesa(id)

    fun salvarAbastecimento(abastecimento: Abastecimento, aoConcluir: () -> Unit) {
        viewModelScope.launch {
            repo.salvarAbastecimento(abastecimento)
            _mensagens.emit("Abastecimento salvo.")
            aoConcluir()
        }
    }

    fun excluirAbastecimento(abastecimento: Abastecimento, aoConcluir: () -> Unit) {
        viewModelScope.launch {
            repo.excluirAbastecimento(abastecimento)
            _mensagens.emit("Abastecimento excluído.")
            aoConcluir()
        }
    }

    fun salvarDespesa(despesa: Despesa, aoConcluir: () -> Unit) {
        viewModelScope.launch {
            repo.salvarDespesa(despesa)
            _mensagens.emit("Despesa salva.")
            aoConcluir()
        }
    }

    fun excluirDespesa(despesa: Despesa, aoConcluir: () -> Unit) {
        viewModelScope.launch {
            repo.excluirDespesa(despesa)
            _mensagens.emit("Despesa excluída.")
            aoConcluir()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ManutencaoApp
                GastosViewModel(app.container.gastosRepository)
            }
        }
    }
}
