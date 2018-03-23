package com.hardkernel.odroid

import java.io.File

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.util.Log

internal class UpdatePackage(packageName: String) {
    private val header = "updatepackage"
    private val model = "odroidn1"
    private val variant = "eng"
    private val branch = "rk3399_7.1.2_master"

    internal var buildNumber = -1
    internal var downloadId: Long = -1
    private var validPackageName: String? = null

    init {
        val packageNameChunk = packageName.split("-".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
        if (validatePackageName(packageNameChunk)) {
            buildNumber = Integer.parseInt(packageNameChunk[4].split("\\.".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[0])
            validPackageName = "$header-$model-$variant-$branch-$buildNumber.zip"
        }
    }

    private fun validatePackageName(chunk: Array<String>): Boolean {
        if (chunk.size <= 4)
            return false
        if (chunk[0] != header
                ||chunk[1] != model
                || chunk[2] != variant
                || chunk[3] != branch) {
            return false
        }
        return true
    }

    fun localUri(context: Context): Uri {
        return Uri.parse("file://${getDownloadDir(context)}/update.zip")
    }

    /*
     * Request to download update package if necessary
     */
    fun requestDownload(context: Context, dm: DownloadManager): Long {
        val name = validPackageName ?: return 0

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

        downloadId = dm.enqueue(request)

        return downloadId
    }

    /*
     *  * Request to retrive the latest update package version
     */
    @Throws(IllegalArgumentException::class)
    fun checkLatestVersion(context:Context, dm: DownloadManager): Long {
        File(getDownloadDir(context), LATEST_VERSION).delete()

        val request = DownloadManager.Request(
                Uri.parse(mRemoteUrl + LATEST_VERSION))
        request.setVisibleInDownloadsUi(false)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
        request.setDestinationInExternalFilesDir(context,
                Environment.DIRECTORY_DOWNLOADS,
                LATEST_VERSION)

        return dm.enqueue(request)
    }

    companion object {
        private const val TAG = "UpdatePackage"

        const val LATEST_VERSION = "latestupdate_nougat"
        const val PACKAGE_MAXSIZE = (500 * 1024 * 1024).toLong()   /* 500MB */
        const val OFFICAL_SERVER_URL = "https://dn.odroid.com/RK3399/Android/ODROID-N1/"
        const val MIRROR_SERVER_URL = "https://www.odroid.in/mirror/dn.odroid.com/RK3399/Android/ODROID-N1/"

        private var mRemoteUrl = MIRROR_SERVER_URL

        fun getDownloadDir(context: Context): File {
            return context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        }

        fun availableSpace(context: Context):Long {
            val state = StatFs(getDownloadDir(context).path)
            return state.availableBlocksLong * state.blockSizeLong
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
