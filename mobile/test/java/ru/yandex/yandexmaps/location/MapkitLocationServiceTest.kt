package ru.yandex.yandexmaps.location

import com.gojuno.koptional.toOptional
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.location.Location
import com.yandex.mapkit.location.LocationListener
import com.yandex.mapkit.location.LocationManager
import io.reactivex.Observable
import io.reactivex.schedulers.TestScheduler
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyDouble
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import ru.yandex.yandexmaps.app.lifecycle.AppLifecycleDelegation
import java.util.concurrent.TimeUnit

class MapkitLocationServiceTest {
    private lateinit var mapKitLocationManager: LocationManager
    private lateinit var androidLocationManager: AndroidLocationManagerProxy
    private lateinit var locationService: LocationService
    private val mainThreadScheduler = TestScheduler()
    private lateinit var appLifecycleDelegationSuspendable: AppLifecycleDelegation.Suspendable

    @Before
    fun setUp() {
        mapKitLocationManager = mock(LocationManager::class.java)
        androidLocationManager = mock(AndroidLocationManagerProxy::class.java)
        val appLifecycleDelegation: AppLifecycleDelegation = object : AppLifecycleDelegation {
            override fun suspendAlongLifecycle(suspendable: AppLifecycleDelegation.Suspendable, notifyAboutCurrentState: Boolean) {
                appLifecycleDelegationSuspendable = suspendable
            }

            override fun remove(suspendable: AppLifecycleDelegation.Suspendable) {}
        }
        locationService = MapkitLocationService(mapKitLocationManager, androidLocationManager, mainThreadScheduler, appLifecycleDelegation)
    }

    @Test
    fun lastLocationFromLocationManager() {
        val locationSubscriber = locationService.dangerousLocationObservable.test()
        appLifecycleDelegationSuspendable.resume()
        mainThreadScheduler.triggerActions()
        val locationListener = retrieveLocationListener()
        locationListener.onLocationUpdated(SOME_RANDOM_WALK_LOCATION)
        mainThreadScheduler.triggerActions()
        Assert.assertEquals(1, locationSubscriber.valueCount().toLong())
        val location = locationService.location
        val location2 = locationSubscriber.values()[0]
        Assert.assertNotNull(location)
        Assert.assertEquals(location, location2.toNullable())
        Assert.assertEquals(location!!.position, SOME_RANDOM_POINT)
        Assert.assertEquals(location.accuracy, SOME_RANDOM_ACCURACY)
        Assert.assertEquals(location.altitude, SOME_RANDOM_ALTITUDE)
        Assert.assertEquals(location.altitudeAccuracy, SOME_RANDOM_ALTITUDE_ACCURACY)
        Assert.assertEquals(location.heading, SOME_RANDOM_HEADING)
        Assert.assertEquals(location.speed, SOME_RANDOM_WALK_SPEED)
        Assert.assertEquals(location.absoluteTimestamp, SOME_RANDOM_ABSOLUTE_TIME_STAMP)
        Assert.assertEquals(location.relativeTimestamp, SOME_RANDOM_RELATIVE_TIME_STAMP)
    }

    @Test
    fun locationNotExpiredImmediately() {
        appLifecycleDelegationSuspendable.resume()
        mainThreadScheduler.triggerActions()
        val locationListener = retrieveLocationListener()
        locationListener.onLocationUpdated(SOME_RANDOM_WALK_LOCATION)
        Assert.assertNotNull(locationService.location)
        mainThreadScheduler.advanceTimeBy(MapkitLocationService.TOO_LONG_WITHOUT_LOCATION_TIMER_MS - 1, TimeUnit.MILLISECONDS)
        Assert.assertNotNull(locationService.location)
        mainThreadScheduler.advanceTimeBy(MapkitLocationService.TOO_LONG_WITHOUT_LOCATION_TIMER_MS + 1, TimeUnit.MILLISECONDS)
        Assert.assertNull(locationService.location)
    }

