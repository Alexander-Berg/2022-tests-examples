import { Int32, int64, Throwing } from '../../../../../../common/ys'
import { Eventus } from '../../../../../eventus/code/events/eventus'
import { EventusEvent } from '../../../../../eventus-common/code/eventus-event'
import { BaseSimpleAction } from '../../../../../testopithecus-common/code/mbt/base-simple-action'
import {
  App,
  Feature,
  FeatureID,
  MBTAction,
  MBTActionType,
  MBTComponent,
  MBTHistory,
} from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { filterByOrders, requireNonNull } from '../../../../../testopithecus-common/code/utils/utils'
import { fakeMid } from '../../../utils/mail-utils'
import { GroupOperationsComponent } from '../../components/group-operations-component'
import { MaillistComponent } from '../../components/maillist-component'
import { ApplyLabelFeature } from '../../feature/apply-label-feature'
import { FolderName, LabelName } from '../../feature/folder-list-features'
import { MessageView } from '../../feature/mail-view-features'
import { ContainerGetterFeature, MessageContainerType } from '../../feature/message-list/container-getter-feature'
import { GroupMode, GroupModeFeature } from '../../feature/message-list/group-mode-feature'
import { MessageListDisplayFeature } from '../../feature/message-list/message-list-display-feature'
import { MoveToFolderFeature } from '../../feature/move-to-folder-feature'
import { DefaultFolderName } from '../../model/folder-data-model'

export abstract class BaseGroupModeAction implements MBTAction {
  public constructor(private type: MBTActionType) {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      GroupModeFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      ContainerGetterFeature.get.included(modelFeatures)
    )
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const groupModeModel = GroupModeFeature.get.forceCast(model)
    const messageListModel = MessageListDisplayFeature.get.forceCast(model)
    const containersModel = ContainerGetterFeature.get.forceCast(model)
    const messages = messageListModel.getMessageList(10)
    const selectedMessageOrders = groupModeModel.getSelectedMessages()
    if (selectedMessageOrders === null) {
      return false
    }
    const selectedMessages: MessageView[] = []
    for (const order of selectedMessageOrders.values()) {
      selectedMessages.push(messages[order])
    }
    const currentContainer = containersModel.getCurrentContainer()
    if (currentContainer.type !== MessageContainerType.folder) {
      return false
    }

    return this.canBePerformedImpl(
      selectedMessages,
      selectedMessageOrders,
      requireNonNull(currentContainer.name, 'Мы не находимся в папке'),
    )
  }

  public async perform(model: App, application: App, _history: MBTHistory): Throwing<Promise<MBTComponent>> {
    this.performImpl(GroupModeFeature.get.forceCast(model))
    this.performImpl(GroupModeFeature.get.forceCast(application))
    return new MaillistComponent()
  }

  public getActionType(): MBTActionType {
    return this.type
  }

  public abstract canBePerformedImpl(
    messages: MessageView[],
    selectedOrders: Set<Int32>,
    currentFolder: FolderName,
  ): Throwing<boolean>

  public abstract performImpl(modelOrApplication: GroupMode): Throwing<void>

  public abstract events(): EventusEvent[]

  public abstract tostring(): string
}

export abstract class BaseMarkSelectedMessages implements MBTAction {
  public constructor(private type: MBTActionType) {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      MessageListDisplayFeature.get.included(modelFeatures) &&
      GroupModeFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const messageListModel = MessageListDisplayFeature.get.forceCast(model)
    const groupModeModel = GroupModeFeature.get.forceCast(model)
    const selectedMessageOrders = groupModeModel.getSelectedMessages()
    if (selectedMessageOrders === null) {
      return false
    }
    const messages = messageListModel.getMessageList(10)
    const unreadCount = filterByOrders(messages, selectedMessageOrders).filter((message) => !message.read).length
    return this.canBePerformedImpl(unreadCount)
  }

