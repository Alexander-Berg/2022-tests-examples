package com.yandex.android.advid.service

import android.os.IBinder
import android.os.Parcel
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class YandexAdvIdInterfaceMethodOrderingTest {

    private val interfaceMock = mock(YandexAdvIdInterface::class.java).apply {
        doReturn("id").`when`(this).advId
    }

    private val stubImpl = object: YandexAdvIdInterface.Stub() {
        override fun isAdvIdTrackingLimited(): Boolean {
            return interfaceMock.isAdvIdTrackingLimited
        }

        override fun getAdvId(): String {
            return interfaceMock.advId
        }

    }

    @Test
    fun testGetAdvId() {
        stubImpl.onTransact(IBinder.FIRST_CALL_TRANSACTION, mock(Parcel::class.java), mock(Parcel::class.java), 0);
        verify(interfaceMock).getAdvId()
    }

    @Test
    fun testIsTrackingLimited() {
        stubImpl.onTransact(IBinder.FIRST_CALL_TRANSACTION + 1, mock(Parcel::class.java), mock(Parcel::class.java), 0);
        verify(interfaceMock).isAdvIdTrackingLimited()
    }

    @Test
    fun testUnknownMethod() {
        stubImpl.onTransact(IBinder.FIRST_CALL_TRANSACTION + 2, mock(Parcel::class.java), mock(Parcel::class.java), 0);
        verifyZeroInteractions(interfaceMock)
    }

}
