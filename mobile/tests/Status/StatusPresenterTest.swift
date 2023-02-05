//
//  StatusPresenterTest.swift
//  ExclusiveEmailTests
//
//  Created by Aleksey Makhutin on 23.06.2021.
//

import Foundation
import XCTest
import Utils
import UtilsUI
import TestUtils
import Styler
import NetworkLayer
@testable import ExclusiveEmail

internal final class StatusPresenterTest: XCTestCase {
    override func setUp() {
        super.setUp()
        Styler.initializeSharedInstanceForDevelopment()
    }

    func testStartAndStates() {
        let render = MockRender()
        let router = MockRouter()
        let networkPerformer = MockNetworkPerformer()
        let accountDataSource = MockAccountDataSource()
        let presenter = StatusPresenter(render: render,
                                        dataSource: networkPerformer,
                                        router: router,
                                        accountDataSource: accountDataSource)
        presenter.start()

        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.state, .loading)
        render.props = nil

        presenter.rerender()
        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.state, .loading)
    }

    func testWaitForReload() {
        let render = MockRender()
        let router = MockRouter()
        let networkPerformer = MockNetworkPerformer()
        let accountDataSource = MockAccountDataSource()
        let presenter = StatusPresenter(render: render,
                                        dataSource: networkPerformer,
                                        router: router,
                                        accountDataSource: accountDataSource)
        presenter.start()

        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.state, .loading)
        render.props = nil
        networkPerformer.finishLoading(with: .success(.notFound))

        presenter.waitForReloadStatus()
        presenter.rerender()
        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.state, .loading)
    }

    func testBuildPropsWithNotFoundEmailStatus() {
        let render = MockRender()
        let router = MockRouter()
        let networkPerformer = MockNetworkPerformer()
        let accountDataSource = MockAccountDataSource()
        let presenter = StatusPresenter(render: render,
                                        dataSource: networkPerformer,
                                        router: router,
                                        accountDataSource: accountDataSource)
        presenter.start()

        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.state, .loading)
        networkPerformer.finishLoading(with: .success(.notFound))
        
        switch render.props?.state {
        case let .loaded(props: props):
            XCTAssertFalse(props.isNotAvailable)
            XCTAssertTrue(!props.text.isEmpty)
        default:
            XCTFail("Props should exist")
        }
    }

    func testBuildPropsWithDifferentEmailStatus() {
        func check(status: ExclusiveEmailStatusModel.Status, isExclusiveEmailEqualToDefaultEmail: Bool = false) {
            let email = Email(login: "test", domain: "domain")
            let model = ExclusiveEmailStatusModel(email: email,
                                                  status: status,
                                                  registerAllowed: false,
                                                  nextAvailableRegisterDate: Date(timeIntervalSince1970: 1_626_685_776))
            let render = MockRender()
            let router = MockRouter()
            let networkPerformer = MockNetworkPerformer()
            let accountDataSource = MockAccountDataSource()
            accountDataSource.email = isExclusiveEmailEqualToDefaultEmail ? email.description : "someEmail"
            let presenter = StatusPresenter(render: render,
                                            dataSource: networkPerformer,
                                            router: router,
                                            accountDataSource: accountDataSource)
            presenter.start()

            XCTAssertNotNil(render.props)
            XCTAssertEqual(render.props?.state, .loading)
            networkPerformer.finishLoading(with: .success(.found(model)))

            switch render.props?.state {
            case .loaded(props: let props):
                switch status {
                case .deleted:
                    XCTAssertFalse(props.isNotAvailable)
                case .emailNotAvailable:
                    if case .show(let buttonProps) = props.buttonState {
                        buttonProps.onButtonTap()
                        XCTAssertTrue(router.isSearchOpen)
                    } else {
                        XCTFail("button props should exist")
                    }
                    XCTAssertTrue(props.isNotAvailable)
                case .subscriptionExpired:
                    if case .show(let buttonProps) = props.buttonState {
                        buttonProps.onButtonTap()
                        XCTAssertTrue(router.isAboutDeletedOpen)
                    } else {
                        XCTFail("button props should exist")
                    }
                    XCTAssertFalse(props.isNotAvailable)
                case .registered:
                    if isExclusiveEmailEqualToDefaultEmail {
                        XCTAssertEqual(props.text, "Enabled and used as your default email address.")
                    } else {
                        XCTAssertEqual(props.text, "Enabled but not used as your default email address for sending messages.")
                    }
                    XCTAssertFalse(props.isNotAvailable)
                    XCTAssertEqual(props.buttonState, .hide)
                default:
                    XCTAssertFalse(props.isNotAvailable)
                    XCTAssertEqual(props.buttonState, .hide)
                }
                XCTAssertTrue(!props.text.isEmpty)
            default:
                XCTFail("Props should exist")
            }
        }

        check(status: .pending)
        check(status: .registered, isExclusiveEmailEqualToDefaultEmail: false)
        check(status: .registered, isExclusiveEmailEqualToDefaultEmail: true)
        check(status: .deleted)
        check(status: .infoNotAvailable)
        check(status: .emailNotAvailable)
        check(status: .subscriptionExpired)
    }

    func testDifferentState() {
        func check(state: StatusProps.State, withNetworkError networkError: Bool = false) {
            let render = MockRender()
            let router = MockRouter()
            let networkPerformer = MockNetworkPerformer()
            let accountDataSource = MockAccountDataSource()
            let presenter = StatusPresenter(render: render,
                                            dataSource: networkPerformer,
                                            router: router,
                                            accountDataSource: accountDataSource)
            presenter.start()

            XCTAssertNotNil(render.props)
            XCTAssertEqual(render.props?.state, .loading)

            switch state {
            case .error:
                if networkError {
                    networkPerformer.finishLoading(with: .failure(TestError.networkError))
                    XCTAssertEqual(router.error, .network)
                } else {
                    networkPerformer.finishLoading(with: .failure(TestError.someError))
                }
                switch render.props?.state {
                case .error(props: let errorProps):
                    errorProps.onTap()
                default:
                    break
                }
                XCTAssertEqual(render.props?.state, .loading)
            case .loaded(props: let props):
                let model = ExclusiveEmailStatusModel(email: props.email,
                                                      status: .pending,
                                                      registerAllowed: false,
                                                      nextAvailableRegisterDate: Date())
                networkPerformer.finishLoading(with: .success(.found(model)))
                XCTAssertNotNil(render.props)
                if case .loaded(let loadedProps) = render.props?.state {
                    XCTAssertEqual(loadedProps.email, props.email)
                } else {
                    XCTFail("loaded props should exist")
                }
            default:
                break
            }
        }

        check(state: .error(props: ErrorProps.empty))
        check(state: .loaded(props: StatusDescriptionProps(iconAndTint: (UIImage(), UIColor.red),
                                                           email: Email(login: "test", domain: "loaded"),
                                                           text: "text",
                                                           isNotAvailable: false,
                                                           rightBarButtonType: .nothing,
                                                           buttonState: .hide)))
    }

    func testCheckRightBarButtonTap() {
        func check(status: ExclusiveEmailStatusModel.Status) {
            let render = MockRender()
            let router = MockRouter()
            let networkPerformer = MockNetworkPerformer()
            let accountDataSource = MockAccountDataSource()
            let presenter = StatusPresenter(render: render,
                                            dataSource: networkPerformer,
                                            router: router,
                                            accountDataSource: accountDataSource)
            presenter.start()

            XCTAssertNotNil(render.props)
            XCTAssertEqual(render.props?.state, .loading)

            let email = Email(login: "test", domain: "more")
            let date = Date()
            networkPerformer.finishLoading(with: .success(.found(ExclusiveEmailStatusModel(email: email,
                                                                                           status: status,
                                                                                           registerAllowed: false,
                                                                                           nextAvailableRegisterDate: date))))
            render.props?.onRightButtonTap()
            switch status {
            case .registered:
                XCTAssertEqual(router.email, email)
                XCTAssertTrue(router.isMoreOpen)
                XCTAssertFalse(router.isDeleteOpen)
                XCTAssertEqual(router.nextAvailableRegisterDate, date)
            case .subscriptionExpired:
                XCTAssertEqual(router.email, email)
                XCTAssertFalse(router.isMoreOpen)
                XCTAssertTrue(router.isDeleteOpen)
                XCTAssertEqual(router.nextAvailableRegisterDate, date)
            default:
                XCTAssertFalse(router.isMoreOpen)
                XCTAssertFalse(router.isDeleteOpen)
            }
        }

        check(status: .registered)
        check(status: .registered)
        check(status: .pending)
        check(status: .emailNotAvailable)
        check(status: .infoNotAvailable)
        check(status: .subscriptionExpired)
        check(status: .deleted)
    }

    func testDeallocating() {
        let render = MockRender()
        let router = MockRouter()
        let networkPerformer = MockNetworkPerformer()
        let accountDataSource = MockAccountDataSource()
        var presenter: StatusPresenter? = StatusPresenter(render: render,
                                                          dataSource: networkPerformer,
                                                          router: router,
                                                          accountDataSource: accountDataSource)
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

private extension StatusPresenterTest {
    final class MockRender: StatusRendering {
        var props: StatusProps?
        func render(props: StatusProps) {
            self.props = props
        }
    }

    final class MockRouter: StatusRouting {
        var error: Router.Error?
        var email: Email?
        var nextAvailableRegisterDate: Date?
        var isInfoOpen = false
        var isSearchOpen = false
        var isAboutDeletedOpen = false
        var isMoreOpen = false
        var isDeleteOpen = false

        func show(error: Router.Error) {
            self.error = error
        }

        func showMore(with email: Email, nextAvailableRegisterDate: Date) {
            self.email = email
            self.nextAvailableRegisterDate = nextAvailableRegisterDate
            self.isMoreOpen = true
        }

        func openDelete(with email: Email, nextAvailableRegisterDate: Date) {
            self.email = email
            self.nextAvailableRegisterDate = nextAvailableRegisterDate
            self.isDeleteOpen = true
        }

        func openStatusInfo() {
            self.isInfoOpen = true
        }

        func openSearch() {
            self.isSearchOpen = true
        }

        func openAboutDeleted() {
            self.isAboutDeletedOpen = true
        }
    }

    final class MockNetworkPerformer: StatusDataSource {
        var completion: ((Result<DomainStatusModel>) -> Void)?

        func finishLoading(with result: Result<DomainStatusModel>) {
            self.completion?(result)
        }

        func loadStatus(completion: @escaping (Result<DomainStatusModel>) -> Void) {
            self.completion = completion
        }
    }

    final class MockAccount: AccountInfo {
        var avatar: AvatarViewModel?
        let login = "test"
        let hasSubscription = true
        let hasExclusiveEmail = true

        var defaultEmail: String = "default"
    }

    final class MockAccountDataSource: AccountDataSource {
        var email: String? {
            didSet {
                (self.selectedAccount as! MockAccount).defaultEmail = self.email ?? "someEmail"
            }
        }

        var selectedAccount: AccountInfo = MockAccount()

        var availableAccounts: [AccountInfo] = []

        weak var delegate: AccountDataSourceDelegate?
    }
}

extension StatusProps.State: Equatable {
    public static func == (lhs: StatusProps.State, rhs: StatusProps.State) -> Bool {
        switch (lhs, rhs) {
        case (.loaded, .loaded), (.error, .error), (.loading, .loading):
            return true
        default:
            return false
        }
    }
}

extension StatusDescriptionProps.ButtonState: Equatable {
    public static func == (lhs: StatusDescriptionProps.ButtonState, rhs: StatusDescriptionProps.ButtonState) -> Bool {
        switch (lhs, rhs) {
        case (.hide, hide), (.show, .show):
            return true
        default:
            return false
        }
    }
}