  public async perform(model: App, application: App, _history: MBTHistory): Throwing<Promise<MBTComponent>> {
    this.performImpl(GroupModeFeature.get.forceCast(model))
    this.performImpl(GroupModeFeature.get.forceCast(application))
    return new MaillistComponent()
  }

  public tostring(): string {
    return this.getActionType()
  }

  public getActionType(): MBTActionType {
    return this.type
  }

  public abstract events(): EventusEvent[]

  public abstract canBePerformedImpl(selectedUnreadCount: Int32): Throwing<boolean>

  public abstract performImpl(modelOrApplication: GroupMode): Throwing<void>
}

export class GroupModeInitialSelectAction extends BaseSimpleAction<GroupMode, MaillistComponent> {
  public static readonly type: MBTActionType = 'GroupModeInitialSelectAction'

  public constructor(private order: Int32) {
    super(GroupModeSelectAction.type)
  }

  public requiredFeature(): Feature<GroupMode> {
    return GroupModeFeature.get
  }

  public canBePerformedImpl(model: GroupMode): Throwing<boolean> {
    const isInGroupMode = model.isInGroupMode()
    return !isInGroupMode
  }

  public events(): EventusEvent[] {
    return [Eventus.groupActionsEvents.selectMessage(this.order, int64(-1))]
  }

  public performImpl(modelOrApplication: GroupMode, _currentComponent: MaillistComponent): Throwing<MBTComponent> {
    modelOrApplication.initialMessageSelect(this.order)
    return new GroupOperationsComponent()
  }

  public tostring(): string {
    return `${GroupModeInitialSelectAction.type}(${this.order})`
  }
}

export class GroupModeSelectAction extends BaseSimpleAction<GroupMode, GroupOperationsComponent> {
  public static readonly type: MBTActionType = 'GroupModeSelectAction'

  public constructor(private order: Int32) {
    super(GroupModeSelectAction.type)
  }

  public requiredFeature(): Feature<GroupMode> {
    return GroupModeFeature.get
  }

  public canBePerformedImpl(model: GroupMode): Throwing<boolean> {
    return model.isInGroupMode()
  }

  public events(): EventusEvent[] {
    return [Eventus.groupActionsEvents.selectMessage(this.order, int64(-1))]
  }

  public performImpl(
    modelOrApplication: GroupMode,
    _currentComponent: GroupOperationsComponent,
  ): Throwing<MBTComponent> {
    modelOrApplication.selectMessage(this.order)
    return new GroupOperationsComponent()
  }

  public tostring(): string {
    return `${GroupModeSelectAction.type}(${this.order})`
  }
}

export class GroupModeSelectAllAction extends BaseSimpleAction<GroupMode, GroupOperationsComponent> {
  public static readonly type: MBTActionType = 'GroupModeSelectAllAction'

  public constructor() {
    super(GroupModeSelectAllAction.type)
  }

  public requiredFeature(): Feature<GroupMode> {
    return GroupModeFeature.get
  }

  public canBePerformedImpl(model: GroupMode): Throwing<boolean> {
    return model.isInGroupMode()
  }

  public events(): EventusEvent[] {
    return []
  }

  public performImpl(
    modelOrApplication: GroupMode,
    _currentComponent: GroupOperationsComponent,
  ): Throwing<MBTComponent> {
    modelOrApplication.selectAllMessages()
    return new GroupOperationsComponent()
  }

  public tostring(): string {
    return `${GroupModeSelectAllAction.type}`
  }
}

export class GroupModeMarkAsReadAction extends BaseMarkSelectedMessages {
  public static readonly type: MBTActionType = 'GroupModeMarkAsUnreadAction'

  public constructor() {
    super(GroupModeMarkAsReadAction.type)
  }

  public canBePerformedImpl(selectedUnreadCount: Int32): Throwing<boolean> {
    return selectedUnreadCount > 0
  }

  public performImpl(modelOrApplication: GroupMode): Throwing<void> {
    modelOrApplication.markAsRead()
  }

