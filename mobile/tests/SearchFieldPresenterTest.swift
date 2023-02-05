//
//  SearchFieldPresenterTest.swift
//  ExclusiveEmailTests
//
//  Created by Aleksey Makhutin on 23.06.2021.
//

import Foundation
import XCTest
import Utils
@testable import ExclusiveEmail

internal final class SearchFieldPresenterTest: XCTestCase {
    func testStartAndStates() {
        let render = MockRender()
        let presenter = SearchFieldPresenter()

        presenter.render = render
        presenter.start()
        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.state, .normal, "First state should normal")

        func rerender(state: SearchFieldProps.State) {
            presenter.rerender(state: .error)
            XCTAssertNotNil(render.props)
            XCTAssertEqual(render.props?.state, .error)
        }

        rerender(state: .normal)
        rerender(state: .error)
        rerender(state: .domainError)
    }

    func testDidChangeEmailDelegate() {
        let render = MockRender()
        let delegate = MockDelegate()
        let presenter = SearchFieldPresenter()
        presenter.render = render
        presenter.delegate = delegate
        presenter.start()

        let randomText = ["Foo", "Bar", "Baz", ""]
        let repeatCount = 20
        for _ in 0..<repeatCount {
            let email = Email(login: randomText.randomElement() ?? "", domain: randomText.randomElement() ?? "")
            render.props?.onEmailChange(email)
            XCTAssertEqual(delegate.email.login, email.login)
            XCTAssertEqual(delegate.email.domain, email.domain, "presenter should not add tld")
        }
    }

    func testDeallocating() {
        let render = MockRender()
        let delegate = MockDelegate()
        var presenter: SearchFieldPresenter? = SearchFieldPresenter()
        presenter?.render = render
        presenter?.delegate = delegate

        presenter?.start()
        render.props?.onEmailChange(Email(login: "test", domain: "deallocation"))

        weak var weakPresenter = presenter
        XCTAssertNotNil(weakPresenter)
        presenter = nil
        XCTAssertNil(weakPresenter)
    }
}

private extension SearchFieldPresenterTest {
    final class MockRender: SearchFieldRendering {
        var props: SearchFieldProps?

        func render(props: SearchFieldProps) {
            self.props = props
        }
    }

    final class MockDelegate: SearchFieldPresenterDelegate {
        var email: Email = .empty

        func searchFieldPresenter(_ searchFieldPresenter: SearchFieldPresenting, emailDidChange email: Email) {
            XCTAssertTrue(Thread.isMainThread)
            self.email = email
        }
    }
}
