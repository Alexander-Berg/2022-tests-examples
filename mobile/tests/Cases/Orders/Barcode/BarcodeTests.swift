import MarketUITestMocks
import XCTest

final class BarcodeTests: DSBSQuestionnaireTestCase {

    // MARK: - Public

    var popupPage: BarcodePage!
    var snippetPage: HoveringSnippetPage!

    var orderId = "4815230"

    typealias OrderHandlerMapper = OrdersState.UserOrdersHandlerMapper
    typealias BarcodeInfo = Order.Delivery.SimpleBarcodeInfo
    typealias Delivery = Order.Mapper.SimpleDelivery
    typealias SimpleOrder = Order.Mapper
    typealias Outlet = Order.SimpleOutlet
    typealias LockCode = ResolveOrderLockCode.BoxBotPincode

    override var user: UserAuthState {
        .loginWithYandexPlus
    }

    func test_Barcode_FromMorda_Open() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4712")
        Allure.addEpic("Barcode")
        Allure.addFeature("Получение посылки в ПВЗ по штрихкоду")
        Allure.addTitle("Получение посылки через штрихкод на морде")

        enable(toggles: FeatureNames.barcodeButtonFeature)

        var mordaPage: MordaPage!
        let ordersState = setupState(resolvers: [.recent(withGrades: true)], deliveryType: .outlet)

        "Мокаем состояние".ybm_run { _ in
            stateManager?.setState(newState: ordersState)
        }

        "Переходим на морду".ybm_run { _ in
            mordaPage = goToMorda()
        }

        "Проверяем наличие виджета".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                mordaPage.element.isVisible && mordaPage.singleActionContainerWidget.container.element.isVisible
            })
            snippetPage = mordaPage.singleActionContainerWidget.container.orderSnippet()

            XCTAssertEqual(snippetPage.actionButton.element.label, "Штрихкод")
            XCTAssertEqual(snippetPage.additionalActionButton.element.label, "Карта")
        }

        "Нажимаем на «Получить по штрихкоду»".ybm_run { _ in
            snippetPage.actionButton.element.tap()
        }

        checkBarcodePopup()
    }

    func test_Barcode_FromOrders_Open() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4712")
        Allure.addEpic("Barcode")
        Allure.addFeature("Получение посылки в ПВЗ по штрихкоду")
        Allure.addTitle("Получение посылки через штрихкод в списке заказов")

        enable(toggles: FeatureNames.barcodeButtonFeature)

        var ordersPage: OrdersListPage!
        let ordersState = setupState(resolvers: [.all], deliveryType: .outlet)

        "Мокаем состояние".ybm_run { _ in
            stateManager?.setState(newState: ordersState)
        }

        "Переходим на список заказов".ybm_run { _ in
            ordersPage = goToOrdersListPage()
        }

        "Нажимаем на «Получить по штрихкоду»".ybm_run { _ in
            XCTAssertEqual(
                ordersPage.showBarcodeButton(orderId: orderId).buttons.firstMatch.label,
                "Получить по штрихкоду"
            )
            ordersPage.showBarcodeButton(orderId: orderId).tap()
        }

        checkBarcodePopup()
    }

    func test_Barcode_FromOrderDetails_Open() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4712")
        Allure.addEpic("Barcode")
        Allure.addFeature("Получение посылки в ПВЗ по штрихкоду")
        Allure.addTitle("Получение посылки через штрихкод с морды")

        enable(toggles: FeatureNames.barcodeButtonFeature)

        var orderDetails: OrderDetailsPage!
        let ordersState = setupState(resolvers: [.all, .byId], deliveryType: .outlet)

        "Мокаем состояние".ybm_run { _ in
            stateManager?.setState(newState: ordersState)
        }

        "Переходим на детали заказа".ybm_run { _ in
            orderDetails = goToOrderDetailsPage(orderId: orderId)
        }

        "Нажимаем на «Получить по штрихкоду»".ybm_run { _ in
            XCTAssertEqual(orderDetails.showBarcodeButton.buttons.firstMatch.label, "Получить по штрихкоду")
            orderDetails.showBarcodeButton.tap()
        }

        checkBarcodePopup()
    }

    func testBarcodeOrdersListPostamate() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4716")
        Allure.addEpic("Lockcode")
        Allure.addFeature("Получение посылки в постамате по лок-коду")
        Allure.addTitle("Получение посылки в постамате по лок-коду из списка заказов")

        enable(toggles: FeatureNames.barcodeButtonFeature)

        var ordersPage: OrdersListPage!
        var ordersState = setupState(
            resolvers: [.all],
            deliveryType: .outlet,
            outlet: .postamat
        )

        "Мокаем состояние".ybm_run { _ in
            ordersState.setOrderLockCode(lockCode: .makeDefaultCode(with: orderId))
            ordersState.setAvailabilityOrderOption(mapper: .makePostamateForOrders(ids: [orderId]))
            stateManager?.setState(newState: ordersState)
        }

        "Переходим на список заказов".ybm_run { _ in
            ordersPage = goToOrdersListPage()
        }

        "Проверям лок-код".ybm_run { _ in
            wait(forVisibilityOf: ordersPage.element)

            XCTAssertEqual(ordersPage.lockCode.label, "Код получения — 123456")
        }
    }

    func testBarcodeOrderDetailsPostamate() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4717")
        Allure.addEpic("Lockcode")
        Allure.addFeature("Получение посылки в постамате по лок-коду")
        Allure.addTitle("Получение посылки в постамате по лок-коду из деталей заказа")

        enable(toggles: FeatureNames.barcodeButtonFeature)

        var orderDetails: OrderDetailsPage!
        var ordersState = setupState(
            resolvers: [.all, .byId],
            deliveryType: .outlet,
            outlet: .postamat
        )

        "Мокаем состояние".ybm_run { _ in
            ordersState.setOrderLockCode(lockCode: .makeDefaultCode(with: orderId))
            ordersState.setAvailabilityOrderOption(mapper: .makePostamateForOrders(ids: [orderId]))
            stateManager?.setState(newState: ordersState)
        }

        "Переходим на детали заказа".ybm_run { _ in
            orderDetails = goToOrderDetailsPage(orderId: orderId)
        }

        "Проверям лок-код".ybm_run { _ in
            wait(forVisibilityOf: orderDetails.element)

            XCTAssertEqual(orderDetails.lockCode.label, "Код получения — 123456")
        }
    }
}

