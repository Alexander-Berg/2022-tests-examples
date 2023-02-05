import MarketUITestMocks
import Metrics
import XCTest

final class PromoScreenHealthChainTests: LocalMockTestCase {

    override func setUp() {
        super.setUp()
        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
    }

    func test_shouldSendPromoScreenChain_whenMordaShown() throws {
        Allure.addEpic("Метрики здоровья")
        Allure.addTitle("Проверяем отправку цепочки SCREEN_OPENED_MAIN_COMPONENT.PROMO_SCREEN")

        var morda: MordaPage!

        "Открываем морду".ybm_run { _ in
            morda = goToMorda()
        }

        "Ждем появления первого viewport".ybm_run { _ in
            wait(forVisibilityOf: morda.historyWidget.container.element)
        }

        try "Проверяем отправку событий".ybm_run { _ in
            let screenOpenEvents = MetricRecorder.events(from: .health).with(name: "SCREEN_OPENED_MAIN_COMPONENT")
            let promoEvent =
                try XCTUnwrap(
                    screenOpenEvents
                        .first(where: { ($0.parameters["portion"] as? String) == "PROMO_SCREEN" })
                )

            let mainRequestId = try XCTUnwrap(promoEvent.parameters["requestId"] as? String)

            let appWillFinishLaunchingEvents = MetricRecorder.events(from: .health)
                .with(name: "APP_WILL_FINISH_LAUNCHING")
            XCTAssertEqual(appWillFinishLaunchingEvents.count, 1)
            XCTAssertEqual(appWillFinishLaunchingEvents.first?.parameters["requestId"] as? String, mainRequestId)

            let didWillFinishLaunchingEvents = MetricRecorder.events(from: .health)
                .with(name: "APP_DID_FINISH_LAUNCHING")
            XCTAssertEqual(didWillFinishLaunchingEvents.count, 1)
            XCTAssertEqual(didWillFinishLaunchingEvents.first?.parameters["requestId"] as? String, mainRequestId)
        }
    }

    func test_shouldNotSendPromoScreenChain_whenOnboardingShown() {
        Allure.addEpic("Метрики здоровья")
        Allure.addTitle("Проверяем, что не отправили метрику SCREEN_OPENED_MAIN_COMPONENT.PROMO_SCREEN на онбординге")

        "Настраиваем стейт кешбэка".ybm_run { _ in
            var authState = UserAuthState()
            authState.setPlusBalanceState(.withZeroMarketCashback)
            stateManager?.setState(newState: authState)
        }

        "Открываем приложение".ybm_run { _ in
            appWithOnboarding()
            wait(forVisibilityOf: WelcomeOnboardingPage.current.element, timeout: 20)
        }

        "Проверяем, что не отправили событие на онбординге".ybm_run { _ in
            XCTAssertFalse(
                MetricRecorder.events(from: .health)
                    .with(name: "SCREEN_OPENED_MAIN_COMPONENT")
                    .with(params: ["portion": "PROMO_SCREEN"])
                    .isNotEmpty
            )
        }

    }
}
