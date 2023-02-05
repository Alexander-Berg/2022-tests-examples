import UIKit

public extension TestSnapshotHelper.Default {
    static var cellInsets: UIEdgeInsets { return UIEdgeInsets(top: inset, left: 0, bottom: inset, right: 0) }
}

extension TestSnapshotHelper {

    public static func viewToVerifyCell<T: CommonTableCellInfo>(_ cell: UITableViewCell & CommonTableCell, with info: T,
        backgroundStyle: TestSnapshotContainer.Style = Default.backgroundStyle,
        insets: UIEdgeInsets = Default.cellInsets) -> UIView
    {
        let width = Default.viewWidth
        let height = type(of: info).cellType().height(for: info, width: width)
        cell.frame = CGRect(width: width, height: height)
        cell.update(with: info, animation: .none)
        
        return makeViewToVerify(cell, backgroundStyle: backgroundStyle, insets: insets)
    }

}

