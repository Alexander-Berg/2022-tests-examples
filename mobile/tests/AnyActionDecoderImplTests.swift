import MarketFlexCore
import XCTest
@testable import MarketFlexActions

// swiftlint:disable implicitly_unwrapped_optional

final class AnyActionDecoderImplTests: XCTestCase {

    var logger: ActionDecodingLoggerStub!
    var decoder: AnyActionDecoderImpl!

    override func setUp() {
        super.setUp()

        logger = ActionDecodingLoggerStub()
        decoder = AnyActionDecoderImpl(logger: logger)
    }

    func test_debugActionDecoding() throws {
        let json = """
        {
            "type": "DebugAction",
            "message": "message_value"
        }
        """

        let result = try decoder.performDecode(json)

        let expected = AnyAction(action: DebugAction(
            message: "message_value"
        ))

        XCTAssertEqual(result, expected)
        XCTAssertTrue(logger.loggedCorruptedActions.isEmpty)
        XCTAssertTrue(logger.loggedUnsupportedActions.isEmpty)
    }

    func test_corruptedActionDecoding() throws {
        let json = """
        {
            "type": "DebugAction"
        }
        """

        let result = try decoder.performDecode(json)

        let debug = try XCTUnwrap(result.action as? DebugAction)
        XCTAssertEqual(debug.message, "Ошибка парсинга экшена\nТип: DebugAction")

        let logEvent = try XCTUnwrap(logger.loggedCorruptedActions.first)
        XCTAssertEqual(logEvent.type, "DebugAction")
        XCTAssert(logEvent.error is DecodingError)

        XCTAssertTrue(logger.loggedUnsupportedActions.isEmpty)
    }

    func test_unsupportedActionDecoding() throws {
        let json = """
        {
            "type": "type_value"
        }
        """

        let result = try decoder.performDecode(json)

        let debug = try XCTUnwrap(result.action as? DebugAction)
        XCTAssertEqual(debug.message, "Не поддерживаемый тип экшена\nТип: type_value")

        let logEvent = try XCTUnwrap(logger.loggedUnsupportedActions.first)
        XCTAssertEqual(logEvent, "type_value")

        XCTAssertTrue(logger.loggedCorruptedActions.isEmpty)
    }
}
