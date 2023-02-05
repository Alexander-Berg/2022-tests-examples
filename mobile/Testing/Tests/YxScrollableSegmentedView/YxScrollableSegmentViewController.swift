//
//  YxScrollableSegmentViewController.swift
//  YxSwissKnife
//
//  Created by Edgar Serobyan on 7/11/19.
//  Copyright © 2019 Yandex. All rights reserved.
//

import UIKit

class YxScrollableSegmentViewController: UIViewController {
    
    override func viewDidLoad() {
        super.viewDidLoad() 
        
        let scrollableSegmentedView = YxScrollableSegmentedView(
            titles: ["wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww", "xxxxxxxxxxxxx", "tttttttt"],
            frame: CGRect(x: 15, y: 100, width: self.view.frame.width - 30, height: 60)
        )
        scrollableSegmentedView.onSelectedSegmentChanged = test
        scrollableSegmentedView.selectedSegmentIndex = 0
        
        self.view.addSubview(scrollableSegmentedView)
    }
    
    func test() {
        print("test")
    }
}
