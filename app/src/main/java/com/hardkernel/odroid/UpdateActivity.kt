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

class UpdateActivity():Activity() {
    private val tag = "ODROIDUtility"
    private var downloadManager: DownloadManager? = null
    private var enqueue: Long = 0
    private var m_updatePackage: UpdatePackage? = null

    companion object {
        private var checkCustomServer = false
        private val LATEST_VERSION = "latestupdate_nougat"
        private val FILE_SELECT_CODE = 0

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
                cursor = (this as Context).contentResolver.query(uri, projection, selection, selectionArgs, null)
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

    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L)
            if (id != enqueue) {
                Log.v(tag, "Ingnoring unrelated download " + id)
                return
            }

            val query = DownloadManager.Query()
            query.setFilterById(id)
            val cursor = downloadManager!!.query(query)

            if (!cursor.moveToFirst()) {
                Log.e(tag, "Not able to move the cursor for downloaded content.")
                return
            }

            val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
            if (DownloadManager.ERROR_INSUFFICIENT_SPACE == status) {
                Log.e(tag, "Download is failed due to insufficient space")
                return
            }
            if (DownloadManager.STATUS_SUCCESSFUL != status) {
                Log.e(tag, "Download Failed")
                return
            }

            /* Get URI of downloaded file */
            val uri = Uri.parse(cursor.getString(
                    cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)))

            cursor.close()

            val file = File(uri.path)
            if (!file.exists()) {
                Log.e(tag, "Not able to find downloaded file: " + uri.path)
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
                    Log.d(tag, e.toString())
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
        setContentView(R.layout.update_activity)

        val url = ServerInfo.read()
        if (url == null)
            ServerInfo.write(UpdatePackage.remoteUrl)

        downloadManager = getSystemService(
                Context.DOWNLOAD_SERVICE) as DownloadManager

        registerReceiver(mReceiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        button_check_online_update.setOnClickListener { checkLatestVersion() }

        button_package_install_from_storage.setOnClickListener { updatePackageFromStorage() }


        val btn = button_update_url
        val editText = edittext

        fun selectServer(server:String, edit:Boolean) {
            checkCustomServer = edit
            editText.setText(server, TextView.BufferType.NORMAL)
            editText.isEnabled = edit

            btn.isEnabled = edit
            val url = server

            ServerInfo.write(url)
            UpdatePackage.remoteUrl =url
        }

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

        btn.setOnClickListener {
            val url = editText.text.toString()

            val pref = getSharedPreferences("utility", MODE_PRIVATE)
            val editor = pref.edit()

            if (checkCustomServer) {
                editor.putString("custom_server", url)
                editor.putBoolean("custom_server_rb", true)
            } else {
                editor.putBoolean("custom_server_rb", false)
            }
            editor.commit()

            ServerInfo.write(url)
            UpdatePackage.remoteUrl = url
        }

        val pref = getSharedPreferences("utility", MODE_PRIVATE)
        checkCustomServer = pref.getBoolean("custom_server_rb", false)

        if (checkCustomServer) {
            rb_custom_server.isChecked = true
            editText.setText(pref.getString("custom_server", UpdatePackage.remoteUrl),
                    TextView.BufferType.EDITABLE)

        } else {
            rb_mirror_server.isChecked = true
            editText.setText(UpdatePackage.remoteUrl, TextView.BufferType.EDITABLE)
            editText.isEnabled = false
        }
    }

    internal object ServerInfo {
        private var file: File? = null
        private val FILENAME = "server.cfg"

        init {
            file = File((this as Context).getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
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
        File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                LATEST_VERSION).delete()

        try {
            val request = DownloadManager.Request(
                    Uri.parse(remote + LATEST_VERSION))
            request.setVisibleInDownloadsUi(false)
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
            request.setDestinationInExternalFilesDir(this,
                    Environment.DIRECTORY_DOWNLOADS,
                    LATEST_VERSION)

            enqueue = downloadManager!!.enqueue(request)
        } catch (e: IllegalArgumentException) {
            Toast.makeText(this,
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
                        enqueue = m_updatePackage!!.requestDownload(this,
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
            Toast.makeText(this,
                    "Please install a File Manager.",
                    Toast.LENGTH_SHORT).show()
        }

    }

    private fun installPackage(packageFile: File) {
        Log.e(tag, "installPackage = " + packageFile.path)
        try {
            RecoverySystem.verifyPackage(packageFile, null, null)

            AlertDialog.Builder(this)
                    .setTitle("Selected package file is verified")
                    .setMessage("Your Android can be updated, do you want to proceed?")
                    .setPositiveButton("Proceed") { dialog, whichButton ->
                        try {
                            RecoverySystem.installPackage(this,
                                    packageFile)
                        } catch (e: Exception) {
                            Toast.makeText(this,
                                    "Error while install OTA package: " + e,
                                    Toast.LENGTH_LONG).show()
                        }
                    }
                    .setCancelable(true)
                    .create().show()
        } catch (e: Exception) {
            Toast.makeText(this,
                    "The package file seems to be corrupted!!\n" + "Please select another package file...",
                    Toast.LENGTH_LONG).show()
        }
    }

    private fun sufficientSpace(): Boolean {
        val stat = StatFs(UpdatePackage.getDownloadDir(this)!!.path)

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
                val path = getPath(this, uri) ?: return
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