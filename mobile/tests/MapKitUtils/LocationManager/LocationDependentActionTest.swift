//
//  LocationDependentActionTest.swift
//  YandexGeoToolboxTestApp
//
//  Created by Iskander Yadgarov on 25.04.16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import XCTest
import YandexMapKit

class LocationDependentActionTest: XCTestCase {
    
    class TestLocationManager: LocationManager {
        
        private(set) var listeners: [LocationManagerListener] = []
        
        var location: YMKLocation? = nil
        var status: AuthorizationStatus = .authorizedAlways {
            didSet {
                listeners.forEach {
                    $0.onLocationStatusUpdate(status)
                }
            }
        }
        
        var configuration = LocationManagerConfiguration(desiredAccuracy: .best, distanceFilter: 0.0)
        
        // MARK: Location Manager methods
        
        func subscribe(_ locationListener: LocationManagerListener) {
            listeners.append(locationListener)
        }
        func unsubscribe(_ listener: LocationManagerListener) {
        }
        func suspend() {}
        func resume() {}
        
        var lastReportedLocation: YMKLocation? {
            return location
        }
        
        var authorizationStatus: AuthorizationStatus {
            return status
        }

    }
    
    fileprivate let locationManager = TestLocationManager()

    override func setUp() {
        super.setUp()
    }
    
    override func tearDown() {
        // Put teardown code here. This method is called after the invocation of each test method in the class.
        super.tearDown()
    }
    
    func testLocationDependentActionExecutedWhenStatusIsDenied() {
        locationManager.status = .denied
        let exp = expectation(description: "")
        
        locationManager.performWhenLocationIsAvailableOrNotAuthorized() { location, status in
            exp.fulfill()
            
            if status != .denied {
                XCTFail("Status is expected to be Denied")
            }
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testLocationDependentActionExecutedWhenStatusIsRestricted() {
        locationManager.status = .restricted
        let exp = expectation(description: "")
        
        locationManager.performWhenLocationIsAvailableOrNotAuthorized() { location, status in
            exp.fulfill()
            
            if status != .restricted {
                XCTFail("Status is expected to be Restricted")
            }
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testBlockExecutedWhenStatusChangedToDenied() {
        locationManager.status = .authorizedAlways
        let exp = expectation(description: "")
        
        locationManager.performWhenLocationIsAvailableOrNotAuthorized() { location, status in
            exp.fulfill()
            
            if status != .denied {
                XCTFail("Status is expected to be Denied")
            }
        }
        
        dispatch(after: 0.5) {
            self.locationManager.status = .denied
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)

    }
    
    func testBlockExecutedWhenStatusChangedToRestricted() {
        locationManager.status = .authorizedAlways
        let exp = expectation(description: "")
        
        locationManager.performWhenLocationIsAvailableOrNotAuthorized() { location, status in
            exp.fulfill()
            
            if status != .restricted {
                XCTFail("Status is expected to be Restricted")
            }
        }
        
        dispatch(after: 0.5) {
            self.locationManager.status = .restricted
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
        
    }

    func testPerformanceExample() {
        // This is an example of a performance test case.
        self.measure {
            // Put the code you want to measure the time of here.
        }
    }

}
