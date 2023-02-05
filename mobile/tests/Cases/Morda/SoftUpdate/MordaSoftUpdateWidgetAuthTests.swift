import MarketUITestMocks
import XCTest

final class MordaSoftUpdateWidgetAuthTests: LocalMockTestCase {

    typealias HandlerMapper = OrdersState.UserOrdersHandlerMapper
    typealias Delivery = Order.Mapper.SimpleDelivery
    typealias SimpleOrder = Order.Mapper

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testSoftUpdateVisibleForLoggedInUserWithoutOrders() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3292")
        Allure.addEpic("Морда")
        Allure.addFeature("Виджет SoftUpdate")
        Allure.addTitle("Виджет отображается для залогина без заказов")

        var morda: MordaPage!

        var orderState = OrdersState()
        orderState.setOrdersResolvers(mapper: .empty, for: [.recent(withGrades: true)])

        "Включаем feature toggle softUpdate".ybm_run { _ in
            enable(toggles: FeatureNames.softUpdateWidget)
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "MordaSet_SoftUpdateOutdatedVersion")
            stateManager?.setState(newState: orderState)
        }

        "Авторизуемся, открываем морду".ybm_run { _ in
            morda = goToMorda()
        }

        "Проверяем наличие виджета".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { [weak self] in
                self?.mockServer?.handledRequests.contains { $0.contains("lookup") } == true
            })

            wait(forVisibilityOf: morda.singleActionContainerWidget.container.softUpdateWidget.element)
        }
    }

    func testSoftUpdateAbsentForLoggedInUserWithOrders() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3293")
        Allure.addEpic("Морда")
        Allure.addFeature("Виджет SoftUpdate")
        Allure.addTitle("Виджет не отображается для залогина с заказами")

        var morda: MordaPage!

        let mapper = HandlerMapper(orders: [
            SimpleOrder(status: .processing),
            SimpleOrder(status: .processing)
        ])

        "Включаем feature toggle softUpdate".ybm_run { _ in
            enable(toggles: FeatureNames.softUpdateWidget)
        }

        var orderState = OrdersState()
        orderState.setOrdersResolvers(mapper: mapper, for: [.recent(withGrades: true)])

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "MordaSet_SoftUpdateOutdatedVersion")
            stateManager?.setState(newState: orderState)
        }

        "Авторизуемся, открываем морду".ybm_run { _ in
            morda = goToMorda()
            ybm_wait(forVisibilityOf: [morda.singleActionContainerWidget.element])
        }

        "Проверяем отсутствие виджета".ybm_run { _ in
            XCTAssertFalse(morda.singleActionContainerWidget.container.softUpdateWidget.element.isVisible)
        }
    }
}
