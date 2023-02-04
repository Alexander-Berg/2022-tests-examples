package com.yandex.maps.testapp.car

import android.content.Intent
import android.text.SpannableString
import android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE
import androidx.car.app.AppManager
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.validation.HostValidator
import androidx.car.app.Session
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.car.app.model.Distance.UNIT_KILOMETERS
import androidx.car.app.navigation.model.PlaceListNavigationTemplate
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapSurface
import com.yandex.maps.testapp.map.MapPerformanceActivity.*
import com.yandex.maps.testapp.map.PerformanceTestScenario

class CarService : CarAppService() {

    override fun onCreate() {
        super.onCreate()
        MapKitFactory.getInstance().onStart()
    }

    override fun onCreateSession(): Session {
        return CarSession()
    }

    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }
}

class CarSession : Session(), PerformanceTestScenario.Callback {

    private lateinit var mapSurface : MapSurface
    private lateinit var moscowScenario : PerformanceTestScenario
    private lateinit var spbScenario : PerformanceTestScenario

    override fun onCreateScreen(intent: Intent): Screen {
        mapSurface = MapSurface(carContext, OverlayRenderer(carContext))
        mapSurface.map.run {
            move(CameraPosition(Point(55.734276, 37.589483), 15f, 0f, 0f))
        }

        carContext.getCarService(AppManager::class.java).setSurfaceCallback(mapSurface)

        moscowScenario = createMskTilesDecoderTest(mapSurface.mapWindow, this, 120)
        spbScenario = createDefaultSpbCenterTest(carContext, mapSurface.mapWindow, this)

        return PlaceListNavigationScreen(carContext, this)
    }

    fun testMoscow() {
        moscowScenario.execute()
    }

    fun testSPb() {
        spbScenario.execute()
    }

    override fun onScenarioFinished(scenarioName: String, renderResult: String, decoderResult: String) {
        logResult("RENDER_PERFORMANCE:", scenarioName, renderResult)
        logResult("DECODER_PERFORMANCE:", scenarioName, decoderResult)
    }
}

class PlaceListNavigationScreen(context: CarContext, private val carSession: CarSession) : Screen(context) {
    override fun onGetTemplate(): Template {
        val moscowTitle = SpannableString("  Moscow")
        moscowTitle.setSpan(DistanceSpan.create(Distance.create(0.0, UNIT_KILOMETERS)), 0, 1, SPAN_INCLUSIVE_INCLUSIVE);
        val spbTitle = SpannableString("  Saint-Petersburg")
        spbTitle.setSpan(DistanceSpan.create(Distance.create(634.0, UNIT_KILOMETERS)), 0, 1, SPAN_INCLUSIVE_INCLUSIVE);
        return PlaceListNavigationTemplate.Builder()
                .setTitle("Place List")
                .setItemList(
                    ItemList.Builder()
                        .addItem(Row.Builder().setTitle(moscowTitle).setOnClickListener{ carSession.testMoscow() }.build())
                        .addItem(Row.Builder().setTitle(spbTitle).setOnClickListener{ carSession.testSPb() }.build())
                        .build())
                .build()
    }
}
