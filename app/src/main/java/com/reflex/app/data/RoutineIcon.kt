package com.reflex.app.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

enum class RoutineIcon(val key: String, val label: String, val icon: ImageVector) {
    BOLT("BOLT", "Bolt", Icons.Default.Bolt),
    RUN("RUN", "Run", Icons.Default.DirectionsRun),
    SUNNY("SUNNY", "Morning", Icons.Default.WbSunny),
    BEDTIME("BEDTIME", "Bedtime", Icons.Default.Bedtime),
    MEDITATE("MEDITATE", "Mind", Icons.Default.SelfImprovement),
    BOOK("BOOK", "Read", Icons.Default.MenuBook),
    HYDRATE("HYDRATE", "Water", Icons.Default.LocalDrink),
    FITNESS("FITNESS", "Gym", Icons.Default.FitnessCenter),
    CODE("CODE", "Code", Icons.Default.Code),
    WORK("WORK", "Work", Icons.Default.Work),
    STUDY("STUDY", "Study", Icons.Default.School),
    ART("ART", "Create", Icons.Default.Brush),
    TIMER("TIMER", "Timer", Icons.Default.Timer),
    CHECK("CHECK", "Task", Icons.Default.CheckCircle),
    STAR("STAR", "Star", Icons.Default.Star),
    HEART("HEART", "Health", Icons.Default.Favorite);

    companion object {
        fun fromKey(key: String): RoutineIcon {
            return entries.find { it.key.equals(key, ignoreCase = true) } ?: BOLT
        }
    }
}
