# Room Database Rules
-keep class androidx.room.RoomDatabase { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }
-keep class com.reflex.app.data.** { *; }
-dontwarn androidx.room.paging.**

# Keep Application, Activities, Services, Receivers
-keep class com.reflex.app.ReflexApplication { *; }
-keep class com.reflex.app.MainActivity { *; }
-keep class com.reflex.app.ui.FloatingTaskCaptureActivity { *; }
-keep class com.reflex.app.service.** { *; }
-keep class com.reflex.app.receiver.** { *; }

# Keep ViewModel, Data models and State holders
-keep class com.reflex.app.ui.** { *; }
-keep class com.reflex.app.util.** { *; }

# Compose Keep Rules
-keepclassmembers class * extends androidx.compose.ui.Modifier { *; }
-dontwarn androidx.compose.**
