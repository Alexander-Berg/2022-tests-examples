//
//  Created by Aleksey Makhutin on 09.11.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import Utils
import Styler
import TestUtils
@testable import Tabbar

internal final class BarPresenterTest: XCTestCase {
    override func setUp() {
        super.setUp()
        Styler.initializeSharedInstanceForDevelopment()
    }

    func testChangingSelectedServiceOnTap() {
        let render = Renderer()
        let router = Router()
        let supplementaryDataSource = SupplementaryDataSource()
        let services = [BarService.mail, .calendar, .telemost, .mail360]
        let presenter = BarPresenter(router: router,
                                     barServiceSupplementaryDataSource: supplementaryDataSource,
                                     services: services,
                                     render: render)
        presenter.selectedService = .calendar
        self.validateSelected(index: 1, in: render.props)

        (0..<services.count - 1).forEach {
            render.props.buttons[$0].onTap()
            self.validateSelected(index: $0, in: render.props)
        }
        render.props.buttons[3].onTap()
        self.validateSelected(index: 2, in: render.props)

        render.props.buttons[2].onTap()
        self.validateSelected(index: 2, in: render.props)
        render.props.buttons[2].onTap()
        self.validateSelected(index: 2, in: render.props)
    }

    func testOpenigService() {
        let render = Renderer()
        let router = Router()
        let supplementaryDataSource = SupplementaryDataSource()
        let services = [BarService.mail, .calendar, .telemost, .mail360]
        let presenter = BarPresenter(router: router,
                                     barServiceSupplementaryDataSource: supplementaryDataSource,
                                     services: services,
                                     render: render)
        XCTAssertNil(router.openedService)
        presenter.selectedService = .calendar
        XCTAssertNil(router.openedService)

        (0..<services.count).forEach {
            render.props.buttons[$0].onTap()
            XCTAssertEqual(router.openedService, services[$0])
        }

        render.props.buttons[2].onTap()
        XCTAssertEqual(router.openedService, .telemost)

        render.props.buttons[2].onTap()
        XCTAssertEqual(router.openedService, .telemost)
    }

    func testRenderCurrentDate() {
        let render = Renderer()
        let router = Router()
        let dateProvider = MockDateProvider()
        let supplementaryDataSource = SupplementaryDataSource()
        let presenter = BarPresenter(router: router,
                                     barServiceSupplementaryDataSource: supplementaryDataSource,
                                     services: [.calendar],
                                     render: render,
                                     dateProvider: dateProvider)
        for testElem in ["Foo", "Bar", "Baz"] {
            dateProvider.dayOfMonth = testElem
            presenter.selectedService = .calendar

            if case .compound(let compound) = render.props.buttons.first?.icon {
                XCTAssertEqual(compound.text, testElem)
            } else {
                XCTFail("calendar must be contains compound icon")
            }
        }
    }

    func testDeallocating() {
        let render = Renderer()
        let router = Router()
        var supplementaryDataSource: SupplementaryDataSource? = SupplementaryDataSource()
        var presenter: BarPresenter? = BarPresenter(router: router,
                                                    barServiceSupplementaryDataSource: supplementaryDataSource!,
                                                    services: [.mail, .calendar],
                                                    render: render)
        presenter?.selectedService = .mail

        render.props.buttons[1].onTap()

        weak var weakPresenter = presenter
        weak var weakSupplementaryDataSource = supplementaryDataSource
        XCTAssertNotNil(weakPresenter)
        XCTAssertNotNil(weakSupplementaryDataSource)

        presenter = nil
        XCTAssertNil(weakPresenter)
        XCTAssertNotNil(weakSupplementaryDataSource)

        supplementaryDataSource = nil
        XCTAssertNil(weakSupplementaryDataSource)
    }
    
    private func validateSelected(index selectedIndex: Int, in props: BarProps) {
        props.buttons.enumerated().forEach { offset, prop in
            XCTAssertEqual(prop.isSelected, offset == selectedIndex)
        }
    }
}

private final class Renderer: BarRendering {
    var props: BarProps!

    func render(_ props: BarProps) {
        self.props = props
    }

    init() {}
}

private final class Router: BarRouting {
    var openedService: BarService!

    func openServiceFromBar(_ barService: BarService) {
        self.openedService = barService
    }
}

private final class SupplementaryDataSource: BarServiceSupplementaryDataSource {
    func supplementaryFor(barService: BarService) -> BarServiceSupplementary? {
        return nil
    }
}

private final class MockDateProvider: DateProviding {
    var dayOfMonth: String = ""
}
