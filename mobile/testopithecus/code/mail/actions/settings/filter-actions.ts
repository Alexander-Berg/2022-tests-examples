import { Int32, Throwing } from '../../../../../../common/ys'
import { EventusEvent } from '../../../../../eventus-common/code/eventus-event'
import { FilterLogicType } from '../../../../../mapi/code/api/entities/filters/filter-requests'
import { BaseSimpleAction } from '../../../../../testopithecus-common/code/mbt/base-simple-action'
import { Feature, MBTActionType, MBTComponent } from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { ApplyLabelComponent } from '../../components/apply-label-component'
import { MoveToFolderComponent } from '../../components/move-to-folder-component'
import {
  FilterConditionLogicComponent,
  FilterCreateOrUpdateComponent,
  FilterUpdateRuleMoreComponent,
} from '../../components/settings/filter-create-or-update-components'
import { FilterListComponent } from '../../components/settings/filter-list-component'
import {
  FilterActionToggle,
  FilterConditionField,
  FilterConditionLogic,
  FilterConditionLogicFeature,
  FilterCreateOrUpdateRule,
  FilterCreateOrUpdateRuleFeature,
  FiltersList,
  FiltersListFeature,
  FilterUpdateRuleMore,
  FilterUpdateRuleMoreFeature,
} from '../../feature/settings/filters-features'

export abstract class BaseFiltersListAction extends BaseSimpleAction<FiltersList, MBTComponent> {
  public constructor(type: MBTActionType) {
    super(type)
  }

  public requiredFeature(): Feature<FiltersList> {
    return FiltersListFeature.get
  }

  public events(): EventusEvent[] {
    return []
  }
}

export abstract class BaseFilterCreateOrUpdateAction extends BaseSimpleAction<FilterCreateOrUpdateRule, MBTComponent> {
  public constructor(type: MBTActionType) {
    super(type)
  }

  public requiredFeature(): Feature<FilterCreateOrUpdateRule> {
    return FilterCreateOrUpdateRuleFeature.get
  }

  public events(): EventusEvent[] {
    return []
  }
}

export abstract class BaseFilterUpdateRuleMoreAction extends BaseSimpleAction<FilterUpdateRuleMore, MBTComponent> {
  public constructor(type: MBTActionType) {
    super(type)
  }

