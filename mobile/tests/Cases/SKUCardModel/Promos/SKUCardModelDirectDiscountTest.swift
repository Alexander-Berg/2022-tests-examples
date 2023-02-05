import XCTest

class SKUCardModelDirectDiscountTest: LocalMockTestCase {
    func testDirectDiscountInDefaultOffer() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4708")
        Allure.addEpic("КМ")
        Allure.addFeature("Прямая скидка")
        Allure.addTitle("ДО с прямой скидкой отображается корректно")

        var sku: SKUPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardModel_DirectDiscount")
        }

        "Открываем КМ".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Проверяем отображение прямой скидки".ybm_run { _ in
            let price = sku.price
            sku.collectionView.ybm_swipeCollectionView(toFullyReveal: price.price)
            XCTAssertTrue(price.price.isVisible)
            XCTAssertTrue(price.oldPrice.isVisible)
            XCTAssertTrue(price.discountBadge.additionalSaleBadge.isVisible)
        }

        let addToCartButton = sku.addToCartButton

        "Проверяем отображение кнопки \"Добавить в корзину\"".ybm_run { _ in
            sku.collectionView.ybm_swipeCollectionView(toFullyReveal: addToCartButton.element)
            XCTAssertTrue(addToCartButton.element.isVisible)
        }

        "Мокаем состояние добавления в корзину".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardModel_DirectDiscount_AddToCart")
        }

        "Добавляем в корзину".ybm_run { _ in
            addToCartButton.element.tap()
            XCTAssertEqual(addToCartButton.element.label, "1 товар в корзине")
            wait(forVisibilityOf: addToCartButton.plusButton)
            XCTAssertTrue(addToCartButton.plusButton.isHittable)
            XCTAssertTrue(addToCartButton.minusButton.isHittable)
        }
    }
}
