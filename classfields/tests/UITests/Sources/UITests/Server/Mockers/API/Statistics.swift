//
//  Statistics.swift
//  UITests
//
//  Created by Denis Mamnitskii on 17.08.2021.
//

import AutoRuProtoModels
import SwiftProtobuf

extension Mocker {
    @discardableResult
    func mock_statsPredict() -> Self {
        server.addHandler("POST /stats/predict") { request, _ in
            Response.okResponse(fileName: "trade_in_predict_price")
        }
        return self
    }
}
