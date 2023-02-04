import UIKit
import XCTest
import Snapshots
import CoreGraphics

extension Snapshot {
    static func compareWithSnapshot(
        element: XCUIElement,
        identifier: SnapshotIdentifier,
        wait: Bool = true,
        timeout: TimeInterval = 1.0
    ) {
        self.compareWithSnapshot(
            image: (wait ? element.waitAndScreenshot(timeout: timeout) : element.screenshot()).image,
            identifier: identifier
        )
    }

    static func screenshotCollectionView(
        fromCell: XCUIElement,
        toCell: XCUIElement,
        windowInsets: UIEdgeInsets = .zero,
        timeout: TimeInterval = 1.0
    ) -> UIImage {
        XCTContext.runActivity(named: "Скриншотим часть collection view") { _ in
            self.capturePartOfCollectionView(
                fromCell: fromCell,
                toCell: toCell,
                windowInsets: windowInsets,
                timeout: timeout
            )
        }
    }

    private static func capturePartOfCollectionView(
        fromCell: XCUIElement,
        toCell: XCUIElement,
        windowInsets: UIEdgeInsets = .zero,
        timeout: TimeInterval = 1.0
    ) -> UIImage {
        let windowFrame = XCUIApplication.make().windows.element(boundBy: 0).frame.inset(by: windowInsets)

        assert(fromCell.elementType == .cell, "From element should be a cell")

        func collectionView(contaning cellIdentifier: String) -> XCUIElement {
            return XCUIApplication.make().collectionViews
                .containing(.cell, identifier: cellIdentifier)
                .element(boundBy: 0)
        }

        func scrollToCompleteVisibleIfNeeded(collectionView: XCUIElement, cell: XCUIElement) {
            if (cell.frame.maxY < windowFrame.minY)
                || (cell.frame.maxY >= windowFrame.minY && cell.frame.minY < windowFrame.minY) {
                collectionView.scrollTo(
                    element: cell,
                    swipeDirection: .down,
                    windowInsets: windowInsets
                )
            } else if (cell.frame.minY > windowFrame.maxY)
                || (cell.frame.minY <= windowFrame.maxY && cell.frame.maxY > windowFrame.maxY) {
                collectionView.scrollTo(
                    element: cell,
                    swipeDirection: .up,
                    windowInsets: windowInsets
                )
            }
        }

        var images: [UIImage] = []
        var isToCellReached = false
        var cellIdentifier = fromCell.identifier

        while !isToCellReached {
            let parent = collectionView(contaning: cellIdentifier)
            let cell = parent.cells.matching(identifier: cellIdentifier).element(boundBy: 0)

            scrollToCompleteVisibleIfNeeded(collectionView: parent, cell: cell)
            images.append(cell.waitAndScreenshot(timeout: timeout).image)

            if toCell.isFullyVisible(), cell.identifier == toCell.identifier {
                isToCellReached = true
                break
            }

            let cells = parent.cells.allElementsBoundByIndex.map { $0.identifier }
            for (index, item) in cells.enumerated() where item == cellIdentifier {
                if index == cells.count - 1 {
                    XCTFail("To cell is unreached")
                }

                cellIdentifier = cells[index + 1]
                break
            }
        }

        if images.isEmpty {
            XCTFail("No images captured")
        }

        let height = images.map { $0.size.height }.reduce(0.0, +)
        let size = CGSize(width: images[0].size.width, height: height)

        UIGraphicsBeginImageContextWithOptions(size, false, images[0].scale)
        defer {
            UIGraphicsEndImageContext()
        }

        var yValue: CGFloat = 0
        for image in images {
            image.draw(at: CGPoint(x: 0, y: yValue))
            yValue += image.size.height
        }

        guard let composedImage = UIGraphicsGetImageFromCurrentImageContext() else {
            XCTFail("Invalid image compose")
            return UIImage()
        }

        return composedImage
    }
}

extension XCUIElement {
    func waitAndScreenshot(timeout: TimeInterval = 1.0) -> XCUIScreenshot {
        _ = self.waitForExistence(timeout: timeout)
        return self.screenshot()
    }
}

extension UIImage {
    func filling(element: XCUIElement, color: UIColor = UIColor(red: 0.93, green: 0, blue: 0.99, alpha: 1.0)) -> UIImage {
        UIGraphicsBeginImageContextWithOptions(self.size, false, self.scale)
        defer {
            UIGraphicsEndImageContext()
        }

        self.draw(at: CGPoint.zero)

        guard let context = UIGraphicsGetCurrentContext() else {
            fatalError("Invalid context")
        }

        context.setFillColor(color.cgColor)
        context.fill(element.frame)

        guard let newImage = UIGraphicsGetImageFromCurrentImageContext() else {
            fatalError("Unable to draw new image")
        }

        return newImage
    }
}
