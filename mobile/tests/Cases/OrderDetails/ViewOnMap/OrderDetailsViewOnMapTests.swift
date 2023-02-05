import MarketUITestMocks
import XCTest

final class OrderDetailsShowOnMapButtonTests: LocalMockTestCase {

    typealias OrdersHandlerMapper = OrdersState.UserOrdersHandlerMapper
    typealias SimpleOrder = MarketUITestMocks.Order.Mapper
    typealias SimpleOutlet = MarketUITestMocks.Order.SimpleOutlet

    private func setState(orderId: String, withPhotos: Bool = false) {
        let pictures: [MarketUITestMocks.Picture] = withPhotos ? Picture.default : []
        let outlet = SimpleOutlet(
            id: 10_000_977_915,
            name: "outlet",
            yandexMapsOutletUrl: "https://yandex.ru/web-maps/org/98631801263",
            pictures: pictures
        )

        let orderMapper = OrdersHandlerMapper(
            orders: [
                SimpleOrder(
                    id: orderId,
                    status: .pickup,
                    substatus: .pickupReceived,
                    outlet: outlet,
                    delivery: .init(
                        deliveryPartnerType: .shop,
                        type: .outlet
                    )
                )
            ]
        )

        var orderState = OrdersState()
        orderState.setOrdersResolvers(mapper: orderMapper, for: [.all, .byId])
        orderState.setOutlet(outlets: [outlet])

        stateManager?.setState(newState: orderState)
    }

    // MARK: - Public

    func test_OpenOrderDetailsWithViewOnMapAvailable_ShowOnYandexMapsExists() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4672")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5680")
        Allure.addEpic("ПВЗ")
        Allure.addFeature("Как добраться до ПВЗ")
        Allure.addTitle("Кнопка на карте в деталях, нет фотографий как добраться")

        var orderDetailsPage: OrderDetailsPage!
        var outletMapInfoPage: OutletMapInfoPage!
        let orderId = "32784413"

        "Мокаем состояния".ybm_run { _ in
            setState(orderId: orderId)
        }

        "Переходим в детали заказа".ybm_run { _ in
            orderDetailsPage = goToOrderDetailsPage(orderId: orderId)
        }

        "Жмем элемент с адресом 'Показать на карте'".ybm_run { _ in
            outletMapInfoPage = orderDetailsPage.showOnMapButton.tap()
        }

        "Ждем открытия попапа и проверяем что кнопка доступна".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                outletMapInfoPage.showOnYandexMapsButton.isEnabled
            })
        }

        "Проверяем что фото 'Как добраться' не отображаются".ybm_run { _ in
            outletMapInfoPage.element.swipeDown()
            XCTAssertFalse(outletMapInfoPage.picturesContainer.element.isVisible)
        }
    }

    func test_OpenOrderDetailsWithViewOnMapAvailable_OutletPicturesExists() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5558")
        Allure.addEpic("ПВЗ")
        Allure.addFeature("Как добраться до ПВЗ")
        Allure.addTitle("Фотографии как добраться на экране об аутлете")

        enable(toggles: FeatureNames.outletPhotos)

        var orderDetailsPage: OrderDetailsPage!
        var outletMapInfoPage: OutletMapInfoPage!
        let orderId = "32784413"

        "Мокаем состояния".ybm_run { _ in
            setState(orderId: orderId, withPhotos: true)
        }

        "Переходим в детали заказа".ybm_run { _ in
            orderDetailsPage = goToOrderDetailsPage(orderId: orderId)
        }

        "Жмем элемент с адресом 'Показать на карте'".ybm_run { _ in
            outletMapInfoPage = orderDetailsPage.showOnMapButton.tap()
        }

        "Ждем открытия попапа, скроллим до фотографий".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                outletMapInfoPage.showOnYandexMapsButton.isEnabled
            })
            outletMapInfoPage.element.swipe(to: .down, untilVisible: outletMapInfoPage.picturesContainer.element)
        }

        "Жмем на фотку и хотим чтобы она открылась".ybm_run { _ in
            let galleryPage = outletMapInfoPage.picturesContainer.cellPage(at: 0).tap()
            XCTAssertTrue(galleryPage.image.waitForExistence(timeout: XCTestCase.defaultTimeOut))
        }
    }

}
