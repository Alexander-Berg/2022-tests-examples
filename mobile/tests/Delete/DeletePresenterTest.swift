//
//  DeletePresenter.swift
//  ExclusiveEmailTests
//
//  Created by Aleksey Makhutin on 26.07.2021.
//

import Foundation
import XCTest
import Utils
@testable import ExclusiveEmail

internal final class DeletePresenterTest: XCTestCase {
    func testStartAndRerender() {
        let render = MockRender()
        let router = MockRouter()
        let networkPerformer = MockNetworkPerformer()
        let email = Email(login: "test", domain: "email")
        let date = Calendar.current.date(byAdding: .month, value: -1, to: Date())!
        let presenter = DeletePresenter(render: render,
                                        email: email,
                                        nextAvailableRegisterDate: date,
                                        networkPerformer: networkPerformer,
                                        router: router)
        presenter.start()

        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.buttonState, .normal)
        XCTAssertNil(render.props?.infoText, "RegisterAllowedDate less than current")
        render.props = nil

        presenter.rerender()
        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.buttonState, .normal)
        XCTAssertNil(render.props?.infoText, "RegisterAllowedDate less than current")
    }

    func testOnTapAndButtonState() {
        func checkFinishWith(result: Result<Void>) {
            let render = MockRender()
            let router = MockRouter()
            let networkPerformer = MockNetworkPerformer()
            let email = Email(login: "test", domain: "email")
            let date = Calendar.current.date(byAdding: .month, value: -1, to: Date())!
            let delegate = MockDelegate()
            let presenter = DeletePresenter(render: render,
                                            email: email,
                                            nextAvailableRegisterDate: date,
                                            networkPerformer: networkPerformer,
                                            router: router)
            presenter.delegate = delegate
            presenter.start()

            XCTAssertNotNil(render.props)
            XCTAssertEqual(render.props?.buttonState, .normal)
            XCTAssertNil(render.props?.infoText, "RegisterAllowedDate less than current")

            render.props?.onTap()

            XCTAssertEqual(render.props?.buttonState, .loading)

            networkPerformer.finish(with: result)

            result.onError { error in
                if error.yo_isNetworkError {
                    XCTAssertEqual(router.error, .network)
                } else {
                    XCTAssertEqual(router.error, .deleting)
                }
                XCTAssertFalse(delegate.isDelete)
            }
            result.onValue { _ in
                XCTAssertTrue(delegate.isDelete)
                XCTAssertNil(router.error)
            }
        }

        checkFinishWith(result: .failure(NSError(domain: "", code: 0, userInfo: nil)))
        checkFinishWith(result: .failure(MockError.error))
        checkFinishWith(result: .success(()))
    }

    func testInfoText() {
        let render = MockRender()
        let router = MockRouter()
        let networkPerformer = MockNetworkPerformer()
        let email = Email(login: "test", domain: "email")
        let date = Calendar.current.date(byAdding: .month, value: 1, to: Date())!
        let delegate = MockDelegate()
        let presenter = DeletePresenter(render: render,
                                        email: email,
                                        nextAvailableRegisterDate: date,
                                        networkPerformer: networkPerformer,
                                        router: router)
        presenter.delegate = delegate
        presenter.start()

        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.buttonState, .normal)
        XCTAssertNotNil(render.props?.infoText, "RegisterAllowedDate older than current")
    }

    func testDeallocating() {
        let render = MockRender()
        let router = MockRouter()
        let networkPerformer = MockNetworkPerformer()
        let email = Email(login: "test", domain: "email")
        let date = Calendar.current.date(byAdding: .month, value: -1, to: Date())!
        var presenter: DeletePresenter? = DeletePresenter(render: render,
                                                          email: email,
                                                          nextAvailableRegisterDate: date,
                                                          networkPerformer: networkPerformer,
                                                          router: router)
        presenter?.start()

        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.buttonState, .normal)
        XCTAssertNil(render.props?.infoText, "RegisterAllowedDate less than current")

        render.props = nil
        presenter?.rerender()

        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.buttonState, .normal)
        XCTAssertNil(render.props?.infoText, "RegisterAllowedDate less than current")
        render.props?.onTap()

        weak var weakPresenter = presenter
        XCTAssertNotNil(weakPresenter)
        presenter = nil
        XCTAssertNil(weakPresenter)
    }
}

private extension DeletePresenterTest {
    final class MockRender: DeleteRendering {
        var props: DeleteProps?

        func render(props: DeleteProps) {
            self.props = props
        }
    }

    final class MockRouter: DeleteRouting {
        var error: Router.Error?
        func show(error: Router.Error) {
            self.error = error
        }
    }

    final class MockNetworkPerformer: DeleteNetworkPerformer {
        var completion: ((Result<Void>) -> Void)?

        func deleteExclusiveEmail(completion: @escaping (Result<Void>) -> Void) {
            self.completion = completion
        }

        func finish(with result: Result<Void>) {
            self.completion?(result)
        }
    }

    final class MockDelegate: DeletePresenterDelegate {
        var isDelete = false
        func deletePresenterDidDeleteExclusiveEmail(_ deletePresenter: DeletePresenter) {
            self.isDelete = true
        }
    }

    enum MockError: Error {
        case error
    }
}
