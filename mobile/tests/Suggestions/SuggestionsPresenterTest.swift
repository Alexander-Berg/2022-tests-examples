//
//  SuggestionsPresenterTest.swift
//  ExclusiveEmailTests
//
//  Created by Aleksey Makhutin on 24.06.2021.
//

import Foundation
import XCTest
import Utils
import TestUtils
import NetworkLayer
@testable import ExclusiveEmail

internal final class SuggestionsPresenterTest: XCTestCase {
    func testStart() {
        let email = Email(login: "Some", domain: "test")
        let dataSource = MockDataSource()
        let render = MockRender()
        let infinityDebouncer = Debouncer(queue: .main, delay: .infinity)
        let presenter = SuggestionsPresenter(dataSource: dataSource, debouncer: infinityDebouncer)
        presenter.render = render
        presenter.start()

        XCTAssertEqual(dataSource.email, Email.empty)
        XCTAssertEqual(dataSource.limit, 10)

        self.checkEmptyProps(props: render.props, state: .loading)

        let status: SuggestionsModel.DomainStatus = .available
        dataSource.result = .success(SuggestionsModel(domainStatus: status, emails: [
            email
        ]))
        dataSource.finishLoading()

        XCTAssertEqual(render.props?.cells.map { $0.email }, [email])
        XCTAssertEqual(render.props?.domainStatus, .available)
        XCTAssertEqual(render.props?.state, .normal)
    }

    func testRerenderAndStates() {
        let dataSource = MockDataSource()
        let render = MockRender()
        let debouncer = MockDebouncer()
        let presenter = SuggestionsPresenter(dataSource: dataSource, debouncer: debouncer)
        presenter.render = render
        presenter.start()

        XCTAssertEqual(dataSource.email, Email.empty)
        XCTAssertEqual(dataSource.limit, 10)
        self.checkEmptyProps(props: render.props, state: .loading)
        dataSource.result = .success(SuggestionsModel(domainStatus: .available, emails: []))
        dataSource.finishLoading()

        // Check normal state
        let normalEmail = Email(login: "normal", domain: "state")
        presenter.rerender(with: normalEmail, errorText: nil)
        dataSource.result = .success(SuggestionsModel(domainStatus: .available, emails: [normalEmail]))
        dataSource.finishLoading()

        XCTAssertEqual(dataSource.email, normalEmail)
        XCTAssertEqual(dataSource.limit, 10)
        XCTAssertEqual(render.props?.cells.map { $0.email }, [normalEmail])
        XCTAssertEqual(render.props?.domainStatus, .available)
        XCTAssertEqual(render.props?.state, .normal)

        // Check error state
        let errorEmail = Email(login: "error", domain: "state")
        presenter.rerender(with: errorEmail, errorText: nil)
        dataSource.result = .failure(MockError.error)
        dataSource.finishLoading()

        XCTAssertEqual(dataSource.email, errorEmail)
        XCTAssertEqual(dataSource.limit, 10)
        let errorPops = ErrorProps(title: "", text: "", buttonTitle: "", onTap: Command(action: {}))
        self.checkEmptyProps(props: render.props, state: .error(errorProps: errorPops))

        // Check loading state
        let loadingEmail = Email(login: "l", domain: "state")
        presenter.rerender(with: loadingEmail, errorText: nil)
        dataSource.result = .failure(MockError.error)

        XCTAssertEqual(dataSource.email, loadingEmail)
        XCTAssertEqual(dataSource.limit, 10)
        self.checkEmptyProps(props: render.props, state: .loading)
    }

    func testLoadMore() {
        let email = Email(login: "Some", domain: "test")
        let dataSource = MockDataSource()
        let render = MockRender()
        let debouncer = MockDebouncer()
        let presenter = SuggestionsPresenter(dataSource: dataSource, debouncer: debouncer)
        presenter.render = render
        presenter.start()

        presenter.rerender(with: email, errorText: nil)

        XCTAssertEqual(dataSource.email, email)
        XCTAssertEqual(dataSource.limit, 10)
        self.checkEmptyProps(props: render.props, state: .loading)
        dataSource.result = .success(SuggestionsModel(domainStatus: .available, emails: []))
        dataSource.finishLoading()

        XCTAssertEqual(render.props?.state, .normal)
        XCTAssertEqual(render.props?.loadMoreProps.isEnabled, true)

        render.props?.loadMoreProps.onLoadMore()
        XCTAssertEqual(dataSource.email, email, "email should not change")
        XCTAssertEqual(render.props?.state, .loadMoreLoading, "presenter should load more")
        XCTAssertEqual(render.props?.loadMoreProps.isEnabled, true, "Should enabled when is loadMoreLoading")
        XCTAssertNil(dataSource.limit)
        dataSource.finishLoading()
        XCTAssertEqual(render.props?.loadMoreProps.isEnabled, false)

        dataSource.completion = { _ in }
        dataSource.result = .failure(MockError.error)

        render.props?.loadMoreProps.onLoadMore()
        dataSource.finishLoading() // Should not load from dataSource
        XCTAssertEqual(dataSource.email, email, "email should not change")
        XCTAssertEqual(render.props?.state, .normal, "loadMore should load only once for one email")
        XCTAssertEqual(render.props?.loadMoreProps.isEnabled, false)

        // should loadmore when email updated
        presenter.rerender(with: email, errorText: nil)
        dataSource.result = .success(SuggestionsModel(domainStatus: .available, emails: []))
        XCTAssertEqual(render.props?.loadMoreProps.isEnabled, false)
        dataSource.finishLoading()
        XCTAssertEqual(render.props?.loadMoreProps.isEnabled, true)

        XCTAssertEqual(render.props?.loadMoreProps.isEnabled, true)
        render.props?.loadMoreProps.onLoadMore()
        XCTAssertEqual(render.props?.loadMoreProps.isEnabled, true, "Should enabled when is loadMoreLoading state")
        XCTAssertEqual(dataSource.email, email, "email should not change")
        XCTAssertEqual(render.props?.state, .loadMoreLoading, "presenter should load more")
        XCTAssertNil(dataSource.limit)
        dataSource.finishLoading()
        XCTAssertEqual(render.props?.loadMoreProps.isEnabled, false)
    }

