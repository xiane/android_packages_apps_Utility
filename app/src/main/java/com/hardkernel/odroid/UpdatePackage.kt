package com.hardkernel.odroid

import java.io.File

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log

internal class UpdatePackage {
    private var m_buildNumber = -1
    private var m_downloadId: Long = -1

    private val HEADER = "updatepackage"
    private val MODEL = "odroidn1"
    private val VARIANT = "eng"
    private val BRANCH = "rk3399_7.1.2_master"

    constructor(packageName: String) {
        val s = packageName.split("-".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
        if (s.size <= 4)
            return

        if (s[0] != HEADER || s[1] != MODEL ||
                s[2] != VARIANT || s[3] != BRANCH)
            return

        buildNumber = Integer.parseInt(s[4].split("\\.".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[0])
    }

    var buildNumber: Int
    get() {
        return m_buildNumber
    }
    set(value) {
        Log.d(TAG, "Build Number is set as $value")
        m_buildNumber = value
    }

    private val packageName: String?
    get() {
        return if (m_buildNumber == -1) null else "$HEADER-$MODEL-$VARIANT-$BRANCH-${Integer.toString(m_buildNumber)}.zip"
    }

    fun localUri(context: Context): Uri {
        return Uri.parse("file://${getDownloadDir(context)}/update.zip")
    }

    fun downloadId(): Long {
        return m_downloadId
    }

    /*
     * Request to download update package if necessary
     */
    fun requestDownload(context: Context, dm: DownloadManager): Long {
        val name = packageName ?: return 0

        val uri = Uri.parse(remoteUrl + name)

        val request = DownloadManager.Request(uri)
        request.setTitle("Downloading new update package")
        request.setDescription(uri.path)
        request.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationToSystemCache()
        request.setDestinationUri(localUri(context))

        Log.d(TAG, "Requesting to download ${uri.path} to ${localUri(context)}")

        /* Remove if the same file is exist */
        val file = File(localUri(context).path)
        if (file.exists())
            file.delete()

        m_downloadId = dm.enqueue(request)

        return m_downloadId
    }

    companion object {
        private val TAG = "UpdatePackage"

        val PACKAGE_MAXSIZE = (500 * 1024 * 1024).toLong()   /* 500MB */

        val OFFICAL_SERVER_URL = "https://dn.odroid.com/RK3399/Android/ODROID-N1/"
        val MIRROR_SERVER_URL = "https://www.odroid.in/mirror/dn.odroid.com/RK3399/Android/ODROID-N1/"

        private var mRemoteUrl = MIRROR_SERVER_URL

        fun getDownloadDir(context: Context): File? {
            return context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        }

        var remoteUrl: String
        set(url){
             mRemoteUrl = url
        }
        get() {
            return mRemoteUrl
        }
    }
}
