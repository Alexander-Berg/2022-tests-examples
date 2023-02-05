import XCTest

final class CartGiftsTest: LocalMockTestCase {

    override func setUp() {
        super.setUp()
        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
        disable(toggles: FeatureNames.cartRedesign)
    }

    func testBundleAppearance() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-2869")
        Allure.addEpic("Корзина")
        Allure.addFeature("Акционные товары")
        Allure.addTitle("Отображение акционного комплекта")

        var cart: CartPage!

        "Мокаем ручки".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "CartSet_Gifts")
        }

        "Открываем корзину".ybm_run { _ in
            cart = goToCart()
            wait(forVisibilityOf: cart.compactSummary.element)
        }

        "Проверяем сниппет основного товара".ybm_run { _ in
            let item = cart.cartItem(at: 0)
            let giftView = item.giftView.imageView
            cart.element.ybm_swipeCollectionView(toFullyReveal: item.element)
            XCTAssertFalse(giftView.element.isVisible)
            cart.element.ybm_swipeCollectionView(toFullyReveal: item.countPicker)
            XCTAssertTrue(item.countPicker.isVisible)
        }

        "Проверяем сниппет подарка".ybm_run { _ in
            cart.collectionView.ybm_swipeCollectionView(toFullyReveal: cart.cartItem(at: 1).element)

            let item = cart.cartItem(at: 1)
            XCTAssertFalse(item.giftView.imageView.element.isVisible)
            XCTAssertFalse(item.countPicker.isVisible)

            cart.element.ybm_swipeCollectionView(toFullyReveal: item.price)
            XCTAssertEqual(item.price.label, "Подарок")
        }
    }

    func testBundleChangeCount() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-2870")
        Allure.addEpic("Корзина")
        Allure.addFeature("Акционные товары")
        Allure.addTitle("Изменение количества товаров")

        var cart: CartPage!
        var mainOffer: CartPage.CartItem!
        var giftOffer: CartPage.CartItem!

        "Мокаем ручки".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "CartSet_Gifts")
        }

        "Открываем корзину".ybm_run { _ in
            cart = goToCart()
            wait(forExistanceOf: cart.element)

            cart.element.ybm_swipeCollectionView(toFullyReveal: cart.cartItem(at: 0).element)
            mainOffer = cart.cartItem(at: 0)
            wait(forExistanceOf: mainOffer.element)

            cart.element.ybm_swipeCollectionView(toFullyReveal: cart.cartItem(at: 1).element)
            giftOffer = cart.cartItem(at: 1)
            wait(forExistanceOf: giftOffer.element)
        }

        "Мокаем ручки для изменения количества товаров".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "CartSet_Gifts_IncreaseCount")
        }

        "Увеличиваем количество основного товара".ybm_run { _ in
            mainOffer.countPicker.tap()

            wait(forVisibilityOf: cart.pickerWheel)
            cart.pickerWheel.adjust(toPickerWheelValue: "2")
            cart.countPickerDoneButton.tap()

            wait(forInvisibilityOf: cart.pickerWheel)

            XCTAssertEqual(mainOffer.countInfo.label, "2")
            XCTAssertEqual(giftOffer.giftCountInfo.label, "2 шт.")
        }

        "Мокаем ручки для изменения количества товаров".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "CartSet_Gifts_DecreaseCount")
        }

        "Уменьшаем количество основного товара".ybm_run { _ in
            mainOffer.countPicker.tap()

            wait(forVisibilityOf: cart.pickerWheel)
            cart.pickerWheel.adjust(toPickerWheelValue: "1")
            cart.countPickerDoneButton.tap()

            wait(forInvisibilityOf: cart.pickerWheel)

            XCTAssertEqual(mainOffer.countInfo.label, "1")
            XCTAssertEqual(giftOffer.giftCountInfo.label, "1 шт.")
        }
    }
}
