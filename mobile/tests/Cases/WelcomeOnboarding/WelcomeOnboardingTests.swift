import MarketUITestMocks
import XCTest

class WelcomeOnboardingTests: LocalMockTestCase {

    var welcomeOnboarding: WelcomeOnboardingPage!
    var morda: MordaPage!

    func testFirstOpening() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5739")
        Allure.addEpic("Welcome Onboarding")
        Allure.addFeature("Первое открытие приложения")
        Allure.addTitle("Проверяем проход по велкому")

        "Настраиваем стейт кешбэка".ybm_run { _ in
            var authState = UserAuthState()
            authState.setPlusBalanceState(.withZeroMarketCashback)
            stateManager?.setState(newState: authState)
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "PushNotifications_On")
        }

        "Открываем приложение, видим велком онбординг".ybm_run { _ in
            welcomeOnboarding = appWithOnboarding().onboarding
            wait(forVisibilityOf: welcomeOnboarding.geoCell, timeout: 20)
        }

        "Перезагружаем приложение. Проходим онбординг".ybm_run { _ in
            relaunchApp(clearData: false)
            welcomeOnboarding = appWithOnboarding().onboarding
            wait(forVisibilityOf: welcomeOnboarding.geoCell, timeout: 20)
            welcomeOnboarding.geoActionButton.tap()

            morda = MordaPage.current
            wait(forVisibilityOf: morda.element)
        }

        "Перезагружаем приложение. Ждем открытия морды".ybm_run { _ in
            relaunchApp()
            appAfterOnboardingAndPopups()

            morda = MordaPage.current
            wait(forVisibilityOf: morda.element)
        }
    }
}
