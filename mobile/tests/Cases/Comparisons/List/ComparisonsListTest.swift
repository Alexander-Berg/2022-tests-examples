import XCTest

final class ComparisonsListTest: LocalMockTestCase {

    func testLoginViewForNotAuthorized() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3502")
        Allure.addEpic("Сравнение")
        Allure.addFeature("Экран списков сравнения")
        Allure.addTitle("Экран списков сравнения. Незалогин. Плашка авторизации.")

        var root: RootPage!
        var profile: ProfilePage!
        var comparisonList: ComparisonListPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Comparisons_Basic")
        }

        "Запускаем приложение".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
        }

        "Перейти в профиль".ybm_run { _ in
            profile = goToProfile(root: root)
        }

        "Переходим в спискок сравнений".ybm_run { _ in
            wait(forVisibilityOf: profile.comparison.element)

            comparisonList = profile.comparison.tap()
            wait(forVisibilityOf: comparisonList.collectionView.element)
        }

        "По клику на кнопку открывается экран авторизации".ybm_run { _ in
            wait(forVisibilityOf: comparisonList.loginButton)
            comparisonList.loginButton.tap()

            completeAuthFlow()

            wait(forInvisibilityOf: comparisonList.loginButton)
        }
    }
}
