import MarketUITestMocks
import XCTest

final class SKUCardModelTransitionToCart: LocalMockTestCase {

    func testUnloginUser() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-1000")
        Allure.addEpic("КМ")
        Allure.addFeature("Переход в корзину")
        Allure.addTitle(
            "Проверяем переход в корзину при добавлении товара у пользователя, незалогиненного в приложении"
        )

        var sku: SKUPage!
        var cart: CartPage!

        let price = "762 ₽"
        let goodTitle = "Смесь Nutrilon (Nutricia) 1 Premium (c рождения) 800 г"

        disable(toggles: FeatureNames.cartRedesign)

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_MoscowYPlusDelivery")
        }
        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            var config = CustomSKUConfig(
                productId: 13_621_355,
                offerId: "J6zCIjgXkqgppGtMDBpsrQ"
            )
            config.title = "Смесь Nutrilon (Nutricia) 1 Premium (c рождения) 800 г"
            config.price = 762
            skuState.setSkuInfoState(with: .custom(config))
            stateManager?.setState(newState: skuState)
        }

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Ждем завершения загрузки информации".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { sku.collectionView.isVisible })
        }

        "Проверяем данные о товаре".ybm_run { _ in
            XCTAssertEqual(sku.price.price.label, price)
            XCTAssertEqual(sku.title.text, goodTitle)
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_NutrilonAddedToCart")
        }

        "Проверяем отображение кнопки \"Добавить в корзину\"".ybm_run { _ in
            sku.collectionView.ybm_swipeCollectionView(toFullyReveal: sku.addToCartButton.element)
            XCTAssertEqual(sku.addToCartButton.element.label, "Добавить в корзину")
            sku.addToCartButton.element.tap()
        }

        "Переходим в корзину с КМ".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                sku.addToCartButton.element.isVisible
                    && sku.addToCartButton.element.label == "1 товар в корзине"
            })
            cart = goToCart()
        }

        "Проверяем равенство цены на SKU и в корзине".ybm_run { _ in
            checkCart(cart: cart, price: price, goodTitle: goodTitle)
        }
    }

    private func checkCart(cart: CartPage, price: String, goodTitle: String) {
        wait(forVisibilityOf: cart.element)

        let addedGood = cart.cartItem(at: 0)
        wait(forVisibilityOf: addedGood.element)

        XCTAssertEqual(addedGood.price.label, price)
        XCTAssertEqual(addedGood.title.label, goodTitle)
    }
}
