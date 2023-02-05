import XCTest

final class OrderDelayedTests: AgitationsUnauthorizedTestCase {

    private let bundleName = "Agitations_OrderDelayed"
    private let popupDescription =
        "Извините, продавец не успеет привезти заказ вовремя. Новая дата — 28 апреля с 10:00 до 22:00."

    func test_OrderDelayed_ClosePopup() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4469")
        Allure.addEpic("Информирование и подтверждение пользователем переноса доставки в DSBS")
        Allure.addTitle("Продавец не доставит вовремя + понятно")

        setup(bundleName: bundleName)
        openPopup(description: popupDescription)
        closePopup()
    }
}
