//
//  ChangeLoginPresenterTest.swift
//  ExclusiveEmailTests
//
//  Created by Aleksey Makhutin on 07.07.2021.
//

import Foundation
import XCTest
import Utils
import TestUtils
import NetworkLayer
@testable import ExclusiveEmail

internal final class ChangeLoginPresenterTest: XCTestCase {
    func testStartAndRerender() {
        let render = MockRender()
        let email = Email(login: "login_test", domain: "email")
        let searchFieldPresenter = MockSearchFieldPresenter()
        let loginFormatter = MockFormatter()
        let networkPerformer = MockNetworkPerformer()
        let router = MockRouter()
        let presenter = ChangeLoginPresenter(render: render,
                                             email: email,
                                             searchFieldPresenter: searchFieldPresenter,
                                             loginFormatter: loginFormatter,
                                             networkPerformer: networkPerformer,
                                             router: router)
        searchFieldPresenter.render = presenter
        searchFieldPresenter.props = SearchFieldProps(state: .normal,
                                                      onEmailChange: CommandWith(action: { _ in }))
        presenter.start()

        XCTAssertFalse(render.props?.isDoneHidden ?? true)
        XCTAssertEqual(searchFieldPresenter.renderCount, 1)
        XCTAssertNotNil(render.props)
        XCTAssertNil(render.props?.errorText)
        XCTAssertEqual(render.props?.searchFieldProps.state, searchFieldPresenter.props?.state)
        XCTAssertEqual(render.props?.state, .normal)
        XCTAssertEqual(searchFieldPresenter.state, .normal)

        render.props = nil
        presenter.rerender()
        XCTAssertEqual(searchFieldPresenter.renderCount, 1)
        XCTAssertNotNil(render.props)
        XCTAssertNil(render.props?.errorText)
        XCTAssertEqual(render.props?.searchFieldProps.state, searchFieldPresenter.props?.state)
        XCTAssertEqual(searchFieldPresenter.state, .normal)
    }

    func testOnDoneTap() {
        enum Action {
            case nothing(loginError: [FormatterError])
            case closeLogin
            case changeExclusiveEmail(result: Result<Void>)
        }
        func check(with login: String, newLogin: String, action: Action, isDoneHidden: Bool) {
            let render = MockRender()
            let searchFieldPresenter = MockSearchFieldPresenter()
            let loginFormatter = MockFormatter()
            let networkPerformer = MockNetworkPerformer()
            let router = MockRouter()
            let delegate = MockDelegate()
            let email = Email(login: login, domain: "test")
            let newEmail = Email(login: newLogin, domain: "test")
            let presenter = ChangeLoginPresenter(render: render,
                                                 email: email,
                                                 searchFieldPresenter: searchFieldPresenter,
                                                 loginFormatter: loginFormatter,
                                                 networkPerformer: networkPerformer,
                                                 router: router)
            searchFieldPresenter.render = presenter
            searchFieldPresenter.props = SearchFieldProps(state: .normal,
                                                          onEmailChange: CommandWith(action: { _ in }))
            presenter.start()
            if case let .nothing(loginErrors) = action {
                loginFormatter.errors = loginErrors
            }
            searchFieldPresenter.delegate?.searchFieldPresenter(searchFieldPresenter, emailDidChange: newEmail)
            presenter.delegate = delegate

            XCTAssertEqual(searchFieldPresenter.renderCount, 2)
            XCTAssertNotNil(render.props)
            XCTAssertEqual(render.props?.searchFieldProps.state, searchFieldPresenter.props?.state)
            XCTAssertEqual(render.props?.state, .normal)
            render.props?.onDoneTap()

            switch action {
            case .changeExclusiveEmail(let result):
                XCTAssertEqual(networkPerformer.login, newEmail.login)
                XCTAssertEqual(render.props?.state, .loading)
                XCTAssertTrue(render.props?.isDoneHidden == true)

                networkPerformer.finish(with: result)
                XCTAssertEqual(render.props?.state, .normal)
                result.onValue { _ in
                    XCTAssertTrue(delegate.isLoginChanged)
                    XCTAssertNil(router.error)
                }
                result.onError { error in
                    if error.yo_isNetworkError {
                        XCTAssertEqual(router.error, .network)
                    } else {
                        XCTAssertEqual(router.error, .loginChanging)
                    }
                }
                XCTAssertFalse(router.isCloseLogin)
            case .closeLogin:
                XCTAssertTrue(router.isCloseLogin)
                XCTAssertNil(router.error)
                XCTAssertFalse(delegate.isLoginChanged)
            case .nothing:
                XCTAssertFalse(router.isCloseLogin)
                XCTAssertNil(router.error)
                XCTAssertFalse(delegate.isLoginChanged)
            }
            XCTAssertEqual(render.props?.isDoneHidden, isDoneHidden)
        }

        check(with: "login", newLogin: "login", action: .closeLogin, isDoneHidden: false)
        check(with: "login", newLogin: "", action: .nothing(loginError: []), isDoneHidden: true)
        let loginErrors = [
            FormatterError.invalidChars,
            .invalidLoginDotAndDash,
            .invalidLoginPrefix,
            .invalidLoginSuffix
        ]
        loginErrors.forEach { error in
            check(with: "login", newLogin: "newLogin", action: .nothing(loginError: [error]), isDoneHidden: true)
        }
        let results = [
            Result<Void>.failure(TestError.someError),
            .failure(TestError.networkError),
            .success(())
        ]
        results.forEach { error in
            check(with: "login", newLogin: "newLogin", action: .changeExclusiveEmail(result: error), isDoneHidden: false)
        }
    }

