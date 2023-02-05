//
//  Created by Timur Turaev on 19.11.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import Styler
import TestUtils
@testable import Tabbar

internal final class RouterTest: XCTestCase {
    override func setUp() {
        super.setUp()
        Styler.initializeSharedInstanceForDevelopment()
    }

    func testOpeningService() throws {
        let provider = UserContentControllerProvider()
        let userContentRender = TestUserContentRender()
        let barRender = TestBarRender()
        let supplementaryDataSource = SupplementaryDataSource()
        let router = Router(userContentControllerProvider: provider,
                            barServiceSupplementaryDataSource: supplementaryDataSource,
                            allAvailableServices: [.mail, .calendar, .telemost],
                            userContentRender: userContentRender,
                            barRender: barRender)
        router.open(service: .mail)
        XCTAssertEqual(userContentRender.mode, .master(hideNavigationBar: false))
        XCTAssertEqual(userContentRender.viewController?.title, "\(Service.mail)")
        weak var mailViewController: UIViewController? = userContentRender.viewController

        autoreleasepool {
            router.open(service: .calendar)
        }

        XCTAssertNil(mailViewController)
        XCTAssertEqual(userContentRender.mode, .master(hideNavigationBar: true))
        XCTAssertEqual(userContentRender.viewController?.title, "\(Service.calendar)")
    }

    func testSwitchingService() throws {
        let provider = UserContentControllerProvider()
        let userContentRender = TestUserContentRender()
        let barRender = TestBarRender()
        let supplementaryDataSource = SupplementaryDataSource()
        let router = Router(userContentControllerProvider: provider,
                            barServiceSupplementaryDataSource: supplementaryDataSource,
                            allAvailableServices: [.mail, .calendar, .telemost],
                            userContentRender: userContentRender,
                            barRender: barRender)
        router.open(service: .mail)
        XCTAssertEqual(userContentRender.viewController?.title, "\(Service.mail)")

        barRender.props?.buttons[1].onTap()
        XCTAssertEqual(userContentRender.viewController?.title, "\(Service.calendar)")

        barRender.props?.buttons[2].onTap()
        XCTAssertEqual(userContentRender.viewController?.title, "\(Service.telemost(serviceLocation: .tabbar))")

        barRender.props?.buttons[0].onTap()
        XCTAssertEqual(userContentRender.viewController?.title, "\(Service.mail)")
    }

    func testOpeningOpenedService() {
        let provider = UserContentControllerProvider()
        let userContentRender = TestUserContentRender()
        let barRender = TestBarRender()
        let supplementaryDataSource = SupplementaryDataSource()
        let router = Router(userContentControllerProvider: provider,
                            barServiceSupplementaryDataSource: supplementaryDataSource,
                            allAvailableServices: [.mail, .calendar, .telemost],
                            userContentRender: userContentRender,
                            barRender: barRender)

        router.open(service: .calendar)
        XCTAssertEqual(userContentRender.mode, .master(hideNavigationBar: true))
        XCTAssertEqual(userContentRender.viewController?.title, "\(Service.calendar)")

        router.open(service: .calendar)
        XCTAssertEqual(userContentRender.mode, .master(hideNavigationBar: true))
        XCTAssertEqual(userContentRender.viewController?.title, "\(Service.calendar)")

        router.open(service: .mail)
        XCTAssertEqual(userContentRender.mode, .master(hideNavigationBar: false))
        XCTAssertEqual(userContentRender.viewController?.title, "\(Service.mail)")
        weak var mailViewController = userContentRender.viewController

        router.open(service: .mail)
        XCTAssertEqual(userContentRender.mode, .master(hideNavigationBar: false))
        XCTAssertEqual(userContentRender.viewController?.title, "\(Service.mail)")
        XCTAssertTrue(mailViewController === userContentRender.viewController)
        XCTAssertEqual((userContentRender.viewController as! TestUIViewController).resetStateCounter, 1)
    }

