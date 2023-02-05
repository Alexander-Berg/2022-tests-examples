//
//  DownloadCacheStartupActionTests.swift
//  YandexMapsStartupActions-Unit-Tests
//
//  Created by Mikhail Kurenkov on 2/26/20.
//

import XCTest
import RxTest
import RxSwift
import YandexMapsCommonTypes
import YandexMapsMocks
import YandexMapsUtils
@testable import YandexMapsStartupActions

class DownloadCacheStartupActionTests: XCTestCase {
    
    typealias Deps = DownloadCacheStartupActionDepsMock
    
    func testAvailableWiaWifiAndHasOfflineCacheAndCanShow() {
        let deps = Deps(region: .russia, network: .wifi, hasOfflineCache: true, canShow: true)
        testDownloadCacheStartupAction(deps: deps, result: true, timeout: 6)
        testDownloadCacheStartupAction(deps: deps, result: true, timeout: 4)
    }
    
    func testNotAvailableWiaWifiAndHasOfflineCacheAndCanShow() {
        let deps = Deps(region: .uzbekistan, network: .wifi, hasOfflineCache: true, canShow: true)
        testDownloadCacheStartupAction(deps: deps, result: false, timeout: 6)
        testDownloadCacheStartupAction(deps: deps, result: false, timeout: 4)
    }

    func testAvailableWiaLTEAndHasOfflineCacheAndCanShow() {
        let deps = Deps(region: .russia, network: .mobileLTE, hasOfflineCache: true, canShow: true)
        testDownloadCacheStartupAction(deps: deps, result: false, timeout: 6)
        testDownloadCacheStartupAction(deps: deps, result: false, timeout: 4)
    }

    func testNotAvailableWiaLTEAndHasOfflineCacheAndCanShow() {
        let deps = Deps(region: .uzbekistan, network: .mobileLTE, hasOfflineCache: true, canShow: true)
        testDownloadCacheStartupAction(deps: deps, result: false, timeout: 6)
        testDownloadCacheStartupAction(deps: deps, result: false, timeout: 4)
    }

    func testAvailableWiaWifiAndNoOfflineCacheAndCanShow() {
        let deps = Deps(region: .russia, network: .wifi, hasOfflineCache: false, canShow: true)
        testDownloadCacheStartupAction(deps: deps, result: false, timeout: 6)
        testDownloadCacheStartupAction(deps: deps, result: nil, timeout: 4)
    }

    func testAvailableWithoutNetworkAndNoOfflineCacheAndCanShow() {
        let deps = Deps(region: .russia, network: .unknown, hasOfflineCache: false, canShow: true)
        testDownloadCacheStartupAction(deps: deps, result: false, timeout: 6)
        testDownloadCacheStartupAction(deps: deps, result: nil, timeout: 4)
    }

    func testAvailableWiaLTEAndNoOfflineCacheAndCanShow() {
        let deps = Deps(region: .russia, network: .mobileLTE, hasOfflineCache: false, canShow: true)
        testDownloadCacheStartupAction(deps: deps, result: false, timeout: 6)
        testDownloadCacheStartupAction(deps: deps, result: nil, timeout: 4)
    }

    func testNotAvailableWiaLTEAndNoOfflineCacheAndCanShow() {
        let deps = Deps(region: .uzbekistan, network: .mobileLTE, hasOfflineCache: false, canShow: true)
        testDownloadCacheStartupAction(deps: deps, result: false, timeout: 6)
        testDownloadCacheStartupAction(deps: deps, result: false, timeout: 4)
    }

    func testAvailableWiaWifiAndHasOfflineCacheAndNotCanShow() {
        let deps = Deps(region: .russia, network: .wifi, hasOfflineCache: true, canShow: false)
        testDownloadCacheStartupAction(deps: deps, result: false, timeout: 6)
        testDownloadCacheStartupAction(deps: deps, result: false, timeout: 4)
    }
    
    func testDownloadCacheStartupAction(deps: Deps, result: Bool?, timeout: TestTime) {
        let scheduler = TestScheduler(initialClock: 0)

        let action = DownloadCacheStartupAction(deps: deps, scheduler: scheduler)

        let observer = scheduler.start(created: 1, subscribed: 2, disposed: 2 + timeout) {
            return action.canBePerformed.asObservable()
        }
        
        if let result = result {
            XCTAssert(observer.events.count == 2)
            XCTAssert(observer.events.first?.value == .next(result))
            XCTAssert(observer.events.last?.value == .completed)
        } else {
            XCTAssert(observer.events.isEmpty)
        }
    }
    
}

class DownloadCacheStartupActionDepsMock: DownloadCacheStartupActionDeps {
    let startupActionsDebugPreferencesProvider: StartupActionsDebugPreferencesProvider
    let regionProvider: RegionProvider
    let reachabilityManager: ReachabilityManager
    let hasOfflineCacheForNearestRegionProvider: HasOfflineCacheForNearestRegionProvider
    let applicationInfoProvider: ApplicationInfoProvider
    let startupActionsRepository: StartupActionsRepository
    let downloadCacheNotificationRouter: DownloadCacheNotificationRouter

    init(region: Region, network: ReachabilityManager.NetworkTechnology, hasOfflineCache: Bool, canShow: Bool) {
        self.regionProvider = FakeRegionProvider(region: region)
        self.reachabilityManager = FakeReachabilityManager(networkTechnology: network, started: true)
        self.hasOfflineCacheForNearestRegionProvider
            = FakeHasOfflineCacheForNearestRegionProvider(hasOfflineCache: hasOfflineCache)
        self.startupActionsRepository = FakeStartupActionsRepository()
        self.applicationInfoProvider = FakeApplicationInfoProvider(launchType: .anotherLaunch)
        self.downloadCacheNotificationRouter = FakeDownloadCacheNotificationRouter(canShow: canShow)
        self.startupActionsDebugPreferencesProvider = FakeStartupActionsDebugPreferencesProvider(alwaysMigration: false)
    }
    
}
