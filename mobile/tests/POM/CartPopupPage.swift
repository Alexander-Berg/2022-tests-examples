import XCTest

class CartPopupPage: PageObject, PopupPage {

    // MARK: - Properties

    static let rootIdentifier = CartPopupAccessibility.root

    class DeliveryBlock: PageObject {
        /// До бесплатной доставки осталось 1 812 ₽
        var title: XCUIElement {
            element.descendants(matching: .staticText).firstMatch
        }
    }

    class RecomendationCell: PageObject, SKUEntryPoint {
        var title: XCUIElement {
            element.staticTexts.matching(identifier: CartPopupAccessibility.cellTitle).firstMatch
        }

        /// Цена
        ///
        /// label = склеенное значение oldPrice и currentPrice
        var price: XCUIElement {
            element.staticTexts.matching(identifier: CartPopupAccessibility.cellPrice).firstMatch
        }

        var addButton: XCUIElement {
            element.buttons.firstMatch
        }
    }

    /// Ячейка в блоке рекомендаций
    /// Содержит 2 ячейки с товарами
    class RecomendationBlockCell: PageObject {

        var right: RecomendationCell {
            let el = element.otherElements.matching(identifier: CartPopupAccessibility.rightCell).firstMatch
            return RecomendationCell(element: el)
        }

        var left: RecomendationCell {
            let el = element.otherElements.matching(identifier: CartPopupAccessibility.leftCell).firstMatch
            return RecomendationCell(element: el)
        }
    }

    class GoToCartButton: PageObject, CartEntryPoint {}

    /// Кнопка для закрытия попапа
    var closeButton: XCUIElement {
        element.buttons.matching(identifier: CartPopupAccessibility.closeButton).firstMatch
    }

    /// Заголовок "Товар добавлен в корзину"
    var title: XCUIElement {
        element.staticTexts.matching(identifier: CartPopupAccessibility.titleLabel).firstMatch
    }

    /// Название товара
    var name: XCUIElement {
        element.staticTexts.matching(identifier: CartPopupAccessibility.nameLabel).firstMatch
    }

    /// Фотография товара
    var image: XCUIElement {
        element.images.matching(identifier: CartPopupAccessibility.image).firstMatch
    }

    /// Дисклеймер
    var disclaimer: XCUIElement {
        element.staticTexts.matching(identifier: CartPopupAccessibility.disclaimerLabel).firstMatch
    }

    /// Стоимость после скидки
    var currentPrice: XCUIElement {
        element.staticTexts.matching(identifier: CartPopupAccessibility.priceLabel).firstMatch
    }

    /// Стоимость до скидки
    var oldPrice: XCUIElement {
        element.staticTexts.matching(identifier: CartPopupAccessibility.oldPriceLabel).firstMatch
    }

    /// Картинка подарка
    var giftImageView: XCUIElement {
        element.images.matching(
            identifier: CartPopupAccessibility.giftImage
        ).firstMatch
    }

    /// Плюс между картинками
    var plusLabel: XCUIElement {
        element.staticTexts.matching(
            identifier: CartPopupAccessibility.plusLabel
        ).firstMatch
    }

    /// Кнопка "Перейти в корзину"
    var goToCartButton: GoToCartButton {
        let el = element
            .buttons
            .matching(identifier: CartPopupAccessibility.goToCartButton).firstMatch

        return GoToCartButton(element: el)
    }

    /// Кнопка "Каунтер"
    var counterButton: CartButtonPage {
        let elem = element.buttons.matching(identifier: CartPopupAccessibility.counterButton).firstMatch
        return CartButtonPage(element: elem)
    }

    /// Блок с кружочком и тайтлом "До бесплатной доставки осталось.."
    var freeDelivery: DeliveryBlock {
        let el = element
            .otherElements
            .matching(identifier: CartPopupAccessibility.freeDeliveryRoot)
            .firstMatch

        return DeliveryBlock(element: el)
    }

    var bundleHeaderView: CartPopupBundleHeaderViewPage {
        let item = element.otherElements
            .matching(identifier: CartPopupBundleHeaderViewAccessibility.root)
            .firstMatch
        return CartPopupBundleHeaderViewPage(element: item)
    }

    /// Ячейки рекомендационных товаров
    ///
    /// Достаем столько, сколько надо ячеек tableView
    func getRecomendationBlockCells(count: Int) -> [RecomendationBlockCell] {
        var result: [RecomendationBlockCell] = []

        for index in 0 ..< count {
            let el = element
                .cells
                .matching(identifier: CartPopupAccessibility.recomendationCell)
                .element(boundBy: index)

            result.append(RecomendationBlockCell(element: el))
        }

        return result
    }

    /// Метод получения ячейки, состоящей из двух ячеек, в блоке рекомендаций
    func getRecommendationBlockCell(at index: Int) -> RecomendationBlockCell {
        let el = element
            .cells
            .matching(identifier: CartPopupAccessibility.recomendationCell)
            .element(boundBy: index)

        return RecomendationBlockCell(element: el)
    }
}

final class CartPopupBundleHeaderViewPage: PageObject {
    class GoToCartButtonPage: PageObject, CartEntryPoint {}

    var goToCartButton: GoToCartButtonPage {
        let item = element.buttons
            .matching(identifier: CartPopupBundleHeaderViewAccessibility.goToCartButton)
            .firstMatch
        return GoToCartButtonPage(element: item)
    }

    var returnButton: XCUIElement {
        element.buttons
            .matching(identifier: CartPopupBundleHeaderViewAccessibility.returnButton)
            .firstMatch
    }

    var bundleView: GiftPickerViewPage {
        let item = element.otherElements
            .matching(identifier: GiftPickerViewAccessibility.root)
            .firstMatch
        return GiftPickerViewPage(element: item)
    }
}
