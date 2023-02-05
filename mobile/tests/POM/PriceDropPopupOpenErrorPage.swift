import XCTest

final class PriceDropPopupOpenErrorPage: PageObject {

    // Экран поп-апа с ошибкой
    static var current: PriceDropPopupOpenErrorPage {
        let elem = XCUIApplication().otherElements[PriceDropPopupAccessibility.OpenErrorScreen.root]
        return PriceDropPopupOpenErrorPage(element: elem)
    }

    // Заголовок ошибки поп-апа
    var title: XCUIElement {
        element
            .staticTexts
            .matching(identifier: PriceDropPopupAccessibility.OpenErrorScreen.title)
            .firstMatch
    }

    // Подзаголовок ошибки поп-апа
    var subtitle: XCUIElement {
        element
            .staticTexts
            .matching(identifier: PriceDropPopupAccessibility.OpenErrorScreen.subtitle)
            .firstMatch
    }

    // Картинка поп-апа
    var image: XCUIElement {
        element
            .images
            .matching(identifier: PriceDropPopupAccessibility.OpenErrorScreen.image)
            .firstMatch
    }

    // Кнопка "К покупкам"
    var actionButton: ActionButton {
        let elem = element
            .buttons
            .matching(identifier: PriceDropPopupAccessibility.OpenErrorScreen.actionButton)
            .firstMatch
        return ActionButton(element: elem)
    }

    class ActionButton: PageObject, CatalogEntryPoint {}
}
