import UIKit

public final class TestFrameLayoutSnapshotHelper<T> where T: UIView, T: UpdatableFrameLayoutProvider {
    
    public typealias Default = TestSnapshotHelper.Default
    
    // MARK: -
    
    public static func makeViewToVerify(for info: T.UpdateInfo, boundingSize: CGSize,
        backgroundStyle: TestSnapshotContainer.Style = Default.backgroundStyle, insets: UIEdgeInsets = Default.insets) 
        -> UIView
    {
        let view = makeView(for: info, boundingSize: boundingSize)
        return TestSnapshotHelper.makeViewToVerify(view, backgroundStyle: backgroundStyle,
            insets: insets)
    }
    
    // MARK: - Private
    
    private static func makeView(for info: T.UpdateInfo, boundingSize: CGSize) -> T {
        let view = T()
        
        let size = T.layout(for: info, boundingSize: boundingSize).size
        
        view.frame.size = size
        view.update(with: info, animation: .none)
        
        return view
    }
    
}

public extension TestFrameLayoutSnapshotHelper {

    static func makeViewToVerify(for info: T.UpdateInfo, width: CGFloat = Default.viewWidth,
        backgroundStyle: TestSnapshotContainer.Style = Default.backgroundStyle,
        insets: UIEdgeInsets = Default.insets) -> UIView
    {
        return makeViewToVerify(for: info, boundingSize: CGSize(width: width, height: .greatestFiniteMagnitude),
            backgroundStyle: backgroundStyle, insets: insets)
    }
    
}

