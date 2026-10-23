package com.example.personas.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.personas.BuildConfig
import com.example.personas.domain.CapturaFoto
import com.example.personas.domain.FotosRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ArchivoFotosRepository(context: Context) : FotosRepository {
    private val context = context.applicationContext

    override fun prepararCaptura(): CapturaFoto {
        val file = FotoImporter.nuevaCaptura(context)
        return try {
            CapturaFoto(file.absolutePath,
                FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.files", file).toString())
        } catch (e: Exception) {
            FotoImporter.borrar(context, file.absolutePath)
            throw e
        }
    }

    override fun origenArchivo(ruta: String): String = Uri.fromFile(File(ruta)).toString()
    override suspend fun importar(uri: String): String = withContext(Dispatchers.IO) {
        FotoImporter.importar(context, Uri.parse(uri))
    }
    override fun borrar(ruta: String) = FotoImporter.borrar(context, ruta)
}
