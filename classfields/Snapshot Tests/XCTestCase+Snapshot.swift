//
//  XCTestCase+Snapshot.swift
//  Unit Tests
//
//  Created by Alexey Salangin on 12/28/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils
import AsyncDisplayKit
import SwiftUI
import YRETextureUtils
import YRECoreUI

extension XCTestCase {
    func assertSnapshot(_ view: UIView, file: StaticString = #file, function: String = #function) {
        let lightSnapshot = Self.image(for: view, isDarkMode: false)

        Snapshot.compareWithSnapshot(
            image: lightSnapshot,
            identifier: SnapshotIdentifier(
                suite: SnapshotIdentifier.suiteName(from: file),
                identifier: function,
                options: [.screen]
            )
        )

        let darkSnapshot = Self.image(for: view, isDarkMode: true)

        Snapshot.compareWithSnapshot(
            image: darkSnapshot,
            identifier: SnapshotIdentifier(
                suite: SnapshotIdentifier.suiteName(from: file),
                identifier: function,
                options: [.screen, .darkMode]
            )
        )
    }

    func assertSnapshot(
        _ node: ASDisplayNode,
        file: StaticString = #file,
        function: String = #function,
        backgroundColor: UIColor = .clear
    ) {
        if node.frame.width == 0 || node.frame.height == 0 {
            let width = UIScreen.main.bounds.width
            let minSize = CGSize(width: width, height: 0)
            let maxSize = CGSize(width: width, height: CGFloat.greatestFiniteMagnitude)
            let sizeRange = ASSizeRange(min: minSize, max: maxSize)

            let layout = ASCalculateRootLayout(node, sizeRange)
            layout.position = .zero
            node.frame = layout.frame

            node.setNeedsLayout()
            node.layoutIfNeeded()
        }

        let backgroundNode = DynamicBackgroundNode()
        backgroundNode.addSubnode(node)
        backgroundNode.dynamicBackgroundColor = backgroundColor
        backgroundNode.frame = node.frame
        backgroundNode.recursivelyEnsureDisplaySynchronously(true)

        self.assertSnapshot(backgroundNode.view, file: file, function: function)
    }

    func assertSnapshot<T: View>(
        _ view: T,
        file: StaticString = #file,
        function: String = #function
    ) {
        let viewController = UIHostingController(rootView: view)
        viewController.disableSafeArea()
        self.assertSnapshot(viewController.view, file: file, function: function)
    }

    // MARK: - Private
    private static func image(for view: UIView, isDarkMode: Bool = false) -> UIImage {
        let window = view.window ?? UIWindow()

        window.makeKeyAndVisible()

        if view.window == nil, (view as? UIWindow) != window {
            // The status bar affects the size of the views with autolayout.
            let viewController = NoStatusBarViewController()
            viewController.view.addSubview(view)
            window.rootViewController = viewController
        }

        window.overrideUserInterfaceStyle = isDarkMode ? .dark : .light

        if view.frame.width == 0 || view.frame.height == 0 {
            view.sizeToFit()
            view.setNeedsLayout()
            view.layoutIfNeeded()
        }

        let bounds = view.bounds

        XCTAssertNotEqual(bounds.width, .zero, "Zero width for view \(view)")
        XCTAssertNotEqual(bounds.height, .zero, "Zero height for view \(view)")

        let format = UIGraphicsImageRendererFormat()
        format.preferredRange = .standard

        let renderer = UIGraphicsImageRenderer(bounds: bounds, format: format)

        let snapshot = renderer.image { _ in
            view.drawHierarchy(in: bounds, afterScreenUpdates: true)
        }
        return snapshot
    }
}


fileprivate final class NoStatusBarViewController: UIViewController {
    override var prefersStatusBarHidden: Bool {
        true
    }
}
