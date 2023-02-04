//
//  FiltersAPIStubConfigurator.swift
//  UI Tests
//
//  Created by Alexey Salangin on 6/29/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import Swifter

struct FiltersAPIStubConfigurator {
    enum AnyOfferKind {
        case offer
        case site
        case village
    }

    let dynamicStubs: HTTPDynamicStubs
    let anyOfferKind: AnyOfferKind

    func setupSearchCounter(predicate: Predicate<HttpRequest>, handler: @escaping () -> Void) {
        let stubFilename: String
        let requestPath: String

        switch self.anyOfferKind {
            case .offer:
                stubFilename = "offersNumber-number.debug"
                requestPath = "/2.0/offers/number"
            case .site:
                stubFilename = "offerWithSiteSearch-count.debug"
                requestPath = "/1.0/offerWithSiteSearch.json"
            case .village:
                stubFilename = "offerWithSiteSearch-count.debug"
                requestPath = "/1.0/offerWithSiteSearch.json"
        }

        let middleware = MiddlewareBuilder.predicate(predicate, stubFilename: stubFilename, handler: handler).build()
        self.dynamicStubs.register(method: .GET, path: requestPath, middleware: middleware, context: .init())
    }

    func setupDeveloperSearch() {
        self.dynamicStubs.register(
            method: .GET,
            path: "/1.0/suggest/developer",
            filename: "suggest-developer-capital-group.debug"
        )
    }

    func setupVillageDeveloperSearch() {
        self.dynamicStubs.register(
            method: .GET,
            path: "/2.0/village/developerSuggest",
            filename: "suggest-village-developer-capital-group.debug"
        )
    }

    func setupSamoletDeveloperSearch() {
        self.dynamicStubs.register(
            method: .GET,
            path: "/1.0/suggest/developer",
            filename: "suggest-developer-samolet.debug"
        )
    }

    func setupTagSuggestList() {
        self.dynamicStubs.setupStub(remotePath: "/1.0/suggest/tags",
                                    filename: "tags-sell-apartment.debug")
    }

    /// @pavelcrane: Have no idea what's this for. Kept for now to investigate later.
    func unregisterAllHandlers() {
        self.dynamicStubs.register(method: .GET, path: "/2.0/offers/number", filename: "offersNumber-number.debug")
    }
}
