import MarketUITestMocks
import XCTest

final class OrderUnitInformationTest: LocalMockTestCase {

    override var user: UserAuthState {
        .loginWithYandexPlus
    }

    // MARK: - Public

    func test_OrderUnitInfo() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5689")
        Allure.addEpic("Мои заказы")
        Allure.addFeature("Единицы измерений")
        Allure.addTitle("Проверка отображения цена за основную ед. измерений (уп) и количества уп")

        let orderId = "4815230"
        let orderItemIdWithUnit = "1"
        let orderItemIdDefault = "2"
        let unitName = "уп"

        var ordersPage: OrdersListPage!
        var orderDetailsPage: OrderDetailsPage!

        let mapper = OrdersState.UserOrdersHandlerMapper(
            orders: [
                Order.Mapper(
                    id: orderId,
                    status: .processing,
                    msku: [orderItemIdWithUnit, orderItemIdDefault]
                )
            ],
            orderItemUnitInfo: [orderItemIdWithUnit: unitName]
        )

        var ordersState = OrdersState()
        ordersState.setOrdersResolvers(mapper: mapper, for: [.all, .byId])

        "Мокаем состояние".run {
            stateManager?.setState(newState: ordersState)
        }

        "Переходим в мои заказы".run {
            ordersPage = goToOrdersListPage()
            wait(forVisibilityOf: ordersPage.element)
        }

        "Проверяем цену и количество у товара с единицами".run {
            XCTAssertEqual(ordersPage.orderItemPriceLabel(itemId: orderItemIdWithUnit).label, "81 990 ₽/уп")
            XCTAssertEqual(ordersPage.orderItemCountLabel(itemId: orderItemIdWithUnit).label, "1 уп.")
        }

        "Проверяем цену и количество у товара без единиц".run {
            XCTAssertEqual(ordersPage.orderItemPriceLabel(itemId: orderItemIdDefault).label, "81 990 ₽")
            XCTAssertEqual(ordersPage.orderItemCountLabel(itemId: orderItemIdDefault).label, "1 шт.")
        }

        "Переходим в детали заказа".run {
            let orderDetailsButton = ordersPage.detailsButton(orderId: orderId)
            ordersPage.element.ybm_swipeCollectionView(toFullyReveal: orderDetailsButton.element)

            orderDetailsPage = orderDetailsButton.tap()
            wait(forVisibilityOf: orderDetailsPage.element)
        }

        "Проверяем цену и количество в деталях заказа у товара с единицами".run {
            orderDetailsPage.element.swipe(
                to: .down,
                untilVisible: orderDetailsPage.orderItemPriceLabel(itemId: orderItemIdWithUnit)
            )
            XCTAssertEqual(orderDetailsPage.orderItemPriceLabel(itemId: orderItemIdWithUnit).label, "81 990 ₽/уп")
            XCTAssertEqual(orderDetailsPage.orderItemCountLabel(itemId: orderItemIdWithUnit).label, "1 уп.")
        }

        "Проверяем цену и количество в деталях заказа у товара без единиц".run {
            orderDetailsPage.element.swipe(
                to: .down,
                untilVisible: orderDetailsPage.orderItemPriceLabel(itemId: orderItemIdWithUnit)
            )
            XCTAssertEqual(orderDetailsPage.orderItemPriceLabel(itemId: orderItemIdDefault).label, "81 990 ₽")
            XCTAssertEqual(orderDetailsPage.orderItemCountLabel(itemId: orderItemIdDefault).label, "1 шт.")
        }
    }
}
