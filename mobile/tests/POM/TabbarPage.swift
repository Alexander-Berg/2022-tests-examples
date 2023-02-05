import MarketUI
import XCTest

final class TabBarPage: PageObject {

    final class TabItemPage: PageObject {

        /*
         badgeView недоступен в иерархии views, его значение добавляется в label кнопки таба,
         внутри которой он отображается.
         Значение badgeView проверять через TabBarPage.current.cartTabItem.element.label
         Например, после добавления товара в корзину, бейдж можно проверить так
         TabBarPage.current.cartTabItem.element.label == "Корзина1"
         "Корзина1" - означает, что на кнопка "Корзина" есть бейдж со значением "1".

         Это сделано для доступности кнопок таба в VoiceOver для слабовидящих пользователей.
         */
        var badgeView: BadgeViewObject {
            let elem = element
                .otherElements
                .matching(identifier: TabBarAccessibility.badgeView)
                .firstMatch
            return BadgeViewObject(element: elem)
        }

        func tap() {
            element.tap()
        }
    }

    static var current: TabBarPage {
        let elem = XCUIApplication()
            .otherElements
            .matching(identifier: TabBarAccessibility.tabBar)
            .firstMatch
        return TabBarPage(element: elem)
    }

    var mordaTabItem: TabItemPage {
        tabItem(withIdentifier: TabBarAccessibility.mordaTab)
    }

    var catalogTabItem: TabItemPage {
        tabItem(withIdentifier: TabBarAccessibility.catalogTab)
    }

    var discountsTabItem: TabItemPage {
        tabItem(withIdentifier: TabBarAccessibility.discountsTab)
    }

    var cartTabItem: TabItemPage {
        tabItem(withIdentifier: TabBarAccessibility.cartTab)
    }

    var profileTabItem: TabItemPage {
        tabItem(withIdentifier: TabBarAccessibility.profileTab)
    }

    var productTabItem: TabItemPage {
        tabItem(withIdentifier: TabBarAccessibility.productTab)
    }

    var expressTabItem: TabItemPage {
        tabItem(withIdentifier: TabBarAccessibility.expressTab)
    }

    var mordaPage: MordaPage {
        tabView(withIdentifier: MordaAccessibility.container)
    }

    var catalogPage: CatalogPage {
        tabView(withIdentifier: CatalogAccessibility.container)
    }

    var emptyCartPage: EmptyCartPage {
        EmptyCartPage(element: element)
    }

    var cartPage: CartPage {
        CartPage(element: element)
    }

    var profilePage: ProfilePage {
        tabView(withIdentifier: ProfileAccessibility.container)
    }

    var expressPage: ExpressPage {
        tabView(withIdentifier: ExpressAccessibility.container)
    }

    // MARK: - Private

    private func tabItem(withIdentifier accessibilityIdentifier: String) -> TabItemPage {
        let elem = element
            .buttons
            .matching(identifier: accessibilityIdentifier)
            .firstMatch
        return TabItemPage(element: elem)
    }

    private func tabView<T: PageObject>(withIdentifier accessibilityIdentifier: String) -> T {
        let elem = element
            .otherElements
            .matching(identifier: accessibilityIdentifier)
            .firstMatch
        return T(element: elem)
    }
}

final class BadgeViewObject: PageObject {

    var badgeLabel: XCUIElement {
        element
            .staticTexts
            .matching(identifier: TabBarAccessibility.badgeLabel)
            .firstMatch
    }
}
