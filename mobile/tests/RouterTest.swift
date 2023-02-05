//
//  RouterTest.swift
//  ExclusiveEmailTests
//
//  Created by Aleksey Makhutin on 04.08.2021.
//

import Foundation
import XCTest
import Utils
import UtilsUI
import TestUtils
import Styler
@testable import ExclusiveEmail

internal final class RouterTest: XCTestCase {
    override func setUp() {
        super.setUp()
        Styler.initializeSharedInstanceForDevelopment()
    }

    func testAnalyticDeallocating() {
        func check(flow: Flow) {
            var analytics: Analytics? = MockAnalytics()
            weak var weakAnalytics = analytics
            var router: Router? = Router(window: nil,
                                         networkPerformerForLogin: { _ in return MockNetworkPerformer() },
                                         loadMoreView: MockLoadMore(),
                                         accountDataSource: MockAccountDataSource(),
                                         flow: flow,
                                         analytics: analytics)
            var controller: UIViewController? = router?.open()
            XCTAssertNotNil(controller)
            XCTAssertNotNil(router)
            XCTAssertNotNil(analytics)
            XCTAssertNotNil(weakAnalytics)
            controller = nil
            router = nil
            analytics = nil

            XCTAssertNil(weakAnalytics)
        }

        Flow.allCases.forEach { flow in
            check(flow: flow)
        }
    }

    func testAnalyticDeallocatingWithSuccessVC() {
        func check(flow: Flow) {
            weak var weakAnalytics: Analytics?
            autoreleasepool {
                var analytics: Analytics? = MockAnalytics()
                weakAnalytics = analytics
                var router: Router? = Router(window: nil,
                                             networkPerformerForLogin: { _ in return MockNetworkPerformer() },
                                             loadMoreView: MockLoadMore(),
                                             accountDataSource: MockAccountDataSource(),
                                             flow: .normal,
                                             analytics: analytics)
                var delegate: MockDelegate? = MockDelegate()
                router?.delegate = delegate
                var controller: UIViewController? = router?.open()
                router?.openRegistrationSuccess(with: Email(login: "login", domain: "domain"), for: "login")
                XCTAssertNotNil(delegate?.controller, "Success view controller should exist")
                XCTAssertNotNil(controller)
                XCTAssertNotNil(router)
                XCTAssertNotNil(analytics)
                XCTAssertNotNil(weakAnalytics)
                delegate = nil
                controller = nil
                router = nil
                analytics = nil
            }
            XCTAssertNil(weakAnalytics)
        }

        Flow.allCases.forEach { flow in
            check(flow: flow)
        }
    }
}

private extension RouterTest {
    final class MockNetworkPerformer: NetworkPerformer {
        func register(email: Email, completion: @escaping (Result<Void>) -> Void) {
        }

        func loadSuggestionsFor(email: Email, limit: Int?, completion: @escaping (Result<SuggestionsModel>) -> Void) {
        }

        func loadZeroSuggest(completion: @escaping (Result<ZeroSuggestionModel>) -> Void) {
        }

        func loadStatus(completion: @escaping (Result<DomainStatusModel>) -> Void) {
        }

        func deleteExclusiveEmail(completion: @escaping (Result<Void>) -> Void) {
        }

        func changeExlusiveEmailLogin(_ login: String, completion: @escaping (Result<Void>) -> Void) {
        }
    }

    final class MockAnalytics: Analytics {
        func openExclusiveEmail(_ service: Service) {
        }

        func closeExclusiveEmail(_ service: Service) {
        }

        func clickExclusiveEmailService(_ service: Service, clickEvent: ClickEvent) {
        }

        func clickSuggestExclusiveEmail(_ position: Int) {
        }

        func showExclusiveEmail(_ status: ExclusiveEmailStatusModel.Status) {
        }
    }

    final class MockLoadMore: UIView, LoadMoreView {
        func render(props: LoadMoreProps) {
        }

        func endRefreshing() {
        }
    }

    final class MockDelegate: RouterDelegate {
        func routerDidChangeExclusiveEmail(_ router: Router) {
        }

        var controller: UIViewController?

        func router(_ router: Router, didConnectExclusiveEmailFor login: String) {
        }
        
        func router(_ router: Router, didRequestOpenSubscriptionsFor login: String, onPurchased: @escaping () -> Void) {
        }

        func router(_ router: Router, didRequestShowErrorNotificationWithText text: String) {
        }

        func router(_ router: Router, didRequestOpenControllerWithStub controller: UIViewController, for login: String) {
            self.controller = controller
        }

        func router(_ router: Router, didRequestOpenURL urlByLanguage: [LanguageKind: String]) {
        }
    }

    final class MockAccount: AccountInfo {
        var avatar: AvatarViewModel?
        let login = "test"
        let defaultEmail = "someEmail"
        var hasSubscription = false
        let hasExclusiveEmail = false
    }

    final class MockAccountDataSource: AccountDataSource {
        var hasSubscription = false {
            didSet {
                (self.selectedAccount as! MockAccount).hasSubscription = self.hasSubscription
            }
        }

        var selectedAccount: AccountInfo = MockAccount()

        lazy var availableAccounts: [AccountInfo] = [self.selectedAccount]

        weak var delegate: AccountDataSourceDelegate?
    }
}
