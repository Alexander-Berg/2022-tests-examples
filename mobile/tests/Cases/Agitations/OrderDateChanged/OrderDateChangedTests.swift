import XCTest

final class OrderDateChangedTests: AgitationsUnauthorizedTestCase {

    private let bundleName = "Agitations_OrderDateChanged"
    private let mainPopupDescription = "На 28 апреля с 10:00 до 22:00. Об этом нам сообщил продавец."
    private let acceptPopupDescription = "Пусть покупка вас порадует"
    private let declinePopupDescription =
        "Заказ доставят 28 апреля с 10:00 до 22:00. Разберёмся, почему продавец изменил дату доставки."

    func test_OrderDateChanged_UserAccepted_ClosePopup() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4470")
        Allure.addEpic("Информирование и подтверждение пользователем переноса доставки в DSBS")
        Allure.addTitle("Покупатель переносил дату + понятно")

        setup(bundleName: bundleName)
        openPopup(description: mainPopupDescription)
        acceptPopup(description: acceptPopupDescription)
        closePopup()
    }

    func test_OrderDateChanged_UserDeclined_ClosePopup() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4471")
        Allure.addEpic("Информирование и подтверждение пользователем переноса доставки в DSBS")
        Allure.addTitle("Покупатель не переносил дату + понятно")

        setup(bundleName: bundleName)
        openPopup(description: mainPopupDescription)
        declinePopup(description: declinePopupDescription)
        closePopup()
    }
}
