import XCTest

class NavigationBarPage: PageObject {
    /// Кастомный контейнер с заголовком и подзаголовком.
    final class CustomNavigationBarTitle: PageObject {}
    final class OrderBarButton: PageObject {}

    /// Бар из текущего `XCUIApplication`.
    static var current: NavigationBarPage {
        let barElement = XCUIApplication().navigationBars.firstMatch
        return NavigationBarPage(element: barElement)
    }

    /// Кнопка <--, возвращающая к корневой странице, из которой был совершен поиск через поисковую строку
    var backButton: XCUIElement {
        XCUIApplication().navigationBars.buttons.element(boundBy: 0)
    }

    var closeButton: XCUIElement {
        element
            .buttons.matching(identifier: NavigationBarAccessibility.closeButton)
            .firstMatch
    }

    var deleteButton: XCUIElement {
        element
            .buttons.matching(identifier: NavigationBarAccessibility.deleteButton)
            .firstMatch
    }

    /// Кнопка с выбором региона в навигейшн баре
    var selectRegionButton: XCUIElement {
        element
            .buttons.matching(identifier: NavigationBarAccessibility.selectRegionButton)
            .firstMatch
    }

    /// Кнопка "Оформить" в навигейшн баре
    var orderBarButton: OrderBarButton {
        let elem = element.buttons.matching(identifier: NavigationBarAccessibility.orderBarButton).firstMatch
        return OrderBarButton(element: elem)
    }

    /// Заголовок
    var title: XCUIElement {
        element
            .staticTexts
            .firstMatch
    }
}

extension NavigationBarPage.OrderBarButton: CheckoutEntryPoint {}
extension NavigationBarPage.OrderBarButton: CheckoutDeliveryEntryPoint {}
