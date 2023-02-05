import MarketUITestMocks
import XCTest

final class QnAAnswersTest: LocalMockTestCase {

    func testShowAllAnswers() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3513")
        Allure.addEpic("КМ")
        Allure.addFeature("Вопросы и ответы")
        Allure.addTitle("Проверяем просмотр всех ответов к вопросу")

        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
        enable(toggles: FeatureNames.qa_split)

        "Мокаем SKU с большим количеством ВиО".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Experiments_QnA")
            mockStateManager?.pushState(bundleName: "SKUCardSet_QnA_MoreThanThree")
        }

        var skuState = SKUInfoState()

        "Настраиваем стейт".ybm_run { _ in
            skuState.setSkuInfoState(with: .default)
            stateManager?.setState(newState: skuState)
        }

        var sku: SKUPage!

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        var qna: QnAPage!

        "Тестируем тап по кол-ву вопросов".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.qnaFastLink,
                inset: sku.stickyViewInset
            )
            sku.qnaFastLink.tap()
            qna = QnAPage.current
            wait(forVisibilityOf: qna.element)
            XCTAssertEqual(qna.navigationBar.title.label, "Вопросы")
        }

        "Переходим на экран вопроса и проверяем текст нав бара".ybm_run { _ in
            qna.element.ybm_swipeCollectionView(
                toFullyReveal: qna.showAllAnswers,
                inset: sku.stickyViewInset
            )
            qna.showAllAnswers.tap()
            let answerPage = QnAAnswersPage.current
            wait(forVisibilityOf: answerPage.element)
            XCTAssertEqual(answerPage.navigationBar.title.label, "Вопрос")
        }
    }

    func testQuestionWithEmptyAnswers() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3514")
        Allure.addEpic("КМ")
        Allure.addFeature("Вопросы и ответы")
        Allure.addTitle("Проверяем отображение вопроса, у которого нет ответов")

        var sku: SKUPage!

        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
        enable(toggles: FeatureNames.qa_split)

        "Мокаем эксп с ВиО".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Experiments_QnA")
        }

        var skuState = SKUInfoState()

        "Настраиваем стейт".ybm_run { _ in
            skuState.setSkuInfoState(with: .default)
            stateManager?.setState(newState: skuState)
        }

        "Мокаем SKU с ВиО без ответов".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "QnA_Question_EmptyAnswers")
        }

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        var qna: QnAPage!

        "Тестируем тап по кол-ву вопросов".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.qnaFastLink,
                inset: sku.stickyViewInset
            )
            sku.qnaFastLink.tap()
            qna = QnAPage.current
            wait(forVisibilityOf: qna.element)
            XCTAssertEqual(qna.navigationBar.title.label, "Вопросы")
        }

        "Проверяем текст, что пока нет ответов".ybm_run { _ in
            qna.element.ybm_swipeCollectionView(
                toFullyReveal: qna.emptyAnswers,
                inset: sku.stickyViewInset
            )

            XCTAssertTrue(qna.emptyAnswers.isVisible)
            XCTAssertEqual(qna.emptyAnswers.label, "На вопрос пока никто не ответил")
        }
    }

}
