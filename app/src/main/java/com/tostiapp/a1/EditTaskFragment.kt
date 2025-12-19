package com.tostiapp.a1

import android.app.Activity
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.tostiapp.a1.data.WorkEntry
import com.tostiapp.a1.databinding.FragmentEditTaskBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EditTaskFragment : Fragment() {

    private var _binding: FragmentEditTaskBinding? = null
    private val binding get() = _binding!!

    private val workViewModel: WorkViewModel by viewModels()
    private var workEntryId: Int = -1
    private var selectedDate: Date = Date()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            workEntryId = it.getInt("workEntryId")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        applyTheme()

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.entry_types,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.editEntryTypeSpinner.adapter = adapter
        }

        binding.editDateInput.setOnClickListener {
            showDatePickerDialog()
        }

        workViewModel.getWorkEntryById(workEntryId).observe(viewLifecycleOwner) { workEntry ->
            workEntry?.let {
                selectedDate = Date(it.date)
                updateDateInput()
                binding.editStartTime.setText(it.startTime)
                binding.editEndTime.setText(it.endTime)
                binding.editBreakTime.setText(it.breakTime?.toString() ?: "")
                binding.editTaskDescription.setText(it.task)
                binding.editTravelKms.setText(it.travelKms?.toString() ?: "")
                binding.editTravelHours.setText(it.travelHours?.toString() ?: "")
                binding.editCompany.setText(it.company)

                val entryTypes = resources.getStringArray(R.array.entry_types)
                val position = entryTypes.indexOf(it.entryType)
                binding.editEntryTypeSpinner.setSelection(position)
            }
        }

        binding.updateButton.setOnClickListener {
            val startTime = binding.editStartTime.text.toString()
            val endTime = binding.editEndTime.text.toString()
            val task = binding.editTaskDescription.text.toString()
            val entryType = binding.editEntryTypeSpinner.selectedItem.toString()
            val breakTime = binding.editBreakTime.text.toString().toIntOrNull()
            val travelKms = binding.editTravelKms.text.toString().toDoubleOrNull()
            val travelHours = binding.editTravelHours.text.toString().toDoubleOrNull()
            val company = binding.editCompany.text.toString()

            if (startTime.isNotEmpty() && endTime.isNotEmpty()) {
                val updatedWorkEntry = WorkEntry(
                    id = workEntryId,
                    date = selectedDate.time,
                    startTime = startTime,
                    endTime = endTime,
                    breakTime = breakTime,
                    task = task,
                    entryType = entryType,
                    travelKms = travelKms,
                    travelHours = travelHours,
                    company = company
                )
                workViewModel.update(updatedWorkEntry)
                findNavController().popBackStack()
            } else {
                Toast.makeText(context, "Inserisci orario di inizio e fine", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applyTheme()
    }

    private fun applyTheme() {
        val sharedPreferences = requireActivity().getSharedPreferences("app_prefs", Activity.MODE_PRIVATE)
        val textColor = sharedPreferences.getInt("text_color", -1)
        val textSize = sharedPreferences.getInt("text_size", 14)
        ThemeManager.applyTheme(requireView(), textColor, textSize.toFloat())
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(requireContext(), {
            _, selectedYear, selectedMonth, selectedDay ->
            val newCalendar = Calendar.getInstance()
            newCalendar.set(selectedYear, selectedMonth, selectedDay)
            selectedDate = newCalendar.time
            updateDateInput()
        }, year, month, day).show()
    }

    private fun updateDateInput() {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        binding.editDateInput.setText(sdf.format(selectedDate))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
