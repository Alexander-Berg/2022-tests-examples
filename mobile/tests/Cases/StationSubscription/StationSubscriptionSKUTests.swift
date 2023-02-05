import MarketUITestMocks
import XCTest

class StationSubscriptionSKUTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testSKUPage() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5376")
        Allure.addEpic("StationSubscription")
        Allure.addFeature("КМ")
        Allure.addTitle("Содержимое КМ")

        var skuPage: SKUPage!

        "Настраиваем FT и мокаем startup для получения эксперимента station_subscription_exp".ybm_run { _ in
            app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
            enable(toggles: FeatureNames.stationSubscription)
            setupExperiments([.stationSubscriptionExp])
        }

        "Мокаем состояние".ybm_run { _ in
            setupSKUInfoState()
        }

        "Открываем КМ".ybm_run { _ in
            skuPage = goToDefaultSKUPage()
        }

        "Проверяем цену подписки".ybm_run { _ in
            XCTAssertEqual(skuPage.price.price.label, "599 ₽/мес")
        }

        "Проверяем подпись «Вместе с подпиской Плюс Мульти»".ybm_run { _ in
            let plusMulti = skuPage
                .cellUniqueElement(withIdentifier: SKUAccessibility.OfferPriceSection.stationSubscriptionPlusMultiLabel)

            XCTAssert(
                plusMulti.isVisible,
                "Подписи «Вместе с подпиской Плюс Мульти» не видно"
            )

            let text = plusMulti.textViews.firstMatch
            XCTAssert(
                text.isVisible,
                "Подписи «Вместе с подпиской Плюс Мульти» не видно"
            )
            XCTAssertEqual(
                text.label,
                "Вместе с подпиской Плюс Мульти"
            )
        }

        "Проверяем кнопку \"Оформить подписку\"".ybm_run { _ in
            skuPage.element
                .ybm_swipeCollectionView(
                    toFullyReveal: skuPage.stationSubscriptionCheckoutButton.element,
                    inset: skuPage.stickyViewInset,
                    withVelocity: .init(20)
                )
            XCTAssert(
                skuPage.stationSubscriptionCheckoutButton.element.isVisible,
                "Кнопки \"Оформить подписку\" не видно"
            )
        }

        "Проверяем компактный оффер".ybm_run { _ in
            checkPurchaseSubscriptionCompactOffer(PurchaseSubscriptionViewPage.current)
        }
    }

    func testCompactOfferOnOpinionsPage() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5408")
        Allure.addEpic("StationSubscription")
        Allure.addFeature("КМ")
        Allure.addTitle("Отзывы")

        var skuPage: SKUPage!

        "Настраиваем FT и мокаем startup для получения эксперимента station_subscription_exp".ybm_run { _ in
            app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
            enable(toggles: FeatureNames.stationSubscription)
            setupExperiments([.stationSubscriptionExp])
        }

        "Мокаем состояние".ybm_run { _ in
            setupSKUInfoState()
        }

        "Открываем КМ".ybm_run { _ in
            skuPage = goToDefaultSKUPage()
        }

        "Переходим к отзывам".ybm_run { _ in
            wait(forVisibilityOf: skuPage.opinionsFastLink.element)
            let opinionsPage = skuPage.opinionsFastLink.tap()
            wait(forVisibilityOf: opinionsPage.element)
        }

        "Проверяем компактный оффер".ybm_run { _ in
            checkPurchaseSubscriptionCompactOffer(PurchaseSubscriptionViewPage.current)
        }
    }

    func testCompactOfferOnDescriptionPage() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5410")
        Allure.addEpic("StationSubscription")
        Allure.addFeature("КМ")
        Allure.addTitle("Описание")

        var skuPage: SKUPage!

        "Настраиваем FT и мокаем startup для получения эксперимента station_subscription_exp".ybm_run { _ in
            app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
            enable(toggles: FeatureNames.stationSubscription)
            setupExperiments([.stationSubscriptionExp])
        }

        "Мокаем состояние".ybm_run { _ in
            setupSKUInfoState()
        }

        "Открываем КМ".ybm_run { _ in
            skuPage = goToDefaultSKUPage()
        }

        "Переходим полному описанию".ybm_run { _ in
            skuPage.element.ybm_swipeCollectionView(
                toFullyReveal: skuPage.descriptionDetailsButton,
                inset: skuPage.stickyViewInset
            )
            skuPage.descriptionDetailsButton.tap()
            wait(forInvisibilityOf: skuPage.element)
        }

        "Проверяем компактный оффер".ybm_run { _ in
            checkPurchaseSubscriptionCompactOffer(PurchaseSubscriptionViewPage.current)
        }
    }

    func testCompactOfferOnSpecsPage() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5409")
        Allure.addEpic("StationSubscription")
        Allure.addFeature("КМ")
        Allure.addTitle("Характеристики")

        var skuPage: SKUPage!

        "Настраиваем FT и мокаем startup для получения эксперимента station_subscription_exp".ybm_run { _ in
            app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
            enable(toggles: FeatureNames.stationSubscription)
            setupExperiments([.stationSubscriptionExp])
        }

        "Мокаем состояние".ybm_run { _ in
            setupSKUInfoState()
        }

        "Открываем КМ".ybm_run { _ in
            skuPage = goToDefaultSKUPage()
        }

        "Переходим полному набору характеристик".ybm_run { _ in
            skuPage.element.ybm_swipeCollectionView(
                toFullyReveal: skuPage.specsDetailsButton.element,
                inset: skuPage.stickyViewInset
            )
            let specsPage = skuPage.specsDetailsButton.tap()
            wait(forVisibilityOf: specsPage.element)
        }

        "Проверяем компактный оффер".ybm_run { _ in
            // По какой-то причине `PurchaseSubscriptionView` в `StickyView` таблицы не ищется.
            // Инициализирую `PurchaseSubscriptionViewPage` всем приложением,
            // чтобы кнопку и текст искать напрямую в нём по их идентификатору.
            let purchaseSubscription = PurchaseSubscriptionViewPage(element: XCUIApplication())
            checkPurchaseSubscriptionCompactOffer(purchaseSubscription)
        }
    }

    func testQuestionsAndAnswers() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5411")
        Allure.addEpic("StationSubscription")
        Allure.addFeature("КМ")
        Allure.addTitle("Вопросы и ответы")

        var skuPage: SKUPage!
        var qna: QnAPage!

        "Настраиваем FT и мокаем startup для получения экспериментов station_subscription_exp и qa_split".ybm_run { _ in
            app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
            enable(toggles: FeatureNames.stationSubscription, FeatureNames.qa_split)
            setupExperiments([.stationSubscriptionExp, .qaSplit])
        }

        "Мокаем состояние".ybm_run { _ in
            setupSKUInfoState()
        }

        "Открываем КМ".ybm_run { _ in
            skuPage = goToDefaultSKUPage()
        }

        "Переходим на вопросы и ответы".ybm_run { _ in
            skuPage.element.ybm_swipeCollectionView(
                toFullyReveal: skuPage.qnaFastLink,
                inset: skuPage.stickyViewInset
            )
            skuPage.qnaFastLink.tap()
            qna = QnAPage.current
            wait(forVisibilityOf: qna.element)
        }

        "Проверяем компактный оффер".ybm_run { _ in
            checkPurchaseSubscriptionCompactOffer(PurchaseSubscriptionViewPage.current)
        }
    }
}

