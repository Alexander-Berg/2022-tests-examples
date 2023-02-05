import XCTest

class CheckoutStateStoreTests: LocalMockTestCase {

    func testDeliveryAddressSuggest() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4461")
        Allure.addEpic("Хранение состояния чекаута")
        Allure.addFeature("Первое флоу")
        Allure.addTitle("Автоматический выбор ранее созданного адреса, который соответствует выбранному региону")

        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!

        setupEnvironment(with: "CheckoutStateStore_Address")

        "Открываем корзину и нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            cartPage = goToCart()
            wait(forExistanceOf: cartPage.cartItem(at: 0).element)

            checkoutPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: checkoutPage.element)
        }

        "Проверяем, что подставился московский адрес".ybm_run { _ in
            XCTAssertEqual(
                checkoutPage.addressChooserButton().title.label,
                "Москва, Красная площадь, д. 7"
            )
        }

        "Меняем регион на Самару".ybm_run { _ in
            NavigationBarPage.current.closeButton.tap()
            wait(forExistanceOf: cartPage.cartItem(at: 0).element)

            NavigationBarPage.current.selectRegionButton.tap()

            mockStateManager?.pushState(bundleName: "CheckoutStateStore_Address_updateDeliveryRegion")

            let regionSelectPage = RegionSelectPage.current
            wait(forVisibilityOf: regionSelectPage.element)
            regionSelectPage.regionInput.ybm_clearAndEnterText("Самара")
            ybm_wait(forFulfillmentOf: { regionSelectPage.geoSuggest.first?.isVisible ?? false })
            regionSelectPage.geoSuggest.first?.tap()
        }

        "Проверяем, что подставился самарский адрес".ybm_run { _ in
            checkoutPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: checkoutPage.element)

            XCTAssertEqual(
                checkoutPage.addressChooserButton().title.label,
                "Самара, площадь Ленина, д. 4А"
            )
        }
    }

    func testFirstFlowWithoutAuth() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4457")
        Allure.addEpic("Хранение состояния чекаута")
        Allure.addFeature("Первое флоу")
        Allure.addTitle("Сохранение данных получателя/доставки при возврате в чекаут без авторизации")

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

        disable(toggles: FeatureNames.cartRedesign)

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

    private func setupEnvironment(with bundleName: String) {
        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
        app.launchEnvironment[TestLaunchEnvironmentKeys.locationLatitude] = "55.741"
        app.launchEnvironment[TestLaunchEnvironmentKeys.locationLongitude] = "37.432"

        disable(
            toggles:
            FeatureNames.checkoutFirstFlowLikeRepeatedV2,
            FeatureNames.checkoutPresetsRedesign,
            FeatureNames.cartRedesign
        )

        mockStateManager?.pushState(bundleName: bundleName)
    }
}
