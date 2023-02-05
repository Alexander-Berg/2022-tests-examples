import MarketUITestMocks
import XCTest

final class SKUCardModelSetTests: LocalMockTestCase {
    func testCardModelWithSetPromo() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4268")
        Allure.addEpic("КМ")
        Allure.addFeature("Комплекты")

        var sku: SKUPage!

        disable(toggles: FeatureNames.skuSetRedesignedFeature)

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardModel_Set")
        }

        "Открываем КМ".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Проверяем отображение ДО".ybm_run { _ in
            let addToCartButton = sku.addToCartButton.element
            sku.collectionView.ybm_swipeCollectionView(toFullyReveal: addToCartButton)
            XCTAssertTrue(sku.price.price.isVisible)
            XCTAssertTrue(addToCartButton.isVisible)
        }

        "Настраиваем стейт для добавления в корзину товара с ДО".ybm_run { _ in
            stateManager?.setState(
                newState: CartState.makeStateWithSingleItemInCart(
                    sku: "1835268768",
                    wareMd5: "JLY-bRrujzEFwmMUTwJNlg"
                )
            )
        }

        "Нажимаем на кнопку \"Добавить в корзину\"".ybm_run { _ in
            sku.addToCartButton.element.tap()
        }

        "Проверяем состояние кнопки \"Добавить в корзину\"".ybm_run { _ in
            let addToCartButton = sku.addToCartButton
            ybm_wait(forFulfillmentOf: { addToCartButton.element.label == "1 товар в корзине" })
            wait(forVisibilityOf: addToCartButton.plusButton)
            XCTAssertTrue(addToCartButton.plusButton.isVisible)
        }

        "Проверяем отображение заголовка блока комплекта".ybm_run { _ in
            sku.collectionView.ybm_swipeCollectionView(toFullyReveal: sku.setItemView.element)
            XCTAssertTrue(sku.setSectionHeader.isVisible)
            XCTAssertEqual(sku.setSectionHeader.staticTexts.firstMatch.label, "Вместе дешевле")
        }

        "Открываем условия акции".ybm_run { _ in
            sku.setSectionHeader.tap()
            let webViewHeader = app.navigationBars["Яндекс.Маркет - покупки с быстрой доставкой"]
            wait(forVisibilityOf: webViewHeader)
        }

        "Закрываем условия акции".ybm_run { _ in
            WebViewPage.current.navigationBar.backButton.tap()
            wait(forVisibilityOf: sku.element)
        }

        "Проверяем кнопку \"Комплектом в корзину\" и блок с товарами".ybm_run { _ in
            sku.collectionView.ybm_swipeCollectionView(toFullyReveal: sku.setCartButton.element)
            XCTAssertTrue(sku.setCartButton.element.isVisible)
            XCTAssertTrue(sku.setItemView.element.isVisible)
            XCTAssertEqual(sku.setItemView.title.label, "Гигиенический душ встраиваемый NOBILI AV00600CR")
            XCTAssertEqual(sku.setItemView.plus.label, "+")
        }

        "Открываем все комплекты по акции".ybm_run { _ in
            sku.collectionView.ybm_swipeCollectionView(toFullyReveal: sku.setShowAllProductsButton)
            sku.setShowAllProductsButton.tap()
            let webViewHeader = app.navigationBars["Вместе дешевле"]
            wait(forVisibilityOf: webViewHeader)
        }

        "Закрываем все комплекты по акции".ybm_run { _ in
            WebViewPage.current.navigationBar.backButton.tap()
            wait(forVisibilityOf: sku.element)
        }
    }

    func testDisplayingSetWidgetWhenSwitchingColors() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3372")
        Allure.addEpic("КМ")
        Allure.addFeature("Комплекты")
        Allure.addTitle("Переключение свойств 2 рода")

        var sku: SKUPage!

        disable(toggles: FeatureNames.skuSetRedesignedFeature)

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardModel_Set_Color1")
        }

        "Открываем КМ".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Проверяем отображение заголовка блока комплекта".ybm_run { _ in
            sku.collectionView.ybm_swipeCollectionView(toFullyReveal: sku.setSectionHeader)
            XCTAssertTrue(sku.setSectionHeader.isVisible)
            XCTAssertEqual(sku.setSectionHeader.staticTexts.firstMatch.label, "Вместе дешевле")
        }

        "Мокаем состояние для альтернативного цвета товара".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardModel_Set_Color2")
        }

        "Выбираем второй вариант цвета".ybm_run { _ in
            sku.collectionView.ybm_swipeCollectionView(to: .up, toFullyReveal: sku.filter.element)
            let secondColorCell = sku.filter.collectionView.cellElement(at: .init(row: 1, section: 0))
            secondColorCell.tap()
            wait(forVisibilityOf: sku.element)
            sku.collectionView.ybm_swipeCollectionView(to: .up, toFullyReveal: sku.title)
            ybm_wait { sku.title.label == "Компьютерный стол TetChair WD-01, ШхГ: 80х48 см, цвет: light" }
        }

        "Проверяем отсутствие блока комплекта".ybm_run { _ in
            sku.collectionView.swipeUp()
            XCTAssertFalse(sku.setSectionHeader.exists)
        }
    }

    func testOpenLinkedSKUFromSetWidget() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3369")
        Allure.addEpic("КМ")
        Allure.addFeature("Комплекты")
        Allure.addTitle("Переход на КМ товара из связанного комплекта")

        performOpenSKUTest(linked: true)
    }

    func testOpenUnlinkedSKUFromSetWidget() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3423")
        Allure.addEpic("КМ")
        Allure.addFeature("Комплекты")
        Allure.addTitle("Переход на КМ товара из несвязанного комплекта")

        performOpenSKUTest(linked: false)
    }

    private func performOpenSKUTest(linked: Bool) {
        var sku: SKUPage!
        var newSku: SKUPage!

        disable(toggles: FeatureNames.skuSetRedesignedFeature)

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardModel_Set")
        }

        "Открываем КМ".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Мокаем состояние для перехода к другому товару".ybm_run { _ in
            mockStateManager?.pushState(
                bundleName: linked ? "SKUCardModel_Set_LinkedSKU" : "SKUCardModel_Set_UnlinkedSKU"
            )
        }

        "Переходим на КМ товара из подборки".ybm_run { _ in
            sku.collectionView.ybm_swipeCollectionView(toFullyReveal: sku.setItemView.element)
            newSku = sku.setItemView.tap()
            wait(forVisibilityOf: newSku.element)
        }

        "Проверяем кнопку \"Комплектом в корзину\" и блок с товарами".ybm_run { _ in
            if linked {
                newSku.collectionView.ybm_swipeCollectionView(toFullyReveal: newSku.setCartButton.element)
                XCTAssertTrue(newSku.setCartButton.element.isVisible)
                XCTAssertTrue(newSku.setItemView.element.isVisible)
            } else {
                XCTAssertFalse(newSku.setCartButton.element.exists)
                XCTAssertFalse(newSku.setItemView.element.exists)
            }
        }
    }

}
