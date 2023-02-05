import MarketUITestMocks
import XCTest

final class CheckoutStateStoreAuthTests: CheckoutStateStoreTests {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testDeliveryAndPaymentOptions() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4253")
        Allure.addEpic("Хранение состояния чекаута")
        Allure.addFeature("Повторное флоу")
        Allure.addTitle("Сохранение данных доставки и способа оплаты при оформлении мультизаказа")

        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!

        disable(toggles: FeatureNames.all_checkout_global)

        setupEnvironment(with: "CheckoutStateStore_Multiorder")

        "Открываем корзину и нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            cartPage = goToCart()
            wait(forExistanceOf: cartPage.cartItem(at: 0).element)

            checkoutPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: checkoutPage.element)
        }

        "Выбираем адрес доставки".ybm_run { _ in
            let checkoutPresetSelectorPage = checkoutPage.addressChooserButton().tap()
            wait(forVisibilityOf: checkoutPresetSelectorPage.element)

            checkoutPresetSelectorPage.addressCell(at: 0).element.tap()
            checkoutPresetSelectorPage.doneButton.tap()

            ybm_wait { !checkoutPage.loadingPage.element.isVisible }
            checkoutPage.element
                .ybm_swipeCollectionView(toFullyReveal: checkoutPage.dateSelectorCell(at: 0).element)
            checkoutPage.dateSelectorCell(at: 0).element.tap()
            wait(forVisibilityOf: PickerPage.current.element)

            PickerPage.current.pickerWheel(at: 0).adjust(toPickerWheelValue: "чт, 29 июля, бесплатно")
            KeyboardPage.current.tapDone()

            wait(forVisibilityOf: checkoutPage.deliverySlotsCell(at: 0).element)
            checkoutPage.deliverySlotsCell(at: 0).slot(at: 2).element.tap()
            checkTimeSlot(checkoutPage, title: "15:00–16:00", slot: 2)
        }

        "Выбираем ПВЗ".ybm_run { _ in
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.outletChooserButton().element)

            let checkoutPresetSelectorPage = checkoutPage.outletChooserButton().tap()
            wait(forVisibilityOf: checkoutPresetSelectorPage.element)

            checkoutPresetSelectorPage.outletCell(at: 0).element.tap()
            checkoutPresetSelectorPage.doneButton.tap()
        }

        "Выбираем способ оплаты".ybm_run { _ in
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentMethodCell.element)
            let paymentChooser = checkoutPage.paymentMethodCell.tap()
            paymentChooser.selectPaymentMethod(with: "CASH_ON_DELIVERY")
            paymentChooser.continueButton.tap()
        }

        "Выходим и снова заходим на страницу чекаута".ybm_run { _ in
            NavigationBarPage.current.closeButton.tap()

            checkoutPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: checkoutPage.element)

            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.addressChooserButton().element)
            let addressLabel = checkoutPage.addressChooserButton().title.label
            XCTAssertEqual(
                addressLabel,
                "Москва, Усачёва улица, д. 62, кв. 12\n1 подъезд, 22 этаж, домофон 12Test, \"Тестирование\""
            )

            checkoutPage.element
                .ybm_swipeCollectionView(toFullyReveal: checkoutPage.dateSelectorCell(at: 0).element)
            XCTAssertEqual(
                checkoutPage.dateSelectorCell(at: 0).value.label,
                "чт, 29 июля, 0 ₽"
            )

            checkTimeSlot(checkoutPage, title: "15:00–16:00", slot: 2)

            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.outletChooserButton().element)
            XCTAssertEqual(
                checkoutPage.outletChooserButton().titleLabel.label,
                "Витафарм"
            )
            XCTAssertEqual(
                checkoutPage.outletChooserButton().addressLabel.label,
                "Москва, Тверская, д. 5/6"
            )

            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentMethodCell.element)
            let paymentTypeLabel = checkoutPage.paymentMethodCell.title.label
            XCTAssertEqual(
                paymentTypeLabel,
                "Наличными при получении"
            )
        }
    }

    func testFirstFlowWithAuth() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4449")
        Allure.addEpic("Хранение состояния чекаута")
        Allure.addFeature("Первое флоу")
        Allure.addTitle("Сохранение данных получателя/доставки при возврате в чекаут с авторизацией")

        var cartPage: CartPage!
        var mapViewPage: CheckoutMapViewPage!
        var editAddressPage: EditAddressPage!
        var recipientPage: CheckoutRecipientPage!
        var checkoutPage: CheckoutPage!

        let fullAddress = "Москва, Рублёвское шоссе, д. 18к1"
        let apart = "34"
        let entrance = "3"
        let intercom = "34K3432"
        let floor = "33"
        let comment = "Don't worry, be happy"

        setupEnvironment(with: "CheckoutStateStore_FirstFlow")

        "Открываем корзину и нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            cartPage = goToCart()
            wait(forExistanceOf: cartPage.cartItem(at: 0).element)

            mapViewPage = cartPage.compactSummary.orderButton.tap()
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
        }

        "Выбираем любой ПВЗ на карте, нажать продолжить".ybm_run { _ in
            ybm_wait { !mapViewPage.loadingPage.element.isVisible }

            mapViewPage.tapToCurrentLocation()
            mapViewPage.element.ybm_tapCenter()

            wait(forVisibilityOf: OutletMapInfoPage.current.element)
            OutletMapInfoPage.current.continueButton.tap()

            recipientPage = CheckoutRecipientPage.current
            wait(forVisibilityOf: recipientPage.element)
        }

        "Заполнить все поля, нажать \"продолжить\"".ybm_run { _ in
            recipientPage.nameTextField.typeText("Temik Vzorvish")
            recipientPage.emailTextField.typeText("vzorvanii@tip.ok")
            recipientPage.phoneTextField.typeText("88899900999")

            KeyboardPage.current.tapDone()
            recipientPage.continueButton.tap()
        }

        "Проверяем данные доставки и получателя".ybm_run { _ in
            checkoutPage = CheckoutPage.current
            wait(forVisibilityOf: checkoutPage.element)

            XCTAssertEqual(
                checkoutPage.addressChooserButton().title.label,
                "\(fullAddress), кв. \(apart)\n\(entrance) подъезд, \(floor) этаж, домофон \(intercom), \"\(comment)\""
            )

            checkoutPage.element
                .ybm_swipeCollectionView(toFullyReveal: checkoutPage.outletChooserButton().element)
            XCTAssertEqual(checkoutPage.outletChooserButton().titleLabel.label, "АСНА")

            checkoutPage.element
                .ybm_swipeCollectionView(toFullyReveal: checkoutPage.recipientInfoCell.element)
            XCTAssertEqual(
                checkoutPage.recipientInfoCell.title.label,
                "Temik Vzorvish\nvzorvanii@tip.ok, +7 (889) 990-09-99"
            )
        }

        "Закрываем чекаут и снова переходит на него".ybm_run { _ in
            NavigationBarPage.current.closeButton.tap()
            wait(forExistanceOf: cartPage.cartItem(at: 0).element)

            checkoutPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: checkoutPage.element)
        }

        "Проверяем данные доставки и получателя".ybm_run { _ in
            XCTAssertEqual(
                checkoutPage.addressChooserButton().title.label,
                "\(fullAddress), кв. \(apart)\n\(entrance) подъезд, \(floor) этаж, домофон \(intercom), \"\(comment)\""
            )

            checkoutPage.element
                .ybm_swipeCollectionView(toFullyReveal: checkoutPage.outletChooserButton().element)
            XCTAssertEqual(checkoutPage.outletChooserButton().titleLabel.label, "АСНА")

            checkoutPage.element
                .ybm_swipeCollectionView(toFullyReveal: checkoutPage.recipientInfoCell.element)
            XCTAssertEqual(
                checkoutPage.recipientInfoCell.title.label,
                "Temik Vzorvish\nvzorvanii@tip.ok, +7 (889) 990-09-99"
            )
        }
    }

    // MARK: - Private

    private func setupEnvironment(with bundleName: String) {
        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
        app.launchEnvironment[TestLaunchEnvironmentKeys.locationLatitude] = "55.741"
        app.launchEnvironment[TestLaunchEnvironmentKeys.locationLongitude] = "37.432"

        disable(
            toggles: FeatureNames.checkoutFirstFlowLikeRepeatedV2,
            FeatureNames.checkoutPresetsRedesign,
            FeatureNames.cartRedesign
        )

        var checkoutState = CheckoutState()
        checkoutState.setRegion(region: .moscow)

        stateManager?.setState(newState: checkoutState)
        mockStateManager?.pushState(bundleName: bundleName)
    }

    private func checkTimeSlot(
        _ checkoutPage: CheckoutPage,
        title: String,
        row: Int = 0,
        slot: Int = 0
    ) {
        let timeSlot = checkoutPage.deliverySlotsCell(at: row).slot(at: slot)
        XCTAssertTrue(timeSlot.isSelected)
        XCTAssertEqual(timeSlot.title, title)
    }
}
