//
//  RootPresenterTest.swift
//  BackupTests
//
//  Created by Aleksey Makhutin on 11.08.2021.
//

import Foundation

import XCTest
import Utils
import TestUtils
@testable import Backup

internal final class BackupPresenterTest: XCTestCase {
    func testStartAndRerender() {
        let enotPresenter = MockEnotPresenter()
        let managebackupPresenter = MockManageBackupPresenter()
        let render = MockRender()
        let presenter = RootPresenter(render: render,
                                      enotPresenter: enotPresenter,
                                      manageBackupPresenter: managebackupPresenter)

        presenter.start()
        XCTAssertNil(render.props)
        XCTAssertEqual(enotPresenter.startCount, 1)
        XCTAssertEqual(enotPresenter.rerenderCount, 0)
        XCTAssertEqual(managebackupPresenter.startCount, 1)
        XCTAssertEqual(managebackupPresenter.rerenderCount, 0)

        presenter.render(props: EnotProps.any)
        XCTAssertGreaterThan(render.props?.title.count ?? 0, 3)
        XCTAssertEqual(enotPresenter.startCount, 1)
        XCTAssertEqual(enotPresenter.rerenderCount, 0)
        XCTAssertEqual(managebackupPresenter.startCount, 1)
        XCTAssertEqual(managebackupPresenter.rerenderCount, 0)

        XCTAssertNotNil(render.props, "should build props from child presenter render")
        XCTAssertEqual(render.props?.sections.count, 1)
        XCTAssertEqual(render.props?.sections[0].cells.count, 1)
        XCTAssertEqual(render.props?.sections[0].cells[0].type, .enotMail(props: .any))
        render.props = nil

        presenter.render(props: ManageBackupProps.any)
        XCTAssertGreaterThan(render.props?.title.count ?? 0, 3)
        XCTAssertEqual(enotPresenter.startCount, 1)
        XCTAssertEqual(enotPresenter.rerenderCount, 0)
        XCTAssertEqual(managebackupPresenter.startCount, 1)
        XCTAssertEqual(managebackupPresenter.rerenderCount, 0)

        XCTAssertNotNil(render.props, "should build props from child presenter render")
        XCTAssertEqual(render.props?.sections.count, 2)
        XCTAssertEqual(render.props?.sections[0].cells.count, 1)
        XCTAssertEqual(render.props?.sections[0].cells[0].type, .enotMail(props: .any))
        XCTAssertEqual(render.props?.sections[1].cells.count, 0)
        render.props = nil

        presenter.rerender()
        XCTAssertEqual(enotPresenter.startCount, 1)
        XCTAssertEqual(enotPresenter.rerenderCount, 1)
        XCTAssertEqual(managebackupPresenter.startCount, 1)
        XCTAssertEqual(managebackupPresenter.rerenderCount, 1)

        XCTAssertNil(render.props, "should build props not from rerender")
    }

    func testCorrectConvertItemToCell() {
        func check(with item: ManageBackupProps.ItemType) {
            let enotPresenter = MockEnotPresenter()
            let managebackupPresenter = MockManageBackupPresenter()
            let render = MockRender()
            let presenter = RootPresenter(render: render,
                                          enotPresenter: enotPresenter,
                                          manageBackupPresenter: managebackupPresenter)

            presenter.start()
            XCTAssertNil(render.props)
            XCTAssertEqual(enotPresenter.startCount, 1)
            XCTAssertEqual(enotPresenter.rerenderCount, 0)
            XCTAssertEqual(managebackupPresenter.startCount, 1)
            XCTAssertEqual(managebackupPresenter.rerenderCount, 0)
            presenter.render(props: ManageBackupProps(items: [item]))

            switch item {
            case .loading:
                XCTAssertEqual(render.props?.sections[0].cells[0], RootCell(type: .loading, onTap: nil))
            case .button(let title, let onTap):
                XCTAssertEqual(render.props?.sections[0].cells[0], RootCell(type: .button(title: title), onTap: onTap))
            case let .text(text, withLoading):
                XCTAssertEqual(render.props?.sections[0].cells[0], RootCell(type: .text(text, withLoading: withLoading), onTap: nil))
            case .error(let props):
                XCTAssertEqual(render.props?.sections[0].cells[0], RootCell(type: .error(props: props), onTap: nil))
            }
        }

        check(with: .button(title: "button", onTap: Command(action: {})))
        check(with: .error(props: .any))
        check(with: .loading)
        check(with: .text("text", withLoading: false))
        check(with: .text("text", withLoading: true))
    }

