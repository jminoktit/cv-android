package com.cvapp

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.switchmaterial.SwitchMaterial
import org.json.JSONArray
import org.json.JSONObject

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private val sectionKeys = listOf("summary", "skills", "projects", "experience", "education", "languages")
    private val sectionLabels = mapOf(
        "summary" to "Summary", "skills" to "Skills", "projects" to "Projects",
        "experience" to "Experience", "education" to "Education", "languages" to "Languages"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportActionBar?.hide()
        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        setupThemes()
        setupFontSizes()
        setupViewModes()
        setupToggles()
        setupVisibility()
        setupSectionOrder()
        setupAddRemoveSection()
        setupReset()
        loadCurrentSettings()
    }

    private fun loadCurrentSettings() {
        val theme = prefs.getString("theme", "dark") ?: "dark"
        val fontSize = prefs.getString("fontSize", "md") ?: "md"
        val viewMode = prefs.getString("viewMode", "standard") ?: "standard"

        findViewById<ChipGroup>(R.id.themeGroup).let { g ->
            when (theme) { "dark" -> g.check(R.id.chipDark); "light" -> g.check(R.id.chipLight); "ocean" -> g.check(R.id.chipOcean) }
        }
        findViewById<ChipGroup>(R.id.fontGroup).let { g ->
            when (fontSize) { "sm" -> g.check(R.id.chipSmall); "md" -> g.check(R.id.chipMedium); "lg" -> g.check(R.id.chipLarge) }
        }
        findViewById<ChipGroup>(R.id.viewModeGroup).let { g ->
            when (viewMode) {
                "standard" -> g.check(R.id.chipStandard)
                "minimal" -> g.check(R.id.chipMinimal)
                "modern" -> g.check(R.id.chipModern)
                "timeline" -> g.check(R.id.chipTimeline)
                "grid" -> g.check(R.id.chipGrid)
                "classic" -> g.check(R.id.chipClassic)
            }
        }
    }

    private fun setupThemes() {
        findViewById<ChipGroup>(R.id.themeGroup).setOnCheckedStateChangeListener { group, _ ->
            val id = group.checkedChipId
            val theme = when (id) {
                R.id.chipLight -> "light"
                R.id.chipOcean -> "ocean"
                else -> "dark"
            }
            prefs.edit().putString("theme", theme).apply()
            setResult(RESULT_OK)
        }
    }

    private fun setupFontSizes() {
        findViewById<ChipGroup>(R.id.fontGroup).setOnCheckedStateChangeListener { group, _ ->
            val id = group.checkedChipId
            val size = when (id) {
                R.id.chipSmall -> "sm"
                R.id.chipLarge -> "lg"
                else -> "md"
            }
            prefs.edit().putString("fontSize", size).apply()
            setResult(RESULT_OK)
        }
    }

    private fun setupViewModes() {
        findViewById<ChipGroup>(R.id.viewModeGroup).setOnCheckedStateChangeListener { group, _ ->
            val id = group.checkedChipId
            val mode = when (id) {
                R.id.chipMinimal -> "minimal"
                R.id.chipModern -> "modern"
                R.id.chipTimeline -> "timeline"
                R.id.chipGrid -> "grid"
                R.id.chipClassic -> "classic"
                else -> "standard"
            }
            prefs.edit().putString("viewMode", mode).apply()
            setResult(RESULT_OK)
        }
    }

    private fun setupToggles() {
        setupToggleById(R.id.toggleCompact, "compact", "Compact Mode")
        setupToggleById(R.id.toggleAvatar, "showAvatar", "Show Avatar")
        setupToggleById(R.id.toggleProjectLinks, "showProjectLinks", "Project Links")
    }

    private fun setupToggleById(viewId: Int, key: String, label: String) {
        val view = findViewById<View>(viewId) ?: return
        val labelView = view.findViewById<TextView>(R.id.toggleLabel)
        val switchView = view.findViewById<SwitchMaterial>(R.id.toggleSwitch)
        if (labelView != null) labelView.text = label
        if (switchView != null) {
            switchView.isChecked = prefs.getBoolean(key, true)
            switchView.setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean(key, isChecked).apply()
                setResult(RESULT_OK)
            }
        }
    }

    private fun setupVisibility() {
        val container = findViewById<LinearLayout>(R.id.visibilityContainer)
        val visibleJson = prefs.getString("visible", "{}") ?: "{}"
        val visible = try { JSONObject(visibleJson) } catch(e: Exception) { JSONObject() }

        sectionKeys.forEach { key ->
            val row = LayoutInflater.from(this).inflate(R.layout.item_toggle, container, false)
            val label = row.findViewById<TextView>(R.id.toggleLabel)
            val switch = row.findViewById<SwitchMaterial>(R.id.toggleSwitch)
            label.text = sectionLabels[key] ?: key
            switch.isChecked = visible.optBoolean(key, true)
            switch.setOnCheckedChangeListener { _, isChecked ->
                visible.put(key, isChecked)
                prefs.edit().putString("visible", visible.toString()).apply()
                setResult(RESULT_OK)
            }
            container.addView(row)
        }
    }

    private fun setupSectionOrder() {
        val container = findViewById<LinearLayout>(R.id.orderContainer)
        renderSectionOrder(container)
    }

    private fun renderSectionOrder(container: LinearLayout) {
        container.removeAllViews()
        val orderJson = prefs.getString("sectionOrder", "") ?: ""
        val order = if (orderJson.isNotEmpty()) {
            try {
                val arr = JSONArray(orderJson)
                (0 until arr.length()).map { arr.getString(it) }
            } catch(e: Exception) { sectionKeys.toList() }
        } else sectionKeys.toList()

        order.forEachIndexed { idx, key ->
            val row = LayoutInflater.from(this).inflate(R.layout.section_order_item, container, false)
            val label = row.findViewById<TextView>(R.id.orderLabel)
            val btnUp = row.findViewById<MaterialButton>(R.id.btnOrderUp)
            val btnDown = row.findViewById<MaterialButton>(R.id.btnOrderDown)

            label.text = "${idx + 1}. ${sectionLabels[key] ?: key}"
            btnUp.isEnabled = idx > 0
            btnDown.isEnabled = idx < order.size - 1

            btnUp.setOnClickListener {
                if (idx > 0) {
                    val mutableOrder = order.toMutableList()
                    val temp = mutableOrder[idx]
                    mutableOrder[idx] = mutableOrder[idx - 1]
                    mutableOrder[idx - 1] = temp
                    saveOrder(mutableOrder)
                    renderSectionOrder(container)
                    setResult(RESULT_OK)
                }
            }
            btnDown.setOnClickListener {
                if (idx < order.size - 1) {
                    val mutableOrder = order.toMutableList()
                    val temp = mutableOrder[idx]
                    mutableOrder[idx] = mutableOrder[idx + 1]
                    mutableOrder[idx + 1] = temp
                    saveOrder(mutableOrder)
                    renderSectionOrder(container)
                    setResult(RESULT_OK)
                }
            }
            container.addView(row)
        }
    }

    private fun saveOrder(order: List<String>) {
        val arr = JSONArray()
        order.forEach { arr.put(it) }
        prefs.edit().putString("sectionOrder", arr.toString()).apply()
    }

    private fun setupAddRemoveSection() {
        findViewById<MaterialButton>(R.id.btnAddSection).setOnClickListener {
            val et = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etNewSection)
            val name = et.text?.toString()?.trim() ?: return@setOnClickListener
            if (name.isEmpty()) { Toast.makeText(this, "Enter a section name", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            addSection(name)
            et.setText("")
            Toast.makeText(this, "Section '$name' added", Toast.LENGTH_SHORT).show()
        }

        findViewById<MaterialButton>(R.id.btnRemoveSection).setOnClickListener {
            removeLastSection()
        }
    }

    private fun addSection(name: String) {
        val orderJson = prefs.getString("sectionOrder", "") ?: ""
        val order = if (orderJson.isNotEmpty()) {
            try {
                val arr = JSONArray(orderJson); (0 until arr.length()).map { arr.getString(it) }
            } catch(e: Exception) { sectionKeys.toMutableList() }
        } else sectionKeys.toMutableList()

        val newKey = name.lowercase().replace(" ", "_")
        if (!order.contains(newKey)) {
            val mutableOrder = order.toMutableList()
            mutableOrder.add(newKey)
            saveOrder(mutableOrder)

            val visibleJson = prefs.getString("visible", "{}") ?: "{}"
            val visible = try { JSONObject(visibleJson) } catch(e: Exception) { JSONObject() }
            visible.put(newKey, true)
            prefs.edit().putString("visible", visible.toString()).apply()

            // Reload order UI
            renderSectionOrder(findViewById(R.id.orderContainer))
            setResult(RESULT_OK)
        }
    }

    private fun removeLastSection() {
        val orderJson = prefs.getString("sectionOrder", "") ?: ""
        val order = if (orderJson.isNotEmpty()) {
            try {
                val arr = JSONArray(orderJson); (0 until arr.length()).map { arr.getString(it) }
            } catch(e: Exception) { sectionKeys.toList() }
        } else sectionKeys.toList()

        if (order.size <= 1) {
            Toast.makeText(this, "Cannot remove last section", Toast.LENGTH_SHORT).show()
            return
        }

        val removed = order.last()
        if (removed in sectionKeys) {
            Toast.makeText(this, "Cannot remove default section", Toast.LENGTH_SHORT).show()
            return
        }

        val mutableOrder = order.toMutableList()
        mutableOrder.removeAt(mutableOrder.lastIndex)
        saveOrder(mutableOrder)
        renderSectionOrder(findViewById(R.id.orderContainer))
        setResult(RESULT_OK)
        Toast.makeText(this, "Section '$removed' removed", Toast.LENGTH_SHORT).show()
    }

    private fun setupReset() {
        findViewById<MaterialButton>(R.id.btnReset).setOnClickListener {
            prefs.edit().clear().apply()
            loadCurrentSettings()
            // Re-render visibility toggles
            findViewById<LinearLayout>(R.id.visibilityContainer).removeAllViews()
            setupVisibility()
            renderSectionOrder(findViewById(R.id.orderContainer))
            setResult(RESULT_OK)
            Toast.makeText(this, "Reset to defaults", Toast.LENGTH_SHORT).show()
        }
    }
}
