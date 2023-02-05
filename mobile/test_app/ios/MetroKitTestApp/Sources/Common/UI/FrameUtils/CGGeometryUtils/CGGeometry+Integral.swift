//
//  CGGeometry+Integral.swift
//  MetroToolbox
//
//  Created by Ilya Lobanov on 07/05/2018.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import UIKit

public extension CGSize {

    public var integral: CGSize {
        return CGSize(width: width.rounded(.up), height: height.rounded(.up))
    }
    
}

public extension CGPoint {
    
    public var integral: CGPoint {
        return CGPoint(x: x.rounded(.toNearestOrEven), y: y.rounded(.toNearestOrEven))
    }
    
    func roundedToPixel(_ rule: FloatingPointRoundingRule) -> CGPoint {
        return CGPoint(x: x.roundedToPixel(rule), y: y.roundedToPixel(rule))
    }
    
}

private extension CGFloat {
    
    func roundedToPixel(_ rule: FloatingPointRoundingRule) -> CGFloat {
        let scale = UIScreen.main.scale
        return (self * scale).rounded(rule) / scale
    }
    
}
