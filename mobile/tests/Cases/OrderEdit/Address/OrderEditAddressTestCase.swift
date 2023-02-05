import MarketUITestMocks
import XCTest

class OrderEditAddressTestCase: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func openMap(
        address: EditAddress,
        redeliveryInfo: OrderRedeliveryInfo = (polygon: [.defaultArea], outlets: [.defaultOutlet])
    ) {
        var orderDetailsPage: OrderDetailsPage!

        "Настраиваем стейт".ybm_run { _ in
            setupInitialState(address: address, redeliveryInfo: redeliveryInfo)
        }

        "Переходим в детали заказа".ybm_run { _ in
            orderDetailsPage = goToOrderDetailsPage(orderId: Constants.orderId)
        }

        "Нажимаем `Изменить` адрес доставки".ybm_run { _ in
            wait(forVisibilityOf: orderDetailsPage.deliveryAddress.editButton.element)
            _ = orderDetailsPage.deliveryAddress.editButton.tap()
        }

        "Проверяем заголовок экрана".ybm_run { _ in
            XCTAssertEqual(NavigationBarPage.current.title.label, "Изменить адрес доставки")
        }
    }

    // MARK: - Helper Methods

    typealias OrdersHandlerMapper = OrdersState.UserOrdersHandlerMapper
    typealias SimpleOrder = Order.Mapper
    typealias DeliveryTimeInterval = ResolveRecentUserOrders.RecentUserOrders.DeliveryTimeInterval
    typealias AddressResult = ResolveUserAddressAndRegionByGpsCoordinate.UserAddressResult
    typealias AddressDetails = ResolveUserAddresses.Mapper.AddressDetails
    typealias OrderEditRequest = ResolveRecentUserOrders.RecentUserOrders.OrderEditRequest
    typealias OrderRedeliveryInfo = (
        polygon: [ResolveDeliveryArea.LastMilePolygon],
        outlets: [ResolveRedeliveryInfo.RedeliveryOutlet]
    )

    func setupInitialState(
        address: EditAddress,
        redeliveryInfo: OrderRedeliveryInfo = (polygon: [.defaultArea], outlets: [.defaultOutlet]),
        withEditRequest: Bool = false
    ) {
        enable(toggles: FeatureNames.orderEditMapModuleFeature)

        setupOrdersState(
            address: address,
            redeliveryInfo: redeliveryInfo,
            withEditRequest: withEditRequest
        )
        setupUserState(address: address)
    }

    func setupUserState(address: EditAddress) {
        var userState = UserAuthState()
        userState.setUserAddressByGpsCoordinate(
            result: address.info.result,
            byGps: [
                .init(
                    region: .moscow,
                    address: address.info.details
                )
            ]
        )
        stateManager?.setState(newState: userState)
    }

    // MARK: - Private

    private func setupOrdersState(
        address: EditAddress,
        redeliveryInfo: OrderRedeliveryInfo,
        withEditRequest: Bool = false
    ) {
        var orderState = OrdersState()

        let editRequest: OrderEditRequest = .changeAddressRequest(
            orderId: Constants.orderId,
            status: .processing
        )

        let order = SimpleOrder(
            id: Constants.orderId,
            status: .delivery,
            payment: .applePay,
            delivery: .init(
                deliveryPartnerType: .yandex,
                fromDate: Constants.date,
                toDate: Constants.date,
                fromTime: Constants.fromTime,
                toTime: Constants.toTime,
                type: .service,
                address: address.info.address
            )
        )
        let orderMapper = OrdersHandlerMapper(
            orders: [order],
            orderEditRequest: withEditRequest ? [editRequest] : []
        )
        orderState.setOrdersResolvers(mapper: orderMapper, for: [.all, .byId])

        orderState.setEditVariants(orderEditVariants: .boxberry(
            orderId: Int(Constants.orderId) ?? 0,
            possibilities: [.address]
        ))
        orderState.setRedeliveryArea(polygon: .defaultArea)
        orderState.setRedeliveryInfo(polygon: redeliveryInfo.polygon, outlets: redeliveryInfo.outlets)
        orderState.setOrderEditingOptions(
            orderEditingOptions: .defaultEditingOptions(orderId: Constants.orderId)
        )
        orderState.setOrderEditingRequest(orderEditingRequest: .init(
            deliveryTimeInterval: [Constants.newTimeInterval],
            orderEditRequest: [editRequest]
        ))

        orderState.setOutlet2(outlets: [.postamat])

        stateManager?.setState(newState: orderState)
    }
}

// MARK: - Nested Types

extension OrderEditAddressTestCase {

    enum Constants {
        static let orderId = "4815230"
        static let date = "09-12-2019"
        static let fromTime = "10:00"
        static let toTime = "18:00"
        static let newTimeInterval: DeliveryTimeInterval = .init(fromTime: "14:00", toTime: "18:00")
    }

    enum EditAddress {
        case novinskiy
        case rublevskoye
        case shabolovka

        typealias Info = (address: Address, result: AddressResult, details: AddressDetails)

        var info: Info {
            switch self {
            case .novinskiy:
                return (.novinskiy, .novinskiy, .novinskiy)
            case .rublevskoye:
                return (.rublevskoye, .rublevskoye, .rublevskoye)
            default:
                return (.novinskiy, .shabolovka, .shabolovka)
            }
        }
    }
}
