import BeruLegacyNetworking
import MarketProtocols
import OHHTTPStubs
import SwiftyJSON
import XCTest

@testable import BeruServices

final class BrandsServiceTests: NetworkingTestCase {

    // MARK: - Properties

    private var brandsService: BrandsService!

    // MARK: - Lifecycle

    override func setUp() {
        super.setUp()
        brandsService = BrandsServiceImpl(apiClient: DependencyProvider().apiClient)
    }

    override func tearDown() {
        super.tearDown()
        brandsService = nil
    }

    // MARK: - Load Popular Brands

    func test_resolvePopularBrands_shouldSendProperRequestAndReturnProperBrandsCount() throws {
        // given
        let hid = 1
        let count = 16
        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            let json = JSON(body)
            let params = json["params"].array
            guard
                let paramsDict = params?.first,
                let parsedHid = paramsDict["hid"].int,
                let parsedCount = paramsDict["count"].int,
                let parsedOffset = paramsDict["offset"].int
            else { return false }

            return parsedOffset == 0
                && hid == parsedHid
                && count == parsedCount
        }
        stub(
            requestPartName: "resolvePopularBrands",
            responseFileName: "resolve_popular_brands",
            testBlock: isMethodPOST()
                && containsQueryParams(["name": "resolvePopularBrands"])
                && verifyJsonBody(checkBodyBlock)
        )

        // when
        let result = brandsService.loadPopularBrands(withHid: hid, count: count).expect(in: self)

        // then
        let brands = try XCTUnwrap(result.get())
        XCTAssertEqual(brands.count, count)
    }

    func test_resolvePopularBrands_shouldThrowError() throws {
        // given
        stub(
            requestPartName: "resolvePopularBrands",
            responseFileName: "resolve_popular_brands_error"
        )

        // when
        let result = brandsService.loadPopularBrands(withHid: 1, count: 10).expect(in: self)

        // then
        XCTAssertThrowsError(try result.get()) { error in
            XCTAssertEqual(error as? ServiceError, ServiceError.invalidResponseFormat())
        }
    }

    // MARK: - Check Brand Zone

    func test_checkBrandzone_shouldSendProperRequestAndReturnProperResult() throws {
        // given
        let id = 1
        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            let json = JSON(body)
            let params = json["params"].array
            guard
                let paramsDict = params?.first,
                let parsedType = paramsDict["type"].string,
                let parsedId = paramsDict["brand_id"].int
            else { return false }

            return parsedType == "brand"
                && parsedId == id
        }
        stub(
            requestPartName: "hasCms",
            responseFileName: "has_cms",
            testBlock: isMethodPOST()
                && containsQueryParams(["name": "hasCms"])
                && verifyJsonBody(checkBodyBlock)
        )

        // when
        let result = brandsService.checkBrandzone(forBrandWithId: id).expect(in: self)

        // then
        let response = try XCTUnwrap(result.get())
        XCTAssertTrue(response)
    }

    func test_checkBrandzone_shouldThrowError() throws {
        // given
        stub(
            requestPartName: "hasCms",
            responseFileName: "has_cms_error"
        )

        // when
        let result = brandsService.checkBrandzone(forBrandWithId: 1).expect(in: self)

        // then
        XCTAssertThrowsError(try result.get()) { error in
            XCTAssertEqual(error as? ServiceError, ServiceError.invalidResponseFormat())
        }
    }
}
