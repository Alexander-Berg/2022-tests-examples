import MarketUITestMocks
import XCTest

final class MultilandingTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    override func setUp() {
        super.setUp()
        enable(toggles: FeatureNames.multilanding)
        enable(toggles: FeatureNames.multiDimensionOnSku)
        enable(toggles: FeatureNames.productCardVideo)
    }

    // swiftlint:disable:next function_body_length
    func testWithoutVideoWithout3DWithAlsoViewed() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5668")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5671")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6043")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6046")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6061")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6044")
        Allure.addEpic("Мультилендинг")
        Allure.addTitle(
            "Кнопка Похожие, открытие мультилендинга, кнопки в шапке, переход на КМ, добавление в корзину, пагинация"
        )

        var sku: SKUPage!
        var multilanding: MultilandingPage!

        var skuState = SKUInfoState()
        var wishlistState = WishlistState()
        var comparisonState = ComparisonState()
        var cartState = CartState()

        "Мокаем состояние SKU".ybm_run { _ in
            skuState.setSkuInfoState(with: .multilanding)
            skuState.setAlsoViewed(fapiOffers: [.default, .protein])
            skuState.setThreeDimensionalModel(model: .emptyModel)
            skuState.setVideoStream(videoStream: .emptyVideoStream)
            stateManager?.setState(newState: skuState)
        }

        "Мокаем состояние избранного".ybm_run { _ in
            wishlistState.setAddWishlistItem(with: .default)
            wishlistState.setWishlistItems(items: [.default])
            wishlistState.setDeleteWishlistItem(.default, with: 0)
            stateManager?.setState(newState: wishlistState)
        }

        "Мокаем состояние сравнения".ybm_run { _ in
            comparisonState.addItemToComparison()
            comparisonState.removeItemFromComparison()
            comparisonState.setComparisonItems(items: [.init(items: [.default], category: .protein)])
            stateManager?.setState(newState: comparisonState)
        }

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "На КМ отображается галерея товара".ybm_run { _ in
            wait(forVisibilityOf: sku.gallery.element)
        }

        let similarGoodsButton = sku.gallery.similarGoodsButton

        "В галерее товара отображается кнопка Похожие".ybm_run { _ in
            XCTAssertTrue(similarGoodsButton.isVisible)
        }

        "Тапаем на Сравнение".ybm_run { _ in
            wait(forVisibilityOf: sku.navigationBar.comparisonButton)
            sku.navigationBar.comparisonButton.tap()
        }

        "Товар добавлен в сравнение".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { () -> Bool in
                sku.navigationBar.comparisonButton.isSelected
            })
        }

        "Тапаем на Избранное".ybm_run { _ in
            wait(forVisibilityOf: sku.navigationBar.wishlistButton)
            sku.navigationBar.wishlistButton.tap()
        }

        "Товар добавлен в избранное".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { () -> Bool in
                sku.navigationBar.wishlistButton.isSelected
            })
        }

        "Тапаем на Похожие".ybm_run { _ in
            similarGoodsButton.tap()
        }

        "Открылся экран мультилендинга".ybm_run { _ in
            multilanding = MultilandingPage.current
            wait(forVisibilityOf: multilanding.element)
        }

        "Отображается кнопка Назад".ybm_run { _ in
            wait(forVisibilityOf: multilanding.navigationBar.backButton)
        }

        "Отображается заголовок".ybm_run { _ in
            wait(forVisibilityOf: multilanding.navigationBar.title)
        }

        "Отображается кнопка Поделиться".ybm_run { _ in
            wait(forVisibilityOf: multilanding.navigationBar.shareButton)
        }

        "Отображается закрашенная кнопка Сравнение".ybm_run { _ in
            wait(forVisibilityOf: multilanding.navigationBar.comparisonButton)
            ybm_wait(forFulfillmentOf: { () -> Bool in
                multilanding.navigationBar.comparisonButton.isSelected
            })
        }

        "Отображается закрашенная кнопка Избранное".ybm_run { _ in
            wait(forVisibilityOf: multilanding.navigationBar.wishlistButton)
            ybm_wait(forFulfillmentOf: { () -> Bool in
                multilanding.navigationBar.wishlistButton.isSelected
            })
        }

        "Тапаем на Сравнение".ybm_run { _ in
            multilanding.navigationBar.comparisonButton.tap()
        }

        "Товар удалён из сравнения".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { () -> Bool in
                !multilanding.navigationBar.comparisonButton.isSelected
            })
        }

        "Тапаем на Избранное".ybm_run { _ in
            multilanding.navigationBar.wishlistButton.tap()
        }

        "Товар удалён из избранного".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { () -> Bool in
                !multilanding.navigationBar.wishlistButton.isSelected
            })
        }

        "Мокаем состояние корзины".ybm_run { _ in
            cartState.addItemsToCartState(with: .protein)
            cartState.setCartStrategy(with: [.proteinWithCartId])
            stateManager?.setState(newState: cartState)
        }
        let snippet1 = multilanding.snippetAt(item: 1, section: 0)
        let snippet2 = multilanding.snippetAt(item: 2, section: 0)
        let snippet3 = multilanding.snippetAt(item: 0, section: 8)

        "Тапаем на кнопку В корзину".ybm_run { _ in
            wait(forVisibilityOf: snippet2.element)
            snippet2.addToCartButton.element.tap()
        }

        "Товар добавлен в корзину".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { () -> Bool in
                snippet2.addToCartButton.element.label == "1"
            })
            ybm_wait(forFulfillmentOf: { () -> Bool in
                TabBarPage.current.cartTabItem.element.label == "Корзина1"
            })
        }

        "Тапаем на снипет".ybm_run { _ in
            wait(forVisibilityOf: snippet1.element)
            snippet1.element.tap()
        }

        "Открывается КМ".ybm_run { _ in
            wait(forVisibilityOf: sku.element)
        }

        "Возвращаемся назад на мультилендинг".ybm_run { _ in
            wait(forVisibilityOf: sku.navigationBar.backButton)
            sku.navigationBar.backButton.tap()
        }

        "Очищаем список запросов".ybm_run { _ in
            mockServer?.handledRequests.removeAll()
        }

        "Скролим вниз".ybm_run { _ in
            multilanding.collectionView.swipe(to: .down, untilVisible: snippet3.element)
            multilanding.collectionView.swipe(to: .down, times: 2, until: false)
        }

        "Проверяем, что запрос отправлен".ybm_run { _ in
            XCTAssertTrue(
                mockServer?.handledRequests.contains { $0.contains("resolveAlsoViewed") } ?? false
            )
        }

        "Тапаем на Поделиться, выбираем копирование".ybm_run { _ in
            multilanding.navigationBar.shareButton.tap()
            let activityListView = ActivityListViewPage.current
            let copyButton = activityListView.copyButton
            wait(forExistanceOf: copyButton)
        }
    }

    func testWithoutVideoWith3DWithAlsoViewed() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5669")
        Allure.addEpic("Мультилендинг")
        Allure.addTitle("Кнопка Похожие на изображении отсутствует, если есть 3д")

        var sku: SKUPage!
        var skuState = SKUInfoState()

        "Мокаем состояние".ybm_run { _ in
            skuState.setSkuInfoState(with: .multilanding)
            skuState.setAlsoViewed(capiOffers: [.protein, .protein1])
            skuState.setThreeDimensionalModel(model: .ps5Model)
            skuState.setVideoStream(videoStream: .emptyVideoStream)
            stateManager?.setState(newState: skuState)
        }

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "На КМ отображается галерея товара".ybm_run { _ in
            wait(forVisibilityOf: sku.gallery.element)
        }

        "В галерее товара отсутствует кнопка Похожие".ybm_run { _ in
            XCTAssertFalse(sku.gallery.similarGoodsButton.isVisible)
        }
    }

    func testWithVideoWithout3DWithAlsoViewed() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5687")
        Allure.addEpic("Мультилендинг")
        Allure.addTitle("Кнопка Похожие на видео товара присутствует")

        var sku: SKUPage!
        var skuState = SKUInfoState()

        "Мокаем состояние".ybm_run { _ in
            skuState.setSkuInfoState(with: .multilandingWithVideo)
            skuState.setAlsoViewed(capiOffers: [.protein, .protein1])
            skuState.setThreeDimensionalModel(model: .emptyModel)
            skuState.setVideoStream(videoStream: .ps5VideoStream)
            stateManager?.setState(newState: skuState)
        }

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "На КМ отображается галерея товара".ybm_run { _ in
            wait(forVisibilityOf: sku.gallery.element)
        }

        "В галерее товара присутствует кнопка Похожие".ybm_run { _ in
            XCTAssertTrue(sku.gallery.similarGoodsButton.isVisible)
        }
    }

    func testWithoutVideoWithout3DWithoutAlsoViewed() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5670")
        Allure.addEpic("Мультилендинг")
        Allure.addTitle("Кнопка Похожие на изображении отсутствует, если нет карусели")

        var sku: SKUPage!
        var skuState = SKUInfoState()

        "Мокаем состояние".ybm_run { _ in
            skuState.setSkuInfoState(with: .multilanding)
            skuState.setAlsoViewed(capiOffers: [])
            skuState.setThreeDimensionalModel(model: .emptyModel)
            skuState.setVideoStream(videoStream: .emptyVideoStream)
            stateManager?.setState(newState: skuState)
        }

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "На КМ отображается галерея товара".ybm_run { _ in
            wait(forVisibilityOf: sku.gallery.element)
        }

        "В галерее товара отсутствует кнопка Похожие".ybm_run { _ in
            XCTAssertFalse(sku.gallery.similarGoodsButton.isVisible)
        }
    }

}
