package com.roberto.manutencao.ui.telas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.roberto.manutencao.data.Datas
import com.roberto.manutencao.data.Despesa
import com.roberto.manutencao.data.Veiculo
import com.roberto.manutencao.ui.componentes.BotaoPrimario
import com.roberto.manutencao.ui.componentes.CabecalhoInterno
import com.roberto.manutencao.ui.componentes.Hud
import com.roberto.manutencao.ui.componentes.Linha
import com.roberto.manutencao.ui.formatarDinheiro
import com.roberto.manutencao.ui.theme.Oficina
import java.time.LocalDate

@Composable
fun DespesasScreen(
    selecionado: Veiculo?,
    despesas: List<Despesa>?,
    onVoltar: () -> Unit,
    onNova: () -> Unit,
    onAbrir: (Long) -> Unit
) {
    val c = Oficina.cores
    val t = Oficina.tipos
    val lista = despesas.orEmpty()
    val ano = LocalDate.now().year
    val totalAno = lista.filter { Datas.paraLocalDate(it.data).year == ano }.sumOf { it.valorCentavos }

    Column(Modifier.fillMaxSize()) {
        CabecalhoInterno(
            eyebrow = "Despesas · ${selecionado?.apelido.orEmpty()}",
            titulo = "Despesas *avulsas*.",
            onVoltar = onVoltar
        )
        LazyColumn(Modifier.weight(1f)) {
            item {
                Text(
                    if (lista.isEmpty()) "IPVA, licenciamento, seguro, multas, pedágio e o que mais aparecer."
                    else "${formatarDinheiro(totalAno)} em $ano · ${formatarDinheiro(lista.sumOf { it.valorCentavos })} no total.",
                    style = t.bodyLg,
                    color = c.muted,
                    modifier = Modifier.padding(16.dp)
                )
            }
            items(lista, key = { it.id }) { despesa ->
                Linha()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onAbrir(despesa.id) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(Modifier.weight(1f)) {
                        Hud("${Datas.formatar(despesa.data)} · ${despesa.categoria.rotulo}")
                        Spacer(Modifier.height(4.dp))
                        Text(
                            despesa.descricao.ifBlank { despesa.categoria.rotulo },
                            style = t.body,
                            color = c.ink
                        )
                        if (despesa.observacao.isNotBlank()) {
                            Text(despesa.observacao, style = t.bodySm, color = c.muted, maxLines = 2)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(formatarDinheiro(despesa.valorCentavos), style = t.body, color = c.inkStrong)
                }
            }
            if (lista.isNotEmpty()) item { Linha() }
        }
        Linha()
        BotaoPrimario(
            "Adicionar despesa",
            onClick = onNova,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            seta = true
        )
    }
}
