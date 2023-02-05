import XCTest

final class ApplePaySummaryPage: PageObject {

    // MARK: - Properties

    static var current: ApplePaySummaryPage {
        let elem = XCUIApplication().otherElements[ApplePaySummaryAccessibility.root]
        return .init(element: elem)
    }

    var totalTitle: XCUIElement {
        element
            .staticTexts
            .matching(identifier: ApplePaySummaryAccessibility.totalTitle)
            .firstMatch
    }

    var totalDetails: XCUIElement {
        element
            .staticTexts
            .matching(identifier: ApplePaySummaryAccessibility.totalDetails)
            .firstMatch
    }

    var discountTitle: XCUIElement {
        element
            .staticTexts
            .matching(identifier: ApplePaySummaryAccessibility.discountTitle)
            .firstMatch
    }

    var discountDetails: XCUIElement {
        element
            .staticTexts
            .matching(identifier: ApplePaySummaryAccessibility.discountDetails)
            .firstMatch
    }

    var promoCodeDiscountTitle: XCUIElement {
        element
            .staticTexts
            .matching(identifier: ApplePaySummaryAccessibility.promoCodeDiscountTitle)
            .firstMatch
    }

    var promoCodeDiscountDetails: XCUIElement {
        element
            .staticTexts
            .matching(identifier: ApplePaySummaryAccessibility.promoCodeDiscountDetails)
            .firstMatch
    }

    var coinDiscountTitle: XCUIElement {
        element
            .staticTexts
            .matching(identifier: ApplePaySummaryAccessibility.coinDiscountTitle)
            .firstMatch
    }

    var coinDiscountDetails: XCUIElement {
        element
            .staticTexts
            .matching(identifier: ApplePaySummaryAccessibility.coinDiscountDetails)
            .firstMatch
    }

    var priceDropDiscountTitle: XCUIElement {
        element
            .staticTexts
            .matching(identifier: ApplePaySummaryAccessibility.priceDropDiscountTitle)
            .firstMatch
    }

    var priceDropDiscountDetails: XCUIElement {
        element
            .staticTexts
            .matching(identifier: ApplePaySummaryAccessibility.priceDropDiscountDetails)
            .firstMatch
    }

    var deliveryTitle: XCUIElement {
        element
            .staticTexts
            .matching(identifier: ApplePaySummaryAccessibility.deliveryTitle)
            .firstMatch
    }

    var deliveryDetails: XCUIElement {
        element
            .staticTexts
            .matching(identifier: ApplePaySummaryAccessibility.deliveryDetails)
            .firstMatch
    }

    var sumTitle: XCUIElement {
        element
            .staticTexts
            .matching(identifier: ApplePaySummaryAccessibility.sumTitle)
            .firstMatch
    }

    var sumDetails: XCUIElement {
        element
            .staticTexts
            .matching(identifier: ApplePaySummaryAccessibility.sumDetails)
            .firstMatch
    }

    var merchant: XCUIElement {
        element
            .links
            .matching(identifier: ApplePaySummaryAccessibility.merchant)
            .firstMatch
    }

    var payButton: XCUIElement {
        element
            .buttons
            .matching(identifier: ApplePaySummaryAccessibility.payButton)
            .firstMatch
    }

    var licenceAgreementDisclaimer: XCUIElement {
        element
            .links
            .matching(identifier: ApplePaySummaryAccessibility.licenceAgreementDisclaimer)
            .firstMatch
    }

    var termsOfReturnDisclaimer: XCUIElement {
        element
            .textViews
            .matching(identifier: ApplePaySummaryAccessibility.termsOfReturnDisclaimer)
            .firstMatch
    }

    var supportDisclaimer: XCUIElement {
        element
            .textViews
            .matching(identifier: ApplePaySummaryAccessibility.supportDisclaimer)
            .firstMatch
    }
}
