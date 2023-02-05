import XCTest

class ComparisonTestDeleteOneItem: ComparisonBaseTest {

    func testDeleteOneItem() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3531")
        Allure.addEpic("Сравнение")
        Allure.addFeature("Экран сравнения")
        Allure.addTitle("При удалении последнего товара автоматически удаляется весь список сравнения")

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Comparisons_Delete")
        }

        var comparisonScreen: ComparisonPage!
        var toCategoryButton: XCUIElement!

        "Открываем список сравнений".ybm_run { _ in
            comparisonScreen = goToComparisonScreen()
        }

        "Удаляем элемент".ybm_run { _ in
            let model = comparisonScreen.collectionView
                .cellUniqueElement(withIdentifier: ComparisonAccessibility.Model.root)
                .otherElements.firstMatch
            let button = model.buttons.matching(identifier: ComparisonAccessibility.Model.delete).firstMatch
            button.tap()
            toCategoryButton = comparisonScreen.goToCategoryButton
            wait(forVisibilityOf: toCategoryButton)
        }

        "Переходим к категории".ybm_run { _ in
            toCategoryButton.tap()
            let feedElement = XCUIApplication().otherElements[FeedAccessibility.root]
            wait(forExistanceOf: feedElement)
        }
    }

    func testDeleteList() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3541")
        Allure.addEpic("Сравнение")
        Allure.addFeature("Экран сравнения")
        Allure.addTitle("Удаление списка по кнопке")

        var root: RootPage!
        var comparison: ComparisonPage!
        var comparisonList: ComparisonListPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Comparisons_3Items")
        }

        "Запускаем приложение".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
        }

        "Открываем экран списков сравнения".ybm_run { _ in
            comparisonList = goToComparison(root: root)
        }

        "Открываем экран сранения".ybm_run { _ in
            comparison = comparisonList.fistComparisonCell.tap()

            wait(forVisibilityOf: comparison.element)
        }

        var models: [XCUIElement] = []

        "Проверяем, что карточки видны".ybm_run { _ in
            models = comparison.collectionView.allCellUniqueElement(withIdentifier: ComparisonAccessibility.Model.root)
            XCTAssertEqual(models.count, 3)
            wait(forVisibilityOf: models[0])
        }

        var toCategoryButton: XCUIElement!
        "Скроллим вниз и удаляем список".ybm_run { _ in
            let deleteCell = comparison.collectionView.collectionView.cells.buttons
                .matching(identifier: ComparisonAccessibility.deleteListButton)
            comparison.collectionView.collectionView.swipe(to: .down, until: deleteCell.element.isVisible)
            deleteCell.firstMatch.tap()
            toCategoryButton = comparison.goToCategoryButton
            wait(forVisibilityOf: toCategoryButton)
        }

        "Переходим к категории".ybm_run { _ in
            toCategoryButton.tap()
            let feedElement = XCUIApplication().otherElements[FeedAccessibility.root]
            wait(forExistanceOf: feedElement)
        }

    }

}
