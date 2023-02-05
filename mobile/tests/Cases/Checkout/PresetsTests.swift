import MarketUITestMocks
import XCTest

final class PresetsTests: LocalMockTestCase {

    func testWithUsualAndClickAndCollectCart() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4826")
        Allure.addEpic("Пресеты 2.0")
        Allure.addFeature("Первый заказ")
        Allure.addTitle("Обычный и C&C")

        var cartPage: CartPage!
        var mapViewPage: CheckoutMapViewPage!
        var editAddressPage: EditAddressPage!
        var recipientPage: CheckoutRecipientPage!
        var checkoutPage: CheckoutPage!
        var checkoutPresetSelectorPage: CheckoutPresetSelectorPage!

        let fullAddress = "Москва, Рублёвское шоссе, д. 18к1"

        var checkoutState = CheckoutState()

        "Мокаем новые моки".ybm_run { _ in
            checkoutState.setUserContactState(handlers: [.add])
            checkoutState.setUserAddressState(handlers: [.add])

            stateManager?.setState(newState: checkoutState)
        }

        disable(toggles: FeatureNames.checkoutFirstFlowLikeRepeatedV2, FeatureNames.cartRedesign)

        app.launchEnvironment[TestLaunchEnvironmentKeys.locationLatitude] = "55.741"
        app.launchEnvironment[TestLaunchEnvironmentKeys.locationLongitude] = "37.432"

