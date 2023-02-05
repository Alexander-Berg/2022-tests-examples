//
//  BoundedContent+FrameRepresentable.swift
//  MetroToolbox
//
//  Created by Ilya Lobanov on 11/02/2018.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import UIKit

public extension CGRect {
    
    public mutating func fitHeight(for content: BoundedContent, maxHeight: CGFloat = .greatestFiniteMagnitude) {
        size.height = content.boundingSize(fixedWidth: frame.width, maxHeight: maxHeight).height
    }

    public mutating func fitWidth(for content: BoundedContent, maxWidth: CGFloat = .greatestFiniteMagnitude) {
        size.width = content.boundingSize(maxWidth: maxWidth, fixedHeight: frame.height).width
    }

    public mutating func fitSize(for content: BoundedContent, maxSize: CGSize) {
        size = content.boundingSize(maxSize: maxSize)
    }

    // MARK: - Helpers

    public mutating func fitHeight(for string: String, font: UIFont, numberOfLines: Int = 0) {
        fitHeight(for: LabelFrameLayoutInfo(string: string, font: font, numberOfLines: numberOfLines))
    }

    public mutating func fitWidth(
        for string: String, font: UIFont, numberOfLines: Int = 0, maxWidth: CGFloat = .greatestFiniteMagnitude)
    {
        fitWidth(for: LabelFrameLayoutInfo(string: string, font: font, numberOfLines: numberOfLines), maxWidth: maxWidth)
    }

    public mutating func fitSize(
        for string: String, font: UIFont, numberOfLines: Int = 0,
        maxSize: CGSize = CGSize(width: CGFloat.greatestFiniteMagnitude, height: CGFloat.greatestFiniteMagnitude))
    {
        fitSize(for: LabelFrameLayoutInfo(string: string, font: font, numberOfLines: numberOfLines), maxSize: maxSize)
    }
    
}

public extension SingleFrameRepresentable {
    
    @discardableResult
    public func fitHeight(for content: BoundedContent, maxHeight: CGFloat = .greatestFiniteMagnitude) -> Self {
        frame.fitHeight(for: content, maxHeight: maxHeight)
        return self
    }

    @discardableResult
    public func fitWidth(for content: BoundedContent, maxWidth: CGFloat = .greatestFiniteMagnitude) -> Self {
        frame.fitWidth(for: content, maxWidth: maxWidth)
        return self
    }

    @discardableResult
    public func fitSize(for content: BoundedContent, maxSize: CGSize) -> Self
    {
        frame.fitSize(for: content, maxSize: maxSize)
        return self
    }

    // MARK: - Helpers

    @discardableResult
    public func fitHeight(for string: String, font: UIFont, numberOfLines: Int = 0) -> Self {
        frame.fitHeight(for: string, font: font, numberOfLines: numberOfLines)
        return self
    }

    @discardableResult
    public func fitWidth(
        for string: String, font: UIFont, numberOfLines: Int = 0, maxWidth: CGFloat = .greatestFiniteMagnitude) -> Self
    {
        frame.fitWidth(for: string, font: font, numberOfLines: numberOfLines, maxWidth: maxWidth)
        return self
    }

    @discardableResult
    public func fitSize(
        for string: String, font: UIFont, numberOfLines: Int = 0,
        maxSize: CGSize = CGSize(width: CGFloat.greatestFiniteMagnitude, height: CGFloat.greatestFiniteMagnitude)) -> Self
    {
        frame.fitSize(for: string, font: font, numberOfLines: numberOfLines, maxSize: maxSize)
        return self
    }
    
}
