import MarketUI
import XCTest

/// Сниппет в виджете SingleAction
class HoveringSnippetPage: PageObject {

    class ActionButton: PageObject, OrderEditPaymentEntryPoint, QuestionnairePageEntryPoint,
        OutletOnMapEntryPoint, OrderDetailsEntryPoint, GrowingCashbackPromoPageEntryPoint,
        GrowingCashbackInfoPageEntryPoint {}

    var titleLabel: XCUIElement {
        element
            .staticTexts
            .matching(identifier: HoveringSnippetAccessibility.titleLabel)
            .firstMatch
    }

    var subtitleLabel: XCUIElement {
        element
            .staticTexts
            .matching(identifier: HoveringSnippetAccessibility.subtitleLabel)
            .firstMatch
    }

    var actionButton: ActionButton {
        let actionButtonElement = element
            .buttons
            .matching(identifier: HoveringSnippetAccessibility.actionButton)
            .firstMatch
        return ActionButton(element: actionButtonElement)
    }

    var additionalActionButton: ActionButton {
        let additionalActionButtonElement = element
            .buttons
            .matching(identifier: HoveringSnippetAccessibility.additionalActionButton)
            .firstMatch
        return ActionButton(element: additionalActionButtonElement)
    }

    var iconImageView: XCUIElement {
        element
            .images
            .matching(identifier: HoveringSnippetAccessibility.iconImageView)
            .firstMatch
    }

}
