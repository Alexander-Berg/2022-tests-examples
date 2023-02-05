//
//  PinImageResources.swift
//  YandexMetro
//
//  Created by Ilya Lobanov on 31/01/2018.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import UIKit

struct PinImageResources {
    enum Direction {
        case top
        case left
        case bottom
        case right
    }

    // MARK: - Mask
    
    static let bottomMask = StyleKit_Metro.imageOfPin_mask_0.withRenderingMode(.alwaysTemplate)
    static let leftMask = StyleKit_Metro.imageOfPin_mask_90.withRenderingMode(.alwaysTemplate)
    static let topMask = StyleKit_Metro.imageOfPin_mask_180.withRenderingMode(.alwaysTemplate)
    static let rightMask = StyleKit_Metro.imageOfPin_mask_270.withRenderingMode(.alwaysTemplate)
    
    static func mask(direction: Direction) -> UIImage {
        switch direction {
        case .top: return topMask
        case .left: return leftMask
        case .bottom: return bottomMask
        case .right: return rightMask
        }
    }
    
    // MARK: - Shadow
    
    static let bottomShadow = StyleKit_Metro.imageOfPin_shadow_0.withRenderingMode(.alwaysTemplate)
    static let leftShadow = StyleKit_Metro.imageOfPin_shadow_90.withRenderingMode(.alwaysTemplate)
    static let topShadow = StyleKit_Metro.imageOfPin_shadow_180.withRenderingMode(.alwaysTemplate)
    static let rightShadow = StyleKit_Metro.imageOfPin_shadow_270.withRenderingMode(.alwaysTemplate)

    static func shadow(direction: Direction) -> UIImage {
        switch direction {
        case .top: return topShadow
        case .left: return leftShadow
        case .bottom: return bottomShadow
        case .right: return rightShadow
        }
    }

    // MARK: - Icons

    static var undefinedIcon: UIImage { return StyleKit_Metro.imageOfPin_dot }
    static var aIcon: UIImage { return StyleKit_Metro.imageOfPin_a }
    static var bIcon: UIImage { return StyleKit_Metro.imageOfPin_b }
    
    // MARK: - Pins
    
    static func pin(icon: UIImage, color: UIColor, direction: Direction) -> UIImage {
        let images: [UIImage] = [
            shadow(direction: direction).tinted(in: color),
            mask(direction: direction).tinted(in: color),
            icon
        ]
        return combine(images: images)
    }
    
    static func undefined(color: UIColor, direction: Direction) -> UIImage {
        return pin(icon: undefinedIcon, color: color, direction: direction)
    }
    
    static func a(color: UIColor, direction: Direction) -> UIImage {
        return pin(icon: aIcon, color: color, direction: direction)
    }
    
    static func b(color: UIColor, direction: Direction) -> UIImage {
        return pin(icon: bIcon, color: color, direction: direction)
    }
    
    // MARK: - Private
    
    private static func combine(images: [UIImage]) -> UIImage {
        let boundingSize: (CGSize, CGSize) -> CGSize = {
            CGSize(width: max($0.width, $1.width), height: max($0.height, $1.height))
        }
        
        let size: CGSize = images.reduce(.zero) { boundingSize($0, $1.size) }
        
        assert(size.width > 0 && size.height > 0)
        
        UIGraphicsBeginImageContextWithOptions(size, false, UIScreen.main.scale)
        defer {
            UIGraphicsEndImageContext()
        }
        
        for image in images {
            image.draw(at: .zero)
        }
        
        return UIGraphicsGetImageFromCurrentImageContext()!
    }
    
}
