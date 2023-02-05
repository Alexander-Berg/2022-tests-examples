import MarketUITestMocks
import XCTest

class DSBSOrderCancellationTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testDSBSOrderCancellation() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3752")
        Allure.addEpic("DSBS Заказы")
        Allure.addFeature("Отмена заказов. DSBS заказ")

        performOrderCancellation(bundleName: "DSBSOrderCancellation", orderId: "32675272")
    }

    func testYandexDeliveryOrderCancellation() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3752")
        Allure.addEpic("DSBS Заказы")
        Allure.addFeature("Отмена заказов. 1P заказ")

        performOrderCancellation(bundleName: "YandexDeliveryOrderCancellation", orderId: "32677352")
    }

    private func performOrderCancellation(bundleName: String, orderId: String) {
        var root: RootPage!
        var profile: ProfilePage!
        var orders: OrdersListPage!
        var orderDetails: OrderDetailsPage!
        var cancelOrder: CancelOrderPage!

        "Мокаем ручки".run {
            mockStateManager?.pushState(bundleName: bundleName)
        }

        "Открываем профиль".run {
            root = appAfterOnboardingAndPopups()
            profile = goToProfile(root: root)
            wait(forVisibilityOf: profile.element)
        }

        "Открываем список заказов".run {
            profile.element.ybm_swipeCollectionView(toFullyReveal: profile.myOrders.element)
            orders = profile.myOrders.tap()
            wait(forVisibilityOf: orders.element)
        }

        "Открываем заказ".run {
            let detailsButton = orders.detailsButton(orderId: orderId)
            orders.element.ybm_swipeCollectionView(toFullyReveal: detailsButton.element)
            orderDetails = detailsButton.tap()
        }

        "Открываем страницу отмены заказа".run {
            let button = orderDetails.cancellationButton
            orderDetails.element.ybm_swipeCollectionView(toFullyReveal: button.element)
            cancelOrder = button.tap()
            wait(forVisibilityOf: cancelOrder.confirmButton)
        }

        "Мокаем ручки".run {
            let orderByIdRule = MockMatchRule(
                id: "ORDER_BY_ID_RULE",
                matchFunction:
                isPOSTRequest &&
                    isFAPIRequest &&
                    hasExactFAPIResolvers(["resolveUserOrderByIdFull"]),
                mockName: "resolveUserOrderByIdFull_after_cancellation"
            )
            mockServer?.addRule(orderByIdRule)
        }

        "Отменяем заказ".run {
            cancelOrder.confirmButton.tap()
            wait(forVisibilityOf: cancelOrder.finishButton)
            cancelOrder.finishButton.tap()
            wait(forVisibilityOf: orders.element)
        }

        "Проверяем детали отмененного заказа".run {
            let status = orders.status(orderId: orderId)
            let firstCell = orders.collectionView.cells.firstMatch
            orders.element.ybm_swipeCollectionView(toFullyReveal: firstCell)
            orders.element.ybm_swipeCollectionView(toFullyReveal: status)
            ybm_wait(forFulfillmentOf: { status.label == "Отменён" })
        }
    }
}
