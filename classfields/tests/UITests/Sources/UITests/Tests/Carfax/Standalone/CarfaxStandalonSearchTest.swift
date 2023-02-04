//
//  CarfaxStandalonSearchTest.swift
//  UITests
//
//  Created by Pavel Savchenkov on 31.07.2021.
//

import Foundation
import XCTest
import Snapshots

/// @depends_on AutoRuStandaloneCarHistory 
final class CarfaxStandalonSearchTest: BaseTest {
    lazy var mainSteps = MainSteps(context: self)

    override func setUp() {
        super.setUp()
        setupServer()
        launch()
    }

    // MARK: - Helpers
    private func setupServer() {
        mocker
            .mock_base()
            .setForceLoginMode(.forceLoggedIn)
            .mock_paymentInitWithAttachedCard()
            .mock_paymentProcess()
            .mock_paymentClosed()
            .mock_reportsList()
            .startMock()
    }

    func test_shouldBuyReportFromSearch() {
        mocker
            .mock_reportsList()
            .mock_reportLayoutForSearch(bought: false)
            .mock_reportLayoutForReport(bought: true)

        let carfaxReportSteps = CarfaxStandaloneCardBasicSteps(context: self)
        let carfaxReportListSteps = CarReportsListSteps(context: self)

        openReportSearch()
            .typeInSearchBar(text: carNumber)
            .tapOnSearchButton()
            .tapOnBuySingleReportButton()
            .wait()
            .tapOnPurchaseButton()

        carfaxReportSteps
            .checkReportTitle(withText: reportTitle)
            .tapBack()

        carfaxReportListSteps
            .checkReportSnippetIsDisplayed(withVin: carVin)
    }

    private func openReportSearch() -> CarReportSearchSteps {
        self.mainSteps
            .openCarReportsList()
            .tapOnSearch()
    }

    private let carNumber = "Е687НУ777"
    private let carVin = "WVWZZZ1KZBW515003"
    private let reportTitle = "Volkswagen Golf Plus, 2010"
}
