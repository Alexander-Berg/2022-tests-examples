//
//  LoginStartupActionTests.swift
//  YandexMapsStartupActions-Unit-Tests
//
//  Created by Mikhail Kurenkov on 2/26/20.
//

import XCTest
import YandexMapsCommonTypes
import YandexMapsMocks
@testable import YandexMapsStartupActions

class LoginStartupActionTests: XCTestCase {
    
    func testNotAuthorizedAndHasAccounts() {
        let deps = LoginStartupActionDepsMock(hasAccounts: true)
        let action = LoginStartupAction(deps: deps, reason: .transportIntroFavorite)
        XCTAssert(action.canBePerformed)
    }
    
    func testNotAuthorizedAndNotAccounts() {
        let deps = LoginStartupActionDepsMock(hasAccounts: false)
        let action = LoginStartupAction(deps: deps, reason: .transportIntroFavorite)
        XCTAssert(!action.canBePerformed)
    }
    
    func testAuthorizedAndHasAccounts() {
        let deps = LoginStartupActionDepsMock(hasAccounts: true)
        deps.accountService.login(completion: { _, _ in })
        let action = LoginStartupAction(deps: deps, reason: .transportIntroFavorite)
        XCTAssert(!action.canBePerformed)
    }
    
    func testAuthorizedAndNotAccounts() {
        let deps = LoginStartupActionDepsMock(hasAccounts: false)
        deps.accountService.login(completion: { _, _ in })
        let action = LoginStartupAction(deps: deps, reason: .transportIntroFavorite)
        XCTAssert(!action.canBePerformed)
    }
    
}

class LoginStartupActionDepsMock: LoginStartupActionDeps {
    
    let accountService: AccountService
    let applicationInfoProvider: ApplicationInfoProvider
    let startupActionsRepository: StartupActionsRepository
    var loginStartupActionEventTracker: LoginStartupActionEventTracker

    init(hasAccounts: Bool) {
        self.accountService = FakeAccountService(hasAccounts: hasAccounts, isStaff: false)
        self.startupActionsRepository = FakeStartupActionsRepository()
        self.applicationInfoProvider = FakeApplicationInfoProvider(launchType: .anotherLaunch)
        self.loginStartupActionEventTracker = FakeLoginStartupActionEventTracker()
    }

}
