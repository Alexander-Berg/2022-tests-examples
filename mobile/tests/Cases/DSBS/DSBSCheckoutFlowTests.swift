import MarketModels
import MarketUITestMocks
import UIUtils
import XCTest

class DSBSCheckoutFlowTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    override func setUp() {
        super.setUp()
        disable(
            toggles:
            FeatureNames.mordaRedesign,
            FeatureNames.checkoutPresetsRedesign,
            FeatureNames.cartRedesign
        )
    }

    func testCartAndCheckout() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3752")
        Allure.addEpic("DSBS Чекаут")
        Allure.addFeature("Оформление заказа")

        var root: RootPage!
        var cart: CartPage!
        var checkoutPage: CheckoutPage!
        var merchantPopupPage: MerchantPopupPage!
        var finishMultiorderPage: FinishMultiorderPage!

        "Мокаем ручки".run {
            mockStateManager?.pushState(bundleName: "DSBSCheckoutFlow")
        }

        "Открываем корзину".run {
            root = appAfterOnboardingAndPopups()
            cart = goToCart(root: root)
        }

        "Проверяем заголовок бизнес группы".run {
            swipeAndCheck(
                page: cart.element,
                element: cart.businessGroupHeader(at: 0).text,
                check: { XCTAssertTrue($0.label.contains("Доставка Яндекса и партнёров")) }
            )
        }

        "Проверяем маленькое саммари".run {
            wait(forExistanceOf: cart.compactSummary.totalPrice)
            XCTAssertEqual(cart.compactSummary.totalPrice.label, "55\(String.ble_nonBreakingSpace)190 ₽")
        }

        "Проверяем большое саммари".run {
            swipeAndCheck(
                page: cart.element,
                summaryCell: cart.summary.weight,
                check: { XCTAssertEqual($0.details.label, "0,3 кг") }
            )
            swipeAndCheck(
                page: cart.element,
                summaryCell: cart.summary.totalItems,
                check: { XCTAssertEqual($0.details.label, "55\(String.ble_nonBreakingSpace)190 ₽") }
            )
            swipeAndCheck(
                page: cart.element,
                summaryCell: cart.summary.totalPrice,
                check: { XCTAssertEqual($0.details.label, "55\(String.ble_nonBreakingSpace)190 ₽") }
            )
        }

        "Переходим в чекаут".run {
            checkoutPage = cart.compactSummary.orderButton.tap()
            wait(forVisibilityOf: checkoutPage.element)
        }

        "Меняем адреса доставки".run {
            checkAddress(
                addressButton: checkoutPage.addressChooserButton(at: 0),
                indexToSelect: 0,
                matchingAddress: "Москва, 2-я Магистральная улица, д. 18Бс3"
            )
            checkoutPage.element.ybm_swipeCollectionView(
                toFullyReveal: checkoutPage.addressChooserButton(at: 1).element
            )
            checkAddress(
                addressButton: checkoutPage.addressChooserButton(at: 1),
                indexToSelect: 1,
                matchingAddress: "Реутов, улица Реутовских Ополченцев, д. 10"
            )
        }

        "Мокаем ручки".run {
            let blueSuppliersRule = MockMatchRule(
                id: "SUPPLIER_ID_BLUE",
                matchFunction:
                isPOSTRequest &&
                    isFAPIRequest &&
                    hasExactFAPIResolvers(["resolveSupplierInfoById"]) &&
                    hasStringInBody(#""isWhite":false"#),
                mockName: "supplierInfoById_blue"
            )
            let whiteSuppliersRule = MockMatchRule(
                id: "SUPPLIER_ID_WHITE",
                matchFunction:
                isPOSTRequest &&
                    isFAPIRequest &&
                    hasExactFAPIResolvers(["resolveSupplierInfoById"]) &&
                    hasStringInBody(#""isWhite":true"#),
                mockName: "supplierInfoById_white"
            )
            mockServer?.addRule(blueSuppliersRule)
            mockServer?.addRule(whiteSuppliersRule)
        }

        "Открываем поп-ап мерчанта".run {
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.merchantInfoDisclaimerCell.element)
            merchantPopupPage = checkoutPage.merchantInfoDisclaimerCell.tap()
            wait(forVisibilityOf: merchantPopupPage.element)
        }

        "Проверяем юринфо мерчантов".run {
            checkMerchantPopup(merchantPopupPage: merchantPopupPage)
        }

        "Закрываем попап мерчанта".run {
            XCUIApplication().otherElements.matching(identifier: PopupEmdeddingAccessibility.backgroundView).element
                .swipe(to: .up, until: !merchantPopupPage.element.isVisible)
        }

        "Выбираем способ оплаты наличными".run {
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentMethodCell.element)

            let paymentMethodPopupPage = checkoutPage.paymentMethodCell.tap()
            wait(forVisibilityOf: paymentMethodPopupPage.element)
            paymentMethodPopupPage.selectPaymentMethod(with: "CASH_ON_DELIVERY")
            paymentMethodPopupPage.continueButton.tap()

            wait(forInvisibilityOf: paymentMethodPopupPage.element)

            XCTAssertEqual(checkoutPage.paymentMethodCell.title.label, "Наличными при получении")
        }

        "Оформляем заказ".run {
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentButton.element)
            finishMultiorderPage = checkoutPage.paymentButton.tap()
        }

        "Проверяем экран Оформленного заказа".run {
            ybm_wait {
                finishMultiorderPage.element.isVisible &&
                    NavigationBarPage.current.title.isVisible
            }
            checkFinishMultiorderPage(finishMultiorderPage: finishMultiorderPage)
        }
    }

    func checkAddress(
        addressButton: CheckoutPage.AddressChooserButton,
        indexToSelect: Int,
        matchingAddress: String
    ) {
        wait(forVisibilityOf: addressButton.element)

        let checkoutPresetSelectorPage = addressButton.tap()
        wait(forVisibilityOf: checkoutPresetSelectorPage.element)

        let addressCell = checkoutPresetSelectorPage.addressCell(at: indexToSelect)
        wait(forVisibilityOf: addressCell.element)

        let addressCellTitle = addressCell.title
        XCTAssertEqual(addressCellTitle.label, matchingAddress)

        addressCellTitle.tap()
        checkoutPresetSelectorPage.doneButton.tap()

        ybm_wait {
            !checkoutPresetSelectorPage.element.exists
        }

        XCTAssertEqual(addressButton.title.label, matchingAddress)
    }

    func checkMerchantPopup(merchantPopupPage page: MerchantPopupPage) {
        swipeAndCheck(
            page: page.element,
            element: page.title(at: 0),
            check: { XCTAssertEqual($0.label, "Первую посылку доставит Яндекс.Маркет") }
        )
        swipeAndCheck(
            page: page.element,
            element: page.fullName(at: 0).caption,
            check: { XCTAssertEqual($0.label, "ООО «ЯНДЕКС»") }
        )
        swipeAndCheck(
            page: page.element,
            element: page.actualAddress(at: 0).caption,
            check: {
                XCTAssertEqual($0.label, "119021, Россия, г. Москва, ул. Льва Толстого, д. 16")
            }
        )
        swipeAndCheck(
            page: page.element,
            element: page.juridicalAddress(at: 0).caption,
            check: {
                XCTAssertEqual($0.label, "119021, Россия, г. Москва, ул. Льва Толстого, д. 16")
            }
        )
        swipeAndCheck(
            page: page.element,
            element: page.ogrn(at: 0).caption,
            check: { XCTAssertEqual($0.label, "1027700229193") }
        )
        swipeAndCheck(
            page: page.element,
            element: page.support(at: 0).caption,
            check: {
                XCTAssertEqual(
                    $0.label,
                    "8\(String.ble_nonBreakingSpace)(495)\(String.ble_nonBreakingSpace)414-30-00 с 9:00 до 21:00"
                )
            }
        )

        swipeAndCheck(
            page: page.element,
            element: page.supplierSubtitle(at: 0),
            check: { XCTAssertEqual($0.label, #"Продавец ООО "Мягкая синяя булка""#) }
        )
        swipeAndCheck(
            page: page.element,
            element: page.fullName(at: 1).caption,
            check: { XCTAssertEqual($0.label, #"ООО "Мягкая синяя булка""#) }
        )
        swipeAndCheck(
            page: page.element,
            element: page.actualAddress(at: 1).caption,
            check: { XCTAssertEqual($0.label, "127081, г. Москва, улица Чермянская, дом 3, строение 2, помещение 1а") }
        )
        swipeAndCheck(
            page: page.element,
            element: page.juridicalAddress(at: 1).caption,
            check: { XCTAssertEqual($0.label, "127081, г. Москва, улица Чермянская, дом 3, строение 2, помещение 1б") }
        )
        swipeAndCheck(
            page: page.element,
            element: page.ogrn(at: 1).caption,
            check: { XCTAssertEqual($0.label, "1107746275900") }
        )
        swipeAndCheck(
            page: page.element,
            element: page.support(at: 1).caption,
            check: {
                XCTAssertEqual($0.label, "+7\(String.ble_nonBreakingSpace)(921)\(String.ble_nonBreakingSpace)746-68-56")
            }
        )

        swipeAndCheck(
            page: page.element,
            element: page.title(at: 1),
            check: { XCTAssertEqual($0.label, #"Вторую посылку доставит продавец ООО "ДСБС 13""#) }
        )
        swipeAndCheck(
            page: page.element,
            element: page.fullName(at: 2).caption,
            check: { XCTAssertEqual($0.label, #"ООО "ДСБС 13""#) }
        )
        swipeAndCheck(
            page: page.element,
            element: page.actualAddress(at: 2).caption,
            check: { XCTAssertEqual($0.label, "121828, г. Москва, улица Отрадная, дом 14, строение 2, помещение 8а") }
        )
        swipeAndCheck(
            page: page.element,
            element: page.juridicalAddress(at: 2).caption,
            check: { XCTAssertEqual($0.label, "121828, г. Москва, улица Отрадная, дом 14, строение 2, помещение 8б") }
        )
        swipeAndCheck(
            page: page.element,
            element: page.ogrn(at: 2).caption,
            check: { XCTAssertEqual($0.label, "1107746275900") }
        )
        swipeAndCheck(
            page: page.element,
            element: page.support(at: 2).caption,
            check: {
                XCTAssertEqual($0.label, "+7\(String.ble_nonBreakingSpace)(921)\(String.ble_nonBreakingSpace)746-68-34")
            }
        )
    }

    func checkFinishMultiorderPage(finishMultiorderPage: FinishMultiorderPage) {
        XCTAssertEqual(NavigationBarPage.current.title.label, "Спасибо")

        swipeAndCheck(
            page: finishMultiorderPage.element,
            element: finishMultiorderPage.deliveryStatus(at: 0),
            check: { XCTAssertEqual($0.label, "Доставка курьером в пятницу, 23 апреля, \nс 10:00 до 22:00") }
        )
        swipeAndCheck(
            page: finishMultiorderPage.element,
            element: finishMultiorderPage.paymentStatus(at: 0),
            check: { XCTAssertEqual($0.label, "Оплата при получении 299 ₽") }
        )
        swipeAndCheck(
            page: finishMultiorderPage.element,
            element: finishMultiorderPage.titleOfOrderItem(at: 0),
            check: { XCTAssertEqual($0.label, "Смартфон Sony Xperia Z5 Compact, графитовый черный\n1 шт") }
        )
        swipeAndCheck(
            page: finishMultiorderPage.element,
            element: finishMultiorderPage.deliveryStatus(at: 1),
            check: { XCTAssertEqual($0.label, "Доставка курьером в пятницу, 23\(String.ble_nonBreakingSpace)апреля") }
        )
        swipeAndCheck(
            page: finishMultiorderPage.element,
            element: finishMultiorderPage.paymentStatus(at: 1),
            check: { XCTAssertEqual($0.label, "Оплата при получении 55\(String.ble_nonBreakingSpace)090 ₽") }
        )
        swipeAndCheck(
            page: finishMultiorderPage.element,
            element: finishMultiorderPage.titleOfOrderItem(at: 1),
            check: { XCTAssertEqual($0.label, "Смартфон Samsung Galaxy S10+ 128 ГБ гранат\n1 шт") }
        )
    }

    private func swipeAndCheck(
        page: XCUIElement,
        summaryCell: CartPage.SummaryPrimaryCell,
        check: (CartPage.SummaryPrimaryCell) -> Void
    ) {
        page.ybm_swipeCollectionView(toFullyReveal: summaryCell.element)
        check(summaryCell)
    }

}
