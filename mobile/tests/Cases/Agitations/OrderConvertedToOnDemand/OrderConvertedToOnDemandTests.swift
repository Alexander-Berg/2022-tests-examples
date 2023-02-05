import MarketUITestMocks
import XCTest

final class OrderConvertedToOnDemandTests: AgitationsUnauthorizedTestCase {

    private var orderState = OrdersState()

    func test_OrderConvertedToOnDemand_DeliveryLate_NotReadyForLastMile() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4691")
        Allure.addEpic("Откат часовых слотов в ондеманд")
        Allure.addTitle("Не успели привезти")

        checkPopup(with: .deliveryLate_NotReadyForLastMile)
        closePopup()
    }

    func test_OrderConvertedToOnDemand_DeliveryLate_ReadyForLastMile_ReceiveOrder() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4693")
        Allure.addEpic("Откат часовых слотов в ондеманд")
        Allure.addTitle("Не успели привезти + позже")

        checkPopup(with: .deliveryLate_ReadyForLastMile)
        closePopup()
    }

    func test_OrderConvertedToOnDemand_ClientDidNotReceive_NotReadyForLastMile() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4698")
        Allure.addEpic("Откат часовых слотов в ондеманд")
        Allure.addTitle("Клиент не получил заказ")

        checkPopup(with: .clientDidNotReceive_NotReadyForLastMile)
        closePopup()
    }

    func test_OrderConvertedToOnDemand_ClientDidNotReceive_ReadyForLastMile_ReceiveOrder() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4701")
        Allure.addEpic("Откат часовых слотов в ондеманд")
        Allure.addTitle("Клиент не получил заказ + позже")

        checkPopup(with: .clientDidNotReceive_ReadyForLastMile)
        closePopup()
    }
}

// MARK: - Private

private extension OrderConvertedToOnDemandTests {

    typealias PopupPage = AgitationPopupPage
    typealias OrdersHandlerMapper = OrdersState.UserOrdersHandlerMapper
    typealias SimpleOrder = Order.Mapper

    func checkPopup(with config: Config) {
        "Мокаем состояния".ybm_run { _ in
            setupAgitation(with: config)
        }

        "Открываем приложение".ybm_run { _ in
            _ = appAfterOnboardingAndPopups()
        }

        "Ждем попап".ybm_run { _ in
            popupPage = PopupPage.currentPopup
            wait(forVisibilityOf: popupPage.descriptionLabel)
        }

        "Мокаем пустую агитацию".ybm_run { _ in
            setupEmptyAgitation()
        }
    }

    private func setupAgitation(with config: Config) {
        let order = SimpleOrder(
            id: Constants.orderId,
            status: config.readyForLastMile ? .delivery : .readyForLastMile,
            delivery: .init(
                deliveryPartnerType: .yandex,
                fromDate: "05-08-2020",
                toDate: "05-08-2020",
                fromTime: "12:00",
                toTime: "18:00",
                type: .service
            )
        )

        orderState.setOrderAgitations(agitations: [config.agitation])
        orderState.setOrdersResolvers(mapper: OrdersHandlerMapper(orders: [order]), for: [.byId])

        stateManager?.setState(newState: orderState)
    }

    private func setupEmptyAgitation() {
        orderState.setOrderAgitations(agitations: [])
        stateManager?.setState(newState: orderState)
    }
}

// MARK: - Nested Types

private extension OrderConvertedToOnDemandTests {
    enum Constants {
        static let orderId = "4815230"

        static let popupDescriptionDeliveryLate =
            "Доставим по клику в удобный момент. Скоро у заказа появится кнопка «Вызвать курьера». Нажмёте, и он приедет в течение 15-30 минут."
        static let popupDescriptionClientDidNotReceive =
            "Вы не получили его, но это легко исправить. Вызовите курьера в удобный момент до 1 мая, и он приедет в течение 15-30 минут."

    }

    struct Config {
        let isByUser: Bool
        let readyForLastMile: Bool
        let popupDescription: String

        var agitation: ResolveOrderAgitations.Agitation {
            isByUser
                ? .orderConvertedToOnDemand(for: Constants.orderId)
                : .orderConvertedToOnDemandByUser(for: Constants.orderId)
        }

        static var deliveryLate_NotReadyForLastMile: Self {
            .init(
                isByUser: false,
                readyForLastMile: false,
                popupDescription: Constants.popupDescriptionDeliveryLate
            )
        }

        static var deliveryLate_ReadyForLastMile: Self {
            .init(
                isByUser: false,
                readyForLastMile: true,
                popupDescription: Constants.popupDescriptionDeliveryLate
            )
        }

        static var clientDidNotReceive_NotReadyForLastMile: Self {
            .init(
                isByUser: false,
                readyForLastMile: false,
                popupDescription: Constants.popupDescriptionClientDidNotReceive
            )
        }

        static var clientDidNotReceive_ReadyForLastMile: Self {
            .init(
                isByUser: false,
                readyForLastMile: true,
                popupDescription: Constants.popupDescriptionClientDidNotReceive
            )
        }
    }
}
