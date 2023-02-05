import CoreGraphics

extension CGRect {

    public mutating func set(width: CGFloat) {
        size.width = width
    }
    
    public mutating func set(height: CGFloat) {
        size.height = height
    }
    
    public mutating func set(size: CGSize) {
        self.size = size
    }
    
    // MARK: - Stretch
    
    public mutating func stretch(to attr: FrameHorizontalEdgeAttribute, of other: CGRect, spacing: CGFloat = 0.0) {
        var target: CGFloat
        switch attr {
        case .top: target = other.origin.y
        case .bottom: target = other.origin.y + other.size.height
        }
        
        let stretched = type(of: self).stretch(origin: self.origin.y, size: self.size.height, to: target,
            spacing: spacing, attr: OneDimensionalEdge(attr))
        
        set(y: stretched.origin)
        set(height: stretched.size)
    }
    
    public mutating func stretch(to attr: FrameVerticalEdgeAttribute, of other: CGRect, spacing: CGFloat = 0.0) {
        var target: CGFloat
        switch attr {
        case .left: target = other.origin.x
        case .right: target = other.origin.x + other.size.width
        }
        
        let stretched = type(of: self).stretch(origin: self.origin.x, size: self.size.width, to: target,
            spacing: spacing, attr: OneDimensionalEdge(attr))
        
        set(x: stretched.origin)
        set(width: stretched.size)
    }
    
}

// MARK: - Private

private enum OneDimensionalEdge {
    case min
    case max
    
    init(_ raw: FrameHorizontalEdgeAttribute) {
        switch raw {
        case .top: self = .min
        case .bottom: self = .max
        }
    }
    
    init(_ raw: FrameVerticalEdgeAttribute) {
        switch raw {
        case .left: self = .min
        case .right: self = .max
        }
    }
}

fileprivate extension CGRect {

    private static func stretch(origin: CGFloat, size: CGFloat, to target: CGFloat, spacing: CGFloat,
        attr: OneDimensionalEdge) -> (origin: CGFloat, size: CGFloat)
    {
        var min = origin
        var max = origin + size
        if target > max {
            // 'min' - 'max' - 'target'
            // move 'max' edge
            max = target - spacing
        } else if target > origin {
            // 'min' - 'target' - 'max'
            // attr == .min: move 'max'
            // attr == .max: move 'min'
            switch attr {
            case .min:
                max = target - spacing
            case .max:
                max = target + spacing
            }
        } else {
            // 'target' - 'min' - 'max'
            // move 'min' edge
            min = target + spacing
        }
        assert(max >= min, "incorrect size of stretched frame")
        return (min, max - min)
    }

}
