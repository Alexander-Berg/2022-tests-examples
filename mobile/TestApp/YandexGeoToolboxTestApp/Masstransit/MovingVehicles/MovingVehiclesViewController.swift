//
//  MovingVehiclesViewController.swift
//  YandexGeoToolboxTestApp
//
//  Created by Konstantin Kiselev on 11/09/16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import UIKit

class MovingVehiclesViewController: UIViewController, MapItemsConfigurationOwner {
    
    struct Deps {
        private var appCtx: AppContext? = nil
        
        var mapKit: YMK.MapKit
        var mapItemsLayoutFactory: MapItemsLayoutFactory
        
        init(mapKit: YMK.MapKit, mapItemsLayoutFactory: MapItemsLayoutFactory) {
            self.mapKit = mapKit
            self.mapItemsLayoutFactory = mapItemsLayoutFactory
        }
        
        init(appCtx: AppContext) {
            self.init(mapKit: appCtx.mapKit.value, mapItemsLayoutFactory: appCtx.mapItemsLayoutFactory)
            self.appCtx = appCtx
        }
        
        func make(with impl: ImplType) -> MovingVehiclesViewController {
            return MovingVehiclesViewController(self, impl: impl)
        }
    }
    
    enum ImplType {
        case xml
        case mapkit
    }
    
    fileprivate let deps: Deps
    fileprivate var map: MapFacade
    fileprivate var vehicles: MapVehicles? = nil {
        didSet {
            if let ext = vehicles {
                let rendererFactory = DefaultMapVehiclesImageRendererFactory.instance
                ext.vehicleImagesRenderer = rendererFactory.makeMapVehiclesImageRenderer(style: .Common)
                ext.selectedVehicleZIndex = 30
                ext.addListener(vehiclesListener)
                map.addExtension(ext)
            }
        }
    }
    fileprivate var mapItemsContainer: TouchesPassView!
    fileprivate var mapItemsLayoutManager: MapItemsLayoutManager!
    fileprivate var mapItemsZoomController: MapItemsLayoutZoomButtonsController!

    var mapItemsConfiguration: MapItemsConfiguration {
        return MapItemsConfiguration(
            layout: deps.mapItemsLayoutFactory.makeMinimalisticLayout(), topLayoutGuide: .inset(64.0),
            bottomLayoutGuide: .inset(0.0))
    }
    fileprivate let vehiclesListener = MapVehiclesListenerImpl()
    fileprivate let impl: ImplType

    init(_ deps: Deps, impl: ImplType) {
        self.deps = deps
        self.impl = impl
        map = MapKitMapFacade(mapKit: deps.mapKit)

        super.init(nibName: nil, bundle: nil)

        mapItemsZoomController = MapItemsLayoutZoomButtonsController.Deps(
            map: map, zoomTarget: Computed { [weak self] in self?.map.cameraPosition.target }).make()
        
        switch impl {
        case .mapkit:
            self.title = "Moving vehicles (mapkit)"
        case .xml:
            self.title = "Moving vehicles (xml)"
        }
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setupViews() {
        view.backgroundColor = UIColor.white

        apply(map.targetView) { v in
            v.frame = view.bounds
            v.autoresizingMask = [.flexibleWidth, .flexibleHeight]
            view.addSubview(v)
        }
        
        mapItemsContainer = apply(TouchesPassView()) { v in
            v.frame = view.bounds
            v.autoresizingMask = [.flexibleWidth, .flexibleHeight]
            view.addSubview(v)
        }
        
        mapItemsLayoutManager = MapItemsLayoutManager.Deps(
            mapItemsContainer: mapItemsContainer, hideableView: mapItemsContainer, nonHideableView: mapItemsContainer,
            map: map).make()
        
        mapItemsLayoutManager.setView(
            mapItemsZoomController.zoomInButton, forIdentifier: MapItemsLayoutFactory.Identifiers.DefaultZoomIn)

        mapItemsLayoutManager.setView(
            mapItemsZoomController.zoomOutButton, forIdentifier: MapItemsLayoutFactory.Identifiers.DefaultZoomOut)
        
        mapItemsLayoutManager.ownByOwner(self)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupViews()
        
        let cp = YMK.CameraPosition(
            target: YMK.Point(lat: 55.686527, lon: 37.567117), zoom: 13, azimuth: 0, tilt: 0)
        
        map.move(cameraPosition: cp, animated: false, duration: 0.0, completion: nil)
        load(impl)
    }
    
    fileprivate func load(_ impl: ImplType) {
        vehiclesListener.onVehicleTap = { [weak self] event in
            var msg = ""
            msg += "[vehicle] = \(event.vehicle.vehicleId)\n"
            msg += "--------------------------\n"
            msg += "[name] = \(event.vehicle.transportName)\n"
            msg += "[type] = \(event.vehicle.transportType.toString())\n"
            msg += "[thread] = \(event.vehicle.threadId)\n"
            
            event.selected = true
            let alert = UIAlertController(title: "", message: msg, preferredStyle: .actionSheet)

            alert.addAction(UIAlertAction(title: "Dismiss", style: .cancel) { [weak alert] action in
                event.selected = false
                alert?.dismiss(animated: true, completion: nil)
            })
            self?.navigationController?.present(alert, animated: true, completion: nil)
        }
        
        switch impl {
        case .mapkit:
            vehicles = MapKitMapVehicles.Deps().make()
            assert(true)
            
        case .xml:
            DefaultMasstransitServicesFactory.instance.masstransitServices.upon { [weak self] services in
                guard let slf = self else { return }
                
                slf.vehicles = DefaultMapVehicles.Deps(
                    config: DefaultMapVehicles.Config(), forecastClient: services.vehiclesForecastClient)
                    .make()
                
            }
        }
    }
}
