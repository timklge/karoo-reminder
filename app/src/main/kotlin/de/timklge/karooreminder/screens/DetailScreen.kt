package de.timklge.karooreminder.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxkeppeker.sheets.core.models.base.UseCaseState
import com.maxkeppeler.sheets.color.ColorDialog
import com.maxkeppeler.sheets.color.models.ColorConfig
import com.maxkeppeler.sheets.color.models.ColorSelection
import com.maxkeppeler.sheets.color.models.ColorSelectionMode
import com.maxkeppeler.sheets.color.models.MultipleColors
import com.maxkeppeler.sheets.color.models.SingleColor
import de.timklge.karooreminder.Dropdown
import de.timklge.karooreminder.DropdownOption
import de.timklge.karooreminder.R
import de.timklge.karooreminder.ReminderTrigger
import de.timklge.karooreminder.streamUserProfile
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.PlayBeepPattern
import io.hammerhead.karooext.models.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(isCreating: Boolean, reminder: Reminder, onSubmit: (updatedReminder: Reminder?) -> Unit, onCancel: () -> Unit){
    val ctx = LocalContext.current
    val karooSystem = remember { KarooSystemService(ctx) }
    LaunchedEffect(Unit) {
        karooSystem.connect{}
    }
    var title by remember { mutableStateOf(reminder.name) }
    var text by remember { mutableStateOf(reminder.text) }
    var selectedColor by remember { mutableStateOf(reminder.displayForegroundColor) }
    val colorDialogState by remember { mutableStateOf(UseCaseState()) }
    var duration by remember { mutableStateOf(reminder.interval.toString()) }
    var isActive by remember { mutableStateOf(reminder.isActive) }
    var autoDismiss by remember { mutableStateOf(reminder.isAutoDismiss) }
    var deleteDialogVisible by remember { mutableStateOf(false) }
    var toneDialogVisible by remember { mutableStateOf(false) }
    var selectedTone by remember { mutableStateOf(reminder.tone) }
    var autoDismissSeconds by remember { mutableStateOf(reminder.autoDismissSeconds.toString()) }
    var selectedTrigger by remember { mutableStateOf(reminder.trigger) }

    val profile by karooSystem.streamUserProfile().collectAsStateWithLifecycle(null)

    fun getUpdatedReminder(): Reminder = Reminder(reminder.id, title, duration.toIntOrNull() ?: 1,
        text = text,
        displayForegroundColor = selectedColor,
        isActive = isActive,
        trigger = selectedTrigger,
        isAutoDismiss = autoDismiss, tone = selectedTone, autoDismissSeconds = autoDismissSeconds.toIntOrNull() ?: 15)

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

            apply {
                val dropdownOptions = ReminderTrigger.entries.toList().map { unit -> DropdownOption(unit.id, unit.label) }
                val dropdownInitialSelection by remember(selectedTrigger) {
                    mutableStateOf(dropdownOptions.find { option -> option.id == selectedTrigger.id }!!)
                }
                Dropdown(label = "Trigger", options = dropdownOptions, selected = dropdownInitialSelection) { selectedOption ->
                    val previousTrigger = selectedTrigger
                    selectedTrigger = ReminderTrigger.entries.find { entry -> entry.id == selectedOption.id }!!

                    if (selectedTrigger != previousTrigger) {
                        duration = when (selectedTrigger) {
                            ReminderTrigger.ELAPSED_TIME -> 30.toString()
                            ReminderTrigger.DISTANCE -> 10.toString()
                            ReminderTrigger.HR_LIMIT_MAXIMUM_EXCEEDED -> 160.toString()
                            ReminderTrigger.POWER_LIMIT_MAXIMUM_EXCEEDED -> 200.toString()
                            ReminderTrigger.HR_LIMIT_MINIMUM_EXCEEDED -> 60.toString()
                            ReminderTrigger.POWER_LIMIT_MINIMUM_EXCEEDED -> 100.toString()
                            ReminderTrigger.SPEED_LIMIT_MAXIMUM_EXCEEDED -> 40.toString()
                            ReminderTrigger.SPEED_LIMIT_MINIMUM_EXCEEDED -> 20.toString()
                            ReminderTrigger.CADENCE_LIMIT_MAXIMUM_EXCEEDED -> 120.toString()
                            ReminderTrigger.CADENCE_LIMIT_MINIMUM_EXCEEDED -> 60.toString()
                        }
                    }
                }
            }

            OutlinedTextField(value = duration, modifier = Modifier.fillMaxWidth(),
                onValueChange = { duration = it },
                label = {
                    when(selectedTrigger){
                        ReminderTrigger.ELAPSED_TIME ->  Text("Interval")
                        ReminderTrigger.DISTANCE -> Text("Distance")
                        ReminderTrigger.HR_LIMIT_MAXIMUM_EXCEEDED -> Text("Maximum heart rate")
                        ReminderTrigger.POWER_LIMIT_MAXIMUM_EXCEEDED -> Text("Maximum power")
                        ReminderTrigger.HR_LIMIT_MINIMUM_EXCEEDED -> Text("Minimum heart rate")
                        ReminderTrigger.POWER_LIMIT_MINIMUM_EXCEEDED -> Text("Minimum power")
                        ReminderTrigger.SPEED_LIMIT_MAXIMUM_EXCEEDED -> Text("Maximum speed")
                        ReminderTrigger.SPEED_LIMIT_MINIMUM_EXCEEDED -> Text("Minimum speed")
                        ReminderTrigger.CADENCE_LIMIT_MAXIMUM_EXCEEDED -> Text("Maximum cadence")
                        ReminderTrigger.CADENCE_LIMIT_MINIMUM_EXCEEDED -> Text("Minimum cadence")
                    }
                },
                suffix = {
                    Text(selectedTrigger.getSuffix(profile?.preferredUnit?.distance == UserProfile.PreferredUnit.UnitType.IMPERIAL))
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            ColorDialog(
                state = colorDialogState,
                selection = ColorSelection(
                    selectedColor = SingleColor(colorRes = selectedColor?.colorRes),
                    onSelectColor = { c -> selectedColor = ReminderColor.getColor(ctx, c) },
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

                        ContextCompat.getColor(LocalContext.current, R.color.hRed),
                        ContextCompat.getColor(LocalContext.current, R.color.hPurple),
                        ContextCompat.getColor(LocalContext.current, R.color.hYellow),
                        ContextCompat.getColor(LocalContext.current, R.color.hGreen),
                        ContextCompat.getColor(LocalContext.current, R.color.hBlue),
                        ContextCompat.getColor(LocalContext.current, R.color.hCyan),
                    ),
                )
            )

            FilledTonalButton(modifier = Modifier
                .fillMaxWidth(),
                onClick = {
                    colorDialogState.show()
            }) {
                Surface(shape = CircleShape, color = Color(ContextCompat.getColor(ctx, selectedColor?.colorRes ?: R.color.hRed)),
                    modifier = Modifier
                        .height(40.dp)
                        .shadow(5.dp, CircleShape)
                        .width(40.dp), content = {})

                Spacer(modifier = Modifier.width(5.dp))

                Text("Change Color")
            }

            FilledTonalButton(modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
                onClick = {
                    toneDialogVisible = true
                }) {
                Icon(Icons.Default.Build, contentDescription = "Change Tone")
                Spacer(modifier = Modifier.width(5.dp))
                Text("Change Tone")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = autoDismiss, onCheckedChange = { autoDismiss = it})
                Spacer(modifier = Modifier.width(10.dp))
                Text("Auto-Dismiss")
            }

            if (autoDismiss){
                OutlinedTextField(value = autoDismissSeconds, modifier = Modifier.fillMaxWidth(),
                    onValueChange = { autoDismissSeconds = it },
                    label = { Text("Display duration") },
                    suffix = { Text("s") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
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
                Spacer(modifier = Modifier.width(5.dp))
                Text("Save")
            }

            FilledTonalButton(modifier = Modifier
                .fillMaxWidth()
                .height(50.dp), onClick = {
                onCancel()
            }) {
                Icon(Icons.Default.Close, contentDescription = "Cancel Editing")
                Spacer(modifier = Modifier.width(5.dp))
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

            if (toneDialogVisible){
                Dialog(onDismissRequest = { toneDialogVisible = false }) {
                    var dialogSelectedTone by remember { mutableStateOf(selectedTone) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column(modifier = Modifier
                            .padding(5.dp)
                            .verticalScroll(rememberScrollState())
                            .fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {

                            ReminderBeepPattern.entries.forEach { pattern ->
                                val tones = pattern.tones

                                Row(modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        dialogSelectedTone = pattern
                                        karooSystem.dispatch(PlayBeepPattern(tones))
                                    }, verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = dialogSelectedTone == pattern, onClick = {
                                        dialogSelectedTone = pattern
                                        karooSystem.dispatch(PlayBeepPattern(tones))
                                    })
                                    Text(
                                        text = pattern.displayName,
                                        modifier = Modifier.padding(start = 10.dp)
                                    )
                                }
                            }

                            FilledTonalButton(modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp), onClick = {
                                selectedTone = dialogSelectedTone
                                toneDialogVisible = false
                            }) {
                                Icon(Icons.Default.Done, contentDescription = "Save")
                                Text("Save")
                            }

                            FilledTonalButton(modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp), onClick = {
                                toneDialogVisible = false
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel")
                                Text("Cancel")
                            }
                        }
                    }
                }
            }
        }
    }
}