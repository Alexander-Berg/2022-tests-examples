import BeruLegacyNetworking
import BeruMapping
import MarketModels
import XCTest

@testable import BeruServices

class SecretSaleServiceTests: NetworkingTestCase {

    var service: SecretSaleServiceImpl!

    override func setUp() {
        super.setUp()

        // Некоторые из запросов SecretSaleServiceImpl имеют authenticationType = .required
        let apiClient = DependencyProvider().legacyAPIClient
        apiClient.token = token

        service = SecretSaleServiceImpl(
            apiClient: APIClient(apiClient: apiClient)
        )
    }

    override func tearDown() {
        service = nil
        super.tearDown()
    }

    // MARK: - obtainCurrentSecretSale method

    func test_shouldReturnSecretSaleModel_whenCurrentSalesRequestSucceeded() throws {
        // given
        stub(
            requestPartName: "api/",
            responseFileName: "resolve_secret_sale",
            headers: [
                "X-User-Authorization": "OAuth \(token)"
            ]
        )

        // when
        let result = service.obtainCurrentSecretSale().expect(in: self)

        // then
        let model = try result.get()

        XCTAssertNotNil(model?.id)
        XCTAssertNotNil(model?.startDate)
        XCTAssertNotNil(model?.endDate)
    }

    func test_shouldReturnNil_whenResponseIsEmpty() throws {
        // given
        stub(requestPartName: "api/", responseFileName: "resolve_secret_sale_empty")

        // when
        let result = service.obtainCurrentSecretSale().expect(in: self)

        // then
        XCTAssertNil(try result.get())
    }

    func test_shouldReturnError_whenNetworkErrorOccured() {
        // given
        stubNetworkError(requestPartName: "api/", with: .notConnectedToInternet)

        // when
        let result = service.obtainCurrentSecretSale().expect(in: self)

        // then
        XCTAssertThrowsError(try result.get())
    }

    // MARK: - bindUserSecretSale method

    func test_shouldReturnSecretSaleModel_whenBindingSucceded() throws {
        // given
        stub(requestPartName: "api/", responseFileName: "resolve_secret_sale")

        // when
        let result = service.bindUserWithSecretSale(id: "12345", token: "eyJhbGciOiJIUzUxMiIsInR5").expect(in: self)

        // then
        let model = try result.get()

        XCTAssertNotNil(model.id)
        XCTAssertNotNil(model.startDate)
        XCTAssertNotNil(model.endDate)
    }

    func test_shouldReturnSaleError_whenBindingErrorOccured() {
        // given
        stub(requestPartName: "api/", responseFileName: "resolve_secret_sale_error")

        // when
        let result = service.bindUserWithSecretSale(id: "12345", token: "eyJhbGciOiJIUzUxMiIsInR5").expect(in: self)

        // then
        if case let .failure(error) = result, let bindingError = error as? SaleBindingError {
            XCTAssertNotNil(bindingError.saleError)
            XCTAssertNil(bindingError.authError)
        } else {
            XCTFail("Result is not SaleBindingError")
        }
    }

    func test_shouldReturnAuthError_whenBindingErrorOccured() {
        // given
        stub(requestPartName: "api/", responseFileName: "resolve_secret_sale_auth_error")

        // when
        let result = service.bindUserWithSecretSale(id: "12345", token: "eyJhbGciOiJIUzUxMiIsInR5").expect(in: self)

        // then
        if case let .failure(error) = result, let bindingError = error as? SaleBindingError {
            XCTAssertNotNil(bindingError.authError)
            XCTAssertNil(bindingError.saleError)
        } else {
            XCTFail("Result is not SaleBindingError")
        }
    }

    func test_shouldReturnSaleAndAuthErrors_whenBindingErrorOccured() {
        // given
        stub(requestPartName: "api/", responseFileName: "resolve_secret_sale_both_errors")

        // when
        let result = service.bindUserWithSecretSale(id: "12345", token: "eyJhbGciOiJIUzUxMiIsInR5").expect(in: self)

        // then
        if case let .failure(error) = result, let bindingError = error as? SaleBindingError {
            XCTAssertNotNil(bindingError.authError)
            XCTAssertNotNil(bindingError.saleError)
        } else {
            XCTFail("Result is not SaleBindingError")
        }
    }

    func test_shouldReturnError_whenNetworkErrorOccuredWhileBinding() {
        // given
        stubNetworkError(requestPartName: "api/", with: .notConnectedToInternet)

        // when
        let result = service.bindUserWithSecretSale(id: "12345", token: "eyJhbGciOiJIUzUxMiIsInR5").expect(in: self)

        // then
        if case let .failure(error as ApiClientError) = result,
           case let .network(_, _, _, originalError as URLError) = error {
            XCTAssertEqual(originalError.code, .notConnectedToInternet)
        } else {
            XCTFail("Actual error doesn't equal to expected")
        }
    }
}

// MARK: - Test data

private extension SecretSaleServiceTests {
    private var token: String {
        "123124123123123"
    }
}
