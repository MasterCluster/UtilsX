/*
 * Copyright (C) 2022 Leonid Belousov / mastercluster.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
 * [BasePermissionRequester]
 *
 * Encapsulates the runtime permission(s) request flow.
 * Handles the granted and rejected flow paths providing the appropriate callbacks.
 *
 *
 * Usage example:
 *
 * private val permissionRequester = object : BasePermissionRequester(
 *     activity,
 *     arrayOf(
 *         Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
 *     )
 * ) {
 *     override fun onPermissionGranted() {
 *         // All permissions granted, ok to proceed!
 *     }
 *
 *     override fun onShowPermissionNotGranted(context: Context) {
 *         // The user denied to grant permission(s) when prompted.
 *         // Show a dialog explaining why the app requires that permission(s).
 *     }
 *
 *     override fun onShowPermissionRationale(context: Context) {
 *         // User chose to not grant the permission(s) permanently,
 *         // so the system won't show the permission prompt again.
 *         // The user has to grant the permission(s) manually in the app settings.
 *         // Show a dialog with the instructions.
 *     }
 * }
 *
 *
 * How to call:
 *
 * permissionRequester.tryRequestAndContinue()
 *
 */

abstract class BasePermissionRequester(
    private val activity: ComponentActivity,
    private val requiredPermissions: Array<String>
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
                onPermissionGranted()
            } else {
                onShowPermissionNotGranted(activity)
            }
        }
    }

    fun tryRequestAndContinue() {
        when {
            hasRequiredPermissions(activity) -> {
                onPermissionGranted()
            }
            shouldShowRequestPermissionRationale(activity) -> {
                onShowPermissionRationale(activity)
            }
            else -> {
                resultLauncher.launch(requiredPermissions)
            }
        }
    }

    abstract fun onPermissionGranted()

    abstract fun onShowPermissionRationale(context: Context)

    abstract fun onShowPermissionNotGranted(context: Context)


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