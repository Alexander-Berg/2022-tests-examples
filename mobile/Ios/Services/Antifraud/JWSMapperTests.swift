import MarketDTO
import MarketModels
import XCTest

@testable import BeruServices

class JWSMapperTests: XCTestCase {

    // MARK: - Properties

    var mocksFactory: CheckoutMapperMocksFactory!

    // MARK: - Lifecycle

    override func setUp() {
        super.setUp()
        mocksFactory = CheckoutMapperMocksFactory()
    }

    override func tearDown() {
        mocksFactory = nil
        super.tearDown()
    }

    // MARK: - Tests

    func test_makeJWSParams() {
        // given
        let inputToken = "test token"

        // when
        let result = JWSMapper.makeResolveJWSParams(from: inputToken)

        // then
        XCTAssertEqual(result.token, inputToken)
    }

    func test_jwsMapping_whenValidResolveJWSResult() throws {
        // given
        let validInput = "eyJhbGciOiJIUzI1NiJ9"
            + ".eyJleHAiOjE2MzY1ODc2MjUsInV1aWQiOiI0NjM4ZGRhYWQyMDY0ZDdkODgxNzU1NTlmZmIyY2I3MCJ9"
            + ".L7zZKWCCzNX53dAwtPwHpbfSGzTe4K8tFTEwGMuU6cs"

        // when
        let result = try JWSMapper.extractJWS(from: validInput)

        // then
        let expirationDate = Date(timeIntervalSince1970: 1_636_587_625) // 2021-11-10T23:40:25.000Z
        let expectedResult = JWS(
            expirationDate: expirationDate,
            rawRepresentation: validInput
        )
        XCTAssertEqual(result, expectedResult)
    }

    func test_jwsMapping_whenEmptyResult() {
        // given
        let emptyInput = ""

        // when
        XCTAssertThrowsError(try JWSMapper.extractJWS(from: emptyInput), "") { error in
            XCTAssertEqual(error as? JWSMappingError, JWSMappingError.invalidFormat)
        }
    }
}
