import MarketProtocols
import OHHTTPStubs
import XCTest

@testable import BeruServices

final class DeliveryBucketsServiceImplTests: NetworkingTestCase {

    var deliveryBucketsService: DeliveryBucketsService?

    override func setUp() {
        super.setUp()
        deliveryBucketsService = DeliveryBucketsServiceImpl(apiClient: DependencyProvider().apiClient)
    }

    override func tearDown() {
        deliveryBucketsService = nil
        super.tearDown()
    }

    func test_shouldFetchDeliveryBucketsTests() {
        // given
        stub(
            requestPartName: Constants.requestPartName,
            responseFileName: Constants.FetchDeliveryBuckets.responseFile,
            testBlock: containsQueryParams(["name": Constants.FetchDeliveryBuckets.resolverName])
        )
        let items = [
            makeDeliveryBucketsItem()
        ]

        // when
        let result = deliveryBucketsService?.fetchDeliveryBuckets(
            regionId: Constants.regionId,
            items: items,
            option: .express,
            location: nil
        ).expect(in: self)

        // then
        switch result {
        case let .success(buckets):
            XCTAssertFalse(buckets.isEmpty)
        default:
            XCTFail("Request should not fail")
        }
    }

    func test_shouldFail_whenServerRespondsWith500Error() {
        // given
        stubError(
            requestPartName: Constants.requestPartName,
            code: Int32(Constants.errorNumber500),
            testBlock: containsQueryParams(["name": Constants.FetchDeliveryBuckets.resolverName])
        )
        let items = [
            makeDeliveryBucketsItem()
        ]

        // when
        let result = deliveryBucketsService?.fetchDeliveryBuckets(
            regionId: Constants.regionId,
            items: items,
            option: .express,
            location: nil
        ).expect(in: self)

        // then
        guard case let .failure(error as ApiClientError) = result else {
            XCTFail("Can't be successfull with 500 response")
            return
        }

        guard case let .network(response: response, _, _, _) = error else {
            XCTFail("Wrong type of error")
            return
        }

        XCTAssertEqual(response?.statusCode, Constants.errorNumber500)
    }

    // MARK: - Private

    private func makeDeliveryBucketsItem() -> DeliveryBucketsItem {
        DeliveryBucketsItem(
            marketSku: Constants.FetchDeliveryBuckets.marketSku,
            count: Constants.FetchDeliveryBuckets.count,
            wareId: Constants.FetchDeliveryBuckets.wareId
        )
    }
}

// MARK: - Nested Types

extension DeliveryBucketsServiceImplTests {
    enum Constants {
        static let regionId: Int = 213
        static let requestPartName: String = "api/v1"
        static let errorNumber500: Int = 500

        enum FetchDeliveryBuckets {
            static let responseFile: String = "pharmacy_buckets"
            static let resolverName: String = "resolveDeliveryBuckets"
            static let marketSku: String = "101290865743"
            static let count: Int = 1
            static let wareId: String = "mTfEEHEaXgQ_dn93Ypi8dw"
        }
    }
}
