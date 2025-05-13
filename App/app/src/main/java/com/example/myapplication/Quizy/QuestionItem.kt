// QuestionItem.kt
package com.example.myapplication.Quizy

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.QuizQuestion

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
}@Composable
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
                ) // ← poprawka: usunięto zbędny nawias
            }
        }
    }
}

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
    ) // ← poprawka: usunięto zbędny nawias
}
