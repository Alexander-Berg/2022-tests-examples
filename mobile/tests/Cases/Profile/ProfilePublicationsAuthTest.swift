import MarketUITestMocks
import XCTest

class ProfilePublicationsAuthTest: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testAuthorizedUserOpenMyOrdersFromMyPublications() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3931")
        Allure.addEpic("ЛК")
        Allure.addFeature("Мои публикации")
        Allure.addTitle("Проверяем зеро-стейт отзывов в моих публикациях")

        var root: RootPage!
        var profile: ProfilePage!
        var myOpinons: MyOpinionsPage!
        var orders: OrdersListPage!

        "Запускаем приложение и авторизуемся".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            profile = goToProfile(root: root)
            wait(forVisibilityOf: profile.collectionView)
        }

        "Мокаем состояния".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Empty_orders")
        }

        "Переходим в \"Мои публикации\". Проверяем, что страница Отзывов в зеро-стейте".ybm_run { _ in
            myOpinons = profile.myPublications.tap()
            ybm_wait(forVisibilityOf: [myOpinons.element, myOpinons.toOrdersButton])
        }

        "Переходим в мои заказы".ybm_run { _ in
            myOpinons.toOrdersButton.tap()
            orders = OrdersListPage.current
            wait(forVisibilityOf: orders.element)
        }
    }

    func testAuthorizedUserOpenMyQuestionsWithZeroQuestions() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3934")
        Allure.addEpic("ЛК")
        Allure.addFeature("Мои публикации")
        Allure.addTitle("Проверяем зеро-стейт вопросов в моих публикациях")

        var root: RootPage!
        var profile: ProfilePage!
        var publications: PublicationsPage!
        var myQuestions: MyQuestionsPage!

        "Запускаем приложение и авторизуемся".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            profile = goToProfile(root: root)
            wait(forVisibilityOf: profile.collectionView)
        }

        "Переходим в \"Мои публикации\".".ybm_run { _ in
            profile.myPublications.tap()
            publications = PublicationsPage.current
        }

        "Переходим в \"Вопросы\". Проверяем, что страница Вопросы в зеро-стейте".ybm_run { _ in
            myQuestions = publications.tapQuestionButton()
            ybm_wait(forVisibilityOf: [myQuestions.element])
            XCTAssertTrue(myQuestions.emptyStateTitle.isVisible)
        }
    }

    func testAuthorizedUserOpenMyAnswersWithZeroQuestions() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3961")
        Allure.addEpic("ЛК")
        Allure.addFeature("Мои публикации")
        Allure.addTitle("Проверяем зеро-стейт ответов в моих публикациях")

        var root: RootPage!
        var profile: ProfilePage!
        var publications: PublicationsPage!
        var myAnswers: MyAnswersPage!

        "Запускаем приложение и авторизуемся".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            profile = goToProfile(root: root)
            wait(forVisibilityOf: profile.collectionView)
        }

        "Переходим в \"Мои публикации\".".ybm_run { _ in
            profile.myPublications.tap()
            publications = PublicationsPage.current
        }

        "Переходим в \"Ответы\". Проверяем, что страница Ответы в зеро-стейте".ybm_run { _ in
            myAnswers = publications.tapAnswersButton()
            ybm_wait(forVisibilityOf: [myAnswers.element])
            XCTAssertTrue(myAnswers.emptyStateTitle.isVisible)
        }
    }

    func testAuthorizedUserDeletedOpinionsInMyopinons() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3939")
        Allure.addEpic("ЛК")
        Allure.addFeature("Мои публикации")
        Allure.addTitle("Проверяем удаление отзывов в моих публикациях")

        var root: RootPage!
        var profile: ProfilePage!
        var myOpinons: MyOpinionsPage!

        "Запускаем приложение и авторизуемся".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            profile = goToProfile(root: root)
            wait(forVisibilityOf: profile.collectionView)
        }

        "Мокаем состояния".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "MyReviews")
        }

        "Переходим в \"Мои публикации\". Попадаем на не пустую страницу отзывов".ybm_run { _ in
            myOpinons = profile.myPublications.tap()
            ybm_wait(forVisibilityOf: [myOpinons.element, myOpinons.collectionView])
        }

        "Открываем котекстное меню. Выбираем удалить отзыв. Тестируем отмену".ybm_run { _ in
            myOpinons.openContextMenuButton.tap()
            let contextMenuPopup = ContextMenuPopupPage.currentPopup
            contextMenuPopup.delete.tap()

            let confirmPopup = ConfirmPopupPage.currentPopup
            confirmPopup.rightButton.tap()
            XCTAssertTrue(myOpinons.collectionView.isVisible)
        }

        "Мокаем удаление отзыва".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "RemoveReview")
        }

        "Открываем котекстное меню. Выбираем удалить отзыв. Тестируем удаление отзыва".ybm_run { _ in
            myOpinons.openContextMenuButton.tap()
            let contextMenuPopup = ContextMenuPopupPage.currentPopup
            contextMenuPopup.delete.tap()

            let confirmPopup = ConfirmPopupPage.currentPopup
            confirmPopup.leftButton.tap()
            XCTAssertFalse(myOpinons.collectionView.isVisible)
        }
    }

    func testAuthorizedUserDeletedAnswerInMyAnswers() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3963")
        Allure.addEpic("ЛК")
        Allure.addFeature("Мои публикации")
        Allure.addTitle("Проверяем удаление ответов в моих публикациях")

        var root: RootPage!
        var profile: ProfilePage!
        var publications: PublicationsPage!
        var myAnswers: MyAnswersPage!

        "Запускаем приложение и авторизуемся".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            profile = goToProfile(root: root)
            wait(forVisibilityOf: profile.collectionView)
        }

        "Мокаем состояния".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "MyAnswers")
        }

        "Переходим в \"Мои публикации\".".ybm_run { _ in
            profile.myPublications.tap()
            publications = PublicationsPage.current
        }

        "Переходим в \"Ответы\". Попадаем на не пустую страницу ответов".ybm_run { _ in
            myAnswers = publications.tapAnswersButton()
            ybm_wait(forVisibilityOf: [myAnswers.element, myAnswers.collectionView])
        }

        "Открываем меню подтверждения удаления. Тестируем отмену".ybm_run { _ in
            myAnswers.openConfirmMenuButton.tap()

            let confirmPopup = ConfirmPopupPage.currentPopup
            confirmPopup.rightButton.tap()
            XCTAssertTrue(myAnswers.collectionView.isVisible)
        }

        "Мокаем удаление ответа".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "RemoveAnswer")
        }

        "Открываем меню подтверждения удаления. Тестируем удаление ответа".ybm_run { _ in
            myAnswers.openConfirmMenuButton.tap()

            let confirmPopup = ConfirmPopupPage.currentPopup
            wait(forVisibilityOf: confirmPopup.element)
            confirmPopup.leftButton.tap()
            wait(forVisibilityOf: myAnswers.collectionView)
        }

    }

    func testAuthorizedUserMoveToSKUCardAndEditOpinonInMyOpinons() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3932")
        Allure.addEpic("ЛК")
        Allure.addFeature("Мои публикации")
        Allure.addTitle("Проверяем  переход в КМ и редактирование отзыва в моих публикациях")

        var root: RootPage!
        var profile: ProfilePage!
        var myOpinons: MyOpinionsPage!

        "Запускаем приложение и авторизуемся".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            profile = goToProfile(root: root)
            wait(forVisibilityOf: profile.collectionView)
        }

        "Мокаем состояния".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "MyReviews")
            mockStateManager?.pushState(bundleName: "SkuCard")
        }

        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            var config = CustomSKUConfig(
                productId: 386_962_128,
                skuId: 386_962_128,
                offerId: "P7T_etH6VfZ4Osh7LNVq6w"
            )
            config.title = "Galaxy S10+ Ceramic 12/1024GB"
            skuState.setSkuInfoState(with: .custom(config))
            stateManager?.setState(newState: skuState)
        }

        "Переходим в \"Мои публикации\". Попадаем на не пустую страницу отзывов".ybm_run { _ in
            profile.collectionView.ybm_swipeCollectionView(toFullyReveal: profile.myPublications.element)
            myOpinons = profile.myPublications.tap()
            ybm_wait(forVisibilityOf: [myOpinons.element, myOpinons.collectionView])
        }

        "Тестируем  переход на верную КМ".ybm_run { _ in
            myOpinons.modelInfoCell.tap()
            let sku = SKUPage.current
            XCTAssertEqual(sku.title.text, Constants.skuTitle)
            sku.navigationBar.backButton.tap()
        }

        "Открываем контекстное меню. Тестируем редактирование отзыва".ybm_run { _ in
            myOpinons.openContextMenuButton.tap()
            let contextMenuPopup = ContextMenuPopupPage.currentPopup
            contextMenuPopup.edit.tap()

            let leaveOpinion = LeaveOpinionPopUp.currentPopup
            let text = Constants.reviewText

            leaveOpinion.prosText.typeText(text)
            leaveOpinion.consText.typeText(text)
            leaveOpinion.commentText.typeText(text)

            let opinionCreationMultifactorPage = leaveOpinion.tapContinueButton()
            let opinionCreationSucceededPage = opinionCreationMultifactorPage.tapContinueButton()
            opinionCreationSucceededPage.succeededButton.tap()

            XCTAssertTrue(myOpinons.moderationStateCell.isVisible)
            XCTAssertEqual(myOpinons.reviewText.text, Constants.newReviewText)
        }
    }

    func testAuthorizedUserMoveToSKUCardAndVoteAnswerInMyAnswers() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3962")
        Allure.addEpic("ЛК")
        Allure.addFeature("Мои публикации")
        Allure.addTitle("Проверяем  переход в КМ, лайк/дизлайк, переход по комментарию ответа в моих публикациях")

        var root: RootPage!
        var profile: ProfilePage!
        var publications: PublicationsPage!
        var myAnswers: MyAnswersPage!

        "Запускаем приложение и авторизуемся".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            profile = goToProfile(root: root)
            wait(forVisibilityOf: profile.collectionView)
        }

        "Мокаем состояния".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "MyAnswers")
            mockStateManager?.pushState(bundleName: "SkuCard")
            mockStateManager?.pushState(bundleName: "QnA")
        }

        "Переходим в \"Мои публикации\".".ybm_run { _ in
            profile.collectionView.ybm_swipeCollectionView(toFullyReveal: profile.myPublications.element)
            profile.myPublications.tap()
            publications = PublicationsPage.current
        }

        "Переходим в \"Ответы\". Попадаем на не пустую страницу ответов".ybm_run { _ in
            myAnswers = publications.tapAnswersButton()
            ybm_wait(forVisibilityOf: [myAnswers.element, myAnswers.collectionView])
        }

        "Тестируем переход на верную КМ".ybm_run { _ in
            myAnswers.modelInfoCell.tap()
            let sku = SKUPage.current
            XCTAssertEqual(sku.title.text, Constants.skuTitle)
            sku.navigationBar.backButton.tap()
        }

        "Тестируем лайк/дизлайк".ybm_run { _ in
            myAnswers.likeButton.tap()
            XCTAssertEqual(myAnswers.likeCount.text, "1")
            myAnswers.likeButton.tap()
            XCTAssertEqual(myAnswers.likeCount.text, "0")

            myAnswers.dislikeButton.tap()
            XCTAssertEqual(myAnswers.dislikeCount.text, "1")
            myAnswers.dislikeButton.tap()
            XCTAssertEqual(myAnswers.dislikeCount.text, "0")
        }

        "Тестируем нажатие \"Показать N комментариев\"".ybm_run { _ in
            let qnaAnswersPage = myAnswers.tapShowCommentsButton()
            qnaAnswersPage.showCommentsButton.tap()
            XCTAssertEqual(qnaAnswersPage.showCommentsButton.label, Constants.commentCounterText)
        }
    }

    func testAuthorizedUserMoveToSKUCardVoteQuestionAndAnswersInMyQuestions() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3936")
        Allure.addEpic("ЛК")
        Allure.addFeature("Мои публикации")
        Allure.addTitle("Проверяем  переход в КМ, лайк и количество ответом вопроса в моих публикациях")

        var root: RootPage!
        var profile: ProfilePage!
        var publications: PublicationsPage!
        var myQuestions: MyQuestionsPage!

        "Мокаем состояния".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "MyQuestions")
            mockStateManager?.pushState(bundleName: "SkuCard")
            mockStateManager?.pushState(bundleName: "QnA")
        }

        "Запускаем приложение и авторизуемся".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            profile = goToProfile(root: root)
            wait(forVisibilityOf: profile.collectionView)
        }

        "Переходим в \"Мои публикации\".".ybm_run { _ in
            profile.collectionView.ybm_swipeCollectionView(toFullyReveal: profile.myPublications.element)
            profile.myPublications.tap()
            publications = PublicationsPage.current
        }

        "Переходим в \"Вопросы\". Попадаем на не пустую страницу вопросов".ybm_run { _ in
            myQuestions = publications.tapQuestionButton()
            ybm_wait(forVisibilityOf: [myQuestions.element, myQuestions.collectionView])
        }

        "Тестируем переход на верную КМ".ybm_run { _ in
            myQuestions.modelInfoCell.tap()
            let sku = SKUPage.current
            XCTAssertEqual(sku.title.text, Constants.skuTitle)
            sku.navigationBar.backButton.tap()
        }

        "Тестируем лайк".ybm_run { _ in
            myQuestions.likeButton.tap()
            XCTAssertEqual(myQuestions.likeCount.text, "1")
            myQuestions.likeButton.tap()
            XCTAssertEqual(myQuestions.likeCount.text, "0")
        }

        "Тестируем нажатие \"Показать N ответов\"".ybm_run { _ in
            let qnaAnswers = myQuestions.tapShowAnswersButton()
            ybm_wait(forVisibilityOf: [qnaAnswers.element, qnaAnswers.collectionView])

            for index in 0 ..< 2 {
                XCTAssertTrue(
                    qnaAnswers
                        .cells
                        .matching(
                            identifier:
                            QnAAnswersCollectionViewCellAccessibility.baseIdentifier
                                + "-\(index + 1)-0-"
                                + QnAAnswersAccessibility.answerAuthorCell
                                + "-\(index)"
                        )
                        .firstMatch
                        .isVisible
                )
            }
        }
    }
}

// MARK: - NestedTypes

extension ProfilePublicationsAuthTest {

    enum Constants {

        static let skuTitle = "Galaxy S10+ Ceramic 12/1024GB"
        static let reviewText = "(Не очень)"
        static let newReviewText =
            "Опыт использования:\n\n \nМеньше месяца\n \nДостоинства:\n\n \n(Не очень)Удобный\n \nНедостатки:\n\n \n(Не очень)Не обнаружил\n \nКомментарий:\n\n \n(Не очень)"
        static let commentCounterText = "1 комментарий"
    }
}
