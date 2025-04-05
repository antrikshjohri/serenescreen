package app.serenescreen
import android.os.UserManager

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserHandle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.*
import app.serenescreen.data.AppModel
import app.serenescreen.data.Constants
import app.serenescreen.data.Prefs
import app.serenescreen.helper.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import android.util.Log

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext by lazy { application.applicationContext }
    private val prefs = Prefs(appContext)


    val firstOpen = MutableLiveData<Boolean>()
    val refreshHome = MutableLiveData<Boolean>()
    val toggleDateTime = MutableLiveData<Unit>()
    val updateSwipeApps = MutableLiveData<Any>()
    val appList = MutableLiveData<List<AppModel>?>()
    val hiddenApps = MutableLiveData<List<AppModel>?>()
    val isSereneScreenDefault = MutableLiveData<Boolean>()
    val launcherResetFailed = MutableLiveData<Boolean>()
    val homeAppAlignment = MutableLiveData<Int>()
    val showMessageDialog = MutableLiveData<String>()


    fun selectedApp(appModel: AppModel, flag: Int) {
        when (flag) {
            Constants.FLAG_LAUNCH_APP -> {
                launchApp(appModel.appPackage, appModel.activityClassName, appModel.user)
            }
            Constants.FLAG_HIDDEN_APPS -> {
                launchApp(appModel.appPackage, appModel.activityClassName, appModel.user)
            }
            Constants.FLAG_SET_HOME_APP_1 -> {
                prefs.appName1 = appModel.appLabel
                prefs.appPackage1 = appModel.appPackage
                prefs.appUser1 = appModel.user.toString()
                prefs.appActivityClassName1 = appModel.activityClassName.toString()
                refreshHome(false)
            }
            Constants.FLAG_SET_HOME_APP_2 -> {
                prefs.appName2 = appModel.appLabel
                prefs.appPackage2 = appModel.appPackage
                prefs.appUser2 = appModel.user.toString()
                prefs.appActivityClassName2 = appModel.activityClassName.toString()
                refreshHome(false)
            }
            Constants.FLAG_SET_HOME_APP_3 -> {
                prefs.appName3 = appModel.appLabel
                prefs.appPackage3 = appModel.appPackage
                prefs.appUser3 = appModel.user.toString()
                prefs.appActivityClassName3 = appModel.activityClassName.toString()
                refreshHome(false)
            }
            Constants.FLAG_SET_HOME_APP_4 -> {
                prefs.appName4 = appModel.appLabel
                prefs.appPackage4 = appModel.appPackage
                prefs.appUser4 = appModel.user.toString()
                prefs.appActivityClassName4 = appModel.activityClassName.toString()
                refreshHome(false)
            }
            Constants.FLAG_SET_HOME_APP_5 -> {
                prefs.appName5 = appModel.appLabel
                prefs.appPackage5 = appModel.appPackage
                prefs.appUser5 = appModel.user.toString()
                prefs.appActivityClassName5 = appModel.activityClassName.toString()
                refreshHome(false)
            }
            Constants.FLAG_SET_HOME_APP_6 -> {
                prefs.appName6 = appModel.appLabel
                prefs.appPackage6 = appModel.appPackage
                prefs.appUser6 = appModel.user.toString()
                prefs.appActivityClassName6 = appModel.activityClassName.toString()
                refreshHome(false)
            }
            Constants.FLAG_SET_HOME_APP_7 -> {
                prefs.appName7 = appModel.appLabel
                prefs.appPackage7 = appModel.appPackage
                prefs.appUser7 = appModel.user.toString()
                prefs.appActivityClassName7 = appModel.activityClassName.toString()
                refreshHome(false)
            }
            Constants.FLAG_SET_HOME_APP_8 -> {
                prefs.appName8 = appModel.appLabel
                prefs.appPackage8 = appModel.appPackage
                prefs.appUser8 = appModel.user.toString()
                prefs.appActivityClassName8 = appModel.activityClassName.toString()
                refreshHome(false)
            }
            Constants.FLAG_SET_HOME_APP_9 -> {
                prefs.appName9 = appModel.appLabel
                prefs.appPackage9 = appModel.appPackage
                prefs.appUser9 = appModel.user.toString()
                prefs.appActivityClassName9 = appModel.activityClassName.toString()
                refreshHome(false)
            }
            Constants.FLAG_SET_HOME_APP_10 -> {
                prefs.appName10 = appModel.appLabel
                prefs.appPackage10 = appModel.appPackage
                prefs.appUser10 = appModel.user.toString()
                prefs.appActivityClassName10 = appModel.activityClassName.toString()
                refreshHome(false)
            }
            Constants.FLAG_SET_HOME_APP_11 -> {
                prefs.appName11 = appModel.appLabel
                prefs.appPackage11 = appModel.appPackage
                prefs.appUser11 = appModel.user.toString()
                prefs.appActivityClassName11 = appModel.activityClassName.toString()
                refreshHome(false)
            }
            Constants.FLAG_SET_HOME_APP_12 -> {
                prefs.appName12 = appModel.appLabel
                prefs.appPackage12 = appModel.appPackage
                prefs.appUser12 = appModel.user.toString()
                prefs.appActivityClassName12 = appModel.activityClassName.toString()
                refreshHome(false)
            }
            Constants.FLAG_SET_HOME_APP_13 -> {
                prefs.appName13 = appModel.appLabel
                prefs.appPackage13 = appModel.appPackage
                prefs.appUser13 = appModel.user.toString()
                prefs.appActivityClassName13 = appModel.activityClassName.toString()
                refreshHome(false)
            }
            Constants.FLAG_SET_HOME_APP_14 -> {
                prefs.appName14 = appModel.appLabel
                prefs.appPackage14 = appModel.appPackage
                prefs.appUser14 = appModel.user.toString()
                prefs.appActivityClassName14 = appModel.activityClassName.toString()
                refreshHome(false)
            }
            Constants.FLAG_SET_HOME_APP_15 -> {
                prefs.appName15 = appModel.appLabel
                prefs.appPackage15 = appModel.appPackage
                prefs.appUser15 = appModel.user.toString()
                prefs.appActivityClassName15 = appModel.activityClassName.toString()
                refreshHome(false)
            }
            Constants.FLAG_SET_HOME_APP_16 -> {
                prefs.appName16 = appModel.appLabel
                prefs.appPackage16 = appModel.appPackage
                prefs.appUser16 = appModel.user.toString()
                prefs.appActivityClassName16 = appModel.activityClassName.toString()
                refreshHome(false)
            }
            Constants.FLAG_SET_SWIPE_LEFT_APP -> {
                prefs.appNameSwipeLeft = appModel.appLabel
                prefs.appPackageSwipeLeft = appModel.appPackage
                prefs.appUserSwipeLeft = appModel.user.toString()
                prefs.appActivityClassNameSwipeLeft = appModel.activityClassName.toString()
                updateSwipeApps()
            }
            Constants.FLAG_SET_SWIPE_RIGHT_APP -> {
                prefs.appNameSwipeRight = appModel.appLabel
                prefs.appPackageSwipeRight = appModel.appPackage
                prefs.appUserSwipeRight = appModel.user.toString()
                prefs.appActivityClassNameRight = appModel.activityClassName.toString()
                updateSwipeApps()
            }
        }
    }

    fun firstOpen(value: Boolean) {
        firstOpen.postValue(value)
    }

    fun refreshHome(appCountUpdated: Boolean) {
        refreshHome.value = appCountUpdated
    }

    fun toggleDateTime() {
        toggleDateTime.postValue(Unit)
    }

    private fun updateSwipeApps() {
        updateSwipeApps.postValue(Unit)
    }

    private fun launchApp(packageName: String, activityClassName: String?, userHandle: UserHandle) {
        val launcher = appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val activityInfo = launcher.getActivityList(packageName, userHandle)

        val component = if (activityClassName.isNullOrBlank()) {
            // activityClassName will be null for hidden apps.
            when (activityInfo.size) {
                0 -> {
                    appContext.showToast("App not found")
                    return
                }
                1 -> ComponentName(packageName, activityInfo[0].name)
                else -> ComponentName(packageName, activityInfo[activityInfo.size - 1].name)
            }
        } else {
            ComponentName(packageName, activityClassName)
        }

        try {
            launcher.startMainActivity(component, userHandle, null, null)
        } catch (e: SecurityException) {
            try {
                launcher.startMainActivity(component, android.os.Process.myUserHandle(), null, null)
            } catch (e: Exception) {
                appContext.showToast("Unable to launch app")
            }
        } catch (e: Exception) {
            appContext.showToast("Unable to launch app")
        }
    }

    fun getAppList(includeHiddenApps: Boolean = false) {
        viewModelScope.launch {
            appList.value = getAppsList(appContext, prefs, includeHiddenApps)
        }
    }

    fun getHiddenApps() {
        viewModelScope.launch {
            hiddenApps.value = getHiddenAppsList(appContext, prefs)
        }
    }

    fun isSereneScreenDefault() {
        isSereneScreenDefault.value = isSereneScreenDefault(appContext)
    }

    fun resetDefaultLauncherApp(context: Context) {
        resetDefaultLauncher(context)
        launcherResetFailed.value = getDefaultLauncherPackage(appContext).contains(".")
    }

    fun setWallpaperWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val uploadWorkRequest = PeriodicWorkRequestBuilder<WallpaperWorker>(8, TimeUnit.HOURS)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager
            .getInstance(appContext)
            .enqueueUniquePeriodicWork(
                Constants.WALLPAPER_WORKER_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                uploadWorkRequest
            )
    }

    fun cancelWallpaperWorker() {
        WorkManager.getInstance(appContext).cancelAllWork()
        prefs.dailyWallpaperUrl = ""
    }

    fun updateHomeAlignment(gravity: Int) {
        prefs.homeAlignment = gravity
        homeAppAlignment.value = prefs.homeAlignment
    }

    fun showMessageDialog(message: String) {
        showMessageDialog.postValue(message)
    }
}