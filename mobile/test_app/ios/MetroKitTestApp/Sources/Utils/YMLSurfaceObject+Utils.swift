//
//  YMLSurfaceObject+Utils.swift
//  MetroKitTestApp
//
//  Created by Ilya Lobanov on 03/10/2018.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import Foundation

extension YMLSurfaceObject {

    var stationMetadata: YMLSurfaceObjectStationMetadata? {
        let schemeMetadata = metadata.getItemOf(YMLSurfaceObjectSchemeMetadata.self) as? YMLSurfaceObjectSchemeMetadata
        return schemeMetadata?.impl.stationMetadata
    }
    
    var asSchemeSurfaceObject: YMLSchemeSurfaceObject? {
        return YMLSurfaceObjectConverter.asSchemeSurfaceObject(with: self)
    }

}
