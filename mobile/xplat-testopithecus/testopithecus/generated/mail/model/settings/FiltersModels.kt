// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/model/settings/filters-models.ts >>>

package com.yandex.xplat.testopithecus

import com.yandex.xplat.common.*
import com.yandex.xplat.eventus.common.*
import com.yandex.xplat.eventus.*
import com.yandex.xplat.mapi.*
import com.yandex.xplat.testopithecus.common.*

public open class FiltersListModel(private val accountSettingModel: AccountSettingModel): FiltersList {
    private val maxPromoShowNumber: Int = 3
    open override fun getFilterList(): YSArray<FilterView> {
        return mutableListOf()
    }

    open override fun isPromoShown(): Boolean {
        return this.accountSettingModel.getFilterScreenOpenCounter() <= this.maxPromoShowNumber
    }

    open override fun tapOnCreateRuleButton(): Unit {
    }

    open override fun tapOnFilterByIndex(index: Int): Unit {
    }

}

public open class FilterCreateOrUpdateRuleModel(private val filterConditionLogicModel: FilterConditionLogicModel): FilterCreateOrUpdateRule {
    private var markAsReadToggle: Boolean = false
    private var deleteToggle: Boolean = false
    private var applyToExistingEmailsToggle: Boolean = false
    private var applyLabelValue: LabelName? = null
    private var moveToFolderValue: FolderName? = null
    private var subjects: YSArray<String> = mutableListOf()
    private var froms: YSArray<String> = mutableListOf()
    open override fun getActionToggle(actionToggle: FilterActionToggle): Boolean {
        when (actionToggle) {
            FilterActionToggle.applyToExistingEmails -> {
                return this.applyToExistingEmailsToggle
            }
            FilterActionToggle.delete -> {
                return this.deleteToggle
            }
            FilterActionToggle.markAsRead -> {
                return this.markAsReadToggle
            }
        }
    }

    open override fun getApplyLabelValue(): LabelName? {
        return this.applyLabelValue
    }

    open override fun getConditionField(conditionField: FilterConditionField): YSArray<String> {
        when (conditionField) {
            FilterConditionField.subject -> {
                return this.subjects
            }
            FilterConditionField.from -> {
                return this.froms
            }
        }
    }

    open override fun isConditionLogicButtonShown(): Boolean {
        return this.subjects.size > 0 && this.froms.size > 0
    }

    open override fun tapOnConditionLogicButton(): Unit {
    }

    open override fun getConditionLogic(): FilterLogicType? {
        return this.filterConditionLogicModel.getLogicType()
    }

    open override fun getMoveToFolderValue(): FolderName? {
        return this.moveToFolderValue
    }

    open override fun setActionToggle(actionToggle: FilterActionToggle, value: Boolean): Unit {
        when (actionToggle) {
            FilterActionToggle.applyToExistingEmails -> {
                this.applyToExistingEmailsToggle = value
            }
            FilterActionToggle.delete -> {
                this.deleteToggle = value
            }
            FilterActionToggle.markAsRead -> {
                this.markAsReadToggle = value
            }
        }
    }

    open override fun setConditionField(conditionField: FilterConditionField, value: String): Unit {
        when (conditionField) {
            FilterConditionField.subject -> {
                this.subjects.add(value)
            }
            FilterConditionField.from -> {
                this.froms.add(value)
            }
        }
    }

    open override fun tapOnApplyLabel(): Unit {
    }

    open override fun tapOnConditionField(conditionField: FilterConditionField): Unit {
    }

    open override fun tapOnCreate(): Unit {
    }

    open override fun tapOnMoveToFolder(): Unit {
    }

    open override fun tapOnMore(): Unit {
    }

}

public open class FilterConditionLogicModel: FilterConditionLogic {
    private var conditionalLogic: FilterLogicType? = null
    open override fun getLogicTypes(): YSArray<FilterLogicType> {
        return mutableListOf(FilterLogicType.and, FilterLogicType.or)
    }

    open override fun setLogicType(logicType: FilterLogicType): Unit {
        this.conditionalLogic = logicType
    }

    open fun getLogicType(): FilterLogicType? {
        return this.conditionalLogic
    }

}

public open class FilterUpdateRuleMoreModel: FilterUpdateRuleMore {
    open override fun changeEnableStatus(enable: Boolean): Unit {
    }

    open override fun delete(): Unit {
    }

}

