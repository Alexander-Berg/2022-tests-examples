import XCTest

final class PostamateTests: LocalMockTestCase {

    // MARK: - Public

    func testPostamateInProfile() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3229")
        Allure.addEpic("Постаматы")
        Allure.addFeature("Постаматы в профиле")
        Allure.addTitle("Кнопка \"Открыть постамат Беру\"")

        var profile: ProfilePage!

        "Переходим в профиль".ybm_run { _ in
            profile = goToProfile()
        }

        "Проверяем кнопку \"Открыть постамат Беру\"".ybm_run { _ in
            profile.collectionView.ybm_swipeCollectionView(toFullyReveal: profile.postamate.element)
            XCTAssert(profile.postamate.element.isVisible)
            XCTAssertEqual(profile.postamate.title.label, "Открыть постамат Яндекс.Маркета")

            // Проверяем что кнопка постаматов ниже кнопки "Справка"
            XCTAssertGreaterThan(profile.postamate.element.frame.origin.y, profile.help.element.frame.origin.y)
        }

        "Нажимаем на кнопку \"Открыть постамат Беру\"".ybm_run { _ in
            profile.postamate.element.tap()
            wait(forInvisibilityOf: profile.element)
        }

        "Проверяем экран получения заказа".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { NavigationBarPage.current.title.label == "Получение заказа" })
        }
    }

}
