// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/model/left-column/manage-labels-model.ts >>>

package com.yandex.xplat.testopithecus

import com.yandex.xplat.common.*
import com.yandex.xplat.eventus.common.*
import com.yandex.xplat.eventus.*
import com.yandex.xplat.mapi.*
import com.yandex.xplat.testopithecus.common.*

public open class ManageLabelsModel(private var accHandler: MailAppModelHandler): ManageableLabel {
    private var nameOfCreatedLabel: LabelName? = null
    private var colorIndex: Int? = null
    private var oldNameOfEditedLabel: LabelName? = null
    private var newNameOfEditedLabel: LabelName? = null
    private var oldColorIndexOfEditedLabel: Int? = null
    private var newColorIndexOfEditedLabel: Int? = null
    open override fun openLabelManager(): Unit {
    }

    open override fun closeLabelManager(): Unit {
    }

    open override fun deleteLabel(labelName: LabelName, deletionMethod: ContainerDeletionMethod): Unit {
        this.accHandler.getCurrentAccount().messagesDB.removeLabel(labelName)
    }

    open override fun getLabelList(): YSArray<LabelName> {
        return this.accHandler.getCurrentAccount().messagesDB.getLabelList()
    }

    open override fun closeCreateLabelScreen(): Unit {
        this.dropAll()
    }

    open override fun closeEditLabelScreen(): Unit {
        this.dropAll()
    }

    open override fun enterNameForEditedLabel(labelName: LabelName): Unit {
        this.newNameOfEditedLabel = labelName
    }

    open override fun enterNameForNewLabel(labelName: LabelName): Unit {
        this.nameOfCreatedLabel = labelName
    }

    open override fun getCurrentEditedLabelColorIndex(): Int {
        if (this.newColorIndexOfEditedLabel == null) {
            return this.oldColorIndexOfEditedLabel!!
        }
        return this.newColorIndexOfEditedLabel!!
    }

    open override fun getCurrentEditedLabelName(): LabelName {
        if (this.newNameOfEditedLabel == null) {
            return this.oldNameOfEditedLabel!!
        }
        return this.newNameOfEditedLabel!!
    }

    open override fun getCurrentNewLabelColorIndex(): Int {
        return requireNonNull(this.colorIndex, "Color is not set")
    }

    open override fun getCurrentNewLabelName(): LabelName {
        if (this.nameOfCreatedLabel == null) {
            return ""
        }
        return this.nameOfCreatedLabel!!
    }

    open override fun openCreateLabelScreen(): Unit {
        this.colorIndex = 0
    }

    open override fun openEditLabelScreen(labelName: LabelName): Unit {
        this.oldNameOfEditedLabel = labelName
        this.oldColorIndexOfEditedLabel = 0
    }

    open override fun setEditedLabelColor(index: Int): Unit {
        this.newColorIndexOfEditedLabel = index
    }

    open override fun setNewLabelColor(index: Int): Unit {
        this.colorIndex = index
    }

    open override fun submitEditedLabel(): Unit {
        this.accHandler.getCurrentAccount().messagesDB.renameLabel(requireNonNull(this.oldNameOfEditedLabel, "Old label name is not set"), requireNonNull(this.newNameOfEditedLabel, "New label name is not set"))
        this.dropAll()
    }

    open override fun submitNewLabel(): Unit {
        this.accHandler.getCurrentAccount().messagesDB.createLabel(requireNonNull(this.nameOfCreatedLabel, "Label name is not set"))
        this.dropAll()
    }

    private fun dropAll(): Unit {
        this.oldColorIndexOfEditedLabel = null
        this.newColorIndexOfEditedLabel = null
        this.oldNameOfEditedLabel = null
        this.newNameOfEditedLabel = null
    }

}

