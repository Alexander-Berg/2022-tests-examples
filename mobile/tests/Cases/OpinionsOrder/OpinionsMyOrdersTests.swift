import MarketUITestMocks
import XCTest

class OpinionsMyOrdersTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    // MARK: - Public

    var root: RootPage!
    var profile: ProfilePage!
    var opinionCreation: LeaveOpinionPopUp!
    var opinionCreationMultifactorPage: OpinionCreationMultifactorPage!
    var opinionCreationSucceededPage: OpinionCreationSucceededPage!
    var orders: OrdersListPage!
    var orderDetails: OrderDetailsPage!

    func testCreateOrEditOpinionFromMyOrders() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4215")
        Allure.addEpic("ЛК")
        Allure.addFeature("Мои заказы")
        Allure.addTitle("Проверяем создание/изменение отзыва")

        setupMock()

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SaveOpinion")
        }

        "Запускаем приложение и авторизуемся".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            profile = goToProfile(root: root)
            wait(forVisibilityOf: profile.collectionView)
        }

        "Переходим в мои заказы".ybm_run { _ in
            orders = profile.myOrders.tap()
            wait(forVisibilityOf: orders.element)
        }

        "Создаем отзыв".ybm_run { _ in
            orders.rate()
            writeOpinion()
            wait(forVisibilityOf: orders.element)
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "EditOpinion")
        }

        "Редактируем отзыв".ybm_run { _ in
            orders.rate()
            editOpinion()
            wait(forVisibilityOf: orders.element)
        }
    }

    func testCreateOrEditOpinionFromMyOrderDetails() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4380")
        Allure.addEpic("ЛК")
        Allure.addFeature("Мои заказы. Детали заказа")
        Allure.addTitle("Проверяем создание/изменение отзыва")

        setupMock()

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SaveOpinion")
        }

        "Запускаем приложение и авторизуемся".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            profile = goToProfile(root: root)
            wait(forVisibilityOf: profile.collectionView)
        }

        "Переходим в мои заказы".ybm_run { _ in
            orders = profile.myOrders.tap()
            wait(forVisibilityOf: orders.element)
        }

        "Переходим в детали заказа".ybm_run { _ in
            let detailsButton = orders.detailsButton(orderId: Constants.orderId)
            orders.collectionView.ybm_swipeCollectionView(toFullyReveal: detailsButton.element)
            orderDetails = detailsButton.tap()
            wait(forVisibilityOf: orderDetails.element)
        }

        "Cвайпаем до появления кнопки \"Оценить\". Создаем отзыв".ybm_run { _ in
            orderDetails.collectionView.ybm_swipeCollectionView(toFullyReveal: orderDetails.rateButton)
            orderDetails.rateButton.tap()
            writeOpinion()
            wait(forVisibilityOf: orderDetails.element)
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "EditOpinion")
        }

        "Редактируем отзыв".ybm_run { _ in
            orderDetails.rateButton.tap()
            editOpinion()
            wait(forVisibilityOf: orderDetails.element)
        }
    }

}

// MARK: - Private helpers

private extension OpinionsMyOrdersTests {

    func setupMock() {
        "Настраиваем стейт".ybm_run { _ in
            let orderMapper = OrdersState.UserOrdersHandlerMapper(
                orders: [
                    Order.Mapper(
                        id: Constants.orderId,
                        status: .delivered,
                        msku: ["101077347763"]
                    )
                ]
            )
            var orderState = OrdersState()
            orderState.setOrdersResolvers(mapper: orderMapper, for: [.all, .byId])
            stateManager?.setState(newState: orderState)

            var skuState = SKUInfoState()
            skuState.setSkuInfoProductOffersWithHyperIdState(
                with: .init(
                    results: .default,
                    collections: .default
                )
            )
            stateManager?.setState(newState: skuState)
        }
    }

    func writeOpinion() {
        let opinionGrade = OpinionGradePage.current
        wait(forVisibilityOf: opinionGrade.element)
        opinionGrade.tapStars()
        opinionCreation = LeaveOpinionPopUp.currentPopup
        wait(forVisibilityOf: opinionCreation.element)

        typeOpinion(opinionCreation)

        opinionCreationMultifactorPage = opinionCreation.tapContinueButton()
        wait(forVisibilityOf: opinionCreationMultifactorPage.element)
        opinionCreationSucceededPage = opinionCreationMultifactorPage.tapContinueButton()

        wait(forVisibilityOf: opinionCreationSucceededPage.element)
        opinionCreationSucceededPage.succeededButton.tap()
    }

    func editOpinion() {
        opinionCreation = LeaveOpinionPopUp.currentPopup
        wait(forVisibilityOf: opinionCreation.element)

        typeOpinion(opinionCreation)

        opinionCreation.collectionView.ybm_swipeCollectionView(toFullyReveal: opinionCreation.continueButton)
        opinionCreationMultifactorPage = opinionCreation.tapContinueButton()
        wait(forVisibilityOf: opinionCreationMultifactorPage.element)
        opinionCreationSucceededPage = opinionCreationMultifactorPage.tapContinueButton()

        wait(forVisibilityOf: opinionCreationSucceededPage.element)
        opinionCreationSucceededPage.succeededButton.tap()
    }

    func typeOpinion(_ opinionCreation: LeaveOpinionPopUp) {
        wait(forVisibilityOf: opinionCreation.prosText.element)
        XCTAssertEqual(opinionCreation.prosText.placeholder.label, Constants.prosText)
        opinionCreation.prosText.typeText(Constants.prosText)

        opinionCreation.collectionView.ybm_swipeCollectionView(toFullyReveal: opinionCreation.consText.element)
        XCTAssertEqual(opinionCreation.consText.placeholder.label, Constants.consText)
        opinionCreation.consText.typeText(Constants.consText)

        opinionCreation.collectionView.ybm_swipeCollectionView(toFullyReveal: opinionCreation.commentText.element)
        XCTAssertEqual(opinionCreation.commentText.placeholder.label, Constants.commentText)
        opinionCreation.commentText.typeText(Constants.commentText)
    }

}

// MARK: - Nested types

private extension OpinionsMyOrdersTests {

    enum Constants {
        static let orderId = "51457525"
        static let prosText = "Достоинства:"
        static let consText = "Недостатки:"
        static let commentText = "Комментарий:"
    }
}
