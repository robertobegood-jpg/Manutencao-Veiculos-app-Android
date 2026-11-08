package com.roberto.manutencao.ui.componentes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.roberto.manutencao.data.SituacaoItem
import com.roberto.manutencao.data.StatusItem
import com.roberto.manutencao.ui.coresStatus
import com.roberto.manutencao.ui.descreverSituacao
import com.roberto.manutencao.ui.rotuloStatus
import com.roberto.manutencao.ui.theme.Oficina

/** Uma linha da lista de itens: ponto de status, nome, quanto falta e a régua. */
@Composable
fun LinhaSituacao(situacao: SituacaoItem, onClick: () -> Unit) {
    val c = Oficina.cores
    val t = Oficina.tipos
    val (corPonto, corTexto) = coresStatus(situacao.status)
    val ativo = situacao.status != StatusItem.INATIVO

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        PontoStatus(corPonto, Modifier.padding(top = 9.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    situacao.item.nome,
                    style = t.body,
                    color = if (ativo) c.ink else c.muted,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Hud(rotuloStatus(situacao.status), cor = corTexto)
            }
            Text(descreverSituacao(situacao), style = t.bodySm, color = c.muted)
            if (ativo && situacao.status != StatusItem.SEM_HISTORICO) {
                Spacer(Modifier.height(8.dp))
                BarraProgresso(situacao.progresso, corPonto)
            }
        }
    }
}
