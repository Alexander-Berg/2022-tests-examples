//
//  FlowLayoutViewTest.swift
//  UtilsUITests
//
//  Created by Aleksey Makhutin on 17.11.2021.
//

import UIKit
import XCTest
@testable import UtilsUI

internal final class FlowLayoutViewTest: XCTestCase {
    // swiftlint:disable:next function_body_length
    func testLayoutCorrectly() {
        let flowLayoutView = FlowLayoutView(frame: CGRect(x: 0, y: 0, width: 320, height: 44))
        flowLayoutView.minimumInteritemSpacing = 7
        flowLayoutView.contentInsets = UIEdgeInsets(top: 5, left: 15, bottom: 5, right: 5)
        flowLayoutView.preferredLineHeight = 30
        flowLayoutView.shouldUsePreferredLineHeight = true

        [30, 45, 100, 200, 400].forEach { width in
            flowLayoutView.addSubview(self.createView(with: width))
        }
        flowLayoutView.layoutSubviews()
        let portraitLayoutFrames = [
            CGRect(x: 15, y: 5, width: 30, height: 30),
            CGRect(x: 52, y: 5, width: 45, height: 30),
            CGRect(x: 104, y: 5, width: 100, height: 30),
            CGRect(x: 15, y: 35, width: 200, height: 30),
            CGRect(x: 15, y: 65, width: 300, height: 30)
        ]

        XCTAssertEqual(flowLayoutView.subviews.map { $0.frame }, portraitLayoutFrames)

        let trucatedView = flowLayoutView.subviews[4] as? MockView
        trucatedView?.size.width = 600
        flowLayoutView.layoutSubviews()
        XCTAssertEqual(flowLayoutView.subviews.map { $0.frame }, portraitLayoutFrames, "should resize already truncated view")

        flowLayoutView.frame = CGRect(x: 0, y: 0, width: 640, height: 44)
        flowLayoutView.layoutSubviews()
        let landscapeLayoutFrames = [
            CGRect(x: 15, y: 5, width: 30, height: 30),
            CGRect(x: 52, y: 5, width: 45, height: 30),
            CGRect(x: 104, y: 5, width: 100, height: 30),
            CGRect(x: 211, y: 5, width: 200, height: 30),
            CGRect(x: 15, y: 35, width: 620, height: 30)
        ]
        XCTAssertEqual(flowLayoutView.subviews.map { $0.frame }, landscapeLayoutFrames)

        var variableWidthView = flowLayoutView.subviews[2] as? MockView
        variableWidthView?.size.width += 450
        flowLayoutView.layoutSubviews()
        let risezedToBiggerViewLayoutFrames = [
            CGRect(x: 15, y: 5, width: 30, height: 30),
            CGRect(x: 52, y: 5, width: 45, height: 30),
            CGRect(x: 15, y: 35, width: 550, height: 30),
            CGRect(x: 15, y: 65, width: 200, height: 30),
            CGRect(x: 15, y: 95, width: 620, height: 30)
        ]
        XCTAssertEqual(flowLayoutView.subviews.map { $0.frame }, risezedToBiggerViewLayoutFrames)

        variableWidthView = flowLayoutView.subviews[3] as? MockView
        variableWidthView?.size.width -= 150
        flowLayoutView.layoutSubviews()
        let risezedToSmallerViewLayoutFrames = [
            CGRect(x: 15, y: 5, width: 30, height: 30),
            CGRect(x: 52, y: 5, width: 45, height: 30),
            CGRect(x: 15, y: 35, width: 550, height: 30),
            CGRect(x: 572, y: 35, width: 50, height: 30),
            CGRect(x: 15, y: 65, width: 620, height: 30)
        ]
        XCTAssertEqual(flowLayoutView.subviews.map { $0.frame }, risezedToSmallerViewLayoutFrames)

        flowLayoutView.insertSubview(self.createView(with: 55), at: 0)
        flowLayoutView.layoutSubviews()
        let insertFirstViewLayoutFrames = [
            CGRect(x: 15, y: 5, width: 55, height: 30),
            CGRect(x: 77, y: 5, width: 30, height: 30),
            CGRect(x: 114, y: 5, width: 45, height: 30),
            CGRect(x: 15, y: 35, width: 550, height: 30),
            CGRect(x: 572, y: 35, width: 50, height: 30),
            CGRect(x: 15, y: 65, width: 620, height: 30)
        ]
        XCTAssertEqual(flowLayoutView.subviews.map { $0.frame }, insertFirstViewLayoutFrames)

        flowLayoutView.insertSubview(self.createView(with: 82), at: 2)
        flowLayoutView.layoutSubviews()
        let insertMiddleViewLayoutFrames = [
            CGRect(x: 15, y: 5, width: 55, height: 30),
            CGRect(x: 77, y: 5, width: 30, height: 30),
            CGRect(x: 114, y: 5, width: 82, height: 30),
            CGRect(x: 203, y: 5, width: 45, height: 30),
            CGRect(x: 15, y: 35, width: 550, height: 30),
            CGRect(x: 572, y: 35, width: 50, height: 30),
            CGRect(x: 15, y: 65, width: 620, height: 30)
        ]
        XCTAssertEqual(flowLayoutView.subviews.map { $0.frame }, insertMiddleViewLayoutFrames)

        let removableView = flowLayoutView.subviews[2]
        removableView.removeFromSuperview()
        flowLayoutView.layoutSubviews()
        let removedViewLayoutFrames = [
            CGRect(x: 15, y: 5, width: 55, height: 30),
            CGRect(x: 77, y: 5, width: 30, height: 30),
            CGRect(x: 114, y: 5, width: 45, height: 30),
            CGRect(x: 15, y: 35, width: 550, height: 30),
            CGRect(x: 572, y: 35, width: 50, height: 30),
            CGRect(x: 15, y: 65, width: 620, height: 30)
        ]
        XCTAssertEqual(flowLayoutView.subviews.map { $0.frame }, removedViewLayoutFrames)

        flowLayoutView.shouldUsePreferredLineHeight = false
        flowLayoutView.subviews.forEach { ($0 as? MockView)?.size.height = 60 }
        flowLayoutView.layoutSubviews()
        let withoutPrefferedHeightLayoutFrames = [
            CGRect(x: 15, y: 5, width: 55, height: 60),
            CGRect(x: 77, y: 5, width: 30, height: 60),
            CGRect(x: 114, y: 5, width: 45, height: 60),
            CGRect(x: 15, y: 65, width: 550, height: 60),
            CGRect(x: 572, y: 65, width: 50, height: 60),
            CGRect(x: 15, y: 125, width: 620, height: 60)
        ]
        XCTAssertEqual(flowLayoutView.subviews.map { $0.frame }, withoutPrefferedHeightLayoutFrames)

        flowLayoutView.subviews[5].removeFromSuperview() // remove big with view for testing last fill view
        let lastFillWidthView = flowLayoutView.subviews[4] as? MockView
        lastFillWidthView?.size.width = 30
        flowLayoutView.shouldLastViewFillWidth = true
        flowLayoutView.layoutSubviews()
        let lastViewfilledInLineWithOtherViewLayoutFrames = [
            CGRect(x: 15, y: 5, width: 55, height: 60),
            CGRect(x: 77, y: 5, width: 30, height: 60),
            CGRect(x: 114, y: 5, width: 45, height: 60),
            CGRect(x: 15, y: 65, width: 550, height: 60),
            CGRect(x: 572, y: 65, width: 63, height: 60)
        ]
        XCTAssertEqual(flowLayoutView.subviews.map { $0.frame }, lastViewfilledInLineWithOtherViewLayoutFrames)

        flowLayoutView.shouldLastViewFillWidth = false
        flowLayoutView.layoutSubviews()
        let lastViewNotfilledInLineWithOtherViewLayoutFrames = [
            CGRect(x: 15, y: 5, width: 55, height: 60),
            CGRect(x: 77, y: 5, width: 30, height: 60),
            CGRect(x: 114, y: 5, width: 45, height: 60),
            CGRect(x: 15, y: 65, width: 550, height: 60),
            CGRect(x: 572, y: 65, width: 30, height: 60)
        ]
        XCTAssertEqual(flowLayoutView.subviews.map { $0.frame }, lastViewNotfilledInLineWithOtherViewLayoutFrames)

        lastFillWidthView?.size.width = 70
        flowLayoutView.shouldLastViewFillWidth = true
        flowLayoutView.layoutSubviews()
        let lastViewfilledInNextLineLayoutFrames = [ // does not fit in the line with other view
            CGRect(x: 15, y: 5, width: 55, height: 60),
            CGRect(x: 77, y: 5, width: 30, height: 60),
            CGRect(x: 114, y: 5, width: 45, height: 60),
            CGRect(x: 15, y: 65, width: 550, height: 60),
            CGRect(x: 15, y: 125, width: 620, height: 60)
        ]
        XCTAssertEqual(flowLayoutView.subviews.map { $0.frame }, lastViewfilledInNextLineLayoutFrames)

        lastFillWidthView?.size.width = 70
        flowLayoutView.shouldLastViewFillWidth = false
        flowLayoutView.layoutSubviews()
        let lastViewNotfilledInNextLineLayoutFrames = [
            CGRect(x: 15, y: 5, width: 55, height: 60),
            CGRect(x: 77, y: 5, width: 30, height: 60),
            CGRect(x: 114, y: 5, width: 45, height: 60),
            CGRect(x: 15, y: 65, width: 550, height: 60),
            CGRect(x: 15, y: 125, width: 70, height: 60)
        ]
        XCTAssertEqual(flowLayoutView.subviews.map { $0.frame }, lastViewNotfilledInNextLineLayoutFrames)

        flowLayoutView.subviews.forEach { $0.removeFromSuperview() }
        flowLayoutView.addSubview(self.createView(with: 625))
        flowLayoutView.addSubview(self.createView(with: 625))
        flowLayoutView.layoutSubviews()
        let checkNotNeedMoveFirstViewToNextLine = [
            CGRect(x: 15, y: 5, width: 620, height: 30), // fist view y should equal contentInset.top
            CGRect(x: 15, y: 35, width: 620, height: 30)
        ]
        XCTAssertEqual(flowLayoutView.subviews.map { $0.frame }, checkNotNeedMoveFirstViewToNextLine)
    }

    private func createView(with width: CGFloat, height: CGFloat = 30) -> UIView {
        return MockView(frame: CGRect(x: 0, y: 0, width: width, height: height))
    }
}

extension FlowLayoutViewTest {
    final class MockView: UIView {
        var size = CGSize.zero

        override init(frame: CGRect) {
            self.size = frame.size
            super.init(frame: frame)
        }

        required init?(coder: NSCoder) {
            fatalError("init(coder:) has not been implemented")
        }

        override func sizeThatFits(_ size: CGSize) -> CGSize {
            return self.size
        }
    }
}
