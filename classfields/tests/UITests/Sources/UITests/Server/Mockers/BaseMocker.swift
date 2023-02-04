//
//  BaseMocker.swift
//  UITests
//
//  Created by Vitalii Stikhurov on 30.04.2021.
//

import Foundation
import AutoRuProtoModels
import SwiftProtobuf

struct Mocker {
    let basicMockReproducer = BasicMockReproducer()
    let advancedMockReproducer = AdvancedMockReproducer()
    let port: UInt16
    let server: StubServer

    init() {
        self.port = UInt16.random(in: 1024 ..< UInt16.max)
        self.server = StubServer(port: port)
    }
}

extension Mocker {
    func startMock() {
        try! server.start()
    }

    func stopMock() {
        server.stop()
    }

    @discardableResult
    func setForceLoginMode(_ mode: StubServer.LoginMode) -> Self {
        server.forceLoginMode = mode
        return self
    }

    func mock_base() -> Self {
        server.addHandler("POST /device/hello") { (_, _) -> Response? in
            return Response.okResponse(fileName: "hello_ok")
        }

        server.addHandler("GET /geo/suggest *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "geo_suggest_1")
        }
        return self
    }
}
