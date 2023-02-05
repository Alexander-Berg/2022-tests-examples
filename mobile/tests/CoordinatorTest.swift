//
//  Created by Timur Turaev on 19.11.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import Styler
import UIKit
import TestUtils
@testable import Tabbar

internal final class CoordinatorTest: XCTestCase {
    override func setUpWithError() throws {
        try super.setUpWithError()

        Styler.initializeSharedInstanceForDevelopment()
    }

    func testServiceOpeningWithinSameLogin() throws {
        let login = "TestLogin"
        let dataSource = TestUserContentDataSource()
        let supplementaryDataSource = SupplementaryDataSource()
        let coordinator = Coordinator(userServiceDataSource: dataSource, barServiceSupplementaryDataSource: supplementaryDataSource)

        let tabbarViewController = TabbarViewController()
        coordinator.open(service: .mail, forLogin: login, onTabbar: tabbarViewController, allAvailableServices: [.mail, .calendar])

        var barProps = tabbarViewController.barProps!
        var contentViewController = tabbarViewController.contentViewController
        XCTAssertEqual(dataSource.createdViewControllers.count, 1)
        XCTAssertTrue(contentViewController === dataSource.createdViewControllers.last)
        XCTAssertEqual(contentViewController.title, "\(Service.mail)")
        XCTAssertEqual(barProps.buttons.count, 2)

        coordinator.open(service: .telemost(serviceLocation: .tabbar), forLogin: login, onTabbar: tabbarViewController, allAvailableServices: [.mail, .telemost, .mail360])
        barProps = tabbarViewController.barProps!
        contentViewController = tabbarViewController.contentViewController
        XCTAssertEqual(dataSource.createdViewControllers.count, 2)
        XCTAssertTrue(contentViewController === dataSource.createdViewControllers.last)
        XCTAssertEqual(contentViewController.title, "\(Service.telemost(serviceLocation: .tabbar))")
        XCTAssertEqual(barProps.buttons.count, 3)

        coordinator.open(service: .mail, forLogin: login, onTabbar: tabbarViewController, allAvailableServices: [.mail, .telemost, .mail360])
        barProps = tabbarViewController.barProps!
        contentViewController = tabbarViewController.contentViewController
        XCTAssertEqual(dataSource.createdViewControllers.count, 3)
        XCTAssertTrue(contentViewController === dataSource.createdViewControllers.last)
        XCTAssertEqual(contentViewController.title, "\(Service.mail)")
        XCTAssertEqual(barProps.buttons.count, 3)
    }

    func testServiceOpeningForDifferentLogins() throws {
        let login = "TestLogin"
        let dataSource = TestUserContentDataSource()
        let supplementaryDataSource = SupplementaryDataSource()
        let coordinator = Coordinator(userServiceDataSource: dataSource, barServiceSupplementaryDataSource: supplementaryDataSource)
        let tabbarViewController = TabbarViewController()

        coordinator.open(service: .mail, forLogin: login, onTabbar: tabbarViewController, allAvailableServices: [.mail, .calendar])
        var barProps = tabbarViewController.barProps!
        var contentViewController = tabbarViewController.contentViewController
        XCTAssertEqual(dataSource.createdViewControllers.count, 1)
        XCTAssertTrue(contentViewController === dataSource.createdViewControllers.last)
        XCTAssertEqual(contentViewController.title, "\(Service.mail)")
        XCTAssertEqual(barProps.buttons.count, 2)

        coordinator.open(service: .telemost(serviceLocation: .tabbar), forLogin: login + login, onTabbar: tabbarViewController, allAvailableServices: [.mail, .telemost, .mail360])
        barProps = tabbarViewController.barProps!
        contentViewController = tabbarViewController.contentViewController
        XCTAssertEqual(dataSource.createdViewControllers.count, 2)
        XCTAssertTrue(contentViewController === dataSource.createdViewControllers.last)
        XCTAssertEqual(contentViewController.title, "\(Service.telemost(serviceLocation: .tabbar))")
        XCTAssertEqual(barProps.buttons.count, 3)

        coordinator.open(service: .mail, forLogin: login + login, onTabbar: tabbarViewController, allAvailableServices: [.mail, .telemost, .mail360])
        barProps = tabbarViewController.barProps!
        contentViewController = tabbarViewController.contentViewController
        XCTAssertEqual(dataSource.createdViewControllers.count, 3)
        XCTAssertTrue(contentViewController === dataSource.createdViewControllers.last)
        XCTAssertEqual(contentViewController.title, "\(Service.mail)")
        XCTAssertEqual(barProps.buttons.count, 3)
    }

    func testOpeningServiceFromBar() {
        let login = "TestLogin"
        let dataSource = TestUserContentDataSource()
        let supplementaryDataSource = SupplementaryDataSource()
        let coordinator = Coordinator(userServiceDataSource: dataSource, barServiceSupplementaryDataSource: supplementaryDataSource)
        let tabbarViewController = TabbarViewController()

        coordinator.open(service: .mail, forLogin: login, onTabbar: tabbarViewController, allAvailableServices: [.mail, .calendar, .telemost])

        XCTAssertEqual(dataSource.createdViewControllers.count, 1)
        XCTAssertEqual(tabbarViewController.contentViewController.title, "\(Service.mail)")

        tabbarViewController.barProps?.buttons.dropFirst().first?.onTap()
        XCTAssertEqual(dataSource.createdViewControllers.count, 2)
        XCTAssertEqual(tabbarViewController.contentViewController.title, "\(Service.calendar)")

        tabbarViewController.barProps?.buttons.first?.onTap()
        XCTAssertEqual(dataSource.createdViewControllers.count, 2)
        XCTAssertEqual(tabbarViewController.contentViewController.title, "\(Service.mail)")
    }

