import MarketBNPL
import XCTest

final class BNPLPlanConstructorPage: PageObject {

    // MARK: - Properties

    static var current: BNPLPlanConstructorPage {
        let element = XCUIApplication()
            .otherElements
            .matching(identifier: BNPLPlanConstructorAccessibility.root)
            .firstMatch
        return BNPLPlanConstructorPage(element: element)
    }

    /// Лейбл с первым платежом
    var deposit: XCUIElement {
        element
            .staticTexts
            .matching(identifier: BNPLPlanConstructorAccessibility.deposit)
            .firstMatch
    }

    /// Лейбл с остальными платежами
    var payments: XCUIElement {
        element
            .staticTexts
            .matching(identifier: BNPLPlanConstructorAccessibility.payments)
            .firstMatch
    }

    /// Кнопка ОФормить
    var checkoutButton: CheckoutButton {
        CheckoutButton(
            element: element
                .buttons
                .matching(identifier: BNPLPlanConstructorAccessibility.createOrderButton)
                .firstMatch
        )
    }

    /// План платежей
    var planView: XCUIElement {
        element
            .otherElements
            .matching(identifier: BNPLPlanConstructorAccessibility.planView)
            .firstMatch
    }

    /// Кнопка подробнее о BNPL
    var detailsButton: DetailsButton {
        DetailsButton(
            element: element
                .buttons
                .matching(identifier: BNPLPlanConstructorAccessibility.detailsButton)
                .firstMatch
        )
    }
}

// MARK: - Nested Types

extension BNPLPlanConstructorPage {

    final class DetailsButton: PageObject, WebViewEntryPoint {}

    final class CheckoutButton: PageObject, CheckoutEntryPoint {

        func tap() -> CheckoutPage {
            element.tap()

            let elem = XCUIApplication().otherElements[CheckoutAccessibility.root]
            return CheckoutPage(element: elem)
        }
    }
}
