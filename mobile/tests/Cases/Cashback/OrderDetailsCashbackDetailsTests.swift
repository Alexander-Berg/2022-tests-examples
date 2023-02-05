import MarketUITestMocks
import XCTest

final class OrderDetailsCashbackDetailsTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginWithYandexPlus
    }

    func testCashbackDetails() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5619")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5621")
        Allure.addEpic("Детализация кешбэка")
        Allure.addFeature("Детали заказа")
        Allure.addTitle("Мультизаказ с акцией Mastercard")

        enable(toggles: FeatureNames.cashbackDetailsButton)

        var orderDetailsPage: OrderDetailsPage!
        var popupPage: CashbackDetailsAboutPage!

        "Мокаем состояние".ybm_run { _ in
            setupOrderDetails()
        }

        "Открываем детали заказа".ybm_run { _ in
            orderDetailsPage = goToOrderDetailsPage(orderId: "4815230")
            wait(forVisibilityOf: orderDetailsPage.element)
        }

        "Проверяем наличие кешбэка".ybm_run { _ in
            orderDetailsPage.element
                .ybm_swipeCollectionView(toFullyReveal: orderDetailsPage.cashbackDetailedTitle.element)
            XCTAssertEqual(
                orderDetailsPage.cashbackDetailedTitle.title.label.trimmingCharacters(in: .whitespacesAndNewlines),
                "Вернётся на Плюс"
            )
            XCTAssertEqual(
                orderDetailsPage.cashbackDetailedTitle.amount.label.trimmingCharacters(in: .whitespacesAndNewlines),
                "1 050"
            )
            orderDetailsPage.cashbackDetailedTitle.element.tap()
        }

        let cashbackItems = [
            ("Стандартный кешбэк", " 100"),
            ("Повышенный кешбэк", " 300"),
            ("По акции Mastercard", " 150"),
            ("За первую покупку", " 500")
        ]

        let groupTitles = ["Придёт с товаром", "Придёт после доставки последнего заказа"]

        "Проверяем попап".ybm_run { _ in
            popupPage = CashbackDetailsAboutPage.current
            wait(forVisibilityOf: popupPage.element)
            XCTAssertEqual(popupPage.title.label, "Вернётся на Плюс  1050")

            for (index, cashbackItem) in cashbackItems.enumerated() {
                let detailsItem = popupPage.detailsItem(at: index)
                XCTAssertEqual(detailsItem.title.label, cashbackItem.0)
                XCTAssertEqual(detailsItem.value.label, cashbackItem.1)
            }

            for (index, title) in groupTitles.enumerated() {
                XCTAssertEqual(popupPage.groupTitle(at: index).label, title)
            }

            XCTAssertEqual(
                popupPage.groupDescription(at: 0).label,
                "Кешбэк за акции начислится после доставки заказов №123699-702"
            )

            XCTAssertEqual(popupPage.linkButton.label, "Подробнее об акциях")
            XCTAssertEqual(popupPage.closeButton.label, "Понятно")
            popupPage.linkButton.tap()
        }

        "Проверяем открытие вебвью".ybm_run { _ in
            wait(forVisibilityOf: WebViewPage.current.element)
        }
    }

}

private extension OrderDetailsCashbackDetailsTests {

    private func setupOrderDetails() {
        let orderId = 4_815_230
        let cashbackDetails = modify(CashbackOptionsDetails.orderDetailsCashback) {
            $0.id = orderId
            $0.amount = 1_050
        }
        let orderMapper = OrdersState.UserOrdersHandlerMapper(orders: [
            Order.Mapper(
                id: "\(orderId)",
                status: .processing,
                cashbackEmitInfo: .init(
                    totalAmount: 550,
                    status: .inited
                ),
                cashbackDetails: cashbackDetails
            )
        ])
        var orderState = OrdersState()
        orderState.setOrdersResolvers(mapper: orderMapper, for: [.all, .byId])

        stateManager?.setState(newState: orderState)
    }

}
