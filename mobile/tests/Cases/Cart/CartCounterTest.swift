import MarketUITestMocks
import XCTest

final class CartCounterTest: LocalMockTestCase {

    // MARK: - Public

    // swiftlint:disable function_body_length
    func testItemsCounter() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3109")
        Allure.addEpic("Корзина")
        Allure.addFeature("Каунтер")
        Allure.addTitle("Проверяем отображение каунтера товаров в корзине")

        var root: RootPage!
        var cartPage: CartPage!
        var firstItem: CartPage.CartItem!
        var secondItem: CartPage.CartItem!
        var pickerWheel: XCUIElement!

        var cartState = CartState()

        disable(toggles: FeatureNames.cartRedesign)

        "Настраиваем стейт".ybm_run { _ in
            cartState.setThresholdInfoState(with: .init(
                info: .no_free_delivery,
                forReason: .regionWithoutThreshold
            ))
            cartState.setCartStrategy(with: [
                modify(FAPIOffer.protein) { $0.cartItemInCartId = Constants.firstItemId }
            ])
            cartState.setUserOrdersState(with: .basic)

            stateManager?.setState(newState: cartState)
        }

        "Открываем корзину".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            cartPage = goToCart(root: root)
            ybm_wait(forFulfillmentOf: { cartPage.element.isVisible })
        }

        "Открываем пикер количества товара".ybm_run { _ in
            firstItem = cartPage.cartItem(at: 0)
            pickerWheel = cartPage.pickerWheel
            firstItem.countPicker.tap()
            ybm_wait(forFulfillmentOf: { pickerWheel.isVisible })
        }

        "Мокаем товар с примененным 200 шт.".ybm_run { _ in
            cartState.changeCartItemsState(with: makeChangeCartItemsBody(count: 200))
            cartState.setUserOrdersState(with: makeUserOrderOptions(firstItemCount: 200))
            stateManager?.setState(newState: cartState)
        }

        "Выбираем количество 200 шт.".ybm_run { _ in
            self.mockServer?.handledRequests.removeAll() // очищаем хранилище запросов

            XCTAssertEqual(pickerWheel.otherElements.count, 200) // выбор максимально возможного количества товара

            pickerWheel.adjust(toPickerWheelValue: "200")
            cartPage.countPickerDoneButton.tap()
            wait(forInvisibilityOf: pickerWheel)

            // проверяем вызов функции changeCartItems
            ybm_wait(forFulfillmentOf: {
                self.mockServer?.handledRequests.contains { $0.contains("name=changeCartItems") } ?? false
            })
        }

        "Проверяем количество товара".ybm_run { _ in
            cartPage.element.ybm_swipeCollectionView(to: .up, toFullyReveal: firstItem.countInfo)
            XCTAssertEqual(firstItem.countInfo.label, "200")

            let totalItems = cartPage.summary.totalItems
            cartPage.element.ybm_swipeCollectionView(toFullyReveal: totalItems.element)
            XCTAssertEqual(totalItems.title.label, "Товары (200)")

            ybm_wait(forFulfillmentOf: { TabBarPage.current.cartTabItem.element.label == "Корзина99+" })
        }

        "Открываем пикер количества товара".ybm_run { _ in
            firstItem.countPicker.tap()
        }

        "Мокаем товар с примененным 100 шт.".ybm_run { _ in
            cartState.changeCartItemsState(with: makeChangeCartItemsBody(count: 100))
            cartState.setUserOrdersState(with: makeUserOrderOptions(firstItemCount: 100))
            stateManager?.setState(newState: cartState)
        }

        "Выбираем количество 100 шт.".ybm_run { _ in
            self.mockServer?.handledRequests.removeAll() // очищаем хранилище запросов

            var pickerWheel: XCUIElement!
            ybm_wait(forFulfillmentOf: {
                pickerWheel = XCUIApplication().pickerWheels.firstMatch
                return pickerWheel.isVisible
            })
            pickerWheel.adjust(toPickerWheelValue: "100")
            cartPage.countPickerDoneButton.tap()
            wait(forInvisibilityOf: pickerWheel)

            // проверяем вызов функции changeCartItems
            ybm_wait(forFulfillmentOf: {
                self.mockServer?.handledRequests.contains { $0.contains("name=changeCartItems") } ?? false
            })
        }

        "Проверяем количество товара".ybm_run { _ in
            cartPage.element.ybm_swipeCollectionView(to: .up, toFullyReveal: firstItem.countInfo)
            XCTAssertEqual(firstItem.countInfo.label, "100")

            let totalItems = cartPage.summary.totalItems
            cartPage.element.ybm_swipeCollectionView(toFullyReveal: totalItems.element)
            XCTAssertEqual(totalItems.title.label, "Товары (100)")

            ybm_wait(forFulfillmentOf: { TabBarPage.current.cartTabItem.element.label == "Корзина99+" })
        }

        "Мокаем добавление второго товара".ybm_run { _ in
            cartState.setCartStrategy(with: [
                modify(FAPIOffer.protein) { $0.cartItemInCartId = Constants.firstItemId },
                modify(FAPIOffer.protein) { $0.cartItemInCartId = Constants.secondItemId }
            ])
            cartState.setUserOrdersState(with: makeUserOrderOptions(firstItemCount: 100, secondItemCount: 1))

            stateManager?.setState(newState: cartState)
        }

        "Перезаходим в корзину".ybm_run { _ in
            _ = goToProfile(root: root)
            cartPage = goToCart(root: root)
            firstItem = cartPage.cartItem(at: 0)
            secondItem = cartPage.cartItem(at: 1)
        }

        "Открываем пикер количества у нового товара".ybm_run { _ in
            secondItem.countPicker.tap()
        }

        "Мокаем товар с примененным 25 шт. у нового товара".ybm_run { _ in
            cartState.changeCartItemsState(with: makeChangeCartItemsBody(count: 25))
            cartState.setUserOrdersState(with: makeUserOrderOptions(firstItemCount: 100, secondItemCount: 25))

            stateManager?.setState(newState: cartState)
        }

        "Выбираем 25 шт.".ybm_run { _ in
            self.mockServer?.handledRequests.removeAll() // очищаем хранилище запросов

            var pickerWheel: XCUIElement!
            ybm_wait(forFulfillmentOf: {
                pickerWheel = XCUIApplication().pickerWheels.firstMatch
                return pickerWheel.isVisible
            })
            pickerWheel.adjust(toPickerWheelValue: "25")
            cartPage.countPickerDoneButton.tap()
            wait(forInvisibilityOf: pickerWheel)

            // проверяем вызов функции changeCartItems
            ybm_wait(forFulfillmentOf: {
                self.mockServer?.handledRequests.contains { $0.contains("name=changeCartItems") } ?? false
            })
        }

        "Проверяем количество товара".ybm_run { _ in
            cartPage.element.ybm_swipeCollectionView(to: .up, toFullyReveal: firstItem.countInfo)
            XCTAssertEqual(firstItem.countInfo.label, "100")

            cartPage.element.ybm_swipeCollectionView(toFullyReveal: secondItem.countInfo)
            XCTAssertEqual(secondItem.countInfo.label, "25")

            let totalItems = cartPage.summary.totalItems
            cartPage.element.ybm_swipeCollectionView(toFullyReveal: totalItems.element)
            XCTAssertEqual(totalItems.title.label, "Товары (125)")

            ybm_wait(forFulfillmentOf: { TabBarPage.current.cartTabItem.element.label == "Корзина99+" })
        }
    }

    // MARK: - Helper Methods

    typealias ChangeCartItemsBody = ChangeCartItems.ChangeCartItemsBody
    typealias UserOrderOptions = ResolveUserOrderOptions.UserOrderOptions

    private func makeChangeCartItemsBody(count: Int) -> ChangeCartItemsBody {
        ChangeCartItems.ChangeCartItemsBody(
            cartItems: [CartItemInCart.makeFromOffer(.protein, count: count)],
            threshold: [.already_free]
        )
    }

    private func makeUserOrderOptions(firstItemCount: Int, secondItemCount: Int? = nil) -> UserOrderOptions {
        var firstItem = Item.basic
        firstItem.label = String(Constants.firstItemId)
        firstItem.count = firstItemCount

        var cartItem = CartItem.basic
        cartItem.items = [firstItem]

        if let secondItemCount = secondItemCount {
            var secondItem = Item.basic
            secondItem.label = String(Constants.secondItemId)
            secondItem.count = secondItemCount
            cartItem.items.append(secondItem)
        }

        return ResolveUserOrderOptions.UserOrderOptions(
            region: .moscow,
            summary: .basic,
            shops: [cartItem]
        )
    }

}

// MARK: - Nested Types

private extension CartCounterTest {

    enum Constants {
        static let firstItemId = 111
        static let secondItemId = 222
    }

}
