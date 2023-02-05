import MarketUITestMocks
import XCTest

class CreditCheckoutTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testCreditWidgetVisibility() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4430")
        Allure.addEpic("Кредиты")
        Allure.addFeature("Тинькофф")
        Allure.addTitle("Виджет в КМ")

        var feedPage: FeedPage!
        var snippetPage: FeedSnippetPage!
        var skuPage: SKUPage!

        "Настраиваем FT и мокаем startup для получения эксперимента all_tinkoff_credit_exp".ybm_run { _ in
            app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
            enable(toggles: FeatureNames.tinkoffCredit)
            mockStateManager?.pushState(bundleName: "Experiments_Tinkoff_Credit")
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Credit_OrdinaryGoods")
        }

        "Переходим в выдачу. Находим товар стоимостью от 3 000Р до 200 000Р".ybm_run { _ in
            feedPage = goToFeed(with: "iPhone")
            wait(forExistanceOf: feedPage.element)
        }

        "Открываем КМ этого товара".ybm_run { _ in
            snippetPage = feedPage.collectionView.cellPage(at: 0)
            skuPage = snippetPage.tap()
            wait(forExistanceOf: skuPage.element)
        }

        "Проверяем, что под основной ценой товара присутствует сумма ежемесячного платежа".ybm_run { _ in
            let monthlyPayment = skuPage.creditInfo.monthlyPayment
            skuPage.collectionView.ybm_swipeCollectionView(toFullyReveal: monthlyPayment)

            XCTAssert(monthlyPayment.isVisible)
            XCTAssertTrue(monthlyPayment.label.starts(with: "от 2 523 ₽ / мес"))
        }

        "Проверяем, что присутствует кнопка \"Оформить\"".ybm_run { _ in
            XCTAssert(skuPage.creditInfo.checkoutButton.element.isVisible)
        }

        "Проверяем, что присутствует текст описания кредита".ybm_run { _ in
            let creditDisclaimer = skuPage.creditInfo.creditDisclaimer
            skuPage.collectionView.ybm_swipeCollectionView(toFullyReveal: creditDisclaimer)

            XCTAssert(creditDisclaimer.isVisible)
            XCTAssertEqual(creditDisclaimer.label, "Кредит")
        }

        "Добавляем товар в корзину".ybm_run { _ in
            let button = skuPage.addToCartButton.element
            mockStateManager?.pushState(bundleName: "Credit_OrdinaryGoods_AddItemToCart")
            button.tap()
            ybm_wait(forFulfillmentOf: { button.label == "1 товар в корзине" })
        }

        "Проверяем, что кнопка \"Оформить\" все еще присутствует".ybm_run { _ in
            XCTAssert(skuPage.creditInfo.checkoutButton.element.isVisible)
        }
    }

    func testCreditGoToCheckout() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4431")
        Allure.addEpic("Кредиты")
        Allure.addFeature("Тинькофф")
        Allure.addTitle("Виджет в КМ. Переход в чекаут")

        var feedPage: FeedPage!
        var snippetPage: FeedSnippetPage!
        var skuPage: SKUPage!
        var checkoutPage: CheckoutPage!

        "Настраиваем FT и мокаем startup для получения эксперимента all_tinkoff_credit_exp".ybm_run { _ in
            app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
            app.launchEnvironment[TestLaunchEnvironmentKeys.insideUITestsKTCreditCheckout] = String(true)

            enable(
                toggles:
                FeatureNames.tinkoffCredit
            )
            mockStateManager?.pushState(bundleName: "Credit_Checkout")
        }

        "Переходим в выдачу. Находим товар стоимостью от 3 000Р до 200 000Р".ybm_run { _ in
            feedPage = goToFeed(with: "realme c11")
            wait(forExistanceOf: feedPage.element)
        }

        "Открываем КМ этого товара".ybm_run { _ in
            snippetPage = feedPage.collectionView.cellPage(at: 0)
            skuPage = snippetPage.tap()
            wait(forExistanceOf: skuPage.element)
        }

        "Проверяем, что под основной ценой товара присутствует сумма ежемесячного платежа".ybm_run { _ in
            let monthlyPayment = skuPage.creditInfo.monthlyPayment
            skuPage.collectionView.ybm_swipeCollectionView(toFullyReveal: monthlyPayment)

            XCTAssert(monthlyPayment.isVisible)
            XCTAssertTrue(monthlyPayment.label.starts(with: "от 410 ₽ / мес"))
        }

        "Проверяем, что присутствует кнопка \"Оформить\"".ybm_run { _ in
            XCTAssert(skuPage.creditInfo.checkoutButton.element.isVisible)
        }

        "Проверяем, что присутствует текст описания кредита".ybm_run { _ in
            let creditDisclaimer = skuPage.creditInfo.creditDisclaimer
            skuPage.collectionView.ybm_swipeCollectionView(toFullyReveal: creditDisclaimer)

            XCTAssert(creditDisclaimer.isVisible)
            XCTAssertEqual(creditDisclaimer.label, "Кредит")
        }

        "Нажимаем на кнопку \"Оформить\"".ybm_run { _ in
            checkoutPage = skuPage.creditInfo.checkoutButton.tap()
            wait(forExistanceOf: checkoutPage.element)
        }

        "Проверяем способ оплаты \"В кредит\"".ybm_run { _ in
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentMethodCell.element)

            XCTAssert(checkoutPage.paymentMethodCell.element.isVisible)
            XCTAssertEqual(checkoutPage.paymentMethodCell.title.label, "В кредит")
        }
    }

    func testCreditGoToCheckoutWithPromocode() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4783")
        Allure.addEpic("Кредиты")
        Allure.addFeature("КМ")
        Allure.addTitle("Переход в чекаут с промокодом")

        var feedPage: FeedPage!
        var skuPage: SKUPage!
        var checkoutPage: CheckoutPage!

        "Настраиваем FT".ybm_run { _ in
            app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
            app.launchEnvironment[TestLaunchEnvironmentKeys.insideUITestsKTCreditCheckout] = String(true)

            enable(
                toggles:
                FeatureNames.tinkoffCredit,
                FeatureNames.paymentSDK
            )

            mockStateManager?.pushState(bundleName: "Credit_Checkout")
            mockStateManager?.pushState(bundleName: "Credit_SKU_Checkout_Promocode")
        }

        "Переходим в выдачу. Находим товар стоимостью от 3 000Р до 200 000Р и промокодом -21%".ybm_run { _ in
            feedPage = goToFeed(with: "телевизор kivi 50U710KB 50\" (2020)")
            wait(forExistanceOf: feedPage.element)
        }

        "Открываем КМ этого товара".ybm_run { _ in
            let snippetPage = feedPage.collectionView.cellPage(at: 0)
            skuPage = snippetPage.tap()
            wait(forExistanceOf: skuPage.element)
        }

        "Проверяем, что под основной ценой товара присутствует сумма ежемесячного платежа".ybm_run { _ in
            let monthlyPayment = skuPage.creditInfo.monthlyPayment
            skuPage.collectionView.ybm_swipeCollectionView(toFullyReveal: monthlyPayment)

            XCTAssert(monthlyPayment.isVisible)
            XCTAssertTrue(monthlyPayment.label.starts(with: "от 2 523 ₽ / мес"))
        }

        "Нажимаем на кнопку \"Оформить\"".ybm_run { _ in
            checkoutPage = skuPage.creditInfo.checkoutButton.tap()
            wait(forExistanceOf: checkoutPage.element)
        }

        "Проверяем, что сумма ежемесячного платежа уменьшилась на 21%".ybm_run { _ in
            let summaryTotalCell = checkoutPage.summaryTotalCell
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: summaryTotalCell.title)

            XCTAssertEqual(summaryTotalCell.title.label, "Итого")
            XCTAssertEqual(summaryTotalCell.details.label, "от 1 993 ₽ / мес")
        }
    }

    func testCreditGoToCheckoutAndClose() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4432")
        Allure.addEpic("Кредиты")
        Allure.addFeature("Тинькофф")
        Allure.addTitle("Виджет в КМ. Переход из чекаута в КМ")

        var feedPage: FeedPage!
        var snippetPage: FeedSnippetPage!
        var skuPage: SKUPage!
        var checkoutPage: CheckoutPage!

        "Настраиваем FT и мокаем startup для получения эксперимента all_tinkoff_credit_exp".ybm_run { _ in
            app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
            app.launchEnvironment[TestLaunchEnvironmentKeys.insideUITestsKTCreditCheckout] = String(true)

            enable(
                toggles:
                FeatureNames.tinkoffCredit
            )
            mockStateManager?.pushState(bundleName: "Credit_Checkout")
        }

        "Переходим в выдачу. Находим товар стоимостью от 3 000Р до 200 000Р".ybm_run { _ in
            feedPage = goToFeed(with: "realme c11")
            wait(forExistanceOf: feedPage.element)
        }

        "Открываем КМ этого товара".ybm_run { _ in
            snippetPage = feedPage.collectionView.cellPage(at: 0)
            skuPage = snippetPage.tap()
            wait(forExistanceOf: skuPage.element)
        }

        "Проверяем, что под основной ценой товара присутствует сумма ежемесячного платежа".ybm_run { _ in
            let monthlyPayment = skuPage.creditInfo.monthlyPayment
            skuPage.collectionView.ybm_swipeCollectionView(toFullyReveal: monthlyPayment)

            XCTAssert(monthlyPayment.isVisible)
            XCTAssertTrue(monthlyPayment.label.starts(with: "от 410 ₽ / мес"))
        }

        "Проверяем, что присутствует кнопка \"Оформить\"".ybm_run { _ in
            XCTAssert(skuPage.creditInfo.checkoutButton.element.isVisible)
        }

        "Проверяем, что присутствует текст описания кредита".ybm_run { _ in
            let creditDisclaimer = skuPage.creditInfo.creditDisclaimer
            skuPage.collectionView.ybm_swipeCollectionView(toFullyReveal: creditDisclaimer)

            XCTAssert(creditDisclaimer.isVisible)
            XCTAssertEqual(creditDisclaimer.label, "Кредит")
        }

        "Нажимаем на кнопку \"Оформить\"".ybm_run { _ in
            checkoutPage = skuPage.creditInfo.checkoutButton.tap()
            wait(forExistanceOf: checkoutPage.element)
        }

        "Проверяем способ оплаты \"В кредит\"".ybm_run { _ in
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentMethodCell.element)

            XCTAssert(checkoutPage.paymentMethodCell.element.isVisible)
            XCTAssertEqual(checkoutPage.paymentMethodCell.title.label, "В кредит")
        }

        "Закрываем чекаут".ybm_run { _ in
            NavigationBarPage.current.closeButton.tap()

            XCTAssert(skuPage.element.isVisible)
        }

        "Проверяем, что товар не добавлен в корзину".ybm_run { _ in
            let button = skuPage.addToCartButton.element

            skuPage.collectionView.ybm_swipeCollectionView(to: .up, toFullyReveal: button)
            XCTAssertNotEqual(button.label, "1 товар в корзине")
        }
    }

    func testCreditGoToCheckoutWithThreeItems() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4785")
        Allure.addEpic("Кредиты")
        Allure.addFeature("КМ")
        Allure.addTitle("Переход в чекаут с несколькими товарами добавленными в корзину")

        var feedPage: FeedPage!
        var skuPage: SKUPage!
        var compactOfferPage: CompactOfferViewPage!
        var checkoutPage: CheckoutPage!

        "Настраиваем FT".ybm_run { _ in
            app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
            app.launchEnvironment[TestLaunchEnvironmentKeys.insideUITestsKTCreditCheckout] = String(true)

            enable(
                toggles:
                FeatureNames.tinkoffCredit,
                FeatureNames.paymentSDK
            )
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Credit_Checkout")
            mockStateManager?.pushState(bundleName: "Credit_SKU_Checkout_ThreeItemsSetup")
        }

        "Переходим в выдачу. Находим товар стоимостью от 3 000Р до 200 000Р".ybm_run { _ in
            feedPage = goToFeed(with: "Телевизор Xiaomi Mi TV 4F 32 T2")
            wait(forExistanceOf: feedPage.element)
        }

        "Открываем КМ этого товара".ybm_run { _ in
            let snippetPage = feedPage.collectionView.cellPage(at: 0)
            skuPage = snippetPage.tap()
            wait(forExistanceOf: skuPage.element)
        }

        "Проверяем, что под основной ценой товара присутствует сумма ежемесячного платежа".ybm_run { _ in
            let monthlyPayment = skuPage.creditInfo.monthlyPayment
            skuPage.collectionView.swipe(to: .down, until: monthlyPayment.isVisible)

            XCTAssert(monthlyPayment.isVisible)
            XCTAssertTrue(monthlyPayment.label.starts(with: "от 946 ₽ / мес"))
        }

        "Мокаем добавление товара в корзину".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Credit_SKU_Checkout_ThreeItems")
        }

        "Добавляем товар в корзину".ybm_run { _ in
            let addButton = skuPage.addToCartButton.element
            addButton.tap()

            compactOfferPage = CompactOfferViewPage.current
            let snackBarCartButton = compactOfferPage.cartButton.element
            ybm_wait(forFulfillmentOf: { snackBarCartButton.label == "1" })
        }

        "Мокаем увеличение количества товара в корзине до 3-х".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Credit_SKU_Checkout_ThreeItemsCounterUpTo3")
        }

        "Увеличиваем каунтер товара до 3 шт.".ybm_run { _ in
            let plusButton = skuPage.addToCartButton.plusButton
            plusButton.tap()

            let snackBarCartButton = compactOfferPage.cartButton.element
            ybm_wait(forFulfillmentOf: { snackBarCartButton.label == "3" })
        }

        "Нажимаем на кнопку \"Оформить\"".ybm_run { _ in
            checkoutPage = skuPage.creditInfo.checkoutButton.tap()
            wait(forExistanceOf: checkoutPage.element)
        }

        "Проверяем саммари на количество товаров, общую сумму и сумму ежемесячного платежа".ybm_run { _ in
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.summaryItemsCell.title)
            let countFromItems = checkoutPage.summaryItemsCell.title.label.split(separator: " ")

            XCTAssertEqual(countFromItems.last, "(3)")
            XCTAssertEqual(checkoutPage.summaryItemsCell.details.label, "53 970 ₽")

            let summaryTotalCell = checkoutPage.summaryTotalCell
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: summaryTotalCell.title)

            XCTAssertEqual(summaryTotalCell.title.label, "Итого")
            XCTAssertEqual(summaryTotalCell.details.label, "от 2 837 ₽ / мес")
        }
    }

    func testCartGoToCheckoutWithItemsInCart() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4786")
        Allure.addEpic("Кредиты")
        Allure.addFeature("КМ")
        Allure.addTitle("Переход в чекаут. В корзине другие товары")

        var feedPage: FeedPage!
        var skuPage: SKUPage!
        var checkoutPage: CheckoutPage!

        "Настраиваем FT".ybm_run { _ in
            app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
            app.launchEnvironment[TestLaunchEnvironmentKeys.insideUITestsKTCreditCheckout] = String(true)

            enable(
                toggles:
                FeatureNames.tinkoffCredit,
                FeatureNames.paymentSDK
            )
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Credit_Checkout")
        }

        "Мокаем корзину с товаром стоимостью менее 3000Р".ybm_run { _ in
            var cartState = CartState()
            cartState.setCartStrategy(with: [.protein])
            stateManager?.setState(newState: cartState)
        }

        "Мокаем КМ и чекаут".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Credit_SKU_Checkout_With_Items_In_Cart")
        }

        "Переходим в выдачу. Находим товар стоимостью от 3 000Р до 200 000Р".ybm_run { _ in
            feedPage = goToFeed(with: "Холодильник LG GA-B419SEJL")
            wait(forExistanceOf: feedPage.element)
        }

        "Открываем КМ этого товара".ybm_run { _ in
            let snippetPage = feedPage.collectionView.cellPage(at: 0)
            skuPage = snippetPage.tap()
            wait(forExistanceOf: skuPage.element)
        }

        "Проверяем проверяем наличие кредитного виджета".ybm_run { _ in
            let monthlyPayment = skuPage.creditInfo.monthlyPayment
            skuPage.collectionView.ybm_swipeCollectionView(toFullyReveal: monthlyPayment)

            XCTAssert(monthlyPayment.isVisible)
        }

        "Нажимаем на кнопку \"Оформить\"".ybm_run { _ in
            checkoutPage = skuPage.creditInfo.checkoutButton.tap()
            wait(forExistanceOf: checkoutPage.element)
        }

        "Проверяем саммари на количество товаров и сумму, присутствует только товар с КМ".ybm_run { _ in
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.summaryItemsCell.title)
            let countFromItems = checkoutPage.summaryItemsCell.title.label.split(separator: " ")

            XCTAssertEqual(countFromItems.last, "(1)")
            XCTAssertEqual(checkoutPage.summaryItemsCell.details.label, "34 599 ₽")
        }
    }

    func testCreditGoToCheckoutItemWithGift() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4784")
        Allure.addEpic("Кредиты")
        Allure.addFeature("КМ")
        Allure.addTitle("Переход в чекаут для товара с подарком")

        var feedPage: FeedPage!
        var skuPage: SKUPage!
        var checkoutPage: CheckoutPage!

        "Настраиваем FT".ybm_run { _ in
            app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
            app.launchEnvironment[TestLaunchEnvironmentKeys.insideUITestsKTCreditCheckout] = String(true)

            enable(
                toggles:
                FeatureNames.tinkoffCredit,
                FeatureNames.paymentSDK
            )
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Credit_Checkout")
            mockStateManager?.pushState(bundleName: "Credit_SKU_Checkout_ItemWithGIft")
        }

        "Переходим в выдачу. Находим товар стоимостью от 3 000Р до 200 000Р с подарком".ybm_run { _ in
            feedPage = goToFeed(with: "часы orient ER2700DW")
            wait(forExistanceOf: feedPage.element)
        }

        "Открываем КМ этого товара".ybm_run { _ in
            let snippetPage = feedPage.collectionView.cellPage(at: 0)
            skuPage = snippetPage.tap()
            wait(forExistanceOf: skuPage.element)
        }

        "Проверяем, что под основной ценой товара присутствует сумма ежемесячного платежа".ybm_run { _ in
            let monthlyPayment = skuPage.creditInfo.monthlyPayment
            skuPage.collectionView.ybm_swipeCollectionView(toFullyReveal: monthlyPayment)

            XCTAssert(monthlyPayment.isVisible)
            XCTAssertTrue(monthlyPayment.label.starts(with: "от 912 ₽ / мес"))
        }

        "Нажимаем на кнопку \"Оформить\"".ybm_run { _ in
            checkoutPage = skuPage.creditInfo.checkoutButton.tap()
            wait(forExistanceOf: checkoutPage.element)
        }

        "Проверяем саммари на наличие товара с подарком, общую сумму, сумму скидки и сумму ежемесячного платежа"
            .ybm_run { _ in
                checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.summaryItemsCell.element)
                let countFromItems = checkoutPage.summaryItemsCell.title.label.split(separator: " ")

                XCTAssertEqual(checkoutPage.summaryItemsCell.details.label, "19 998 ₽")
                XCTAssertEqual(countFromItems.last, "(2)")

                checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.summaryDiscountCell.element)

                XCTAssertEqual(checkoutPage.summaryDiscountCell.details.label, "-9 999 ₽")
                XCTAssertEqual(checkoutPage.summaryDiscountCell.title.label, "Скидка на товары")

                let summaryTotalCell = checkoutPage.summaryTotalCell
                checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: summaryTotalCell.element)

                XCTAssertEqual(summaryTotalCell.details.label, "от 912 ₽ / мес")
                XCTAssertEqual(summaryTotalCell.title.label, "Итого")
            }
    }
}
