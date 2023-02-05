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

internal final class ConfirmationPresenterTest: XCTestCase {
    private var networkPeerformerByLogin = [String: MockNetworkPerformer]()

    override func setUp() {
        super.setUp()
        self.networkPeerformerByLogin = [:]
    }

    func testStartAndRerender() {
        func check(flow: Flow) {
            let email = Email(login: "test", domain: "domain")
            let render = MockRender()
            let router = MockRouter()
            let accountDataSource = MockAccountDataSource(availableAccounts: [MockAccount(login: "test", hasSubscription: false)])
            let presenter = ConfirmationPresenter(render: render,
                                                  email: email,
                                                  networkPerformerByLogin: self.networkPerformerByLogin(login:),
                                                  accountDataSource: accountDataSource,
                                                  router: router,
                                                  flow: flow)
            presenter.start()

            XCTAssertEqual(render.props?.email, email)
            XCTAssertEqual(render.props?.buttonState, .normal)

            render.props = nil
            presenter.rerender()

            XCTAssertEqual(render.props?.email, email)
            XCTAssertEqual(render.props?.buttonState, .normal)
        }

        check(flow: .normal)
        check(flow: .selectableAccounts)
    }

    func testSelectAccount() {
        let email = Email(login: "test1", domain: "domain")
        let render = MockRender()
        let router = MockRouter()

        let accounts = [
            MockAccount(login: "test1", hasSubscription: false),
            MockAccount(login: "test2", hasSubscription: false),
            MockAccount(login: "test3", hasSubscription: false),
            MockAccount(login: "test4", hasSubscription: false)
        ]

        let accountDataSource = MockAccountDataSource(availableAccounts: accounts)
        let presenter = ConfirmationPresenter(render: render,
                                              email: email,
                                              networkPerformerByLogin: self.networkPerformerByLogin(login:),
                                              accountDataSource: accountDataSource,
                                              router: router,
                                              flow: .selectableAccounts)
        presenter.start()

        (0..<accounts.count).forEach { offset in
            let accountProps = render.props?.accounts.enumerated().first { $0.offset == offset }?.element
            render.props = nil
            accountProps?.onTap()
            XCTAssertNotNil(render.props)
            XCTAssertTrue(render.props?.accounts.enumerated().first { $0.offset == offset }?.element.isSelected == true)
        }
    }

    func testOnTapAndButtonState() {
        func checkFinishWith(result: Result<Void>, hasSubscription: Bool, flow: Flow) {
            let email = Email(login: "test1", domain: "domain")
            let render = MockRender()
            let router = MockRouter()
            let delegate = Delegate()

            let selectableAccounts = [
                MockAccount(login: "test1", hasSubscription: hasSubscription),
                MockAccount(login: "test2", hasSubscription: hasSubscription),
                MockAccount(login: "test3", hasSubscription: hasSubscription),
                MockAccount(login: "test4", hasSubscription: hasSubscription)
            ]
            let accounts = flow == .selectableAccounts ? selectableAccounts : [MockAccount(login: "test1", hasSubscription: hasSubscription)]

            let accountDataSource = MockAccountDataSource(availableAccounts: accounts)
            let presenter = ConfirmationPresenter(render: render,
                                                  email: email,
                                                  networkPerformerByLogin: self.networkPerformerByLogin(login:),
                                                  accountDataSource: accountDataSource,
                                                  router: router,
                                                  flow: flow)
            presenter.delegate = delegate
            presenter.start()

            XCTAssertEqual(render.props?.email, email)
            XCTAssertEqual(render.props?.buttonState, .normal)
            let oldButtonText = render.props?.buttonText

            let checkSubscriptionFlow: (String) -> Void = { login in
                XCTAssertEqual(render.props?.buttonState, .loading)
                XCTAssertNotEqual(render.props?.buttonText, oldButtonText)
                self.networkPerformerByLogin(login: login)?.finish(with: result)

                result.onError { error in
                    if error.yo_isNetworkError {
                        XCTAssertEqual(router.error, .network)
                    } else {
                        XCTAssertEqual(router.error, .confirmation)
                    }
                }
                result.onValue { _ in
                    XCTAssertEqual(delegate.didConnectForLogin, login)
                    XCTAssertEqual(router.emailForRegistration, email)
                    XCTAssertEqual(router.loginForRegistration, login)
                }
            }

            let checkWithoutSubscriptionFlow: (String) -> Void = { login in
                XCTAssertEqual(router.subscriptionDidOpenForLogin, login)
            }

            accounts.enumerated().forEach { offset, account in
                // select account
                render.props?.accounts.enumerated().first { $0.offset == offset }?.element.onTap()
                XCTAssertTrue(render.props?.accounts.enumerated().first { $0.offset == offset }?.element.isSelected == true)
                render.props?.onTap()
                hasSubscription ? checkSubscriptionFlow(account.login) : checkWithoutSubscriptionFlow(account.login)
            }
        }

        [Result<Void>.failure(TestError.someError), .failure(TestError.networkError), .success(())].forEach { result in
            checkFinishWith(result: result, hasSubscription: true, flow: .normal)
            checkFinishWith(result: result, hasSubscription: false, flow: .normal)
            checkFinishWith(result: result, hasSubscription: true, flow: .selectableAccounts)
            checkFinishWith(result: result, hasSubscription: false, flow: .selectableAccounts)
        }
    }

