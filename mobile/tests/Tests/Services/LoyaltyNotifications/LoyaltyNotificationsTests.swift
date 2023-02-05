import OHHTTPStubs
import XCTest

@testable import BeruServices

class LoyaltyNotificationsTests: NetworkingTestCase {

    var service: LoyaltyNotificationsServiceImpl!

    override func setUp() {
        super.setUp()

        let apiClient = DependencyProvider().legacyAPIClient
        apiClient.token = "123124123123123"

        service = LoyaltyNotificationsServiceImpl(
            apiClient: APIClient(apiClient: apiClient)
        )
    }

    override func tearDown() {
        service = nil
        super.tearDown()
    }

    func test_shouldReceiveListNotifications_whenObtainNotificataions() throws {
        // given
        stub(
            requestPartName: "resolveLoyaltyNotifications",
            responseFileName: "obtain_loyaltyNotification",
            testBlock: isMethodPOST()
        )

        // when
        let result = service.obtainList().expect(in: self)

        // then
        let items = try result.get()

        XCTAssertEqual(items.count, 2)
        let item = try XCTUnwrap(items.first)

        XCTAssertEqual(item.title, "title")
        XCTAssertEqual(item.text, "text")
        XCTAssertEqual(item.id, "123")
        XCTAssertEqual(item.type, "REFERRAL")
        XCTAssertEqual(item.link, "https://ya.ru")
        XCTAssertEqual(item.imageLink, "image")
        XCTAssertEqual(item.linkLabel, "link")
        XCTAssertEqual(item.deepLink, "dLink")
        XCTAssertEqual(Array(item.params.keys).sorted(), ["some1", "some2"])

        let values = try XCTUnwrap(Array(item.params.values) as? [String])
        XCTAssertEqual(values.sorted(), ["val1", "val2"])
    }

    func test_shouldReceiveError_whenLoyaltyNotificationResponseHasError() throws {
        // given
        stub(
            requestPartName: "resolveLoyaltyNotifications",
            responseFileName: "obtain_loyaltyNotificationError",
            testBlock: isMethodPOST()
        )

        // when
        let result = service.obtainList().expect(in: self)

        // then
        XCTAssertThrowsError(try result.get()) { error in
            XCTAssertEqual(error as? ServiceError, ServiceError.invalidResponseClass())
        }
    }

    func test_shouldReceiveError_whenLoyaltyNotificationApplyResponseHasError() throws {
        // given
        stub(
            requestPartName: "resolveAcceptLoyaltyNotifications",
            responseFileName: "obtain_acceptLoyaltyNotificationsError",
            testBlock: isMethodPOST()
        )

        // when
        let result = service.read(by: ["123"]).expect(in: self)

        // then
        XCTAssertThrowsError(try result.get()) { error in
            XCTAssertEqual(error as? ServiceError, ServiceError.unknown())
        }
    }

    func test_shouldReceiveSuccess_whenReadNotification() throws {
        // given
        stub(
            requestPartName: "resolveAcceptLoyaltyNotifications",
            responseFileName: "obtain_acceptLoyaltyNotifications",
            testBlock: isMethodPOST()
        )

        // when
        let result = service.read(by: ["123"]).expect(in: self)

        // then
        XCTAssertNoThrow(try result.get())
    }

}
