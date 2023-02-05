//
//  AnimationInfo.swift
//  MetroToolbox
//
//  Created by Ilya Lobanov on 25/04/2018.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import UIKit

public struct AnimationInfo {
    public var duration: TimeInterval
    public var delay: TimeInterval
    public var options: UIView.AnimationOptions
    
    public init(duration: TimeInterval, delay: TimeInterval = 0.0, options: UIView.AnimationOptions = []) {
        self.duration = duration
        self.delay = delay
        self.options = options
    }
}

public extension AnimationInfo {

    public static var none: AnimationInfo {
        return AnimationInfo(duration: 0.0, delay: 0.0, options: [])
    }
    
    public var isNone: Bool {
        return duration <= 0 && delay <= 0
    }

}
