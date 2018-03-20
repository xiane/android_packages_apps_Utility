package com.hardkernel.odroid

import android.annotation.SuppressLint
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.util.ArrayList

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContentUris
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.RecoverySystem
import android.os.StatFs
import android.os.ServiceManager
import android.os.IPowerManager
import android.os.RemoteException
import android.os.SystemProperties
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.CheckBox
import android.widget.Toast
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.board_activity.*
import kotlinx.android.synthetic.main.rotation_activity.*
import kotlinx.android.synthetic.main.shortcut_activity.*
import kotlinx.android.synthetic.main.update_activity.*

class MainActivity : Activity() {

    private var blueLed = "on"
    private val mCBCECSwitch: CheckBox? = null
    private val mCBOneKeyPlay: CheckBox? = null
    private val mCBAutoPowerOn: CheckBox? = null
    private val mCBAutoChangeLanguage: CheckBox? = null
    private val mCBOneKeyShutdown: CheckBox? = null

    private val mLLOneKeyPlay: LinearLayout? = null
    private val mLLAutoChangeLanguage: LinearLayout? = null
    private val mLLAutoPowerOn: LinearLayout? = null
    private val mLLOneKeyShutdown: LinearLayout? = null

    private var mOrientation: String? = null
    private var mDegree: Int = 0

    private var downloadManager: DownloadManager? = null
    private var enqueue: Long = 0

    private var m_updatePackage: UpdatePackage? = null

