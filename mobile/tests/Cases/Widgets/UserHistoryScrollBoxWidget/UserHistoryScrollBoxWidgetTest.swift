import XCTest

final class UserHistoryScrollBoxWidgetTest: LocalMockTestCase {

    func testWidgetSnippetsAppearance() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3008")
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3851")
        Allure.addEpic("Виджеты")
        Allure.addFeature("Scrollbox")
        Allure.addTitle("История просмотров")

        var mordaPage: MordaPage!
        var widget: MordaPage.HistoryWidget!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "UserHistoryScrollBoxWidgetTest_Basic")
        }

        "Открываем морду".ybm_run { _ in
            mordaPage = goToMorda()
        }

        "Скроллим к скроллбоксу".ybm_run { _ in
            widget = mordaPage.historyWidget

            mordaPage.element.ybm_swipeCollectionView(toFullyReveal: widget.title)
            XCTAssertEqual(widget.title.label, "История просмотров")

            mordaPage.element.ybm_swipeCollectionView(toFullyReveal: widget.container.element)
        }

        "Проверяем добавление товара в козину".ybm_run { _ in
            let snippet = widget.container.cellPage(at: IndexPath(item: 0, section: 0))
            let button = snippet.addToCartButton

            mockStateManager?.pushState(bundleName: "UserHistoryScrollBoxWidgetTest_GiftInCart")

            button.element.tap()

            ybm_wait(forFulfillmentOf: {
                button.element.label == "1"
            })

            XCTAssertTrue(button.plusButton.isVisible)
            XCTAssertTrue(button.minusButton.isVisible)
            ybm_wait(forFulfillmentOf: { TabBarPage.current.cartTabItem.element.label == "Корзина2" })
        }

        "Проверяем отображение виджета".ybm_run { _ in
            let snippetsInfo = generateSnippetsInfo()

            verifySnippets(snippets: snippetsInfo, container: widget.container)
        }
    }

    private func verifySnippets<T: SnippetPage>(snippets: [SnippetInfo], container: ScrollBoxSnipppetContainerPage<T>) {
        container.enumerateCells(cellCount: snippets.count) { snippet, indexPath in
            let snippetInfo = snippets[indexPath.item]

            if snippetInfo.hasGift {
                XCTAssertTrue(snippet.giftView.imageView.element.isVisible)
            } else {
                XCTAssertFalse(snippet.giftView.imageView.element.isVisible)
            }
        }
    }

    private func generateSnippetsInfo() -> [SnippetInfo] {
        [
            SnippetInfo(
                price: "",
                discountPercent: nil,
                oldPrice: nil,
                starsValue: "",
                reviewsCountLabel: "",
                skuName: "",
                isInCart: true,
                reasonsText: nil,
                isSoldOut: false,
                hasGift: true,
                hasCheapestAsGift: false
            ),
            SnippetInfo(
                price: "",
                discountPercent: nil,
                oldPrice: nil,
                starsValue: "",
                reviewsCountLabel: "",
                skuName: "",
                isInCart: true,
                reasonsText: nil,
                isSoldOut: false,
                hasGift: false,
                hasCheapestAsGift: false
            ),
            SnippetInfo.makeEmpty(),
            SnippetInfo(
                price: "",
                discountPercent: nil,
                oldPrice: nil,
                starsValue: "",
                reviewsCountLabel: "",
                skuName: "",
                isInCart: true,
                reasonsText: nil,
                isSoldOut: false,
                hasGift: false,
                hasCheapestAsGift: false
            ),
            SnippetInfo.makeEmpty()
        ]
    }
}
