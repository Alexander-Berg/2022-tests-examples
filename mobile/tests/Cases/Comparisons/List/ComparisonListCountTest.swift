import XCTest

final class ComparisonListCountTest: ComparisonBaseTest {

    func testCount() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3524")
        Allure.addEpic("Сравнение")
        Allure.addFeature("Экран списков сравнения")
        Allure.addTitle("Каунтер КМ в списке")

        disable(toggles: FeatureNames.priceFallSubscription)

        var comparisonList: ComparisonListPage!
        let navigationBar = NavigationBarPage.current
        let root = RootPage(element: app)
        var comparison: ComparisonPage!

        "Мокаем список сравнений".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Comparisons_CountInitial")
        }

        "Перейти на экран сравнений".ybm_run { _ in
            comparisonList = goToComparison()
            wait(forVisibilityOf: comparisonList.fistComparisonCell.element)
            XCTAssertEqual(comparisonList.fistComparisonCell.count.label, "3 товара")
        }

        "Возвращаемся в профиль".ybm_run { _ in
            navigationBar.backButton.tap()
        }

        "Добавить в сравнение КМ из той же категории".ybm_run { _ in
            let defaultSku = goToDefaultSKUPage(root: root)
            wait(forVisibilityOf: defaultSku.navigationBar.wishlistButton)
            defaultSku.navigationBar.comparisonButton.tap()
        }

        "Мокаем список сравнений".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Comparisons_CountIncremented")
        }

        "Перейти на экран сравнений".ybm_run { _ in
            comparisonList = goToComparison(root: root)
            XCTAssertEqual(comparisonList.fistComparisonCell.count.label, "4 товара")
        }

        "Перейти на экран сравнения этой категории, тапнув по категории".ybm_run { _ in
            wait(forVisibilityOf: comparisonList.fistComparisonCell.element)
            comparison = comparisonList.fistComparisonCell.tap()
        }

        "Удалить одну КМ".ybm_run { _ in
            let modelCell = comparison.collectionView.modelCell(with: 0)
            wait(forExistanceOf: modelCell.element)
            modelCell.delete.element.tap()

            wait(forVisibilityOf: comparison.deleteToast)
            wait(forInvisibilityOf: comparison.deleteToast)
        }

        "Мокаем список сравнений".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Comparisons_CountInitial")
        }

        "Перейти на экран сравнений".ybm_run { _ in
            comparisonList = goToComparison(root: root)
            XCTAssertEqual(comparisonList.fistComparisonCell.count.label, "3 товара")
        }
    }
}
