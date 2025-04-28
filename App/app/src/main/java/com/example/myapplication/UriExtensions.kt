package com.example.myapplication

import android.content.Context
import android.net.Uri
import java.io.File
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap

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