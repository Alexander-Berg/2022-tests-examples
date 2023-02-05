package com.yandex.mail.util

import android.os.SystemClock
import androidx.annotation.CheckResult
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.whenever
import java.util.Calendar

/**
 * Returns spy on passed fragment with methods [Fragment.getActivity] and [Fragment.getContext] returning spied version of activity.
 *
 * @param fragment fragment to create spy on
 * @param activityMock function which will mock some methods of activity using Mockito
 */
@CheckResult
fun <T : Fragment> mockActivity(fragment: T, activityMock: (FragmentActivity) -> Unit = {}): T =
    spy(fragment).also { mockedFragment ->
        val mockedActivity = spy(fragment.activity)!!.also { activity ->
            activityMock.invoke(activity)
        }
        whenever(mockedFragment.activity).thenReturn(mockedActivity)
        whenever(mockedFragment.context).thenReturn(mockedActivity)
    }

fun setDate(year: Int, month: Int, dayOfMonth: Int) {
    val calendar = Calendar.getInstance()
    calendar.set(year, month, dayOfMonth)
    SystemClock.setCurrentTimeMillis(calendar.timeInMillis)
}
