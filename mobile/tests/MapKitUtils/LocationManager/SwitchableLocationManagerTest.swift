//
//  SwitchableLocationManagerTest.swift
//  YandexGeoToolboxTestApp
//
//  Created by Iskander Yadgarov on 26.04.16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import XCTest
import YandexMapKit

class SwitchableLocationManagerTest: XCTestCase {
    
    class MockLocationManager: LocationManager {
        
        var location: YMKLocation? = nil
        var status: AuthorizationStatus = .authorizedAlways
        var configuration = LocationManagerConfiguration(desiredAccuracy: .best, distanceFilter: 0.0)
        private(set) var suspended: Bool = false
        
        // MARK: Location Manager methods
        
        func subscribe(_ locationListener: LocationManagerListener) {}
        func unsubscribe(_ listener: LocationManagerListener) {}
        func suspend() {
            suspended = true
        }
        func resume() {
            suspended = false
        }
        
        var lastReportedLocation: YMKLocation? {
            return location
        }
        
        var authorizationStatus: AuthorizationStatus {
            return status
        }
    }
    
    lazy var managers: [LocationManager] = {
        return [MockLocationManager(), MockLocationManager(), MockLocationManager()]
    }()

    var switchableLocationManager: SwitchableLocationManager!
    
    override func setUp() {
        super.setUp()
        
        switchableLocationManager = SwitchableLocationManager(managers)
    }
    
    func testThatCurrentLocationManagerNotResumedWhenSwitchableIsSuspended() {
        switchableLocationManager.suspend()
        
        managers[2].suspend()
        switchableLocationManager.currentIndex = 2
        
        if switchableLocationManager.current !== managers[2] {
            XCTFail("Location managers expected to be the same")
        }
        
        XCTAssert((managers[2] as! MockLocationManager).suspended)
        
        switchableLocationManager.resume()
        
        XCTAssert(!(managers[2] as! MockLocationManager).suspended)
    }
    
    func testThatAllLocationManagersSuspendedExceptCurrent() {
        switchableLocationManager.resume()
        switchableLocationManager.currentIndex = 0
        
        XCTAssert(!(managers[0] as! MockLocationManager).suspended)
        XCTAssert((managers[1] as! MockLocationManager).suspended)
        XCTAssert((managers[2] as! MockLocationManager).suspended)
        
        switchableLocationManager.currentIndex = 1
        
        XCTAssert((managers[0] as! MockLocationManager).suspended)
        XCTAssert(!(managers[1] as! MockLocationManager).suspended)
        XCTAssert((managers[2] as! MockLocationManager).suspended)
    }

}