  public events(): EventusEvent[] {
    return [Eventus.groupActionsEvents.markAsReadSelectedMessages()]
  }
}

export class GroupModeMarkAsUnreadAction extends BaseMarkSelectedMessages {
  public static readonly type: MBTActionType = 'GroupModeMarkAsUnreadAction'

  public constructor() {
    super(GroupModeMarkAsUnreadAction.type)
  }

  public canBePerformedImpl(selectedUnreadCount: Int32): Throwing<boolean> {
    return selectedUnreadCount === 0
  }

  public performImpl(modelOrApplication: GroupMode): Throwing<void> {
    modelOrApplication.markAsUnread()
  }

  public events(): EventusEvent[] {
    return [Eventus.groupActionsEvents.markAsUnreadSelectedMessages()]
  }
}

export class GroupModeDeleteAction implements MBTAction {
  public static readonly type: MBTActionType = 'GroupModeDeleteAction'

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      MessageListDisplayFeature.get.included(modelFeatures) &&
      GroupModeFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public canBePerformed(_model: App): boolean {
    return true
  }

  public async perform(model: App, application: App, _history: MBTHistory): Throwing<Promise<MBTComponent>> {
    GroupModeFeature.get.forceCast(model).delete()
    GroupModeFeature.get.forceCast(application).delete()
    return new MaillistComponent()
  }

  public events(): EventusEvent[] {
    return [Eventus.groupActionsEvents.deleteSelectedMessages()]
  }

  public tostring(): string {
    return this.getActionType()
  }

  public getActionType(): MBTActionType {
    return GroupModeDeleteAction.type
  }
}

export class GroupModeArchiveAction implements MBTAction {
  public static readonly type: MBTActionType = 'GroupModeArchiveAction'

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      MessageListDisplayFeature.get.included(modelFeatures) &&
      GroupModeFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public canBePerformed(model: App): Throwing<boolean> {
    return ContainerGetterFeature.get.castIfSupported(model)!.getCurrentContainer().name !== DefaultFolderName.archive
  }

  public async perform(model: App, application: App, _history: MBTHistory): Throwing<Promise<MBTComponent>> {
    GroupModeFeature.get.forceCast(model).archive()
    GroupModeFeature.get.forceCast(application).archive()
    return new MaillistComponent()
  }

  public events(): EventusEvent[] {
    return [] // TODO: amosov-f
  }

  public tostring(): string {
    return this.getActionType()
  }

  public getActionType(): MBTActionType {
    return GroupModeArchiveAction.type
  }
}

export class GroupModeMarkImportantAction extends BaseGroupModeAction {
  public static readonly type: MBTActionType = 'GroupModeMarkImportantAction'

  public constructor() {
    super(GroupModeMarkImportantAction.type)
  }

  public canBePerformedImpl(
    messages: MessageView[],
    _selectedOrders: Set<Int32>,
    _currentFolder: FolderName,
  ): Throwing<boolean> {
    return messages.map((m) => !m.important).includes(true)
  }

  public events(): EventusEvent[] {
    return []
  }

  public performImpl(modelOrApplication: GroupMode): Throwing<void> {
    modelOrApplication.markAsImportant()
  }

  public tostring(): string {
    return GroupModeMarkImportantAction.type
  }
}

export class GroupModeMarkUnimportantAction extends BaseGroupModeAction {
  public static readonly type: MBTActionType = 'GroupModeMarkUnimportantAction'

  public constructor() {
    super(GroupModeMarkUnimportantAction.type)
  }

  public canBePerformedImpl(
    messages: MessageView[],
    _selectedOrders: Set<Int32>,
    _currentFolder: FolderName,
  ): Throwing<boolean> {
    return messages.map((m) => !m.important).includes(false)
  }

  public events(): EventusEvent[] {
    return []
  }

  public performImpl(modelOrApplication: GroupMode): Throwing<void> {
    modelOrApplication.markAsUnimportant()
  }

