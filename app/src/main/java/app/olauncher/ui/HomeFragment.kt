package app.serenescreen.ui

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import app.serenescreen.MainViewModel
import app.serenescreen.R
import app.serenescreen.data.AppModel
import app.serenescreen.data.Constants
import app.serenescreen.data.Prefs
import app.serenescreen.databinding.FragmentHomeBinding
import app.serenescreen.helper.*
import app.serenescreen.listener.OnSwipeTouchListener
import app.serenescreen.listener.ViewSwipeTouchListener
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.analytics.FirebaseAnalytics

class HomeFragment : Fragment(), View.OnClickListener, View.OnLongClickListener {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var deviceManager: DevicePolicyManager
    private lateinit var vibrator: Vibrator

    private lateinit var firebaseAnalytics: FirebaseAnalytics // Declare FirebaseAnalytics instance

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())
        viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        deviceManager = context?.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        vibrator = context?.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        // Initialize Firebase Analytics
        firebaseAnalytics = FirebaseAnalytics.getInstance(requireContext())

        initObservers()
        setHomeAlignment(prefs.homeAlignment)
        initSwipeTouchListener()
        initClickListeners()
    }

    override fun onResume() {
        super.onResume()
        populateHomeScreen(false)
        viewModel.isSereneScreenDefault()
        if (prefs.showStatusBar) showStatusBar()
        else hideStatusBar()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.lock -> {}
            R.id.clock -> openAlarmApp(requireContext())
            R.id.date -> openCalendar(requireContext())
            R.id.setDefaultLauncher -> {
                viewModel.resetDefaultLauncherApp(requireContext())

                // Log an event when the button is clicked
                val bundle = Bundle().apply {
                    putString(FirebaseAnalytics.Param.ITEM_ID, "set_default_launcher_button")
                    putString(FirebaseAnalytics.Param.ITEM_NAME, "Set Default Launcher")
                    putString(FirebaseAnalytics.Param.CONTENT_TYPE, "button")
                }
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
            }
            else -> {
                try { // Launch app
                    val appLocation = view.tag.toString().toInt()
                    homeAppClicked(appLocation)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onLongClick(view: View): Boolean {
        when (view.id) {
            R.id.homeApp1 -> showAppList(Constants.FLAG_SET_HOME_APP_1, prefs.appName1.isNotEmpty(), true)
            R.id.homeApp2 -> showAppList(Constants.FLAG_SET_HOME_APP_2, prefs.appName2.isNotEmpty(), true)
            R.id.homeApp3 -> showAppList(Constants.FLAG_SET_HOME_APP_3, prefs.appName3.isNotEmpty(), true)
            R.id.homeApp4 -> showAppList(Constants.FLAG_SET_HOME_APP_4, prefs.appName4.isNotEmpty(), true)
            R.id.homeApp5 -> showAppList(Constants.FLAG_SET_HOME_APP_5, prefs.appName5.isNotEmpty(), true)
            R.id.homeApp6 -> showAppList(Constants.FLAG_SET_HOME_APP_6, prefs.appName6.isNotEmpty(), true)
            R.id.homeApp7 -> showAppList(Constants.FLAG_SET_HOME_APP_7, prefs.appName7.isNotEmpty(), true)
            R.id.homeApp8 -> showAppList(Constants.FLAG_SET_HOME_APP_8, prefs.appName8.isNotEmpty(), true)
            R.id.homeApp9 -> showAppList(Constants.FLAG_SET_HOME_APP_9, prefs.appName9.isNotEmpty(), true)
            R.id.homeApp10 -> showAppList(Constants.FLAG_SET_HOME_APP_10, prefs.appName10.isNotEmpty(), true)
            R.id.homeApp11 -> showAppList(Constants.FLAG_SET_HOME_APP_11, prefs.appName11.isNotEmpty(), true)
            R.id.homeApp12 -> showAppList(Constants.FLAG_SET_HOME_APP_12, prefs.appName12.isNotEmpty(), true)
            R.id.homeApp13 -> showAppList(Constants.FLAG_SET_HOME_APP_13, prefs.appName13.isNotEmpty(), true)
            R.id.homeApp14 -> showAppList(Constants.FLAG_SET_HOME_APP_14, prefs.appName14.isNotEmpty(), true)
            R.id.homeApp15 -> showAppList(Constants.FLAG_SET_HOME_APP_15, prefs.appName15.isNotEmpty(), true)
            R.id.homeApp16 -> showAppList(Constants.FLAG_SET_HOME_APP_16, prefs.appName16.isNotEmpty(), true)
        }
        return true
    }

    private fun initObservers() {
        if (prefs.firstSettingsOpen) {
            binding.firstRunTips.visibility = View.VISIBLE
            binding.setDefaultLauncher.visibility = View.GONE
        } else binding.firstRunTips.visibility = View.GONE

        viewModel.refreshHome.observe(viewLifecycleOwner) {
            populateHomeScreen(it)
        }
        viewModel.isSereneScreenDefault.observe(viewLifecycleOwner, Observer {
            if (it != true) {
                prefs.homeBottomAlignment = false
                setHomeAlignment()
            }
            if (binding.firstRunTips.visibility == View.VISIBLE) return@Observer
            if (it) binding.setDefaultLauncher.visibility = View.GONE
            else binding.setDefaultLauncher.visibility = View.VISIBLE
        })
        viewModel.homeAppAlignment.observe(viewLifecycleOwner) {
            setHomeAlignment(it)
        }
        viewModel.toggleDateTime.observe(viewLifecycleOwner) {
            populateDateTime()
        }
    }

    private fun initSwipeTouchListener() {
        val context = requireContext()
        binding.mainLayout.setOnTouchListener(getSwipeGestureListener(context))
        binding.homeApp1.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp1))
        binding.homeApp2.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp2))
        binding.homeApp3.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp3))
        binding.homeApp4.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp4))
        binding.homeApp5.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp5))
        binding.homeApp6.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp6))
        binding.homeApp7.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp7))
        binding.homeApp8.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp8))
        binding.homeApp9.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp9))
        binding.homeApp10.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp10))
        binding.homeApp11.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp11))
        binding.homeApp12.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp12))
        binding.homeApp13.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp13))
        binding.homeApp14.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp14))
        binding.homeApp15.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp15))
        binding.homeApp16.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp16))
    }

    private fun initClickListeners() {
        binding.lock.setOnClickListener(this)
        binding.clock.setOnClickListener(this)
        binding.date.setOnClickListener(this)
        binding.setDefaultLauncher.setOnClickListener(this)
    }

    private fun setHomeAlignment(horizontalGravity: Int = prefs.homeAlignment) {
        val verticalGravity = if (prefs.homeBottomAlignment) Gravity.BOTTOM else Gravity.CENTER_VERTICAL
        binding.homeAppsLayout.gravity = horizontalGravity or verticalGravity
        binding.dateTimeLayout.gravity = horizontalGravity
        binding.homeApp1.gravity = horizontalGravity
        binding.homeApp2.gravity = horizontalGravity
        binding.homeApp3.gravity = horizontalGravity
        binding.homeApp4.gravity = horizontalGravity
        binding.homeApp5.gravity = horizontalGravity
        binding.homeApp6.gravity = horizontalGravity
        binding.homeApp7.gravity = horizontalGravity
        binding.homeApp8.gravity = horizontalGravity
        binding.homeApp9.gravity = horizontalGravity
        binding.homeApp10.gravity = horizontalGravity
        binding.homeApp11.gravity = horizontalGravity
        binding.homeApp12.gravity = horizontalGravity
        binding.homeApp13.gravity = horizontalGravity
        binding.homeApp14.gravity = horizontalGravity
        binding.homeApp15.gravity = horizontalGravity
        binding.homeApp16.gravity = horizontalGravity
    }

    private fun populateDateTime() {
        binding.dateTimeLayout.isVisible = prefs.dateTimeVisibility != Constants.DateTime.OFF
        binding.clock.isVisible = Constants.DateTime.isTimeVisible(prefs.dateTimeVisibility)
        binding.date.isVisible = Constants.DateTime.isDateVisible(prefs.dateTimeVisibility)

        var dateText = SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date())
        if (!prefs.showStatusBar) {
            val battery = (requireContext().getSystemService(Context.BATTERY_SERVICE) as BatteryManager)
                .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            dateText = getString(R.string.day_battery, dateText, battery)
        }
        binding.date.text = dateText.replace(".,", ",")
    }

    private fun populateHomeScreen(appCountUpdated: Boolean) {
        if (appCountUpdated) hideHomeApps()
        populateDateTime()

        val homeAppsNum = prefs.homeAppsNum
        if (homeAppsNum == 0) return

        binding.homeApp1.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp1, prefs.appName1, prefs.appPackage1, prefs.appUser1)) {
            prefs.appName1 = ""
            prefs.appPackage1 = ""
        }
        if (homeAppsNum == 1) return

        binding.homeApp2.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp2, prefs.appName2, prefs.appPackage2, prefs.appUser2)) {
            prefs.appName2 = ""
            prefs.appPackage2 = ""
        }
        if (homeAppsNum == 2) return

        binding.homeApp3.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp3, prefs.appName3, prefs.appPackage3, prefs.appUser3)) {
            prefs.appName3 = ""
            prefs.appPackage3 = ""
        }
        if (homeAppsNum == 3) return

        binding.homeApp4.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp4, prefs.appName4, prefs.appPackage4, prefs.appUser4)) {
            prefs.appName4 = ""
            prefs.appPackage4 = ""
        }
        if (homeAppsNum == 4) return

        binding.homeApp5.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp5, prefs.appName5, prefs.appPackage5, prefs.appUser5)) {
            prefs.appName5 = ""
            prefs.appPackage5 = ""
        }
        if (homeAppsNum == 5) return

        binding.homeApp6.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp6, prefs.appName6, prefs.appPackage6, prefs.appUser6)) {
            prefs.appName6 = ""
            prefs.appPackage6 = ""
        }
        if (homeAppsNum == 6) return

        binding.homeApp7.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp7, prefs.appName7, prefs.appPackage7, prefs.appUser7)) {
            prefs.appName7 = ""
            prefs.appPackage7 = ""
        }
        if (homeAppsNum == 7) return

        binding.homeApp8.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp8, prefs.appName8, prefs.appPackage8, prefs.appUser8)) {
            prefs.appName8 = ""
            prefs.appPackage8 = ""
        }
        if (homeAppsNum == 8) return

        binding.homeApp9.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp9, prefs.appName9, prefs.appPackage9, prefs.appUser9)) {
            prefs.appName9 = ""
            prefs.appPackage9 = ""
        }
        if (homeAppsNum == 9) return

        binding.homeApp10.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp10, prefs.appName10, prefs.appPackage10, prefs.appUser10)) {
            prefs.appName10 = ""
            prefs.appPackage10 = ""
        }
        if (homeAppsNum == 10) return

        binding.homeApp11.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp11, prefs.appName11, prefs.appPackage11, prefs.appUser11)) {
            prefs.appName11 = ""
            prefs.appPackage11 = ""
        }
        if (homeAppsNum == 11) return

        binding.homeApp12.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp12, prefs.appName12, prefs.appPackage12, prefs.appUser12)) {
            prefs.appName12 = ""
            prefs.appPackage12 = ""
        }
        if (homeAppsNum == 12) return

        binding.homeApp13.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp13, prefs.appName13, prefs.appPackage13, prefs.appUser13)) {
            prefs.appName13 = ""
            prefs.appPackage13 = ""
        }
        if (homeAppsNum == 13) return

        binding.homeApp14.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp14, prefs.appName14, prefs.appPackage14, prefs.appUser14)) {
            prefs.appName14 = ""
            prefs.appPackage14 = ""
        }
        if (homeAppsNum == 14) return

        binding.homeApp15.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp15, prefs.appName15, prefs.appPackage15, prefs.appUser15)) {
            prefs.appName15 = ""
            prefs.appPackage15 = ""
        }
        if (homeAppsNum == 15) return

        binding.homeApp16.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp16, prefs.appName16, prefs.appPackage16, prefs.appUser16)) {
            prefs.appName16 = ""
            prefs.appPackage16 = ""
        }
    }

    private fun setHomeAppText(textView: TextView, appName: String, packageName: String, userString: String): Boolean {
        if (isPackageInstalled(requireContext(), packageName, userString)) {
            textView.text = appName
            return true
        }
        textView.text = ""
        return false
    }

    private fun hideHomeApps() {
        binding.homeApp1.visibility = View.GONE
        binding.homeApp2.visibility = View.GONE
        binding.homeApp3.visibility = View.GONE
        binding.homeApp4.visibility = View.GONE
        binding.homeApp5.visibility = View.GONE
        binding.homeApp6.visibility = View.GONE
        binding.homeApp7.visibility = View.GONE
        binding.homeApp8.visibility = View.GONE
        binding.homeApp9.visibility = View.GONE
        binding.homeApp10.visibility = View.GONE
        binding.homeApp11.visibility = View.GONE
        binding.homeApp12.visibility = View.GONE
        binding.homeApp13.visibility = View.GONE
        binding.homeApp14.visibility = View.GONE
        binding.homeApp15.visibility = View.GONE
        binding.homeApp16.visibility = View.GONE
    }

    private fun homeAppClicked(location: Int) {
        if (prefs.getAppName(location).isEmpty()) showLongPressToast()
        else launchApp(
            prefs.getAppName(location),
            prefs.getAppPackage(location),
            prefs.getAppActivityClassName(location),
            prefs.getAppUser(location)
        )
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, "home_app_clicked")
            putString(FirebaseAnalytics.Param.ITEM_NAME, "Home app clicked")
            putString(FirebaseAnalytics.Param.CONTENT_TYPE, "button")
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
    }

    private fun launchApp(appName: String, packageName: String, activityClassName: String?, userString: String) {
        viewModel.selectedApp(
            AppModel(
                appName,
                null,
                packageName,
                activityClassName,
                getUserHandleFromString(requireContext(), userString)
            ),
            Constants.FLAG_LAUNCH_APP
        )
    }

    private fun showAppList(flag: Int, rename: Boolean = false, includeHiddenApps: Boolean = false) {
        viewModel.getAppList(includeHiddenApps)
        try {
            findNavController().navigate(
                R.id.action_mainFragment_to_appListFragment,
                bundleOf(
                    Constants.Key.FLAG to flag,
                    Constants.Key.RENAME to rename
                )
            )
        } catch (e: Exception) {
            findNavController().navigate(
                R.id.appListFragment,
                bundleOf(
                    Constants.Key.FLAG to flag,
                    Constants.Key.RENAME to rename
                )
            )
            e.printStackTrace()
        }
    }

    private fun swipeDownAction() {
        when (prefs.swipeDownAction) {
            Constants.SwipeDownAction.SEARCH -> openSearch(requireContext())
            else -> expandNotificationDrawer(requireContext())
        }
    }

    private fun openSwipeRightApp() {
        if (!prefs.swipeRightEnabled) return
        if (prefs.appPackageSwipeRight.isNotEmpty())
            launchApp(
                prefs.appNameSwipeRight,
                prefs.appPackageSwipeRight,
                prefs.appActivityClassNameRight,
                android.os.Process.myUserHandle().toString()
            )
        else openDialerApp(requireContext())
    }

    private fun openSwipeLeftApp() {
        if (!prefs.swipeLeftEnabled) return
        if (prefs.appPackageSwipeLeft.isNotEmpty())
            launchApp(
                prefs.appNameSwipeLeft,
                prefs.appPackageSwipeLeft,
                prefs.appActivityClassNameSwipeLeft,
                android.os.Process.myUserHandle().toString()
            )
        else openCameraApp(requireContext())
    }

    private fun lockPhone() {
        requireActivity().runOnUiThread {
            try {
                deviceManager.lockNow()
            } catch (e: SecurityException) {
                requireContext().showToast("Please turn on double tap to lock", Toast.LENGTH_LONG)
                findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
            } catch (e: Exception) {
                requireContext().showToast("SereneScreen failed to lock device.\nPlease check your app settings.", Toast.LENGTH_LONG)
                prefs.lockModeOn = false
            }
        }
    }

    private fun showStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            requireActivity().window.insetsController?.show(WindowInsets.Type.statusBars())
        else
            @Suppress("DEPRECATION", "InlinedApi")
            requireActivity().window.decorView.apply {
                systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            }
    }

    private fun hideStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            requireActivity().window.insetsController?.hide(WindowInsets.Type.statusBars())
        else {
            @Suppress("DEPRECATION")
            requireActivity().window.decorView.apply {
                systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE or View.SYSTEM_UI_FLAG_FULLSCREEN
            }
        }
    }

    private fun changeAppTheme() {
        if (prefs.dailyWallpaper.not()) return
        val changedAppTheme = getChangedAppTheme(requireContext(), prefs.appTheme)
        prefs.appTheme = changedAppTheme
        if (prefs.dailyWallpaper) {
            setPlainWallpaperByTheme(requireContext(), changedAppTheme)
            viewModel.setWallpaperWorker()
        }
        requireActivity().recreate()
    }

    private fun showLongPressToast() = requireContext().showToast("Long press to select app")

    private fun textOnClick(view: View) = onClick(view)

    private fun textOnLongClick(view: View) = onLongClick(view)

    private fun getSwipeGestureListener(context: Context): View.OnTouchListener {
        return object : OnSwipeTouchListener(context) {
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                openSwipeLeftApp()
            }

            override fun onSwipeRight() {
                super.onSwipeRight()
                openSwipeRightApp()
            }

            override fun onSwipeUp() {
                super.onSwipeUp()
                showAppList(Constants.FLAG_LAUNCH_APP)
            }

            override fun onSwipeDown() {
                super.onSwipeDown()
                swipeDownAction()
            }

            override fun onLongClick() {
                super.onLongClick()
                try {
                    findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
                    viewModel.firstOpen(false)
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }

            override fun onDoubleClick() {
                super.onDoubleClick()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                    binding.lock.performClick()
                else if (prefs.lockModeOn)
                    lockPhone()
            }
        }
    }

    private fun getViewSwipeTouchListener(context: Context, view: View): View.OnTouchListener {
        return object : ViewSwipeTouchListener(context, view) {
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                openSwipeLeftApp()
            }

            override fun onSwipeRight() {
                super.onSwipeRight()
                openSwipeRightApp()
            }

            override fun onSwipeUp() {
                super.onSwipeUp()
                showAppList(Constants.FLAG_LAUNCH_APP)
            }

            override fun onSwipeDown() {
                super.onSwipeDown()
                swipeDownAction()
            }

            override fun onLongClick(view: View) {
                super.onLongClick(view)
                textOnLongClick(view)
            }

            override fun onClick(view: View) {
                super.onClick(view)
                textOnClick(view)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}