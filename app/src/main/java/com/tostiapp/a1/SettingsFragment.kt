package com.tostiapp.a1

import android.app.Activity
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.tostiapp.a1.databinding.FragmentSettingsBinding
import com.tostiapp.a1.util.LanguageManager

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedPreferences: SharedPreferences
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var languageManager: LanguageManager

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                requireContext().contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
            sharedPreferences.edit {
                putString("background_image", it.toString())
                remove("background_color")
                remove("background_gradient")
            }
            sharedViewModel.notifyBackgroundChanged()
            showConfirmationDialog()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        sharedPreferences = requireActivity().getSharedPreferences("app_prefs", Activity.MODE_PRIVATE)
        languageManager = LanguageManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        applyTheme()

        binding.changeBackgroundColorButton.setOnClickListener {
            showColorPaletteDialog(true)
        }

        binding.changeBackgroundImageButton.setOnClickListener {
            selectImageLauncher.launch("image/*")
        }

        binding.changeTextColorButton.setOnClickListener {
            showColorPaletteDialog(false)
        }

        val currentTextSize = sharedPreferences.getInt("text_size", 14)
        binding.textSizeSeekbar.progress = currentTextSize - 12

        binding.textSizeSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    val textSize = it.progress + 12
                    sharedPreferences.edit { putInt("text_size", textSize) }
                    applyTheme()
                }
            }
        })

        binding.changeLanguageButton.setOnClickListener {
            showLanguageSelectionDialog()
        }
    }

    private fun showColorPaletteDialog(isBackground: Boolean) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_color_palette, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle("Scegli un colore")
            .setNegativeButton("Annulla", null)
            .create()

        val palette = dialogView as ViewGroup
        for (i in 0 until palette.childCount) {
            val child = palette.getChildAt(i)
            val tag = child.tag as String
            if (!isBackground && !tag.startsWith("#")) {
                child.visibility = View.GONE
            }
            child.setOnClickListener { 
                if (isBackground) {
                    if (tag.startsWith("#")) {
                        val color = tag.toColorInt()
                        sharedPreferences.edit {
                            putInt("background_color", color)
                            remove("background_gradient")
                            remove("background_image")
                        }
                    } else {
                        sharedPreferences.edit {
                            putString("background_gradient", tag)
                            remove("background_color")
                            remove("background_image")
                        }
                    }
                    sharedViewModel.notifyBackgroundChanged()
                    showConfirmationDialog()
                } else {
                    if (tag.startsWith("#")) {
                        val color = tag.toColorInt()
                        sharedPreferences.edit { putInt("text_color", color) }
                        applyTheme()
                    }
                }
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    fun applyTheme() {
        val textColor = sharedPreferences.getInt("text_color", -1)
        val textSize = sharedPreferences.getInt("text_size", 14)
        view?.let { ThemeManager.applyTheme(it, textColor, textSize.toFloat()) }
    }

    private fun showConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Nuovo Sfondo")
            .setMessage("Vuoi mantenere questo sfondo o ripristinare quello predefinito?")
            .setPositiveButton("Mantieni") { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton("Ripristina") { dialog, _ ->
                sharedPreferences.edit {
                    remove("background_image")
                    remove("background_gradient")
                    putInt("background_color", Color.WHITE)
                }
                sharedViewModel.notifyBackgroundChanged()
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun showLanguageSelectionDialog() {
        val languages = resources.getStringArray(R.array.languages)
        val currentLanguage = LocaleHelper.getLanguage(requireContext())

        AlertDialog.Builder(requireContext())
            .setTitle("Seleziona Lingua")
            .setItems(languages) { _, which ->
                val selectedLang = when (languages[which]) {
                    "English" -> "en"
                    "Français" -> "fr"
                    "Deutsch" -> "de"
                    else -> "it"
                }
                if (currentLanguage != selectedLang) {
                    showLanguageChangeConfirmationDialog(selectedLang)
                }
            }
            .show()
    }

    private fun showLanguageChangeConfirmationDialog(selectedLang: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Cambia lingua")
            .setMessage("L'applicazione scaricherà la lingua selezionata e verrà riavviata. Continuare?")
            .setPositiveButton("OK") { _, _ ->
                languageManager.downloadLanguage(selectedLang) {
                    LocaleHelper.setLocale(requireContext(), selectedLang)
                    requireActivity().recreate()
                }
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
