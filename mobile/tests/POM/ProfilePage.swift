import MarketCashback
import UIUtils
import XCTest

final class ProfilePage: PageObject {

    /// Кнопка "Войти" (переходит на экран для ввода логина/пароля)
    var auth: XCUIElement {
        element
            .buttons
            .matching(identifier: ProfileAccessibility.auth)
            .firstMatch
    }

    /// Email пользователя
    var email: XCUIElement {
        element
            .staticTexts
            .matching(identifier: ProfileAccessibility.email)
            .firstMatch
    }

    /// Ячейка "Списки сравнения"
    var comparison: ComparisonsCell {
        let elem = cellUniqueElement(withIdentifier: ProfileAccessibility.comparison)
        return ComparisonsCell(element: elem)
    }

    /// Ячейка "Мои заказы"
    var myOrders: MyOrdersCell {
        let elem = cellUniqueElement(withIdentifier: ProfileAccessibility.myOrders)
        return MyOrdersCell(element: elem)
    }

    /// Ячейка "Мои купоны"
    var myBonuses: MyBonusesCell {
        let elem = cellUniqueElement(withIdentifier: ProfileAccessibility.myBonuses)
        return MyBonusesCell(element: elem)
    }

    /// Ячейка "Мои публикации"
    var myPublications: MyPublications {
        let elem = cellUniqueElement(withIdentifier: ProfileAccessibility.myPublications)
        return MyPublications(element: elem)
    }

    /// Ячейка "Уведомления"
    var pushNotifications: PushNotifications {
        let element = cellUniqueElement(withIdentifier: ProfileAccessibility.pushNotifications)
        return PushNotifications(element: element)
    }

    /// Ячейка "Яндекc Плюс"
    var yandexPlus: YandexPlusCell {
        let elem = cellUniqueElement(withIdentifier: ProfileAccessibility.YandexPlus.container)
        return YandexPlusCell(element: elem)
    }

    /// Ячейка "Мои избранные"
    var wishlist: WishlistCell {
        let elem = cellUniqueElement(withIdentifier: ProfileAccessibility.wishlist)
        return WishlistCell(element: elem)
    }

    /// Ячейка "Помощь"
    var help: HelpCell {
        let elem = cellUniqueElement(withIdentifier: ProfileAccessibility.help)
        return HelpCell(element: elem)
    }

    // Ячейка "Помощь рядом"
    var helpIsNear: HelpIsNearCell {
        let elem = cellUniqueElement(withIdentifier: ProfileAccessibility.HelpIsNear.container)
        return HelpIsNearCell(element: elem)
    }

    /// Ячейка "Открыть постамат Яндекс.Маркет"
    var postamate: PostamateCell {
        let elem = cellUniqueElement(withIdentifier: ProfileAccessibility.postamate)
        return PostamateCell(element: elem)
    }

    /// Ячейка "Мои покупки"
    var myPurchases: MyPurchasesCell {
        let elem = cellUniqueElement(withIdentifier: ProfileAccessibility.myPurchases)
        return MyPurchasesCell(element: elem)
    }

    /// Ячейка "Настройки"
    var settings: SettingsCell {
        let elem = cellUniqueElement(withIdentifier: ProfileAccessibility.settings)
        return SettingsCell(element: elem)
    }

    /// Зеленый тултип
    var hint: HintPage {
        let elem = element
            .otherElements
            .matching(identifier: HintAccessibility.root)
            .firstMatch
        return HintPage(element: elem)
    }

    // Ячейка "Реферальной программы"
    var referral: ReferralCell {
        let elem = cellUniqueElement(withIdentifier: ProfileAccessibility.Referral.container)
        return ReferralCell(element: elem)
    }

    // Ячейка растущего кешбэка
    var growingCashback: GrowingCashbackCell {
        let elem = cellUniqueElement(withIdentifier: ProfileAccessibility.GrowingCashback.container)
        return GrowingCashbackCell(element: elem)
    }
}

// MARK: - Nested types

extension ProfilePage {

    final class PerkCell: PageObject {

        /// Заголовок ячейки
        var title: XCUIElement {
            element
                .staticTexts
                .firstMatch
        }
    }

    final class MyOrdersCell: PageObject, OrdersListEntryPoint {

        /// Заголовок ячейки
        var title: XCUIElement {
            element
                .staticTexts
                .firstMatch
        }
    }

    final class MyBonusesCell: PageObject {

        /// Заголовок ячейки
        var title: XCUIElement {
            element
                .staticTexts
                .firstMatch
        }

        /// Происходит переход в экран с купонами пользователя
        func tap() -> SmartshoppingPage {
            element.tap()

            let elem = XCUIApplication()
                .otherElements
                .matching(identifier: SmartshoppingAccessibility.container)
                .firstMatch
            return SmartshoppingPage(element: elem)
        }
    }

    final class MyPublications: PageObject {

        /// Заголовок ячейки
        var title: XCUIElement {
            element
                .staticTexts
                .firstMatch
        }

