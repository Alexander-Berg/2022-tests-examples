import XCTest

final class OrderItemsRemovedByUserTests: AgitationsUnauthorizedTestCase {

    private let postpaidBundleName = "Agitations_OrderItemsRemovedByUser_Postpaid"
    private let prepaidBundleName = "Agitations_OrderItemsRemovedByUser_Prepaid"

    private let mainPopupDescription = "Продавец сообщил, что вы решили отказаться от этих товаров"
    private let acceptPopupDescription = "Эти товары можно заменить — вдруг что-то понравится"
    private let declinePostpaidPopupDescription =
        "Сумма вашего заказа теперь 13 600 ₽. Разберёмся, почему продавец удалил товары из заказа."
    private let declinePrepaidPopupDescription =
        "110 ₽ вернём на вашу карту в течение нескольких дней. Разберёмся, почему продавец удалил товары из заказа."

    func test_OrderItemsRemovedByUser_Postpaid_UserAccepted_OpenOrderDetails() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4645")
        Allure.addEpic("Удаление товаров из заказа FBS (отгрузка) & DBS (все этапы)")
        Allure.addFeature("Удаление товара из заказа")
        Allure.addTitle("Удаление, постоплата. Покупатель изменил заказ + подробнее о заказе")

        setup(bundleName: postpaidBundleName)
        openPopup(description: mainPopupDescription)
        acceptPopup(description: acceptPopupDescription)
        openOrderDetails()
    }

    func test_OrderItemsRemovedByUser_Postpaid_UserAccepted_OpenSimilars() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4518")
        Allure.addEpic("Удаление товаров из заказа FBS (отгрузка) & DBS (все этапы)")
        Allure.addFeature("Удаление товара из заказа")
        Allure.addTitle("Удаление, постоплата. Покупатель изменил заказ + выбрать похожие товары")

        setup(bundleName: postpaidBundleName)
        openPopup(description: mainPopupDescription)
        acceptPopup(description: acceptPopupDescription)
        chooseSimilarItem()
    }

    func test_OrderItemsRemovedByUser_Postpaid_UserDeclined_OpenOrderDetails() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4647")
        Allure.addEpic("Удаление товаров из заказа FBS (отгрузка) & DBS (все этапы)")
        Allure.addFeature("Удаление товара из заказа")
        Allure.addTitle("Удаление, постоплата. Покупатель не менял заказ + подробнее о заказе")

        setup(bundleName: postpaidBundleName)
        openPopup(description: mainPopupDescription)
        declinePopup(description: declinePostpaidPopupDescription)
        openOrderDetails()
    }

    func test_OrderItemsRemovedByUser_Postpaid_UserDeclined_OpenSimilars() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4646")
        Allure.addEpic("Удаление товаров из заказа FBS (отгрузка) & DBS (все этапы)")
        Allure.addFeature("Удаление товара из заказа")
        Allure.addTitle("Удаление, постоплата. Покупатель не менял заказ + выбрать похожие товары")

        setup(bundleName: postpaidBundleName)
        openPopup(description: mainPopupDescription)
        declinePopup(description: declinePostpaidPopupDescription)
        chooseSimilarItem()
    }

    func test_OrderItemsRemovedByUser_Prepaid_UserDeclined_OpenOrderDetails() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4647")
        Allure.addEpic("Удаление товаров из заказа FBS (отгрузка) & DBS (все этапы)")
        Allure.addFeature("Удаление товара из заказа")
        Allure.addTitle("Удаление, постоплата. Покупатель не менял заказ + подробнее о заказе")

        setup(bundleName: prepaidBundleName)
        openPopup(description: mainPopupDescription)
        declinePopup(description: declinePrepaidPopupDescription)
        openOrderDetails()
    }

    // MARK: - Private

    private func openOrderDetails() {
        "Открываем детали заказа".ybm_run { _ in
            let orderDetails = popupPage.orderButton.tap()
            ybm_wait(forFulfillmentOf: { !self.popupPage.descriptionLabel.isVisible })
            ybm_wait(forVisibilityOf: [orderDetails.status])
        }
    }

    private func chooseSimilarItem() {
        "Выберем похожий товар".ybm_run { _ in
            ybm_wait(forVisibilityOf: [popupPage.lastButton])
            popupPage.lastButton.tap()
        }

        "Ждем открытия похожих товаров".ybm_run { _ in
            let similarsFeedPage = SimilarsFeedPage.current
            ybm_wait(forVisibilityOf: [
                similarsFeedPage.navigationLabel,
                similarsFeedPage.textLabel,
                similarsFeedPage.bottomButton
            ])

            XCTAssertEqual(
                similarsFeedPage.textLabel.label,
                "Товар закончился, но есть\nпохожие — выбирайте"
            )

            XCTAssertEqual(
                similarsFeedPage.bottomButton.label,
                "Перейти в корзину"
            )

            XCTAssertTrue(similarsFeedPage.navigationLabel.label.contains("Похожие товары"))
        }
    }
}
