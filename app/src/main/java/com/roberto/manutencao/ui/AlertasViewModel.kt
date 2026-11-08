package com.roberto.manutencao.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.roberto.manutencao.ManutencaoApp
import com.roberto.manutencao.alertas.Agendador
import com.roberto.manutencao.data.AlertasStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class AlertasViewModel(
    private val app: Application,
    private val store: AlertasStore
) : ViewModel() {

    val ativos: StateFlow<Boolean> = store.ativos
    val lembreteKmDias: StateFlow<Int> = store.lembreteKmDias
    val pediuPermissao: StateFlow<Boolean> = store.pediuPermissao

    private val _mensagens = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val mensagens: SharedFlow<String> = _mensagens.asSharedFlow()

    fun definirAtivos(valor: Boolean) {
        store.definirAtivos(valor)
        if (valor) Agendador.agendar(app, substituir = true) else Agendador.cancelar(app)
    }

    fun definirLembreteKm(dias: Int) = store.definirLembreteKm(dias)

    fun marcarPermissaoPedida() = store.marcarPermissaoPedida()

    fun verificarAgora() {
        Agendador.verificarAgora(app)
        viewModelScope.launch { _mensagens.emit("Verificando. O aviso chega em instantes.") }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ManutencaoApp
                AlertasViewModel(app, app.container.alertasStore)
            }
        }
    }
}
