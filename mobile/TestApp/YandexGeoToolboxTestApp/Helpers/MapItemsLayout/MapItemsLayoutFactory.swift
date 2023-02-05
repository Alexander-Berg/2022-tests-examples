//
//  MapItemsLayoutFactory.swift
//  YandexTransport
//
//  Created by Aleksey Fedotov on 10.08.16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import Foundation

class MapItemsLayoutFactory {
    
    struct Identifiers {
        static let DefaultZoomIn = MapItemsLayoutZoomButtonsController.Identifiers.ZoomIn
        static let DefaultZoomOut = MapItemsLayoutZoomButtonsController.Identifiers.ZoomOut
        static let DefaultTraffic = MapItemsLayoutTrafficController.Identifiers.Traffic
    }
    
    struct Deps {
        func make() -> MapItemsLayoutFactory {
            return MapItemsLayoutFactory(self)
        }
    }
    
    private let deps: Deps
    
    private init(_ deps: Deps) {
        self.deps = deps
    }
    
    func makeMinimalisticLayout(
        zoomIn: MapItemsIdentifier = Identifiers.DefaultZoomIn,
        zoomOut: MapItemsIdentifier = Identifiers.DefaultZoomOut) -> MapItemsMinimalisticLayout
    {
        return MapItemsMinimalisticLayout.Deps(zoomInId: zoomIn, zoomOutId: zoomOut).make()
    }
    
}
