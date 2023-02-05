import { Int32, Nullable, Throwing } from '../../../../../../common/ys'
import { FilterLogicType } from '../../../../../mapi/code/api/entities/filters/filter-requests'
import {
  FilterActionToggle,
  FilterConditionField,
  FilterConditionLogic,
  FilterCreateOrUpdateRule,
  FilterUpdateRuleMore,
  FiltersList,
  FilterView,
} from '../../feature/settings/filters-features'
import { FolderName, LabelName } from '../../feature/folder-list-features'
import { AccountSettingModel } from './account-settings-model'

export class FiltersListModel implements FiltersList {
  public constructor(private readonly accountSettingModel: AccountSettingModel) {}

  private readonly maxPromoShowNumber: Int32 = 3

  public getFilterList(): Throwing<FilterView[]> {
    // TODO: сделать
    return []
  }

  public isPromoShown(): Throwing<boolean> {
    return this.accountSettingModel.getFilterScreenOpenCounter() <= this.maxPromoShowNumber
  }

  public tapOnCreateRuleButton(): Throwing<void> {
    // do nothing
  }

  public tapOnFilterByIndex(index: Int32): Throwing<void> {
    // do nothing
  }
}

export class FilterCreateOrUpdateRuleModel implements FilterCreateOrUpdateRule {
  public constructor(private readonly filterConditionLogicModel: FilterConditionLogicModel) {}

  private markAsReadToggle: boolean = false
  private deleteToggle: boolean = false
  private applyToExistingEmailsToggle: boolean = false
  private applyLabelValue: Nullable<LabelName> = null
  private moveToFolderValue: Nullable<FolderName> = null
  private subjects: string[] = []
  private froms: string[] = []

  public getActionToggle(actionToggle: FilterActionToggle): Throwing<boolean> {
    switch (actionToggle) {
      case FilterActionToggle.applyToExistingEmails:
        return this.applyToExistingEmailsToggle
      case FilterActionToggle.delete:
        return this.deleteToggle
      case FilterActionToggle.markAsRead:
        return this.markAsReadToggle
    }
  }

  public getApplyLabelValue(): Throwing<Nullable<LabelName>> {
    return this.applyLabelValue
  }

  public getConditionField(conditionField: FilterConditionField): Throwing<string[]> {
    switch (conditionField) {
      case FilterConditionField.subject:
        return this.subjects
      case FilterConditionField.from:
        return this.froms
    }
  }

  public isConditionLogicButtonShown(): Throwing<boolean> {
    return this.subjects.length > 0 && this.froms.length > 0
  }

  public tapOnConditionLogicButton(): Throwing<void> {
    // do nothing
  }

  public getConditionLogic(): Throwing<Nullable<FilterLogicType>> {
    return this.filterConditionLogicModel.getLogicType()
  }

  public getMoveToFolderValue(): Throwing<Nullable<FolderName>> {
    return this.moveToFolderValue
  }

  public setActionToggle(actionToggle: FilterActionToggle, value: boolean): Throwing<void> {
    switch (actionToggle) {
      case FilterActionToggle.applyToExistingEmails:
        this.applyToExistingEmailsToggle = value
        break
      case FilterActionToggle.delete:
        this.deleteToggle = value
        break
      case FilterActionToggle.markAsRead:
        this.markAsReadToggle = value
        break
    }
  }

  public setConditionField(conditionField: FilterConditionField, value: string): Throwing<void> {
    switch (conditionField) {
      case FilterConditionField.subject:
        this.subjects.push(value)
        break
      case FilterConditionField.from:
        this.froms.push(value)
        break
    }
  }

  public tapOnApplyLabel(): Throwing<void> {
    // do nothing
  }

  public tapOnConditionField(conditionField: FilterConditionField): Throwing<void> {
    // do nothing
  }

  public tapOnCreate(): Throwing<void> {
    // do nothing
  }

  public tapOnMoveToFolder(): Throwing<void> {
    // do nothing
  }

  public tapOnMore(): Throwing<void> {
    // do nothing
  }
}

export class FilterConditionLogicModel implements FilterConditionLogic {
  private conditionalLogic: Nullable<FilterLogicType> = null

  public getLogicTypes(): Throwing<FilterLogicType[]> {
    return [FilterLogicType.and, FilterLogicType.or]
  }

  public setLogicType(logicType: FilterLogicType): Throwing<void> {
    this.conditionalLogic = logicType
  }

  public getLogicType(): Throwing<Nullable<FilterLogicType>> {
    return this.conditionalLogic
  }
}

export class FilterUpdateRuleMoreModel implements FilterUpdateRuleMore {
  public changeEnableStatus(enable: boolean): Throwing<void> {
    // TODO: сделать
  }

  public delete(): Throwing<void> {
    // TODO: сделать
  }
}
