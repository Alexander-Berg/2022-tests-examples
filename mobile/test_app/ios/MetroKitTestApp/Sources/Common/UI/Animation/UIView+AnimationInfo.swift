//
//  UIView+AnimationInfo.swift
//  MetroToolbox
//
//  Created by Ilya Lobanov on 25/04/2018.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import UIKit

public extension UIView {
    
    public static func animate(with info: AnimationInfo, animations: @escaping () -> Void,
        completion: ((Bool) -> Void)?)
    {
        UIView.animate(withDuration: info.duration, delay: info.delay, options: info.options,
            animations: animations, completion: completion)
    }
    
    public static func animate(with info: AnimationInfo, animations: @escaping () -> Void) {
        UIView.animate(with: info, animations: animations, completion: nil)
    }

}
