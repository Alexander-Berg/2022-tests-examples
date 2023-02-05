import MarketUITestMocks
import XCTest

final class OrderCashbackTestCase: LocalMockTestCase {

    override var user: UserAuthState {
        .loginWithYandexPlus
    }

    func testCashbackWillBeAccrued() {
        let data = TestData(
            allure: TestData.Allure(
                link: "https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3610",
                epic: "Мои заказы",
                feature: "Кешбэк",
                title: "Заказ до доставки. Инфо и бейдж в блоке \"Итого\""
            ),
            prepare: TestData.Prepare(orderId: "7314191"),
            result: TestData.Result(amount: 30, title: "Вернётся на Плюс", status: .inited)
        )

        performTest(data: data)
    }

    func testCashbackAccrued() {
        let data = TestData(
            allure: TestData.Allure(
                link: "https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3660",
                epic: "Мои заказы",
                feature: "Кешбэк",
                title: "Заказ после доставки. Инфо и бейдж в блоке \"Итого\""
            ),
            prepare: TestData.Prepare(orderId: "7383270"),
            result: TestData.Result(amount: 10, title: "Начислено на Плюс", status: .cleared)
        )

        performTest(data: data)
    }

    private func performTest(data: TestData) {
        Allure.addTestPalmLink(data.allure.link)
        Allure.addEpic(data.allure.epic)
        Allure.addFeature(data.allure.feature)
        Allure.addTitle(data.allure.title)

        var orderDetailsPage: OrderDetailsPage!

        let orderMapper = OrdersHandlerMapper(orders: [
            SimpleOrder(
                id: data.prepare.orderId,
                status: .processing,
                cashbackEmitInfo: .init(
                    totalAmount: data.result.amount,
                    status: data.result.status
                )
            )
        ])
        var orderState = OrdersState()
        orderState.setOrdersResolvers(mapper: orderMapper, for: [.all, .byId])

        var authState = UserAuthState()
        authState.setPlusBalanceState(.noMarketCashback)

        stateManager?.setState(newState: authState)
        stateManager?.setState(newState: orderState)

        "Переходим в детали заказа".ybm_run { _ in
            orderDetailsPage = goToOrderDetailsPage(orderId: data.prepare.orderId)
        }

        "Проверяем наличие кешбэка".ybm_run { _ in
            orderDetailsPage.element
                .ybm_swipeCollectionView(toFullyReveal: orderDetailsPage.cashbackDetailedTitle.element)
            XCTAssertEqual(
                orderDetailsPage.cashbackDetailedTitle.amount.label.trimmingCharacters(in: .whitespacesAndNewlines),
                "\(data.result.amount)"
            )
            XCTAssertEqual(
                orderDetailsPage.cashbackDetailedTitle.title.label.trimmingCharacters(in: .whitespacesAndNewlines),
                data.result.title
            )
        }
    }
}

// MARK: - Nested types

extension OrderCashbackTestCase {

    private struct TestData {
        struct Allure {
            let link: String
            let epic: String
            let feature: String
            let title: String
        }

        struct Prepare {
            let orderId: String
        }

        struct Result {
            let amount: Int
            let title: String
            let status: CashbackStatus
        }

        let allure: Allure
        let prepare: Prepare
        let result: Result
    }

    typealias OrdersHandlerMapper = OrdersState.UserOrdersHandlerMapper
    typealias SimpleOrder = Order.Mapper
    typealias CashbackStatus = Order.Mapper.SimpleCashback.Status
    typealias PlusBalance = UserAuthState.UserPlusBalanceState
}
