import MarketUITestMocks
import XCTest

final class SKUCardModelAddToCartAuth: SKUCardModelBaseTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    struct RecomendationCell {
        var pair: (left: SKU, right: SKU)

        let addToCart = "ДОБАВИТЬ"
    }

    func testAuthorizedLessThan2499() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-1040")
        Allure.addEpic("КМ")
        Allure.addFeature("Добавление в корзину")
        Allure.addTitle("Залогин, товар дешевле 2499")

        var root: RootPage!
        var sku: SKUPage!

        disable(toggles: FeatureNames.cartRedesign)

        // данные для теста
        let skuData = SKU(
            title: "Смесь Nutrilon (Nutricia) 1 Premium (c рождения) 800 г",
            price: "762 ₽",
            oldPrice: nil
        )

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
            root = appAfterOnboardingAndPopups()
            sku = goToDefaultSKUPage(root: root)
        }

        "Ждем завершения загрузки информации".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { sku.collectionView.isVisible })
        }

        "Мокаем состояние добавления в корзину".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_NutrilonAddedToCart")
        }

        "Проверяем отображение кнопки \"Добавить в корзину\"".ybm_run { _ in
            checkAddToCartButton(with: "Добавить в корзину", on: sku, titleAfterTap: "1 товар в корзине")
        }

        "Переходим в корзину и проверяем данные".ybm_run { _ in
            let cart = goToCart(root: root)
            checkCart(cart: cart, sku: skuData)
        }
    }

    func testAuthorizedMoreThan2499() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-1041")
        Allure.addEpic("КМ")
        Allure.addFeature("Добавление в корзину")
        Allure.addTitle("Залогин, товар дороже 2499")

        var root: RootPage!
        var sku: SKUPage!

        disable(toggles: FeatureNames.cartRedesign)

        // данные для теста

        let skuData = SKU(
            title: "Смартфон Apple iPhone 8 64GB серебристый (MQ6H2RU/A)",
            price: "39 490 ₽",
            oldPrice: "44 990 ₽"
        )

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
            root = appAfterOnboardingAndPopups()
            sku = goToDefaultSKUPage(root: root)
        }

        "Мокаем добавление и переход в корзину".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_IPhoneInCart")
        }

        "Проверяем отображение кнопки \"Добавить в корзину\"".ybm_run { _ in
            checkAddToCartButton(with: "Добавить в корзину", on: sku, titleAfterTap: "1 товар в корзине")
        }

        "Переходим в корзину и проверяем данные".ybm_run { _ in
            let cart = goToCart(root: root)
            checkCart(cart: cart, sku: skuData)
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