  public requiredFeature(): Feature<FilterUpdateRuleMore> {
    return FilterUpdateRuleMoreFeature.get
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class FiltersListTapOnCreateRuleButtonAction extends BaseFiltersListAction {
  public static readonly type: MBTActionType = 'FiltersListTapOnCreateRuleButtonAction'

  public constructor() {
    super(FiltersListTapOnCreateRuleButtonAction.type)
  }

  public performImpl(modelOrApplication: FiltersList, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.tapOnCreateRuleButton()
    return new FilterCreateOrUpdateComponent()
  }
}

export class FiltersListTapOnFilterByIndexAction extends BaseFiltersListAction {
  public static readonly type: MBTActionType = 'FiltersListTapOnFilterByIndexAction'

  public constructor(private readonly index: Int32) {
    super(FiltersListTapOnFilterByIndexAction.type)
  }

  public canBePerformedImpl(model: FiltersList): Throwing<boolean> {
    const filtersListLength = model.getFilterList().length
    return this.index < filtersListLength
  }

  public performImpl(modelOrApplication: FiltersList, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.tapOnFilterByIndex(this.index)
    return new FilterCreateOrUpdateComponent()
  }

  public tostring(): string {
    return `${FiltersListTapOnFilterByIndexAction.type};index=${this.index}`
  }
}

export class FilterCreateUpdateSetActionToggleAction extends BaseFilterCreateOrUpdateAction {
  public static readonly type: MBTActionType = 'FilterCreateUpdateSetActionToggleAction'

  public constructor(private readonly actionToggle: FilterActionToggle, private readonly value: boolean) {
    super(FilterCreateUpdateSetActionToggleAction.type)
  }

  public performImpl(
    modelOrApplication: FilterCreateOrUpdateRule,
    currentComponent: MBTComponent,
  ): Throwing<MBTComponent> {
    modelOrApplication.setActionToggle(this.actionToggle, this.value)
    return currentComponent
  }

  public tostring(): string {
    return `${FilterCreateUpdateSetActionToggleAction.type};toggle=${this.actionToggle};value=${this.value}`
  }
}

export class FilterCreateUpdateSetConditionAction extends BaseFilterCreateOrUpdateAction {
  public static readonly type: MBTActionType = 'FilterCreateUpdateSetConditionAction'

  public constructor(private readonly conditionField: FilterConditionField, private readonly value: string) {
    super(FilterCreateUpdateSetConditionAction.type)
  }

  public performImpl(
    modelOrApplication: FilterCreateOrUpdateRule,
    currentComponent: MBTComponent,
  ): Throwing<MBTComponent> {
    modelOrApplication.setConditionField(this.conditionField, this.value)
    return currentComponent
  }

  public tostring(): string {
    return `${FilterCreateUpdateSetConditionAction.type};field=${this.conditionField};value=${this.value}`
  }
}

export class FilterCreateUpdateTapOnConditionLogicButtonAction extends BaseFilterCreateOrUpdateAction {
  public static readonly type: MBTActionType = 'FilterCreateUpdateTapOnConditionLogicButtonAction'

  public canBePerformedImpl(model: FilterCreateOrUpdateRule): Throwing<boolean> {
    return model.isConditionLogicButtonShown()
  }

  public constructor() {
    super(FilterCreateUpdateTapOnConditionLogicButtonAction.type)
  }

  public performImpl(
    modelOrApplication: FilterCreateOrUpdateRule,
    currentComponent: MBTComponent,
  ): Throwing<MBTComponent> {
    modelOrApplication.tapOnConditionLogicButton()
    return new FilterConditionLogicComponent()
  }

  public tostring(): string {
    return `${FilterCreateUpdateTapOnConditionLogicButtonAction.type}`
  }
}

export class FilterUpdateTapOnMoreButtonAction extends BaseFilterCreateOrUpdateAction {
  public static readonly type: MBTActionType = 'FilterUpdateTapOnMoreButtonAction'

  public constructor() {
    super(FilterUpdateTapOnMoreButtonAction.type)
  }

  public performImpl(
    modelOrApplication: FilterCreateOrUpdateRule,
    currentComponent: MBTComponent,
  ): Throwing<MBTComponent> {
    modelOrApplication.tapOnMore()
    return new FilterUpdateRuleMoreComponent()
  }

  public tostring(): string {
    return `${FilterCreateUpdateTapOnConditionLogicButtonAction.type}`
  }
}

export class FilterCreateUpdateTapOnMoveToFolderAction extends BaseFilterCreateOrUpdateAction {
  public static readonly type: MBTActionType = 'FilterCreateUpdateTapOnMoveToFolderAction'

  public constructor() {
    super(FilterCreateUpdateTapOnMoveToFolderAction.type)
  }

  public performImpl(
    modelOrApplication: FilterCreateOrUpdateRule,
    currentComponent: MBTComponent,
  ): Throwing<MBTComponent> {
    modelOrApplication.tapOnMoveToFolder()
    return new MoveToFolderComponent()
  }
}

export class FilterCreateUpdateTapOnApplyLabelAction extends BaseFilterCreateOrUpdateAction {
  public static readonly type: MBTActionType = 'FilterCreateUpdateTapOnApplyLabelAction'

  public constructor() {
    super(FilterCreateUpdateTapOnApplyLabelAction.type)
  }

  public performImpl(
    modelOrApplication: FilterCreateOrUpdateRule,
    currentComponent: MBTComponent,
  ): Throwing<MBTComponent> {
    modelOrApplication.tapOnApplyLabel()
    return new ApplyLabelComponent()
  }
}

export class FilterCreateUpdateTapOnCreateAction extends BaseFilterCreateOrUpdateAction {
  public static readonly type: MBTActionType = 'FilterCreateUpdateTapOnCreateAction'

  public constructor() {
    super(FilterCreateUpdateTapOnCreateAction.type)
  }

  public canBePerformedImpl(model: FilterCreateOrUpdateRule): Throwing<boolean> {
    const conditionsSubject = model.getConditionField(FilterConditionField.subject)
    const conditionsFrom = model.getConditionField(FilterConditionField.from)
    const actionDelete = model.getActionToggle(FilterActionToggle.delete)
    const actionMarkRead = model.getActionToggle(FilterActionToggle.markAsRead)
    const actionMoveToFolder = model.getMoveToFolderValue()
    const actionApplyLabel = model.getApplyLabelValue()
    return (
      (conditionsSubject.length > 0 || conditionsFrom.length > 0) &&
      (actionDelete || actionMarkRead || actionMoveToFolder !== null || actionApplyLabel !== null)
    )
  }

  public performImpl(
    modelOrApplication: FilterCreateOrUpdateRule,
    currentComponent: MBTComponent,
  ): Throwing<MBTComponent> {
    modelOrApplication.tapOnCreate()
    return new FilterListComponent()
  }
}

export class FilterSetConditionLogicAction extends BaseSimpleAction<FilterConditionLogic, MBTComponent> {
  public static readonly type: MBTActionType = 'FilterSetConditionLogicAction'

  public constructor(private readonly conditionLogic: FilterLogicType) {
    super(FilterSetConditionLogicAction.type)
  }

  public requiredFeature(): Feature<FilterConditionLogic> {
    return FilterConditionLogicFeature.get
  }

  public events(): EventusEvent[] {
    return []
  }

  public performImpl(modelOrApplication: FilterConditionLogic, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.setLogicType(this.conditionLogic)
    return new FilterCreateOrUpdateComponent()
  }

  public tostring(): string {
    return `${FilterSetConditionLogicAction.type};logicType=${this.conditionLogic.toString()}`
  }
}

export class FilterUpdateRuleChangeEnableStatusAction extends BaseFilterUpdateRuleMoreAction {
  public static readonly type: MBTActionType = 'FilterUpdateRuleChangeEnableStatusAction'

  public constructor(private readonly enable: boolean) {
    super(FilterUpdateRuleChangeEnableStatusAction.type)
  }

  public performImpl(modelOrApplication: FilterUpdateRuleMore, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.changeEnableStatus(this.enable)
    return new FilterListComponent()
  }

  public tostring(): string {
    return `${FilterUpdateRuleChangeEnableStatusAction.type};enable=${this.enable}`
  }
}

export class FilterUpdateRuleDeleteAction extends BaseFilterUpdateRuleMoreAction {
  public static readonly type: MBTActionType = 'FilterUpdateRuleDeleteAction'

  public constructor() {
    super(FilterUpdateRuleDeleteAction.type)
  }

  public performImpl(modelOrApplication: FilterUpdateRuleMore, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.delete()
    return new FilterListComponent()
  }
}
