//
//  CreatePresenterTest.swift
//  BackupTests
//
//  Created by Aleksey Makhutin on 25.08.2021.
//

import Foundation

import XCTest
import Utils
import TestUtils
import NetworkLayer
@testable import Backup

internal final class CreatePresenterTest: XCTestCase {
    func testStartAndRerender() {
        let render = MockRender()
        let router = MockRouter()
        let dataSource = MockDataSource()
        let presenter = CreatePresenter(render: render,
                                        dataSource: dataSource,
                                        router: router,
                                        overwrite: false)
        presenter.start()
        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.state, .loading)
        XCTAssertEqual(render.props?.controlProps.buttonState, .disabled)
        XCTAssertEqual(render.props?.controlProps.state, .normal)
        XCTAssertEqual(render.props?.title, "backup.title".localizedString())
        XCTAssertEqual(render.props?.controlProps.overwrite, false)

        render.props = nil
        presenter.rerender()
        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.state, .loading)
        XCTAssertEqual(render.props?.controlProps.buttonState, .disabled)
        XCTAssertEqual(render.props?.controlProps.state, .normal)
        XCTAssertEqual(render.props?.title, "backup.title".localizedString())
        XCTAssertEqual(render.props?.controlProps.overwrite, false)
    }

    func testStartAndRerenderWithOverwrite() {
        let render = MockRender()
        let router = MockRouter()
        let dataSource = MockDataSource()
        let presenter = CreatePresenter(render: render,
                                        dataSource: dataSource,
                                        router: router,
                                        overwrite: true)
        presenter.start()
        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.state, .loading)
        XCTAssertEqual(render.props?.controlProps.buttonState, .disabled)
        XCTAssertEqual(render.props?.controlProps.state, .normal)
        XCTAssertEqual(render.props?.title, "backup.title.overwrite".localizedString())
        XCTAssertEqual(render.props?.controlProps.overwrite, true)

        render.props = nil
        presenter.rerender()
        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.state, .loading)
        XCTAssertEqual(render.props?.controlProps.buttonState, .disabled)
        XCTAssertEqual(render.props?.controlProps.state, .normal)
        XCTAssertEqual(render.props?.title, "backup.title.overwrite".localizedString())
        XCTAssertEqual(render.props?.controlProps.overwrite, true)
    }

    func testFinishLoadingWithError() {
        let render = MockRender()
        let router = MockRouter()
        let dataSource = MockDataSource()
        let presenter = CreatePresenter(render: render,
                                        dataSource: dataSource,
                                        router: router,
                                        overwrite: false)
        presenter.start()
        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.state, .loading)
        XCTAssertEqual(render.props?.controlProps.buttonState, .disabled)

        dataSource.finishLoading(result: .failure(TestError.someError))
        XCTAssertEqual(render.props?.controlProps.buttonState, .disabled)
        XCTAssertEqual(render.props?.controlProps.state, .normal)

        if case .error(let props) = render.props?.state {
            props.onTap()
        } else {
            XCTFail("props should be exist")
        }

        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.state, .loading)
        XCTAssertEqual(render.props?.controlProps.buttonState, .disabled)

        dataSource.finishLoading(result: .failure(TestError.networkError))
        XCTAssertEqual(render.props?.controlProps.buttonState, .disabled)
        XCTAssertEqual(render.props?.controlProps.state, .normal)

        if case .error(let props) = render.props?.state {
            props.onTap()
        } else {
            XCTFail("props should be exist")
        }

        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.state, .loading)
        XCTAssertEqual(render.props?.controlProps.buttonState, .disabled)
    }

    func testCorrectLoadAndMappingBackupFolder() {
        func check(groups: [BackupFoldersGroup], expectProps props: [CreateCellProps]) {
            let render = MockRender()
            let router = MockRouter()
            let dataSource = MockDataSource()
            let presenter = CreatePresenter(render: render,
                                            dataSource: dataSource,
                                            router: router,
                                            overwrite: false)
            presenter.start()
            XCTAssertNotNil(render.props)
            XCTAssertEqual(render.props?.state, .loading)
            XCTAssertEqual(render.props?.controlProps.buttonState, .disabled)

            dataSource.finishLoading(result: .success(groups))
            XCTAssertEqual(render.props?.state, .normal)
            XCTAssertEqual(render.props?.controlProps.buttonState, .normal)
            XCTAssertEqual(render.props?.controlProps.state, .normal)

            XCTAssertEqual(props, render.props?.cell)

            XCTAssertEqual(render.props?.controlProps.detailFirstPart, "5 801", "should correct count in control panel")
        }

        check(groups: self.groups, expectProps: self.expectProps)
    }

    func testRecountingMailWhenZeroSelectedFolder() {
        let render = MockRender()
        let router = MockRouter()
        let dataSource = MockDataSource()
        let presenter = CreatePresenter(render: render,
                                        dataSource: dataSource,
                                        router: router,
                                        overwrite: false)
        presenter.start()
        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.state, .loading)
        XCTAssertEqual(render.props?.controlProps.buttonState, .disabled)

        let groupsWithoutSelectedFolder = self.groups.map { BackupFoldersGroup(folders: $0.folders.filter { !$0.isSelected }) }

        dataSource.finishLoading(result: .success(groupsWithoutSelectedFolder))
        XCTAssertEqual(render.props?.state, .normal)
        XCTAssertEqual(render.props?.controlProps.buttonState, .disabled, "when none selected folder")
        XCTAssertEqual(render.props?.controlProps.state, .normal)

        XCTAssertEqual(render.props?.controlProps.detailFirstPart, "", "should correct count in control panel")
        XCTAssertEqual(render.props?.controlProps.detailSecondPart, "0", "should correct count in control panel")

        render.props?.cell[0].onTap()

        XCTAssertEqual(render.props?.controlProps.detailFirstPart, "0", "should correct count in control panel")
        XCTAssertEqual(render.props?.controlProps.buttonState, .normal, "That one folder is selected")

        render.props?.cell[3].onTap()

        XCTAssertEqual(render.props?.controlProps.detailFirstPart, "3 330 003", "should correct count in control panel")
        XCTAssertEqual(render.props?.controlProps.buttonState, .disabled, "That mails count is greather than 100000")

        render.props?.controlProps.onTap()
        XCTAssertEqual(dataSource.fids, [1, 6], "Should send correct fids to dataSource")
    }

    func testRecountingMailWhenTapOnCellAndSendNeededFidToDataSource() {
        let render = MockRender()
        let router = MockRouter()
        let dataSource = MockDataSource()
        let presenter = CreatePresenter(render: render,
                                        dataSource: dataSource,
                                        router: router,
                                        overwrite: false)
        presenter.start()
        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.state, .loading)
        XCTAssertEqual(render.props?.controlProps.buttonState, .disabled)

        dataSource.finishLoading(result: .success(self.groups))
        XCTAssertEqual(render.props?.state, .normal)
        XCTAssertEqual(render.props?.controlProps.buttonState, .normal)
        XCTAssertEqual(render.props?.controlProps.state, .normal)

        XCTAssertEqual(render.props?.cell, self.expectProps)

        XCTAssertEqual(render.props?.controlProps.detailFirstPart, "5 801", "should correct count in control panel")
        XCTAssertEqual(render.props?.controlProps.buttonState, .normal, "That mails count is less than 100000")

        render.props?.cell[0].onTap()
        render.props?.cell[5].onTap()
        render.props?.cell[6].onTap()

        XCTAssertEqual(render.props?.controlProps.detailFirstPart, "3 334 570", "should correct count in control panel")
        XCTAssertEqual(render.props?.controlProps.buttonState, .disabled, "That mails count is greather than 100000")

        render.props?.controlProps.onTap()
        XCTAssertEqual(dataSource.fids, [2, 6, 7], "Should send correct fids to dataSource")
    }

    func testCreatingBackup() {
        func check(result: Result<Void>) {
            let render = MockRender()
            let router = MockRouter()
            let dataSource = MockDataSource()
            let delegate = MockDelegate()
            let presenter = CreatePresenter(render: render,
                                            dataSource: dataSource,
                                            router: router,
                                            overwrite: false)
            presenter.delegate = delegate
            presenter.start()
            XCTAssertNotNil(render.props)
            XCTAssertEqual(render.props?.state, .loading)
            XCTAssertEqual(render.props?.controlProps.buttonState, .disabled)

            dataSource.finishLoading(result: .success(self.groups))
            XCTAssertEqual(render.props?.state, .normal)
            XCTAssertEqual(render.props?.controlProps.buttonState, .normal)
            XCTAssertEqual(render.props?.controlProps.state, .normal)

            XCTAssertEqual(render.props?.cell, self.expectProps)

            XCTAssertEqual(render.props?.controlProps.detailFirstPart, "5 801", "should correct count in control panel")

            render.props?.controlProps.onTap()
            XCTAssertEqual(render.props?.controlProps.buttonState, .loading)
            XCTAssertEqual(dataSource.fids, [0, 2, 5, 7], "Should send correct fids to dataSource")

            dataSource.finishCreating(result: result)

            result.onValue { _ in
                XCTAssertTrue(delegate.isCreate)
                XCTAssertNil(router.error)
            }

            result.onError { error in
                XCTAssertEqual(router.error, error.yo_isNetworkError ? .network : .createBackup)
                XCTAssertFalse(delegate.isCreate)
                XCTAssertEqual(render.props?.controlProps.buttonState, .normal)
            }
        }

        check(result: .success(()))
        check(result: .failure(TestError.someError))
        check(result: .failure(TestError.networkError))
    }

    private var groups: [BackupFoldersGroup] {
        return [
            BackupFoldersGroup(folders: [
                BackupFolder(id: 0, name: "zero", messagesCount: 123, indent: 0, isSelected: true)
            ]),
            BackupFoldersGroup(folders: [
                BackupFolder(id: 1, name: "first", messagesCount: 0, indent: 0, isSelected: false)
            ]),
            BackupFoldersGroup(folders: [
                BackupFolder(id: 2, name: "second", messagesCount: 123, indent: 0, isSelected: true),
                BackupFolder(id: 3, name: "secondChild", messagesCount: 321, indent: 1, isSelected: false)
            ]),
            BackupFoldersGroup(folders: [
                BackupFolder(id: 4, name: "third", messagesCount: 123, indent: 0, isSelected: false),
                BackupFolder(id: 5, name: "thirdChild", messagesCount: 1111, indent: 1, isSelected: true),
                BackupFolder(id: 6, name: "thirdChildChild", messagesCount: 3_330_003, indent: 2, isSelected: false),
                BackupFolder(id: 7, name: "thirdChildChild", messagesCount: 4444, indent: 2, isSelected: true),
                BackupFolder(id: 8, name: "thirdChildChildChild", messagesCount: 4444, indent: 3, isSelected: false),
                BackupFolder(id: 9, name: "thirdChildChildChildChild", messagesCount: 4444, indent: 4, isSelected: false)
            ])
        ]
    }

    private var expectProps: [CreateCellProps] {
        return [
            CreateCellProps(title: "zero", detail: "123", isSelected: true, type: .alone),

            CreateCellProps(title: "first", detail: "0", isSelected: false, type: .alone),

            CreateCellProps(title: "second", detail: "123", isSelected: true, type: .parent),
            CreateCellProps(title: "secondChild", detail: "321", isSelected: false, type: .lastChild(indent: 1)),

            CreateCellProps(title: "third", detail: "123", isSelected: false, type: .parent),
            CreateCellProps(title: "thirdChild", detail: "1 111", isSelected: true, type: .child(indent: 1)),
            CreateCellProps(title: "thirdChildChild", detail: "3 330 003", isSelected: false, type: .child(indent: 2)),
            CreateCellProps(title: "thirdChildChild", detail: "4 444", isSelected: true, type: .child(indent: 2)),
            CreateCellProps(title: "thirdChildChildChild", detail: "4 444", isSelected: false, type: .child(indent: 3)),
            CreateCellProps(title: "thirdChildChildChildChild", detail: "4 444", isSelected: false, type: .lastChild(indent: 4))
        ]
    }
}

