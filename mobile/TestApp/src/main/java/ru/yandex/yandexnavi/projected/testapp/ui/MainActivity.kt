package ru.yandex.yandexnavi.projected.testapp.ui

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.RequestPoint
import com.yandex.mapkit.RequestPointType
import com.yandex.mapkit.directions.driving.DrivingOptions
import com.yandex.mapkit.directions.driving.DrivingRoute
import com.yandex.mapkit.directions.driving.DrivingRouter
import com.yandex.mapkit.directions.driving.DrivingSession
import com.yandex.mapkit.directions.driving.Event
import com.yandex.mapkit.directions.driving.VehicleOptions
import com.yandex.mapkit.directions.guidance.AnnotatedEventTag
import com.yandex.mapkit.directions.guidance.LocationViewSourceFactory
import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraListener
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.CameraUpdateReason
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.road_events.EventTag
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.navikit.Display
import com.yandex.navikit.guidance.Guidance
import com.yandex.navikit.guidance.GuidanceListener
import com.yandex.navikit.guidance.bg.BgGuidanceSuspendReason
import com.yandex.navikit.guidance_layer.NaviGuidanceLayer
import com.yandex.navikit.guidance_layer.NaviGuidanceLayerFactory
import com.yandex.navikit.guidance_layer.NaviGuidanceLayerListener
import com.yandex.navikit.routing.ParkingRouteType
import com.yandex.navikit.ui.RectProvider
import com.yandex.navikit.ui.guidance.FasterAlternativeWidgetAction
import com.yandex.navikit.ui.parking.ParkingPointInfo
import com.yandex.runtime.Error
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.mapview
import ru.yandex.yandexnavi.projected.platformkit.presentation.protect.FinishActivityController
import ru.yandex.yandexnavi.projected.testapp.MainApplication
import ru.yandex.yandexnavi.projected.testapp.R
import ru.yandex.yandexnavi.projected.testapp.di.SHOW_STUB
import ru.yandex.yandexnavi.projected.testapp.tools.showBuildRouteDialog
import ru.yandex.yandexnavi.ui.PlatformColorProviderImpl
import ru.yandex.yandexnavi.ui.PlatformImageProviderImpl
import ru.yandex.yandexnavi.ui.balloons.BalloonFactoryImpl
import ru.yandex.yandexnavi.ui.common.isVisible
import ru.yandex.yandexnavi.ui.guidance.StatusPanelImpl
import ru.yandex.yandexnavi.ui.guidance.StatusPanelStyle
import ru.yandex.yandexnavi.ui.guidance.eta.EtaRouteProgressViewImpl
import ru.yandex.yandexnavi.ui.guidance.maneuver.ContextManeuverView
import ru.yandex.yandexnavi.ui.guidance.nextcamera.NextCameraViewImpl
import ru.yandex.yandexnavi.ui.guidance.parking.ParkingSnippetProviderImpl
import ru.yandex.yandexnavi.ui.guidance.speed.SpeedLimitView
import ru.yandex.yandexnavi.ui.guidance.speed.SpeedViewImpl
import javax.inject.Inject

object SimulationState {
    var isSimulationEnabled = false
}

