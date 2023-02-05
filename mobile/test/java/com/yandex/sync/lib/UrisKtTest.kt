package com.yandex.sync.lib

import android.accounts.Account
import android.provider.CalendarContract.CALLER_IS_SYNCADAPTER
import android.provider.CalendarContract.Calendars
import com.yandex.sync.lib.utils.SyncTestRunner
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(SyncTestRunner::class)
class UrisKtTest {

    @Test
    fun asSyncAdapter() {
        val account = Account("name", "type")
        val uri = Calendars.CONTENT_URI.asSyncAdapter(account)

        assertEquals("true", uri.getQueryParameter(CALLER_IS_SYNCADAPTER))
        assertEquals(account.name, uri.getQueryParameter(Calendars.ACCOUNT_NAME))
        assertEquals(account.type, uri.getQueryParameter(Calendars.ACCOUNT_TYPE))
    }
}