    private val mSharepreference: SharedPreferences? = null

    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L)
            if (id != enqueue) {
                Log.v(TAG, "Ingnoring unrelated download " + id)
                return
            }

            val query = DownloadManager.Query()
            query.setFilterById(id)
            val cursor = downloadManager!!.query(query)

            if (!cursor.moveToFirst()) {
                Log.e(TAG, "Not able to move the cursor for downloaded content.")
                return
            }

            val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
            if (DownloadManager.ERROR_INSUFFICIENT_SPACE == status) {
                Log.e(TAG, "Download is failed due to insufficient space")
                return
            }
            if (DownloadManager.STATUS_SUCCESSFUL != status) {
                Log.e(TAG, "Download Failed")
                return
            }

            /* Get URI of downloaded file */
            val uri = Uri.parse(cursor.getString(
                    cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)))

            cursor.close()

            val file = File(uri.path)
            if (!file.exists()) {
                Log.e(TAG, "Not able to find downloaded file: " + uri.path)
                return
            }

            if (file.name == LATEST_VERSION) {
                try {
                    val text = StringBuilder()

                    val br = BufferedReader(FileReader(file))
                    text.append(br.readLine())
                    br.close()

                    m_updatePackage = UpdatePackage(text.toString())

                    var currentVersion = 0
                    val version = Build.VERSION.INCREMENTAL.split("-".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                    if (version.size < 4) {
                        Toast.makeText(context,
                                "Not able to detect the version number installed. " + "Remote package will be installed anyway!",
                                Toast.LENGTH_LONG).show()
                    } else {
                        currentVersion = Integer.parseInt(version[3])
                    }

                    if (currentVersion < m_updatePackage!!.buildNumber) updatePckageFromOnline()
                    else {
                        if (currentVersion > m_updatePackage!!.buildNumber) Toast.makeText(context,
                                "The current installed build number might be wrong",
                                Toast.LENGTH_LONG).show()
                        else Toast.makeText(context,
                                "Already latest Android image is installed.",
                                Toast.LENGTH_LONG).show()
                    }
                } catch (e: IOException) {
                    Log.d(TAG, e.toString())
                    e.printStackTrace()
                }

            } else if (id == m_updatePackage!!.downloadId) {
                /* Update package download is done, time to install */
                installPackage(File(m_updatePackage!!.localUri(context).path))
            }
        }
    }

    @SuppressLint("ApplySharedPref")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        context = applicationContext

        val url = ServerInfo.read()
        if (url == null)
            ServerInfo.write(UpdatePackage.remoteUrl)

        downloadManager = context!!.getSystemService(
                Context.DOWNLOAD_SERVICE) as DownloadManager

        registerReceiver(mReceiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        val display = windowManager.defaultDisplay
        mDegree = display.rotation * 90
        mOrientation = if (mDegree == 0) "landscape" else "portrait"

        cpuActivity = CpuActivity(this, TAG)
        cpuActivity!!.onCreate()

        cb_kodi.setOnCheckedChangeListener { _, isChecked ->
            val pref = getSharedPreferences("utility", Context.MODE_PRIVATE)
            val editor = pref.edit()
            editor.putBoolean("kodi", isChecked)
            editor.commit()
        }

        val boot_ini = File(BOOT_INI)
        if (boot_ini.exists()) {
            try {
                var line: String?
                val bufferedReader = BufferedReader(FileReader(BOOT_INI))
                line = bufferedReader.readLine()
                while (line != null) {
                    if (line.startsWith("setenv bootargs"))
                        break

                    if (line.startsWith("setenv hdmimode")) {
                        Log.e(TAG, line)
                    }

                    if (line.startsWith("setenv vout_mode")) {
                        Log.e(TAG, line)
                        val vout_mode = line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\""))
                    }

                    if (line.startsWith("setenv led_onoff")) {
                        blueLed = line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\""))

                        Log.e(TAG, "blue led : $blueLed")
                    }

                    line = bufferedReader.readLine()
                }
                bufferedReader.close()
            } catch (e1: IOException) {
                // TODO Auto-generated catch block
                e1.printStackTrace()
            }

        } else {
            //default value
            Log.e(TAG, "Not found $BOOT_INI")
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Not found boot.ini")
                    .setMessage("Check and Format Internal FAT storage?")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton("Yes") { dialog, which -> startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)) }
                    .setNegativeButton("No", null).show()
        }

        blue_led.setOnCheckedChangeListener { buttonView, isChecked ->
            blueLed = if (isChecked) "on" else "off"
            blue_led.setText(if (isChecked) R.string.on else R.string.off)
            modifyBootIni()
        }

        blue_led.isChecked = blueLed == "on"
        blue_led.setText(if (blueLed == "on") R.string.on else R.string.off)

        /*
        mCBSelfAdaption = (CheckBox)findViewById(R.id.cb_selfadaption);
        mCBSelfAdaption.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // TODO Auto-generated method stub
                updateHDMISelfAdaption();
            }
        });

        mLLOneKeyPlay = (LinearLayout)findViewById(R.id.layout_one_key_play);
        mLLAutoChangeLanguage = (LinearLayout)findViewById(R.id.layout_auto_change_language);
        mLLAutoPowerOn = (LinearLayout)findViewById(R.id.layout_auto_power_on);
        mLLOneKeyShutdown= (LinearLayout)findViewById(R.id.layout_one_key_shutdown);

        mCBCECSwitch = (CheckBox)findViewById(R.id.cb_cecswitch);
        mCBCECSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // TODO Auto-generated method stub
            }
        });

        mCBOneKeyPlay = (CheckBox)findViewById(R.id.cb_one_key_play);
        mCBOneKeyPlay.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // TODO Auto-generated method stub
            }
        });

        mCBAutoPowerOn = (CheckBox)findViewById(R.id.cb_auto_power_on);
        mCBAutoPowerOn.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // TODO Auto-generated method stub
            }
        });

        mCBAutoChangeLanguage = (CheckBox)findViewById(R.id.cb_auto_change_language);
        mCBAutoChangeLanguage.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // TODO Auto-generated method stub
            }
        });

        mCBOneKeyShutdown = (CheckBox) findViewById(R.id.cb_one_key_shutdown);
        mCBOneKeyShutdown.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            }
        });
        */
        button_apply_reboot.setOnClickListener {
            modifyBootIni()
            reboot()
        }

        button_check_online_update.setOnClickListener { checkLatestVersion() }

        button_package_install_from_storage.setOnClickListener { updatePackageFromStorage() }

        updateActivity = UpdateActivity(this, TAG)
        updateActivity!!.onCreate()

        if (mOrientation == "landscape") {
            radio_landscape.isChecked = true
            radioGroup_degree.visibility = View.GONE
            mDegree = 0
        } else {
            radio_portrait.isChecked = true
            radioGroup_degree.visibility = View.VISIBLE
        }

        if (mDegree == 90) {
            radio_90.isChecked = true
            radio_270.isChecked = false
        } else {
            radio_90.isChecked = false
            radio_270.isChecked = true
        }

        radio_portrait.setOnClickListener {
            radioGroup_degree.visibility = View.VISIBLE
            mDegree = 270
            radio_90.isChecked = false
            radio_270.isChecked = true
        }

        radio_landscape.setOnClickListener {
            radioGroup_degree.visibility = View.GONE
            mDegree = 0
        }

        radio_90.setOnClickListener {
            mDegree = 90
        }

        radio_90.setOnClickListener {
            mDegree = 270
        }
        button_rotation_apply.setOnClickListener {
            if (mDegree == 0) {
                android.provider.Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)
                android.provider.Settings.System.putInt(contentResolver, Settings.System.USER_ROTATION, 0)
            } else if (mDegree == 90) {
                android.provider.Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)
                android.provider.Settings.System.putInt(contentResolver, Settings.System.USER_ROTATION, 1)
            } else if (mDegree == 270) {
                android.provider.Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)
                android.provider.Settings.System.putInt(contentResolver, Settings.System.USER_ROTATION, 3)
            }
        }
        shortcutActivity()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mReceiver)
    }

    fun modifyBootIni() {
        val vout_mode = "setenv vout_mode \"hdmi\""

        val Blueled = "setenv led_onoff \"" + blueLed + "\""

        val lines = ArrayList<String>()
        var line: String? = null

        try {
            val f1 = File(BOOT_INI)
            val fr = FileReader(f1)
            val br = BufferedReader(fr)

            line = br.readLine()
            while (line != null) {
                if (line.startsWith("setenv vout_mode")) {
                    line = vout_mode
                }

                if (line.startsWith("setenv led_onoff")) {
                    line = Blueled
                }

                Log.e(TAG, line)

                lines.add(line + "\n")

                line = br.readLine()
            }
            fr.close()
            br.close()

            val fw = FileWriter(f1)
            val out = BufferedWriter(fw)
            for (s in lines)
                out.write(s)
            out.flush()
            out.close()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        Log.e(TAG, "Update boot.ini")
    }

    private fun reboot() {
        try {
            val pm = IPowerManager.Stub.asInterface(ServiceManager
                    .getService(Context.POWER_SERVICE))
            pm.reboot(false, null, false)
        } catch (e: RemoteException) {
            Log.e(TAG, "PowerManager service died!", e)
            return
        }

    }

    override fun onResume() {
        super.onResume()

        cpuActivity!!.onResume()
        val pref = getSharedPreferences("utility", Context.MODE_PRIVATE)
        cb_kodi.isChecked = pref.getBoolean("kodi", false)

        radioGroup_degree.visibility = if (radio_portrait.isChecked) View.VISIBLE else View.GONE
    }

    internal object ServerInfo {
        private var file: File? = null
        private val FILENAME = "server.cfg"

        init {
            file = File(context!!.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    FILENAME)

            if (!file!!.exists()) {
                try {
                    file!!.createNewFile()
                    write(UpdatePackage.remoteUrl)
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        }

        fun read(): String? {
            var text: String? = null

            try {
                val br = BufferedReader(FileReader(file!!))
                text = br.readLine()
                br.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return text
        }

        fun write(url: String) {
            try {
                val fw = FileWriter(file!!.absoluteFile)
                val bw = BufferedWriter(fw)
                bw.write(url)
                bw.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }

    /*
     * Request to retrive the latest update package version
     */
    fun checkLatestVersion() {
        val remote = UpdatePackage.remoteUrl

        /* Remove if the same file is exist */
        File(context!!.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                LATEST_VERSION).delete()

        try {
            val request = DownloadManager.Request(
                    Uri.parse(remote + LATEST_VERSION))
            request.setVisibleInDownloadsUi(false)
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
            request.setDestinationInExternalFilesDir(context,
                    Environment.DIRECTORY_DOWNLOADS,
                    LATEST_VERSION)

            enqueue = downloadManager!!.enqueue(request)
        } catch (e: IllegalArgumentException) {
            Toast.makeText(context,
                    "URL must be HTTP/HTTPS forms.",
                    Toast.LENGTH_SHORT).show()
        }

    }

    fun updatePckageFromOnline() {
        AlertDialog.Builder(this)
                .setTitle("New update package is found!")
                .setMessage("Do you want to download new update package?\n" + "It would take a few minutes or hours depends on your network speed.")
                .setPositiveButton("Download"
                ) { _, whichButton ->
                    if (sufficientSpace()) {
                        enqueue = m_updatePackage!!.requestDownload(context!!,
                                downloadManager!!)
                    }
                }
                .setCancelable(true)
                .create().show()
    }

    fun updatePackageFromStorage() {
        val intent = Intent()
        intent.action = Intent.ACTION_GET_CONTENT

        intent.type = "application/zip"
        intent.addCategory(Intent.CATEGORY_OPENABLE)

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a File to Update"),
                    FILE_SELECT_CODE)
        } catch (ex: android.content.ActivityNotFoundException) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(context,
                    "Please install a File Manager.",
                    Toast.LENGTH_SHORT).show()
        }

    }

    private fun installPackage(packageFile: File) {
        Log.e(TAG, "installPackage = " + packageFile.path)
        try {
            RecoverySystem.verifyPackage(packageFile, null, null)

            AlertDialog.Builder(this)
                    .setTitle("Selected package file is verified")
                    .setMessage("Your Android can be updated, do you want to proceed?")
                    .setPositiveButton("Proceed") { dialog, whichButton ->
                        try {
                            RecoverySystem.installPackage(context,
                                    packageFile)
                        } catch (e: Exception) {
                            Toast.makeText(context,
                                    "Error while install OTA package: " + e,
                                    Toast.LENGTH_LONG).show()
                        }
                    }
                    .setCancelable(true)
                    .create().show()
        } catch (e: Exception) {
            Toast.makeText(context,
                    "The package file seems to be corrupted!!\n" + "Please select another package file...",
                    Toast.LENGTH_LONG).show()
        }

    }

    private fun sufficientSpace(): Boolean {
        val stat = StatFs(UpdatePackage.getDownloadDir(context!!)!!.path)

        val available = stat.availableBlocks.toDouble() * stat.blockSize.toDouble()

        if (available < UpdatePackage.PACKAGE_MAXSIZE) {
            AlertDialog.Builder(this)
                    .setTitle("Check free space")
                    .setMessage("Insufficient free space!\nAbout ${UpdatePackage.PACKAGE_MAXSIZE / 1024 / 1024} MBytes free space is required.")
                    .setPositiveButton(android.R.string.yes) { _, _ -> finish() }
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show()

            return false
        }

        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        when (requestCode) {
            FILE_SELECT_CODE -> if (resultCode == Activity.RESULT_OK) {
                // Get the Uri of the selected file
                val uri = data.data
                val path = getPath(context, uri) ?: return
                installPackage(File(path))
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun shortcutActivity() {
        val pref = getSharedPreferences("utility", Context.MODE_PRIVATE)
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val pkg_f7 = pref.getString("shortcut_f7", null)
        val pkg_f8 = pref.getString("shortcut_f8", null)
        val pkg_f9 = pref.getString("shortcut_f9", null)
        val pkg_f10 = pref.getString("shortcut_f10", null)

        val appIntentList = getAvailableAppList(context)
        val appTitles = ArrayList<String>()

        appTitles.add("No shortcut")
        for (intent in appIntentList) {
            appTitles.add(intent.`package`)
        }

        val adapter = ApplicationAdapter(this, R.layout.applist_dropdown_item_1line, appTitles, appList!!)
        adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line)

        shortcut_f7.adapter = adapter
        shortcut_f8.adapter = adapter
        shortcut_f9.adapter = adapter
        shortcut_f10.adapter = adapter

        shortcut_f7.setSelection(appTitles.indexOf(pkg_f7))
        shortcut_f8.setSelection(appTitles.indexOf(pkg_f8))
        shortcut_f9.setSelection(appTitles.indexOf(pkg_f9))
        shortcut_f10.setSelection(appTitles.indexOf(pkg_f10))

        val listner = object : OnItemSelectedListener {

            override fun onItemSelected(spinner: AdapterView<*>, view: View, position: Int, arg3: Long) {
                val edit = pref.edit()
                var keycode = 0

                when (spinner.id) {
                    R.id.shortcut_f7 -> keycode = KeyEvent.KEYCODE_F7
                    R.id.shortcut_f8 -> keycode = KeyEvent.KEYCODE_F8
                    R.id.shortcut_f9 -> keycode = KeyEvent.KEYCODE_F9
                    R.id.shortcut_f10 -> keycode = KeyEvent.KEYCODE_F10
                }

                val shortcut_pref = "shortcut_f${keycode - KeyEvent.KEYCODE_F1 + 1}"

                if (position == 0) {
                    wm.setApplicationShortcut(keycode, null)
                    edit.putString(shortcut_pref, "No shortcut")
                } else {
                    wm.setApplicationShortcut(keycode, appIntentList[position - 1])
                    edit.putString(shortcut_pref, appIntentList[position - 1].`package`)
                }
                edit.commit()
            }

            override fun onNothingSelected(arg0: AdapterView<*>) {

            }
        }

        shortcut_f7.onItemSelectedListener = listner
        shortcut_f8.onItemSelectedListener = listner
        shortcut_f9.onItemSelectedListener = listner
        shortcut_f10.onItemSelectedListener = listner
    }

    companion object {

        private val TAG = "ODROIDUtility"

        val WINDOW_AXIS = "/sys/class/graphics/fb0/window_axis"
        val FREE_SCALE_AXIS = "/sys/class/graphics/fb0/free_scale_axis"
        val FREE_SCALE = "/sys/class/graphics/fb0/free_scale"
        val FREE_SCALE_VALUE = "0x10001"

        val DISP_CAP = "/sys/devices/virtual/amhdmitx/amhdmitx0/disp_cap"

        //private final static String BOOT_INI = Environment.getExternalStorageDirectory() + "/boot.ini";
        private val BOOT_INI = "/storage/internal/boot.ini"

        private var context: Context? = null

        private val LATEST_VERSION = "latestupdate_nougat"
        private val FILE_SELECT_CODE = 0

        //For sharedPreferences
        private val PREFERENCE_BOX_SETTING = "preference_box_settings"
        private val SWITCH_ON = "true"
        private val SWITCH_OFF = "false"
        private val SWITCH_CEC = "switch_cec"
        private val SWITCH_ONE_KEY_PLAY = "switch_one_key_play"
        //private static final String SWITCH_ONE_KEY_POWER_OFF = "switch_one_key_power_off";
        private val SWITCH_AUTO_POWER_ON = "switch_auto_power_on"
        private val SWITCH_AUTO_CHANGE_LANGUAGE = "switch_auto_change_languace"
        private val SWITCH_ONE_KEY_SHUTDOWN = "switch_one_key_shutdown"

        //For start service
        private val CEC_ACTION = "CEC_LANGUAGE_AUTO_SWITCH"

        private var cpuActivity: CpuActivity? = null
        private var updateActivity: UpdateActivity? = null

        fun checkBootINI() {
            val boot_ini = File(BOOT_INI)
            if (!boot_ini.exists()) {
                //SystemProperties.set("ctl.start", "makebootini");
            }
        }

        fun getPath(context: Context?, uri: Uri?): String? {
            val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

            // DocumentProvider
            if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
                if (isDownloadsDocument(uri)) {
                    val id = DocumentsContract.getDocumentId(uri)
                    val contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), java.lang.Long.valueOf(id)!!)
                    return getDataColumn(context, contentUri, null, null)
                }
            } else if ("file".equals(uri!!.scheme, ignoreCase = true)) {
                return uri.path
            }
            return null
        }

        fun getDataColumn(context: Context?, uri: Uri, selection: String?, selectionArgs: Array<String>?): String? {
            var cursor: Cursor? = null
            val column = "_data"
            val projection = arrayOf(column)
            try {
                cursor = context!!.contentResolver.query(uri, projection, selection, selectionArgs, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndexOrThrow(column)
                    return cursor.getString(index)
                }
            } finally {
                if (cursor != null)
                    cursor.close()
            }
            return null
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is DownloadsProvider.
         */
        fun isDownloadsDocument(uri: Uri?): Boolean {
            return "com.android.providers.downloads.documents" == uri!!.authority
        }

        private var appList: List<ApplicationInfo>? = null
        fun getAvailableAppList(context: Context?): List<Intent> {
            val pm = context!!.packageManager
            appList = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val launchApps = appList!!.mapNotNullTo(ArrayList()) { pm.getLaunchIntentForPackage(it.packageName) }

            val home = Intent(Intent.ACTION_MAIN)
            home.`package` = "home"
            launchApps.add(home)

            return launchApps
        }
    }
}