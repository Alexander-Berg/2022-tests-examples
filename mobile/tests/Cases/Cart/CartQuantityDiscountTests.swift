import XCTest

final class CartQuantityDiscountTests: LocalMockTestCase {

    func testThatQuantityDiscountIsDisplayed() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4608")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4550")
        Allure.addEpic("Корзина")
        Allure.addFeature("Скидка за количество")
        Allure.addTitle("Проверяем, что скидка за кол-во отображается в сниппете товара корзины")

        var cartPage: CartPage!

        disable(toggles: FeatureNames.cartRedesign)

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "CartQuantityDiscount")
        }

        "Переходим в корзину".ybm_run { _ in
            cartPage = goToCart()
        }

        "Ждем завершения загрузки информации".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { cartPage.collectionView.isVisible })
        }

        "Проверяем, отображение скидки за кол-во".ybm_run { _ in
            let cartItem = cartPage.cartItem(at: 0)
            XCTAssertEqual(cartItem.price.label, "289 ₽")
            XCTAssertEqual(
                cartItem.quantityDiscount.label,
                "2 шт - 10%, 5 шт - 20%"
            )
        }
    }

    func testThatQuantityDiscountIsApplied() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4608")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4550")
        Allure.addEpic("Корзина")
        Allure.addFeature("Скидка за количество")
        Allure.addTitle("Проверяем, что скидка за кол-во применена, когда количество товара увеличилось")

        var cartPage: CartPage!
        var cartItem: CartPage.CartItem!

        disable(toggles: FeatureNames.cartRedesign)

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "CartQuantityDiscount")
        }

        "Переходим в корзину".ybm_run { _ in
            cartPage = goToCart()
        }

        "Ждем завершения загрузки информации".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { cartPage.collectionView.isVisible })
        }

        "Открываем пикер количества товара".ybm_run { _ in
            cartItem = cartPage.cartItem(at: 0)
            cartItem.countPicker.tap()
        }

        "Мокаем товар с примененным 3 шт.".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "CartQuantityDiscount_Counter_3")
        }

        "Выбираем кол-во товара 3 шт.".ybm_run { _ in

            var pickerWheel: XCUIElement!
            ybm_wait {
                pickerWheel = XCUIApplication().pickerWheels.firstMatch
                return pickerWheel.isVisible
            }

            pickerWheel.adjust(toPickerWheelValue: "3")
            cartPage.countPickerDoneButton.tap()
            wait(forInvisibilityOf: pickerWheel)

            // проверяем вызов функции changeCartItems
            ybm_wait(forFulfillmentOf: {
                self.mockServer?.handledRequests.contains { $0.contains("name=changeCartItems") } ?? false
            })
        }

        "Проверяем применение первого порога".ybm_run { _ in
            cartPage.element.ybm_swipeCollectionView(to: .up, toFullyReveal: cartItem.countInfo)
            XCTAssertEqual(cartItem.countInfo.label, "3")
            XCTAssertEqual(cartItem.price.label, "90 ₽")
            XCTAssertEqual(cartItem.oldPrice.label, "289 ₽")
            XCTAssertEqual(cartItem.quantityDiscount.label, "2 шт - 10%, 5 шт - 20%")
            XCTAssertEqual(cartItem.priceDiscount.label, "- 10%")

        }

        "Проверяем, что скидка применилась в summary".ybm_run { _ in
            cartPage.collectionView.ybm_swipeCollectionView(toFullyReveal: cartPage.summary.totalPrice.element)
            XCTAssertEqual(cartPage.summary.totalPrice.details.label, "290 ₽")
            XCTAssertEqual(cartPage.summary.discount.details.label, "-750 ₽")
        }

        "Открываем пикер количества товара".ybm_run { _ in
            cartItem.countPicker.tap()
        }

        "Мокаем товар с примененным 5 шт.".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "CartQuantityDiscount_Counter_5")
        }

        "Выбираем кол-во товара 5 шт.".ybm_run { _ in

            var pickerWheel: XCUIElement!
            ybm_wait {
                pickerWheel = XCUIApplication().pickerWheels.firstMatch
                return pickerWheel.isVisible
            }

            pickerWheel.adjust(toPickerWheelValue: "5")
            cartPage.countPickerDoneButton.tap()
            wait(forInvisibilityOf: pickerWheel)

            // проверяем вызов функции changeCartItems
            ybm_wait(forFulfillmentOf: {
                self.mockServer?.handledRequests.contains { $0.contains("name=changeCartItems") } ?? false
            })
        }

        "Проверяем применение второго порога".ybm_run { _ in
            cartPage.element.ybm_swipeCollectionView(to: .up, toFullyReveal: cartItem.countInfo)
            XCTAssertEqual(cartItem.countInfo.label, "5")
            XCTAssertEqual(cartItem.price.label, "80 ₽")
            XCTAssertEqual(cartItem.oldPrice.label, "289 ₽")
            XCTAssertEqual(cartItem.quantityDiscount.label, "2 шт - 10%, 5 шт - 20%")
            XCTAssertEqual(cartItem.priceDiscount.label, "- 20%")
        }

        "Проверяем, что скидка применилась в summary".ybm_run { _ in
            cartPage.collectionView.ybm_swipeCollectionView(toFullyReveal: cartPage.summary.totalPrice.element)
            XCTAssertEqual(cartPage.summary.totalPrice.details.label, "400 ₽")
            XCTAssertEqual(cartPage.summary.discount.details.label, "-620 ₽")
        }
    }
}
