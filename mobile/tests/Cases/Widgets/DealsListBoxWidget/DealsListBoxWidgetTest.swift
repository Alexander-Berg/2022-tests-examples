import Foundation
import MarketUITestMocks
import XCTest

final class DealsListBoxWidgetTest: LocalMockTestCase {

    func testWidgetSnippetsAppearance() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3010")
        Allure.addEpic("Морда")
        Allure.addFeature("Акционные товары")
        Allure.addTitle("Только сегодня")

        var mordaPage: MordaPage!
        var widget: MordaPage.DealsWidget!

        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            let defaultSKU = modify(SKUInfoCollections.default) { sku in
                sku.promo = [.cheapestAsGift]
            }
            skuState.setSkuInfoProductOffersWithHyperIdState(
                with: .init(
                    results: .several(SKUFAPIResults.default, SKUFAPIResults.protein),
                    collections: .several(defaultSKU, .protein)
                )
            )
            stateManager?.setState(newState: skuState)
        }

        "Открываем морду".ybm_run { _ in
            mordaPage = goToMorda()
            widget = mordaPage.dealsWidget
        }

        "Проверяем сниппеты".ybm_run { _ in
            var previousSnippets: [String] = []
            func testSnippet(snippetInfo: SnippetInfo) {
                let snippet = widget.cellPage(after: previousSnippets)
                widget.collectionView.ybm_swipeCollectionView(toFullyReveal: snippet.element)

                verifySnippet(snippet: snippet, snippetInfo: snippetInfo)

                previousSnippets.append(snippet.element.identifier)
            }

            let snippetsInfo = generateSnippetsInfo()
            snippetsInfo.forEach(testSnippet)
        }
    }

    private func verifySnippet(
        snippet: SnippetPage,
        snippetInfo: SnippetInfo
    ) {
        XCTAssertEqual(snippet.priceLabel.label, snippetInfo.price)
        XCTAssertEqual(snippet.titleLabel.label, snippetInfo.skuName)
        XCTAssertEqual(snippet.ratingView.label, snippetInfo.starsValue)
        XCTAssertEqual(snippet.reviewsCountLabel.label, snippetInfo.reviewsCountLabel)

        // опциональная инфа
        // скидка
        if let oldPrice = snippetInfo.oldPrice, let discount = snippetInfo.discountPercent {
            XCTAssertEqual(snippet.oldPriceLabel.label, oldPrice)
            XCTAssertEqual(snippet.discountBadge.discount.label, discount)
        } else {
            XCTAssertFalse(snippet.oldPriceLabel.exists)
            XCTAssertFalse(snippet.discountBadge.discount.exists)
        }

        if snippetInfo.hasGift {
            XCTAssertTrue(snippet.giftView.imageView.element.isVisible)
        } else {
            XCTAssertFalse(snippet.giftView.imageView.element.isVisible)
        }
    }

    private func generateSnippetsInfo() -> [SnippetInfo] {
        [
            SnippetInfo(
                price: "81\u{00A0}990\u{00A0}₽",
                discountPercent: "-\u{2009}14%",
                oldPrice: "94\u{00A0}990\u{00A0}₽",
                starsValue: "Рейтинг: 4.5 из 5",
                reviewsCountLabel: "89 отзывов",
                skuName: "Смартфон Apple iPhone 12 256GB, синий",
                isInCart: false,
                reasonsText: "86% рекомендуют",
                isSoldOut: false,
                hasGift: false,
                hasCheapestAsGift: true
            ),
            SnippetInfo(
                price: "81\u{00A0}990\u{00A0}₽",
                discountPercent: "-\u{2009}14%",
                oldPrice: "94\u{00A0}990\u{00A0}₽",
                starsValue: "Рейтинг: 4.5 из 5",
                reviewsCountLabel: "89 отзывов",
                skuName: "Смартфон Apple iPhone 12 256GB, синий",
                isInCart: false,
                reasonsText: "86% рекомендуют",
                isSoldOut: false,
                hasGift: false,
                hasCheapestAsGift: true
            )
        ]
    }
}