class MainActivity :
    AppCompatActivity(),
    NaviGuidanceLayerListener,
    InputListener,
    CameraListener,
    GuidanceListener,
    DrivingSession.DrivingRouteListener {
    @Inject
    lateinit var guidance: Guidance

    @Inject
    lateinit var router: DrivingRouter

    @Inject
    lateinit var sp: SharedPreferences

    private lateinit var guidanceLayer: NaviGuidanceLayer
    private var drivingSession: DrivingSession? = null

    private val speedView get() = findViewById<SpeedViewImpl>(R.id.speedview_guidance)
    private val speedLimitView get() = findViewById<SpeedLimitView>(R.id.speedlimitview_guidance)
    private val etaRouteProgressView get() = findViewById<EtaRouteProgressViewImpl>(R.id.group_progresseta)
    private val contextManeuverView get() = findViewById<ContextManeuverView>(R.id.contextmaneuverview)
    private val statusPanel get() = findViewById<StatusPanelImpl>(R.id.text_statuspanel)
    private val nextCameraView get() = findViewById<NextCameraViewImpl>(R.id.next_camera_view)

    private val simulation get() = findViewById<Button>(R.id.simulation)
    private val overview get() = findViewById<Button>(R.id.overview)
    private val stickManeuverBalloonButton get() = findViewById<Button>(R.id.maneuverballoonstick)
    private val toAndroidAutoSettings get() = findViewById<Button>(R.id.aa_settings)
    private val mapView get() = findViewById<MapView>(R.id.mapview)
    private var userLocationLayer: UserLocationLayer? = null

    private val statusPanelRectProvider by lazy { RectProviderImpl(statusPanel) }
    private val contextManeuverRectProvider by lazy { RectProviderImpl(contextManeuverView) }

    private var isOverviewEnabled = false
    private var isStickManeuverOnRoadEnabled = true
    private val activityBlockController = FinishActivityController(this)

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        updateLocationSpecificService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MainApplication.component.inject(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)

        userLocationLayer = MapKitFactory.getInstance().createUserLocationLayer(mapview.mapWindow)

        ActivityCompat.requestPermissions(
            this,
            listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION).toTypedArray(),
            0
        )

        findViewById<CheckBox>(R.id.show_stub).apply {
            isChecked = sp.getBoolean(SHOW_STUB, false)
            setOnCheckedChangeListener { _, isChecked ->
                sp.edit().putBoolean(SHOW_STUB, isChecked).apply()
                Toast.makeText(context, "App process restart requires", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.zoom_in).setOnClickListener { stepZoom(1.0f) }
        findViewById<Button>(R.id.zoom_out).setOnClickListener { stepZoom(-1.0f) }
        stickManeuverBalloonButton.setOnClickListener { switchStickManeuverBalloonState() }
        overview.setOnClickListener { switchOverview() }
        simulation.setOnClickListener { switchSimulation() }
        toAndroidAutoSettings.setOnClickListener { goToAndroidAutoSettings() }

        updateSimulationButtonText()
        updateLocationSpecificService()

        mapView.map.addInputListener(this)
        mapView.map.addCameraListener(this)
        guidance.addGuidanceListener(this)

        AnnotatedEventTag.values().forEach { tag ->
            guidance.configurator().setRoadEventTagAnnotated(tag, true)
        }
        guidanceLayer = NaviGuidanceLayerFactory.createNaviGuidanceLayer(
            mapView.mapWindow,
            mapView.mapWindow.map.mapObjects.addCollection(),
            mapView.mapWindow.map.mapObjects,
            mapView.mapWindow.map.mapObjects,
            mapView.mapWindow.map.mapObjects,
            mapView.mapWindow.map.mapObjects,
            BalloonFactoryImpl(this),
            ParkingSnippetProviderImpl(this),
            Display.getDisplayMetrics(),
            PlatformImageProviderImpl(this),
            PlatformColorProviderImpl(this),
        )
            .also { it.addLayerListener(this) }
        guidanceLayer.apply {
            setRoadEventsAvailable(
                listOf(
                    EventTag.SPEED_CONTROL,
                    EventTag.LANE_CONTROL,
                    EventTag.NO_STOPPING_CONTROL,
                    EventTag.CROSS_ROAD_CONTROL,
                    EventTag.ROAD_MARKING_CONTROL,
                    EventTag.MOBILE_CONTROL,
                    EventTag.POLICE
                )
            )
            setContextBalloonsVisible(true)
            setAlternativesVisible(true)
            setTrafficLightsUnderRoadEvents(false)
            setCameraAlertsEnabled(true)
            setManeuverBalloonVisible(true)
            setManeuverAndLaneBalloonsMerged(true)
            setRouteAlertsEnabled(true)

            presentersFactory().let { factory ->
                speedView.apply {
                    presenter = factory.createSpeedPresenter()
                }

                nextCameraView.apply {
                    presenter = factory.createNextCameraPresenter()
                }

                speedLimitView.apply {
                    presenter = factory.createSpeedLimitPresenter()
                }

                etaRouteProgressView.presenter = factory.createEtaRouteProgressPresenter()

                contextManeuverView.apply {
                    presenter = factory.createManeuverPresenter()
                    canBeVisible = true
                    nextStreetCanBeLarge = true
                }

                statusPanel.apply {
                    presenter = factory.createStatusPanelPresenter()
                    style = StatusPanelStyle.CENTER
                }
            }

            setOverlapRects(
                listOf<RectProvider>(
                    RectProviderImpl(speedLimitView),
                    RectProviderImpl(speedView),
                    RectProviderImpl(etaRouteProgressView),
                    RectProviderImpl(nextCameraView),
                    contextManeuverRectProvider,
                    statusPanelRectProvider
                )
            )
        }

        updateGuidanceViews()

        activityBlockController.startListening()
    }

    private fun goToAndroidAutoSettings() {
        startActivity(Intent("com.google.android.projection.gearhead.SETTINGS"))
    }

    override fun onDestroy() {
        super.onDestroy()

        mapView.map.removeCameraListener(this)
        mapView.map.removeInputListener(this)

        speedLimitView.presenter?.onDismiss()
        speedView.presenter?.onDismiss()
        etaRouteProgressView.onDismiss()
        contextManeuverView.presenter?.onDismiss()
        statusPanel.presenter?.dismiss()
        nextCameraView.presenter?.dismiss()

        guidanceLayer.removeLayerListener(this)
        guidanceLayer.destroy()

        guidance.removeGuidanceListener(this)

        activityBlockController.stopListening()
    }

    private fun buildRoute(from: Point, to: Point) {
        val drivingOptions = DrivingOptions()
        drivingOptions.routesCount = 1
        drivingOptions.avoidTolls = false

        val vehicleOptions = VehicleOptions()

        drivingSession?.cancel()
        drivingSession = router.requestRoutes(
            listOf(
                RequestPoint(from, RequestPointType.WAYPOINT, null),
                RequestPoint(to, RequestPointType.WAYPOINT, null)
            ),
            drivingOptions,
            vehicleOptions,
            this
        )
    }

    override fun onDrivingRoutesError(error: Error) {
        Toast.makeText(this, "Error occurred while request routes!", Toast.LENGTH_LONG).show()
    }

    override fun onDrivingRoutes(routes: MutableList<DrivingRoute>) {
        if (routes.isNotEmpty()) {
            guidance.start(routes.first())
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    private fun switchOverview() {
        if (guidance.route() == null) {
            Toast.makeText(this, "Can't enable overview state without route!", Toast.LENGTH_LONG).show()
            return
        }

        if (!isOverviewEnabled) {
            enableOverview()
        } else {
            disableOverview()
        }
    }

    private fun enableOverview() {
        changeZoom(12.0f)
        guidance.routeBuilder().requestAlternatives()
        isOverviewEnabled = true
        overview.text = "Overview Off"
        updateGuidanceViews()
    }

    private fun disableOverview() {
        guidance.routeBuilder().startGuidance()
        isOverviewEnabled = false
        overview.text = "Overview On"
        updateGuidanceViews()
    }

    private fun switchSimulation() {
        if (SimulationState.isSimulationEnabled) {
            stopSimulation()
        } else {
            startSimulation()
        }
    }

    private fun startSimulation() {
        if (guidance.route() == null) {
            Toast.makeText(this, "Can't start simulation without route!", Toast.LENGTH_LONG).show()
            return
        }
        guidance.apply {
            startSimulationWithExistingRoute()
            setSimulatedSpeed(82.0)
        }
        SimulationState.isSimulationEnabled = true
        updateSimulationButtonText()
    }

    private fun stopSimulation() {
        if (isOverviewEnabled) {
            disableOverview()
        }
        guidance.stopSimulation()
        SimulationState.isSimulationEnabled = false
        updateSimulationButtonText()
    }

    private fun switchStickManeuverBalloonState() {
        isStickManeuverOnRoadEnabled = !isStickManeuverOnRoadEnabled
        guidanceLayer.setManeuverAndLaneBalloonsMerged(isStickManeuverOnRoadEnabled)
        stickManeuverBalloonButton.text =
            "Maneuver balloon on road ${if (isStickManeuverOnRoadEnabled) "Off" else "On"}"
    }

    private fun checkLocation(complete: (Point) -> Unit) {
        val location = guidance.location
        if (location != null) {
            complete(location.location.position)
        } else {
            Toast.makeText(this, "No location available!", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateLocationSpecificService() {
        userLocationLayer?.setSource(LocationViewSourceFactory.createLocationViewSource(guidance.mapkitGuide()))
        userLocationLayer?.setVisible(true)
    }

    private fun updateSimulationButtonText() {
        simulation.text = if (SimulationState.isSimulationEnabled) "Stop sim" else "Start sim"
    }

    private fun stepZoom(step: Float) {
        changeZoom(mapView.mapWindow.map.cameraPosition.zoom + step)
    }

    private fun changeZoom(newZoom: Float) {
        val currentCameraPosition = mapView.mapWindow.map.cameraPosition
        val newCameraPosition = CameraPosition(
            currentCameraPosition.target,
            newZoom,
            currentCameraPosition.azimuth,
            currentCameraPosition.tilt
        )
        val animation = Animation(Animation.Type.SMOOTH, 0.3f)
        mapView.mapWindow.map.move(newCameraPosition, animation) { }
    }

    private fun updateGuidanceViews() {
        updateSpeedVisibility()
        updateSpeedLimitVisibility()
        updateETAVisibility()
        updateManeuverVisibility()
        updateStatusVisibility()
        updateNextCameraVisibility()
    }

    private fun updateETAVisibility() {
        val visible = guidance.route() != null && !isOverviewEnabled
        etaRouteProgressView.canBeVisible = visible
    }

    private fun updateManeuverVisibility() {
        val visible = guidance.route() != null &&
            !isOverviewEnabled &&
            guidanceLayer.isManeuverVisible
        contextManeuverRectProvider.setRectVisible(visible)
        contextManeuverView.isVisible = visible
    }

    private fun updateStatusVisibility() {
        val visible = guidance.route() != null &&
            !isOverviewEnabled &&
            guidanceLayer.isStatusPanelVisible
        statusPanelRectProvider.setRectVisible(visible)
        statusPanel.isVisible = visible
    }

    private fun updateNextCameraVisibility() {
        val visible = !isOverviewEnabled && guidanceLayer.isNextCameraVisible
        nextCameraView.isVisible = visible
    }

    private fun updateSpeedVisibility() {
        val visible = guidanceLayer.isSpeedVisible && !isOverviewEnabled
        speedView.isVisible = visible
    }

    private fun updateSpeedLimitVisibility() {
        val visible = guidanceLayer.isSpeedLimitVisible && !isOverviewEnabled
        speedLimitView.isVisible = visible
    }

    override fun onSpeedVisibilityChanged() {
        updateSpeedVisibility()
    }

    override fun onZeroSpeedActionTapped(info: Any) {
    }

    override fun onFasterAlternativeVisibilityChanged() {
    }

    override fun onSpeedLimitVisibilityChanged() {
        updateSpeedLimitVisibility()
    }

    override fun onZeroSpeedBannerVisibilityChanged() {
    }

    override fun onParkingWidgetVisibilityChanged() {
    }

    override fun onManeuverTapped(point: Point, zoom: Float) {
        val currentCameraPosition = mapView.mapWindow.map.cameraPosition
        val newCameraPosition = CameraPosition(
            point,
            zoom,
            currentCameraPosition.azimuth,
            currentCameraPosition.tilt
        )
        val animation = Animation(Animation.Type.SMOOTH, 0.3f)
        mapView.mapWindow.map.move(newCameraPosition, animation) { }
    }

    override fun onSpeedLimitTapped() {
    }

    override fun onRouteEventTapped(event: Event, tag: EventTag) {
        guidanceLayer.selectRoadEvent(event.eventId, tag)
    }

    override fun onParkingPointTapped(parkingPointInfo: ParkingPointInfo) {
        // do nothing
    }

    override fun onAdvertPinTapped(info: Any) {
    }

    override fun onStatusPanelVisibilityChanged() {
        updateStatusVisibility()
    }

    override fun onManeuverVisibilityChanged() {
        updateManeuverVisibility()
    }

    override fun onNextCameraViewVisibilityChanged() {
        updateNextCameraVisibility()
    }

    override fun onFinishGuidanceTapped() {
        if (isOverviewEnabled) {
            disableOverview()
        }
        guidance.stop()
    }

    override fun onWayPointTapped(info: Any) {
    }

    override fun onRouteChanged() {
        updateGuidanceViews()
    }

    override fun onFreeDriveRouteChanged() {
    }

    override fun onFinishedRoute() {
    }

    override fun onReachedWayPoint() {
    }

    override fun onRoutePositionUpdated() {
    }

    override fun onLocationUpdated() {
    }

    override fun onParkingRouteBuilt(type: ParkingRouteType) {
    }

    override fun onMapTap(map: com.yandex.mapkit.map.Map, point: Point) {
        guidanceLayer.deselectRoadEvent()
    }

    override fun onMapLongTap(map: com.yandex.mapkit.map.Map, point: Point) {
        guidanceLayer.deselectRoadEvent()

        showBuildRouteDialog(this) {
            checkLocation {
                buildRoute(it, point)
            }
        }
    }

    override fun onFasterAlternativeWidgetAction(action: FasterAlternativeWidgetAction) {
    }

    override fun onCameraPositionChanged(
        map: com.yandex.mapkit.map.Map,
        cameraPosition: CameraPosition,
        cameraUpdateReason: CameraUpdateReason,
        finished: Boolean
    ) {
    }

    override fun onFasterAlternativeUpdated() {}

    override fun onBackgroundGuidanceWillBeSuspended(reason: BgGuidanceSuspendReason) {}

    override fun onFasterAlternativeAnnotated() {}

    override fun onBackgroundGuidanceTaskRemoved() {}

    override fun onGuidanceResumedChanged() {}
}
