import AutoMate
import MarketUITestMocks
import XCTest

final class OrderDetailsSupplierInfoTest: LocalMockTestCase {

    func testMyOrdersSelfEmployedSupplierInfo() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4803")
        Allure.addEpic("Срок хранения")
        Allure.addFeature("Продление срока хранения")
        Allure.addTitle("Ошибка продления срока хранения, переход с деталей заказа. Пустой ответ")

        "Настраиваем стейт".ybm_run { _ in
            setupOrderDetails()
        }

        var orderDetailsPage: OrderDetailsPage!
        var merchantPopupPage: MerchantPopupPage!

        "Переходим в детали заказа".ybm_run { _ in
            orderDetailsPage = goToOrderDetailsPage(orderId: Constants.orderId)
        }

        "Скроллим к кнопке мерчанта".ybm_run { _ in
            wait(forVisibilityOf: orderDetailsPage.collectionView)
            orderDetailsPage.element.ybm_swipeCollectionView(toFullyReveal: orderDetailsPage.merchantButton.element)
            wait(forVisibilityOf: orderDetailsPage.merchantButton.element)
        }

        "Тапаем по кнопке".ybm_run { _ in
            merchantPopupPage = orderDetailsPage.merchantButton.tap()
        }

        "Проверяем тексты".ybm_run { _ in
            let inn = merchantPopupPage.inn().caption.label
            let name = merchantPopupPage.fullName().caption.label
            XCTAssertEqual(inn, SupplierInfo.selfEmployed.legalInfo.inn)
            XCTAssertEqual(name, SupplierInfo.selfEmployed.makeLegalInfoFullName())
        }

    }

    // MARK: - Nested

    typealias OrdersHandlerMapper = OrdersState.UserOrdersHandlerMapper
    typealias SimpleOrder = Order.Mapper

    private func setupOrderDetails() {
        var orderState = OrdersState()
        let order = SimpleOrder(
            id: Constants.orderId,
            status: .delivered,
            msku: [Constants.mskuId]
        )
        let orderMapper = OrdersHandlerMapper(
            orders: [order],
            supplierId: [
                Constants.mskuId: SupplierInfo.selfEmployed.id
            ]
        )
        orderState.setOrdersResolvers(mapper: orderMapper, for: [.all, .byId])
        orderState.setSupplierInfoById(suppliers: [.selfEmployed])
        stateManager?.setState(newState: orderState)
    }

    private enum Constants {
        static let mskuId = "123456789zxcvbn"
        static let orderId = "12345"
    }

}
