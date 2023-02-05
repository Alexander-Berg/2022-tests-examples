import MarketUITestMocks
import XCTest

final class ReferralProgramSingleActionTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testReferralProgramSingleActionWidgetIsVisiable() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4478")
        Allure.addEpic("Морда")
        Allure.addFeature("Реферальная программа")
        Allure.addTitle("Виджет на главной")

        var morda: MordaPage!
        var snippet: HoveringSnippetPage!

        "Настраиваем FT и мокаем startup для получения эксперимента".ybm_run { _ in
            app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
            app.launchEnvironment[TestLaunchEnvironmentKeys.enabledToggles] = FeatureNames.referralProgram
            mockStateManager?.pushState(bundleName: "Experiments_ReferralProgram")
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "ReferralProgram")
        }

        "Авторизуемся, открываем морду".ybm_run { _ in
            let root = appAfterOnboardingAndPopups()
            morda = goToMorda(root: root)
        }

        "Проверяем наличие виджета".ybm_run { _ in
            snippet = morda.singleActionContainerWidget.container.referralProgramWidget.snippet
            ybm_wait(forVisibilityOf: [snippet.element])

            XCTAssertEqual(snippet.titleLabel.label, "Посоветуйте нас друзьям")
            XCTAssertEqual(snippet.subtitleLabel.label, "И получите 300 баллов Плюса")
            XCTAssertEqual(snippet.actionButton.element.label, "Подробнее")
            XCTAssertEqual(snippet.additionalActionButton.element.label, "Закрыть")
        }

        "Закрываем виджет".ybm_run { _ in
            snippet.additionalActionButton.element.tap()
            wait(forInvisibilityOf: snippet.element)
        }
    }

    func testReferralProgramSingleActionWidgetDetails() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4480")
        Allure.addEpic("Морда")
        Allure.addFeature("Реферальная программа")
        Allure.addTitle("Кнопка \"Подробнее\" на виджете")

        var morda: MordaPage!
        var snippet: HoveringSnippetPage!
        var referralPage: ReferralPromocodePage!

        "Настраиваем FT и мокаем startup для получения эксперимента".ybm_run { _ in
            app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
            app.launchEnvironment[TestLaunchEnvironmentKeys.enabledToggles] = FeatureNames.referralProgram
            mockStateManager?.pushState(bundleName: "Experiments_ReferralProgram")
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "ReferralProgram")
        }

        "Авторизуемся, открываем морду".ybm_run { _ in
            let root = appAfterOnboardingAndPopups()
            morda = goToMorda(root: root)
        }

        "Проверяем наличие виджета".ybm_run { _ in
            snippet = morda.singleActionContainerWidget.container.referralProgramWidget.snippet
            ybm_wait(forVisibilityOf: [snippet.element])
        }

        "Открываем окно реферальной программы".ybm_run { _ in
            snippet.actionButton.element.tap()
            referralPage = ReferralPromocodePage.current

            wait(forVisibilityOf: referralPage.element)
        }
    }
}
