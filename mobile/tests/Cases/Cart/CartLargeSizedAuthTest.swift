import MarketUITestMocks
import XCTest

class CartLargeSizedAuthTest: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testFreeDelivery() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-2818")
        Allure.addEpic("Корзина")
        Allure.addFeature("КГТ")
        Allure.addTitle("Тяжелая корзина + монетка на бесплатную доставку")

        var cart: CartPage!
        var root: RootPage!
        var allCoinsInCart: [CartPage.Coins.Item] = []

        var cartState = CartState()

        "Мокаем ручки".ybm_run { _ in
            cartState.setCoinsForCart(coins: [.testCoin])
            cartState.setUserOrdersState(with: .largeSized)
            stateManager?.setState(newState: cartState)
        }

        "Авторизуемся, переходим в корзину".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            cart = goToCart(root: root)
        }

        "Проверяем наличие нотификации КГТ".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                cart.element.isVisible && cart.threshold.element.isVisible
            })
            XCTAssertEqual(
                cart.threshold.deliveryText.label,
                "Ваш заказ крупногабаритный, поэтому доставка будет дороже, чем обычно\(String.ble_nonBreakingSpace)"
            )
        }

        "Проверяем количество купонов".ybm_run { _ in
            cart.element.ybm_swipeCollectionView(toFullyReveal: cart.coins.coinsItem(at: 0).element)
            allCoinsInCart = cart.allCoins
            XCTAssertEqual(allCoinsInCart.count, 1)
        }

        let mockData: [(el: CartPage.Coins.Item, title: String, subtitle: String, endDate: String)] = [
            (allCoinsInCart[0], "Скидка 1000 ₽", "на любой заказ", "7.10")
        ]

        "Проверяем содержание купонов и того, что среди них нет free delivery".ybm_run { _ in
            for coin in mockData {
                coin.el.element.tap()
                XCTAssertTrue(coin.el.element.isVisible)
                XCTAssertEqual(coin.el.title.label, coin.title)
                XCTAssertEqual(coin.el.subtitle.label, coin.subtitle)
                XCTAssertEqual(coin.el.endDate.label, coin.endDate)
            }
        }
    }
}
