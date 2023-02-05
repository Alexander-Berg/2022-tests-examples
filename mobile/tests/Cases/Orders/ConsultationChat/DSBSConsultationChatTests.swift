import MarketUITestMocks
import XCTest

final class DSBSConsultationChatTests: DSBSQuestionnaireTestCase {

    var root: RootPage!

    let chatId = "1337123"
    let orderId = "4815230"

    override var user: UserAuthState {
        .loginWithYandexPlus
    }

    typealias AvailiabilityHandlerMapper = ResolveOrderOptionAvailiability.Mapper
    typealias Availiability = AvailiabilityHandlerMapper.OrderAvailabilities

    typealias OrderHandlerMapper = OrdersState.UserOrdersHandlerMapper
    typealias Delivery = Order.Mapper.SimpleDelivery
    typealias SimpleOrder = Order.Mapper

    func test_DSBSConsultationChat_FromOrders_Open() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4296")
        Allure.addEpic("DSBS")
        Allure.addFeature("Арбитраж")
        Allure.addTitle("Открытие чата со списка заказов")

        var ordersPage: OrdersListPage!

        let toDateFormatted = formatDate(-(.day * 2))

        let mapper = OrderHandlerMapper(orders: [
            SimpleOrder(
                id: orderId,
                status: .delivery,
                substatus: .service,
                rgb: "WHITE",
                delivery: Delivery(
                    toDate: toDateFormatted,
                    type: .service
                )
            )
        ])

        var ordersState = setup()
        ordersState.setOrdersResolvers(mapper: mapper, for: [.all, .byId])

        "Мокаем состояние".ybm_run { _ in
            stateManager?.setState(newState: ordersState)
        }

        "Переходим на список заказов".ybm_run { _ in
            ordersPage = goToOrdersListPage(root: root)
        }

        "Нажимаем на «Чат по заказу»".ybm_run { _ in
            ordersPage.consultationButton(orderId: orderId).tap()
        }

