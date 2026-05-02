package app.serenescreen.ui

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.*
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.appcompat.widget.SwitchCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import app.serenescreen.BuildConfig
import app.serenescreen.MainViewModel
import app.serenescreen.data.AppModel
import app.serenescreen.R
import app.serenescreen.data.Constants
import app.serenescreen.data.Prefs
import app.serenescreen.databinding.FragmentSettingsBinding
import app.serenescreen.helper.*
import app.serenescreen.listener.DeviceAdmin
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.launch

class SettingsFragment : Fragment(), View.OnClickListener, View.OnLongClickListener {
    companion object {
        private const val SLIDER_LABEL_FLOATING = 0
        private const val SLIDER_LABEL_GONE = 2
        private const val SLIDER_LABEL_VISIBLE = 3
    }

    private data class CustomBottomSheetLayout(
        val root: NestedScrollView,
        val container: LinearLayout
    )

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var deviceManager: DevicePolicyManager
    private lateinit var componentName: ComponentName

    private lateinit var firebaseAnalytics: FirebaseAnalytics // Declare FirebaseAnalytics instance

    private val requestHomeRoleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            viewModel.isSereneScreenDefault()
            if (it.resultCode != Activity.RESULT_OK && viewModel.isSereneScreenDefault.value != true) {
                viewModel.launcherResetFailed.value = true
            }
        }

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
        populateDateTime()
        populateSwipeApps()
        populateSwipeDownAction()
        populateActionHints()
        initClickListeners()
        applySwitchStyles()
        initSliderListeners()
        initObservers()
        applyWindowInsets()
        binding.root.post {
            if (!isAdded || _binding == null) return@post
            populateStatusBar()
        }
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
            R.id.setLauncher, R.id.setLauncherRow -> requestDefaultLauncher()
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

    private fun requestDefaultLauncher() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = requireContext().getSystemService(RoleManager::class.java)
            if (roleManager != null &&
                roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            ) {
                requestHomeRoleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME))
                return
            }
        }
        viewModel.resetDefaultLauncherApp(requireContext())
        viewModel.isSereneScreenDefault()
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
        val dialog = BottomSheetDialog(requireContext())
        val layout = createCustomBottomSheetLayout(dialog)
        var selectedTheme = prefs.appTheme

        fun render() {
            layout.root.background = createBottomSheetSurfaceDrawable()
            layout.container.removeAllViews()
            addBottomSheetHandle(layout.container)
            addBottomSheetHeader(
                layout.container,
                getString(R.string.theme_short),
                getString(R.string.choose_how_serene_looks)
            )

            val resolvedPreviewTheme = resolveThemePreviewMode(selectedTheme)
            layout.container.addView(
                createThemePreviewCard(resolvedPreviewTheme == AppCompatDelegate.MODE_NIGHT_YES),
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = dpToPx(16)
                    marginEnd = dpToPx(16)
                    bottomMargin = dpToPx(22)
                }
            )

            layout.container.addView(
                createSegmentedSelector(
                    labels = listOf(
                        getString(R.string.light),
                        getString(R.string.dark),
                        getString(R.string.system_default)
                    ),
                    selectedIndex = when (selectedTheme) {
                        AppCompatDelegate.MODE_NIGHT_NO -> 0
                        AppCompatDelegate.MODE_NIGHT_YES -> 1
                        else -> 2
                    }
                ) { index ->
                    val nextTheme = when (index) {
                        0 -> AppCompatDelegate.MODE_NIGHT_NO
                        1 -> AppCompatDelegate.MODE_NIGHT_YES
                        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    }
                    if (selectedTheme == nextTheme) return@createSegmentedSelector
                    selectedTheme = nextTheme
                    updateTheme(selectedTheme)
                    render()
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = dpToPx(16)
                    marginEnd = dpToPx(16)
                }
            )

        }

        render()
        dialog.show()
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
        val dialog = BottomSheetDialog(requireContext())
        val content = createBottomSheetContent(
            dialog,
            getString(R.string.swipe_down_short),
            getString(R.string.choose_what_happens_on_swipe_down)
        )
        content.addView(createRadioOnlyRow(
            title = getString(R.string.notifications),
            checked = prefs.swipeDownAction == Constants.SwipeDownAction.NOTIFICATIONS
        ) {
            updateSwipeDownAction(Constants.SwipeDownAction.NOTIFICATIONS)
            dialog.dismiss()
        })
        content.addView(createDivider())
        content.addView(createRadioOnlyRow(
            title = getString(R.string.search),
            checked = prefs.swipeDownAction == Constants.SwipeDownAction.SEARCH
        ) {
            updateSwipeDownAction(Constants.SwipeDownAction.SEARCH)
            dialog.dismiss()
        })
        dialog.show()
    }

    private fun showAlignmentDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val layout = createCustomBottomSheetLayout(dialog)
        var selectedAlignment = prefs.homeAlignment
        var selectedBottomAlignment = prefs.homeBottomAlignment

        fun render() {
            layout.root.background = createBottomSheetSurfaceDrawable()
            layout.container.removeAllViews()
            addBottomSheetHandle(layout.container)
            addBottomSheetHeader(
                layout.container,
                getString(R.string.home_layout_alignment),
                getString(R.string.choose_how_app_names_align)
            )

            layout.container.addView(
                createAlignmentPreviewCard(selectedAlignment, selectedBottomAlignment),
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = dpToPx(16)
                    marginEnd = dpToPx(16)
                    bottomMargin = dpToPx(20)
                }
            )

            layout.container.addView(
                createSegmentedSelector(
                    labels = listOf(
                        getString(R.string.left),
                        getString(R.string.center),
                        getString(R.string.right)
                    ),
                    selectedIndex = when (selectedAlignment) {
                        Gravity.START -> 0
                        Gravity.CENTER -> 1
                        else -> 2
                    }
                ) { index ->
                    val nextAlignment = when (index) {
                        0 -> Gravity.START
                        1 -> Gravity.CENTER
                        else -> Gravity.END
                    }
                    if (!tryApplyAlignmentSelection(nextAlignment, selectedBottomAlignment)) return@createSegmentedSelector
                    selectedAlignment = nextAlignment
                    render()
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = dpToPx(16)
                    marginEnd = dpToPx(16)
                }
            )

            layout.container.addView(
                createSwitchCardRow(
                    title = getString(R.string.align_to_bottom),
                    checked = selectedBottomAlignment
                ) { isChecked ->
                    if (!tryApplyAlignmentSelection(selectedAlignment, isChecked)) return@createSwitchCardRow
                    selectedBottomAlignment = isChecked
                    render()
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = dpToPx(16)
                    marginEnd = dpToPx(16)
                    topMargin = dpToPx(16)
                }
            )

        }

        render()
        dialog.show()
    }

    private fun tryApplyAlignmentSelection(alignment: Int, bottomAligned: Boolean): Boolean {
        if (viewModel.isSereneScreenDefault.value != true) {
            requireContext().showToast(getString(R.string.please_set_serenescreen_as_default_first), Toast.LENGTH_LONG)
            return false
        }
        prefs.homeBottomAlignment = bottomAligned
        populateAlignment()
        viewModel.updateHomeAlignment(alignment)
        return true
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

    private fun applySwitchStyles() {
        binding.autoShowKeyboardSwitch?.let(::styleSettingsSwitch)
        binding.dateTimeSwitch?.let(::styleSettingsSwitch)
        binding.statusBarSwitch?.let(::styleSettingsSwitch)
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

    private fun setHomeBottomAlignment(enabled: Boolean) {
        if (viewModel.isSereneScreenDefault.value != true) {
            requireContext().showToast(getString(R.string.please_set_serenescreen_as_default_first), Toast.LENGTH_LONG)
            return
        }
        prefs.homeBottomAlignment = enabled
        populateAlignment()
        viewModel.updateHomeAlignment(prefs.homeAlignment)
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
        binding.statusBarSwitch?.apply {
            isChecked = prefs.showStatusBar
            jumpDrawablesToCurrentState()
        }
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
        val hostActivity = activity ?: return
        hostActivity.window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = hostActivity.window.insetsController ?: return
            controller.show(WindowInsets.Type.statusBars())
        } else
            @Suppress("DEPRECATION", "InlinedApi")
            hostActivity.window.decorView.apply {
                systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            }
    }

    private fun hideStatusBar() {
        val hostActivity = activity ?: return
        hostActivity.window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = hostActivity.window.insetsController ?: return
            controller.hide(WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            hostActivity.window.decorView.apply {
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
        val hostActivity = requireActivity() as? androidx.appcompat.app.AppCompatActivity
        if (prefs.appTheme == appTheme && hostActivity?.delegate?.localNightMode == appTheme) return
        prefs.appTheme = appTheme
        populateAppThemeText(appTheme)
        setAppTheme(appTheme)
    }

    private fun setAppTheme(theme: Int) {
        val hostActivity = requireActivity() as? androidx.appcompat.app.AppCompatActivity ?: return
        hostActivity.delegate.localNightMode = theme
        hostActivity.delegate.applyDayNight()
        if (prefs.dailyWallpaper) {
            setPlainWallpaper(theme)
            viewModel.setWallpaperWorker()
        }
        hostActivity.window.decorView.post {
            if (!isAdded || _binding == null) return@post
            refreshSettingsThemeSurfaces()
            applySwitchStyles()
            populateAppThemeText()
            populateAlignment()
            populateDateTime()
            populateKeyboardText()
            populateSwipeApps()
            populateSwipeDownAction()
            populateStatusBar()
        }
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
        setHomeBottomAlignment(!prefs.homeBottomAlignment)
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

    private fun showSwipeAppBottomSheet(flag: Int) {
        val title = when (flag) {
            Constants.FLAG_SET_SWIPE_LEFT_APP -> getString(R.string.swipe_left_short)
            else -> getString(R.string.swipe_right_short)
        }
        val currentName = when (flag) {
            Constants.FLAG_SET_SWIPE_LEFT_APP -> prefs.appNameSwipeLeft
            else -> prefs.appNameSwipeRight
        }
        val currentPackage = when (flag) {
            Constants.FLAG_SET_SWIPE_LEFT_APP -> prefs.appPackageSwipeLeft
            else -> prefs.appPackageSwipeRight
        }
        val currentClassName = when (flag) {
            Constants.FLAG_SET_SWIPE_LEFT_APP -> prefs.appActivityClassNameSwipeLeft
            else -> prefs.appActivityClassNameRight
        }
        val currentUser = when (flag) {
            Constants.FLAG_SET_SWIPE_LEFT_APP -> prefs.appUserSwipeLeft
            else -> prefs.appUserSwipeRight
        }

        val dialog = BottomSheetDialog(requireContext())
        val content = createBottomSheetContent(
            dialog,
            title,
            getString(R.string.choose_app_or_action_for_gesture)
        )
        content.addView(createCurrentSelectionLabel(getString(R.string.current_selection, currentName)).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(16)
            }
        })

        val searchField = EditText(requireContext()).apply {
            hint = getString(R.string.search_apps)
            inputType = InputType.TYPE_CLASS_TEXT
            isSingleLine = true
            setTextColor(requireContext().getColorFromAttr(R.attr.primaryColor))
            setHintTextColor(requireContext().getColorFromAttr(R.attr.primaryColorTrans80))
            background = createRoundedStrokeDrawable(fillWithShade = true)
            setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_search, 0, 0, 0)
            compoundDrawablePadding = dpToPx(10)
            setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(18)
            }
        }
        content.addView(searchField)

        val listContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(18), dpToPx(6), dpToPx(18), dpToPx(6))
        }
        val listCard = MaterialCardView(requireContext()).apply {
            radius = dpToPx(18).toFloat()
            strokeWidth = dpToPx(1)
            strokeColor = requireContext().getColorFromAttr(R.attr.primaryColorInverseTrans50)
            setCardBackgroundColor(requireContext().getColorFromAttr(R.attr.customBackground))
            addView(
                listContainer,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(8)
            }
        }
        content.addView(listCard)

        fun renderApps(apps: List<AppModel>) {
            listContainer.removeAllViews()
            apps.forEachIndexed { index, app ->
                val selected = isSwipeAppSelected(
                    app = app,
                    currentName = currentName,
                    currentPackage = currentPackage,
                    currentClassName = currentClassName,
                    currentUser = currentUser
                )
                listContainer.addView(createRadioOnlyRow(app.appLabel, selected) {
                    saveSwipeAppSelection(app, flag)
                    populateSwipeApps()
                    dialog.dismiss()
                })
                if (index < apps.lastIndex) {
                    listContainer.addView(createDivider())
                }
            }
        }

        lifecycleScope.launch {
            val apps = getAppsList(requireContext(), prefs, true)
            renderApps(apps)
            searchField.addTextChangedListener { editable ->
                val query = editable?.toString().orEmpty().trim()
                val filtered = if (query.isBlank()) {
                    apps
                } else {
                    apps.filter { it.appLabel.contains(query, ignoreCase = true) }
                }
                renderApps(filtered)
            }
        }

        dialog.behavior.skipCollapsed = true
        dialog.show()
    }

    private fun isSwipeAppSelected(
        app: AppModel,
        currentName: String,
        currentPackage: String,
        currentClassName: String?,
        currentUser: String
    ): Boolean {
        if (currentPackage.isBlank()) {
            return app.appLabel.equals(currentName, ignoreCase = true)
        }
        val packageMatches = app.appPackage == currentPackage
        val classMatches = currentClassName.isNullOrBlank() || app.activityClassName == currentClassName
        val userMatches = currentUser.isBlank() || app.user.toString() == currentUser
        return packageMatches && classMatches && userMatches
    }

    private fun saveSwipeAppSelection(app: AppModel, flag: Int) {
        when (flag) {
            Constants.FLAG_SET_SWIPE_LEFT_APP -> {
                prefs.appNameSwipeLeft = app.appLabel
                prefs.appPackageSwipeLeft = app.appPackage
                prefs.appUserSwipeLeft = app.user.toString()
                prefs.appActivityClassNameSwipeLeft = app.activityClassName.toString()
            }
            Constants.FLAG_SET_SWIPE_RIGHT_APP -> {
                prefs.appNameSwipeRight = app.appLabel
                prefs.appPackageSwipeRight = app.appPackage
                prefs.appUserSwipeRight = app.user.toString()
                prefs.appActivityClassNameRight = app.activityClassName.toString()
            }
        }
    }

    private fun createBottomSheetContent(
        dialog: BottomSheetDialog,
        title: String,
        subtitle: String
    ): LinearLayout {
        val root = NestedScrollView(requireContext()).apply {
            isFillViewport = true
        }
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(24))
        }
        root.addView(
            container,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        container.addView(View(requireContext()).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(2).toFloat()
                setColor(requireContext().getColorFromAttr(R.attr.primaryColorTrans50))
            }
            layoutParams = LinearLayout.LayoutParams(dpToPx(44), dpToPx(4)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dpToPx(20)
            }
            alpha = 0.45f
        })
        container.addView(TextView(requireContext()).apply {
            text = title
            setTextColor(requireContext().getColorFromAttr(R.attr.primaryColor))
            textSize = 22f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        })
        container.addView(TextView(requireContext()).apply {
            text = subtitle
            setTextColor(requireContext().getColorFromAttr(R.attr.primaryColorTrans80))
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(8)
                bottomMargin = dpToPx(20)
            }
        })
        dialog.setContentView(root)
        return container
    }

    private fun createCustomBottomSheetLayout(dialog: BottomSheetDialog): CustomBottomSheetLayout {
        val root = NestedScrollView(requireContext()).apply {
            isFillViewport = true
            background = createBottomSheetSurfaceDrawable()
        }
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(24))
        }
        root.addView(
            container,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        val initialBottomPadding = dpToPx(24)
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            container.updatePadding(bottom = initialBottomPadding + bottomInset)
            insets
        }
        dialog.setContentView(root)
        dialog.setOnShowListener {
            dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
                ?.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
        return CustomBottomSheetLayout(root, container)
    }

    private fun addBottomSheetHandle(container: LinearLayout) {
        container.addView(View(requireContext()).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(2).toFloat()
                setColor(requireContext().getColorFromAttr(R.attr.primaryColorTrans50))
            }
            layoutParams = LinearLayout.LayoutParams(dpToPx(44), dpToPx(4)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dpToPx(20)
            }
            alpha = 0.45f
        })
    }

    private fun addBottomSheetHeader(
        container: LinearLayout,
        title: String,
        subtitle: String
    ) {
        container.addView(TextView(requireContext()).apply {
            text = title
            gravity = Gravity.CENTER_HORIZONTAL
            setTextColor(requireContext().getColorFromAttr(R.attr.primaryColor))
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        })
        container.addView(TextView(requireContext()).apply {
            text = subtitle
            gravity = Gravity.CENTER_HORIZONTAL
            setTextColor(requireContext().getColorFromAttr(R.attr.primaryColorTrans80))
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(8)
                bottomMargin = dpToPx(22)
            }
        })
    }

    private fun createBottomSheetSurfaceDrawable(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadii = floatArrayOf(
                dpToPx(30).toFloat(), dpToPx(30).toFloat(),
                dpToPx(30).toFloat(), dpToPx(30).toFloat(),
                0f, 0f, 0f, 0f
            )
            setColor(requireContext().getColorFromAttr(R.attr.customBackground))
        }
    }

    private fun createThemePreviewCard(isDark: Boolean): MaterialCardView {
        val backgroundColor = if (isDark) 0xFF121316.toInt() else 0xFFF7F7F8.toInt()
        val textColor = if (isDark) requireContext().getColor(android.R.color.white) else requireContext().getColorFromAttr(R.attr.primaryColor)
        return MaterialCardView(requireContext()).apply {
            radius = dpToPx(24).toFloat()
            strokeWidth = dpToPx(1)
            strokeColor = previewCardStrokeColor()
            setCardBackgroundColor(backgroundColor)
            addView(createPreviewContent(textColor, Gravity.START, bottomAligned = false))
        }
    }

    private fun createAlignmentPreviewCard(
        gravity: Int,
        bottomAligned: Boolean
    ): MaterialCardView {
        return MaterialCardView(requireContext()).apply {
            radius = dpToPx(24).toFloat()
            strokeWidth = dpToPx(1)
            strokeColor = previewCardStrokeColor()
            setCardBackgroundColor(previewCardBackgroundColor())
            addView(createPreviewContent(requireContext().getColorFromAttr(R.attr.primaryColor), gravity, bottomAligned))
        }
    }

    private fun resolveThemePreviewMode(selectedTheme: Int): Int {
        return if (selectedTheme == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
            if (requireContext().isDarkThemeOn()) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        } else {
            selectedTheme
        }
    }

    private fun createPreviewContent(
        textColor: Int,
        gravity: Int,
        bottomAligned: Boolean
    ): View {
        val labels = listOf("Calendar", "Camera", "Chrome", "Clock", "Contacts", "Drive", "Messages", "Phone")
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            minimumHeight = dpToPx(300)
            setPadding(dpToPx(18))
            addView(TextView(requireContext()).apply {
                text = "9:06"
                setTextColor(textColor)
                textSize = 30f
                this.gravity = gravity
            })
            addView(TextView(requireContext()).apply {
                text = "Sat, 2 May, 100%"
                setTextColor(textColor)
                textSize = 12f
                this.gravity = gravity
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dpToPx(4) }
            })
            if (bottomAligned) {
                addView(View(requireContext()), LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                ))
            }
            addView(LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                labels.forEachIndexed { index, label ->
                    addView(TextView(requireContext()).apply {
                        text = label
                        setTextColor(textColor)
                        textSize = 14f
                        this.gravity = gravity
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            topMargin = if (index == 0) dpToPx(20) else dpToPx(8)
                        }
                    })
                }
            })
            if (!bottomAligned) {
                addView(View(requireContext()), LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                ))
            }
        }
    }

    private fun createSegmentedSelector(
        labels: List<String>,
        selectedIndex: Int,
        onSelected: (Int) -> Unit
    ): View {
        val context = requireContext()
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            clipToOutline = true
            labels.forEachIndexed { index, label ->
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                    background = createSegmentedOptionDrawable(index == selectedIndex)
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply {
                        if (index > 0) marginStart = dpToPx(8)
                    }
                    minimumHeight = dpToPx(56)
                    setPadding(dpToPx(12), dpToPx(14), dpToPx(12), dpToPx(14))
                    isClickable = true
                    isFocusable = true
                    setOnClickListener { onSelected(index) }
                    addView(TextView(context).apply {
                        text = label
                        setTextColor(
                            if (index == selectedIndex) {
                                context.getColorFromAttr(R.attr.primaryColor)
                            } else {
                                context.getColorFromAttr(R.attr.primaryColorTrans80)
                            }
                        )
                        textSize = 16f
                        typeface = Typeface.create(
                            if (index == selectedIndex) "sans-serif-medium" else "sans-serif",
                            Typeface.NORMAL
                        )
                    })
                })
            }
        }
    }

    private fun createSegmentedOptionDrawable(selected: Boolean): GradientDrawable {
        val fillColor = if (selected) segmentedSelectedFillColor() else requireContext().getColorFromAttr(R.attr.customBackground)
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(20).toFloat()
            setColor(fillColor)
            setStroke(dpToPx(1), segmentedStrokeColor())
        }
    }

    private fun segmentedSelectedFillColor(): Int {
        return if (requireContext().isDarkThemeOn()) 0xFF2C2C30.toInt() else 0xFFECECEF.toInt()
    }

    private fun segmentedStrokeColor(): Int {
        return if (requireContext().isDarkThemeOn()) 0xFF4B4B52.toInt() else 0xFFD3D3D8.toInt()
    }

    private fun previewCardBackgroundColor(): Int {
        return if (requireContext().isDarkThemeOn()) 0xFF121316.toInt() else 0xFFF7F7F8.toInt()
    }

    private fun previewCardStrokeColor(): Int {
        return requireContext().getColorFromAttr(R.attr.primaryColorTrans50)
    }

    private fun refreshSettingsThemeSurfaces() {
        val currentBinding = _binding ?: return
        val backgroundColor = requireContext().getColorFromAttr(R.attr.customBackground)
        currentBinding.scrollView.setBackgroundColor(backgroundColor)
        currentBinding.settingsContent?.setBackgroundColor(backgroundColor)

        currentBinding.firstTile?.background = createSettingsSectionDrawable()
        (currentBinding.autoShowKeyboardRow?.parent as? View)?.background = createSettingsSectionDrawable()
        (currentBinding.swipeLeftRow?.parent as? View)?.background = createSettingsSectionDrawable()
        (currentBinding.rateRow?.parent as? View)?.background = createSettingsSectionDrawable()

        val appearanceCard = currentBinding.homeAppsNumRow?.parent as? ViewGroup
        appearanceCard?.background = createSettingsSectionDrawable()
        val rowIds = setOf(
            R.id.setLauncherRow,
            R.id.homeAppsNumRow,
            R.id.textSizeRow,
            R.id.appThemeRow,
            R.id.alignmentRow,
            R.id.dateTimeRow,
            R.id.statusBarRow,
            R.id.autoShowKeyboardRow,
            R.id.swipeLeftRow,
            R.id.swipeRightRow,
            R.id.swipeDownRow,
            R.id.rateRow
        )
        currentBinding.scrollLayout?.let { refreshSelectableRowBackgrounds(it, rowIds) }
        currentBinding.scrollLayout?.let { refreshSettingsForegroundTheme(it) }
        currentBinding.scrollLayout?.let { refreshDividerTheme(it) }
    }

    private fun refreshSelectableRowBackgrounds(view: View, rowIds: Set<Int>) {
        if (view.id in rowIds) {
            val typedValue = android.util.TypedValue()
            requireContext().theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
            view.setBackgroundResource(typedValue.resourceId)
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                refreshSelectableRowBackgrounds(view.getChildAt(index), rowIds)
            }
        }
    }

    private fun refreshSettingsForegroundTheme(view: View) {
        when (view) {
            is TextView -> {
                val currentAlpha = Color.alpha(view.currentTextColor)
                val targetColor = if (currentAlpha < 250) {
                    requireContext().getColorFromAttr(R.attr.primaryColorTrans80)
                } else {
                    requireContext().getColorFromAttr(R.attr.primaryColor)
                }
                view.setTextColor(targetColor)
                view.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
            }
            is ImageView -> {
                view.setColorFilter(requireContext().getColorFromAttr(R.attr.primaryColor))
            }
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                refreshSettingsForegroundTheme(view.getChildAt(index))
            }
        }
    }

    private fun refreshDividerTheme(view: View) {
        if (view.id == View.NO_ID && view.layoutParams?.height == dpToPx(1)) {
            view.setBackgroundColor(requireContext().getColorFromAttr(R.attr.primaryColorInverseTrans50))
            view.alpha = 0.25f
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                refreshDividerTheme(view.getChildAt(index))
            }
        }
    }

    private fun createSettingsSectionDrawable(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(18).toFloat()
            setColor(requireContext().getColorFromAttr(R.attr.customTileColor))
        }
    }

    private fun createSwitchCardRow(
        title: String,
        checked: Boolean,
        onToggle: (Boolean) -> Unit
    ): View {
        val switchView = SwitchCompat(requireContext()).apply {
            isChecked = checked
            isClickable = false
            isFocusable = false
            showText = false
        }
        styleSettingsSwitch(switchView)
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = createRoundedStrokeDrawable()
            setPadding(dpToPx(18), dpToPx(16), dpToPx(18), dpToPx(16))
            isClickable = true
            isFocusable = true
            setOnClickListener { onToggle(!checked) }
            addView(TextView(requireContext()).apply {
                text = title
                setTextColor(requireContext().getColorFromAttr(R.attr.primaryColor))
                textSize = 16f
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                )
            })
            addView(switchView)
        }
    }

    private fun styleSettingsSwitch(switchView: SwitchCompat) {
        switchView.thumbTintList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(
                requireContext().getColor(android.R.color.white),
                requireContext().getColor(android.R.color.white)
            )
        )
        switchView.trackTintList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(
                0xFF4A7DFF.toInt(),
                requireContext().getColorFromAttr(R.attr.primaryColorTrans50)
            )
        )
        switchView.trackDrawable?.alpha = 255
        switchView.thumbDrawable?.alpha = 255
    }

    private fun createRadioOnlyRow(
        title: String,
        checked: Boolean,
        onClick: () -> Unit
    ): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dpToPx(58)
            setPadding(0, dpToPx(8), 0, dpToPx(8))
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
            addView(TextView(requireContext()).apply {
                text = title
                setTextColor(requireContext().getColorFromAttr(R.attr.primaryColor))
                textSize = 16f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                )
            })
            addView(AppCompatRadioButton(requireContext()).apply {
                isChecked = checked
                isClickable = false
                jumpDrawablesToCurrentState()
            })
        }
    }

    private fun createDivider(): View {
        return View(requireContext()).apply {
            setBackgroundColor(requireContext().getColorFromAttr(R.attr.primaryColorInverseTrans50))
            alpha = 0.25f
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(1)
            )
        }
    }

    private fun createCurrentSelectionLabel(text: String): View {
        return TextView(requireContext()).apply {
            this.text = text
            setTextColor(requireContext().getColorFromAttr(R.attr.primaryColor))
            textSize = 15f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            background = createRoundedStrokeDrawable(fillWithShade = true)
            setPadding(dpToPx(14), dpToPx(9), dpToPx(14), dpToPx(9))
        }
    }

    private fun createRoundedStrokeDrawable(fillWithShade: Boolean = false): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(16).toFloat()
            setColor(
                if (fillWithShade) requireContext().getColorFromAttr(R.attr.primaryShadeColor)
                else requireContext().getColorFromAttr(R.attr.customBackground)
            )
            setStroke(dpToPx(1), requireContext().getColorFromAttr(R.attr.primaryColorInverseTrans50))
        }
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
        showSwipeAppBottomSheet(flag)
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
