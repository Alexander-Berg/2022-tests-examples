import MarketUITestMocks
import XCTest

final class SKUUpsellPopupTestCase: LocalMockTestCase {

    func testPopupDismissedWhenSKUDismissed() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6015")
        Allure.addEpic("КМ")
        Allure.addFeature("Попап рекомендаций Upsell")
        Allure.addTitle("Попап скрывается при уходе с КТ")

        var upsellPage: SKUUpsellPage!

        "Настраиваем тоглы".ybm_run { _ in
            enable(toggles: FeatureNames.upsell)
        }

        openSKUUpsellPopup()

        "Нажимаем на Перейти в корзину".ybm_run { _ in
            upsellPage = SKUUpsellPage.current
            let productInCartWidget = upsellPage.productInCartWidget
            productInCartWidget.goToCartButton.tap()

            let tabBarPage = TabBarPage.current
            let cartPage = tabBarPage.cartPage
            XCTAssertTrue(cartPage.element.waitForExistence(timeout: XCTestCase.defaultTimeOut))
        }

        "Возвращаемся на КМ: попапа нет".ybm_run { _ in
            TabBarPage.current.mordaTabItem.element.tap()

            let skuPage = SKUPage.current
            XCTAssertTrue(skuPage.element.waitForExistence(timeout: XCTestCase.defaultTimeOut))

            upsellPage = SKUUpsellPage.current
            XCTAssertFalse(upsellPage.element.waitForExistence(timeout: XCTestCase.defaultTimeOut))
        }

        "Устанавливаем таймаут ответа мок-сервера".ybm_run { _ in
            mockServer?.stop()
            mockServer?.responseTimeout = 1
            mockServer?.start()
        }

        "Добавляем товар в корзину, переходим на другой тап до открытия попапа: попапа нет".ybm_run { _ in
            SKUPage.current.addToCartButton.element.tap()
            TabBarPage.current.cartTabItem.element.tap()

            upsellPage = SKUUpsellPage.current
            XCTAssertFalse(upsellPage.element.waitForExistence(timeout: XCTestCase.defaultTimeOut))
        }

        "Возвращаемся на КМ: попапа нет".ybm_run { _ in
            TabBarPage.current.mordaTabItem.element.tap()

            let skuPage = SKUPage.current
            XCTAssertTrue(skuPage.element.waitForExistence(timeout: XCTestCase.defaultTimeOut))

            upsellPage = SKUUpsellPage.current
            XCTAssertFalse(upsellPage.element.waitForExistence(timeout: XCTestCase.defaultTimeOut))
        }
    }

    func testPopupDismissedWhenSwipedDown() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5892")
        Allure.addEpic("КМ")
        Allure.addFeature("Попап рекомендаций Upsell")
        Allure.addTitle("Скрытие попапа свайпом вниз")

        "Настраиваем тоглы".ybm_run { _ in
            enable(toggles: FeatureNames.upsell)
        }

        openSKUUpsellPopup()

        "Свайпнуть попап вниз: попап скрылся".ybm_run { _ in
            SKUUpsellPage.current.element.swipeDown()

            XCTAssertFalse(SKUUpsellPage.current.element.waitForExistence(timeout: XCTestCase.defaultTimeOut))
        }
    }
}

// MARK: - Private

private extension SKUUpsellPopupTestCase {

    func openSKUUpsellPopup() {
        "Настраиваем стейт".ybm_run { _ in
            setupSKUState()
        }

        "Открываем SKU".ybm_run { _ in
            goToDefaultSKUPage()
        }

        "Мокаем корзину".ybm_run { _ in
            setupCartState()
        }

        "Добавляем в корзину".ybm_run { _ in
            SKUPage.current.addToCartButton.element.tap()
        }

        "Открываем попап: появился попап".ybm_run { _ in
            XCTAssertTrue(SKUUpsellPage.current.element.waitForExistence(timeout: XCTestCase.defaultTimeOut))
        }
    }

    func setupSKUState() {
        stateManager?.mockingStrategy = .dtoMock

        var skuUpsellState = SKUUpsellState()
        skuUpsellState.setSkuInfoState(with: .iphone)
        stateManager?.setState(newState: skuUpsellState)

        var cmsState = CMSState()
        cmsState.setCMSState(with: .skuUpsellCollections)
        stateManager?.setState(
            newState: cmsState,
            matchedBy: hasStringInBody("\"type\":\"mp_product_upsell_app\"")
        )
    }

    func setupCartState() {
        var cartState = CartState()
        cartState.setCartStrategy(with: [.iphone])
        cartState.addItemsToCartState(with: .iphone)
        stateManager?.setState(newState: cartState)
    }
}
