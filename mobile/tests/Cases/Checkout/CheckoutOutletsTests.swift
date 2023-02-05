import MarketUITestMocks
import XCTest

final class CheckoutOutletsTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testOutletStoragePeriod() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3312")
        Allure.addEpic("Чекаут")
        Allure.addFeature("ПВЗ")
        Allure.addTitle("Срок хранения")

        var root: RootPage!
        var cartPage: CartPage!
        var deliveryPage: CheckoutDeliveryPage!
        var pickupInfoPage: CheckoutPickupInfoPage!

        "Мокаем ручки".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Checkout_Outlets_StoragePeriod")
        }

        "Открываем корзину".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            cartPage = goToCart(root: root)
            wait(forExistanceOf: cartPage.cartItem(at: 0).element)
        }

        "Нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            deliveryPage = cartPage.compactSummary.orderButton.tap()
            ybm_wait(forVisibilityOf: [deliveryPage.element])
        }

        "Проверяем срок хранения у выбранного ПВЗ".ybm_run { _ in
            deliveryPage.element.swipe(to: .down, untilVisible: deliveryPage.outletInfo.element)
            XCTAssertEqual(
                deliveryPage.outletInfo.pickupInfo.storagePeriod.label,
                "Срок хранения заказа 3 дня"
            )
        }

        "Проверяем срок хранения на экране подробностей выбранного ПВЗ".ybm_run { _ in
            pickupInfoPage = deliveryPage.outletInfo.detailsButton.tap()
            ybm_wait(forFulfillmentOf: {
                pickupInfoPage.element.isVisible
            })
            XCTAssertEqual(pickupInfoPage.pickupInfo.storagePeriod.label, "Срок хранения заказа 3 дня")
        }
    }

    func testOutletTimeInterval() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4859")
        Allure.addEpic("Чекаут")
        Allure.addFeature("Время доставки в ПВЗ")
        Allure.addTitle("Чекаут")

        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!
        var checkoutPresetSelectorPage: CheckoutPresetSelectorPage!

        disable(toggles: FeatureNames.cartRedesign)

        "Мокаем состояние".ybm_run { _ in
            setupEnvironment()
        }

        "Открываем корзину".ybm_run { _ in
            cartPage = goToCart()
        }

        "Переходим в чекаут".ybm_run { _ in
            wait(forVisibilityOf: cartPage.compactSummary.orderButton.element)
            checkoutPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: checkoutPage.element)
        }

        "Выбираем доставку самовывозом".ybm_run { _ in
            checkoutPresetSelectorPage = checkoutPage.addressChooserButton().tap()
            checkoutPresetSelectorPage.selectChip(at: 0)
            checkoutPresetSelectorPage.doneButton.tap()
            wait(forInvisibilityOf: checkoutPresetSelectorPage.element)
        }

        "Проверяем текст с датой и временем доставки в чекауте".ybm_run { _ in
            XCTAssertEqual(
                checkoutPage.shipmentHeaderCell(at: 0).title.label,
                "Доставка в пункт выдачи 11 июля к 14:00, 49 ₽"
            )
        }
    }

    // MARK: - Helper Methods

    typealias OrdersHandlerMapper = OrdersState.UserOrdersHandlerMapper
    typealias SimpleOrder = Order.Mapper

    private func setupEnvironment() {
        disable(toggles: FeatureNames.checkoutPresetsRedesign)
        setupUserAddress()
        setupOrderOptions()
        setupOrderDetails()
    }

    private func setupUserAddress() {
        var userState = UserAuthState()
        userState.setAddressesState(addresses: [.default])
        userState.setFavoritePickupPoints(favoritePickups: [.rublevskoye])
        stateManager?.setState(newState: userState)
    }

    private func setupOrderOptions() {
        var cartState = CartState()
        cartState.setUserOrdersState(with: .outlet)
        stateManager?.setState(newState: cartState)
    }

    private func setupOrderDetails() {
        var orderState = OrdersState()
        let order = SimpleOrder(
            status: .delivered,
            payment: .applePay,
            delivery: .init(deliveryPartnerType: .yandex, type: .service)
        )
        let orderMapper = OrdersHandlerMapper(orders: [order])
        orderState.setOrdersResolvers(mapper: orderMapper, for: [.all])
        orderState.setOutlet(outlets: [.rublevskoye])
        stateManager?.setState(newState: orderState)
    }
}
