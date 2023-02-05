//
//  ListPresenterTest.swift
//  FiltersTests
//
//  Created by Aleksey Makhutin on 15.11.2021.
//

import Foundation
import XCTest
import Utils
import NetworkLayer
@testable import Filters

// swiftlint:disable type_body_length
internal final class ListPresenterTest: XCTestCase {
    func testStart() {
        let render = MockRender()
        let router = MockRouter()
        let networkPerformer = MockNetworkPerformer()
        let storing = MockStoring()
        let presenter = ListPresenter(render: render, router: router, networkPerfomer: networkPerformer, promoStrategy: .dependsOnViewsCounter(filtersStoring: storing, maximum: 3))
        presenter.start()

        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.state, .loading)
        XCTAssertNotNil(networkPerformer.completion, "networkPerformer should loading list")
        networkPerformer.finishLoading(with: .success([]))

        YOAssertEqualWithoutAssossiatedValue(lhs: render.props?.state, rhs: .normal(props: RulesProps(rules: [], onCreateTap: .empty)))
    }

    func testActionsMapping() {
        func check(with stop: Bool) {
            let render = MockRender()
            let router = MockRouter()
            let networkPerformer = MockNetworkPerformer()
            let storing = MockStoring()
            let presenter = ListPresenter(render: render,
                                          router: router,
                                          networkPerfomer: networkPerformer,
                                          promoStrategy: .dependsOnViewsCounter(filtersStoring: storing, maximum: 3))
            presenter.start()

            XCTAssertNotNil(render.props)
            XCTAssertEqual(render.props?.state, .loading)
            XCTAssertNotNil(networkPerformer.completion, "networkPerformer should loading list")

            let actions: [RuleModel.Action] = [
                .applyLabel(label: RuleModel.Container(id: 1, name: "labelName")),
                .autoReply(text: "text"),
                .delete,
                .forward(store: true, email: "email"),
                .markRead,
                .moveToFolder(folder: RuleModel.Container(id: 2, name: "folderName")),
                .notify(email: "email")
            ]

            let ruleModel = RuleModel(id: 1, name: "foo", isEnabled: true, stop: stop, logic: .and, conditions: [], actions: actions, flags: [.spam(.only)])

            let expectedProps = RuleProps(kind: .editOnDesktopOnly,
                                          isEnabled: true,
                                          conditions: [.init(logic: nil, content: "Only spam")],
                                          actions: [
                                            "— Add label \"labelName\"",
                                            "— Automatic response",
                                            "— Delete",
                                            "— Forward email to \"email\"",
                                            "— Mark as read",
                                            "— Move to folder \"folderName\"",
                                            "— Notify at email",
                                            stop ? "— Don't apply other rules" : nil].compactMap { $0 },
                                          onTap: .empty)

            networkPerformer.finishLoading(with: .success([ruleModel]))
            XCTAssertEqual(render.props?.state, .normal(props: RulesProps(rules: [expectedProps], onCreateTap: .empty)))
        }

        check(with: true)
        check(with: false)
    }

    func testConditionsAndLogicMapping() {
        func check(with logic: RuleModel.Logic) {
            let render = MockRender()
            let router = MockRouter()
            let networkPerformer = MockNetworkPerformer()
            let storing = MockStoring()
            let presenter = ListPresenter(render: render,
                                          router: router,
                                          networkPerfomer: networkPerformer,
                                          promoStrategy: .dependsOnViewsCounter(filtersStoring: storing, maximum: 3))
            presenter.start()

            XCTAssertNotNil(render.props)
            XCTAssertEqual(render.props?.state, .loading)
            XCTAssertNotNil(networkPerformer.completion, "networkPerformer should loading list")

            func generateAllOperatorCondition(with key: RuleModel.Condition.Key, value: String) -> [RuleModel.Condition] {
                return [RuleModel.Condition.Operator.contains, .equals, .notContain, .notEqual].map { someOperator in
                    return .init(key: key, operator: someOperator, value: value)
                }
            }

            var conditions: [RuleModel.Condition] = []
            conditions.append(contentsOf: generateAllOperatorCondition(with: .attachmentName, value: "attachmentName"))
            conditions.append(contentsOf: generateAllOperatorCondition(with: .body, value: "body"))
            conditions.append(contentsOf: generateAllOperatorCondition(with: .cc, value: "cc"))
            conditions.append(contentsOf: generateAllOperatorCondition(with: .from, value: "from"))
            conditions.append(contentsOf: generateAllOperatorCondition(with: .header(name: "name"), value: "header"))
            conditions.append(contentsOf: generateAllOperatorCondition(with: .subject, value: "subject"))
            conditions.append(contentsOf: generateAllOperatorCondition(with: .to, value: "to"))
            conditions.append(contentsOf: generateAllOperatorCondition(with: .toOrCC, value: "toOrCC"))

            let ruleModel = RuleModel(id: 1,
                                      name: "foo",
                                      isEnabled: true,
                                      stop: false,
                                      logic: logic,
                                      conditions: conditions,
                                      actions: [],
                                      flags: [.spam(.only)])

            let logicString = logic == .and ? "and" : "or"
            let expectedProps = RuleProps(kind: .editOnDesktopOnly,
                                          isEnabled: true,
                                          conditions: [RuleConditionProps(logic: "If", content: "\"Attachment name\" contains \"attachmentName\""),
                                                       RuleConditionProps(logic: logicString, content: "\"Attachment name\" matches \"attachmentName\""),
                                                       RuleConditionProps(logic: logicString, content: "\"Attachment name\" doesn't contain \"attachmentName\""),
                                                       RuleConditionProps(logic: logicString, content: "\"Attachment name\" doesn't match \"attachmentName\""),
                                                       RuleConditionProps(logic: logicString, content: "\"Body of the email\" contains \"body\""),
                                                       RuleConditionProps(logic: logicString, content: "\"Body of the email\" matches \"body\""),
                                                       RuleConditionProps(logic: logicString, content: "\"Body of the email\" doesn't contain \"body\""),
                                                       RuleConditionProps(logic: logicString, content: "\"Body of the email\" doesn't match \"body\""),
                                                       RuleConditionProps(logic: logicString, content: "\"Cc\" contains \"cc\""),
                                                       RuleConditionProps(logic: logicString, content: "\"Cc\" matches \"cc\""),
                                                       RuleConditionProps(logic: logicString, content: "\"Cc\" doesn't contain \"cc\""),
                                                       RuleConditionProps(logic: logicString, content: "\"Cc\" doesn't match \"cc\""),
                                                       RuleConditionProps(logic: logicString, content: "\"From\" contains \"from\""),
                                                       RuleConditionProps(logic: logicString, content: "\"From\" matches \"from\""),
                                                       RuleConditionProps(logic: logicString, content: "\"From\" doesn't contain \"from\""),
                                                       RuleConditionProps(logic: logicString, content: "\"From\" doesn't match \"from\""),
                                                       RuleConditionProps(logic: logicString, content: "header \"name\" contains \"header\""),
                                                       RuleConditionProps(logic: logicString, content: "header \"name\" matches \"header\""),
                                                       RuleConditionProps(logic: logicString, content: "header \"name\" doesn't contain \"header\""),
                                                       RuleConditionProps(logic: logicString, content: "header \"name\" doesn't match \"header\""),
                                                       RuleConditionProps(logic: logicString, content: "\"Subject\" contains \"subject\""),
                                                       RuleConditionProps(logic: logicString, content: "\"Subject\" matches \"subject\""),
                                                       RuleConditionProps(logic: logicString, content: "\"Subject\" doesn't contain \"subject\""),
                                                       RuleConditionProps(logic: logicString, content: "\"Subject\" doesn't match \"subject\""),
                                                       RuleConditionProps(logic: logicString, content: "\"To\" contains \"to\""),
                                                       RuleConditionProps(logic: logicString, content: "\"To\" matches \"to\""),
                                                       RuleConditionProps(logic: logicString, content: "\"To\" doesn't contain \"to\""),
                                                       RuleConditionProps(logic: logicString, content: "\"To\" doesn't match \"to\""),
                                                       RuleConditionProps(logic: logicString, content: "\"To or cc\" contains \"toOrCC\""),
                                                       RuleConditionProps(logic: logicString, content: "\"To or cc\" matches \"toOrCC\""),
                                                       RuleConditionProps(logic: logicString, content: "\"To or cc\" doesn't contain \"toOrCC\""),
                                                       RuleConditionProps(logic: logicString, content: "\"To or cc\" doesn't match \"toOrCC\"")],
                                          actions: [],
                                          onTap: .empty)

            networkPerformer.finishLoading(with: .success([ruleModel]))
            XCTAssertEqual(render.props?.state, .normal(props: RulesProps(rules: [expectedProps], onCreateTap: .empty)))
        }

        check(with: .or)
        check(with: .and)
    }

    func testFlagsMapping() {
        // https://st.yandex-team.ru/MOBILEMAIL-19144
        let flagsMapping: [([RuleModel.Flag], String)] = [
            ([.spam(.no)], "All emails except spam"),
            ([.spam(.no), .attachment(has: true)], "All emails with attachments except spam"),
            ([.spam(.no), .attachment(has: false)], "All emails without attachments except spam"),

            ([.spam(.all)], "All emails"),
            ([.spam(.all), .attachment(has: true)], "All emails with attachments"),
            ([.spam(.all), .attachment(has: false)], "All emails without attachments"),

            ([.spam(.only)], "Only spam"),
            ([.spam(.only), .attachment(has: true)], "Only spam with attachments"),
            ([.spam(.only), .attachment(has: false)], "Only spam without attachments")
        ]

        func check(flags: [RuleModel.Flag], expectedCondition: String) {
            let render = MockRender()
            let router = MockRouter()
            let networkPerformer = MockNetworkPerformer()
            let storing = MockStoring()
            let presenter = ListPresenter(render: render,
                                          router: router,
                                          networkPerfomer: networkPerformer,
                                          promoStrategy: .dependsOnViewsCounter(filtersStoring: storing, maximum: 3))
            presenter.start()

            let ruleModel = RuleModel(id: 1, name: "foo", isEnabled: true, stop: false, logic: .and, conditions: [], actions: [RuleModel.Action.markRead], flags: flags)

            let expectedProps = RuleProps(kind: .editOnDesktopOnly,
                                          isEnabled: true,
                                          conditions: [.init(logic: nil, content: expectedCondition)],
                                          actions: ["— Mark as read"],
                                          onTap: .empty)

            networkPerformer.finishLoading(with: .success([ruleModel]))
            XCTAssertEqual(render.props?.state, .normal(props: RulesProps(rules: [expectedProps], onCreateTap: .empty)))
        }

        flagsMapping.forEach {
            check(flags: $0.0, expectedCondition: $0.1)
        }
    }

    func testErrorRoutingAndButton() {
        func check(error: Error) {
            let render = MockRender()
            let router = MockRouter()
            let networkPerformer = MockNetworkPerformer()
            let storing = MockStoring()
            let presenter = ListPresenter(render: render,
                                          router: router,
                                          networkPerfomer: networkPerformer,
                                          promoStrategy: .dependsOnViewsCounter(filtersStoring: storing, maximum: 3))
            presenter.start()

            XCTAssertNotNil(render.props)
            XCTAssertEqual(render.props?.state, .loading)
            XCTAssertNotNil(networkPerformer.completion, "networkPerformer should loading list")

            networkPerformer.finishLoading(with: .failure(error))

            if error.yo_isNetworkError {
                XCTAssertEqual(router.error, .network, "should show toast")
            } else {
                XCTAssertEqual(router.error, .list, "should show toast")
            }

            XCTAssertNotNil(render.props)
            YOAssertEqualWithoutAssossiatedValue(lhs: render.props?.state,
                                                 rhs: .error(props: ErrorProps(title: "",
                                                                               text: "",
                                                                               buttonTitle: "",
                                                                               onTap: .empty)))
            networkPerformer.completion = nil
            if case let .error(errorProps) = render.props?.state {
                errorProps.onTap()
                XCTAssertNotNil(networkPerformer.completion, "networkPerformer should loading list")
                YOAssertEqualWithoutAssossiatedValue(lhs: render.props?.state, rhs: .loading)
                networkPerformer.finishLoading(with: .success([]))
                YOAssertEqualWithoutAssossiatedValue(lhs: render.props?.state, rhs: .normal(props: RulesProps(rules: [], onCreateTap: .empty)))
            } else {
                XCTFail("ErrorProps should exist")
            }
        }
        check(error: TestError.someError)
        check(error: TestError.networkError)
    }

    func testOnCreateTap() {
        let render = MockRender()
        let router = MockRouter()
        let networkPerformer = MockNetworkPerformer()
        let storing = MockStoring()
        let presenter = ListPresenter(render: render, router: router, networkPerfomer: networkPerformer, promoStrategy: .dependsOnViewsCounter(filtersStoring: storing, maximum: 3))
        presenter.start()

        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.state, .loading)
        XCTAssertNotNil(networkPerformer.completion, "networkPerformer should loading list")
        networkPerformer.finishLoading(with: .success([]))

        XCTAssertFalse(router.isCreateOpen)
        if case let .normal(props) = render.props?.state {
            props.onCreateTap()
            XCTAssertTrue(router.isCreateOpen)
        } else {
            XCTFail("props should exist")
        }
    }

    func testOnRuleTap() {
        let render = MockRender()
        let router = MockRouter()
        let networkPerformer = MockNetworkPerformer()
        let storing = MockStoring()
        let presenter = ListPresenter(render: render,
                                      router: router,
                                      networkPerfomer: networkPerformer,
                                      promoStrategy: .dependsOnViewsCounter(filtersStoring: storing, maximum: 3))
        presenter.start()

        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.state, .loading)
        XCTAssertNotNil(networkPerformer.completion, "networkPerformer should loading list")

        let expectedRuleModel = RuleModel(id: 1,
                                          name: "1",
                                          isEnabled: true,
                                          stop: false,
                                          logic: .and,
                                          conditions: [.init(key: .subject, operator: .contains, value: "123")],
                                          actions: [.applyLabel(label: .init(id: 1, name: "1"))],
                                          flags: [.spam(.no)])
        XCTAssertEqual(expectedRuleModel.kind, .onlySubject)

        networkPerformer.finishLoading(with: .success([expectedRuleModel]))

        if case let .normal(props) = render.props?.state {
            XCTAssertEqual(props.rules.count, 1)
            router.ruleModel = nil
            XCTAssertNil(router.ruleModel)
            props.rules.first?.onTap()
            XCTAssertEqual(router.ruleModel, expectedRuleModel)
        } else {
            XCTFail("props should exist")
        }
    }

    func testOnNonEditableRuleTap() {
        let render = MockRender()
        let router = MockRouter()
        let networkPerformer = MockNetworkPerformer()
        let storing = MockStoring()
        let presenter = ListPresenter(render: render, router: router, networkPerfomer: networkPerformer, promoStrategy: .dependsOnViewsCounter(filtersStoring: storing, maximum: 3))
        presenter.start()

        XCTAssertNotNil(render.props)
        XCTAssertEqual(render.props?.state, .loading)
        XCTAssertNotNil(networkPerformer.completion, "networkPerformer should loading list")

        let expectedRuleModel = RuleModel(id: 1,
                                          name: "1",
                                          isEnabled: true,
                                          stop: false,
                                          logic: .and,
                                          conditions: [.init(key: .subject, operator: .contains, value: "123")],
                                          actions: [.applyLabel(label: .init(id: 1, name: "1")), .notify(email: "12345")],
                                          flags: [.spam(.no)])

        XCTAssertEqual(expectedRuleModel.kind, .editOnDesktopOnly)

        networkPerformer.finishLoading(with: .success([expectedRuleModel]))

        if case let .normal(props) = render.props?.state {
            XCTAssertEqual(props.rules.count, 1)
            router.ruleModel = nil
            XCTAssertNil(router.ruleModel)
            props.rules.first?.onTap()
            XCTAssertNil(router.ruleModel)
        } else {
            XCTFail("props should exist")
        }
    }

    func testDealocating() {
        enum Action: CaseIterable {
            case loading
            case finishLoading
            case finishWithError
            case onCreateTap
            case onRuleTap
            case onErrorTap
        }
        func check(_ action: Action) {
            let render = MockRender()
            let router = MockRouter()
            let networkPerformer = MockNetworkPerformer()
            let storing = MockStoring()
            var presenter: ListPresenter? = ListPresenter(render: render,
                                                          router: router,
                                                          networkPerfomer: networkPerformer,
                                                          promoStrategy: .dependsOnViewsCounter(filtersStoring: storing, maximum: 3))
            presenter?.start()

            XCTAssertNotNil(render.props)
            XCTAssertEqual(render.props?.state, .loading)
            XCTAssertNotNil(networkPerformer.completion, "networkPerformer should loading list")

            let ruleModel = RuleModel(id: 1, name: "1", isEnabled: true, stop: false, logic: .and, conditions: [], actions: [], flags: [])

            switch action {
            case .loading:
                break
            case .finishLoading:
                networkPerformer.finishLoading(with: .success([ruleModel]))
            case .finishWithError:
                networkPerformer.finishLoading(with: .failure(TestError.someError))
            case .onCreateTap:
                networkPerformer.finishLoading(with: .success([ruleModel]))
                if case let .normal(props) = render.props?.state {
                    props.onCreateTap()
                } else {
                    XCTFail("props should exist")
                }
            case .onRuleTap:
                networkPerformer.finishLoading(with: .success([ruleModel]))
                if case let .normal(props) = render.props?.state {
                    props.rules.first?.onTap()
                } else {
                    XCTFail("props should exist")
                }
            case .onErrorTap:
                networkPerformer.finishLoading(with: .failure(TestError.someError))
                if case let .error(errorProps) = render.props?.state {
                    errorProps.onTap()
                } else {
                    XCTFail("ErrorProps should exist")
                }
            }

            weak var weakPresenter = presenter
            XCTAssertNotNil(weakPresenter)
            presenter = nil
            XCTAssertNil(weakPresenter)
        }

        Action.allCases.forEach { check($0) }
    }

    private func YOAssertEqualWithoutAssossiatedValue(lhs: ListProps.State?, rhs: ListProps.State?) {
        switch (lhs, rhs) {
        case (.loading, .loading), (.error, .error), (.normal, .normal):
            break
        default:
             XCTFail("ListProps.State is not equal")
        }
    }
}

