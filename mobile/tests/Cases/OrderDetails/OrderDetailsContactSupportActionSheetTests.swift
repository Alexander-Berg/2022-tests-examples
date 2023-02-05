import MarketUITestMocks
import XCTest

final class OrderDetailsContactSupportActionSheetTests: LocalMockTestCase {

    // MARK: - Public

    func test_TapContactSupportButtonWithAllSupportChannelsAvailable_PhoneButtonAndChatButtonExists() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-4168")
        testContactSupportPage(
            isChatAvailable: true,
            isPhoneAvailable: true
        )
    }

    func test_TapContactSupportButtonWithOnlyChatChannelAvailable_ChatButtonExists() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-4169")
        testContactSupportPage(
            isChatAvailable: true,
            isPhoneAvailable: false
        )
    }

    // MARK: - Private

    private func testContactSupportPage(isChatAvailable: Bool, isPhoneAvailable: Bool) {
        Allure.addEpic("Скрытие саппорта")
        Allure.addTitle("Чат \(isChatAvailable ? "" : "не")доступен, звонок \(isPhoneAvailable ? "" : "не")доступен")

        enable(toggles: FeatureNames.orderSupportChat)
        app.launchEnvironment[TestLaunchEnvironmentKeys.enabledTogglesInfo] = toggleInfo

        var orderDetailsPage: OrderDetailsPage!
        let orderId = "4815230"

        configureMocks(
            isChatAvailable: isChatAvailable,
            isPhoneAvailable: isPhoneAvailable,
            orderId: orderId
        )

        "Переходим в детали заказа".ybm_run { _ in
            orderDetailsPage = goToOrderDetailsPage(orderId: orderId)
        }

        if isPhoneAvailable && isChatAvailable {
            checkActionSheet(in: orderDetailsPage)
        } else if isChatAvailable {
            checkSupportChatButton(in: orderDetailsPage)
        }
    }

    private func checkActionSheet(in orderDetailsPage: OrderDetailsPage) {
        var contactSupportPage: ContactSupportPage!

        "Листаем вниз до кнопки связи с поддержкой".ybm_run { _ in
            orderDetailsPage.element.swipe(
                to: .down,
                untilVisible: orderDetailsPage.contactSupportButton.element
            )
        }

        "Жмем кнопку 'Связаться с поддержкой'".ybm_run { _ in
            contactSupportPage = orderDetailsPage.contactSupportButton.tap()
        }

        "Ждем открытия попапа и проверяем доступные кнопки".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                contactSupportPage.element.isVisible
            })
            XCTAssertTrue(contactSupportPage.callButton.element.isVisible)
            XCTAssertTrue(contactSupportPage.chatButton.element.isVisible)
        }
    }

    private func checkSupportChatButton(in orderDetailsPage: OrderDetailsPage) {
        "Листаем вниз до кнопки чата с поддержкой".ybm_run { _ in
            orderDetailsPage.element.swipe(
                to: .down,
                untilVisible: orderDetailsPage.supportChatButton
            )
        }

        "Проверяем наличие кнопки".ybm_run { _ in
            wait(forVisibilityOf: orderDetailsPage.supportChatButton)
            XCTAssertEqual(orderDetailsPage.supportChatButton.label, "Чат с поддержкой")
        }
    }

    private var toggleInfo: String {
        let orderSupportChatInfo = [
            FeatureNames.orderSupportChat.lowercased(): ["enableAnonyms": true]
        ]
        let info = (try? JSONSerialization.data(
            withJSONObject: orderSupportChatInfo,
            options: .prettyPrinted
        )).flatMap { String(data: $0, encoding: .utf8) }
        return info ?? ""
    }

    private func configureMocks(
        isChatAvailable: Bool,
        isPhoneAvailable: Bool,
        orderId: String
    ) {
        var bundleCounter = 0

        if !isChatAvailable {
            let oldBundleName = makeBundleName(withIndex: bundleCounter)
            bundleCounter += 1
            let newBundleName = makeBundleName(withIndex: bundleCounter)

            mockStateManager?.changeMock(
                bundleName: oldBundleName,
                newBundleName: newBundleName,
                filename: "POST_api_v1_resolveAvailableSupportChannelsInfo",
                changes: [
                    (
                        #""isChatAvailable" : true"#,
                        #""isChatAvailable" : false"#
                    )
                ]
            )
        }

        if !isPhoneAvailable {
            let oldBundleName = makeBundleName(withIndex: bundleCounter)
            bundleCounter += 1
            let newBundleName = makeBundleName(withIndex: bundleCounter)

            mockStateManager?.changeMock(
                bundleName: oldBundleName,
                newBundleName: newBundleName,
                filename: "POST_api_v1_resolveAvailableSupportChannelsInfo",
                changes: [
                    (
                        #""isPhoneAvailable" : true"#,
                        #""isPhoneAvailable" : false"#
                    )
                ]
            )
        }

        "Мокаем состояния".ybm_run { _ in
            let defaultBundleName = makeBundleName()
            mockStateManager?.pushState(bundleName: defaultBundleName)

            let modifiedBundleName = makeBundleName(withIndex: bundleCounter)
            mockStateManager?.pushState(bundleName: modifiedBundleName)

            setupState(orderId: orderId)
        }
    }

    private func makeBundleName(withIndex index: Int? = nil) -> String {
        let bundleName = "OrderDetails_ContactSupport_PhoneAndChatAvailable"
        guard let index = index, index != 0 else { return bundleName }

        return "\(bundleName)\(index)"
    }

    typealias OrdersHandlerMapper = OrdersState.UserOrdersHandlerMapper
    typealias SimpleOrder = Order.Mapper
    typealias Buyer = ResolveRecentUserOrders.RecentUserOrders.Buyer
    typealias OrderEditRequest = ResolveRecentUserOrders.RecentUserOrders.OrderEditRequest
    typealias EditRequstStatus = OrderEditRequest.Status
    typealias Payment = Order.Payment
    typealias Promos = Order.OrderPromo

    func setupState(orderId: String) {
        let orderMapper = OrdersHandlerMapper(
            orders: [
                SimpleOrder(
                    id: orderId,
                    status: .delivery
                )
            ]
        )

        var orderState = OrdersState()
        orderState.setOrdersResolvers(mapper: orderMapper, for: [.all, .byId])

        stateManager?.setState(newState: orderState)
    }
}
