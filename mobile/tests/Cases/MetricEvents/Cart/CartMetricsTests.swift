import MarketUITestMocks
import Metrics
import XCTest

class CartMetricsTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testViewCartMetrics() throws {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5219")
        Allure.addEpic("КОРЗИНА")
        Allure.addFeature("Метрика")
        Allure.addTitle("Метрика просмотра корзины")

        "Настраиваем стейт".ybm_run { _ in
            setupState()
        }

        "Открываем корзину".ybm_run { _ in
            let cartPage = goToCart()
            wait(forVisibilityOf: cartPage.cartItem(at: 0).element)
        }

        try "Проверяем отправку события просмотра корзины в Firebase".ybm_run { _ in
            let event = try XCTUnwrap(MetricRecorder.events(from: .firebase).with(name: "view_cart").first)

            XCTAssertEqual(try XCTUnwrap(event.parameters["currency"] as? String), "RUB")
            XCTAssertEqual(try XCTUnwrap(event.parameters["value"] as? Int), 35_898)

            let items = try XCTUnwrap(event.parameters["items"] as? [[String: Any]])
            XCTAssertEqual(try XCTUnwrap(items[0]["item_category"] as? String), "14247341")
            XCTAssertEqual(try XCTUnwrap(items[0]["item_id"] as? String), "100963252802")
            XCTAssertEqual(
                try XCTUnwrap(items[0]["item_name"] as? String),
                "Протеин CMTech Whey Protein Клубничный крем, 30 порций"
            )
        }

        try "Проверяем отправку события просмотра корзины в Adjust".ybm_run { _ in
            let event = try XCTUnwrap(MetricRecorder.events(from: .adjust).with(name: "b1h3d8").first)
            XCTAssertEqual(
                try XCTUnwrap(event.parameters["criteo_p"] as? String),
                "%5B%7B%22i%22:%22100963252802%22,%22pr%22:1413.000000,%22q%22:1%7D%5D"
            )

            let customerId = try XCTUnwrap(event.parameters["customer_id"] as? String)
            XCTAssertFalse(customerId.isEmpty)
        }
    }

    func testSelectPromocodeMetrics() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5220")
        Allure.addEpic("КОРЗИНА")
        Allure.addFeature("Метрика")
        Allure.addTitle("Метрика применения промокода")

        var cart: CartPage!

        "Настраиваем стейт".ybm_run { _ in
            setupState()
        }

        "Открываем корзину".ybm_run { _ in
            cart = goToCart()
        }

        "Добавляем промокод".ybm_run { _ in
            let promocode = cart.promocode
            cart.collectionView.ybm_swipeCollectionView(toFullyReveal: promocode.element)
            wait(forVisibilityOf: promocode.input)

            promocode.input.tap()
            promocode.input.typeText("WIN15")
            promocode.applyButton.tap()
        }

        "Проверяем отправку события просмотра корзины в Firebase".ybm_run { _ in
            ybm_wait {
                MetricRecorder.events(from: .firebase)
                    .with(name: "select_promotion")
                    .with(params: ["promotion_name": "WIN15"])
                    .isNotEmpty
            }
        }
    }

    func testSelectPromotionWithCoinsMetrics() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5221")
        Allure.addEpic("КОРЗИНА")
        Allure.addFeature("Метрика")
        Allure.addTitle("Метрика применения купона")

        var cart: CartPage!

        "Настраиваем стейт".ybm_run { _ in
            setupState(coins: true)
        }

        "Авторизуемся, переходим в корзину".ybm_run { _ in
            cart = goToCart()
        }

        "Проверяем наличие купона".ybm_run { _ in
            cart.element.ybm_swipeCollectionView(toFullyReveal: cart.coins.coinsItem(at: 0).element)
            XCTAssertEqual(cart.allCoins.count, 1)
        }

        "Проверяем отправку события просмотра корзины в Firebase".ybm_run { _ in
            ybm_wait {
                MetricRecorder.events(from: .firebase)
                    .with(name: "select_promotion")
                    .with(params: [
                        "promotion_name": "Скидка 1000 ₽",
                        "promotion_id": "1560005"
                    ])
                    .isNotEmpty
            }
        }
    }

    // MARK: - Helper Methods

    private func setupState(coins: Bool = false) {
        var cartState = CartState()

        cartState.setCartStrategy(with: [.protein])
        cartState.setUserOrdersState(with: .basic)
        if coins {
            cartState.setCoinsForCart(coins: [.testCoin])
        }

        stateManager?.setState(newState: cartState)
    }
}
