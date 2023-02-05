//  Copyright © 2020 Yandex. All rights reserved.
//

import UIKit
import YandexNaviKit
import YandexMapsMobile
import YandexNaviGuidanceUI
import YandexNaviHelpers
import YandexNaviDayNight

import CoreLocation

private class RectProviderImpl: NSObject, YNKRectProvider {

    // MARK: Constructors

    init(_ view: UIView?, _ coordConverter: PlatformCoordConverter) {
        self.coordConverter = coordConverter
        super.init()
        self.view = view
    }

    // MARK: Public Methods

    func isRectVisible() -> Bool {
        return (isContentAvailable ?? false) && (view != .none)
    }

    func getRect() -> YMKScreenRect? {
        guard let view = self.view else { return .none }
        return coordConverter.toScreenRect(from: view).toMapkitRect()
    }

    func setContentAvailable(_ contentAvailable: Bool) {
        isContentAvailable = contentAvailable
        view?.isHidden = !isRectVisible()
    }

    func setView(_ view: UIView?) {
        self.view = view
    }

    // MARK: Private Properties

    private weak var view: UIView?
    private unowned let coordConverter: PlatformCoordConverter
    private var isContentAvailable: Bool?
}

class TestAppMapViewController: UIViewController {

    // MARK: Private properties
    private let systemLocationManager = CLLocationManager()

    private var scale: CGFloat { return UIScreen.main.scale }
    private var coordConverter: PlatformCoordConverter!
    private var mapView: YMKMapView!
    private var locationManager: YMKLocationManager!
    private var drivingRouter: YMKDrivingRouter!
    private var drivingSession: YMKDrivingSession?
    private var guidanceLayer: YNKNaviGuidanceLayer!

    private var userLocationLayer: YMKUserLocationLayer!

    private var guidance: YNKGuidance {
        return AppDelegate.shared.guidance
    }

    private let buttonFactory: (String, Selector) -> UIButton = { title, selector in
        let btn = UIButton()
        btn.backgroundColor = .white
        btn.setTitleColor(.black, for: .normal)
        btn.setTitle(title, for: .normal)
        btn.titleLabel?.font = YSFontFactory.font(ofSize: 10)
        btn.addTarget(self, action: selector, for: .touchUpInside)
        return btn
    }

    private let zoomButtonFactory: (UIButton) -> UIButton = { button in
        button.titleLabel?.font = YSFontFactory.font(ofSize: 24)
        return button
    }

