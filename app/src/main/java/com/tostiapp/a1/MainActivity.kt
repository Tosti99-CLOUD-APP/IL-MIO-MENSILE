package com.tostiapp.a1

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.tostiapp.a1.databinding.ActivityMainBinding

class MainActivity : BaseActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val sharedViewModel: SharedViewModel by viewModels()
    private lateinit var appUpdateManager: AppUpdateManager

    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            popupSnackbarForCompleteUpdate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        applyCustomBackground()

        sharedViewModel.backgroundChanged.observe(this) {
            applyCustomBackground()
        }

        appUpdateManager = AppUpdateManagerFactory.create(this)
        appUpdateManager.registerListener(installStateUpdatedListener)
        checkForUpdate()
    }

    override fun onDestroy() {
        super.onDestroy()
        appUpdateManager.unregisterListener(installStateUpdatedListener)
    }

    override fun onResume() {
        super.onResume()
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                popupSnackbarForCompleteUpdate()
            }
        }
    }

    private fun checkForUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                @Suppress("DEPRECATION")
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.FLEXIBLE,
                    this,
                    REQUEST_CODE_UPDATE
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_UPDATE) {
            if (resultCode != RESULT_OK) {
                Log.e("AppUpdate", "Update flow failed! Result code: $resultCode")
            }
        }
    }

    private fun popupSnackbarForCompleteUpdate() {
        Snackbar.make(
            binding.root,
            "Un aggiornamento è stato scaricato.",
            Snackbar.LENGTH_INDEFINITE
        ).apply {
            setAction("RIAVVIA") { appUpdateManager.completeUpdate() }
            show()
        }
    }

    fun applyCustomBackground() {
        val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val backgroundColor = sharedPreferences.getInt("background_color", -1)
        val backgroundGradient = sharedPreferences.getString("background_gradient", null)
        val backgroundImageUri = sharedPreferences.getString("background_image", null)

        when {
            backgroundImageUri != null -> {
                Glide.with(this)
                    .load(backgroundImageUri.toUri())
                    .into(object : CustomTarget<Drawable>() {
                        override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                            binding.root.background = resource
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {}
                    })
            }
            backgroundGradient != null -> {
                val gradientDrawableId = getGradientDrawableId(backgroundGradient)
                if (gradientDrawableId != 0) {
                    binding.root.background = ContextCompat.getDrawable(this, gradientDrawableId)
                } else {
                    binding.root.setBackgroundColor(Color.WHITE) // Default
                }
            }
            backgroundColor != -1 -> {
                binding.root.setBackgroundColor(backgroundColor)
            }
            else -> {
                 binding.root.setBackgroundColor(Color.WHITE) // Default
            }
        }
    }

    private fun getGradientDrawableId(gradientName: String): Int {
        return when (gradientName) {
            "gradient_1" -> R.drawable.gradient_1
            "gradient_2" -> R.drawable.gradient_2
            "gradient_3" -> R.drawable.gradient_3
            "gradient_4" -> R.drawable.gradient_4
            "gradient_5" -> R.drawable.gradient_5
            "gradient_6" -> R.drawable.gradient_6
            "gradient_7" -> R.drawable.gradient_7
            "gradient_8" -> R.drawable.gradient_8
            "gradient_9" -> R.drawable.gradient_9
            "gradient_10" -> R.drawable.gradient_10
            "gradient_11" -> R.drawable.gradient_11
            "gradient_12" -> R.drawable.gradient_12
            "gradient_13" -> R.drawable.gradient_13
            "gradient_14" -> R.drawable.gradient_14
            "gradient_15" -> R.drawable.gradient_15
            "gradient_16" -> R.drawable.gradient_16
            "gradient_17" -> R.drawable.gradient_17
            "gradient_18" -> R.drawable.gradient_18
            "gradient_19" -> R.drawable.gradient_19
            "gradient_20" -> R.drawable.gradient_20
            else -> 0
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    companion object {
        private const val REQUEST_CODE_UPDATE = 123
    }
}
