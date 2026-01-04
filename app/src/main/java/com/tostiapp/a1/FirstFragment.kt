package com.tostiapp.a1

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.tostiapp.a1.data.WorkEntry
import com.tostiapp.a1.databinding.FragmentFirstBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!

    private val workViewModel: WorkViewModel by viewModels()
    private var selectedDate: Date = Date()
    private lateinit var monthFormatter: SimpleDateFormat
    private lateinit var timeFormatter: SimpleDateFormat
    private lateinit var dateFormatter: SimpleDateFormat

    private var monthForPdf: String? = null
    private var isPreviewForPdf: Boolean = false

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                monthForPdf?.let { createPdf(it, isPreviewForPdf) }
            } else {
                Toast.makeText(requireContext(), "Permission denied. Unable to save PDF.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateFormatters()
        setupMenu()
        applyTheme() // Apply theme

        val adapter = TaskListAdapter(
            onDeleteClicked = { workEntry ->
                workViewModel.delete(workEntry)
            },
            onEditClicked = { workEntry ->
                val bundle = bundleOf("workEntryId" to workEntry.id)
                findNavController().navigate(R.id.action_FirstFragment_to_EditTaskFragment, bundle)
            }
        )
        binding.recyclerview.adapter = adapter
        binding.recyclerview.layoutManager = LinearLayoutManager(requireContext())

        // Setup Spinner
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.entry_types,
            android.R.layout.simple_spinner_item
        ).also { spinnerAdapter ->
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.entryTypeSpinner.adapter = spinnerAdapter
        }

        binding.dateInput.setOnClickListener {
            showDatePickerDialog()
        }
        updateDateInput()

        binding.entryTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedItem = parent.getItemAtPosition(position).toString()
                val isVacation = selectedItem == getString(R.string.entry_type_vacation)

                binding.travelKms.isEnabled = !isVacation
                binding.travelHours.isEnabled = !isVacation
                binding.breakTime.isEnabled = !isVacation
                binding.taskDescription.isEnabled = !isVacation
                binding.company.isEnabled = !isVacation

                if (isVacation) {
                    binding.travelKms.setText("")
                    binding.travelHours.setText("")
                    binding.breakTime.setText("")
                    binding.taskDescription.setText("")
                    binding.company.setText("")
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }

        workViewModel.allWorkEntries.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }

        binding.startTime.setOnClickListener {
            showTimePickerDialog(true)
        }

        binding.endTime.setOnClickListener {
            showTimePickerDialog(false)
        }

        binding.saveButton.setOnClickListener {
            val startTime = binding.startTime.text.toString()
            val endTime = binding.endTime.text.toString()
            val task = binding.taskDescription.text.toString()
            val entryType = binding.entryTypeSpinner.selectedItem.toString()
            val breakTime = binding.breakTime.text.toString().toIntOrNull()
            val travelKms = binding.travelKms.text.toString().toDoubleOrNull()
            val travelHours = binding.travelHours.text.toString().toDoubleOrNull()
            val company = binding.company.text.toString()

            if (startTime.isNotEmpty() && endTime.isNotEmpty()) {
                workViewModel.insert(WorkEntry(date = selectedDate.time, startTime = startTime, endTime = endTime, breakTime = breakTime, task = task, entryType = entryType, travelKms = travelKms, travelHours = travelHours, company = company))
                binding.startTime.setText("")
                binding.endTime.setText("")
                binding.breakTime.setText("")
                binding.taskDescription.setText("")
                binding.travelKms.setText("")
                binding.travelHours.setText("")
                binding.company.setText("")
            } else {
                Toast.makeText(requireContext(), getString(R.string.toast_start_end_time_required), Toast.LENGTH_SHORT).show()
            }
        }

        binding.importantMessage.setOnClickListener {
            showFormatSelectionDialog()
        }

        checkDateAndShowMessage()
    }

    override fun onResume() {
        super.onResume()
        updateFormatters()
        applyTheme() // Re-apply theme on resume
    }

    private fun updateFormatters() {
        val currentLocale = resources.configuration.locales[0]
        monthFormatter = SimpleDateFormat("MMMM yyyy", currentLocale)
        timeFormatter = SimpleDateFormat("HH:mm", currentLocale)
        dateFormatter = SimpleDateFormat("dd/MM/yyyy", currentLocale)
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

        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            val newCalendar = Calendar.getInstance()
            newCalendar.set(selectedYear, selectedMonth, selectedDay)
            selectedDate = newCalendar.time
            updateDateInput()
        }, year, month, day).show()
    }

    private fun updateDateInput() {
        binding.dateInput.setText(dateFormatter.format(selectedDate))
    }

    private fun showTimePickerDialog(isStartTime: Boolean) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
            val time = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
            if (isStartTime) {
                binding.startTime.setText(time)
            } else {
                binding.endTime.setText(time)
            }
        }, hour, minute, true).show()
    }

    private fun showFormatSelectionDialog(isPreview: Boolean = false) {
        if (isPreview) {
            showMonthSelectionDialog(getString(R.string.format_pdf), true)
        } else {
            val formats = arrayOf(getString(R.string.format_word), getString(R.string.format_excel), getString(R.string.format_pdf))
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.dialog_title_select_format))
                .setItems(formats) { _, which ->
                    val selectedFormat = formats[which]
                    showMonthSelectionDialog(selectedFormat)
                }
                .show()
        }
    }

    private fun showMonthSelectionDialog(format: String, isPreview: Boolean = false) {
        val workEntries = workViewModel.allWorkEntries.value ?: return
        val months = workEntries.map { monthFormatter.format(it.date) }.distinct().toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.dialog_title_select_month))
            .setItems(months) { _, which ->
                val selectedMonth = months[which]
                when (format) {
                    getString(R.string.format_word) -> generateAndShareTxt(selectedMonth)
                    getString(R.string.format_excel) -> generateAndShareCsv(selectedMonth)
                    getString(R.string.format_pdf) -> requestStoragePermissionAndCreatePdf(selectedMonth, isPreview)
                }
            }
            .show()
    }

    private fun requestStoragePermissionAndCreatePdf(month: String, isPreview: Boolean) {
        this.monthForPdf = month
        this.isPreviewForPdf = isPreview

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            createPdf(month, isPreview)
        } else {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    createPdf(month, isPreview)
                }
                shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                    // Explain to the user why you need the permission
                    Toast.makeText(requireContext(), "Storage permission is required to save PDF files.", Toast.LENGTH_LONG).show()
                    requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }
    }

    private fun generateAndShareTxt(month: String) {
        val workEntries = workViewModel.allWorkEntries.value ?: return
        val entriesForMonth = workEntries.filter { monthFormatter.format(it.date) == month }

        if (entriesForMonth.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.pdf_no_data_message), Toast.LENGTH_SHORT).show()
            return
        }

        val fileName = "Riepilogo_${month.replace(" ", "_")}.txt"
        val file = File(requireContext().cacheDir, fileName)

        try {
            file.printWriter().use { out ->
                out.println(getString(R.string.pdf_summary_title) + " - " + month)
                out.println("--------------------------------------------------")
                entriesForMonth.forEach { entry ->
                    try {
                        val startTime = timeFormatter.parse(entry.startTime)
                        val endTime = timeFormatter.parse(entry.endTime)

                        if (startTime != null && endTime != null) {
                            val breakTime = entry.breakTime ?: 0
                            val diff = endTime.time - startTime.time - (breakTime * 60 * 1000)
                            val hours = diff / (1000 * 60 * 60)
                            val minutes = (diff / (1000 * 60)) % 60

                            out.println("${getString(R.string.pdf_header_date)}: ${dateFormatter.format(entry.date)}")
                            out.println("${getString(R.string.pdf_header_task)}: ${entry.task}")
                            entry.company?.let { if(it.isNotEmpty()) out.println("${getString(R.string.text_company_label)}$it") }
                            out.println("${getString(R.string.txt_label_type)}: ${entry.entryType}")
                            out.println("${getString(R.string.txt_label_hours)}: $hours:$minutes")
                            out.println("${getString(R.string.pdf_header_break)}: ${entry.breakTime} ${getString(R.string.txt_label_minutes)}")
                            entry.travelKms?.let { out.println("${getString(R.string.pdf_header_travel_kms)}: $it") }
                            entry.travelHours?.let { out.println("${getString(R.string.pdf_header_travel_hours)}: $it") }
                            out.println("--------------------------------------------------")
                        }
                    } catch (e: ParseException) {
                        Log.e("FirstFragment", "Error parsing time", e)
                    }
                }
            }

            val uri = FileProvider.getUriForFile(requireContext(), "${requireActivity().packageName}.provider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_file)))
        } catch (e: IOException) {
            Log.e("FirstFragment", "Failed to generate file", e)
            Toast.makeText(requireContext(), getString(R.string.toast_failed_to_generate_file), Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateAndShareCsv(month: String) {
        val workEntries = workViewModel.allWorkEntries.value ?: return
        val entriesForMonth = workEntries.filter { monthFormatter.format(it.date) == month }

        if (entriesForMonth.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.pdf_no_data_message), Toast.LENGTH_SHORT).show()
            return
        }

        val fileName = "Riepilogo_${month.replace(" ", "_")}.csv"
        val file = File(requireContext().cacheDir, fileName)

        var totalWorkHours = 0.0
        var totalLeaveHours = 0.0
        var totalVacationHours = 0.0
        var totalTravelKms = 0.0
        var totalTravelHours = 0.0

        try {
            file.printWriter().use { out ->
                // Header
                out.println(getString(R.string.pdf_summary_title))
                out.println("${getString(R.string.pdf_month_label, month)}\n")

                // Column Titles
                out.println("${getString(R.string.pdf_header_date)};${getString(R.string.pdf_header_task)};${getString(R.string.hint_company)};${getString(R.string.pdf_header_work_hours)};${getString(R.string.pdf_header_leave_hours)};${getString(R.string.pdf_header_vacation_hours)};${getString(R.string.pdf_header_break)};${getString(R.string.pdf_header_travel_kms)};${getString(R.string.pdf_header_travel_hours)}")

                // Data Rows
                entriesForMonth.forEach { entry ->
                    try {
                        val startTime = timeFormatter.parse(entry.startTime)
                        val endTime = timeFormatter.parse(entry.endTime)

                        if (startTime != null && endTime != null) {
                            val breakTime = entry.breakTime ?: 0
                            
                            val endCalendar = Calendar.getInstance()
                            endCalendar.time = endTime
                            if (endTime.before(startTime)) {
                                endCalendar.add(Calendar.DAY_OF_YEAR, 1)
                            }

                            val diff = endCalendar.timeInMillis - startTime.time - (breakTime * 60 * 1000)
                            val positiveDiff = if (diff < 0) 0 else diff
                            val hours = positiveDiff / (1000 * 60 * 60)
                            val minutes = (positiveDiff / (1000 * 60)) % 60
                            val totalHours = hours + minutes / 60.0

                            var workHours = 0.0
                            var leaveHours = 0.0
                            var vacationHours = 0.0

                            when (entry.entryType) {
                                getString(R.string.entry_type_work) -> workHours = totalHours
                                getString(R.string.entry_type_leave) -> leaveHours = totalHours
                                getString(R.string.entry_type_vacation) -> vacationHours = totalHours
                            }

                            totalWorkHours += workHours
                            totalLeaveHours += leaveHours
                            totalVacationHours += vacationHours
                            totalTravelKms += entry.travelKms ?: 0.0
                            totalTravelHours += entry.travelHours ?: 0.0

                            val date = dateFormatter.format(entry.date)

                            val workHoursStr = if (workHours > 0) String.format(Locale.getDefault(), "%.2f", workHours) else ""
                            val leaveHoursStr = if (leaveHours > 0) String.format(Locale.getDefault(), "%.2f", leaveHours) else ""
                            val vacationHoursStr = if (vacationHours > 0) String.format(Locale.getDefault(), "%.2f", vacationHours) else ""
                            val travelKmsStr = if ((entry.travelKms ?: 0.0) > 0) String.format(Locale.getDefault(), "%.2f", entry.travelKms) else ""
                            val travelHoursStr = if ((entry.travelHours ?: 0.0) > 0) String.format(Locale.getDefault(), "%.2f", entry.travelHours) else ""

                            out.println("$date;${entry.task};${entry.company ?: ""};$workHoursStr;$leaveHoursStr;$vacationHoursStr;$breakTime;$travelKmsStr;$travelHoursStr")
                        }
                    } catch (e: ParseException) {
                        Log.e("FirstFragment", "Error parsing time", e)
                    }
                }

                // Totals Row
                val totalWorkHoursStr = String.format(Locale.getDefault(), "%.2f", totalWorkHours)
                val totalLeaveHoursStr = String.format(Locale.getDefault(), "%.2f", totalLeaveHours)
                val totalVacationHoursStr = String.format(Locale.getDefault(), "%.2f", totalVacationHours)
                val totalTravelKmsStr = String.format(Locale.getDefault(), "%.2f", totalTravelKms)
                val totalTravelHoursStr = String.format(Locale.getDefault(), "%.2f", totalTravelHours)
                out.println("\n${getString(R.string.pdf_totals_label)};;;${totalWorkHoursStr};${totalLeaveHoursStr};${totalVacationHoursStr};;${totalTravelKmsStr};${totalTravelHoursStr}")
            }

            val uri = FileProvider.getUriForFile(requireContext(), "${requireActivity().packageName}.provider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_file)))
        } catch (e: IOException) {
            Log.e("FirstFragment", "Failed to generate file", e)
            Toast.makeText(requireContext(), getString(R.string.toast_failed_to_generate_file), Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("NewApi")
    private fun createPdf(month: String, isPreview: Boolean = false) {
        val workEntries = workViewModel.allWorkEntries.value ?: return
        val entriesForMonth = workEntries.filter { monthFormatter.format(it.date) == month }

        if (entriesForMonth.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.pdf_no_data_message), Toast.LENGTH_SHORT).show()
            return
        }

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        var totalWorkHours = 0.0
        var totalLeaveHours = 0.0
        var totalVacationHours = 0.0
        var totalTravelKms = 0.0
        var totalTravelHours = 0.0

        // Draw title
        paint.textSize = 16f
        paint.isFakeBoldText = true
        canvas.drawText(getString(R.string.pdf_summary_title), 40f, 40f, paint)
        paint.isFakeBoldText = false
        paint.textSize = 12f
        canvas.drawText(getString(R.string.pdf_month_label, month), 40f, 60f, paint)

        // Table Headers
        var yPosition = 90f
        paint.textSize = 8f
        paint.isFakeBoldText = true

        val headers = listOf(
            Pair(getString(R.string.pdf_header_date), 65f),
            Pair(getString(R.string.pdf_header_task), 100f),
            Pair(getString(R.string.hint_company), 70f),
            Pair(getString(R.string.pdf_header_work_hours), 50f),
            Pair(getString(R.string.pdf_header_leave_hours), 50f),
            Pair(getString(R.string.pdf_header_vacation_hours), 50f),
            Pair(getString(R.string.pdf_header_break), 40f),
            Pair(getString(R.string.pdf_header_travel_kms), 50f),
            Pair(getString(R.string.pdf_header_travel_hours), 50f)
        )

        var xPosition = 40f
        headers.forEach {
            canvas.drawText(it.first, xPosition, yPosition, paint)
            xPosition += it.second
        }
        yPosition += 10f
        canvas.drawLine(40f, yPosition, 555f, yPosition, paint)
        yPosition += 15f

        // Table Rows
        paint.isFakeBoldText = false

        entriesForMonth.forEach { entry ->
            try {
                val startTime = timeFormatter.parse(entry.startTime)
                val endTime = timeFormatter.parse(entry.endTime)

                if (startTime != null && endTime != null) {
                    val breakTime = entry.breakTime ?: 0
                    
                    val endCalendar = Calendar.getInstance()
                    endCalendar.time = endTime
                    if (endTime.before(startTime)) {
                        endCalendar.add(Calendar.DAY_OF_YEAR, 1)
                    }

                    val diff = endCalendar.timeInMillis - startTime.time - (breakTime * 60 * 1000)
                    val positiveDiff = if (diff < 0) 0 else diff
                    val hours = positiveDiff / (1000 * 60 * 60)
                    val minutes = (positiveDiff / (1000 * 60)) % 60
                    val totalHours = hours + minutes / 60.0

                    var workHours = 0.0
                    var leaveHours = 0.0
                    var vacationHours = 0.0

                    when (entry.entryType) {
                        getString(R.string.entry_type_work) -> workHours = totalHours
                        getString(R.string.entry_type_leave) -> leaveHours = totalHours
                        getString(R.string.entry_type_vacation) -> vacationHours = totalHours
                    }

                    totalWorkHours += workHours
                    totalLeaveHours += leaveHours
                    totalVacationHours += vacationHours
                    totalTravelKms += entry.travelKms ?: 0.0
                    totalTravelHours += entry.travelHours ?: 0.0

                    val date = dateFormatter.format(entry.date)

                    val rowData = listOf(
                        Pair(date, 65f),
                        Pair(entry.task, 100f),
                        Pair(entry.company ?: "", 70f),
                        Pair(if (workHours > 0) String.format(Locale.getDefault(), "%.2f", workHours) else "", 50f),
                        Pair(if (leaveHours > 0) String.format(Locale.getDefault(), "%.2f", leaveHours) else "", 50f),
                        Pair(if (vacationHours > 0) String.format(Locale.getDefault(), "%.2f", vacationHours) else "", 50f),
                        Pair(breakTime.toString(), 40f),
                        Pair(if ((entry.travelKms ?: 0.0) > 0) String.format(Locale.getDefault(), "%.2f", entry.travelKms) else "", 50f),
                        Pair(if ((entry.travelHours ?: 0.0) > 0) String.format(Locale.getDefault(), "%.2f", entry.travelHours) else "", 50f)
                    )

                    xPosition = 40f
                    rowData.forEach {
                        canvas.drawText(it.first, xPosition, yPosition, paint)
                        xPosition += it.second
                    }
                    yPosition += 15f
                }
            } catch (e: ParseException) {
                Log.e("FirstFragment", "Error parsing time", e)
            }
        }
        
        // Totals Row
        yPosition += 10f
        canvas.drawLine(40f, yPosition, 555f, yPosition, paint)
        yPosition += 15f
        paint.isFakeBoldText = true

        val totalWorkHoursStr = String.format(Locale.getDefault(), "%.2f", totalWorkHours)
        val totalLeaveHoursStr = String.format(Locale.getDefault(), "%.2f", totalLeaveHours)
        val totalVacationHoursStr = String.format(Locale.getDefault(), "%.2f", totalVacationHours)
        val totalTravelKmsStr = String.format(Locale.getDefault(), "%.2f", totalTravelKms)
        val totalTravelHoursStr = String.format(Locale.getDefault(), "%.2f", totalTravelHours)

        val totalsData = listOf(
            Pair(getString(R.string.pdf_totals_label), 65f + 100f + 70f),
            Pair(totalWorkHoursStr, 50f),
            Pair(totalLeaveHoursStr, 50f),
            Pair(totalVacationHoursStr, 50f),
            Pair("", 40f), // Skip break total
            Pair(totalTravelKmsStr, 50f),
            Pair(totalTravelHoursStr, 50f)
        )

        xPosition = 40f
        totalsData.forEach {
            canvas.drawText(it.first, xPosition, yPosition, paint)
            xPosition += it.second
        }

        pdfDocument.finishPage(page)

        try {
            val fileName = "Riepilogo_${month.replace(" ", "_")}.pdf"
            val folderName = "Riepilogo Ore"

            if (isPreview) {
                val file = File(requireContext().cacheDir, fileName)
                file.outputStream().use { pdfDocument.writeTo(it) }
                val uri = FileProvider.getUriForFile(requireContext(), "${requireActivity().packageName}.provider", file)
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(viewIntent)
            } else {
                var fileOutputStream: OutputStream? = null

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = requireContext().contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + File.separator + folderName)
                    }
                    val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL)
                    val uri = resolver.insert(collection, contentValues)
                    if (uri != null) {
                        fileOutputStream = resolver.openOutputStream(uri)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val appDir = File(downloadsDir, folderName)
                    if (!appDir.exists()) {
                        appDir.mkdirs()
                    }
                    val file = File(appDir, fileName)
                    fileOutputStream = FileOutputStream(file)
                }

                fileOutputStream?.use {
                    pdfDocument.writeTo(it)
                    Toast.makeText(requireContext(), "PDF salvato in Download/Riepilogo Ore", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: IOException) {
            Log.e("FirstFragment", "Failed to save PDF", e)
            Toast.makeText(requireContext(), getString(R.string.toast_failed_to_save_pdf), Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }

    private fun openSavedFilesViewer() {
        val savedFiles = mutableListOf<Pair<Uri, String>>()
        val folderName = "Riepilogo Ore"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL)
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
            )
            val selection = "(${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR ${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR ${MediaStore.Files.FileColumns.MIME_TYPE} = ?) AND ${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ?"
            val selectionArgs = arrayOf("application/pdf", "text/plain", "text/csv", "%$folderName%")

            try {
                requireContext().contentResolver.query(
                    collection,
                    projection,
                    selection,
                    selectionArgs,
                    "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                    val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val name = cursor.getString(nameColumn)
                        val contentUri: Uri = Uri.withAppendedPath(
                            collection,
                            id.toString()
                        )
                        savedFiles.add(Pair(contentUri, name))
                    }
                }
            } catch (e: Exception) {
                Log.e("FirstFragment", "Error querying MediaStore", e)
                Toast.makeText(requireContext(), "Error finding saved files.", Toast.LENGTH_SHORT).show()
                return
            }
        } else {
            @Suppress("DEPRECATION")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val appDir = File(downloadsDir, folderName)

            if (appDir.exists() && appDir.isDirectory) {
                appDir.listFiles { _, name ->
                    val lowerCaseName = name.lowercase(Locale.getDefault())
                    lowerCaseName.endsWith(".pdf") || lowerCaseName.endsWith(".txt") || lowerCaseName.endsWith(".csv")
                }?.forEach { file ->
                    val fileUri = FileProvider.getUriForFile(requireContext(), "${requireActivity().packageName}.provider", file)
                    savedFiles.add(Pair(fileUri, file.name))
                }
            }
        }

        if (savedFiles.isEmpty()) {
            Toast.makeText(requireContext(), "No saved files found.", Toast.LENGTH_SHORT).show()
            return
        }

        savedFiles.sortByDescending { it.second }

        val fileNames = savedFiles.map { it.second }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Select file to open")
            .setItems(fileNames) { _, which ->
                val selectedFileUri = savedFiles[which].first
                val selectedFileName = savedFiles[which].second

                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    val mimeType = when {
                        selectedFileName.lowercase(Locale.getDefault()).endsWith(".pdf") -> "application/pdf"
                        selectedFileName.lowercase(Locale.getDefault()).endsWith(".csv") -> "text/csv"
                        selectedFileName.lowercase(Locale.getDefault()).endsWith(".txt") -> "text/plain"
                        else -> "*/*"
                    }
                    setDataAndType(selectedFileUri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try {
                    startActivity(viewIntent)
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(requireContext(), "No application found to open this file.", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun setupMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.main_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_create_pdf -> {
                        showFormatSelectionDialog()
                        true
                    }
                    R.id.action_view_pdf -> {
                        openSavedFilesViewer()
                        true
                    }
                    R.id.action_view_all_tasks -> {
                        findNavController().navigate(R.id.action_FirstFragment_to_TaskListFragment)
                        true
                    }
                    R.id.action_settings -> {
                        findNavController().navigate(R.id.action_FirstFragment_to_settingsFragment)
                        true
                    }
                    R.id.action_info -> {
                        findNavController().navigate(R.id.action_FirstFragment_to_infoFragment)
                        true
                    }
                    R.id.menu_preview_pdf -> {
                        showFormatSelectionDialog(isPreview = true)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun checkDateAndShowMessage() {
        val calendar = Calendar.getInstance()
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        if (dayOfMonth >= 25) {
            binding.importantMessage.visibility = View.VISIBLE
        } else {
            binding.importantMessage.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}