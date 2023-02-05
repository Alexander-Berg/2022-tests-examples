import MarketUITestMocks
import XCTest

final class DeeplinkYandexCardTests: YandexCardTests {

    override var user: UserAuthState {
        .loginWithYandexPlus
    }

    func testYandexCardDeeplink() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6129")
        Allure.addEpic("Морда")
        Allure.addFeature("YandexCard")
        Allure.addTitle("Открытие диплинка сдк банка с морды")

        "Открываем морду".ybm_run { _ in
            goToMorda()
        }

        "Открываем диплинк на сдк банка".ybm_run { _ in
            open(market: .yandexBank)

            let yanbankSDK = YandexBankPage.current
            wait(forVisibilityOf: yanbankSDK.element)
        }
    }
}