private extension ListPresenterTest {
    final class MockRender: ListRendering {
        var props: ListProps?

        func render(props: ListProps) {
            self.props = props
        }
    }

    final class MockRouter: ListRouting {
        var error: Router.Error?
        var isCreateOpen = false
        var ruleModel: RuleModel?

        func openRule(model: RuleModel) {
            self.ruleModel = model
        }

        func createRule() {
            self.isCreateOpen = true
        }

        func show(_ error: Router.Error) {
            self.error = error
        }
    }

    final class MockNetworkPerformer: ListNetworkPerformer {
        typealias FiltersListCompletion = (Result<[RuleModel]>) -> Void
        var force = false
        var completion: FiltersListCompletion?

        func loadFiltersList(force: Bool, completion: @escaping (Result<[RuleModel]>) -> Void) {
            self.force = force
            self.completion = completion
        }

        func finishLoading(with result: Result<[RuleModel]>) {
            self.completion?(result)
        }
    }

    final class MockStoring: FiltersStoring {
        var listViewsCount: Int = 0
    }
}

extension ListProps.State: Equatable {
    public static func == (lhs: ListProps.State, rhs: ListProps.State) -> Bool {
        switch (lhs, rhs) {
        case (.loading, .loading):
            return true
        case (.normal(let lProps), .normal(let rProps)):
             return lProps == rProps
        case (.error(let lProps), .error(let rProps)):
            return lProps == rProps
        default:
            return false
        }
    }
}
