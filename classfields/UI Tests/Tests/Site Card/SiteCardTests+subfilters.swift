//
//  SiteCardTests+subfilters.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 11.01.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import XCTest

extension SiteCardTests {
    func testMainFiltersArePassedToSubfiltersInOfferPlanList() {
        SiteCardAPIStubConfiguration.setupSiteSearchResultsListMoscow(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupSiteCardLevelAmurskaya(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupOfferStat(using: self.dynamicStubs, site: .levelAmurskaya)

        self.performCommonSubfilterTests(shouldSetAllFilters: true) { cardSteps in
            cardSteps
                .isScreenPresented()
                .scrollToPlanCell()

            let offerPlanListSteps = cardSteps
                .isPlanCellTappable()
                .tapPlanCell()

            let legacySiteSubfilterSteps = offerPlanListSteps
                .isScreenPresented()
                .isFilterButtonTappable()
                .tapFilterButton()

            legacySiteSubfilterSteps
                .isScreenPresented()
                .compareWithScreenshot(identifier: "legacySubfilters.site.plan")
        }
    }

    func testMainFiltersArePassedToSubfiltersInSiteOfferList() {
        SiteCardAPIStubConfiguration.setupSiteSearchResultsListMoscow(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupSiteCardLevelAmurskaya(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupOfferStat(using: self.dynamicStubs, site: .levelAmurskaya)

        self.performCommonSubfilterTests(shouldSetAllFilters: true) { cardSteps in
            cardSteps
                .isScreenPresented()
                .scrollToSubfilters()
                .isRoomsTotalsSelected(SiteSubfilterSteps.RoomsTotal.allCases)
                .priceParameter(.totalPrice, hasValue: "от 100 до 200 ₽")
                .singleSelectionParameter(.deliveryDate, hasValue: "Сдан")

            SiteOfferListByPlanAPIStubConfigurator.setupSiteSearchResultsListByPlan(using: self.dynamicStubs)

            let siteOfferListByPlanSteps = cardSteps
                .scrollToRoomStatisticsCell(title: "1-комнатные")
                .isRoomStatisticsCellTappable(title: "1-комнатные")
                .tapRoomStatisticsCell(title: "1-комнатные")

            let legacySiteSubfilterSteps = siteOfferListByPlanSteps
                .isScreenPresented()
                .isFilterButtonTappable()
                .tapFilterButton()

            legacySiteSubfilterSteps
                .isScreenPresented()
                .compareWithScreenshot(identifier: "legacySubfilters.site")
        }
    }

    func disabled_testResetSubfilters() { // Fix and turn on in Cards
        SiteCardAPIStubConfiguration.setupSiteSearchResultsListMoscow(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupSiteCardLevelAmurskaya(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupOfferStatEmptyList(using: self.dynamicStubs, site: .levelAmurskaya)

        self.performCommonSubfilterTests(shouldSetAllFilters: false) { cardSteps in
            cardSteps
                .isScreenPresented()
                .scrollToPlanCell()
                .scrollToSubfilters()

            cardSteps
                .scrollToResetFiltersCell()
                .scrollToRoomStatisticsCell(title: "1-комнатные")
                .isResetFiltersCellAvailable()
                .isSubfilterCellAvailable()

            SiteCardAPIStubConfiguration.setupOfferStat(using: self.dynamicStubs, site: .levelAmurskaya)

            cardSteps
                .tapSubmitOrResetButtonInSubfilterCell()
                .isRoomStatisticsCellTappable(title: "1-комнатные")
        }
    }

    func testSubfiltersOpenedFromNewbuildingOffer() {
        OfferCardAPIStubConfiguration.setupOfferSearchResultsList_offerFromSite(using: self.dynamicStubs)
        OfferCardAPIStubConfiguration.setupOfferCardInSite(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupSiteCardSalarevoPark(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupOfferStatSalarevoPark(using: self.dynamicStubs)

        self.relaunchApp(with: .cardTests)

        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()

        searchResultsSteps.openFilters()
        filtersSteps
            .isScreenPresented()
            .tapOnApartmentTypeButton(.all)
            .tapOnRoomsTotalButtons(FiltersSteps.RoomsTotal.allCases)

        filtersSteps.submitFilters()

        let list = SearchResultsListSteps()
        list
            .isScreenPresented()

        let offerList = list.withOfferList()
        offerList
            .isListNonEmpty()

        let cell = offerList.cell(withIndex: 0)
        cell
            .isPresented()
            .tap()

        let offerCardSteps = OfferCardSteps()
        let siteCardSteps = offerCardSteps
            .isOfferCardPresented()
            .scrollToSiteLink()
            .tapSiteLink()

        siteCardSteps
            .isScreenPresented()
            .scrollToSubfilters()
            .isRoomsTotalsSelected(SiteSubfilterSteps.RoomsTotal.allCases)

        siteCardSteps
            .scrollToPlanCell()
    }

    func disabled_testSubfiltersOpenedFromRentOffer() {
        SiteCardAPIStubConfiguration.setupSiteCardLevelAmurskaya(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupOfferStat(using: self.dynamicStubs, site: .levelAmurskaya)

        self.relaunchApp(with: .mapTests)

        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()

        searchResultsSteps.openFilters()
        filtersSteps
            .isScreenPresented()
            .switchToAction(.rent)
            .tapOnRoomsTotalButtons(FiltersSteps.RoomsTotal.allCases)

        filtersSteps
            .tapOnPrice()

        let numberRangePicker = FilterNumberRangePickerSteps()
        numberRangePicker
            .isScreenPresented(with: "Цена")
            .enter(.init(from: "100", to: "200"))
            .apply()

        filtersSteps.submitFilters()

        let searchResultsMap = SearchResultsMapSteps()
        searchResultsMap
            .isScreenPresented()

        let mapSteps = MapScreenSteps()
        mapSteps
            .tapMultihouseButton()

        let multihouseSteps = MultihouseOfferListSteps()
        let siteCardSteps = multihouseSteps
            .isScreenPresented()
            .multihouseHeader()
            .isPresented()
            .tap()

        siteCardSteps
            .isScreenPresented()
            .isLoadingIndicatorHidden()
            .scrollToSubfilters()
            .isRoomsTotalsNotSelected(SiteSubfilterSteps.RoomsTotal.allCases)
            .priceParameter(.totalPrice, hasValue: "за всё")
    }


    // MARK: - Private

    private func performCommonSubfilterTests(shouldSetAllFilters: Bool, specificCardTests: (SiteCardSteps) -> Void) {
        self.relaunchApp(with: .cardTests)

        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()

        searchResultsSteps.openFilters()
        filtersSteps
            .isScreenPresented()
            .tapOnApartmentTypeButton(.newbuilding)

        if shouldSetAllFilters {
            filtersSteps
                .tapOnRoomsTotalButtons(FiltersSteps.RoomsTotal.allCases)

            filtersSteps
                .tapOnPrice()
                .isPricePickerPresented()
                .enter(price: .init(from: "100", to: "200"))
                .apply()

            filtersSteps
                .openNumberRangePicker(for: .totalArea)
                .clear()
                .enter(.init(from: "100", to: "200"))
                .apply()
                .isScreenClosed()

            filtersSteps
                 .openNumberRangePicker(for: .kitchenArea)
                 .clear()
                 .enter(.init(from: "100", to: "200"))
                 .apply()

             filtersSteps
                 .openSingleSelectionPicker(for: .bathroomType)
                 .tapOnRow("Совмещённый")

             filtersSteps
                 .openNumberRangePicker(for: .floor)
                 .clear()
                 .enter(.init(from: "5", to: "10"))
                 .apply()

            filtersSteps
                .openSingleSelectionPicker(for: .deliveryDate)
                .tapOnRow("Сдан")
        }

        filtersSteps.submitFilters()

        let list = SearchResultsListSteps()
        list
            .isScreenPresented()

        let siteList = list.withSiteList()
        siteList
            .isListNonEmpty()

        let cell = siteList.cell(withIndex: 0)
        cell
            .isPresented()
            .isCallButtonTappable()
            .callButtonLabelStarts(with: "Позвонить")
            .tap()

        let siteCardSteps = SiteCardSteps()
        specificCardTests(siteCardSteps)
    }
}
