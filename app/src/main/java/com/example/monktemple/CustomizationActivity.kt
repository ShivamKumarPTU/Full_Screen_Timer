package com.example.monktemple

import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.monktemple.customization.CustomizationManager
import com.example.monktemple.databinding.ActivityCustomizationBinding
import com.example.monktemple.Utlis.SessionManager
import com.example.monktemple.Utlis.ShowToast
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CustomizationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomizationBinding

    @Inject
    lateinit var customizationManager: CustomizationManager

    @Inject
    lateinit var sessionManager: SessionManager

    private lateinit var backButton: Button
    private lateinit var themeSpinner: Spinner
    private lateinit var soundSpinner: Spinner
    private lateinit var saveButton: Button
    private lateinit var templatesRecyclerView: RecyclerView
    private lateinit var minimalistSwitch: Switch
    private lateinit var animationsSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCustomizationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeViews()
        setupSpinners()
        setupRecyclerView()
        setupClickListeners()
        loadCurrentSettings()
    }

    private fun initializeViews() {
        backButton = binding.backButton
        themeSpinner = binding.themeSpinner
        soundSpinner = binding.soundSpinner
        saveButton = binding.saveButton
        templatesRecyclerView = binding.templatesRecyclerView
        minimalistSwitch = binding.minimalistSwitch
        animationsSwitch = binding.animationsSwitch
    }

    private fun setupSpinners() {
        // Theme Spinner
        val themes = CustomizationManager.AppTheme.values().map { it.name }
        val themeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, themes)
        themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        themeSpinner.adapter = themeAdapter

        // Sound Spinner
        val sounds = customizationManager.getAvailableSounds().map { it.name }
        val soundAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sounds)
        soundAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        soundSpinner.adapter = soundAdapter
    }

    private fun setupRecyclerView() {
        val templates = customizationManager.getPresetTemplates() + customizationManager.getSessionTemplates()
        val adapter = TemplatesAdapter(templates) { template ->
            applyTemplate(template)
        }

        templatesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CustomizationActivity)
            this.adapter = adapter
            setHasFixedSize(true)
        }
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            finish()
        }

        saveButton.setOnClickListener {
            saveSettings()
        }

        binding.testSoundButton.setOnClickListener {
            customizationManager.playTimerSound()
        }

        binding.createTemplateButton.setOnClickListener {
            showCreateTemplateDialog()
        }
    }

    private fun loadCurrentSettings() {
        // Load current theme
        val currentTheme = customizationManager.getCurrentTheme()
        val themePosition = CustomizationManager.AppTheme.values().indexOf(currentTheme)
        if (themePosition >= 0) {
            themeSpinner.setSelection(themePosition)
        }

        // Load current sound
        val currentSound = customizationManager.getTimerSound()
        val sounds = customizationManager.getAvailableSounds()
        val soundPosition = sounds.indexOfFirst { it.soundId == currentSound.soundId }
        if (soundPosition >= 0) {
            soundSpinner.setSelection(soundPosition)
        }

        // Load UI preferences
        val uiPreferences = customizationManager.getUIPreferences()
        minimalistSwitch.isChecked = uiPreferences.minimalistMode
        animationsSwitch.isChecked = uiPreferences.animationLevel > 0
    }

    private fun saveSettings() {
        try {
            // Save theme
            val selectedTheme = CustomizationManager.AppTheme.values()[themeSpinner.selectedItemPosition]
            customizationManager.setAppTheme(selectedTheme)

            // Save sound
            val sounds = customizationManager.getAvailableSounds()
            val selectedSound = sounds[soundSpinner.selectedItemPosition]
            customizationManager.setTimerSound(selectedSound)

            // Save UI preferences
            val uiPreferences = customizationManager.getUIPreferences().copy(
                minimalistMode = minimalistSwitch.isChecked,
                animationLevel = if (animationsSwitch.isChecked) 2 else 0
            )
            customizationManager.saveUIPreferences(uiPreferences)

            // Update session manager
            sessionManager.customTheme = selectedTheme.name
            sessionManager.timerSound = selectedSound.soundId

            ShowToast("Settings saved successfully!")

        } catch (e: Exception) {
            ShowToast("Error saving settings")
            e.printStackTrace()
        }
    }

    private fun applyTemplate(template: CustomizationManager.SessionTemplate) {
        val appliedTemplate = customizationManager.applyTemplate(template.templateId)
        if (appliedTemplate != null) {
            loadCurrentSettings() // Refresh UI to show applied template
            ShowToast("Applied ${template.name} template!")
        } else {
            ShowToast("Error applying template")
        }
    }

    private fun showCreateTemplateDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_template, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.templateNameInput)
        val durationInput = dialogView.findViewById<EditText>(R.id.templateDurationInput)
        val goalInput = dialogView.findViewById<EditText>(R.id.templateGoalInput)

        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Create Session Template")
            .setView(dialogView)
            .setPositiveButton("Create") { dialog, _ ->
                createNewTemplate(
                    nameInput.text.toString(),
                    durationInput.text.toString(),
                    goalInput.text.toString()
                )
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create()

        dialog.show()
    }

    private fun createNewTemplate(name: String, duration: String, goal: String) {
        if (name.isBlank() || duration.isBlank()) {
            ShowToast("Please enter name and duration")
            return
        }

        try {
            val durationMillis = duration.toLong() * 60 * 1000 // Convert minutes to milliseconds

            val newTemplate = CustomizationManager.SessionTemplate(
                name = name,
                duration = durationMillis,
                goalName = goal.ifBlank { "Focus Session" },
                sound = customizationManager.getTimerSound().soundId,
                theme = customizationManager.getCurrentTheme(),
                description = "Custom session template",
                tags = listOf("custom")
            )

            if (customizationManager.saveSessionTemplate(newTemplate)) {
                ShowToast("Template created successfully!")
                setupRecyclerView() // Refresh templates list
            } else {
                ShowToast("Error creating template")
            }
        } catch (e: NumberFormatException) {
            ShowToast("Please enter a valid duration in minutes")
        } catch (e: Exception) {
            ShowToast("Error creating template")
        }
    }
}

// Templates Adapter
class TemplatesAdapter(
    private var templates: List<CustomizationManager.SessionTemplate>,
    private val onTemplateClick: (CustomizationManager.SessionTemplate) -> Unit
) : RecyclerView.Adapter<TemplatesAdapter.TemplateViewHolder>() {

    class TemplateViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.templateName)
        val duration: TextView = itemView.findViewById(R.id.templateDuration)
        val goal: TextView = itemView.findViewById(R.id.templateGoal)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): TemplateViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_template, parent, false)
        return TemplateViewHolder(view)
    }

    override fun onBindViewHolder(holder: TemplateViewHolder, position: Int) {
        val template = templates[position]

        holder.name.text = template.name
        holder.duration.text = "${template.duration / (60 * 1000)} min"
        holder.goal.text = template.goalName

        holder.itemView.setOnClickListener {
            onTemplateClick(template)
        }
    }

    override fun getItemCount(): Int = templates.size
}