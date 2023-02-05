import XCTest

final class CartRecommendationsWidgetAdapterTest: LocalMockTestCase {
    /*
      Тест сниппетов виджета YBMCartRecommendationsWidgetAdapter который используется на экранах:
      КМ ("С этим товаром смотрят", "С этим товаром часто покупают"),
      Выдача ("Стоит приглядеться", "Все со скидкой"),
      Корзина ("Не забыть купить", "С этими товарами часто покупают")
     */
    func testWidgetSnippets() {
        Allure.addEpic("Поисковая выдача без товаров")
        Allure.addFeature("Виджеты")
        Allure.addTitle("Проверяем отображение сниппетов")

        var recommendedByHistoryCell: FeedPage.CollectionView.CarouselCell!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "CartRecommendationsWidgetAdapterSet_Basic")
        }

        var feedPage: FeedPage!
        var collectionView: FeedPage.CollectionView!

        "Открываем выдачу".ybm_run { _ in
            feedPage = goToFeed()
            collectionView = feedPage.collectionView
        }

        "Скроллим к виджету".ybm_run { _ in
            recommendedByHistoryCell = collectionView.recommendedByHistoryCell
            feedPage.collectionView.element.swipe(to: .down, untilVisible: recommendedByHistoryCell.element)
        }

        "Проверяем виджет \"Подобрали для вас\"".ybm_run { _ in
            wait(forVisibilityOf: recommendedByHistoryCell.collectionView)

            XCTAssertEqual(recommendedByHistoryCell.title.label, "Подобрали для вас")
            XCTAssertTrue(recommendedByHistoryCell.collectionView.isVisible)
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "CartRecommendationsWidgetAdapterSet_GiftInCart")
        }

        "Нажимаем кнопку \"В корзину\"".ybm_run { _ in
            let snippet = recommendedByHistoryCell.cellPage(at: IndexPath(item: 1, section: 0))
            let button = snippet.addToCartButton
            button.element.tap()

            ybm_wait(forFulfillmentOf: {
                button.element.label == "1"
            })

            ybm_wait(forFulfillmentOf: { TabBarPage.current.cartTabItem.element.label == "Корзина3" })
        }

        "Проверяем сниппеты".ybm_run { _ in
            let snippets = generateSnippetsInfo()
            verifySnippets(snippets: snippets, widget: recommendedByHistoryCell)
        }
    }

    private func generateSnippetsInfo() -> [SnippetInfo] {
        [
            SnippetInfo(
                price: "152 ₽",
                discountPercent: "- 40%",
                oldPrice: "254 ₽",
                starsValue: "Рейтинг: 5.0 из 5",
                reviewsCountLabel: "188 отзывов",
                skuName: "Гель для душа Axe Dark Temptation",
                isInCart: false,
                reasonsText: "93% рекомендуют",
                isSoldOut: false,
                hasGift: false,
                hasCheapestAsGift: false
            ),
            SnippetInfo(
                price: "2 180 ₽",
                discountPercent: "- 34%",
                oldPrice: "3 290 ₽",
                starsValue: "Рейтинг: 4.0 из 5",
                reviewsCountLabel: "3 088 отзывов",
                skuName: "Умный браслет Xiaomi Mi Smart Band 5",
                isInCart: true,
                reasonsText: nil,
                isSoldOut: false,
                hasGift: false,
                hasCheapestAsGift: false
            ),
            SnippetInfo(
                price: "202 ₽",
                discountPercent: "- 39%",
                oldPrice: "329 ₽",
                starsValue: "Рейтинг: 5.0 из 5",
                reviewsCountLabel: "179 отзывов",
                skuName: "Дезодорант спрей Axe Кожа и печеньки",
                isInCart: false,
                reasonsText: "94% рекомендуют",
                isSoldOut: false,
                hasGift: false,
                hasCheapestAsGift: false
            ),
            SnippetInfo(
                price: "204 ₽",
                discountPercent: "- 38%",
                oldPrice: "329 ₽",
                starsValue: "Рейтинг: 5.0 из 5",
                reviewsCountLabel: "634 отзыва",
                skuName: "Дезодорант спрей Axe Dark Temptation",
                isInCart: false,
                reasonsText: "94% рекомендуют",
                isSoldOut: false,
                hasGift: false,
                hasCheapestAsGift: false
            )
        ]
    }

    private func verifySnippets<T: SnippetPage>(snippets: [SnippetInfo], widget: LegacyScrollBoxWidgetPage<T>) {
        widget.enumerateCells(cellCount: snippets.count) { snippet, indexPath in
            let snippetInfo = snippets[indexPath.item]

            // разобрали
            guard !snippetInfo.isSoldOut else {
                XCTAssertTrue(snippet.soldOutImage.exists)
                XCTAssertFalse(snippet.priceLabel.exists)
                XCTAssertFalse(snippet.oldPriceLabel.exists)
                XCTAssertFalse(snippet.addToCartButton.element.exists)
                XCTAssertFalse(snippet.discountBadge.discount.exists)
                XCTAssertTrue(snippet.reviewsCountLabel.exists)
                XCTAssertTrue(snippet.ratingView.exists)
                XCTAssertFalse(snippet.reasonToBuy.isVisible)
                return
            }

            // всегда присутствующая информация
            XCTAssertEqual(snippet.addToCartButton.element.label, snippetInfo.isInCart ? "1" : "В корзину")
            XCTAssertEqual(snippet.priceLabel.label, snippetInfo.price)
            wait(forExistanceOf: snippet.image)
            XCTAssertEqual(snippet.titleLabel.label, snippetInfo.skuName)
            XCTAssertEqual(snippet.ratingView.label, snippetInfo.starsValue)
            XCTAssertEqual(snippet.reviewsCountLabel.label, snippetInfo.reviewsCountLabel)
            XCTAssertFalse(snippet.soldOutImage.exists)

            // опциональная инфа
            // скидка
            if let oldPrice = snippetInfo.oldPrice, let discount = snippetInfo.discountPercent {
                XCTAssertEqual(snippet.oldPriceLabel.label, oldPrice)
                XCTAssertEqual(snippet.discountBadge.discount.label, discount)
            } else {
                XCTAssertFalse(snippet.oldPriceLabel.exists)
                XCTAssertFalse(snippet.discountBadge.discount.exists)
            }

            // причины
            if let reasons = snippetInfo.reasonsText {
                XCTAssertEqual(snippet.reasonToBuy.label, reasons)
            } else {
                XCTAssertTrue(snippet.reasonToBuy.label.isEmpty)
            }

            if snippetInfo.hasGift {
                XCTAssertTrue(snippet.giftView.imageView.element.isVisible)
            } else {
                XCTAssertFalse(snippet.giftView.imageView.element.isVisible)
            }

            if snippetInfo.hasCheapestAsGift {
                XCTAssertTrue(snippet.cheapestAsGiftView.imageView.isVisible)
            } else {
                XCTAssertFalse(snippet.cheapestAsGiftView.imageView.isVisible)
            }
        }
    }

}

extension Optional where Wrapped == String {
    var safe: String {
        switch self {
        case let .some(result):
            return "\"\(result)\""
        default:
            return "nil"
        }
    }
}
