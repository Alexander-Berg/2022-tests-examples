import MarketUITestMocks
import XCTest

class SamplesOrderTests: ServicesTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testSampleOrder() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6018")
        Allure.addEpic("Samples")
        Allure.addFeature("Мои заказы")
        Allure.addTitle("Скрыта кнопка в корзину для пробников")

        var ordersListPage: OrdersListPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SampleOrder")
        }

        "Идем на мои заказы".ybm_run { _ in
            ordersListPage = goToOrdersListPage()
            wait(forVisibilityOf: ordersListPage.element)
        }

        "Проверяем, что кнопки \"В корзину\" не отображаются".ybm_run { _ in
            XCTAssertFalse(ordersListPage.cartButton(itemId: Constants.sampleItemId).isVisible)
        }
    }

    func testSampleOrderDetails() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6018")
        Allure.addEpic("Samples")
        Allure.addFeature("Мои заказы")
        Allure.addTitle("Подробности заказа")

        var ordersListPage: OrdersListPage!
        var orderDetailsPage: OrderDetailsPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SampleOrder")
        }

        "Идем на мои заказы".ybm_run { _ in
            ordersListPage = goToOrdersListPage()
            wait(forVisibilityOf: ordersListPage.element)
        }

        "Нажимаем на кнопку \"Подробнее\" и переходим на экран деталей заказа".ybm_run { _ in
            let detailsButton = ordersListPage.detailsButton(orderId: Constants.orderId)
            wait(forVisibilityOf: detailsButton.element)
            orderDetailsPage = detailsButton.tap()
            wait(forVisibilityOf: orderDetailsPage.element)
        }

        "Проверяем отсутствие кнопок \"В корзину\" у пробника".ybm_run { _ in
            orderDetailsPage.element.ybm_swipeCollectionView(
                toFullyReveal: orderDetailsPage.orderItemTitleLabel(itemId: Constants.sampleItemId)
            )
            XCTAssertFalse(orderDetailsPage.cartButton(itemId: Constants.sampleItemId).isVisible)
        }
    }
}

// MARK: - Nested Types

private extension SamplesOrderTests {

    enum Constants {
        static let orderId = "33106734"
        static let sampleItemId = "10052478"
    }
}
