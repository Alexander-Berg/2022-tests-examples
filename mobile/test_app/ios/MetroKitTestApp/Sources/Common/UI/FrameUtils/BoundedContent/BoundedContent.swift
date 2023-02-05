//
//  BoundedContent.swift
//  MetroToolbox
//
//  Created by Ilya Lobanov on 07/02/2018.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import CoreGraphics

public protocol BoundedContent {
    func boundingSize(maxSize: CGSize) -> CGSize
}

public extension BoundedContent {

    func boundingSize(maxWidth: CGFloat, maxHeight: CGFloat) -> CGSize {
        return boundingSize(maxSize: CGSize(width: maxWidth, height: maxHeight))
    }

    func boundingSize(maxWidth: CGFloat = .greatestFiniteMagnitude) -> CGSize {
        return boundingSize(maxWidth: maxWidth, maxHeight: .greatestFiniteMagnitude)
    }
    
    func boundingSize(fixedWidth: CGFloat, maxHeight: CGFloat = .greatestFiniteMagnitude) -> CGSize {
        var size = boundingSize(maxWidth: fixedWidth, maxHeight: maxHeight)
        size.width = fixedWidth
        return size
    }

    func boundingSize(maxWidth: CGFloat = .greatestFiniteMagnitude, fixedHeight: CGFloat) -> CGSize {
        var size = boundingSize(maxWidth: maxWidth, maxHeight: fixedHeight)
        size.height = fixedHeight
        return size
    }

}
