import XCTest

final class ComparisonListNavigationTest: ComparisonBaseTest {

    func testNavigation() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3527")
        Allure.addEpic("Сравнение")
        Allure.addFeature("Экран списков сравнения")
        Allure.addTitle("Навигация")

        var profile: ProfilePage!
        var comparisonList: ComparisonListPage!
        let navigationBar = NavigationBarPage.current

        "Перейти в профиль".ybm_run { _ in
            profile = goToProfile()
        }

        "Переходим в спискок сравнений".ybm_run { _ in
            wait(forVisibilityOf: profile.comparison.element)

            comparisonList = profile.comparison.tap()
            wait(forVisibilityOf: comparisonList.collectionView.element)
        }

        "Возвращаемся в профиль".ybm_run { _ in
            navigationBar.backButton.tap()
        }

        "Переходим в спискок сравнений".ybm_run { _ in
            wait(forVisibilityOf: profile.comparison.element)

            comparisonList = profile.comparison.tap()
            wait(forVisibilityOf: comparisonList.collectionView.element)
        }

        "Возвращаемся в профиль".ybm_run { _ in
            app.swipeBack()
            wait(forVisibilityOf: RootPage(element: app).tabBar.profilePage.element)
        }
    }
}
