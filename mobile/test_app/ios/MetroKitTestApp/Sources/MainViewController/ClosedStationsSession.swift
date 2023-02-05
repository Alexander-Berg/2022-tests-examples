//
//  ClosedStationsSession.swift
//  MetroKitTestApp
//
//  Created by Ilya Lobanov on 23/03/2018.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import Foundation

final class ClosedStationsSession {
    let stationTrackers: [StationTracker]
    
    init(stationTrackers: [StationTracker]) {
        self.stationTrackers = stationTrackers
    }
}

extension YMLSurface {
    
    func showClosedStations() -> ClosedStationsSession {
        let objects = schemeCollection.objects
    
        let images: [YMLZoomLevel: StationTracker.PlacemarkInfo.ImageInfo] = [
            YMLZoomLevel(value: 2): .normal(image: .init(image: UIImage(named: "trouble-station-2x")!, anchor: YMLPoint(x: 0.5, y: 0.5))),
            YMLZoomLevel(value: 4): .normal(image: .init(image: UIImage(named: "trouble-station-4x")!, anchor: YMLPoint(x: 0.5, y: 0.5)))
        ]
    
        let placemarkInfo = StationTracker.PlacemarkInfo(style: .plane(images: images), z: 20.0)
    
        let stationTrackers: [StationTracker] = objects.compactMap {
            guard let stationId = $0.schemeMetadata?.impl.stationMetadata?.stationId, stationId.contains("r") else {
                return nil
            }
            
            return trackStation(stationId: stationId, serviceId: nil, placemarkInfo: placemarkInfo)
        }
        
        return ClosedStationsSession(stationTrackers: stationTrackers)
    }
    
}
