import MarketFlexCore
import MarketNetworking
import XCTest

// swiftlint:disable implicitly_unwrapped_optional

final class DocumentServiceDecoratorTests: XCTestCase {

    var mock: DocumentServiceErrorMock!
    var logger: DocumentServiceLoggerMock!
    var decorator: DocumentServiceDecorator!

    override func setUp() {
        super.setUp()

        mock = DocumentServiceErrorMock()
        logger = DocumentServiceLoggerMock()
        decorator = DocumentServiceDecorator(service: mock, logger: logger)
    }

    func test_documentDecodingError() async throws {
        mock.error = DecodingError.dataCorrupted(.init(codingPath: [], debugDescription: ""))

        _ = try? await decorator.obtainDocument(.init(documentQuery: .main))

        let error = try XCTUnwrap(logger.loggedDocumentDecodingErrors.first)
        XCTAssert(error is DecodingError)

        XCTAssertNil(logger.loggedRequestErrors.first)
    }

    func test_documentRequestError() async throws {
        mock.error = NSError(domain: "test.error", code: 123)

        _ = try? await decorator.obtainDocument(.init(documentQuery: .main))

        let error = try XCTUnwrap(logger.loggedRequestErrors.first) as NSError
        XCTAssertEqual(error.domain, "test.error")
        XCTAssertEqual(error.code, 123)

        XCTAssertNil(logger.loggedDocumentDecodingErrors.first)
    }
}
