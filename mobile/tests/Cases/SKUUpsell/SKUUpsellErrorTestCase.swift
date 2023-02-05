import MarketUITestMocks
import XCTest

final class SKUUpsellErrorTestCase: LocalMockTestCase {

    func testNoPopupIfResolveCmsFailed() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5893")
        Allure.addEpic("КМ")
        Allure.addFeature("Попап рекомендаций Upsell")
        Allure.addTitle("Не получен ответ на resolveCms")

        var sku: SKUPage!

        "Настраиваем тоглы".ybm_run { _ in
            enable(toggles: FeatureNames.upsell)
        }

        "Настраиваем стейт".ybm_run { _ in
            stateManager?.mockingStrategy = .dtoMock

            var skuState = SKUInfoState()
            skuState.setSkuInfoState(with: .default)
            stateManager?.setState(newState: skuState)

            var cmsState = CMSState()
            cmsState.setCMSState(with: .errorCollections)
            stateManager?.setState(
                newState: cmsState,
                matchedBy: hasStringInBody("\"type\":\"mp_product_upsell_app\"")
            )
        }

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Добавляем в корзину".ybm_run { _ in
            sku.addToCartButton.element.tap()
        }

        "Проверяем, что попап не открылся".ybm_run { _ in
            let upsellPage = SKUUpsellPage.current
            XCTAssertFalse(upsellPage.element.waitForExistence(timeout: XCTestCase.defaultTimeOut))
        }
    }

    func testOnlyProductInCartWidgetIsVisibleIfProductsWidgetsAreEmpty() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5894")
        Allure.addEpic("КМ")
        Allure.addFeature("Попап рекомендаций Upsell")
        Allure.addTitle("Ошибка при получении данных о товарах")

        "Настраиваем тоглы".ybm_run { _ in
            enable(toggles: FeatureNames.upsell)
        }

        "Настраиваем стейт".ybm_run { _ in
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

            var djState = DJState()
            djState.setDJProductsCollections(mapper: .init(
                title: "Не забудьте купить",
                djMetaPlace: "repeat_purchases_multipopup_block",
                offers: []
            ))
            stateManager?.setState(newState: djState)
        }

        "Открываем SKU".ybm_run { _ in
            goToDefaultSKUPage()
        }

        "Мокаем корзину".ybm_run { _ in
            var cartState = CartState()
            cartState.setCartStrategy(with: [.iphone])
            cartState.addItemsToCartState(with: .iphone)
            stateManager?.setState(newState: cartState)
        }

        "Вызываем попап Upsell у товара".ybm_run { _ in
            SKUPage.current.addToCartButton.element.tap()
        }

        "Получаем ошибку/пустой ответ по товарным виджетам: отображается только виджет Товар в корзине".ybm_run { _ in
            let upsellPage = SKUUpsellPage.current

            XCTAssertTrue(
                upsellPage.element.waitForExistence(timeout: XCTestCase.defaultTimeOut)
            )
            XCTAssertTrue(
                upsellPage
                    .productInCartWidget
                    .element
                    .waitForExistence(timeout: XCTestCase.defaultTimeOut)
            )
            XCTAssertFalse(
                upsellPage
                    .repeatPurchasesSnippetAt(index: 1)
                    .element
                    .waitForExistence(timeout: XCTestCase.defaultTimeOut)
            )
        }
    }
}