    func testHeaderProps() {
        func checkHeaderProps(with status: SuggestionsModel.DomainStatus, shouldHeaderPropsNil: Bool) {
            let dataSource = MockDataSource()
            let render = MockRender()
            let infinityDebouncer = Debouncer(queue: .main, delay: .infinity)
            let presenter = SuggestionsPresenter(dataSource: dataSource, debouncer: infinityDebouncer)
            presenter.render = render
            presenter.start()

            let testEmail = Email(login: "test", domain: "email")
            XCTAssertEqual(dataSource.email, Email.empty)
            XCTAssertEqual(dataSource.limit, 10)
            self.checkEmptyProps(props: render.props, state: .loading)
            dataSource.result = .success(SuggestionsModel(domainStatus: status, emails: [testEmail]))
            dataSource.finishLoading()

            XCTAssertEqual(render.props?.cells.map { $0.email }, [testEmail])
            XCTAssertEqual(render.props?.domainStatus, status)
            if shouldHeaderPropsNil {
                XCTAssertNil(render.props?.headerProps)
            } else {
                XCTAssertNotNil(render.props?.headerProps)
            }
        }
        checkHeaderProps(with: .available, shouldHeaderPropsNil: true)
        checkHeaderProps(with: .notAllowed, shouldHeaderPropsNil: true)
        checkHeaderProps(with: .occupied, shouldHeaderPropsNil: false)
    }

    func testDelegate() {
        let dataSource = MockDataSource()
        let render = MockRender()
        let delegate = MockDelegate()
        let infinityDebouncer = Debouncer(queue: .main, delay: .infinity)
        let presenter = SuggestionsPresenter(dataSource: dataSource, debouncer: infinityDebouncer)
        presenter.render = render
        presenter.delegate = delegate
        presenter.start()

        XCTAssertEqual(dataSource.email, Email.empty)
        XCTAssertEqual(dataSource.limit, 10)
        self.checkEmptyProps(props: render.props, state: .loading)

        let firstEmail = Email(login: "first", domain: "email")
        let secondEmail = Email(login: "second", domain: "email")
        let thirdEmail = Email(login: "third", domain: "email")
        dataSource.result = .success(SuggestionsModel(domainStatus: .available,
                                                      emails: [firstEmail, secondEmail, thirdEmail]))
        dataSource.finishLoading()

        XCTAssertNil(delegate.email)
        render.props?.cells[0].onTap()
        XCTAssertEqual(delegate.email, firstEmail)
        render.props?.cells[1].onTap()
        XCTAssertEqual(delegate.email, secondEmail)
        render.props?.cells[2].onTap()
        XCTAssertEqual(delegate.email, thirdEmail)
    }

