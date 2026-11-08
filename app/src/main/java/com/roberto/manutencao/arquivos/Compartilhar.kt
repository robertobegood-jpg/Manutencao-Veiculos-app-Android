package com.roberto.manutencao.arquivos

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/** Abre a janela de compartilhar do Android (WhatsApp, Drive, e-mail...) com arquivos do app. */
object Compartilhar {

    fun pastaExportacoes(context: Context): File =
        File(context.cacheDir, "exportacoes").apply { mkdirs() }

    fun arquivos(context: Context, arquivos: List<File>, tipo: String, titulo: String) {
        if (arquivos.isEmpty()) return
        val uris = ArrayList<Uri>(arquivos.map {
            FileProvider.getUriForFile(context, "${context.packageName}.arquivos", it)
        })
        val envio = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris.first())
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        }
        envio.type = tipo
        envio.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        // ClipData garante a permissão de leitura em todos os apps de destino.
        val clip = ClipData.newRawUri(arquivos.first().name, uris.first())
        uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
        envio.clipData = clip
        context.startActivity(Intent.createChooser(envio, titulo).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
    }
}
