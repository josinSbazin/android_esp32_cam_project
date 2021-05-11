package ru.rhanza.cams.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Device(val name: String) : Parcelable