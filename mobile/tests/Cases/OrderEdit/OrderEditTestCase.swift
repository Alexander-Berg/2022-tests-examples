import MarketUITestMocks
import XCTest

class OrderEditTestCase: LocalMockTestCase {

    typealias OrdersHandlerMapper = OrdersState.UserOrdersHandlerMapper
    typealias SimpleOrder = Order.Mapper
    typealias Buyer = ResolveRecentUserOrders.RecentUserOrders.Buyer
    typealias OrderEditRequest = ResolveRecentUserOrders.RecentUserOrders.OrderEditRequest
    typealias EditRequstStatus = OrderEditRequest.Status
    typealias Payment = Order.Payment
    typealias Promos = Order.OrderPromo
    typealias DeliveryTimeInterval = ResolveRecentUserOrders.RecentUserOrders.DeliveryTimeInterval

    let newDeliveryTimeInterval = DeliveryTimeInterval(fromTime: "14:00", toTime: "18:00")

    func setupState(
        orderId: String,
        orderEditRequest: [OrderEditRequest] = [],
        status: SimpleOrder.Status = .processing,
        needOrderEditPossibilities: Bool = true,
        enableEditPossibilites: Bool = true,
        buyer: Buyer = .defaultBuyer,
        payment: Payment? = nil,
        needOrderEditingOptions: Bool = true
    ) {
        let orderMapper = OrdersHandlerMapper(
            orders: [
                SimpleOrder(
                    id: orderId,
                    status: status,
                    payment: payment,
                    rgb: "WHITE",
                    delivery: .init(
                        deliveryPartnerType: .yandex,
                        fromDate: "10-12-2019",
                        toDate: "10-12-2019",
                        fromTime: "10:00",
                        toTime: "18:00",
                        type: .outlet
                    ),
                    promos: Promos.allPromos
                )
            ],
            orderEditRequest: orderEditRequest,
            buyer: [buyer],
            sku: .empty
        )

        var orderState = OrdersState()
        orderState.setOrdersResolvers(mapper: orderMapper, for: [.all, .recent(withGrades: true), .byId])
        orderState.setOrderEditingOptions(
            orderEditingOptions:
            needOrderEditingOptions
                ? .defaultEditingOptions(orderId: orderId)
                : .emptyEditingOptions(orderId: orderId)
        )
        orderState.setOrderPaymentByOrderIds(orderPaymentByOrderIds: .defaultOrderPayment)

        if needOrderEditPossibilities {
            orderState.setEditVariants(orderEditVariants: .boxberry(
                orderId: Int(orderId) ?? 0,
                possibilities: [.deliveryDates(enabled: enableEditPossibilites), .recipient, .paymentMethod]
            ))
        }

        if orderEditRequest.isNotEmpty {
            orderState.setOrderEditingRequest(orderEditingRequest: .init(
                deliveryTimeInterval: [newDeliveryTimeInterval],
                orderEditRequest: orderEditRequest
            ))
        } else {
            orderState.setOrderEditingRequest(orderEditingRequest: .init(
                deliveryTimeInterval: [],
                orderEditRequest: [.emptyRequest(orderId: orderId)]
            ))
        }

        stateManager?.setState(newState: orderState)
    }

    // MARK: - Public

    func changeDeliveryDate(in editPage: OrderEditPage) {
        "???????????????? ?????????? ?? ???????? ???????????????? ?? ?????????????????? ???????????? `??????????????????`".ybm_run { _ in
            editPage.dateSelector.element.tap()
            wait(forVisibilityOf: PickerWheelPage.current.element)

            PickerWheelPage.current.adjust(toPickerWheelValue: "??????????????????????, 19 ????????????")
            KeyboardAccessoryPage.current.doneButton.tap()

            editPage.timeSelector.element.tap()
            wait(forVisibilityOf: PickerWheelPage.current.element)

            PickerWheelPage.current.adjust(toPickerWheelValue: "?? 18:00 ???? 22:00")
            KeyboardAccessoryPage.current.doneButton.tap()

            XCTAssertTrue(editPage.saveButton.button.isEnabled)
        }
    }

    func changeRecipientName(
        in editPage: OrderEditPage,
        name: String = "???????????? ????????"
    ) {
        "???????????????? ???????? `?????? ?? ?????????????? ???? ????????????????`".ybm_run { _ in
            editPage.nameField.element.tap()
            wait(forVisibilityOf: editPage.nameField.clearButton)
            editPage.nameField.clearButton.tap()
            ybm_wait(forFulfillmentOf: { editPage.nameField.field.text.isEmpty })

            editPage.nameField.field.typeText(name)
            KeyboardAccessoryPage.current.doneButton.tap()
            wait(forVisibilityOf: editPage.nameField.validationMark)
            XCTAssertEqual(editPage.nameField.field.text, name)
        }
    }

    func changeRecipientPhone(
        in editPage: OrderEditPage,
        phone: String = "89277747475",
        formattedPhone: String = "+7 (927) 774-74-75"
    ) {
        "???????????????? ???????? `??????????????`".ybm_run { _ in
            editPage.phoneField.element.tap()
            wait(forVisibilityOf: editPage.phoneField.clearButton)
            editPage.phoneField.clearButton.tap()
            ybm_wait(forFulfillmentOf: { editPage.phoneField.field.text.isEmpty })

            editPage.phoneField.field.typeText(phone)
            KeyboardAccessoryPage.current.doneButton.tap()
            wait(forVisibilityOf: editPage.phoneField.validationMark)
            XCTAssertEqual(editPage.phoneField.field.text, formattedPhone)
        }
    }

    func checkChangeInfo(
        in orderDetailsPage: OrderDetailsPage,
        orderId: String,
        image: String,
        title: String
    ) {
        var ordersListPage: OrdersListPage!

        "?????????????????? ???????????? ?????????????????? ???????? ????????????????".ybm_run { _ in
            orderDetailsPage.element.swipe(to: .up, until: !orderDetailsPage.editRequests.isEmpty)
            guard let changeInfo = orderDetailsPage.editRequests.first else {
                XCTFail("couldn't be nil")
                return
            }
            XCTAssertTrue(changeInfo.image.exists)
            XCTAssertEqual(changeInfo.image.label, image)
            XCTAssertTrue(changeInfo.title.isVisible)
            XCTAssertEqual(changeInfo.title.label, title)
        }

        "?????????????????? ?????????? ?? `?????? ????????????`".ybm_run { _ in
            NavigationBarPage.current.backButton.tap()
            ordersListPage = OrdersListPage.current
            wait(forVisibilityOf: ordersListPage.element)
        }

        "?????????????????? ???????????? ?????????????????? ???????? ???????????????? ?? `???????? ??????????????`".ybm_run { _ in
            guard let editRequest = ordersListPage.editRequests(orderId: orderId).first else {
                XCTFail("No edit requests")
                return
            }
            XCTAssertTrue(editRequest.image.exists)
            XCTAssertEqual(editRequest.image.label, image)
            XCTAssertTrue(editRequest.title.isVisible)
            XCTAssertEqual(editRequest.title.label, title)
        }
    }

}