        "Мокаем ручки".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Experiments_checkPresets")
            mockStateManager?.pushState(bundleName: "Preset_UsualAndCC")
        }

        "Открываем корзину и нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            cartPage = goToCart()
            wait(forExistanceOf: cartPage.cartItem(at: 0).element)

            mapViewPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: mapViewPage.element)
            ybm_wait { !mapViewPage.loadingPage.element.isVisible }

            mapViewPage.selectChip(at: 1)
            XCTAssertEqual(mapViewPage.selectedChipTitle, "Курьер")
        }

        "Нажимаем \"Привезти сюда\" и затем нажимаем кнопку \"Продолжить\"".ybm_run { _ in
            ybm_wait { mapViewPage.summary.addressTextField.text == fullAddress }

            editAddressPage = mapViewPage.summary.tap()
            XCTAssertEqual(editAddressPage.addressCellView.label, fullAddress)
            editAddressPage.continueButton.tap()
        }

        "Выбрать на карте пункт самовывоза Фармы, нажать Продолжить".ybm_run { _ in
            ybm_wait { !mapViewPage.loadingPage.element.isVisible }

            mapViewPage.element.ybm_tapCenter()

            wait(forVisibilityOf: OutletMapInfoPage.current.element)

            OutletMapInfoPage.current.continueButton.tap()

            recipientPage = CheckoutRecipientPage.current
            wait(forVisibilityOf: recipientPage.element)
        }

        "Заполнить все поля, нажать \"продолжить\"".ybm_run { _ in
            recipientPage.nameTextField.typeText("Юай тест")
            recipientPage.emailTextField.typeText("test@test.com")
            recipientPage.phoneTextField.typeText("79999999999")

            KeyboardPage.current.tapDone()
            recipientPage.continueButton.tap()

            checkoutPage = CheckoutPage.current
            wait(forVisibilityOf: checkoutPage.element)

            XCTAssertEqual(checkoutPage.addressChooserButton().title.label, "Курьером\n" + fullAddress)

            checkoutPage.element
                .ybm_swipeCollectionView(toFullyReveal: checkoutPage.outletChooserButton().addressLabel)
            XCTAssertEqual(checkoutPage.outletChooserButton().titleLabel.label, "АСНА")
            XCTAssertEqual(
                checkoutPage.outletChooserButton().addressLabel.label,
                "Москва, Рублёвское ш, д.18 кор.1"
            )
        }

        "Кликнуть на адрес самовывоза фармы Кликнуть на таб Курьер".ybm_run { _ in
            checkoutPresetSelectorPage = checkoutPage.outletChooserButton().tap()
            XCTAssertFalse(checkoutPresetSelectorPage.outletDetailsCell().element.isVisible)

            XCTAssertEqual(
                checkoutPresetSelectorPage.noSuitableTitle,
                "Среди ваших пунктов самовывоза нет подходящих. Выберите доступный или добавьте новый."
            )
        }
    }

    func testAddressEditingWithKGT() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4827")
        Allure.addEpic("Пресеты 2.0")
        Allure.addFeature("Мультикорзина с КГТ")
        Allure.addTitle("Редактирование адреса")

        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!
        var checkoutPresetSelectorPage: CheckoutPresetSelectorPage!

        setupEnvironment(with: "Preset_AddressEditingWithKGT")

        "Открываем корзину и нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            cartPage = goToCart()
            wait(forExistanceOf: cartPage.cartItem(at: 0).element)

            checkoutPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: checkoutPage.element)

            XCTAssertEqual(
                checkoutPage.addressChooserButton().title.label,
                "Курьером\n" + "Москва, Красная площадь, д. 7"
            )
        }

        "Кликаем на предвыбранный адрес, затем кликаем на иконку \"знака вопроса\"".ybm_run { _ in
            checkoutPresetSelectorPage = checkoutPage.addressChooserButton().tap()

            XCTAssertEqual(
                checkoutPresetSelectorPage.addressDetailsCell(at: 1).textView.label,
                "Недоступен для этих товаров "
            )

            let checkoutShipmentPopupPage = checkoutPresetSelectorPage.addressDetailsCell(at: 1).tap()
            XCTAssertEqual(checkoutShipmentPopupPage.shipmentItems().count, 1)
            XCTAssertEqual(
                checkoutShipmentPopupPage.shipmentItem(at: 0).title.label,
                "Холодильник Weissgauff WCD 337 NFX"
            )

            checkoutShipmentPopupPage.element.swipeDown()
        }

        "Редактируем адрес \"Кстово, площадь Ленина 4А\" на адрес \"Москва, Красная площадь д7\"".ybm_run { _ in
            let editAddressPage = checkoutPresetSelectorPage.addressCell(at: 1).tapEdit()
            editAddressPage.addressCellView.tap()

            let mapViewPage = CheckoutMapViewPage.current

            ybm_wait { mapViewPage.summary.doneButton.isEnabled }

            mapViewPage.summary.tap()

            editAddressPage.continueButton.tap()

            XCTAssertFalse(checkoutPresetSelectorPage.addressDetailsCell(at: 1).element.isVisible)

            checkoutPresetSelectorPage.element.swipeDown()
            XCTAssertEqual(
                checkoutPage.addressChooserButton().title.label,
                "Курьером\n" + "Москва, Красная площадь, д. 7"
            )
        }
    }

    func testChangeOrderType() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4828")
        Allure.addEpic("Пресеты 2.0")
        Allure.addFeature("Повторный заказ")
        Allure.addTitle("Изменение типа заказа")

        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!
        var checkoutPresetSelectorPage: CheckoutPresetSelectorPage!

        setupEnvironment(with: "Preset_ChangeDeliveryType")

        "Открываем корзину и нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            cartPage = goToCart()
            wait(forExistanceOf: cartPage.cartItem(at: 0).element)

            checkoutPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: checkoutPage.element)

            XCTAssertEqual(
                checkoutPage.addressChooserButton().title.label,
                "Курьером\n" + "Москва, Красная площадь, д. 7"
            )
        }

        "Кликаем на предвыбранный адрес доставки.".ybm_run { _ in
            checkoutPresetSelectorPage = checkoutPage.addressChooserButton().tap()
            XCTAssertEqual(checkoutPresetSelectorPage.headerStackView.selectedChipButton.label, "Курьер")

            checkoutPresetSelectorPage.headerStackView.selectedChipButton(at: 0).tap()
            XCTAssertEqual(
                checkoutPresetSelectorPage.outletDetailsCell(at: 1).textView.label,
                "Недоступен для посылки "
            )
            let checkoutShipmentPopupPage = checkoutPresetSelectorPage.outletDetailsCell(at: 1).tap()
            XCTAssertEqual(
                checkoutShipmentPopupPage.shipmentItem(at: 0).title.label,
                "Планшет Apple iPad Pro 11 (2020) 128Gb Wi-Fi, space gray"
            )
            checkoutShipmentPopupPage.element.swipeDown()
        }

        "Выбираем доступный пункт самовывоза".ybm_run { _ in
            checkoutPresetSelectorPage.outletCell(at: 0).element.tap()
            checkoutPresetSelectorPage.doneButton.tap()

            XCTAssertEqual(checkoutPage.outletChooserButton().titleLabel.label, "5 Post (Пятерочка)")
            XCTAssertEqual(
                checkoutPage.outletChooserButton().addressLabel.label,
                "Москва, Комсомольский пр-кт, д. 45"
            )
        }
    }

    func testAddNewAddress() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4829")
        Allure.addEpic("Пресеты 2.0")
        Allure.addFeature("Товар недоступный на самовывоз и доставку почтой")
        Allure.addTitle("Добавление нового адреса")

        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!
        var checkoutPresetSelectorPage: CheckoutPresetSelectorPage!
        var editAddressPage: EditAddressPage!

        let addressId = "23440c81-d33e-4ce3-81b7-a9b0e15450b8"
        var checkoutState = CheckoutState()

        setupEnvironment(with: "Preset_AddNewAddress")

        "Открываем корзину и нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            checkoutState.setUserAddressState(id: addressId, handlers: [.add, .delete, .update])
            stateManager?.setState(newState: checkoutState)

            cartPage = goToCart()
            wait(forExistanceOf: cartPage.cartItem(at: 0).element)

            checkoutPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: checkoutPage.element)

            XCTAssertEqual(
                checkoutPage.addressChooserButton().title.label,
                "Курьером\n" + "Москва, Красная площадь, д. 1"
            )
        }

        "Выбираем адрес Москва, Красная площадь 7 и нажимаем на редактирование".ybm_run { _ in
            checkoutPresetSelectorPage = checkoutPage.addressChooserButton().tap()
            checkoutPresetSelectorPage.headerStackView.selectedChipButton(at: 0).tap()

            let mapViewPage = checkoutPresetSelectorPage.tapAddAddressFromHeader()
            ybm_wait { mapViewPage.summary.doneButton.isEnabled }
            editAddressPage = mapViewPage.summary.tap()

            editAddressPage.continueButton.tap()
            ybm_wait { !editAddressPage.element.isVisible }

            XCTAssertEqual(
                checkoutPage.addressChooserButton().title.label,
                "Курьером\n" + "Москва, Красная площадь, д. 7"
            )

            checkoutPresetSelectorPage = checkoutPage.addressChooserButton().tap()
            XCTAssertFalse(checkoutPresetSelectorPage.addressDetailsCell().element.isVisible)

            editAddressPage = checkoutPresetSelectorPage.addressCell(at: 2).tapEdit()
        }

        "Кликнуть на кнопку удалить".ybm_run { _ in
            editAddressPage.deleteButton.tap()
            app.alerts.firstMatch.descendants(matching: .button).element(boundBy: 0).tap()

            ybm_wait { editAddressPage.deleteButton.isEnabled }
            editAddressPage.deleteButton.tap()
            app.alerts.firstMatch.descendants(matching: .button).element(boundBy: 1).tap()

            wait(forVisibilityOf: checkoutPage.element)
            XCTAssertEqual(checkoutPage.addressChooserButton().title.label, "Выбрать адрес доставки")
            checkoutPresetSelectorPage = checkoutPage.addressChooserButton().tap()

            XCTAssertEqual(
                checkoutPresetSelectorPage.headerStackView.selectedChipButton.label,
                "Курьер"
            )
            XCTAssertNotEqual(
                checkoutPresetSelectorPage.addressCell(at: 0).title.label,
                "Москва, Красная площадь, д. 7"
            )
            XCTAssertNotEqual(
                checkoutPresetSelectorPage.addressCell(at: 1).title.label,
                "Москва, Красная площадь, д. 7"
            )
        }
    }

    func testUnavailableAddressEditing() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4830")
        Allure.addEpic("Пресеты 2.0")
        Allure.addFeature("Повторный заказ на доставку")
        Allure.addTitle("Редактирование недоступного ")

        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!
        var checkoutPresetSelectorPage: CheckoutPresetSelectorPage!
        var editAddressPage: EditAddressPage!

        let fullAddress = "Москва, Красная площадь, д. 1"
        let apart = "34"
        let entrance = "3"
        let intercom = "34K3432"
        let floor = "33"
        let comment = "Don't worry, be happy"

        var userState = UserAuthState()

        "Мокаем новые моки".ybm_run { _ in
            userState.setContactsState(contacts: [.basic])
            stateManager?.setState(newState: userState)
        }

        setupEnvironment(with: "Preset_UnavailableAddressEditing")

        "Открываем корзину и нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            cartPage = goToCart()
            wait(forExistanceOf: cartPage.cartItem(at: 0).element)

            checkoutPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: checkoutPage.element)

            XCTAssertEqual(
                checkoutPage.addressChooserButton().title.label,
                "Курьером\n" + "Москва, Красная площадь, д. 7"
            )
        }

        "Кликаем на предвыбранный адрес доставки.".ybm_run { _ in
            checkoutPresetSelectorPage = checkoutPage.addressChooserButton().tap()
            XCTAssertEqual(checkoutPresetSelectorPage.headerStackView.selectedChipButton.label, "Курьер")

            XCTAssertEqual(
                checkoutPresetSelectorPage.addressDetailsCell(at: 1).textView.label,
                "Недоступен для посылки "
            )
        }

        "Редактируем адрес \"Кстово, площадь Ленина 4А\"".ybm_run { _ in
            editAddressPage = checkoutPresetSelectorPage.addressCell(at: 1).tapEdit()
            editAddressPage.apartmentInput.typeText(apart)
            editAddressPage.entranceInput.typeText(entrance)
            editAddressPage.intercomInput.typeText(intercom)
            editAddressPage.floorInput.typeText(floor)
            editAddressPage.commentInput.typeText(comment)

            KeyboardPage.current.tapDone()
            editAddressPage.continueButton.tap()

            XCTAssertEqual(
                checkoutPresetSelectorPage.addressCell(at: 1).title.label,
                "Кстово, площадь Ленина, д. 4А, кв. 34"
            )

            XCTAssertEqual(
                checkoutPresetSelectorPage.addressDetailsCell(at: 1).textView.label,
                "Недоступен для посылки "
            )
        }

        "Мокаем ручки для изменения адреса".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Preset_UnavailableAddressEditing_changeSecondPreset")
        }

        "Кликаем на адрес для перехода на карту и выбираем доступный адрес".ybm_run { _ in
            editAddressPage = checkoutPresetSelectorPage.addressCell(at: 1).tapEdit()
            editAddressPage.addressCellView.tap()

            let mapViewPage = CheckoutMapViewPage.current

            ybm_wait { mapViewPage.summary.doneButton.isEnabled }
            mapViewPage.summary.tap()

            editAddressPage.continueButton.tap()
        }

        "Проверяем результат".ybm_run { _ in
            XCTAssertEqual(
                checkoutPresetSelectorPage.addressCell(at: 0).title.label,
                "\(fullAddress), кв. \(apart)"
            )
            XCTAssertEqual(
                checkoutPresetSelectorPage.addressDetailsCell(at: 0).textView.label,
                "\(entrance) подъезд, \(floor) этаж, домофон \(intercom), \"\(comment)\""
            )

            checkoutPresetSelectorPage.addressCell(at: 0).element.tap()
            checkoutPresetSelectorPage.doneButton.tap()

            ybm_wait { !checkoutPresetSelectorPage.element.isVisible }
            XCTAssertEqual(
                checkoutPage.addressChooserButton().title.label,
                "Курьером\n" +
                    "\(fullAddress), кв. \(apart)\n\(entrance) подъезд, \(floor) этаж, домофон \(intercom), \"\(comment)\""
            )
        }
    }

    private func setupEnvironment(with bundleName: String, lat: String = "55.752", lon: String = "37.623") {
        app.launchEnvironment[TestLaunchEnvironmentKeys.locationLatitude] = lat
        app.launchEnvironment[TestLaunchEnvironmentKeys.locationLongitude] = lon

        disable(toggles: FeatureNames.cartRedesign)

        "Мокаем ручки".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Experiments_checkPresets")
            mockStateManager?.pushState(bundleName: bundleName)
        }
    }
}