  public tostring(): string {
    return GroupModeMarkUnimportantAction.type
  }
}

export class GroupModeMarkSpamAction extends BaseGroupModeAction {
  public static readonly type: MBTActionType = 'GroupModeMarkSpamAction'

  public constructor() {
    super(GroupModeMarkSpamAction.type)
  }

  public canBePerformedImpl(
    messages: MessageView[],
    selectedOrders: Set<Int32>,
    currentFolder: FolderName,
  ): Throwing<boolean> {
    return currentFolder !== DefaultFolderName.spam
  }

  public events(): EventusEvent[] {
    return []
  }

  public performImpl(modelOrApplication: GroupMode): Throwing<void> {
    modelOrApplication.markAsSpam()
  }

  public tostring(): string {
    return GroupModeMarkSpamAction.type
  }
}

export class GroupModeMarkNotSpamAction extends BaseGroupModeAction {
  public static readonly type: MBTActionType = 'GroupModeMarkNotSpamAction'

  public constructor() {
    super(GroupModeMarkNotSpamAction.type)
  }

  public canBePerformedImpl(
    messages: MessageView[],
    selectedOrders: Set<Int32>,
    currentFolder: FolderName,
  ): Throwing<boolean> {
    return currentFolder === DefaultFolderName.spam
  }

  public events(): EventusEvent[] {
    return []
  }

  public performImpl(modelOrApplication: GroupMode): Throwing<void> {
    modelOrApplication.markAsNotSpam()
  }

  public tostring(): string {
    return GroupModeMarkNotSpamAction.type
  }
}

export class GroupModeMoveToFolderAction implements MBTAction {
  public static readonly type: MBTActionType = 'GroupModeMoveToFolderAction'

  public constructor(private folderName: FolderName) {}

  public canBePerformed(model: App): Throwing<boolean> {
    const currentContainer = ContainerGetterFeature.get.castIfSupported(model)!.getCurrentContainer()
    return currentContainer.type === MessageContainerType.folder && currentContainer.name !== this.folderName
  }

  public events(): EventusEvent[] {
    return []
  }

  public getActionType(): string {
    return GroupModeMoveToFolderAction.type
  }

  public async perform(model: App, application: App, _history: MBTHistory): Throwing<Promise<MBTComponent>> {
    GroupModeFeature.get.forceCast(model).openMoveToFolderScreen()
    GroupModeFeature.get.forceCast(application).openMoveToFolderScreen()
    MoveToFolderFeature.get.forceCast(model).tapOnFolder(this.folderName)
    MoveToFolderFeature.get.forceCast(application).tapOnFolder(this.folderName)
    return new MaillistComponent()
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      MessageListDisplayFeature.get.included(modelFeatures) &&
      GroupModeFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public tostring(): string {
    return `${GroupModeMoveToFolderAction.type}(${this.folderName})`
  }
}

export class GroupModeApplyLabelsAction implements MBTAction {
  public static readonly type: MBTActionType = 'GroupModeApplyLabelsAction'

  public constructor(private labelNames: LabelName[]) {}

  public canBePerformed(model: App): Throwing<boolean> {
    return ![DefaultFolderName.spam, DefaultFolderName.trash].includes(
      ContainerGetterFeature.get.castIfSupported(model)!.getCurrentContainer().name,
    )
  }

  public events(): EventusEvent[] {
    return []
  }

  public getActionType(): string {
    return GroupModeApplyLabelsAction.type
  }

