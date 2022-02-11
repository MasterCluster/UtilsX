package com.mastercluster.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * 2022 (c) Leonid Belousov / mastercluster.com
 *
 * Usage example:
 *
 * private val permissionRequester = PermissionFlowLifecycleObserver(activity) {
 *       // Permission(s) granted
 * }
 *
 * ...
 * permissionRequester.requestPermissions()
 *
 */

abstract class BasePermissionFlowLifecycleObserver(
    private val activity: ComponentActivity,
    private val requiredPermissions: Array<String>,
    private val onPermissionGranted: () -> Unit
) : DefaultLifecycleObserver {

    private lateinit var resultLauncher: ActivityResultLauncher<Array<String>>

    init {
        startObserving()
    }

    private fun startObserving() {
        activity.lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        resultLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (hasRequiredPermissions(permissions)) {
                onPermissionGranted.invoke()
            } else {
                showPermissionNotGranted(activity)
            }
        }
    }

    fun requestPermissions() {
        when {
            hasRequiredPermissions(activity) -> {
                onPermissionGranted.invoke()
            }
            shouldShowRequestPermissionRationale(activity) -> {
                showPermissionRationale(activity)
            }
            else -> {
                resultLauncher.launch(requiredPermissions)
            }
        }
    }

    abstract fun showPermissionRationale(context: Context)

    abstract fun showPermissionNotGranted(context: Context)

    private fun hasRequiredPermissions(context: Context) =
        requiredPermissions.all {
            ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    private fun hasRequiredPermissions(permissions: Map<String, Boolean>) =
        requiredPermissions.all {
            permissions[it] == true
        }

    private fun shouldShowRequestPermissionRationale(activity: Activity) =
        requiredPermissions.any {
            ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
        }

}
