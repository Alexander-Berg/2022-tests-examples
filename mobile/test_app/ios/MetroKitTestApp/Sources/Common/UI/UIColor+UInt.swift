//
//  UIColor+UInt.swift
//  MetroToolbox
//
//  Created by Ilya Lobanov on 31/01/2018.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import UIKit

public extension UIColor {

    convenience init(rgb value: UInt) {
        self.init(byteRed: UInt8((value >> 16) & 0xff),
            green: UInt8((value >> 8) & 0xff),
            blue: UInt8(value & 0xff),
            alpha: 0xff)
    }

    convenience init(rgba value: UInt) {
        self.init(byteRed: UInt8((value >> 24) & 0xff),
            green: UInt8((value >> 16) & 0xff),
            blue: UInt8((value >> 8) & 0xff),
            alpha: UInt8(value & 0xff))
    }
    
    convenience init(byteRed red: UInt8, green: UInt8, blue: UInt8, alpha: UInt8 = 0xff) {
        self.init(red: CGFloat(red) / 255.0,
            green: CGFloat(green) / 255.0,
            blue: CGFloat(blue) / 255.0,
            alpha: CGFloat(alpha) / 255.0)
    }

}
