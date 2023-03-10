// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/components/folder-manager/edit-folder-component.ts >>>

package com.yandex.xplat.testopithecus

import com.yandex.xplat.common.*
import com.yandex.xplat.eventus.common.*
import com.yandex.xplat.eventus.*
import com.yandex.xplat.mapi.*
import com.yandex.xplat.testopithecus.common.*

public open class EditFolderComponent: MBTComponent {
    open override fun getComponentType(): String {
        return EditFolderComponent.type
    }

    open override fun assertMatches(model: App, application: App): Unit {
        val manageableFolderModel = ManageableFolderFeature.`get`.castIfSupported(model)
        val manageableFolderApplication = ManageableFolderFeature.`get`.castIfSupported(application)
        if (manageableFolderModel != null && manageableFolderApplication != null) {
            val currentFolderNameModel = manageableFolderModel.getCurrentEditedFolderName()
            val currentFolderNameApplication = manageableFolderApplication.getCurrentEditedFolderName()
            assertStringEquals(currentFolderNameModel, currentFolderNameApplication, "Folder name is incorrect")
            val currentParentFolderModel = manageableFolderModel.getCurrentParentFolderForEditedFolder()
            val currentParentFolderApplication = manageableFolderApplication.getCurrentParentFolderForEditedFolder()
            assertStringEquals(currentParentFolderModel, currentParentFolderApplication, "Parent folder name is incorrect")
        }
    }

    open override fun tostring(): String {
        return this.getComponentType()
    }

    companion object {
        @JvmStatic val type: String = "EditFolderComponent"
    }
}

public open class EditFolderActions: MBTComponentActions {
    open override fun getActions(_model: App): YSArray<MBTAction> {
        val actions: YSArray<MBTAction> = mutableListOf()
        return actions
    }

}

