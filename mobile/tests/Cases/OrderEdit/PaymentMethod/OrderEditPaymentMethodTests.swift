import FormKit
import MarketUI
import MarketUITestMocks
import XCTest

final class OrderEditPaymentMethodTests: OrderEditTestCase {

    // MARK: - Public

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testEditPaymentMethodFromCardToCard() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3271")
        Allure.addEpic("Редактирование заказа")
        Allure.addFeature("Изменение способа оплаты")
        Allure.addTitle("Изменение предоплаты. Картой онлайн. Точка входа из моих заказов")

        enable(toggles: FeatureNames.applePay)
        disable(toggles: FeatureNames.paymentSDK)

        var editPaymentPage: OrderEditPaymentPage!
        let orderId = "32447784"

        "Мокаем состояния".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "OrderWithCardPayment")
            setupState(
                orderId: orderId,
                orderEditRequest: [.changePaymentMethodRequest(orderId: orderId, status: .applied)],
                status: .unpaid
            )
        }

        "Переходим в экран изменения предоплаты".ybm_run { _ in
            let ordersListPage = goToOrdersListPage()

            editPaymentPage = ordersListPage.payButton(orderId: orderId).tap()
            ybm_wait { editPaymentPage.element.isVisible }
        }

        "Проверяем отображение способов оплаты".ybm_run { _ in
            checkPaymentMethod(withIdentifier: "APPLE_PAY", in: editPaymentPage) { paymentMethod in
                XCTAssertTrue(paymentMethod.element.isVisible)
                XCTAssertEqual(paymentMethod.title.label, "Apple Pay")
                XCTAssertEqual(
                    paymentMethod.image.identifier,
                    RadioButtonViewCellAccessibility.Selectability.selectable
                )
            }

            checkPaymentMethod(withIdentifier: "YANDEX", in: editPaymentPage) { paymentMethod in
                XCTAssertTrue(paymentMethod.element.isVisible)
                XCTAssertEqual(paymentMethod.title.label, "Картой онлайн")
                XCTAssertEqual(
                    paymentMethod.image.identifier,
                    RadioButtonViewCellAccessibility.Selectability.selected
                )
            }
        }

        "Тапнуть на кнопку перехода на экран оплаты".ybm_run { _ in
            let continueButton = editPaymentPage.continueButton
            ybm_wait(forVisibilityOf: [continueButton])

            continueButton.tap()
        }

        "Открывается экран оплаты".ybm_run { _ in
            let trustPage = TrustPage.current

            ybm_wait(forVisibilityOf: [trustPage.element])
            XCTAssertEqual(trustPage.navigationBar.title.label, "Оплата картой")
        }
    }

    func testEditPaymentMethodFromCardToApplePay() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3272")
        Allure.addEpic("Редактирование заказа")
        Allure.addFeature("Изменение способа оплаты")
        Allure.addTitle("Изменение предоплаты. Карта. Точка входа на морде")

        enable(toggles: FeatureNames.applePay)

        var mordaPage: MordaPage!
        var editPaymentPage: OrderEditPaymentPage!
        var applePaySummary: ApplePaySummaryPage!

        let orderId = "51839622"

        "Мокаем состояния".ybm_run { _ in
            setupState(
                orderId: orderId,
                orderEditRequest: [.changePaymentMethodRequest(orderId: orderId, status: .applied)],
                status: .unpaid
            )
        }

        "Пользователь находится на морде".ybm_run { _ in
            mordaPage = goToMorda()
        }

        "Переходим в экран изменения предоплаты".ybm_run { _ in
            let snippet = mordaPage.singleActionContainerWidget.container.orderSnippet()
            ybm_wait(forVisibilityOf: [snippet.actionButton.element])

            editPaymentPage = snippet.actionButton.tap()
            ybm_wait(forVisibilityOf: [editPaymentPage.element])
        }

        "Проверяем отображение способов оплаты".ybm_run { _ in
            checkPaymentMethod(withIdentifier: "APPLE_PAY", in: editPaymentPage) { paymentMethod in
                XCTAssertTrue(paymentMethod.element.isVisible)
                XCTAssertEqual(paymentMethod.title.label, "Apple Pay")
                XCTAssertEqual(
                    paymentMethod.image.identifier,
                    RadioButtonViewCellAccessibility.Selectability.selectable
                )
            }

            checkPaymentMethod(withIdentifier: "YANDEX", in: editPaymentPage) { paymentMethod in
                XCTAssertTrue(paymentMethod.element.isVisible)
                XCTAssertEqual(paymentMethod.title.label, "Картой онлайн")
                XCTAssertEqual(
                    paymentMethod.image.identifier,
                    RadioButtonViewCellAccessibility.Selectability.selected
                )
            }
        }

        "Сменить способ оплаты на Apple pay".ybm_run { _ in
            editPaymentPage.selectPaymentMethod(withIdentifier: "APPLE_PAY")
        }

        "Мокаем состояния".ybm_run { _ in
            let payment: Payment = .applePay
            setupState(
                orderId: orderId,
                orderEditRequest: [
                    .changePaymentMethodRequest(
                        orderId: orderId,
                        status: .applied,
                        payment: payment
                    )
                ],
                status: .unpaid,
                payment: payment
            )
        }

        "Тапнуть на кнопку перехода на экран оплаты".ybm_run { _ in
            let continueButton = editPaymentPage.continueButton
            ybm_wait(forVisibilityOf: [continueButton])

            continueButton.tap()
        }

        "Открывается экран с саммари по заказу".ybm_run { _ in
            applePaySummary = ApplePaySummaryPage.current
            ybm_wait(forVisibilityOf: [applePaySummary.element])
        }

        "Проверить отображение".ybm_run { _ in
            XCTAssertEqual(applePaySummary.totalTitle.label, "Товары (3)")
            XCTAssertEqual(applePaySummary.totalDetails.label, "245 970 ₽")

            XCTAssertEqual(applePaySummary.discountTitle.label, "Скидка на товары")
            XCTAssertEqual(applePaySummary.discountDetails.label, "-9 036 ₽")

            XCTAssertEqual(applePaySummary.coinDiscountTitle.label, "Купоны")
            XCTAssertEqual(applePaySummary.coinDiscountDetails.label, "-1 000 ₽")

            XCTAssertEqual(applePaySummary.promoCodeDiscountTitle.label, "Скидка по промокоду")
            XCTAssertEqual(applePaySummary.promoCodeDiscountDetails.label, "-300 ₽")

            XCTAssertEqual(applePaySummary.priceDropDiscountTitle.label, "Скидка по акции")
            XCTAssertEqual(applePaySummary.priceDropDiscountDetails.label, "-180 ₽")

            XCTAssertEqual(applePaySummary.deliveryTitle.label, "Самовывоз")
            XCTAssertEqual(applePaySummary.deliveryDetails.label, "бесплатно")

            XCTAssertEqual(applePaySummary.sumTitle.label, "Итого")
            XCTAssertEqual(applePaySummary.sumDetails.label, "777 ₽")

            XCTAssertEqual(applePaySummary.merchant.label, "Информация о товарах, услугах и продавце")

            XCTAssertTrue(applePaySummary.payButton.isVisible)
            XCTAssertTrue(applePaySummary.payButton.isHittable)

            XCTAssertEqual(
                applePaySummary.licenceAgreementDisclaimer.label,
                "Нажимая «Подтвердить заказ» или «Перейти к оплате», " +
                    "вы соглашаетесь с условиями использования сервиса Яндекс.Маркет"
            )
            XCTAssertEqual(
                applePaySummary.termsOfReturnDisclaimer.value as? String,
                "Полный возврат денежных средств, если вы не получили товар. Подробнее"
            )
            XCTAssertEqual(
                applePaySummary.supportDisclaimer.value as? String,
                "8\(String.ble_nonBreakingSpace)(495)\(String.ble_nonBreakingSpace)414-30-00 Служба поддержки"
            )
        }

        "Нажать кнопку оплаты".ybm_run { _ in
            applePaySummary.payButton.tap()
            ybm_wait { !applePaySummary.payButton.isVisible }
        }
    }

    func testEditPaymentMethodFromApplePayToCard() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3273")
        Allure.addEpic("Редактирование заказа")
        Allure.addFeature("Изменение способа оплаты")
        Allure.addTitle("Изменение предоплаты. Apple. Точка входа в корзине")

        enable(toggles: FeatureNames.applePay)
        disable(toggles: FeatureNames.paymentSDK)

        var cartPage: CartPage!
        var editPaymentPage: OrderEditPaymentPage!

        let orderId = "4717083"

        "Мокаем состояния".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "OrderWithApplePayPayment")
            setupState(
                orderId: orderId,
                orderEditRequest: [.changePaymentMethodRequest(orderId: orderId, status: .applied)],
                status: .unpaid,
                payment: .applePay
            )
        }

        "Пользователь находится в корзине".ybm_run { _ in
            cartPage = goToCart()
        }

        "Переходим в экран изменения предоплаты".ybm_run { _ in
            cartPage.element.ybm_swipeCollectionView(toFullyReveal: cartPage.unpaidOrderButton.element)
            editPaymentPage = cartPage.unpaidOrderButton.tap()
            ybm_wait { editPaymentPage.element.isVisible }
        }

        "Проверяем отображение способов оплаты".ybm_run { _ in
            checkPaymentMethod(withIdentifier: "APPLE_PAY", in: editPaymentPage) { paymentMethod in
                XCTAssertTrue(paymentMethod.element.isVisible)
                XCTAssertEqual(paymentMethod.title.label, "Apple Pay")
                XCTAssertEqual(
                    paymentMethod.image.identifier,
                    RadioButtonViewCellAccessibility.Selectability.selected
                )
            }

            checkPaymentMethod(withIdentifier: "YANDEX", in: editPaymentPage) { paymentMethod in
                XCTAssertTrue(paymentMethod.element.isVisible)
                XCTAssertEqual(paymentMethod.title.label, "Картой онлайн")
                XCTAssertEqual(
                    paymentMethod.image.identifier,
                    RadioButtonViewCellAccessibility.Selectability.selectable
                )
            }
        }

        "Сменить способ оплаты на картой онлайн".ybm_run { _ in
            editPaymentPage.selectPaymentMethod(withIdentifier: "YANDEX")
        }

        "Мокаем состояния".ybm_run { _ in
            let payment: Payment = .prepaid
            setupState(
                orderId: orderId,
                orderEditRequest: [
                    .changePaymentMethodRequest(
                        orderId: orderId,
                        status: .applied,
                        payment: payment
                    )
                ],
                status: .unpaid,
                payment: payment
            )
        }

        "Тапнуть на кнопку перехода на экран оплаты".ybm_run { _ in
            let continueButton = editPaymentPage.continueButton
            ybm_wait(forVisibilityOf: [continueButton])

            continueButton.tap()
        }

        "Открывается экран оплаты картой онлайн".ybm_run { _ in
            let trustPage = TrustPage.current

            ybm_wait(forVisibilityOf: [trustPage.element])
            XCTAssertEqual(trustPage.navigationBar.title.label, "Оплата картой")
        }
    }

    // MARK: - Private

    private func checkPaymentMethod(
        withIdentifier identifier: String,
        in editPaymentPage: OrderEditPaymentPage,
        check: (RadioButtonPage) -> Void
    ) {
        check(editPaymentPage.paymentMethod(withIdentifier: identifier))
    }
}
