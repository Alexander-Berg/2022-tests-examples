import MarketUITestMocks
import UIUtils
import XCTest

final class CheckoutRepurshareFlowAuthTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginWithYandexPlus
    }

    func testWithMulticart() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3872")
        Allure.addEpic("Чекаут")
        Allure.addFeature("Флоу повторной покупки")
        Allure.addTitle("Обычная мультикорзина")

        var root: RootPage!
        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!
        var checkoutPresetSelectorPage: CheckoutPresetSelectorPage!
        var editAddressPage: EditAddressPage!
        var paymentMethodPopupPage: CheckoutPaymentMethodPopupPage!
        var finishMultiorderPage: FinishMultiorderPage!

        let city = "Москва"
        let street = "Красная площадь"
        let house = "1"
        let fullAddress = "\(city), \(street), д. \(house)"

        disable(
            toggles:
            FeatureNames.checkoutPresetsRedesign,
            FeatureNames.cartRedesign
        )

        "Мокаем ручки".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Checkout_RepurshareFlowMulticart")
        }

        "Открываем корзину".ybm_run { _ in
            root = appAfterOnboardingAndPopups()

            cartPage = goToCart(root: root)
            wait(forExistanceOf: cartPage.cartItem(at: 0).element)
        }

        "Нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            checkoutPage = cartPage.compactSummary.orderButton.tap()
        }

        "Проверяем, что данные перенеслись из прошлого заказа".ybm_run { _ in
            let addressLabel = checkoutPage.addressChooserButton().title.label
            XCTAssertEqual(addressLabel, fullAddress)

            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.recipientInfoCell.element)

            let recepientLabel = checkoutPage.recipientInfoCell.title.label
            XCTAssertEqual(recepientLabel, "Ge Gege\n99@99.ru, +79999999999")

            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentMethodCell.element)

            let paymentMethodLabel = checkoutPage.paymentMethodCell.title.label
            XCTAssertEqual(paymentMethodLabel, "Картой онлайн")
        }

        "Проверяем информацию на экране подтверждения в саммари".ybm_run { _ in
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.summaryItemsCell.element)

            let countFromItems = checkoutPage.summaryItemsCell.title.label.split(separator: " ")
            XCTAssertEqual(checkoutPage.summaryItemsCell.details.label, "10 364 ₽")

            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.summaryDeliveryCell.element)

            let countFromDelivery = checkoutPage.summaryDeliveryCell.title.label.split(separator: " ")
            XCTAssertEqual(checkoutPage.summaryDeliveryCell.details.label, "бесплатно")

            XCTAssertEqual(countFromItems.last, countFromDelivery.last)
            XCTAssertEqual(countFromItems.last, "(3)")

            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.summaryDiscountCell.element)
            XCTAssertEqual(checkoutPage.summaryDiscountCell.details.label, "-54 ₽")

            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.summaryTotalCell.element)
            XCTAssertEqual(checkoutPage.summaryTotalCell.details.label, "10 310 ₽")

            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.secureOfferBadgeCell.element)
            XCTAssertEqual(checkoutPage.secureOfferBadgeCell.title.label, "Безопасное оформление заказа")
        }

        "Нажимаем на выбранный адрес".ybm_run { _ in
            checkoutPage.element.ybm_swipeCollectionView(
                to: .up,
                toFullyReveal: checkoutPage.addressChooserButton().element
            )

            checkoutPresetSelectorPage = checkoutPage.addressChooserButton().tap()

            ybm_wait(forVisibilityOf: [checkoutPresetSelectorPage.element])

            checkoutPresetSelectorPage.element
                .ybm_swipeCollectionView(toFullyReveal: checkoutPresetSelectorPage.selectedAddressCell.element)
            XCTAssertEqual(checkoutPresetSelectorPage.selectedAddressCell.title.label, fullAddress)
        }

        "Нажимаем на карандаш рядом с выбранным адресом".ybm_run { _ in
            editAddressPage = checkoutPresetSelectorPage.selectedAddressCell.tapEdit()
            ybm_wait(forVisibilityOf: [editAddressPage.element])

            XCTAssertEqual(editAddressPage.addressCellView.label, fullAddress)
        }

        "Выбираем способ оплаты \"наличными при получении\" и подтверждаем заказ".ybm_run { _ in
            editAddressPage.continueButton.tap()
            ybm_wait(forVisibilityOf: [checkoutPresetSelectorPage.element])
            checkoutPresetSelectorPage.doneButton.tap()

            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentMethodCell.element)

            paymentMethodPopupPage = checkoutPage.paymentMethodCell.tap()
            paymentMethodPopupPage.selectPaymentMethod(with: "CASH_ON_DELIVERY")
            paymentMethodPopupPage.continueButton.tap()

            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentButton.element)

            finishMultiorderPage = checkoutPage.paymentButton.tap()
            let navigationTextTitle = NavigationBarPage.current.title

            ybm_wait(forFulfillmentOf: {
                finishMultiorderPage.element.isVisible && navigationTextTitle.isVisible
            })

            XCTAssertEqual(navigationTextTitle.label, "Спасибо")

            let firstItem = finishMultiorderPage.titleOfOrderItem(at: 0)
            XCTAssertEqual(firstItem.label, "Аккумуляторная дрель-шуруповерт Zitrek Green 12 32 Н·м зелeный\n1 шт")

            let secondItem = finishMultiorderPage.titleOfOrderItem(at: 1)
            finishMultiorderPage.element.ybm_swipeCollectionView(toFullyReveal: secondItem)
            XCTAssertEqual(secondItem.label, "Конфеты Snickers minis, коробка 7000 г\n1 шт")

            let thirdItem = finishMultiorderPage.titleOfOrderItem(at: 2)
            finishMultiorderPage.element.ybm_swipeCollectionView(toFullyReveal: thirdItem)
            XCTAssertEqual(thirdItem.label, "Твердотельный накопитель Samsung 860 EVO 500 GB (MZ-76E500BW)\n1 шт")
        }
    }
}
