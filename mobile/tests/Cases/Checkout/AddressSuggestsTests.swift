import FormKit
import MarketUI
import MarketUITestMocks
import XCTest

// swiftlint:disable function_body_length
final class AddressSuggestsTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testSuggestWhenUserHaveNoAddress() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3054")
        Allure.addEpic("Саджесты")
        Allure.addFeature("Саджесты адреса")
        Allure.addTitle("Пользователь без адресов")

        var root: RootPage!
        var cartPage: CartPage!
        var deliveryPage: CheckoutDeliveryPage!
        var paymentMethodPage: CheckoutPaymentMethodPage!
        var editAddressPage: EditAddressPage!

        let street = "улица 50 лет Октября"
        let house = "1"
        let apartment = "2"
        let entrance = "3"
        let intercom = "4"
        let suggest = "улица 50 лет Октября, д. 1, кв. 2"
        let presetAddress = "г. Москва, " + suggest

        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)

        "Мокаем startup для получения эксперимента checkout_fix_exp (3-шаговое флоу чекатута)".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Experiments_checkout_fix_exp")
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Suggests_Empty_Addresses")
        }

        "Авторизируемся и переходим корзину".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            cartPage = goToCart(root: root)
            wait(forExistanceOf: cartPage.threshold.element)
        }

        "Нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            deliveryPage = cartPage.compactSummary.orderButton.tap()
        }

        "Выбираем способ доставки \"Сервис\"".ybm_run { _ in
            deliveryPage.serviceDeliveryTypeCell.tap()
        }

        "Тапаем на поле улица и проверяем отсутствие саджестов".ybm_run { _ in
            deliveryPage.element.swipe(to: .down, untilVisible: deliveryPage.streetInput.element)
            XCTAssertFalse(deliveryPage.streetSuggestView.element.isVisible)
        }

        "Заполняем поля улица, дом, квартира".ybm_run { _ in
            deliveryPage.streetInput.textField.tap()
            deliveryPage.streetInput.textField.typeText(street)
            ybm_wait(forFulfillmentOf: { deliveryPage.streetSuggestView.element.isVisible })
            deliveryPage.streetSuggestView.firstElement.tap()

            deliveryPage.element.swipe(to: .down, untilVisible: deliveryPage.houseInput.element)
            deliveryPage.houseInput.textField.tap()
            deliveryPage.houseInput.textField.typeText(house)
            KeyboardAccessoryPage.current.nextButton.tap()

            deliveryPage.apartmentInput.textField.typeText(apartment)
            deliveryPage.apartmentInput.textField.tap()
            KeyboardAccessoryPage.current.doneButton.tap()

            // ждем прогрузки чекаута
            sleep(3)
        }

        "Раскрываем подъезд, домофон и заполняем их".ybm_run { _ in
            deliveryPage.element.swipe(to: .down, untilVisible: deliveryPage.entranceIntercomExpander)
            deliveryPage.entranceIntercomExpander.tap()

            deliveryPage.element.swipe(to: .down, untilVisible: deliveryPage.entranceInput.element)
            deliveryPage.entranceInput.textField.tap()
            deliveryPage.entranceInput.textField.typeText(entrance)
            KeyboardAccessoryPage.current.nextButton.tap()

            deliveryPage.intercomInput.textField.typeText(intercom)
            KeyboardAccessoryPage.current.doneButton.tap()
        }

        "Переходим на 2ой шаг чекаута".ybm_run { _ in
            deliveryPage.element.swipe(to: .down, untilVisible: deliveryPage.continueButton.element)
            paymentMethodPage = deliveryPage.continueButton.tap()
            ybm_wait(forFulfillmentOf: { paymentMethodPage.element.isVisible })
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Suggests_Address_Suggest")
        }

        "Переходим назад".ybm_run { _ in
            NavigationBarPage.current.backButton.tap()
            ybm_wait(forFulfillmentOf: { deliveryPage.element.isVisible })
        }

        "Скролим к кнопке \"Добавить новый адрес\" и тапаем на него".ybm_run { _ in
            deliveryPage.element.swipe(to: .up, untilVisible: deliveryPage.addNewAddressButton.element)
            editAddressPage = deliveryPage.addNewAddressButton.tap()
            ybm_wait(forFulfillmentOf: { editAddressPage.element.isVisible })
        }

        "Проверяем поля ввода и саджест".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                editAddressPage.cityInput.element.isVisible
                    && editAddressPage.streetInput.element.isVisible
                    && editAddressPage.houseInput.element.isVisible
                    && editAddressPage.apartmentInput.element.isVisible
            })

            editAddressPage.streetInput.textField.tap()

            ybm_wait(forFulfillmentOf: {
                editAddressPage.streetSuggestView.element.isVisible
                    && editAddressPage.streetSuggestView.firstSuggest.textView.text == suggest
                    && editAddressPage.streetSuggestView.firstSuggest.image.identifier == "CheckoutGenericAddress"
            })
        }

        "Выбираем саджест и проверяем заполненность полей".ybm_run { _ in
            editAddressPage.streetSuggestView.firstSuggest.element.tap()

            ybm_wait(forFulfillmentOf: {
                editAddressPage.streetInput.textField.text == street
                    && editAddressPage.houseInput.textField.text == house
                    && editAddressPage.apartmentInput.textField.text == apartment
            })

            editAddressPage.entranceIntercomExpander.tap()

            ybm_wait(forFulfillmentOf: {
                editAddressPage.entranceInput.textField.text == entrance
                    && editAddressPage.intercomInput.textField.text == intercom
            })
        }

        "Сохраняем изменения".ybm_run { _ in
            editAddressPage.element.swipeDown()
            editAddressPage.element.swipe(to: .down, untilVisible: editAddressPage.continueButton)
            editAddressPage.continueButton.tap()
            ybm_wait(forFulfillmentOf: { deliveryPage.element.isVisible })
            // проверяем вызов функции addUserPreset
            ybm_wait(forFulfillmentOf: {
                self.mockServer?.handledRequests.contains { $0.contains("name=addUserPreset") } ?? false
            })
        }

        "Проверяем пресеты на 1ом экране чекаута".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { deliveryPage.element.isVisible })

            func checkPreset(index: Int) {
                let preset = deliveryPage.addressPreset(at: index)
                deliveryPage.element.swipe(to: .down, untilVisible: preset)
                let texts = preset.staticTexts.allElementsBoundByIndex
                ybm_wait(forFulfillmentOf: { texts.contains { $0.label == presetAddress } })
            }

            checkPreset(index: 0)
            checkPreset(index: 1)
        }
    }

    func testSuggestWhenUserHaveHomeWorkSuggests() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3055")
        Allure.addEpic("Саджесты")
        Allure.addFeature("Саджесты адреса")
        Allure.addTitle("Пользователь с адресами из личного кабинета")

        var root: RootPage!
        var cartPage: CartPage!
        var deliveryPage: CheckoutDeliveryPage!
        var paymentMethodPage: CheckoutPaymentMethodPage!
        var summaryPage: CheckoutSummaryPage!

        let street = "улица 50 лет Октября"
        let house = "1"
        let homeSuggestText = "улица 50 лет Октября, д. 1"
        let workSuggestText = "улица Льва Толстого, д. 16"
        let recipientAddress = "г. Москва, " + homeSuggestText

        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)

        "Мокаем startup для получения эксперимента checkout_fix_exp (3-шаговое флоу чекатута)".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Experiments_checkout_fix_exp")
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Suggests_HomeWork_Suggests")
        }

        "Авторизируемся и переходим корзину".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            cartPage = goToCart(root: root)
            wait(forExistanceOf: cartPage.threshold.element)
        }

        "Нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            deliveryPage = cartPage.compactSummary.orderButton.tap()
        }

        "Выбираем способ доставки \"Сервис\"".ybm_run { _ in
            deliveryPage.serviceDeliveryTypeCell.tap()
        }

        "Тапаем на поле улица и проверяем наличие саджестов".ybm_run { _ in
            deliveryPage.element.swipe(to: .down, untilVisible: deliveryPage.streetInput.element)
            deliveryPage.streetInput.textField.tap()
            ybm_wait(forFulfillmentOf: { deliveryPage.streetSuggestView.element.isVisible })
        }

        "Проверяем саджесты дома и работы".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                let homeSuggest = deliveryPage.streetSuggestView.suggest(index: 0)
                return homeSuggest.textView.text == homeSuggestText
                    && homeSuggest.image.identifier == "CheckoutHomeAddress"
            })

            ybm_wait(forFulfillmentOf: {
                let workSuggest = deliveryPage.streetSuggestView.suggest(index: 1)
                return workSuggest.textView.text == workSuggestText
                    && workSuggest.image.identifier == "CheckoutWorkAddress"
            })
        }

        "Выбираем Саджест дома".ybm_run { _ in
            deliveryPage.streetSuggestView.firstElement.tap()
            ybm_wait(forFulfillmentOf: { !deliveryPage.streetSuggestView.element.isVisible })
        }

        "Проверяем заполненность полей Саджетом дома".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                deliveryPage.streetInput.textField.text == street
                    && deliveryPage.houseInput.textField.text == house
                    && deliveryPage.apartmentInput.textField.text.isEmpty
            })

            deliveryPage.element.swipe(to: .down, untilVisible: deliveryPage.entranceIntercomExpander)
            deliveryPage.entranceIntercomExpander.tap()

            deliveryPage.element.swipe(to: .down, untilVisible: deliveryPage.entranceInput.element)
            ybm_wait(forFulfillmentOf: {
                deliveryPage.entranceInput.textField.text.isEmpty
                    && deliveryPage.intercomInput.textField.text.isEmpty
            })
        }

        "Переходим на 2ой шаг чекаута".ybm_run { _ in
            deliveryPage.element.swipe(to: .down, untilVisible: deliveryPage.continueButton.element)
            paymentMethodPage = deliveryPage.continueButton.tap()
            ybm_wait(forFulfillmentOf: { paymentMethodPage.element.isVisible })
        }

        "Выбираем способ оплаты наличными для успешной оплаты".ybm_run { _ in
            let cashPaymentMethodCell = paymentMethodPage.paymentMethod(at: 2)
            cashPaymentMethodCell.element.tap()
        }

        "Переходим в саммари чекаута".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { paymentMethodPage.element.isVisible })
            paymentMethodPage.element.swipe(
                to: .down,
                untilVisible: paymentMethodPage.continueButton.element
            )
            summaryPage = paymentMethodPage.continueButton.tap()
            wait(forVisibilityOf: summaryPage.element)
        }

        "Проверяем адрес получателя".ybm_run { _ in
            summaryPage.element.swipe(to: .down, untilVisible: summaryPage.recipientAddress)
            ybm_wait(forFulfillmentOf: {
                summaryPage.recipientAddress.textViews.firstMatch.text.contains(recipientAddress)
            })
        }
    }

    func testSuggestWhenDeletingDeliveryAddress() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3056")
        Allure.addEpic("Саджесты")
        Allure.addFeature("Саджесты адреса")
        Allure.addTitle("Удаление адреса")

        var root: RootPage!
        var cartPage: CartPage!
        var deliveryPage: CheckoutDeliveryPage!
        var editAddressPage: EditAddressPage!

        let homeSuggestText = "улица 50 лет Октября, д. 1"
        let workSuggestText = "улица Льва Толстого, д. 16"
        let addressSuggestText1 = "улица Маршала Баграмяна, д. 1, кв. 2"
        let addressSuggestText2 = "Производственная улица, д. 1, кв. 2"

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Suggests_With_Addresses")
        }

        "Авторизируемся и переходим корзину".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            cartPage = goToCart(root: root)
            wait(forExistanceOf: cartPage.threshold.element)
        }

        "Нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            deliveryPage = cartPage.compactSummary.orderButton.tap()
        }

        "Выбираем способ доставки \"Сервис\"".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                deliveryPage.serviceDeliveryTypeCell.ybm_safeTap()
                return deliveryPage.serviceDeliveryTypeCell.images.firstMatch
                    .identifier == RadioButtonViewCellAccessibility.Selectability.selected
            })
        }

        "Скролим к кнопке \"Добавить новый адрес\" и тапаем на него".ybm_run { _ in
            deliveryPage.element.swipe(to: .down, untilVisible: deliveryPage.addNewAddressButton.element)
            editAddressPage = deliveryPage.addNewAddressButton.tap()
            ybm_wait(forFulfillmentOf: { editAddressPage.element.isVisible })
        }

        "Проверяем поля ввода".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                editAddressPage.cityInput.element.isVisible
                    && editAddressPage.streetInput.element.isVisible
                    && editAddressPage.houseInput.element.isVisible
                    && editAddressPage.apartmentInput.element.isVisible
            })
        }

        "Проверяем саджесты".ybm_run { _ in
            editAddressPage.streetInput.textField.tap()
            ybm_wait(forFulfillmentOf: { editAddressPage.streetSuggestView.element.isVisible })

            func checkSuggest(index: Int, text: String, imageId: String) {
                ybm_wait(forFulfillmentOf: {
                    let suggest = editAddressPage.streetSuggestView.suggest(index: index)
                    return suggest.textView.text == text
                        && suggest.image.identifier == imageId
                })
            }

            checkSuggest(index: 0, text: homeSuggestText, imageId: "CheckoutHomeAddress")
            checkSuggest(index: 1, text: workSuggestText, imageId: "CheckoutWorkAddress")
            checkSuggest(index: 2, text: addressSuggestText1, imageId: "CheckoutGenericAddress")
            checkSuggest(index: 3, text: addressSuggestText2, imageId: "CheckoutGenericAddress")
            XCTAssertFalse(editAddressPage.streetSuggestView.suggest(index: 4).element.isVisible)
        }

        "Вводим несколько букв в поле улицы, встречающиеся в саджесте и проверяем выдачу".ybm_run { _ in
            let textInput = "Багра"
            editAddressPage.streetInput.textField.typeText(textInput)

            ybm_wait(forFulfillmentOf: {
                editAddressPage.streetSuggestView.allAddressess.allSatisfy { $0.textView.label.contains(textInput) }
                    && editAddressPage.streetSuggestView.allAddressess.count == 3
            })

            ybm_wait(forFulfillmentOf: {
                let suggests = editAddressPage.streetSuggestView.allSuggests
                return suggests.count == 1
                    && suggests.first?.textView.label == addressSuggestText1
                    && suggests.first?.image.identifier == "CheckoutGenericAddress"
            })
        }

        "Переходим назад".ybm_run { _ in
            NavigationBarPage.current.backButton.tap()
            ybm_wait(forFulfillmentOf: { deliveryPage.element.isVisible })
        }

        "Редактируем первый пресет".ybm_run { _ in
            let firstPreset = deliveryPage.addressPreset()

            deliveryPage.collectionView.swipe(to: .up, untilVisible: firstPreset)

            firstPreset.buttons.firstMatch.tap()
            ybm_wait(forFulfillmentOf: { editAddressPage.element.isVisible })
        }

        "Мокаем состояние c удаленным адресом".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Delete_Address_Deleted")
        }

        "Удаляем пресет".ybm_run { _ in
            self.mockServer?.handledRequests.removeAll() // очищаем хранилище запросов
            NavigationBarPage.current.deleteButton.tap()
            app.buttons["Удалить"].firstMatch.tap()
            ybm_wait(forFulfillmentOf: { deliveryPage.element.isVisible })
            // проверяем вызов функции deleteUserPreset
            ybm_wait(forFulfillmentOf: {
                self.mockServer?.handledRequests.contains { $0.contains("name=deleteUserPreset") } ?? false
            })
        }

        "Скролим снова к кнопке \"Добавить новый адрес\" и тапаем на него".ybm_run { _ in
            deliveryPage.element.swipe(to: .down, untilVisible: deliveryPage.addNewAddressButton.element)
            editAddressPage = deliveryPage.addNewAddressButton.tap()
            ybm_wait(forFulfillmentOf: { editAddressPage.element.isVisible })
        }

        "Проверяем саджесты".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { editAddressPage.streetInput.element.isVisible })
            editAddressPage.streetInput.textField.tap()
            ybm_wait(forFulfillmentOf: { editAddressPage.streetSuggestView.element.isVisible })

            func checkSuggest(index: Int, text: String, imageId: String) {
                ybm_wait(forFulfillmentOf: {
                    let suggest = editAddressPage.streetSuggestView.suggest(index: index)
                    return suggest.textView.text == text
                        && suggest.image.identifier == imageId
                })
            }

            checkSuggest(index: 0, text: homeSuggestText, imageId: "CheckoutHomeAddress")
            checkSuggest(index: 1, text: workSuggestText, imageId: "CheckoutWorkAddress")
            checkSuggest(index: 2, text: addressSuggestText1, imageId: "CheckoutGenericAddress")
            XCTAssertFalse(editAddressPage.streetSuggestView.suggest(index: 3).element.isVisible)
        }
    }

    func testSuggestWhenAddingDeliveryAddress() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3057")
        Allure.addEpic("Саджесты")
        Allure.addFeature("Саджесты адреса")
        Allure.addTitle("Добавить адрес")

        var root: RootPage!
        var cartPage: CartPage!
        var deliveryPage: CheckoutDeliveryPage!
        var editAddressPage: EditAddressPage!

        let addStreet = "проспект Маршала Жукова"
        let addHouse = "1"
        let addApartment = "2"
        let homeSuggestText = "улица 50 лет Октября, д. 1"
        let workSuggestText = "улица Льва Толстого, д. 16"
        let addressSuggestText1 = "улица Маршала Баграмяна, д. 1, кв. 2"
        let addressSuggestText2 = "Производственная улица, д. 1, кв. 2"
        let addressSuggestText3 = "\(addStreet), д. \(addHouse), кв. \(addApartment)"

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Suggests_With_Addresses")
        }

        "Авторизируемся и переходим корзину".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            cartPage = goToCart(root: root)
            wait(forExistanceOf: cartPage.threshold.element)
        }

        "Нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            deliveryPage = cartPage.compactSummary.orderButton.tap()
            ybm_wait(forFulfillmentOf: { deliveryPage.element.isVisible })
        }

        "Выбираем способ доставки \"Сервис\"".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                deliveryPage.serviceDeliveryTypeCell.ybm_safeTap()
                return deliveryPage.serviceDeliveryTypeCell.images.firstMatch
                    .identifier == RadioButtonViewCellAccessibility.Selectability.selected
            })
        }

        "Скролим к кнопке \"Добавить новый адрес\" и тапаем на него".ybm_run { _ in
            deliveryPage.element.swipe(to: .down, untilVisible: deliveryPage.addNewAddressButton.element)
            editAddressPage = deliveryPage.addNewAddressButton.tap()
            ybm_wait(forFulfillmentOf: { editAddressPage.element.isVisible })
        }

        "Проверяем поля ввода".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                editAddressPage.cityInput.element.isVisible
                    && editAddressPage.streetInput.element.isVisible
                    && editAddressPage.houseInput.element.isVisible
                    && editAddressPage.apartmentInput.element.isVisible
            })
        }

        "Проверяем саджесты".ybm_run { _ in
            editAddressPage.streetInput.textField.tap()
            ybm_wait(forFulfillmentOf: { editAddressPage.streetSuggestView.element.isVisible })

            func checkSuggest(index: Int, text: String, imageId: String) {
                ybm_wait(forFulfillmentOf: {
                    let suggest = editAddressPage.streetSuggestView.suggest(index: index)
                    return suggest.textView.text == text
                        && suggest.image.identifier == imageId
                })
            }

            checkSuggest(index: 0, text: homeSuggestText, imageId: "CheckoutHomeAddress")
            checkSuggest(index: 1, text: workSuggestText, imageId: "CheckoutWorkAddress")
            checkSuggest(index: 2, text: addressSuggestText1, imageId: "CheckoutGenericAddress")
            checkSuggest(index: 3, text: addressSuggestText2, imageId: "CheckoutGenericAddress")
            XCTAssertFalse(editAddressPage.streetSuggestView.suggest(index: 4).element.isVisible)
        }

        "Добавляем новый адресс".ybm_run { _ in
            editAddressPage.streetInput.textField.typeText(addStreet)
            editAddressPage.streetSuggestView.firstElement.tap()

            editAddressPage.element.swipe(to: .down, untilVisible: editAddressPage.houseInput.element)
            editAddressPage.houseInput.textField.tap()
            editAddressPage.houseInput.textField.typeText(addHouse)
            KeyboardAccessoryPage.current.nextButton.tap()

            editAddressPage.apartmentInput.textField.typeText(addApartment)
            KeyboardAccessoryPage.current.doneButton.tap()
        }

        "Мокаем состояние c добавленным адресом".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Add_Address_Added")
        }

        "Сохраняем пресет".ybm_run { _ in
            self.mockServer?.handledRequests.removeAll() // очищаем хранилище запросов

            editAddressPage.element.swipe(to: .down, untilVisible: editAddressPage.continueButton)
            editAddressPage.continueButton.tap()
            ybm_wait(forFulfillmentOf: { deliveryPage.element.isVisible })

            // проверяем вызов функции addUserPreset
            ybm_wait(forFulfillmentOf: {
                self.mockServer?.handledRequests.contains { $0.contains("name=addUserPreset") } ?? false
            })
        }

        "Скролим снова к кнопке \"Добавить новый адрес\" и тапаем на него".ybm_run { _ in
            deliveryPage.element.swipe(to: .down, untilVisible: deliveryPage.addNewAddressButton.element)
            editAddressPage = deliveryPage.addNewAddressButton.tap()
            ybm_wait(forFulfillmentOf: { editAddressPage.element.isVisible })
        }

        "Проверяем саджесты".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { editAddressPage.streetInput.element.isVisible })
            editAddressPage.streetInput.textField.tap()
            ybm_wait(forFulfillmentOf: { editAddressPage.streetSuggestView.element.isVisible })

            func checkSuggest(index: Int, text: String, imageId: String) {
                ybm_wait(forFulfillmentOf: {
                    let suggest = editAddressPage.streetSuggestView.suggest(index: index)
                    return suggest.textView.text == text
                        && suggest.image.identifier == imageId
                })
            }

            checkSuggest(index: 0, text: homeSuggestText, imageId: "CheckoutHomeAddress")
            checkSuggest(index: 1, text: workSuggestText, imageId: "CheckoutWorkAddress")
            checkSuggest(index: 2, text: addressSuggestText1, imageId: "CheckoutGenericAddress")
            checkSuggest(index: 3, text: addressSuggestText2, imageId: "CheckoutGenericAddress")
            checkSuggest(index: 4, text: addressSuggestText3, imageId: "CheckoutGenericAddress")
            XCTAssertFalse(editAddressPage.streetSuggestView.suggest(index: 5).element.isVisible)
        }
    }

    func testSuggestWithDifferenceRegions() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3058")
        Allure.addEpic("Саджесты")
        Allure.addFeature("Саджесты адреса")
        Allure.addTitle("Разные регионы")

        var root: RootPage!
        var cartPage: CartPage!
        var deliveryPage: CheckoutDeliveryPage!
        var editAddressPage: EditAddressPage!

        let togliattiCity = "Тольятти"
        let sochiCity = "Сочи"
        let moscowHomeSuggestText = "улица 50 лет Октября, д. 1"
        let moscowWorkSuggestText = "улица Льва Толстого, д. 16"
        let moscowAddressSuggestText = "улица Маршала Баграмяна, д. 1, кв. 2"
        let togliattiAddressSuggestText1 = "бульвар 50 лет Октября, д. 63, кв. 69"
        let togliattiAddressSuggestText2 = "улица Карла Маркса, д. 59, кв. 2"

        func checkSuggest(index: Int, text: String, imageId: String) {
            ybm_wait(forFulfillmentOf: {
                let suggest = editAddressPage.streetSuggestView.suggest(index: index)
                return suggest.textView.text == text
                    && suggest.image.identifier == imageId
            })
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Difference_Regions_Suggest_Moscow")
        }

        "Авторизируемся и переходим корзину".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            cartPage = goToCart(root: root)
            wait(forExistanceOf: cartPage.threshold.element)
        }

        "Нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            deliveryPage = cartPage.compactSummary.orderButton.tap()
        }

        "Выбираем способ доставки \"Сервис\"".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                deliveryPage.serviceDeliveryTypeCell.ybm_safeTap()
                return deliveryPage.serviceDeliveryTypeCell.images.firstMatch
                    .identifier == RadioButtonViewCellAccessibility.Selectability.selected
            })
        }

        "Скролим к кнопке \"Добавить новый адрес\" и тапаем на него".ybm_run { _ in
            deliveryPage.element.swipe(to: .down, untilVisible: deliveryPage.addNewAddressButton.element)
            editAddressPage = deliveryPage.addNewAddressButton.tap()
            ybm_wait(forFulfillmentOf: { editAddressPage.element.isVisible })
        }

        "Проверяем саджесты в Москве".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { editAddressPage.streetInput.element.isVisible })
            editAddressPage.streetInput.textField.tap()
            ybm_wait(forFulfillmentOf: { editAddressPage.streetSuggestView.element.isVisible })

            checkSuggest(index: 0, text: moscowHomeSuggestText, imageId: "CheckoutHomeAddress")
            checkSuggest(index: 1, text: moscowWorkSuggestText, imageId: "CheckoutWorkAddress")
            checkSuggest(index: 2, text: moscowAddressSuggestText, imageId: "CheckoutGenericAddress")
            XCTAssertFalse(editAddressPage.streetSuggestView.suggest(index: 3).element.isVisible)
        }

        "Мокаем состояние адресов в Тольятти".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Difference_Regions_Suggest_Togliatti")
        }

        "Меняем регион на Тольятти".ybm_run { _ in
            editAddressPage.element.swipe(to: .up, untilVisible: editAddressPage.cityInput.element)
            editAddressPage.cityInput.textField.ybm_clearAndEnterText(togliattiCity)
            ybm_wait(forFulfillmentOf: { editAddressPage.citySuggestView.firstElement.isVisible })
            editAddressPage.citySuggestView.firstElement.tap()
        }

        "Проверяем саджесты в Тольятти".ybm_run { _ in
            editAddressPage.element.swipe(to: .down, untilVisible: editAddressPage.streetInput.element)
            editAddressPage.streetInput.textField.tap()
            ybm_wait(forFulfillmentOf: { editAddressPage.streetSuggestView.element.isVisible })

            checkSuggest(index: 0, text: togliattiAddressSuggestText1, imageId: "CheckoutGenericAddress")
            checkSuggest(index: 1, text: togliattiAddressSuggestText2, imageId: "CheckoutGenericAddress")
            XCTAssertFalse(editAddressPage.streetSuggestView.suggest(index: 2).element.isVisible)
        }

        "Мокаем состояние адресов в Сочи".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Difference_Regions_Suggest_Sochi")
        }

        "Меняем регион на Сочи".ybm_run { _ in
            editAddressPage.element.swipe(to: .up, untilVisible: editAddressPage.cityInput.element)
            editAddressPage.cityInput.textField.ybm_clearAndEnterText(sochiCity)
            ybm_wait(forFulfillmentOf: { editAddressPage.citySuggestView.firstElement.isVisible })
            editAddressPage.citySuggestView.firstElement.tap()
        }

        "Проверяем саджесты в Сочи".ybm_run { _ in
            editAddressPage.element.swipe(to: .down, untilVisible: editAddressPage.streetInput.element)
            editAddressPage.streetInput.textField.tap()
            XCTAssertFalse(editAddressPage.streetSuggestView.element.isVisible)
        }
    }

    func testSuggestWithMultiDelivery() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3059")
        Allure.addEpic("Саджесты")
        Allure.addFeature("Саджесты адреса")
        Allure.addTitle("Чекаут МСЦ")

        var root: RootPage!
        var cartPage: CartPage!
        var deliveryPage: CheckoutDeliveryPage!
        var editAddressPage: EditAddressPage!
        var paymentMethodPage: CheckoutPaymentMethodPage!
        var summaryPage: CheckoutSummaryPage!

        let firstStreet = "улица Суворова"
        let firstHouse = "1"
        let firstApartment = "2"
        let secondStreet = "улица Тимура Фрунзе"
        let secondHouse = "3"
        let secondApartment = "4"
        let homeSuggestText = "улица 50 лет Октября, д. 1"
        let workSuggestText = "улица Льва Толстого, д. 16"
        let addressSuggestText2 = "улица Маршала Баграмяна, д. 1, кв. 2"
        let addressSuggestText3 = "\(firstStreet), д. \(firstHouse), кв. \(firstApartment)"
        let addressSuggestText4 = "\(secondStreet), д. \(secondHouse), кв. \(secondApartment)"

        func checkSuggest(index: Int, text: String, imageId: String) {
            ybm_wait(forFulfillmentOf: {
                let suggest = editAddressPage.streetSuggestView.suggest(index: index)
                return suggest.textView.text == text
                    && suggest.image.identifier == imageId
            })
        }

        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)

        "Мокаем startup для получения эксперимента checkout_fix_exp (3-шаговое флоу чекатута)".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Experiments_checkout_fix_exp")
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Multi_Suggests")
        }

        "Авторизируемся и переходим корзину".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            cartPage = goToCart(root: root)
            wait(forExistanceOf: cartPage.threshold.element)
        }

        "Нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            deliveryPage = cartPage.compactSummary.orderButton.tap()
            ybm_wait(forFulfillmentOf: { deliveryPage.element.isVisible })
        }

        "Выбираем способ доставки \"Сервис\"".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                deliveryPage.serviceDeliveryTypeCell.ybm_safeTap()
                return deliveryPage.serviceDeliveryTypeCell.images.firstMatch
                    .identifier == RadioButtonViewCellAccessibility.Selectability.selected
            })
        }

        "Скролим к кнопке \"Раздельная доставка\" и тапаем на него".ybm_run { _ in
            deliveryPage.element.swipe(to: .down, untilVisible: deliveryPage.separatedDeliveryControl)
            deliveryPage.separatedDeliveryControl.tap()
            ybm_wait(forFulfillmentOf: { !deliveryPage.separatedDeliveryControl.isVisible })
        }

        "Редактируем первый пресет".ybm_run { _ in
            let firstPreset = deliveryPage.addressPreset()

            deliveryPage.collectionView.swipe(to: .down, untilVisible: firstPreset)

            firstPreset.buttons.firstMatch.tap()
            ybm_wait(forFulfillmentOf: { EditAddressPage.current.element.isVisible })
            editAddressPage = EditAddressPage.current
        }

        "Мокаем состояние c удаленным адресом".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Multi_Suggests_Deleted")
        }

        "Удаляем пресет".ybm_run { _ in
            self.mockServer?.handledRequests.removeAll() // очищаем хранилище запросов

            NavigationBarPage.current.deleteButton.tap()
            app.buttons["Удалить"].firstMatch.tap()
            ybm_wait(forFulfillmentOf: { deliveryPage.element.isVisible })

            // проверяем вызов функции deleteUserPreset
            ybm_wait(forFulfillmentOf: {
                self.mockServer?.handledRequests.contains { $0.contains("name=deleteUserPreset") } ?? false
            })
        }

        "Скролим к кнопке \"Добавить новый адрес\" и тапаем на него".ybm_run { _ in
            deliveryPage.element.swipe(to: .down, untilVisible: deliveryPage.addNewAddressButton.element)
            editAddressPage = deliveryPage.addNewAddressButton.tap()
            ybm_wait(forFulfillmentOf: { editAddressPage.element.isVisible })
        }

        "Добавляем новый пресет".ybm_run { _ in
            editAddressPage.collectionView.swipe(to: .down, untilVisible: editAddressPage.streetInput.element)
            editAddressPage.streetInput.textField.tap()
            editAddressPage.streetInput.textField.typeText(firstStreet)
            editAddressPage.streetSuggestView.firstElement.tap()

            editAddressPage.element.swipe(to: .down, untilVisible: editAddressPage.houseInput.element)
            editAddressPage.houseInput.textField.tap()
            editAddressPage.houseInput.textField.typeText(firstHouse)
            KeyboardAccessoryPage.current.nextButton.tap()

            editAddressPage.apartmentInput.textField.typeText(firstApartment)
            KeyboardAccessoryPage.current.doneButton.tap()
        }

        "Мокаем состояние c добавленным адресом".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Multi_Suggests_AddedFirst")
        }

        "Сохраняем пресет".ybm_run { _ in
            self.mockServer?.handledRequests.removeAll() // очищаем хранилище запросов

            editAddressPage.element.swipe(to: .down, untilVisible: editAddressPage.continueButton)
            editAddressPage.continueButton.tap()
            ybm_wait(forFulfillmentOf: { deliveryPage.element.isVisible })

            // проверяем вызов функции addUserPreset
            ybm_wait(forFulfillmentOf: {
                self.mockServer?.handledRequests.contains { $0.contains("name=addUserPreset") } ?? false
            })
        }

        "Переходим ко 2ой посылке".ybm_run { _ in
            deliveryPage.element.swipe(to: .down, untilVisible: deliveryPage.continueButton.element)
            deliveryPage.continueButton.element.tap()
            ybm_wait(forFulfillmentOf: { CheckoutDeliveryPage.current.element.isVisible })
            deliveryPage = CheckoutDeliveryPage.current
        }

        "Скролим к кнопке \"Добавить новый адрес\" и тапаем на него".ybm_run { _ in
            deliveryPage.element.swipe(to: .down, untilVisible: deliveryPage.addNewAddressButton.element)
            editAddressPage = deliveryPage.addNewAddressButton.tap()
            ybm_wait(forFulfillmentOf: { editAddressPage.element.isVisible })
        }

        "Проверяем саджесты".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { editAddressPage.streetInput.element.isVisible })
            editAddressPage.streetInput.textField.tap()
            ybm_wait(forFulfillmentOf: { editAddressPage.streetSuggestView.element.isVisible })

            checkSuggest(index: 0, text: homeSuggestText, imageId: "CheckoutHomeAddress")
            checkSuggest(index: 1, text: workSuggestText, imageId: "CheckoutWorkAddress")
            checkSuggest(index: 2, text: addressSuggestText2, imageId: "CheckoutGenericAddress")
            checkSuggest(index: 3, text: addressSuggestText3, imageId: "CheckoutGenericAddress")
            XCTAssertFalse(editAddressPage.streetSuggestView.suggest(index: 4).element.isVisible)
        }

        "Добавляем новый пресет".ybm_run { _ in
            editAddressPage.streetInput.textField.typeText(secondStreet)
            editAddressPage.streetSuggestView.firstElement.tap()

            editAddressPage.element.swipe(to: .down, untilVisible: editAddressPage.houseInput.element)
            editAddressPage.houseInput.textField.tap()
            editAddressPage.houseInput.textField.typeText(secondHouse)
            KeyboardAccessoryPage.current.nextButton.tap()

            editAddressPage.apartmentInput.textField.typeText(secondApartment)
            KeyboardAccessoryPage.current.doneButton.tap()
        }

        "Мокаем состояние c добавленным адресом".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Multi_Suggests_AddedSecond")
        }

        "Сохраняем пресет".ybm_run { _ in
            self.mockServer?.handledRequests.removeAll() // очищаем хранилище запросов

            editAddressPage.element.swipe(to: .down, untilVisible: editAddressPage.continueButton)
            editAddressPage.continueButton.tap()
            ybm_wait(forFulfillmentOf: { deliveryPage.element.isVisible })

            // проверяем вызов функции addUserPreset
            ybm_wait(forFulfillmentOf: {
                self.mockServer?.handledRequests.contains { $0.contains("name=addUserPreset") } ?? false
            })
        }

        "Переходим на экран способа оплаты".ybm_run { _ in
            deliveryPage.element.swipe(to: .down, untilVisible: deliveryPage.continueButton.element)
            paymentMethodPage = deliveryPage.continueButton.tap()
            ybm_wait(forFulfillmentOf: { paymentMethodPage.element.isVisible })
        }

        "Выбираем способ оплаты наличными для успешной оплаты".ybm_run { _ in
            let cashPaymentMethodCell = paymentMethodPage.paymentMethod(at: 2)
            cashPaymentMethodCell.element.tap()
        }

        "Переходим в саммари чекаута".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { paymentMethodPage.element.isVisible })
            paymentMethodPage.element.ybm_swipeCollectionView(
                to: .down,
                toFullyReveal: paymentMethodPage.continueButton.element
            )
            summaryPage = paymentMethodPage.continueButton.tap()
            wait(forVisibilityOf: summaryPage.element)
        }

        "Скролим к кнопке \"Изменить\" у доставки первой посылки".ybm_run { _ in
            summaryPage.element.swipe(to: .down, untilVisible: summaryPage.deliveryChangeControl)
            summaryPage.deliveryChangeControl.tap()
            ybm_wait(forFulfillmentOf: { CheckoutDeliveryPage.current.element.isVisible })
            deliveryPage = CheckoutDeliveryPage.current
        }

        "Скролим к кнопке \"Добавить новый адрес\" и тапаем на него".ybm_run { _ in
            deliveryPage.element.swipe(to: .up, untilVisible: deliveryPage.addNewAddressButton.element)
            editAddressPage = deliveryPage.addNewAddressButton.tap()
            ybm_wait(forFulfillmentOf: { editAddressPage.element.isVisible })
        }

        "Проверяем саджесты".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { editAddressPage.streetInput.element.isVisible })
            editAddressPage.streetInput.textField.tap()
            ybm_wait(forFulfillmentOf: { editAddressPage.streetSuggestView.element.isVisible })

            func checkSuggest(index: Int, text: String, imageId: String) {
                ybm_wait(forFulfillmentOf: {
                    let suggest = editAddressPage.streetSuggestView.suggest(index: index)
                    return suggest.textView.text == text
                        && suggest.image.identifier == imageId
                })
            }

            checkSuggest(index: 0, text: homeSuggestText, imageId: "CheckoutHomeAddress")
            checkSuggest(index: 1, text: workSuggestText, imageId: "CheckoutWorkAddress")
            checkSuggest(index: 2, text: addressSuggestText2, imageId: "CheckoutGenericAddress")
            checkSuggest(index: 3, text: addressSuggestText4, imageId: "CheckoutGenericAddress")
            checkSuggest(index: 4, text: addressSuggestText3, imageId: "CheckoutGenericAddress")
            XCTAssertFalse(editAddressPage.streetSuggestView.suggest(index: 5).element.isVisible)
        }
    }

    func testSuggestWhenAddingDeliveryAddressWithJustHouse() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3060")
        Allure.addEpic("Саджесты")
        Allure.addFeature("Саджесты адреса")
        Allure.addTitle("В адресе только дом")

        var root: RootPage!
        var cartPage: CartPage!
        var deliveryPage: CheckoutDeliveryPage!
        var editAddressPage: EditAddressPage!

        let addHouse = "1"
        let homeSuggestText = "улица 50 лет Октября, д. 1"
        let workSuggestText = "улица Льва Толстого, д. 16"
        let addressSuggestText1 = "улица Маршала Баграмяна, д. 1, кв. 2"
        let addressSuggestText2 = "Производственная улица, д. 1, кв. 2"
        let addressSuggestText3 = "д. \(addHouse)"

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Suggests_With_Addresses")
        }

        "Авторизируемся и переходим корзину".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            cartPage = goToCart(root: root)
            wait(forExistanceOf: cartPage.threshold.element)
        }

        "Нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            deliveryPage = cartPage.compactSummary.orderButton.tap()
            ybm_wait(forFulfillmentOf: { deliveryPage.element.isVisible })
        }

        "Выбираем способ доставки \"Сервис\"".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                deliveryPage.serviceDeliveryTypeCell.ybm_safeTap()
                return deliveryPage.serviceDeliveryTypeCell.images.firstMatch
                    .identifier == RadioButtonViewCellAccessibility.Selectability.selected
            })
        }

        "Скролим к кнопке \"Добавить новый адрес\" и тапаем на него".ybm_run { _ in
            deliveryPage.element.swipe(to: .down, untilVisible: deliveryPage.addNewAddressButton.element)
            editAddressPage = deliveryPage.addNewAddressButton.tap()
            ybm_wait(forFulfillmentOf: { editAddressPage.element.isVisible })
        }

        "Добавляем новый адресс с одним лишь номером дома".ybm_run { _ in
            editAddressPage.element.swipe(to: .down, untilVisible: editAddressPage.houseInput.element)
            editAddressPage.houseInput.textField.tap()
            editAddressPage.houseInput.textField.typeText(addHouse)
            KeyboardAccessoryPage.current.doneButton.tap()
        }

        "Мокаем состояние c добавленным адресом".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Add_Address_With_Just_House_Added")
        }

        "Сохраняем пресет".ybm_run { _ in
            self.mockServer?.handledRequests.removeAll() // очищаем хранилище запросов

            editAddressPage.element.swipe(to: .down, untilVisible: editAddressPage.continueButton)
            editAddressPage.continueButton.tap()
            ybm_wait(forFulfillmentOf: { deliveryPage.element.isVisible })

            // проверяем вызов функции addUserPreset
            ybm_wait(forFulfillmentOf: {
                self.mockServer?.handledRequests.contains { $0.contains("name=addUserPreset") } ?? false
            })
        }

        "Скролим снова к кнопке \"Добавить новый адрес\" и тапаем на него".ybm_run { _ in
            deliveryPage.element.swipe(to: .down, untilVisible: deliveryPage.addNewAddressButton.element)
            editAddressPage = deliveryPage.addNewAddressButton.tap()
            ybm_wait(forFulfillmentOf: { editAddressPage.element.isVisible })
        }

        "Проверяем саджесты".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { editAddressPage.streetInput.element.isVisible })
            editAddressPage.streetInput.textField.tap()
            ybm_wait(forFulfillmentOf: { editAddressPage.streetSuggestView.element.isVisible })

            func checkSuggest(index: Int, text: String, imageId: String) {
                ybm_wait(forFulfillmentOf: {
                    let suggest = editAddressPage.streetSuggestView.suggest(index: index)
                    return suggest.textView.text == text
                        && suggest.image.identifier == imageId
                })
            }

            checkSuggest(index: 0, text: homeSuggestText, imageId: "CheckoutHomeAddress")
            checkSuggest(index: 1, text: workSuggestText, imageId: "CheckoutWorkAddress")
            checkSuggest(index: 2, text: addressSuggestText1, imageId: "CheckoutGenericAddress")
            checkSuggest(index: 3, text: addressSuggestText2, imageId: "CheckoutGenericAddress")
            checkSuggest(index: 4, text: addressSuggestText3, imageId: "CheckoutGenericAddress")
            XCTAssertFalse(editAddressPage.streetSuggestView.suggest(index: 5).element.isVisible)
        }
    }

}