private extension CreatePresenterTest {
    final class MockRender: CreateRendering {
        var props: CreateProps?

        func render(props: CreateProps) {
            self.props = props
        }
    }

    final class MockRouter: CreateRouting {
        var error: Router.Error?

        func showError(_ error: Router.Error) {
            self.error = error
        }
    }

    final class MockDataSource: CreateDataSource {
        var loadCompletion: ((Result<[BackupFoldersGroup]>) -> Void)?
        var createCompletion: ((Result<Void>) -> Void)?
        var fids = [YOIDType]()

        func finishLoading(result: Result<[BackupFoldersGroup]>) {
            self.loadCompletion?(result)
        }

        func finishCreating(result: Result<Void>) {
            self.createCompletion?(result)
        }

        func loadSelectedBackupFolder(completion: @escaping (Result<[BackupFoldersGroup]>) -> Void) {
            self.loadCompletion = completion
        }

        func createBackup(fids: [YOIDType], completion: @escaping (Result<Void>) -> Void) {
            self.fids = fids
            self.createCompletion = completion
        }
    }

    final class MockDelegate: CreatePresenterDelegate {
        var isCreate = false
        
        func createPresenterDidCreateBackup(_ createPresenter: CreatePresenter) {
            self.isCreate = true
        }
    }
}

private extension CreateCellProps {
    init(title: String,
         detail: String,
         isSelected: Bool,
         type: CreateCellType) {
        self.init(title: title,
                  detail: detail,
                  accessibilityValue: "Accessibility.tabs.mails",
                  isSelected: isSelected,
                  type: type,
                  onTap: Command(action: {}))
    }
}
