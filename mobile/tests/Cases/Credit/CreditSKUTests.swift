import MarketUITestMocks
import XCTest

class CreditSkuTests: LocalMockTestCase {

    func testSKUMonthlyPaymentVisibilityForOrdinaryGoods() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4068")
        Allure.addEpic("Кредиты")
        Allure.addFeature("Тинькофф")
        Allure.addTitle("Ежемесячный платеж. КМ")

        disable(toggles: FeatureNames.hideStickyViewFeature)

        var feedPage: FeedPage!
        var snippetPage: FeedSnippetPage!
        var skuPage: SKUPage!
        var compactOfferView: CompactOfferViewPage!

        "Настраиваем FT и мокаем startup для получения эксперимента all_tinkoff_credit_exp".ybm_run { _ in
            app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
            enable(toggles: FeatureNames.tinkoffCredit)
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Credit_OrdinaryGoods")
            mockStateManager?.pushState(bundleName: "Experiments_Tinkoff_Credit")
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

        "Проверяем, что сумма ежемесячного платежа присутствует в снекбаре".ybm_run { _ in
            compactOfferView = CompactOfferViewPage.current
            let monthlyPayment = compactOfferView.creditInfo

            XCTAssert(monthlyPayment.isVisible)
            XCTAssertEqual(monthlyPayment.label, "от 2 523 ₽ / мес")
        }
    }

    func testSKUMonthlyPaymentImmutability() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4750")
        Allure.addEpic("Кредиты")
        Allure.addFeature("Тинькофф")
        Allure.addTitle("Виджет в КМ. Пересчет ежемесячного платежа")

        var feedPage: FeedPage!
        var snippetPage: FeedSnippetPage!
        var skuPage: SKUPage!
        var compactOfferPage: CompactOfferViewPage!

        "Настраиваем FT".ybm_run { _ in
            enable(
                toggles:
                FeatureNames.tinkoffCredit,
                FeatureNames.paymentSDK
            )
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Credit_SKU")
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
            XCTAssertTrue(monthlyPayment.label.starts(with: "от 2 155 ₽ / мес"))
        }

        "Проверяем, что присутствует кнопка \"Оформить\"".ybm_run { _ in
            XCTAssert(skuPage.creditInfo.checkoutButton.element.isVisible)
        }

        "Добавляем товар в корзину".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Credit_SKU_AddToCart")

            let addButton = skuPage.addToCartButton.element
            addButton.tap()

            compactOfferPage = CompactOfferViewPage.current
            let snackBarCartButton = compactOfferPage.cartButton.element
            ybm_wait(forFulfillmentOf: { snackBarCartButton.label == "1" })
        }

        "Увеличиваем каунтер товара до 2 шт.".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Credit_SKU_ItemCounterUp")

            let plusButton = skuPage.addToCartButton.plusButton
            plusButton.tap()

            let snackBarCartButton = compactOfferPage.cartButton.element
            ybm_wait(forFulfillmentOf: { snackBarCartButton.label == "2" })
        }

        "Проверяем, что сумма ежемесячного платежа не изменилась".ybm_run { _ in
            let monthlyPayment = skuPage.creditInfo.monthlyPayment

            XCTAssertTrue(monthlyPayment.label.starts(with: "от 2 155 ₽ / мес"))
        }
    }

    func testSKUMonthlyPaymentInvisibilityForCheapGoods() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4301")
        Allure.addEpic("Кредиты")
        Allure.addFeature("Тинькофф")
        Allure.addTitle("Ежемесячный платеж. КМ дешевого товара")

        var feedPage: FeedPage!
        var snippetPage: FeedSnippetPage!
        var skuPage: SKUPage!
        var compactOfferView: CompactOfferViewPage!

        "Настраиваем FT и мокаем startup для получения эксперимента all_tinkoff_credit_exp".ybm_run { _ in
            app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
            enable(toggles: FeatureNames.tinkoffCredit)
            mockStateManager?.pushState(bundleName: "Experiments_Tinkoff_Credit")
        }

        "Мокаем состояние".ybm_run { _ in
            setupFeedState()
            mockStateManager?.pushState(bundleName: "Credit_CheapGoods")
        }

        "Переходим в выдачу. Находим товар стоимостью до 3 000Р".ybm_run { _ in
            feedPage = goToFeed(with: "карандаш")
            wait(forExistanceOf: feedPage.element)
        }

        "Открываем КМ этого товара".ybm_run { _ in
            snippetPage = feedPage.collectionView.cellPage(at: 0)
            skuPage = snippetPage.tap()
            wait(forExistanceOf: skuPage.element)
        }

        "Проверяем, что под основной ценой товара отсутствует сумма ежемесячного платежа".ybm_run { _ in
            skuPage.collectionView.ybm_swipeCollectionView(toFullyReveal: skuPage.price.element)

            let creditInfo = skuPage.creditInfo
            XCTAssertFalse(creditInfo.element.isVisible)
        }

        "Проверяем, что сумма ежемесячного платежа отсутствует в снекбаре".ybm_run { _ in
            compactOfferView = CompactOfferViewPage.current
            skuPage.collectionView.ybm_swipeCollectionView(toFullyReveal: compactOfferView.cartButton.element)
            XCTAssertFalse(compactOfferView.creditInfo.isVisible)
        }
    }

    func testSKUMonthlyPaymentInvisibilityForExpensiveGoods() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4302")
        Allure.addEpic("Кредиты")
        Allure.addFeature("Тинькофф")
        Allure.addTitle("Ежемесячный платеж. КМ дорогого товара")

        var feedPage: FeedPage!
        var snippetPage: FeedSnippetPage!
        var skuPage: SKUPage!
        var compactOfferView: CompactOfferViewPage!

        "Настраиваем FT и мокаем startup для получения эксперимента all_tinkoff_credit_exp".ybm_run { _ in
            app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
            enable(toggles: FeatureNames.tinkoffCredit)
            mockStateManager?.pushState(bundleName: "Experiments_Tinkoff_Credit")
        }

        "Мокаем состояние".ybm_run { _ in
            setupFeedState()
            mockStateManager?.pushState(bundleName: "Credit_ExpensiveGoods")
        }

        "Переходим в выдачу. Находим товар стоимостью выше 200 000Р".ybm_run { _ in
            feedPage = goToFeed(with: "Ноутбук Apple MacBook Pro 16 Late 2019")
            wait(forExistanceOf: feedPage.element)
        }

        "Открываем КМ этого товара".ybm_run { _ in
            snippetPage = feedPage.collectionView.cellPage(at: 0)
            skuPage = snippetPage.tap()
            wait(forExistanceOf: skuPage.element)
        }

        "Проверяем, что под основной ценой товара отсутствует сумма ежемесячного платежа".ybm_run { _ in
            skuPage.collectionView.ybm_swipeCollectionView(toFullyReveal: skuPage.price.element)

            let creditInfo = skuPage.creditInfo
            XCTAssertFalse(creditInfo.element.isVisible)
        }

        "Проверяем, что сумма ежемесячного платежа отсутствует в снекбаре".ybm_run { _ in
            compactOfferView = CompactOfferViewPage.current
            skuPage.collectionView.ybm_swipeCollectionView(toFullyReveal: compactOfferView.cartButton.element)
            XCTAssertFalse(compactOfferView.creditInfo.isVisible)
        }
    }
}

private extension CreditSkuTests {
    func setupFeedState() {
        var feedState = FeedState()
        feedState.setSearchOrRedirectState(mapper: .init(fromOffers: [.protein]))
        stateManager?.setState(newState: feedState)
    }
}
