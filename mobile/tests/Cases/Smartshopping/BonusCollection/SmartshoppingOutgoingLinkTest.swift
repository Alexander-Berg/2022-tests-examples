import MarketUITestMocks
import XCTest

final class SmartshoppingOutgoingLinkTest: SmartbonusDetailsTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testValidLink() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3092")
        Allure.addEpic("Купоны")
        Allure.addFeature("Переход с купона по ссылке")
        Allure.addTitle("Валидная ссылка на категорию")

        var root: RootPage!
        var smartshoppingPage: SmartshoppingPage!
        var bonusPopup: SmartbonusDetailsPopupPage!

        let endDate = getNewDate(byAddingDays: 14)

        "Мокаем состояние".ybm_run { _ in
            changeCoinsEndDateInMock(
                bundleName: "SmartshoppingSet_OutgoingLink",
                newDate: endDate
            )
        }

        "Открываем приложение, авторизуемся, переходим в коллекцию купонов".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            smartshoppingPage = goToMyBonuses(root: root)
        }

        "Нажимаем на купон и ждём открытия попапа".ybm_run { _ in
            let bonusSnippet = smartshoppingPage.singleCouponCard
            wait(forVisibilityOf: bonusSnippet.element)
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
        }

    }

    func testSpecialLink() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3093")
        Allure.addEpic("Купоны")
        Allure.addFeature("Переход с купона по ссылке")
        Allure.addTitle("Валидная ссылка на special страницу")

        var root: RootPage!
        var smartshoppingPage: SmartshoppingPage!
        var bonusPopup: SmartbonusDetailsPopupPage!

        let endDate = getNewDate(byAddingDays: 14)

        "Мокаем webview".ybm_run { _ in
            let url = (app.launchEnvironment[TestLaunchEnvironmentKeys.capiUrl] ?? "") + "/webview"
            app.launchEnvironment[TestLaunchEnvironmentKeys.webViewPagesUrl] = url
        }

        "Мокаем состояние".ybm_run { _ in
            changeCoinsEndDateInMock(
                bundleName: "SmartshoppingSet_SpecialLink",
                newDate: endDate
            )
        }

        "Открываем приложение, авторизуемся, переходим в коллекцию купонов".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            smartshoppingPage = goToMyBonuses(root: root)
        }

        "Нажимаем на купон и ждём открытия попапа".ybm_run { _ in
            let bonusSnippet = smartshoppingPage.singleCouponCard
            wait(forVisibilityOf: bonusSnippet.element)
            bonusSnippet.tap()
            bonusPopup = SmartbonusDetailsPopupPage.currentPopup
            wait(forExistanceOf: bonusPopup.element)
        }

        "Мокаем веб страницу".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SmartshoppingSet_SpecialLinkWebview")
        }

        "Нажимаем на кнопку 'Выбрать товары'".ybm_run { _ in
            bonusPopup.bottomButton.tap()
        }

        "Проверяем переход на вебвью с нужным тайтлом".ybm_run { _ in
            let webview = WebViewPage.current
            ybm_wait { webview.element.isVisible }
            ybm_wait {
                webview.navigationBar.element
                    .identifier.starts(with: "Пора праздновать - последняя распродажа года на маркетплейсе")
            }
        }

    }

}
