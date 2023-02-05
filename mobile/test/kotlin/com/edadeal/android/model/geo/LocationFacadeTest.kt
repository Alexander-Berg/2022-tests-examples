package com.edadeal.android.model.geo

import com.edadeal.android.AndroidLocation
import com.edadeal.android.data.Prefs
import com.edadeal.android.data.UsrRepository
import com.edadeal.android.di.ModuleLifecycle
import com.edadeal.android.dto.Experiment
import com.edadeal.android.helpers.ExperimentsFactory
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.subjects.BehaviorSubject
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LocationFacadeTest {

    private val usrRepository = mock<UsrRepository>()
    private val prefs = mock<Prefs>()
    private val locationSubject = BehaviorSubject.create<AndroidLocation>()
    private val experiments = ExperimentsFactory.getExperiments(id = "",
        mapOf(Experiment.EDADEAL_UI to mapOf("perf_disable_real_location_cache" to setOf("0"))))
    private lateinit var locationFacade: LocationFacade

    @BeforeTest
    fun prepare() {
        val moduleLifecycle: ModuleLifecycle = mock()
        whenever(moduleLifecycle.onCreated(any())).then {}
        locationFacade = LocationFacade(moduleLifecycle, usrRepository, locationSubject, prefs, experiments)
    }

    @Test
    fun `waitRealLocation should return from prefs`() {
        whenever(prefs.getRealLocation()).thenReturn(LOCATION_FROM_PREFS)

        val location = locationFacade.waitRealLocation()
        assertEquals(LOCATION_FROM_PREFS, location)
    }

    @Test
    fun `waitRealLocation should return from subject`() {
        locationSubject.onNext(LOCATION_FROM_SUBJECT)
        whenever(prefs.getRealLocation()).thenReturn(null)
        val location = locationFacade.waitRealLocation()
        assertEquals(LOCATION_FROM_SUBJECT, location)
        verify(prefs).setRealLocation(any())
    }

    companion object {
        private val LOCATION_FROM_PREFS = AndroidLocation("GPS", 55.755811, 37.617617)
        private val LOCATION_FROM_SUBJECT = AndroidLocation("NETWORK", 56.852672, 53.206893)
    }
}
