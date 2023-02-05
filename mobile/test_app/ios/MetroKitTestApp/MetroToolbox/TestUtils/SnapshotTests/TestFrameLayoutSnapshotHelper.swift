import UIKit

public final class TestFrameLayoutSnapshotHelper<T> where T: UIView, T: FrameLayoutProvider {
    
    public typealias Default = TestSnapshotHelper.Default
    
    // MARK: -
    
    public static func makeViewToVerify(for info: T.Info, boundingSize: CGSize,
        backgroundStyle: TestSnapshotContainer.Style = Default.backgroundStyle, insets: UIEdgeInsets = Default.insets) 
        -> UIView
    {
        let view = makeView(for: info, boundingSize: boundingSize)
        return TestSnapshotHelper.makeViewToVerify(view, backgroundStyle: backgroundStyle,
            insets: insets)
    }
    
    // MARK: - Private
    
    private static func makeView(for info: T.Info, boundingSize: CGSize) -> T {
        let view = T()
        
        let size = T.layout(for: info, boundingSize: boundingSize).size
        
        view.frame.size = size
        view.update(with: info, animated: false)
        
        return view
    }
    
}

public extension TestFrameLayoutSnapshotHelper {

    public static func makeViewToVerify(for info: T.Info, width: CGFloat = Default.viewWidth,
        backgroundStyle: TestSnapshotContainer.Style = Default.backgroundStyle,
        insets: UIEdgeInsets = Default.insets) -> UIView
    {
        return makeViewToVerify(for: info, boundingSize: CGSize(width: width, height: .greatestFiniteMagnitude),
            backgroundStyle: backgroundStyle, insets: insets)
    }
    
}

