import MarketUITestMocks
import Metrics
import XCTest

final class CheckoutCheckPaymentErrorTest: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func test_shouldSendHealthEvent_whenPaymentFailed() {
        Allure.addEpic("Метрики здоровья")
        Allure.addTitle("Проверяем отправку событий ошибок оплаты")

        var root: RootPage!
        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!

        app.launchEnvironment[TestLaunchEnvironmentKeys.paymentUrl] = "failable://webview.ru"

        disable(
            toggles:
            FeatureNames.paymentSDK,
            FeatureNames.checkoutPresetsRedesign,
            FeatureNames.cartRedesign
        )

        "Мокаем ручки".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Checkout_CheckPayment")
        }

        "Открываем корзину".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            cartPage = goToCart(root: root)
            ybm_wait(forFulfillmentOf: { NavigationBarPage.current.orderBarButton.element.isEnabled })
        }

        "Нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            checkoutPage = cartPage.compactSummary.orderButton.tap()
            ybm_wait(forVisibilityOf: [checkoutPage.element])
        }

        "Выбираем адрес доставки".ybm_run { _ in
            let checkoutPresetSelectorPage = checkoutPage.addressChooserButton().tap()
            ybm_wait(forVisibilityOf: [checkoutPresetSelectorPage.element])

            checkoutPresetSelectorPage.doneButton.tap()
            ybm_wait(forVisibilityOf: [checkoutPage.element])
        }

        "Нажимаем на кнопку \"Оплатить\"".ybm_run { _ in
            checkoutPage.element.swipe(to: .down, untilVisible: checkoutPage.paymentButton.element)
            checkoutPage.paymentButton.tap()
        }

        "Проверяем ошибку на экране оплаты картой".ybm_run { _ in
            let trustPage = TrustPage.current

            ybm_wait(forVisibilityOf: [trustPage.errorView.title], timeout: 20)
            XCTAssertEqual(trustPage.errorView.title.label, "Что-то пошло не так")
        }

        "Мокаем ручки".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Checkout_CheckPayment_Failed")
        }

        "Проверяем отправленные метрики".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { () -> Bool in
                MetricRecorder.events(from: .health)
                    .with(name: "PAYMENT_STATUS_CHECK_FAILED")
                    .with(params: ["portion": "CHECKOUT"])
                    .isNotEmpty
                    && MetricRecorder.events(from: .health)
                    .with(name: "CARD_PAYMENT_SCREEN_LOADING_FAILED")
                    .with(params: ["portion": "CHECKOUT"])
                    .isNotEmpty
            })
        }
    }

    func test_shouldSendHealthEvent_whenCheckCompletionPaymentFailed() {
        Allure.addEpic("Метрики здоровья")
        Allure.addTitle("Проверяем отправку события PAYMENT_COMPLETION_CHECK_FAILED")

        var root: RootPage!
        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!

        disable(
            toggles:
            FeatureNames.paymentSDK,
            FeatureNames.mordaRedesign,
            FeatureNames.checkoutPresetsRedesign,
            FeatureNames.cartRedesign
        )

        "Мокаем ручки".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Checkout_CheckCompletionPayment_Failed")
        }

        "Открываем корзину".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            cartPage = goToCart(root: root)
            ybm_wait(forFulfillmentOf: { NavigationBarPage.current.orderBarButton.element.isEnabled })
        }

        "Нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            checkoutPage = cartPage.compactSummary.orderButton.tap()
            ybm_wait(forVisibilityOf: [checkoutPage.element])
        }

        "Выбираем адрес доставки".ybm_run { _ in
            let checkoutPresetSelectorPage = checkoutPage.addressChooserButton().tap()
            ybm_wait(forVisibilityOf: [checkoutPresetSelectorPage.element])

            checkoutPresetSelectorPage.doneButton.tap()
            ybm_wait(forVisibilityOf: [checkoutPage.element])
        }

        "Нажимаем на кнопку \"Оплатить\"".ybm_run { _ in
            checkoutPage.element.swipe(to: .down, untilVisible: checkoutPage.paymentButton.element)
            checkoutPage.paymentButton.tap()
        }

        "Проверяем отправленные метрики".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { () -> Bool in
                MetricRecorder.events(from: .health)
                    .with(name: "PAYMENT_COMPLETION_CHECK_FAILED")
                    .with(params: ["portion": "CHECKOUT"])
                    .isNotEmpty
            })
        }
    }
}
