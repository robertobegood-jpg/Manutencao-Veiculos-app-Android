package com.roberto.manutencao.ui.componentes

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.roberto.manutencao.ui.formatarKm
import com.roberto.manutencao.ui.theme.Oficina

@Composable
fun AtualizarKmDialog(
    kmAtual: Int,
    onDismiss: () -> Unit,
    onConfirmar: (Int) -> Unit
) {
    var texto by rememberSaveable { mutableStateOf("") }
    val km = texto.toIntOrNull()
    val erro = if (km != null && km < kmAtual) "Menor que o atual (${kmAtual.formatarKm()})." else null

    DialogoOficina(
        eyebrow = "Hodômetro",
        titulo = "Atualizar quilometragem",
        onDismiss = onDismiss,
        acoes = {
            BotaoTexto("Cancelar", onClick = onDismiss, cor = Oficina.cores.muted)
            BotaoPrimario(
                "Salvar km",
                onClick = { km?.let(onConfirmar) },
                habilitado = km != null && erro == null
            )
        }
    ) {
        Text(
            "Atual: ${kmAtual.formatarKm()}.",
            style = Oficina.tipos.bodySm,
            color = Oficina.cores.muted
        )
        Spacer(Modifier.height(12.dp))
        CampoTexto(
            valor = texto,
            onValor = { novo -> texto = novo.filter(Char::isDigit).take(7) },
            rotulo = "Km no painel agora",
            erro = erro,
            teclado = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
}
