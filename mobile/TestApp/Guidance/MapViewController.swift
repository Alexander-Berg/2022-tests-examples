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

private enum SimulationMode {
    case route
    case geometry
    case ticketNumber
}

class MapViewController: UIViewController {

    // MARK: Private properties
    private let systemLocationManager = CLLocationManager()

    private var scale: CGFloat { return UIScreen.main.scale }
    private var coordConverter: PlatformCoordConverter!
    private var mapView: YMKMapView!
    private var guidanceLayer: YNKNaviGuidanceLayer!

    private var userLocationLayer: YMKUserLocationLayer!

    private var guidance: YNKGuidance {
        return AppDelegate.shared.guidance
    }

    private var routeBuilder: YNKRouteBuilder {
        return AppDelegate.shared.guidance.routeBuilder()
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
    private lazy var northAtTheTopButton: UIButton = zoomButtonFactory(buttonFactory("N", #selector(northAtTheTop)))
    private lazy var disableAllButton: UIButton = zoomButtonFactory(buttonFactory("V", #selector(disableAll)))

    private lazy var overviewButton: UIButton = buttonFactory("Overview", #selector(updateOverviewViewVisibility))
    private lazy var stickManeuverBalloonButton: UIButton =
        buttonFactory("Maneuver balloon on road Off", #selector(switchStickManeuverBalloonState))
    private lazy var simulationButton: UIButton = buttonFactory("Simulation", #selector(updateSimulationViewVisibility))
    private lazy var avoidTollsButton: UIButton = buttonFactory("Avoid tolls On", #selector(switchAvoidTolls))
    private lazy var copyGeometryToClipboardButton: UIButton = buttonFactory("Copy geometry", #selector(copyGeometry))
    private let contentView = DayNightPassthroughView()

    private var etaContainer: (UIView & Dismissible)!
    private var speedView: ContextSpeedView!
    private var speedLimitView: ContextSpeedLimitView!
    private var maneuverView: ContextManeuverView!
    private var statusPanel: StatusPanel!
    private var nextCameraView: NextCameraView!
    private var fasterAlternativeWidget: FasterAlternativeWidgetView!

    private var maneuverWrapper: RectProviderImpl!
    private var speedWrapper: RectProviderImpl!
    private var speedLimitWrapper: RectProviderImpl!
    private var statusPanelWrapper: RectProviderImpl!
    private var etaWrapper: RectProviderImpl!
    private var nextCameraWrapper: RectProviderImpl!

    private var fasterAlternativeWidgetCard: UIView = UIView()
    private var simulationView: UIStackView = UIStackView()
    private lazy var simulateWithRouteButton: UIButton = buttonFactory("Route", #selector(startSimulationWithExistingRoute))
    private lazy var simulateWithGeometryButton: UIButton = buttonFactory("Geometry", #selector(startSimulationWithGeometry))
    private lazy var simulateWithTicketButton: UIButton = buttonFactory("MAPKITSIM-4261", #selector(startSimulationWithTicketNumber))
    private lazy var simulationStopButton: UIButton = buttonFactory("Stop simulation", #selector(stopSimulation))

    private var overviewView: UIStackView = UIStackView()
    private lazy var overviewOnButton: UIButton = buttonFactory("Overview On", #selector(moveToOverview))
    private lazy var cancelOverviewButton: UIButton = buttonFactory("Cancel", #selector(cancelOverview))

    private var isSimulated = false
    private var isAvoidTolls = false
    private var isStickManeuverOnRoadEnabled = true
    private var currentNorthOnTop = false

    private var inactivityTimer: Timer?

    private var lastToPoint: YMKPoint?
    private var lastFromPoint: YMKPoint?

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
        mapView.mapWindow.map.isFastTapEnabled = true
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

        contentView.addSubview(northAtTheTopButton)
        northAtTheTopButton.activateConstraintAnchor(.top, toItem: zoomOutButton, spacing: 30)
        northAtTheTopButton.activateConstraintAnchorToParent(.right)

        contentView.addSubview(disableAllButton)
        disableAllButton.activateConstraintAnchor(.top, toItem: northAtTheTopButton, spacing: 30)
        disableAllButton.activateConstraintAnchorToParent(.right)

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

        contentView.addSubview(copyGeometryToClipboardButton)
        copyGeometryToClipboardButton.activateConstraintAnchorToParent(.bottom)
        copyGeometryToClipboardButton.activateConstraintAnchor(.left, toItem: stickManeuverBalloonButton, spacing: 5)

        contentView.addSubview(fasterAlternativeWidgetCard)
        fasterAlternativeWidgetCard.activateConstraintsFillParentToHorizontalSafeArea((10, 10))
        fasterAlternativeWidgetCard.activateConstraintsAnchorToSafeArea(.bottom)
        fasterAlternativeWidgetCard.backgroundColor = DayNight.eta
        fasterAlternativeWidgetCard.layer.zPosition = 1
        fasterAlternativeWidgetCard.isHidden = true
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

        guidance.start(with: nil)
        guidance.add(self)

        routeBuilder.addListener(with: self)

        guidanceLayer = YNKNaviGuidanceLayerFactory.createNaviGuidanceLayer(
            with: mapView.mapWindow,
            routePolylinesCollection: mapView.mapWindow.map.mapObjects.add(),
            balloonsCollection: mapView.mapWindow.map.mapObjects,
            routeEventsCollection: mapView.mapWindow.map.mapObjects,
            parkingSnippetCollection: mapView.mapWindow.map.mapObjects,
            routePointsCollection: mapView.mapWindow.map.mapObjects,
            balloonFactory: BalloonFactoryImpl(
                coordConverter: coordConverter, scale: scale, useYMapsStyles: true),
            parkingSnippetProvider: ParkingSnippetProvider(coordConverter: coordConverter, scale: scale),
            displayMetrics: YNKGetDisplayMetrics(),
            platformImageProvider: PlatformImageProvider(targetDisplayScale: scale),
            platformColorProvider: PlatformColorProvider())
        guidanceLayer.add(self)
        let events: [YMKRoadEventsEventTag] = [
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
        guidanceLayer.setWayPointPinsVisible(true)
        guidanceLayer.setOverlapRects([maneuverWrapper,
            statusPanelWrapper,
            speedWrapper,
            speedLimitWrapper,
            etaWrapper,
            nextCameraWrapper])

        let factory = guidanceLayer.presentersFactory()
        let config = ContextGuidanceLayoutConfig()

        etaContainer = ContextGuidanceViewsFactory.createCombinedEtaRouteProgressView(
            presenter: factory.createEtaRouteProgressPresenter(), useShadow: true)
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

        speedLimitView = ContextSpeedLimitView(presenter: factory.createSpeedLimitPresenter(),
            dimensions: ContextSpeedViewDefaultDimensions())
        speedLimitWrapper.setView(speedLimitView)
        contentView.addSubview(speedLimitView)
        speedLimitView.widthConstraint = speedLimitView.activateConstraintWidth(config.speedLimitSize)
        speedLimitView.activateConstraintHeight(config.speedLimitSize)
        speedLimitView.activateConstraintAnchorToParent(.top)
        speedLimitView.activateConstraintAnchorToParent(.right, spacing: 5)
        speedLimitView.spacingConstraint = speedLimitView.activateConstraintAnchor(
            .left, toItem: speedView, spacing: -config.smallSpacing)
        speedLimitView?.setVisibility(speedLimitWrapper.isRectVisible(), width: ContextGuidanceLayoutConfig().speedLimitSize)

        statusPanel = StatusPanel(presenter: factory.createStatusPanelPresenter(),
            statusViewDimensions: MobileStatusPanelDesignGuide(), useShadow: true)
        statusPanelWrapper.setView(statusPanel)
        statusPanel.style = .center
        contentView.addSubview(statusPanel)
        statusPanel.activateConstraintAnchor(.bottom, toItem: etaContainer)
        statusPanel.activateConstraintEqual(.left, toItem: etaContainer)
        statusPanel.activateConstraintEqual(.right, toItem: etaContainer)
        statusPanel.isHidden = !statusPanelWrapper.isRectVisible()

        nextCameraView = NextCameraView(presenter: factory.createNextCameraPresenter(),
            dimensions: NextCameraViewMobileDimensions(), useShadow: true)
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
        maneuverView.activateConstraintAnchorToParent(.top, spacing: 8.adaptedSize())
        maneuverView.activateConstraintAnchorToParent(.left, spacing: 8.adaptedSize())
        maneuverView.translatesAutoresizingMaskIntoConstraints = true
        maneuverView.isHidden = !maneuverWrapper.isRectVisible()

        fasterAlternativeWidget = FasterAlternativeWidgetView(
            presenter: factory.createFasterAlternativeWidgetPresenter())
        fasterAlternativeWidgetCard.addSubview(fasterAlternativeWidget)
        fasterAlternativeWidgetCard.activateConstraintHeight(fasterAlternativeWidget.maxHeight())
        fasterAlternativeWidget.activateConstraintsFillParent()

        contentView.addSubview(simulationView)
        simulationView.axis = .vertical
        simulationView.isHidden = true
        simulationView.activateConstraintAnchorToParent(.centerX)
        simulationView.activateConstraintAnchorToParent(.centerY)

        simulationView.addArrangedSubview(simulateWithRouteButton)
        simulationView.addArrangedSubview(simulateWithGeometryButton)
        simulationView.addArrangedSubview(simulateWithTicketButton)
        simulationView.addArrangedSubview(simulationStopButton)

        contentView.addSubview(overviewView)
        overviewView.axis = .vertical
        overviewView.isHidden = true
        overviewView.activateConstraintAnchorToParent(.centerX)
        overviewView.activateConstraintAnchorToParent(.centerY)

        overviewView.addArrangedSubview(overviewOnButton)
        overviewView.addArrangedSubview(cancelOverviewButton)

        setFollowingEnabledImpl(true)
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
        if !isOverviewStateEnabled() {
            guard guidance.route() != .none else {
                showWarning("Can't enable overview state when main route unset!")
                return
            }
            routeBuilder.requestAlternatives()
        } else {
            routeBuilder.startGuidance()
        }
        updateOverviewViewVisibility()
    }

    @objc private func cancelOverview() {
        if isOverviewStateEnabled() {
            routeBuilder.clearRoutes()
        }
        updateOverviewViewVisibility()
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

    @objc private func northAtTheTop() {
        currentNorthOnTop = !currentNorthOnTop
        startInactivityTimer()
        let currentCameraPosition = mapView.mapWindow.map.cameraPosition
        let newCameraPosition = YMKCameraPosition(
            target: currentCameraPosition.target,
            zoom: currentCameraPosition.zoom,
            azimuth: currentNorthOnTop ? 0 :
                (guidance.location?.location.heading?.floatValue ?? 0),
            tilt: currentCameraPosition.tilt)
        let animation = YMKAnimation(type: .smooth, duration: 0.3)
        mapView.mapWindow.map.move(with: newCameraPosition, animationType: animation, cameraCallback: { _ in })

        guidanceLayer.setLanesInFixedBalloonAvailable(currentNorthOnTop)
        showWarning("Changed north on top to \(currentNorthOnTop)")
    }

    @objc private func disableAll() {
        if disableAllButton.titleLabel!.text == "V" {
            disableAllButton.setTitle("H", for: .normal)
            guidanceLayer.setLayerObjectsVisible(false)
            showWarning("Hidden all object on layer")
        } else {
            disableAllButton.setTitle("V", for: .normal)
            guidanceLayer.setLayerObjectsVisible(true)
            showWarning("Shown all object on layer")
        }
    }

    @objc private func updateOverviewViewVisibility() {
        overviewView.isHidden = !overviewView.isHidden
    }

    @objc private func updateSimulationViewVisibility() {
        simulationView.isHidden = !simulationView.isHidden
    }

    @objc private func startSimulationWithExistingRoute() {
        startSimulation(with: .route)
        updateSimulationViewVisibility()
    }

    @objc private func startSimulationWithGeometry() {
        startSimulation(with: .geometry)
        updateSimulationViewVisibility()
    }

    @objc private func startSimulationWithTicketNumber() {
        startSimulation(with: .ticketNumber)
        updateSimulationViewVisibility()
    }

    @objc private func stopSimulation() {
        guard isSimulated else { return }

        if isOverviewStateEnabled() {
            routeBuilder.clearRoutes()
        }
        guidance.stopSimulation()
        isSimulated = false
        updateSimulationViewVisibility()
    }

    @objc private func switchAvoidTolls() {
        isAvoidTolls = !isAvoidTolls
        guidance.configurator().setTollAvoidanceEnabledWithOn(isAvoidTolls)
        avoidTollsButton.setTitle("Avoid tolls " + (isAvoidTolls ? "Off" : "On"), for: .normal)
    }

    @objc private func copyGeometry() {
        guard let route = guidance.route() ?? routeFromVariants() else { showWarning("No route!"); return }
        ("[" + route.geometry.points
            .map { (point) -> String in return "[\(point.latitude), \(point.longitude)]" }
            .joined(separator: ", ") + "]")
                .saveToClipboard()
        showWarning("Saved to clipboard")
    }

    private func routeFromVariants() -> YMKDrivingRoute? {
        guard let index = guidance.routeBuilder().selectedRouteIndex() else { return nil }
        return guidance.routeBuilder().routes[index.intValue]
    }

    private func startSimulation(with mode: SimulationMode) {
        guard guidance.route() != .none || mode == .ticketNumber else {
            showWarning("Can't start simulation without route or ticket number!")
            return
        }

        guidance.setSimulatedSpeed(82)
        switch mode {
            case .route:
                guidance.startSimulationWithExistingRoute()
            case .geometry:
                guidance.startSimulation(withGeometry: guidance.route()!.geometry)
            case .ticketNumber:
                guidance.startSimulation(withTicketNumber: 4261)
        }
        isSimulated = true
    }

    private func buildRoute(from: YMKPoint, to: YMKPoint) {
        let routingOptions = YNKRoutingOptions()

        routeBuilder.requestRoutes(with: [
            YMKRequestPoint(point: from, type: .waypoint, pointContext: .none),
            YMKRequestPoint(point: to, type: .waypoint, pointContext: .none)],
            routingOptions: routingOptions)
    }

    private func showWarning(_ text: String) {
        let alert = UIAlertController(title: "Warning", message: text, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "Ok", style: .default, handler: .none))
        present(alert, animated: true, completion: .none)
    }

    private func checkLocation(_ completion: (YMKPoint) -> Void) {
        guard let myPosition = guidance.location?.location.position else {
            showWarning("Can't build route without location!")
            return
        }
        completion(myPosition)
    }

    private func stepZoom(_ step: Float) {
        startInactivityTimer()
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
        setFollowingEnabled(false)
        inactivityTimer?.invalidate()
        inactivityTimer = Timer.scheduledTimer(
            withTimeInterval: TimeInterval(5),
            repeats: false,
            block: { [weak self] _ in
                self?.setFollowingEnabled(true)
                self?.inactivityTimer = .none
            })
    }

    private func setFollowingEnabled(_ enabled: Bool) {
        guard routeBuilder.routes.count == 0 else { return }
        setFollowingEnabledImpl(enabled)
    }

    private func setFollowingEnabledImpl(_ enabled: Bool) {
        guidanceLayer.setManeuverBalloonVisible(enabled && !currentNorthOnTop)
        userLocationLayer.isAutoZoomEnabled = enabled
        userLocationLayer.isHeadingEnabled = enabled && !currentNorthOnTop
        if enabled {
            let screenSize = CGSize(width: UIScreen.main.bounds.size.width * UIScreen.main.scale,
                height: UIScreen.main.bounds.size.height * UIScreen.main.scale)
            userLocationLayer.setAnchorWithAnchorNormal(
                CGPoint(x: 0.5 * screenSize.width, y: 0.5 * screenSize.height),
                anchorCourse: CGPoint(x: 0.5 * screenSize.width, y: 0.78 * screenSize.height))
        } else {
            userLocationLayer.resetAnchor()
        }
    }

    private func updateETAVisibility() {
        let routeExists = guidance.route() != .none
        etaWrapper.setContentAvailable(routeExists && !isOverviewStateEnabled())
    }

    private func updateStatusVisibility() {
        let routeExists = guidance.route() != .none
        statusPanelWrapper.setContentAvailable(guidanceLayer.isStatusPanelVisible &&
            !isOverviewStateEnabled() && routeExists)
    }

    private func updateNextCameraVisibility() {
        nextCameraWrapper.setContentAvailable(!isOverviewStateEnabled() &&
            guidanceLayer.isNextCameraVisible)
    }

    private func updateSpeedVisibility() {
        speedWrapper.setContentAvailable(guidanceLayer.isSpeedVisible && !isOverviewStateEnabled())
    }

    private func updateSpeedLimitVisibility() {
        let visible = guidanceLayer.isSpeedLimitVisible && !isOverviewStateEnabled()
        speedLimitWrapper.setContentAvailable(visible)
        speedLimitView?.setVisibility(visible,
            width: ContextGuidanceLayoutConfig().speedLimitSize)
    }

    private func updateManeuverVisibility() {
        maneuverWrapper.setContentAvailable(!isOverviewStateEnabled() &&
            guidanceLayer.isManeuverVisible)
    }

    private func updateVisibility() {
        updateManeuverVisibility()
        updateSpeedVisibility()
        updateSpeedLimitVisibility()
        updateNextCameraVisibility()
        updateStatusVisibility()
        updateETAVisibility()
    }

    private func isOverviewStateEnabled() -> Bool {
        return routeBuilder.routes.count > 0
    }
}

extension MapViewController: CLLocationManagerDelegate {
    func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        userLocationLayer?.setSourceWith(YMKGuideLocationViewSourceFactory.createLocationViewSource(with: guidance.mapkitGuide()))
        userLocationLayer?.setVisibleWithOn(true)
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
    }
}

extension MapViewController: YNKNaviGuidanceLayerListener {

    func onFasterAlternativeWidgetAction(_ action: YNKFasterAlternativeWidgetAction) {
        if action == .go {
            guidance.routeBuilder().requestAlternatives()
        }
        guidanceLayer.notifyFasterAlternativeWidgetWasClosed()
    }

    func onParkingPointTapped(with parkingPointInfo: YNKParkingPointInfo) {
    }

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
        guidance.stop()
        guidance.start(with: nil)
    }

    func onFasterAlternativeVisibilityChanged() {
        fasterAlternativeWidgetCard.isHidden = !guidanceLayer.isFasterAlternativeVisible
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
}

extension MapViewController: YNKGuidanceListener {
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

extension MapViewController: YMKMapInputListener {
    func onMapTap(with map: YMKMap, point: YMKPoint) {
        guidanceLayer.deselectRoadEvent()
        startInactivityTimer()
    }

    func onMapLongTap(with map: YMKMap, point: YMKPoint) {
        guidanceLayer.deselectRoadEvent()
        startInactivityTimer()
        let alert = UIAlertController(title: "Action", message: "Build route to?", preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "To", style: .default, handler: { [weak self] _ in
            guard let strong = self else { return }

            strong.lastToPoint = point

            guard let fromPoint = strong.lastFromPoint else {
                strong.checkLocation { [weak self] from in
                    self?.buildRoute(from: from, to: point)
                }
                return
            }

            strong.buildRoute(from: fromPoint, to: point)
            strong.lastFromPoint = nil
        }))
        alert.addAction(UIAlertAction(title: "From", style: .default, handler: { [weak self] _ in
            guard let strong = self else { return }

            guard let toPoint = strong.lastToPoint else {
                strong.lastFromPoint = point
                strong.showWarning("Please set To point!")
                return
            }

            strong.buildRoute(from: point, to: toPoint)
        }))
        alert.addAction(UIAlertAction(title: "Cancel", style: .destructive, handler: .none))
        present(alert, animated: true, completion: .none)
    }
}

extension MapViewController: YMKMapCameraListener {
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

extension MapViewController: YNKRouteBuilderListener {
    func onRouteSelectionChanged() {
    }

    func onRoutesChanged(with reason: YNKRouteChangeReason) {
        switch reason {
        case .routerResponse, .autofill:
            overviewOnButton.setTitle("Start guidance", for: .normal)
            setFollowingEnabledImpl(false)

            var bbox: YMKBoundingBox!
            for route in routeBuilder.routes {
                if let bbox_ = bbox {
                    bbox = YMKMergeBounds(bbox_, YMKGetPolylineBounds(route.geometry))
                } else {
                    bbox = YMKGetPolylineBounds(route.geometry)
                }
            }

            let animation = YMKAnimation(type: .smooth, duration: 0.3)
            var newCameraPosition = mapView.mapWindow.map.cameraPosition(with: bbox)
            newCameraPosition = YMKCameraPosition(
                target: newCameraPosition.target,
                zoom: max(newCameraPosition.zoom - 0.5, mapView.mapWindow.map.getMinZoom()),
                azimuth: 0,
                tilt: newCameraPosition.tilt)
            mapView.mapWindow.map.move(with: newCameraPosition, animationType: animation, cameraCallback: nil)
        case .clear, .immediateGuidance:
            overviewOnButton.setTitle("Overview On", for: .normal)
            setFollowingEnabledImpl(true)
        default:
            break
        }

        updateVisibility()
    }

    func onRoutesRequestErrorWithError(_ error: Error) {
        showWarning("Error occurred while request routes!")
    }
}

extension String {
    func saveToClipboard() {
        UIPasteboard.general.string = self
    }
}
