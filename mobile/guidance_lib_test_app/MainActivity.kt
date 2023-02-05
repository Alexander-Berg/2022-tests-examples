package ru.yandex.yandexnavi.guidance_lib_test_app

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.PointF
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.yandex.mapkit.*
import com.yandex.mapkit.directions.driving.DrivingRoute
import com.yandex.mapkit.directions.driving.Event
import com.yandex.mapkit.directions.guidance.AnnotatedEventTag
import com.yandex.mapkit.directions.guidance.LocationViewSourceFactory
import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.BoundingBoxHelper
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.*
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.road_events.EventTag
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.navikit.Display
import com.yandex.navikit.ScreenSaverMode
import com.yandex.navikit.guidance.GuidanceListener
import com.yandex.navikit.guidance.RouteBuilderListener
import com.yandex.navikit.guidance.RouteChangeReason
import com.yandex.navikit.guidance.RoutingOptions
import com.yandex.navikit.guidance.bg.BgGuidanceSuspendReason
import com.yandex.navikit.guidance.generic.GenericGuidanceComponent
import com.yandex.navikit.guidance_layer.*
import com.yandex.navikit.routing.ParkingRouteType
import com.yandex.navikit.ui.RectProvider
import com.yandex.navikit.ui.guidance.FasterAlternativeWidgetAction
import com.yandex.navikit.ui.parking.ParkingPointInfo
import com.yandex.navilib.widget.NaviFrameLayout
import com.yandex.runtime.Error
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*
import ru.yandex.yandexnavi.balloons_gallery.api.BalloonsGallery
import ru.yandex.yandexnavi.ui.PlatformColorProviderImpl
import ru.yandex.yandexnavi.ui.PlatformImageProviderImpl
import ru.yandex.yandexnavi.ui.balloons.BalloonFactoryImpl
import ru.yandex.yandexnavi.ui.common.isVisible
import ru.yandex.yandexnavi.ui.guidance.StatusPanelImpl
import ru.yandex.yandexnavi.ui.guidance.StatusPanelStyle
import ru.yandex.yandexnavi.ui.guidance.eta.EtaRouteProgressViewImpl
import ru.yandex.yandexnavi.ui.guidance.faster_alternative.FasterAlternativeWidgetImpl
import ru.yandex.yandexnavi.ui.guidance.maneuver.ContextManeuverView
import ru.yandex.yandexnavi.ui.guidance.nextcamera.NextCameraViewImpl
import ru.yandex.yandexnavi.ui.guidance.parking.ParkingSnippetProviderImpl
import ru.yandex.yandexnavi.ui.guidance.speed.SpeedLimitView
import ru.yandex.yandexnavi.ui.guidance.speed.SpeedViewImpl
import java.util.concurrent.TimeUnit
import kotlin.math.max

private class RectProviderImpl(private val view: View) : RectProvider {
    private var rectVisible = true

    override fun getRect(): ScreenRect? {
        val origin = IntArray(2)
        view.getLocationInWindow(origin)
        return ScreenRect(
            ScreenPoint(origin[0].toFloat(), origin[1].toFloat()),
            ScreenPoint(view.width.toFloat() + origin[0].toFloat(), view.height.toFloat() + origin[1].toFloat())
        )
    }

    override fun isRectVisible(): Boolean {
        return rectVisible
    }

    fun setRectVisible(visible: Boolean) {
        rectVisible = visible
    }
}

private enum class SimulationMode {
    ROUTE,
    GEOMETRY,
    TICKET_NUMBER
}

