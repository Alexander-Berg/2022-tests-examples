import XCTest

final class SKUCardModelQuantityDiscountTests: SKUCardModelBaseTestCase {

    func testThatQuantityDiscountIsDisplayedCorrectly() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4606")
        Allure.addEpic("КМ")
        Allure.addFeature("Скидка за кол-во в карточке товара")
        Allure.addTitle("Проверяем, что скидка за кол-во отображается корректно в карточке товара")

        var sku: SKUPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardModelQuantityDiscount")
        }

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Ждем завершения загрузки информации".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { sku.collectionView.isVisible })
        }

        "Проверяем отображении скидки за кол-во".ybm_run { _ in
            sku.collectionView.ybm_swipeCollectionView(toFullyReveal: sku.addToCartButton.element)

            ybm_wait(forVisibilityOf: [sku.addToCartButton.element])
            XCTAssertEqual(sku.quantityDiscount.label, "от 2 шт. - по 90 ₽/шт.")
        }
    }

    func testThatQuantityDiscountIsApplied() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4606")
        Allure.addEpic("КМ")
        Allure.addFeature("Скидка за кол-во в карточке товара")
        Allure
            .addTitle(
                "Проверяем, что при добавлении товара в корзину до первого порога, порог срабатывает и скидка применяется"
            )

        var sku: SKUPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardModelQuantityDiscount")
        }

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Ждем завершения загрузки информации".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { sku.collectionView.isVisible })
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardModelQuantityDiscount_AddCart")
        }

        "Увеличиваем кол-во товара до 2".ybm_run { _ in
            checkAddToCartButton(
                with: "Добавить в корзину",
                count: 2,
                on: sku,
                titleAfterTap: "2 товара в корзине"
            )

            ybm_wait(forVisibilityOf: [sku.addToCartButton.element])
            XCTAssertEqual(sku.quantityDiscount.label, "от 2 шт. - по 90 ₽/шт.")
        }

        "Проверяем применилась ли скидка".ybm_run { _ in
            ybm_wait { sku.price.price.isVisible && sku.price.oldPrice.isVisible }
            XCTAssertEqual(sku.price.price.label, "90 ₽")
            XCTAssertEqual(sku.price.oldPrice.label, "100 ₽")
            XCTAssertEqual(sku.price.discountBadge.discount.label, "- 10%")
        }
    }

    /// Проверка кнопки "Добавить в корзину"
    private func checkAddToCartButton(with title: String, count: Int, on sku: SKUPage, titleAfterTap: String) {
        sku.collectionView.ybm_swipeCollectionView(toFullyReveal: sku.addToCartButton.element)

        ybm_wait(forFulfillmentOf: { sku.addToCartButton.element.isVisible })
        ybm_wait(forFulfillmentOf: { sku.addToCartButton.element.label == title })

        // swiftlint:disable:next empty_count
        if count > 0 {
            sku.addToCartButton.element.tap()
        }
        for _ in 1 ..< count {
            sku.addToCartButton.plusButton.tap()
        }

        ybm_wait(forFulfillmentOf: { sku.addToCartButton.element.label == titleAfterTap })
    }
}
