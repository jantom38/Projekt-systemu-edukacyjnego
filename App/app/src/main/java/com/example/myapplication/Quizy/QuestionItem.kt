// QuestionItem.kt
package com.example.myapplication.Quizy

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.QuizQuestion

/**
 * @file QuestionItem.kt
 * Ten plik zawiera funkcje kompozycyjne do wyświetlania różnych typów pytań quizowych
 * oraz obsługę interakcji użytkownika z ich opcjami.
 */

/**
 * Funkcja kompozycyjna do wyświetlania pojedynczego pytania quizowego.
 *
 * Ta funkcja renderuje pytanie quizowe na podstawie jego typu (wielokrotnego wyboru, prawda/fałsz lub otwarte)
 * i udostępnia elementy interfejsu użytkownika, aby użytkownicy mogli wybrać lub wprowadzić swoje odpowiedzi.
 *
 * @param question Obiekt [QuizQuestion] do wyświetlenia.
 * @param index Indeks pytania w quizie.
 * @param selectedAnswers Lista stringów reprezentujących aktualnie wybrane odpowiedzi dla pytania.
 * @param onAnswerSelected Funkcja zwrotna wywoływana, gdy użytkownik wybierze lub zmieni odpowiedź.
 * Otrzymuje listę stringów reprezentujących nowy wybór.
 */
@Composable
fun QuestionItem(
    question: QuizQuestion,
    index: Int,
    selectedAnswers: List<String>,
    onAnswerSelected: (List<String>) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Pytanie #$index",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = question.questionText,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            when (question.questionType) {
                "multiple_choice" -> MultipleChoiceOptions(
                    options = question.options ?: emptyMap(),
                    selected = selectedAnswers,
                    onSelectionChange = onAnswerSelected
                )
                "true_false" -> TrueFalseOptions(
                    selected = selectedAnswers.firstOrNull() ?: "",
                    onSelectionChange = { onAnswerSelected(listOf(it)) }
                )
                else -> OpenAnswerField(
                    answer = selectedAnswers.firstOrNull() ?: "",
                    onAnswerChange = { onAnswerSelected(listOf(it)) }
                )
            }
        }
    }
}

/**
 * Funkcja kompozycyjna do wyświetlania opcji wielokrotnego wyboru.
 *
 * Udostępnia pola wyboru, aby użytkownicy mogli wybrać jedną lub więcej opcji.
 *
 * @param options Mapa, w której klucze to identyfikatory opcji (np. "A", "B"), a wartości to teksty opcji.
 * @param selected Lista aktualnie wybranych identyfikatorów opcji.
 * @param onSelectionChange Funkcja zwrotna wywoływana, gdy zmienia się wybór, dostarczająca nową listę wybranych identyfikatorów opcji.
 */
@Composable
private fun MultipleChoiceOptions(
    options: Map<String, String>,
    selected: List<String>,
    onSelectionChange: (List<String>) -> Unit
) {
    Column {
        options.forEach { (key, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = selected.contains(key),
                    onCheckedChange = { isChecked ->
                        val newSelection = selected.toMutableList().apply {
                            if (isChecked) add(key) else remove(key)
                        }
                        onSelectionChange(newSelection)
                    }
                )
                Text(
                    text = value,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

/**
 * Funkcja kompozycyjna do wyświetlania opcji prawda/fałsz.
 *
 * Udostępnia przyciski radiowe, aby użytkownicy mogli wybrać "Prawda" lub "Fałsz".
 *
 * @param selected Aktualnie wybrana opcja ("True" lub "False").
 * @param onSelectionChange Funkcja zwrotna wywoływana, gdy zmienia się wybór, dostarczająca nowo wybraną opcję.
 */
@Composable
private fun TrueFalseOptions(
    selected: String,
    onSelectionChange: (String) -> Unit
) {
    Column {
        listOf("True" to "Prawda", "False" to "Fałsz").forEach { (key, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selected == key,
                    onClick = { onSelectionChange(key) }
                )
                Text(
                    text = value,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

/**
 * Funkcja kompozycyjna do wyświetlania pola odpowiedzi otwartej.
 *
 * @param answer Bieżący tekst w polu odpowiedzi.
 * @param onAnswerChange Funkcja zwrotna wywoływana, gdy zmienia się tekst w polu odpowiedzi.
 * Dostarcza nowy tekst jako string.
 */
@Composable
private fun OpenAnswerField(
    answer: String,
    onAnswerChange: (String) -> Unit
) {
    OutlinedTextField(
        value = answer,
        onValueChange = onAnswerChange,
        label = { Text("Twoja odpowiedź") },
        modifier = Modifier.fillMaxWidth()
    )
}