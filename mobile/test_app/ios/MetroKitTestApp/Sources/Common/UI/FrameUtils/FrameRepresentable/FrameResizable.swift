import CoreGraphics

public protocol FrameResizable: class, FrameRepresentable {
    @discardableResult
    func set(width: CGFloat) -> Self
    
    @discardableResult
    func set(height: CGFloat) -> Self

    @discardableResult
    func set(size: CGSize) -> Self
    
    @discardableResult
    func stretch(to attr: FrameHorizontalEdgeAttribute, of other: FrameRepresentable, spacing: CGFloat) -> Self
    
    @discardableResult
    func stretch(to attr: FrameVerticalEdgeAttribute, of other: FrameRepresentable, spacing: CGFloat) -> Self

}

// MARK: default parameter values support
public extension FrameResizable {

    @discardableResult
    func stretch(to attr: FrameHorizontalEdgeAttribute, of other: FrameRepresentable) -> Self {
        return stretch(to: attr, of: other, spacing: 0.0)
    }
    
    @discardableResult
    func stretch(to attr: FrameVerticalEdgeAttribute, of other: FrameRepresentable) -> Self {
        return stretch(to: attr, of: other, spacing: 0.0)
    }
    
}

// MARK: Double parameters support
public extension FrameResizable {
    
    @discardableResult
    func set(width: Double) -> Self {
        return set(width: CGFloat(width))
    }
    
    @discardableResult
    func set(height: Double) -> Self {
        return set(height: CGFloat(height))
    }
    
    @discardableResult
    func stretch(to attr: FrameHorizontalEdgeAttribute, of other: FrameRepresentable, spacing: Double) -> Self {
        return stretch(to: attr, of: other, spacing: CGFloat(spacing))
    }
    
    @discardableResult
    func stretch(to attr: FrameVerticalEdgeAttribute, of other: FrameRepresentable, spacing: Double) -> Self {
        return stretch(to: attr, of: other, spacing: CGFloat(spacing))
    }
    
}

