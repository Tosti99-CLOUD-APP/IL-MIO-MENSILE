package com.tostiapp.a1

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.tostiapp.a1.databinding.FragmentSecondBinding

class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyTheme()
    }

    override fun onResume() {
        super.onResume()
        // Re-apply theme on resume to catch changes made in SettingsFragment
        applyTheme()
    }

    fun applyTheme() {
        val sharedPreferences = requireActivity().getSharedPreferences("app_prefs", Activity.MODE_PRIVATE)
        val textColor = sharedPreferences.getInt("text_color", -1)
        val textSize = sharedPreferences.getInt("text_size", 14)
        view?.let { ThemeManager.applyTheme(it, textColor, textSize.toFloat()) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}