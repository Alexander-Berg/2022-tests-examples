import MarketUITestMocks
import XCTest

final class SKUCardModelOpinionsAuthTest: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testTapReply() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-1005")
        Allure.addEpic("КМ")
        Allure.addFeature("Отзывы")
        Allure.addTitle("Проверяем тап на \"Ответить\" в блоке отзывов")

        var root: RootPage!
        var sku: SKUPage!
        var commentView: WriteCommentPage!
        var opinionDetailsPage: OpinionDetailsPage!

        let inputText = "some text"

        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)

        "Настраиваем стейт".run {
            var skuState = SKUInfoState()
            skuState.setSkuInfoState(with: .default)
            stateManager?.setState(newState: skuState)
        }

        "Запускаем приложение и авторизуемся".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
        }

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage(root: root)
        }

        "Раскрываем блок отзывов".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.opinionsHeader,
                inset: sku.stickyViewInset
            )
            sku.opinionsHeader.tap()
        }

        "Тестируем кнопку для ответа: вводим текст комментария, закрываем экран".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.opinionReplyButton().target.element,
                inset: sku.stickyViewInset
            )
            XCTAssertEqual(sku.opinionReplyButton().target.element.label, "Ответить")

            // переход к комментарию для ответа
            commentView = checkCommentView(sku: sku, text: inputText)

            // закрываем экран ответа
            commentView.navigationBar.closeButton.tap()

            ybm_wait(forVisibilityOf: [sku.element])
        }

        "Мокаем добавление комментария".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_AddComment")
        }

        "Открываем заново, вводим текст и отправляем отзыв".ybm_run { _ in
            // значение осталось таким же
            commentView = checkCommentView(sku: sku, text: inputText)
            // отправляем комментарий
            opinionDetailsPage = commentView.sendButton.tap()
        }

        "Проверяем отображение экрана отзыва с добавленным ответом".ybm_run { _ in
            // проверяем наличие страницы отзыва
            wait(forVisibilityOf: opinionDetailsPage.element)
            let cellPage = opinionDetailsPage.cellPage(at: IndexPath(item: 3, section: 1))
            // проверяем новый комментарий
            opinionDetailsPage.collectionView.ybm_swipeCollectionView(toFullyReveal: cellPage.element)
            XCTAssertEqual(cellPage.text.text, inputText)
        }
    }
}

private extension SKUCardModelOpinionsAuthTest {
    /// Функция для проверки экрана ввода ответа на комментарий ДО и ПОСЛЕ ввода текста
    func checkCommentView(sku: SKUPage, text: String) -> WriteCommentPage {
        let commentView = sku.opinionReplyButton().target.tap()
        wait(forVisibilityOf: commentView.element)
        XCTAssert(!commentView.inputText.text.contains(text))

        // тап для активации ввода
        commentView.inputText.tap()
        commentView.inputText.typeText(text)
        XCTAssert(commentView.inputText.text.contains(text))

        return commentView
    }
}
