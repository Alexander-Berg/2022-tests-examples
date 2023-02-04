//
//  SavedSearchAPIStubConfigurator.swift
//  UI Tests
//
//  Created by Arkady Smirnov on 4/28/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import Swifter
import YRETestsUtils

final class SavedSearchAPIStubConfigurator {
    enum SingleItemList: String, CaseIterable {
        case byRegion = "savedSearch-by-region-get.debug"
        case byViewPort = "savedSearch-by-viewport-get.debug"
        case byGeointent = "savedSearch-by-geointent-get.debug"
        case byPolygons = "savedSearch-by-polygons-get.debug"
        case byCommuteTime = "savedSearch-by-commutetime-get.debug"
        case byComposite = "savedSearch-by-composite-get.debug"
    }

    enum Constants {
        static let timeout: TimeInterval = 0.5
    }

    static func setupListing(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: "/2.0/savedSearch",
            filename: "savedSearch-get.debug",
            requestTime: Constants.timeout
        )
    }

    static func setupSingleItemListing(using dynamicStubs: HTTPDynamicStubs, type: SingleItemList) {
        dynamicStubs.register(
            method: .GET,
            path: "/2.0/savedSearch",
            filename: type.rawValue,
            requestTime: Constants.timeout
        )
    }

    static func setupListingWithModifiedVisitTime(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: "/2.0/savedSearch",
            filename: "savedSearch-modifiedVisitTime-get.debug",
            requestTime: Constants.timeout
        )
    }

    static func setupEmptyListing(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: "/2.0/savedSearch",
            filename: "savedSearch-empty-get.debug",
            requestTime: Constants.timeout
        )
    }

    static func setupSearchResultList(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: "/1.0/offerWithSiteSearch.json",
            filename: "offerWithSiteSearch-get.debug",
            requestTime: Constants.timeout
        )
    }

    static func setupRequiredFeature(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: "/1.0/device/requiredFeature",
            filename: "requiredFeature-get.debug",
            requestTime: Constants.timeout
        )
    }

    static func setupRequiredFeatureWithUnknownFeature(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: "/1.0/device/requiredFeature",
            filename: "requiredFeature-get.savedSearch.debug",
            requestTime: Constants.timeout
        )
    }

    static func setupRequiredFeatureWithError(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: "/1.0/device/requiredFeature",
            middleware: MiddlewareBuilder
                .chainOf([
                    .requestTime(Constants.timeout),
                    .respondWith(.internalServerError()),
                ])
                .build()
        )
    }

    static func setupPut(using dynamicStubs: HTTPDynamicStubs, with expectation: XCTestExpectation) {
        dynamicStubs.register(
            method: .PUT,
            path: "/2.0/savedSearch/:path",
            middleware: MiddlewareBuilder
                .flatMap({ (request, _, _) -> MiddlewareBuilder in
                    let errorChain = MiddlewareBuilder.chainOf([
                        .expectation(expectation),
                        .requestTime(Constants.timeout),
                        .respondWith(.internalServerError())
                    ])

                    guard let identifier = request.params[":path"],
                        let json = ResourceProvider.jsonObject(from: "savedSearch-put.debug") as? NSDictionary
                    else {
                        assertionFailure("Couldn't load stub-file")
                        return errorChain
                    }

                    let mutableJSON = NSMutableDictionary(dictionary: json)
                    if let responseJSON = mutableJSON["response"] as? NSDictionary {
                        let mutableResponseJSON = NSMutableDictionary(dictionary: responseJSON)
                        mutableResponseJSON["id"] = identifier
                        mutableJSON["response"] = mutableResponseJSON

                        let result = HttpResponseBody.json(mutableJSON)

                        return .chainOf([
                            .expectation(expectation),
                            .requestTime(Constants.timeout),
                            .respondWith(.ok(HttpResponseBodyProvider(generator: result))),
                        ])
                    }
                    else {
                        assertionFailure("Invalid format of JSON objectin stub-file")
                        return errorChain
                    }
                })
                .build()
            )
    }

    static func setupListing(using dynamicStubs: HTTPDynamicStubs, with expectation: XCTestExpectation) {
        dynamicStubs.register(
            method: .GET,
            path: "/2.0/savedSearch",
            middleware: MiddlewareBuilder
                .chainOf([
                    .expectation(expectation),
                    .requestTime(Constants.timeout),
                    .respondWith(.ok(.contentsOfJSON("savedSearch-get.debug"))),
                ])
                .build()
            )
    }

    static func setupDelete(using dynamicStubs: HTTPDynamicStubs, with expectation: XCTestExpectation) {
        dynamicStubs.register(
            method: .DELETE,
            path: "/2.0/savedSearch/1eaf3b2",
            middleware: MiddlewareBuilder
                .chainOf([
                    .expectation(expectation),
                    .requestTime(Constants.timeout),
                    .respondWith(.ok(.contentsOfJSON("savedSearch-put.debug"))),
                ])
                .build()
            )
    }

    static func setupSearchVisit(using dynamicStubs: HTTPDynamicStubs, with expectation: XCTestExpectation) {
        dynamicStubs.register(
            method: .PUT,
            path: "/2.0/savedSearch/1eaf3b2/visit",
            middleware: MiddlewareBuilder
                .chainOf([
                    .expectation(expectation),
                    .requestTime(Constants.timeout),
                    .respondWith(.ok(.contentsOfJSON("savedSearch-put.debug"))),
                ])
                .build()
            )
    }
}

extension SavedSearchAPIStubConfigurator {
    static func setupOffersListForNewItems(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: "/1.0/offerWithSiteSearch.json",
            middleware: MiddlewareBuilder
                .flatMap({ request, _, _ in
                    let predicate: Predicate<HttpRequest> = .contains(queryKey: "updateTimeMin")
                    if predicate.matches(request) {
                        return .respondWith(.ok(.contentsOfJSON("offerSnippet-common-singleItem.debug")))
                    }
                    else {
                        return .respondWith(.ok(.contentsOfJSON("offerSnippet-common-empty.debug")))
                    }
                })
                .build()
        )
    }
}
