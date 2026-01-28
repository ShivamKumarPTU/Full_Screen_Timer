package com.example.monktemple.customization

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.util.Log
import androidx.core.content.edit
import com.example.monktemple.R
//import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomizationManager @Inject constructor(
    private val context: Context
) {

    companion object {
        private const val PREF_CUSTOMIZATION = "app_customization"
        private const val KEY_THEME = "theme"
        private const val KEY_TIMER_SOUND = "timer_sound"
        private const val KEY_SESSION_TEMPLATES = "session_templates"
        private const val KEY_CUSTOM_SOUNDS = "custom_sounds"
        private const val KEY_UI_PREFERENCES = "ui_preferences"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_CUSTOMIZATION, Context.MODE_PRIVATE)
    private val gson = Gson()

    enum class AppTheme {
        LIGHT, DARK, SYSTEM, AMOLED, BLUE_LIGHT, NATURE, SPACE
    }

    data class SessionTemplate(
        val templateId: String = "",
        val name: String = "",
        val duration: Long = 0,
        val goalName: String = "",
        val sound: String = "",
        val theme: AppTheme = AppTheme.SYSTEM,
        val description: String = "",
        val tags: List<String> = emptyList(),
        val createdAt: Long = System.currentTimeMillis()
    )

    data class SoundProfile(
        val soundId: String = "",
        val name: String = "",
        val resourceId: Int = 0,
        val category: String = "", // nature, ambient, white-noise, etc.
        val volume: Int = 80
    )

    data class UIPreferences(
        val showStats: Boolean = true,
        val showCommunity: Boolean = true,
        val minimalistMode: Boolean = false,
        val animationLevel: Int = 2, // 0: none, 1: minimal, 2: full
        val fontSize: Int = 16,
        val colorScheme: String = "default"
    )

    // Theme Management
    fun setAppTheme(theme: AppTheme) {
        prefs.edit { putString(KEY_THEME, theme.name) }
        applyTheme(theme)
    }

    fun getCurrentTheme(): AppTheme {
        return try {
            AppTheme.valueOf(prefs.getString(KEY_THEME, AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name)
        } catch (e: Exception) {
            AppTheme.SYSTEM
        }
    }

    private fun applyTheme(theme: AppTheme) {
        // This would typically set the theme resource
        when (theme) {
            AppTheme.LIGHT -> context.setTheme(R.style.Theme_MonkTemple_Light)
            AppTheme.DARK -> context.setTheme(R.style.Theme_MonkTemple_Dark)
            AppTheme.AMOLED -> context.setTheme(R.style.Theme_MonkTemple_Amoled)
            AppTheme.BLUE_LIGHT -> context.setTheme(R.style.Theme_MonkTemple_BlueLight)
            AppTheme.NATURE -> context.setTheme(R.style.Theme_MonkTemple_Nature)
            AppTheme.SPACE -> context.setTheme(R.style.Theme_MonkTemple_Space)
            else -> context.setTheme(R.style.Theme_MonkTemple_Light)
        }
    }

    // Sound Management
    fun setTimerSound(soundProfile: SoundProfile) {
        val soundJson = gson.toJson(soundProfile)
        prefs.edit { putString(KEY_TIMER_SOUND, soundJson) }
    }

    fun getTimerSound(): SoundProfile {
        return try {
            val soundJson = prefs.getString(KEY_TIMER_SOUND, null)
            soundJson?.let {
                gson.fromJson(it, SoundProfile::class.java)
            } ?: getDefaultSound()
        } catch (e: Exception) {
            getDefaultSound()
        }
    }

    fun playTimerSound() {
        try {
            val soundProfile = getTimerSound()
            val mediaPlayer = MediaPlayer.create(context, soundProfile.resourceId)
            mediaPlayer?.setVolume(soundProfile.volume / 100f, soundProfile.volume / 100f)
            mediaPlayer?.start()
        } catch (e: Exception) {
            Log.e("Customization", "Error playing timer sound", e)
        }
    }

    fun getAvailableSounds(): List<SoundProfile> {
        return listOf(
            SoundProfile("default", "Default Bell", R.raw.timersound,"classic", 80),
            SoundProfile("nature", "Forest Birds", R.raw.birdsound, "nature", 70),
            SoundProfile("ocean", "Ocean Waves", R.raw.oceansound, "nature", 75),
            SoundProfile("bell", "Crystal Bell", R.raw.crystalsound, "classic", 80),
            SoundProfile("gong", "Meditation Gong", R.raw.meditationsound, "meditation", 85),
            SoundProfile("rain", "Gentle Rain", R.raw.rainsound, "ambient", 65)
        )
    }

    private fun getDefaultSound(): SoundProfile {
        return SoundProfile("default", "Default Bell", R.raw.timersound, "classic", 80)
    }

    // Session Templates
    fun saveSessionTemplate(template: SessionTemplate): Boolean {
        return try {
            val templates = getSessionTemplates().toMutableList()
            val templateWithId = if (template.templateId.isEmpty()) {
                template.copy(templateId = "template_${System.currentTimeMillis()}")
            } else {
                template
            }

            // Remove existing template with same ID
            templates.removeAll { it.templateId == templateWithId.templateId }
            templates.add(templateWithId)

            val json = gson.toJson(templates)
            prefs.edit { putString(KEY_SESSION_TEMPLATES, json) }
            true
        } catch (e: Exception) {
            Log.e("Customization", "Error saving template", e)
            false
        }
    }

    fun getSessionTemplates(): List<SessionTemplate> {
        return try {
            val json = prefs.getString(KEY_SESSION_TEMPLATES, "[]") ?: "[]"
            val type = object : TypeToken<List<SessionTemplate>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getSessionTemplate(templateId: String): SessionTemplate? {
        return getSessionTemplates().find { it.templateId == templateId }
    }

    fun deleteSessionTemplate(templateId: String): Boolean {
        return try {
            val templates = getSessionTemplates().filter { it.templateId != templateId }
            val json = gson.toJson(templates)
            prefs.edit { putString(KEY_SESSION_TEMPLATES, json) }
            true
        } catch (e: Exception) {
            Log.e("Customization", "Error deleting template", e)
            false
        }
    }

    // UI Preferences
    fun saveUIPreferences(preferences: UIPreferences): Boolean {
        return try {
            val json = gson.toJson(preferences)
            prefs.edit { putString(KEY_UI_PREFERENCES, json) }
            true
        } catch (e: Exception) {
            Log.e("Customization", "Error saving UI preferences", e)
            false
        }
    }

    fun getUIPreferences(): UIPreferences {
        return try {
            val json = prefs.getString(KEY_UI_PREFERENCES, null)
            json?.let { gson.fromJson(it, UIPreferences::class.java) } ?: UIPreferences()
        } catch (e: Exception) {
            UIPreferences()
        }
    }

    // Preset Templates
    fun getPresetTemplates(): List<SessionTemplate> {
        return listOf(
            SessionTemplate(
                templateId = "pomodoro",
                name = "Pomodoro",
                duration = 25 * 60 * 1000, // 25 minutes
                goalName = "Deep Work",
                sound = "bell",
                theme = AppTheme.LIGHT,
                description = "Classic Pomodoro technique for focused work sessions",
                tags = listOf("work", "productivity", "pomodoro")
            ),
            SessionTemplate(
                templateId = "meditation",
                name = "Meditation",
                duration = 15 * 60 * 1000, // 15 minutes
                goalName = "Mindfulness",
                sound = "gong",
                theme = AppTheme.NATURE,
                description = "Mindfulness meditation session",
                tags = listOf("meditation", "mindfulness", "relaxation")
            ),
            SessionTemplate(
                templateId = "study",
                name = "Study Session",
                duration = 45 * 60 * 1000, // 45 minutes
                goalName = "Learning",
                sound = "rain",
                theme = AppTheme.BLUE_LIGHT,
                description = "Extended study session with ambient background",
                tags = listOf("study", "learning", "academic")
            ),
            SessionTemplate(
                templateId = "quick_focus",
                name = "Quick Focus",
                duration = 10 * 60 * 1000, // 10 minutes
                goalName = "Quick Task",
                sound = "default",
                theme = AppTheme.SYSTEM,
                description = "Short burst of focused work",
                tags = listOf("quick", "focus", "task")
            )
        )
    }

    fun applyTemplate(templateId: String): SessionTemplate? {
        val template = getSessionTemplate(templateId) ?: getPresetTemplates().find { it.templateId == templateId }
        template?.let {
            setAppTheme(it.theme)
            setTimerSound(getAvailableSounds().find { sound -> sound.soundId == it.sound } ?: getDefaultSound())
        }
        return template
    }

    fun exportSettings(): String {
        val settings = mapOf(
            "theme" to getCurrentTheme().name,
            "timerSound" to getTimerSound(),
            "templates" to getSessionTemplates(),
            "uiPreferences" to getUIPreferences()
        )
        return gson.toJson(settings)
    }

    fun importSettings(settingsJson: String): Boolean {
        return try {
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val settings = gson.fromJson<Map<String, Any>>(settingsJson, type)

            settings["theme"]?.let { themeName ->
                try {
                    setAppTheme(AppTheme.valueOf(themeName.toString()))
                } catch (e: Exception) {
                    Log.e("Customization", "Error applying imported theme", e)
                }
            }

            // Note: More complex import logic would be needed for full implementation
            true
        } catch (e: Exception) {
            Log.e("Customization", "Error importing settings", e)
            false
        }
    }
}