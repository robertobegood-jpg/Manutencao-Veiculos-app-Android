package com.roberto.manutencao.ui.telas

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.roberto.manutencao.arquivos.Backup
import com.roberto.manutencao.arquivos.Compartilhar
import com.roberto.manutencao.data.Veiculo
import com.roberto.manutencao.ui.componentes.BotaoPrimario
import com.roberto.manutencao.ui.componentes.BotaoSecundario
import com.roberto.manutencao.ui.componentes.BotaoTexto
import com.roberto.manutencao.ui.componentes.CabecalhoInterno
import com.roberto.manutencao.ui.componentes.DialogoOficina
import com.roberto.manutencao.ui.componentes.Eyebrow
import com.roberto.manutencao.ui.componentes.Hud
import com.roberto.manutencao.ui.theme.Oficina

@Composable
fun DadosScreen(
    selecionado: Veiculo?,
    ocupado: String?,
    onVoltar: () -> Unit,
    onGerarPdf: (Long, (java.io.File) -> Unit) -> Unit,
    onGerarCsv: (Long, (List<java.io.File>) -> Unit) -> Unit,
    onBackup: (Uri) -> Unit,
    onRestaurar: (Uri) -> Unit
) {
    val c = Oficina.cores
    val t = Oficina.tipos
    val contexto = LocalContext.current
    var arquivoParaRestaurar by remember { mutableStateOf<Uri?>(null) }
    val livre = ocupado == null

    val salvarBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) onBackup(uri)
    }
    val escolherBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) arquivoParaRestaurar = uri
    }

    Column(Modifier.fillMaxSize()) {
        CabecalhoInterno(eyebrow = "Dados", titulo = "Relatórios e *backup*.", onVoltar = onVoltar)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (ocupado != null) {
                Hud(ocupado, cor = c.accentInk)
                Spacer(Modifier.height(16.dp))
            }

            Eyebrow("01", "Relatório${selecionado?.let { " · ${it.apelido}" }.orEmpty()}")
            Spacer(Modifier.height(8.dp))
            Text(
                "Histórico completo do veículo em PDF: itens, trocas, abastecimentos e despesas. Útil na revisão e na hora de vender.",
                style = t.body,
                color = c.muted
            )
            Spacer(Modifier.height(12.dp))
            BotaoPrimario(
                "Gerar relatório em PDF",
                onClick = {
                    selecionado?.let { v ->
                        onGerarPdf(v.id) { arquivo ->
                            Compartilhar.arquivos(contexto, listOf(arquivo), "application/pdf", "Enviar relatório")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                habilitado = livre && selecionado != null,
                seta = true
            )

            Spacer(Modifier.height(32.dp))
            Eyebrow("02", "Planilhas")
            Spacer(Modifier.height(8.dp))
            Text(
                "Manutenções, abastecimentos, despesas e itens em CSV, para abrir no Excel ou no Google Planilhas.",
                style = t.body,
                color = c.muted
            )
            Spacer(Modifier.height(12.dp))
            BotaoSecundario(
                "Exportar planilhas",
                onClick = {
                    selecionado?.let { v ->
                        onGerarCsv(v.id) { arquivos ->
                            Compartilhar.arquivos(contexto, arquivos, "text/csv", "Enviar planilhas")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                habilitado = livre && selecionado != null
            )

            Spacer(Modifier.height(32.dp))
            Eyebrow("03", "Backup")
            Spacer(Modifier.height(8.dp))
            Text(
                "Todos os veículos, registros e fotos num arquivo .zip. Guarde no Drive ou passe para outro celular.",
                style = t.body,
                color = c.muted
            )
            Spacer(Modifier.height(12.dp))
            BotaoSecundario(
                "Fazer backup",
                onClick = { salvarBackup.launch(Backup.nomeSugerido()) },
                modifier = Modifier.fillMaxWidth(),
                habilitado = livre
            )
            Spacer(Modifier.height(8.dp))
            BotaoSecundario(
                "Restaurar backup",
                onClick = { escolherBackup.launch(arrayOf("application/zip", "application/octet-stream", "application/x-zip-compressed")) },
                modifier = Modifier.fillMaxWidth(),
                habilitado = livre
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Restaurar troca tudo o que está neste celular pelo conteúdo do arquivo.",
                style = t.bodySm,
                color = c.muted
            )
        }
    }

    arquivoParaRestaurar?.let { uri ->
        DialogoOficina(
            eyebrow = "Restaurar",
            titulo = "Substituir os dados deste celular?",
            onDismiss = { arquivoParaRestaurar = null },
            acoes = {
                BotaoTexto("Cancelar", onClick = { arquivoParaRestaurar = null }, cor = c.muted)
                BotaoSecundario("Restaurar", onClick = {
                    arquivoParaRestaurar = null
                    onRestaurar(uri)
                }, perigo = true)
            }
        ) {
            Text(
                "Veículos, registros, compras, abastecimentos, despesas e fotos atuais serão trocados pelos do backup. Se tiver dúvida, faça um backup antes.",
                style = t.body,
                color = c.ink
            )
        }
    }
}
