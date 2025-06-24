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
 *  This file contains composable functions for displaying different types of quiz questions
 * and handling user interactions with their options.
 */

/**
 *  Composable function to display a single quiz question.
 *
 * This function renders a quiz question based on its type (multiple choice, true/false, or open-ended)
 * and provides UI elements for users to select or input their answers.
 *
 * @param question The [QuizQuestion] object to display.
 * @param index The index of the question in the quiz.
 * @param selectedAnswers A list of strings representing the currently selected answers for the question.
 * @param onAnswerSelected A callback function invoked when the user selects or changes an answer.
 * It receives a list of strings representing the new selection.
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
 *  Composable function to display multiple-choice options.
 *
 * It provides checkboxes for users to select one or more options.
 *
 * @param options A map where keys are option identifiers (e.g., "A", "B") and values are option texts.
 * @param selected A list of currently selected option identifiers.
 * @param onSelectionChange A callback invoked when the selection changes, providing the new list of selected option identifiers.
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
 *  Composable function to display true/false options.
 *
 * It provides radio buttons for users to select either "True" or "False".
 *
 * @param selected The currently selected option ("True" or "False").
 * @param onSelectionChange A callback invoked when the selection changes, providing the new selected option.
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
 *  Composable function to display an open-ended answer field.
 *
 * @param answer The current text in the answer field.
 * @param onAnswerChange A callback invoked when the text in the answer field changes.
 * It provides the new text as a string.
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