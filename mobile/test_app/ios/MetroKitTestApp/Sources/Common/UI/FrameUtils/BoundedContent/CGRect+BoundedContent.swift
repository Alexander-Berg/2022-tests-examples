//
//  CGRect+BoundedContent.swift
//  MetroToolbox
//
//  Created by Ilya Lobanov on 07/02/2018.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import CoreGraphics

extension CGRect: BoundedContent {

    public func boundingSize(maxSize: CGSize) -> CGSize {
        return size.intersection(maxSize)
    }
    
}

extension CGSize: BoundedContent {

    public func boundingSize(maxSize: CGSize) -> CGSize {
        return intersection(maxSize)
    }
    
}