    func testMultipleOpeningServices() {
        let provider = UserContentControllerProvider()
        let userContentRender = TestUserContentRender()
        let barRender = TestBarRender()
        let supplementaryDataSource = SupplementaryDataSource()
        let router = Router(userContentControllerProvider: provider,
                            barServiceSupplementaryDataSource: supplementaryDataSource,
                            allAvailableServices: [.mail, .calendar, .telemost],
                            userContentRender: userContentRender,
                            barRender: barRender)

        router.open(service: .mail)

        XCTAssertEqual(userContentRender.mode, .master(hideNavigationBar: false))
        XCTAssertEqual(userContentRender.viewController?.title, "\(Service.mail)")

        router.open(service: .calendar)
        XCTAssertEqual(userContentRender.mode, .master(hideNavigationBar: true))
        XCTAssertEqual(userContentRender.viewController?.title, "\(Service.calendar)")

        router.open(service: .telemost(serviceLocation: .tabbar))
        XCTAssertEqual(userContentRender.mode, .master(hideNavigationBar: true))
        XCTAssertEqual(userContentRender.viewController?.title, "\(Service.telemost(serviceLocation: .tabbar))")

        router.open(service: .mail360(forceBanner: nil))
        XCTAssertEqual(userContentRender.mode, .master(hideNavigationBar: true))
        XCTAssertEqual(userContentRender.viewController?.title, "\(Service.telemost(serviceLocation: .tabbar))")
    }

    func testDeallocating() throws {
        var barRender: TestBarRender? = TestBarRender()
        weak var weakBarRender = barRender

        var provider: UserContentControllerProvider? = UserContentControllerProvider()
        weak var weakProvider = provider

        var userContentRender: TestUserContentRender? = TestUserContentRender()
        weak var weakUserContentRender = userContentRender

        var supplementaryDataSource: SupplementaryDataSource? = SupplementaryDataSource()
        weak var weakSupplementaryDataSource = supplementaryDataSource

        var router: Router? = Router(userContentControllerProvider: provider!,
                                     barServiceSupplementaryDataSource: supplementaryDataSource!,
                                     allAvailableServices: [.mail, .calendar, .telemost],
                                     userContentRender: userContentRender!,
                                     barRender: barRender!)
        weak var weakRouter = router

        router!.open(service: .mail)

        weak var mailViewController: UIViewController? = userContentRender!.viewController

        XCTAssertNotNil(weakRouter)
        XCTAssertNotNil(weakProvider)
        XCTAssertNotNil(weakUserContentRender)
        XCTAssertNotNil(mailViewController)
        XCTAssertNotNil(weakBarRender)
        XCTAssertNotNil(weakSupplementaryDataSource)

        router = nil
        XCTAssertNil(router)
        XCTAssertNotNil(weakProvider)
        XCTAssertNotNil(weakUserContentRender)
        XCTAssertNotNil(mailViewController)
        XCTAssertNotNil(weakBarRender)
        XCTAssertNotNil(weakSupplementaryDataSource)

        provider = nil
        XCTAssertNil(weakProvider)
        XCTAssertNotNil(weakUserContentRender)
        XCTAssertNotNil(mailViewController)
        XCTAssertNotNil(weakBarRender)
        XCTAssertNotNil(weakSupplementaryDataSource)

        userContentRender = nil
        XCTAssertNil(weakUserContentRender)
        XCTAssertNil(mailViewController)
        XCTAssertNotNil(weakBarRender)
        XCTAssertNotNil(weakSupplementaryDataSource)

        barRender = nil
        XCTAssertNil(weakBarRender)
        XCTAssertNotNil(weakSupplementaryDataSource)

        supplementaryDataSource = nil
        XCTAssertNil(weakSupplementaryDataSource)
    }
}

private final class UserContentControllerProvider: UserContentControllerProviding {
    func makeViewController(for service: Service, completion: (UserContentViewController?) -> Void) {
        switch service {
        case .calendar, .docs, .telemost, .touch:
            let viewController = UIViewController()
            viewController.title = "\(service)"
            completion(viewController)
        case .mail:
            let viewController = TestUIViewController()
            viewController.title = "\(service)"
            completion(viewController)
        case .mail360:
            completion(nil)
        }
    }
}

private final class TestUIViewController: UIViewController {
    var resetStateCounter = 0

    override func yo_resetStateOnOpeningInSameTab() {
        self.resetStateCounter += 1
    }
}

private final class TestUserContentRender: UserContentRendering {
    var viewController: UIViewController?
    var mode: UserContentOpeningMode?

    func render(_ props: UserContentProps) {
        self.viewController = props.viewController
        self.mode = props.mode
    }
}

private final class TestBarRender: BarRendering {
    var props: BarProps?

    func render(_ props: BarProps) {
        self.props = props
    }
}

private final class SupplementaryDataSource: BarServiceSupplementaryDataSource {
    func supplementaryFor(barService: BarService) -> BarServiceSupplementary? {
        return nil
    }
}