    func testDealocating() {
        let enotPresenter = MockEnotPresenter()
        let managebackupPresenter = MockManageBackupPresenter()
        let render = MockRender()
        var presenter: RootPresenter? = RootPresenter(render: render,
                                                      enotPresenter: enotPresenter,
                                                      manageBackupPresenter: managebackupPresenter)
        
        presenter?.start()
        XCTAssertNil(render.props)
        XCTAssertEqual(enotPresenter.startCount, 1)
        XCTAssertEqual(enotPresenter.rerenderCount, 0)
        XCTAssertEqual(managebackupPresenter.startCount, 1)
        XCTAssertEqual(managebackupPresenter.rerenderCount, 0)

        presenter?.render(props: EnotProps.any)
        XCTAssertEqual(enotPresenter.startCount, 1)
        XCTAssertEqual(enotPresenter.rerenderCount, 0)
        XCTAssertEqual(managebackupPresenter.startCount, 1)
        XCTAssertEqual(managebackupPresenter.rerenderCount, 0)

        XCTAssertNotNil(render.props, "should build props from child presenter render")
        XCTAssertEqual(render.props?.sections.count, 1)
        XCTAssertEqual(render.props?.sections[0].cells.count, 1)
        XCTAssertEqual(render.props?.sections[0].cells[0].type, .enotMail(props: .any))
        render.props = nil

        presenter?.render(props: ManageBackupProps.any)
        XCTAssertEqual(enotPresenter.startCount, 1)
        XCTAssertEqual(enotPresenter.rerenderCount, 0)
        XCTAssertEqual(managebackupPresenter.startCount, 1)
        XCTAssertEqual(managebackupPresenter.rerenderCount, 0)

        XCTAssertNotNil(render.props, "should build props from child presenter render")
        XCTAssertEqual(render.props?.sections.count, 2)
        XCTAssertEqual(render.props?.sections[0].cells.count, 1)
        XCTAssertEqual(render.props?.sections[0].cells[0].type, .enotMail(props: .any))
        XCTAssertEqual(render.props?.sections[1].cells.count, 0)

        weak var weakPresenter = presenter
        XCTAssertNotNil(weakPresenter)
        presenter = nil
        XCTAssertNil(weakPresenter)
    }
}

private extension BackupPresenterTest {
    final class MockRender: RootRendering {
        var props: RootProps?
        func render(props: RootProps) {
            self.props = props
        }
    }

    final class MockManageBackupPresenter: ManageBackupPresenting {
        weak var render: ManageBackupRendering?

        var startCount = 0
        var rerenderCount = 0

        func start() {
            self.startCount += 1
        }

        func rerender() {
            self.rerenderCount += 1
        }
    }

    final class MockEnotPresenter: EnotPresenting {
        weak var render: EnotRendering?

        var startCount = 0
        var rerenderCount = 0

        func start() {
            self.startCount += 1
        }

        func rerender() {
            self.rerenderCount += 1
        }
    }
}

private extension EnotProps {
    static let any = EnotProps(isSwitchOn: false,
                               isSwitchEnabled: false,
                               isButtonHidden: false,
                               onSwitchChanged: CommandWith(action: { _ in }),
                               onButtonTap: Command(action: {}))
}

private extension ManageBackupProps {
    static let any = ManageBackupProps(items: [])
}

extension RootCellType: Equatable {
    public static func == (lhs: RootCellType, rhs: RootCellType) -> Bool {
        switch (lhs, rhs) {
        case (.enotMail, .enotMail):
            return true
        case (.text, .text):
            return true
        case (.button, .button):
            return true
        case (.loading, .loading):
            return true
        case (.error, .error):
            return true
        default:
            return false
        }
    }
}

extension RootCell: Equatable {
    public static func == (lhs: RootCell, rhs: RootCell) -> Bool {
        switch (lhs.onTap, rhs.onTap) {
        case (.none, .none), (.some, .some):
            return rhs.type == lhs.type
        default:
            return false
        }
    }
}
