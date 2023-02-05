//
//  MapItemsMinimalisticLayout.swift
//  YandexTransport
//
//  Created by Aleksey Fedotov on 19.05.16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import Foundation

class MapItemsMinimalisticLayout: MapItemsCommonLayout {
    
    struct Deps {
        let zoomInId: MapItemsIdentifier
        let zoomOutId: MapItemsIdentifier
        
        init(zoomInId: MapItemsIdentifier, zoomOutId: MapItemsIdentifier)
        {
            self.zoomInId = zoomInId
            self.zoomOutId = zoomOutId
        }
        
        func make() -> MapItemsMinimalisticLayout {
            return MapItemsMinimalisticLayout(self)
        }
    }
    
    private let deps: Deps
    
    private var zoomInId: MapItemsIdentifier { get { return deps.zoomInId } }
    private var zoomOutId: MapItemsIdentifier { get { return deps.zoomOutId } }
    
    init(_ deps: Deps) {
        self.deps = deps
        
        super.init()
        
        addItem(id: zoomInId)
        addItem(id: zoomOutId)
    }
    
    internal override func layoutVerticalOrientation() {
        super.layoutVerticalOrientation()
        
        // right-side buttons
        
        var rightSideButtonsFrames = [itemFrame(id: zoomInId), itemFrame(id: zoomOutId)]
        
        rightSideButtonsFrames = bounds.setSize(rightSideButtonsFrames, size: buttonsSize)
        rightSideButtonsFrames = bounds.setSpacing(Static.defaultItemsSpacing, type: .vertical, items: rightSideButtonsFrames)
        rightSideButtonsFrames = bounds.centerInContainer(.vertical, margin: -8.0, items: rightSideButtonsFrames)
        rightSideButtonsFrames = bounds.setMargin(Static.horizontalMargin, from: .right, items: rightSideButtonsFrames)
        
        setAttributesForItem(zoomInId, MapItemsLayoutAttributes(frame: rightSideButtonsFrames[0]))
        setAttributesForItem(zoomOutId, MapItemsLayoutAttributes(frame: rightSideButtonsFrames[1]))
    }
}
