import MarketUITestMocks
import XCTest

final class BNPLFeedTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testSnippetBNPLLabelVisibility() {
        Allure.addTestPalmLink("https://st.yandex-team.ru/BLUEMARKETAPPS-37312")
        Allure.addEpic("BNPL")
        Allure.addFeature("Выдача")
        Allure.addTitle("Отображение лейбла BNPL")

        var feedPage: FeedPage!

        "Настраиваем FT".run {
            enable(toggles: FeatureNames.BNPL)
        }

        "Мокаем состояние".run {
            setupFeedState()
        }

        "Открываем выдачу".ybm_run { _ in
            _ = appAfterOnboardingAndPopups()
            feedPage = open(search: "Xiaomi Redmi Note 10 Pro")
        }

        "Проверяем лейбл BNPL".run {
            let snippetPage = feedPage.collectionView.cellPage(at: 0)
            let productName = snippetPage.financialProductName
            wait(forVisibilityOf: productName)

            XCTAssertEqual(productName.label, "Оплата частями")
        }
    }
}

// MARK: - Helper Methods

private extension BNPLFeedTests {

    func setupFeedState() {
        var feedState = FeedState()
        feedState.setSearchOrUrlTransformState(
            mapper: FeedState.SearchResultFAPI(fromOffers: [Constants.capiOffer])
        )
        feedState.setSearchStateFAPI(
            mapper: FeedState.SearchResultFAPI(fromOffers: [Constants.capiOffer])
        )
        stateManager?.setState(newState: feedState)
    }
}

// MARK: - Nested Types

private extension BNPLFeedTests {

    enum Constants {
        static let bnpl = "BNPL"
        static let capiOffer = modify(CAPIOffer.protein) {
            $0.bnplAvailable = true
            $0.financialProductPriority = [bnpl]
        }
    }
}
