import MarketUITestMocks
import UIUtils
import XCTest

final class CashbackForDeliveryTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginWithYandexPlus
    }

    func test_cashbackForDeliveryType_PromotionWithCashback() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5025")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5028")
        Allure.addEpic("Чекаут")
        Allure.addFeature("Буст пвз баллами плюса")
        Allure.addTitle("Буст ПВЗ. Плашка промоутирования с баллами")

        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!
        var promotionView: CashbackDeliveryPromotionPage!

        enable(
            toggles:
            FeatureNames.cashbackDeliveryPromotion,
            FeatureNames.cashbackDeliveryPromotionInversion
        )

        "Мокаем состояние".ybm_run { _ in
            setupUserAddress()
            setupOrderOptions(.specialPickup)
            setupOrderDetails(with: .applePay)
        }

        "Открываем корзину".ybm_run { _ in
            cartPage = goToCart()
        }

        "Переходим в чекаут".ybm_run { _ in
            wait(forVisibilityOf: cartPage.compactSummary.orderButton.element)
            checkoutPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: checkoutPage.element)
        }

        "Проверяем что плашка промоутирвоания показалась".ybm_run { _ in
            checkoutPage.collectionView
                .ybm_swipeCollectionView(toFullyReveal: checkoutPage.cashbackDeliveryPromotionCell.element)
            promotionView = checkoutPage.cashbackDeliveryPromotionCell.promotionView
            XCTAssertTrue(promotionView.element.isVisible)
            XCTAssertTrue(promotionView.title.label.contains("За самовывоз"))
            XCTAssertEqual(
                promotionView.description.label,
                "Заберите заказ в пункте самовывоза Маркета и получите больше баллов"
            )
            XCTAssertTrue(promotionView.questionMark.element.isVisible)
        }

        "Проверяем онбординг".ybm_run { _ in
            let aboutPage = promotionView.questionMark.tap()
            wait(forVisibilityOf: aboutPage.element)
            aboutPage.dimmingBackgroundView.tap()
        }

        "Проверяем наличие баллов пвз в пресетах".ybm_run { _ in
            checkoutPage.collectionView
                .ybm_swipeCollectionView(to: .up, toFullyReveal: checkoutPage.addressChooserButton().element)
            let checkoutPresetSelectorPage = checkoutPage.addressChooserButton().tap()
            wait(forVisibilityOf: checkoutPresetSelectorPage.element)

            checkoutPresetSelectorPage.headerStackView.selectedChipButton(at: 0).tap()
            XCTAssertTrue(checkoutPresetSelectorPage.outletCell(at: 1).longCashbackLabel.isVisible)
            checkoutPresetSelectorPage.outletCell(at: 1).element.tap()
            checkoutPresetSelectorPage.doneButton.tap()
            wait(forVisibilityOf: checkoutPage.element)
        }

        "Проверяем наличие баллов у аутлета на чекауте".ybm_run { _ in
            XCTAssertTrue(checkoutPage.outletChooserButton().shortCashbackLabel.isVisible)
        }

    }

    func test_cashbackForDeliveryType_PromotionWithoutCashback() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5035")
        Allure.addEpic("Чекаут")
        Allure.addFeature("Буст пвз баллами плюса")
        Allure.addTitle("Буст ПВЗ. Плашка промоутирования без баллов")

        var rootPage: RootPage!
        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!
        var promotionView: CashbackDeliveryPromotionPage!

        enable(
            toggles:
            FeatureNames.cashbackDeliveryPromotion,
            FeatureNames.cashbackDeliveryPromotionInversion
        )

        "Мокаем состояние".ybm_run { _ in
            setupUserAddress()
            setupOrderOptions(.default)
            setupOrderDetails(with: .applePay)
        }

        "Открываем корзину".ybm_run { _ in
            rootPage = appAfterOnboardingAndPopups()
            cartPage = goToCart(root: rootPage)
        }

        "Переходим в чекаут".ybm_run { _ in
            wait(forVisibilityOf: cartPage.compactSummary.orderButton.element)
            checkoutPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: checkoutPage.element)
        }

        "Проверяем что плашка промоутирвоания показалась".ybm_run { _ in
            checkoutPage.collectionView
                .ybm_swipeCollectionView(toFullyReveal: checkoutPage.cashbackDeliveryPromotionCell.element)
            promotionView = checkoutPage.cashbackDeliveryPromotionCell.promotionView
            XCTAssertTrue(promotionView.element.isVisible)
            XCTAssertTrue(promotionView.title.label.contains("Получите бесплатную доставку"))
            XCTAssertEqual(
                promotionView.description.label,
                "Заберите заказ в пункте самовывоза Маркета — туда привезём бесплатно"
            )
            XCTAssertFalse(promotionView.questionMark.element.isVisible)
        }

    }

    // MARK: - Helper Methods

    typealias OrdersHandlerMapper = OrdersState.UserOrdersHandlerMapper
    typealias SimpleOrder = Order.Mapper

    private func setupUserAddress() {
        var userState = UserAuthState()
        userState.setAddressesState(addresses: [.default])
        userState.setFavoritePickupPoints(favoritePickups: [.rublevskoye, .branded])
        stateManager?.setState(newState: userState)
    }

    private func setupOrderOptions(_ cashback: OrderOptionsCashback = .default) {
        var cartState = CartState()

        var orderOptionsState = ResolveUserOrderOptions.UserOrderOptions.specialPickup
        orderOptionsState.cashback = cashback
        cartState.setUserOrdersState(with: orderOptionsState)

        stateManager?.setState(newState: cartState)
    }

    private func setupOrderDetails(with payment: Order.Payment) {
        var orderState = OrdersState()

        let order = SimpleOrder(
            status: .delivery,
            payment: payment,
            delivery: .init(deliveryPartnerType: .yandex, type: .service)
        )
        let orderMapper = OrdersHandlerMapper(orders: [order])
        orderState.setOrdersResolvers(mapper: orderMapper, for: [.all])
        orderState.setOutlet(outlets: [.rublevskoye, .branded])

        stateManager?.setState(newState: orderState)
    }

}
