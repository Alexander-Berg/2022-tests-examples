import MarketUITestMocks
import XCTest

final class SKUUpsellWidgetTestCase: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testPopupContent() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5893")
        Allure.addEpic("КМ")
        Allure.addFeature("Попап рекомендаций Upsell")
        Allure.addTitle("Состав попапа соответсвует открытой КТ")

        openUpsellPopup()

        "Появился попап, состав соответствует открытой КТ".ybm_run { _ in
            checkPopupContent(name: "Смартфон Apple iPhone 12 256GB, синий", price: "81 990 ₽")
        }

        "Мокаем вторую SKU".ybm_run { _ in
            var skuUpsellState = SKUUpsellState()
            skuUpsellState.setSkuInfoState(with: .protein)
            stateManager?.setState(newState: skuUpsellState)
        }

        "Открываем SKU из попапа".ybm_run { _ in
            SKUUpsellPage.current.repeatPurchasesSnippetAt(index: 1).element.tap()
        }

        "Мокаем корзину".ybm_run { _ in
            var cartState = CartState()
            cartState.setCartStrategy(with: [.proteinWithCartId])
            cartState.addItemsToCartState(with: .protein)
            stateManager?.setState(newState: cartState)
        }

        "Добавляем в корзину".ybm_run { _ in
            let skuPage = SKUPage.current
            skuPage.element.ybm_swipeCollectionView(toFullyReveal: skuPage.addToCartButton.element)
            skuPage.addToCartButton.element.tap()
        }

        "Попап повторно не появился".ybm_run { _ in
            let upsellPage = SKUUpsellPage.current
            ybm_wait(forFulfillmentOf: { !upsellPage.element.isVisible })
        }
    }

    func testProductInCartWidget() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5889")
        Allure.addEpic("КМ")
        Allure.addFeature("Попап рекомендаций Upsell")
        Allure.addTitle("Upsell. Виджет Товар в корзине")

        openUpsellPopup()

        "Появился попап, виджет Товар в корзине соответствует КТ".ybm_run { _ in
            let upsellPage = SKUUpsellPage.current
            XCTAssertTrue(upsellPage.element.waitForExistence(timeout: XCTestCase.defaultTimeOut))

            let productInCartWidget = upsellPage.productInCartWidget
            XCTAssertEqual(productInCartWidget.title.label, "Товар в корзине")
            XCTAssertTrue(productInCartWidget.image.waitForExistence(timeout: XCTestCase.defaultTimeOut))
            XCTAssertEqual(productInCartWidget.name.label, "Смартфон Apple iPhone 12 256GB, синий")
            XCTAssertEqual(productInCartWidget.supplier.label, "Яндекс.Маркет")
            XCTAssertEqual(productInCartWidget.price.label, "81 990 ₽")
            XCTAssertEqual(productInCartWidget.oldPrice.label, "94 990₽")
            XCTAssertEqual(productInCartWidget.cashback.label, "Attachment.png, Файл 13")
            XCTAssertEqual(productInCartWidget.promocode.label, "–14 %")
            XCTAssertFalse(productInCartWidget.cartButton.minusButton.isEnabled)
            XCTAssertEqual(productInCartWidget.goToCartButton.label, "Перейти в корзину")
            XCTAssertTrue(productInCartWidget.goToCartButton.isEnabled)
        }
    }

    func testRepeatPurchasesWidget() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5897")
        Allure.addEpic("КМ")
        Allure.addFeature("Попап рекомендаций Upsell")
        Allure.addTitle("Upsell. Виджет 'Не забудьте купить'")

        openUpsellPopup()

        "Вызываем попап Upsell: есть рол-виджет, его заголовок 'Не забудьте купить'".ybm_run { _ in
            XCTAssertEqual(SKUUpsellPage.current.repeatPurchasesTitle.label, "Не забудьте купить")
        }

        "Свайпаем вверх-вниз: лента листается, грид выдача в ряд 2 товара".ybm_run { _ in
            let upsellPage = SKUUpsellPage.current

            let lastSnippet = upsellPage.repeatPurchasesSnippetAt(index: 6)
            upsellPage.element.ybm_swipeCollectionView(toFullyReveal: lastSnippet.element)

            let firstSnippet = upsellPage.repeatPurchasesSnippetAt(index: 1)
            let secondSnippet = upsellPage.repeatPurchasesSnippetAt(index: 2)
            let thirdSnippet = upsellPage.repeatPurchasesSnippetAt(index: 3)
            upsellPage.element.ybm_swipeCollectionView(
                to: .up,
                toFullyReveal: firstSnippet.element
            )

            XCTAssertEqual(
                firstSnippet.element.frame.size.height,
                secondSnippet.element.frame.size.height,
                accuracy: 1e-5
            )
            XCTAssertEqual(
                secondSnippet.element.frame.size.height,
                thirdSnippet.element.frame.size.height,
                accuracy: 1e-5
            )
            XCTAssertNotEqual(
                firstSnippet.element.frame.origin.x,
                secondSnippet.element.frame.origin.x,
                accuracy: 1e-5
            )
            XCTAssertEqual(
                firstSnippet.element.frame.origin.y,
                secondSnippet.element.frame.origin.y,
                accuracy: 1e-5
            )
            XCTAssertEqual(
                firstSnippet.element.frame.origin.x,
                thirdSnippet.element.frame.origin.x,
                accuracy: 1e-5
            )
            XCTAssertNotEqual(
                firstSnippet.element.frame.origin.y,
                thirdSnippet.element.frame.origin.y,
                accuracy: 1e-5
            )
        }

        "Мокаем корзину".ybm_run { _ in
            var cartState = CartState()
            cartState.setCartStrategy(with: [.proteinWithCartId])
            cartState.addItemsToCartState(with: .protein)
            stateManager?.setState(newState: cartState)
        }

        "Нажимаем В корзину: кнопка сменилась на каунтер".ybm_run { _ in
            let snippet = SKUUpsellPage.current.repeatPurchasesSnippetAt(index: 1)

            snippet.addToCartButton.element.tap()
            ybm_wait(forFulfillmentOf: {
                snippet.addToCartButton.element.label == "1"
            })
        }

        "Мокаем КМ".ybm_run { _ in
            var skuState = SKUInfoState()
            skuState.setSkuInfoState(with: .protein)
            stateManager?.setState(newState: skuState)
        }

        "Тапнуть на снипет: перешли на КМ, вместо В корзину - каунтер".ybm_run { _ in
            SKUUpsellPage.current.repeatPurchasesSnippetAt(index: 1).element.tap()

            let skuPage = SKUPage.current
            ybm_wait(forFulfillmentOf: { skuPage.element.isVisible })
            skuPage.element.ybm_swipeCollectionView(toFullyReveal: skuPage.addToCartButton.element)
            XCTAssertEqual(skuPage.addToCartButton.element.label, "1 товар в корзине")
        }
    }

    func testResaleProductInCartWidget() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6303")
        Allure.addEpic("КМ")
        Allure.addFeature("Попап рекомендаций Upsell")
        Allure.addTitle("Upsell. Виджет Товар в корзине. Признак ресейла.")

        openUpsellPopup(true)

        "Появился попап, виджет Товар в корзине соответствует КТ".ybm_run { _ in
            let upsellPage = SKUUpsellPage.current
            XCTAssertTrue(upsellPage.element.waitForExistence(timeout: XCTestCase.defaultTimeOut))

            let productInCartWidget = upsellPage.productInCartWidget
            XCTAssertEqual(productInCartWidget.title.label, "Товар в корзине")
            XCTAssertTrue(productInCartWidget.image.waitForExistence(timeout: XCTestCase.defaultTimeOut))
            XCTAssertEqual(productInCartWidget.name.label, "Смартфон Apple iPhone 12 256GB, синий")
            XCTAssertEqual(productInCartWidget.resale.label, "Ресейл﻿﻿•﻿﻿Яндекс.Маркет")
            XCTAssertEqual(productInCartWidget.price.label, "81 990 ₽")
            XCTAssertEqual(productInCartWidget.oldPrice.label, "94 990₽")
            XCTAssertEqual(productInCartWidget.cashback.label, "Attachment.png, Файл 13")
            XCTAssertEqual(productInCartWidget.promocode.label, "–14 %")
            XCTAssertFalse(productInCartWidget.cartButton.minusButton.isEnabled)
            XCTAssertEqual(productInCartWidget.goToCartButton.label, "Перейти в корзину")
            XCTAssertTrue(productInCartWidget.goToCartButton.isEnabled)
        }
    }
}

