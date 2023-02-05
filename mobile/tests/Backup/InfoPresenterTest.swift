//
//  InfoPresenterTest.swift
//  BackupTests
//
//  Created by Aleksey Makhutin on 23.08.2021.
//

import Foundation

import XCTest
import Utils
import TestUtils
@testable import Backup

internal final class InfoPresenterTest: XCTestCase {
    func testStartAndRerender() {
        let render = MockRender()
        let router = MockRouter()
        let date = Date()
        let presenter = InfoPresenter(render: render, router: router, backupDate: date)
        presenter.start()

        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.cells.count, 3)

        render.props = nil
        presenter.rerender()
        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.cells.count, 3)
    }

    func testCheckFirstDetailCell() {
        let render = MockRender()
        let router = MockRouter()
        let date = Date()
        let presenter = InfoPresenter(render: render, router: router, backupDate: date)
        presenter.start()

        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.cells.count, 3)

        if case .detail(let props) = render.props?.cells[0].type {
            props.onTap()
            XCTAssertTrue(router.isDeleteOpen)
        } else {
            XCTFail("First cell should be detail")
        }

        XCTAssertNil(render.props?.cells[0].onTap)
    }

    func testSecondButtonCell() {
        let render = MockRender()
        let router = MockRouter()
        let date = Date()
        let presenter = InfoPresenter(render: render, router: router, backupDate: date)
        presenter.start()

        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.cells.count, 3)

        guard case .button = render.props?.cells[1].type else {
            XCTFail("Second cell should be button")
            return
        }

        XCTAssertNotNil(render.props?.cells[1].onTap)
        render.props?.cells[1].onTap?()
        XCTAssertTrue(router.isRestoringOpen)
    }

    func testThirdDescriptionCell() {
        let render = MockRender()
        let router = MockRouter()
        let date = Date()
        let presenter = InfoPresenter(render: render, router: router, backupDate: date)
        presenter.start()

        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.cells.count, 3)

        guard case .description = render.props?.cells[2].type else {
            XCTFail("Third cell should be description")
            return
        }

        XCTAssertNil(render.props?.cells[2].onTap)
    }

    func testDealocating() {
        let render = MockRender()
        let router = MockRouter()
        var presenter: InfoPresenter? = InfoPresenter(render: render, router: router, backupDate: Date())

        presenter?.start()
        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.cells.count, 3)

        render.props = nil
        presenter?.rerender()
        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.cells.count, 3)

        weak var weakPresenter = presenter
        XCTAssertNotNil(weakPresenter)
        presenter = nil
        XCTAssertNil(weakPresenter)
    }
}

private extension InfoPresenterTest {
    final class MockRender: InfoRendering {
        var props: InfoProps?

        func render(props: InfoProps) {
            self.props = props
        }
    }

    final class MockRouter: InfoRouting {
        var isDeleteOpen = false
        var isRestoringOpen = false

        func openDeleteBackup() {
            self.isDeleteOpen = true
        }

        func openRestoreBackup() {
            self.isRestoringOpen = true
        }
    }
}
