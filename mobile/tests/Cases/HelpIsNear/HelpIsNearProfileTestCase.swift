import MarketUITestMocks
import XCTest

final class HelpIsNearProfileTestCase: LocalMockTestCase {

    override var user: UserAuthState {
        .loginWithYandexPlus
    }

    func testHelpIsNearInProfileUnsubscribed() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4342")
        Allure.addEpic("Личный кабинет")
        Allure.addFeature("Помощь рядом")
        Allure.addTitle("Отображение плашки \"Помощь рядом\"")

        var profile: ProfilePage!
        var root: RootPage!

        "Мокаем состояние, когда пользователь не подписан".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "HelpIsNearUnsubscribed")
        }

        "Открываем приложение и авторизуемся".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            profile = goToProfile(root: root)
        }

        "Ищем ячейку \"Помощь рядом\"".ybm_run { _ in
            wait(forVisibilityOf: profile.helpIsNear.element)
            XCTAssertEqual(
                profile.helpIsNear.title.label,
                "Помощь рядом"
            )

            XCTAssertEqual(
                profile.helpIsNear.subtitle.label,
                "Социальный проект Яндекса"
            )
        }

        "Мокаем состояние, когда пользователь не подписан".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "HelpIsNearSubscribed")
            goToMorda(root: root)
            profile = goToProfile(root: root)
        }

        "Ищем ячейку \"Помощь рядом\"".ybm_run { _ in
            wait(forVisibilityOf: profile.helpIsNear.element)
            XCTAssertEqual(
                profile.helpIsNear.title.label,
                "Помощь рядом"
            )

            XCTAssertEqual(
                profile.helpIsNear.subtitle.label,
                "Спасибо, что помогаете!"
            )

            XCTAssertEqual(
                profile.helpIsNear.value.label,
                "300 ₽"
            )
        }

    }

    func testHelpIsNearInProfileSubscribed() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4343")
        Allure.addEpic("Личный кабинет")
        Allure.addFeature("Помощь рядом")
        Allure.addTitle("Нажати на плашку \"Помощь рядом\"")

        var profile: ProfilePage!
        var root: RootPage!
        var webPage: WebViewPage!

        "Мокаем состояние, когда пользователь подписан".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "HelpIsNearSubscribed")
        }

        "Открываем приложение и авторизуемся".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            profile = goToProfile(root: root)
        }

        "Проверяем открытие лендинга".ybm_run { _ in
            webPage = profile.helpIsNear.tap()

            wait(forVisibilityOf: webPage.element)
        }
    }
}
