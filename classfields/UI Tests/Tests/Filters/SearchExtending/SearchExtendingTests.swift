//
//  SearchExtendingTests.swift
//  UI Tests
//
//  Created by Timur Guliamov on 08.09.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import YREAppConfig

final class SearchExtendingTests: BaseTestCase {
    func testShowToastAndBottomSheet() {
        let configuration = ExternalAppConfiguration.filterUITests
        self.relaunchApp(with: configuration)

        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()
        let searchExtendingSteps = SearchExtendingSteps()
        
        self.configureStubs(countStub: .withExtendedNumber1, filtersStub: .priceAndViewPort)
        
        searchResultsSteps
            .openFilters()
        
        filtersSteps
            .isScreenPresented()
        
        searchExtendingSteps
            .isToastPresented()
            .tapOnToastButton()
            .isBottomSheetPresented()
            .isBottomSheetCellsCountEqual(count: 2)
            .tapOnBottomSheetCellSwitch(index: 0)
            .makeBottomSheetScreenshot()
            .tapOnBottomSheetApplyButton()
        
        filtersSteps
            .isScreenNotPresented()
    }
    
    func testChangeNumberOnToastAndCloseIfNeeded() {
        let configuration = ExternalAppConfiguration.filterUITests
        self.relaunchApp(with: configuration)

        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()
        let searchExtendingSteps = SearchExtendingSteps()
        
        self.configureStubs(countStub: .withExtendedNumber1, filtersStub: nil)

        searchResultsSteps
            .openFilters()
        
        filtersSteps
            .isScreenPresented()
            
        searchExtendingSteps
            .isToastPresented()
            .makeScreenshot(suffix: "_first")
        
        self.configureStubs(countStub: .withExtendedNumber2, filtersStub: nil)
        
        filtersSteps
            .tapOnRoomsTotalButton(.rooms4Plus)
            
        searchExtendingSteps
            .isToastPresented()
            .makeScreenshot(suffix: "_second")
        
        self.configureStubs(countStub: .withoutExtendedNumber, filtersStub: nil)
        
        filtersSteps
            .tapOnRoomsTotalButton(.rooms4Plus)
        
        searchExtendingSteps
            .isToastNotPresented()
    }
    
    func testToastSwipes() {
        let configuration = ExternalAppConfiguration.filterUITests
        self.relaunchApp(with: configuration)

        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()
        let searchExtendingSteps = SearchExtendingSteps()

        self.configureStubs(countStub: .withExtendedNumber1, filtersStub: nil)
        
        searchResultsSteps
            .openFilters()
        
        filtersSteps
            .isScreenPresented()
            
        searchExtendingSteps
            .isToastPresented()
            .swipeToast(direction: .up)
            .isToastPresented()
            .swipeToast(direction: .down)
            .isToastNotPresented()
        
        filtersSteps
            .tapOnRoomsTotalButton(.rooms3)
            
        searchExtendingSteps
            .isToastPresented()
            .swipeToast(direction: .left)
            .isToastNotPresented()
        
        filtersSteps
            .tapOnRoomsTotalButton(.rooms3)
            
        searchExtendingSteps
            .isToastPresented()
            .swipeToast(direction: .right)
            .isToastNotPresented()
    }
    
    func testChangeNumberOnBottomSheetApplyButton() {
        let configuration = ExternalAppConfiguration.filterUITests
        self.relaunchApp(with: configuration)

        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()
        let searchExtendingSteps = SearchExtendingSteps()

        self.configureStubs(countStub: .withExtendedNumber1, filtersStub: .price)
        
        searchResultsSteps
            .openFilters()
        
         filtersSteps
            .isScreenPresented()
            
        searchExtendingSteps
            .isToastPresented()
            .tapOnToastButton()
            .isBottomSheetPresented()
            .makeBottomSheetApplyButtonScreenshot(suffix: "_first")

        self.configureStubs(countStub: .withoutExtendedNumber, filtersStub: .price)
        
        searchExtendingSteps
            .isBottomSheetCellsCountEqual(count: 1)
            .tapOnBottomSheetCellSwitch(index: 0)
            .makeBottomSheetApplyButtonScreenshot(suffix: "_second")
    }

    func testBottomSheetError() {
        let configuration = ExternalAppConfiguration.filterUITests
        self.relaunchApp(with: configuration)

        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()
        let searchExtendingSteps = SearchExtendingSteps()

        self.configureStubs(countStub: .withExtendedNumber2, filtersStub: nil)

        searchResultsSteps
            .openFilters()

         filtersSteps
            .isScreenPresented()

        searchExtendingSteps
            .isToastPresented()
            .tapOnToastButton()
            .waitForTopNotificationViewExistence()
    }
    
    // MARK: - Private
    
    private enum ExtendedNumberStubType {
        case withExtendedNumber1
        case withExtendedNumber2
        case withoutExtendedNumber
    }
    
    private enum ExtendedFiltersStubType {
        case price
        case viewPort
        case priceAndViewPort
    }
    
    private func configureStubs(
        countStub: ExtendedNumberStubType?,
        filtersStub: ExtendedFiltersStubType?
    ) {
        switch countStub {
            case .withExtendedNumber1:
                APIStubConfigurator.setupOfferSearchResultsCountWithExtendedNumber_1(using: self.dynamicStubs)
            case.withExtendedNumber2:
                APIStubConfigurator.setupOfferSearchResultsCountWithExtendedNumber_2(using: self.dynamicStubs)
            case .withoutExtendedNumber:
                APIStubConfigurator.setupOfferSearchResultsCount(using: self.dynamicStubs)
            case .none: break
        }
        
        switch filtersStub {
            case .viewPort:
                APIStubConfigurator.setupSearchExtendingResultWithViewPort(using: self.dynamicStubs)
            case .price:
                APIStubConfigurator.setupSearchExtendingResultWithPrice(using: self.dynamicStubs)
            case .priceAndViewPort:
                APIStubConfigurator.setupSearchExtendingResultWithPriceAndViewPort(using: self.dynamicStubs)
            case .none: break
        }
    }
}
