import MarketUITestMocks
import XCTest

final class ReferralProgramEntryPointTest: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testMenuItemCellIsVisible() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4481")
        Allure.addEpic("Профиль")
        Allure.addFeature("Реферальная программа")
        Allure.addTitle("Блок в Профиле")

        var profile: ProfilePage!

        "Настраиваем FT и мокаем startup для получения эксперимента".ybm_run { _ in
            app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
            app.launchEnvironment[TestLaunchEnvironmentKeys.enabledToggles] = FeatureNames.referralProgram
            mockStateManager?.pushState(bundleName: "Experiments_ReferralProgram")
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "ReferralProgram")
        }

        "Настраиваем стейт".ybm_run { _ in
            var authState = UserAuthState()
            authState.setPlusBalanceState(.noMarketCashback)
            stateManager?.setState(newState: authState)
        }

        "Авторизуемся, открываем профиль".ybm_run { _ in
            let root = appAfterOnboardingAndPopups()
            profile = goToProfile(root: root)
        }

        "Проверяем наличие пункта в профиле".ybm_run { _ in
            profile.collectionView.swipe(to: .down, untilVisible: profile.referral.element)

            XCTAssertEqual(profile.referral.title.label, "Приглашайте друзей")
            XCTAssertEqual(profile.referral.subtitle.label, "И получайте 300 баллов Плюса за каждого друга")
        }

        "Открываем окно реферальной программы".ybm_run { _ in
            let referral = profile.referral.tap()
            wait(forVisibilityOf: referral.element)
        }
    }

    func testReferralProgramForNotFinishedState() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4483")
        Allure.addEpic("Профиль")
        Allure.addFeature("Реферальная программа")
        Allure.addTitle("Онбординг")

        var root: RootPage!
        var profile: ProfilePage!
        var referral: ReferralPromocodePage!

        "Настраиваем FT и мокаем startup для получения эксперимента".ybm_run { _ in
            app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
            app.launchEnvironment[TestLaunchEnvironmentKeys.enabledToggles] = FeatureNames.referralProgram
            mockStateManager?.pushState(bundleName: "Experiments_ReferralProgram")
        }

        let endDate = makeStringRepresentation(of: Date().addingTimeInterval(.week))

        "Изменяем дату окончания промокода на валидную".ybm_run { _ in
            mockStateManager?.changeMock(
                bundleName: "ReferralProgram",
                newBundleName: "ReferralProgramPromocodeDateUpdate",
                filename: "POST_api_v1_resolveReferralPromocode",
                changes: [
                    (
                        #""refererPromoCodeExpiredDate" : "2021-06-28T13:36:36.562790Z""#,
                        "\"refererPromoCodeExpiredDate\" : \"\(endDate.en)\""
                    )
                ]
            )
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "ReferralProgram")
            mockStateManager?.pushState(bundleName: "ReferralProgramPromocodeDateUpdate")
        }

        "Настраиваем стейт".ybm_run { _ in
            var authState = UserAuthState()
            authState.setPlusBalanceState(.noMarketCashback)
            stateManager?.setState(newState: authState)
        }

        "Авторизуемся, открываем профиль".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            profile = goToProfile(root: root)
        }

        "Открываем окно реферальной программы".ybm_run { _ in
            profile.collectionView.swipe(to: .down, untilVisible: profile.referral.element)
            referral = profile.referral.tap()
            wait(forVisibilityOf: referral.element)

            XCTAssertEqual(
                referral.title.label.trimmingCharacters(in: .whitespacesAndNewlines),
                "Ваш промокод\nSOME PROMO"
            )
            XCTAssertEqual(
                referral.text.label,
                "Друзьям — скидка 500 ₽ на первый заказ в приложении от 5 000 ₽, а вам —  300 баллов Плюса. Промокод действует до \(endDate.ru). "
            )
            XCTAssertEqual(referral.button.label, "Отправить промокод другу")
        }

        "Нажать на значок копирования".ybm_run { _ in
            let toast = referral.copyClipboard()
            wait(forVisibilityOf: toast.element)

            XCTAssertEqual(toast.text.firstMatch.label, "Промокод скопирован")

            wait(forInvisibilityOf: toast.element)
        }

        "Нажать на \"Условия акции\"".ybm_run { _ in
            referral.openDetails()
            let browser = WebViewPage.current
            wait(forVisibilityOf: browser.element)

            browser.navigationBar.closeButton.tap()

            wait(forInvisibilityOf: browser.element)
        }

        "Нажать на кнопку \"Отправить промокод другу\"".ybm_run { _ in
            let popup = XCUIApplication()
                .navigationBars
                .matching(identifier: "UIActivityContentView")
                .firstMatch
            referral.share()
            wait(forVisibilityOf: popup)

            popup.buttons.firstMatch.tap()

            wait(forInvisibilityOf: popup)
        }
    }

}

// MARK: - ReferralProgramMockHelper

extension ReferralProgramEntryPointTest: ReferralProgramMockHelper {}
