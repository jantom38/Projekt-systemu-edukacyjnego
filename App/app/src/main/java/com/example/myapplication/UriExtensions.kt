package com.example.myapplication

import android.content.Context
import android.net.Uri
import java.io.File
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import okhttp3.ResponseBody
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

fun Uri.toFile(context: Context): File {
    val contentResolver = context.contentResolver
    val mimeType = contentResolver.getType(this) ?: ""
    val fileName = this.getFileName(context)
    val extension = mimeType.extensionFromMimeType()
        .takeIf { fileName.endsWith(it, ignoreCase = true) }
        ?: fileName.substringAfterLast('.', "" ).let { if (it.isNotBlank()) ".$it" else "" }
    val file = File.createTempFile(
        "upload_${System.currentTimeMillis()}",
        extension,
        context.cacheDir
    )
    contentResolver.openInputStream(this)!!.use { input ->
        file.outputStream().use { output ->
            input.copyTo(output)
        }
    }
    return file
}
fun Uri.getFileName(context: Context): String {
    var name = ""
    context.contentResolver.query(this, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && nameIndex != -1) {
            name = cursor.getString(nameIndex)
        }
    }
    return name
}
fun String.extensionFromMimeType(): String {
    val ext = MimeTypeMap.getSingleton()
        .getExtensionFromMimeType(this)
    return if (ext != null) ".$ext" else ""
}

suspend fun savePdfToDownloads(context: Context, body: ResponseBody, fileName: String): Pair<Boolean, String?> {
    return withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                } else {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

                    if (!downloadsDir.exists()) {
                        downloadsDir.mkdirs()
                    }
                    val file = File(downloadsDir, fileName)
                    put(MediaStore.MediaColumns.DATA, file.absolutePath)
                }
            }

            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                ?: return@withContext Pair(false, "Nie udało się utworzyć wpisu w MediaStore.")

            resolver.openOutputStream(uri)?.use { outputStream ->
                body.byteStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return@withContext Pair(false, "Nie udało się otworzyć strumienia do zapisu.")

            Pair(true, null)

        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, e.message ?: "Wystąpił nieznany błąd zapisu.")
        }
    }
}