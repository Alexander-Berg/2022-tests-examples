import XCTest

class ComparisonTestGoToSKUEntrypoints: ComparisonBaseTest {

    func testOpenModelWithDifferentEntrypoints() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3536")
        Allure.addEpic("Сравнение")
        Allure.addFeature("Экран сравнения")
        Allure.addTitle("Переход к карточке товара")

        var comparison: ComparisonPage!
        var comparisonList: ComparisonListPage!

        "Открываем экран списков сравнения".ybm_run { _ in
            comparisonList = goToComparison()
        }

        "Открываем экран сранения".ybm_run { _ in
            comparison = comparisonList.fistComparisonCell.tap()

            wait(forVisibilityOf: comparison.element)
        }

        let modelCell = comparison.collectionView.modelCell(with: 0)

        "Нажимаем на картинку, переходим в КМ".ybm_run { _ in
            let skuPage = modelCell.photo.tap()
            wait(forVisibilityOf: skuPage.element)
        }

        "Нажимаем назад".ybm_run { _ in
            NavigationBarPage.current.backButton.tap()
            wait(forVisibilityOf: comparison.element)
        }

        "Нажимаем на название, переходим в КМ".ybm_run { _ in
            let skuPage = modelCell.title.tap()
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

        "Нажимаем на название в превьюшке сверху, переходим в КМ".ybm_run { _ in
            let skuPage = modelCell.title.tap()
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