// MARK: - Private

private extension BarcodeTests {

    func checkBarcodePopup() {
        "Проверяем наличие попапа".ybm_run { _ in
            wait(forVisibilityOf: BarcodePage.current.element)
            popupPage = BarcodePage.current
        }

        "Проверяем попап".ybm_run { _ in
            XCTAssertTrue(popupPage.orderId.isVisible)
            XCTAssertEqual(popupPage.orderId.staticTexts.element(boundBy: 0).label, "Номер заказа")
            XCTAssertEqual(popupPage.orderId.staticTexts.element(boundBy: 1).label, "481﻿﻿523﻿﻿0")
            XCTAssertTrue(popupPage.barcodeImage.isVisible)
            XCTAssertTrue(popupPage.code.isVisible)
            XCTAssertEqual(popupPage.code.staticTexts.element(boundBy: 0).label, "Код получения")
            XCTAssertEqual(popupPage.code.staticTexts.element(boundBy: 1).label, "Cod﻿﻿e")
            XCTAssertTrue(popupPage.closeButton.isVisible)
            XCTAssertEqual(popupPage.closeButton.buttons.firstMatch.label, "Готово")
        }

        "Закрываем попап".ybm_run { _ in
            popupPage.closeButton.tap()
        }

        "Проверяем закрытие попапа".ybm_run { _ in
            wait(forInvisibilityOf: popupPage.element)
        }
    }

    func formatDate(_ timeInterval: TimeInterval) -> String {
        let toDate = Date().addingTimeInterval(timeInterval)
        return getDateString(withDateFormat: "dd-MM-yyyy", date: toDate)
    }

    func setupState(
        resolvers: [OrdersState.OrderResolvers],
        deliveryType: Delivery.DeliveryType,
        outlet: Outlet? = nil
    ) -> OrdersState {
        var ordersState = OrdersState()
        let toDateFormatted = formatDate(.day * 2)
        let barcodeInfo = BarcodeInfo(code: "Code", barcodeData: "Barcode")
        let mapper = OrderHandlerMapper(orders: [
            SimpleOrder(
                id: orderId,
                status: .pickup,
                substatus: .received,
                outlet: outlet,
                delivery: Delivery(
                    toDate: toDateFormatted,
                    type: deliveryType,
                    verificationCode: barcodeInfo
                )
            )
        ])
        ordersState.setOrdersResolvers(mapper: mapper, for: resolvers)

        return ordersState
    }
}
