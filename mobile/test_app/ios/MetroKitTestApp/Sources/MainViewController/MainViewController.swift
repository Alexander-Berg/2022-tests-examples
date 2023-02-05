//
//  MainViewController.swift
//  MetroKitTestApp
//
//  Created by Ilya Lobanov on 14/03/2017.
//  Copyright © 2017 Yandex LLC. All rights reserved.
//

import UIKit

final class MainViewController: UIViewController {

    enum NavBarState {
        case normal
        case route
    }
    
    typealias SchemeView = UIView & YMLSchemeViewProtocol


    private weak var schemeViewController: YMLSchemeViewController!
    
    private var schemeView: SchemeView {
        return schemeViewController.schemeView
    }
    
    private var schemeWindow: YMLSchemeWindow {
        return schemeView.schemeWindow
    }

    private let schemeManager: SchemeManager = MetrokitSchemeManager(impl: MetroKit.instance.createSchemeManager())

    private var scheme: YMLScheme? {
        didSet {
            infoService = scheme?.makeInfoService()
        }
    }

    private var requestSchemeAsync: Async<YMLScheme?>?
    private var requestSchemeListAsync: Async<YMLSchemeList?>?
    
    private(set) var infoService: YMLSchemeInfoService?
    private(set) var searchPrompt: YMLSchemeSearchPrompt?
    private(set) var alertsManager: YMLSchemeAlertsManager?

    private var selectedStationTrackers: [StationTracker] = []
    
    // MARK: UIViewController

