import MarketUITestMocks
import XCTest

final class PromocodeSKUCardTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testPromocodeDisplayingOnSKUCard() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3940")
        Allure.addEpic("КМ. Промокод")
        Allure.addFeature("Корректное отображение ДО с промокодом")

        var sku: SKUPage!
        var promocodePopup: PromocodePopupPage!

        "Настраиваем FT".ybm_run { _ in
            enable(
                toggles:
                FeatureNames.promocode_by_promo,
                FeatureNames.promocodeAutoApplyingV2
            )
        }

        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)

        "Мокаем startup для получения эксперимента promocode_auto_applying_enabled_test".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Experiments_Promocode_Autoapplying")
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Promocode_SKU")
        }

        "Открываем КМ".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Проверяем отображение цены в ДО".ybm_run { _ in
            let price = sku.price.price
            sku.collectionView.ybm_swipeCollectionView(toFullyReveal: price)
            XCTAssertEqual(price.label, "277 ₽")
        }

        let promocode = sku.promocode

        "Проверяем наличие блока промокода в ДО".ybm_run { _ in
            sku.collectionView.swipe(to: .down, until: promocode.element.isVisible)
            XCTAssertEqual(promocode.label, "Еще -25% по промокоду UPAKWDSK до 29 августа")
        }

        "Открываем попап с информацией о промокоде".ybm_run { _ in
            promocodePopup = promocode.tap()
        }

        "Проверяем попап с информацией о промокоде".ybm_run { _ in
            wait(forVisibilityOf: promocodePopup.title)
            XCTAssertEqual(promocodePopup.title.label, "Промокод UPAKWDSK на скидку 25% применится в корзине")
            XCTAssertEqual(promocodePopup.info.label, "Он действует до 29 августа ")
            XCTAssertEqual(promocodePopup.basePrice.label, "277 ₽")
            XCTAssertEqual(promocodePopup.discount.label, "-69 ₽")
            XCTAssertEqual(promocodePopup.totalPrice.label, "208 ₽")
            XCTAssertTrue(promocodePopup.addToCart.isHittable)
        }

        "Открываем все товары".ybm_run { _ in
            promocodePopup.showAllWithPromocode.tap()
        }

        let webView = WebViewPage.current

        "Проверяем открытие web view".ybm_run { _ in
            wait(forVisibilityOf: webView.element)
        }

        "Закрываем web view".ybm_run { _ in
            webView.navigationBar.backButton.tap()
        }

        "Проверяем отображение кнопки \"Добавить в корзину\" в ДО".ybm_run { _ in
            wait(forVisibilityOf: sku.element)
            let addToCartButton = sku.addToCartButton.element
            sku.collectionView.ybm_swipeCollectionView(toFullyReveal: addToCartButton)
            XCTAssertTrue(addToCartButton.isHittable)
        }
    }

    func testAddingToCartProductWithPromocode() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4779")
        Allure.addEpic("КМ. Промокод")
        Allure.addFeature("Добавление товара с промокодом в корзину")

        var sku: SKUPage!
        var cartPage: CartPage!

        "Настраиваем FT".ybm_run { _ in
            enable(
                toggles:
                FeatureNames.promocode_by_promo,
                FeatureNames.promocodeAutoApplyingV2
            )
            disable(toggles: FeatureNames.cartRedesign)
        }

        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)

        "Мокаем startup для получения эксперимента promocode_auto_applying_enabled_test".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Experiments_Promocode_Autoapplying")
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Promocode_SKU")
        }

        "Открываем КМ".ybm_run { _ in
            sku = goToDefaultSKUPage()
            wait(forVisibilityOf: sku.element)
        }

        "Мокам состояние доавления к корзину".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Promocode_SKU_AddToCart")
        }

        "Добавляем товар в корзину".ybm_run { _ in
            let addToCartButton = sku.addToCartButton.element
            sku.collectionView.ybm_swipeCollectionView(toFullyReveal: addToCartButton)
            addToCartButton.tap()

        }

        "Мокаем состояние корзины".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Promocode_SKU_AddedToCart")
        }

        "Переходим в корзину".ybm_run { _ in
            cartPage = sku.addToCartButton.tap()
            wait(forVisibilityOf: cartPage.element)
        }

        "Проверяем состояние корзины".ybm_run { _ in
            let promocode = cartPage.promocode
            cartPage.collectionView.ybm_swipeCollectionView(toFullyReveal: promocode.element)
            XCTAssertEqual(promocode.input.text, "UPAKWDSK")

            let discount = cartPage.summary.promocodeDiscount.details
            cartPage.collectionView.ybm_swipeCollectionView(toFullyReveal: discount)
            XCTAssertEqual(discount.label, "-70 ₽")

            let total = cartPage.summary.totalPrice.details
            cartPage.collectionView.ybm_swipeCollectionView(toFullyReveal: total)
            XCTAssertEqual(total.label, "207 ₽")
        }
    }
}
