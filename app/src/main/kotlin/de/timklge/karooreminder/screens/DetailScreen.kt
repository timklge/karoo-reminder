package de.timklge.karooreminder.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.maxkeppeker.sheets.core.models.base.UseCaseState
import com.maxkeppeler.sheets.color.ColorDialog
import com.maxkeppeler.sheets.color.models.ColorConfig
import com.maxkeppeler.sheets.color.models.ColorSelection
import com.maxkeppeler.sheets.color.models.ColorSelectionMode
import com.maxkeppeler.sheets.color.models.MultipleColors
import com.maxkeppeler.sheets.color.models.SingleColor
import de.timklge.karooreminder.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(isCreating: Boolean, reminder: Reminder, onSubmit: (updatedReminder: Reminder?) -> Unit, onCancel: () -> Unit){
    var title by remember { mutableStateOf(reminder.name) }
    var text by remember { mutableStateOf(reminder.text) }
    var selectedColor by remember { mutableIntStateOf(reminder.foregroundColor) }
    val colorDialogState by remember { mutableStateOf(UseCaseState()) }
    var duration by remember { mutableStateOf(reminder.interval.toString()) }
    var isActive by remember { mutableStateOf(reminder.isActive) }
    var deleteDialogVisible by remember { mutableStateOf(false) }

    fun getUpdatedReminder(): Reminder = Reminder(reminder.id, title, duration.toIntOrNull() ?: 0, text, selectedColor, isActive)

    Column(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)) {
        TopAppBar(title = { Text(if(isCreating) "Create Reminder" else "Edit Reminder") })
        Column(modifier = Modifier
            .padding(5.dp)
            .verticalScroll(rememberScrollState())
            .fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Text") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            OutlinedTextField(value = duration, modifier = Modifier.fillMaxWidth(),
                onValueChange = { duration = it },
                label = { Text("Interval") },
                suffix = { Text("min") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            ColorDialog(
                state = colorDialogState,
                selection = ColorSelection(
                    selectedColor = SingleColor(selectedColor),
                    onSelectColor = { c -> selectedColor = c },
                ),
                config = ColorConfig(
                    displayMode = ColorSelectionMode.TEMPLATE,
                    allowCustomColorAlphaValues = false,
                    templateColors = MultipleColors.ColorsInt(
                        ContextCompat.getColor(LocalContext.current, R.color.bRed),
                        ContextCompat.getColor(LocalContext.current, R.color.bPurple),
                        ContextCompat.getColor(LocalContext.current, R.color.bYellow),
                        ContextCompat.getColor(LocalContext.current, R.color.bGreen),
                        ContextCompat.getColor(LocalContext.current, R.color.bBlue),
                        ContextCompat.getColor(LocalContext.current, R.color.bCyan),
                    ),
                )
            )

            FilledTonalButton(modifier = Modifier
                .fillMaxWidth(),
                onClick = {
                    colorDialogState.show()
            }) {
                Surface(shape = CircleShape, color = Color(selectedColor),
                    modifier = Modifier
                        .height(40.dp)
                        .shadow(5.dp, CircleShape)
                        .width(40.dp), content = {})

                Spacer(modifier = Modifier.width(20.dp))

                Text("Change Color")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = isActive, onCheckedChange = { isActive = it})
                Spacer(modifier = Modifier.width(10.dp))
                Text("Is Active")
            }

            FilledTonalButton(modifier = Modifier
                .fillMaxWidth()
                .height(50.dp), onClick = {
                onSubmit(getUpdatedReminder())
            }) {
                Icon(Icons.Default.Done, contentDescription = "Save Reminder")
                Text("Save")
            }

            FilledTonalButton(modifier = Modifier
                .fillMaxWidth()
                .height(50.dp), onClick = {
                onCancel()
            }) {
                Icon(Icons.Default.Close, contentDescription = "Cancel Editing")
                Text("Cancel")
            }

            if (!isCreating) {
                FilledTonalButton(modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp), onClick = {
                    deleteDialogVisible = true
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Reminder")
                    Text("Delete")
                }
            }

            if (deleteDialogVisible){
                AlertDialog(onDismissRequest = { deleteDialogVisible = false },
                    confirmButton = { Button(onClick = {
                        deleteDialogVisible = false
                        onSubmit(null)
                    }) { Text("OK") }
                    },
                    dismissButton = { Button(onClick = {
                        deleteDialogVisible = false
                    }) { Text("Cancel") }
                },
                title = { Text("Delete reminder") }, text = { Text("Really delete this reminder?") })
            }
        }
    }
}