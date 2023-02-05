//
//  UIImage+Utils.swift
//  MetroToolbox
//
//  Created by Alexander Shchavrovskiy on 12.07.2018.
//

import UIKit

public extension UIImage {

    // Image rendering mode should be .alwaysTemplate
    // Image size should be non-zero
    func tinted(in color: UIColor) -> UIImage {
        assert(size.width > 0 && size.height > 0)

        UIGraphicsBeginImageContextWithOptions(size, false, scale)
        defer {
            UIGraphicsEndImageContext()
        }

        color.set()
        draw(in: CGRect(x: 0, y: 0, width: size.width, height: size.height))

        return UIGraphicsGetImageFromCurrentImageContext()!
    }
    
}
