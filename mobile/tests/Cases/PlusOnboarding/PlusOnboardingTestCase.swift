import MarketUITestMocks
import XCTest

class PlusOnboardingTestCase: LocalMockTestCase {

    // MARK: - Public

    func completeTestFlow(
        with pages: [PageContent],
        bundleName: String
    ) {
        var root: RootPage!
        var morda: MordaPage!
        var onboarding: PlusOnboardingPage!

        enable(toggles: FeatureNames.plusBenefits)
        app.launchEnvironment[TestLaunchEnvironmentKeys.enabledTogglesInfo] = toggleInfo

        "Мокаем данные".ybm_run { _ in
            mockStateManager?.pushState(bundleName: bundleName)
        }

        "Открываем приложение и авторизуемся неплюсовиком".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
        }

        "Переходим на морду".ybm_run { _ in
            morda = goToMorda(root: root)
        }

        "Нажимаем на таблетку Плюса".ybm_run { _ in
            morda.plusButton.element.tap()
        }

        "Ждем открытия онбординга".ybm_run { _ in
            onboarding = PlusOnboardingPage.current
            wait(forExistanceOf: onboarding.element)
        }

        for (index, page) in pages.enumerated() {
            "Проверяем контент \(index + 1) страницы".ybm_run { _ in
                XCTAssertTrue(onboarding.image.exists)
                XCTAssertEqual(onboarding.title.label, page.title)
                XCTAssertEqual(onboarding.text.label, page.text)
                XCTAssertEqual(onboarding.button.label, page.buttonTitle)

                onboarding.button.tap()
            }
        }

        "Проверяем открытие Дома Плюса".ybm_run { _ in
            wait(forExistanceOf: HomePlusPage.current.element)
        }

        "Закрываем Дома Плюса".ybm_run { _ in
            HomePlusPage.current.element.swipeDown()
            wait(forVisibilityOf: morda.element)
        }

        "Нажимаем на таблетку Плюса".ybm_run { _ in
            morda.plusButton.element.tap()
        }

        "Проверяем открытие Дома Плюса".ybm_run { _ in
            wait(forExistanceOf: HomePlusPage.current.element)
        }
    }

    func setZeroBalanceState() {
        "Настраиваем стейт".ybm_run { _ in
            var authState = UserAuthState()
            authState.setPlusBalanceState(.withZeroMarketCashback)
            stateManager?.setState(newState: authState)
        }
    }

    func setNoZeroBalanceState() {
        "Настраиваем стейт".ybm_run { _ in
            var authState = UserAuthState()
            authState.setPlusBalanceState(.withMarketCashback_5)
            stateManager?.setState(newState: authState)
        }
    }

    var toggleInfo: String {
        let name = FeatureNames.plusBenefits.lowercased()
        let toggleAdditionalInfo = [
            name: [
                "onboardingEnabled": true,
                "deliveryPopupEnabled": true
            ]
        ]
        guard let toggleInfosData = try? JSONSerialization.data(
            withJSONObject: toggleAdditionalInfo,
            options: .prettyPrinted
        )
        else {
            return ""
        }
        return String(data: toggleInfosData, encoding: .utf8) ?? ""
    }
}

extension PlusOnboardingTestCase {

    struct PageContent {
        let title: String
        let text: String
        let buttonTitle: String
    }

}
