//
//  ConfirmationPresenterTest.swift
//  ExclusiveEmailTests
//
//  Created by Aleksey Makhutin on 07.07.2021.
//

import Foundation
import XCTest
import Utils
import UtilsUI
import NetworkLayer
@testable import ExclusiveEmail

internal final class PromoPresenterTest: XCTestCase {
    func testStartAndRerender() {
        let render = MockRender()
        let router = MockRouter()
        let dataSource = MockDataSource()
        let accountDataSource = MockAccountDataSource()
        let presenter = PromoPresenter(render: render,
                                       dataSource: dataSource,
                                       accountDataSource: accountDataSource,
                                       router: router)
        presenter.start()

        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.state, .loading)

        render.props = nil
        presenter.rerender()

        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.state, .loading)
    }

    func testHasSubscriptionSelectedAccount() {
        let render = MockRender()
        let router = MockRouter()
        let dataSource = MockDataSource()
        let accountDataSource = MockAccountDataSource()
        let presenter = PromoPresenter(render: render,
                                       dataSource: dataSource,
                                       accountDataSource: accountDataSource,
                                       router: router)
        presenter.start()

        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.state, .loading)
        XCTAssertEqual(accountDataSource.selectedAccount.hasSubscription, render.props?.hasSubscription)

        (0..<2).forEach { _ in
            render.props = nil
            accountDataSource.hasSubscription.toggle()
            presenter.rerender()

            XCTAssertNotNil(render.props)
            XCTAssertEqual(render.props?.state, .loading)
            XCTAssertEqual(accountDataSource.selectedAccount.hasSubscription, render.props?.hasSubscription)
        }
    }

    func testStates() {
        func check(state: PromoProps.State, withNetworkError networkError: Bool = false) {
            let render = MockRender()
            let router = MockRouter()
            let dataSource = MockDataSource()
            let accountDataSource = MockAccountDataSource()
            let presenter = PromoPresenter(render: render,
                                           dataSource: dataSource,
                                           accountDataSource: accountDataSource,
                                           router: router)
            presenter.start()

            XCTAssertNotNil(render.props)
            XCTAssertEqual(render.props?.state, .loading)

            switch state {
            case .error:
                if networkError {
                    dataSource.finish(with: .failure(TestError.networkError))
                    XCTAssertEqual(router.error, .network)
                    XCTAssertEqual(render.props?.state, .error(props: ErrorProps.empty))
                } else {
                    dataSource.finish(with: .failure(TestError.someError))
                    XCTAssertEqual(render.props?.state, .error(props: ErrorProps.empty))
                }
            case .loaded(email: let email):
                dataSource.finish(with: .success(ZeroSuggestionModel(email: email)))
                XCTAssertEqual(render.props?.state, .loaded(email: email))
            default:
                break
            }
        }

        check(state: .error(props: ErrorProps.empty), withNetworkError: false)
        check(state: .error(props: ErrorProps.empty), withNetworkError: true)
        check(state: .loading)
        check(state: .loaded(email: Email(login: "test", domain: "email")))
    }

    func testCheckOnInfoTap() {
        let render = MockRender()
        let router = MockRouter()
        let dataSource = MockDataSource()
        let accountDataSource = MockAccountDataSource()
        let presenter = PromoPresenter(render: render,
                                       dataSource: dataSource,
                                       accountDataSource: accountDataSource,
                                       router: router)
        presenter.start()

        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.state, .loading)

        render.props?.onInfoActionTap()
        XCTAssertTrue(router.isInfoOpen)
    }

    func testCheckOnExtraTap() {
        let render = MockRender()
        let router = MockRouter()
        let dataSource = MockDataSource()
        let accountDataSource = MockAccountDataSource()
        let presenter = PromoPresenter(render: render,
                                       dataSource: dataSource,
                                       accountDataSource: accountDataSource,
                                       router: router)
        presenter.start()

        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.state, .loading)
        dataSource.finish(with: .success(ZeroSuggestionModel(email: .empty)))
        render.props?.onExtraActionTap()
        XCTAssertTrue(router.isSearchOpen)
    }

    func testCheckOnActionTap() {
        func check(state: PromoProps.State) {
            let render = MockRender()
            let router = MockRouter()
            let dataSource = MockDataSource()
            let accountDataSource = MockAccountDataSource()
            let presenter = PromoPresenter(render: render,
                                           dataSource: dataSource,
                                           accountDataSource: accountDataSource,
                                           router: router)
            presenter.start()

            XCTAssertNotNil(render.props)
            XCTAssertEqual(render.props?.state, .loading)

            switch state {
            case .error:
                dataSource.finish(with: .failure(TestError.someError))
                switch render.props?.state {
                case .error(props: let errorProps):
                    errorProps.onTap()
                default:
                    break
                }
                XCTAssertNil(router.email)
                XCTAssertEqual(render.props?.state, .loading)
            case .loaded(email: let email):
                dataSource.finish(with: .success(ZeroSuggestionModel(email: email)))
                XCTAssertEqual(render.props?.state, .loaded(email: email))

                render.props?.onActionTap()
                XCTAssertEqual(router.email, email)
                XCTAssertEqual(render.props?.state, .loaded(email: email))
            default:
                break
            }
        }

        check(state: .loaded(email: Email(login: "test", domain: "action")))
        check(state: .error(props: ErrorProps.empty))
    }

    func testDeallocating() {
        let render = MockRender()
        let router = MockRouter()
        let dataSource = MockDataSource()
        let accountDataSource = MockAccountDataSource()
        var presenter: PromoPresenter? = PromoPresenter(render: render,
                                                        dataSource: dataSource,
                                                        accountDataSource: accountDataSource,
                                                        router: router)
        presenter?.start()

        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.state, .loading)

        render.props = nil
        presenter?.rerender()

        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.state, .loading)

        weak var weakPresenter = presenter
        XCTAssertNotNil(weakPresenter)
        presenter = nil
        XCTAssertNil(weakPresenter)
    }
}

