//
//  MapItemsCommonLayout.swift
//  YandexTransport
//
//  Created by Aleksey Fedotov on 19.05.16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import Foundation

class MapItemsCommonLayout: NSObject, MapItemsLayout {
    
    struct Static {
        
        static let horizontalMargin: CGFloat = 8.0
        static let bottomMargin: CGFloat = 16.0
        static let topMargin: CGFloat = 16.0
        
        static let defaultItemsSpacing: CGFloat = 12.0
        
        static let iPadBottomButtonsBlockWidth: CGFloat = 375.0
        
        static let textButtonsContentInset: CGFloat = 27.0
    }
    
    weak var manager: MapItemsLayoutManager?
    
    var bounds = CGRect.zero {
        didSet {
            invalidate()
        }
    }
    
    func items() -> Set<MapItemsLayoutItem> {
        return Set<MapItemsLayoutItem>(mapItems.keys)
    }
    
    func attributesForItem(_ item: MapItemsLayoutItem) -> MapItemsLayoutAttributes {
        
        if let attributes = mapItems[item] {
            return attributes
        } else {
            return MapItemsLayoutAttributes(frame: CGRect.zero, hidden: true)
        }
    }
    
    internal var mapItems = [MapItemsLayoutItem: MapItemsLayoutAttributes]()
    
    internal let buttonsSize = CGSize(
        width: MapItemsLayoutButtonView.buttonSize, height: MapItemsLayoutButtonView.buttonSize)
    
    internal func attributesForItem(id: MapItemsIdentifier) -> MapItemsLayoutAttributes {
        return attributesForItem(MapItemsLayoutItem(identifier: id.value))
    }
    
    internal func setAttributesForItem(_ id: MapItemsIdentifier, _ attrs: MapItemsLayoutAttributes) {
        
        let key = MapItemsLayoutItem(identifier: id.value)
        if mapItems.keys.contains(key) {
            mapItems[key] = attrs
        }
    }
    
    internal func itemFrame(id: MapItemsIdentifier) -> CGRect {
        return attributesForItem(id: id).frame
    }
    
    internal func setItemHidden(id: MapItemsIdentifier, hidden: Bool) {
        
        let attributes = attributesForItem(MapItemsLayoutItem(identifier: id.value))
        attributes.hidden = hidden
        setAttributesForItem(id, attributes)
    }
    
    internal func addItem(id: MapItemsIdentifier) {
        mapItems[MapItemsLayoutItem(identifier: id.value)] = MapItemsLayoutAttributes()
    }
    
    internal func removeItem(id: MapItemsIdentifier) {
        mapItems.removeValue(forKey: MapItemsLayoutItem(identifier: id.value))
    }
    
    func invalidate() {
        
        if manager == nil {
            return
        }
        
        layoutVerticalOrientation()
    }
    
    internal func layoutVerticalOrientation() {
        
    }
    
}
