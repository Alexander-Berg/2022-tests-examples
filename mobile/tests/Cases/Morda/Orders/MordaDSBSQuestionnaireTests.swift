import MarketUITestMocks
import XCTest

final class MordaDSBSQuestionnaireTests: DSBSQuestionnaireTestCase {

    // MARK: - Public

    override var user: UserAuthState {
        .loginNoSubscription
    }

    typealias HandlerMapper = OrdersState.UserOrdersHandlerMapper
    typealias Delivery = Order.Mapper.SimpleDelivery
    typealias SimpleOrder = Order.Mapper

    var morda: MordaPage!
    var snippet: HoveringSnippetPage!

    func test_OpenMordaDSBSOrder_NotReceived_Continue() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3950")
        Allure.addEpic("DSBS")
        Allure.addFeature("Виджет Заказ у меня")
        Allure.addTitle("Заказ не получен, новой даты нет, нажимаем продолжить")

        setupAndAnswerStillNo()

        notDeliveredNoNewDateFlow_tapContinue()
    }

    func test_OpenMordaDSBSOrder_NotReceived_CallSupport() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3951")
        Allure.addEpic("DSBS")
        Allure.addFeature("Виджет Заказ у меня")
        Allure.addTitle("Заказ не получен, новой даты нет, звоним в поддержку")

        setupAndAnswerStillNo()

        notDeliveredNoNewDateFlow_consultationChat()
    }

    func test_OpenMordaDSBSOrder_NotReceived_NewDateFoundOut() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3944")
        Allure.addEpic("DSBS")
        Allure.addFeature("Виджет Заказ у меня")
        Allure.addTitle("Заказ не получен, новая дата есть")

        setupAndAnswerStillNo()

        notDelivered_NewDateFoundOut()
    }

    func test_OpenMordaDSBSOrder_Received_RateViewAppears() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3943")
        Allure.addEpic("DSBS")
        Allure.addFeature("Виджет Заказ у меня")
        Allure.addTitle("Заказ получен")

        setupMordaOrdersWithDSBSOrderAndQuestionnaireConditions()

        "Проверяем и нажимаем кнопку 'У меня'".ybm_run { _ in
            XCTAssertEqual(snippet.actionButton.element.label, "У меня")
            snippet.actionButton.element.tap()
        }

        alreadyGotItScenario_feedbackPopupOpens()
    }

    // MARK: - Private

    private func setupAndAnswerStillNo() {
        setupMordaOrdersWithDSBSOrderAndQuestionnaireConditions()

        "Проверяем и нажимаем кнопку 'Еще нет'".ybm_run { _ in
            XCTAssertEqual(snippet.additionalActionButton.element.label, "Еще нет")
            snippet.additionalActionButton.element.tap()
        }
    }

    private func setupMordaOrdersWithDSBSOrderAndQuestionnaireConditions() {

        let toDate = Date().addingTimeInterval(-(.day * 2))
        let toDateFormatted = getDateString(withDateFormat: "dd-MM-yyyy", date: toDate)

        let recentUserOrdersMapper = HandlerMapper(orders: [
            SimpleOrder(
                status: .delivery,
                substatus: .service,
                delivery: Delivery(
                    toDate: toDateFormatted,
                    type: .service
                ),
                withFeedback: true
            )
        ])

        enable(toggles: FeatureNames.alreadyDeliveredQuestionnaire)

        var orderState = OrdersState()
        orderState.setOrdersResolvers(mapper: recentUserOrdersMapper, for: [.recent(withGrades: true)])

        "Мокаем состояния".ybm_run { _ in
            stateManager?.setState(newState: orderState)
        }

        "Открываем морду".ybm_run { _ in
            morda = goToMorda()
        }

        "Проверяем наличие виджета".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { [weak self] in
                self?.mockServer?.handledRequests.contains { $0.contains("resolveRecentUserOrders") } == true
            })

            ybm_wait(forFulfillmentOf: {
                self.morda.element.isVisible
                    && self.morda.singleActionContainerWidget.container.element.isVisible
            })
            snippet = morda.singleActionContainerWidget.container.orderSnippet()
        }
    }
}
