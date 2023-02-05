//
//  StartScreenBannerInteractorImplTests.swift
//  Pods
//
//  Created by Mikhail Kurenkov on 4/22/20.
//

import XCTest
import RxTest
import YandexMapsCommonTypes
import YandexMapsShutter
import YandexMapsMocks
import YandexMapsRx
import RxSwift
import RxCocoa

class StartScreenBannerInteractorImplTests: XCTestCase {

    // MARK: - Public Methods

    func testDisplayBanner() {
        let deps = makeDeps(notificationId: notificationId)
        let scheduler = TestScheduler(initialClock: 0)
        let interactor = StartScreenBannerInteractorImpl(deps: deps, tracker: tracker, scheduler: scheduler)

        interactor.onDisplayBanner()
        XCTAssert(interactor.banner.value != nil)
        
        interactor.dismissBanner()
        scheduler.advanceTo(3)
        XCTAssert(interactor.banner.value != nil)

        scheduler.advanceTo(4)
        XCTAssert(interactor.banner.value == nil)
    }
    
    func testPerformBannerAction() {
        let deps = makeDeps(notificationId: notificationId)
        let scheduler = TestScheduler(initialClock: 0)
        let interactor = StartScreenBannerInteractorImpl(deps: deps, tracker: tracker, scheduler: scheduler)

        interactor.onDisplayBanner()
        XCTAssert(interactor.banner.value != nil)

        interactor.performBannerAction()
        XCTAssert(interactor.banner.value == nil)
    }
    
    func testPerformBannerDone() {
        let deps = makeDeps(notificationId: notificationId)
        let scheduler = TestScheduler(initialClock: 0)
        let interactor = StartScreenBannerInteractorImpl(deps: deps, tracker: tracker, scheduler: scheduler)

        interactor.onDisplayBanner()
        XCTAssert(interactor.banner.value != nil)

        interactor.performBannerDone()
        XCTAssert(interactor.banner.value == nil)
    }
    
    func testWithoutNotification() {
        let deps = makeDeps(notificationId: nil)
        let scheduler = TestScheduler(initialClock: 0)
        let interactor = StartScreenBannerInteractorImpl(deps: deps, tracker: tracker, scheduler: scheduler)

        XCTAssert(interactor.banner.value == nil)
    }
    
    func makeDeps(notificationId: String?) -> FakeStartScreenBannerInteractorImplDeps {
        let notification = notificationId.flatMap {
            return StartScreenNotification(id: $0, description: $0, bannerUrlTemplate: nil, actionUrl: nil, kind: .discovery)
        }
        return FakeStartScreenBannerInteractorImplDeps(notification: notification)
    }

    // MARK: - Private Properties

    private let notificationId = "notificationId"
    private let tracker = FakeStartScreenBannerInteractorEventTracker()

}

class FakeStartScreenBannerInteractorImplDeps: StartScreenBannerInteractorImplDeps {
    
    // MARK: - Public Properties

    let urlImageLoader: URLImageLoader
    var startScreenNotificationProvider: StartScreenNotificationProvider { fakeNotificationProvider }
    let userActionsTracker: UserActionsTracker = FakeUserActionsTracker()
    let fakeNotificationProvider: FakeStartScreenNotificationProvider
    let shutterExperimentsProvider: ShutterExperimentsProvider
    
    
    // MARK: - Constructors

    init(notification: StartScreenNotification?) {
        self.urlImageLoader = FakeURLImageLoader()
        self.fakeNotificationProvider = FakeStartScreenNotificationProvider(notification: notification)
        self.shutterExperimentsProvider = FakeShutterExperimentsProvider()
    }
    
}

class FakeShutterExperimentsProvider: ShutterExperimentsProvider {
    func shutterReplaceDiscoveryTabName() -> String? { nil }
    func shutterReplaceDiscoveryTabUrl() -> String? { nil }
    func shutterTaxiOpenWebView() -> TaxiOpenInView? { nil }
    func shutterTaxiTabOnHomeScreen() -> Bool { false }
    func shutterHiddenSearchTabOnHomeScreen() -> Bool { false }
}

class FakeURLImageLoader: URLImageLoader {

    // MARK: - Constructors

    init(result: SingleEvent<UIImage> = .error(TestError())) {
        self.result = result
    }

    // MARK: - Public Methods

    func load(from url: URL) -> Single<UIImage> {
        return Single.create { observer in
            observer(self.result)
            return Disposables.create()
        }
    }

    // MARK: - Private Properties

    private let result: SingleEvent<UIImage>

    // MARK: - Private Nested

    struct TestError: Swift.Error { }

}

public class FakeStartScreenNotificationProvider: StartScreenNotificationProvider {

    // MARK: - Public Properties
    
    public var avalableNotifications: ReadonlyVariable<[StartScreenNotification]?>
    public var notificationToDisplay: ReadonlyVariable<StartScreenNotification?>
    
    // MARK: - Constructors
    
    public init(notification: StartScreenNotification? = nil) {
        self.avalableNotifications = ReadonlyVariable(value: [notification].compactMap { $0 })
        self.notificationToDisplay = ReadonlyVariable(value: notification)
    }
    
    // MARK: - Public Methods
        
    public func onHideNotification(with id: String) { }
    
}

class FakeStartScreenBannerInteractorEventTracker: StartScreenBannerInteractorEventTracker {
    func reportBannerAppear(with banner: StartScreenBanner) {
        
    }
    
    func reportBannerClick(with banner: StartScreenBanner) {
        
    }
    
    func reportBannerDone(with banner: StartScreenBanner) {
        
    }
}