// MARK: - Private

private extension SKUUpsellWidgetTestCase {

    func openUpsellPopup(_ isResale: Bool = false) {
        var skuPage: SKUPage!
        var skuInfoMapper: ResolveSKUInfo.Mapper = .iphone
        var offer: FAPIOffer = .iphone
        var itemToCart: AddItemsToCart.AddItemsToCartBody = .iphone

        if isResale {
            skuInfoMapper = .resale
            offer = .resale
            itemToCart = .resale
        }

        "Настраиваем тоглы".ybm_run { _ in
            enable(toggles: FeatureNames.upsell)
            if isResale {
                enableResale()
            }
        }

        "Настраиваем стейт".ybm_run { _ in
            stateManager?.mockingStrategy = .dtoMock

            var skuUpsellState = SKUUpsellState()
            skuUpsellState.setSkuInfoState(with: skuInfoMapper)
            stateManager?.setState(newState: skuUpsellState)

            var cmsState = CMSState()
            cmsState.setCMSState(with: .skuUpsellCollections)
            stateManager?.setState(
                newState: cmsState,
                matchedBy: hasStringInBody("\"type\":\"mp_product_upsell_app\"")
            )

            let offers: [FAPIOffer] = Array(repeating: .protein, count: 8)
            var djState = DJState()
            djState.setDJProductsCollections(mapper: .init(
                title: "Не забудьте купить",
                djMetaPlace: "repeat_purchases_multipopup_block",
                offers: offers
            ))
            stateManager?.setState(newState: djState)
        }

        "Открываем SKU".ybm_run { _ in
            skuPage = goToDefaultSKUPage()
        }

        "Мокаем корзину".ybm_run { _ in
            var cartState = CartState()
            cartState.setCartStrategy(with: [offer])
            cartState.addItemsToCartState(with: itemToCart)
            stateManager?.setState(newState: cartState)
        }

        "Добавляем в корзину".ybm_run { _ in
            skuPage.element.ybm_swipeCollectionView(toFullyReveal: skuPage.addToCartButton.element)
            skuPage.addToCartButton.element.tap()
        }
    }

