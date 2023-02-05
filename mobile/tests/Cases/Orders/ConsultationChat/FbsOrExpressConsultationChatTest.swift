import MarketUITestMocks
import XCTest

final class FbsOrExpressConsultationChatTest: LocalMockTestCase {

    var root: RootPage!

    let chatId = "1234567"
    let orderId = "8901234"

    override var user: UserAuthState {
        .loginWithYandexPlus
    }

    typealias AvailiabilityHandlerMapper = ResolveOrderOptionAvailiability.Mapper
    typealias Availiability = AvailiabilityHandlerMapper.OrderAvailabilities
    typealias OrderHandlerMapper = OrdersState.UserOrdersHandlerMapper
    typealias Delivery = Order.Mapper.SimpleDelivery
    typealias SimpleOrder = Order.Mapper

    func test_FbsOrExpressConsultationChat_FromOrders_NoChat() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5796")
        Allure.addTitle("Нет кнопки 'Чат с продавцом' у FBS/экспресс заказа, если чат не был инициирован продавцом")

        var ordersPage: OrdersListPage!

        let toDateFormatted = formatDate(-(.day * 2))

        let mapper = OrderHandlerMapper(orders: [
            SimpleOrder(
                id: orderId,
                status: .processing,
                substatus: .started,
                rgb: "WHITE",
                delivery: Delivery(
                    deliveryPartnerType: .yandex,
                    toDate: toDateFormatted,
                    type: .service,
                    features: [.express]
                )
            )
        ])

        var ordersState = OrdersState()
        ordersState.setOrdersResolvers(mapper: mapper, for: [.all, .byId])

        "Мокаем состояние".ybm_run { _ in
            stateManager?.setState(newState: ordersState)
        }

        "Переходим на список заказов".ybm_run { _ in
            ordersPage = goToOrdersListPage()
        }

        "Убеждаемся, что кнопки `Чат с продавцом` нет".ybm_run { _ in
            XCTAssertFalse(ordersPage.consultationFbsOrExpressButton(orderId: orderId).exists)
        }
    }

    func test_FbsOrExpressConsultationChat_FromOrders_ChatExists() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5795")
        Allure.addTitle("Есть кнопка 'Чат с продавцом' у FBS/экспресс заказа, если чат был инициирован продавцом")

        var ordersPage: OrdersListPage!

        let toDateFormatted = formatDate(-(.day * 2))

        let mapper = OrderHandlerMapper(orders: [
            SimpleOrder(
                id: orderId,
                status: .processing,
                substatus: .started,
                rgb: "WHITE",
                delivery: Delivery(
                    deliveryPartnerType: .yandex,
                    toDate: toDateFormatted,
                    type: .service,
                    features: [.express]
                )
            )
        ])

        var ordersState = setupChat()
        ordersState.setOrdersResolvers(mapper: mapper, for: [.all, .byId])

        "Мокаем состояние".ybm_run { _ in
            stateManager?.setState(newState: ordersState)
        }

        "Переходим на список заказов".ybm_run { _ in
            ordersPage = goToOrdersListPage()
        }

        "Убеждаемся, что кнопка `Чат с продавцом` есть".ybm_run { _ in
            XCTAssertTrue(ordersPage.consultationFbsOrExpressButton(orderId: orderId).exists)
        }
    }

    // MARK: - Private

    private func formatDate(_ timeInterval: TimeInterval) -> String {
        let toDate = Date().addingTimeInterval(timeInterval)
        return getDateString(withDateFormat: "dd-MM-yyyy", date: toDate)
    }

    private func getDateString(withDateFormat dateFormat: String, date: Date) -> String {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = dateFormat
        dateFormatter.locale = Locale(identifier: "ru")
        return dateFormatter.string(from: date)
    }

    private func setupChat() -> OrdersState {
        app.launchEnvironment[TestLaunchEnvironmentKeys.enabledToggles] = FeatureNames.orderConsultation

        var orderState = OrdersState()

        let chatHandler = OrderConsultation(
            orderId: Int(orderId) ?? 0,
            chatId: chatId,
            consultationStatus: "DIRECT_CONVERSATION",
            conversationStatus: "WAITING_FOR_CLIENT"
        )
        orderState.setConsultationChats(mapper: chatHandler, for: [.create, .get])
        orderState.setAvailableSupportChannelsInfo(availableSupportChannelsInfo: .makeFbsOrExpressAvaliableInfo(
            with: chatId
        ))
        return orderState
    }
}
