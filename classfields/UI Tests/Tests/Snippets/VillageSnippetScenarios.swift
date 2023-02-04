//
//  VillageSnippetScenarios.swift
//  UI Tests
//
//  Created by Pavel Zhuravlev on 04.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest

final class VillageSnippetScenarios {
    init(snippet: VillageSnippetSteps, dynamicStubs: HTTPDynamicStubs) {
        self.snippet = snippet
        self.dynamicStubs = dynamicStubs
    }

    // MARK: Private

    private let snippet: VillageSnippetSteps
    private let dynamicStubs: HTTPDynamicStubs
}

// MARK: - Calls

extension VillageSnippetScenarios {
    @discardableResult
    func runBillingOnCall() -> Self {
        let callScenarios = VillageCallScenarios(dynamicStubs: self.dynamicStubs, callButtonHandler: self.snippet)
        callScenarios.runBillingOnCall()

        return self
    }

    @discardableResult
    func runCallWithTwoPhoneNumbers() -> Self {
        let callScenarios = VillageCallScenarios(dynamicStubs: self.dynamicStubs, callButtonHandler: self.snippet)
        callScenarios.runCallWithTwoPhoneNumbers()

        return self
    }

    @discardableResult
    func runCallWithError() -> Self {
        let callScenarios = VillageCallScenarios(dynamicStubs: self.dynamicStubs, callButtonHandler: self.snippet)
        callScenarios.runCallWithError()

        return self
    }

    @discardableResult
    func runAddToCallHistory() -> Self {
        // To load a list of phones to call
        SnippetsListAPIStubConfigurator.setupGetVillageOnePhoneNumber(using: self.dynamicStubs)
        // To load an item of Call History
        VillageCardAPIStubConfiguration.setupVillageCardBilling(using: self.dynamicStubs)

        let requestExpectation = XCTestExpectation(description: "Request of Call History list")
        VillageCardAPIStubConfiguration
            .setupVillageCardExpectation(requestExpectation, using: self.dynamicStubs)

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

extension VillageSnippetScenarios {
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
