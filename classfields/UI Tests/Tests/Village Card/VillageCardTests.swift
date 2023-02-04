//
//  VillageCardTests.swift
//  UI Tests
//
//  Created by Pavel Zhuravlev on 06.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest

final class VillageCardTests: BaseTestCase {
    private func performCommonTests(specificCardTests: (VillageCardSteps) -> Void) {
        self.relaunchApp(with: .cardTests)

        SearchResultsListSteps()
            .isScreenPresented()
            .withVillageList()
            .isListNonEmpty()
            .cell(withIndex: 0)
            .isPresented()
            .tap()

        let cardSteps = VillageCardSteps()
        cardSteps
            .isScreenPresented()
        specificCardTests(cardSteps)
    }
}

// MARK: - Card

extension VillageCardTests {
    func testCallsFromCard() {
        SnippetsListAPIStubConfigurator.setupVillagesList(using: self.dynamicStubs, stubKind: .noPhoto)
        VillageCardAPIStubConfiguration.setupVillageCardBilling(using: self.dynamicStubs)

        self.performCommonTests { cardSteps in
            let callScenarios = VillageCallScenarios(
                dynamicStubs: self.dynamicStubs,
                callButtonHandler: cardSteps
            )
            callScenarios
                .runBillingOnCall()
                .runCallWithTwoPhoneNumbers()
                .runCallWithError()
        }
    }
}

// MARK: - Gallery

extension VillageCardTests {
    func testCallsFromGallery() {
        SnippetsListAPIStubConfigurator.setupVillagesList(using: self.dynamicStubs, stubKind: .noPhoto)
        VillageCardAPIStubConfiguration.setupVillageCardBilling(using: self.dynamicStubs)

        self.performCommonTests { cardSteps in
            cardSteps
                .tapOnGallery()
            let callScenarios = VillageCallScenarios(
                dynamicStubs: self.dynamicStubs,
                callButtonHandler: AnyOfferGallerySteps()
            )
            callScenarios
                .runBillingOnCall()
                .runCallWithTwoPhoneNumbers()
                .runCallWithError()
        }
    }
}

// MARK: - VillageOffers

extension VillageCardTests {
    func testCallsFromVillageOffersList() {
        SnippetsListAPIStubConfigurator.setupVillagesList(using: self.dynamicStubs, stubKind: .noPhoto)
        VillageCardAPIStubConfiguration.setupVillageCardBilling(using: self.dynamicStubs)

        self.performCommonTests { cardSteps in
            let villageOfferList = cardSteps.tapStatisticsCell()
            villageOfferList
                .isScreenPresented()

            let callScenarios = VillageCallScenarios(
                dynamicStubs: self.dynamicStubs,
                callButtonHandler: villageOfferList
            )
            callScenarios
                .runBillingOnCall()
                .runCallWithTwoPhoneNumbers()
                .runCallWithError()
        }
    }
}