    func testSwitchingService() {
        let login = "TestLogin"
        let dataSource = TestUserContentDataSource()
        let supplementaryDataSource = SupplementaryDataSource()
        let coordinator = Coordinator(userServiceDataSource: dataSource, barServiceSupplementaryDataSource: supplementaryDataSource)
        let tabbarViewController = TabbarViewController()

        coordinator.open(service: .mail, forLogin: login, onTabbar: tabbarViewController, allAvailableServices: [.mail, .calendar, .telemost])

        XCTAssertEqual(dataSource.createdViewControllers.count, 1)
        XCTAssertEqual(tabbarViewController.contentViewController.title, "\(Service.mail)")

        coordinator.switch(to: .calendar)
        XCTAssertEqual(dataSource.createdViewControllers.count, 2)
        XCTAssertEqual(tabbarViewController.contentViewController.title, "\(Service.calendar)")

        coordinator.switch(to: .mail)
        XCTAssertEqual(dataSource.createdViewControllers.count, 2)
        XCTAssertEqual(tabbarViewController.contentViewController.title, "\(Service.mail)")
    }

    func testSwitchingToTouch() {
        let login = "TestLogin"
        let dataSource = TestUserContentDataSource()
        let supplementaryDataSource = SupplementaryDataSource()
        let coordinator = Coordinator(userServiceDataSource: dataSource, barServiceSupplementaryDataSource: supplementaryDataSource)
        let tabbarViewController = TabbarViewController()

        coordinator.open(service: .mail, forLogin: login, onTabbar: tabbarViewController, allAvailableServices: [.mail, .calendar, .telemost, .mail360])

        dataSource.touchViewController = TestContentViewController(service: .touch(.disk))
        coordinator.switch(to: .touch(.disk))

        XCTAssertEqual(dataSource.createdViewControllers.count, 1) // mail
        XCTAssertEqual(tabbarViewController.contentViewController.title, "\(Service.touch(.disk))")
        XCTAssertTrue(tabbarViewController.barProps!.buttons[3].isSelected)
        XCTAssertFalse(tabbarViewController.barProps!.buttons[0].isSelected)

        coordinator.switch(to: .mail)
        XCTAssertEqual(dataSource.createdViewControllers.count, 1) // still mail
        XCTAssertEqual(tabbarViewController.contentViewController.title, "\(Service.mail)")
    }
    
    func testShouldSwitchToMail360WhenOpenTelemostFromMail360() {
        let login = "TestLogin"
        let dataSource = TestUserContentDataSource()
        let supplementaryDataSource = SupplementaryDataSource()
        let coordinator = Coordinator(userServiceDataSource: dataSource, barServiceSupplementaryDataSource: supplementaryDataSource)
        let tabbarViewController = TabbarViewController()

        coordinator.open(service: .mail, forLogin: login, onTabbar: tabbarViewController, allAvailableServices: [.mail, .calendar, .mail360])
        XCTAssertTrue(tabbarViewController.barProps!.buttons[0].isSelected)
        
        coordinator.switch(to: .telemost(serviceLocation: .mail360))
        XCTAssertTrue(tabbarViewController.barProps!.buttons[2].isSelected)
        XCTAssertFalse(tabbarViewController.barProps!.buttons[0].isSelected)
    }

    func testDeallocating() throws {
        let login = "TestLogin"
        var supplementaryDataSource: SupplementaryDataSource? = SupplementaryDataSource()
        weak var weakSupplementaryDataSource = supplementaryDataSource

        var dataSource: TestUserContentDataSource? = .init(saveViewControllers: false)
        weak var weakDataSource = dataSource

        var coordinator: Coordinator? = Coordinator(userServiceDataSource: dataSource!, barServiceSupplementaryDataSource: supplementaryDataSource!)
        weak var weakCoordinator = coordinator

        let tabbarViewController = TabbarViewController()
        coordinator!.open(service: .mail, forLogin: login, onTabbar: tabbarViewController, allAvailableServices: [.mail, .calendar])

        XCTAssertNotNil(coordinator)
        XCTAssertNotNil(dataSource)
        XCTAssertNotNil(supplementaryDataSource)

        coordinator = nil
        XCTAssertNil(weakCoordinator)
        XCTAssertNotNil(dataSource)
        XCTAssertNotNil(supplementaryDataSource)

        dataSource = nil
        XCTAssertNil(weakDataSource)

        supplementaryDataSource = nil
        XCTAssertNil(weakSupplementaryDataSource)
    }
}

private final class TestUserContentDataSource: UserServiceDataSource {
    private let saveViewControllers: Bool
    var createdViewControllers: [UserContentViewController] = .empty
    var touchViewController: UIViewController?

    init(saveViewControllers: Bool = true) {
        self.saveViewControllers = saveViewControllers
    }

    func makeViewController(for service: Service, completion: (UserContentViewController?) -> Void) {
        guard [Service.mail, .calendar, .telemost(serviceLocation: .tabbar), .mail360(forceBanner: nil)].contains(service) else {
            completion(self.touchViewController)
            return
        }

        let viewController = TestContentViewController(service: service)
        if self.saveViewControllers {
            self.createdViewControllers.append(viewController)
        }
        completion(viewController)
    }
}

private final class TestContentViewController: UIViewController {
    init(service: Service) {
        super.init(nibName: nil, bundle: nil)
        self.title = "\(service)"
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

private final class SupplementaryDataSource: BarServiceSupplementaryDataSource {
    func supplementaryFor(barService: BarService) -> BarServiceSupplementary? {
        return nil
    }
}
