//
//  DeleteEnotPresenterTest.swift
//  BackupTests
//
//  Created by Aleksey Makhutin on 11.08.2021.
//

import Foundation

import XCTest
import Utils
import TestUtils
@testable import Backup

internal final class DeleteEnotPresenterTest: XCTestCase {
    func testStartAndRerender() {
        let render = MockRender()
        let networkPerformer = MockNetworkPerformer()
        let router = MockRouter()
        let delegate = Delegate()
        let presenter = DeleteEnotPresenter(render: render, networkPerformer: networkPerformer, router: router)
        presenter.delegate = delegate
        presenter.start()
        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.buttonState, .normal)

        render.props = nil
        presenter.rerender()
        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.buttonState, .normal)
    }

    func testCorrectOverriding() {
        let render = MockRender()
        let networkPerformer = MockNetworkPerformer()
        let router = MockRouter()
        let delegate = Delegate()
        let presenter = DeleteEnotPresenter(render: render, networkPerformer: networkPerformer, router: router)
        presenter.delegate = delegate
        presenter.showError(isNetworkError: true)
        XCTAssertEqual(router.error, .network)
        router.error = nil

        presenter.showError(isNetworkError: false)
        XCTAssertEqual(router.error, .enotOff)

        var deleteResult: Result<Void>?
        presenter.delete { result in
            deleteResult = result
        }
        XCTAssertNotNil(networkPerformer.completion)
        networkPerformer.finishDeleting(with: .success(()))
        XCTAssertNotNil(deleteResult)

        presenter.didFinishDeleting(with: true)
        XCTAssertTrue(delegate.result == true)
        delegate.result = nil
        XCTAssertNil(presenter.delegate, "Delegate should be nil, because didFinishDeleting must be executed once")

        presenter.delegate = delegate
        presenter.didFinishDeleting(with: false)
        XCTAssertTrue(delegate.result == false)
    }

    func testDealocating() {
        let render = MockRender()
        let networkPerformer = MockNetworkPerformer()
        let router = MockRouter()
        let delegate = Delegate()
        var presenter: DeleteEnotPresenter? = DeleteEnotPresenter(render: render, networkPerformer: networkPerformer, router: router)
        presenter?.delegate = delegate
        presenter?.start()

        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.buttonState, .normal)

        render.props = nil
        presenter?.rerender()
        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.buttonState, .normal)

        render.props?.onTap()

        weak var weakPresenter = presenter
        XCTAssertNotNil(weakPresenter)
        presenter = nil
        XCTAssertNil(weakPresenter)
    }
}

private extension DeleteEnotPresenterTest {
    final class MockRouter: DeleteEnotRouting {
        var error: Router.Error?

        func showError(_ error: Router.Error) {
            self.error = error
        }
    }

    final class Delegate: DeleteEnotPresenterDelegate {
        var result: Bool?

        func deleteEnotPresenter(_ deleteEnotPresenter: DeleteEnotPresenter, didFinishDeletingWithResult result: Bool) {
            self.result = result
        }
    }

    final class MockNetworkPerformer: DeleteEnotNetworkPerformer {
        var completion: ((Result<Void>) -> Void)?

        func finishDeleting(with result: Result<Void>) {
            self.completion?(result)
        }

        func deleteHiddenTrash(_ completion: @escaping (Result<Void>) -> Void) {
            self.completion = completion
        }
    }

    final class MockRender: DeleteRendering {
        var props: DeleteProps?

        func render(props: DeleteProps) {
            self.props = props
        }
    }
}
