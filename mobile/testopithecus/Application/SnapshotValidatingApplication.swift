//
//  SnapshotValidatingApplication.swift
//  YandexMobileMailAutoTests
//
//  Created by Artem I. Novikov on 07.06.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import testopithecus
import SnapshotTesting

public class SnapshotValidatingApplication: SnapshotValidating {
    private let aboutPage = AboutPage()
    private let messageListPage = MessageListPage()
    private let tabBarPage = TabBarPage()
    private let foldersListPage = FoldersListPage()
    private let rootSettingsPage = RootSettingsPage()
    private let selectParentFolderPage = SelectParentFolderPage()
    private let accountSettingsPage = AccountSettingsPage()

    public func verifyScreen(_ componentName: String, _ testName: String) throws {
        try XCTContext.runActivity(named: "Verifying screenshot of \(componentName)") { _ in
            let screenshot = XCUIApplication.yandexMobileMail.screenshot().image.fill(elements: self.elementsToPaintOver).withRemovedStatusBarAndHomeIndicator
            try self.verify(image: screenshot, componentName: componentName, testName: testName)
        }
    }

    public func verifyElement(_ element: XCUIElement) throws {
        let screenshot = element.screenshot().image.withRemovedStatusBarAndHomeIndicator
        try self.verify(image: screenshot)
    }

    private var elementsToPaintOver: [XCUIElement] {
        var elementsToPaintOver = self.messageListPage.dateElements + self.rootSettingsPage.personalSettingsButtonElements + [
            self.foldersListPage.accountSwitcherScrollView,
            self.foldersListPage.currentAccountEmailLabel,
            self.foldersListPage.currentAccountNameLabel,
            self.selectParentFolderPage.rootFolderCell,
            self.aboutPage.version,
            self.tabBarPage.calendarDate,
            self.accountSettingsPage.signature
        ].filter { $0.exists && $0.isHittable }

        if self.accountSettingsPage.navigationBarTitle.exists && self.accountSettingsPage.navigationBarTitle.label.contains("@") {
            elementsToPaintOver.append(self.accountSettingsPage.navigationBar)
        }
        return elementsToPaintOver
    }

    private func verify(image: UIImage?, componentName: String = "", testName: String = "") throws {
        let snapshotDirectory = URL(fileURLWithPath: #file, isDirectory: true)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .appendingPathComponent("__Snapshots__")
            .appendingPathComponent(UIDevice.current.type.rawValue)
            .appendingPathComponent(componentName)
            .path
        let failure = verifySnapshot(matching: UIImageView(image: image),
                                     as: .image(precision: 0.99),
                                     snapshotDirectory: snapshotDirectory,
                                     testName: testName.transliteratedToLatin.withRemovedModifierLetterPrime)
        guard let message = failure else { return }
        XCTFail(message)
    }
}
