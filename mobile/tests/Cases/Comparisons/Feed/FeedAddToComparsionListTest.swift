import MarketUITestMocks
import XCTest

class FeedAddToComparsionListTest: LocalMockTestCase {
    func testAddAndRemoveFromComparsionList() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3530")
        Allure.addEpic("Сравнение")
        Allure.addFeature("Поисковая выдача")
        Allure.addTitle("Незалогин. Удаление из поисковой выдачи")

        let search = "iphone"
        var feedPage: FeedPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "FeedSet_Basics")
        }

        "Настраиваем стейт".ybm_run { _ in
            var comparisonState = ComparisonState()
            comparisonState.setComparisonItems(items: [.init(items: [.default], category: .protein)])
            comparisonState.removeItemFromComparison()
            comparisonState.addItemToComparison()
            stateManager?.setState(newState: comparisonState)
        }

        "Открываем выдачу".ybm_run { _ in
            feedPage = goToFeed(with: search)
        }

        let snippetPage = feedPage.collectionView.cellPage(at: 0)

        "Проверяем кнопку сравнения на первой карточке товара".ybm_run { _ in
            feedPage.collectionView.element.ybm_swipe(toFullyReveal: snippetPage.element)
            XCTAssertTrue(snippetPage.comparsionButton.isVisible)
            XCTAssertTrue(snippetPage.comparsionButton.isSelected)
        }

        "Тапаем на кнопку, проверяем тост, возвращаем товар".ybm_run { _ in
            snippetPage.comparsionButton.tap()
            ybm_wait(forFulfillmentOf: {
                !snippetPage.comparsionButton.isSelected
            })

            let popup = RemoveFromComparsionToastPopupPage.currentPopup
            wait(forExistanceOf: popup.element)
            XCTAssertEqual(popup.titleLabel.label, "Товар удалён")

            popup.actionButton.tap()
        }

        "Проверяем, что товар вернулся".ybm_run { _ in
            let popup = AddToComparsionToastPopupPage.currentPopup
            wait(forExistanceOf: popup.element)
            XCTAssertEqual(popup.titleLabel.label, "Товар теперь в списке сравнения")

            XCTAssertTrue(snippetPage.comparsionButton.isSelected)
        }
    }
}
