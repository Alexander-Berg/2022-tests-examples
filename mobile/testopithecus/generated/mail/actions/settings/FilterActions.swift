// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/actions/settings/filter-actions.ts >>>

import Foundation

open class BaseFiltersListAction: BaseSimpleAction<FiltersList, MBTComponent> {
  public override init(_ type: MBTActionType) {
    super.init(type)
  }

  @discardableResult
  open override func requiredFeature() -> Feature<FiltersList> {
    return FiltersListFeature.`get`
  }

  @discardableResult
  open override func events() -> YSArray<EventusEvent> {
    return YSArray()
  }

}

open class BaseFilterCreateOrUpdateAction: BaseSimpleAction<FilterCreateOrUpdateRule, MBTComponent> {
  public override init(_ type: MBTActionType) {
    super.init(type)
  }

  @discardableResult
  open override func requiredFeature() -> Feature<FilterCreateOrUpdateRule> {
    return FilterCreateOrUpdateRuleFeature.`get`
  }

  @discardableResult
  open override func events() -> YSArray<EventusEvent> {
    return YSArray()
  }

}

open class BaseFilterUpdateRuleMoreAction: BaseSimpleAction<FilterUpdateRuleMore, MBTComponent> {
  public override init(_ type: MBTActionType) {
    super.init(type)
  }

  @discardableResult
  open override func requiredFeature() -> Feature<FilterUpdateRuleMore> {
    return FilterUpdateRuleMoreFeature.`get`
  }

  @discardableResult
  open override func events() -> YSArray<EventusEvent> {
    return YSArray()
  }

}

open class FiltersListTapOnCreateRuleButtonAction: BaseFiltersListAction {
  public static let type: MBTActionType = "FiltersListTapOnCreateRuleButtonAction"
  public init() {
    super.init(FiltersListTapOnCreateRuleButtonAction.type)
  }

  @discardableResult
  open override func performImpl(_ modelOrApplication: FiltersList, _ currentComponent: MBTComponent) throws -> MBTComponent {
    (try modelOrApplication.tapOnCreateRuleButton())
    return FilterCreateOrUpdateComponent()
  }

}

open class FiltersListTapOnFilterByIndexAction: BaseFiltersListAction {
  public static let type: MBTActionType = "FiltersListTapOnFilterByIndexAction"
  private let index: Int32
  public init(_ index: Int32) {
    self.index = index
    super.init(FiltersListTapOnFilterByIndexAction.type)
  }

  @discardableResult
  open override func canBePerformedImpl(_ model: FiltersList) throws -> Bool {
    let filtersListLength = (try model.getFilterList()).length
    return self.index < filtersListLength
  }

  @discardableResult
  open override func performImpl(_ modelOrApplication: FiltersList, _ currentComponent: MBTComponent) throws -> MBTComponent {
    (try modelOrApplication.tapOnFilterByIndex(self.index))
    return FilterCreateOrUpdateComponent()
  }

  @discardableResult
  open override func tostring() -> String {
    return "\(FiltersListTapOnFilterByIndexAction.type);index=\(self.index)"
  }

}

open class FilterCreateUpdateSetActionToggleAction: BaseFilterCreateOrUpdateAction {
  public static let type: MBTActionType = "FilterCreateUpdateSetActionToggleAction"
  private let actionToggle: FilterActionToggle
  private let value: Bool
  public init(_ actionToggle: FilterActionToggle, _ value: Bool) {
    self.actionToggle = actionToggle
    self.value = value
    super.init(FilterCreateUpdateSetActionToggleAction.type)
  }

  @discardableResult
  open override func performImpl(_ modelOrApplication: FilterCreateOrUpdateRule, _ currentComponent: MBTComponent) throws -> MBTComponent {
    (try modelOrApplication.setActionToggle(self.actionToggle, self.value))
    return currentComponent
  }

  @discardableResult
  open override func tostring() -> String {
    return "\(FilterCreateUpdateSetActionToggleAction.type);toggle=\(self.actionToggle);value=\(self.value)"
  }

}

open class FilterCreateUpdateSetConditionAction: BaseFilterCreateOrUpdateAction {
  public static let type: MBTActionType = "FilterCreateUpdateSetConditionAction"
  private let conditionField: FilterConditionField
  private let value: String
  public init(_ conditionField: FilterConditionField, _ value: String) {
    self.conditionField = conditionField
    self.value = value
    super.init(FilterCreateUpdateSetConditionAction.type)
  }

  @discardableResult
  open override func performImpl(_ modelOrApplication: FilterCreateOrUpdateRule, _ currentComponent: MBTComponent) throws -> MBTComponent {
    (try modelOrApplication.setConditionField(self.conditionField, self.value))
    return currentComponent
  }

  @discardableResult
  open override func tostring() -> String {
    return "\(FilterCreateUpdateSetConditionAction.type);field=\(self.conditionField);value=\(self.value)"
  }

}

open class FilterCreateUpdateTapOnConditionLogicButtonAction: BaseFilterCreateOrUpdateAction {
  public static let type: MBTActionType = "FilterCreateUpdateTapOnConditionLogicButtonAction"
  public init() {
    super.init(FilterCreateUpdateTapOnConditionLogicButtonAction.type)
  }

  @discardableResult
  open override func canBePerformedImpl(_ model: FilterCreateOrUpdateRule) throws -> Bool {
    return (try model.isConditionLogicButtonShown())
  }

  @discardableResult
  open override func performImpl(_ modelOrApplication: FilterCreateOrUpdateRule, _ currentComponent: MBTComponent) throws -> MBTComponent {
    (try modelOrApplication.tapOnConditionLogicButton())
    return FilterConditionLogicComponent()
  }

