//
//  DirectoryAlertsMock.swift
//  YandexDisk
//
//  Created by Mariya Kachalova on 22/11/2018.
//  Copyright © 2018 Yandex. All rights reserved.
//

import UIAtoms
#if !DEV_TEST
@testable import YandexDisk
#endif

final class DirectoryAlertsMock: NSObject, YDDirectoryActionsAlertProtocol, YDTransferActionsAlertProtocol {
    func dismissCurrentActionSheet() {}

    func hideAlertActivityView() {}

    func showDeleteSuccessfulToast(fileItems _: [YOFileItem]) {}

    private(set) var deleteConfirmationAction: (() -> Void)?
    func showDeleteConfirmationDialog(
        with _: YDPopoverPresentationSource?,
        actionBlock: @escaping () -> Void,
        cancel _: (() -> Void)?
    ) {
        deleteConfirmationAction = actionBlock
    }

    private(set) var showMoreItems: [OperationItem]?
    func showMoreConfirmationDialog(for items: [OperationItem], source _: YDPopoverPresentationSource?) {
        showMoreItems = items
    }

    private(set) var errorAlert: YDDirectoryAlertError?
    func showAlert(forError error: YDDirectoryAlertError) {
        errorAlert = error
    }

    func showAlert(forError error: YDDirectoryAlertError, context _: String?) {
        errorAlert = error
    }

    private(set) var continueAlert: YDDirectoryContinueAlertType?
    private(set) var continueAlertAction: (() -> Void)?
    func show(_ type: YDDirectoryContinueAlertType, action: @escaping () -> Void) {
        continueAlert = type
        continueAlertAction = action
    }

    private(set) var processAlert: YDDirectoryProcessAlertType?
    func showTransferProcess(for type: YDDirectoryProcessAlertType) {
        processAlert = type
    }

    private(set) var dialogType: YDDirectoryDialogAlertType?
    private(set) var dialogTypeAction: (() -> Void)?
    func showDialog(with type: YDDirectoryDialogAlertType, actionBlock: @escaping () -> Void, cancel _: (() -> Void)?) {
        dialogType = type
        dialogTypeAction = actionBlock
    }
}