// MARK: - Helper Methods

private extension StationSubscriptionSKUTests {

    typealias Experiment = ResolveBlueStartup.Experiment

    func setupExperiments(_ experiments: [Experiment]) {
        var defaultState = DefaultState()
        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
        defaultState.setExperiments(experiments: experiments)
        stateManager?.setState(newState: defaultState)
    }

    func setupSKUInfoState() {
        var skuState = SKUInfoState()
        var config = CustomSKUConfig(productId: 123, offerId: "offerId")
        config.title = "Яндекс.Станция, подписка на 3 года, черная"
        config.price = 599
        config.isYaSubscriptionOffer = true
        config.rating.overviewsCount = 100
        config.rating.preciseRating = 4.52
        config.rating.rating = 4.5
        config.rating.ratingCount = 37
        config.rating.reviewsCount = 3
        config.specs = .pharma
        skuState.setSkuInfoState(with: .custom(config))
        stateManager?.setState(newState: skuState)
    }

    func checkPurchaseSubscriptionCompactOffer(
        _ purchaseSubscription: PurchaseSubscriptionViewPage,
        file: StaticString = #file,
        line: UInt = #line
    ) {
        XCTAssert(
            purchaseSubscription.purchaseSubscriptionButton.element.isVisible,
            "Кнопки \"Оформить\" не видно"
        )
        XCTAssertEqual(
            purchaseSubscription.price.label,
            "599 ₽ / мес Вместе с Плюс Мульти"
        )
    }
}
