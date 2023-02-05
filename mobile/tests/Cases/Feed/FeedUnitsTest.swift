import MarketUITestMocks
import XCTest

final class FeedUnitsTest: LocalMockTestCase {

    typealias SearchSnippet = FeedPage.CollectionView.CellPage

    func testFeedUnitInfo() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5572")
        Allure.addEpic("Выдача")
        Allure.addFeature("Единицы измерения")
        Allure.addTitle("Отображение единиц измерения на выдаче")

        enable(toggles: FeatureNames.CMSShowUnitPriceIgnore)

        "Мокаем состояние".ybm_run { _ in
            var feedState = FeedState()
            feedState.setSearchOrRedirectState(mapper: .init(
                offers: FAPIOffer.allWithUnit
            ))
            stateManager?.setState(newState: feedState)
        }

        var feedPage: FeedPage!

        "Открываем выдачу".run {
            feedPage = goToFeed()
        }

        var snippet: SearchSnippet!

        "Находим первый сниппет на выдаче".run {
            snippet = feedPage.collectionView.cellPage(at: 0)
            feedPage.collectionView.element.ybm_swipe(toFullyReveal: snippet.element)
        }

        "Проверяем отображение основной единицы в цене на сниппете".run {
            XCTAssertTrue(snippet.currentPrice.isVisible)
            XCTAssertEqual(snippet.currentPrice.label, "81 990 ₽/уп")
        }

        "Проверяем отображения цены за второстепенную единицу на сниппете".run {
            XCTAssertTrue(snippet.unitPrice.isVisible)
            XCTAssertEqual(snippet.unitPrice.label, "1 100 ₽ / шт")
        }
    }
}
