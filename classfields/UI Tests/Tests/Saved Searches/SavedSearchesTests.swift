//
//  SavedSearchesTests.swift
//  UI Tests
//
//  Created by Arkady Smirnov on 4/27/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YREAppConfig

final class SavedSearchesTests: BaseTestCase {
    func testSaveSearchFromMap() {
        let rootNavigationSteps = RootNavigationSteps()
        let mapSteps = SearchResultsMapSteps()

        SavedSearchAPIStubConfigurator.setupListing(using: self.dynamicStubs)

        self.relaunchApp(with: .savedSearchTests)

        rootNavigationSteps.tabBarTapOnSearchItem()

        mapSteps.tapOnSubscribeButton()

        self.runSaveSearchFlow()
    }

    func testSaveSearchFromList() {
        let rootNavigationSteps = RootNavigationSteps()
        let mapSteps = SearchResultsMapSteps()
        let searchResultsList = SearchResultsListSteps()

        SavedSearchAPIStubConfigurator.setupListing(using: self.dynamicStubs)

        self.relaunchApp(with: .savedSearchTests)

        rootNavigationSteps.tabBarTapOnSearchItem()

        mapSteps
            .tapOnSwitchToListButton()
        searchResultsList
            .isScreenPresented()
            .tapOnSubscribeButton()

        self.runSaveSearchFlow()
    }

    func testRemoveSavedSearch() {
        let deleteExpectation = XCTestExpectation()
        deleteExpectation.expectedFulfillmentCount = 1
        let savedSearchesListSteps = SavedSearchesListSteps()

        SavedSearchAPIStubConfigurator.setupListing(using: self.dynamicStubs)
        SavedSearchAPIStubConfigurator.setupRequiredFeature(using: self.dynamicStubs)
        self.relaunchApp(with: .savedSearchTests)

        savedSearchesListSteps
            .screenIsPresented()
            .swipeLeftFirstRow()

        // Before tap Delete button we should:
        // - setup expecation of call PUT method for saving search
        // - replace responce of listing to empty result
        SavedSearchAPIStubConfigurator.setupDelete(using: self.dynamicStubs, with: deleteExpectation)
        SavedSearchAPIStubConfigurator.setupEmptyListing(using: self.dynamicStubs)
        savedSearchesListSteps
            .tapOnDelete()
            .confirmToDelete()
            .emptyViewIsPresented()

        self.wait(for: [deleteExpectation], timeout: Constants.timeout)
    }


    func testOpenSavedSearchResults() {
        let visitExpectation = XCTestExpectation()
        visitExpectation.expectedFulfillmentCount = 1
        let savedSearchesListSteps = SavedSearchesListSteps()
        let savedSearchResultListSteps = SavedSearchResultListSteps()
        let saveSearchPopupSteps = SaveSearchPopupSteps()
        let rootNavigationSteps = RootNavigationSteps()

        SavedSearchAPIStubConfigurator.setupListingWithModifiedVisitTime(using: self.dynamicStubs)
        SavedSearchAPIStubConfigurator.setupRequiredFeature(using: self.dynamicStubs)
        SavedSearchAPIStubConfigurator.setupSearchResultList(using: self.dynamicStubs)

        // Setup expecation of call visit search method
        SavedSearchAPIStubConfigurator.setupSearchVisit(using: self.dynamicStubs, with: visitExpectation)

        self.relaunchApp(with: .savedSearchTests)

        savedSearchesListSteps
            .screenIsPresented()

        rootNavigationSteps.tabBarFavoriteItemHasBadge()

        savedSearchesListSteps
            .segmentHasCount()
            .firstRowHasCounter()
            .tapOnFirstRow()

        savedSearchResultListSteps
            .screenIsPresented()
            .newOffersPresented()

        self.wait(for: [visitExpectation], timeout: Constants.timeout)

        savedSearchResultListSteps
            .tapOnSearchParamsButton()

        saveSearchPopupSteps
            .screenIsPresented()
            .ensureTitleViewHasText()
            .tapCloseButton()

        savedSearchResultListSteps
            .screenIsPresented()
            .tapOnCloseButton()

        savedSearchesListSteps
            .firstRowCounterHidden()
            .screenIsPresented()
    }

