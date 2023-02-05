import MarketUITestMocks
import XCTest

final class ScrollBoxWidgetTest: LocalMockTestCase {

    func testWidgetSnippetsAppearance() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3206")

        Allure.addEpic("Виджеты")
        Allure.addFeature("Акции 2=3")
        Allure.addTitle("Многим нравится. Проверка отображения сниппетов и информации в них в скроллбокс-виджете")

        var mordaPage: MordaPage!

        "Открываем морду".ybm_run { _ in
            mordaPage = goToMorda()
        }

        "Тестируем виджет \"Многим нравится\"".ybm_run { _ in
            let widget = mordaPage.popularProductsWidget

            widget.collectionView.ybm_swipeCollectionView(toFullyReveal: widget.title)
            XCTAssertEqual(widget.title.label, "Многим нравится")

            widget.collectionView.ybm_swipeCollectionView(toFullyReveal: widget.container.element)
            XCTAssertTrue(widget.container.collectionView.isVisible)

            // массив мок данных
            let snippets = generateSnippetsInfo()

            // тест сниппетов
            verify(in: widget.collectionView, container: widget.container, snippets: snippets)
        }
    }

    func testSnippetOpenSKU() {
        Allure.addEpic("Виджеты")
        Allure.addFeature("Scrollbox")
        Allure.addTitle("Проверка перехода на экран КМ после тапа на сниппет")

        var mordaPage: MordaPage!
        var widget: MordaPage.PopularProductsWidget!

        func testTransitionToSKUCard(from snippet: SnippetPage) {
            let skuFromWidget = snippet.tap() // Переход на КМ товара из виджета
            ybm_wait(forFulfillmentOf: { () -> Bool in
                skuFromWidget.didFinishLoadingInfo
            })
            skuFromWidget.navigationBar.backButton.tap()
        }

        "Открываем морду".ybm_run { _ in
            mordaPage = goToMorda()
            widget = mordaPage.popularProductsWidget
            widget.collectionView.ybm_swipeCollectionView(toFullyReveal: widget.title)
        }

        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            skuState.setSkuInfoState(with: .default)
            stateManager?.setState(newState: skuState)
        }

        "Проверяем переход из первого сниппета".ybm_run { _ in
            let firstSnippet = widget.container.cellPage(at: IndexPath(item: 0, section: 0))
            widget.collectionView.ybm_swipeCollectionView(toFullyReveal: firstSnippet.element)
            testTransitionToSKUCard(from: firstSnippet)
        }

        "Проверяем переход из второго сниппета".ybm_run { _ in
            "Мокаем нужную SKU".ybm_run { _ in
                mockStateManager?.pushState(bundleName: "WidgetsSet_SKUUsb")
            }

            let secondSnippet = widget.container.cellPage(at: IndexPath(item: 1, section: 0))
            testTransitionToSKUCard(from: secondSnippet)
        }
    }

    // MARK: - Private

    /// Данные на сниппетах для виджета "Многим нравится" на морде.
    /// 3 сниппетa на все случаи жизни
    private func generateSnippetsInfo() -> [SnippetInfo] {
        [
            SnippetInfo(
                price: "1 190 ₽",
                discountPercent: nil,
                oldPrice: nil,
                starsValue: "",
                reviewsCountLabel: "",
                skuName: "Протеин CMTech (Банан)",
                isInCart: false,
                reasonsText: nil,
                isSoldOut: false,
                hasGift: false,
                hasCheapestAsGift: false
            ),
            SnippetInfo(
                price: "840 ₽",
                discountPercent: nil,
                oldPrice: nil,
                starsValue: "",
                reviewsCountLabel: "",
                skuName: "Протеин CMTech Whey Protein (900 г) нейтральный",
                isInCart: false,
                reasonsText: nil,
                isSoldOut: false,
                hasGift: false,
                hasCheapestAsGift: false
            ),
            SnippetInfo(
                price: "1 190 ₽",
                discountPercent: nil,
                oldPrice: nil,
                starsValue: "",
                reviewsCountLabel: "",
                skuName: "Протеин CMTech (Банан)",
                isInCart: false,
                reasonsText: nil,
                isSoldOut: false,
                hasGift: false,
                hasCheapestAsGift: false
            )
        ]
    }

    private func verify<T: SnippetPage>(
        in collectionView: XCUIElement,
        container: ScrollBoxSnipppetContainerPage<T>,
        snippets: [SnippetInfo]
    ) {
        container.enumerateCells(cellCount: snippets.count) { snippet, indexPath in
            let snippetInfo = snippets[indexPath.item]

            // разобрали
            guard !snippetInfo.isSoldOut else {
                collectionView.ybm_swipeCollectionView(toFullyReveal: snippet.soldOutView)
                XCTAssertTrue(snippet.soldOutImage.exists)
                XCTAssertTrue(snippet.soldOutView.exists)
                XCTAssertFalse(snippet.priceLabel.exists)
                XCTAssertFalse(snippet.oldPriceLabel.exists)
                XCTAssertFalse(snippet.addToCartButton.element.exists)
                XCTAssertFalse(snippet.discountBadge.discount.exists)
                XCTAssertFalse(snippet.reviewsCountLabel.exists)
                XCTAssertFalse(snippet.ratingView.exists)
                XCTAssertFalse(snippet.reasonToBuy.exists)
                return
            }

            collectionView.ybm_swipeCollectionView(toFullyReveal: snippet.priceLabel)
            XCTAssertEqual(snippet.priceLabel.label, snippetInfo.price)

            collectionView.ybm_swipeCollectionView(toFullyReveal: snippet.image)
            wait(forExistanceOf: snippet.image)

            collectionView.ybm_swipeCollectionView(toFullyReveal: snippet.titleLabel)
            XCTAssertEqual(snippet.titleLabel.label, snippetInfo.skuName)
            XCTAssertFalse(snippet.soldOutView.exists)
            XCTAssertFalse(snippet.soldOutImage.exists)

            // опциональная инфа
            // скидка
            if let oldPrice = snippetInfo.oldPrice, let discount = snippetInfo.discountPercent {
                collectionView.ybm_swipeCollectionView(toFullyReveal: snippet.oldPriceLabel)
                XCTAssertEqual(snippet.oldPriceLabel.label, oldPrice)
                XCTAssertEqual(snippet.discountBadge.discount.label, discount)
            } else {
                XCTAssertFalse(snippet.oldPriceLabel.exists)
                XCTAssertFalse(snippet.discountBadge.discount.exists)
            }

            // причины
            if let reasons = snippetInfo.reasonsText {
                collectionView.ybm_swipeCollectionView(toFullyReveal: snippet.reasonToBuy)
                XCTAssertEqual(snippet.reasonToBuy.label, reasons)
            } else {
                XCTAssertFalse(snippet.reasonToBuy.exists)
            }

            if snippetInfo.hasGift {
                collectionView.ybm_swipeCollectionView(toFullyReveal: snippet.giftView.element)
                XCTAssertTrue(snippet.giftView.imageView.element.isVisible)
            } else {
                XCTAssertFalse(snippet.giftView.imageView.element.isVisible)
            }

            if snippetInfo.hasCheapestAsGift {
                collectionView.ybm_swipeCollectionView(toFullyReveal: snippet.cheapestAsGiftView.element)
                XCTAssertTrue(snippet.cheapestAsGiftView.imageView.isVisible)
            } else {
                XCTAssertFalse(snippet.cheapestAsGiftView.imageView.isVisible)
            }
        }
    }
}
