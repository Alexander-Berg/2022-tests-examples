//
//  SharkCreditTest_PromoTests.swift
//  UITests
//
//  Created by Dmitry Sinev on 17.03.2022.
//

import AutoRuProtoModels
import XCTest
import Snapshots

/// @depends_on AutoRuCredit
final class SharkCreditTest_PromoTests: BaseTest {
    var sharkMocker: SharkMocker!
    var settings: [String: Any] = [:]

    override var appSettings: [String: Any] {
        get {
            return self.settings
        }
        set { self.settings = newValue }
    }

    override func setUp() {
        super.setUp()
        settings = super.appSettings
        settings["offersHistoryEnabled"] = true
        settings["webHosts"] = "http://127.0.0.1:\(port)"
        settings["currentHosts"] = [
            "PublicAPI": "http://127.0.0.1:\(port)/"
        ]
        sharkMocker = SharkMocker(server: server)
    }
    
    func test_showCreditPromo() {
        let products: [SharkMocker.Product] = [.tinkoff_auto, .sberbank, .alfa499]
        let mocker = sharkMocker
            .baseMock(offerId: "1")
            .mockProductList(products: products, isMany: true, isForPromo: true)
            .mockNoApplication()
        mocker.mockCalculator()
        var experiments = BackendState.Experiments()
        experiments.add(exp: BackendState.Experiments.CreditNewPromo())
        api.device.hello.post.ok(mock: experiments.toMockSource())
        mocker.start()
        settings["skipCreditAlert"] = true

        openCredits()
        // Эта дурь с точными подкрутками нужна, так как оказалось, что, когда свайпы попадают в элементы вроде слайдеров или текстовых полей, то они просто не срабатывают, а там малая форма кредита, где их много - поэтому нужно вручную находить гарантировано нормальные элементы и свайпить в них.
            .focus(on: .promoTop, { cell in
                cell.validateSnapshot(snapshotId: "test_showCreditPromo_Header")
                let touchPoint = cell.rootElement.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 1.2))
                let whereToScroll = cell.rootElement.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: -0.2))
                touchPoint.press(forDuration: 0.1, thenDragTo: whereToScroll, withVelocity: .slow, thenHoldForDuration: 0.1)
            })
            .focus(on: .promoBetterWithUs, { cell in
                let touchPoint = cell.rootElement.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.1))
                let whereToScroll = cell.rootElement.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: -1.0))
                touchPoint.press(forDuration: 0.1, thenDragTo: whereToScroll, withVelocity: .slow, thenHoldForDuration: 0.1)
                cell.validateSnapshot(snapshotId: "test_showCreditPromo_BetterWithUs1")
                cell.swipe(.left)
                cell.swipe(.left)
                cell.swipe(.left)
                cell.validateSnapshot(snapshotId: "test_showCreditPromo_BetterWithUs2")
            })
            .scroll(to: .promo3Steps)
            .focus(on: .promo3Steps, { cell in
                cell.validateSnapshot(snapshotId: "test_showCreditPromo_3Steps")
            })
            .scroll(to: .promoSendFormButton)
            .tap(.promoSendFormButton)
            .focus(on: .promoCalculatorTop, { cell in
                let touchPoint = cell.rootElement.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.1))
                let whereToScroll = cell.rootElement.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: -1.0))
                touchPoint.press(forDuration: 0.1, thenDragTo: whereToScroll, withVelocity: .slow, thenHoldForDuration: 0.1)
            })
            .scroll(to: .promoFAQ_question)
            .should(.promoPartners0, .exist)
            .should(.promoPartners1, .exist)
            .tap(.promoFAQ_question)
            .scroll(to: .promoFAQ_answer)
            .focus(on: .promoFAQ_question, { cell in
                cell.validateSnapshot(snapshotId: "test_showCreditPromo_Question")
            })
            .focus(on: .promoFAQ_answer, { cell in
                cell.validateSnapshot(snapshotId: "test_showCreditPromo_Answer")
            })
    }

    func test_noCreditPromoChangedTexts() {
        let products: [SharkMocker.Product] = [.tinkoff_auto, .sberbank, .alfa499]
        let mocker = sharkMocker
            .mockProductList(products: products, isMany: true, isForPromo: true)
            .mockNoApplication()
        mocker.mockCalculator()
        var experiments = BackendState.Experiments()
        experiments.add(exp: BackendState.Experiments.HideCreditPercents())
        api.device.hello.post.ok(mock: experiments.toMockSource())
        mocker.start()
        settings["skipCreditAlert"] = true

        openCredits()
            .should(.percentlessTitle, .exist)
    }

    private func openCredits() -> CreditLKScreen {
        launchMain { screen in
            screen
                .should(provider: .mainScreen, .exist)
                .focus { $0.tap(.navBarTab(.credits)) }
                .should(provider: .creditLKScreen, .exist)
        }
    }
}
