package ir.amir.applimiter.data

import android.graphics.Bitmap

data class AppEntry(
    val packageName: String,
    val label: String,
    val isGame: Boolean,
    val icon: Bitmap?
)
