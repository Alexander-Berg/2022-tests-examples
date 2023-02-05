import UIKit

public final class TestSnapshotHelper {
    
    public struct Default {
        public static let viewWidth = UIScreen.main.bounds.width
        public static let inset: CGFloat = 5
        public static let insets = UIEdgeInsets(top: inset, left: inset, bottom: inset, right: inset)
        public static let backgroundStyle = TestSnapshotContainer.Style.great
    }
    
    public static func makeViewToVerify(_ view: UIView,
        backgroundStyle: TestSnapshotContainer.Style = Default.backgroundStyle, insets: UIEdgeInsets = Default.insets)
        -> UIView
    {
        let container = TestSnapshotContainer(frame: .zero, style: backgroundStyle)
        let width = view.bounds.width + insets.left + insets.right
        let height = view.bounds.height + insets.top + insets.bottom
        
        container.frame = CGRect(width: width, height: height)
        container.addSubview(view)
        
        view.frame.origin = CGPoint(x: insets.left, y: insets.top)
        
        return container
    }
    
}

public extension TestSnapshotHelper {
    
    static func makeViewToVerify(_ view: UIView,
        backgroundStyle: TestSnapshotContainer.Style = Default.backgroundStyle,
        inset: CGFloat) -> UIView
    {
        let insets = UIEdgeInsets(top: inset, left: inset, bottom: inset, right: inset)
        return makeViewToVerify(view, backgroundStyle: backgroundStyle, insets: insets)
    }
    
}
