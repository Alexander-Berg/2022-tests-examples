import XCTest

class SmartshoppingBonusAfterOrderTestCase: LocalMockTestCase {

    // MARK: - Private

    func completeCheckoutFlow() {
        var checkoutDeliveryPage: CheckoutDeliveryPage!
        var checkoutPaymentMethodPage: CheckoutPaymentMethodPage!
        var checkoutSummaryPage: CheckoutSummaryPage!

        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)

        "Мокаем startup для получения эксперимента checkout_fix_exp (3-шаговое флоу чекатута)".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Experiments_checkout_fix_exp")
        }

        "Мокаем состояние полного флоу добавления в корзину и чекаута одного SKU".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Single_SKU_AddToCart_Checkout_Purchase_2")
            mockStateManager?.pushState(bundleName: "Single_SKU_AddedToCart")
        }

        "Переходим в корзину".ybm_run { _ in
            let cartPage = goToCart()
            wait(forVisibilityOf: cartPage.element)
        }

        "Переходим на экран информации о доставке в чекауте".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { NavigationBarPage.current.orderBarButton.element.isEnabled })
            checkoutDeliveryPage = NavigationBarPage.current.orderBarButton.tap()
            wait(forVisibilityOf: checkoutDeliveryPage.element)
        }

        "Заполняем данные на экране информации о доставке".ybm_run { _ in
            checkoutDeliveryPage.element.swipe(to: .down, untilVisible: checkoutDeliveryPage.nameTextField)

            checkoutDeliveryPage.nameTextField.tap()
            checkoutDeliveryPage.nameTextField.typeText("Иван Иванов")
            KeyboardAccessoryPage.current.nextButton.tap()

            checkoutDeliveryPage.emailTextField.typeText("pochta@yandex.ru")
            KeyboardAccessoryPage.current.nextButton.tap()

            checkoutDeliveryPage.phoneTextField.typeText("81234567890")
            KeyboardAccessoryPage.current.doneButton.tap()
        }

        "Переходим на экран выбора типа оплаты в чекауте".ybm_run { _ in
            checkoutDeliveryPage.element.swipe(to: .down, untilVisible: checkoutDeliveryPage.continueButton.element)
            checkoutDeliveryPage.element.ybm_swipe(toFullyReveal: checkoutDeliveryPage.continueButton.element)
            checkoutPaymentMethodPage = checkoutDeliveryPage.continueButton.tap()
            ybm_wait(forFulfillmentOf: { checkoutPaymentMethodPage.element.isVisible })
        }

        "Выбираем способ оплаты наличными для успешной оплаты".ybm_run { _ in
            let cashPaymentMethodCell = checkoutPaymentMethodPage.paymentMethod(at: 2)
            cashPaymentMethodCell.element.tap()
        }

        "Переходим в саммари чекаута".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { checkoutPaymentMethodPage.element.isVisible })
            checkoutPaymentMethodPage.element.swipe(
                to: .down,
                untilVisible: checkoutPaymentMethodPage.continueButton.element
            )
            checkoutSummaryPage = checkoutPaymentMethodPage.continueButton.tap()
            wait(forVisibilityOf: checkoutSummaryPage.element)
        }

        "Переходим на экран спасибки".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { checkoutSummaryPage.element.isVisible })
            _ = checkoutSummaryPage.topOrderButton.tap()
        }
    }
}
