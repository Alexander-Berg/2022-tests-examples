import MarketUITestMocks
import XCTest

final class DeliveryServiceInformationTest: LocalMockTestCase {

    typealias OrdersHandlerMapper = OrdersState.UserOrdersHandlerMapper
    typealias SimpleOrder = Order.Mapper
    typealias CashbackStatus = Order.Mapper.SimpleCashback.Status

    func setState(orderId: String) {
        let orderMapper = OrdersHandlerMapper(
            orders: [
                SimpleOrder(
                    id: orderId,
                    status: .delivery,
                    rgb: "WHITE",
                    delivery: .init(
                        deliveryPartnerType: .shop,
                        type: .service,
                        trackCode: "YNM14155886",
                        checkpointIds: ["1crs2yp3wkz"]
                    )
                )
            ],
            deliveryCheckpoint: [
                .init(
                    id: "1crs2yp3wkz",
                    deliveryStatusText: "Заказ создан",
                    type: "info",
                    date: "29-05-2020 12:07:12"
                )
            ]
        )

        var orderState = OrdersState()
        orderState.setOrdersResolvers(mapper: orderMapper, for: [.all, .byId])
        orderState.setEditVariants(orderEditVariants: .boxberry(orderId: Int(orderId) ?? 0))

        stateManager?.setState(newState: orderState)
    }

    func testDeliveryServiceInformation() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3091")
        Allure.addEpic("Мои заказы")
        Allure.addFeature("Информация о СД")
        Allure.addTitle("Показываем информацию о СД - другие")

        var orderDetailsPage: OrderDetailsPage!
        let orderId = "4815230"

        "Мокаем состояния".ybm_run { _ in
            setState(orderId: orderId)
        }

        "Переходим в детали заказа".ybm_run { _ in
            orderDetailsPage = goToOrderDetailsPage(orderId: orderId)
        }

        "Проверяем информацию по СД".ybm_run { _ in
            wait(forVisibilityOf: orderDetailsPage.deliveryServiceInfo)
            XCTAssertEqual(
                orderDetailsPage.deliveryServiceInfo.text,
                "Заказ везёт «Boxberry», код вашей посылки — YNM14155886. Уточнить статус заказа или изменить условия доставки можно на сайте и по телефону 8 (800) 222-80-00."
            )
        }

        "Проверяем переход по Ссылке".ybm_run { _ in
            orderDetailsPage.deliveryServiceInfo.links["сайте"].firstMatch.tap()
            wait(forVisibilityOf: WebViewPage.current.element)

            WebViewPage.current.navigationBar.closeButton.tap()
            wait(forVisibilityOf: orderDetailsPage.element)
        }

        "Проверяем номер телефона".ybm_run { _ in
            XCTAssertTrue(orderDetailsPage.deliveryServiceInfo.links["8 (800) 222-80-00"].firstMatch.isVisible)
        }

        "Проверяем копирование в буфер номера посылки".ybm_run { _ in
            orderDetailsPage.deliveryServiceInfo.links["YNM14155886"].firstMatch.tap()

            let popup = DefaultToastPopupPage.currentPopup
            wait(forVisibilityOf: popup.element)

            XCTAssertEqual(popup.text.label, "Номер посылки скопирован в буфер обмена")
        }
    }

}
