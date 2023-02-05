//
//  YMMainBarSnapshotTests.swift
//  YandexGeoToolboxTestApp
//
//  Created by Konstantin Kiselev on 13/04/16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import XCTest

class YMMainBarSnapshotTests: FBSnapshotTestCase {

    override func setUp() {
        super.setUp()
        self.recordMode = false
    }

    fileprivate func makeBarInContainer() -> (bar: YMMainBar, container: UIView) {
        let barWidth = 320
        let barHeight = 64
        
        let container = UIView(frame: CGRect(x: 0, y: 0, width: barWidth + 10, height: barHeight + 10))
        let mainbar = YMMainBar()

        container.backgroundColor = UIColor.yellow
        container.addSubview(mainbar)
        
        mainbar.translatesAutoresizingMaskIntoConstraints = false
        mainbar.attachLeftInContainer(margin: 5)
        mainbar.attachRightInContainer(margin: 5)
        mainbar.attachTopInContainer(margin: 5)
        
        return (mainbar, container)
    }
    
    fileprivate func makeIcon() -> UIImage {
        UIGraphicsBeginImageContextWithOptions(CGSize(width: 20, height: 20), false, UIScreen.main.scale)

        UIColor.red.setFill()
        UIGraphicsGetCurrentContext()!.fill(CGRect(x: 0, y: 0, width: 20, height: 20))
        
        let ret = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        return ret!
    }

    func testWithEmptyTextAndWithoutButton() {
        let (bar, container) = makeBarInContainer()
        bar.searchTextField.placeholder = "Search addresses & places"

        FBSnapshotVerifyView(view: container)
    }
    
    func testWithTextAndWithoutButton() {
        let (bar, container) = makeBarInContainer()
        bar.searchTextField.text = "Lva Tolstogo, 16"
        
        FBSnapshotVerifyView(view: container)
    }

    func testWithVeryLongTextAndWithoutButton() {
        let (bar, container) = makeBarInContainer()
        bar.searchTextField.text = "Search addresses & places very long very long very long very long very long very"
        
        FBSnapshotVerifyView(view: container)
    }
    
    func testWithTextAndCancelButton() {
        let (bar, container) = makeBarInContainer()
        
        bar.searchTextField.text = "Lva Tolstogo, 16"
        bar.rightButtonInfo = YMSearchBarButtonInfo(identifier: "cancel", text: "Cancel", image: nil, width: nil)
        
        FBSnapshotVerifyView(view: container)
    }
    
    func testWithCancelButtonAndCustomWidth() {
        let (bar, container) = makeBarInContainer()
        bar.rightButtonInfo = YMSearchBarButtonInfo(identifier: "cancel", text: "Cancel", image: nil, width: 100)
        
        FBSnapshotVerifyView(view: container)
    }
    
    func testWithRemovedCancelButton() {
        let (bar, container) = makeBarInContainer()
        bar.rightButtonInfo = YMSearchBarButtonInfo(identifier: "cancel", text: "Cancel", image: nil, width: nil)
        bar.rightButtonInfo = nil
        
        FBSnapshotVerifyView(view: container)
    }
    
    func testWithImageButton() {
        let (bar, container) = makeBarInContainer()
        let icon = makeIcon()
        bar.rightButtonInfo = YMSearchBarButtonInfo(identifier: "cancel", text: nil, image: icon, width: nil)
        
        FBSnapshotVerifyView(view: container)
    }

    func testWithImageAndTextButton() {
        let (bar, container) = makeBarInContainer()
        let icon = makeIcon()
        bar.rightButtonInfo = YMSearchBarButtonInfo(identifier: "cancel", text: "?", image: icon, width: 100)
        
        FBSnapshotVerifyView(view: container)
    }

}
