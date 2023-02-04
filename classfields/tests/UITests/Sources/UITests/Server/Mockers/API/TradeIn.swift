//
//  TradeIn.swift
//  UITests
//
//  Created by Denis Mamnitskii on 13.08.2021.
//

import AutoRuProtoModels
import SwiftProtobuf

extension Mocker {
    @discardableResult
    func mock_tradeInIsAvailable(with isAvailable: Bool) -> Self {
        server.addHandler("POST /trade-in/is_available") { request, _ in
            Response.responseWithStatus(body: "{\"is_available\": \(isAvailable)}".data(using: .utf8))
        }
        return self
    }

    @discardableResult
    func mock_tradeInApply(withError: Bool = false) -> Self {
        let status = withError ? "ERROR" : "SUCCESS"
        server.addHandler("PUT /trade-in/apply") { request, _ in
            Response.responseWithStatus(body: "{\"status\":\"\(status)\"}".data(using: .utf8))
        }
        return self
    }

    @discardableResult
    func mock_requestTradeIn(id: String) -> Self {
        server.api.offer.category(.cars).offerID(id).tradeIn.post
            .ok(mock: .file("success"))
        return self
    }
}
