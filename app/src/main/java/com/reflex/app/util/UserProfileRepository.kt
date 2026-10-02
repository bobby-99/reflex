package com.reflex.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream
import java.io.File

data class UserProfile(
    val name: String = "User",
    val avatarEmoji: String = "⚡",
    val userEmail: String = "",
    val focusGoalMinutes: Int = 60,
    val bio: String = "Local-first productivity",
    val avatarPath: String? = null,
    val avatarBase64: String? = null
)

object UserProfileRepository {

    private const val PREFS_NAME = "reflex_user_profile"
    private const val KEY_NAME = "profile_name"
    private const val KEY_AVATAR = "profile_avatar"
    private const val KEY_EMAIL = "profile_email"
    private const val KEY_GOAL = "profile_goal_mins"
    private const val KEY_BIO = "profile_bio"
    private const val KEY_AVATAR_PATH = "profile_avatar_path"
    private const val KEY_AVATAR_BASE64 = "profile_avatar_base64"

    private val _profile = MutableStateFlow(UserProfile())
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val path = prefs.getString(KEY_AVATAR_PATH, null)
        val b64 = prefs.getString(KEY_AVATAR_BASE64, null)

        var validPath: String? = null
        if (!path.isNullOrBlank()) {
            val file = File(path)
            if (file.exists()) {
                validPath = path
            }
        }
        if (validPath == null && !b64.isNullOrBlank()) {
            try {
                val bytes = Base64.decode(b64, Base64.DEFAULT)
                val avatarFile = File(context.filesDir, "profile_avatar.jpg")
                avatarFile.writeBytes(bytes)
                validPath = avatarFile.absolutePath
            } catch (e: Exception) {
                AppLog.w("UserProfileRepository", "Failed to decode cached avatar base64", e)
            }
        }

        _profile.value = UserProfile(
            name = prefs.getString(KEY_NAME, "User") ?: "User",
            avatarEmoji = prefs.getString(KEY_AVATAR, "⚡") ?: "⚡",
            userEmail = prefs.getString(KEY_EMAIL, "") ?: "",
            focusGoalMinutes = prefs.getInt(KEY_GOAL, 60),
            bio = prefs.getString(KEY_BIO, "Local-first productivity") ?: "Local-first productivity",
            avatarPath = validPath,
            avatarBase64 = b64
        )
    }

    fun updateProfile(context: Context, profile: UserProfile) {
        _profile.value = profile
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_NAME, profile.name)
            .putString(KEY_AVATAR, profile.avatarEmoji)
            .putString(KEY_EMAIL, profile.userEmail)
            .putInt(KEY_GOAL, profile.focusGoalMinutes)
            .putString(KEY_BIO, profile.bio)
            .putString(KEY_AVATAR_PATH, profile.avatarPath)
            .putString(KEY_AVATAR_BASE64, profile.avatarBase64)
            .apply()
    }

    fun updateNameAndBio(context: Context, name: String, bio: String) {
        val trimmedName = name.trim().ifBlank { "User" }
        val trimmedBio = bio.trim()
        val updated = _profile.value.copy(
            name = trimmedName,
            bio = trimmedBio
        )
        updateProfile(context, updated)
        SettingsRepository.setUserName(context, trimmedName)
    }

    fun setAvatar(context: Context, bitmap: Bitmap) {
        try {
            val file = File(context.filesDir, "profile_avatar.jpg")
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            val bytes = outputStream.toByteArray()
            file.writeBytes(bytes)
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

            val updated = _profile.value.copy(
                avatarPath = file.absolutePath,
                avatarBase64 = b64
            )
            updateProfile(context, updated)
        } catch (e: Exception) {
            AppLog.e("UserProfileRepository", "Failed to save profile avatar bitmap", e)
        }
    }

    fun removeAvatar(context: Context) {
        try {
            val file = File(context.filesDir, "profile_avatar.jpg")
            if (file.exists()) file.delete()
        } catch (_: Exception) {}

        val updated = _profile.value.copy(
            avatarPath = null,
            avatarBase64 = null
        )
        updateProfile(context, updated)
    }
}
