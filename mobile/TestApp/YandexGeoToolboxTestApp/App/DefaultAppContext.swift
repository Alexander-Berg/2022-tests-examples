//
//  DefaultAppContext.swift
//  YandexGeoToolboxTestApp
//
//  Created by Konstantin Kiselev on 10/09/16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import Foundation
import UIKit
import YandexDataSync

class DefaultAppContext {

    struct Deps {
        var startupClientID: YXStartupClientIdentifier
        var mapKit: Computed<YMK.MapKit>
        var accountManager: Computed<AccountManager>
        var locationManager: Computed<LocationManager>
        var rootViewController: UIViewController
        var databaseManager: YDSDatabaseManager
        var masstransitServicesFactory: DefaultMasstransitServicesFactory
        
        func make() -> DefaultAppContext {
            return DefaultAppContext(self)
        }
    }
    
    fileprivate let deps: Deps
    fileprivate var appDelegate: AppDelegate { return UIApplication.shared.delegate as! AppDelegate }
    
    fileprivate let _mapItemsLayoutFactory: MapItemsLayoutFactory
    
    fileprivate init(_ deps: Deps) {
        self.deps = deps
        _mapItemsLayoutFactory = MapItemsLayoutFactory.Deps().make()
    }
    
}

extension DefaultAppContext: AppContext {
    
    var uuid: String { return deps.startupClientID.uuid }
    var deviceId: String { return deps.startupClientID.deviceID }
    
    var mapKit: Computed<YMK.MapKit> { return deps.mapKit }
    var accountManager: Computed<AccountManager> { return deps.accountManager }
    var locationManager: Computed<LocationManager> { return deps.locationManager }
    var databaseManager: YDSDatabaseManager { return deps.databaseManager }

    var mapItemsLayoutFactory: MapItemsLayoutFactory { return _mapItemsLayoutFactory }
    var rootViewController: UIViewController { return deps.rootViewController }

    var masstransitServices: Deferred<MasstransitServices> {
        return deps.masstransitServicesFactory.masstransitServices
    }
}



