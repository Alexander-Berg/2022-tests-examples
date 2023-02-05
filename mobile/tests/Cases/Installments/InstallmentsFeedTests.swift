import MarketUITestMocks
import XCTest

final class InstallmentsFeedTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testSnippetInstallmentsLabelVisibility() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5253")
        Allure.addEpic("Рассрочка. Тинькофф")
        Allure.addFeature("Выдача")
        Allure.addTitle("Отображение лейбла Рассрочки")

        var feedPage: FeedPage!
        var snippetPage: FeedPage.CollectionView.CellPage!

        "Настраиваем FT".ybm_run { _ in
            enable(
                toggles:
                FeatureNames.paymentSDK,
                FeatureNames.tinkoffInstallments
            )
        }

        "Мокаем состояние".ybm_run { _ in
            setupFeedState()
        }

        "Открываем выдачу. Находим товар стоимостью от 3 000Р до 200 000Р".ybm_run { _ in
            _ = appAfterOnboardingAndPopups()
            feedPage = open(search: "Xiaomi Redmi Note 10 Pro")
        }

        "Проверяем лейблы рассрочки".ybm_run { _ in
            snippetPage = feedPage.collectionView.cellPage(at: 0)
            let productName = snippetPage.financialProductName
            let paymentAmount = snippetPage.financialProductPaymentAmount
            let paymentPeriod = snippetPage.financialProductPaymentPeriod

            wait(forVisibilityOf: snippetPage.element)
            feedPage.collectionView.element.ybm_swipeCollectionView(toFullyReveal: snippetPage.element)

            XCTAssert(productName.isVisible, "Лейбл фин. продукта не виден")
            XCTAssert(paymentAmount.isVisible, "Лейбл суммы платежа не виден")
            XCTAssert(paymentPeriod.isVisible, "Лейбл периода платежа не виден")

            XCTAssertEqual(productName.label, "Рассрочка")
            XCTAssertEqual(paymentAmount.label, "2 333 ₽")
            XCTAssertEqual(paymentPeriod.label, "× 12 мес")
        }
    }
}

// MARK: - Helper Methods

private extension InstallmentsFeedTests {

    func setupFeedState() {
        var feedState = FeedState()
        feedState.setSearchOrUrlTransformState(mapper: .init(fromOffers: [Constants.capiOffer]))
        feedState.setSearchStateFAPI(mapper: .init(fromOffers: [Constants.capiOffer]))
        stateManager?.setState(newState: feedState)
    }
}

// MARK: - Nested Types

private extension InstallmentsFeedTests {

    enum Constants {
        static let tinkoffInstallments = "TINKOFF_INSTALLMENTS"
        static let capiOffer = modify(CAPIOffer.protein) {
            $0.installmentsInfo = .default
            $0.financialProductPriority = [tinkoffInstallments]
        }
    }
}
