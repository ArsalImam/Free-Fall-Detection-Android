package io.xbird.library.enum

import android.content.Context
import android.content.SharedPreferences
import org.apache.commons.lang3.StringUtils

enum class ServiceState {
    STARTED,
    STOPPED,
}

private const val name = "SERVICE_STATE_NAME"
private const val key = "SERVICE_STATE_KEY"

fun setServiceState(context: Context, state: ServiceState) {
    val sharedPrefs = getPreferences(context)
    sharedPrefs.edit().let {
        it.putString(key, state.name)
        it.apply()
    }
}

fun getServiceState(context: Context): ServiceState {
    val sharedPrefs = getPreferences(context)
    val value = sharedPrefs.getString(key, ServiceState.STOPPED.name)
    return ServiceState.valueOf(value ?: StringUtils.EMPTY)
}

private fun getPreferences(context: Context): SharedPreferences {
    return context.getSharedPreferences(name, 0)
}