//
//  TestColorView.swift
//  MetroToolbox
//
//  Created by Ilya Lobanov on 21/02/2018.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import UIKit

public final class TestColorView: UIView, UpdatableFrameLayoutProvider {
    
    public struct Layout: FrameLayout {
        public let size: CGSize
    }

    public struct Info {
        public let color: UIColor
        public let size: CGSize
    }

    // MARK: - FrameLayoutProvider

    public private(set) var info: Info?

    public func update(with info: Info, animation: AnimationInfo) {
        self.info = info
        backgroundColor = info.color
    }

    public static func layout(for info: Info, boundingSize: CGSize) -> Layout {
        let size = CGSize(width: min(info.size.width, boundingSize.width),
            height: min(info.size.height, boundingSize.height))
        
        return Layout(size: size)
    }

}

