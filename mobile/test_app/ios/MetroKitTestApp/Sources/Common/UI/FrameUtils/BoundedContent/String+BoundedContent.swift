//
//  String+FrameUtils.swift
//  MetroToolbox
//
//  Created by Ilya Lobanov on 06/02/2018.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import UIKit

extension NSAttributedString: BoundedContent {

    public func boundingSize(maxSize: CGSize) -> CGSize {
        guard !string.isEmpty else { return .zero }
        
        return boundingRect(with: maxSize, options: .usesLineFragmentOrigin, context: nil).size
    }
    
}

public extension String {
    
    public func boundingSize(maxWidth: CGFloat = .greatestFiniteMagnitude, font: UIFont, numberOfLines: Int = 0)
        -> CGSize
    {
        return LabelFrameLayoutInfo(string: self, font: font, numberOfLines: numberOfLines)
            .boundingSize(maxWidth: maxWidth)
    }
    
    public func boundingSize(fixedWidth: CGFloat, font: UIFont, numberOfLines: Int = 0) -> CGSize {
        return LabelFrameLayoutInfo(string: self, font: font, numberOfLines: numberOfLines)
            .boundingSize(fixedWidth: fixedWidth)
    }
    
}

