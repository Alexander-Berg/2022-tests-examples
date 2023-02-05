package com.yandex.metrokit.testapp.common

import android.content.res.Resources
import com.yandex.metrokit.testapp.R

fun formatByteSize(res: Resources, size: Int): String {
    var convertingSize = size

    if (convertingSize < 1024) {
        return res.getString(R.string.bytes, convertingSize)
    }
    convertingSize /= 1024
    if (convertingSize < 1024) {
        return res.getString(R.string.kilobytes, convertingSize)
    }
    convertingSize /= 1024
    if (convertingSize < 1024) {
        return res.getString(R.string.megabytes, convertingSize)
    }
    return res.getString(R.string.gigabytes, convertingSize / 1024.0)
}