    // TODO: @arkadysmirnov Disabled for Snapshots test plan. Failed on agents.
    func disabled_testOpenSearchParams() {
        let savedSearchesListSteps = SavedSearchesListSteps()
        let savedSearchResultListSteps = SavedSearchResultListSteps()

        SavedSearchAPIStubConfigurator.setupRequiredFeature(using: self.dynamicStubs)
        SavedSearchAPIStubConfigurator.setupSearchResultList(using: self.dynamicStubs)

        self.relaunchApp(with: .savedSearchTests)

        SavedSearchAPIStubConfigurator.SingleItemList.allCases.forEach { type in
            SavedSearchAPIStubConfigurator.setupSingleItemListing(using: self.dynamicStubs, type: type)

            savedSearchesListSteps
                .screenIsPresented()
                .performPullToRefresh()
                .pullToRefreshBecomeHidden()
                .tapOnFirstRow()

            savedSearchResultListSteps
                .screenIsPresented()
                .tapOnSearchParamsButton()

            self.runParamsScreenSteps(for: type)

            savedSearchResultListSteps
                .screenIsPresented()
                .tapOnCloseButton()
        }
    }

    func testSyncSavedSearchesOnPullToRefresh() {
        let listingExpectation = XCTestExpectation()
        listingExpectation.expectedFulfillmentCount = 1
        let savedSearchesListSteps = SavedSearchesListSteps()
        SavedSearchAPIStubConfigurator.setupEmptyListing(using: self.dynamicStubs)
        SavedSearchAPIStubConfigurator.setupRequiredFeature(using: self.dynamicStubs)

        self.relaunchApp(with: .savedSearchTests)

        savedSearchesListSteps
            .screenIsPresented()
            .emptyViewIsPresented()
            .emptyViewActionIsHidden()
            .compareEmptyViewWithSnapshot(identifier: "savedSearches_authorized_emptyView")

        // change response of listing method
        SavedSearchAPIStubConfigurator.setupListing(using: self.dynamicStubs)
        savedSearchesListSteps
            .performPullToRefresh()
            .emptyViewIsHidden()
            .alertViewIsHidden()
            
        // Setup expecation of listing GET method that indicates start of sync process
        SavedSearchAPIStubConfigurator.setupListing(using: self.dynamicStubs, with: listingExpectation)

        savedSearchesListSteps
            .performPullToRefresh()
            .pullToRefreshBecomeHidden()

        self.wait(for: [listingExpectation], timeout: Constants.timeout)
    }

    func testSavedSearchAppUpdateStubScreen() {
        let savedSearchesListSteps = SavedSearchesListSteps()
        SavedSearchAPIStubConfigurator.setupListing(using: self.dynamicStubs)
        SavedSearchAPIStubConfigurator.setupRequiredFeatureWithUnknownFeature(using: self.dynamicStubs)

        self.relaunchApp(with: .savedSearchTests)

        savedSearchesListSteps
            .screenIsPresented()
            .appUpdateViewIsPresented()
    }

    func testSavedSearchErrorScreen() {
        let savedSearchesListSteps = SavedSearchesListSteps()
        SavedSearchAPIStubConfigurator.setupListing(using: self.dynamicStubs)
        SavedSearchAPIStubConfigurator.setupRequiredFeatureWithError(using: self.dynamicStubs)

        self.relaunchApp(with: .savedSearchTests)

        savedSearchesListSteps
            .screenIsPresented()
            .errorViewIsPresented()
    }

    func testEmptySavedSearchAuthorization() {
        SavedSearchAPIStubConfigurator.setupEmptyListing(using: self.dynamicStubs)
        SavedSearchAPIStubConfigurator.setupRequiredFeature(using: self.dynamicStubs)

        self.relaunchApp(with: .unauthorizedSavedSearchTests)

        let savedSearchesListSteps = SavedSearchesListSteps()

        savedSearchesListSteps
            .screenIsPresented()
            .emptyViewIsPresented()
            .emptyViewActionIsExist()
            .tapOnEmptyViewAction()

        savedSearchesListSteps
            .emptyViewIsPresented()
            .emptyViewActionIsHidden()
            .alertViewIsHidden()
    }

    func testSavedSearchAuthorization_emptyToFilled() {
        let listingExpectation = XCTestExpectation()
        listingExpectation.expectedFulfillmentCount = 1

        SavedSearchAPIStubConfigurator.setupEmptyListing(using: self.dynamicStubs)
        SavedSearchAPIStubConfigurator.setupRequiredFeature(using: self.dynamicStubs)

        self.relaunchApp(with: .unauthorizedSavedSearchTests)

        let savedSearchesListSteps = SavedSearchesListSteps()

        savedSearchesListSteps
            .screenIsPresented()
            .emptyViewIsPresented()
            .emptyViewActionIsExist()
            .compareEmptyViewWithSnapshot(identifier: "savedSearches_unauthorized_emptyView")

        SavedSearchAPIStubConfigurator.setupListing(using: self.dynamicStubs, with: listingExpectation)

        savedSearchesListSteps
            .tapOnEmptyViewAction()
            .emptyViewIsHidden()
            .alertViewIsHidden()

        self.wait(for: [listingExpectation], timeout: Constants.timeout)
    }

