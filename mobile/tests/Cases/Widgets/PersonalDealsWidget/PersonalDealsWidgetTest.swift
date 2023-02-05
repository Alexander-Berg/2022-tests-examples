import Foundation
import XCTest

final class PersonalDealsWidgetTest: LocalMockTestCase {
    func testWidgetSnippetsAppearance() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3011")
        Allure.addEpic("Виджеты")
        Allure.addTitle("Все со скидкой")

        var mordaPage: MordaPage!
        var widget: MordaPage.PersonalDealsWidget!

        "Открываем морду".ybm_run { _ in
            mordaPage = goToMorda()
        }

        "Скроллим к виджету".ybm_run { _ in
            widget = mordaPage.personalDealsWidget
            widget.collectionView.ybm_swipeCollectionView(toFullyReveal: widget.title)
        }

        "Проверяем отображение".ybm_run { _ in
            XCTAssertEqual(widget.title.label, "Всё со скидкой")

            var previousSnippets: [String] = []
            func testSnippet(snippetInfo: SnippetInfo) {
                let snippet = widget.cellPage(after: previousSnippets)
                previousSnippets.append(snippet.element.identifier)

                verifySnippet(
                    in: widget.collectionView,
                    snippet: snippet,
                    snippetInfo: snippetInfo
                )
            }

            let mockData = [
                makeMainSnippetInfo(),
                makeTopRightSnippetInfo(),
                makeBottomRightSnippetInfo()
            ]
            mockData.forEach(testSnippet)
        }
    }

    private func verifySnippet(
        in collectionView: XCUIElement,
        snippet: SnippetPage,
        snippetInfo: SnippetInfo
    ) {
        collectionView.ybm_swipeCollectionView(toFullyReveal: snippet.image)
        if snippetInfo.hasGift {
            XCTAssertTrue(snippet.giftView.imageView.element.isVisible)
        } else {
            XCTAssertFalse(snippet.giftView.imageView.element.isVisible)
        }
    }

    private func makeMainSnippetInfo() -> SnippetInfo {
        SnippetInfo(
            price: "999 ₽",
            discountPercent: "- 37%",
            oldPrice: "1 590 ₽",
            starsValue: "",
            reviewsCountLabel: "",
            skuName: "Tide капсулы 3 in 1 Pods Color, контейнр, 2 уп., 30 шт.",
            isInCart: false,
            reasonsText: nil,
            isSoldOut: false,
            hasGift: false,
            hasCheapestAsGift: false
        )
    }

    private func makeTopRightSnippetInfo() -> SnippetInfo {
        SnippetInfo(
            price: "118 ₽",
            discountPercent: "- 33%",
            oldPrice: "175 ₽",
            starsValue: "",
            reviewsCountLabel: "",
            skuName: "GARNIER Color Naturals стойкая",
            isInCart: false,
            reasonsText: nil,
            isSoldOut: false,
            hasGift: false,
            hasCheapestAsGift: false
        )
    }

    private func makeBottomRightSnippetInfo() -> SnippetInfo {
        SnippetInfo(
            price: "220 ₽",
            discountPercent: "- 38%",
            oldPrice: "352 ₽",
            starsValue: "",
            reviewsCountLabel: "",
            skuName: "Туалетная бумага Papia белая",
            isInCart: false,
            reasonsText: nil,
            isSoldOut: false,
            hasGift: false,
            hasCheapestAsGift: false
        )
    }
}
