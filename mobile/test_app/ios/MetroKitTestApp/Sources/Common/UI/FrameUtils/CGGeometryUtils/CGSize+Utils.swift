//
//  CGSize+Utils.swift
//  MetroToolbox
//
//  Created by Ilya Lobanov on 22/02/2018.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import CoreGraphics
import UIKit

public extension CGSize {
 
    public var boundingRect: CGRect {
        return CGRect(origin: .zero, size: self)
    }
    
}

public extension CGSize {

    public func intersection(_ other: CGSize) -> CGSize {
        return CGSize(width: min(width, other.width), height: min(height, other.height))
    }

    public static func intersection(_ lhs: CGSize, _ others: CGSize...) -> CGSize {
        return others.reduce(lhs) { $0.intersection($1) }
    }
    
}
