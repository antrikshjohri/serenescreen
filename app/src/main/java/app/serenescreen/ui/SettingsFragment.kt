package app.serenescreen.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.Toast
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import app.serenescreen.BuildConfig
import app.serenescreen.MainViewModel
import app.serenescreen.R
import app.serenescreen.data.Constants
import app.serenescreen.data.Prefs
import app.serenescreen.databinding.FragmentSettingsBinding
import app.serenescreen.helper.*
import app.serenescreen.listener.DeviceAdmin
import com.google.firebase.analytics.FirebaseAnalytics

class SettingsFragment : Fragment(), View.OnClickListener, View.OnLongClickListener {
    companion object {
        private const val SLIDER_LABEL_FLOATING = 0
        private const val SLIDER_LABEL_GONE = 2
        private const val SLIDER_LABEL_VISIBLE = 3
    }

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var deviceManager: DevicePolicyManager
    private lateinit var componentName: ComponentName

    private lateinit var firebaseAnalytics: FirebaseAnalytics // Declare FirebaseAnalytics instance


    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())
        viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: throw Exception("Invalid Activity")
        viewModel.isSereneScreenDefault()

        deviceManager = context?.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        componentName = ComponentName(requireContext(), DeviceAdmin::class.java)
        checkAdminPermission()

        // Initialize Firebase Analytics
        firebaseAnalytics = FirebaseAnalytics.getInstance(requireContext())


        binding.homeAppsNum.text = prefs.homeAppsNum.toString()
        binding.homeAppsNumSlider.value = prefs.homeAppsNum.toFloat()
        populateKeyboardText()
        //populateLockSettings()
        //populateWallpaperText()
        populateAppThemeText()
        populateTextSize()
        populateAlignment()
        populateStatusBar()
        populateDateTime()
        populateSwipeApps()
        populateSwipeDownAction()
        populateActionHints()
        initClickListeners()
        initSliderListeners()
        initObservers()
        applyWindowInsets()
    }

    override fun onClick(view: View) {
        hideHomeAppsSelector()
        binding.dateTimeSelectLayout.visibility = View.GONE
        binding.appThemeSelectLayout.visibility = View.GONE
        binding.swipeDownSelectLayout.visibility = View.GONE
        binding.textSizesLayout.visibility = View.GONE
        if (view.id != R.id.alignmentBottom)
            binding.alignmentSelectLayout.visibility = View.GONE

        when (view.id) {
            R.id.serenescreenHiddenApps -> showHiddenApps()
            R.id.appInfo -> openAppInfo(requireContext(), android.os.Process.myUserHandle(), BuildConfig.APPLICATION_ID)
            R.id.setLauncher, R.id.setLauncherRow -> viewModel.resetDefaultLauncherApp(requireContext())
            R.id.toggleLock -> toggleLockMode()
            R.id.autoShowKeyboardSwitch, R.id.autoShowKeyboardRow -> toggleKeyboardText()
            R.id.homeAppsNum, R.id.homeAppsNumRow -> {
                showHomeAppsSelector()
            }
            R.id.dailyWallpaperUrl -> requireContext().openUrl(prefs.dailyWallpaperUrl)
            R.id.dailyWallpaper -> toggleDailyWallpaperUpdate()
            R.id.alignment, R.id.alignmentRow -> showAlignmentDialog()
            R.id.alignmentLeft -> viewModel.updateHomeAlignment(Gravity.START)
            R.id.alignmentCenter -> viewModel.updateHomeAlignment(Gravity.CENTER)
            R.id.alignmentRight -> viewModel.updateHomeAlignment(Gravity.END)
            R.id.alignmentBottom -> updateHomeBottomAlignment()
            R.id.statusBarSwitch, R.id.statusBarRow -> toggleStatusBar()
            R.id.dateTimeRow, R.id.dateTimeMode -> showDateTimeModeDialog()
            R.id.dateTimeSwitch -> toggleDateTimeEnabled()
            R.id.dateTimeOn -> toggleDateTime(Constants.DateTime.ON)
            R.id.dateTimeOff -> toggleDateTime(Constants.DateTime.OFF)
            R.id.dateOnly -> toggleDateTime(Constants.DateTime.DATE_ONLY)
            R.id.appThemeText, R.id.appThemeRow -> showThemeDialog()
            R.id.themeLight -> updateTheme(AppCompatDelegate.MODE_NIGHT_NO)
            R.id.themeDark -> updateTheme(AppCompatDelegate.MODE_NIGHT_YES)
            R.id.themeSystem -> updateTheme(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            R.id.textSizeValue, R.id.textSizeRow -> showTextSizeDialog()
            R.id.actionAccessibility -> openAccessibilityService()
            R.id.closeAccessibility -> toggleAccessibilityVisibility(false)
            //R.id.notWorking -> requireContext().openUrl(Constants.URL_DOUBLE_TAP)

            R.id.textSize1 -> updateTextSizeScale(Constants.TextSize.ONE)
            R.id.textSize2 -> updateTextSizeScale(Constants.TextSize.TWO)
            R.id.textSize3 -> updateTextSizeScale(Constants.TextSize.THREE)
            R.id.textSize4 -> updateTextSizeScale(Constants.TextSize.FOUR)
            R.id.textSize5 -> updateTextSizeScale(Constants.TextSize.FIVE)
            R.id.textSize6 -> updateTextSizeScale(Constants.TextSize.SIX)
            R.id.textSize7 -> updateTextSizeScale(Constants.TextSize.SEVEN)

            R.id.swipeLeftApp, R.id.swipeLeftRow -> showAppListIfEnabled(Constants.FLAG_SET_SWIPE_LEFT_APP)
            R.id.swipeRightApp, R.id.swipeRightRow -> showAppListIfEnabled(Constants.FLAG_SET_SWIPE_RIGHT_APP)
            R.id.swipeDownAction, R.id.swipeDownRow -> showSwipeDownDialog()
            R.id.notifications -> updateSwipeDownAction(Constants.SwipeDownAction.NOTIFICATIONS)
            R.id.search -> updateSwipeDownAction(Constants.SwipeDownAction.SEARCH)

            //R.id.rate -> requireContext().openUrl(Constants.URL_SERENESCREEN_PLAY_STORE)
            R.id.rate, R.id.rateRow -> {
                //prefs.rateClicked = true
                requireActivity().rateApp()
                // Log an event when the button is clicked
                val bundle = Bundle().apply {
                    putString(FirebaseAnalytics.Param.ITEM_ID, "rate_on_play_store_button")
                    putString(FirebaseAnalytics.Param.ITEM_NAME, "Rate us on Play Store")
                    putString(FirebaseAnalytics.Param.CONTENT_TYPE, "button")
                }
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)

            }
        }
    }

    override fun onLongClick(view: View): Boolean {
        when (view.id) {
            R.id.alignment -> {
                prefs.appLabelAlignment = prefs.homeAlignment
                findNavController().navigate(R.id.action_settingsFragment_to_appListFragment)
            }
            R.id.dailyWallpaper -> removeWallpaper()
            R.id.appThemeText -> {
                binding.appThemeSelectLayout.visibility = View.VISIBLE
                binding.themeSystem.visibility = View.VISIBLE
            }
            R.id.swipeLeftApp -> toggleSwipeLeft()
            R.id.swipeRightApp -> toggleSwipeRight()
            R.id.toggleLock -> startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        return true
    }

    private fun initClickListeners() {
        binding.serenescreenHiddenApps.setOnClickListener(this)
        binding.scrollLayout.setOnClickListener(this)
        binding.appInfo.setOnClickListener(this)
        binding.setLauncher.setOnClickListener(this)
        binding.root.findViewById<View>(R.id.setLauncherRow)?.setOnClickListener(this)
        binding.autoShowKeyboardSwitch?.setOnClickListener(this)
        binding.root.findViewById<View>(R.id.autoShowKeyboardRow)?.setOnClickListener(this)
        //binding.toggleLock.setOnClickListener(this)
        binding.homeAppsNum.setOnClickListener(this)
        binding.root.findViewById<View>(R.id.homeAppsNumRow)?.setOnClickListener(this)
        //binding.dailyWallpaperUrl.setOnClickListener(this)
        //binding.dailyWallpaper.setOnClickListener(this)
        binding.alignment.setOnClickListener(this)
        binding.root.findViewById<View>(R.id.alignmentRow)?.setOnClickListener(this)
        binding.alignmentLeft.setOnClickListener(this)
        binding.alignmentCenter.setOnClickListener(this)
        binding.alignmentRight.setOnClickListener(this)
        binding.alignmentBottom.setOnClickListener(this)
        binding.statusBarSwitch?.setOnClickListener(this)
        binding.root.findViewById<View>(R.id.statusBarRow)?.setOnClickListener(this)
        binding.dateTimeSwitch?.setOnClickListener(this)
        binding.dateTimeMode?.setOnClickListener(this)
        binding.root.findViewById<View>(R.id.dateTimeRow)?.setOnClickListener(this)
        binding.dateTimeOn.setOnClickListener(this)
        binding.dateTimeOff.setOnClickListener(this)
        binding.dateOnly.setOnClickListener(this)
        binding.swipeLeftApp.setOnClickListener(this)
        binding.root.findViewById<View>(R.id.swipeLeftRow)?.setOnClickListener(this)
        binding.swipeRightApp.setOnClickListener(this)
        binding.root.findViewById<View>(R.id.swipeRightRow)?.setOnClickListener(this)
        binding.swipeDownAction.setOnClickListener(this)
        binding.root.findViewById<View>(R.id.swipeDownRow)?.setOnClickListener(this)
        binding.search.setOnClickListener(this)
        binding.notifications.setOnClickListener(this)
        binding.appThemeText.setOnClickListener(this)
        binding.root.findViewById<View>(R.id.appThemeRow)?.setOnClickListener(this)
        binding.themeLight.setOnClickListener(this)
        binding.themeDark.setOnClickListener(this)
        binding.themeSystem.setOnClickListener(this)
        binding.textSizeValue.setOnClickListener(this)
        binding.root.findViewById<View>(R.id.textSizeRow)?.setOnClickListener(this)
        binding.actionAccessibility.setOnClickListener(this)
        binding.closeAccessibility.setOnClickListener(this)
        binding.notWorking.setOnClickListener(this)

        binding.rate.setOnClickListener(this)
        binding.root.findViewById<View>(R.id.rateRow)?.setOnClickListener(this)

        binding.textSize1.setOnClickListener(this)
        binding.textSize2.setOnClickListener(this)
        binding.textSize3.setOnClickListener(this)
        binding.textSize4.setOnClickListener(this)
        binding.textSize5.setOnClickListener(this)
        binding.textSize6.setOnClickListener(this)
        binding.textSize7.setOnClickListener(this)

        //binding.dailyWallpaper.setOnLongClickListener(this)
        binding.alignment.setOnLongClickListener(this)
        binding.appThemeText.setOnLongClickListener(this)
        binding.swipeLeftApp.setOnLongClickListener(this)
        binding.swipeRightApp.setOnLongClickListener(this)
        //binding.toggleLock.setOnLongClickListener(this)
    }

    private fun initSliderListeners() {
        binding.homeAppsNumSlider.setLabelFormatter { value ->
            value.toInt().toString()
        }
        binding.homeAppsNumSlider.setLabelBehavior(SLIDER_LABEL_GONE)
        binding.homeAppsNumSlider.addOnChangeListener { _: Slider, value: Float, fromUser: Boolean ->
            if (!fromUser) return@addOnChangeListener
            updateHomeAppsNum(value.toInt(), dismissSelector = false)
        }
        binding.homeAppsNumSlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                slider.setLabelBehavior(SLIDER_LABEL_VISIBLE)
            }

            override fun onStopTrackingTouch(slider: Slider) {
                slider.setLabelBehavior(SLIDER_LABEL_FLOATING)
            }
        })
    }

    private fun showHomeAppsSelector() {
        val context = requireContext()
        val container = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(dpToPx(24), dpToPx(12), dpToPx(24), 0)
        }
        val valueLabel = android.widget.TextView(context).apply {
            text = prefs.homeAppsNum.toString()
            setTextColor(context.getColorFromAttr(R.attr.primaryColor))
            textSize = 16f
        }
        val slider = Slider(context).apply {
            valueFrom = 0f
            valueTo = 16f
            stepSize = 1f
            value = prefs.homeAppsNum.toFloat()
            setLabelFormatter { value -> value.toInt().toString() }
            setLabelBehavior(SLIDER_LABEL_VISIBLE)
            addOnChangeListener { _, value, fromUser ->
                if (!fromUser) return@addOnChangeListener
                valueLabel.text = value.toInt().toString()
                updateHomeAppsNum(value.toInt(), dismissSelector = false)
            }
        }
        container.addView(valueLabel)
        container.addView(slider)
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.apps_on_home_screen)
            .setView(container)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun hideHomeAppsSelector() {
        binding.homeAppsNumSlider.clearFocus()
        binding.homeAppsNumSlider.setLabelBehavior(SLIDER_LABEL_GONE)
        binding.appsNumSelectLayout.visibility = View.GONE
        binding.appsNumSelectLayout.alpha = 1f
        binding.appsNumSelectLayout.translationY = 0f
    }

    private fun showTextSizeDialog() {
        val context = requireContext()
        val container = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(dpToPx(24), dpToPx(12), dpToPx(24), 0)
        }
        val valueLabel = android.widget.TextView(context).apply {
            text = getTextSizeStep().toString()
            setTextColor(context.getColorFromAttr(R.attr.primaryColor))
            textSize = 16f
        }
        var selectedStep = getTextSizeStep()
        val slider = Slider(context).apply {
            valueFrom = 1f
            valueTo = 7f
            stepSize = 1f
            value = selectedStep.toFloat()
            setLabelFormatter { value -> value.toInt().toString() }
            setLabelBehavior(SLIDER_LABEL_VISIBLE)
            addOnChangeListener { _, value, fromUser ->
                if (!fromUser) return@addOnChangeListener
                selectedStep = value.toInt()
                valueLabel.text = selectedStep.toString()
            }
        }
        container.addView(valueLabel)
        container.addView(slider)
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.text_size)
            .setView(container)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                updateTextSizeScale(getTextSizeScaleForStep(selectedStep))
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showThemeDialog() {
        val labels = arrayOf(
            getString(R.string.light),
            getString(R.string.dark),
            getString(R.string.system_default)
        )
        val values = intArrayOf(
            AppCompatDelegate.MODE_NIGHT_NO,
            AppCompatDelegate.MODE_NIGHT_YES,
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        )
        val selectedIndex = values.indexOf(prefs.appTheme).coerceAtLeast(0)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.theme_short)
            .setSingleChoiceItems(labels, selectedIndex) { dialog, which ->
                updateTheme(values[which])
                dialog.dismiss()
            }
            .show()
    }

    private fun showDateTimeModeDialog() {
        val labels = arrayOf(
            getString(R.string.date_time_full),
            getString(R.string.date_only)
        )
        val values = intArrayOf(
            Constants.DateTime.ON,
            Constants.DateTime.DATE_ONLY
        )
        val selectedIndex = values.indexOf(getSelectedDateTimeMode()).coerceAtLeast(0)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.show_date_time_amp)
            .setSingleChoiceItems(labels, selectedIndex) { dialog, which ->
                toggleDateTime(values[which])
                dialog.dismiss()
            }
            .show()
    }

    private fun showSwipeDownDialog() {
        val labels = arrayOf(
            getString(R.string.notifications),
            getString(R.string.search)
        )
        val values = intArrayOf(
            Constants.SwipeDownAction.NOTIFICATIONS,
            Constants.SwipeDownAction.SEARCH
        )
        val selectedIndex = values.indexOf(prefs.swipeDownAction).coerceAtLeast(0)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.swipe_down_short)
            .setSingleChoiceItems(labels, selectedIndex) { dialog, which ->
                updateSwipeDownAction(values[which])
                dialog.dismiss()
            }
            .show()
    }

    private fun showAlignmentDialog() {
        val labels = arrayOf(
            getString(R.string.left),
            getString(R.string.center),
            getString(R.string.right),
            if (prefs.homeBottomAlignment) getString(R.string.bottom_on) else getString(R.string.bottom_off)
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.home_layout_alignment)
            .setItems(labels) { dialog, which ->
                when (which) {
                    0 -> viewModel.updateHomeAlignment(Gravity.START)
                    1 -> viewModel.updateHomeAlignment(Gravity.CENTER)
                    2 -> viewModel.updateHomeAlignment(Gravity.END)
                    3 -> updateHomeBottomAlignment()
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun initObservers() {
        if (prefs.firstSettingsOpen) {
            prefs.firstSettingsOpen = false
        }
        viewModel.isSereneScreenDefault.observe(viewLifecycleOwner) {
            if (it) {
                binding.setLauncher.text = getString(R.string.default_label)
                prefs.toShowHintCounter = prefs.toShowHintCounter + 1
            } else {
                binding.setLauncher.text = getString(R.string.not_set)
            }
        }
        viewModel.homeAppAlignment.observe(viewLifecycleOwner) {
            populateAlignment()
        }
        viewModel.updateSwipeApps.observe(viewLifecycleOwner) {
            populateSwipeApps()
        }
    }

    private fun applyWindowInsets() {
        val currentBinding = _binding ?: return
        val contentView = currentBinding.root.findViewById<View>(R.id.settingsContent) ?: currentBinding.scrollLayout
        val initialScrollTop = currentBinding.scrollLayout.paddingTop
        val initialContentBottom = contentView.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(currentBinding.mainActivityLayout) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            currentBinding.scrollLayout.updatePadding(
                top = initialScrollTop + systemBars.top
            )
            contentView.updatePadding(
                bottom = initialContentBottom + systemBars.bottom + dpToPx(24)
            )

            insets
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun toggleSwipeLeft() {
        prefs.swipeLeftEnabled = !prefs.swipeLeftEnabled
        if (prefs.swipeLeftEnabled) {
            binding.swipeLeftApp.setTextColor(requireContext().getColorFromAttr(R.attr.primaryColor))
            requireContext().showToast("Swipe left app enabled")
        } else {
            binding.swipeLeftApp.setTextColor(requireContext().getColorFromAttr(R.attr.primaryColorTrans50))
            requireContext().showToast("Swipe left app disabled")
        }
    }

    private fun toggleSwipeRight() {
        prefs.swipeRightEnabled = !prefs.swipeRightEnabled
        if (prefs.swipeRightEnabled) {
            binding.swipeRightApp.setTextColor(requireContext().getColorFromAttr(R.attr.primaryColor))
            requireContext().showToast("Swipe right app enabled")
        } else {
            binding.swipeRightApp.setTextColor(requireContext().getColorFromAttr(R.attr.primaryColorTrans50))
            requireContext().showToast("Swipe right app disabled")
        }
    }

    private fun toggleStatusBar() {
        prefs.showStatusBar = !prefs.showStatusBar
        populateStatusBar()
    }

    private fun populateStatusBar() {
        if (prefs.showStatusBar) {
            showStatusBar()
        } else {
            hideStatusBar()
        }
        binding.statusBarSwitch?.isChecked = prefs.showStatusBar
    }

    private fun toggleDateTime(selected: Int) {
        prefs.dateTimeVisibility = selected
        if (selected != Constants.DateTime.OFF) {
            prefs.dateTimeLastVisibleMode = selected
        }
        populateDateTime()
        viewModel.toggleDateTime()
    }

    private fun toggleDateTimeEnabled() {
        val nextVisibility = if (prefs.dateTimeVisibility == Constants.DateTime.OFF) {
            getSelectedDateTimeMode()
        } else {
            Constants.DateTime.OFF
        }
        toggleDateTime(nextVisibility)
    }

    private fun getSelectedDateTimeMode(): Int {
        return if (prefs.dateTimeVisibility == Constants.DateTime.OFF) {
            prefs.dateTimeLastVisibleMode
        } else {
            prefs.dateTimeVisibility
        }
    }

    private fun populateDateTime() {
        binding.dateTimeSwitch?.isChecked = prefs.dateTimeVisibility != Constants.DateTime.OFF
        binding.dateTimeMode?.text = getString(
            when (getSelectedDateTimeMode()) {
                Constants.DateTime.DATE_ONLY -> R.string.date_only
                else -> R.string.date_time_full
            }
        )
    }

    private fun showStatusBar() {
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            requireActivity().window.insetsController?.show(WindowInsets.Type.statusBars())
        else
            @Suppress("DEPRECATION", "InlinedApi")
            requireActivity().window.decorView.apply {
                systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            }
    }

    private fun hideStatusBar() {
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            requireActivity().window.insetsController?.hide(WindowInsets.Type.statusBars())
        else {
            @Suppress("DEPRECATION")
            requireActivity().window.decorView.apply {
                systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE or View.SYSTEM_UI_FLAG_FULLSCREEN
            }
        }
    }

    private fun showHiddenApps() {
        if (prefs.hiddenApps.isEmpty()) {
            requireContext().showToast("No hidden apps")
            return
        }
        viewModel.getHiddenApps()
        findNavController().navigate(
            R.id.action_settingsFragment_to_appListFragment,
            bundleOf(Constants.Key.FLAG to Constants.FLAG_HIDDEN_APPS)
        )
    }

    private fun checkAdminPermission() {
        val isAdmin: Boolean = deviceManager.isAdminActive(componentName)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
            prefs.lockModeOn = isAdmin
    }

    private fun toggleAccessibilityVisibility(show: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            binding.notWorking.visibility = View.VISIBLE
        if (isAccessServiceEnabled(requireContext()))
            binding.actionAccessibility.text = getString(R.string.disable)
        binding.accessibilityLayout.isVisible = show
        binding.scrollView.animateAlpha(if (show) 0.5f else 1f)
    }

    private fun openAccessibilityService() {
        toggleAccessibilityVisibility(false)
        // prefs.lockModeOn = true
        //populateLockSettings()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun toggleLockMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            toggleAccessibilityVisibility(true)
            if (prefs.lockModeOn) {
                prefs.lockModeOn = false
                removeActiveAdmin()
            }
        } else {
            val isAdmin: Boolean = deviceManager.isAdminActive(componentName)
            if (isAdmin) {
                removeActiveAdmin("Admin permission removed.")
                prefs.lockModeOn = false
            } else {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                intent.putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    getString(R.string.admin_permission_message)
                )
                requireActivity().startActivityForResult(intent, Constants.REQUEST_CODE_ENABLE_ADMIN)
            }
        }
        //populateLockSettings()
    }

    private fun removeActiveAdmin(toastMessage: String? = null) {
        try {
            deviceManager.removeActiveAdmin(componentName) // for backward compatibility
            requireContext().showToast(toastMessage)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeWallpaper() {
        setPlainWallpaper(requireContext(), android.R.color.black)
        if (!prefs.dailyWallpaper) return
        prefs.dailyWallpaper = false
        //populateWallpaperText()
        viewModel.cancelWallpaperWorker()
    }

    private fun toggleDailyWallpaperUpdate() {
        prefs.dailyWallpaper = !prefs.dailyWallpaper
        //populateWallpaperText()
        if (prefs.dailyWallpaper) {
            viewModel.setWallpaperWorker()
            showWallpaperToasts()
        } else viewModel.cancelWallpaperWorker()
    }

    private fun showWallpaperToasts() {
        if (isSereneScreenDefault(requireContext()))
            requireContext().showToast("Your wallpaper will update shortly")
        else
            requireContext().showToast("SereneScreen is not default launcher.\nDaily wallpaper update may fail.", Toast.LENGTH_LONG)
    }

    private fun updateHomeAppsNum(num: Int, dismissSelector: Boolean = true) {
        prefs.homeAppsNum = num
        binding.homeAppsNum.text = num.toString()
        if (binding.homeAppsNumSlider.value.toInt() != num) {
            binding.homeAppsNumSlider.value = num.toFloat()
        }
        if (dismissSelector) {
            binding.appsNumSelectLayout.animate()
                .alpha(0f)
                .translationY(-20f)
                .setDuration(200)
                .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
                .withEndAction {
                    hideHomeAppsSelector()
                }
                .start()
        }
    }

    private fun updateTextSizeScale(sizeScale: Float) {
        if (prefs.textSizeScale == sizeScale) return
        prefs.textSizeScale = sizeScale
        requireActivity().recreate()
    }

    private fun toggleKeyboardText() {
//        if (prefs.autoShowKeyboard && prefs.keyboardMessageShown.not()) {
//            viewModel.showMessageDialog(getString(R.string.keyboard_message))
//            prefs.keyboardMessageShown = true
//            prefs.autoShowKeyboard = !prefs.autoShowKeyboard
//            populateKeyboardText()
//        } else {
        prefs.autoShowKeyboard = !prefs.autoShowKeyboard
        populateKeyboardText()
//        }
    }

    private fun updateTheme(appTheme: Int) {
        if (AppCompatDelegate.getDefaultNightMode() == appTheme) return
        prefs.appTheme = appTheme
        populateAppThemeText(appTheme)
        setAppTheme(appTheme)
    }

    private fun setAppTheme(theme: Int) {
        if (AppCompatDelegate.getDefaultNightMode() == theme) return
        if (prefs.dailyWallpaper) {
            setPlainWallpaper(theme)
            viewModel.setWallpaperWorker()
        }
        requireActivity().recreate()
    }

    private fun setPlainWallpaper(appTheme: Int) {
        when (appTheme) {
            AppCompatDelegate.MODE_NIGHT_YES -> setPlainWallpaper(requireContext(), android.R.color.black)
            AppCompatDelegate.MODE_NIGHT_NO -> setPlainWallpaper(requireContext(), android.R.color.white)
            else -> {
                if (requireContext().isDarkThemeOn())
                    setPlainWallpaper(requireContext(), android.R.color.black)
                else setPlainWallpaper(requireContext(), android.R.color.white)
            }
        }
    }

    private fun populateAppThemeText(appTheme: Int = prefs.appTheme) {
        when (appTheme) {
            AppCompatDelegate.MODE_NIGHT_YES -> binding.appThemeText.text = getString(R.string.dark)
            AppCompatDelegate.MODE_NIGHT_NO -> binding.appThemeText.text = getString(R.string.light)
            else -> binding.appThemeText.text = getString(R.string.system_default)
        }
    }

    private fun populateTextSize() {
        binding.textSizeValue.text = getTextSizeStep().toString()
    }

    private fun getTextSizeStep(): Int {
        return when (prefs.textSizeScale) {
            Constants.TextSize.TWO -> 2
            Constants.TextSize.THREE -> 3
            Constants.TextSize.FOUR -> 4
            Constants.TextSize.FIVE -> 5
            Constants.TextSize.SIX -> 6
            Constants.TextSize.SEVEN -> 7
            else -> 1
        }
    }

    private fun getTextSizeScaleForStep(step: Int): Float {
        return when (step) {
            2 -> Constants.TextSize.TWO
            3 -> Constants.TextSize.THREE
            4 -> Constants.TextSize.FOUR
            5 -> Constants.TextSize.FIVE
            6 -> Constants.TextSize.SIX
            7 -> Constants.TextSize.SEVEN
            else -> Constants.TextSize.ONE
        }
    }

    private fun populateKeyboardText() {
        binding.autoShowKeyboardSwitch?.isChecked = prefs.autoShowKeyboard
    }

    /*private fun populateWallpaperText() {
        if (prefs.dailyWallpaper) binding.dailyWallpaper.text = getString(R.string.on)
        else binding.dailyWallpaper.text = getString(R.string.off)
    }*/

    private fun updateHomeBottomAlignment() {
        if (viewModel.isSereneScreenDefault.value != true) {
            requireContext().showToast(getString(R.string.please_set_serenescreen_as_default_first), Toast.LENGTH_LONG)
            return
        }
        prefs.homeBottomAlignment = !prefs.homeBottomAlignment
        populateAlignment()
        viewModel.updateHomeAlignment(prefs.homeAlignment)
    }

    private fun populateAlignment() {
        when (prefs.homeAlignment) {
            Gravity.START -> binding.alignment.text = getString(R.string.left)
            Gravity.CENTER -> binding.alignment.text = getString(R.string.center)
            Gravity.END -> binding.alignment.text = getString(R.string.right)
        }
        binding.alignmentBottom.text = if (prefs.homeBottomAlignment)
            getString(R.string.bottom_on)
        else getString(R.string.bottom_off)
    }

    /*private fun populateLockSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            binding.toggleLock.text = getString(
                if (isAccessServiceEnabled(requireContext())) R.string.on
                else R.string.off
            )
        } else {
            binding.toggleLock.text = getString(
                if (prefs.lockModeOn) R.string.on
                else R.string.off
            )
        }
    }*/

    private fun populateSwipeDownAction() {
        binding.swipeDownAction.text = when (prefs.swipeDownAction) {
            Constants.SwipeDownAction.NOTIFICATIONS -> getString(R.string.notifications)
            else -> getString(R.string.search)
        }
    }

    private fun updateSwipeDownAction(swipeDownFor: Int) {
        if (prefs.swipeDownAction == swipeDownFor) return
        prefs.swipeDownAction = swipeDownFor
        populateSwipeDownAction()
    }

    private fun shareApp() {
        val message = "Are you using your phone or your phone is using you?\n" +
                Constants.URL_SERENESCREEN_PLAY_STORE
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, message)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    private fun rateApp() {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(Constants.URL_SERENESCREEN_PLAY_STORE)
        )
        var flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        flags = flags or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
        intent.addFlags(flags)
        startActivity(intent)
    }

    private fun populateSwipeApps() {
        binding.swipeLeftApp.text = prefs.appNameSwipeLeft
        binding.swipeRightApp.text = prefs.appNameSwipeRight
        if (!prefs.swipeLeftEnabled)
            binding.swipeLeftApp.setTextColor(requireContext().getColorFromAttr(R.attr.primaryColorTrans50))
        if (!prefs.swipeRightEnabled)
            binding.swipeRightApp.setTextColor(requireContext().getColorFromAttr(R.attr.primaryColorTrans50))
    }

    private fun showAppListIfEnabled(flag: Int) {
        if ((flag == Constants.FLAG_SET_SWIPE_LEFT_APP) and !prefs.swipeLeftEnabled) {
            requireContext().showToast("Long press to enable")
            return
        }
        if ((flag == Constants.FLAG_SET_SWIPE_RIGHT_APP) and !prefs.swipeRightEnabled) {
            requireContext().showToast("Long press to enable")
            return
        }
        viewModel.getAppList(true)
        findNavController().navigate(
            R.id.action_settingsFragment_to_appListFragment,
            bundleOf(Constants.Key.FLAG to flag)
        )
    }

    private fun populateActionHints() {
        when (prefs.toShowHintCounter) {
            Constants.HINT_RATE_US -> {
                //viewModel.showMessageDialog(getString(R.string.rate_us_message)) Removed this pop up message <Antriksh>
                binding.scrollView.post {
                    binding.scrollView.fullScroll(View.FOCUS_DOWN)
                }
            }
        }
        if (viewModel.isSereneScreenDefault.value != true) return
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
