//
//  ScrollAwareThing.swift
//  YxSwissKnife
//
//  Created by Denis Malykh on 11.05.17.
//  Copyright © 2017 Yandex. All rights reserved.
//

import UIKit

class ScrollAwareThing: UIView {

    @IBOutlet weak var offset: NSLayoutConstraint!
}

extension ScrollAwareThing: YxScrollViewAwareable {
    func didScroll(to offset: CGPoint, in scrollView: YxScrollView) {
        self.offset.constant = -offset.y
    }
}
