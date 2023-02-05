//
//  SearchPresenterTest.swift
//  ExclusiveEmailTests
//
//  Created by Aleksey Makhutin on 23.06.2021.
//

import Foundation
import XCTest
import Utils
@testable import ExclusiveEmail

internal final class SearchPresenterTest: XCTestCase {
    func testStartAndRerender() {
        let searchFieldPresenter = MockSearchFieldPresenter()
        let suggestionsPresenter = MockSuggestionsPresenter()
        let render = MockRender()
        let router = MockRouter()
        let loginFormatter = MockFormatter()
        let domainFormatter = MockDomainFormatter()
        let presenter = SearchPresenter(render: render,
                                        loginFormatter: loginFormatter,
                                        domainFormatter: domainFormatter,
                                        searchFieldPresenter: searchFieldPresenter,
                                        suggestionsPresenter: suggestionsPresenter,
                                        router: router)
        searchFieldPresenter.render = presenter
        suggestionsPresenter.render = presenter
        presenter.start()

        XCTAssertNotNil(render.props)
        XCTAssertTrue(searchFieldPresenter.isStart)
        XCTAssertTrue(suggestionsPresenter.isStart)
        XCTAssertEqual(render.rerenderCount, 2)
        XCTAssertEqual(searchFieldPresenter.rerenderCount, 2, "should rerender twice")
        XCTAssertEqual(suggestionsPresenter.rerenderCount, 1)
        XCTAssertEqual(suggestionsPresenter.email, .empty)
        XCTAssertEqual(searchFieldPresenter.state, .normal)

        presenter.rerender()
        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.rerenderCount, 3)

