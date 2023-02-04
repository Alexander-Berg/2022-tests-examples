//
//  FavoritesAPIStubConfigurator.swift
//  UI Tests
//
//  Created by Arkady Smirnov on 3/10/21.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest

class FavoritesAPIStubConfigurator {
    static func setupListing(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: "/1.0/favorites.json",
            filename: "favorites.debug",
            requestTime: Constants.timeout
        )
    }

    static func setupAddOfferExpectation(
        _ expectation: XCTestExpectation,
        using dynamicStubs: HTTPDynamicStubs
    ) {
        dynamicStubs.register(
            method: .PATCH,
            path: "/1.0/favorites.json",
            middleware: MiddlewareBuilder
                .chainOf([
                    .expectation(expectation),
                    .respondWith(.ok(.contentsOfJSON("favorites.debug")))
                ])
                .build()
        )
    }

    static func setupListExpectation(
        _ expectation: XCTestExpectation,
        using dynamicStubs: HTTPDynamicStubs
    ) {
        dynamicStubs.register(
            method: .GET,
            path: "/1.0/favorites.json",
            middleware: MiddlewareBuilder
                .chainOf([
                    .expectation(expectation),
                    .respondWith(.ok(.contentsOfJSON("favorites.debug"))),
                ])
                .build()
        )
    }
}
