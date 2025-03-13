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
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.res.painterResource
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
import de.timklge.karooreminder.R
import de.timklge.karooreminder.ReminderTrigger
import de.timklge.karooreminder.streamUserProfile
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.HardwareType
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
    DisposableEffect(Unit) {
        onDispose {
            karooSystem.disconnect()
        }
    }
    var title by remember { mutableStateOf(reminder.name) }
    var text by remember { mutableStateOf(reminder.text) }
    var selectedColor by remember { mutableStateOf(reminder.displayForegroundColor) }
    val colorDialogState by remember { mutableStateOf(UseCaseState()) }
    var duration by remember {
        mutableStateOf(if (reminder.intervalFloat != null){
            java.text.DecimalFormat("#.##").format(reminder.intervalFloat)
        } else {
            reminder.interval.toString()
        })
    }
    var isActive by remember { mutableStateOf(reminder.isActive) }
    var autoDismiss by remember { mutableStateOf(reminder.isAutoDismiss) }
    var deleteDialogVisible by remember { mutableStateOf(false) }
    var toneDialogVisible by remember { mutableStateOf(false) }
    var triggerDialogVisible by remember { mutableStateOf(false) }
    var selectedTone by remember { mutableStateOf(reminder.tone) }
    var autoDismissSeconds by remember { mutableStateOf(reminder.autoDismissSeconds.toString()) }
    var selectedTrigger by remember { mutableStateOf(reminder.trigger) }

    val profile by karooSystem.streamUserProfile().collectAsStateWithLifecycle(null)

    fun getUpdatedReminder(): Reminder {
        val durationString = duration.replace(",", ".")

        return Reminder(reminder.id, title, interval = durationString.toDoubleOrNull()?.toInt() ?: 1,
            intervalFloat = if (selectedTrigger.isDecimalValue()) durationString.toDoubleOrNull() else null,
            text = text,
            displayForegroundColor = selectedColor,
            isActive = isActive,
            trigger = selectedTrigger,
            isAutoDismiss = autoDismiss, tone = selectedTone, autoDismissSeconds = autoDismissSeconds.toIntOrNull() ?: 15)
    }

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

            FilledTonalButton(modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
                onClick = {
                    triggerDialogVisible = true
                }) {
                Icon(Icons.Default.Build, contentDescription = "Change Trigger", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text("Trigger: ${selectedTrigger.label}")
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
                        ReminderTrigger.ENERGY_OUTPUT -> Text("Energy Output")
                        ReminderTrigger.CORE_TEMPERATURE_LIMIT_MAXIMUM_EXCEEDED -> Text("Maximum core temp")
                        ReminderTrigger.CORE_TEMPERATURE_LIMIT_MINIMUM_EXCEEDED -> Text("Minimum core temp")
                        ReminderTrigger.FRONT_TIRE_PRESSURE_LIMIT_MAXIMUM_EXCEEDED -> Text("Max front tire pressure")
                        ReminderTrigger.FRONT_TIRE_PRESSURE_LIMIT_MINIMUM_EXCEEDED -> Text("Min front tire pressure")
                        ReminderTrigger.REAR_TIRE_PRESSURE_LIMIT_MAXIMUM_EXCEEDED -> Text("Max rear tire pressure")
                        ReminderTrigger.REAR_TIRE_PRESSURE_LIMIT_MINIMUM_EXCEEDED -> Text("Min rear tire pressure")
                        ReminderTrigger.AMBIENT_TEMPERATURE_LIMIT_MAXIMUM_EXCEEDED -> Text("Maximum temp")
                        ReminderTrigger.AMBIENT_TEMPERATURE_LIMIT_MINIMUM_EXCEEDED -> Text("Minimum temp")
                        ReminderTrigger.GRADIENT_LIMIT_MAXIMUM_EXCEEDED -> Text("Maximum gradient")
                        ReminderTrigger.GRADIENT_LIMIT_MINIMUM_EXCEEDED -> Text("Minimum gradient")
                    }
                },
                suffix = {
                    Text(selectedTrigger.getSuffix(profile?.preferredUnit?.distance == UserProfile.PreferredUnit.UnitType.IMPERIAL))
                },
                keyboardOptions = KeyboardOptions(keyboardType = if (selectedTrigger.isDecimalValue()) KeyboardType.Decimal else KeyboardType.Number),
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
                .fillMaxWidth()
                .height(60.dp),
                onClick = {
                    colorDialogState.show()
            }) {
                Surface(shape = CircleShape, color = Color(ContextCompat.getColor(ctx, selectedColor?.colorRes ?: R.color.hRed)),
                    modifier = Modifier
                        .height(30.dp)
                        .shadow(5.dp, CircleShape)
                        .width(30.dp), content = {})

                Spacer(modifier = Modifier.width(5.dp))

                Text("Color")
            }

            FilledTonalButton(modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
                onClick = {
                    toneDialogVisible = true
                }) {
                Icon(painterResource(R.drawable.volume), contentDescription = "Tone", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text("Tone: ${selectedTone.displayName}")
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

            if (triggerDialogVisible){
                Dialog(onDismissRequest = { triggerDialogVisible = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Column(modifier = Modifier
                            .padding(5.dp)
                            .verticalScroll(rememberScrollState())
                            .fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {

                            ReminderTrigger.entries.forEach { trigger ->
                                Row(modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedTrigger = trigger
                                        triggerDialogVisible = false
                                    }, verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = selectedTrigger == trigger, onClick = {
                                        selectedTrigger = trigger
                                        triggerDialogVisible = false
                                    })

                                    Column(modifier = Modifier.padding(start = 10.dp)) {
                                        Text(
                                            text = trigger.label,
                                        )

                                        if (trigger == ReminderTrigger.ENERGY_OUTPUT){
                                            Text(
                                                text = "Powermeter required",
                                                color = Color.Gray,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (toneDialogVisible){
                Dialog(onDismissRequest = { toneDialogVisible = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Column(modifier = Modifier
                            .padding(5.dp)
                            .verticalScroll(rememberScrollState())
                            .fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {

                            ReminderBeepPattern.entries.forEach { pattern ->
                                val tones = if (karooSystem.hardwareType == HardwareType.K2) pattern.tonesKaroo2 else pattern.tonesKaroo3

                                Row(modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedTone = pattern
                                        karooSystem.dispatch(PlayBeepPattern(tones))
                                        toneDialogVisible = false
                                    }, verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = selectedTone == pattern, onClick = {
                                        selectedTone = pattern
                                        toneDialogVisible = false
                                        karooSystem.dispatch(PlayBeepPattern(tones))
                                    })
                                    Text(
                                        text = pattern.displayName,
                                        modifier = Modifier.padding(start = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}