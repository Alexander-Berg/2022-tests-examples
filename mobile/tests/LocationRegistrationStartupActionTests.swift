//
//  LocationRegistrationStartupActionTests.swift
//  YandexMapsStartupActions-Unit-Tests
//
//  Created by Mikhail Kurenkov on 2/26/20.
//

import XCTest
import YandexMapsCommonTypes
import YandexMapsMocks
@testable import YandexMapsStartupActions

class LocationRegistrationStartupActionTests: XCTestCase {
    
    func testNotDetermined() {
        let deps = LocationRegistrationStartupActionDepsMock(authorizationStatus: .notDetermined)
        let action = LocationRegistrationStartupAction(deps: deps)
        XCTAssert(action.canBePerformed)
    }
    
    func testAuthorizedAlways() {
        let deps = LocationRegistrationStartupActionDepsMock(authorizationStatus: .authorizedAlways)
        let action = LocationRegistrationStartupAction(deps: deps)
        XCTAssert(!action.canBePerformed)
    }
    
    func testAuthorizedWhenInUse() {
        let deps = LocationRegistrationStartupActionDepsMock(authorizationStatus: .authorizedWhenInUse)
        let action = LocationRegistrationStartupAction(deps: deps)
        XCTAssert(!action.canBePerformed)
    }
    
    func testDenied() {
        let deps = LocationRegistrationStartupActionDepsMock(authorizationStatus: .denied)
        let action = LocationRegistrationStartupAction(deps: deps)
        XCTAssert(!action.canBePerformed)
    }
    
    func testRestricted() {
        let deps = LocationRegistrationStartupActionDepsMock(authorizationStatus: .restricted)
        let action = LocationRegistrationStartupAction(deps: deps)
        XCTAssert(!action.canBePerformed)
    }
    
}

class LocationRegistrationStartupActionDepsMock: LocationRegistrationStartupActionDeps {
    
    let locationAuthorizationManager: LocationAuthorizationManager

    init(authorizationStatus: AuthorizationStatus) {
        self.locationAuthorizationManager
            = FakeLocationAuthorizationManager(authorizationStatus: authorizationStatus)
    }
    
}
