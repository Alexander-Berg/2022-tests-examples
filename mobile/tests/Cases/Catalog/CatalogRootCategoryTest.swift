import MarketUITestMocks
import XCTest

final class CatalogRootCategoryTest: LocalMockTestCase {

    func testRootCategory() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-965")
        Allure.addEpic("Каталог")
        Allure.addFeature("Рутовая категория")
        Allure.addTitle("Рутовая категория")

        var catalog: CatalogPage!
        var recommendationCell: CatalogPage.RecommendationCell!

        "Мокаем департаменты".ybm_run { _ in
            setupCatalogCMSState()
            mockStateManager?.pushState(bundleName: "CatalogSet_RootWidgetRecommendation")
        }

        "Открываем каталог".ybm_run { _ in
            catalog = goToCatalog()
        }

        "Проверяем отображение заголовка виджета рекомендаций".ybm_run { _ in
            let title = catalog.recommendationsTitle
            catalog.element.ybm_swipeCollectionView(toFullyReveal: title)
            XCTAssertTrue(title.isVisible)
            XCTAssertEqual(title.label, "Популярные товары")
        }

        "Проверяем отображение сниппетов в виджете рекомендаций с картинкой, именем, ценой"
            .ybm_run { _ in
                recommendationCell = catalog.recommendationCell(at: IndexPath(item: 2, section: 1))
                ybm_wait(forFulfillmentOf: { recommendationCell.element.isVisible })

                let image = recommendationCell.image
                XCTAssertTrue(image.exists)

                let name = recommendationCell.name
                XCTAssertTrue(name.isVisible)
                XCTAssertEqual(name.label, "Смартфон Apple iPhone 12 128GB Red")

                let price = recommendationCell.price
                XCTAssertTrue(price.isVisible)
                XCTAssertEqual(price.label, "64 970 ₽")

                let cartButton = recommendationCell.cartButton
                XCTAssertTrue(cartButton.element.isVisible)
            }
    }

    func testRecommendationWidget() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3262")
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3852")
        Allure.addEpic("Каталог")
        Allure.addFeature("Рутовая категория")
        Allure.addTitle("Виджет рекомендаций в рутовой категории")

        var catalog: CatalogPage!
        var recommendationCell: CatalogPage.RecommendationCell!
        var sku: SKUPage!

        "Мокаем состояние".ybm_run { _ in
            setupCatalogCMSState()
            setupCartState()
            mockStateManager?.pushState(bundleName: "CatalogSet_RootWidgetRecommendation")
            mockStateManager?.pushState(bundleName: "CatalogSet_ItemInCart")
        }

        "Открываем каталог".ybm_run { _ in
            catalog = goToCatalog()
        }

        "Скролим до виджета рекомендаций".ybm_run { _ in
            recommendationCell = catalog.recommendationCell(at: IndexPath(item: 2, section: 1))
            ybm_wait(forFulfillmentOf: { recommendationCell.element.isVisible })
        }

        "Проверяем добавление товара в корзину и что кнопка изменила свое название".ybm_run { _ in
            setupCartState(offer: .protein)

            let cartButton = recommendationCell.cartButton
            cartButton.element.tap()

            ybm_wait(forFulfillmentOf: { cartButton.element.label == "1" })
            XCTAssertTrue(cartButton.plusButton.isVisible)
            XCTAssertTrue(cartButton.minusButton.isVisible)
            ybm_wait(forFulfillmentOf: { TabBarPage.current.cartTabItem.element.label == "Корзина1" })
        }

        "Переходим на карточку товара".ybm_run { _ in
            sku = recommendationCell.tap()
        }

        "Ждем завершения загрузки карточки товара".ybm_run { _ in
            wait(forExistanceOf: sku.element)
        }

        "Возвращаемся обратно в каталог, проверяем что вернулись в тоже место".ybm_run { _ in
            NavigationBarPage.current.backButton.tap()
            ybm_wait(forFulfillmentOf: { recommendationCell.element.isVisible })
            XCTAssertEqual(recommendationCell.cartButton.element.label, "1")
        }
    }
}

// MARK: - Private

private extension CatalogRootCategoryTest {

    func setupCatalogCMSState() {
        stateManager?.mockingStrategy = .dtoMock

        var cmsState = CMSState()
        cmsState.setCMSState(with: CMSState.CMSCollections.rootCatalogCollections)
        stateManager?.setState(newState: cmsState)
    }

    func setupCartState(offer: FAPIOffer? = nil) {
        var cartState = CartState()
        cartState.setCartStrategy(with: [offer].compactMap { $0 })
        stateManager?.setState(newState: cartState)
    }
}
