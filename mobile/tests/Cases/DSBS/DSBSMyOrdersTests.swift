import XCTest

class DSBSMyOrdersTests: LocalMockTestCase {

    func testMyOrders() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3753")
        Allure.addEpic("DSBS")
        Allure.addFeature("Мои заказы")

        var root: RootPage!
        var myOrders: OrdersListPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "DSBSMyOrders")
            addMockMatchRuleForUserOrder(id: "1", orderId: "7371836")
            addMockMatchRuleForUserOrder(id: "1", orderId: "7375986")
        }

        "Открываем заказы и нажимаем 'Показать на карте'".ybm_run { _ in
            root = appAfterOnboardingAndPopups()

            myOrders = goToOrdersListPage(root: root)
            wait(forVisibilityOf: myOrders.element)

            let order = myOrders.showOnMapButton(orderId: "7375586")
            myOrders.element.ybm_swipeCollectionView(toFullyReveal: order.element)

            let map = order.tap()
            wait(forVisibilityOf: map.element)
        }

        "Открываем подробности заказа".ybm_run { _ in
            NavigationBarPage.current.backButton.tap()
            wait(forVisibilityOf: myOrders.element)

            let order = myOrders.detailsButton(orderId: "7371836")
            myOrders.element.ybm_swipeCollectionView(toFullyReveal: order.element)

            let details = order.tap()
            wait(forVisibilityOf: details.element)

            swipeAndCheck(page: details.collectionView, element: details.status) { XCTAssertTrue($0.exists) }
            swipeAndCheck(page: details.collectionView, element: details.creationDate.element) {
                XCTAssertTrue($0.exists)
            }
            swipeAndCheck(page: details.collectionView, element: details.deliveryAddress.element) {
                XCTAssertTrue($0.exists)
            }
            swipeAndCheck(page: details.collectionView, element: details.recipient.element) { XCTAssertTrue($0.exists) }
            swipeAndCheck(page: details.collectionView, element: details.buyer.element) { XCTAssertTrue($0.exists) }
            swipeAndCheck(page: details.collectionView, element: details.merchantButton.element) {
                XCTAssertTrue($0.exists)
            }
            swipeAndCheck(page: details.collectionView, element: details.cancellationButton.element) {
                XCTAssertTrue($0.exists)
            }
        }

        "Просмотр документов и возврат заказа".ybm_run { _ in
            NavigationBarPage.current.backButton.tap()
            wait(forVisibilityOf: myOrders.element)

            let order = myOrders.detailsButton(orderId: "7375986")
            myOrders.element.ybm_swipeCollectionView(toFullyReveal: order.element)

            let details = order.tap()
            details.collectionView.ybm_swipeCollectionView(toFullyReveal: details.receiptsButton.element)

            let receiptsButton = details.receiptsButton.tap()
            wait(forVisibilityOf: receiptsButton)

            XCTAssertTrue(receiptsButton.exists)

            NavigationBarPage.current.backButton.tap()
            wait(forVisibilityOf: details.element)

            XCTAssertTrue(details.refundButton.exists)

            details.refundButton.tap()

            let yandexLogin = YandexLoginPage(element: app.webViews.firstMatch)
            wait(forVisibilityOf: yandexLogin.element)

            closeSWCAlert(yandexLogin.element)

            XCTAssertTrue(yandexLogin.element.exists)
        }
    }

    private func addMockMatchRuleForUserOrder(
        id: String,
        orderId: String
    ) {
        let rule = MockMatchRule(
            id: id,
            matchFunction:
            isPOSTRequest &&
                isFAPIRequest &&
                hasExactFAPIResolvers(["resolveUserOrderByIdFull"]) &&
                hasStringInBody("\"orderId\":\(orderId)"),
            mockName: "resolveUserOrderByIdFull_\(orderId)"
        )

        mockServer?.addRule(rule)
    }
}
