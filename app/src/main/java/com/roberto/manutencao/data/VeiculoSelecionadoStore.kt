package com.roberto.manutencao.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Guarda qual veículo está selecionado, para o app abrir sempre no último usado. */
class VeiculoSelecionadoStore(context: Context) {
    private val prefs = context.getSharedPreferences("preferencias", Context.MODE_PRIVATE)

    private val _id = MutableStateFlow(prefs.getLong(CHAVE, -1L).takeIf { it > 0 })
    val id: StateFlow<Long?> = _id.asStateFlow()

    fun selecionar(id: Long?) {
        val editor = prefs.edit()
        if (id == null) editor.remove(CHAVE) else editor.putLong(CHAVE, id)
        editor.apply()
        _id.value = id
    }

    private companion object {
        const val CHAVE = "veiculo_selecionado"
    }
}
