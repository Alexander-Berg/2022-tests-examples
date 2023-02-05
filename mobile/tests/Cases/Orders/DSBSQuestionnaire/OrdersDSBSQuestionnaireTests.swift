import MarketUITestMocks
import XCTest

final class OrdersDSBSQuestionnaireTests: DSBSQuestionnaireTestCase {

    // MARK: - Public

    var ordersListPage: OrdersListPage!
    let orderId = "4815230"

    func test_OpenOrdersDSBSOrder_NotReceived_Continue() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3952")
        Allure.addEpic("DSBS")
        Allure.addFeature("Виджет Заказ у меня")
        Allure.addTitle("Заказ не получен, новой даты нет, нажимаем продолжить")

        setupAndAnswerStillNo()

        notDeliveredNoNewDateFlow_tapContinue()
    }

    func test_OpenOrdersDSBSOrder_NotReceived_CallSupport() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3953")
        Allure.addEpic("DSBS")
        Allure.addFeature("Виджет Заказ у меня")
        Allure.addTitle("Заказ не получен, новой даты нет, звоним в поддержку")

        setupAndAnswerStillNo()

        notDeliveredNoNewDateFlow_consultationChat()
    }

    func test_OpenOrdersDSBSOrder_NotReceived_NewDateFoundOut() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3945")
        Allure.addEpic("DSBS")
        Allure.addFeature("Виджет Заказ у меня")
        Allure.addTitle("Заказ не получен, новая дата есть")

        setupAndAnswerStillNo()

        notDelivered_NewDateFoundOut()
    }

    func test_OpenOrdersDSBSOrder_Received_RateViewAppears() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3947")
        Allure.addEpic("DSBS")
        Allure.addFeature("Виджет Заказ у меня")
        Allure.addTitle("Заказ получен")

        setupOrdersWithDSBSOrderAndQuestionnaireConditions()

        "Переходим в детали заказа и проверяем кнопку 'У меня'".ybm_run { _ in
            wait(forVisibilityOf: ordersListPage.alreadyGotItButton(orderId: orderId))
            XCTAssertEqual(ordersListPage.alreadyGotItButton(orderId: orderId).label, "У меня")
        }

        "Нажимаем кнопку 'У меня'".ybm_run { _ in
            ordersListPage.alreadyGotItButton(orderId: orderId).tap()
        }

        alreadyGotItScenario_feedbackPopupOpens()
    }

    // MARK: - Private

    private func setupAndAnswerStillNo() {
        setupOrdersWithDSBSOrderAndQuestionnaireConditions()

        "Переходим в детали заказа и проверяем кнопку 'Еще нет'".ybm_run { _ in
            wait(forVisibilityOf: ordersListPage.stillNoButton(orderId: orderId))
            XCTAssertEqual(ordersListPage.stillNoButton(orderId: orderId).label, "Еще нет")
        }

        "Нажимаем кнопку 'Еще нет'".ybm_run { _ in
            ordersListPage.stillNoButton(orderId: orderId).tap()
        }
    }

    private func setupOrdersWithDSBSOrderAndQuestionnaireConditions() {
        enable(toggles: FeatureNames.alreadyDeliveredQuestionnaire)

        "Мокаем состояния".ybm_run { _ in
            setState()
        }

        "Переходим на список заказов".ybm_run { _ in
            ordersListPage = goToOrdersListPage()
        }
    }

    private func setState() {
        let toDate = Date().addingTimeInterval(-(.day * 2))
        let endDate = getDateString(withDateFormat: "dd-MM-yyyy", date: toDate)

        let orderMapper = OrdersHandlerMapper(
            orders: [
                SimpleOrder(
                    id: orderId,
                    status: .delivery,
                    substatus: .service,
                    rgb: "WHITE",
                    delivery: .init(
                        deliveryPartnerType: .shop,
                        toDate: endDate,
                        type: .service
                    )
                )
            ]
        )

        var orderState = OrdersState()
        orderState.setOrdersResolvers(mapper: orderMapper, for: [.all])
        orderState.setGradesById(orderGradesById: .notSubmittedFeedback(orderId: Int(orderId) ?? 0))

        stateManager?.setState(newState: orderState)
    }

    typealias OrdersHandlerMapper = OrdersState.UserOrdersHandlerMapper
    typealias SimpleOrder = Order.Mapper
    typealias CashbackStatus = Order.Mapper.SimpleCashback.Status
}
