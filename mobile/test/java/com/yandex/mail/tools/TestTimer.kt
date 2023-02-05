package com.yandex.mail.tools

import android.os.SystemClock
import com.yandex.xplat.xmail.HighPrecisionTimer

class TestTimer : HighPrecisionTimer {
    override fun getCurrentTimestampInMillis(): Long {
        return SystemClock.currentThreadTimeMillis()
    }
}
