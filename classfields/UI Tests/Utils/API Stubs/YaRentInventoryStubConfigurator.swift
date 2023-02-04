//
//  YaRentInventoryStubConfigurator.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 06.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import XCTest

final class YaRentInventoryStubConfigurator {
    static func setupInitialInventory(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.lastInventory,
            filename: "inventory-owner-initial.debug"
        )
    }

    static func setupFilledInventory(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.lastInventory,
            filename: "inventory-owner-filled.debug"
        )
    }

    static func setupOwnerNeedToConfirmInventory(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.lastInventory,
            filename: "inventory-owner-needToConfirm.debug"
        )
    }

    static func setupTenantNeedToConfirmInventory(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.lastInventory,
            filename: "inventory-tenant-needToConfirm.debug"
        )
    }

    static func setupEditInventory(using dynamicStubs: HTTPDynamicStubs, expectation: XCTestExpectation) {
        let middleware = MiddlewareBuilder()
            .expectation(expectation)
            .respondWith(.ok(.contentsOfJSON("inventory-owner-initial.debug")))
            .build()

        dynamicStubs.register(method: .POST, path: Paths.editInventory, middleware: middleware)
    }

    static func setupRequestConfirmationSMSInfo(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .POST,
            path: Paths.requestConfirmationInventory,
            filename: "inventory-confirmation-smsInfo.debug"
        )
    }

    static func setupSubmitConfirmation(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .POST,
            path: Paths.submitConfirmationInventory,
            filename: "commonEmptyResponse.debug"
        )
    }

    private enum Paths {
        static let lastInventory = "2.0/rent/user/me/inventory/\(Self.ownerRequestID)/last"
        static let editInventory = "2.0/rent/user/me/inventory/\(Self.ownerRequestID)/edit"
        static let requestConfirmationInventory = "2.0/rent/user/me/inventory/\(Self.ownerRequestID)/confirmation-code/request"
        static let submitConfirmationInventory = "2.0/rent/user/me/inventory/\(Self.ownerRequestID)/confirmation-code/submit"

        private static let ownerRequestID = "ownerRequestID"
    }
}
