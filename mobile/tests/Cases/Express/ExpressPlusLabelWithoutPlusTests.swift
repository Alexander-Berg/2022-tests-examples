import MarketUITestMocks
import XCTest

final class ExpressPlusLabelWithoutPlusTests: ExpressPlusTestFlow {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testPlusLabelWithPlus() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/4946")
        Allure.addEpic("Экспресс")
        Allure.addFeature("Таблетка Плюса")
        Allure.addTitle("Таблетка Плюса на экране экспресса у неплюсовика")

        enable(toggles: FeatureNames.showPlus)
        makeExpressPlusTestFlow()
    }
}
