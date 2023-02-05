//
//  RestorePresenterTest.swift
//  BackupTests
//
//  Created by Aleksey Makhutin on 23.08.2021.
//

import Foundation

import XCTest
import Utils
import TestUtils
import NetworkLayer
@testable import Backup

internal final class RestorePresenterTest: XCTestCase {
    func testStartAndRerender() {
        let render = MockRender()
        let router = MockRouter()
        let networkPerformer = MockNetworkPerformer()
        let presenter = RestorePresenter(render: render, networkPerformer: networkPerformer, router: router)
        presenter.start()

        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.state, .normal)

        render.props = nil
        presenter.rerender()
        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.state, .normal)
    }

    func testLoadingState() {
        func checkWithMethod(method: RestoreBackupMethod, result: Result<Void>) {
            let render = MockRender()
            let router = MockRouter()
            let networkPerformer = MockNetworkPerformer()
            let delegate = MockDelegate()
            let presenter = RestorePresenter(render: render, networkPerformer: networkPerformer, router: router)
            presenter.delegate = delegate
            presenter.start()

            XCTAssertNotNil(render.props)
            XCTAssertEqual(render.props?.state, .normal)

            switch method {
            case .fullHierarchy:
                render.props?.onFullHierarchyTap()
            case .restoredFolder:
                render.props?.onRestoredFolderTap()
            }

            XCTAssertEqual(render.props?.state, .loading)
            networkPerformer.finishLoading(with: result)
            XCTAssertEqual(render.props?.state, .normal)

            result.onValue { _ in
                XCTAssertEqual(networkPerformer.method, method)
                XCTAssertNil(router.error)
                XCTAssertEqual(delegate.method, method)
            }

            result.onError { error in
                XCTAssertEqual(networkPerformer.method, method)
                XCTAssertNil(delegate.method)
                XCTAssertEqual(router.error, error.yo_isNetworkError ? .network : .restoreBackup)
            }
        }

        let results: [Result<Void>] = [.success(()), .failure(TestError.someError), .failure(TestError.networkError)]
        results.forEach { result in
            checkWithMethod(method: .restoredFolder, result: result)
            checkWithMethod(method: .fullHierarchy, result: result)
        }
    }

    func testDealocating() {
        let render = MockRender()
        let router = MockRouter()
        let networkPerformer = MockNetworkPerformer()
        var presenter: RestorePresenter? = RestorePresenter(render: render, networkPerformer: networkPerformer, router: router)
        presenter?.start()

        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.state, .normal)

        render.props = nil
        presenter?.rerender()
        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.state, .normal)

        render.props?.onFullHierarchyTap()

        weak var weakPresenter = presenter
        XCTAssertNotNil(weakPresenter)
        presenter = nil
        XCTAssertNil(weakPresenter)
    }
}

private extension RestorePresenterTest {
    final class MockRender: RestoreRendering {
        var props: RestoreProps?

        func render(props: RestoreProps) {
            self.props = props
        }
    }

    final class MockRouter: RestoreRouting {
        var error: Router.Error?

        func showError(_ error: Router.Error) {
            self.error = error
        }
    }

    final class MockNetworkPerformer: RestoreNetworkPerformer {
        var completion: ((Result<Void>) -> Void)?
        var method: RestoreBackupMethod?

        func finishLoading(with result: Result<Void>) {
            completion?(result)
        }

        func restoreBackup(method: RestoreBackupMethod, completion: @escaping (Result<Void>) -> Void) {
            self.method = method
            self.completion = completion
        }
    }

    final class MockDelegate: RestorePresenterDelegate {
        var method: RestoreBackupMethod?

        func restorePresenter(_ restorePresenter: RestorePresenter, didRestoreBackupWith method: RestoreBackupMethod) {
            self.method = method
        }
    }
}
