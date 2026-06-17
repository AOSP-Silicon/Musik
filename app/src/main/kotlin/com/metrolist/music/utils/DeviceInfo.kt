package com.metrolist.music.utils

object DeviceInfo {
    val ARCHITECTURE =
        android.os.Build.SUPPORTED_ABIS.firstOrNull()?.lowercase() ?: "universal"
}
