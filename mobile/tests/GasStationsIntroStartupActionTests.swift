//
//  GasStationsIntroStartupActionTests.swift
//  YandexMapsStartupActions-Unit-Tests
//
//  Created by Mikhail Kurenkov on 2/26/20.
//

import XCTest
import YandexMapsCommonTypes
import YandexMapsUtils
import YandexMapsMocks
import YandexMapsEventTracker
import YandexMapsStories
@testable import YandexMapsStartupActions

class GasStationsIntroStartupActionTests: XCTestCase {
    
    func testFirstLaunchInRussia() {
        let deps = GasStationsIntroStartupActionDepsMock(region: .russia, launchType: .firstLaunch)
        let action = GasStationsIntroStartupAction(deps: deps)
        XCTAssert(action.canBePerformed)
    }
    
    func testFirstLaunchInKazakhstan() {
        let deps = GasStationsIntroStartupActionDepsMock(region: .kazakhstan, launchType: .firstLaunch)
        let action = GasStationsIntroStartupAction(deps: deps)
        XCTAssert(!action.canBePerformed)
    }
     
     func testShownAnyIntroInCurrentVersionInRussia() {
         let deps = GasStationsIntroStartupActionDepsMock(region: .russia, launchType: .firstLaunch)

         deps.startupActionsRepository.markActionAsShown(FirstGroupLaunchConditions.anyFirstGroupElementKey,
                                                         in: deps.applicationInfoProvider.info.currentVersion)
         
         let action = GasStationsIntroStartupAction(deps: deps)
         XCTAssert(!action.canBePerformed)
     }
     
     func testShownSearchIntroInCurrentVersionInRussia() {
         let deps = GasStationsIntroStartupActionDepsMock(region: .russia, launchType: .anotherLaunch)
         
         deps.startupActionsRepository.markActionAsShown(StartupActionKeys.gasStations,
                                                         in: deps.applicationInfoProvider.info.currentVersion)
         
         let action = GasStationsIntroStartupAction(deps: deps)
         XCTAssert(!action.canBePerformed)
    }
     
    func testShownAnyIntroInPreviousVersionInUkraine() {
        let deps = GasStationsIntroStartupActionDepsMock(region: .ukraine, launchType: .update)
        
        deps.startupActionsRepository.markActionAsShown(FirstGroupLaunchConditions.anyFirstGroupElementKey,
                                                        in: "previous version")
        
        let action = GasStationsIntroStartupAction(deps: deps)
        XCTAssert(!action.canBePerformed)
    }
    
    func testShownAnyIntroInPreviousVersionInRussia() {
        let deps = GasStationsIntroStartupActionDepsMock(region: .russia, launchType: .update)
        
        deps.startupActionsRepository.markActionAsShown(FirstGroupLaunchConditions.anyFirstGroupElementKey,
                                                        in: "previous version")
        
        let action = GasStationsIntroStartupAction(deps: deps)
        XCTAssert(action.canBePerformed)
    }
     
     func testShownSearchIntroInPreviousVersionInRussia() {
         let deps = GasStationsIntroStartupActionDepsMock(region: .russia, launchType: .update)
         
         deps.startupActionsRepository.markActionAsShown(StartupActionKeys.gasStations,
                                                         in: "previous version")
         
         let action = GasStationsIntroStartupAction(deps: deps)
         XCTAssert(!action.canBePerformed)
     }
    
}

class GasStationsIntroStartupActionDepsMock: GasStationsIntroStartupActionDeps {

    let regionProvider: RegionProvider
    let startupActionsRepository: StartupActionsRepository
    let applicationInfoProvider: ApplicationInfoProvider
    let fullScreenIntroRouter: FullScreenIntroRouter
    let appAnalytics: GenaMetricsEventTracker
    let storiesScreenRouter: ShutterStoriesScreenRouter
    let gasStationIntroShowInVersionIgnoring: GasStationIntroShowInVersionIgnoring
    let reachabilityManager: ReachabilityManager
    let genericEventTracker: GenericEventTracker
    let startupActionsDebugPreferencesProvider: StartupActionsDebugPreferencesProvider

    init(region: Region, launchType: ApplicationInfo.LaunchType) {
        self.storiesScreenRouter = FakeShutterStoriesScreenRouter()
        self.regionProvider = FakeRegionProvider(region: region)
        self.startupActionsRepository = FakeStartupActionsRepository()
        self.applicationInfoProvider = FakeApplicationInfoProvider(launchType: launchType)
        self.fullScreenIntroRouter = FakeFullScreenIntroRouter()
        self.appAnalytics = GenaMetricsEventTracker(FakeGenericEventTracker())
        self.gasStationIntroShowInVersionIgnoring = FakeGasStationIntroShowInVersionIgnoring()
        self.reachabilityManager = FakeReachabilityManager(networkTechnology: .wifi, started: true)
        self.genericEventTracker = FakeGenericEventTracker()
        self.startupActionsDebugPreferencesProvider = FakeStartupActionsDebugPreferencesProvider()
    }
    
}


class FakeGasStationIntroShowInVersionIgnoring: GasStationIntroShowInVersionIgnoring {
    var ignoreShownInVersion: Bool { false }
}
