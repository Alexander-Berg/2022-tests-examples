import XCTest

final class ComparisonsTestGoBack: ComparisonBaseTest {

    func testTapBackAndSwipeBack() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3533")
        Allure.addEpic("Сравнение")
        Allure.addFeature("Экран сравнения")
        Allure.addTitle("Тап в нативный/хардварный BACK ведет на экран управления списками сравнения")

        var root: RootPage!
        var comparison: ComparisonPage!
        var comparisonList: ComparisonListPage!

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

        "Нажимаем 'Назад'".ybm_run { _ in
            NavigationBarPage.current.backButton.tap()
            wait(forVisibilityOf: comparisonList.element)
        }

        "Открываем экран сранения".ybm_run { _ in
            comparison = comparisonList.fistComparisonCell.tap()

            wait(forVisibilityOf: comparison.element)
        }

        "Делаем свайп назад".ybm_run { _ in
            app.swipeBack()
            wait(forVisibilityOf: comparisonList.element)
        }

    }
}
