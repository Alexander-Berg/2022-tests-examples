import CoreGraphics

extension CGRect {
    
    public mutating func set(x: CGFloat) {
        origin.x = x
    }
    
    public mutating func set(y: CGFloat) {
        origin.y = y
    }
    
    public mutating func set(origin: CGPoint) {
        self.origin = origin
    }
    
    // MARK: - Equalize
    
    public mutating func equalize(_ attr: FrameHorizontalEdgeAttribute, with otherAttr: FrameHorizontalEdgeAttribute,
        of other: CGRect, constant: CGFloat = 0.0)
    {
        equalize(FrameAttribute(attr), with: FrameAttribute(otherAttr), of: other, constant: constant)
    }
    
    public mutating func equalize(_ attr: FrameHorizontalEdgeAttribute, with otherAttr: FrameVerticalEdgeAttribute,
        of other: CGRect, constant: CGFloat = 0.0)
    {
        equalize(FrameAttribute(attr), with: FrameAttribute(otherAttr), of: other, constant: constant)
    }
    
    public mutating func equalize(_ attr: FrameHorizontalEdgeAttribute, with otherAttr: FrameCenterAttribute,
        of other: CGRect, constant: CGFloat = 0.0)
    {
        equalize(FrameAttribute(attr), with: FrameAttribute(otherAttr), of: other, constant: constant)
    }
    
    public mutating func equalize(_ attr: FrameVerticalEdgeAttribute, with otherAttr: FrameHorizontalEdgeAttribute,
        of other: CGRect, constant: CGFloat = 0.0)
    {
        equalize(FrameAttribute(attr), with: FrameAttribute(otherAttr), of: other, constant: constant)
    }
    
    public mutating func equalize(_ attr: FrameVerticalEdgeAttribute, with otherAttr: FrameVerticalEdgeAttribute,
        of other: CGRect, constant: CGFloat = 0.0)
    {
        equalize(FrameAttribute(attr), with: FrameAttribute(otherAttr), of: other, constant: constant)
    }
    
    public mutating func equalize(_ attr: FrameVerticalEdgeAttribute, with otherAttr: FrameCenterAttribute,
        of other: CGRect, constant: CGFloat = 0.0)
    {
        equalize(FrameAttribute(attr), with: FrameAttribute(otherAttr), of: other, constant: constant)
    }
    
    public mutating func equalize(_ attr: FrameCenterAttribute, with otherAttr: FrameHorizontalEdgeAttribute,
        of other: CGRect, constant: CGFloat = 0.0)
    {
        equalize(FrameAttribute(attr), with: FrameAttribute(otherAttr), of: other, constant: constant)
    }
    
    public mutating func equalize(_ attr: FrameCenterAttribute, with otherAttr: FrameVerticalEdgeAttribute,
        of other: CGRect, constant: CGFloat = 0.0)
    {
        equalize(FrameAttribute(attr), with: FrameAttribute(otherAttr), of: other, constant: constant)
    }
    
    public mutating func equalize(_ attr: FrameCenterAttribute, with otherAttr: FrameCenterAttribute,
        of other: CGRect, constant: CGFloat = 0.0)
    {
        equalize(FrameAttribute(attr), with: FrameAttribute(otherAttr), of: other, constant: constant)
    }
    
    // MARK: - Inset
    
    public mutating func inset(from attr: FrameHorizontalEdgeAttribute, in other: CGRect, by inset: CGFloat = 0.0) {
        let constant = attr == .top ? inset : -inset
        return equalize(attr, with: attr, of: other, constant: constant)
    }
    
    public mutating func inset(from attr: FrameVerticalEdgeAttribute, in other: CGRect, by inset: CGFloat = 0.0) {
        let constant = attr == .left ? inset : -inset
        return equalize(attr, with: attr, of: other, constant: constant)
    }
    
    // MARK: - Attach
    
    public mutating func attach(to attr: FrameHorizontalEdgeAttribute, of other: CGRect, spacing: CGFloat = 0.0) {
        let constant = attr == .top ? -spacing : spacing
        return equalize(attr.opposite, with: attr, of: other, constant: constant)
    }
    
    public mutating func attach(to attr: FrameVerticalEdgeAttribute, of other: CGRect, spacing: CGFloat = 0.0) {
        let constant = attr == .left ? -spacing : spacing
        return equalize(attr.opposite, with: attr, of: other, constant: constant)
    }
    
}

// MARK: Private

private enum FrameAttribute {
    case left
    case right
    case top
    case bottom
    case centerX
    case centerY
    
    init(_ raw: FrameHorizontalEdgeAttribute) {
        switch raw {
        case .top: self = .top
        case .bottom: self = .bottom
        }
    }
    
    init(_ raw: FrameVerticalEdgeAttribute) {
        switch raw {
        case .left: self = .left
        case .right: self = .right
        }
    }
    
    init(_ raw: FrameCenterAttribute) {
        switch raw {
        case .centerX: self = .centerX
        case .centerY: self = .centerY
        }
    }
}

extension CGRect {

    private mutating func equalize(_ attr: FrameAttribute, with otherAttr: FrameAttribute, of other: CGRect,
        constant: CGFloat)
    {
        var targetValue: CGFloat
        var origin = self.origin
        
        switch otherAttr {
        case .left:
            targetValue = other.origin.x
        case .right:
            targetValue = other.origin.x + other.size.width
        case .top:
            targetValue = other.origin.y
        case .bottom:
            targetValue = other.origin.y + other.size.height
        case .centerX:
            targetValue = other.origin.x + other.size.width / 2.0
        case .centerY:
            targetValue = other.origin.y + other.size.height / 2.0
        }
        
        targetValue = targetValue + constant
        
        switch attr {
        case .left:
            origin.x = targetValue
        case .right:
            origin.x = targetValue - self.size.width
        case .top:
            origin.y = targetValue
        case .bottom:
            origin.y = targetValue - self.size.height
        case .centerX:
            origin.x = targetValue - self.size.width / 2.0
        case .centerY:
            origin.y = targetValue - self.size.height / 2.0
        }
        
        self.origin = origin
    }
    
}
