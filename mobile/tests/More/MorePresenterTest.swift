//
//  MorePresenterTest.swift
//  Filters
//
//  Created by Aleksey Makhutin on 25.11.2021.
//

import Foundation
import XCTest
import Utils
import NetworkLayer
@testable import Filters

internal final class MorePresenterTest: XCTestCase {
    func testStartAndRender() {
        let render = MockRender()
        let networkPerformer = MockNetworkPerformer()
        let router = MockRouter()
        let rule = RuleModel.newRule
        let presenter = MorePresenter(render: render,
                                      rule: rule,
                                      networkPerformer: networkPerformer,
                                      router: router)
        presenter.start()
        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.state, .normal)
    }

    func testOnSwitchTapAndTitleCorrect() {
        func check(isEnabled: Bool, result: Result<Void>) {
            let render = MockRender()
            let networkPerformer = MockNetworkPerformer()
            let router = MockRouter()
            let rule = RuleModel.newRule.modelBySetting(value: \.isEnabled, to: isEnabled)
            let presenter = MorePresenter(render: render,
                                          rule: rule,
                                          networkPerformer: networkPerformer,
                                          router: router)
            presenter.start()
            XCTAssertNotNil(render.props)
            XCTAssertEqual(render.props?.switchTitle, isEnabled ? "Disable" : "Enable")

            render.props?.onSwitchTap()
            XCTAssertEqual(networkPerformer.enable, !isEnabled)
            XCTAssertNotNil(networkPerformer.completion)
            XCTAssertEqual(render.props?.state, .loading)
            networkPerformer.finichLoading(result: result)

            result.onValue { _ in
                XCTAssertEqual(render.props?.switchTitle, isEnabled ? "Enable" : "Disable")
                XCTAssertEqual(render.props?.state, .normal)
                XCTAssertTrue(router.isClosed)
            }
            result.onError { error in
                XCTAssertEqual(render.props?.switchTitle, isEnabled ? "Disable" : "Enable")
                XCTAssertEqual(render.props?.state, .normal)
                XCTAssertEqual(router.error, error.yo_isNetworkError ? .network : .more)
            }
        }
        [Result<Void>.success(()), .failure(TestError.someError), .failure(TestError.networkError)].forEach { result in
            check(isEnabled: true, result: result)
            check(isEnabled: false, result: result)
        }
    }

    func testOnDeleteTap() {
        func check(result: Result<Void>) {
            let render = MockRender()
            let networkPerformer = MockNetworkPerformer()
            let router = MockRouter()
            let rule = RuleModel.newRule
            let presenter = MorePresenter(render: render,
                                          rule: rule,
                                          networkPerformer: networkPerformer,
                                          router: router)
            presenter.start()
            XCTAssertNotNil(render.props)

            render.props?.onDeleteTap()
            XCTAssertNotNil(networkPerformer.completion)
            XCTAssertEqual(render.props?.state, .loading)
            XCTAssertTrue(networkPerformer.isDelete)
            networkPerformer.finichLoading(result: result)

            result.onValue { _ in
                XCTAssertEqual(render.props?.state, .normal)
                XCTAssertTrue(router.isClosed)
            }
            result.onError { error in
                XCTAssertEqual(render.props?.state, .normal)
                XCTAssertEqual(router.error, error.yo_isNetworkError ? .network : .more)
            }
        }
        [Result<Void>.success(()), .failure(TestError.someError), .failure(TestError.networkError)].forEach { result in
            check(result: result)
        }
    }

    func testDealocating() {
        enum Action: CaseIterable {
            case normal
            case onDeleteTap
            case deleteFinishLoading
            case deleteFinishWithError
            case onSwitchTap
            case switchFinishLoading
            case switchFinishWithError
        }
        func check(_ action: Action) {
            let render = MockRender()
            let networkPerformer = MockNetworkPerformer()
            let router = MockRouter()
            let rule = RuleModel.newRule
            var presenter: MorePresenter? = MorePresenter(render: render,
                                                          rule: rule,
                                                          networkPerformer: networkPerformer,
                                                          router: router)
            presenter?.start()
            XCTAssertNotNil(render.props)

            switch action {
            case .normal:
                break
            case .onDeleteTap:
                render.props?.onDeleteTap()
            case .deleteFinishWithError:
                render.props?.onDeleteTap()
                networkPerformer.finichLoading(result: .failure(TestError.someError))
            case .deleteFinishLoading:
                render.props?.onDeleteTap()
                networkPerformer.finichLoading(result: .success(()))
            case .onSwitchTap:
                render.props?.onSwitchTap()
            case .switchFinishWithError:
                render.props?.onSwitchTap()
                networkPerformer.finichLoading(result: .failure(TestError.someError))
            case .switchFinishLoading:
                render.props?.onSwitchTap()
                networkPerformer.finichLoading(result: .success(()))
            }

            weak var weakPresenter = presenter
            XCTAssertNotNil(weakPresenter)
            presenter = nil
            XCTAssertNil(weakPresenter, "\(action)")
        }

        Action.allCases.forEach { check($0) }
    }
}

extension MorePresenterTest {
    final class MockRender: MoreRendering {
        var props: MoreProps?
        func render(props: MoreProps) {
            self.props = props
        }
    }

    final class MockNetworkPerformer: MoreNetworkPerformer {
        var enable: Bool?
        var completion: ((Result<Void>) -> Void)?
        var isDelete = false

        func finichLoading(result: Result<Void>) {
            self.completion?(result)
        }

        func changeRuleAbility(_ rule: RuleModel, enable: Bool, completion: @escaping (Result<Void>) -> Void) {
            self.enable = enable
            self.completion = completion
        }

        func deleteRule(_ rule: RuleModel, completion: @escaping (Result<Void>) -> Void) {
            self.isDelete = true
            self.completion = completion
        }
    }

    final class MockRouter: MoreRouting {
        var error: Router.Error?
        var isClosed = false
        func show(_ error: Router.Error) {
            self.error = error
        }

        func closeMore() {
            self.isClosed = true
        }
    }
}
