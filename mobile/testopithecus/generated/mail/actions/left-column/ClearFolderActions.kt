// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/actions/left-column/clear-folder-actions.ts >>>

package com.yandex.xplat.testopithecus

import com.yandex.xplat.common.*
import com.yandex.xplat.eventus.common.*
import com.yandex.xplat.eventus.*
import com.yandex.xplat.mapi.*
import com.yandex.xplat.testopithecus.common.*

public open class ClearTrashFolderAction(private var confirmDeletionIfNeeded: Boolean = true): BaseSimpleAction<ClearFolderInFolderList, MBTComponent>(ClearTrashFolderAction.type) {
    open override fun events(): YSArray<EventusEvent> {
        return mutableListOf()
    }

    open override fun performImpl(modelOrApplication: ClearFolderInFolderList, currentComponent: MBTComponent): MBTComponent {
        modelOrApplication.clearTrash(this.confirmDeletionIfNeeded)
        return currentComponent
    }

    open override fun requiredFeature(): Feature<ClearFolderInFolderList> {
        return ClearFolderInFolderListFeature.`get`
    }

    open override fun canBePerformedImpl(model: ClearFolderInFolderList): Boolean {
        return model.doesClearTrashButtonExist()
    }

    companion object {
        @JvmStatic val type: MBTActionType = "ClearTrashFolderAction"
    }
}

public open class ClearSpamFolderAction(private var confirmDeletionIfNeeded: Boolean = true): BaseSimpleAction<ClearFolderInFolderList, MBTComponent>(ClearSpamFolderAction.type) {
    open override fun events(): YSArray<EventusEvent> {
        return mutableListOf()
    }

    open override fun performImpl(modelOrApplication: ClearFolderInFolderList, currentComponent: MBTComponent): MBTComponent {
        modelOrApplication.clearSpam(this.confirmDeletionIfNeeded)
        return currentComponent
    }

    open override fun requiredFeature(): Feature<ClearFolderInFolderList> {
        return ClearFolderInFolderListFeature.`get`
    }

    open override fun canBePerformedImpl(model: ClearFolderInFolderList): Boolean {
        return model.doesClearSpamButtonExist()
    }

    companion object {
        @JvmStatic val type: MBTActionType = "ClearSpamFolderAction"
    }
}

