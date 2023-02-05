import XCTest

final class OrdersShowOnMapTests: LocalMockTestCase {

    // MARK: - Public

    func test_OpenOrderDetailsWithViewOnMapAvailable_ShowOnYandexMapsExists() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4672")
        Allure.addEpic("ПВЗ")
        Allure.addFeature("Как добраться до ПВЗ")
        Allure.addTitle("Кнопка на карте в деталях")

        var ordersPage: OrdersListPage!
        var outletMapInfoPage: OutletMapInfoPage!
        let orderId = "32784413"

        "Мокаем состояния".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Orders_PickupViewOnMap_MapPermalink")
        }

        "Переходим на список заказов".ybm_run { _ in
            ordersPage = goToOrdersListPage()
        }

        "Жмем элемент с адресом 'Показать на карте'".ybm_run { _ in
            outletMapInfoPage = ordersPage.showOnMapButton(orderId: orderId).tap()
        }

        "Ждем открытия попапа и проверяем что кнопка доступна".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                outletMapInfoPage.showOnYandexMapsButton.isEnabled
            })
        }
    }

}
