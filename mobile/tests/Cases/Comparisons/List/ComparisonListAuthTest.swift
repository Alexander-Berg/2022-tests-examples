import XCTest

final class ComparisonListAuthTest: ComparisonBaseTest {

    func testLoginViewForNotAuthorized() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3502")
        Allure.addEpic("Сравнение")
        Allure.addFeature("Экран списков сравнения")
        Allure.addTitle("Незалогин. Плашка авторизации.")

        var profile: ProfilePage!
        var comparisonList: ComparisonListPage!

        "Тапнуть на иконку профиля в таббаре".ybm_run { _ in
            profile = goToProfile()
        }

        "Тапнуть на \"Списки сравнения\"".ybm_run { _ in
            wait(forVisibilityOf: profile.comparison.element)

            comparisonList = profile.comparison.tap()
            wait(forVisibilityOf: comparisonList.collectionView.element)
        }

        "Тапнуть на \"Войти\"".ybm_run { _ in
            wait(forVisibilityOf: comparisonList.loginButton)
            comparisonList.loginButton.tap()

            completeAuthFlow()

            wait(forInvisibilityOf: comparisonList.loginButton)
        }
    }
}
