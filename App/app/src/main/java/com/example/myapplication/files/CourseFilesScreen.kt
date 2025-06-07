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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.net.URLConnection

data class CourseFile(
    val id: Long,
    val fileName: String,
    val fileUrl: String
)
class CourseFilesViewModel(context: Context, private val courseId: Long) : ViewModel() {
    private val _files = mutableStateOf<List<CourseFile>>(emptyList())
    val files: State<List<CourseFile>> = _files

    private val _quizzes = mutableStateOf<List<Quiz>>(emptyList())
    val quizzes: State<List<Quiz>> = _quizzes

    private val _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    private val apiService = RetrofitClient.getInstance(context)

    init {
        loadContent()
    }

    fun loadContent() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Load files
                _files.value = apiService.getCourseFiles(courseId)
                Log.d("CourseFiles", "Loaded files: ${_files.value}")

                // Load quizzes
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
///screen usera do wyswietlania plikow
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
                FileCard(file = file,context = context)
            }
        }
    }
}
@Composable
private fun QuizzesTab(quizzes: List<Quiz>, navController: NavHostController, courseId: Long) { // Dodajemy courseId
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
                QuizCard(quiz = quiz, navController = navController, courseId = courseId) // Przekazujemy courseId
            }
        }
    }
}

@Composable
fun QuizCard(quiz: Quiz, navController: NavHostController, courseId: Long) { // Dodajemy courseId
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                quiz.id?.let {
                    navController.navigate("solve_quiz/$it/$courseId") // Zaktualizowana trasa
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

//    @Composable
//     fun FilesTab(files: List<CourseFile>, context: Context) {
//        if (files.isEmpty()) {
//            Box(
//                modifier = Modifier.fillMaxSize(),
//                contentAlignment = Alignment.Center
//            ) {
//                Text("Brak plików dla tego kursu")
//            }
//        } else {
//            LazyColumn(
//                modifier = Modifier.fillMaxSize(),
//                contentPadding = PaddingValues(bottom = 16.dp)
//            ) {
//                items(files) { file ->
//                    FileCard(file = file, context = context)
//                }
//            }
//        }
//    }

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
            e.printStackTrace() // loguje do Logcat
            Toast.makeText(context, "Błąd pobierania: ${e.message ?: "nieznany"}", Toast.LENGTH_LONG).show()
            return
        }

    }

    val uri: Uri = FileProvider.getUriForFile(
        context,
        "com.example.myapplication.fileprovider", // authorities z AndroidManifest
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



fun getMimeType(fileName: String): String {
    return when (fileName.substringAfterLast('.', "").lowercase()) {
        "pdf"   -> "application/pdf"
        "doc"   -> "application/msword"
        "docx"  -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "xls"   -> "application/vnd.ms-excel"
        "xlsx"  -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "ppt"   -> "application/vnd.ms-powerpoint"
        "pptx"  -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        "jpg", "jpeg" -> "image/jpeg"
        "png"   -> "image/png"
        else     -> URLConnection.guessContentTypeFromName(fileName) ?: "*/*"
    }
}

// Helper: pobiera plik w tle i otwiera po pobraniu
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

        // Po pobraniu: otwórz plik na głównym wątku
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
                // Sprawdź czy jest aplikacja
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

@Composable
fun FileCard(file: CourseFile, context: Context) {
    val baseUrl = "http://10.0.2.2:8080"
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
            Text(text = fullUrl)
        }
    }
}