        XCTAssertEqual(searchFieldPresenter.rerenderCount, 2, "should not rerender presenters, should get props from cache")
        XCTAssertEqual(suggestionsPresenter.rerenderCount, 1, "should not rerender presenters, should get props from cache")
    }

    func testSearchFieldPropsState() {
        enum Formatter {
            case nothing
            case login
            case domain
            case both
        }
        func checkSearchFieldPropsState(domainStatus: SuggestionsModel.DomainStatus, searchFieldState: SearchFieldProps.State, formatter: Formatter) {
            let searchFieldPresenter = MockSearchFieldPresenter()
            let suggestionsPresenter = MockSuggestionsPresenter()
            let email = Email(login: "test", domain: "email")
            let render = MockRender()
            let router = MockRouter()
            let loginFormatter = MockFormatter()
            let domainFormatter = MockDomainFormatter()

            switch formatter {
            case .nothing:
                break
            case .domain:
                domainFormatter.errors = [.invalidChars]
            case .login:
                loginFormatter.errors = [.invalidChars]
            case .both:
                loginFormatter.errors = [.invalidChars]
                domainFormatter.errors = [.invalidChars]
            }

            let presenter = SearchPresenter(render: render,
                                            loginFormatter: loginFormatter,
                                            domainFormatter: domainFormatter,
                                            searchFieldPresenter: searchFieldPresenter,
                                            suggestionsPresenter: suggestionsPresenter,
                                            router: router)
            searchFieldPresenter.render = presenter
            suggestionsPresenter.render = presenter
            suggestionsPresenter.suggestionsProps = SuggestionsProps(state: .normal,
                                                                     domainStatus: domainStatus,
                                                                     cells: [],
                                                                     headerProps: nil,
                                                                     loadMoreProps: LoadMoreProps(isEnabled: true, onLoadMore: Command(action: {})))
            presenter.start()
            searchFieldPresenter.delegate?.searchFieldPresenter(searchFieldPresenter, emailDidChange: email)

            switch formatter {
            case .nothing:
                break
            case .domain:
                XCTAssertEqual(email.domain, domainFormatter.domain)
            case .login:
                XCTAssertEqual(email.login, loginFormatter.login)
            case .both:
                XCTAssertEqual(email.login, loginFormatter.login)
                XCTAssertEqual(email.domain, domainFormatter.domain)
            }
            XCTAssertNotNil(render.props)
            XCTAssertEqual(render.rerenderCount, 3)
            XCTAssertEqual(render.props?.searchFieldProps.state, searchFieldState)
        }

        checkSearchFieldPropsState(domainStatus: .available, searchFieldState: .normal, formatter: .nothing)
        checkSearchFieldPropsState(domainStatus: .notAllowed, searchFieldState: .normal, formatter: .nothing)
        checkSearchFieldPropsState(domainStatus: .occupied, searchFieldState: .domainError, formatter: .nothing)

        checkSearchFieldPropsState(domainStatus: .available, searchFieldState: .loginError, formatter: Formatter.login)
        checkSearchFieldPropsState(domainStatus: .notAllowed, searchFieldState: .loginError, formatter: Formatter.login)
        checkSearchFieldPropsState(domainStatus: .occupied, searchFieldState: .error, formatter: Formatter.login)

        checkSearchFieldPropsState(domainStatus: .available, searchFieldState: .domainError, formatter: Formatter.domain)
        checkSearchFieldPropsState(domainStatus: .notAllowed, searchFieldState: .domainError, formatter: Formatter.domain)
        checkSearchFieldPropsState(domainStatus: .occupied, searchFieldState: .domainError, formatter: Formatter.domain)

        checkSearchFieldPropsState(domainStatus: .available, searchFieldState: .error, formatter: Formatter.both)
        checkSearchFieldPropsState(domainStatus: .notAllowed, searchFieldState: .error, formatter: Formatter.both)
        checkSearchFieldPropsState(domainStatus: .occupied, searchFieldState: .error, formatter: Formatter.both)
    }

    func testOpenEmailConfirmation() {
        let searchFieldPresenter = MockSearchFieldPresenter()
        let suggestionsPresenter = MockSuggestionsPresenter()
        let render = MockRender()
        let router = MockRouter()
        let loginFormatter = MockFormatter()
        let domainFormatter = MockDomainFormatter()
        let presenter = SearchPresenter(render: render,
                                        loginFormatter: loginFormatter,
                                        domainFormatter: domainFormatter,
                                        searchFieldPresenter: searchFieldPresenter,
                                        suggestionsPresenter: suggestionsPresenter,
                                        router: router)
        searchFieldPresenter.render = presenter
        suggestionsPresenter.render = presenter
        presenter.start()

        let email = Email(login: "Check", domain: "OpenEmailConfirmation")
        suggestionsPresenter.delegate?.suggestionsPresenter(suggestionsPresenter, didSelectEmail: email)

        XCTAssertEqual(router.email, email)
    }

    func testRerenderSuggestionsWhenEmailDidChange() {
        let searchFieldPresenter = MockSearchFieldPresenter()
        let suggestionsPresenter = MockSuggestionsPresenter()
        let render = MockRender()
        let router = MockRouter()
        let loginFormatter = MockFormatter()
        let domainFormatter = MockDomainFormatter()
        let presenter = SearchPresenter(render: render,
                                        loginFormatter: loginFormatter,
                                        domainFormatter: domainFormatter,
                                        searchFieldPresenter: searchFieldPresenter,
                                        suggestionsPresenter: suggestionsPresenter,
                                        router: router)
        searchFieldPresenter.render = presenter
        suggestionsPresenter.render = presenter
        presenter.start()

        XCTAssertEqual(render.rerenderCount, 2)
        XCTAssertEqual(searchFieldPresenter.rerenderCount, 2, "should rerender twice")
        XCTAssertEqual(suggestionsPresenter.rerenderCount, 1)

        let email = Email(login: "Check", domain: "RerenderSuggestions")
        searchFieldPresenter.delegate?.searchFieldPresenter(searchFieldPresenter, emailDidChange: email)

        XCTAssertEqual(render.rerenderCount, 3)
        XCTAssertEqual(searchFieldPresenter.rerenderCount, 3, "should rerender twice")
        XCTAssertEqual(suggestionsPresenter.rerenderCount, 2)
    }

    func testSuggestionsDelegateShowError() {
        let searchFieldPresenter = MockSearchFieldPresenter()
        let suggestionsPresenter = MockSuggestionsPresenter()
        let render = MockRender()
        let router = MockRouter()
        let loginFormatter = MockFormatter()
        let domainFormatter = MockDomainFormatter()
        let presenter = SearchPresenter(render: render,
                                        loginFormatter: loginFormatter,
                                        domainFormatter: domainFormatter,
                                        searchFieldPresenter: searchFieldPresenter,
                                        suggestionsPresenter: suggestionsPresenter,
                                        router: router)
        searchFieldPresenter.render = presenter
        suggestionsPresenter.render = presenter
        presenter.start()

        suggestionsPresenter.delegate?.suggestionsPresenter(suggestionsPresenter, didReceiveNetworkError: MockError.error)
        XCTAssertNotNil(router.error)
    }

    func testLoginFormatterError() {
        let searchFieldPresenter = MockSearchFieldPresenter()
        let suggestionsPresenter = MockSuggestionsPresenter()
        let email = Email(login: "test_Formatter", domain: "formatter")
        let render = MockRender()
        let router = MockRouter()
        let loginFormatter = MockFormatter()
        let domainFormatter = MockDomainFormatter()
        let presenter = SearchPresenter(render: render,
                                        loginFormatter: loginFormatter,
                                        domainFormatter: domainFormatter,
                                        searchFieldPresenter: searchFieldPresenter,
                                        suggestionsPresenter: suggestionsPresenter,
                                        router: router)
        searchFieldPresenter.render = presenter
        suggestionsPresenter.render = presenter
        presenter.start()
        searchFieldPresenter.delegate?.searchFieldPresenter(searchFieldPresenter, emailDidChange: email)
        XCTAssertEqual(render.rerenderCount, 3)
        XCTAssertEqual(loginFormatter.login, email.login)
        XCTAssertNil(suggestionsPresenter.errorText)

        loginFormatter.login = nil
        loginFormatter.errors = [.invalidChars, .invalidLoginDotAndDash, .invalidLoginPrefix, .invalidLoginSuffix]
        searchFieldPresenter.delegate?.searchFieldPresenter(searchFieldPresenter, emailDidChange: email)
        XCTAssertEqual(loginFormatter.login, email.login)
        XCTAssertNotNil(suggestionsPresenter.errorText)
        loginFormatter.errors.forEach { error in
            XCTAssertTrue(suggestionsPresenter.errorText?.contains(error.description) == true)
        }
    }

    func testDomainFormatterError() {
        let searchFieldPresenter = MockSearchFieldPresenter()
        let suggestionsPresenter = MockSuggestionsPresenter()
        let email = Email(login: "test_Formatter", domain: "formatter")
        let render = MockRender()
        let router = MockRouter()
        let loginFormatter = MockFormatter()
        let domainFormatter = MockDomainFormatter()
        let presenter = SearchPresenter(render: render,
                                        loginFormatter: loginFormatter,
                                        domainFormatter: domainFormatter,
                                        searchFieldPresenter: searchFieldPresenter,
                                        suggestionsPresenter: suggestionsPresenter,
                                        router: router)
        searchFieldPresenter.render = presenter
        suggestionsPresenter.render = presenter
        presenter.start()
        searchFieldPresenter.delegate?.searchFieldPresenter(searchFieldPresenter, emailDidChange: email)
        XCTAssertEqual(render.rerenderCount, 3)
        XCTAssertEqual(domainFormatter.domain, email.domain)
        XCTAssertNil(suggestionsPresenter.errorText)

        domainFormatter.domain = nil
        domainFormatter.errors = [.invalidChars, .invalidDomainSuffix, .invalidDomainPrefix, .invalidDomainDoubleDashesStandInARow, .invalidDomainDots]
        searchFieldPresenter.delegate?.searchFieldPresenter(searchFieldPresenter, emailDidChange: email)
        XCTAssertEqual(domainFormatter.domain, email.domain)
        XCTAssertNotNil(suggestionsPresenter.errorText)
        domainFormatter.errors.forEach { error in
            XCTAssertTrue(suggestionsPresenter.errorText?.contains(error.description) == true)
        }
    }

    func testDeallocating() {
        let searchFieldPresenter = MockSearchFieldPresenter()
        let suggestionsPresenter = MockSuggestionsPresenter()
        let render = MockRender()
        let router = MockRouter()
        let loginFormatter = MockFormatter()
        let domainFormatter = MockDomainFormatter()
        var presenter: SearchPresenter? = SearchPresenter(render: render,
                                                          loginFormatter: loginFormatter,
                                                          domainFormatter: domainFormatter,
                                                          searchFieldPresenter: searchFieldPresenter,
                                                          suggestionsPresenter: suggestionsPresenter,
                                                          router: router)
        searchFieldPresenter.render = presenter
        suggestionsPresenter.render = presenter
        presenter?.start()
        XCTAssertEqual(render.rerenderCount, 2)

        presenter?.rerender()
        XCTAssertEqual(render.rerenderCount, 3)
        XCTAssertEqual(searchFieldPresenter.rerenderCount, 2, "should rerender twice")
        XCTAssertEqual(suggestionsPresenter.rerenderCount, 1)

        let email = Email(login: "Check", domain: "RerenderSuggestions")
        searchFieldPresenter.delegate?.searchFieldPresenter(searchFieldPresenter, emailDidChange: email)

        XCTAssertEqual(render.rerenderCount, 4)
        XCTAssertEqual(searchFieldPresenter.rerenderCount, 3, "should rerender twice")
        XCTAssertEqual(suggestionsPresenter.rerenderCount, 2)

        weak var weakPresenter = presenter
        XCTAssertNotNil(weakPresenter)
        presenter = nil
        XCTAssertNil(weakPresenter)
    }
}