    override func viewDidLoad() {
        super.viewDidLoad()

        view.backgroundColor = .white
        setupNavBar()
        setupViews()
        setupSurfaceController()
        loadDefaultScheme()
        setupFocusRect()
        setupFPS()
        
        checkStartupConfig()
    }
    
    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        updateFocusRect()
    }

    // MARK: Private

    private func setupViews() {
        schemeViewController = apply(YMLSchemeViewController()) {
            $0.willMove(toParent: self)
            addChild($0)
            
            view.addSubview($0.view)
            $0.view.frame = view.bounds
            $0.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
            
            $0.didMove(toParent: self)
            
            $0.delegate = self
        }

        routeCollection = RouteCollection(container: view)
        routeCollection.onShowRoute = { [weak self] index in
            self?.showRoute(at: index)
        }

        schemeWindow.cameraController.addCameraListener(with: self)
        schemeWindow.addTapListener(with: self)
        schemeWindow.isIsZoomOnDoubleTapEnabled = true
    }

    // MARK: Private - FPS
    
    private let fpsLabel = UILabel()
    
    private func setupFPS() {
        fpsLabel.textColor = UIColor(white: 0.1, alpha: 1.0)
        fpsLabel.backgroundColor = UIColor(white: 0.9, alpha: 1.0)
        fpsLabel.textAlignment = .center
        fpsLabel.layer.cornerRadius = 16
        fpsLabel.layer.masksToBounds = true
        fpsLabel.font = UIFont.boldSystemFont(ofSize: 14.0)
        
        view.addSubview(fpsLabel)
        
        // Layout
        
        let inset: CGFloat = 16
        
        fpsLabel.translatesAutoresizingMaskIntoConstraints = false
        fpsLabel.heightAnchor.constraint(equalToConstant: 32).isActive = true
        fpsLabel.widthAnchor.constraint(equalToConstant: 48).isActive = true
        if #available(iOS 11.0, *) {
            fpsLabel.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: inset).isActive = true
            fpsLabel.leftAnchor.constraint(equalTo: view.safeAreaLayoutGuide.leftAnchor, constant: inset).isActive = true
        } else {
            fpsLabel.topAnchor.constraint(equalTo: topLayoutGuide.topAnchor, constant: inset).isActive = true
            fpsLabel.leftAnchor.constraint(equalTo: view.leftAnchor, constant: inset).isActive = true
        }
    }

    // MARK: Private - Nav Bar

    private var navBarState: NavBarState = .normal {
        didSet {
            updateNavBar()
        }
    }

    private func setupNavBar() {
        navigationItem.title = "MetroKit"
        navigationItem.rightBarButtonItem = UIBarButtonItem(title: "Schemes", style: .plain, target: self, action: #selector(handleSchemesBarButton))
        updateNavBar()
    }

    private func updateNavBar() {
        switch navBarState {
        case .normal:
            navigationItem.leftBarButtonItem = UIBarButtonItem(title: "Actions", style: .plain, target: self,
                action: #selector(handleAction))
        case .route:
            navigationItem.leftBarButtonItem = UIBarButtonItem(title: "Clear", style: .done, target: self,
                action: #selector(handleCloseRouting))
        }
    }

    @objc private func handleSchemesBarButton() {
        let schemeListController = SchemeListViewController(schemeManager: schemeManager)

        schemeListController.onSelectScheme = { [weak self] summary in
            guard let slf = self else { return }

            slf.requestSchemeAsync = apply(slf.schemeManager.requestScheme(id: summary.schemeId)) {
                $0.onCompletion { [weak self] scheme in
                    if let scheme = scheme {
                        self?.showScheme(scheme)
                    }
                }
                $0.start()
            }
        }

        let nc = UINavigationController(rootViewController: schemeListController)
        present(nc, animated: true)
    }

    @objc private func handleCloseRouting() {
        dismissRoute()
    }

    @objc private func handleAction(_ item: UIBarButtonItem) {
        typealias Action = SchemeSettingsAlert.Action

        let actions: [Action] = [
            Action(title: "Share PDF") { [weak self] in
                self?.sharePDF()
            },
            Action(title: "Language") { [weak self] in
                self?.showLanguageSettings(barButtonItem: item)
            },
            Action(title: "Style") { [weak self] in
                self?.showStyleSettings(barButtonItem: item)
            },
            Action(title: "Set scale") { [weak self] in
                self?.showScaleSettings(barButtonItem: item)
            },
            Action(title: "Fit scheme") { [weak self] in
                guard let surface = self?.schemeWindow.surfaceController.mainSurface else { return }
                self?.updateCamera(fitting: surface)
            },
            Action(title: "Toggle camera animation") { [weak self] in
                self?.toggleCameraAnimation()
            },
            makeToggleFocusRectAction(),
            makeFocusVisibilityAction()
        ]

        let settings = SchemeSettingsAlert(title: "Actions", message: nil, actions: actions)
        settings.present(on: self, barButtonItem: item, animated: true)
    }

    // MARK: Private - Scheme

    private func setupSurfaceController() {
        schemeWindow.surfaceController.addListener(with: self)
        schemeWindow.surfaceController.mainSurface?.addListener(with: self)
    }

    private func loadDefaultScheme() {
        navigationItem.rightBarButtonItem?.isEnabled = false
        navigationItem.leftBarButtonItem?.isEnabled = false

        requestSchemeListAsync = schemeManager.requestSchemeList()
        requestSchemeListAsync?.onCompletion { [weak self] l in
            guard let slf = self else { return }

            slf.navigationItem.rightBarButtonItem?.isEnabled = true
            slf.navigationItem.leftBarButtonItem?.isEnabled = true

            if let summary = l?.selectSummary(withId: slf.schemeManager.lastSchemeId) {
                slf.printDescription(for: summary, title: "🐝 Default scheme was loaded")
                slf.requestSchemeAsync = apply(slf.schemeManager.requestScheme(id: summary.schemeId)) {
                    $0.onCompletion { [weak self] scheme in
                        if let scheme = scheme {
                            self?.showScheme(scheme)
                        }
                    }
                    $0.start()
                }
            }

        }
        requestSchemeListAsync?.start()
    }

    private func showScheme(_ scheme: YMLScheme) {
        self.scheme = scheme
        dismissRoute()
        schemeWindow.surfaceController.setSchemeWith(scheme, language: YMLLanguage(value: YXPlatformCurrentState.currentLanguage()))
        setupSurfaceController()

//        if let infoService = infoService {
//            printDescription(for: infoService)
//            checkSearch(for: infoService, query: "90  ,.. - ОДА")
//            checkSearch(for: infoService, query: "19 -  а")
//            checkSearch(for: infoService, query: "Атинская")
//            checkSearch(for: infoService, query: "Ку")
//
//            checkAlerts(for: infoService)
//
//            if let st = (infoService.stations.first { s in return s.historyInfo != nil }) {
//                print(st.customDescription)
//            }
//        }

        checkSchemeResources(for: scheme, style: schemeWindow.surfaceController.style)
        checkSearchPrompt(for: scheme)
        
        alertsManager = scheme.makeAlertsManager()
        alertsManager?.requestUpdate()
        alertsManager?.addListener(with: self)
        
        let sp = scheme.makeSearchPrompt()
        sp.startSearchForNearestStations()
        sp.addListener(with: self)
        
        searchPrompt = sp
    }

    // MARK: Private - Route

    private var routingSession: RoutingSession?

    private var routeCollection: RouteCollection!

    private var routingResult: YMLRoutingResult? = nil {
        didSet {
            routeCollection.routingResult = routingResult
        }
    }

    func allRoutes() -> [YMLRoute] {
        return (routingResult?.main ?? []) + (routingResult?.additional ?? [])
    }

    private func route(at index: Int) -> YMLRoute? {
        let routes = allRoutes()

        if routes.count <= index {
            return nil
        }
        return routes[index]
    }

    private func showRoute(at index: Int) {
        guard let r = route(at: index), let infoService = infoService,
            let details = infoService.resolveDetails(with: r),
            let style = schemeWindow.surfaceController.style
        else { return }

        let helper = RouteDetailsDescription(details: details, style: style)
        helper.printDescription()
        
        selectedStationTrackers = []
        routingSession = schemeWindow.surfaceController.show(route: r, infoService: infoService)
        navBarState = .route


        if let surface = schemeWindow.surfaceController.routeSurface {
            updateCamera(fitting: surface)
        }
    }

    private func dismissRoute() {
        routingResult = nil
        routingSession = nil
        navBarState = .normal
    }

    // MARK: Private - Closed Stations

    var closedStationsSession: ClosedStationsSession? = nil

    private func sharePDF() {
        let url = NSURL.fileURL(withPath: NSTemporaryDirectory().appending("scheme.pdf"))
        schemeWindow.renderToPdf(withFilePath: url.path)
        let vc = UIActivityViewController(activityItems: [url], applicationActivities: [])
        present(vc, animated: true)
    }

    // MARK: Private - Language

    private func showLanguageSettings(barButtonItem: UIBarButtonItem) {
        let actions = self.scheme?.languages.map { language in
            SchemeSettingsAlert.Action(title: language.value) { [weak self] in
                self?.schemeWindow.surfaceController.setLanguageWith(language)
            }
        }

        let settings = SchemeSettingsAlert(title: "Language", message: nil, actions: actions ?? [])
        settings.present(on: self, barButtonItem: barButtonItem, animated: true)
    }
    
    // MARK: Private - Style

    private func showStyleSettings(barButtonItem: UIBarButtonItem) {
        let actions = self.scheme?.styles.map { style in
            SchemeSettingsAlert.Action(title: style) { [weak self] in
                self?.schemeWindow.surfaceController.setStyleWithStyle(style)
            }
        }

        let settings = SchemeSettingsAlert(title: "Style", message: nil, actions: actions ?? [])
        settings.present(on: self, barButtonItem: barButtonItem, animated: true)
    }
    
    // MARK: Private - Zoom

    private func showScaleSettings(barButtonItem: UIBarButtonItem) {
        let cameraController = schemeWindow.cameraController
        let scales: [Int] = Array(Int(ceil(cameraController.minScale))...Int(floor(cameraController.maxScale)))
        let actions = scales.map { scale in
            SchemeSettingsAlert.Action(title: "\(scale)") { [weak self] in
                guard let cameraController = self?.schemeWindow.cameraController else { return }
                let newCamera = YMLCamera(scale: Float(scale), position: cameraController.camera.position)
                let animation = YMLCameraAnimations.smoothCameraAnimation(withDuration: 0.5)
                cameraController.setCameraWith(newCamera, animation: animation)
            }
        }

        let settings = SchemeSettingsAlert(title: "Scale", message: nil, actions: actions)
        settings.present(on: self, barButtonItem: barButtonItem, animated: true)
    }

    // MARK: Private - Camera

    private func updateCamera(fitting surface: YMLSurface) {
        let animation = YMLCameraAnimations.smoothCameraAnimation(withDuration: 1.0)

        schemeWindow.cameraController.setCameraFittingBoxWithTargetBox(surface.schemeCollection.bbox,
            animation: animation)
    }
    
    // MARK: Private - Camera Animation
    
    private var isCameraAnimating: Bool = false
    private let cameraAnimationPositions: [YMLPoint] = [
        YMLPoint(x: 64, y: 64), YMLPoint(x: 64, y: -64), YMLPoint(x: -64, y: -64)
    ]
    private let cameraAnimationCount: Int = 30
    private var cameraAnimationIndex: Int = 0
    
    private func toggleCameraAnimation() {
        isCameraAnimating = !isCameraAnimating
        cameraAnimationIndex = 0
        
        if isCameraAnimating {
            tryToAnimateCamera()
        }
    }
    
    private func tryToAnimateCamera() {
        let positionIndex = cameraAnimationIndex % cameraAnimationPositions.count
    
        guard isCameraAnimating || positionIndex != 0 else {
            return
        }
        
        cameraAnimationIndex += 1
        
        let cameraController = schemeWindow.cameraController
        let duration: TimeInterval = 5
        
        let animation = YMLAnimation(type: .linear, duration: Float(duration))
        let oldCamera = cameraController.camera
        let newPosition = cameraAnimationPositions[positionIndex]
        let newCamera = YMLCamera(scale: oldCamera.scale, position: newPosition)
        
        cameraController.setCameraWith(newCamera, animation: animation)
        
        
        if cameraAnimationIndex < cameraAnimationCount {
            DispatchQueue.main.asyncAfter(deadline: .now() + duration + 0.333) { [weak self] in
                self?.tryToAnimateCamera()
            }
        } else {
            toggleCameraAnimation()
        }
    }
    
    // MARK: Private - Focus Rect

    private struct SchemeFocusInsets {
        static let normal = UIEdgeInsets(top: 16, left: 16, bottom: 16, right: 16)
        static let custom = UIEdgeInsets(top: 100, left: 32, bottom: 16, right: 16)
    }
 
    private var safeAreaInsets: UIEdgeInsets {
        if #available(iOS 11.0, *) {
            return view.safeAreaInsets
        } else {
            return .zero
        }
    }
    
    private var schemeFocusInsets = SchemeFocusInsets.normal
    
    private let focusView = UIView()
    
    private func setupFocusRect() {
        let focusTint = UIColor(red: 0, green: 0.7, blue: 0.7, alpha: 0.1)
        schemeView.addSubview(focusView)
        focusView.isUserInteractionEnabled = false
        focusView.isHidden = true
        focusView.backgroundColor = focusTint
        focusView.layer.borderColor = focusTint.cgColor
        focusView.layer.borderWidth = 4
        focusView.layer.cornerRadius = 16
    }
    
    private func toggleCameraFocus() {
        schemeFocusInsets = (schemeFocusInsets == SchemeFocusInsets.normal)
            ? SchemeFocusInsets.custom
            : SchemeFocusInsets.normal
        updateFocusRect()
    }
    
    private func makeToggleFocusRectAction() -> SchemeSettingsAlert.Action {
        let focusTitle = schemeFocusInsets == SchemeFocusInsets.normal ? "custom" : "normal"
        let title = "Set " + focusTitle + " camera focus"
        return SchemeSettingsAlert.Action(title: title) { [weak self] in
            self?.toggleCameraFocus()
        }
    }
    
    private func makeFocusVisibilityAction() -> SchemeSettingsAlert.Action {
        let title = focusView.isHidden ? "Show focus" : "Hide focus"
        return SchemeSettingsAlert.Action(title: title) { [weak self] in
            guard let slf = self else { return }
            slf.focusView.isHidden = !slf.focusView.isHidden
        }
    }
    
    private func updateFocusRect() {
        let animation = YMLCameraAnimations.smoothCameraAnimation(withDuration: 0.3)
        
        var focusRect = schemeView.bounds.inset(by: safeAreaInsets)
        focusRect = focusRect.inset(by: routeCollection.contentInsets)
        focusRect = focusRect.inset(by: schemeFocusInsets)
        
        let cameraController = schemeWindow.cameraController
        
        focusView.frame = focusRect
        
        cameraController.setFocusRectWithFocus(YMLScreenRect(focusRect), animation: animation)
    }
}

extension MainViewController: YMLSchemeSearchPromptListener {
    
    func onSchemeDataUsageUpdate() {
        
    }

    func onSearchPromptNearestStationsUpdate(with nearestStations: [YMLNearestStation]) {
        print(nearestStations)
    }
    
}

extension MainViewController: YMLSurfaceListener {

    func didTapSurfaceObject(with object: YMLSurfaceObject) {
        guard object.isValid else { return }

        if let metadata = object.stationMetadata {
            selectStation(withId: metadata.stationId, serviceIds: metadata.serviceIds)
        }
    }
    
    private func selectStation(withId stationId: String, serviceIds: [String]) {
        let surfaceController: YMLSurfaceController = schemeWindow.surfaceController
    
        if surfaceController.routeSurface != nil {
            dismissRoute()
            return
        }
        
        if let selectedStation = selectedStationTrackers.enumerated().first(where: { $0.element.stationId == stationId }) {
            selectedStationTrackers.remove(at: selectedStation.offset)
            return
        }

        guard let mainSurface = surfaceController.mainSurface else { return }
        guard let infoService = infoService else { return }
        guard let style = surfaceController.style else { return }
        
        let service = infoService.services(withStationId: stationId).first { serviceIds.contains($0.id) }

        let placemarkInfo: StationTracker.PlacemarkInfo = selectedStationTrackers.isEmpty ?
            .makePinA(stationId: stationId, service: service, style: style) :
            .makePinB(stationId: stationId, service: service, style: style)

        if let tracker = mainSurface.trackStation(stationId: stationId, serviceId: service?.id, placemarkInfo: placemarkInfo) {
            selectedStationTrackers.append(tracker)
        }

        if selectedStationTrackers.count == 2 {
            guard let router = scheme?.makeRouter() else {
                selectedStationTrackers = []
                return
            }
            let fromStation = selectedStationTrackers[0].stationId
            let toStation = selectedStationTrackers[1].stationId

            let request = YMLRoutingRequest(fromStationId: fromStation, toStationId: toStation)
            routingResult = router.buildRoute(with: request)

            selectedStationTrackers = []
            showRoute(at: 0)
        }
    }

}

extension MainViewController: YMLCameraListener {

    func didChangeCamera(with source: YMLCameraChangingSource, finished: Bool) {
    }

}

extension MainViewController: YMLSurfaceControllerListener {
    
    func didUpdateMainSurface() {
        schemeWindow.surfaceController.mainSurface?.addListener(with: self)
    }
    
    func didUpdateRouteSurface() {
        
    }
    
    func didChangeZoomLevel(with zoomLevel: YMLZoomLevel) {
        
    }
    
}

extension MainViewController: YMLSchemeWindowTapListener {

    func didTap(with point: YMLScreenPoint) {
        selectedStationTrackers = []
    }

    func didLongTap(with point: YMLScreenPoint) {
        showAlert(title: "Long Tap", message: "x: \(point.x), y: \(point.y)")
    }

    func didDoubleTap(with point: YMLScreenPoint) {
    }

    private func showAlert(title: String, message: String) {
        let alert = UIAlertController(title: title, message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "Ok", style: .cancel))
        present(alert, animated: true)
    }

}

