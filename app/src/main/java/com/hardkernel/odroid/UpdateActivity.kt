package com.hardkernel.odroid

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.*
import android.database.Cursor
import android.net.Uri
import android.os.*
import android.provider.DocumentsContract
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.update_activity.*
import java.io.*

@SuppressLint("Registered")
class UpdateActivity:Activity() {
    private val tag = "ODROIDUtility"
    private lateinit var downloadManager:DownloadManager
    private var enqueue: Long = 0
    private lateinit var updatePackage: UpdatePackage

    companion object {
        private var checkCustomServer = false
        private const val FILE_SELECT_CODE = 0
        @SuppressLint("StaticFieldLeak")
        private lateinit var context:Context

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

        private fun getDataColumn(context: Context?, uri: Uri, selection: String?, selectionArgs: Array<String>?): String? {
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
        private fun isDownloadsDocument(uri: Uri?): Boolean {
            return "com.android.providers.downloads.documents" == uri!!.authority
        }
    }

    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L)
            if (id != enqueue) {
                Log.v(tag, "Ingnoring unrelated download $id")
                return
            }

            val query = DownloadManager.Query()
            query.setFilterById(id)
            val cursor = downloadManager.query(query)

            if (!cursor.moveToFirst()) {
                Log.e(tag, "Not able to move the cursor for downloaded content.")
                return
            }

            val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
            when {
                DownloadManager.ERROR_INSUFFICIENT_SPACE == status -> {
                    Log.e(tag, "Download is failed due to insufficient space")
                    return
                }
                DownloadManager.STATUS_SUCCESSFUL != status -> {
                    Log.e(tag, "Download Failed")
                    return
                }
            }

