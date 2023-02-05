import XCTest

/// Попап с информацией о кредите на КМ
final class CreditPopupPage: PageObject, PopupPage {

    // MARK: - Properties

    static let rootIdentifier = CreditPopupAccessibility.root

    var titleLabel: XCUIElement {
        element
            .staticTexts.matching(identifier: CreditPopupAccessibility.titleLabel)
            .firstMatch
    }

    var initialPayment: KeyValueViewPage {
        let elem = element
            .any.matching(identifier: CreditPopupAccessibility.initialPayment)
            .firstMatch
        return KeyValueViewPage(element: elem)
    }

    var creditTerm: KeyValueViewPage {
        let elem = element
            .any.matching(identifier: CreditPopupAccessibility.creditTerm)
            .firstMatch
        return KeyValueViewPage(element: elem)
    }

    var monthlyPayment: KeyValueViewPage {
        let elem = element
            .any.matching(identifier: CreditPopupAccessibility.monthlyPayment)
            .firstMatch
        return KeyValueViewPage(element: elem)
    }

    var cartButton: XCUIElement {
        element
            .buttons.matching(identifier: CreditPopupAccessibility.cartButton)
            .firstMatch
    }

    var disclaimerLink: XCUIElement {
        element.links.firstMatch
    }
}
