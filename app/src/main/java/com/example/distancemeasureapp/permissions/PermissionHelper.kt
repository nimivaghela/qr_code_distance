package com.example.distancemeasureapp.permissions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat

class PermissionHelper(activity: Activity, permissions: Array<String>, requestCode: Int) {
    private var showRational: Boolean = false
    private var permissionCallback: PermissionCallback? = null
    private var activity: Activity? = activity
    private var permissions: Array<String>? = permissions
    private var REQUEST_CODE: Int = requestCode

    fun requestPermissions(
        activity: Activity,
        permissions: Array<String>,
        callback: PermissionCallback
    ) {
        if (ContextCompat.checkSelfPermission(
                activity,
                permissions[0]
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity, permissions,
                REQUEST_CODE
            )
        } else {
            callback.onPermissionGranted()
        }
    }

    fun requestPermissions(permissionCallback: PermissionCallback) {
        this.permissionCallback = permissionCallback
        checkPermissionsPresentInAndroidManifest()
        if (!checkSelfPermission(permissions)) {
            showRational = shouldShowRational(permissions)
            if (activity != null) {
                ActivityCompat.requestPermissions(
                    activity!!,
                    filterNotGrantedPermissions(permissions),
                    REQUEST_CODE
                )
            }
        } else {
            this.permissionCallback?.onPermissionGranted()
        }
    }

    private fun filterNotGrantedPermissions(permissions: Array<String>?): Array<String> {
        val notGrantedPermissions = ArrayList<String>()
        val context = activity as Context
        permissions?.forEach {
            if (ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED)
                notGrantedPermissions.add(it)
        }

        return notGrantedPermissions.toTypedArray()
    }

    fun checkSelfPermission(permissions: Array<String>?): Boolean {
        val context = activity as Context
        permissions?.forEach {
            if (ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED)
                return false
        }
        return true
    }

    private fun checkPermissionsPresentInAndroidManifest() {
        permissions?.forEach {
            if (!hasPermissionInManifest(it)) {
                permissionCallback?.onPermissionNotFoundInManifest()
            }
        }
    }

    private fun hasPermissionInManifest(permission: String): Boolean {
        val context = activity
        val info = context?.packageManager?.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        info?.let {
            it.requestedPermissions.forEach { requestedPermission ->
                if (requestedPermission == permission)
                    return true
            }
        }

        return false
    }


    fun onRequestPermissionResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE) {
            var denied = false
            val grantedPermissions = ArrayList<String>()

            for ((i, grantResult) in grantResults.withIndex()) {
                if (grantResult != PackageManager.PERMISSION_GRANTED)
                    denied = true
                else
                    grantedPermissions.add(permissions[i])
            }

            if (denied) {
                val currentRational = shouldShowRational(permissions)

                if (!showRational && !currentRational) {
                    permissionCallback?.onPermissionDeniedBySystem()
                } else {
                    if (grantedPermissions.isNotEmpty())
                        permissionCallback?.onIndividualPermissionGranted(permissions)
                    else
                        permissionCallback?.onPermissionDenied()
                }
            } else {
                permissionCallback?.onPermissionGranted()
            }
        }
    }

    private fun shouldShowRational(permissions: Array<String>?): Boolean {
        var currentRational = false
        permissions?.let {
            for (permission in it) {
                if (activity != null) {
                    if (shouldShowRequestPermissionRationale(activity!!, permission)) {
                        currentRational = true
                        break
                    }
                }
            }
        }

        return currentRational
    }

    fun openSettings() {
        val context = activity
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val uri: Uri = Uri.fromParts("package", context?.packageName, null)
        intent.data = uri
        context?.startActivity(intent)
    }

}

interface PermissionCallback {
    fun onPermissionGranted()
    fun onPermissionDenied()
    fun onPermissionDeniedBySystem()
    fun onIndividualPermissionGranted(grantedPermission: Array<String>)
    fun onPermissionNotFoundInManifest()
}


