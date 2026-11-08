package com.roberto.manutencao.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Tema(val rotulo: String) {
    ESCURO("Escuro"),
    CLARO("Claro"),
    SISTEMA("Sistema")
}

/** Tema escolhido em "Mais". O escuro é o padrão do sistema Oficina. */
class TemaStore(context: Context) {
    private val prefs = context.getSharedPreferences("preferencias", Context.MODE_PRIVATE)

    private val _tema = MutableStateFlow(
        Tema.entries.firstOrNull { it.name == prefs.getString(CHAVE, null) } ?: Tema.ESCURO
    )
    val tema: StateFlow<Tema> = _tema.asStateFlow()

    fun definir(tema: Tema) {
        prefs.edit().putString(CHAVE, tema.name).apply()
        _tema.value = tema
    }

    private companion object {
        const val CHAVE = "tema"
    }
}
