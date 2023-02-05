//
//  MainViewController+ResourcesDebug.swift
//  MetroKitTestApp
//
//  Created by Ilya Lobanov on 24/03/2018.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import Foundation

extension MainViewController {

    func checkSchemeResources(for scheme: YMLScheme, style: String?) {
        guard let style = style else { return }
    
        guard let originalId = scheme.makeInfoService().services.first?.styles.serviceStyles[style]?.originalIcon else { assert(false); return }
        guard let templateId = scheme.makeInfoService().services.first?.styles.serviceStyles[style]?.templateIcon else { assert(false); return }

        let rm = scheme.makeResourcesManager()
        
        checkImgResources(for: originalId, resourceManager: rm)
        checkImgResources(for: templateId, resourceManager: rm)
    }

    private func checkImgResources(for imgId: YMLSchemeImageResource, resourceManager: YMLSchemeResourcesManager) {
        let imgData = resourceManager.getImageWith(imgId)

        guard let img = (imgData.flatMap { UIImage(data: $0.data) }) else {
            assert(false)
            return
        }
        print(img.size)
    }

}
