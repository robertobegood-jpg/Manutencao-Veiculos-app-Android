package com.roberto.manutencao.ui.componentes

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.roberto.manutencao.arquivos.Anexos
import com.roberto.manutencao.ui.theme.Oficina
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Fotos da nota fiscal de um registro: miniaturas, câmera e galeria.
 * [anexos] são nomes de arquivo na pasta de anexos.
 */
@Composable
fun SecaoNotaFiscal(
    anexos: List<String>,
    onAdicionar: (List<String>) -> Unit,
    onRemover: (String) -> Unit
) {
    val c = Oficina.cores
    val t = Oficina.tipos
    val contexto = LocalContext.current
    val escopo = rememberCoroutineScope()
    var arquivoDaCamera by rememberSaveable { mutableStateOf<String?>(null) }
    var abrindo by remember { mutableStateOf<String?>(null) }
    var processando by remember { mutableStateOf(false) }
    var aviso by remember { mutableStateOf<String?>(null) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { tirou ->
        val nome = arquivoDaCamera
        arquivoDaCamera = null
        if (nome == null) return@rememberLauncherForActivityResult
        if (!tirou) {
            Anexos.excluir(contexto, listOf(nome))
            return@rememberLauncherForActivityResult
        }
        processando = true
        escopo.launch {
            val ok = withContext(Dispatchers.IO) { Anexos.otimizar(contexto, nome) }
            processando = false
            if (ok) onAdicionar(listOf(nome)) else aviso = "Não foi possível ler a foto."
        }
    }
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(6)) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        processando = true
        escopo.launch {
            val nomes = withContext(Dispatchers.IO) { uris.mapNotNull { Anexos.importar(contexto, it) } }
            processando = false
            if (nomes.isNotEmpty()) onAdicionar(nomes)
            if (nomes.size < uris.size) aviso = "Algumas imagens não puderam ser lidas."
        }
    }

    fun tirarFoto() {
        val nome = Anexos.novoNome()
        arquivoDaCamera = nome
        try {
            camera.launch(Anexos.uri(contexto, Anexos.arquivo(contexto, nome)))
        } catch (e: ActivityNotFoundException) {
            arquivoDaCamera = null
            aviso = "Nenhum app de câmera encontrado."
        }
    }

    Column(Modifier.fillMaxWidth()) {
        Hud(if (anexos.isEmpty()) "Nota fiscal" else "Nota fiscal · ${anexos.size}")
        Spacer(Modifier.height(8.dp))
        if (anexos.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                anexos.forEach { nome ->
                    MiniaturaAnexo(nome, onAbrir = { abrindo = nome }, onRemover = { onRemover(nome) })
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BotaoSecundario("Tirar foto", onClick = ::tirarFoto, modifier = Modifier.weight(1f), habilitado = !processando)
            BotaoSecundario(
                "Galeria",
                onClick = {
                    galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.weight(1f),
                habilitado = !processando
            )
        }
        if (processando) Hud("Preparando a foto", Modifier.padding(top = 8.dp))
        aviso?.let { Text(it, style = t.bodySm, color = c.danger, modifier = Modifier.padding(top = 8.dp)) }
    }

    abrindo?.let { nome ->
        VisualizadorAnexo(nome, onFechar = { abrindo = null })
    }
}

/** Carrega a foto em segundo plano, numa resolução adequada ao tamanho exibido. */
@Composable
private fun rememberFoto(nome: String, ladoMaximo: Int): ImageBitmap? {
    val contexto = LocalContext.current
    val foto by produceState<ImageBitmap?>(null, nome, ladoMaximo) {
        value = withContext(Dispatchers.IO) { Anexos.carregar(contexto, nome, ladoMaximo)?.asImageBitmap() }
    }
    return foto
}

@Composable
fun MiniaturaAnexo(nome: String, onAbrir: () -> Unit, onRemover: (() -> Unit)? = null) {
    val c = Oficina.cores
    val foto = rememberFoto(nome, 320)
    Box(
        Modifier
            .size(88.dp)
            .background(c.bgSunken)
            .border(1.dp, c.line)
            .clickable(role = Role.Image, onClick = onAbrir)
    ) {
        if (foto != null) {
            Image(foto, contentDescription = "Foto da nota", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Hud("Foto", Modifier.align(Alignment.Center))
        }
        if (onRemover != null) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(28.dp)
                    .background(c.invertBg)
                    .clickable(role = Role.Button, onClick = onRemover),
                contentAlignment = Alignment.Center
            ) {
                Text("×", style = Oficina.tipos.body, color = c.invertFg)
            }
        }
    }
}

/** Foto em tela cheia; toque para fechar. */
@Composable
fun VisualizadorAnexo(nome: String, onFechar: () -> Unit) {
    val foto = rememberFoto(nome, 2000)
    Dialog(onDismissRequest = onFechar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(onClick = onFechar),
            contentAlignment = Alignment.Center
        ) {
            if (foto != null) {
                Image(foto, contentDescription = "Foto da nota", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            } else {
                Hud("Carregando", cor = Color.White)
            }
            Hud(
                "Toque para fechar",
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp),
                cor = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}
