//
//  XCUIElement+Screenshot.swift
//  UI Tests
//
//  Created by Alexey Salangin on 12/27/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils

extension XCUIElement {
    func yreWaitAndScreenshot(
        timeout: TimeInterval = 1.0,
        ignoreEdges: UIEdgeInsets = .zero
    ) -> UIImage {
        _ = self.waitForExistence(timeout: timeout)
        // `image` from `XCUIScreenshot` does not always have the correct scale and size.
        // Sometimes image scale = 1 but screen scale = 2 or 3.
        let pngData = self.screenshot().pngRepresentation
        var image = UIImage(data: pngData, scale: UIScreen.main.scale) ?? UIImage()

        // crop images to remove ignored edge zones.
        let cropRect = CGRect(origin: .zero, size: image.size).inset(by: ignoreEdges)
        image = image.sd_croppedImage(with: cropRect) ?? image

        return image
    }

    func yreWaitAndCompareScreenshot(
        timeout: TimeInterval = 1.0,
        identifier: String,
        threshold: Snapshot.Threshold = .justNoticeable,
        overallTolerance: Double = Snapshot.defaultOverallTolerance,
        ignoreEdges: UIEdgeInsets = .zero,
        file: StaticString = #file,
        line: UInt = #line
    ) {
        let screenshot = self.yreWaitAndScreenshot(timeout: timeout)
        Snapshot.compareWithSnapshot(
            image: screenshot,
            identifier: identifier,
            threshold: threshold,
            overallTolerance: overallTolerance,
            ignoreEdges: ignoreEdges,
            file: file,
            line: line
        )
    }
}
