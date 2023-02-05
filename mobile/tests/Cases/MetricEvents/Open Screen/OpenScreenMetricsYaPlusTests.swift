import MarketUITestMocks
import Metrics
import UIUtils
import XCTest

class OpenScreenMetricsYaPlusTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginWithYandexPlus
    }

    func testShouldSendMetrics_Cart() throws {
        Allure.addEpic("Продуктовая метрика")
        Allure.addFeature("Открытие экрана")
        Allure.addTitle("Корзина, чекаут")

        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!

        disable(toggles: FeatureNames.cartRedesign)

        "Мокаем ручки".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "OpenScreenMetricsTests_Cart")
        }

        "Открываем корзину".ybm_run { _ in
            let root = appAfterOnboardingAndPopups()

            cartPage = goToCart(root: root)
            wait(forExistanceOf: cartPage.cartItem(at: 0).element)
        }

        "Нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            checkoutPage = cartPage.compactSummary.orderButton.tap()
        }

        "Выбираем способ оплаты \"наличными при получении\" и подтверждаем заказ".ybm_run { _ in
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentMethodCell.element)

            let paymentMethodPopupPage = checkoutPage.paymentMethodCell.tap()
            paymentMethodPopupPage.selectPaymentMethod(with: "CASH_ON_DELIVERY")
            paymentMethodPopupPage.continueButton.tap()

            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentButton.element)

            let finishMultiorderPage = checkoutPage.paymentButton.tap()
            let navigationTextTitle = NavigationBarPage.current.title

            ybm_wait(forFulfillmentOf: {
                finishMultiorderPage.element.isVisible && navigationTextTitle.isVisible
            })
        }

        try "Чекаем метрики".run {
            try checkOpenScreenEvent(name: "CART")
            try checkOpenScreenEvent(name: "CHECKOUT_CONFIRM")
            try checkOpenScreenEvent(name: "TYPAGE")
        }
    }

    // MARK: - Private methods

    @discardableResult
    private func checkOpenScreenEvent(name: String) throws -> MetricRecorderEvent {
        try XCTUnwrap(
            MetricRecorder.events(from: .appmetrica).with(name: "OPEN-PAGE_VISIBLE")
                .with(params: ["pageName": name]).first
        )
    }
}
