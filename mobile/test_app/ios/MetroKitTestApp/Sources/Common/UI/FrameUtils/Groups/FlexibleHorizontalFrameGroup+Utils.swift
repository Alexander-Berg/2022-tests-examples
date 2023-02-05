import UIKit

extension FlexibleHorizontalFrameGroup.Item {
    
    public init(string: String, font: UIFont, numberOfLines: Int = 0, maxWidth: CGFloat = .greatestFiniteMagnitude,
        priority: Priority = .low)
    {
        let info = LabelFrameLayoutInfo(string: string, font: font, numberOfLines: numberOfLines)
        self.init(content: info, maxWidth: maxWidth, priority: priority)
    }
    
}
