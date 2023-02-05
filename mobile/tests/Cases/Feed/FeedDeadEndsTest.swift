import XCTest

class FeedDeadEndsTest: LocalMockTestCase {

    func testEmptyFeed() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-1073")
        Allure.addEpic("Выдача")
        Allure.addFeature("Тупики на выдаче")
        Allure.addTitle("Пустая выдача")

        var feedPage: FeedPage!
        var collectionView: FeedPage.CollectionView!
        let search = "оасырчшщлфдысмлрпангшщдж"

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "FeedSet_Empty")
        }

        "Открываем выдачу".ybm_run { _ in
            _ = appAfterOnboardingAndPopups()
            feedPage = open(search: search)

            collectionView = feedPage.collectionView
            ybm_wait(forFulfillmentOf: {
                feedPage.element.isVisible
            })
        }

        "Проверяем грустную звездочку на выдаче".ybm_run { _ in
            let noResultsView = feedPage.collectionView.noResultsView
            ybm_wait(forFulfillmentOf: {
                noResultsView.element.isVisible
            })
        }

        "Проверяем виджет \"Подобрали для вас\"".ybm_run { _ in
            let recommendedByHistoryWidget = feedPage.collectionView.recommendedByHistoryCell
            test(widget: recommendedByHistoryWidget, with: "Подобрали для вас", in: collectionView)
        }

        "Проверяем виджет \"Каталог товаров\"".ybm_run { _ in
            let catalogCell = feedPage.collectionView.catalogCell
            test(widget: catalogCell, with: "Каталог товаров", in: collectionView)
        }
    }

    private func test(
        widget: FeedPage.CollectionView.CarouselCell,
        with title: String,
        in collectionView: FeedPage.CollectionView
    ) {
        collectionView.element.swipe(to: .down, untilVisible: widget.element)

        XCTAssertEqual(widget.title.label, title)
        XCTAssertTrue(widget.element.isVisible)
        XCTAssertTrue(widget.collectionView.exists)
    }
}
