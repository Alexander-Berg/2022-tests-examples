//
//  Dictionaries.swift
//  UITests
//
//  Created by Pavel Savchenkov on 21.10.2021.
//

import Foundation

extension Mocker {

    @discardableResult
    func mock_dictionariesMessagePresets() -> Self {
        server.addHandler("GET /reference/catalog/all/dictionaries/v1/message_presets") { (_, _) -> Response? in
            return Response.okResponse(fileName: "message_presets", userAuthorized: true)
        }
        return self
    }

    @discardableResult
    func mock_dictionariesMessageHelloPresets() -> Self {
        server.addHandler("GET /reference/catalog/all/dictionaries/v1/message_hello_presets") { (_, _) -> Response? in
            return Response.okResponse(fileName: "message_presets", userAuthorized: true)
        }
        return self
    }

    @discardableResult
    func mock_dictionariesSellerMessagePresets() -> Self {
        server.addHandler("GET /reference/catalog/all/dictionaries/v1/seller_message_presets") { (_, _) -> Response? in
            return Response.okResponse(fileName: "message_presets", userAuthorized: true)
        }
        return self
    }

    @discardableResult
    func mock_dictionariesSellerMessageHelloPresets() -> Self {
        server.addHandler("GET /reference/catalog/all/dictionaries/v1/seller_message_hello_presets") { (_, _) -> Response? in
            return Response.okResponse(fileName: "message_presets", userAuthorized: true)
        }
        return self
    }
}
