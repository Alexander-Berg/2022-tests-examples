//
//  NetworkTestCase.swift
//  YandexMobileMailAutoTests
//
//  Created by Anastasia Kononova on 10/07/2019.
//  Copyright © 2019 Yandex. All rights reserved.
//

import Foundation
import XCTest

public class NetworkTestCase: XProxyTestCase {
    public enum UseCases: String {
        case pullRoRefresh = "network_metrics_pull_to_refresh"
        case loadMore = "network_metrics_load_more"
        case loadInbox = "network_metrics_load_inbox"
        case loadFolder = "network_metrics_load_folder"
        case sendMessage = "network_metrics_send_message"
        case unsubscribe = "network_metrics_unsubscribe"
        case attachmentUploading = "network_metrics_attachment_uploading"
        case suggectAsYouType = "network_metrics_suggect_as_you_type"
    }

    override var configuration: XProxyConfiguration {
        return .edge
    }

    public override var launchArguments: [String] {
        return super.launchArguments + [CommandLineArguments.useBadNetworkConnection]
    }
}
