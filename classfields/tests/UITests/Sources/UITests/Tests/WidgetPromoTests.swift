//
//  WidgetPromoTests.swift
//  UITests
//
//  Created by Alexander Igantev on 12/17/20.
//

import XCTest
import Snapshots
import AutoRuProtoModels
import SwiftProtobuf

final class WidgetPromoTests: BaseTest {
    private lazy var mainSteps = MainSteps(context: self)
    private var launchSettings: [String: Any] = [:]

    override var appSettings: [String: Any] {
        var settings = super.appSettings
        launchSettings.forEach { settings[$0.key] = $0.value }
        return settings
    }

    override func setUp() {
        setupServer()
        super.setUp()
    }

    func testWidgetBanner_openSavedFilters() {
        launchSettings = [
            "widgetPromoSavedFiltersHidden": false
        ]
        launch()

        mainSteps
            .openFavoritesTab()
            .waitForLoading()
            .tapSegment(at: .searches)
            .wait(for: 1)
            .validateSnapshot(of: "widget_promo")
    }

    func testWidgetWizard() {
        launchSettings = [
            "widgetPromoSavedFiltersHidden": false
        ]
        launch()

        mainSteps
            .openFavoritesTab()
            .waitForLoading()
            .tapSegment(at: .searches)
            .wait(for: 1)
            .tap("promo_widget_wizard")
            .validateSnapshot(of: "ModalViewControllerHost", snapshotId: "\(#function)_step_1")
            .tap("next_button")
            .wait(for: 1)
            .validateSnapshot(of: "ModalViewControllerHost", snapshotId: "\(#function)_step_2")
            .tap("next_button")
            .wait(for: 1)
            .validateSnapshot(of: "ModalViewControllerHost", snapshotId: "\(#function)_step_3")
    }

    private func setupServer() {

        advancedMockReproducer.setup(server: self.server, mockFolderName: "Favorites/Searches")

        server.forceLoginMode = .forceLoggedIn
        try! server.start()
    }
}
