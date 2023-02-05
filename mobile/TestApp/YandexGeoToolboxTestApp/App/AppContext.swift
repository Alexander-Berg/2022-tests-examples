//
//  AppContext.swift
//  YandexGeoToolboxTestApp
//
//  Created by Konstantin Kiselev on 10/09/16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import Foundation
import YandexDataSync

protocol AppContext: class {
    
    var uuid: String { get }
    var deviceId: String { get }
    
    var mapKit: Computed<YMK.MapKit> { get }
    var accountManager: Computed<AccountManager> { get }
    var locationManager: Computed<LocationManager> { get }
    var databaseManager: YDSDatabaseManager { get }
    
    var mapItemsLayoutFactory: MapItemsLayoutFactory { get }
    var rootViewController: UIViewController { get }
    
    var masstransitServices: Deferred<MasstransitServices> { get }
}
