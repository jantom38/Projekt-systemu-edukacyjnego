package com.example.myapplication.files

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.RetrofitClient
import com.example.myapplication.getFileName
import com.example.myapplication.toFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.IOException

class FileUploadViewModel(context: Context) : ViewModel() {
    private val apiService = RetrofitClient.getInstance(context)
    private val _uploadState = MutableStateFlow<FileUploadState>(FileUploadState.Idle)
    val uploadState: StateFlow<FileUploadState> = _uploadState

    fun uploadFile(courseId: Long, uri: Uri, context: Context) {
        viewModelScope.launch {
            _uploadState.value = FileUploadState.Loading
            try {
                // Konwersja URI na plik
                val file = uri.toFile(context)

                // Walidacja rozmiaru pliku (opcjonalnie)
                val fileSizeMB = file.length() / (1024 * 1024)
                if (fileSizeMB > 10) { // 10MB limit
                    throw IllegalArgumentException("Plik jest zbyt duży (max 10MB)")
                }


                val originalName = uri.getFileName(context)
                val requestFile = file.asRequestBody(
                    context.contentResolver.getType(uri)?.toMediaTypeOrNull()
                )
                val filePart = MultipartBody.Part.createFormData(
                    name = "file",
                    filename = originalName,    // <<< używamy prawdziwej nazwy
                    body = requestFile
                )

                val response = apiService.uploadFile(courseId, filePart)

                if (!response.isSuccessful) {
                    throw IOException("Błąd serwera: ${response.code()}")
                }

                _uploadState.value = FileUploadState.Success("Plik przesłany pomyślnie")
            } catch (e: Exception) {
                _uploadState.value = FileUploadState.Error(
                    e.message ?: "Nieznany błąd podczas przesyłania pliku"
                )
                Log.e("FileUpload", "Błąd uploadu", e)
            } finally {
                // Opcjonalne: usunięcie pliku tymczasowego
                // file?.delete() // Odkomentuj jeśli chcesz czyścić pliki tymczasowe
            }
        }
    }
}

sealed class FileUploadState {
    object Idle : FileUploadState()
    object Loading : FileUploadState()
    data class Success(val message: String) : FileUploadState()
    data class Error(val error: String) : FileUploadState()
}
