import MarketUITestMocks
import XCTest

final class CartHeaderTest: LocalMockTestCase {

    func testBaseHeader() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3354")
        Allure.addEpic("Корзина")
        Allure.addFeature("Хедер")
        Allure.addTitle("Непустая корзина, появляется экран логина")

        var orderButton: XCUIElement!
        let navigationBar = NavigationBarPage.current

        "Открываем корзину".ybm_run { _ in
            _ = goToCart()

            ybm_wait(forFulfillmentOf: { navigationBar.element.isVisible })
        }

        "Проверяем, что кнопка “Оформить” в хедере активна".ybm_run { _ in
            orderButton = navigationBar.orderBarButton.element

            ybm_wait(forFulfillmentOf: {
                orderButton.isVisible
                    && orderButton.isEnabled
            })

            XCTAssertEqual(orderButton.label, "Оформить")
        }
    }

    func testEmptyCartHeader() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3355")
        Allure.addEpic("Корзина")
        Allure.addFeature("Хедер")
        Allure.addTitle("Пустая корзина")

        let navigationBar = NavigationBarPage.current
        var cartState = CartState()

        "Мокаем ручки".ybm_run { _ in
            cartState.setCartStrategy(with: [])
            stateManager?.setState(newState: cartState)
        }

        "Открываем корзину".ybm_run { _ in
            _ = goToEmptyCart()

            ybm_wait(forFulfillmentOf: { navigationBar.element.isVisible })
        }

        "Проверяем, что кнопка “Оформить” в хедере неактивна".ybm_run { _ in
            let orderButton = navigationBar.orderBarButton

            ybm_wait(forFulfillmentOf: { orderButton.element.isVisible })

            XCTAssertFalse(orderButton.element.isEnabled)
            XCTAssertEqual(navigationBar.orderBarButton.element.label, "Оформить")
        }
    }
}

final class CartHeaderAuthTest: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testGoToCheckout() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3357")
        Allure.addEpic("Корзина")
        Allure.addFeature("Хедер")
        Allure.addTitle("Переход в чекаут под авторизованным пользователем")

        var checkoutPage: CheckoutPage!
        let navigationBar = NavigationBarPage.current
        var cartState = CartState()
        var orderState = OrdersState()

        "Мокаем ручки".ybm_run { _ in
            cartState.setUserOrdersState(with: .dropship)
            cartState.setCartStrategy(with: [.protein])
            orderState.setOrdersResolvers(
                mapper: .init(orders: [.init(status: .delivered)]),
                for: [.all]
            )

            stateManager?.setState(newState: cartState)
            stateManager?.setState(newState: orderState)
        }

        "Открываем корзину".ybm_run { _ in
            _ = goToCart()

            wait(forVisibilityOf: navigationBar.element)
            ybm_wait(forFulfillmentOf: { navigationBar.orderBarButton.element.isEnabled })
        }

        "Нажимаем на кнопку оформить".ybm_run { _ in
            let orderButton = navigationBar.orderBarButton

            wait(forVisibilityOf: orderButton.element)

            checkoutPage = orderButton.tap()
        }

        "Ждем открытия чекаута".ybm_run { _ in
            wait(forVisibilityOf: checkoutPage.element)
        }
    }
}
