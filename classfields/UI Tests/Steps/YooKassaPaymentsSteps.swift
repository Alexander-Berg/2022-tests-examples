//
//  Created by Alexey Aleshkov on 25/03/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils
import YREAccessibilityIdentifiers

final class YooKassaPaymentsSteps {
    @discardableResult
    func moduleIsPresented() -> Self {
        self.rootView
            .yreEnsureExistsWithTimeout(timeout: Consts.animationTimeout)
        return self
    }

    @discardableResult
    func moduleIsDismissed() -> Self {
        self.rootView
            .yreEnsureNotExistsWithTimeout(timeout: Consts.animationTimeout)
        return self
    }

    @discardableResult
    func paymentScreenIsPresented() -> Self {
        self.processButton
            .yreEnsureExistsWithTimeout(timeout: Consts.animationTimeout)
        return self
    }

    @discardableResult
    func paymentScreenIsDismissed() -> Self {
        self.processButton
            .yreEnsureNotExistsWithTimeout(timeout: Consts.animationTimeout)
        return self
    }

    @discardableResult
    func cancel() -> Self {
        let backgroundView = self.rootView
            .children(matching: .other)
            .element
            .yreEnsureExistsWithTimeout(timeout: Consts.animationTimeout)

        let titleLabelPredicate = NSPredicate(format: "label CONTAINS[c] %@", KassaIdentifiers.purchaseTitle)
        let titleLabel = backgroundView
            .children(matching: .any)
            .containing(titleLabelPredicate)
            .element
            .yreEnsureExistsWithTimeout(timeout: Consts.animationTimeout)

        let backgroundFrame = backgroundView.frame
        let titleFrame = titleLabel.frame

        let point = backgroundView.coordinate(withNormalizedOffset: CGVector.zero)
        let offset = CGVector(
            dx: backgroundFrame.size.width / 2.0,
            dy: (titleFrame.minY - backgroundFrame.minY) / 2.0
        )
        let tapPoint = point.withOffset(offset)
        tapPoint.tap()

        return self
    }

    @discardableResult
    func proceedPayment() -> Self {
        self.processButton
            .yreEnsureExists()
            .yreTap()

        return self
    }

    @discardableResult
    func fillForm(cardNumber: String, expireMonth: String, expireShortYear: String, cvc: String) -> Self {
        XCTAssert(cardNumber.count >= 6)
        XCTAssert(expireShortYear.count == 2)
        XCTAssert(expireMonth.count == 2)
        XCTAssert(cvc.count >= 3)

        let app = XCUIApplication()

        let formElements = app.scrollViews.otherElements

        let cardNumberFieldParentOptional = formElements
            .containing(.staticText, identifier: KassaIdentifiers.cardNumberField)
            .allElementsBoundByIndex
            .last
        let cardNumberFieldParent = YREUnwrap(cardNumberFieldParentOptional)
        let cardNumberField = cardNumberFieldParent.textViews.element
        cardNumberField
            .yreEnsureExistsWithTimeout(timeout: Consts.animationTimeout)
            .yreTap()
            .yreTypeText(cardNumber)

        let expireDateFieldParentOptional = formElements
            .containing(.staticText, identifier: KassaIdentifiers.expireDateField)
            .allElementsBoundByIndex
            .last
        let expireDateFieldParent = YREUnwrap(expireDateFieldParentOptional)
        let expireDateField = expireDateFieldParent.textViews.element
        expireDateField
            .yreEnsureExistsWithTimeout(timeout: Consts.animationTimeout)
            .yreTap()
            .yreTypeText(expireMonth + expireShortYear)

        let cvcFieldParentOptional = formElements
            .containing(.staticText, identifier: KassaIdentifiers.cvcField)
            .allElementsBoundByIndex
            .last
        let cvcFieldParent = YREUnwrap(cvcFieldParentOptional)
        let cvcField = cvcFieldParent.textViews.element
        cvcField
            .yreEnsureExistsWithTimeout(timeout: Consts.animationTimeout)
            .yreTap()
            .yreTypeText(cvc)

        return self
    }

    @discardableResult
    func submitPayment() -> Self {
        let submitButton = ElementsProvider.obtainElement(
            identifier: KassaIdentifiers.submitButton,
            type: .button
        )
        submitButton
            .yreEnsureExists()
            .yreTap()

        return self
    }

    // MARK: Private

    private lazy var rootView = ElementsProvider.obtainElement(
        identifier: KassaIdentifiers.tokenizationModule,
        type: .other
    )

    private lazy var processButton = ElementsProvider.obtainElement(
        identifier: KassaIdentifiers.proceedButton,
        type: .button,
        in: self.rootView
    )

    private enum KassaIdentifiers {
        static let tokenizationModule: String = PaymentsAccessibilityIdentifiers.tokenizationModule

        static let purchaseTitle = "Яндекс.Недвижимость"

        static let proceedButton = "Продолжить"
        static let cardNumberField = "Номер карты"
        static let expireDateField = "ММ / ГГ"
        static let cvcField = "CVC"
        static let submitButton = "Заплатить"
    }

    private enum Consts {
        static let animationTimeout: TimeInterval = 30
    }
}

extension YooKassaPaymentsSteps {
    func makeActivity() -> YooKassaPaymentStepsActivity {
        return .init(self)
    }
}

final class YooKassaPaymentStepsActivity {
    init(_ steps: YooKassaPaymentsSteps) {
        self.steps = steps
    }

    @discardableResult
    func fillForm(cardNumber: String, expireMonth: String, expireShortYear: String, cvc: String) -> Self {
        self.cardNumber = cardNumber
        self.expireMonth = expireMonth
        self.expireShortYear = expireShortYear
        self.cvc = cvc
        self.isSuccess = true

        return self
    }

    @discardableResult
    func cancel() -> Self {
        self.isSuccess = false

        return self
    }

    @discardableResult
    func run() -> Self {
        let isSuccess = YREUnwrap(self.isSuccess)

        XCTContext.runActivity(named: "Yandex.Checkout Story", block: { _ -> Void in
            if isSuccess {
                self.performSubmit()
            }
            else {
                self.performCancel()
            }
        })

        return self
    }

    private let steps: YooKassaPaymentsSteps

    private var isSuccess: Bool?

    private var cardNumber: String?
    private var expireMonth: String?
    private var expireShortYear: String?
    private var cvc: String?

    private func performSubmit() {
        let cardNumber = YREUnwrap(self.cardNumber)
        let expireMonth = YREUnwrap(self.expireMonth)
        let expireShortYear = YREUnwrap(self.expireShortYear)
        let cvc = YREUnwrap(self.cvc)

        self.steps
            .moduleIsPresented()
            .paymentScreenIsPresented()
            .proceedPayment()
            .fillForm(
                cardNumber: cardNumber,
                expireMonth: expireMonth,
                expireShortYear: expireShortYear,
                cvc: cvc
            )
            .submitPayment()
            .moduleIsDismissed()
    }

    private func performCancel() {
        self.steps
            .moduleIsPresented()
            .paymentScreenIsPresented()
            .cancel()
            .paymentScreenIsDismissed()
            .moduleIsDismissed()
    }
}
