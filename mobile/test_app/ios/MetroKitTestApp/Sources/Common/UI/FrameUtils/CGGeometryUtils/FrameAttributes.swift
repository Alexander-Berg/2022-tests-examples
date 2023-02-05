import Foundation

public enum FrameVerticalEdgeAttribute {
    case left
    case right
}

public enum FrameHorizontalEdgeAttribute {
    case top
    case bottom
}

public enum FrameCenterAttribute {
    case centerX
    case centerY
}

// MARK: - Helpers

public extension FrameVerticalEdgeAttribute {
    
    public var opposite: FrameVerticalEdgeAttribute {
        switch self {
        case .left: return .right
        case .right: return .left
        }
    }
    
}

public extension FrameHorizontalEdgeAttribute {
    
     public var opposite: FrameHorizontalEdgeAttribute {
        switch self {
        case .top: return .bottom
        case .bottom: return .top
        }
    }
    
}
