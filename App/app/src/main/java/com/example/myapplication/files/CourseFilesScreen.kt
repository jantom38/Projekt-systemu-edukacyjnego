package com.example.myapplication.files

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.myapplication.Quiz
import com.example.myapplication.RetrofitClient
import com.example.myapplication.ServerConfig.getBaseUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.net.URLConnection

/**
 * Klasa danych reprezentująca plik kursu.
 *
 * @param id Unikalny identyfikator pliku.
 * @param fileName Nazwa pliku.
 * @param fileUrl Adres URL pliku.
 */
data class CourseFile(
    val id: Long,
    val fileName: String,
    val fileUrl: String
)

/**
 * ViewModel zarządzający danymi plików i quizów dla konkretnego kursu.
 *
 * @param context Kontekst aplikacji.
 * @param courseId Identyfikator kursu.
 */
class CourseFilesViewModel(context: Context, private val courseId: Long) : ViewModel() {
    private val _files = mutableStateOf<List<CourseFile>>(emptyList())
    /** Lista plików kursu. */
    val files: State<List<CourseFile>> = _files

    private val _quizzes = mutableStateOf<List<Quiz>>(emptyList())
    /** Lista quizów kursu. */
    val quizzes: State<List<Quiz>> = _quizzes

    private val _isLoading = mutableStateOf(true)
    /** Flaga wskazująca, czy trwa ładowanie danych. */
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    /** Komunikat błędu, jeśli wystąpił problem podczas ładowania danych. */
    val error: State<String?> = _error

    private val apiService = RetrofitClient.getInstance(context)

    init {
        loadContent()
    }

    /**
     * Ładuje pliki i quizy dla kursu z serwera.
     */
    fun loadContent() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Ładowanie plików
                _files.value = apiService.getCourseFiles(courseId)
                Log.d("CourseFiles", "Loaded files: ${_files.value}")

                // Ładowanie quizów
                val quizResponse = apiService.getCourseQuizzes(courseId)
                if (quizResponse.success) {
                    _quizzes.value = quizResponse.quizzes
                    Log.d("CourseFiles", "Loaded quizzes: ${_quizzes.value}")
                } else {
                    _error.value = "Błąd ładowania quizów"
                }
            } catch (e: HttpException) {
                _error.value = when (e.code()) {
                    401 -> "Brak autoryzacji"
                    404 -> "Kurs nie znaleziony"
                    else -> "Błąd serwera: ${e.code()}"
                }
                Log.e("CourseFiles", "HTTP error", e)
            } catch (e: Exception) {
                _error.value = "Błąd połączenia: ${e.message ?: "Nieznany błąd"}"
                Log.e("CourseFiles", "Network error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}

/**
 * Ekran wyświetlający pliki i quizy dla wybranego kursu.
 *
 * @param navController Kontroler nawigacji do przechodzenia między ekranami.
 * @param courseId Identyfikator kursu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseFilesScreen(navController: NavHostController, courseId: Long) {
    val context = LocalContext.current
    val viewModel: CourseFilesViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CourseFilesViewModel(context, courseId) as T
        }
    })

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Pliki", "Quizy")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kurs") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("menu") }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Wstecz")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            when {
                viewModel.isLoading.value -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                viewModel.error.value != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = viewModel.error.value!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                        Button(
                            onClick = { viewModel.loadContent() },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Spróbuj ponownie")
                        }
                    }
                }

                else -> {
                    when (selectedTabIndex) {
                        0 -> FilesTab(viewModel.files.value, context)
                        1 -> QuizzesTab(viewModel.quizzes.value, navController, courseId)
                    }
                }
            }
        }
    }
}

/**
 * Zakładka wyświetlająca listę plików kursu.
 *
 * @param files Lista plików kursu.
 * @param context Kontekst aplikacji.
 */
@Composable
private fun FilesTab(files: List<CourseFile>, context: Context) {
    if (files.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Brak plików dla tego kursu")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(files) { file ->
                FileCard(file = file, context = context)
            }
        }
    }
}

/**
 * Zakładka wyświetlająca listę quizów kursu.
 *
 * @param quizzes Lista quizów kursu.
 * @param navController Kontroler nawigacji.
 * @param courseId Identyfikator kursu.
 */
