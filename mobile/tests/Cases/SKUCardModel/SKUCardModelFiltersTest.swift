import MarketUITestMocks
import XCTest

final class SKUCardModelFiltersTest: LocalMockTestCase {

    func testBasicFunctionality() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-989")
        Allure.addEpic("КМ")
        Allure.addFeature("Фильтры")
        Allure.addTitle("Базовая функциональность")

        var sku: SKUPage!
        var filterPopup: FilterPopupPage!

        let skuDescription = "Контактные линзы Acuvue OASYS with Hydraclear Plus (6 линз) R 8,4 D -3,75"
        let skuPrice = "81 990 ₽"

        "Мокаем состояние".run {
            setupSKUInfoState()
        }

        "Открываем КМ".run {
            sku = goToDefaultSKUPage()
            ybm_wait { sku.didFinishLoadingInfo }
        }

        "Тапаем на кнопку \"Еще 9\"".run {
            sku.collectionView.ybm_swipeCollectionView(
                toFullyReveal: sku.filter.element,
                inset: sku.stickyViewInset
            )

            let moreButton = sku.filter.moreButton
            sku.filter.collectionView.element.ybm_swipeCollectionView(
                to: .left,
                toFullyReveal: moreButton
            )

            moreButton.tap()

            filterPopup = FilterPopupPage.currentPopup
            wait(forVisibilityOf: filterPopup.element)
        }

        "Проверить, что отображается кнопка \"Готово\"".run {
            wait(forVisibilityOf: filterPopup.doneButton)
        }

        "Проверить, что у текущего фильтра отображается цена и описание".run {
            XCTAssertEqual(filterPopup.titleLabel.label, skuDescription)
            XCTAssertEqual(filterPopup.priceLabel.label, skuPrice)
        }

        "Проверить, что отображаются значения фильтра".run {
            XCTAssertTrue(filterPopup.filterCollection.element.isVisible, "Значений фильтра не видно")
        }
    }
}

// MARK: - Helper Methods

private extension SKUCardModelFiltersTest {

    func setupSKUInfoState() {
        var skuState = SKUInfoState()
        skuState.setSkuInfoState(with: .lenses)
        stateManager?.setState(newState: skuState)
    }
}
