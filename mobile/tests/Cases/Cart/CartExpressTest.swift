import MarketUITestMocks
import XCTest

final class CartExpressTest: LocalMockTestCase {

    // TODO: актуализировать кейсы и тесты https://st.yandex-team.ru/BLUEMARKETAPPS-31201

    // MARK: - Properties

    private var cartPage: CartPage!

    private let dateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "ru")
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter
    }()

    // MARK: - Tests

    func testExpressDeliveryThresholdNotVisible() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4391")
        Allure.addEpic("Корзина")
        Allure.addFeature("Экспресс. Трешхолды")
        Allure.addTitle("Трешхолд не отображается, если в корзине несколько товаров в разных посылках")

        var cartState = CartState()

        "Настраиваем стейт".ybm_run { _ in
            cartState.setCartStrategy(with: makeCartStrategy())

            stateManager?.setState(newState: cartState)
        }

        "Открываем корзину".ybm_run { _ in
            let root = appAfterOnboardingAndPopups()
            cartPage = goToCart(root: root)
        }

        "Проверяем, что трешхолд не отображается".ybm_run { _ in
            XCTAssertFalse(cartPage.threshold.element.isVisible)
        }
    }

    func testThatExpressProductHasTodayDeliveryHeader() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4254")
        Allure.addEpic("Корзина")
        Allure.addFeature("Экспресс. Хедер над экспресс товаром")
        Allure.addTitle(
            "Экспресс-товар в корзине должен иметь хедер: Экспресс-доставка Яндекса"
        )

        var cartState = CartState()

        "Настраиваем стейт".ybm_run { _ in
            var offer = FAPIOffer.protein
            offer.delivery = .express
            let strategy = ResolveUserCartWithStrategiesAndBusinessGroups.VisibleStrategiesFromUserCart(
                offers: [offer],
                groupType: .express
            )
            cartState.setCartStrategy(with: strategy)

            let todayDate = dateFormatter.string(from: Date())
            cartState.setUserOrdersState(with: makeUserOrderOptions(withDeliveryDate: todayDate))

            stateManager?.setState(newState: cartState)
        }

        "Открываем корзину".ybm_run { _ in
            let root = appAfterOnboardingAndPopups()
            cartPage = goToCart(root: root)
        }

        "Проверяем заголовок экспресс-товара".ybm_run { _ in
            let header = cartPage.businessGroupHeader(at: 0).text
            header.tap()
            XCTAssertEqual(header.label, "Экспресс-доставка Яндекса")
        }
    }

    func testThatExpressProductHasTomorrowDeliveryHeader() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4254")
        Allure.addEpic("Корзина")
        Allure.addFeature("Экспресс. Хедер над экспресс товаром")
        Allure.addTitle(
            "Экспресс-товар в корзине должен иметь хедер: Экспресс-доставка Яндекса завтра с времяНачала до времяКонца"
        )

        var cartState = CartState()

        "Настраиваем стейт".ybm_run { _ in
            var offer = FAPIOffer.protein
            offer.delivery = .express
            let strategy = ResolveUserCartWithStrategiesAndBusinessGroups.VisibleStrategiesFromUserCart(
                offers: [offer],
                groupType: .express
            )
            cartState.setCartStrategy(with: strategy)

            let tomorrowDate = dateFormatter.string(from: Date().addingTimeInterval(.day))
            cartState.setUserOrdersState(with: makeUserOrderOptions(withDeliveryDate: tomorrowDate))

            stateManager?.setState(newState: cartState)
        }

        "Открываем корзину".ybm_run { _ in
            let root = appAfterOnboardingAndPopups()
            cartPage = goToCart(root: root)
        }

        "Проверяем заголовок экспресс-товара".ybm_run { _ in
            let header = cartPage.businessGroupHeader(at: 0).text
            header.tap()
            XCTAssertEqual(header.label, "Экспресс-доставка Яндекса")
        }
    }

    // MARK: - Helper Methods

    private func makeCartStrategy() -> ResolveUserCartWithStrategiesAndBusinessGroups.VisibleStrategiesFromUserCart {
        let firstOffer = modify(FAPIOffer.protein) {
            $0.cartItemInCartId = Constants.firstItemId
            $0.shopId = String(Constants.firstItemShopId)
            $0.wareId = Constants.firstWareId
            $0.delivery = .express
        }
        let secondOffer = modify(FAPIOffer.protein) {
            $0.cartItemInCartId = Constants.secondItemId
            $0.shopId = String(Constants.secondItemShopId)
            $0.wareId = Constants.secondWareId
            $0.delivery = .express
        }

        var strategies = ResolveUserCartWithStrategiesAndBusinessGroups
            .VisibleStrategiesFromUserCart(offers: [firstOffer, secondOffer])

        let firstCartStrategy = ResolveUserCartWithStrategiesAndBusinessGroups.CombineStrategy.CartStrategy(
            offers: [firstOffer],
            shopId: Constants.firstItemShopId
        )
        let secondCartStrategy = ResolveUserCartWithStrategiesAndBusinessGroups.CombineStrategy.CartStrategy(
            offers: [secondOffer],
            shopId: Constants.secondItemShopId
        )
        strategies.combineStrategy = [
            ResolveUserCartWithStrategiesAndBusinessGroups
                .CombineStrategy(carts: [firstCartStrategy, secondCartStrategy])
        ]

        return strategies
    }

    private func makeUserOrderOptions(
        withDeliveryDate deliveryDate: String
    ) -> ResolveUserOrderOptions.UserOrderOptions {
        var cartItem = CartItem.express
        cartItem.items = [.basic]

        return ResolveUserOrderOptions.UserOrderOptions(
            region: .moscow,
            summary: .basic,
            shops: [cartItem]
        )
    }
}

// MARK: - Nested Types

private extension CartExpressTest {

    enum Constants {
        static let firstItemId = 111
        static let secondItemId = 222

        static let firstItemShopId = 555
        static let secondItemShopId = 666

        static let firstWareId = "wareId_1"
        static let secondWareId = "wareId_2"
    }

}
