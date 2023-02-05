//
//  TestableView.swift
//  YandexTransportTests
//
//  Created by Yury Potapov on 16.01.18.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import UIKit

class TestableView: UIView {
    struct Info {
        let color: UIColor
        let size: CGSize
    }
    
    struct Layout: FrameLayout {
        let size: CGSize
    }
    
    private var _info: Info? {
        didSet {
            update()
        }
    }
    
    private func update() {
        backgroundColor = info?.color
    }
}

extension TestableView: FrameLayoutProvider {
    var info: Info? {
        get { return _info }
        set { _info = newValue }
    }
    
    static func layout(for info: Info, boundingSize: CGSize) -> Layout {
        let size = CGSize(
            width: min(boundingSize.width, info.size.width),
            height: min(boundingSize.height, info.size.height))
        
        return Layout(size: size)
    }
}

