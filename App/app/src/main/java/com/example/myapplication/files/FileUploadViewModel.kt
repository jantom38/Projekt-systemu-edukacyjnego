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

/**
 * @file FileUploadViewModel.kt
 * Ten plik zawiera ViewModel [FileUploadViewModel] oraz klasy stanu [FileUploadState]
 * do obsługi procesu przesyłania plików na serwer.
 */

/**
 * ViewModel odpowiedzialny za przesyłanie plików.
 * Zarządza stanem procesu uploadu (bezczynny, ładowanie, sukces, błąd)
 * i komunikuje się z API w celu przesłania pliku.
 *
 * @param context Kontekst aplikacji, wymagany do operacji na plikach i inicjalizacji RetrofitClient.
 */
class FileUploadViewModel(context: Context) : ViewModel() {
    /** Instancja RetrofitClient do komunikacji z API.*/
    private val apiService = RetrofitClient.getInstance(context)

    /** Prywatny MutableStateFlow przechowujący aktualny stan uploadu pliku.*/
    private val _uploadState = MutableStateFlow<FileUploadState>(FileUploadState.Idle)
    /** Publiczny StateFlow do obserwowania stanu uploadu pliku przez UI.*/
    val uploadState: StateFlow<FileUploadState> = _uploadState

    /**
     * Rozpoczyna proces przesyłania pliku na serwer.
     * Ustawia stan na `Loading`, wykonuje operację uploadu, a następnie aktualizuje stan
     * na `Success` lub `Error` w zależności od wyniku.
     *
     * @param courseId Identyfikator kursu, do którego plik ma zostać przesłany.
     * @param uri URI pliku do przesłania.
     * @param context Kontekst aplikacji, używany do dostępu do zawartości URI i tworzenia plików tymczasowych.
     */
    fun uploadFile(courseId: Long, uri: Uri, context: Context) {
        viewModelScope.launch {
            _uploadState.value = FileUploadState.Loading
            try {
                val file = uri.toFile(context)

                val fileSizeMB = file.length() / (1024 * 1024)
                if (fileSizeMB > 10) {
                    throw IllegalArgumentException("Plik jest zbyt duży (maks. 10MB)")
                }

                val originalName = uri.getFileName(context)
                val requestFile = file.asRequestBody(
                    context.contentResolver.getType(uri)?.toMediaTypeOrNull()
                )
                val filePart = MultipartBody.Part.createFormData(
                    name = "file",
                    filename = originalName,
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
                // file?.delete()
            }
        }
    }
}

/**
 * Klasa sealowana reprezentująca różne stany operacji przesyłania pliku.
 */
sealed class FileUploadState {
    /** Stan początkowy, gdy nic się nie dzieje.*/
    object Idle : FileUploadState()
    /** Stan, gdy plik jest w trakcie przesyłania.*/
    object Loading : FileUploadState()
    /** Stan, gdy plik został pomyślnie przesłany.*/
    data class Success(val message: String) : FileUploadState()
    /** Stan, gdy podczas przesyłania wystąpił błąd.*/
    data class Error(val error: String) : FileUploadState()
}