import MarketUI
import XCTest

/// Ячейка товара в поисковой выдаче и в вишлисте
final class FeedSnippetPage: PageObject, SKUEntryPoint {
    /// Старая цена (зачеркнутая)
    var oldPriceLabel: XCUIElement {
        element.staticTexts.matching(identifier: FeedSnippetAccessibility.oldPrice).firstMatch
    }

    /// Кнопка добавления в корзину
    var addToCartButton: CartButtonPage {
        let elem = element.buttons.matching(identifier: FeedSnippetAccessibility.addToCart).firstMatch
        return CartButtonPage(element: elem)
    }

    /// Название товара
    var titleLabel: XCUIElement {
        element.buttons.matching(identifier: FeedSnippetAccessibility.title).firstMatch
    }

    /// Имя вендора
    var vendorLabel: XCUIElement {
        element.staticTexts.matching(identifier: FeedSnippetAccessibility.vendorTitle).firstMatch
    }

    /// Картинка товара
    var imageView: XCUIElement {
        element.images.matching(identifier: FeedSnippetAccessibility.imageView).firstMatch
    }

    // Вьюшка "товар закончился"
    var soldOutView: XCUIElement {
        element.staticTexts.matching(identifier: FeedSnippetAccessibility.soldOut).firstMatch
    }

    // Вьюшка "Скоро в продаже"
    var superHypeView: XCUIElement {
        element.staticTexts.matching(identifier: FeedSnippetAccessibility.superHype).firstMatch
    }

    // Лейбл Б/У Ресейл
    var resaleLabel: XCUIElement {
        element.staticTexts.matching(identifier: FeedSnippetAccessibility.resale).firstMatch
    }

    /// Бейдж скидки
    var saleBadge: DiscountBadgeViewPage {
        let page = element.otherElements
            .matching(identifier: DiscountBadgeViewAccessibility.root)
            .firstMatch
        return DiscountBadgeViewPage(element: page)
    }

    /// Текущая цена (красная)
    var currentPrice: XCUIElement {
        element.staticTexts.matching(identifier: FeedSnippetAccessibility.currPrice).firstMatch
    }

    /// Цена за единицу (1 100 ₽ / м²)
    var unitPrice: XCUIElement {
        element.staticTexts.matching(identifier: FeedSnippetAccessibility.unitPrice).firstMatch
    }

    /// Ежемесячный платеж (кредиты)
    var creditInfo: XCUIElement {
        element
            .staticTexts.matching(identifier: FeedSnippetAccessibility.creditInfo).firstMatch
    }

    /// Название финансового продукта
    var financialProductName: XCUIElement {
        element
            .staticTexts
            .matching(identifier: FeedSnippetAccessibility.financialProductName)
            .firstMatch
    }

    /// Сумма регулярного или всего платежа
    var financialProductPaymentAmount: XCUIElement {
        element
            .staticTexts
            .matching(identifier: FeedSnippetAccessibility.financialProductPaymentAmount)
            .firstMatch
    }

    /// Период регулярного платежа или общий срок кредитования
    var financialProductPaymentPeriod: XCUIElement {
        element
            .staticTexts
            .matching(identifier: FeedSnippetAccessibility.financialProductPaymentPeriod)
            .firstMatch
    }

    /// Кэшбек (баллы плюса)
    var cashback: XCUIElement {
        element
            .staticTexts.matching(identifier: FeedSnippetAccessibility.cashback).firstMatch
    }

    /// Кол-во отзывов
    var numberOfReviews: XCUIElement {
        element.staticTexts.matching(identifier: FeedSnippetAccessibility.reviewsNumber).firstMatch
    }

    /// Рейтинг товара (звездочки)
    var ratingStars: XCUIElement {
        element.images.matching(identifier: FeedSnippetAccessibility.ratingView).firstMatch
    }

    /// Рейтинг товара в виде текста, без звездочек
    var ratingLabel: XCUIElement {
        element.staticTexts.matching(identifier: FeedSnippetAccessibility.ratingView).firstMatch
    }

    var triggersLabel: XCUIElement {
        element.staticTexts.matching(identifier: FeedSnippetAccessibility.triggers).firstMatch
    }

    /// Кнопка добавления в вишлист
    var wishListButton: XCUIElement {
        element.buttons.matching(identifier: FeedSnippetAccessibility.addToWishList).firstMatch
    }

    /// Кнопка добавления в список сравнения
    var comparsionButton: XCUIElement {
        element.buttons.matching(identifier: ComparisonButtonAccessibility.button).firstMatch
    }

    /// Причины купить (N% рекомендуют)
    ///
    /// Вложенная вьюшка в containerView
    final class ReasonToBuyPage: PageObject {
        var reasonLabel: XCUIElement {
            element.staticTexts.matching(identifier: SnippetsAccessibility.reasonToBuy).firstMatch
        }
    }

    var reasonsToBuyRecomendations: ReasonToBuyPage {
        let page = element
            .staticTexts.matching(identifier: FeedSnippetAccessibility.reasonsToBuy).firstMatch
        return ReasonToBuyPage(element: page)
    }

    var deliveryLabel: XCUIElement {
        element.staticTexts.matching(identifier: FeedSnippetAccessibility.delivery).firstMatch
    }

    /// Дисклеймер
    var disclaimerLabel: XCUIElement {
        element.staticTexts.matching(identifier: FeedSnippetAccessibility.disclaimer).firstMatch
    }

    var giftViewPage: GiftViewPage {
        let page = element.otherElements
            .matching(identifier: GiftViewAccessibility.root)
            .firstMatch
        return GiftViewPage(element: page)
    }

    var cheapestAsGiftView: CheapestAsGiftViewPage {
        let page = element.otherElements
            .matching(identifier: CheapestAsGiftAccessibility.root)
            .firstMatch
        return CheapestAsGiftViewPage(element: page)
    }

    var promoBadge: PromoBadgePage {
        PromoBadgePage(element: element)
    }

    /// Текущая цена (красная)
    var discountLabel: XCUIElement {
        element.staticTexts.matching(identifier: FeedSnippetAccessibility.discountLabel).firstMatch
    }

    /// Таймер флеш акции
    var flashTimer: XCUIElement {
        element
            .staticTexts
            .matching(identifier: FeedSnippetAccessibility.flashTimer)
            .firstMatch
    }
}
