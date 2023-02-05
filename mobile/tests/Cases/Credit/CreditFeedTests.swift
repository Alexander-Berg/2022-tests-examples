import MarketUITestMocks
import XCTest

class CreditFeedTests: LocalMockTestCase {

    func testFeedMonthlyPaymentVisibilityForOrdinaryGoods() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4041")
        Allure.addEpic("Кредиты")
        Allure.addFeature("Тинькофф")
        Allure.addTitle("Ежемесячный платеж. Выдача, обычный товар с кредитом")

        var feedPage: FeedPage!

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

        "Проверяем, что ежемесячный платеж отображен".ybm_run { _ in
            let monthlyPayment = feedPage.collectionView.cellPage(
                at: 0
            ).financialProductPaymentAmount
            feedPage.collectionView.element.ybm_swipe(toFullyReveal: monthlyPayment, withVelocity: .slow)

            XCTAssert(monthlyPayment.isVisible)
            XCTAssertEqual(monthlyPayment.label, "от 2\u{00a0}523\u{202f}₽")
        }
    }

    func testFeedMonthlyPaymentInvisibilityForCheapGoods() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4041")
        Allure.addEpic("Кредиты")
        Allure.addFeature("Тинькофф")
        Allure.addTitle("Ежемесячный платеж. Выдача, дешевый товар без кредита")

        var feedPage: FeedPage!
        var snippetPage: FeedSnippetPage!

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

        "Проверяем, что ежемесячный платеж не отображен".ybm_run { _ in
            snippetPage = feedPage.collectionView.cellPage(at: IndexPath(item: 0, section: 4))
            XCTAssertFalse(snippetPage.financialProductPaymentAmount.isVisible)
        }
    }

    func testFeedMonthlyPaymentInvisibilityForExpensiveGoods() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4041")
        Allure.addEpic("Кредиты")
        Allure.addFeature("Тинькофф")
        Allure.addTitle("Ежемесячный платеж. Выдача, дорогой товар без кредита")

        var feedPage: FeedPage!
        var snippetPage: FeedSnippetPage!

        "Настраиваем FT и мокаем startup для получения эксперимента all_tinkoff_credit_exp".ybm_run { _ in
            app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
            enable(toggles: FeatureNames.tinkoffCredit)
            mockStateManager?.pushState(bundleName: "Experiments_Tinkoff_Credit")
        }

        "Мокаем состояние".ybm_run { _ in
            setupFeedState()
            mockStateManager?.pushState(bundleName: "Credit_ExpensiveGoods")
        }

        "Переходим в выдачу. Находим товар стоимостью свыше 200 000Р".ybm_run { _ in
            feedPage = goToFeed(with: "Ноутбук Apple MacBook Pro 16 Late 2019")
            wait(forExistanceOf: feedPage.element)
        }

        "Проверяем, что ежемесячный платеж не отображен".ybm_run { _ in
            snippetPage = feedPage.collectionView.cellPage(at: IndexPath(item: 0, section: 4))
            XCTAssertFalse(snippetPage.financialProductPaymentAmount.isVisible)
        }
    }

    func testFeedMonthlyPaymentInvisibilityForDsbsGoods() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4782")
        Allure.addEpic("Кредиты")
        Allure.addFeature("Выдача")
        Allure.addTitle("Ежемесячный платеж неподходящего вендора")

        var feedPage: FeedPage!

        "Настраиваем FT".ybm_run { _ in
            enable(
                toggles:
                FeatureNames.tinkoffCredit,
                FeatureNames.paymentSDK
            )
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Credit_Feed_DsbsGoods")
        }

        "Переходим в выдачу. Находим dsbs товар стоимостью от 3 000Р до 200 000Р".ybm_run { _ in
            feedPage = goToFeed(with: "Бирюса Холодильник Бирюса 118")
            wait(forExistanceOf: feedPage.element)
        }

        "Проверяем, что ежемесячный платеж не отображен".ybm_run { _ in
            let snippetPage = feedPage.collectionView.snippetFirstMatchingCell()
            wait(forExistanceOf: snippetPage.element)
            feedPage.collectionView.element.ybm_swipe(toFullyReveal: snippetPage.element)

            XCTAssert(snippetPage.element.isVisible)
            XCTAssertFalse(snippetPage.financialProductPaymentAmount.isVisible)
        }
    }

    func testFeedMonthlyPaymentInvisibilityForClickAndCollectGoods() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4782")
        Allure.addEpic("Кредиты")
        Allure.addFeature("Выдача")
        Allure.addTitle("Ежемесячный платеж для товара из торгового зала")

        var feedPage: FeedPage!

        "Настраиваем FT".ybm_run { _ in
            enable(
                toggles:
                FeatureNames.tinkoffCredit,
                FeatureNames.paymentSDK
            )
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Credit_Feed_ClickAndCollectGoods")
        }

        "Переходим в выдачу. Находим товар из торгового зала стоимостью от 3 000Р до 200 000Р".ybm_run { _ in
            feedPage = goToFeed(with: "Колистин")
            wait(forExistanceOf: feedPage.element)
        }

        "Проверяем, что ежемесячный платеж не отображен".ybm_run { _ in
            let snippetPage = feedPage.collectionView.snippetFirstMatchingCell()
            wait(forExistanceOf: snippetPage.element)
            feedPage.collectionView.element.ybm_swipe(toFullyReveal: snippetPage.element)

            XCTAssert(snippetPage.element.isVisible)
            XCTAssertFalse(snippetPage.financialProductPaymentAmount.isVisible)
        }
    }
}

private extension CreditFeedTests {
    func setupFeedState() {
        var feedState = FeedState()
        feedState.setSearchOrRedirectState(mapper: .init(fromOffers: [.protein]))
        stateManager?.setState(newState: feedState)
    }
}
