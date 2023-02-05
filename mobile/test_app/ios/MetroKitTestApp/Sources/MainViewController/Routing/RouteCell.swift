//
//  RouteCell.swift
//  MetroKitTestApp
//
//  Created by Ilya Lobanov on 24/10/2017.
//  Copyright © 2017 Yandex LLC. All rights reserved.
//

import UIKit

final class RouteCell: UICollectionViewCell {
    static let identifier = "\(self)"
    
    var attributedText: NSAttributedString? {
        set {
            label.attributedText = newValue
        }
        get {
            return label.attributedText
        }
    }
    
    private weak var label: UILabel!

    override init(frame: CGRect) {
        super.init(frame: frame)

        contentView.backgroundColor = .white
        contentView.layer.cornerRadius = 8.0
        contentView.layer.borderColor = UIColor.black.withAlphaComponent(0.12).cgColor
        contentView.layer.borderWidth = 1 / UIScreen.main.scale

        label = applyBlock(UILabel()) {
            $0.autoresizingMask = [.flexibleRightMargin, .flexibleBottomMargin]
            $0.numberOfLines = 0
            contentView.addSubview($0)
        }
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    // MARK: UIView
    
    override func layoutSubviews() {
        super.layoutSubviews()
        
        let horPadding = CGFloat(24.0)
        let vertPadding = CGFloat(12.0)
        
        var lframe = bounds
        lframe.origin.x = min(bounds.width, horPadding)
        lframe.origin.y = min(bounds.height, vertPadding)
        lframe.size.width = max(0.0, bounds.width - 2 * horPadding)
        lframe.size.height = max(0.0, bounds.height - 2 * vertPadding)
        
        label.frame = lframe
    }
    
    // MARK: UICollectionViewCell
    
    override var isHighlighted: Bool {
        didSet {
            contentView.backgroundColor = isHighlighted ? UIColor.black.withAlphaComponent(0.05) : .white
        }
    }
}
