//
//  LabelFrameLayoutInfo+BoundedContent.swift
//  MetroToolbox
//
//  Created by Ilya Lobanov on 25/02/2018.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import UIKit

extension LabelFrameLayoutInfo: BoundedContent {
    
    public func boundingSize(maxSize: CGSize) -> CGSize {
        if maxSize.width <= 0 || maxSize.height <= 0 {
            return .zero
        }
    
        guard let text = text, !text.string.isEmpty else { return .zero }
        
        let textStorage: NSTextStorage
        
        switch text {
        case .plain(let string, let font):
            textStorage = NSTextStorage(string: string, attributes: [.font: font])
        case .attributed(let value):
            textStorage = NSTextStorage(attributedString: value)
        }
        
        let textContainer = NSTextContainer(size: maxSize)
        textContainer.maximumNumberOfLines = numberOfLines
        textContainer.lineFragmentPadding = 0.0
    
        let layoutManager = NSLayoutManager()
        layoutManager.addTextContainer(textContainer)
    
        textStorage.addLayoutManager(layoutManager)
    
        return layoutManager.usedRect(for: textContainer).size
    }
    
}

