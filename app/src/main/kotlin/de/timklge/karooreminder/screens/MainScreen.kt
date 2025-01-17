package de.timklge.karooreminder.screens

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import de.timklge.karooreminder.KarooReminderExtension
import de.timklge.karooreminder.R
import de.timklge.karooreminder.dataStore
import de.timklge.karooreminder.streamUserProfile
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.UserProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


val preferencesKey = stringPreferencesKey("reminders")

suspend fun saveReminders(context: Context, reminders: MutableList<Reminder>) {
    context.dataStore.edit { t ->
        t[preferencesKey] = Json.encodeToString(reminders)
    }
}

@Composable
fun ReminderAppNavHost(modifier: Modifier = Modifier, navController: NavHostController = rememberNavController()){
    val scope = rememberCoroutineScope()
    val reminders = remember {
        mutableStateListOf<Reminder>()
    }

    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        ctx.dataStore.data.distinctUntilChanged().collect { t ->
            reminders.clear()
            try {
                val entries = Json.decodeFromString<MutableList<Reminder>>(
                    t[preferencesKey] ?: defaultReminders
                )
                entries.forEach {
                    if (it.displayForegroundColor == null){
                        it.displayForegroundColor = ReminderColor.getColor(ctx, it.foregroundColor)
                    }
                }
                reminders.addAll(entries)
            } catch(e: Throwable){
                Log.e(KarooReminderExtension.TAG,"Failed to read preferences", e)
            }
        }
    }

    NavHost(modifier = modifier, navController = navController, startDestination = "reminders") {
        composable(route = "reminder/{id}", arguments = listOf(
            navArgument("id") {
                type = NavType.IntType
                nullable = false
            }
        )) { stack ->
            val reminderId = stack.arguments?.getInt("id")
            val reminder = reminders.find { it.id  == reminderId}
            val reminderIndex = reminders.indexOf(reminder)

            reminder?.let { r ->
                DetailScreen(false, r, { updatedReminder ->
                    if (updatedReminder != null) {
                        reminders[reminderIndex] = updatedReminder
                    } else {
                        reminders.remove(r)
                    }

                    scope.launch {
                        saveReminders(ctx, reminders)
                    }
                    navController.popBackStack()
                }, { navController.popBackStack() })
            }
        }
        composable(route = "create") {
            val nextReminderId = reminders.maxOfOrNull { it.id + 1 } ?: 0
            val newReminder = Reminder(nextReminderId, "", 30, "")

            DetailScreen(true, newReminder, { updatedReminder ->
                updatedReminder?.let { r ->
                    reminders.add(r)
                }

                scope.launch {
                    saveReminders(ctx, reminders)
                }

                navController.popBackStack()
            }, { navController.popBackStack() })
        }
        composable(route = "reminders") {
            MainScreen(reminders, { reminder -> navController.navigate(route = "reminder/${reminder.id}") }, { navController.navigate(route = "create") })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(reminders: MutableList<Reminder>, onNavigateToReminder: (r: Reminder) -> Unit, onNavigateToCreateReminder: () -> Unit) {
    var karooConnected by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val karooSystem = remember { KarooSystemService(ctx) }

    var showWarnings by remember { mutableStateOf(false) }
    val profile by karooSystem.streamUserProfile().collectAsStateWithLifecycle(null)

    LaunchedEffect(Unit) {
        delay(1000L)
        showWarnings = true
    }

    Scaffold(
        topBar = { TopAppBar(title = {Text("Reminder")}) },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            FloatingActionButton(onClick = {
                onNavigateToCreateReminder()
            }) {
                Icon(Icons.Rounded.Add, "Add")
            }
        },
        content = {
            Column(
                Modifier
                    .padding(it)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.background)) {
                reminders.forEach { reminder ->
                    Card(Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .alpha(if (reminder.isActive) 1f else 0.6f)
                        .clickable { onNavigateToReminder(reminder) }
                        .padding(5.dp), shape = RoundedCornerShape(corner = CornerSize(10.dp))
                    ) {
                        Row(
                            Modifier
                                .height(60.dp)
                                .padding(5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = Color(ContextCompat.getColor(ctx, reminder.displayForegroundColor?.colorRes ?: R.color.hRed)),
                                modifier = Modifier
                                    .height(60.dp)
                                    .shadow(5.dp, CircleShape)
                                    .width(40.dp), content = {})

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(reminder.name)

                            Spacer(Modifier.weight(1.0f))

                            Text("${reminder.trigger.getPrefix()}${reminder.interval}${reminder.trigger.getSuffix(profile?.preferredUnit?.distance == UserProfile.PreferredUnit.UnitType.IMPERIAL)}")
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    karooSystem.connect { connected ->
                        karooConnected = connected
                    }
                }

                if (showWarnings){
                    if (reminders.isEmpty()) Text(modifier = Modifier.padding(5.dp), text = "No reminders added.")

                    if (!karooConnected){
                        Text(modifier = Modifier.padding(5.dp), text = "Could not read device status. Is your Karoo updated?")
                    }
                }
            }
        }
    )
}