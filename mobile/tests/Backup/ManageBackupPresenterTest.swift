//
//  ManageBackupPresenterTest.swift
//  BackupTests
//
//  Created by Aleksey Makhutin on 11.08.2021.
//

import Foundation

import XCTest
import Utils
import TestUtils
@testable import Backup
import Combine
import NetworkLayer

internal final class ManageBackupPresenterTest: XCTestCase {
    func testStartAndRerender() {
        let render = MockRender()
        let router = MockRouter()
        let dataSource = MockDataSource()
        let storage = MockStorage()
        let backupStatusSynchronizer = BackupStateSynchronizer(storage: storage, statusLoader: dataSource)
        let presenter = ManageBackupPresenter(backupStatusSynchronizer: backupStatusSynchronizer, router: router)
        presenter.render = render

        presenter.start()
        XCTAssertNotNil(render.props)
        XCTAssertNotNil(dataSource.completion, "should request status")
        XCTAssertEqual(render.props?.items, [.loading], "should loading until request status is not complete")

        render.props = nil
        presenter.rerender()
        XCTAssertNotNil(render.props)
        XCTAssertNotNil(dataSource.completion, "should request status")
        XCTAssertEqual(render.props?.items, [.loading], "should loading until request status is not complete")
    }

    func testLoadStatus() {
        func check(with result: Result<BackupStatus>) {
            let render = MockRender()
            let router = MockRouter()
            let dataSource = MockDataSource()
            let storage = MockStorage()
            let backupStatusSynchronizer = BackupStateSynchronizer(storage: storage, statusLoader: dataSource)
            let presenter = ManageBackupPresenter(backupStatusSynchronizer: backupStatusSynchronizer, router: router)
            presenter.render = render

            presenter.start()
            XCTAssertNotNil(render.props)
            XCTAssertNotNil(dataSource.completion, "should request status")
            XCTAssertEqual(render.props?.items, [.loading], "should loading until request status is not complete")

            dataSource.finish(with: result)

            result.onError { error in
                XCTAssertNotNil(render.props)
                XCTAssertEqual(render.props?.items, [.error(props: .any)])
                if error.yo_isNetworkError {
                    XCTAssertEqual(router.error, .network)
                }

                dataSource.completion = nil
                if case .error(let errorProps) = render.props?.items.first {
                    errorProps.onTap()
                    XCTAssertNotNil(dataSource.completion, "Should create new request")
                    XCTAssertEqual(render.props?.items, [.loading], "should loading until request status is not complete")
                } else {
                    XCTFail("ErrorPops should exist")
                }
            }
            
            let checkCreateButton: (ManageBackupProps.ItemType?) -> Void = { item in
                guard case .button( _, let onTap) = item else {
                    XCTFail("button should exist")
                    return
                }
                
                onTap()
                XCTAssertTrue(router.isOpenCreating)
            }
            
            let checkRecoveryButton: (ManageBackupProps.ItemType?, Date) -> Void = { item, checkDate  in
                guard case .button(_, let onTap) = item else {
                    XCTFail("button should exist")
                    return
                }
                onTap()
                XCTAssertEqual(router.backupDate, checkDate)
            }

            result.onValue { status in
                XCTAssertEqual(render.props?.items.count, 2)

                switch status.currentBackup {
                case .nothing:
                    XCTAssertEqual(render.props?.items.first, .text("", withLoading: false))
                case .exist(let checkDate):
                    XCTAssertEqual(render.props?.items.first, .button(title: "", onTap: Command(action: {})))
                    checkRecoveryButton(render.props?.items[0], checkDate)
                }

                if status.hasBeingCreatedBackup {
                    XCTAssertEqual(render.props?.items.last, .text("", withLoading: true))
                } else {
                    XCTAssertEqual(render.props?.items.last, .button(title: "", onTap: Command(action: {})))
                    checkCreateButton(render.props?.items[1])
                }
            }
        }

        check(with: .failure(TestError.networkError))
        check(with: .failure(TestError.someError))
        check(with: .success(BackupStatus(currentBackup: .nothing, hasBeingCreatedBackup: false, restoringBackup: .nothing)))
        check(with: .success(BackupStatus(currentBackup: .nothing, hasBeingCreatedBackup: true, restoringBackup: .nothing)))
        check(with: .success(BackupStatus(currentBackup: .exist(created: Date(timeIntervalSince1970: 1_626_685_776)),
                                          hasBeingCreatedBackup: false,
                                          restoringBackup: .nothing)))
        check(with: .success(BackupStatus(currentBackup: .exist(created: Date(timeIntervalSince1970: 1_626_685_776)),
                                          hasBeingCreatedBackup: true,
                                          restoringBackup: .nothing)))
    }

    func testDealocating() {
        let render = MockRender()
        let router = MockRouter()

        let dataSource = MockDataSource()
        let storage = MockStorage()
        let backupStatusSynchronizer = BackupStateSynchronizer(storage: storage, statusLoader: dataSource)
        var presenter: ManageBackupPresenter? = ManageBackupPresenter(backupStatusSynchronizer: backupStatusSynchronizer, router: router)
        presenter?.render = render

        presenter?.start()
        XCTAssertNotNil(render.props)
        XCTAssertNotNil(dataSource.completion, "should request status")
        XCTAssertEqual(render.props?.items, [.loading], "should loading until request status is not complete")

        render.props = nil
        presenter?.rerender()
        XCTAssertNotNil(render.props)
        XCTAssertNotNil(dataSource.completion, "should request status")
        XCTAssertEqual(render.props?.items, [.loading], "should loading until request status is not complete")
        
        weak var weakPresenter = presenter
        XCTAssertNotNil(weakPresenter)
        presenter = nil
        XCTAssertNil(weakPresenter)
    }
}

private extension ManageBackupPresenterTest {
    final class MockRender: ManageBackupRendering {
        var props: ManageBackupProps?

        func render(props: ManageBackupProps) {
            self.props = props
        }
    }

    final class MockRouter: ManageBackupRouting {
        var error: Router.Error?

        var isOpenRecovery = false
        var isOpenCreating = false
        var backupDate: Date?

        func showError(_ error: Router.Error) {
            self.error = error
        }

        func openBackupRecovery() {
            self.isOpenRecovery = true
        }

        func openBackupCreating(overwrite: Bool) {
            self.isOpenCreating = true
        }

        func openBackupInfo(date: Date) {
            self.backupDate = date
        }
    }

    final class MockDataSource: BackupStatusLoader {
        var currentStatus: Result<BackupStatus>?

        var completion: ((Result<BackupStatus>) -> Void)?

        func finish(with result: Result<BackupStatus>) {
            self.currentStatus = result
            self.completion?(result)
        }
        func loadStatus(completion: @escaping (Result<BackupStatus>) -> Void) {
            self.completion = completion
        }
    }

    final class MockStorage: BackupWritableStateStoring {
        @Published
        var currentBackupState: BackupState?

        func setNewBackupState(_ newState: BackupState) {
            self.currentBackupState = newState
        }

        var newStatePublisher: AnyPublisher<BackupState, Never> {
            return self.$currentBackupState
                .dropFirst()
                .compactMap { $0 }
                .eraseToAnyPublisher()
        }
    }
}

extension ManageBackupProps.ItemType: Equatable {
    public static func == (lhs: ManageBackupProps.ItemType, rhs: ManageBackupProps.ItemType) -> Bool {
        switch (lhs, rhs) {
        case (.button, .button):
            return true
        case (.text, .text):
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

extension ErrorProps {
    static let any = ErrorProps(title: "", text: "", buttonTitle: "", onTap: Command(action: {}))
}
