import MarketUITestMocks
import XCTest

final class CartUnitsTest: LocalMockTestCase {

    // MARK: - Public

    func testShowUnitInfoOldCart() {
        disable(toggles: FeatureNames.cartRedesign)
        testShowUnitInfoCart()
    }

    func testShowUnitInfoRedesignCart() {
        enable(toggles: FeatureNames.cartRedesign)
        testShowUnitInfoCart()
    }

    func testUnitCalculator() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5868")
        Allure.addEpic("Корзина")
        Allure.addFeature("Калькулятор единиц")
        Allure.addTitle("Проверяем отображение калькулятора единиц измерения")

        enable(toggles: FeatureNames.cartRedesign)
        var cartState = CartState()
        var cartPage: CartPage!

        "Настраиваем стейт".run {
            cartState.setCartStrategy(with: [Constants.fapiOffer])
            stateManager?.setState(newState: cartState)
        }

        "Открываем корзину".run {
            cartPage = goToCart()
            ybm_wait(forFulfillmentOf: { cartPage.element.isVisible })
        }

        "Проверяем каунтер и калькулятор".run {
            let firstItem = cartPage.cartItem(with: 0)
            cartPage.element.ybm_swipeCollectionView(toFullyReveal: firstItem.cartButtonRedesign.element)
            XCTAssertEqual(cartPage.cartItem(at: 0).cartButtonRedesign.element.label, "1 уп")
            XCTAssertEqual(cartPage.cartItem(at: 0).units.label, "1 уп = 0,50 шт")
        }

        "Мокаем увеличение товара в корзине".run {
            let cartItemInCart = CartItemInCart.makeFromOffer(Constants.fapiOffer, count: 2)

            cartState.changeCartItemsState(
                with: CartState.ChangeCartItemsBody(
                    cartItems: [cartItemInCart],
                    threshold: []
                )
            )

            var offer = Constants.fapiOffer
            offer.availableCount = 2

            let cartStrategy = ResolveUserCartWithStrategiesAndBusinessGroups.VisibleStrategiesFromUserCart(
                offers: [offer],
                useAvailableCount: true
            )

            cartState.setCartStrategy(with: cartStrategy)
            cartState.setUserOrdersState(with: makeUserOrderOptions(firstItemCount: 2))
            stateManager?.setState(newState: cartState)
        }

        "Нажимаем + на товаре".run {
            cartPage.cartItem(with: 0).cartButtonRedesign.plusButton.tap()
        }

        "Ждем, пока стейт обновится и в таббаре появится 2 товара".run {
            ybm_wait(forFulfillmentOf: { TabBarPage.current.cartTabItem.element.label == "Корзина2" })
        }

        "Проверяем каунтер и калькулятор".run {
            let firstItem = cartPage.cartItem(with: 0)
            XCTAssertEqual(firstItem.cartButtonRedesign.element.label, "2 уп")
            XCTAssertEqual(firstItem.units.label, "2 уп = 1,00 шт")
        }
    }
}

// MARK: - Nested Types

private extension CartUnitsTest {
    enum Constants {
        static let firstItemId = 111

        static let fapiOffer = modify(FAPIOffer.proteinWitUnit) {
            $0.cartItemInCartId = firstItemId
        }
    }
}

// MARK: - Private

private extension CartUnitsTest {
    private func testShowUnitInfoCart() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5573")
        Allure.addEpic("Корзина")
        Allure.addFeature("Единицы")
        Allure.addTitle("Проверяем отображение единиц на кнопке количества в корзине")

        var cartPage: CartPage!
        var cartState = CartState()

        "Настраиваем стейт".run {
            cartState.setThresholdInfoState(with: .init(
                info: .no_free_delivery,
                forReason: .regionWithoutThreshold
            ))
            cartState.setCartStrategy(with: [
                FAPIOffer.proteinWitUnit,
                FAPIOffer.default
            ])
            cartState.setUserOrdersState(with: .basic)

            stateManager?.setState(newState: cartState)
        }

        "Открываем корзину".run {
            cartPage = goToCart()
            ybm_wait(forFulfillmentOf: { cartPage.element.isVisible })
        }

        "Проверяем отображение единиц на кнопке количества у первого товара".run {
            let item = cartPage.cartItem(at: 0)
            wait(forVisibilityOf: item.element)
            XCTAssertEqual(item.countInfo.label, "1 уп")
        }

        "Проверяем, что единицы не отображаются кнопке количества у второго товара".run {
            let item = cartPage.cartItem(at: 1)
            cartPage.element.ybm_swipeCollectionView(toFullyReveal: item.element)
            XCTAssertEqual(item.countInfo.label, "1")
        }
    }

    typealias UserOrderOptions = ResolveUserOrderOptions.UserOrderOptions

    private func makeUserOrderOptions(firstItemCount: Int) -> UserOrderOptions {
        var firstItem = Item.basic
        firstItem.label = String(Constants.firstItemId)
        firstItem.count = firstItemCount

        var cartItem = CartItem.basic
        cartItem.items = [firstItem]

        return ResolveUserOrderOptions.UserOrderOptions(
            region: .moscow,
            summary: .basic,
            shops: [cartItem]
        )
    }
}