    @Test
    fun locationExpiredAfterSomeTimeWithoutUpdate() {
        appLifecycleDelegationSuspendable.resume()
        mainThreadScheduler.triggerActions()
        val locationListener = retrieveLocationListener()
        locationListener.onLocationUpdated(SOME_RANDOM_WALK_LOCATION)
        mainThreadScheduler.triggerActions()
        Assert.assertNotNull(locationService.location)
        mainThreadScheduler.advanceTimeBy(MapkitLocationService.TOO_LONG_WITHOUT_LOCATION_TIMER_MS + 1, TimeUnit.MILLISECONDS)
        Assert.assertNull(locationService.location)
    }

    @Test
    fun emitNothingOnReset_whenThereIsNoLastLocation() {
        assertLocations(Runnable { locationService.resetSource() })
    }

    @Test
    fun emitLastLocationOnReset_whenLastLocationIsFresh() {
        val location = LocationBuilder().timestamp(System.currentTimeMillis()).build()
        `when`(androidLocationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)).thenReturn(location)
        assertLocations(Runnable { locationService.resetSource() }, location)
    }

    @Test
    fun emitLastLocationAndNullOnReset_whenLastLocationIsNotSoFresh() {
        val location = LocationBuilder().timestamp(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(2)).build()
        `when`(androidLocationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)).thenReturn(location)
        assertLocations(Runnable { locationService.resetSource() }, location)
    }

    @Test
    fun emitNothingOnReset_whenLastLocationIsOutdated() {
        val location = LocationBuilder().timestamp(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(15)).build()
        `when`(androidLocationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)).thenReturn(location)
        assertLocations(Runnable { locationService.resetSource() })
    }

    @Test
    fun preferMoreFreshLocation_whenThereAreSeveralLastLocations() {
        val gpsLocation = LocationBuilder().timestamp(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(2)).build()
        val networkLocation = LocationBuilder().timestamp(System.currentTimeMillis()).build()
        `when`(androidLocationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)).thenReturn(gpsLocation)
        `when`(androidLocationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)).thenReturn(networkLocation)
        assertLocations(Runnable { locationService.resetSource() }, gpsLocation, networkLocation)
    }

    @Test
    fun `If app gets suspended then we should unsubscribe from mapkit location`() {
        appLifecycleDelegationSuspendable.resume()
        appLifecycleDelegationSuspendable.suspend()
        mainThreadScheduler.triggerActions()
        val inOrder = inOrder(mapKitLocationManager)
        val locationListener = retrieveLocationListener()
        inOrder.verify(mapKitLocationManager).subscribeForLocationUpdates(
            anyDouble(), anyLong(), anyDouble(), anyBoolean(), any(), eq(locationListener)
        )
        inOrder.verify(mapKitLocationManager).unsubscribe(eq(locationListener))
    }

    @Test
    fun `If we resume and suspend multiple times then unsubscribe should be called each time after suspend`() {
        appLifecycleDelegationSuspendable.resume()
        appLifecycleDelegationSuspendable.suspend()
        appLifecycleDelegationSuspendable.resume()
        appLifecycleDelegationSuspendable.suspend()
        appLifecycleDelegationSuspendable.resume()
        appLifecycleDelegationSuspendable.suspend()
        mainThreadScheduler.triggerActions()
        verify(mapKitLocationManager, times(3))
            .subscribeForLocationUpdates(anyDouble(), anyLong(), anyDouble(), anyBoolean(), any(), any())
        verify(mapKitLocationManager, times(3)).unsubscribe(any())
    }

    @Test
    fun `If waitForLocationInBackground called then subscribe and unsubscribe from mapkit location`() {
        val subscriber = locationService.waitForLocationInBackground().test()
        mainThreadScheduler.triggerActions()
        val locationListener = retrieveLocationListener()
        locationListener.onLocationUpdated(SOME_RANDOM_WALK_LOCATION)
        mainThreadScheduler.triggerActions()
        subscriber.awaitTerminalEvent()
        Assert.assertEquals(1, subscriber.valueCount().toLong())
        Assert.assertEquals(SOME_RANDOM_WALK_LOCATION, subscriber.values()[0])
        verify(mapKitLocationManager).subscribeForLocationUpdates(anyDouble(), anyLong(), anyDouble(), anyBoolean(), any(), eq(locationListener))
        verify(mapKitLocationManager).unsubscribe(eq(locationListener))
    }

    @Test
    fun `Must not subscribe when instance created`() {
        verify(mapKitLocationManager, never()).subscribeForLocationUpdates(anyDouble(), anyLong(), anyDouble(), anyBoolean(), any(), any())
    }

    @Test
    fun waitForLocationInBackground_doesNotUnsubscribeIfSuspend_unsubscribesWhenEmitted() {
        appLifecycleDelegationSuspendable.resume()
        mainThreadScheduler.triggerActions()
        val originalLocationListener = retrieveLocationListener()
        val subscriber = locationService.waitForLocationInBackground().test()
        mainThreadScheduler.triggerActions()
        val backgroundLocationListener = retrieveLocationListener()
        appLifecycleDelegationSuspendable.suspend()
        mainThreadScheduler.triggerActions()
        verify(mapKitLocationManager, times(2))
            .subscribeForLocationUpdates(anyDouble(), anyLong(), anyDouble(), anyBoolean(), any(), any())
        verify(mapKitLocationManager, times(1)).unsubscribe(eq(originalLocationListener))
        backgroundLocationListener.onLocationUpdated(SOME_RANDOM_WALK_LOCATION)
        mainThreadScheduler.triggerActions()
        subscriber.awaitTerminalEvent()
        Assert.assertEquals(1, subscriber.valueCount().toLong())
        verify(mapKitLocationManager, times(1)).unsubscribe(eq(backgroundLocationListener))
    }

    @Test
    fun waitForLocationInBackground_unsubscribeFromGpsIfUnsubscribeFromStream() {
        val subscriber = locationService.waitForLocationInBackground().test()
        mainThreadScheduler.triggerActions()
        val locationListener = retrieveLocationListener()
        verify(mapKitLocationManager).subscribeForLocationUpdates(anyDouble(), anyLong(), anyDouble(), anyBoolean(), any(), eq(locationListener))
        verify(mapKitLocationManager, never()).unsubscribe(any())
        subscriber.dispose()
        mainThreadScheduler.triggerActions()
        verify(mapKitLocationManager).unsubscribe(eq(locationListener))
    }

    @Test
    fun doesNotSubscribeToGpsUntilResumeNotCalled() {
        locationService.dangerousLocationObservable.subscribe()
        mainThreadScheduler.triggerActions()
        verify(mapKitLocationManager, never()).subscribeForLocationUpdates(anyDouble(), anyLong(), anyDouble(), anyBoolean(), any(), any())
        appLifecycleDelegationSuspendable.resume()
        mainThreadScheduler.triggerActions()
        verify(mapKitLocationManager, times(1)).subscribeForLocationUpdates(anyDouble(), anyLong(), anyDouble(), anyBoolean(), any(), any())
    }

    @Test
    fun setSource_disableLifecycleUpdates() {
        appLifecycleDelegationSuspendable.resume()
        mainThreadScheduler.triggerActions()
        val locationListener = retrieveLocationListener()
        verify(mapKitLocationManager, times(1)).subscribeForLocationUpdates(anyDouble(), anyLong(), anyDouble(), anyBoolean(), any(), eq(locationListener))
        verify(mapKitLocationManager, never()).unsubscribe(any())
        locationService.setSource(Observable.empty())
        mainThreadScheduler.triggerActions()
        verify(mapKitLocationManager).unsubscribe(any())
    }

    @Test
    fun resetSource_enableLifecycleUpdatesIfAppResumed() {
        appLifecycleDelegationSuspendable.resume()
        locationService.setSource(Observable.empty())
        mainThreadScheduler.triggerActions()
        verify(mapKitLocationManager).unsubscribe(any())
        locationService.resetSource()
        appLifecycleDelegationSuspendable.resume()
        mainThreadScheduler.triggerActions()
        verify(mapKitLocationManager, times(2)).subscribeForLocationUpdates(anyDouble(), anyLong(), anyDouble(), anyBoolean(), any(), any())
    }

    @Test
    fun resetSource_doesNotEnableLifecycleUpdatesIfAppSuspended() {
        locationService.setSource(Observable.empty())
        verify(mapKitLocationManager, never()).unsubscribe(any())
        locationService.resetSource()
        verify(mapKitLocationManager, never()).subscribeForLocationUpdates(anyDouble(), anyLong(), anyDouble(), anyBoolean(), any(), any())
    }

    private fun assertLocations(action: Runnable, vararg locations: Location?) {
        val subscriber = locationService.dangerousLocationObservable.test()
        action.run()
        subscriber.assertValueSet(locations.map { it.toOptional() })
    }

    private fun retrieveLocationListener(): LocationListener {
        val locationListenerArgumentCaptor = ArgumentCaptor.forClass(LocationListener::class.java)
        verify(mapKitLocationManager, atLeastOnce()).subscribeForLocationUpdates(
            anyDouble(),
            anyLong(),
            anyDouble(),
            anyBoolean(),
            any(),
            locationListenerArgumentCaptor.capture()
        )
        return locationListenerArgumentCaptor.value
    }

    private inner class LocationBuilder @JvmOverloads internal constructor(location: Location = SOME_RANDOM_WALK_LOCATION) {
        private val point: Point
        private val accuracy: Double
        private val altitude: Double
        private val altitudeAccuracy: Double
        private val heading: Double
        private val speed: Double
        private var absoluteTimeStamp: Long
        private val relativeTimeStamp: Long
        fun timestamp(timestamp: Long): LocationBuilder {
            absoluteTimeStamp = timestamp
            return this
        }

        fun build(): Location {
            return Location(point, accuracy, altitude, altitudeAccuracy, heading, speed, EMPTY_INDOOR_LEVEL_ID, absoluteTimeStamp, relativeTimeStamp)
        }

        init {
            point = location.position
            accuracy = location.accuracy!!
            altitude = location.altitude!!
            altitudeAccuracy = location.altitudeAccuracy!!
            heading = location.heading!!
            speed = location.speed!!
            absoluteTimeStamp = location.absoluteTimestamp
            relativeTimeStamp = location.relativeTimestamp
        }
    }

    companion object {
        private const val SOME_RANDOM_LAT = 0.123552341566109
        private const val SOME_RANDOM_LON = 0.23552341566109499
        private const val SOME_RANDOM_HEADING = 43.1415926535
        private const val SOME_RANDOM_ACCURACY = 33.1415926535
        private const val SOME_RANDOM_ALTITUDE = 23.1415926535
        private const val SOME_RANDOM_ALTITUDE_ACCURACY = 13.1415926535
        private const val SOME_RANDOM_WALK_SPEED = 1.0
        private const val SOME_RANDOM_ABSOLUTE_TIME_STAMP: Long = 14521L
        private const val SOME_RANDOM_RELATIVE_TIME_STAMP: Long = 114521L
        private val SOME_RANDOM_POINT = Point(SOME_RANDOM_LAT, SOME_RANDOM_LON)
        private val EMPTY_INDOOR_LEVEL_ID: String? = null
        private val SOME_RANDOM_WALK_LOCATION = Location(
            SOME_RANDOM_POINT,
            SOME_RANDOM_ACCURACY,
            SOME_RANDOM_ALTITUDE,
            SOME_RANDOM_ALTITUDE_ACCURACY,
            SOME_RANDOM_HEADING,
            SOME_RANDOM_WALK_SPEED,
            EMPTY_INDOOR_LEVEL_ID,
            SOME_RANDOM_ABSOLUTE_TIME_STAMP,
            SOME_RANDOM_RELATIVE_TIME_STAMP
        )
    }
}