private extension PromoPresenterTest {
    final class MockRender: PromoRendering {
        var props: PromoProps?
        func render(props: PromoProps) {
            self.props = props
        }
    }

    final class MockRouter: PromoRouting {
        var email: Email?
        var error: Router.Error?
        var isSearchOpen = false
        var isInfoOpen = false

        func openConfirmation(with email: Email) {
            self.email = email
        }

        func openSearch() {
            self.isSearchOpen = true
        }

        func openInfo() {
            self.isInfoOpen = true
        }

        func show(error: Router.Error) {
            self.error = error
        }
    }

    final class MockDataSource: PromoDataSource {
        private var completion: ((Result<ZeroSuggestionModel>) -> Void)?

        func finish(with result: Result<ZeroSuggestionModel>) {
            self.completion?(result)
        }

        func loadZeroSuggest(completion: @escaping (Result<ZeroSuggestionModel>) -> Void) {
            self.completion = completion
        }
    }

    final class MockAccount: AccountInfo {
        var avatar: AvatarViewModel?
        let login = "test"
        let defaultEmail = "default"
        var hasSubscription = false
        var hasExclusiveEmail = false
    }

    final class MockAccountDataSource: AccountDataSource {
        var hasSubscription = false {
            didSet {
                (self.selectedAccount as! MockAccount).hasSubscription = self.hasSubscription
            }
        }
        var selectedAccount: AccountInfo = MockAccount()

        var availableAccounts: [AccountInfo] = []

        weak var delegate: AccountDataSourceDelegate?
    }
}

extension PromoProps.State: Equatable {
    public static func == (lhs: PromoProps.State, rhs: PromoProps.State) -> Bool {
        switch (lhs, rhs) {
        case (.loading, .loading):
            return true
        case (.loaded(let lEmail), .loaded(let rEmail)):
            return lEmail == rEmail
        case (.error, .error):
            return true
        default:
            return false
        }
    }
}

extension ErrorProps {
    static let empty = ErrorProps(title: "", text: "", buttonTitle: "", onTap: Command(action: {}))
}
