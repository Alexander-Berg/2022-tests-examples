//
//  FrameLayout.swift
//  ContactsUltra
//
//  Created by Ilya Lobanov on 08/01/2018.
//  Copyright © 2018 Ilya Lobanov. All rights reserved.
//

import UIKit

public protocol FrameLayout {
    var size: CGSize { get }
}

public protocol UpdatableFrameLayoutProvider: Updatable, FrameLayoutProvider where UpdateInfo == LayoutInfo {}

public protocol Updatable: class {
    associatedtype UpdateInfo
    
    var info: UpdateInfo? { get }
    
    func update(with info: UpdateInfo, animation: AnimationInfo)
}

public protocol FrameLayoutProvider: class {
    associatedtype LayoutInfo
    associatedtype Layout: FrameLayout
    
    // if some dimension isn't limited, CGFloat.greatestFiniteMagnitude should be used
    // e.g. CGSize(width: 48.0, height: .greatestFiniteMagnitude)
    static func layout(for info: LayoutInfo, boundingSize: CGSize) -> Layout
}

public extension FrameLayoutProvider {
    
    public static func layout(for info: LayoutInfo, width: CGFloat) -> Layout {
        return layout(for: info, boundingSize: CGSize(width: width, height: .greatestFiniteMagnitude))
    }
    
}

public extension FrameLayoutProvider where Self: Updatable, Self.LayoutInfo == Self.UpdateInfo {

    public func layoutForCurrentInfo(boundingSize: CGSize) -> Layout? {
        return info.map {
            type(of: self).layout(for: $0, boundingSize: boundingSize)
        }
    }
    
    public func layoutForCurrentInfo(width: CGFloat) -> Layout? {
        return info.map {
            type(of: self).layout(for: $0, width: width)
        }
    }

}

public extension FrameLayoutProvider where Self: UIView & Updatable, Self.LayoutInfo == Self.UpdateInfo {
    
    public func layoutForCurrentInfo() -> Layout? {
        return info.map {
            type(of: self).layout(for: $0, boundingSize: bounds.size)
        }
    }
    
}
