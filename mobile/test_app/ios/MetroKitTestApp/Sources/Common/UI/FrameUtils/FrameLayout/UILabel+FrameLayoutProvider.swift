//
//  CommonLabel.swift
//  MetroToolbox
//
//  Created by Ilya Lobanov on 21/02/2018.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import UIKit

public struct LabelFrameLayoutInfo {
    public enum Text {
        case plain(string: String, font: UIFont)
        case attributed(NSAttributedString)
        
        public init(string: String, font: UIFont = Default.font) {
            self = .plain(string: string, font: font)
        }
        
        public init(_ attributed: NSAttributedString) {
            self = .attributed(attributed)
        }
        
        public var string: String {
            switch self {
            case .plain(let string, _):
                return string
            case .attributed(let value):
                return value.string
            }
        }
    }
    
    public var text: Text?
    public var numberOfLines: Int
    
    public init(text: Text?, numberOfLines: Int = Default.numberOfLines) {
        self.text = text
        self.numberOfLines = numberOfLines
    }
    
    public init(string: String, font: UIFont = Default.font, numberOfLines: Int = Default.numberOfLines) {
        self.init(text: Text(string: string, font: font), numberOfLines: numberOfLines)
    }
    
    public init(attrString: NSAttributedString, numberOfLines: Int = Default.numberOfLines) {
        self.init(text: Text(attrString), numberOfLines: numberOfLines)
    }
    
    // MARK: - Default values
    
    public struct Default {
        public static let font: UIFont = UIFont.systemFont(ofSize: UIFont.systemFontSize)
        public static let numberOfLines: Int = 1
    }
}

public struct LabelFrameLayout: FrameLayout {
    public var size: CGSize
}

extension UILabel: UpdatableFrameLayoutProvider {
    
    public var info: LabelFrameLayoutInfo? {
        if let text = attributedText {
            return LabelFrameLayoutInfo(attrString: text, numberOfLines: numberOfLines)
        } else if let text = text {
            return LabelFrameLayoutInfo(string: text, font: font, numberOfLines: numberOfLines)
        } else {
            return nil
        }
    }
    
    public func update(with info: LabelFrameLayoutInfo, animation: AnimationInfo) {
        if let infoText = info.text {
            switch infoText {
            case .plain(let string, let font):
                attributedText = nil
                text = string
                self.font = font
            case .attributed(let value):
                text = nil
                attributedText = value
            }
        } else {
            attributedText = nil
            text = nil
            font = LabelFrameLayoutInfo.Default.font
        }
        
        numberOfLines = info.numberOfLines
    }
    
    public static func layout(for info: LabelFrameLayoutInfo, boundingSize: CGSize) -> LabelFrameLayout {
        return Layout(size: info.boundingSize(maxWidth: boundingSize.width, maxHeight: boundingSize.height).integral)
    }
    
}
