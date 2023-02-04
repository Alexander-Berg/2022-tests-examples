//
//  OfferSnippetTests+SearchResults.swift
//  UI Tests
//
//  Created by Pavel Zhuravlev on 24.03.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest

final class OfferSnippetTests: BaseTestCase {}

extension OfferSnippetTests {
    func testActionsInSearchResults() {
        SnippetsListAPIStubConfigurator.setupOffersList(using: self.dynamicStubs, stubKind: .common(.singleOffer))
        self.relaunchApp(with: .snippetsTests)

        let list = Self.obtainMainList()
        let snippet = Self.obtainSnippet(from: list)

        let scenarios = OfferSnippetScenarios(
            snippet: snippet,
            dynamicStubs: self.dynamicStubs
        )
        scenarios
            .runBillingOnCall()
            .runCallWithTwoPhoneNumbers()
            .runCallWithError()
            .runAddNote()
            .runEditNote()
            .runAbuse()

        // This sometimes leads to an appearance of the system popup which covers some of our UI and prevents touches,
        // so this check is better to be the last one unless we figure out how to handle that popup.
        // After this check we can perform only checks which don't require any gestures on the top of the screen.
        scenarios
            .runSharing()

            // Pay attention - it hides the single offer snippet, so this must be the last check
        scenarios
            .runHideOffer(using: list)
    }
    
    // Temporarily disabled - the same scenario is included into `testActionsInCallHistory`
    func disabled_testAddToCallHistory() {
        SnippetsListAPIStubConfigurator.setupOffersList(using: self.dynamicStubs, stubKind: .common(.singleOffer))

        self.relaunchApp(with: .snippetsTests)

        let list = Self.obtainMainList()
        let snippet = Self.obtainSnippet(from: list)

        let scenarios = OfferSnippetScenarios(
            snippet: snippet,
            dynamicStubs: self.dynamicStubs
        )
        scenarios
            .runAddToCallHistory()
    }

    func testAddToFavorites() {
        SnippetsListAPIStubConfigurator.setupOffersList(using: self.dynamicStubs, stubKind: .common(.singleOffer))

        self.relaunchApp(with: .snippetsTests)

        let list = Self.obtainMainList()
        let snippet = Self.obtainSnippet(from: list)

        let scenarios = OfferSnippetScenarios(
            snippet: snippet,
            dynamicStubs: self.dynamicStubs
        )
        scenarios
            .runAddToFavorites()
    }
}

// MARK: - Main List Additionals

// To check these cases we need a dedicated Stub file,
// but we cannot just reload a list to load a new Stub file, so this requires separate test methods.
extension OfferSnippetTests {
    func testAdditionalActionsWithoutSharing() {
        SnippetsListAPIStubConfigurator.setupOffersList(using: self.dynamicStubs, stubKind: .additionalActions(.noShareURL))
        self.relaunchApp(with: .snippetsTests)

        let list = Self.obtainMainList()
        let snippet = Self.obtainSnippet(from: list)

        let scenarios = OfferSnippetScenarios(
            snippet: snippet,
            dynamicStubs: self.dynamicStubs
        )
        scenarios
            .runNoSharing()
    }

    // https://st.yandex-team.ru/VSAPPS-8218#602d0fd4f38a0d7a84b68f79
    func testPhotoFallbackWhenNoLargeImages() {
        SnippetsListAPIStubConfigurator.setupOffersList(using: self.dynamicStubs, stubKind: .noLargeImages)
        self.relaunchApp(with: .snippetsTests)

        let list = Self.obtainMainList()
        Self.obtainSnippet(from: list)
            .compareWithScreenshot(identifier: "photoFallback")
    }

    // FIXME: Move the screenshot check to Unit test (https://st.yandex-team.ru/VSAPPS-10667)
    func testNoCallButtonIfOfferOutdated() {
        SnippetsListAPIStubConfigurator.setupOffersList(using: self.dynamicStubs, stubKind: .callButton(.hidden))
        self.relaunchApp(with: .snippetsTests)

        let list = Self.obtainMainList()
        Self.obtainSnippet(from: list)
            .compareWithScreenshot(identifier: "callButton.hidden.offer")
    }
}

// MARK: - Resale List

