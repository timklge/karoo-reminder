package de.timklge.karooreminder.model

import android.content.Context
import android.graphics.Color
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import de.timklge.karooreminder.R
import kotlinx.serialization.Serializable
import kotlin.collections.plus

/**
 * Colors for reminders
 *
 * @param colorRes The color resource for the color
 * @param whiteFont Whether the font should be white or black
 * @param mapFromColors The colors that should be mapped to this color (only used for migration of old colors that were stored as integers)
 */
@Serializable
enum class ReminderColor(@ColorRes val colorRes: Int, val whiteFont: Boolean, val mapFromColors: Set<Int> = emptySet()) {
    DARK_RED(R.color.bRed, true),
    DARK_PURPLE(R.color.bPurple, true),
    DARK_YELLOW(R.color.bYellow, true),
    DARK_GREEN(R.color.bGreen, true),
    DARK_BLUE(R.color.bBlue, true),
    DARK_CYAN(R.color.bCyan, true),

    LIGHT_RED(R.color.hRed, false, setOf(Color.parseColor("#FF6060"))),
    LIGHT_PURPLE(R.color.hPurple, false, setOf(Color.parseColor("#FF70FF"))),
    LIGHT_YELLOW(R.color.hYellow, false, setOf(Color.parseColor("#FFFF60"))),
    LIGHT_GREEN(R.color.hGreen, false, setOf(Color.parseColor("#50FF50"))),
    LIGHT_BLUE(R.color.hBlue, false, setOf(Color.parseColor("#7070FF"))),
    LIGHT_CYAN(R.color.hCyan, false, setOf(Color.parseColor("#60FFFF")));

    @ColorRes
    fun getTextColor(): Int {
        return if(whiteFont) R.color.white else R.color.black
    }

    companion object {
        fun getColor(context: Context, color: Int): ReminderColor {
            return entries.find { reminderColor ->
                val mapFromColors = reminderColor.mapFromColors + ContextCompat.getColor(context, reminderColor.colorRes)

                mapFromColors.any { it == color }
            } ?: error("Unknown color resource")
        }
    }
}