    func testFilledSavedSearchAuthorization() {
        SavedSearchAPIStubConfigurator.setupRequiredFeature(using: self.dynamicStubs)
        SavedSearchAPIStubConfigurator.setupListing(using: self.dynamicStubs)

        self.relaunchApp(with: .unauthorizedSavedSearchTests)

        let savedSearchesListSteps = SavedSearchesListSteps()

        savedSearchesListSteps
            .screenIsPresented()
            .emptyViewIsHidden()
            .alertViewIsExist()
            .compareAlertViewWithSnapshot(identifier: "savedSearches_unauthorized_alertView")
            .tapOnAlertAction()

        savedSearchesListSteps
            .emptyViewIsHidden()
            .alertViewIsHidden()
    }

    func testSavedSearchesWidgetPromoNotShownWithEmptyList() throws {
        let config = ExternalAppConfiguration.savedSearchTests
        config.savedSearchesWidgetPromoWasShown = false

        SavedSearchAPIStubConfigurator.setupEmptyListing(using: self.dynamicStubs)
        SavedSearchAPIStubConfigurator.setupRequiredFeature(using: self.dynamicStubs)

        self.relaunchApp(with: config)

        SavedSearchesListSteps()
            .screenIsPresented()

        SavedSearchesWidgetPromoSteps()
            .widgetPromoIsNotPresented()
    }

    func testSavedSearchesWidgetPromoShown() throws {
        let config = ExternalAppConfiguration.savedSearchTests
        config.savedSearchesWidgetPromoWasShown = false

        SavedSearchAPIStubConfigurator.setupListing(using: self.dynamicStubs)
        SavedSearchAPIStubConfigurator.setupRequiredFeature(using: self.dynamicStubs)

        self.relaunchApp(with: config)

        SavedSearchesListSteps()
            .screenIsPresented()

        SavedSearchesWidgetPromoSteps()
            .widgetPromoIsPresented()
            .tapActionButton()
            .widgetPromoIsNotPresented()
    }

    func testSavedSearchesWidgetPromoNotShownTwice() throws {
        SavedSearchAPIStubConfigurator.setupRequiredFeature(using: self.dynamicStubs)
        SavedSearchAPIStubConfigurator.setupListing(using: self.dynamicStubs)

        self.relaunchApp(with: .savedSearchTests)

        SavedSearchesListSteps()
            .screenIsPresented()

        SavedSearchesWidgetPromoSteps()
            .widgetPromoIsNotPresented()
    }

    func testSavedSearchesWidgetPromoCloses() throws {
        let config = ExternalAppConfiguration.savedSearchTests
        config.savedSearchesWidgetPromoWasShown = false

        SavedSearchAPIStubConfigurator.setupRequiredFeature(using: self.dynamicStubs)
        SavedSearchAPIStubConfigurator.setupListing(using: self.dynamicStubs)

        self.relaunchApp(with: config)

        SavedSearchesListSteps()
            .screenIsPresented()

        SavedSearchesWidgetPromoSteps()
            .widgetPromoIsPresented()
            .tapCloseButton()
            .widgetPromoIsNotPresented()
    }

    func testWidgetPromoBannerShown() throws {
        let config = ExternalAppConfiguration.savedSearchTests
        config.savedSearchesWidgetPromoBannerWasHidden = false

        SavedSearchAPIStubConfigurator.setupListing(using: self.dynamicStubs)
        SavedSearchAPIStubConfigurator.setupRequiredFeature(using: self.dynamicStubs)

        self.relaunchApp(with: config)

        SavedSearchesListSteps()
            .screenIsPresented()
            .widgetPromoBannerIsShown()
            .compareWidgetPromoBannerWithSnapshot(identifier: "SavedSearches_widgetPromoBanner_view")
    }

    func testWidgetPromoBannerNotShownIfWasHiddenByUser() throws {
        SavedSearchAPIStubConfigurator.setupListing(using: self.dynamicStubs)
        SavedSearchAPIStubConfigurator.setupRequiredFeature(using: self.dynamicStubs)

        self.relaunchApp(with: .savedSearchTests)

        SavedSearchesListSteps()
            .screenIsPresented()
            .widgetPromoBannerIsHidden()
    }

