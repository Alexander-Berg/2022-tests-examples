import XCTest

final class SKUCardModelCheapestAsGiftTest: LocalMockTestCase {
    func testCheapestAsGiftFlow() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4183")
        Allure.addEpic("КМ")
        Allure.addFeature("Cheapest as gift")

        var sku: SKUPage!
        var newSku: SKUPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardModel_CheapestAsGift")
        }

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Проверяем бейдж 3=2 в ДО".ybm_run { _ in
            let cheapestAsGiftBadge = sku.cheapestAsGiftView.imageView
            sku.collectionView.ybm_swipeCollectionView(toFullyReveal: cheapestAsGiftBadge)
        }

        "Проверяем кнопку \"Добавить в корзину\" в ДО".ybm_run { _ in
            let addToCartButton = sku.addToCartButton.element
            sku.collectionView.ybm_swipeCollectionView(toFullyReveal: addToCartButton)
            XCTAssertTrue(addToCartButton.isHittable)
        }

        "Проверяем блок товаров 3=2".ybm_run { _ in
            sku.collectionView.ybm_swipeCollectionView(toFullyReveal: sku.cheapestAsGiftHeader)
        }

        "Открываем условия акции".ybm_run { _ in
            sku.cheapestAsGiftHeader.tap()
            let webViewHeader = app.navigationBars["Яндекс.Маркет - покупки с быстрой доставкой"]
            wait(forVisibilityOf: webViewHeader)
        }

        "Закрываем условия акции".ybm_run { _ in
            WebViewPage.current.navigationBar.backButton.tap()
            wait(forVisibilityOf: sku.element)
        }

        "Открываем все товары по акции".ybm_run { _ in
            sku.collectionView.ybm_swipeCollectionView(toFullyReveal: sku.cheapestAsGiftShowAllProductsButton)
            sku.cheapestAsGiftShowAllProductsButton.tap()
            let webViewHeader = app.navigationBars["Три товара по цене двух"]
            wait(forVisibilityOf: webViewHeader)
        }

        "Закрываем все товары по акции".ybm_run { _ in
            WebViewPage.current.navigationBar.backButton.tap()
            wait(forVisibilityOf: sku.element)
        }

        "Мокаем состояние для перехода к другому товару".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardModel_CheapestAsGift_AnotherProduct")
        }

        "Открываем товар из блока 3=2".ybm_run { _ in
            sku.collectionView.ybm_swipeCollectionView(toFullyReveal: sku.cheapestAsGiftProduct.element)
            newSku = sku.cheapestAsGiftProduct.tap()
            wait(forVisibilityOf: newSku.element)
        }

        "Мокаем состояние добавления в корзину".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardModel_CheapestAsGift_AnotherProduct_AddToCart")
        }

        "Добавляем товар в корзину".ybm_run { _ in
            newSku.collectionView.ybm_swipeCollectionView(toFullyReveal: newSku.addToCartButton.element)
            newSku.addToCartButton.element.tap()
        }

        "Проверяем состояние кнопки \"Добавить в корзину\"".ybm_run { _ in
            let addToCartButton = newSku.addToCartButton
            ybm_wait(forFulfillmentOf: { addToCartButton.element.label == "1 товар в корзине" })
            wait(forVisibilityOf: addToCartButton.plusButton)
            XCTAssertTrue(addToCartButton.plusButton.isHittable)
            XCTAssertTrue(addToCartButton.minusButton.isHittable)
        }
    }
}
