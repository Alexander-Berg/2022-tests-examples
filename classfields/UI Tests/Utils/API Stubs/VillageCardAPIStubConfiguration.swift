//
//  VillageCardAPIStubConfiguration.swift
//  UI Tests
//
//  Created by Arkady Smirnov on 4/6/21.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest

final class VillageCardAPIStubConfiguration {
    static func setupVillageCardBilling(using dynamicStubs: HTTPDynamicStubs, villageID: String = "1835631") {
        dynamicStubs.setupStub(remotePath: "/2.0/village/\(villageID)/card", filename: "villageCard-billing.debug")
    }

    static func setupGetVillagePhoneNumber(using dynamicStubs: HTTPDynamicStubs, villageID: String = "1835631") {
        dynamicStubs.setupStub(remotePath: "2.0/village/\(villageID)/contacts", filename: "villageCard-getPhone.debug")
    }
}

extension VillageCardAPIStubConfiguration {
    static func setupVillageCardExpectation(
        _ expectation: XCTestExpectation,
        using dynamicStubs: HTTPDynamicStubs,
        villageID: String = "1835631"
    ) {
        dynamicStubs.register(
            method: .GET,
            path: "/2.0/village/\(villageID)/card",
            middleware: MiddlewareBuilder
                .chainOf([
                    .expectation(expectation),
                    .respondWith(.ok(.contentsOfJSON("villageCard-billing.debug"))),
                ])
                .build()
        )
    }
}