        "Ждём открытия чата".ybm_run { _ in
            ybm_wait(forVisibilityOf: [ConsultationChatPage.current.element])
        }
    }

    func test_DSBSConsultationChat_FromOrderDetail_Open() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4298")
        Allure.addEpic("DSBS")
        Allure.addFeature("Арбитраж")
        Allure.addTitle("Открытие чата из деталей заказа")

        var orderDetailsPage: OrderDetailsPage!

        let toDateFormatted = formatDate(-(.day * 2))

        let mapper = OrderHandlerMapper(orders: [
            SimpleOrder(
                id: orderId,
                status: .delivery,
                substatus: .service,
                rgb: "WHITE",
                delivery: Delivery(
                    toDate: toDateFormatted,
                    type: .service
                )
            )
        ])

        var ordersState = setup()
        ordersState.setOrdersResolvers(mapper: mapper, for: [.all, .byId])

        "Мокаем состояние".ybm_run { _ in
            stateManager?.setState(newState: ordersState)
        }

        "Переходим в детали заказа".ybm_run { _ in
            orderDetailsPage = goToOrderDetailsPage(root: root, orderId: orderId)
        }

        "Нажимаем на «Чат по заказу»".ybm_run { _ in
            orderDetailsPage.collectionView.swipe(to: .down, untilVisible: orderDetailsPage.consultationButton)
            orderDetailsPage.consultationButton.tap()
        }

        "Ждём открытия чата".ybm_run { _ in
            ybm_wait(forVisibilityOf: [ConsultationChatPage.current.element])
        }
    }

    func test_DSBSConsultationChat_FromMorda_Open() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4299")
        Allure.addEpic("DSBS")
        Allure.addFeature("Арбитраж")
        Allure.addTitle("Открытие чата с виджета на морде")

        var mordaPage: MordaPage!
        var snippetPage: HoveringSnippetPage!

        app.launchEnvironment[TestLaunchEnvironmentKeys.enabledToggles] = FeatureNames.alreadyDeliveredQuestionnaire

        let toDateFormatted = formatDate(.day * 2)

        let mapper = OrderHandlerMapper(orders: [
            SimpleOrder(
                id: orderId,
                status: .delivery,
                substatus: .service,
                delivery: Delivery(
                    toDate: toDateFormatted,
                    type: .service
                ),
                withFeedback: true,
                withChatId: chatId
            )
        ])

        let availabilityMapper: AvailiabilityHandlerMapper = .makeNoneForOrders(ids: [orderId])

        var ordersState = setup()
        ordersState.setOrdersResolvers(mapper: mapper, for: [.recent(withGrades: true), .byId])
        ordersState.setAvailabilityOrderOption(mapper: availabilityMapper)

        "Мокаем состояние".ybm_run { _ in
            stateManager?.setState(newState: ordersState)
        }

        "Переходим на морду".ybm_run { _ in
            mordaPage = goToMorda()
        }

        "Проверяем наличие виджета".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                mordaPage.element.isVisible && mordaPage.singleActionContainerWidget.container.element.isVisible
            })
            snippetPage = mordaPage.singleActionContainerWidget.container.orderSnippet()
        }

        "Проверяем и нажимаем кнопку «Чат»".ybm_run { _ in
            XCTAssertEqual(snippetPage.additionalActionButton.element.label, "Чат")
            snippetPage.additionalActionButton.element.tap()
        }

        "Ждём открытия чата".ybm_run { _ in
            ybm_wait(forVisibilityOf: [ConsultationChatPage.current.element])
        }
    }

    func test_DSBSConsultationChat_FromQuestionnaireWidget_Open() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4300")
        Allure.addEpic("DSBS")
        Allure.addFeature("Арбитраж")
        Allure.addTitle("Открытие чата с виджета «Заказ у меня»")

        var mordaPage: MordaPage!
        var snippetPage: HoveringSnippetPage!
        var questionnairePage: QuestionnairePage!

        app.launchEnvironment[TestLaunchEnvironmentKeys.enabledToggles] = FeatureNames.alreadyDeliveredQuestionnaire

        let toDateFormatted = formatDate(-(.day * 2))

        let mapper = OrderHandlerMapper(orders: [
            SimpleOrder(
                id: orderId,
                status: .delivery,
                substatus: .service,
                delivery: Delivery(
                    toDate: toDateFormatted,
                    type: .service
                ),
                withFeedback: true
            )
        ])

        var ordersState = setup()
        ordersState.setOrdersResolvers(mapper: mapper, for: [.recent(withGrades: true), .byId])

        "Мокаем состояние".ybm_run { _ in
            stateManager?.setState(newState: ordersState)
        }

        "Переходим на морду".ybm_run { _ in
            mordaPage = goToMorda()
        }

        "Проверяем наличие виджета".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                mordaPage.element.isVisible && mordaPage.singleActionContainerWidget.container.element.isVisible
            })
            snippetPage = mordaPage.singleActionContainerWidget.container.orderSnippet()
        }

        "Проверяем и нажимаем кнопку 'Еще нет'".ybm_run { _ in
            XCTAssertEqual(snippetPage.additionalActionButton.element.label, "Еще нет")
            snippetPage.additionalActionButton.element.tap()
        }

        "Ждем появления попапа 'Вам назвали новую дату доставки?'".ybm_run { _ in
            questionnairePage = QuestionnairePage.current
            wait(forVisibilityOf: questionnairePage.title)
            XCTAssertEqual(questionnairePage.title.text, "Вам назвали новую дату доставки?")
            XCTAssertEqual(questionnairePage.mainActionButton.button.label, "Да")
            XCTAssertEqual(questionnairePage.extraActionButton.button.label, "Нет")
        }

        "Нажимаем кнопку 'Нет' и ждем следующего попапа".ybm_run { _ in
            questionnairePage.extraActionButton.button.tap()
            ybm_wait {
                questionnairePage = QuestionnairePage.current
                return questionnairePage.subtitle
                    .text == "В ближайшие 24 часа уточним у продавца и вернемся с новой датой доставки"
            }
        }

        "Нажимаем на «Чат с продавцом»".ybm_run { _ in
            XCTAssertEqual(questionnairePage.extraActionButton.button.label, "Чат с продавцом")
            questionnairePage.extraActionButton.button.tap()
        }

        "Ждём открытия чата".ybm_run { _ in
            ybm_wait(forVisibilityOf: [ConsultationChatPage.current.element])
        }
    }

    // MARK: - Private

    private func formatDate(_ timeInterval: TimeInterval) -> String {
        let toDate = Date().addingTimeInterval(timeInterval)
        return getDateString(withDateFormat: "dd-MM-yyyy", date: toDate)
    }

    private func setup() -> OrdersState {
        app.launchEnvironment[TestLaunchEnvironmentKeys.enabledToggles] = FeatureNames.orderConsultation

        var orderState = OrdersState()

        let chatHandler = OrderConsultation(orderId: Int(orderId) ?? 0, chatId: chatId)
        orderState.setConsultationChats(mapper: chatHandler, for: [.create, .get])

        return orderState
    }
}
