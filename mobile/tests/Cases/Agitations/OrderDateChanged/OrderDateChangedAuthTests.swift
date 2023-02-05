import XCTest

final class OrderDateChangedAuthTests: AgitationsAuthTestCase {

    private let bundleName = "Agitations_OrderDateChanged"
    private let mainPopupDescription = "На 28 апреля с 10:00 до 22:00. Об этом нам сообщил продавец."
    private let acceptPopupDescription = "Пусть покупка вас порадует"
    private let declinePopupDescription =
        "Заказ доставят 28 апреля с 10:00 до 22:00. Разберёмся, почему продавец изменил дату доставки."

    func test_OrderDateChanged_UserAccepted_OpenChat() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4405")
        Allure.addEpic("Информирование и подтверждение пользователем переноса доставки в DSBS")
        Allure.addTitle("Покупатель переносил дату + чат")

        setup(bundleName: bundleName)
        openPopup(description: mainPopupDescription)
        acceptPopup(description: acceptPopupDescription)
        openChat()
    }

    func test_OrderDateChanged_UserDeclined_OpenChat() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4466")
        Allure.addEpic("Информирование и подтверждение пользователем переноса доставки в DSBS")
        Allure.addTitle("Покупатель не переносил дату + чат")

        setup(bundleName: bundleName)
        openPopup(description: mainPopupDescription)
        declinePopup(description: declinePopupDescription)
        openChat()
    }
}
