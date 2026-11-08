package com.roberto.manutencao.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.roberto.manutencao.ManutencaoApp
import com.roberto.manutencao.data.LeituraKm
import com.roberto.manutencao.data.Veiculo
import com.roberto.manutencao.data.VeiculoRepository
import com.roberto.manutencao.data.VeiculoSelecionadoStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VeiculosViewModel(
    private val repo: VeiculoRepository,
    private val store: VeiculoSelecionadoStore
) : ViewModel() {

    /** null enquanto o banco ainda está carregando. */
    val veiculos: StateFlow<List<Veiculo>?> = repo.observarVeiculos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** O veículo escolhido no seletor; se nenhum foi escolhido, usa o primeiro da lista. */
    val veiculoSelecionado: StateFlow<Veiculo?> = combine(veiculos, store.id) { lista, id ->
        lista?.let { l -> l.firstOrNull { it.id == id } ?: l.firstOrNull() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Quando o hodômetro do veículo selecionado foi atualizado pela última vez. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val ultimaLeitura: StateFlow<Long?> = veiculoSelecionado
        .map { it?.id }
        .distinctUntilChanged()
        .flatMapLatest<Long?, Long?> { id ->
            if (id == null) flowOf(null)
            else repo.observarLeituras(id).map { leituras -> leituras.firstOrNull()?.data }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Todas as leituras do hodômetro do veículo selecionado (para os km rodados no painel). */
    @OptIn(ExperimentalCoroutinesApi::class)
    val leituras: StateFlow<List<LeituraKm>> = veiculoSelecionado
        .map { it?.id }
        .distinctUntilChanged()
        .flatMapLatest { id -> if (id == null) flowOf(emptyList<LeituraKm>()) else repo.observarLeituras(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _mensagens = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val mensagens: SharedFlow<String> = _mensagens.asSharedFlow()

    fun selecionar(id: Long) = store.selecionar(id)

    suspend fun buscar(id: Long): Veiculo? = repo.buscar(id)

    fun salvar(veiculo: Veiculo, aoConcluir: () -> Unit) {
        viewModelScope.launch {
            val limpo = veiculo.copy(
                apelido = veiculo.apelido.trim(),
                marca = veiculo.marca.trim(),
                modelo = veiculo.modelo.trim(),
                placa = veiculo.placa.trim().uppercase()
            )
            val id = repo.salvar(limpo)
            if (veiculo.id == 0L) store.selecionar(id)
            _mensagens.emit("Veículo salvo.")
            aoConcluir()
        }
    }

    fun excluir(veiculo: Veiculo) {
        viewModelScope.launch {
            repo.excluir(veiculo)
            if (store.id.value == veiculo.id) store.selecionar(null)
            _mensagens.emit("Veículo excluído.")
        }
    }

    fun atualizarKm(veiculoId: Long, km: Int) {
        viewModelScope.launch {
            runCatching { repo.atualizarKm(veiculoId, km) }
                .onSuccess { _mensagens.emit("Quilometragem atualizada.") }
                .onFailure { _mensagens.emit(it.message ?: "Não foi possível atualizar.") }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ManutencaoApp
                VeiculosViewModel(app.container.veiculoRepository, app.container.veiculoSelecionadoStore)
            }
        }
    }
}
