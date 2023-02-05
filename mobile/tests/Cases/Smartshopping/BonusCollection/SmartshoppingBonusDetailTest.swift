import MarketUITestMocks
import XCTest

final class SmartshoppingBonusDetailTest: SmartbonusDetailsTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testBonusNoRestrictions() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3285")
        Allure.addEpic("Купоны")
        Allure.addFeature("Переходы с активного купона")
        Allure.addTitle("Переход в каталог")

        var root: RootPage!
        var smartshoppingPage: SmartshoppingPage!
        var bonusPopup: SmartbonusDetailsPopupPage!

        "Мокаем состояние".ybm_run { _ in
            let endDate = getNewDate(byAddingDays: 14)
            changeCoinsEndDateInMock(
                bundleName: "SmartshoppingSet_BonusDetailNoRestrictions",
                newDate: endDate
            )
        }

        "Открываем приложение, авторизуемся, переходим в коллекцию купонов".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            smartshoppingPage = goToMyBonuses(root: root)
        }

        "Нажимаем на купон и ждём открытия попапа".ybm_run { _ in
            let bonusSnippet = smartshoppingPage.singleCouponCard
            bonusSnippet.tap()
            bonusPopup = SmartbonusDetailsPopupPage.currentPopup
            wait(forExistanceOf: bonusPopup.element)
        }

        "Нажимаем на кнопку 'Выбрать товары' и ожидаем перехода в каталог".ybm_run { _ in
            bonusPopup.bottomButton.tap()
            let catalogPage = root.tabBar.catalogPage
            wait(forVisibilityOf: catalogPage.element)
        }

    }

    func testBonusFeedRestrictions() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3284")
        Allure.addEpic("Купоны")
        Allure.addFeature("Переходы с активного купона")
        Allure.addTitle("Переход на выдачу")

        var root: RootPage!
        var smartshoppingPage: SmartshoppingPage!
        var bonusPopup: SmartbonusDetailsPopupPage!

        "Мокаем состояние".ybm_run { _ in
            let endDate = getNewDate(byAddingDays: 14)
            changeCoinsEndDateInMock(
                bundleName: "SmartshoppingSet_BonusDetailFeedRestrictions",
                newDate: endDate
            )
        }

        "Открываем приложение, авторизуемся, переходим в коллекцию купонов".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            smartshoppingPage = goToMyBonuses(root: root)
        }

        "Нажимаем на купон и ждём открытия попапа".ybm_run { _ in
            let bonusSnippet = smartshoppingPage.singleCouponCard
            bonusSnippet.tap()
            bonusPopup = SmartbonusDetailsPopupPage.currentPopup
            wait(forExistanceOf: bonusPopup.element)
        }

        "Нажимаем на кнопку 'Выбрать товары'".ybm_run { _ in
            bonusPopup.bottomButton.tap()
        }

        "Проверяем переход в выдачу и соответствие заголовка навбара".ybm_run { _ in
            let feedPage = FeedPage.current
            wait(forVisibilityOf: feedPage.element)
            XCTAssertEqual(feedPage.navigationBar.title.label, "Можно применить купон")
        }

    }
}
