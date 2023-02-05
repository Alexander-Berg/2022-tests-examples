import MarketModels
import OHHTTPStubs
import XCTest

@testable import BeruServices

class PushNotificationsServiceTests: NetworkingTestCase {

    var service: PushNotificationsServiceImpl!

    override func setUp() {
        super.setUp()

        service = PushNotificationsServiceImpl(apiClient: DependencyProvider().apiClient)
    }

    override func tearDown() {
        service = nil
        super.tearDown()
    }

    func test_shouldFetchPushSubscriptions() throws {
        // given
        stub(
            requestPartName: "resolvePushSubscriptions",
            responseFileName: "pushSubscriptions",
            testBlock: isMethodPOST()
        )

        // when
        let result = service.fetchPushSubscriptions(uuid: "123").expect(in: self)

        // then
        let pushSubscriptions = try result.get()

        XCTAssertEqual(pushSubscriptions.title, "Какие уведомления присылать?")
        XCTAssertNotNil(pushSubscriptions.subtitle)
        XCTAssertEqual(pushSubscriptions.items, [64, 65])

        XCTAssertEqual(pushSubscriptions.subscriptionSectionItems.count, 2)
        XCTAssertEqual(pushSubscriptions.subscriptionSectionItems.first?.id, 64)
        XCTAssertEqual(pushSubscriptions.subscriptionSectionItems.first?.title, "Промокоды и акции")
        XCTAssertNotNil(pushSubscriptions.subscriptionSectionItems.first?.subtitle)
        XCTAssertEqual(pushSubscriptions.subscriptionSectionItems.first?.status, "enabled")
    }

    func test_shouldFetchUpdatedPushSubscriptions() throws {
        // given
        stub(
            requestPartName: "updatePushSubscriptions",
            responseFileName: "updatePushSubscriptions",
            testBlock: isMethodPOST()
        )

        // when
        let item = PushSubscriptionSectionItem(
            entity: "push",
            id: 64,
            title: "some",
            subtitle: "some",
            status: "enabled",
            type: "push",
            updated: 1_232_131
        )
        let result = service.updatePushSubscriptions(
            uuid: "123",
            pushSubscriptionSectionItems: [item]
        ).expect(in: self)

        // then
        XCTAssertNoThrow(try result.get())
    }

}
