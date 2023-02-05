import MarketUITestMocks
import XCTest

final class SKUCardModelQnATest: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testEmptyState() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3642")
        Allure.addEpic("КМ")
        Allure.addFeature("Вопросы и ответы")
        Allure.addTitle("Проверяем пустое состояние")

        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
        enable(toggles: FeatureNames.qa_split)

        "Мокаем SKU с большим количеством ВиО".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Experiments_QnA")
            mockStateManager?.pushState(bundleName: "SKUCardSet_QnA_Empty")
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

        "Открываем экран списка вопросов".ybm_run { _ in
            sku.qnaFastLink.tap()
            qna = QnAPage.current
            XCTAssertEqual(qna.navigationBar.title.label, "Вопросы")
            XCTAssertTrue(qna.emptyView.isVisible)
            XCTAssertTrue(
                qna
                    .emptyView
                    .staticTexts
                    .element(withLabelMatching: "Вопросы о товаре")
                    .isVisible
            )
            XCTAssertTrue(
                qna
                    .emptyView
                    .staticTexts
                    .element(
                        withLabelMatching: "Их пока нет, и ваш может стать первым. Вам ответят покупатели или продавцы."
                    )
                    .isVisible
            )
            XCTAssertEqual(qna.emptyViewAskQuestionButton.label, "Задать вопрос")
        }

        "Открываем форму для вопроса".ybm_run { _ in
            qna.emptyViewAskQuestionButton.tap()
            XCTAssertEqual(QnAFormPage.current.navigationBar.title.label, "Задать вопрос")
        }
    }

    func testTapQnAFastLink() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3506")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3519")
        Allure.addEpic("КМ")
        Allure.addFeature("Вопросы и ответы")
        Allure.addTitle("Проверяем тап по количеству Q&A")

        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
        enable(toggles: FeatureNames.qa_split)

        "Мокаем SKU с большим количеством ВиО".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Experiments_QnA")
            mockStateManager?.pushState(bundleName: "SKUCardSet_QnA_LessThanThree")
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
            XCTAssertEqual(qna.navigationBar.title.label, "Вопросы")
        }

        "Проверяем кнопку назад".ybm_run { _ in
            wait(forVisibilityOf: qna.navigationBar.backButton)
        }

        "Проверяем, что не пустой formController".ybm_run { _ in
            XCTAssertNotEqual(qna.collectionView.cells.count, 0)
        }

    }

    func testLessThanThreeQnA() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3507")
        Allure.addEpic("КМ")
        Allure.addFeature("Вопросы и ответы")
        Allure.addTitle("Проверяем отображение SKU когда ВиО меньше 3")

        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
        enable(toggles: FeatureNames.qa_split)

        "Мокаем SKU c двумя ВиО".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Experiments_QnA")
            mockStateManager?.pushState(bundleName: "SKUCardSet_QnA_LessThanThree")
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

        "Тестируем заголовок секции ВиО".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.qnaHeader,
                inset: sku.stickyViewInset
            )
            sku.qnaHeader.tap()
            XCTAssertTrue(sku.qnaHeader.isVisible)
            XCTAssertEqual(sku.qnaHeader.text, "Вопросы о товаре (2)")
        }

        "Скролим до конца ВиО".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.disclaimer,
                inset: sku.stickyViewInset
            )
            XCTAssertFalse(sku.qnaSeeAllButton.isVisible)
        }
    }

    func testMoreThanThreeQnA() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3508")
        Allure.addEpic("КМ")
        Allure.addFeature("Вопросы и ответы")
        Allure.addTitle("Проверяем отображение SKU когда ВиО больше 3")

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

        "Тестируем заголовок секции ВиО".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.qnaHeader,
                inset: sku.stickyViewInset
            )
            sku.qnaHeader.tap()
            XCTAssertTrue(sku.qnaHeader.isVisible)
            XCTAssertEqual(sku.qnaHeader.text, "Вопросы о товаре (121)")
        }

        "Скролим до конца ВиО".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.disclaimer,
                inset: sku.stickyViewInset
            )
            wait(forVisibilityOf: sku.qnaSeeAllButton)
        }
    }

    func testSeeAllQnA() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3509")
        Allure.addEpic("КМ")
        Allure.addFeature("Вопросы и ответы")
        Allure.addTitle("Проверяем отображение SKU когда ВиО больше 3")

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

        "Скролим до конца ВиО".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.disclaimer,
                inset: sku.stickyViewInset
            )
            wait(forVisibilityOf: sku.qnaSeeAllButton)

            sku.qnaSeeAllButton.tap()
            XCTAssertEqual(QnAPage.current.navigationBar.title.label, "Вопросы")
        }
    }

    func testAddCommentButton() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3634")
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3635")
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3637")
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3638")
        Allure.addEpic("КМ")
        Allure.addFeature("Вопросы и ответы")
        Allure.addTitle("Проверяем форму комментария к ответу")

        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
        enable(toggles: FeatureNames.qa_split)

        "Мокаем SKU с большим количеством ВиО".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Experiments_QnA")
            mockStateManager?.pushState(bundleName: "SKUCardSet_QnA_LessThanThree")
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
            XCTAssertEqual(qna.navigationBar.title.label, "Вопросы")
        }

        "Скролим к кнопке \"Комментировать\"".ybm_run { _ in
            qna.element.ybm_swipeCollectionView(
                toFullyReveal: qna.addCommentButton,
                inset: sku.stickyViewInset
            )
            XCTAssertTrue(qna.addCommentButton.isVisible)
            XCTAssertEqual(qna.addCommentButton.label, "Комментировать")
        }

        var qnaForm: QnAFormPage!

        "Проверяем форму для комментария".ybm_run { _ in
            qna.addCommentButton.tap()
            qnaForm = QnAFormPage.current

            XCTAssertTrue(qnaForm.submitButton.isVisible)
            XCTAssertEqual(qnaForm.submitButton.label, "Добавить комментарий")

            XCTAssert(qnaForm.navigationBar.title.isVisible)
            XCTAssertEqual(qnaForm.navigationBar.title.label, "Комментировать")

            XCTAssert(qnaForm.navigationBar.closeButton.isVisible)

            XCTAssertEqual(qnaForm.charactersCountLabel.label, "2000")
            XCTAssert(qnaForm.charactersCountLabel.isVisible)

            XCTAssertEqual(qnaForm.inputTextView.element.text, "")
            XCTAssert(qnaForm.inputTextView.element.isVisible)

            XCTAssert(KeyboardPage.current.element.isVisible)
        }

        "Проверяем работу каунтера".ybm_run { _ in
            qnaForm.inputTextView.element.typeText("test")
            XCTAssertEqual(qnaForm.charactersCountLabel.label, "1996")
            qnaForm.inputTextView.element.ybm_clearText()
            XCTAssertEqual(qnaForm.charactersCountLabel.label, "2000")
        }

        "Вводим текст больше допустимого и нажимаем \"Добавить комментарий\"".ybm_run { _ in
            let maxCharCount = 2_000
            qnaForm.inputTextView.typeText(String(repeating: "0", count: maxCharCount + 1))
            ybm_wait { qnaForm.charactersCountLabel.label == "-1" }
            qnaForm.submitButton.tap()
            let popup = DefaultToastPopupPage.currentPopup
            wait(forVisibilityOf: popup.element)
            XCTAssertEqual(
                popup.text.label,
                "Комментарий слишком длинный, попробуйте сократить его до 2 000 символов"
            )
        }

        "Нажимаем на кнопку, когда пустая форма".ybm_run { _ in
            qnaForm.inputTextView.clearText()
            qnaForm.submitButton.tap()

            wait(forVisibilityOf: qnaForm.navigationBar.title)
            XCTAssertEqual(qnaForm.navigationBar.title.label, "Комментировать")

            wait(forVisibilityOf: qnaForm.navigationBar.closeButton)
            let hittablePredicate = NSPredicate(format: "isHittable == true")
            wait(forFulfillmentOf: hittablePredicate, for: qnaForm.navigationBar.closeButton)
        }

        "Закрываем форму".ybm_run { _ in
            qnaForm.navigationBar.closeButton.tap()
            wait(forVisibilityOf: qna.addCommentButton)
        }

    }

    func testAddAnswerButton() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3625")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3626")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3627")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3628")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3629")
        Allure.addEpic("КМ")
        Allure.addFeature("Вопросы и ответы")
        Allure.addTitle("Проверяем форму ответа на вопрос")

        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
        enable(toggles: FeatureNames.qa_split)

        "Мокаем SKU c двумя ВиО".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Experiments_QnA")
            mockStateManager?.pushState(bundleName: "SKUCardSet_QnA_LessThanThree")
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

        "Скролим к первой кнопке \"Ответить\"".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.qnaAddAnswer,
                inset: sku.stickyViewInset
            )
            XCTAssertTrue(sku.qnaAddAnswer.isVisible)
            XCTAssertEqual(sku.qnaAddAnswer.label, "Ответить")
        }

        var qnaForm: QnAFormPage!

        "Переходим на форму ответа на вопрос".ybm_run { _ in
            sku.qnaAddAnswer.tap()
            qnaForm = QnAFormPage.current

            XCTAssertTrue(qnaForm.descriptionLabel.isVisible)

            XCTAssertTrue(qnaForm.submitButton.isVisible)
            XCTAssertEqual(qnaForm.submitButton.label, "Добавить ответ")

            XCTAssert(qnaForm.navigationBar.title.isVisible)
            XCTAssertEqual(qnaForm.navigationBar.title.label, "Ответить")
            XCTAssert(qnaForm.navigationBar.closeButton.isVisible)

            XCTAssertEqual(qnaForm.charactersCountLabel.label, "5000")
            XCTAssert(qnaForm.charactersCountLabel.isVisible)

            XCTAssertEqual(qnaForm.inputTextView.element.text, "")
            XCTAssert(qnaForm.inputTextView.element.isVisible)

            XCTAssert(KeyboardPage.current.element.isVisible)
        }

        "Проверяем работу каунтера".ybm_run { _ in
            qnaForm.inputTextView.element.typeText("test")
            XCTAssertEqual(qnaForm.charactersCountLabel.label, "4996")
            qnaForm.inputTextView.element.ybm_clearText()
            XCTAssertEqual(qnaForm.charactersCountLabel.label, "5000")
        }

        "Вводим текст больше допустимого и нажимаем \"Отправить\"".ybm_run { _ in
            let maxCharCount = 5_000
            qnaForm.inputTextView.typeText(String(repeating: "0", count: maxCharCount + 1))
            ybm_wait { qnaForm.charactersCountLabel.label == "-1" }
            qnaForm.submitButton.tap()
            let popup = DefaultToastPopupPage.currentPopup
            wait(forVisibilityOf: popup.element)
            XCTAssertEqual(
                popup.text.label,
                "Ответ слишком длинный, попробуйте сократить его до 5 000 символов"
            )
        }

        "Нажимаем на кнопку, когда пустая форма".ybm_run { _ in
            qnaForm.inputTextView.clearText()
            qnaForm.submitButton.tap()
            XCTAssertEqual(qnaForm.navigationBar.title.label, "Ответить")
            ybm_wait(forVisibilityOf: [qnaForm.navigationBar.closeButton])
        }

        "Закрываем форму".ybm_run { _ in
            qnaForm.navigationBar.closeButton.tap()
            wait(forVisibilityOf: sku.element)
        }

    }

    func testAskQuestionButton() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3615")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3616")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3617")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3618")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3619")
        Allure.addEpic("КМ")
        Allure.addFeature("Вопросы и ответы")
        Allure.addTitle("Проверяем форму задания вопроса")

        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
        enable(toggles: FeatureNames.qa_split)

        "Мокаем SKU c двумя ВиО".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Experiments_QnA")
            mockStateManager?.pushState(bundleName: "SKUCardSet_QnA_LessThanThree")
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

        "Скролим к кнопке \"Задать вопрос\"".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.qnaAskButton,
                inset: sku.stickyViewInset
            )
            XCTAssertTrue(sku.qnaAskButton.isVisible)
            XCTAssertEqual(sku.qnaAskButton.label, "Задать вопрос")

        }

        var qnaForm: QnAFormPage!

        "Проверяем форму для вопроса".ybm_run { _ in
            sku.qnaAskButton.tap()
            qnaForm = QnAFormPage.current

            XCTAssertTrue(qnaForm.submitButton.isVisible)
            XCTAssertEqual(qnaForm.submitButton.label, "Отправить")

            XCTAssert(qnaForm.navigationBar.title.isVisible)
            XCTAssertEqual(qnaForm.navigationBar.title.label, "Задать вопрос")

            XCTAssert(qnaForm.navigationBar.closeButton.isVisible)

            XCTAssertEqual(qnaForm.charactersCountLabel.label, "5000")
            XCTAssert(qnaForm.charactersCountLabel.isVisible)

            XCTAssertEqual(qnaForm.inputTextView.element.text, "")
            XCTAssert(qnaForm.inputTextView.element.isVisible)

            XCTAssert(KeyboardPage.current.element.isVisible)
        }

        "Проверяем работу каунтера".ybm_run { _ in
            qnaForm.inputTextView.element.typeText("test")
            XCTAssertEqual(qnaForm.charactersCountLabel.label, "4996")
            qnaForm.inputTextView.element.ybm_clearText()
            XCTAssertEqual(qnaForm.charactersCountLabel.label, "5000")
        }

        "Вводим текст больше допустимого и нажимаем \"Отправить\"".ybm_run { _ in
            let maxCharCount = 5_000
            qnaForm.inputTextView.typeText(String(repeating: "0", count: maxCharCount + 1))
            ybm_wait { qnaForm.charactersCountLabel.label == "-1" }
            qnaForm.submitButton.tap()
            let popup = DefaultToastPopupPage.currentPopup
            wait(forVisibilityOf: popup.element)
            XCTAssertEqual(
                popup.text.label,
                "Вопрос слишком длинный, попробуйте сократить его до 5 000 символов"
            )
        }

        "Нажимаем на кнопку, когда пустая форма".ybm_run { _ in
            qnaForm.inputTextView.clearText()
            qnaForm.submitButton.tap()

            wait(forVisibilityOf: qnaForm.navigationBar.title)
            XCTAssertEqual(qnaForm.navigationBar.title.label, "Задать вопрос")

            wait(forVisibilityOf: qnaForm.navigationBar.closeButton)
            let hittablePredicate = NSPredicate(format: "isHittable == true")
            wait(forFulfillmentOf: hittablePredicate, for: qnaForm.navigationBar.closeButton)
        }

        "Закрываем форму".ybm_run { _ in
            qnaForm.navigationBar.closeButton.tap()
            wait(forVisibilityOf: sku.qnaAskButton)
        }
    }
}