    func testErrorText() {
        func check(with errors: [FormatterError]) {
            let render = MockRender()
            let searchFieldPresenter = MockSearchFieldPresenter()
            let loginFormatter = MockFormatter()
            let networkPerformer = MockNetworkPerformer()
            let router = MockRouter()
            let delegate = MockDelegate()
            let email = Email(login: "login", domain: "test")
            let newEmail = Email(login: "new_login", domain: "test")
            let presenter = ChangeLoginPresenter(render: render,
                                                 email: email,
                                                 searchFieldPresenter: searchFieldPresenter,
                                                 loginFormatter: loginFormatter,
                                                 networkPerformer: networkPerformer,
                                                 router: router)
            searchFieldPresenter.render = presenter
            searchFieldPresenter.props = SearchFieldProps(state: .normal,
                                                          onEmailChange: CommandWith(action: { _ in }))
            presenter.start()
            XCTAssertEqual(searchFieldPresenter.renderCount, 1)
            XCTAssertNotNil(render.props)
            XCTAssertEqual(render.props?.searchFieldProps.state, searchFieldPresenter.props?.state)
            XCTAssertEqual(render.props?.state, .normal)

            loginFormatter.errors = errors
            searchFieldPresenter.delegate?.searchFieldPresenter(searchFieldPresenter, emailDidChange: newEmail)
            presenter.delegate = delegate
            XCTAssertTrue(render.props?.errorText?.contains(errors.joinedErrorText ?? "") == true)
            XCTAssertEqual(searchFieldPresenter.state, .loginError)
        }

        check(with: [.invalidChars])
        check(with: [.invalidLoginSuffix])
        check(with: [.invalidLoginPrefix])
        check(with: [.invalidLoginDotAndDash])
        check(with: [.invalidChars, .invalidLoginPrefix, .invalidLoginSuffix, .invalidLoginDotAndDash])
    }