        /// Происходит переход в экран с заказами пользователя
        @discardableResult
        func tap() -> MyOpinionsPage {
            element.tap()

            let elem = XCUIApplication()
                .otherElements
                .matching(identifier: MyOpinionsAccessibility.root)
                .firstMatch
            return MyOpinionsPage(element: elem)
        }

    }

    final class PushNotifications: PageObject {

        /// Происходит переход в экран настроек  пушей
        @discardableResult
        func tap() -> PushSubscriptionsPage {
            element.tap()

            let elem = XCUIApplication()
                .otherElements
                .matching(identifier: PushMultiSettingsAccessibility.root)
                .firstMatch
            return PushSubscriptionsPage(element: elem)
        }
    }

    final class WishlistCell: PageObject {

        /// Происходит переход в экран с избранными товарами пользователя
        func tap() -> WishlistPage {
            element.tap()

            let elem = XCUIApplication()
                .otherElements
                .matching(identifier: WishlistAccessibility.root)
                .firstMatch
            return WishlistPage(element: elem)
        }
    }

    final class ComparisonsCell: PageObject {

        /// Происходит переход в экран со списком сравнений пользователя
        func tap() -> ComparisonListPage {
            element.tap()

            let elem = XCUIApplication()
                .otherElements
                .matching(identifier: ComparisonListAccessibility.root)
                .firstMatch
            return ComparisonListPage(element: elem)
        }
    }

    final class HelpCell: PageObject, WebviewEntryPoint {

        /// Заголовок ячейки
        var title: XCUIElement {
            element.staticTexts.firstMatch
        }
    }

    final class PostamateCell: PageObject {

        /// Заголовок ячейки
        var title: XCUIElement {
            element.staticTexts.firstMatch
        }

    }

    final class MyPurchasesCell: PageObject {

        /// Происходит переход в мои покупки
        func tap() -> RecentProductsPage {
            element.tap()

            let elem = XCUIApplication()
                .otherElements
                .matching(identifier: RecentProductsAccessibility.root)
                .firstMatch
            return RecentProductsPage(element: elem)
        }
    }

    final class SettingsCell: PageObject {

        /// Происходит переход в экран с настройками
        func tap() -> SettingsPage {
            element.tap()

            let elem = XCUIApplication()
                .otherElements
                .matching(identifier: SettingsAccessibility.root)
                .firstMatch
            return SettingsPage(element: elem)
        }
    }

    final class YandexPlusCell: PageObject {

        /// Заголовок ячейки
        var title: XCUIElement {
            element.staticTexts
                .matching(identifier: ProfileAccessibility.YandexPlus.title)
                .firstMatch
        }

        /// Описание
        var description: XCUIElement {
            element.staticTexts
                .matching(identifier: ProfileAccessibility.YandexPlus.description)
                .firstMatch
        }

        /// Происходит переход на промостраницу кешбэка
        func tap() -> WebViewPage {
            element.tap()

            let elem = XCUIApplication()
                .otherElements
                .matching(identifier: SettingsAccessibility.root)
                .firstMatch
            return WebViewPage(element: elem)
        }
    }

    final class HelpIsNearCell: PageObject {

        var title: XCUIElement {
            element.staticTexts
                .matching(identifier: ProfileAccessibility.HelpIsNear.title)
                .firstMatch
        }

        var subtitle: XCUIElement {
            element.staticTexts
                .matching(identifier: ProfileAccessibility.HelpIsNear.subtitle)
                .firstMatch
        }

        var value: XCUIElement {
            element.staticTexts
                .matching(identifier: ProfileAccessibility.HelpIsNear.value)
                .firstMatch
        }

        /// Происходит переход на лендинг помощи рядом
        func tap() -> WebViewPage {
            element.tap()
            return WebViewPage.current
        }
    }

    final class ReferralCell: PageObject {

        var title: XCUIElement {
            element.staticTexts
                .matching(identifier: ProfileAccessibility.Referral.title)
                .firstMatch
        }

        var subtitle: XCUIElement {
            element.staticTexts
                .matching(identifier: ProfileAccessibility.Referral.subtitle)
                .firstMatch
        }

        @discardableResult
        func tap() -> ReferralPromocodePage {
            element.tap()
            return ReferralPromocodePage.current
        }
    }

    final class GrowingCashbackCell: PageObject {

        var title: XCUIElement {
            element
                .staticTexts
                .matching(identifier: ProfileAccessibility.GrowingCashback.title)
                .firstMatch
        }

        var iconImageView: XCUIElement {
            element
                .images
                .matching(identifier: ProfileAccessibility.GrowingCashback.imageView)
                .firstMatch
        }

        var closeButton: XCUIElement {
            element
                .buttons
                .matching(identifier: ProfileAccessibility.GrowingCashback.closeButton)
                .firstMatch
        }

        @discardableResult
        func tap() -> PageObject {
            element.tap()
            return GrowingCashbackPromoPage.current
        }
    }
}

// MARK: - CollectionViewPage

extension ProfilePage: CollectionViewPage {

    typealias AccessibilityIdentifierProvider = ProfileCollectionViewCellsAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }
}
