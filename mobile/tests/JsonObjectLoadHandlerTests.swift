//
//  JsonObjectLoadHandlerTests.swift
//  Pods
//
//  Created by Mikhail Kurenkov on 7/27/20.
//

import XCTest
import RxCocoa
@testable import YandexMapsCommonComponents

class JsonObjectLoadHandlerTests: XCTestCase {
    
    func testValidData() {
        let handler = JsonObjectLoadHandler<HandlerTestsData>()
        do {
            let testObject = HandlerTestsData()
            let testData = try JSONEncoder().encode(testObject)
            let obj = try handler.parse(data: testData)
            XCTAssert(obj == testObject)
        } catch {
            XCTAssert(false, "The data should be successfully parsed")
        }
    }
    
    func testJsonParsingMalformedError() {
        let handler = JsonObjectLoadHandler<HandlerTestsData>()
        do {
            let data = Data(repeating: 0, count: 8)
            _ = try handler.parse(data: data)
            XCTAssert(false, "The data should not be parsedы")
        } catch {
            guard case JsonObjectLoadHandlerError.jsonParsingMalformedError = error else {
                XCTAssert(false, "Unexpected error: \(error), expected .jsonParsingMalformedError")
                return
            }
        }
    }
    
    func testJsonParsingUnexpectedFormatErrorKeyNotFound() {
        let handler = JsonObjectLoadHandler<HandlerTestsData>()
        do {
            let json = "{\"intField\":0,\"optionalStringField\":\"string\"}"
            _ = try handler.parse(data: json.data(using: .utf8)!)
            XCTAssert(false, "The data should not be parsedы")
        } catch {
            guard case JsonObjectLoadHandlerError.jsonParsingUnexpectedFormatError = error else {
                XCTAssert(false, "Unexpected error: \(error), expected .jsonParsingUnexpectedFormatError")
                return
            }
        }
    }
    
    func testJsonParsingUnexpectedFormatErrorTypeMismatch() {
        let handler = JsonObjectLoadHandler<HandlerTestsData>()
        do {
            let json = "{\"intField\":\"string\",\"stringField\":\"string\",\"optionalStringField\":\"string\"}"
            _ = try handler.parse(data: json.data(using: .utf8)!)
            XCTAssert(false, "The data should not be parsed")
        } catch {
            guard case JsonObjectLoadHandlerError.jsonParsingUnexpectedFormatError = error else {
                XCTAssert(false, "Unexpected error: \(error), expected .jsonParsingUnexpectedFormatError")
                return
            }
        }
    }
    
    func testHandleNonHTTPResponseError() {
        let handler = JsonObjectLoadHandler<HandlerTestsData>()
        
        var error = handler.mapError(fromRxCocoaURLError: .deserializationError(error: FakeError()))
        guard case JsonObjectLoadHandlerError.jsonParsingMalformedError = error else {
            XCTAssert(false, "Unexpected error: \(error), expected .jsonParsingMalformedError")
            return
        }
        
        let response = URLResponse()
        error = handler.mapError(fromRxCocoaURLError: .nonHTTPResponse(response: response))
        guard case JsonObjectLoadHandlerError.jsonParsingMalformedError = error else {
            XCTAssert(false, "Unexpected error: \(error), expected .jsonParsingMalformedError")
            return
        }
    }

    func testHandleHttpRequestFailedError() {
        let handler = JsonObjectLoadHandler<HandlerTestsData>()

        let response = HTTPURLResponse()
        let error = handler.mapError(fromRxCocoaURLError: .httpRequestFailed(response: response, data: nil))
        guard case JsonObjectLoadHandlerError.httpError = error else {
            XCTAssert(false, "Unexpected error: \(error), expected .httpError")
            return
        }
    }

    func testHandleUnknownError() {
        let handler = JsonObjectLoadHandler<HandlerTestsData>()

        let error = handler.mapError(fromRxCocoaURLError: .unknown)
        guard case JsonObjectLoadHandlerError.unknown = error else {
            XCTAssert(false, "Unexpected error: \(error), expected .unknown")
            return
        }
    }
    
    func testValidDataWhereTopLevelObjectIsJsonFragment() {
        let handler = JsonObjectLoadHandler<Bool>()
        do {
            let testObject = false
            let testData = try JSONSerialization.data(withJSONObject: testObject, options: .fragmentsAllowed)
            let obj = try handler.parse(data: testData)
            XCTAssert(obj == testObject)
        } catch {
            XCTAssert(false, "The data should be successfully parsed")
        }
    }
    
}

struct HandlerTestsData: Codable {
    
    // MARK: - Public properties
    
    let intField: Int
    let stringField: String
    let optionalStringField: String?
    
    // MARK: - Constructors
    
    init(intField: Int, stringField: String, optionalStringField: String?) {
        self.intField = intField
        self.stringField = stringField
        self.optionalStringField = optionalStringField
    }
    
    init() {
        self.init(intField: 0, stringField: "string", optionalStringField: "string")
    }
    
    // MARK: - Public methods
    
    static func ==(lhs: HandlerTestsData, rhs: HandlerTestsData) -> Bool {
        return lhs.stringField == rhs.stringField &&
            lhs.optionalStringField == rhs.optionalStringField &&
            lhs.intField == rhs.intField
    }
    
}

class FakeError: Error { }