            /* Get URI of downloaded file */
            val uri = Uri.parse(cursor.getString(
                    cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)))

            cursor.close()

            val file = File(uri.path)
            if (!file.exists()) {
                Log.e(tag, "Not able to find downloaded file: ${uri.path}")
                return
            }

            if (file.name == UpdatePackage.LATEST_VERSION) {
                try {
                    val text = StringBuilder()

                    val br = file.bufferedReader()
                    text.append(br.readLine())
                    br.close()

                    updatePackage = UpdatePackage(text.toString())

                    var currentVersion = 0
                    val version = Build.VERSION.INCREMENTAL.split("-".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                    if (version.size < 4) {
                        toastLongMessage("Not able to detect the version number installed. " + "Remote package will be installed anyway!")
                    } else {
                        currentVersion = Integer.parseInt(version[3])
                    }

                    when {
                        currentVersion < updatePackage.buildNumber -> updatePckageFromOnline(context)
                        currentVersion == updatePackage.buildNumber ->
                            toastLongMessage("The current installed build number might be wrong")
                        currentVersion > updatePackage.buildNumber ->
                                toastLongMessage("Already latest Android image is installed.")
                    }
                } catch (e: IOException) {
                    Log.d(tag, e.toString())
                    e.printStackTrace()
                }

            } else if (id == updatePackage.downloadId) {
                /* Update package download is done, time to install */
                installPackage(File(updatePackage.localUri(context).path))
            }
        }
    }

    @SuppressLint("ApplySharedPref")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.update_activity)

        context = applicationContext

        val url = ServerInfo.read()
        if (url == null)
            ServerInfo.write(UpdatePackage.remoteUrl)

        downloadManager = getSystemService(
                Context.DOWNLOAD_SERVICE) as DownloadManager

        registerReceiver(mReceiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        button_check_online_update.setOnClickListener {
            try {
                enqueue = updatePackage.checkLatestVersion(context, downloadManager)
            } catch (e:IllegalArgumentException) {
                toastShortMessage("URL must be HTTP/HTTPS forms.")
            }
        }
        button_package_install_from_storage.setOnClickListener { updatePackageFromStorage() }

        rb_offical_server.setOnClickListener {
            selectServer(
                    UpdatePackage.OFFICAL_SERVER_URL,
                    false
            )
        }
        rb_mirror_server.setOnClickListener {
            selectServer(
                    UpdatePackage.MIRROR_SERVER_URL,
                    false
            )
        }
        rb_custom_server.setOnClickListener {
            val pref = getSharedPreferences("utility",
                    MODE_PRIVATE)
            selectServer(pref.getString("custom_server", UpdatePackage.MIRROR_SERVER_URL),
                    true)
        }

        button_update_url.setOnClickListener {
            val serverUrl = edittext.text.toString()

            val pref = getSharedPreferences("utility", MODE_PRIVATE)
            val editor = pref.edit()

            if (checkCustomServer) {
                editor.putString("custom_server", serverUrl)
                editor.putBoolean("custom_server_rb", true)
            } else {
                editor.putBoolean("custom_server_rb", false)
            }
            editor.commit()

            ServerInfo.write(serverUrl)
            UpdatePackage.remoteUrl = serverUrl
        }

        val pref = getSharedPreferences("utility", MODE_PRIVATE)
        checkCustomServer = pref.getBoolean("custom_server_rb", false)

        if (checkCustomServer) {
            rb_custom_server.isChecked = true
            edittext.setText(pref.getString("custom_server", UpdatePackage.remoteUrl),
                    TextView.BufferType.EDITABLE)
        } else {
            rb_mirror_server.isChecked = true
            edittext.setText(UpdatePackage.remoteUrl, TextView.BufferType.EDITABLE)
            edittext.isEnabled = false
        }
    }

    private fun selectServer(server:String, edit:Boolean) {
        checkCustomServer = edit
        edittext.setText(server, TextView.BufferType.NORMAL)
        edittext.isEnabled = edit

        button_update_url.isEnabled = edit

        ServerInfo.write(server)
        UpdatePackage.remoteUrl = server
    }

    internal object ServerInfo {
        private var file: File? = null
        private const val FILENAME = "server.cfg"

        init {
            file = File(UpdatePackage.getDownloadDir(context), FILENAME)

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

            val br = file?.bufferedReader()
            if (br != null) {
                br.use {
                    text = it.readLine()
                }
                br.close()
            }
            return text
        }

        fun write(url: String) {
            val fw = FileWriter(file!!.absoluteFile)
            val bw = BufferedWriter(fw)
            bw.use {
                it.write(url)
                it.close()
            }
            bw.write(url)
            bw.close()
        }
    }

    fun updatePckageFromOnline(context:Context) {
        AlertDialog.Builder(context)
                .setTitle("New update package is found!")
                .setMessage("Do you want to download new update package?\n" + "It would take a few minutes or hours depends on your network speed.")
                .setPositiveButton("Download"
                ) { _, _ ->
                    if (sufficientSpace()) {
                        enqueue = updatePackage.requestDownload(context,
                                downloadManager)
                    }
                }
                .setCancelable(true)
                .create().show()
    }

    private fun updatePackageFromStorage() {
        try {
            startActivityForResult(
                    Intent.createChooser(finderIntent, "Select a File to Update"),
                    FILE_SELECT_CODE)
        } catch (ex: android.content.ActivityNotFoundException) {
            // Potentially direct the user to the Market with a Dialog
            toastShortMessage("Please install a File Manager.")
        }
    }

    private val finderIntent:Intent
        get() {
            val intent = Intent()
            intent.action = Intent.ACTION_GET_CONTENT
            intent.type = "application/zip"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            return intent
        }

    private fun installPackage(packageFile: File) {
        Log.e(tag, "installPackage = ${packageFile.path}")
        try {
            RecoverySystem.verifyPackage(packageFile, null, null)

            AlertDialog.Builder(context)
                    .setTitle("Selected package file is verified")
                    .setMessage("Your Android can be updated, do you want to proceed?")
                    .setPositiveButton("Proceed") { _, _ ->
                        try {
                            RecoverySystem.installPackage(context,
                                    packageFile)
                        } catch (e: Exception) {
                            toastLongMessage("Error while install OTA package: $e")
                        }
                    }
                    .setCancelable(true)
                    .create().show()
        } catch (e: Exception) {
            toastLongMessage("The package file seems to be corrupted!!\n" + "Please select another package file...")
        }
    }

    private fun sufficientSpace(): Boolean {
        if (UpdatePackage.availableSpace(context) < UpdatePackage.PACKAGE_MAXSIZE) {
            AlertDialog.Builder(context)
                    .setTitle("Check free space")
                    .setMessage("Insufficient free space!\nAbout ${UpdatePackage.PACKAGE_MAXSIZE / 1024 / 1024} MBytes free space is required.")
                    .setPositiveButton(android.R.string.yes) { _, _ -> finish() }
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show()

            return false
        }
        return true
    }

    private fun toastLongMessage(text: String) { toastMessage(text, Toast.LENGTH_LONG) }
    private fun toastShortMessage(text: String) { toastMessage(text, Toast.LENGTH_SHORT) }
    private fun toastMessage(text: String, period:Int) { Toast.makeText(this, text, period).show() }

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

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mReceiver)
    }
}