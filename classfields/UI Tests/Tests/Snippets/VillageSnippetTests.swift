//
//  VillageSnippetTests.swift
//  UI Tests
//
//  Created by Erik Burygin on 23.11.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

final class VillageSnippetTests: BaseTestCase { }

extension VillageSnippetTests {
    func testActionsInSearchResults() {
        SnippetsListAPIStubConfigurator.setupVillagesList(using: self.dynamicStubs, stubKind: .noPhoto)
        self.relaunchApp(with: .snippetsTests)

        let list = Self.obtainMainList()
        let snippet = Self.obtainSnippet(from: list)

        let scenarios = VillageSnippetScenarios(
            snippet: snippet,
            dynamicStubs: self.dynamicStubs
        )
        Self.performCommonSteps(using: scenarios)

        // Pay attention - this scenario moves us to the Favorites tab.
        //
        // Currently we want to test it only from the SearchResults list,
        // please do not copy this scenario to other SiteSnippet tests.
        scenarios
            .runAddToFavorites()
    }
}

// MARK: - Favorites

extension VillageSnippetTests {
    func testActionsInFavorites() {
        FavoritesAPIStubConfigurator.setupListing(using: self.dynamicStubs)
        SnippetsListAPIStubConfigurator.setupVillagesList(using: self.dynamicStubs, stubKind: .noPhoto)

        self.relaunchApp(with: .favoritesTests)

        let list = FavoritesListSteps()
        let snippet = list
            .screenIsPresented()
            .tapOnVillagesTag()
            .withVillageList()
            .cell(withIndex: 0)
            .isPresented()

        let scenarios = VillageSnippetScenarios(
            snippet: snippet,
            dynamicStubs: self.dynamicStubs
        )
        Self.performCommonSteps(using: scenarios)
        scenarios
            .runRemoveFromFavorites(whenInFavoritesList: list)
    }
}

// MARK: - Saved Search

extension VillageSnippetTests {
    func testActionsInSavedSearch() {
        SavedSearchAPIStubConfigurator.setupListingWithModifiedVisitTime(using: self.dynamicStubs)
        SavedSearchAPIStubConfigurator.setupRequiredFeature(using: self.dynamicStubs)
        SnippetsListAPIStubConfigurator.setupVillagesList(using: self.dynamicStubs, stubKind: .noPhoto)

        self.relaunchApp(with: .savedSearchTests)

        let list = self.obtainSavedSearchList()
        let snippet = Self.obtainSnippet(from: list)
        let scenarios = VillageSnippetScenarios(
            snippet: snippet,
            dynamicStubs: self.dynamicStubs
        )
        Self.performCommonSteps(using: scenarios)
    }

    private func obtainSavedSearchList() -> VillageListSteps {
        SavedSearchesListSteps()
            .screenIsPresented()
            .tapOnFirstRow()

        let list = SavedSearchResultListSteps()
            .screenIsPresented()
            .withVillageList()
        return list
    }
}

// MARK: - Call History

extension VillageSnippetTests {
    func testActionsInCallHistory() {
        SnippetsListAPIStubConfigurator.setupVillagesList(using: self.dynamicStubs, stubKind: .noPhoto)

        self.relaunchApp(with: .snippetsTests)

        // We have to add an item to Call History - there's currently no way to start an app with predefined item in DB.
        // If you remove this - don't forget to enable `testAddToCallHistory`.
        let mainList = Self.obtainMainList()
        let mainListSnippet = Self.obtainSnippet(from: mainList)
        let mainListScenarios = VillageSnippetScenarios(
            snippet: mainListSnippet,
            dynamicStubs: self.dynamicStubs
        )
        mainListScenarios
            .runAddToCallHistory()

        let callHistory = Self.obtainCallHistoryList(skipSectionOpening: true)
        let callHistorySnippet = Self.obtainSnippet(from: callHistory)
        let callHistoryScenarios = VillageSnippetScenarios(
            snippet: callHistorySnippet,
            dynamicStubs: self.dynamicStubs
        )
        Self.performCommonSteps(using: callHistoryScenarios)
    }

    private static func obtainCallHistoryList(skipSectionOpening: Bool) -> VillageListSteps {
        if !skipSectionOpening {
            RootNavigationSteps()
                .tabBarTapOnComunicationItem()

            CommunicationSteps()
                .isScreenPresented()
                .tapOnCallHistorySegment()
        }

        let list = CallHistorySteps()
            .isScreenPresented()
            .withVillageList()
        return list
    }
}

// MARK: - Developer Villages

extension VillageSnippetTests {
    func testActionsInDeveloperVillages() {
        SnippetsListAPIStubConfigurator.setupVillagesList(using: self.dynamicStubs, stubKind: .noPhoto)
        VillageCardAPIStubConfiguration.setupVillageCardBilling(using: self.dynamicStubs)

        self.relaunchApp(with: .cardTests)

        let mainList = Self.obtainMainList()
        let mainListSnippet = Self.obtainSnippet(from: mainList)

        // Load developer sites
        SnippetsListAPIStubConfigurator.setupVillagesList(using: self.dynamicStubs, stubKind: .noPhoto)

        mainListSnippet
            .tap()

        let snippet = Self.obtainVillageCardVillageSnippet()
        let scenarios = VillageSnippetScenarios(
            snippet: snippet,
            dynamicStubs: self.dynamicStubs
        )
        Self.performCommonSteps(using: scenarios)
    }

    private static func obtainVillageCardVillageSnippet() -> VillageSnippetSteps {
        let card = VillageCardSteps()
        card
            .isScreenPresented()
            .scrollToDeveloperVillages()

        let snippet = card.villageSnippet()
        snippet.isPresented()
        return snippet
    }
}

// MARK: - Common

extension VillageSnippetTests {
    private static func performCommonSteps(using scenarios: VillageSnippetScenarios) {
        scenarios
            .runBillingOnCall()
            .runCallWithTwoPhoneNumbers()
            .runCallWithError()
    }

    static func obtainMainList() -> VillageListSteps {
        let list = SearchResultsListSteps()
        let siteList = list
            .isScreenPresented()
            .withVillageList()
        return siteList
    }

    static func obtainSnippet(from list: VillageListSteps) -> VillageSnippetSteps {
        return list.cell(withIndex: 0)
    }
}
