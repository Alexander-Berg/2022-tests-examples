import MarketToasts
import UIUtils
import XCTest

class ThresholdPopupPage: PageObject, PopupPage {

    // MARK: - Properties

    static let rootIdentifier = ToastPopupAccessibility.thresholdPopup
}

class NotifyPopupPage: PageObject, PopupPage {

    // MARK: - Properties

    static let rootIdentifier = MarketToasts.ToastPopupAccessibility.notifyPopup
}

class NewNotifyPopupPage: PageObject, PopupPage {

    // MARK: - Properties

    static let rootIdentifier = ToastPopupAccessibility.newNotifyPopup

    var closeButton: XCUIElement {
        element
            .buttons
            .matching(identifier: PopupEmdeddingAccessibility.closeButton)
            .firstMatch
    }
}

class DefaultToastPopupPage: PageObject, PopupPage {

    // MARK: - Properties

    static let rootIdentifier = MarketToasts.ToastPopupAccessibility.defaultView
}

class AddToComparsionToastPopupPage: PageObject, PopupPage {
    static let rootIdentifier = ComparisonAccessibility.addedToast

    var actionButton: XCUIElement {
        element
            .buttons
            .matching(identifier: ImageActionNotificationAccessibility.actionButton)
            .firstMatch
    }

    var titleLabel: XCUIElement {
        element
            .staticTexts
            .matching(identifier: ImageActionNotificationAccessibility.titleLabel)
            .firstMatch
    }

    func tap() -> ComparisonPage {
        actionButton.tap()

        let elem = XCUIApplication()
            .otherElements
            .matching(identifier: ComparisonAccessibility.root)
            .firstMatch
        return ComparisonPage(element: elem)
    }
}

class RemoveFromComparsionToastPopupPage: PageObject, PopupPage {
    static let rootIdentifier = ComparisonAccessibility.deleteToast

    var actionButton: XCUIElement {
        element
            .buttons
            .matching(identifier: ImageActionNotificationAccessibility.actionButton)
            .firstMatch
    }

    var titleLabel: XCUIElement {
        element
            .staticTexts
            .matching(identifier: ImageActionNotificationAccessibility.titleLabel)
            .firstMatch
    }
}

class BuyForCashbackPopupPage: PageObject, PopupPage {
    static let rootIdentifier = ToastPopupAccessibility.cashbackBalanceView

    var titleLabel: XCUIElement {
        element
            .staticTexts
            .matching(identifier: ToastPopupAccessibility.titleLabel)
            .firstMatch
    }

    var detailsLabel: XCUIElement {
        element
            .staticTexts
            .matching(identifier: ToastPopupAccessibility.detailsLabel)
            .firstMatch
    }
}
