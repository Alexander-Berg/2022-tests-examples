//
//  Route.swift
//  MetroKitTestApp
//
//  Created by Ilya Lobanov on 10/11/2017.
//  Copyright © 2017 Yandex LLC. All rights reserved.
//

import Foundation


final class RoutingSession {

    private let controller: YMLSurfaceController
    private let route: YMLRoute
    private let surface: YMLSurface
    private let fromStationTracker: StationTracker?
    private let toStationTracker: StationTracker?

    init(controller: YMLSurfaceController, route: YMLRoute, surface: YMLSurface, fromStationTracker: StationTracker?,
        toStationTracker: StationTracker?)
    {
        self.controller = controller
        self.route = route
        self.surface = surface
        self.fromStationTracker = fromStationTracker
        self.toStationTracker = toStationTracker
    }
    
    convenience init?(controller: YMLSurfaceController, route rt: YMLRoute, infoService: YMLSchemeInfoService,
        surface: YMLSurface)
    {
        guard let style = controller.style else { return nil }
    
        let details = infoService.resolveDetails(with: rt)!
        let from = details.allStations().first!
        let to = details.allStations().last!
        
        let fromService = from.displayService
        let toService = to.displayService
    
        let fromInfo = StationTracker.PlacemarkInfo.makePinA(stationId: from.station.id, service: fromService,
            style: style)
        
        let toInfo = StationTracker.PlacemarkInfo.makePinB(stationId: to.station.id, service: toService,
            style: style)
        
        let fromTracker = surface.trackStation(stationId: from.station.id, serviceId: fromService.id, placemarkInfo: fromInfo)
        let toTracker = surface.trackStation(stationId: to.station.id, serviceId: toService.id, placemarkInfo: toInfo)
        
        self.init(controller: controller, route: rt, surface: surface, fromStationTracker: fromTracker,
            toStationTracker: toTracker)
    }
    
    deinit {
        if surface.isValid {
            controller.removeRoute()
        }
    }
    
    var isValid: Bool {
        get {
            return surface.isValid
        }
    }

}


extension YMLSurfaceController {

    func show(route: YMLRoute, infoService: YMLSchemeInfoService) -> RoutingSession? {
        self.setRouteWith(route)
        
        if let surf = self.routeSurface {
            return RoutingSession(controller: self, route: route, infoService: infoService, surface: surf)
        } else {
            return nil
        }
    }

}


extension YMLService {

    func color(withStyle style: String) -> UIColor? {
        return styles.serviceStyles[style]?.color
    }

}