    private lazy var zoomInButton: UIButton = zoomButtonFactory(buttonFactory("+", #selector(stepZoomIn)))
    private lazy var zoomOutButton: UIButton = zoomButtonFactory(buttonFactory("-", #selector(stepZoomOut)))

    private lazy var overviewButton: UIButton = buttonFactory("Overview On", #selector(moveToOverview))
    private lazy var stickManeuverBalloonButton: UIButton =
        buttonFactory("Maneuver balloon on road Off", #selector(switchStickManeuverBalloonState))
    private lazy var simulationButton: UIButton = buttonFactory("Start simulation", #selector(switchSimulation))
    private lazy var avoidTollsButton: UIButton = buttonFactory("Avoid tolls On", #selector(switchAvoidTolls))
    private let contentView = DayNightPassthroughView()

    private var etaContainer: (UIView & Dismissible)!
    private var speedView: ContextSpeedView!
    private var speedLimitView: ContextSpeedLimitView!
    private var maneuverView: ContextManeuverView!
    private var statusPanel: StatusPanel!
    private var nextCameraView: NextCameraView!

    private var maneuverWrapper: RectProviderImpl!
    private var speedWrapper: RectProviderImpl!
    private var speedLimitWrapper: RectProviderImpl!
    private var statusPanelWrapper: RectProviderImpl!
    private var etaWrapper: RectProviderImpl!
    private var nextCameraWrapper: RectProviderImpl!

    private var isSimulated = false
    private var isAvoidTolls = false
    private var isOverviewEnabled = false
    private var isStickManeuverOnRoadEnabled = true

    private lazy var versionLockMode: UIButton = buttonFactory("Version lock", #selector(chooseVersionLock))
    private lazy var licenseLockMode: UIButton = buttonFactory("License lock", #selector(chooseLicenseLock))
    private let carPlayMode = UILabel()

    private var inactivityTimer: Timer?

    // MARK: Deinit

    deinit {
        inactivityTimer?.invalidate()
        mapView.mapWindow.map.removeCameraListener(with: self)
        mapView.mapWindow.map.removeInputListener(with: self)
        guidanceLayer.remove(self)
        guidanceLayer.destroy()
        guidance.remove(self)
    }

    // MARK: Life cycle
    override func loadView() {
        view = UIView()

        mapView = YMKMapView(frame: CGRect.zero, vulkanPreferred: false)
        view.addSubview(mapView)
        mapView.activateConstraintsFillParent()

        view.addSubview(contentView)
        contentView.activateConstraintsFillParentToSafeArea()

        contentView.addSubview(zoomInButton)
        zoomInButton.activateConstraintAnchorToParent(.centerY)
        zoomInButton.activateConstraintAnchorToParent(.right)

        contentView.addSubview(zoomOutButton)
        zoomOutButton.activateConstraintAnchor(.top, toItem: zoomInButton, spacing: 5)
        zoomOutButton.activateConstraintAnchorToParent(.right)

        contentView.addSubview(overviewButton)
        overviewButton.activateConstraintAnchorToParent(.bottom)
        overviewButton.activateConstraintAnchorToParent(.left)

        contentView.addSubview(simulationButton)
        simulationButton.activateConstraintAnchorToParent(.bottom)
        simulationButton.activateConstraintAnchor(.left, toItem: overviewButton, spacing: 5)

        contentView.addSubview(avoidTollsButton)
        avoidTollsButton.activateConstraintAnchorToParent(.bottom)
        avoidTollsButton.activateConstraintAnchor(.left, toItem: simulationButton, spacing: 5)

        contentView.addSubview(stickManeuverBalloonButton)
        stickManeuverBalloonButton.activateConstraintAnchorToParent(.bottom)
        stickManeuverBalloonButton.activateConstraintAnchor(.left, toItem: avoidTollsButton, spacing: 5)

        contentView.addSubview(versionLockMode)
        versionLockMode.activateConstraintAnchorToParent(.left)
        versionLockMode.activateConstraintAnchorToParent(.centerY)

        contentView.addSubview(licenseLockMode)
        licenseLockMode.activateConstraintAnchorToParent(.left)
        licenseLockMode.activateConstraintAnchor(.bottom, toItem: versionLockMode, spacing: 5)

        contentView.addSubview(carPlayMode)
        carPlayMode.activateConstraintAnchorToParent(.left)
        carPlayMode.activateConstraintAnchor(.bottom, toItem: licenseLockMode, spacing: 5)
        carPlayMode.backgroundColor = .white
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        userLocationLayer = YMKMapKit.sharedInstance().createUserLocationLayer(with: mapView.mapWindow)

        mapView.mapWindow.map.addInputListener(with: self)
        mapView.mapWindow.map.addCameraListener(with: self)

        systemLocationManager.delegate = self
        let authorizationStatus = CLLocationManager.authorizationStatus()

        if (authorizationStatus == .authorizedWhenInUse || authorizationStatus == .authorizedAlways) {
            systemLocationManager.startUpdatingLocation()
        } else {
            systemLocationManager.requestWhenInUseAuthorization()
            systemLocationManager.startUpdatingLocation()
        }

        coordConverter = DefaultPlatformCoordConverter(view: { [unowned self] in return self.view }, scale: scale)

        maneuverWrapper = RectProviderImpl(nil, coordConverter)
        speedLimitWrapper = RectProviderImpl(nil, coordConverter)
        speedWrapper = RectProviderImpl(nil, coordConverter)
        statusPanelWrapper = RectProviderImpl(nil, coordConverter)
        etaWrapper = RectProviderImpl(nil, coordConverter)
        nextCameraWrapper = RectProviderImpl(nil, coordConverter)

        drivingRouter = YMKDirections.sharedInstance()?.createDrivingRouter()

        guidance.add(self)

        guidanceLayer = YNKNaviGuidanceLayerFactory.createNaviGuidanceLayer(
            with: mapView.mapWindow,
            routePolylinesCollection: mapView.mapWindow.map.mapObjects.add(),
            balloonsCollection: mapView.mapWindow.map.mapObjects,
            routeEventsCollection: mapView.mapWindow.map.mapObjects,
            parkingSnippetCollection: mapView.mapWindow.map.mapObjects,
            routePointsCollection: mapView.mapWindow.map.mapObjects,
            balloonFactory: BalloonFactoryImpl(coordConverter: coordConverter, scale: scale),
            parkingSnippetProvider: ParkingSnippetProvider(coordConverter: coordConverter, scale: scale),
            displayMetrics: YNKGetDisplayMetrics(),
            platformImageProvider: PlatformImageProvider(),
            platformColorProvider: PlatformColorProvider())
        guidanceLayer.add(self)
        let events: [YMKRoadEventsEventTag] = [
            .drawbridge,
            .closed,
            .reconstruction,
            .accident,
            .trafficAlert,

            .danger,
            .school,
            .overtakingDanger,
            .pedestrianDanger,
            .crossRoadDanger,

            .police,
            .laneControl,
            .roadMarkingControl,
            .crossRoadControl,
            .noStoppingControl,
            .mobileControl,
            .speedControl
        ]
        guidanceLayer.setRoadEventsAvailable(events.map { NSNumber(value: $0.rawValue) })
        guidanceLayer.setAlternativesVisible(true)
        guidanceLayer.setCameraAlertsEnabled(true)
        guidanceLayer.setContextBalloonsVisible(true)
        guidanceLayer.setManeuverBalloonVisible(true)
        guidanceLayer.setTrafficLightsUnderRoadEvents(false)
        guidanceLayer.setRouteAlertsEnabled(true)
        guidanceLayer.setOverlapRects([maneuverWrapper,
            statusPanelWrapper,
            speedWrapper,
            speedLimitWrapper,
            etaWrapper,
            nextCameraWrapper])

        let factory = guidanceLayer.presentersFactory()
        let config = ContextGuidanceLayoutConfig()

        etaContainer = ContextGuidanceViewsFactory.createCombinedEtaRouteProgressView(
            presenter: factory.createEtaRouteProgressPresenter())
        etaWrapper.setView(etaContainer)
        contentView.addSubview(etaContainer)
        etaContainer.activateConstraintsFillParentToHorizontalSafeArea((10, 10))
        etaContainer.activateConstraintAnchor(.bottom, toItem: overviewButton, spacing: 10)
        etaContainer.isHidden = true

        speedView = ContextSpeedView(presenter: factory.createSpeedPresenter(),
                                     dimensions: ContextSpeedViewDefaultDimensions())
        speedWrapper.setView(speedView)
        contentView.addSubview(speedView)
        speedView.activateConstraintWidth(config.speedLimitSize)
        speedView.activateConstraintHeight(config.speedLimitSize)
        speedView.activateConstraintAnchorToParent(.top)
        speedView.isHidden = !speedWrapper.isRectVisible()

        speedLimitView = ContextSpeedLimitView(presenter: factory.createSpeedLimitPresenter(), dimensions: ContextSpeedViewDefaultDimensions())
        speedLimitWrapper.setView(speedLimitView)
        contentView.addSubview(speedLimitView)
        speedLimitView.widthConstraint = speedLimitView.activateConstraintWidth(config.speedLimitSize)
        speedLimitView.activateConstraintHeight(config.speedLimitSize)
        speedLimitView.activateConstraintAnchorToParent(.top)
        speedLimitView.activateConstraintAnchorToParent(.right, spacing: 5)
        speedLimitView.spacingConstraint = speedLimitView.activateConstraintAnchor(
            .left, toItem: speedView, spacing: -config.smallSpacing)
        speedLimitView?.setVisibility(speedLimitWrapper.isRectVisible(), width: ContextGuidanceLayoutConfig().speedLimitSize)

        statusPanel = StatusPanel(
            presenter: factory.createStatusPanelPresenter(),
            statusViewDimensions: MobileStatusPanelDesignGuide())
        statusPanelWrapper.setView(statusPanel)
        statusPanel.style = .center
        contentView.addSubview(statusPanel)
        statusPanel.activateConstraintAnchor(.bottom, toItem: etaContainer)
        statusPanel.activateConstraintEqual(.left, toItem: etaContainer)
        statusPanel.activateConstraintEqual(.right, toItem: etaContainer)
        statusPanel.isHidden = !statusPanelWrapper.isRectVisible()

        nextCameraView = NextCameraView(
            presenter: factory.createNextCameraPresenter(),
            dimensions: NextCameraViewMobileDimensions())
        contentView.addSubview(nextCameraView)
        nextCameraView.activateConstraintAnchorToParent(.right, spacing: 8.adaptedSize())
        nextCameraView.activateConstraintAnchor(.top, toItem: speedView, spacing: 8.adaptedSize())
        nextCameraView.isHidden = !nextCameraWrapper.isRectVisible()
        nextCameraWrapper.setView(nextCameraView)

        maneuverView = ContextManeuverView(
            presenter: factory.createManeuverPresenter(),
            gesturesFactory: DefaultGestureRecognizersFactory())
        maneuverWrapper.setView(maneuverView)
        contentView.addSubview(maneuverView)
        maneuverView.activateConstraintAnchorToParent(.top, spacing: 5)
        maneuverView.activateConstraintAnchorToParent(.left, spacing: 5)
        maneuverView.translatesAutoresizingMaskIntoConstraints = true
        maneuverView.isHidden = !maneuverWrapper.isRectVisible()

        AppDelegate.shared.carPlay.stateListener = {[weak self] in
            self?.updateCarPlayMode()
        }
        updateCarPlayMode()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        UIApplication.shared.isIdleTimerDisabled = true
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        UIApplication.shared.isIdleTimerDisabled = false
    }

    // MARK: Private methods
    @objc private func moveToOverview() {
        guard guidance.route() != .none else {
            showWarning("Can't enable overview state when main route unset!")
            return
        }

        if !isOverviewEnabled {
            enableOverview()
        } else {
            disableOverview()
        }
    }

    private func enableOverview() {
        changeZoom(12)
        guidance.routeBuilder().requestAlternatives()
        isOverviewEnabled = true
        overviewButton.setTitle("Overview Off", for: .normal)
        updateVisibility()
    }

    private func disableOverview() {
        isOverviewEnabled = false
        guidance.routeBuilder().startGuidance()
        overviewButton.setTitle("Overview On", for: .normal)
        updateVisibility()
    }

    @objc private func switchStickManeuverBalloonState() {
        isStickManeuverOnRoadEnabled = !isStickManeuverOnRoadEnabled
        guidanceLayer.setManeuverAndLaneBalloonsMerged(isStickManeuverOnRoadEnabled)
        stickManeuverBalloonButton.setTitle(
            "Maneuver balloon on road " + (isStickManeuverOnRoadEnabled ? "Off" : "On"),
            for: .normal)
    }

    @objc private func stepZoomIn() {
        stepZoom(1)
    }

    @objc private func stepZoomOut() {
       stepZoom(-1)
    }

    @objc private func switchSimulation() {
        if isSimulated {
            stopSimulation()
        } else {
            startSimulation()
        }
    }

    @objc private func chooseVersionLock() {
        let controller = UIAlertController(
            title: "Version Lock mode",
            message: "Choose lock mode",
            preferredStyle: .alert)

        controller.addAction(UIAlertAction(title: "Всё ок", style: .default, handler: { _ in
            AppDelegate.shared.versionRestriction.restriction = .none
        }))

        controller.addAction(UIAlertAction(title: "Надо обновиться", style: .default, handler: { _ in
            AppDelegate.shared.versionRestriction.restriction = .upgradeNeeded(handler: {})
        }))

        present(controller, animated: false)
    }

    @objc private func chooseLicenseLock() {
        let controller = UIAlertController(
            title: "License Lock mode",
            message: "Choose lock mode",
            preferredStyle: .alert)

        controller.addAction(UIAlertAction(title: "Всё ок", style: .default, handler: { _ in
            AppDelegate.shared.licenseRestriction.restriction = .none
        }))

        controller.addAction(UIAlertAction(title: "Купи лицензию", style: .default, handler: { _ in
            AppDelegate.shared.licenseRestriction.restriction = .licenseAvailableForBuying(handler: {})
        }))

        controller.addAction(UIAlertAction(title: "Лицензия не доступна", style: .default, handler: { _ in
            AppDelegate.shared.licenseRestriction.restriction = .licenseNotAvailable(reason: "Извините, но в вашем регионе сервис не доступен")
        }))

        present(controller, animated: false)
    }

    private func updateCarPlayMode() {
        carPlayMode.text = "\(AppDelegate.shared.carPlay.state)"
    }

    private func startSimulation() {
        guard guidance.route() != .none else {
            showWarning("Can't start simulation when main route unset!")
            return
        }

        guidance.startSimulationWithExistingRoute()
        guidance.setSimulatedSpeed(82)
        isSimulated = true
        simulationButton.setTitle("Stop simulation", for: .normal)
    }

    private func stopSimulation() {
        if isOverviewEnabled {
            disableOverview()
        }
        guidance.stopSimulation()
        isSimulated = false
        simulationButton.setTitle("Start simulation", for: .normal)
    }

    @objc private func switchAvoidTolls() {
        isAvoidTolls = !isAvoidTolls
        guidance.configurator().setTollAvoidanceEnabledWithOn(isAvoidTolls)
        avoidTollsButton.setTitle("Avoid tolls " + (isAvoidTolls ? "Off" : "On"), for: .normal)
    }

    private func buildRoute(from: YMKPoint, to: YMKPoint) {
        let drivingOptions = YMKDrivingDrivingOptions()
        drivingOptions.routesCount = 1
        drivingOptions.avoidTolls = NSNumber(booleanLiteral: isAvoidTolls)

        let vehicleOptions = YMKDrivingVehicleOptions()

        drivingSession?.cancel()
        drivingSession = drivingRouter.requestRoutes(with: [
            YMKRequestPoint(point: from, type: .waypoint, pointContext: .none),
            YMKRequestPoint(point: to, type: .waypoint, pointContext: .none)],
            drivingOptions: drivingOptions,
            vehicleOptions: vehicleOptions) { [weak self] routes, error in
                if let routes = routes, let route = routes.first {
                    self?.guidance.start(with: route)
                }
        }
    }

    private func showWarning(_ text: String) {
        let alert = UIAlertController(title: "Warning", message: text, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "Ok", style: .default, handler: .none))
        present(alert, animated: true, completion: .none)
    }

    private func checkLocation(_ completion: (YMKPoint) -> Void) {
        completion(guidance.location?.location.position ?? YMKPoint(lat: 55.751244, lon: 37.618423))
    }

    private func stepZoom(_ step: Float) {
        changeZoom(mapView.mapWindow.map.cameraPosition.zoom + step)
    }

    private func changeZoom(_ newZoom: Float) {
        let currentCameraPosition = mapView.mapWindow.map.cameraPosition
        let newCameraPosition = YMKCameraPosition(
            target: currentCameraPosition.target,
            zoom: newZoom,
            azimuth: currentCameraPosition.azimuth,
            tilt: currentCameraPosition.tilt)
        let animation = YMKAnimation(type: .smooth, duration: 0.3)
        mapView.mapWindow.map.move(with: newCameraPosition, animationType: animation, cameraCallback: { _ in })
    }

    private func startInactivityTimer() {
        guidanceLayer.setManeuverBalloonVisible(false)
        inactivityTimer?.invalidate()
        inactivityTimer = Timer.scheduledTimer(
            withTimeInterval: TimeInterval(5),
            repeats: false,
            block: { [weak self] _ in
                self?.guidanceLayer.setManeuverBalloonVisible(true)
                self?.inactivityTimer = .none
            })
    }

    private func updateETAVisibility() {
        let routeExists = guidance.route() != .none
        etaWrapper.setContentAvailable(routeExists && !isOverviewEnabled)
    }

    private func updateStatusVisibility() {
        let routeExists = guidance.route() != .none
        statusPanelWrapper.setContentAvailable(guidanceLayer.isStatusPanelVisible && !isOverviewEnabled && routeExists)
    }

    private func updateNextCameraVisibility() {
        nextCameraWrapper.setContentAvailable(!isOverviewEnabled && guidanceLayer.isNextCameraVisible)
    }

    private func updateSpeedVisibility() {
        speedWrapper.setContentAvailable(guidanceLayer.isSpeedVisible && !isOverviewEnabled)
    }

    private func updateSpeedLimitVisibility() {
        let visible = guidanceLayer.isSpeedLimitVisible && !isOverviewEnabled
        speedLimitWrapper.setContentAvailable(visible)
        speedLimitView?.setVisibility(visible,
            width: ContextGuidanceLayoutConfig().speedLimitSize)
    }

    private func updateManeuverVisibility() {
        maneuverWrapper.setContentAvailable(!isOverviewEnabled && guidanceLayer.isManeuverVisible)
    }

    private func updateVisibility() {
        updateManeuverVisibility()
        updateSpeedVisibility()
        updateSpeedLimitVisibility()
        updateNextCameraVisibility()
        updateStatusVisibility()
        updateETAVisibility()
    }
}

extension TestAppMapViewController: CLLocationManagerDelegate {
    func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        userLocationLayer?.setSourceWith(YMKGuideLocationViewSourceFactory.createLocationViewSource(with: guidance.mapkitGuide()))
        userLocationLayer?.setVisibleWithOn(true)
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
    }
}

extension TestAppMapViewController: YNKNaviGuidanceLayerListener {

    func onRouteEventTapped(_ event: YMKDrivingEvent, tag: YMKRoadEventsEventTag) {
        guidanceLayer.selectRoadEvent(event.eventId, tag: tag)
    }

    func onRoutingUnavailable() {

    }

    func onManeuverTapped(with point: YMKPoint, zoom: Float) {
        let currentCameraPosition = mapView.mapWindow.map.cameraPosition
        let newCameraPosition = YMKCameraPosition(
            target: point,
            zoom: zoom,
            azimuth: currentCameraPosition.azimuth,
            tilt: currentCameraPosition.tilt)
        let animation = YMKAnimation(type: .smooth, duration: 0.3)
        mapView.mapWindow.map.move(with: newCameraPosition, animationType: animation, cameraCallback: { _ in })
    }

    func onSpeedLimitTapped() {

    }

    func onSpeedVisibilityChanged() {
        updateSpeedVisibility()
    }

    func onRouteEventTapped(_ event: YMKDrivingEvent) {
        print(#function, event.eventId)
    }

    func onManeuverVisibilityChanged() {
        updateManeuverVisibility()
    }

    func onSpeedLimitVisibilityChanged() {
        updateSpeedLimitVisibility()
    }

    func onStatusPanelVisibilityChanged() {
        updateStatusVisibility()
    }

    func onNextCameraViewVisibilityChanged() {
        updateNextCameraVisibility()
    }

    func onFinishGuidanceTapped() {
        if isOverviewEnabled {
            disableOverview()
        }
        guidance.stop()
    }

    func onFasterAlternativeVisibilityChanged() {

    }

    func onZeroSpeedBannerVisibilityChanged() {

    }

    func onParkingWidgetVisibilityChanged() {

    }

    func onAdvertPinTapped(withInfo info: Any) {

    }

    func onZeroSpeedActionTapped(withInfo info: Any) {

    }

    func onWayPointTapped(withInfo info: Any) {

    }

    func onParkingPointTapped(with parkingPointInfo: YNKParkingPointInfo) {

    }

    func onFasterAlternativeWidgetAction(_ action: YNKFasterAlternativeWidgetAction) {

    }
}

extension TestAppMapViewController: YNKGuidanceListener {
    func onFasterAlternativeAnnotated() {

    }

    func onBackgroundGuidanceWillBeSuspended(with reason: YNKBgGuidanceSuspendReason) {

    }

    func onFasterAlternativeUpdated() {

    }

    func onRoutePositionUpdated() {

    }

    func onParkingRouteBuilt(_ type: YNKParkingRouteType) {

    }

    func onRouteChanged() {
        updateVisibility()
    }

    func onFreeDriveRouteChanged() {
    }

    func onFinishedRoute() {

    }

    func onReachedWayPoint() {

    }

    func onLocationUpdated() {

    }

    func onBackgroundGuidanceTaskRemoved() {

    }

    func onGuidanceResumedChanged() {

    }
}

extension TestAppMapViewController: YMKMapInputListener {
    func onMapTap(with map: YMKMap, point: YMKPoint) {
        guidanceLayer.deselectRoadEvent()
        startInactivityTimer()
    }

    func onMapLongTap(with map: YMKMap, point: YMKPoint) {
        guidanceLayer.deselectRoadEvent()
        startInactivityTimer()
        let alert = UIAlertController(title: "Action", message: "Build route to?", preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "Ok", style: .default, handler: { [weak self] _ in
            self?.checkLocation { [weak self] from in
                self?.buildRoute(from: from, to: point)
            }
        }))
        alert.addAction(UIAlertAction(title: "Cancel", style: .destructive, handler: .none))
        present(alert, animated: true, completion: .none)
    }
}

extension TestAppMapViewController: YMKMapCameraListener {
    func onCameraPositionChanged(
        with map: YMKMap,
        cameraPosition: YMKCameraPosition,
        cameraUpdateReason: YMKCameraUpdateReason,
        finished: Bool)
    {
        if cameraUpdateReason == .gestures {
            startInactivityTimer()
        }
    }
}
