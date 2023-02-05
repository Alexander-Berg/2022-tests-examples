//
//  YMLTrackingObject+Utils.swift
//  MetroKitTestApp
//
//  Created by Ilya Lobanov on 03/10/2018.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import Foundation

extension YMLTrackingObject {

    var schemeMetadata: YMLSurfaceObjectSchemeMetadata? {
        return trackingMetadata.getItemOf(YMLSurfaceObjectSchemeMetadata.self) as? YMLSurfaceObjectSchemeMetadata
    }

    var stationMetadata: YMLSurfaceObjectStationMetadata? {
        return schemeMetadata?.impl.stationMetadata
    }

}