    func testWidgetPromoBannerCanBeHidden() throws {
        let config = ExternalAppConfiguration.savedSearchTests
        config.savedSearchesWidgetPromoBannerWasHidden = false

        SavedSearchAPIStubConfigurator.setupListing(using: self.dynamicStubs)
        SavedSearchAPIStubConfigurator.setupRequiredFeature(using: self.dynamicStubs)

        self.relaunchApp(with: config)

        SavedSearchesListSteps()
            .screenIsPresented()
            .widgetPromoBannerIsShown()
            .tapWidgetPromoBannerCloseButton()
            .widgetPromoBannerIsHidden()
    }

    func testWidgetPromoWizardFlow() throws {
        let config = ExternalAppConfiguration.savedSearchTests
        config.savedSearchesWidgetPromoBannerWasHidden = false

        SavedSearchAPIStubConfigurator.setupListing(using: self.dynamicStubs)
        SavedSearchAPIStubConfigurator.setupRequiredFeature(using: self.dynamicStubs)

        self.relaunchApp(with: config)

        SavedSearchesListSteps()
            .screenIsPresented()
            .widgetPromoBannerIsShown()
            .tapWidgetPromoBannerActionButton()

        SavedSearchesWidgetWizardPromoSteps()
            .wizardPromoIsShown()
            .compareWizardStepWithSnapshot(identifier: "savedSearches_widgetWizardPromo_firstStep")
            .tapWizardActionButton1()
            .compareWizardStepWithSnapshot(identifier: "savedSearches_widgetWizardPromo_secondStep")
            .tapWizardActionButton2()
            .compareWizardStepWithSnapshot(identifier: "savedSearches_widgetWizardPromo_thirdStep")
            .tapWizardActionButton3()
            .wizardPromoClosed()
    }

    // MARK: - Private

    private func runSaveSearchFlow() {
        let syncStartedExpectation = XCTestExpectation()
        syncStartedExpectation.expectedFulfillmentCount = 1

        let putExpectation = XCTestExpectation()
        putExpectation.expectedFulfillmentCount = 1

        let saveSearchPopupSteps = SaveSearchPopupSteps()

        saveSearchPopupSteps
            .screenIsPresented()
            .ensureTitleViewHasText()
            .fillForm(title: "test search")
            .closeKeyboard()

        // Before tap Submit button we should:
        // - setup expecation of call PUT method for saving search
        // - setup expecation of listing GET method that indicates start of sync process
        SavedSearchAPIStubConfigurator.setupPut(using: self.dynamicStubs, with: putExpectation)
        SavedSearchAPIStubConfigurator.setupListing(using: self.dynamicStubs, with: syncStartedExpectation)
        saveSearchPopupSteps
            .tapSubmitButton()
        self.wait(for: [putExpectation, syncStartedExpectation], timeout: Constants.timeout)

        saveSearchPopupSteps
            .finalScreenIsPresented()
            .tapPostponeButtonIfExists()
    }

    private func runParamsScreenSteps(for type: SavedSearchAPIStubConfigurator.SingleItemList) {
        let saveSearchPopupSteps = SaveSearchPopupSteps()
        let savedSearchMapSteps = SavedSearchMapSteps()

        saveSearchPopupSteps
            .screenIsPresented()

        switch type {
            case .byRegion:
                saveSearchPopupSteps.compareWithScreenshot(identifier: "SavedSearch.ParamsScreen.byRegion")
            case .byViewPort:
                saveSearchPopupSteps
                    .compareWithScreenshot(identifier: "SavedSearch.ParamsScreen.byViewPort")
                    .tapOnShowSearchArea()
                // The map doesn't have time to draw without delay, and shows wrong snapshot
                Thread.sleep(forTimeInterval: 2)
                savedSearchMapSteps
                    .screenIsPresented()
                    .compareWithScreenshot(identifier: "SavedSearch.ParamsScreen.byViewPort.map")
                    .tapOnCloseButton()
            case .byGeointent:
                saveSearchPopupSteps
                    .compareWithScreenshot(identifier: "SavedSearch.ParamsScreen.byGeointent")
            case .byPolygons:
                saveSearchPopupSteps
                    .compareWithScreenshot(identifier: "SavedSearch.ParamsScreen.byPolygons")
                    .tapOnShowDrawnAreas()
                savedSearchMapSteps
                    .screenIsPresented()
                    .compareWithScreenshot(identifier: "SavedSearch.ParamsScreen.byPolygons.map")
                    .tapOnCloseButton()
            case .byCommuteTime:
                saveSearchPopupSteps
                    .compareWithScreenshot(identifier: "SavedSearch.ParamsScreen.byCommuteTime")
            case .byComposite:
                saveSearchPopupSteps
                    .compareWithScreenshot(identifier: "SavedSearch.ParamsScreen.byComposite")
        }

        saveSearchPopupSteps
            .tapCloseButton()
    }
}
