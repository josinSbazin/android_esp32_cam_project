package ru.rhanza.cams

import android.content.Context
import ru.rhanza.cams.data.Device
import ru.rhanza.cams.devices.DeviceActivity


class Router(private val context: Context) {
    fun toDevicePage(device: Device) {
        context.startActivity(DeviceActivity.newIntent(context, device))
    }
}