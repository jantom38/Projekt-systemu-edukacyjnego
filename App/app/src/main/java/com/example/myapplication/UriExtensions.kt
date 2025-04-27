package com.example.myapplication

import android.content.Context
import android.net.Uri
import java.io.File

fun Uri.toFile(context: Context): File {
    val inputStream = context.contentResolver.openInputStream(this)
        ?: throw IllegalArgumentException("Nie można otworzyć strumienia dla URI")

    val file = File.createTempFile(
        "upload_${System.currentTimeMillis()}",
        ".temp",
        context.cacheDir
    )

    file.outputStream().use { output ->
        inputStream.use { input ->
            input.copyTo(output)
        }
    }
    return file
}