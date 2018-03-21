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
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.Toast
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.board_activity.*
import kotlinx.android.synthetic.main.update_activity.*

class MainActivity : Activity() {

    private var blueLed = "on"

    private var downloadManager: DownloadManager? = null
    private var enqueue: Long = 0

    private var m_updatePackage: UpdatePackage? = null

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

        cb_kodi.setOnCheckedChangeListener { _, isChecked ->
            val pref = getSharedPreferences("utility", Context.MODE_PRIVATE)
            val editor = pref.edit()
            editor.putBoolean("kodi", isChecked)
            editor.commit()
        }

        val params = BootINI.getOption {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Not found boot.ini")
                    .setMessage("Check and Format Internal FAT storage?")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton("Yes") { dialog, which -> startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)) }
                    .setNegativeButton("No", null).show()

            return@getOption mapOf("vout" to "hdmi", "blueLed" to "on")
        }
        blueLed = params["blueLed"]!!

        blue_led.setOnCheckedChangeListener { buttonView, isChecked ->
            blueLed = if (isChecked) "on" else "off"
            blue_led.setText(if (isChecked) R.string.on else R.string.off)
            BootINI.modify(mapOf("blueLed" to blueLed))
        }

        blue_led.isChecked = blueLed == "on"
        blue_led.setText(if (blueLed == "on") R.string.on else R.string.off)

        button_apply_reboot.setOnClickListener {
            BootINI.modify(mapOf("blueLed" to blueLed))
            reboot()
        }

        button_check_online_update.setOnClickListener { checkLatestVersion() }

        button_package_install_from_storage.setOnClickListener { updatePackageFromStorage() }

        updateActivity = UpdateActivity(this, TAG)
        updateActivity!!.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mReceiver)
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

        val pref = getSharedPreferences("utility", Context.MODE_PRIVATE)
        cb_kodi.isChecked = pref.getBoolean("kodi", false)
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

    companion object {

        private val TAG = "ODROIDUtility"

        private var context: Context? = null

        private val LATEST_VERSION = "latestupdate_nougat"
        private val FILE_SELECT_CODE = 0

        private var updateActivity: UpdateActivity? = null

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
    }
}