    func testTapOnLink() {
        func check(flow: Flow) {
            let email = Email(login: "test", domain: "domain")
            let render = MockRender()
            let router = MockRouter()
            let accountDataSource = MockAccountDataSource(availableAccounts: [MockAccount(login: "test", hasSubscription: false)])
            let presenter = ConfirmationPresenter(render: render,
                                                  email: email,
                                                  networkPerformerByLogin: self.networkPerformerByLogin(login:),
                                                  accountDataSource: accountDataSource,
                                                  router: router,
                                                  flow: flow)
            presenter.start()

            XCTAssertEqual(render.props?.email, email)
            XCTAssertEqual(render.props?.buttonState, .normal)

            render.props?.onLinkTap()

            XCTAssertEqual(render.props?.email, email)
            XCTAssertEqual(render.props?.buttonState, .normal)
            XCTAssertTrue(router.isLegalOpen)
        }

        check(flow: .normal)
        check(flow: .selectableAccounts)
    }

    func testDeallocating() {
        func check(flow: Flow) {
            let email = Email(login: "test", domain: "domain")
            let render = MockRender()
            let router = MockRouter()
            let accountDataSource = MockAccountDataSource(availableAccounts: [MockAccount(login: "test", hasSubscription: false)])
            var presenter: ConfirmationPresenter? = ConfirmationPresenter(render: render,
                                                                          email: email,
                                                                          networkPerformerByLogin: self.networkPerformerByLogin(login:),
                                                                          accountDataSource: accountDataSource,
                                                                          router: router,
                                                                          flow: flow)
            presenter?.start()

            XCTAssertEqual(render.props?.email, email)
            XCTAssertEqual(render.props?.buttonState, .normal)

            render.props = nil
            presenter?.rerender()

            XCTAssertEqual(render.props?.email, email)
            XCTAssertEqual(render.props?.buttonState, .normal)

            weak var weakPresenter = presenter
            XCTAssertNotNil(weakPresenter)
            presenter = nil
            XCTAssertNil(weakPresenter)
        }

        check(flow: .normal)
        check(flow: .selectableAccounts)
    }

    private func networkPerformerByLogin(login: String) -> MockNetworkPerformer? {
        if let neetworkPerformer = self.networkPeerformerByLogin[login] {
            return neetworkPerformer
        }
        self.networkPeerformerByLogin[login] = MockNetworkPerformer()
        return self.networkPeerformerByLogin[login]
    }
}

private extension ConfirmationPresenterTest {
    final class MockRender: ConfirmationRendering {
        var props: ConfirmationProps?
        func render(props: ConfirmationProps) {
            self.props = props
        }
    }

    final class MockRouter: ConfirmationRouting {
        var isLegalOpen = false
        var subscriptionDidOpenForLogin: String?
        var emailForRegistration: Email?
        var loginForRegistration: String?
        var error: Router.Error?

        func openRegistrationSuccess(with email: Email, for login: String) {
            self.emailForRegistration = email
            self.loginForRegistration = login
        }

        func show(error: Router.Error) {
            self.error = error
        }

        func openSubscriptions(for login: String) {
            self.subscriptionDidOpenForLogin = login
        }

        func openLegal() {
            self.isLegalOpen = true
        }
    }

    final class MockNetworkPerformer: ConfirmationNetworkPerformer {
        var completion: ((Result<Void>) -> Void)?
        func register(email: Email, completion: @escaping (Result<Void>) -> Void) {
            self.completion = completion
        }

        func finish(with result: Result<Void>) {
            self.completion?(result)
            self.completion = nil
        }
    }

    final class Delegate: ConfirmationDelegate {
        var didConnectForLogin: String?

        func confirmationPresenterDidConnectExclusiveEmail(_ confirmationPresenter: ConfirmationPresenter, for login: String) {
            self.didConnectForLogin = login
        }
    }

    final class MockAccount: AccountInfo {
        var avatar: AvatarViewModel?
        let login: String
        let defaultEmail: String = ""
        let hasExclusiveEmail = false

        var hasSubscription: Bool

        init(login: String, hasSubscription: Bool) {
            self.login = login
            self.hasSubscription = hasSubscription
        }
    }

    final class MockAccountDataSource: AccountDataSource {
        var selectedAccount: AccountInfo

        var availableAccounts: [AccountInfo]

        weak var delegate: AccountDataSourceDelegate?

        func set(hasSubscription: Bool, for login: String) {
            let account = self.availableAccounts.first { $0.login == login }
            (account as! MockAccount).hasSubscription = hasSubscription
        }

        init(availableAccounts: [AccountInfo]) {
            self.availableAccounts = availableAccounts
            self.selectedAccount = availableAccounts.first!
        }
    }

    enum MockError: Error {
        case error
    }
}
