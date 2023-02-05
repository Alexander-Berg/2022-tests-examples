package ru.yandex.yandexbus.inhouse.utils.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import ru.yandex.yandexbus.inhouse.BaseTest
import ru.yandex.yandexbus.inhouse.whenever
import rx.observers.TestSubscriber

@Suppress("DEPRECATION") // we anyway use ConnectivityManager api
class NetworkInfoProviderTest : BaseTest() {

    @Mock
    lateinit var contextMock: Context

    @Mock
    lateinit var connectivityManager: ConnectivityManager

    private lateinit var registeredReceiver: BroadcastReceiver
    private lateinit var networkInfoProvider: NetworkInfoProvider
    private lateinit var testSubscriber: TestSubscriber<NetworkInfoProvider.Event>

    override fun setUp() {
        super.setUp()

        whenever(contextMock.applicationContext).thenReturn(contextMock)
        whenever(contextMock.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivityManager)
        whenever(contextMock.registerReceiver(any(), any())).thenAnswer(CapturingAnswer())

        testSubscriber = TestSubscriber()
        networkInfoProvider = NetworkInfoProviderImpl(contextMock)
    }

    @Test
    fun checkOnlyOneReceiverRegistered() {

        val subscribers = listOf(testSubscriber, TestSubscriber())

        subscribers.forEach { networkInfoProvider.networkChanges.subscribe(it) }

        subscribers.forEach {
            with(it) {
                assertNoValues()
                assertNoErrors()
                assertNotCompleted()
            }
        }
        verify(contextMock, times(1)).registerReceiver(eq(registeredReceiver), any())

        subscribers.forEach { it.unsubscribe() }
        verify(contextMock, times(1)).unregisterReceiver(registeredReceiver)
    }

    @Test
    fun properEventsForInitialConnectToWifi() {
        networkInfoProvider.networkChanges.subscribe(testSubscriber)

        connectToWifi("my_network")

        testSubscriber.assertOnlyValueAndClear(NetworkInfoProvider.Event.CONNECTED_OR_CONNECTING)
    }

    @Test
    fun properEventsForConnectToAnotherWifi() {
        networkInfoProvider.networkChanges.subscribe(testSubscriber)

        connectToWifi("my_network")
        testSubscriber.assertOnlyValuesAndClear(NetworkInfoProvider.Event.CONNECTED_OR_CONNECTING)

        reconnectToWifi("another_network")
        testSubscriber.assertOnlyValuesAndClear(
                NetworkInfoProvider.Event.DISCONNECTED,NetworkInfoProvider.Event.CONNECTED_OR_CONNECTING)
    }

    @Test
    fun properEventsForChangingNetworkType() {
        networkInfoProvider.networkChanges.subscribe(testSubscriber)

        connectToWifi("my_network")
        testSubscriber.assertOnlyValuesAndClear(NetworkInfoProvider.Event.CONNECTED_OR_CONNECTING)

        reconnectToMobile("mobile_network")
        testSubscriber.assertOnlyValuesAndClear(
                NetworkInfoProvider.Event.DISCONNECTED, NetworkInfoProvider.Event.CONNECTED_OR_CONNECTING)

        reconnectToWifi("another_network")
        testSubscriber.assertOnlyValuesAndClear(
                NetworkInfoProvider.Event.DISCONNECTED, NetworkInfoProvider.Event.CONNECTED_OR_CONNECTING)
    }

    @Test
    fun onlyOneEventWifiWithoutInternetAccess() {
        networkInfoProvider.networkChanges.subscribe(testSubscriber)

        connectToWifiWithoutInternetAccess("my_network")

        testSubscriber.assertOnlyValueAndClear(NetworkInfoProvider.Event.CONNECTED_OR_CONNECTING)
    }

    private fun connectToWifiWithoutInternetAccess(networkName: String) {
        //this is how it works on a real device: CONNECTIVITY_CHANGED with same data many times each second
        for (i in 0..10) {
            connectToWifi(networkName)
        }
    }

    private fun reconnectToWifi(networkName: String) {
        //this is how it works on a real device
        disconnect()
        connectToWifi(networkName)
    }

    private fun reconnectToMobile(networkName: String) {
        //this is how it works on a real device
        disconnect()
        connectToMobile(networkName)
    }

    private fun connectToWifi(networkName: String) {
        connectTo(networkName, ConnectivityManager.TYPE_WIFI)
    }

    private fun connectToMobile(networkName: String) {
        connectTo(networkName, ConnectivityManager.TYPE_MOBILE)
    }

    private fun disconnect() {
        val networkInfo = createNetworkInfo(connected = false)
        whenever(connectivityManager.activeNetworkInfo).thenReturn(networkInfo)

        registeredReceiver.onReceive(contextMock, Intent(ConnectivityManager.CONNECTIVITY_ACTION).apply {
            putExtra(ConnectivityManager.EXTRA_NETWORK, networkInfo)
            putExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, true)
        })
    }

    private fun connectTo(networkName: String, type: Int) {

        val networkInfo = createNetworkInfo(connected = true, type = type, name = networkName)
        whenever(connectivityManager.activeNetworkInfo).thenReturn(networkInfo)

        registeredReceiver.onReceive(contextMock, Intent(ConnectivityManager.CONNECTIVITY_ACTION).apply {
            putExtra(ConnectivityManager.EXTRA_NETWORK, networkInfo)
        })
    }

    private fun createNetworkInfo(connected: Boolean, type: Int = -1, name: String = ""): NetworkInfo {
        val networkInfo = mock(NetworkInfo::class.java)
        whenever(networkInfo.extraInfo).thenReturn(name)
        whenever(networkInfo.type).thenReturn(type)
        whenever(networkInfo.isConnectedOrConnecting).thenReturn(connected)
        return networkInfo
    }

    private fun <T> TestSubscriber<T>.assertOnlyValueAndClear(value: T) {
        assertNotCompleted()
        assertNoErrors()
        assertValuesAndClear(value)
    }
    private fun <T> TestSubscriber<T>.assertOnlyValuesAndClear(vararg values: T) {
        assertNotCompleted()
        assertNoErrors()
        assertValuesAndClear(values[0], *(values.sliceArray(1 until values.size)))
    }

    inner class CapturingAnswer : Answer<Intent> {
        override fun answer(invocation: InvocationOnMock?): Intent {
            registeredReceiver = invocation!!.arguments[0] as BroadcastReceiver
            return Intent()
        }
    }
}