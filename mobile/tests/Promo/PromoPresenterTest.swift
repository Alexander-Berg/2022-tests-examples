//
//  PromoPresenterTest.swift
//  Filters
//
//  Created by Aleksey Makhutin on 12.04.2022.
//

import Foundation
import XCTest
import Utils
@testable import Filters

internal final class PromoPresenterTest: XCTestCase {
    private var allModels: [AutoRuleModel] = [
        .delete(from: "delete@from.ru"),
        .applyLabel(from: "apply@label.com", labelID: 2, labelName: "top"),
        .markRead(from: "mark@read.com"),
        .moveToFolder(from: "apply@label.com", folderID: 1, folderName: "Outgoing")
    ]

    func testStartAndRender() {
        let render = MockRender()
        let presenter = PromoPresenter(render: render, rule: .delete(from: "test"))
        presenter.start()

        XCTAssertNotNil(render.props)
    }

    func testShouldUseDataFromRule() {
        func check(model: AutoRuleModel) {
            let render = MockRender()
            let presenter = PromoPresenter(render: render, rule: model)
            presenter.start()

            XCTAssertNotNil(render.props)

            switch model {
            case let .moveToFolder(fromEmail, _, folderName):
                XCTAssertTrue(render.props!.text.contains(fromEmail))
                XCTAssertTrue(render.props!.text.contains(folderName))
            case let .applyLabel(fromEmail, _, labelName):
                XCTAssertTrue(render.props!.text.contains(fromEmail))
                XCTAssertTrue(render.props!.text.contains(labelName))
            case .delete(let from):
                XCTAssertTrue(render.props!.text.contains(from))
            case .markRead(let from):
                XCTAssertTrue(render.props!.text.contains(from))
            }
        }

        self.allModels.forEach(check(model:))
    }

    func testOnCreateTap() {
        func check(model: AutoRuleModel) {
            let render = MockRender()
            let presenter = PromoPresenter(render: render, rule: model)
            let delegate = Delegate()
            presenter.delegate = delegate
            presenter.start()

            XCTAssertNotNil(render.props)
            render.props?.onCreateTap()

            XCTAssertEqual(delegate.model, model)
        }

        self.allModels.forEach(check(model:))
    }

    func testOnClose() {
        let render = MockRender()
        let presenter = PromoPresenter(render: render, rule: .applyLabel(from: "close", labelID: 2, labelName: "test"))
        let delegate = Delegate()
        presenter.delegate = delegate
        presenter.start()

        XCTAssertNotNil(render.props)
        render.props?.onClose()

        XCTAssertTrue(delegate.didClose)
    }

    func testDealocation() {
        let render = MockRender()
        let promoModel = AutoRuleModel.applyLabel(from: "close", labelID: 2, labelName: "test")
        var presenter: PromoPresenter? = PromoPresenter(render: render, rule: promoModel)
        let delegate = Delegate()
        presenter?.delegate = delegate
        presenter?.start()

        XCTAssertNotNil(render.props)
        render.props?.onClose()
        render.props?.onCreateTap()

        XCTAssertTrue(delegate.didClose)
        XCTAssertEqual(delegate.model, promoModel)

        weak var weakPresenter = presenter
        XCTAssertNotNil(weakPresenter)
        presenter = nil
        XCTAssertNil(weakPresenter)
    }
}

extension PromoPresenterTest {
    final class MockRender: PromoRendering {
        var props: PromoProps?

        func render(props: PromoProps) {
            self.props = props
        }
    }

    final class Delegate: PromoPresenterDelegate {
        var model: AutoRuleModel?
        var didClose = false

        func promoPresenter(_ promoPresenter: PromoPresenter, didRequestToOpenEditRuleFromAutoRule model: AutoRuleModel) {
            self.model = model
        }

        func promoPresenterDidClose(_ promoPresenter: PromoPresenter) {
            self.didClose = true
        }
    }
}