  public async perform(model: App, application: App, _history: MBTHistory): Throwing<Promise<MBTComponent>> {
    GroupModeFeature.get.forceCast(model).openApplyLabelsScreen()
    GroupModeFeature.get.forceCast(application).openApplyLabelsScreen()
    const modelApplyLabel = ApplyLabelFeature.get.forceCast(model)
    const appApplyLabel = ApplyLabelFeature.get.forceCast(application)
    modelApplyLabel.selectLabelsToAdd(this.labelNames)
    appApplyLabel.selectLabelsToAdd(this.labelNames)
    modelApplyLabel.tapOnDoneButton()
    appApplyLabel.tapOnDoneButton()
    return new MaillistComponent()
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      MessageListDisplayFeature.get.included(modelFeatures) &&
      GroupModeFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      ApplyLabelFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public tostring(): string {
    return `${GroupModeApplyLabelsAction.type}(${this.labelNames})`
  }
}

export class GroupModeRemoveLabelsAction implements MBTAction {
  public static readonly type: MBTActionType = 'GroupModeRemoveLabelsAction'

  public constructor(private labelNames: LabelName[]) {}

  public canBePerformed(model: App): Throwing<boolean> {
    return ![DefaultFolderName.spam, DefaultFolderName.trash].includes(
      ContainerGetterFeature.get.forceCast(model).getCurrentContainer().name,
    )
  }

  public events(): EventusEvent[] {
    return []
  }

  public getActionType(): string {
    return GroupModeRemoveLabelsAction.type
  }

  public async perform(model: App, application: App, _history: MBTHistory): Throwing<Promise<MBTComponent>> {
    GroupModeFeature.get.forceCast(model).openApplyLabelsScreen()
    GroupModeFeature.get.forceCast(application).openApplyLabelsScreen()
    const modelApplyLabel = ApplyLabelFeature.get.forceCast(model)
    const appApplyLabel = ApplyLabelFeature.get.forceCast(application)
    modelApplyLabel.deselectLabelsToRemove(this.labelNames)
    appApplyLabel.deselectLabelsToRemove(this.labelNames)
    modelApplyLabel.tapOnDoneButton()
    appApplyLabel.tapOnDoneButton()
    return new MaillistComponent()
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      MessageListDisplayFeature.get.included(modelFeatures) &&
      GroupModeFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public tostring(): string {
    return `${GroupModeRemoveLabelsAction.type}(${this.labelNames})`
  }
}

export class GroupModeUnselectMessageAction extends BaseGroupModeAction {
  public static readonly type: MBTActionType = 'GroupModeUnselectMessageAction'

  public constructor(private order: Int32) {
    super(GroupModeUnselectMessageAction.type)
  }

  public canBePerformedImpl(
    messages: MessageView[],
    selectedOrders: Set<Int32>,
    _currentFolder: FolderName,
  ): Throwing<boolean> {
    return selectedOrders.has(this.order)
  }

  public events(): EventusEvent[] {
    return [Eventus.groupActionsEvents.deselectMessage(this.order, fakeMid())]
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    this.performImpl(GroupModeFeature.get.forceCast(model))
    this.performImpl(GroupModeFeature.get.forceCast(application))
    if (GroupModeFeature.get.forceCast(model).isInGroupMode()) {
      return history.currentComponent
    } else {
      return new MaillistComponent()
    }
  }

  public performImpl(modelOrApplication: GroupMode): Throwing<void> {
    modelOrApplication.unselectMessage(this.order)
  }

  public tostring(): string {
    return `${GroupModeUnselectMessageAction.type}(${this.order})`
  }
}

export class GroupModeUnselectAllAction extends BaseGroupModeAction {
  public static readonly type: MBTActionType = 'GroupModeUnselectAllAction'

  public constructor() {
    super(GroupModeUnselectAllAction.type)
  }

  public requiredFeature(): Feature<GroupMode> {
    return GroupModeFeature.get
  }

  public canBePerformedImpl(
    messages: MessageView[],
    selectedOrders: Set<Int32>,
    _currentFolder: FolderName,
  ): Throwing<boolean> {
    return selectedOrders.size >= 1
  }

  public performImpl(modelOrApplication: GroupMode): Throwing<void> {
    modelOrApplication.unselectAllMessages()
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return GroupModeUnselectAllAction.type
  }
}
