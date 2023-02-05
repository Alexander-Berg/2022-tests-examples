import XCTest

class SKUCardModelSizeTableTest: LocalMockTestCase {

    func testSKUWithoutSizeTable() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3079")
        Allure.addEpic("КМ")
        Allure.addFeature("Размерная сетка")
        Allure.addTitle("Товар без фильтра Размер")

        var sku: SKUPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_FiltersBasic")
        }

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Проверяем отсутствие кнопки \"Таблица размеров\"".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(toFullyReveal: sku.filter.element)
            XCTAssertFalse(sku.filterSizeTableButton.isVisible)
        }
    }

    func testSKUWithSizeTable() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3078")
        Allure.addEpic("КМ")
        Allure.addFeature("Размерная сетка")
        Allure.addTitle("Выбор размера")

        var sku: SKUPage!
        var popup: SizeTablePopupPage!
        var popupDoneButton: XCUIElement!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardModelSet_SizeTable_29")
        }

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Открываем таблицу размеров".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(toFullyReveal: sku.filterSizeTableButton)
            sku.filterSizeTableButton.tap()

            popup = SizeTablePopupPage.currentPopup
            popupDoneButton = popup.doneButton
            wait(forVisibilityOf: popupDoneButton)
        }

        "Выбираем размер отличный от того, который был на КМ".ybm_run { _ in
            let sizeCell = popup.sizeTableCollection.cellElement(at: IndexPath(item: 1, section: 6))
            sizeCell.tap()
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardModelSet_SizeTable_31")
        }

        "Закрываем попап и проверяем, что загрузилась выбранная КМ".ybm_run { _ in
            popupDoneButton.tap()

            ybm_wait(forFulfillmentOf: { () -> Bool in
                sku.title.isVisible
                    && sku.title.text == "Тапочки T.Taccardi размер 31, темно-синий"
            })
        }

    }

}