extension YMLServiceStation {
    var displayService: YMLService {
        return service.first!
    }
}

extension YMLRouteDetails {

    func allStations() -> [YMLServiceStation] {
        var ret: [YMLServiceStation] = []
        for s in sections {
            if let e = s.kind.enter {
                ret.append(e.enterStation)
            }
            else if let e = s.kind.exit {
                ret.append(e.exitStation)
            }
            else if let _ = s.kind.wait {
            }
            else if let _ = s.kind.transfer {
            }
            else if let r = s.kind.ride {
                ret += r.stationStops.map { return $0.station }
            }
        }
        
        var retRet: [YMLServiceStation] = []
        var usedIds = Set<String>();

        for s in ret {
            if !usedIds.contains(s.station.id) {
                retRet.append(s)
                usedIds.insert(s.station.id)
            }
        }
        return retRet
    }

}

extension YMLScreenRect {
    
    convenience init(_ cgRect: CGRect) {
        self.init(x: Float(cgRect.minX), y: Float(cgRect.minY),
            width: Float(cgRect.width), height: Float(cgRect.height))
    }
    
}

extension MainViewController: GLKViewControllerDelegate {

    func glkViewControllerUpdate(_ controller: GLKViewController) {
        fpsLabel.text = "\(Int(schemeWindow.fps().rounded()))"
    }
    
}
