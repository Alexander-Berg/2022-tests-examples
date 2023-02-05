import UIKit

public protocol SingleFrameRepresentable: FrameMovable, FrameResizable {
    var frame: CGRect { get set }
}

// MARK: - FrameMovable Default Implementation

extension SingleFrameRepresentable {
    
    @discardableResult
    public func set(x: CGFloat) -> Self {
        frame.set(x: x)
        return self
    }
    
    @discardableResult
    public func set(y: CGFloat) -> Self {
        frame.set(y: y)
        return self
    }
    
    @discardableResult
    public func set(origin: CGPoint) -> Self {
        frame.set(origin: origin)
        return self
    }
    
    @discardableResult
    public func equalize(_ attr: FrameHorizontalEdgeAttribute, with otherAttr: FrameHorizontalEdgeAttribute,
        of other: FrameRepresentable, constant: CGFloat) -> Self
    {
        frame.equalize(attr, with: otherAttr, of: other.frame, constant: constant)
        return self
    }
    
    @discardableResult
    public func equalize(_ attr: FrameHorizontalEdgeAttribute, with otherAttr: FrameVerticalEdgeAttribute,
        of other: FrameRepresentable, constant: CGFloat) -> Self
    {
        frame.equalize(attr, with: otherAttr, of: other.frame, constant: constant)
        return self
    }
    
    @discardableResult
    public func equalize(_ attr: FrameHorizontalEdgeAttribute, with otherAttr: FrameCenterAttribute,
        of other: FrameRepresentable, constant: CGFloat) -> Self
    {
        frame.equalize(attr, with: otherAttr, of: other.frame, constant: constant)
        return self
    }
    
    @discardableResult
    public func equalize(_ attr: FrameVerticalEdgeAttribute, with otherAttr: FrameHorizontalEdgeAttribute,
        of other: FrameRepresentable, constant: CGFloat) -> Self
    {
        frame.equalize(attr, with: otherAttr, of: other.frame, constant: constant)
        return self
    }
    
    @discardableResult
    public func equalize(_ attr: FrameVerticalEdgeAttribute, with otherAttr: FrameVerticalEdgeAttribute,
        of other: FrameRepresentable, constant: CGFloat) -> Self
    {
        frame.equalize(attr, with: otherAttr, of: other.frame, constant: constant)
        return self
    }
    
    @discardableResult
    public func equalize(_ attr: FrameVerticalEdgeAttribute, with otherAttr: FrameCenterAttribute,
        of other: FrameRepresentable, constant: CGFloat) -> Self
    {
        frame.equalize(attr, with: otherAttr, of: other.frame, constant: constant)
        return self
    }
    
    @discardableResult
    public func equalize(_ attr: FrameCenterAttribute, with otherAttr: FrameHorizontalEdgeAttribute,
        of other: FrameRepresentable, constant: CGFloat) -> Self
    {
        frame.equalize(attr, with: otherAttr, of: other.frame, constant: constant)
        return self
    }
    
    @discardableResult
    public func equalize(_ attr: FrameCenterAttribute, with otherAttr: FrameVerticalEdgeAttribute,
        of other: FrameRepresentable, constant: CGFloat) -> Self
    {
        frame.equalize(attr, with: otherAttr, of: other.frame, constant: constant)
        return self
    }
    
    @discardableResult
    public func equalize(_ attr: FrameCenterAttribute, with otherAttr: FrameCenterAttribute,
        of other: FrameRepresentable, constant: CGFloat) -> Self
    {
        frame.equalize(attr, with: otherAttr, of: other.frame, constant: constant)
        return self
    }
    
}

// MARK: - FrameResizable Default Implementation

extension SingleFrameRepresentable {

    @discardableResult
    public func set(width: CGFloat) -> Self {
        frame.set(width: width)
        return self
    }
    
    @discardableResult
    public func set(height: CGFloat) -> Self {
        frame.set(height: height)
        return self
    }
    
    @discardableResult
    public func set(size: CGSize) -> Self {
        frame.set(size: size)
        return self
    }
    
    @discardableResult
    public func stretch(to attr: FrameHorizontalEdgeAttribute, of other: FrameRepresentable, spacing: CGFloat) -> Self {
        frame.stretch(to: attr, of: other.frame, spacing: spacing)
        return self
    }
    
    @discardableResult
    public func stretch(to attr: FrameVerticalEdgeAttribute, of other: FrameRepresentable, spacing: CGFloat) -> Self {
        frame.stretch(to: attr, of: other.frame, spacing: spacing)
        return self
    }
    
}