    func testReloadWhenTapOnErrorUpdateButton() {
        func checkReloadWhenTapOnErrorUpdateButton(isLoadMore: Bool, isNetwrokError: Bool) {
            let dataSource = MockDataSource()
            let render = MockRender()
            let delegate = MockDelegate()
            let infinityDebouncer = Debouncer(queue: .main, delay: .infinity)
            let presenter = SuggestionsPresenter(dataSource: dataSource, debouncer: infinityDebouncer)
            presenter.render = render
            presenter.delegate = delegate
            presenter.start()
            let email = Email(login: "test", domain: "email")

            XCTAssertEqual(dataSource.email, Email.empty)
            XCTAssertEqual(dataSource.limit, 10)
            self.checkEmptyProps(props: render.props, state: .loading)
            presenter.rerender(with: email, errorText: nil)

            if isLoadMore {
                dataSource.result = .success(SuggestionsModel(domainStatus: .available,
                                                              emails: []))
                dataSource.finishLoading()
                render.props?.loadMoreProps.onLoadMore()
            }

            dataSource.result = .failure(isNetwrokError ? TestError.networkError : TestError.someError)
            dataSource.finishLoading()

            let errorPops = ErrorProps(title: "", text: "", buttonTitle: "", onTap: Command(action: {}))
            self.checkEmptyProps(props: render.props, state: .error(errorProps: errorPops))

            if case let .error(props) = render.props?.state {
                props.onTap()

                XCTAssertEqual(dataSource.email, email)
                XCTAssertEqual(dataSource.limit, isLoadMore ? nil : 10)
                isNetwrokError ? XCTAssertNotNil(delegate.networkError) : XCTAssertNil(delegate.networkError)
            } else {
                XCTFail("ErrorProps should exist")
            }
        }

        checkReloadWhenTapOnErrorUpdateButton(isLoadMore: true, isNetwrokError: false)
        checkReloadWhenTapOnErrorUpdateButton(isLoadMore: false, isNetwrokError: false)
        checkReloadWhenTapOnErrorUpdateButton(isLoadMore: true, isNetwrokError: true)
        checkReloadWhenTapOnErrorUpdateButton(isLoadMore: false, isNetwrokError: true)
    }

    func testDeallocating() {
        func checkDeallocating(with state: SuggestionsProps.State) {
            let email = Email(login: "Some", domain: "test")
            let dataSource = MockDataSource()
            let render = MockRender()
            let delegate = MockDelegate()
            let debouncer = MockDebouncer()
            var presenter: SuggestionsPresenter? = SuggestionsPresenter(dataSource: dataSource, debouncer: debouncer)
            presenter?.render = render
            presenter?.delegate = delegate
            presenter?.start()

            XCTAssertEqual(dataSource.email, Email.empty)
            XCTAssertEqual(dataSource.limit, 10)
            self.checkEmptyProps(props: render.props, state: .loading)

            let firstEmail = Email(login: "first", domain: "email")
            let secondEmail = Email(login: "second", domain: "email")
            let thirdEmail = Email(login: "third", domain: "email")
            let otherEmails = (4...10).map { Email(login: "\($0)", domain: "\($0)") }
            dataSource.result = .success(SuggestionsModel(domainStatus: .available,
                                                          emails: [firstEmail, secondEmail, thirdEmail] + otherEmails))
            dataSource.finishLoading()

            switch state {
            case .loading:
                presenter?.rerender(with: email, errorText: nil)
            case .error:
                presenter?.rerender(with: email, errorText: nil)
                dataSource.result = .failure(MockError.error)
                dataSource.finishLoading()
            case .loadMoreLoading:
                render.props?.loadMoreProps.onLoadMore()
            case .normal:
                break
            }
            XCTAssertEqual(state, render.props?.state)

            weak var weakPresenter = presenter
            XCTAssertNotNil(weakPresenter)
            presenter = nil
            XCTAssertNil(weakPresenter)
        }

        let errorPops = ErrorProps(title: "", text: "", buttonTitle: "", onTap: Command(action: {}))
        checkDeallocating(with: .error(errorProps: errorPops))
        checkDeallocating(with: .loading)
        checkDeallocating(with: .normal)
        checkDeallocating(with: .loadMoreLoading)
    }

    private func checkEmptyProps(props: SuggestionsProps?, state: SuggestionsProps.State) {
        XCTAssertEqual(props?.state, state)
        XCTAssertTrue(props?.cells.isEmpty ?? false)
        XCTAssertNotNil(props?.domainStatus)
        XCTAssertNil(props?.headerProps)
    }
}

private extension SuggestionsPresenterTest {
    final class MockRender: SuggestionsRendering {
        var props: SuggestionsProps?

        func render(props: SuggestionsProps) {
            self.props = props
        }
    }

    final class MockDataSource: SuggestionsDataSource {
        var email: Email?
        var limit: Int?
        var result: Result<SuggestionsModel> = .failure(MockError.error)
        var completion: (Result<SuggestionsModel>) -> Void = { _ in }

        func loadSuggestionsFor(email: Email, limit: Int?, completion: @escaping (Result<SuggestionsModel>) -> Void) {
            self.email = email
            self.limit = limit
            self.completion = completion
        }

        func finishLoading() {
            self.completion(self.result)
        }
    }

    final class MockDelegate: SuggestionsPresenterDelegate {
        var email: Email?
        var networkError: Error?

        func suggestionsPresenter(_ suggestionsPresenter: SuggestionsPresenting, didSelectEmail email: Email) {
            self.email = email
        }

        func suggestionsPresenter(_ suggestionsPresenter: SuggestionsPresenting, didReceiveNetworkError error: Error) {
            self.networkError = error
        }
    }

    final class MockDebouncer: Debouncing {
        var performBlockCount = 0
        func debounce(block: @escaping () -> Void) {
            performBlockCount += 1
            block()
        }
    }

    enum MockError: Error {
        case error
    }
}
