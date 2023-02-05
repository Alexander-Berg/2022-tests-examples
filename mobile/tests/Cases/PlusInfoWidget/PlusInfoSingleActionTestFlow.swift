import MarketUITestMocks
import XCTest

class PlusInfoSingleActionTestFlow: LocalMockTestCase {

    // MARK: - Properties

    private var toggleInfo: String {
        let toggleAdditionalInfo = [
            FeatureNames.plusBenefits.lowercased(): ["onboardingEnabled": true]
        ]
        let info = (try? JSONSerialization.data(
            withJSONObject: toggleAdditionalInfo,
            options: .prettyPrinted
        )).flatMap { String(data: $0, encoding: .utf8) }
        return info ?? ""
    }

    // MARK: - Public

    func passWidgetCheckFlowWithZeroBalance() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4243")
        Allure.addEpic("Морда")
        Allure.addFeature("Виджет про баллы")
        Allure.addTitle("Виджет на главной, если нет баллов")

        "Проверяем отсутствие виджета".ybm_run { _ in
            let snippet = getPlusInfoWidget(with: .withZeroMarketCashback)
            XCTAssertFalse(snippet.element.isVisible)
        }
    }

    func passWidgetCheckFlowWithBalance() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4244")
        Allure.addEpic("Морда")
        Allure.addFeature("Виджет про баллы")
        Allure.addTitle("Виджет на главной. Не плюсовик с баллами")

        var snippet: HoveringSnippetPage!

        "Ждем появления виджета".ybm_run { _ in
            snippet = getPlusInfoWidget(with: .withMarketCashback_5)
            wait(forVisibilityOf: snippet.element)
        }

        "Проходим онбординг плюса".ybm_run { _ in
            snippet.actionButton.element.tap()
            let plusOnboardingPage = PlusOnboardingPage.current

            for _ in 0 ... 1 {
                wait(forVisibilityOf: plusOnboardingPage.button)
                plusOnboardingPage.button.tap()
            }
        }

        "Открываем и закрываем Дом Плюса".ybm_run { _ in
            let homeplusPage = HomePlusPage.current
            wait(forVisibilityOf: homeplusPage.element)
            homeplusPage.element.swipeDown()
        }

        "Проверяем отсутствие виджета".ybm_run { _ in
            wait(forVisibilityOf: MordaPage.current.element)
            XCTAssertFalse(snippet.element.isVisible)
        }
    }

    // MARK: - Private

    private func getPlusInfoWidget(with balanceState: UserPlusBalanceState) -> HoveringSnippetPage {
        var morda: MordaPage!

        "Включаем тоглы".ybm_run { _ in
            enable(
                toggles: FeatureNames.plusBenefits,
                FeatureNames.showPlus,
                FeatureNames.mordaRedesign
            )
            app.launchEnvironment[TestLaunchEnvironmentKeys.enabledTogglesInfo] = toggleInfo
        }

        "Настраиваем стейты".ybm_run { _ in
            var authState = UserAuthState()
            authState.setPlusBalanceState(balanceState)
            stateManager?.setState(newState: authState)
        }

        "Открываем морду".ybm_run { _ in
            morda = goToMorda()
        }

        return morda.singleActionContainerWidget.container.plusInfoWidget.snippet
    }

    // MARK: - Nested Types

    typealias UserPlusBalanceState = ResolveUserPlusBalance.UserPlusBalance

}
