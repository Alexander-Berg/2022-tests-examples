//
//  DeletePresenterTest.swift
//  BackupTests
//
//  Created by Aleksey Makhutin on 11.08.2021.
//

import Foundation

import XCTest
import Utils
import TestUtils
import NetworkLayer
@testable import Backup

internal final class DeletePresenterTest: XCTestCase {
    func testStartAndRerender() {
        let render = MockRender()
        let presenter = TestableDeletePresenter(render: render)

        presenter.start()
        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.buttonState, .normal)

        render.props = nil
        presenter.rerender()
        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.buttonState, .normal)
    }

    func testOnTap() {
        func check(with result: Result<Void>) {
            let render = MockRender()
            let presenter = TestableDeletePresenter(render: render)

            presenter.start()
            XCTAssertNotNil(render.props)
            XCTAssertEqual(render.props?.buttonState, .normal)

            render.props?.onTap()
            XCTAssertNotNil(presenter.completion)
            XCTAssertEqual(render.props?.buttonState, .loading)

            presenter.finishDeleting(with: result)
            XCTAssertEqual(render.props?.buttonState, .normal)

            result.onError { error in
                XCTAssertEqual(presenter.isNetworkError, error.yo_isNetworkError)
            }

            result.onValue { _ in
                XCTAssertTrue(presenter.result == true)
            }
        }

        check(with: .failure(TestError.someError))
        check(with: .failure(TestError.networkError))
        check(with: .success(()))
    }

    func testOnClose() {
        let render = MockRender()
        let presenter = TestableDeletePresenter(render: render)

        presenter.start()
        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.buttonState, .normal)

        render.props?.onClose()

        XCTAssertTrue(presenter.result == false)
    }

    func testDealocating() {
        let render = MockRender()
        var presenter: TestableDeletePresenter? = TestableDeletePresenter(render: render)

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

private extension DeletePresenterTest {
    final class TestableDeletePresenter: DeletePresenter {
        var result: Bool?
        var isNetworkError: Bool?
        var completion: ((Result<Void>) -> Void)?

        override func didFinishDeleting(with result: Bool) {
            self.result = result
        }

        override func showError(isNetworkError: Bool) {
            self.isNetworkError = isNetworkError
        }

        override func delete(_ completion: @escaping (Result<Void>) -> Void) {
            self.completion = completion
        }

        func finishDeleting(with result: Result<Void>) {
            self.completion?(result)
        }
    }

    final class MockRender: DeleteRendering {
        var props: DeleteProps?

        func render(props: DeleteProps) {
            self.props = props
        }
    }
}
