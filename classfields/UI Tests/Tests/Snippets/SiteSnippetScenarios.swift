//
//  SiteSnippetScenarios.swift
//  UI Tests
//
//  Created by Pavel Zhuravlev on 31.05.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest

final class SiteSnippetScenarios {
    init(snippet: SiteSnippetSteps, dynamicStubs: HTTPDynamicStubs) {
        self.snippet = snippet
        self.dynamicStubs = dynamicStubs
    }

    // MARK: Private

    private let snippet: SiteSnippetSteps
    private let dynamicStubs: HTTPDynamicStubs

    private enum Consts {
        static let siteID: String = "296015"
    }
}

// MARK: - Calls

extension SiteSnippetScenarios {
    @discardableResult
    func runBillingOnCall() -> Self {
        let callScenarios = SiteCallScenarios(dynamicStubs: self.dynamicStubs, callButtonHandler: self.snippet)
        callScenarios.runBillingOnCall()

        return self
    }

    @discardableResult
    func runCallWithTwoPhoneNumbers(siteID: String = Consts.siteID) -> Self {
        let callScenarios = SiteCallScenarios(
            dynamicStubs: self.dynamicStubs,
            callButtonHandler: self.snippet,
            siteID: siteID
        )
        callScenarios.runCallWithTwoPhoneNumbers()

        return self
    }

    @discardableResult
    func runCallWithError(siteID: String = Consts.siteID) -> Self {
        let callScenarios = SiteCallScenarios(
            dynamicStubs: self.dynamicStubs,
            callButtonHandler: self.snippet,
            siteID: siteID
        )
        callScenarios.runCallWithError()

        return self
    }

    @discardableResult
    func runAddToCallHistory() -> Self {
        // To load a list of phones to call
        SnippetsListAPIStubConfigurator.setupGetSiteSinglePhoneNumber(using: self.dynamicStubs)
        // To load an item of Call History
        SiteCardAPIStubConfiguration.setupSiteCard(using: self.dynamicStubs, stubKind: .billing)

        let requestExpectation = XCTestExpectation(description: "Request of Call History list")
        SiteCardAPIStubConfiguration
            .setupSiteCardExpectation(requestExpectation, using: self.dynamicStubs)

        self.snippet
            .tapOnCallButton()

        // Go to Call History screen

        RootNavigationSteps()
            .tabBarTapOnComunicationItem()
        CommunicationSteps()
            .isScreenPresented()
            .tapOnCallHistorySegment()

        let result = XCTWaiter.yreWait(
            for: [requestExpectation],
            timeout: Constants.timeout
        )
        XCTAssertTrue(result, "Expectation has not been fulfilled")

        return self
    }
}

// MARK: - Favorites

extension SiteSnippetScenarios {
    @discardableResult
    func runAddToFavorites() -> Self {
        let addToFavoritesExpectation = XCTestExpectation(description: "Add an offer to Favorites")
        FavoritesAPIStubConfigurator.setupAddOfferExpectation(addToFavoritesExpectation, using: self.dynamicStubs)

        self.snippet
            .tapOnFavoriteButton()

        FavoritesRequestPushPermissionsSteps()
            .closeIfPresented()

        AddedToFavoritesNotificationSteps()
            .closeIfPresented()

        let addItemResult = XCTWaiter.yreWait(
            for: [addToFavoritesExpectation],
            timeout: Constants.timeout
        )
        XCTAssertTrue(addItemResult, "Expectation has not been fulfilled")

        // Go to Favorites screen

        let requestFavoriteItemsExpectation = XCTestExpectation(description: "Request of Favorite items")
        FavoritesAPIStubConfigurator
            .setupListExpectation(requestFavoriteItemsExpectation, using: self.dynamicStubs)

        let requestOfferExpectation = XCTestExpectation(description: "Request of an item from Favorites list")
        SnippetsListAPIStubConfigurator.setupListExpectation(
            requestOfferExpectation,
            using: self.dynamicStubs
        )

        RootNavigationSteps()
            .tabBarTapOnFavoriteItem()

        let requestItemResult = XCTWaiter.yreWait(
            for: [requestFavoriteItemsExpectation, requestOfferExpectation],
            timeout: Constants.timeout
        )
        XCTAssertTrue(requestItemResult, "Expectation has not been fulfilled")

        return self
    }

    @discardableResult
    func runRemoveFromFavorites(whenInFavoritesList favoritesList: FavoritesListSteps) -> Self {
        self.snippet
            .tapOnFavoriteButton()
        
        favoritesList
            .сonfirmRemoving()
            .withSiteList()
            .isListEmpty()

        return self
    }
}