  @discardableResult
  open override func tostring() -> String {
    return "\(FilterCreateUpdateTapOnConditionLogicButtonAction.type)"
  }

}

open class FilterUpdateTapOnMoreButtonAction: BaseFilterCreateOrUpdateAction {
  public static let type: MBTActionType = "FilterUpdateTapOnMoreButtonAction"
  public init() {
    super.init(FilterUpdateTapOnMoreButtonAction.type)
  }

  @discardableResult
  open override func performImpl(_ modelOrApplication: FilterCreateOrUpdateRule, _ currentComponent: MBTComponent) throws -> MBTComponent {
    (try modelOrApplication.tapOnMore())
    return FilterUpdateRuleMoreComponent()
  }

  @discardableResult
  open override func tostring() -> String {
    return "\(FilterCreateUpdateTapOnConditionLogicButtonAction.type)"
  }

}

open class FilterCreateUpdateTapOnMoveToFolderAction: BaseFilterCreateOrUpdateAction {
  public static let type: MBTActionType = "FilterCreateUpdateTapOnMoveToFolderAction"
  public init() {
    super.init(FilterCreateUpdateTapOnMoveToFolderAction.type)
  }

  @discardableResult
  open override func performImpl(_ modelOrApplication: FilterCreateOrUpdateRule, _ currentComponent: MBTComponent) throws -> MBTComponent {
    (try modelOrApplication.tapOnMoveToFolder())
    return MoveToFolderComponent()
  }

}

open class FilterCreateUpdateTapOnApplyLabelAction: BaseFilterCreateOrUpdateAction {
  public static let type: MBTActionType = "FilterCreateUpdateTapOnApplyLabelAction"
  public init() {
    super.init(FilterCreateUpdateTapOnApplyLabelAction.type)
  }

  @discardableResult
  open override func performImpl(_ modelOrApplication: FilterCreateOrUpdateRule, _ currentComponent: MBTComponent) throws -> MBTComponent {
    (try modelOrApplication.tapOnApplyLabel())
    return ApplyLabelComponent()
  }

}

open class FilterCreateUpdateTapOnCreateAction: BaseFilterCreateOrUpdateAction {
  public static let type: MBTActionType = "FilterCreateUpdateTapOnCreateAction"
  public init() {
    super.init(FilterCreateUpdateTapOnCreateAction.type)
  }

  @discardableResult
  open override func canBePerformedImpl(_ model: FilterCreateOrUpdateRule) throws -> Bool {
    let conditionsSubject = (try model.getConditionField(FilterConditionField.subject))
    let conditionsFrom = (try model.getConditionField(FilterConditionField.from))
    let actionDelete = (try model.getActionToggle(FilterActionToggle.delete))
    let actionMarkRead = (try model.getActionToggle(FilterActionToggle.markAsRead))
    let actionMoveToFolder: String! = (try model.getMoveToFolderValue())
    let actionApplyLabel: String! = (try model.getApplyLabelValue())
    return ((conditionsSubject.length > 0 || conditionsFrom.length > 0) && (actionDelete || actionMarkRead || actionMoveToFolder != nil || actionApplyLabel != nil))
  }

  @discardableResult
  open override func performImpl(_ modelOrApplication: FilterCreateOrUpdateRule, _ currentComponent: MBTComponent) throws -> MBTComponent {
    (try modelOrApplication.tapOnCreate())
    return FilterListComponent()
  }

}

open class FilterSetConditionLogicAction: BaseSimpleAction<FilterConditionLogic, MBTComponent> {
  public static let type: MBTActionType = "FilterSetConditionLogicAction"
  private let conditionLogic: FilterLogicType
  public init(_ conditionLogic: FilterLogicType) {
    self.conditionLogic = conditionLogic
    super.init(FilterSetConditionLogicAction.type)
  }

  @discardableResult
  open override func requiredFeature() -> Feature<FilterConditionLogic> {
    return FilterConditionLogicFeature.`get`
  }

  @discardableResult
  open override func events() -> YSArray<EventusEvent> {
    return YSArray()
  }

  @discardableResult
  open override func performImpl(_ modelOrApplication: FilterConditionLogic, _ currentComponent: MBTComponent) throws -> MBTComponent {
    (try modelOrApplication.setLogicType(self.conditionLogic))
    return FilterCreateOrUpdateComponent()
  }

  @discardableResult
  open override func tostring() -> String {
    return "\(FilterSetConditionLogicAction.type);logicType=\(self.conditionLogic.toString())"
  }

}

open class FilterUpdateRuleChangeEnableStatusAction: BaseFilterUpdateRuleMoreAction {
  public static let type: MBTActionType = "FilterUpdateRuleChangeEnableStatusAction"
  private let enable: Bool
  public init(_ enable: Bool) {
    self.enable = enable
    super.init(FilterUpdateRuleChangeEnableStatusAction.type)
  }

  @discardableResult
  open override func performImpl(_ modelOrApplication: FilterUpdateRuleMore, _ currentComponent: MBTComponent) throws -> MBTComponent {
    (try modelOrApplication.changeEnableStatus(self.enable))
    return FilterListComponent()
  }

  @discardableResult
  open override func tostring() -> String {
    return "\(FilterUpdateRuleChangeEnableStatusAction.type);enable=\(self.enable)"
  }

}

open class FilterUpdateRuleDeleteAction: BaseFilterUpdateRuleMoreAction {
  public static let type: MBTActionType = "FilterUpdateRuleDeleteAction"
  public init() {
    super.init(FilterUpdateRuleDeleteAction.type)
  }

  @discardableResult
  open override func performImpl(_ modelOrApplication: FilterUpdateRuleMore, _ currentComponent: MBTComponent) throws -> MBTComponent {
    (try modelOrApplication.delete())
    return FilterListComponent()
  }

}

