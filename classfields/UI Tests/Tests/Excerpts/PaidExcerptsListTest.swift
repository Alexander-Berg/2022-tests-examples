//
//  PaidExcerptsListTest.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 18.12.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest
import YREAppConfig

final class PaidExcerptsListTest: BaseTestCase {
    func testUnathorizedUserList() {
        let config = ExternalAppConfiguration.paidExcerptsListTests
        config.isAuthorized = false
        self.relaunchApp(with: config)
        
        let paidExcerptsListSteps = self.enterToPaidExcerptsScreen()
        paidExcerptsListSteps
            .isScreenPresented()
            .isAuthViewPresented()
    }
    
    func testEmptyList() {
        ExcerptsListAPIStubConfigurator.setupEmptyList(using: self.dynamicStubs)
        self.relaunchApp(with: .paidExcerptsListTests)
        
        let paidExcerptsListSteps = self.enterToPaidExcerptsScreen()
        paidExcerptsListSteps
            .isScreenPresented()
            .isEmptyViewPresented()
            .tapShowOffersButton()
        
        OfferListSteps.mainList().isScreenPresented()
    }
    
    func testSnippetButtons() {
        self.relaunchApp(with: .paidExcerptsListTests)
        ExcerptsListAPIStubConfigurator.setupSingleReportList(using: self.dynamicStubs,
                                                              stub: .doneWithOffer)
        
        let paidExcerptsListSteps = self.enterToPaidExcerptsScreen()
        paidExcerptsListSteps.tapShowExcerptButtonOnSnippet(row: 0)
        
        PaidExcerptsSteps()
            .isScreenPresented()
            .tapBackButton()
        
        yreSleep(2, message: "Ждем перересовки UI")
        
        paidExcerptsListSteps.tapShowOfferButtonOnSnippet(row: 0)
        
        OfferCardSteps()
            .isOfferCardPresented()
            .tapBackButton()
        
        paidExcerptsListSteps.tapBackButton()
        ExcerptsListAPIStubConfigurator.setupSingleReportList(using: self.dynamicStubs,
                                                              stub: .errorWithOffer)
        InAppServicesSteps().tapOnPaidExcertpsList()
        
        paidExcerptsListSteps.tapPayAgainButtonOnSnippet(row: 0)
        
        PaymentMethodsSteps()
            .isPaymentMethodsScreenPresented()
    }
    
    func testPagination() {
        self.relaunchApp(with: .paidExcerptsListTests)
        ExcerptsListAPIStubConfigurator.setupLongList(using: self.dynamicStubs)
        
        let paidExcerptsListSteps = self.enterToPaidExcerptsScreen()
        
        paidExcerptsListSteps
            .isScreenPresented()
            .isListPresented()
            .scrollToSnippet(row: 10)
    }
    
    private func enterToPaidExcerptsScreen() -> PaidExcerptsListSteps {
        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
            .tapOnPaidExcertpsList()
        
        return PaidExcerptsListSteps()
    }
}
