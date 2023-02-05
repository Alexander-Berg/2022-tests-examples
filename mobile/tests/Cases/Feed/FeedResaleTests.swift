import MarketUITestMocks
import XCTest

final class FeedResaleTests: LocalMockTestCase {
    func testListSnippet() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6301")
        Allure.addEpic("Выдача")
        Allure.addFeature("Б/У Ресейл")
        Allure.addTitle("Качество уцененного товара и признак ресейла на сниппетах. ")

        let searchText = "iphone"

        var feedPage: FeedPage!

        "Мокаем состояние".ybm_run { _ in
            setupResale()
        }

        "Открываем выдачу".ybm_run { _ in
            feedPage = goToFeed(with: searchText)
        }

        "Проверяем признаки ресейла на этом снипете.".ybm_run { _ in
            let snippetPage = feedPage.collectionView.cellPage(at: IndexPath(item: 0, section: 7))
            feedPage.collectionView.element.ybm_swipe(toFullyReveal: snippetPage.element)
            XCTAssertTrue(snippetPage.titleLabel.isVisible)
            XCTAssertTrue(snippetPage.imageView.isVisible)
            XCTAssertEqual(snippetPage.resaleLabel.label, "Ресейл﻿﻿•﻿﻿Хорошее")
        }

        "Проверяем кнопки на этом снипете.".ybm_run { _ in
            let snippetPage = feedPage.collectionView.cellPage(at: IndexPath(item: 0, section: 7))
            XCTAssertTrue(snippetPage.wishListButton.isVisible)
            XCTAssertTrue(snippetPage.comparsionButton.isVisible)
            XCTAssertTrue(snippetPage.addToCartButton.element.isVisible)
        }
    }
}

// MARK: - Private

private extension FeedResaleTests {
    func setupResale() {
        var defaultState = DefaultState()
        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
        enable(toggles: FeatureNames.marketResaleGoods)
        defaultState.setExperiments(experiments: [.resaleExperiment])
        stateManager?.setState(newState: defaultState)

        var feedState = FeedState()
        feedState.setSearchOrRedirectState(mapper: .init(offers: [.resale], deliveryOverride: nil))
        feedState.setSearchStateFAPI(mapper: .init(offers: [.resale], deliveryOverride: nil))

        stateManager?.setState(newState: feedState)
    }
}
