//
//  SiteSnippetTests.swift
//  UI Tests
//
//  Created by Erik Burygin on 02.11.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

final class SiteSnippetTests: BaseTestCase { }

// MARK: - Main List

extension SiteSnippetTests {
    func testActionsInSearchResults() {
        SnippetsListAPIStubConfigurator.setupSitesList(using: self.dynamicStubs, stubKind: .common)
        self.relaunchApp(with: .snippetsTests)

        let list = Self.obtainMainList()
        let snippet = Self.obtainSnippet(from: list)

        let scenarios = SiteSnippetScenarios(
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

extension SiteSnippetTests {
    func testActionsInFavorites() {
        FavoritesAPIStubConfigurator.setupListing(using: self.dynamicStubs)
        SnippetsListAPIStubConfigurator.setupSitesList(using: self.dynamicStubs, stubKind: .common)
        SiteCardAPIStubConfiguration.setupSiteCard(using: self.dynamicStubs, stubKind: .billing)

        self.relaunchApp(with: .favoritesTests)

        let list = FavoritesListSteps()
        let snippet = list
            .screenIsPresented()
            .tapOnSitesTag()
            .withSiteList()
            .cell(withIndex: 0)
            .isPresented()

        let scenarios = SiteSnippetScenarios(
            snippet: snippet,
            dynamicStubs: self.dynamicStubs
        )
        Self.performCommonSteps(using: scenarios)
        scenarios
            .runRemoveFromFavorites(whenInFavoritesList: list)
    }
}

// MARK: - Saved Search

extension SiteSnippetTests {
    func testActionsInSavedSearch() {
        SavedSearchAPIStubConfigurator.setupListingWithModifiedVisitTime(using: self.dynamicStubs)
        SavedSearchAPIStubConfigurator.setupRequiredFeature(using: self.dynamicStubs)
        SnippetsListAPIStubConfigurator.setupSitesList(using: self.dynamicStubs, stubKind: .common)

        self.relaunchApp(with: .savedSearchTests)

        let list = self.obtainSavedSearchList()
        let snippet = Self.obtainSnippet(from: list)
        let scenarios = SiteSnippetScenarios(
            snippet: snippet,
            dynamicStubs: self.dynamicStubs
        )
        Self.performCommonSteps(using: scenarios)
    }

    private func obtainSavedSearchList() -> SiteListSteps {
        SavedSearchesListSteps()
            .screenIsPresented()
            .tapOnFirstRow()

        let list = SavedSearchResultListSteps()
            .screenIsPresented()
            .withSiteList()
        return list
    }
}

// MARK: - Call History

extension SiteSnippetTests {
    func testActionsInCallHistory() {
        SnippetsListAPIStubConfigurator.setupSitesList(using: self.dynamicStubs, stubKind: .common)

        self.relaunchApp(with: .snippetsTests)

        // We have to add an item to Call History - there's currently no way to start an app with predefined item in DB.
        // If you remove this - don't forget to enable `testAddToCallHistory`.
        let mainList = Self.obtainMainList()
        let mainListSnippet = Self.obtainSnippet(from: mainList)
        let mainListScenarios = SiteSnippetScenarios(
            snippet: mainListSnippet,
            dynamicStubs: self.dynamicStubs
        )
        mainListScenarios
            .runAddToCallHistory()

        let callHistory = Self.obtainCallHistoryList(skipSectionOpening: true)
        let callHistorySnippet = Self.obtainSnippet(from: callHistory)
        let callHistoryScenarios = SiteSnippetScenarios(
            snippet: callHistorySnippet,
            dynamicStubs: self.dynamicStubs
        )
        Self.performCommonSteps(using: callHistoryScenarios)
    }

    private static func obtainCallHistoryList(skipSectionOpening: Bool) -> SiteListSteps {
        if !skipSectionOpening {
            RootNavigationSteps()
                .tabBarTapOnComunicationItem()

            CommunicationSteps()
                .isScreenPresented()
                .tapOnCallHistorySegment()
        }

        let list = CallHistorySteps()
            .isScreenPresented()
            .withSiteList()
        return list
    }
}

// MARK: - Developer & Similar Sites

extension SiteSnippetTests {
    func testActionsInSimilarSites() {
        SnippetsListAPIStubConfigurator.setupSitesList(using: self.dynamicStubs, stubKind: .common)
        SiteCardAPIStubConfiguration.setupSiteCard(using: self.dynamicStubs, stubKind: .minimalInfo)

        self.relaunchApp(with: .cardTests)

        let mainList = Self.obtainMainList()
        let mainListSnippet = Self.obtainSnippet(from: mainList)

        // Load similar sites
        SiteCardAPIStubConfiguration.setupSimilarSitesList(using: self.dynamicStubs)
        // Do not load developer sites
        SnippetsListAPIStubConfigurator.unsetupSitesList(using: self.dynamicStubs)

        mainListSnippet
            .tap()

        let snippet = Self.obtainSiteCardSiteSnippet()
        let scenarios = SiteSnippetScenarios(
            snippet: snippet,
            dynamicStubs: self.dynamicStubs
        )
        Self.performCommonSteps(using: scenarios)
    }

    func testActionsInDeveloperSites() {
        SnippetsListAPIStubConfigurator.setupSitesList(using: self.dynamicStubs, stubKind: .common)
        SiteCardAPIStubConfiguration.setupSiteCard(using: self.dynamicStubs, stubKind: .minimalInfo)

        self.relaunchApp(with: .cardTests)

        let mainList = Self.obtainMainList()
        let mainListSnippet = Self.obtainSnippet(from: mainList)

        // Load developer sites
        SnippetsListAPIStubConfigurator.setupSitesList(using: self.dynamicStubs, stubKind: .common)

        mainListSnippet
            .tap()

        let snippet = Self.obtainSiteCardSiteSnippet()
        let scenarios = SiteSnippetScenarios(
            snippet: snippet,
            dynamicStubs: self.dynamicStubs
        )
        Self.performCommonSteps(using: scenarios)
    }

    private static func obtainSiteCardSiteSnippet() -> SiteSnippetSteps {
        let card = SiteCardSteps()
        card
            .isScreenPresented()
            .isLoadingIndicatorHidden()
            .scrollToSiteWrapperCell()

        let snippet = card.siteSnippet()
        snippet.isPresented()
        return snippet
    }
}

// MARK: - Common

extension SiteSnippetTests {
    private static func performCommonSteps(using scenarios: SiteSnippetScenarios) {
        scenarios
            .runBillingOnCall()
            .runCallWithTwoPhoneNumbers()
            .runCallWithError()
    }

    static func obtainMainList() -> SiteListSteps {
        let list = SearchResultsListSteps()
        let siteList = list
            .isScreenPresented()
            .withSiteList()
        return siteList
    }

    static func obtainSnippet(from list: SiteListSteps) -> SiteSnippetSteps {
        return list.cell(withIndex: 0)
    }
}
