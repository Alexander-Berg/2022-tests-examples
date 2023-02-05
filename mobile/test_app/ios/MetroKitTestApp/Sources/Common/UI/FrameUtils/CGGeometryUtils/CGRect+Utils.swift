//
//  CGRect+utils.swift
//  MetroToolbox
//
//  Created by Ilya Lobanov on 16/02/2018.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import CoreGraphics

public extension CGRect {
    
    public init(size: CGSize) {
        self.init(origin: .zero, size: size)
    }
    
    public init(width: CGFloat, height: CGFloat) {
        self.init(size: CGSize(width: width, height: height))
    }
    
    public var bounds: CGRect {
        return CGRect(size: size)
    }

    // MARK: - Static

    public static func union(_ lhs: CGRect, _ others: CGRect...) -> CGRect {
        return others.reduce(lhs) { $0.union($1) }
    }

    public static func union(_ rects: [CGRect]) -> CGRect {
        return rects.reduce(.zero) { $0.union($1) }
    }
    
}
