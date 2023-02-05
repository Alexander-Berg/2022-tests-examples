import MarketUITestMocks
import XCTest

final class CheckoutExpressDeliveryTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    // swiftlint:disable function_body_length
    func testExpressHasDeliveryTimeInCart() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4385")
        Allure.addEpic("Чекаут")
        Allure.addFeature("Экспресс доставка")
        Allure.addTitle("Отображение времени доставки в корзине")

        var cartPage: CartPage!
        var mapViewPage: CheckoutMapViewPage!
        var editAddressPage: EditAddressPage!
        var recipientPage: CheckoutRecipientPage!
        var checkoutPage: CheckoutPage!
        var checkoutPaymentPage: CheckoutPaymentMethodPopupPage!
        var finishMultiorderPage: FinishMultiorderPage!

        let fullAddress = "Москва, Новинский бульвар, д. 8"
        let apart = "34"
        let entrance = "3"
        let intercom = "34K3432"
        let floor = "33"
        let comment = "Позвонить в домофон"

        disable(
            toggles:
            FeatureNames.paymentSDK,
            FeatureNames.checkoutPresetsRedesign,
            FeatureNames.checkoutFirstFlowLikeRepeatedV2,
            FeatureNames.cartRedesign
        )

        "Мокаем ручки".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "CheckoutExpressDelivery")
        }

        "Открываем корзину".ybm_run { _ in
            cartPage = goToCart()
            wait(forExistanceOf: cartPage.cartItem(at: 0).element)
            XCTAssertEqual(
                cartPage.businessGroupHeader(at: 0).text.label,
                "Экспресс-доставка Яндекса"
            )
        }

        "Нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            mapViewPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: mapViewPage.element)
        }

        "Выбираем любую точку на карте, нажать привезти сюда".ybm_run { _ in
            ybm_wait { !mapViewPage.loadingPage.element.isVisible }

            ybm_wait {
                mapViewPage.summary.addressTextField.text == fullAddress
            }

            editAddressPage = mapViewPage.summary.tap()
            XCTAssertEqual(editAddressPage.addressCellView.label, fullAddress)
        }

        "Заполняем все дополнительные поля".ybm_run { _ in
            editAddressPage.apartmentInput.typeText(apart)
            editAddressPage.entranceInput.typeText(entrance)
            editAddressPage.intercomInput.typeText(intercom)
            editAddressPage.floorInput.typeText(floor)
            editAddressPage.commentInput.typeText(comment)

            KeyboardPage.current.tapDone()
            editAddressPage.continueButton.tap()

            recipientPage = CheckoutRecipientPage.current
            wait(forVisibilityOf: recipientPage.element)
        }

        "Заполнить все поля, нажать \"продолжить\"".ybm_run { _ in
            recipientPage.nameTextField.typeText("Тестовый Юзер")
            recipientPage.emailTextField.typeText("test-test@user.com")
            recipientPage.phoneTextField.typeText("88899900999")

            KeyboardPage.current.tapDone()
            recipientPage.continueButton.tap()
        }

        "Проверяем наличие надписей, получателя, способ оплаты".ybm_run { _ in
            checkoutPage = CheckoutPage.current
            wait(forVisibilityOf: checkoutPage.element)

            XCTAssertEqual(
                checkoutPage.shipmentHeaderCell().title.label,
                "Экспресс-доставка, 99 ₽"
            )
            XCTAssertEqual(
                checkoutPage.addressChooserButton().title.label,
                "\(fullAddress), кв. \(apart)\n\(entrance) подъезд, \(floor) этаж, домофон \(intercom), \"\(comment)\""
            )
            checkoutPage.element
                .ybm_swipeCollectionView(toFullyReveal: checkoutPage.dateSelectorCell(at: 0).element)
            XCTAssertEqual(checkoutPage.dateSelectorCell(at: 0).value.label, "пн, 2 августа, 99 ₽")

            let timeSlot = checkoutPage.deliverySlotsCell(at: 0).slot(at: 0)
            XCTAssertTrue(timeSlot.isSelected)
            XCTAssertEqual(timeSlot.title, "11:00–11:40")

            checkoutPage.element
                .ybm_swipeCollectionView(toFullyReveal: checkoutPage.recipientInfoCell.element)
            XCTAssertEqual(
                checkoutPage.recipientInfoCell.title.label,
                "Тестовый Юзер\ntest-test@user.com, +7 (889) 990-09-99"
            )

            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentMethodCell.element)
            XCTAssertEqual(checkoutPage.paymentMethodCell.title.label, "Картой онлайн")
        }

        "Нажимаем на способ оплаты".ybm_run { _ in
            checkoutPage.element
                .ybm_swipeCollectionView(to: .up, toFullyReveal: checkoutPage.paymentMethodCell.element)

            checkoutPaymentPage = checkoutPage.paymentMethodCell.tap()

            checkoutPaymentPage.selectPaymentMethod(with: "YANDEX")
            checkoutPaymentPage.continueButton.tap()
        }

        "Нажимаем кнопку Перейти к оплате и авторизовываемся".ybm_run { _ in
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentMethodCell.element)
            XCTAssertEqual(checkoutPage.paymentMethodCell.title.label, "Картой онлайн")

            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentButton.element)
            finishMultiorderPage = checkoutPage.paymentButton.tap()
        }

        "Закрыть окно для ввода данных карты".ybm_run { _ in
            let trustPage = TrustPage.current

            ybm_wait(forVisibilityOf: [trustPage.element])
            XCTAssertEqual(trustPage.navigationBar.title.label, "Оплата картой")

            trustPage.navigationBar.closeButton.tap()
        }

        "Проверяем экран Оформленного заказа".ybm_run { _ in
            ybm_wait {
                finishMultiorderPage.element.isVisible &&
                    NavigationBarPage.current.title.isVisible
            }
            XCTAssertEqual(NavigationBarPage.current.title.label, "Спасибо")

            let detailsExpander = finishMultiorderPage.orderDetailsExpander()
            finishMultiorderPage.element.ybm_swipeCollectionView(toFullyReveal: detailsExpander)
            detailsExpander.tap()
            finishMultiorderPage.element
                .ybm_swipeCollectionView(toFullyReveal: finishMultiorderPage.paymentMethodOfOrderItem().element)
            XCTAssertEqual(finishMultiorderPage.paymentMethodOfOrderItem().textView.label, "Картой онлайн")
        }
    }

    func testRefreshPriceOnAddressChange() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5952")
        Allure.addEpic("Чекаут")
        Allure.addFeature("Экспресс доставка")
        Allure.addTitle("Рефреш чекаута при изменении адреса доставки")

        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!
        var checkoutPresetSelectorPage: CheckoutPresetSelectorPage!

        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)

        "Мокаем ручки".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "CheckoutExpressReactualizationOnAddressChange")
            prepareUserState()
        }

        "Открываем корзину".ybm_run { _ in
            cartPage = goToCart()
            wait(forExistanceOf: cartPage.cartItem(at: 0).element)
            XCTAssertEqual(
                cartPage.businessGroupHeader(at: 0).text.label,
                "Экспресс-доставка Яндекса"
            )
        }

        "Нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            cartPage.compactSummary.orderButton.element.tap()
            checkoutPage = CheckoutPage.current
            wait(forVisibilityOf: checkoutPage.collectionView)
        }

        "Проверяем параметры чекаута".ybm_run { _ in
            let titleCell = checkoutPage.shipmentHeaderCell()
            XCTAssertEqual(
                titleCell.title.text,
                "Экспресс-доставка, 49 ₽",
                "Заголовок должен быть \"Экспресс-доставка, 49 ₽\""
            )

            XCTAssertEqual(checkoutPage.dateSelectorCell(at: 0).value.label, "сб, 1 января, 49 ₽")

            XCTAssertTrue(checkoutPage.deliverySlotsCell(at: 0).slot(at: 0).isSelected)
        }

        "Открываем список адресов".ybm_run { _ in
            checkoutPresetSelectorPage = checkoutPage.addressChooserButton().tap()
            wait(forVisibilityOf: checkoutPresetSelectorPage.collectionView)
        }

        "Меняем адрес доставки, подтверждаем выбор".ybm_run { _ in
            checkoutPresetSelectorPage.addressCell(at: 1).element.tap()
            mockStateManager?.pushState(bundleName: "CheckoutExpressReactualizationOnAddressChange_update")
            checkoutPresetSelectorPage.doneButton.tap()

            wait(forInvisibilityOf: checkoutPresetSelectorPage.collectionView)
        }

        "Проверяем переактуализацию чекаута".ybm_run { _ in
            XCTAssertEqual(checkoutPage.dateSelectorCell(at: 0).value.label, "вс, 2 января, 199 ₽")
        }
    }

    // MARK: - Helper methods

    private func prepareUserState() {
        var userState = UserAuthState(isLoggedIn: true, isYandexPlus: false)
        let lastState = ResolveUserLastState.UserLastState(
            paymentType: .prepaid,
            contactId: "302c6a40-2f31-4859-8c29-4e5f503e8029",
            paymentMethod: .yandex,
            parcelsInfo: [
                ResolveUserLastState.ParcelInfo(
                    intervalDate: "07-05-2022_07-05-2022",
                    deliveryType: .delivery,
                    addressId: "6d5b0aba-f7b1-4897-a985-21fc663e6470",
                    label: "4372290_AQ0At9pD-OeIz5bQxkj1gA",
                    deliveryFeature: .default,
                    intervalTime: "09:00_18:00"
                )
            ]
        )

        userState.setUserLastState(lastState)
        stateManager?.setState(newState: userState)
    }
}
