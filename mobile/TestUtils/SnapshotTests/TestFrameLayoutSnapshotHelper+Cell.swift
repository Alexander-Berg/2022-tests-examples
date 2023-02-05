import UIKit

extension TestFrameLayoutSnapshotHelper {
    
    public typealias Cell = ViewContainerCell<T>
    public typealias CellInfo = ViewContainerCellInfo<T>
    
    public static func makeViewToVerifyCell(for info: T.UpdateInfo,
        backgroundStyle: TestSnapshotContainer.Style = Default.backgroundStyle,
        insets: UIEdgeInsets = Default.cellInsets) -> UIView
    {
        let cell = Cell()
        let cellInfo = CellInfo(contentInfo: info)
        
        return TestSnapshotHelper.viewToVerifyCell(cell, with: cellInfo, backgroundStyle: backgroundStyle,
            insets: insets)
    }
    
}
