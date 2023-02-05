import XCTest

class DSBSCartTests: LocalMockTestCase {

    override func setUp() {
        super.setUp()
        disable(toggles: FeatureNames.cartRedesign)
    }

    func testDsbsWithPromosCart() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3876")
        Allure.addEpic("DSBS")
        Allure.addFeature("Корзина. Комбайн")
        Allure.addTitle("Промо")

        var root: RootPage!
        var cart: CartPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "DSBSPromoCart")
        }

        "Переходим в корзину".ybm_run { _ in
            root = appAfterOnboardingAndPopups()

            cart = goToCart(root: root)
            ybm_wait(forVisibilityOf: [cart.element, cart.threshold.element])
        }

        "Флеш".ybm_run { _ in
            let item = cart.cartItem(with: 0)
            cart.element.ybm_swipeCollectionView(toFullyReveal: item.element)

            XCTAssertEqual(item.price.label, "2 000 ₽")
            XCTAssertEqual(item.oldPrice.label, "4 612 ₽")
        }

        "Скидка".ybm_run { _ in
            let item = cart.cartItem(with: 1)
            cart.element.ybm_swipeCollectionView(toFullyReveal: item.element)

            XCTAssertEqual(item.price.label, "637 ₽")
            XCTAssertEqual(item.oldPrice.label, "1 335 ₽")
        }

        "Подарок".ybm_run { _ in
            let item = cart.cartItem(with: 4)
            cart.element.ybm_swipeCollectionView(toFullyReveal: item.element)

            XCTAssertEqual(item.price.label, "Подарок")
        }

        "Проверяем саммари".ybm_run { _ in
            let discount = cart.summary.discount
            cart.element.ybm_swipeCollectionView(toFullyReveal: discount.element)

            XCTAssertEqual(discount.title.label, "Скидка на товары")
            XCTAssertEqual(discount.details.label, "-8 658 ₽")
        }
    }

    func testMultiCart() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3876")
        Allure.addEpic("DSBS")
        Allure.addFeature("Корзина. Комбайн")

        var root: RootPage!
        var cart: CartPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "DSBSMultiCart")
        }

        "Переходим в корзину".ybm_run { _ in
            root = appAfterOnboardingAndPopups()

            cart = goToCart(root: root)
            wait(forVisibilityOf: cart.element)
        }

        "Проверяем заголовок".ybm_run { _ in
            XCTAssertEqual(cart.businessGroupHeader(at: 0).text.label, "Доставка Яндекса и партнёров")
        }

        "Проверяем заголовки посылок, поставщика, кнопку 'Избранное'".ybm_run { _ in
            [0, 1, 2].forEach {
                checkCartItems(
                    cart: cart,
                    id: $0,
                    wishlistExist: false
                )
            }
            [3, 4].forEach {
                checkCartItems(
                    cart: cart,
                    id: $0,
                    wishlistExist: true
                )
            }
        }

        "Нажимаем на сниппет дсбс товара без modelId".ybm_run { _ in
            let item = cart.cartItem(with: 2)

            cart.element.ybm_swipeCollectionView(to: .up, toFullyReveal: item.title)
            wait(forVisibilityOf: item.element)

            let skuPage = item.tap()
            wait(forVisibilityOf: skuPage.element)

            XCTAssertTrue(skuPage.element.exists)

            skuPage.navigationBar.backButton.tap()
            wait(forVisibilityOf: cart.element)
        }
    }

    func testSingleCart() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3876")
        Allure.addEpic("DSBS")
        Allure.addFeature("Корзина")

        var root: RootPage!
        var cart: CartPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "DSBSCart")
        }

        "Переходим в корзину".ybm_run { _ in
            root = appAfterOnboardingAndPopups()

            cart = goToCart(root: root)
            wait(forVisibilityOf: cart.element)
        }

        "Проверяем заголовок".ybm_run { _ in
            swipeAndCheck(
                page: cart.element,
                element: cart.businessGroupHeader(at: 0).text,
                check: { XCTAssertEqual($0.label, "Доставка Яндекса и партнёров") }
            )
        }

        "Проверяем, что можно изменить количество".ybm_run { _ in
            swipeAndCheck(
                page: cart.element,
                element: cart.cartItem(at: 0).countPicker,
                check: { XCTAssertTrue($0.exists) }
            )
        }

        "Проверяем summary".ybm_run { _ in
            swipeAndCheck(
                page: cart.element,
                element: cart.summary.promocodeDiscount.details,
                check: { XCTAssertEqual($0.label, "-100 ₽") }
            )

            XCTAssertFalse(cart.summary.weight.element.exists)
        }

    }

    private func checkCartItems(cart: CartPage, id: Int, wishlistExist: Bool) {
        let cartItem = cart.cartItem(with: id)

        swipeAndCheck(
            page: cart.element,
            element: cartItem.supplier,
            check: { XCTAssertTrue($0.label.contains("Продавец:")) }
        )

        XCTAssertEqual(cartItem.wishlistButton.exists, wishlistExist)
    }

    private func addMockMatchRuleForUserOrder(
        id: String,
        orderId: String
    ) {
        let rule = MockMatchRule(
            id: id,
            matchFunction:
            isPOSTRequest &&
                isFAPIRequest &&
                hasExactFAPIResolvers(["resolveUserOrderByIdFull"]) &&
                hasStringInBody("\"orderId\":\(orderId)"),
            mockName: "resolveUserOrderById_\(orderId)"
        )

        mockServer?.addRule(rule)
    }
}
