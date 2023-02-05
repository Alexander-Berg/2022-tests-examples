import MarketUITestMocks
import XCTest

final class OrderCancellationRejectedTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    typealias PopupPage = AgitationPopupPage

    var root: RootPage!
    var profile: ProfilePage!
    var orders: OrdersListPage!
    var orderDetails: OrderDetailsPage!
    var cancelOrder: CancelOrderPage!
    var popupPage: PopupPage!

    override func setUp() {
        super.setUp()
        disable(toggles: FeatureNames.cancelDsbsOrder)
        enable(toggles: FeatureNames.orderConsultation)
    }

    func test_CancellationRejected_OrderDelivered_OpenChat() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4465")
        Allure.addEpic("Запрос на отмену продавцу на этапе PROCESSING/DELIVERY/PICKUP в DSBS")
        Allure.addFeature("Продавец не подтвердил отмену, заказ доставлен + чат")

        performOrderCancellation(bundleName: "Agitations_OrderCancellationRejected", orderId: "32675272")
        openPopup(config: .delivered)
        openChat()
    }

    func test_CancellationRejected_OrderDelivered_ClosePopup() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4467")
        Allure.addEpic("Запрос на отмену продавцу на этапе PROCESSING/DELIVERY/PICKUP в DSBS")
        Allure.addFeature("Продавец не подтвердил отмену, заказ доставлен + понятно")

        performOrderCancellation(bundleName: "Agitations_OrderCancellationRejected", orderId: "32675272")
        openPopup(config: .delivered)
        closePopup()
    }

    func test_CancellationRejected_OrderInDelivery_OpenChat() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4464")
        Allure.addEpic("Запрос на отмену продавцу на этапе PROCESSING/DELIVERY/PICKUP в DSBS")
        Allure.addFeature("Продавец не подтвердил отмену, заказ в доставке + чат")

        performOrderCancellation(bundleName: "Agitations_OrderCancellationRejected", orderId: "32675272")
        openPopup(config: .inDelivery)
        openChat()
    }

    func test_CancellationRejected_OrderInDelivery_ClosePopup() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4468")
        Allure.addEpic("Запрос на отмену продавцу на этапе PROCESSING/DELIVERY/PICKUP в DSBS")
        Allure.addFeature("Продавец не подтвердил отмену, заказ в доставке + понятно")

        performOrderCancellation(bundleName: "Agitations_OrderCancellationRejected", orderId: "32675272")
        openPopup(config: .inDelivery)
        closePopup()
    }

    // MARK: - Private Methods

    private func performOrderCancellation(bundleName: String, orderId: String) {

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

        "Отменяем заказ".run {
            cancelOrder.confirmButton.tap()
            wait(forVisibilityOf: cancelOrder.finishButton)
            cancelOrder.finishButton.tap()
            wait(forVisibilityOf: orders.element)
        }
    }

    private func openPopup(config: Config) {
        "Мокаем данные".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Agitations_OrderCancellationRejected_Popup")
        }

        "Мокаем ручки".run {
            let orderByIdRule = MockMatchRule(
                id: "ORDER_BY_ID_RULE",
                matchFunction:
                isPOSTRequest &&
                    isFAPIRequest &&
                    hasExactFAPIResolvers(["resolveUserOrderByIdFull"]),
                mockName: config.mockName
            )
            mockServer?.addRule(orderByIdRule)
        }

        "Открываем морду".ybm_run { _ in
            let tabBar = root.tabBar
            let mordaTabItem = tabBar.mordaTabItem

            wait(forVisibilityOf: mordaTabItem.element)
            mordaTabItem.tap()
        }

        "Ждем попап".ybm_run { _ in
            popupPage = PopupPage.currentPopup
            wait(forVisibilityOf: popupPage.descriptionLabel)
            XCTAssertEqual(popupPage.descriptionLabel.label, config.description)
        }
    }

    private func openChat() {
        "Открываем чат".ybm_run { _ in
            popupPage.firstButton.tap()
            ybm_wait(forFulfillmentOf: { !self.popupPage.descriptionLabel.isVisible })
            ybm_wait(forVisibilityOf: [ConsultationChatPage.current.element])
        }
    }

    private func closePopup() {
        "Закрываем попап".ybm_run { _ in
            popupPage.lastButton.tap()
            ybm_wait(forFulfillmentOf: { !self.popupPage.descriptionLabel.isVisible })
        }
    }
}

private extension OrderCancellationRejectedTests {
    enum Config {
        case delivered, inDelivery

        var mockName: String {
            switch self {
            case .delivered:
                return "resolveUserOrderByIdFull_delivered"
            case .inDelivery:
                return "resolveUserOrderByIdFull_in_delivery"
            }
        }

        var description: String {
            switch self {
            case .delivered:
                return "Продавец сообщил, что уже доставил посылку вам. Возврат можно оформить в разделе «Заказы»."
            case .inDelivery:
                return "Продавец уже передал его в доставку. Вы сможете вернуть товары, как только получите посылку."
            }
        }
    }
}
