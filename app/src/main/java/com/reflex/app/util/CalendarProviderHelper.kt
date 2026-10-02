package com.reflex.app.util

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.util.TimeZone

data class DeviceCalendar(
    val id: Long,
    val name: String,
    val accountName: String?,
    val isVisible: Boolean,
    val isPrimary: Boolean,
    val color: Int = 0,
    val syncId: String? = null
)

data class DeviceCalendarEvent(
    val id: Long,
    val title: String,
    val description: String? = null,
    val startMillis: Long,
    val endMillis: Long,
    val allDay: Boolean = false,
    val location: String? = null,
    val calendarName: String? = null
)

object CalendarProviderHelper {

    fun hasReadPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasWritePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getAvailableCalendars(context: Context): List<DeviceCalendar> {
        if (!hasReadPermission(context)) return emptyList()

        val list = mutableListOf<DeviceCalendar>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.VISIBLE,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars._SYNC_ID
        )

        try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                val nameIdx = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val accIdx = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
                val visIdx = cursor.getColumnIndex(CalendarContract.Calendars.VISIBLE)
                val primIdx = cursor.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)
                val colorIdx = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_COLOR)
                val syncIdIdx = cursor.getColumnIndex(CalendarContract.Calendars._SYNC_ID)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIdx)
                    val name = cursor.getString(nameIdx) ?: "Calendar $id"
                    val acc = cursor.getString(accIdx)
                    val vis = cursor.getInt(visIdx) == 1
                    val prim = if (primIdx >= 0) cursor.getInt(primIdx) == 1 else false
                    val color = if (colorIdx >= 0) cursor.getInt(colorIdx) else 0
                    val syncId = if (syncIdIdx >= 0) cursor.getString(syncIdIdx) else null

                    list.add(
                        DeviceCalendar(
                            id = id,
                            name = name,
                            accountName = acc,
                            isVisible = vis,
                            isPrimary = prim,
                            color = color,
                            syncId = syncId
                        )
                    )
                }
            }
        } catch (e: Exception) {
            AppLog.w("CalendarProviderHelper", "Failed to query device calendars", e)
        }
        return list
    }

    fun resolveDisplayName(
        calendar: DeviceCalendar,
        overrides: Map<String, String>
    ): String {
        val stableKey = CalendarPreferenceRepository.getCalendarStableKey(calendar.accountName, calendar.syncId, calendar.name)
        overrides[stableKey]?.let { return it }
        if (calendar.name.contains("://") || calendar.name.length > 50) {
            return "Subscribed calendar"
        }
        return calendar.name
    }

    fun getEventsInRange(
        context: Context,
        startMillis: Long,
        endMillis: Long,
        enabledCalendarIds: Set<Long>? = null
    ): List<DeviceCalendarEvent> {
        if (!hasReadPermission(context)) return emptyList()

        val rawEvents = mutableListOf<DeviceCalendarEvent>()
        val builder: Uri.Builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        android.content.ContentUris.appendId(builder, startMillis)
        android.content.ContentUris.appendId(builder, endMillis)

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
            CalendarContract.Instances.CALENDAR_ID
        )

        try {
            context.contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID)
                val titleIdx = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
                val descIdx = cursor.getColumnIndex(CalendarContract.Instances.DESCRIPTION)
                val beginIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
                val endIdx = cursor.getColumnIndex(CalendarContract.Instances.END)
                val allDayIdx = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)
                val locIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)
                val calIdx = cursor.getColumnIndex(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)
                val calIdIdx = cursor.getColumnIndex(CalendarContract.Instances.CALENDAR_ID)

                while (cursor.moveToNext()) {
                    val calId = cursor.getLong(calIdIdx)

                    // Filter by enabled calendar IDs if filter passed
                    if (enabledCalendarIds != null && !enabledCalendarIds.contains(calId)) {
                        continue
                    }

                    val id = cursor.getLong(idIdx)
                    val title = cursor.getString(titleIdx) ?: "Untitled Event"
                    val desc = cursor.getString(descIdx)
                    val begin = cursor.getLong(beginIdx)
                    val end = cursor.getLong(endIdx)
                    val allDay = cursor.getInt(allDayIdx) == 1
                    val location = cursor.getString(locIdx)
                    val calName = cursor.getString(calIdx)

                    rawEvents.add(
                        DeviceCalendarEvent(
                            id = id,
                            title = title,
                            description = desc,
                            startMillis = begin,
                            endMillis = end,
                            allDay = allDay,
                            location = location,
                            calendarName = calName
                        )
                    )
                }
            }
        } catch (e: Exception) {
            AppLog.w("CalendarProviderHelper", "Failed to query day calendar instances", e)
        }

        // Deduplication Safety Net: Match on (title.lowercase().trim(), startMillis, endMillis)
        val deduplicatedEvents = mutableListOf<DeviceCalendarEvent>()
        val seenKeys = mutableSetOf<String>()

        for (event in rawEvents) {
            val key = "${event.title.lowercase().trim()}_${event.startMillis}_${event.endMillis}"
            if (seenKeys.add(key)) {
                deduplicatedEvents.add(event)
            }
        }

        return deduplicatedEvents
    }

    fun addEventToDeviceCalendar(
        context: Context,
        title: String,
        description: String?,
        startMillis: Long,
        endMillis: Long,
        allDay: Boolean = false,
        location: String? = null,
        calendarId: Long? = null
    ): Long? {
        if (!hasWritePermission(context)) return null

        try {
            val targetCalendarId = calendarId ?: getDefaultCalendarId(context) ?: return null

            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DESCRIPTION, description ?: "")
                if (!location.isNullOrBlank()) {
                    put(CalendarContract.Events.EVENT_LOCATION, location)
                }
                put(CalendarContract.Events.ALL_DAY, if (allDay) 1 else 0)
                put(CalendarContract.Events.CALENDAR_ID, targetCalendarId)
                put(CalendarContract.Events.EVENT_TIMEZONE, if (allDay) "UTC" else TimeZone.getDefault().id)
            }

            val uri: Uri? = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            return uri?.lastPathSegment?.toLongOrNull()
        } catch (e: Exception) {
            AppLog.e("CalendarProviderHelper", "Failed to insert event into device calendar", e)
            return null
        }
    }

    private fun getDefaultCalendarId(context: Context): Long? {
        val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.IS_PRIMARY)
        try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                val primaryIdx = cursor.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIdx)
                    val isPrimary = if (primaryIdx >= 0) cursor.getInt(primaryIdx) == 1 else false
                    if (isPrimary) return id
                }

                if (cursor.moveToFirst()) {
                    return cursor.getLong(idIdx)
                }
            }
        } catch (e: Exception) {
            AppLog.w("CalendarProviderHelper", "Failed to resolve default calendar id", e)
        }
        return null
    }

    fun launchAddEventIntent(
        context: Context,
        title: String,
        description: String?,
        startMillis: Long,
        endMillis: Long,
        allDay: Boolean = false,
        location: String? = null
    ) {
        try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title)
                if (!description.isNullOrBlank()) {
                    putExtra(CalendarContract.Events.DESCRIPTION, description)
                }
                if (!location.isNullOrBlank()) {
                    putExtra(CalendarContract.Events.EVENT_LOCATION, location)
                }
                putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, allDay)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            AppLog.e("CalendarProviderHelper", "Failed to launch calendar add intent", e)
        }
    }
}