    func checkPopupContent(name: String, price: String) {
        let upsellPage = SKUUpsellPage.current
        XCTAssertTrue(upsellPage.element.waitForExistence(timeout: XCTestCase.defaultTimeOut))

        let productInCartWidget = upsellPage.productInCartWidget
        XCTAssertTrue(productInCartWidget.image.waitForExistence(timeout: XCTestCase.defaultTimeOut))
        XCTAssertEqual(productInCartWidget.name.label, name)
        XCTAssertEqual(productInCartWidget.price.label, price)

        let snippetName = "Протеин CMTech Whey Protein Клубничный крем, 30 порций"
        let snippetPrice = "1 413 ₽"
        let snippet = upsellPage.repeatPurchasesSnippetAt(index: 1)
        XCTAssertTrue(snippet.image.waitForExistence(timeout: XCTestCase.defaultTimeOut))
        XCTAssertEqual(snippet.titleLabel.label, snippetName)
        XCTAssertEqual(snippet.priceLabel.label, snippetPrice)
    }

    private func enableResale() {
        var defaultState = DefaultState()
        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
        enable(toggles: FeatureNames.marketResaleGoods)
        defaultState.setExperiments(experiments: [.resaleExperiment])
        stateManager?.setState(newState: defaultState)
    }
}
