//
//  TestUploadFileBuilder.swift
//  YandexDiskTests
//
//  Created by Mariya Kachalova on 31.08.2018.
//  Copyright © 2018 Yandex. All rights reserved.
//

#if !DEV_TEST
@testable import YandexDisk
#endif

final class TestUploadFileBuilder {
    private let urlString: String?
    private let type: YDUploadFileType
    private var assetDate: Date?
    private var assetId: String?
    private var parentUrlString = "parent1"
    private var remoteUrlString = "http://1.jpg"
    private var localUrlString = "file://2.jpg"
    private var failReason: String?
    private var resourceType = YDUploadFileResourceType.default
    private var size: UInt64 = 12
    private var uploadedSize: UInt64 = 5
    private var eTime: Date? = Date(timeIntervalSince1970: 1)
    private var contentType: YDUploadFileContentType = .image

    init(_ urlString: String = "url", type: YDUploadFileType = .default) {
        self.urlString = urlString
        self.type = type
    }

    init(assetId: String, type: YDUploadFileType = .default) {
        self.urlString = nil
        self.assetId = assetId
        self.type = type
    }

    func setParentURLString(_ urlString: String) -> TestUploadFileBuilder {
        parentUrlString = urlString
        return self
    }

    func setParentUrl(_ url: URL) -> TestUploadFileBuilder {
        return setParentURLString(url.absoluteString)
    }

    func setAssetDate(_ assetDate: Date) -> TestUploadFileBuilder {
        self.assetDate = assetDate
        return self
    }

    func setFailReason(_ failReason: String) -> TestUploadFileBuilder {
        self.failReason = failReason
        return self
    }

    func setRemoteUrlString(_ urlString: String) -> TestUploadFileBuilder {
        remoteUrlString = urlString
        return self
    }

    func setRemoteUrl(_ url: URL) -> TestUploadFileBuilder {
        return setRemoteUrlString(url.absoluteString)
    }

    func setLocalUrlString(_ urlString: String) -> TestUploadFileBuilder {
        localUrlString = urlString
        return self
    }

    func setAssetId(_ assetId: String?) -> TestUploadFileBuilder {
        self.assetId = assetId
        return self
    }

    func setResourceType(_ resourceType: YDUploadFileResourceType) -> TestUploadFileBuilder {
        self.resourceType = resourceType
        return self
    }

    func setSize(_ size: UInt64) -> TestUploadFileBuilder {
        self.size = size
        return self
    }

    func setUploadedSize(_ uploadedSize: UInt64) -> TestUploadFileBuilder {
        self.uploadedSize = uploadedSize
        return self
    }

    func setETime(_ eTime: Date?) -> TestUploadFileBuilder {
        self.eTime = eTime
        return self
    }

    func setContentType(_ contentType: YDUploadFileContentType) -> TestUploadFileBuilder {
        self.contentType = contentType
        return self
    }

    func build() -> YOUploadFile {
        let uploadFile: YOUploadFile
        if let urlString = urlString {
            uploadFile = YOUploadFile(url: URL(string: urlString)!, type: type, resourceType: resourceType)
        } else {
            let url = UploadFileBuilderUrlBuilder.uploadFileUrl(
                assetIdentifier: assetId!,
                type: type,
                resourceType: resourceType,
                urlSuffix: ""
            )
            uploadFile = YOUploadFile(url: url, type: type, resourceType: resourceType)
        }
        uploadFile.assetIdentifier = assetId
        uploadFile.remoteURL = URL(string: remoteUrlString)
        uploadFile.localURL = URL(string: localUrlString)
        uploadFile.parentURL = URL(string: parentUrlString)!
        uploadFile.size = size
        uploadFile.uploadedSize = uploadedSize
        uploadFile.eTime = eTime
        uploadFile.assetDate = assetDate ?? Date(timeIntervalSince1970: 2)
        uploadFile.contentType = contentType
        uploadFile.failReason = failReason
        return uploadFile
    }
}
