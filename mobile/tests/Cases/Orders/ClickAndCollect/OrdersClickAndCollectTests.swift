import MarketUITestMocks
import XCTest

final class OrdersClickAndCollectTests: LocalMockTestCase {

    typealias OrdersHandlerMapper = OrdersState.UserOrdersHandlerMapper
    typealias OutletHandlerMapper = ResolveOutlets.Mapper

    typealias Outlet = ResolveOutlets.Mapper.SimpleOutlet
    typealias Delivery = Order.Mapper.SimpleDelivery
    typealias SimpleOrder = Order.Mapper

    func testAlcoDetails() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-2778")
        Allure.addEpic("Мои заказы")
        Allure.addFeature("Подробности")
        Allure.addTitle("Алкоголь")

        var orderDetailsPage: OrderDetailsPage!
        var merchantPopupPage: MerchantPopupPage!
        let orderId = "5820757"

        "Мокаем состояния".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "OrdersSet_Alco")
        }

        "Переходим в детали заказа".ybm_run { _ in
            orderDetailsPage = goToOrderDetailsPage(orderId: orderId)
        }

        "Проверяем, что отображается в подробностях заказа".ybm_run { _ in
            XCTAssertEqual(orderDetailsPage.creationDate.title.label, "Дата оформления")
            XCTAssertEqual(orderDetailsPage.creationDate.value.label, "15 апреля, среда")
            XCTAssertEqual(orderDetailsPage.paymentType.title.label, "Способ оплаты")
            XCTAssertEqual(orderDetailsPage.paymentType.value.label, "Картой при получении")

            orderDetailsPage.element.ybm_swipeCollectionView(toFullyReveal: orderDetailsPage.deliveryDate.element)
            XCTAssertEqual(orderDetailsPage.deliveryDate.title.label, "Дата доставки")
            XCTAssertEqual(orderDetailsPage.deliveryDate.value.label, "17–18 апреля")

            orderDetailsPage.element.ybm_swipeCollectionView(toFullyReveal: orderDetailsPage.storagePeriod.element)
            XCTAssertEqual(orderDetailsPage.storagePeriod.title.label, "Срок хранения")
            XCTAssertEqual(orderDetailsPage.storagePeriod.value.label, "5 дней")

            orderDetailsPage.element.ybm_swipeCollectionView(toFullyReveal: orderDetailsPage.deliveryAddress.element)
            XCTAssertEqual(orderDetailsPage.deliveryAddress.title.label, "Выкуп в торговом зале")
            XCTAssertEqual(orderDetailsPage.deliveryAddress.value.label, "Москва, Льва Толстого, д. 3")

            orderDetailsPage.element.ybm_swipeCollectionView(toFullyReveal: orderDetailsPage.seller.element)
            XCTAssertEqual(orderDetailsPage.seller.title.label, "Продавец")
            XCTAssertEqual(
                orderDetailsPage.seller.value.label,
                "OOO «DropShop Online 25», юр.адрес: 127081, г. Москва, улица Чермянская, дом 3, строение 2, помещение 3, ОГРН 1025700765067. Лицензия №11ЛИЦ3434901 от 01 мая 2019 г."
            )

            orderDetailsPage.element.ybm_swipeCollectionView(toFullyReveal: orderDetailsPage.recipient.element)
            XCTAssertEqual(orderDetailsPage.recipient.title.label, "Получатель")
            XCTAssertEqual(orderDetailsPage.recipient.value.label, "Mobile Test, testmobile4@yandex.ru, +75555555555")

            orderDetailsPage.element.ybm_swipeCollectionView(toFullyReveal: orderDetailsPage.shopOrderId.element)
            XCTAssertEqual(
                orderDetailsPage.shopOrderId.title.label,
                "При получении заказа в выбранной точке продажи назовите этот номер"
            )
            XCTAssertEqual(orderDetailsPage.shopOrderId.value.label, "88813617")
        }

        "Открываем попап мерчанта".ybm_run { _ in
            orderDetailsPage.element.ybm_swipeCollectionView(toFullyReveal: orderDetailsPage.merchantButton.element)
            merchantPopupPage = orderDetailsPage.merchantButton.tap()
        }

        "Проверяем, что отображается в попапе мерчанта".ybm_run { _ in
            wait(forExistanceOf: merchantPopupPage.element)
            XCTAssertEqual(merchantPopupPage.fullName().header.label, "Полное название")
            XCTAssertEqual(merchantPopupPage.fullName().caption.label, "OOO «DropShop Online 25»")
            XCTAssertEqual(merchantPopupPage.ogrn().header.label, "ОГРН")
            XCTAssertEqual(merchantPopupPage.ogrn().caption.label, "1025700765067")
            XCTAssertEqual(merchantPopupPage.licenseNumber().header.label, "№ лицензии")
            XCTAssertEqual(
                merchantPopupPage.licenseNumber().caption.label,
                "11ЛИЦ3434901 от 01 мая 2019 г."
            )
            XCTAssertEqual(merchantPopupPage.actualAddress().header.label, "Фактический адрес")
            XCTAssertEqual(merchantPopupPage.actualAddress().caption.label, "Льва Толстого, 3")
            XCTAssertEqual(merchantPopupPage.juridicalAddress().header.label, "Юридический адрес")
            XCTAssertEqual(
                merchantPopupPage.juridicalAddress().caption.label,
                "127081, г. Москва, улица Чермянская, дом 3, строение 2, помещение 3"
            )
            XCTAssertEqual(merchantPopupPage.schedule().header.label, "Часы работы")
            XCTAssertEqual(merchantPopupPage.schedule().caption.label, "Пн-Вс: 10:00-22:00")
        }
    }

    func testStorageLimitDate() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3380")
        Allure.addEpic("Мои заказы")
        Allure.addFeature("Подробности")
        Allure.addTitle("Алкоголь. Срок хранения до даты")

        var orderDetailsPage: OrderDetailsPage!
        let orderId = "4815230"

        let orderMapper = OrdersHandlerMapper(orders: [
            SimpleOrder(
                id: orderId,
                status: .processing,
                substatus: .started,
                delivery: Delivery(
                    outletStorageLimitDate: "2020-04-30",
                    type: .service
                )
            )
        ])

        var orderState = OrdersState()
        orderState.setOrdersResolvers(mapper: orderMapper, for: [.all, .byId])

        "Мокаем состояния".ybm_run { _ in
            stateManager?.setState(newState: orderState)
        }

        "Переходим в список заказов".ybm_run { _ in
            orderDetailsPage = goToOrderDetailsPage(orderId: orderId)
        }

        "Проверяем, что отображается в подробностях заказа".ybm_run { _ in
            orderDetailsPage.element.ybm_swipeCollectionView(toFullyReveal: orderDetailsPage.storagePeriod.element)
            XCTAssertEqual(orderDetailsPage.storagePeriod.title.label, "Срок хранения")
            XCTAssertEqual(orderDetailsPage.storagePeriod.value.label, "до 30 апреля")
        }
    }

}