private extension SearchPresenterTest {
    final class MockRender: SearchRendering {
        var props: SearchProps?
        var rerenderCount = 0
        func render(props: SearchProps) {
            self.props = props
            self.rerenderCount += 1
        }
    }

    final class MockSearchFieldPresenter: SearchFieldPresenting {
        weak var render: SearchFieldRendering?
        weak var delegate: SearchFieldPresenterDelegate?
        var isStart = false
        var rerenderCount = 0
        var state: SearchFieldProps.State?

        var onEmailChange: CommandWith<Email> = CommandWith(action: { _ in
        })

        func start() {
            self.isStart = true
            self.rerender(state: .normal)
        }

        func rerender(state: SearchFieldProps.State) {
            self.rerenderCount += 1
            self.state = state
            self.render?.render(props: SearchFieldProps(state: self.state ?? .normal, onEmailChange: self.onEmailChange))
        }
    }

    final class MockSuggestionsPresenter: SuggestionsPresenting {
        weak var delegate: SuggestionsPresenterDelegate?
        weak var render: SuggestionsRendering?
        
        var isStart = false
        var rerenderCount = 0
        var email: Email?
        var errorText: String?

        var suggestionsProps = SuggestionsProps(state: .loading,
                                                domainStatus: .available,
                                                cells: [],
                                                headerProps: nil,
                                                loadMoreProps: LoadMoreProps(isEnabled: false, onLoadMore: Command(action: {})))

        func start() {
            self.isStart = true
            self.rerender(with: .empty, errorText: nil)
        }

        func rerender(with email: Email, errorText: String?) {
            self.rerenderCount += 1
            self.email = email
            self.errorText = errorText
            self.render?.render(props: self.suggestionsProps)
        }
    }

    final class MockRouter: SearchRouting {
        var error: Router.Error?
        var email: Email?

        func openConfirmation(with email: Email) {
            self.email = email
        }

        func show(error: Router.Error) {
            self.error = error
        }
    }

    final class MockFormatter: LoginFormatting {
        var errors = [FormatterError]()
        var login: String?
        func check(_ login: String) -> [FormatterError] {
            self.login = login
            return self.errors
        }
    }

    final class MockDomainFormatter: DomainFormatting {
        var errors = [FormatterError]()
        var domain: String?
        func check(_ domain: String) -> [FormatterError] {
            self.domain = domain
            return self.errors
        }
    }

    enum MockError: Error {
        case error
    }
}
