//
//  NetworkErrorFormatterTests.swift
//  AutoRu
//
//  Created by Vitalii Stikhurov on 10.04.2020.
//

import Foundation
import XCTest
import AutoRuProtobuf
import AutoRuProtoModels
@testable import AutoRuFormatters

final class NetworkErrorFormatterTests: BaseUnitTest {

    func testConnectionError() {
        let codes = [
            NSURLErrorNotConnectedToInternet: "Проверьте подключение к\(String.nbsp)интернету\nи попробуйте ещё",
            NSURLErrorInternationalRoamingOff: "Проблемы с подключением к\(String.nbsp)интернету (роуминг выключен)",
            NSURLErrorCallIsActive: "Проблемы с подключением к\(String.nbsp)интернету. Завершите звонок и\(String.nbsp)попробуйте ещё",
            NSURLErrorDataNotAllowed: "Проблемы с подключением к\(String.nbsp)интернету (сотовая сеть запретила соединение)"
        ]

        for (code, message) in codes {
            let error = NSError(domain: NSURLErrorDomain, code: code, userInfo: nil)
            let errorMsg = NetworkErrorFormatter.string(from: error)
            XCTAssertEqual(errorMsg, message)
        }
    }

    func testIsNoRoute() {
        let codes = [
            NSURLErrorCannotFindHost,
            NSURLErrorCannotConnectToHost,
            NSURLErrorDNSLookupFailed,
            NSURLErrorNetworkConnectionLost,
            NSURLErrorTimedOut
        ]

        for code in codes {
            let error = NSError(domain: NSURLErrorDomain, code: code, userInfo: nil)
            let errorMsg = NetworkErrorFormatter.string(from: error)
            XCTAssertEqual(errorMsg, "Не удалось подключиться к сервису\nпопробуйте позже")
        }
    }

    func testApiError() {
        var resp = Auto_Api_ErrorResponse()
        resp.status = .error
        resp.error = .noAuth

        let error = Auto_Api_Error(response: resp, httpStatusCode: resp.status.rawValue)
        let errorMsg = NetworkErrorFormatter.string(from: error)
        XCTAssertEqual(errorMsg, "Требуется авторизация")
    }

    func testApiError_withBackendReducer_hasDesc() {
        var resp = Auto_Api_ErrorResponse()
        resp.status = .error
        resp.error = .noAuth
        resp.descriptionRu = "Desc"
        let error = Auto_Api_Error(response: resp, httpStatusCode: resp.status.rawValue)
        let errorMsg = NetworkErrorFormatter.string(from: error) { $0.description }
        XCTAssertEqual(errorMsg, "Desc")
    }

    func testApiError_withBackendReducer_noDesc() {
        var resp = Auto_Api_ErrorResponse()
        resp.status = .error
        resp.error = .noAuth

        let error = Auto_Api_Error(response: resp, httpStatusCode: resp.status.rawValue)
        let errorMsg = NetworkErrorFormatter.string(from: error) { $0.description }
        XCTAssertEqual(errorMsg, "Требуется авторизация")
    }

    func testApiError_withBackendReducer_noDesc_defaultValue() {
        var resp = Auto_Api_ErrorResponse()
        resp.status = .error
        resp.error = .noAuth

        let error = Auto_Api_Error(response: resp, httpStatusCode: resp.status.rawValue)
        let errorMsg = NetworkErrorFormatter.string(from: error, defaultApiErrorString: "Default")
        XCTAssertEqual(errorMsg, "Default")
    }
}
