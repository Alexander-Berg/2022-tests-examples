import XCTest

class FeedTransitionToComparisonListTest: LocalMockTestCase {
    func testTransitionToComparisonListFromFeed() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3528")
        Allure.addEpic("Сравнение")
        Allure.addFeature("Поисковая выдача")
        Allure.addTitle("Незалогин. Добавление из поисковой выдачи")

        let search = "iphone"

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Comparisons_FeedTransitionToList")
        }

        var feedPage: FeedPage!
        let popup = AddToComparsionToastPopupPage.currentPopup
        var comparisonPage: ComparisonPage!

        "Открываем выдачу".ybm_run { _ in
            feedPage = goToFeed(with: search)
        }

        let snippetPage = feedPage.collectionView.cellPage(at: 0)

        "Проверяем кнопку сравнения на первой карточке товара".ybm_run { _ in
            feedPage.collectionView.element.ybm_swipe(toFullyReveal: snippetPage.element)
            wait(forVisibilityOf: snippetPage.comparsionButton)
            XCTAssertFalse(snippetPage.comparsionButton.isSelected)
        }

        "Добавляем товар в список сравнения".ybm_run { _ in
            snippetPage.comparsionButton.tap()

            wait(forExistanceOf: popup.element)
            wait(forExistanceOf: popup.titleLabel)
            XCTAssertEqual(popup.titleLabel.label, "Товар теперь в списке сравнения")

            XCTAssertTrue(snippetPage.comparsionButton.isSelected)
        }

        "Переходим в список сравнения".ybm_run { _ in
            popup.actionButton.tap()

            let elem = XCUIApplication()
                .otherElements
                .matching(identifier: ComparisonAccessibility.root)
                .firstMatch
            wait(forExistanceOf: elem)
            comparisonPage = ComparisonPage(element: elem)
        }

        "Проверяем данные в КМ".ybm_run { _ in
            let cellPage = comparisonPage.collectionView.modelCell(with: 0)
            wait(forExistanceOf: cellPage.element)
            wait(forExistanceOf: cellPage.photo.element)
            wait(forExistanceOf: cellPage.title.element)
            XCTAssertEqual(cellPage.title.element.label, "Планшет Apple iPad (2018) 128Gb Wi-Fi + Cellular, silver")
        }
    }
}
