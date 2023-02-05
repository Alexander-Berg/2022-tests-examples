import XCTest

class ComparisonTestGoToSKUVC: ComparisonBaseTest {

    func testOpenModel() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3500")
        Allure.addEpic("Сравнение")
        Allure.addFeature("Экран сравнения")
        Allure.addTitle("Один товар в списке")

        var comparison: ComparisonPage!
        var comparisonList: ComparisonListPage!
        var comparisonCellPage: ComparisonPage.ComparisonModelCellPage!

        "Открываем экран списков сравнения".ybm_run { _ in
            comparisonList = goToComparison()
        }

        "Открываем экран сранения".ybm_run { _ in
            comparison = comparisonList.fistComparisonCell.tap()
            wait(forVisibilityOf: comparison.element)
        }

        "Переходим в КМ".ybm_run { _ in
            comparisonCellPage = comparison.collectionView.modelCell(with: 0)
            let skuPage = comparisonCellPage.tap()
            wait(forVisibilityOf: skuPage.element)
        }

        "Нажимаем назад".ybm_run { _ in
            NavigationBarPage.current.backButton.tap()
            wait(forVisibilityOf: comparison.element)
        }

        "Скроллим вниз".ybm_run { _ in
            comparison.collectionView.collectionView.swipe(
                to: .down,
                until: comparison.deleteListButton.element.isVisible
            )
        }

        "Переходим в КМ".ybm_run { _ in
            let skuPage = comparisonCellPage.tap()
            wait(forVisibilityOf: skuPage.element)
        }

        "Нажимаем назад".ybm_run { _ in
            NavigationBarPage.current.backButton.tap()
            wait(forVisibilityOf: comparison.element)
        }

        "Проверяем скролл позицию".ybm_run { _ in
            wait(forVisibilityOf: comparison.deleteListButton.element)
        }
    }

}
