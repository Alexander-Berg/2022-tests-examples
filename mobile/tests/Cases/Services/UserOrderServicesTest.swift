import MarketUITestMocks
import XCTest

final class UserOrderServicesTest: ServicesTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testUserOrderServicesStatus() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5224")
        Allure.addEpic("Мои заказы")
        Allure.addFeature("Доп. услуги")
        Allure.addTitle("Статусы услуги в заказах")

        var orderListPage: OrdersListPage!
        var orderItemService: OrderItemService!

        "Мокаем заказ в статусе \"поиск мастера\"".ybm_run { _ in
            orderItemService = .waitingSlot
            setupUserOrders(service: orderItemService)
        }

        "Открываем Мои заказы".ybm_run { _ in
            orderListPage = goToOrdersListPage()
            wait(forExistanceOf: orderListPage.element)
        }

        "Проверяем статус услуги".ybm_run { _ in
            XCTAssertEqual(
                orderListPage.serviceInfo(orderId: orderItemService.orderId).label,
                "​Ищем мастера для установки"
            )
        }

        "Мокаем заказ в статусе \"подтверждён\"".ybm_run { _ in
            orderItemService = .confirmed
            setupUserOrders(service: orderItemService)
            orderListPage.collectionView.swipeDown()
        }

        "Проверяем статус услуги".ybm_run { _ in
            XCTAssertEqual(
                orderListPage.serviceInfo(orderId: orderItemService.orderId).label,
                "​Мастер приедет 29 ноября"
            )
        }

        "Мокаем заказ в статусе \"завершён\"".ybm_run { _ in
            orderItemService = .completed
            setupUserOrders(service: orderItemService)
            orderListPage.collectionView.swipeDown()
        }

        "Проверяем статус услуги".ybm_run { _ in
            XCTAssertEqual(
                orderListPage.serviceInfo(orderId: orderItemService.orderId).label,
                "​Установка выполнена"
            )
        }

        "Мокаем заказ в статусе \"отменён\"".ybm_run { _ in
            orderItemService = .cancelled
            setupUserOrders(service: orderItemService)
            orderListPage.collectionView.swipeDown()
        }

        "Проверяем статус услуги".ybm_run { _ in
            XCTAssertEqual(
                orderListPage.serviceInfo(orderId: orderItemService.orderId).label,
                "​Установка отменена"
            )
        }
    }
}

extension UserOrderServicesTest {
    func setupUserOrders(service: OrderItemService) {
        var ordersState = OrdersState()

        ordersState.setOrdersResolvers(
            mapper: .init(
                orders: [
                    .init(
                        id: service.orderId,
                        status: .processing,
                        servicesTotal: service.price,
                        properties: .withService
                    )
                ],
                orderItemService: [service]
            ),
            for: [.all, .byId]
        )

        stateManager?.setState(newState: ordersState)
    }
}
