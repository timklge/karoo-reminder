package de.timklge.karooreminder.screens

import android.content.Context
import androidx.core.content.ContextCompat
import de.timklge.karooreminder.R
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
class Reminder(val id: Int, var name: String, var interval: Int, var text: String,
               var foregroundColor: Int = android.graphics.Color.parseColor("#700000"),
               val isActive: Boolean = true){

    fun getResourceColor(context: Context): Int {
        return when(foregroundColor){
            ContextCompat.getColor(context, R.color.bRed) -> R.color.bRed
            ContextCompat.getColor(context, R.color.bPurple) -> R.color.bPurple
            ContextCompat.getColor(context, R.color.bYellow) -> R.color.bYellow
            ContextCompat.getColor(context, R.color.bGreen) -> R.color.bGreen
            ContextCompat.getColor(context, R.color.bBlue) -> R.color.bBlue
            ContextCompat.getColor(context, R.color.bCyan) -> R.color.bCyan
            else -> error("Unknown color")
        }
    }
}

val defaultReminders = Json.encodeToString(listOf(Reminder(0, "Drink", 30, "Take a sip!")))