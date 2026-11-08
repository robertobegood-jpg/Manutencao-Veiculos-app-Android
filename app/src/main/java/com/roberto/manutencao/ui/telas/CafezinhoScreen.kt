package com.roberto.manutencao.ui.telas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.roberto.manutencao.Cafezinho
import com.roberto.manutencao.data.Pix
import com.roberto.manutencao.ui.componentes.BotaoPrimario
import com.roberto.manutencao.ui.componentes.BotaoSecundario
import com.roberto.manutencao.ui.componentes.CabecalhoInterno
import com.roberto.manutencao.ui.componentes.Hud
import com.roberto.manutencao.ui.componentes.Segmentado
import com.roberto.manutencao.ui.componentes.marcasDeCanto
import com.roberto.manutencao.ui.formatarDinheiro
import com.roberto.manutencao.ui.theme.Oficina
import kotlinx.coroutines.delay

/** Valores sugeridos. null = quem paga escolhe no app do banco. */
private enum class Oferta(val rotulo: String, val centavos: Long?) {
    CAFE("Café", 500),
    CAFE_PAO("Café e pão", 1000),
    ALMOCO("Almoço", 2500),
    LIVRE("Outro", null)
}

private fun qrCode(texto: String): BitMatrix =
    QRCodeWriter().encode(
        texto,
        BarcodeFormat.QR_CODE,
        0,
        0,
        mapOf(EncodeHintType.MARGIN to 0, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M)
    )

@Composable
fun CafezinhoScreen(onVoltar: () -> Unit) {
    val c = Oficina.cores
    val t = Oficina.tipos
    val area = LocalClipboardManager.current
    var oferta by rememberSaveable { mutableStateOf(Oferta.CAFE) }
    var copiado by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(copiado) {
        if (copiado != null) {
            delay(2500)
            copiado = null
        }
    }

    Column(Modifier.fillMaxSize()) {
        CabecalhoInterno(eyebrow = "Apoie o projeto", titulo = "Um *cafezinho* para o dev.", onVoltar = onVoltar)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "O app é gratuito e sem anúncios. Se ele ajuda você a cuidar do seu veículo, um Pix de qualquer valor ajuda a manter o projeto.",
                style = t.bodyLg,
                color = c.muted,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))

            if (!Cafezinho.configurado) {
                Text(
                    "A chave Pix ainda não foi configurada. Edite o arquivo Cafezinho.kt.",
                    style = t.body,
                    color = c.ambarInk,
                    modifier = Modifier.fillMaxWidth()
                )
                return@Column
            }

            Segmentado(
                opcoes = Oferta.entries.toList(),
                selecionada = oferta,
                rotulo = { it.rotulo },
                onSelecionar = { oferta = it }
            )
            Spacer(Modifier.height(24.dp))

            val payload = remember(oferta) {
                Pix.payload(Cafezinho.CHAVE_PIX, Cafezinho.NOME, Cafezinho.CIDADE, oferta.centavos, "Cafezinho")
            }
            val matriz = remember(payload) { runCatching { qrCode(payload) }.getOrNull() }

            // QR sempre em fundo claro, para qualquer câmera ler bem, emoldurado com marcas de canto.
            Box(
                Modifier
                    .widthIn(max = 280.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .border(1.dp, c.line)
                    .marcasDeCanto(c.lineStrong)
                    .background(Color.White)
                    .padding(20.dp)
            ) {
                if (matriz != null) {
                    Canvas(Modifier.fillMaxSize()) {
                        val n = matriz.width
                        val modulo = size.minDimension / n
                        for (y in 0 until n) for (x in 0 until n) {
                            if (matriz.get(x, y)) {
                                drawRect(
                                    Color(0xFF101014),
                                    topLeft = Offset(x * modulo, y * modulo),
                                    size = Size(modulo + 0.5f, modulo + 0.5f)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Hud(
                listOfNotNull(
                    oferta.centavos?.let { formatarDinheiro(it) } ?: "Valor livre",
                    Cafezinho.NOME
                ).joinToString(" · ")
            )

            Spacer(Modifier.height(24.dp))
            BotaoPrimario(
                "Copiar Pix copia e cola",
                onClick = {
                    area.setText(AnnotatedString(payload))
                    copiado = "Código copiado. Cole no app do seu banco, na opção Pix copia e cola."
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            BotaoSecundario(
                "Copiar chave Pix",
                onClick = {
                    area.setText(AnnotatedString(Cafezinho.CHAVE_PIX))
                    copiado = "Chave copiada."
                },
                modifier = Modifier.fillMaxWidth()
            )
            copiado?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, style = t.bodySm, color = c.verdeInk, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Aponte a câmera do app do banco para o código ou use o copia e cola. Obrigado pelo café.",
                style = t.bodySm,
                color = c.muted,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
