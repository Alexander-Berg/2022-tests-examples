import XCTest

class ProfilePublicationsTest: LocalMockTestCase {

    func testUnloginUserProfileNotContainMyPublications() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3930")
        Allure.addEpic("ЛК")
        Allure.addFeature("Мои публикации")
        Allure.addTitle("Проверяем отсутствие \"Мои публикации\" у незалогированного пользователя")

        var root: RootPage!
        var profile: ProfilePage!

        "Запускаем приложение и переходим в профиль".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            profile = goToProfile(root: root)
        }

        "Проверяем, что \"Мои публикации\" отсутвуют".ybm_run { _ in
            XCTAssertFalse(profile.myPublications.title.isVisible)
        }
    }
}
