import MarketUITestMocks
import XCTest

final class SKUCardModelAddToCart: SKUCardModelBaseTestCase {

    struct RecomendationCell {
        var pair: (left: SKU, right: SKU)

        let addToCart = "ДОБАВИТЬ"
    }

    func testGuestLessThan2499() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-1038")
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3818")
        Allure.addEpic("КМ")
        Allure.addFeature("Добавление в корзину")
        Allure.addTitle("Незалогин, товар дешевле 2499")

        var sku: SKUPage!

        let skuData = SKU(
            title: "Смесь Nutrilon (Nutricia) 1 Premium (c рождения) 800 г",
            price: "762 ₽",
            oldPrice: nil
        )

        disable(toggles: FeatureNames.cartRedesign)

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_MoscowYPlusDelivery")
        }

        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            let config = CustomSKUConfig(
                productId: 13_621_355,
                offerId: "J6zCIjgXkqgppGtMDBpsrQ"
            )
            skuState.setSkuInfoState(with: .custom(config))
            stateManager?.setState(newState: skuState)
        }

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Ждем завершения загрузки информации".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                sku.didFinishLoadingInfo
            })
        }

        "Мокаем состояние добавления в корзину".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_NutrilonAddedToCart")
        }

        "Проверяем отображение кнопки \"Добавить в корзину\"".ybm_run { _ in
            checkAddToCartButton(with: "Добавить в корзину", on: sku, titleAfterTap: "1 товар в корзине")
            ybm_wait { () -> Bool in
                sku.addToCartButton.plusButton.exists && sku.addToCartButton.minusButton.exists
            }
        }

        "Переходим в корзину и проверяем данные".ybm_run { _ in
            let cart = goToCart()
            checkCart(cart: cart, sku: skuData)
        }

        "Проверяем бейдж корзины в таббаре".ybm_run { _ in
            XCTAssertEqual(TabBarPage.current.cartTabItem.element.label, "Корзина1")
        }
    }

    func testGuestMoreThan2499() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-1039")
        Allure.addEpic("КМ")
        Allure.addFeature("Добавление в корзину")
        Allure.addTitle("Незалогин, товар дороже 2499")

        var sku: SKUPage!

        let skuData = SKU(
            title: "Смартфон Apple iPhone 8 64GB серебристый (MQ6H2RU/A)",
            price: "39 490 ₽",
            oldPrice: "44 990 ₽"
        )

        disable(toggles: FeatureNames.cartRedesign)

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_IPhone")
        }

        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            let config = CustomSKUConfig(
                productId: 177_653_084,
                offerId: "go24rjhk7QUrlqh-5kNAmQ"
            )
            skuState.setSkuInfoState(with: .custom(config))
            stateManager?.setState(newState: skuState)
        }

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Мокаем добавление и переход в корзину".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_IPhoneInCart")
        }

        "Проверяем отображение кнопки \"Добавить в корзину\"".ybm_run { _ in
            checkAddToCartButton(with: "Добавить в корзину", on: sku, titleAfterTap: "1 товар в корзине")
        }

        "Переходим в корзину и проверяем данные".ybm_run { _ in
            let cart = goToCart()
            checkCart(cart: cart, sku: skuData)
        }
    }

    func testAddToCartInStickyCompactOfferView() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3826")
        Allure.addEpic("КМ")
        Allure.addFeature("Добавление в корзину")
        Allure.addTitle("Проверяем добавление в корзину по кнопке в залипающей плашке")

        var sku: SKUPage!
        var compactOfferView: CompactOfferViewPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_MoscowYPlusDelivery")
        }

        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            let config = CustomSKUConfig(
                productId: 13_621_355,
                offerId: "J6zCIjgXkqgppGtMDBpsrQ"
            )
            skuState.setSkuInfoState(with: .custom(config))
            stateManager?.setState(newState: skuState)
        }

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Ждем завершения загрузки информации".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                sku.didFinishLoadingInfo
            })
        }

        "Мокаем состояние добавления в корзину".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_NutrilonAddedToCart")
        }

        "Скроллим вниз и нажимаем на кнопку \"В корзину\" в залипающей плашке".ybm_run { _ in
            compactOfferView = CompactOfferViewPage.current
            sku.collectionView.ybm_swipeCollectionView(toFullyReveal: compactOfferView.cartButton.element)
            XCTAssertEqual(compactOfferView.cartButton.element.label, "В корзину")
            compactOfferView.cartButton.element.tap()
        }

        "Проверяем состояние кнопки и бейджа в таб баре".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { compactOfferView.cartButton.element.label == "1" })
            ybm_wait(forFulfillmentOf: { TabBarPage.current.cartTabItem.element.label == "Корзина1" })
        }

        "Мокаем состояние увеличения количества товара".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_NutrilonIncreaseCount")
        }

        "Нажимаем на '+' в кнопке добавления в корзину и проверяем данные".ybm_run { _ in
            compactOfferView.cartButton.plusButton.tap()

            ybm_wait(forFulfillmentOf: { compactOfferView.cartButton.element.label == "2" })
            ybm_wait(forFulfillmentOf: { TabBarPage.current.cartTabItem.element.label == "Корзина2" })
        }
    }

    func testAddSetToCart() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3848")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3373")
        Allure.addEpic("КМ")
        Allure.addFeature("Добавление в корзину")
        Allure.addTitle("Проверяем добавление комплекта в корзину")

        var sku: SKUPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_SetPromo")
        }

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
            ybm_wait(forFulfillmentOf: { sku.didFinishLoadingInfo })
        }

        "Мокаем состояние добавления в корзине".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_SetPromoAddedToCart")
        }

        "Скроллим вниз и нажимаем на кнопку \"В корзину\" в секции комплекта".ybm_run { _ in
            sku.collectionView.ybm_swipeCollectionView(toFullyReveal: sku.setCartButton.element)
            XCTAssertEqual(sku.setCartButton.element.label, "Комплектом в корзину")
            sku.setCartButton.element.tap()
        }

        "Проверяем состояние кнопки и бейджа в таб баре".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { sku.setCartButton.element.label == "1 комплект в корзине" })
            ybm_wait(forFulfillmentOf: { TabBarPage.current.cartTabItem.element.label == "Корзина3" })
        }

        "Нажимаем на кнопку \"Комплект в корзине\" и проверяем переход в корзину".ybm_run { _ in
            let cart = sku.setCartButton.tap()
            wait(forVisibilityOf: cart.element)
            XCTAssertTrue(cart.collectionView.isVisible)
        }

    }

    func testIncreaseCountInCart() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3819")
        Allure.addEpic("КМ")
        Allure.addFeature("Добавление в корзину")
        Allure.addTitle("Увеличение количества товара в корзине")

        var sku: SKUPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_NutrilonAddedToCart")
        }

        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            let config = CustomSKUConfig(
                productId: 13_621_355,
                offerId: "J6zCIjgXkqgppGtMDBpsrQ"
            )
            skuState.setSkuInfoState(with: .custom(config))
            stateManager?.setState(newState: skuState)
        }

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Ждем завершения загрузки информации".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                sku.didFinishLoadingInfo
            })
        }

        "Мокаем состояние увеличения количества товара".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_NutrilonIncreaseCount")
        }

        "Нажимаем на '+' в кнопке добавления в корзину".ybm_run { _ in
            sku.collectionView.ybm_swipeCollectionView(toFullyReveal: sku.addToCartButton.element)
            sku.addToCartButton.plusButton.tap()
        }

        "Проверяем отображение кнопки добавления в корзину и бейджа в таб баре".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { sku.addToCartButton.element.label == "2 товара в корзине" })
            ybm_wait(forFulfillmentOf: { TabBarPage.current.cartTabItem.element.label == "Корзина2" })
        }
    }

    func testDecreaseCountInCartAndRemove() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3820")
        Allure.addEpic("КМ")
        Allure.addFeature("Добавление в корзину")
        Allure.addTitle("Уменьшение количества товара до 0 и его удаление")

        var sku: SKUPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_NutrilonAddedToCart")
        }

        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            let config = CustomSKUConfig(
                productId: 13_621_355,
                offerId: "J6zCIjgXkqgppGtMDBpsrQ"
            )
            skuState.setSkuInfoState(with: .custom(config))
            stateManager?.setState(newState: skuState)
        }

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Ждем завершения загрузки информации".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                sku.didFinishLoadingInfo
            })
        }

        "Мокаем состояние удаления товара".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_NutrilonDeleteFromCart")
        }

        "Нажимаем на '-' в кнопке добавления в корзину и проверяем удаление товара".ybm_run { _ in
            mockStateManager?.addSuspended(filename: "POST_api_v1_deleteItemsFromCart")

            sku.collectionView.ybm_swipeCollectionView(toFullyReveal: sku.addToCartButton.element)
            sku.addToCartButton.minusButton.tap()
            ybm_wait(forFulfillmentOf: { sku.addToCartButton.element.label == "Товар удалён" })

            mockStateManager?.deleteSuspended(filename: "POST_api_v1_deleteItemsFromCart")

            ybm_wait(forFulfillmentOf: { sku.addToCartButton.element.label == "Добавить в корзину" })

            XCTAssertFalse(sku.addToCartButton.plusButton.isVisible)
            XCTAssertFalse(sku.addToCartButton.minusButton.isVisible)
        }
    }

    /// Проверка кнопки "Добавить в корзину"
    private func checkAddToCartButton(with title: String, on sku: SKUPage, titleAfterTap: String) {
        sku.collectionView.ybm_swipeCollectionView(toFullyReveal: sku.addToCartButton.element)

        ybm_wait(forFulfillmentOf: { sku.addToCartButton.element.isVisible })
        ybm_wait(forFulfillmentOf: { sku.addToCartButton.element.label == title })

        sku.addToCartButton.element.tap()
        ybm_wait(forFulfillmentOf: { sku.addToCartButton.element.label == titleAfterTap })
    }
}
