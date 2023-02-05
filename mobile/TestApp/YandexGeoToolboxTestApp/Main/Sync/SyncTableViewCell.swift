//
//  SyncTableViewCell.swift
//  YandexGeoSync
//
//  Created by Ilya Lobanov on 01/02/16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import Foundation


class SyncTableViewCell: UITableViewCell {
    
    struct Info {
        var title: String
    }
    
    private weak var label: UILabel!
    
    override init(style: UITableViewCellStyle, reuseIdentifier: String?) {
        let label = UILabel()
        label.font = Static.FONT
        label.numberOfLines = 0
        self.label = label
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        
        contentView.addSubview(label)
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        var frame = contentView.bounds
        frame.origin = CGPoint(x: Static.INSET, y: Static.INSET)
        frame.size.width -= 2.0 * Static.INSET
        frame.size.height -= 2.0 * Static.INSET
        
        label.frame = frame.integral
    }
    
    // MARK: Public
    
    class func height(width: CGFloat, info: Info) -> CGFloat {
        let height = info.title.heightWithConstrainedWidth(width: width - 2.0 * Static.INSET, font: Static.FONT)
        return ceil(height + 2.0 * Static.INSET)
    }
    
    func update(info: Info) {
        label.text = info.title
        layoutIfNeeded()
    }
    
    // MARK: Private Static
    
    private struct Static {
        static let FONT = UIFont.systemFont(ofSize: 12.0)
        static let INSET: CGFloat = 16.0
    }
}

private extension String {
    
    func heightWithConstrainedWidth(width: CGFloat, font: UIFont) -> CGFloat {
        let constraintRect = CGSize(width: width, height: CGFloat.greatestFiniteMagnitude)
        
        let boundingBox = self.boundingRect(with: constraintRect, options: NSStringDrawingOptions.usesLineFragmentOrigin, attributes: [NSFontAttributeName: font], context: nil)
        
        return boundingBox.height
    }
}
