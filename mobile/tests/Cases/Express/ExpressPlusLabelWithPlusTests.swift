import MarketUITestMocks
import XCTest

final class ExpressPlusLabelWithPlusTests: ExpressPlusTestFlow {

    override var user: UserAuthState {
        .loginWithYandexPlus
    }

    func testPlusLabelWithPlus() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/4945")
        Allure.addEpic("Экспресс")
        Allure.addFeature("Таблетка Плюса")
        Allure.addTitle("Таблетка Плюса на экране экспресса у плюсовика")

        makeExpressPlusTestFlow()
    }
}