extension OfferSnippetTests {
    func testActionsInResaleList() {
        SiteCardAPIStubConfiguration.setupSiteSearchResultsListMoscow(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupSiteCardLevelAmurskaya(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupOfferStat(using: self.dynamicStubs, site: .levelAmurskaya)

        self.relaunchApp(with: .cardTests)

        let resaleList = self.obtainResaleOffersList()
        let resaleSnippet = Self.obtainSnippet(from: resaleList)
        let resaleScenarios = OfferSnippetScenarios(
            snippet: resaleSnippet,
            dynamicStubs: self.dynamicStubs
        )
        resaleScenarios
            .runBillingOnCall()

        resaleScenarios
            .runCallWithTwoPhoneNumbers()
            .runCallWithError()
            .runAbuse()
            .runAddNote()
            .runEditNote()

        // This sometimes leads to an appearance of the system popup which covers some of our UI and prevents touches,
        // so this check is better to be the last one unless we figure out how to handle that popup.
        // After this check we can perform only checks which don't require any gestures on the top of the screen.
        resaleScenarios
            .runSharing()

        // Pay attention - it hides the single offer snippet, so this must be the last check
        resaleScenarios
            .runHideOffer(using: resaleList)
    }

    private func obtainResaleOffersList() -> OfferListSteps {
        let list = SearchResultsListSteps()
        list
            .isScreenPresented()
            .withSiteList()
            .isListNonEmpty()
            .cell(withIndex: 0)
            .isPresented()
            .tap()

        // Important to setup this stub just after the Site List is loaded
        SnippetsListAPIStubConfigurator.setupOffersList(using: self.dynamicStubs, stubKind: .common(.singleOffer))

        let siteCardSteps = SiteCardSteps()
        let resaleOffers = siteCardSteps
            .isScreenPresented()
            .scrollToResaleOffers()
            .tapOnResaleOffers()

        return resaleOffers
    }
}

// MARK: - Saved Search

extension OfferSnippetTests {
    func testActionsInSavedSearch() {
        SavedSearchAPIStubConfigurator.setupListingWithModifiedVisitTime(using: self.dynamicStubs)
        SavedSearchAPIStubConfigurator.setupRequiredFeature(using: self.dynamicStubs)
        SavedSearchAPIStubConfigurator.setupOffersListForNewItems(using: self.dynamicStubs)

        self.relaunchApp(with: .savedSearchTests)

        let list = self.obtainSavedSearchList()
        let snippet = Self.obtainSnippet(from: list)
        let scenarios = OfferSnippetScenarios(
            snippet: snippet,
            dynamicStubs: self.dynamicStubs
        )
        scenarios
            .runBillingOnCall()
            .runCallWithTwoPhoneNumbers()
            .runCallWithError()
            .runAbuse()
            .runAddNote()
            .runEditNote()

        // This sometimes leads to an appearance of the system popup which covers some of our UI and prevents touches,
        // so this check is better to be the last one unless we figure out how to handle that popup.
        // After this check we can perform only checks which don't require any gestures on the top of the screen.
        scenarios
            .runSharing()

        // Pay attention - it hides the single offer snippet, so this must be the last check

        // It's currently really hard to implement this check for a Saved Search –
        // – it reloads a list and get its content from a stub right after the hidding
        // scenarios
        //     .runHideOffer(using: list)
    }

    private func obtainSavedSearchList() -> OfferListSteps {
        SavedSearchesListSteps()
            .screenIsPresented()
            .tapOnFirstRow()

        let list = SavedSearchResultListSteps().screenIsPresented()
        return list.withOfferList()
    }
}

// MARK: - Favorites

extension OfferSnippetTests {
    func testActionsInFavorites() {
        FavoritesAPIStubConfigurator.setupListing(using: self.dynamicStubs)
        SnippetsListAPIStubConfigurator.setupOffersList(using: self.dynamicStubs, stubKind: .common(.singleOffer))
        OfferCardAPIStubConfiguration.setupOfferCardWithoutUsernote(using: self.dynamicStubs)
        SavedSearchAPIStubConfigurator.setupRequiredFeature(using: self.dynamicStubs)

        self.relaunchApp(with: .favoritesTests)

        let list = FavoritesListSteps()
        let snippet = list
            .screenIsPresented()
            .withOfferList()
            .cell(withIndex: 0)
            .isPresented()

        let scenarios = OfferSnippetScenarios(
            snippet: snippet,
            dynamicStubs: self.dynamicStubs
        )
        scenarios
            .runBillingOnCall()
            .runCallWithTwoPhoneNumbers()
            .runCallWithError()
            .runAbuse()
            .runAddNote()
            .runEditNote()
            .runNoHideOfferFeature()

        // This sometimes leads to an appearance of the system popup which covers some of our UI and prevents touches,
        // so this check is better to be the last one unless we figure out how to handle that popup.
        // After this check we can perform only checks which don't require any gestures on the top of the screen.
        scenarios
            .runSharing()
    }
}

// MARK: - Call History

extension OfferSnippetTests {
    func testActionsInCallHistory() {
        SnippetsListAPIStubConfigurator.setupOffersList(using: self.dynamicStubs, stubKind: .common(.singleOffer))
        // To load an item of Call History
        OfferCardAPIStubConfiguration.setupOfferCard(using: self.dynamicStubs)

        self.relaunchApp(with: .snippetsTests)

        // We have to add an item to Call History - there's currently no way to start an app with predefined item in DB.
        // If you remove this - don't forget to enable `testAddToCallHistory`.
        let mainList = Self.obtainMainList()
        let mainListSnippet = Self.obtainSnippet(from: mainList)
        let mainListScenarios = OfferSnippetScenarios(
            snippet: mainListSnippet,
            dynamicStubs: self.dynamicStubs
        )
        mainListScenarios
            .runAddToCallHistory()

        let callHistory = Self.obtainCallHistoryList(skipSectionOpening: true)
        let callHistorySnippet = Self.obtainSnippet(from: callHistory)
        let callHistoryScenarios = OfferSnippetScenarios(
            snippet: callHistorySnippet,
            dynamicStubs: self.dynamicStubs
        )
        callHistoryScenarios
            .runBillingOnCall()
            .runCallWithTwoPhoneNumbers()
            .runCallWithError()
            .runAbuse()
            .runNoHideOfferFeature()

        // To load an item of Call History with added note
        OfferCardAPIStubConfiguration.setupOfferCardWithUsernote(using: self.dynamicStubs)
        callHistoryScenarios
            .runAddNote()

        // To load an item of Call History with edited note
        OfferCardAPIStubConfiguration.setupOfferCardWithEditedUserNote(using: self.dynamicStubs)
        callHistoryScenarios
            .runEditNote()

        // This sometimes leads to an appearance of the system popup which covers some of our UI and prevents touches,
        // so this check is better to be the last one unless we figure out how to handle that popup.
        // After this check we can perform only checks which don't require any gestures on the top of the screen.
        callHistoryScenarios
            .runSharing(offerID: "5205620753106733305")
    }

    private static func obtainCallHistoryList(skipSectionOpening: Bool) -> OfferListSteps {
        if !skipSectionOpening {
            RootNavigationSteps()
                .tabBarTapOnComunicationItem()

            CommunicationSteps()
                .isScreenPresented()
                .tapOnCallHistorySegment()
        }

        let list = CallHistorySteps().withOfferList()
        list
            .isScreenPresented()

        return list
    }
}

// MARK: - Similar Offers

extension OfferSnippetTests {
    func testActionsInSimilarOffers() {
        SnippetsListAPIStubConfigurator.setupOffersList(using: self.dynamicStubs, stubKind: .common(.singleOffer))
        OfferCardAPIStubConfiguration.setupOfferCard(using: self.dynamicStubs)
        OfferCardAPIStubConfiguration.setupSimilarOffer(using: self.dynamicStubs)

        let offerID = "9201692201512167138"
        self.relaunchApp(with: .snippetsTests)

        let card = Self.obtainOfferCard()
        let snippet = Self.obtainSimilarOffersSnippet(using: card)

        let scenarios = OfferSnippetScenarios(
            snippet: snippet,
            dynamicStubs: self.dynamicStubs
        )
        scenarios
            .runBillingOnCall(offerID: offerID)
            .runCallWithTwoPhoneNumbers(offerID: offerID)
            .runCallWithError(offerID: offerID)
            .runAddNote(offerID: offerID)
            .runEditNote()
            .runAbuse()

        // This sometimes leads to an appearance of the system popup which covers some of our UI and prevents touches,
        // so this check is better to be the last one unless we figure out how to handle that popup.
        // After this check we can perform only checks which don't require any gestures on the top of the screen.
        scenarios
            .runSharing(offerID: offerID)

        // Pay attention - it hides the single offer snippet, so this must be the last check
        scenarios
            .runHideSimilarOffer(using: card, offerID: offerID)
    }

    private static func obtainOfferCard() -> OfferCardSteps {
        SearchResultsListSteps()
            .isScreenPresented()
            .withOfferList()
            .cell(withIndex: 0)
            .tap()

        let card = OfferCardSteps()
        card
            .isOfferCardPresented()
            .scrollToSimilarOffers()

        return card
    }

    private static func obtainSimilarOffersSnippet(using card: OfferCardSteps) -> OfferSnippetSteps {
        let snippet = card.similarOffer(withIndex: 0)
        snippet
            .isPresented()
        return snippet
    }
}

// MARK: - Common

extension OfferSnippetTests {
    static func obtainMainList() -> OfferListSteps {
        let list = SearchResultsListSteps()
        let offerList = list
            .isScreenPresented()
            .withOfferList()
        return offerList
    }

    static func obtainSnippet(from list: OfferListSteps) -> OfferSnippetSteps {
        return list.cell(withIndex: 0)
    }
}