    func testBuildInfoText() {
        let render = MockRender()
        let searchFieldPresenter = MockSearchFieldPresenter()
        let loginFormatter = MockFormatter()
        let networkPerformer = MockNetworkPerformer()
        let router = MockRouter()
        let email = Email(login: "info_login", domain: "info_domain")
        let presenter = ChangeLoginPresenter(render: render,
                                             email: email,
                                             searchFieldPresenter: searchFieldPresenter,
                                             loginFormatter: loginFormatter,
                                             networkPerformer: networkPerformer,
                                             router: router)
        searchFieldPresenter.render = presenter
        searchFieldPresenter.props = SearchFieldProps(state: .normal,
                                                      onEmailChange: CommandWith(action: { _ in }))
        presenter.start()

        XCTAssertEqual(searchFieldPresenter.renderCount, 1)
        XCTAssertEqual(render.props?.state, .normal)
        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.searchFieldProps.state, searchFieldPresenter.props?.state)
    }

    func testDeallocating() {
        let render = MockRender()
        let email = Email(login: "login_test", domain: "email")
        let searchFieldPresenter = MockSearchFieldPresenter()
        let loginFormatter = MockFormatter()
        let networkPerformer = MockNetworkPerformer()
        let router = MockRouter()
        var presenter: ChangeLoginPresenter? = ChangeLoginPresenter(render: render,
                                                                    email: email,
                                                                    searchFieldPresenter: searchFieldPresenter,
                                                                    loginFormatter: loginFormatter,
                                                                    networkPerformer: networkPerformer,
                                                                    router: router)
        searchFieldPresenter.render = presenter
        searchFieldPresenter.props = SearchFieldProps(state: .normal,
                                                      onEmailChange: CommandWith(action: { _ in }))
        presenter?.start()

        XCTAssertEqual(searchFieldPresenter.renderCount, 1)
        XCTAssertNotNil(render.props)
        XCTAssertNil(render.props?.errorText)
        XCTAssertEqual(render.props?.searchFieldProps.state, searchFieldPresenter.props?.state)
        XCTAssertEqual(render.props?.state, .normal)
        XCTAssertEqual(searchFieldPresenter.state, .normal)

        render.props = nil
        presenter?.rerender()
        XCTAssertEqual(searchFieldPresenter.renderCount, 1)
        XCTAssertNotNil(render.props)
        XCTAssertNil(render.props?.errorText)
        XCTAssertEqual(render.props?.searchFieldProps.state, searchFieldPresenter.props?.state)
        XCTAssertEqual(searchFieldPresenter.state, .normal)

        weak var weakPresenter = presenter
        XCTAssertNotNil(weakPresenter)
        presenter = nil
        XCTAssertNil(weakPresenter)
    }
}

private extension ChangeLoginPresenterTest {
    final class MockRender: ChangeLoginRendering {
        var props: ChangeLoginProps?

        func render(props: ChangeLoginProps) {
            self.props = props
        }
    }

    final class MockRouter: ChangeLoginRouting {
        var error: Router.Error?
        var isCloseLogin = false

        func show(error: Router.Error) {
            self.error = error
        }

        func closeChangeLogin() {
            self.isCloseLogin = true
        }
    }

    final class MockNetworkPerformer: ChangeLoginNetworkPerformer {
        private var completion: ((Result<Void>) -> Void)?
        var login: String?

        func changeExlusiveEmailLogin(_ login: String, completion: @escaping (Result<Void>) -> Void) {
            self.login = login
            self.completion = completion
        }

        func finish(with result: Result<Void>) {
            self.completion?(result)
        }
    }

    final class MockSearchFieldPresenter: SearchFieldPresenting {
        weak var delegate: SearchFieldPresenterDelegate?
        weak var render: SearchFieldRendering?
        
        var state: SearchFieldProps.State?
        var renderCount = 0
        var props: SearchFieldProps?

        func start() {
            self.rerender(state: .normal)
        }

        func rerender(state: SearchFieldProps.State) {
            self.renderCount += 1
            self.state = state
            self.render?.render(props: self.props ?? SearchFieldProps(state: .normal, onEmailChange: CommandWith(action: { _ in })))
        }
    }

    final class MockFormatter: LoginFormatting {
        var login: String?
        var errors = [FormatterError]()

        func check(_ login: String) -> [FormatterError] {
            self.login = login
            return self.errors
        }
    }

    final class MockDelegate: ChangeLoginPresenterDelegate {
        var isLoginChanged = false
        func changeLoginPresenterDidChangeExclusiveEmail(_ changeLoginPresenter: ChangeLoginPresenter) {
            isLoginChanged = true
        }
    }
}
