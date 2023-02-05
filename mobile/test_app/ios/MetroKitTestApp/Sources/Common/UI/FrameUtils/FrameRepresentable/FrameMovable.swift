import CoreGraphics

public protocol FrameMovable: class, FrameRepresentable {
    @discardableResult
    func set(x: CGFloat) -> Self
    
    @discardableResult
    func set(y: CGFloat) -> Self
    
    @discardableResult
    func set(origin: CGPoint) -> Self
    
    @discardableResult
    func equalize(_ attr: FrameHorizontalEdgeAttribute, with otherAttr: FrameHorizontalEdgeAttribute,
        of other: FrameRepresentable, constant: CGFloat) -> Self
    
    @discardableResult
    func equalize(_ attr: FrameHorizontalEdgeAttribute, with otherAttr: FrameVerticalEdgeAttribute,
        of other: FrameRepresentable, constant: CGFloat) -> Self
    
    @discardableResult
    func equalize(_ attr: FrameHorizontalEdgeAttribute, with otherAttr: FrameCenterAttribute,
        of other: FrameRepresentable, constant: CGFloat) -> Self
    
    @discardableResult
    func equalize(_ attr: FrameVerticalEdgeAttribute, with otherAttr: FrameHorizontalEdgeAttribute,
        of other: FrameRepresentable, constant: CGFloat) -> Self
    
    @discardableResult
    func equalize(_ attr: FrameVerticalEdgeAttribute, with otherAttr: FrameVerticalEdgeAttribute,
        of other: FrameRepresentable, constant: CGFloat) -> Self
    
    @discardableResult
    func equalize(_ attr: FrameVerticalEdgeAttribute, with otherAttr: FrameCenterAttribute,
        of other: FrameRepresentable, constant: CGFloat) -> Self
    
    @discardableResult
    func equalize(_ attr: FrameCenterAttribute, with otherAttr: FrameHorizontalEdgeAttribute,
        of other: FrameRepresentable, constant: CGFloat) -> Self
    
    @discardableResult
    func equalize(_ attr: FrameCenterAttribute, with otherAttr: FrameVerticalEdgeAttribute,
        of other: FrameRepresentable, constant: CGFloat) -> Self
    
    @discardableResult
    func equalize(_ attr: FrameCenterAttribute, with otherAttr: FrameCenterAttribute,
        of other: FrameRepresentable, constant: CGFloat) -> Self
}

public extension FrameMovable {

    @discardableResult
    func inset(from attr: FrameHorizontalEdgeAttribute, in other: FrameRepresentable, by inset: CGFloat) -> Self {
        let constant = attr == .top ? inset : -inset
        return equalize(attr, with: attr, of: other, constant: constant)
    }
    
    @discardableResult
    func inset(from attr: FrameVerticalEdgeAttribute, in other: FrameRepresentable, by inset: CGFloat) -> Self {
        let constant = attr == .left ? inset : -inset
        return equalize(attr, with: attr, of: other, constant: constant)
    }
    
    @discardableResult
    func attach(to attr: FrameHorizontalEdgeAttribute, of other: FrameRepresentable, spacing: CGFloat) -> Self {
        let constant = attr == .top ? -spacing : spacing
        return equalize(attr.opposite, with: attr, of: other, constant: constant)
    }
    
    @discardableResult
    func attach(to attr: FrameVerticalEdgeAttribute, of other: FrameRepresentable, spacing: CGFloat) -> Self {
        let constant = attr == .left ? -spacing : spacing
        return equalize(attr.opposite, with: attr, of: other, constant: constant)
    }
    
}

// MARK: Default parameter values support
public extension FrameMovable {

    @discardableResult
    func equalize(_ attr: FrameHorizontalEdgeAttribute, with otherAttr: FrameHorizontalEdgeAttribute,
        of other: FrameRepresentable) -> Self
    {
        return equalize(attr, with: otherAttr, of: other, constant: 0.0)
    }
    
    @discardableResult
    func equalize(_ attr: FrameHorizontalEdgeAttribute, with otherAttr: FrameVerticalEdgeAttribute,
        of other: FrameRepresentable) -> Self
    {
        return equalize(attr, with: otherAttr, of: other, constant: 0.0)
    }
    
    @discardableResult
    func equalize(_ attr: FrameHorizontalEdgeAttribute, with otherAttr: FrameCenterAttribute,
        of other: FrameRepresentable) -> Self
    {
        return equalize(attr, with: otherAttr, of: other, constant: 0.0)
    }
    
    @discardableResult
    func equalize(_ attr: FrameVerticalEdgeAttribute, with otherAttr: FrameHorizontalEdgeAttribute,
        of other: FrameRepresentable) -> Self
    {
        return equalize(attr, with: otherAttr, of: other, constant: 0.0)
    }
    
    @discardableResult
    func equalize(_ attr: FrameVerticalEdgeAttribute, with otherAttr: FrameVerticalEdgeAttribute,
        of other: FrameRepresentable) -> Self
    {
        return equalize(attr, with: otherAttr, of: other, constant: 0.0)
    }
    
    @discardableResult
    func equalize(_ attr: FrameVerticalEdgeAttribute, with otherAttr: FrameCenterAttribute,
        of other: FrameRepresentable) -> Self
    {
        return equalize(attr, with: otherAttr, of: other, constant: 0.0)
    }
    
    @discardableResult
    func equalize(_ attr: FrameCenterAttribute, with otherAttr: FrameHorizontalEdgeAttribute,
        of other: FrameRepresentable) -> Self
    {
        return equalize(attr, with: otherAttr, of: other, constant: 0.0)
    }
    
    @discardableResult
    func equalize(_ attr: FrameCenterAttribute, with otherAttr: FrameVerticalEdgeAttribute,
        of other: FrameRepresentable) -> Self
    {
        return equalize(attr, with: otherAttr, of: other, constant: 0.0)
    }
    
    @discardableResult
    func equalize(_ attr: FrameCenterAttribute, with otherAttr: FrameCenterAttribute,
        of other: FrameRepresentable) -> Self
    {
        return equalize(attr, with: otherAttr, of: other, constant: 0.0)
    }
    
    @discardableResult
    func inset(from attr: FrameHorizontalEdgeAttribute, in other: FrameRepresentable) -> Self {
        return inset(from: attr, in: other, by: 0.0)
    }
    
    @discardableResult
    func inset(from attr: FrameVerticalEdgeAttribute, in other: FrameRepresentable) -> Self {
        return inset(from: attr, in: other, by: 0.0)
    }
    
    @discardableResult
    func attach(to attr: FrameHorizontalEdgeAttribute, of other: FrameRepresentable) -> Self {
        return attach(to: attr, of: other, spacing: 0.0)
    }
    
    @discardableResult
    func attach(to attr: FrameVerticalEdgeAttribute, of other: FrameRepresentable) -> Self {
        return attach(to: attr, of: other, spacing: 0.0)
    }
    
}