class MainActivity :
    AppCompatActivity(),
    NaviGuidanceLayerListener,
    InputListener,
    CameraListener,
    GuidanceListener,
    RouteBuilderListener {

    private val app get() = (application as MainApplication)
    private val guidance get() = app.guidance
    private val consumer get() = app.consumer
    private val routeBuilder get() = app.guidance.routeBuilder()
    private lateinit var guidanceLayer: NaviGuidanceLayer

    private val speedView get() = findViewById<SpeedViewImpl>(R.id.speedview_guidance)
    private val speedLimitView get() = findViewById<SpeedLimitView>(R.id.speedlimitview_guidance)
    private val etaRouteProgressView get() = findViewById<EtaRouteProgressViewImpl>(R.id.group_progresseta)
    private val contextManeuverView get() = findViewById<ContextManeuverView>(R.id.contextmaneuverview)
    private val statusPanel get() = findViewById<StatusPanelImpl>(R.id.text_statuspanel)
    private val nextCameraView get() = findViewById<NextCameraViewImpl>(R.id.next_camera_view)
    private val fasterAlternativeWidget get() = findViewById<FasterAlternativeWidgetImpl>(R.id.view_faster_alternative_widget)

    private val simulation get() = findViewById<Button>(R.id.simulation)
    private val overview get() = findViewById<Button>(R.id.overview)
    private val stickManeuverBalloonButton get() = findViewById<Button>(R.id.maneuverballoonstick)
    private val mapView get() = findViewById<MapView>(R.id.mapview)
    private var userLocationLayer: UserLocationLayer? = null
    private val fasterAlternativeCard get() = findViewById<NaviFrameLayout>(R.id.view_faster_alternative_card)

    private val statusPanelRectProvider by lazy { RectProviderImpl(statusPanel) }
    private val contextManeuverRectProvider by lazy { RectProviderImpl(contextManeuverView) }

    private var isStickManeuverOnRoadEnabled = true

    private var inactivityTimer: Disposable? = null

    private var currentNorthOnTop = false

    private val isOverviewStateEnabled get() = routeBuilder.routes.isNotEmpty()

    private var lastRouteToPoint: Point? = null
    private var lastRouteFromPoint: Point? = null

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
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        userLocationLayer = MapKitFactory.getInstance().createUserLocationLayer(mapview.mapWindow)

        ActivityCompat.requestPermissions(
            this,
            listOf<String>(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACTIVITY_RECOGNITION
            ).toTypedArray(),
            0
        )

        findViewById<Button>(R.id.zoom_in).setOnClickListener { stepZoom(1.0f) }
        findViewById<Button>(R.id.zoom_out).setOnClickListener { stepZoom(-1.0f) }
        findViewById<Button>(R.id.north_at_top).setOnClickListener { northAtTheTop() }
        stickManeuverBalloonButton.setOnClickListener { switchStickManeuverBalloonState() }
        findViewById<Button>(R.id.avoidtolls).setOnClickListener { switchAvoidTolls() }
        overview.setOnClickListener { updateOverviewGroupVisibility() }
        balloonsDemo.setOnClickListener { demoBalloons() }
        roadEventsDemo.setOnClickListener { demoRoadEvents() }
        disable_all.setOnClickListener {
            val showToast: (String) -> Unit = { text ->
                Toast.makeText(this, text, Toast.LENGTH_LONG).show()
            }
            if (disable_all.text == "V") {
                disable_all.setText("H")
                guidanceLayer.setLayerObjectsVisible(false)
                showToast("Hidden all object on layer")
            } else {
                disable_all.setText("V")
                guidanceLayer.setLayerObjectsVisible(true)
                showToast("Shown all object on layer")
            }
        }
        findViewById<Button>(R.id.copyRouteGeometry).setOnClickListener { copyRouteGeometry() }
        simulation.setOnClickListener { updateSimulationPanelGroupVisibility() }
        button_simulationpanel_route.setOnClickListener { startSimulation(SimulationMode.ROUTE) }
        button_simulationpanel_geometry.setOnClickListener { startSimulation(SimulationMode.GEOMETRY) }
        button_simulationpanel_ticket.setOnClickListener { startSimulation(SimulationMode.TICKET_NUMBER) }
        button_simulationpanel_stop.setOnClickListener { stopSimulation() }

        button_overview_on.setOnClickListener { switchOverview() }
        button_overview_cancel.setOnClickListener { cancelOverview() }

        findViewById<Button>(R.id.bg_guidance).setOnClickListener { updateBGGuidanceGroupVisibility() }
        findViewById<Button>(R.id.button_bg_guidance_automobile_guidance).setOnClickListener {
            GenericGuidanceComponent.getGenericGuidance().registerConsumer(consumer)
            updateBGGuidanceGroupVisibility()
        }
        findViewById<Button>(R.id.button_bg_guidance_reset).setOnClickListener {
            GenericGuidanceComponent.getGenericGuidance().unregisterConsumer()
            updateBGGuidanceGroupVisibility()
        }

        updateAvoidTollsButtonText()
        updateLocationSpecificService()

        mapView.map.isFastTapEnabled = true
        mapView.map.addInputListener(this)
        mapView.map.addCameraListener(this)
        guidance.start(null)
        guidance.addGuidanceListener(this)

        routeBuilder.addListener(this)

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
            BalloonFactoryImpl(this, true),
            ParkingSnippetProviderImpl(this),
            Display.getDisplayMetrics(),
            PlatformImageProviderImpl(this),
            PlatformColorProviderImpl(this),
        )
            .also {
                it.addLayerListener(this)
            }
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
            setWayPointPinsVisible(true)
            setScreenSaverMode(ScreenSaverMode.ALWAYS_ON)

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

                etaRouteProgressView.apply {
                    presenter = factory.createEtaRouteProgressPresenter()
                    canBeVisible = true
                }

                contextManeuverView.apply {
                    presenter = factory.createManeuverPresenter()
                    canBeVisible = true
                    nextStreetCanBeLarge = true
                }

                statusPanel.apply {
                    presenter = factory.createStatusPanelPresenter()
                    style = StatusPanelStyle.CENTER
                }

                fasterAlternativeWidget.apply {
                    presenter = factory.createFasterAlternativeWidgetPresenter()
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
        setFollowingEnabledImpl(true)
    }

    override fun onDestroy() {
        super.onDestroy()

        inactivityTimer?.dispose()

        mapView.map.removeCameraListener(this)
        mapView.map.removeInputListener(this)

        speedLimitView.presenter?.onDismiss()
        speedView.presenter?.onDismiss()
        etaRouteProgressView.onDismiss()
        contextManeuverView.presenter?.onDismiss()
        statusPanel.presenter?.dismiss()
        nextCameraView.presenter?.dismiss()
        fasterAlternativeWidget.presenter?.dismiss()

        guidanceLayer.removeLayerListener(this)
        guidanceLayer.destroy()

        guidance.removeGuidanceListener(this)
    }

    private fun buildRoute(from: Point, to: Point) {
        val routingOptions = RoutingOptions()

        routeBuilder.cancelRoutesRequest()
        routeBuilder.requestRoutes(
            listOf(
                RequestPoint(from, RequestPointType.WAYPOINT, null),
                RequestPoint(to, RequestPointType.WAYPOINT, null)
            ),
            routingOptions
        )
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapView.onStart()
        guidance.onStart()
    }

    override fun onStop() {
        super.onStop()
        MapKitFactory.getInstance().onStop()
        mapView.onStop()
        guidance.onPause(true)
    }

    private fun switchOverview() {
        if (!isOverviewStateEnabled) {
            if (guidance.route() == null) {
                Toast.makeText(this, "Can't enable overview state without route!", Toast.LENGTH_LONG).show()
                return
            }
            routeBuilder.requestAlternatives()
        } else {
            routeBuilder.startGuidance()
        }
        updateOverviewGroupVisibility()
    }

    private fun cancelOverview() {
        if (isOverviewStateEnabled) {
            routeBuilder.clearRoutes()
        }
        updateOverviewGroupVisibility()
    }

    private fun updateSimulationPanelGroupVisibility() {
        group_simulationpanel.isVisible = !group_simulationpanel.isVisible
    }

    private fun updateOverviewGroupVisibility() {
        group_overview.isVisible = !group_overview.isVisible
    }

    private fun updateBGGuidanceGroupVisibility() {
        group_bg_guidance.isVisible = !group_bg_guidance.isVisible
    }

    private fun northAtTheTop() {
        currentNorthOnTop = !currentNorthOnTop
        startInactivityTimer()
        val currentCameraPosition = mapView.mapWindow.map.cameraPosition
        val heading = guidance.location?.location?.heading?.let { it.toFloat() } ?: 0.0f
        val newCameraPosition = CameraPosition(
            currentCameraPosition.target,
            currentCameraPosition.zoom,
            if (currentNorthOnTop) 0.0f else heading,
            currentCameraPosition.tilt
        )
        val animation = Animation(Animation.Type.SMOOTH, 0.3f)
        mapView.mapWindow.map.move(newCameraPosition, animation) { }

        guidanceLayer.setLanesInFixedBalloonAvailable(currentNorthOnTop)
        Toast.makeText(this, "Changed north on top to $currentNorthOnTop", Toast.LENGTH_LONG).show()
    }

    private fun startSimulation(mode: SimulationMode) {
        if (guidance.route() == null && mode != SimulationMode.TICKET_NUMBER) {
            Toast.makeText(this, "Can't start simulation without route or ticket number!", Toast.LENGTH_LONG).show()
            return
        }

        guidance.setSimulatedSpeed(82.0)
        when (mode) {
            SimulationMode.ROUTE -> guidance.startSimulationWithExistingRoute()
            SimulationMode.GEOMETRY -> guidance.startSimulationWithGeometry(guidance.route()!!.geometry)
            SimulationMode.TICKET_NUMBER -> guidance.startSimulationWithTicketNumber(4261)
        }
        app.isSimulationEnabled = true
        updateSimulationPanelGroupVisibility()
    }

    private fun stopSimulation() {
        if (!app.isSimulationEnabled) return
        guidance.stopSimulation()
        app.isSimulationEnabled = false
        updateSimulationPanelGroupVisibility()
    }

    private fun switchStickManeuverBalloonState() {
        isStickManeuverOnRoadEnabled = !isStickManeuverOnRoadEnabled
        guidanceLayer.setManeuverAndLaneBalloonsMerged(isStickManeuverOnRoadEnabled)
        stickManeuverBalloonButton.text = "Maneuver balloon on road " + (if (isStickManeuverOnRoadEnabled) "Off" else "On")
    }

    private fun switchAvoidTolls() {
        app.isAvoidTolls = !app.isAvoidTolls
        guidance.configurator().setTollAvoidanceEnabled(app.isAvoidTolls)
        updateAvoidTollsButtonText()
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

    private fun updateAvoidTollsButtonText() {
        avoidtolls.text = "Avoid tolls " + (if (app.isAvoidTolls) "Off" else "On")
    }

    private fun stepZoom(step: Float) {
        startInactivityTimer()
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
        val visible = guidance.route() != null && !isOverviewStateEnabled
        etaRouteProgressView.setContentVisible(visible)
    }

    private fun updateManeuverVisibility() {
        val visible = guidance.route() != null &&
            !isOverviewStateEnabled &&
            guidanceLayer.isManeuverVisible
        contextManeuverRectProvider.isRectVisible = visible
        contextManeuverView.isVisible = visible
    }

    private fun updateStatusVisibility() {
        val visible = guidance.route() != null &&
            !isOverviewStateEnabled &&
            guidanceLayer.isStatusPanelVisible
        statusPanelRectProvider.isRectVisible = visible
        statusPanel.isVisible = visible
    }

    private fun updateNextCameraVisibility() {
        val visible = !isOverviewStateEnabled && guidanceLayer.isNextCameraVisible
        nextCameraView.isVisible = visible
    }

    private fun updateSpeedVisibility() {
        val visible = guidanceLayer.isSpeedVisible && !isOverviewStateEnabled
        speedView.isVisible = visible
    }

    private fun updateSpeedLimitVisibility() {
        val visible = guidanceLayer.isSpeedLimitVisible && !isOverviewStateEnabled
        speedLimitView.isVisible = visible
    }

    private fun startInactivityTimer() {
        setFollowingEnabled(false)
        inactivityTimer?.dispose()
        inactivityTimer = Observable.timer(5000, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                setFollowingEnabled(true)
            }
    }

    private fun setFollowingEnabledImpl(enabled: Boolean) {
        guidanceLayer.setManeuverBalloonVisible(enabled && !currentNorthOnTop)
        userLocationLayer?.isAutoZoomEnabled = enabled
        userLocationLayer?.isHeadingEnabled = enabled && !currentNorthOnTop
        if (enabled) {
            userLocationLayer?.setAnchor(
                PointF((mapView.width * 0.5).toFloat(), (mapView.height * 0.5).toFloat()),
                PointF((mapView.width * 0.5).toFloat(), (mapView.height * 0.78).toFloat())
            )
        } else {
            userLocationLayer?.resetAnchor()
        }
    }

    private fun setFollowingEnabled(enabled: Boolean) {
        if (isOverviewStateEnabled) return
        setFollowingEnabledImpl(enabled)
    }

    private fun demoBalloons() {
        BalloonsGallery.open(this)
    }

    private fun demoRoadEvents() {
        RoadEventsDemoFragment().let {
            it.show(supportFragmentManager, "demo-road-events")
        }
    }

    private fun copyRouteGeometry() {
        fun routeFromVariants(): DrivingRoute? {
            val index = guidance.routeBuilder().selectedRouteIndex() ?: return null
            return guidance.routeBuilder().routes[index]
        }

        val route = guidance.route() ?: routeFromVariants()
        if (route == null) {
            showToast("No route found!!!")
            return
        }
        "[${route.geometry.points.joinToString(", ") { "[${it.latitude}, ${it.longitude}]" }}]"
            .saveToClipboard()
    }

    private fun String.saveToClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard == null) {
            showToast("Wow there is no ClipboardManager(")
            return
        }
        clipboard.setPrimaryClip(ClipData.newPlainText("route geometry", this))
        showToast("Saved to clipboard!")
    }

    override fun onSpeedVisibilityChanged() {
        updateSpeedVisibility()
    }

    override fun onZeroSpeedActionTapped(info: Any) {
    }

    override fun onFasterAlternativeVisibilityChanged() {
        fasterAlternativeCard.isVisible = guidanceLayer.isFasterAlternativeVisible
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

    override fun onParkingPointTapped(
        parkingPointInfo: ParkingPointInfo
    ) {
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
        guidance.stop()
        guidance.start(null)
    }

    override fun onWayPointTapped(info: Any) {
    }

    override fun onFasterAlternativeWidgetAction(action: FasterAlternativeWidgetAction) {
        if (action == FasterAlternativeWidgetAction.GO) {
            switchOverview()
        }

        guidanceLayer.notifyFasterAlternativeWidgetWasClosed()
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

    override fun onBackgroundGuidanceWillBeSuspended(
        reason: BgGuidanceSuspendReason
    ) {
    }

    override fun onFasterAlternativeUpdated() {
    }

    override fun onFasterAlternativeAnnotated() {
    }

    override fun onBackgroundGuidanceTaskRemoved() {
    }

    override fun onMapTap(map: Map, point: Point) {
        guidanceLayer.deselectRoadEvent()
        startInactivityTimer()
    }

    override fun onMapLongTap(map: Map, point: Point) {
        guidanceLayer.deselectRoadEvent()
        startInactivityTimer()

        AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setMessage("Build route!")
            .setCancelable(true)
            .setPositiveButton("To") { _, _ ->
                lastRouteFromPoint?.let {
                    lastRouteToPoint = point
                    buildRoute(it, point)
                    lastRouteFromPoint = null
                    return@setPositiveButton
                }
                checkLocation {
                    lastRouteToPoint = point
                    buildRoute(it, point)
                }
            }
            .setNeutralButton("From") { _, _ ->
                lastRouteToPoint?.let {
                    buildRoute(point, it)
                    return@setNeutralButton
                }
                lastRouteFromPoint = point
                showToast("Please set To point!")
            }
            .setNegativeButton("Cancel") { _, _ -> }
            .show()
    }

    override fun onCameraPositionChanged(
        map: Map,
        cameraPosition: CameraPosition,
        cameraUpdateReason: CameraUpdateReason,
        finished: Boolean
    ) {
        if (cameraUpdateReason == CameraUpdateReason.GESTURES) {
            startInactivityTimer()
        }
    }

    override fun onRoutesChanged(reason: RouteChangeReason) {
        when (reason) {
            RouteChangeReason.ROUTER_RESPONSE, RouteChangeReason.AUTOFILL -> {
                setFollowingEnabledImpl(false)
                button_overview_on.text = "Start guidance"

                var bbox: BoundingBox? = null
                for (route in routeBuilder.routes) {
                    if (bbox == null) {
                        bbox = BoundingBoxHelper.getBounds(route.geometry)
                    } else {
                        bbox = BoundingBoxHelper.getBounds(bbox!!, BoundingBoxHelper.getBounds(route.geometry))
                    }
                }
                var newCameraPosition = mapView.mapWindow.map.cameraPosition(bbox!!)
                newCameraPosition = CameraPosition(
                    newCameraPosition.target,
                    max(newCameraPosition.zoom - 0.5f, mapView.mapWindow.map.minZoom),
                    0.0f,
                    newCameraPosition.tilt
                )
                val animation = Animation(Animation.Type.SMOOTH, 0.3f)
                mapView.mapWindow.map.move(newCameraPosition, animation) { }
            }
            RouteChangeReason.CLEAR, RouteChangeReason.IMMEDIATE_GUIDANCE -> {
                button_overview_on.text = "Overview On"
                setFollowingEnabledImpl(true)
            }
            else -> {}
        }
        updateGuidanceViews()
    }

    override fun onRouteSelectionChanged() {
    }

    override fun onRoutesRequestError(error: Error) {
        Toast.makeText(this, "Error occurred while request routes!", Toast.LENGTH_LONG).show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onGuidanceResumedChanged() = Unit
}
