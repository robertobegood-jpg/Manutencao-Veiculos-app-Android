package com.roberto.manutencao.arquivos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/**
 * Fotos de notas fiscais: ficam em files/anexos/, reduzidas para no máximo
 * 1600 px no lado maior e salvas em JPEG, para não encher o celular.
 */
object Anexos {
    private const val PASTA = "anexos"
    private const val LADO_MAXIMO = 1600
    private const val QUALIDADE = 82

    fun pasta(context: Context): File = File(context.filesDir, PASTA).apply { mkdirs() }

    fun arquivo(context: Context, nome: String): File = File(pasta(context), nome)

    fun novoNome(): String = "nota_${UUID.randomUUID()}.jpg"

    /** Endereço que outros apps (câmera, compartilhar) podem usar para este arquivo. */
    fun uri(context: Context, arquivo: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.arquivos", arquivo)

    /** Copia uma imagem escolhida na galeria para a pasta de anexos. Devolve o nome do arquivo. */
    fun importar(context: Context, origem: Uri): String? {
        val nome = novoNome()
        val destino = arquivo(context, nome)
        return try {
            val opcoes = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(origem)?.use { BitmapFactory.decodeStream(it, null, opcoes) }
            val amostra = amostragem(opcoes.outWidth, opcoes.outHeight, LADO_MAXIMO)
            val bitmap = context.contentResolver.openInputStream(origem)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = amostra })
            } ?: return null
            val rotacao = context.contentResolver.openInputStream(origem)?.use { rotacaoExif(ExifInterface(it)) } ?: 0
            salvar(reduzir(girar(bitmap, rotacao)), destino)
            nome
        } catch (e: Exception) {
            destino.delete()
            null
        }
    }

    /** Reduz e endireita uma foto tirada pela câmera, no próprio arquivo. */
    fun otimizar(context: Context, nome: String): Boolean {
        val arquivo = arquivo(context, nome)
        return try {
            val opcoes = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(arquivo.path, opcoes)
            val amostra = amostragem(opcoes.outWidth, opcoes.outHeight, LADO_MAXIMO)
            val bitmap = BitmapFactory.decodeFile(arquivo.path, BitmapFactory.Options().apply { inSampleSize = amostra })
                ?: return false
            val rotacao = rotacaoExif(ExifInterface(arquivo.path))
            salvar(reduzir(girar(bitmap, rotacao)), arquivo)
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Lê uma versão pequena da foto para mostrar na tela. */
    fun carregar(context: Context, nome: String, ladoMaximo: Int): Bitmap? {
        val arquivo = arquivo(context, nome)
        if (!arquivo.exists()) return null
        val opcoes = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(arquivo.path, opcoes)
        return BitmapFactory.decodeFile(
            arquivo.path,
            BitmapFactory.Options().apply { inSampleSize = amostragem(opcoes.outWidth, opcoes.outHeight, ladoMaximo) }
        )
    }

    fun excluir(context: Context, nomes: Collection<String>) {
        nomes.forEach { arquivo(context, it).delete() }
    }

    /** Apaga arquivos da pasta que não estão mais ligados a nenhum registro. */
    fun limparOrfaos(context: Context, emUso: Collection<String>) {
        val manter = emUso.toSet()
        val limite = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        pasta(context).listFiles()?.forEach { f ->
            // Arquivos muito recentes podem ser de um registro que ainda está sendo preenchido.
            if (f.name !in manter && f.lastModified() < limite) f.delete()
        }
    }

    private fun amostragem(largura: Int, altura: Int, ladoMaximo: Int): Int {
        var amostra = 1
        while (largura / (amostra * 2) >= ladoMaximo || altura / (amostra * 2) >= ladoMaximo) amostra *= 2
        return amostra
    }

    private fun rotacaoExif(exif: ExifInterface): Int =
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

    private fun girar(bitmap: Bitmap, graus: Int): Bitmap {
        if (graus == 0) return bitmap
        val matriz = Matrix().apply { postRotate(graus.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matriz, true)
    }

    private fun reduzir(bitmap: Bitmap): Bitmap {
        val maior = maxOf(bitmap.width, bitmap.height)
        if (maior <= LADO_MAXIMO) return bitmap
        val escala = LADO_MAXIMO.toFloat() / maior
        return Bitmap.createScaledBitmap(bitmap, (bitmap.width * escala).toInt(), (bitmap.height * escala).toInt(), true)
    }

    private fun salvar(bitmap: Bitmap, destino: File) {
        destino.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, QUALIDADE, it) }
    }
}
