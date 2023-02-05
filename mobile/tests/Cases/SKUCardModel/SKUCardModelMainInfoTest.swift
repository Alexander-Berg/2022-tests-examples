import MarketUITestMocks
import XCTest

final class SKUCardModelMainInfoTest: LocalMockTestCase {

    func testShouldShowMainSKUInfo() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-923")
        Allure.addEpic("КМ")
        Allure.addFeature("Сниппет товара")
        Allure.addTitle("Проверяем, что экран карточки товара отображает всю основную информацию")

        var skuState = SKUInfoState()

        "Настраиваем стейт".ybm_run { _ in
            skuState.setSkuInfoState(with: .default)
            stateManager?.setState(newState: skuState)
        }

        var sku: SKUPage!

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Проверяем отображение фото".ybm_run { _ in
            wait(forVisibilityOf: sku.gallery.element)
        }

        "Проверяем наличие ссылки на вендора".ybm_run { _ in
            wait(forVisibilityOf: sku.vendorLinkButton.element)
        }

        "Проверяем отображение названия товара".ybm_run { _ in
            let title = sku.title
            wait(forVisibilityOf: title)
            XCTAssertEqual(title.text, "Смартфон Apple iPhone 12 256GB, синий")
        }

        "Проверяем отображение рейтинга и количества отзывов".ybm_run { _ in
            wait(forVisibilityOf: sku.opinionsFastLink.element)

            XCTAssertTrue(sku.opinionsFastLink.rating.isVisible)
            XCTAssertTrue(sku.opinionsFastLink.opinionsCount.isVisible)
            XCTAssertEqual(sku.opinionsFastLink.opinionsCount.label, "89 отзывов")
        }

        "Проверяем, что отображается цена".ybm_run { _ in
            let price = sku.price
            sku.collectionView.ybm_swipeCollectionView(toFullyReveal: price.element)
            XCTAssertTrue(price.price.isVisible)
            XCTAssertEqual(sku.price.price.label, "81 990 ₽")
        }

        "Проверяем, что отображается кнопка добавления в корзину".ybm_run { _ in
            let addToCartButton = sku.addToCartButton.element
            sku.collectionView.ybm_swipeCollectionView(toFullyReveal: addToCartButton)
            XCTAssertTrue(addToCartButton.isVisible)
        }
    }

    func testShouldShowSaleInfo() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-924")
        Allure.addEpic("КМ")
        Allure.addFeature("Сниппет товара")
        Allure.addTitle("Проверяем отображение информации о том, что на товар действует скидка")

        var skuState = SKUInfoState()

        "Настраиваем стейт".ybm_run { _ in
            skuState.setSkuInfoState(with: .default)
            stateManager?.setState(newState: skuState)
        }

        var sku: SKUPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_Sale")
        }

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Проверяем отображение бейджа со скидкой".ybm_run { _ in
            let price = sku.price
            sku.collectionView.ybm_swipeCollectionView(toFullyReveal: price.element)
            wait(forExistanceOf: price.discountBadge.discount)
            XCTAssertEqual(price.discountBadge.discount.label, "–14 %")
        }

        "Проверяем отображение цены".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(toFullyReveal: sku.price.element)
            XCTAssertTrue(sku.price.element.isVisible)
            XCTAssertTrue(sku.price.oldPrice.isVisible)
            XCTAssertEqual(sku.price.oldPrice.label, "94 990 ₽")
        }
    }

    func testShouldShowInCart() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-928")
        Allure.addEpic("КМ")
        Allure.addFeature("Сниппет товара")
        Allure.addTitle("Проверяем отображение кнопки \"1 товар в корзине\", когда SKU лежит в корзине")

        var sku: SKUPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_InCart")
        }

        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            let config = CustomSKUConfig(
                productId: 1_971_204_201,
                skuId: 100_307_940_933,
                offerId: "whMnDMjDMnn8L8vrx20vlw"
            )
            skuState.setSkuInfoState(with: .custom(config))
            stateManager?.setState(newState: skuState)
        }

        "Открываем карточку товара".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Проверяем кнопку перехода в корзину".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(toFullyReveal: sku.addToCartButton.element)
            XCTAssertEqual(sku.addToCartButton.element.label, "1 товар в корзине")

            // Проверка на активность кнопки, с предварительным скроллом если необходимо
            // Если кнопку не видно или она не нажимается - тест упадет
            sku.addToCartButton.element.tap()
        }
    }
}
