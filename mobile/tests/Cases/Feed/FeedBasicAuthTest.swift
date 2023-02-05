import MarketUITestMocks
import XCTest

final class FeedBasicAuthTest: LocalMockTestCase {

    override var user: UserAuthState {
        .loginWithYandexPlus
    }

    func testCashback() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3580")
        Allure.addEpic("Выдача")
        Allure.addFeature("Кешбэк")
        Allure.addTitle("Бейдж на сниппетах для плюсовика")

        let search = "iphone"

        var feedPage: FeedPage!
        var feedState = FeedState()

        "Мокаем состояние".ybm_run { _ in
            feedState.setSearchOrUrlTransformState(
                mapper: .init(fromOffers: [.make(offer: .protein, withCashback: "190")])
            )
            feedState.setSearchStateFAPI(
                mapper: .init(fromOffers: [.make(offer: .protein, withCashback: "190")])
            )
            stateManager?.setState(newState: feedState)
        }

        "Открываем выдачу".ybm_run { _ in
            _ = appAfterOnboardingAndPopups()
            feedPage = open(search: search)
        }

        "Проверка наличия кешбэка на сниппете первого товара".ybm_run { _ in
            let snippetPage = feedPage.collectionView.cellPage(at: 0)
            feedPage.collectionView.element.ybm_swipe(toFullyReveal: snippetPage.element)

            XCTAssertTrue(snippetPage.cashback.isVisible)
            XCTAssertEqual(" 190", snippetPage.cashback.label)
        }
    }
}
