//
//  ConciergeAPIStubConfigurator.swift
//  UI Tests
//
//  Created by Alexey Salangin on 16.11.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import XCTest
import Swifter

struct ConciergeAPIStubConfigurator {
    let dynamicStubs: HTTPDynamicStubs

    func setupConcierge() {
        dynamicStubs.setupStub(remotePath: "/2.0/concierge/ticket",
                               filename: "commonEmpty.debug",
                               method: .POST)
    }

    func setupConciergeError() {
        dynamicStubs.setupStub(remotePath: "/2.0/concierge/ticket",
                               filename: "commonError.debug",
                               method: .POST)
    }

    func setupConcierge(predicate: Predicate<HttpRequest>, handler: @escaping () -> Void) {
        let stubFilename = "commonEmpty.debug"
        let requestPath = "/2.0/concierge/ticket"

        let middleware = MiddlewareBuilder.predicate(predicate, stubFilename: stubFilename, handler: handler).build()
        self.dynamicStubs.register(method: .POST, path: requestPath, middleware: middleware, context: .init())
    }
}