@Composable
private fun QuizzesTab(quizzes: List<Quiz>, navController: NavHostController, courseId: Long) {
    if (quizzes.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Brak quizów dla tego kursu")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(quizzes) { quiz ->
                QuizCard(quiz = quiz, navController = navController, courseId = courseId)
            }
        }
    }
}

/**
 * Komponent wyświetlający kartę quizu.
 *
 * @param quiz Obiekt quizu.
 * @param navController Kontroler nawigacji.
 * @param courseId Identyfikator kursu.
 */
@Composable
fun QuizCard(quiz: Quiz, navController: NavHostController, courseId: Long) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                quiz.id?.let {
                    navController.navigate("solve_quiz/$it/$courseId")
                }
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = quiz.title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            quiz.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * Otwiera plik przy użyciu FileProvider.
 *
 * @param context Kontekst aplikacji.
 * @param fileUrl Adres URL pliku do pobrania.
 * @param fileName Nazwa pliku.
 */
fun openFileWithProvider(context: Context, fileUrl: String, fileName: String) {
    val cacheFile = File(context.cacheDir, fileName)
    if (!cacheFile.exists()) {
        try {
            val url = URL(fileUrl)
            (url.openConnection() as HttpURLConnection).apply {
                connect()
                inputStream.use { input ->
                    cacheFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                disconnect()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Błąd pobierania: ${e.message ?: "nieznany"}", Toast.LENGTH_LONG).show()
            return
        }
    }

    val uri: Uri = FileProvider.getUriForFile(
        context,
        "com.example.myapplication.fileprovider",
        cacheFile
    )
    val mimeType = URLConnection.guessContentTypeFromName(fileName) ?: "*/*"
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Brak aplikacji do otwarcia: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Zwraca typ MIME dla podanej nazwy pliku.
 *
 * @param fileName Nazwa pliku.
 * @return Typ MIME dla pliku lub dowolny jeśli nie można określić.
*/
fun getMimeType(fileName: String): String {
    return when (fileName.substringAfterLast('.', "").lowercase()) {
        "pdf" -> "application/pdf"
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "xls" -> "application/vnd.ms-excel"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "ppt" -> "application/vnd.ms-powerpoint"
        "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        else -> URLConnection.guessContentTypeFromName(fileName) ?: "*/*"
    }
}

/**
 * Pobiera plik w tle i otwiera go po zakończeniu pobierania.
 *
 * @param context Kontekst aplikacji.
 * @param fileUrl Adres URL pliku do pobrania.
 * @param fileName Nazwa pliku.
 */
fun fetchAndOpenFile(context: Context, fileUrl: String, fileName: String) {
    CoroutineScope(Dispatchers.IO).launch {
        val cacheFile = File(context.cacheDir, fileName)
        try {
            if (!cacheFile.exists()) {
                Log.d("FileDebug", "Pobieram plik z: $fileUrl")
                val url = URL(fileUrl)
                (url.openConnection() as HttpURLConnection).apply {
                    connect()
                    inputStream.use { input ->
                        cacheFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    disconnect()
                }
            }
        } catch (e: Exception) {
            Log.e("FileDebug", "Błąd pobierania", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Błąd pobierania: ${e.message ?: "nieznany"}", Toast.LENGTH_LONG).show()
            }
            return@launch
        }

        withContext(Dispatchers.Main) {
            try {
                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "com.example.myapplication.fileprovider",
                    cacheFile
                )
                val mimeType = getMimeType(fileName)
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val chooser = Intent.createChooser(viewIntent, "Wybierz aplikację do otwarcia")
                if (viewIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(chooser)
                } else {
                    Toast.makeText(context, "Brak aplikacji do otwarcia pliku", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("FileDebug", "Błąd otwierania", e)
                Toast.makeText(context, "Nie można otworzyć: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

/**
 * Komponent wyświetlający kartę pliku.
 *
 * @param file Obiekt pliku kursu.
 * @param context Kontekst aplikacji.
 */
@Composable
fun FileCard(file: CourseFile, context: Context) {
    val baseUrl = getBaseUrl(context).removeSuffix("/")
    val fullUrl = "$baseUrl${file.fileUrl}"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                fetchAndOpenFile(context, fullUrl, file.fileName)
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = file.fileName)
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}