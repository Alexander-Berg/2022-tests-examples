import { Int32, Throwing } from '../../../../../../common/ys'
import { Eventus } from '../../../../../eventus/code/events/eventus'
import { EventusEvent } from '../../../../../eventus-common/code/eventus-event'
import {
  App,
  Feature,
  FeatureID,
  MBTAction,
  MBTActionType,
  MBTComponent,
  MBTHistory,
} from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { fakeMid } from '../../../utils/mail-utils'
import { ApplyLabelComponent } from '../../components/apply-label-component'
import { ComposeComponent } from '../../components/compose-component'
import { ContextMenuComponent } from '../../components/context-menu-component'
import { MaillistComponent } from '../../components/maillist-component'
import { MoveToFolderComponent } from '../../components/move-to-folder-component'
import { ApplyLabelFeature } from '../../feature/apply-label-feature'
import { ComposeFeature } from '../../feature/compose/compose-features'
import { FolderName, FolderNavigatorFeature, LabelName } from '../../feature/folder-list-features'
import { MessageView, MessageViewerFeature } from '../../feature/mail-view-features'
import { ContainerGetterFeature } from '../../feature/message-list/container-getter-feature'
import { ContextMenu, ContextMenuFeature } from '../../feature/message-list/context-menu-feature'
import { GroupModeFeature } from '../../feature/message-list/group-mode-feature'
import { MessageListDisplayFeature } from '../../feature/message-list/message-list-display-feature'
import { MoveToFolderFeature } from '../../feature/move-to-folder-feature'
import { TranslatorBarFeature } from '../../feature/translator-features'
import { DefaultFolderName } from '../../model/folder-data-model'
import { MarkAsImportant, MarkAsUnimportant } from '../base-actions/labeled-actions'
import { MarkAsRead, MarkAsUnread } from '../base-actions/markable-actions'

export abstract class BaseShortSwipeContextMenuAction implements MBTAction {
  protected constructor(protected order: Int32, private type: MBTActionType) {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return ContextMenuFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const messageListModel = MessageListDisplayFeature.get.forceCast(model)
    const messages = messageListModel.getMessageList(10)
    const canPerform = this.canBePerformedImpl(messages[this.order])
    return this.order < messages.length && canPerform
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    this.performImpl(ContextMenuFeature.get.forceCast(model))
    this.performImpl(ContextMenuFeature.get.forceCast(application))
    return history.currentComponent
  }

  public getActionType(): MBTActionType {
    return this.type
  }

  public tostring(): string {
    return `${this.type}(${this.order})`
  }

  public abstract canBePerformedImpl(message: MessageView): Throwing<boolean>

  public abstract performImpl(modelOrApplication: ContextMenu): Throwing<void>

  public abstract events(): EventusEvent[]
}

export abstract class BaseMailViewContextMenuAction implements MBTAction {
  protected constructor(private type: MBTActionType) {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return ContextMenuFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const openedMessage = MessageViewerFeature.get.forceCast(model).getOpenedMessage().head
    return this.canBePerformedImpl(openedMessage)
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    this.performImpl(ContextMenuFeature.get.forceCast(model))
    this.performImpl(ContextMenuFeature.get.forceCast(application))
    return history.currentComponent
  }

  public getActionType(): MBTActionType {
    return this.type
  }

  public tostring(): string {
    return `${this.type}`
  }

  public abstract canBePerformedImpl(message: MessageView): Throwing<boolean>

  public abstract performImpl(modelOrApplication: ContextMenu): Throwing<void>

  public abstract events(): EventusEvent[]
}

export class ShortSwipeOpenContextMenuAction implements MBTAction {
  public static readonly type: MBTActionType = 'ShortSwipeOpenContextMenuAction'

  public constructor(private readonly order: Int32) {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return ContextMenuFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const isInGroupMode = GroupModeFeature.get.forceCast(model).isInGroupMode()
    const isOutgoingFolderOpened =
      ContainerGetterFeature.get.forceCast(model).getCurrentContainer().name === DefaultFolderName.outgoing
    const messages = MessageListDisplayFeature.get.forceCast(model).getMessageList(10)
    return !isInGroupMode && !isOutgoingFolderOpened && this.order < messages.length
  }

  public events(): EventusEvent[] {
    return []
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ContextMenuFeature.get.forceCast(model).openFromShortSwipe(this.order)
    ContextMenuFeature.get.forceCast(application).openFromShortSwipe(this.order)
    return new ContextMenuComponent()
  }

  public tostring(): string {
    return `${ShortSwipeOpenContextMenuAction.type}(${this.order})`
  }

  public getActionType(): MBTActionType {
    return ShortSwipeOpenContextMenuAction.type
  }
}

export class ShortSwipeContextMenuDeleteAction implements MBTAction {
  public static readonly type: MBTActionType = 'ShortSwipeContextMenuDeleteAction'

  public constructor(private readonly order: Int32) {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return ContextMenuFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public events(): EventusEvent[] {
    return [
      Eventus.messageListEvents.openMessageActions(this.order, fakeMid()),
      Eventus.messageListEvents.deleteMessage(this.order, fakeMid()),
    ]
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    const modelContextMenu = ContextMenuFeature.get.forceCast(model)
    const appContextMenu = ContextMenuFeature.get.forceCast(application)
    modelContextMenu.openFromShortSwipe(this.order)
    appContextMenu.openFromShortSwipe(this.order)
    modelContextMenu.deleteMessage()
    appContextMenu.deleteMessage()
    return new MaillistComponent()
  }

  public tostring(): string {
    return `${ShortSwipeContextMenuDeleteAction.type}(${this.order})`
  }

  public getActionType(): MBTActionType {
    return ShortSwipeContextMenuDeleteAction.type
  }
}

export class ShortSwipeContextMenuMarkAsSpamAction implements MBTAction {
  public static readonly type: MBTActionType = 'ShortSwipeContextMenuMarkAsSpamAction'

  public constructor(private readonly order: Int32) {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return ContextMenuFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    return ContainerGetterFeature.get.forceCast(model).getCurrentContainer().name !== DefaultFolderName.spam
  }

  public events(): EventusEvent[] {
    return [
      Eventus.messageListEvents.openMessageActions(this.order, fakeMid()),
      Eventus.messageListEvents.markMessageAsSpam(this.order, fakeMid()),
    ]
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    const modelContextMenu = ContextMenuFeature.get.forceCast(model)
    const appContextMenu = ContextMenuFeature.get.forceCast(application)
    modelContextMenu.openFromShortSwipe(this.order)
    appContextMenu.openFromShortSwipe(this.order)
    modelContextMenu.markAsSpam()
    appContextMenu.markAsSpam()
    return new MaillistComponent()
  }

  public tostring(): string {
    return `${ShortSwipeContextMenuMarkAsSpamAction.type}(${this.order})`
  }

  public getActionType(): MBTActionType {
    return ShortSwipeContextMenuMarkAsSpamAction.type
  }
}

export class ShortSwipeContextMenuMarkAsNotSpamAction implements MBTAction {
  public static readonly type: MBTActionType = 'ShortSwipeContextMenuMarkAsNotSpamAction'

  public constructor(private readonly order: Int32) {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return ContextMenuFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    return ContainerGetterFeature.get.forceCast(model).getCurrentContainer().name === DefaultFolderName.spam
  }

  public events(): EventusEvent[] {
    return [
      Eventus.messageListEvents.openMessageActions(this.order, fakeMid()),
      Eventus.messageListEvents.markMessageAsNotSpam(this.order, fakeMid()),
    ]
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    const modelContextMenu = ContextMenuFeature.get.forceCast(model)
    const appContextMenu = ContextMenuFeature.get.forceCast(application)
    modelContextMenu.openFromShortSwipe(this.order)
    appContextMenu.openFromShortSwipe(this.order)
    modelContextMenu.markAsNotSpam()
    appContextMenu.markAsNotSpam()
    return new MaillistComponent()
  }

  public tostring(): string {
    return `${ShortSwipeContextMenuMarkAsNotSpamAction.type}(${this.order})`
  }

  public getActionType(): MBTActionType {
    return ShortSwipeContextMenuMarkAsNotSpamAction.type
  }
}

export class ShortSwipeContextMenuOpenApplyLabelsAction implements MBTAction {
  public static readonly type: MBTActionType = 'ShortSwipeContextMenuOpenApplyLabelsAction'

  public constructor(private readonly order: Int32) {}

  public canBePerformed(model: App): Throwing<boolean> {
    return ![DefaultFolderName.spam, DefaultFolderName.trash].includes(
      ContainerGetterFeature.get.forceCast(model).getCurrentContainer().name,
    )
  }

  public events(): EventusEvent[] {
    return [Eventus.messageListEvents.openMessageActions(this.order, fakeMid())]
  }

  public getActionType(): string {
    return ShortSwipeContextMenuOpenApplyLabelsAction.type
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    const modelContextMenu = ContextMenuFeature.get.forceCast(model)
    const appContextMenu = ContextMenuFeature.get.forceCast(application)
    modelContextMenu.openFromShortSwipe(this.order)
    appContextMenu.openFromShortSwipe(this.order)
    modelContextMenu.openApplyLabelsScreen()
    appContextMenu.openApplyLabelsScreen()
    return new ApplyLabelComponent()
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      MessageListDisplayFeature.get.included(modelFeatures) &&
      ContextMenuFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public tostring(): string {
    return `${ShortSwipeContextMenuOpenApplyLabelsAction.type}(${this.order})`
  }
}

export class ShortSwipeContextMenuApplyLabelsAction implements MBTAction {
  public static readonly type: MBTActionType = 'ShortSwipeContextMenuApplyLabelsAction'

  public constructor(private readonly order: Int32, private readonly labels: LabelName[]) {}

  public canBePerformed(model: App): Throwing<boolean> {
    return ![DefaultFolderName.spam, DefaultFolderName.trash].includes(
      ContainerGetterFeature.get.forceCast(model).getCurrentContainer().name,
    )
  }

  public events(): EventusEvent[] {
    return [
      Eventus.messageListEvents.openMessageActions(this.order, fakeMid()),
      Eventus.messageListEvents.markMessageAs(this.order, fakeMid()),
    ]
  }

  public getActionType(): string {
    return ShortSwipeContextMenuApplyLabelsAction.type
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    const modelContextMenu = ContextMenuFeature.get.forceCast(model)
    const appContextMenu = ContextMenuFeature.get.forceCast(application)
    modelContextMenu.openFromShortSwipe(this.order)
    appContextMenu.openFromShortSwipe(this.order)
    modelContextMenu.openApplyLabelsScreen()
    appContextMenu.openApplyLabelsScreen()

    const modelApplyLabel = ApplyLabelFeature.get.forceCast(model)
    const appApplyLabel = ApplyLabelFeature.get.forceCast(application)
    modelApplyLabel.selectLabelsToAdd(this.labels)
    appApplyLabel.selectLabelsToAdd(this.labels)
    modelApplyLabel.tapOnDoneButton()
    appApplyLabel.tapOnDoneButton()

    return history.currentComponent
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      ContextMenuFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      MessageViewerFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public tostring(): string {
    return `${ShortSwipeContextMenuApplyLabelsAction.type}(${this.order})`
  }
}

export class ShortSwipeContextMenuRemoveLabelsAction implements MBTAction {
  public static readonly type: MBTActionType = 'ShortSwipeContextMenuRemoveLabelsAction'

  public constructor(private readonly order: Int32, private readonly labels: LabelName[]) {}

  public canBePerformed(model: App): Throwing<boolean> {
    return ![DefaultFolderName.spam, DefaultFolderName.trash].includes(
      ContainerGetterFeature.get.forceCast(model).getCurrentContainer().name,
    )
  }

  public events(): EventusEvent[] {
    return [
      Eventus.messageListEvents.openMessageActions(this.order, fakeMid()),
      Eventus.messageListEvents.markMessageAs(this.order, fakeMid()),
    ]
  }

  public getActionType(): string {
    return ShortSwipeContextMenuRemoveLabelsAction.type
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    const modelContextMenu = ContextMenuFeature.get.forceCast(model)
    const appContextMenu = ContextMenuFeature.get.forceCast(application)
    modelContextMenu.openFromShortSwipe(this.order)
    appContextMenu.openFromShortSwipe(this.order)
    modelContextMenu.openApplyLabelsScreen()
    appContextMenu.openApplyLabelsScreen()

    const modelApplyLabel = ApplyLabelFeature.get.forceCast(model)
    const appApplyLabel = ApplyLabelFeature.get.forceCast(application)
    modelApplyLabel.deselectLabelsToRemove(this.labels)
    appApplyLabel.deselectLabelsToRemove(this.labels)
    modelApplyLabel.tapOnDoneButton()
    appApplyLabel.tapOnDoneButton()

    return history.currentComponent
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      MessageListDisplayFeature.get.included(modelFeatures) &&
      ContextMenuFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public tostring(): string {
    return `${ShortSwipeContextMenuRemoveLabelsAction.type}`
  }
}

export class ShortSwipeContextMenuOpenReplyComposeAction implements MBTAction {
  public static readonly type: MBTActionType = 'ShortSwipeContextMenuOpenReplyComposeAction'

  public constructor(private readonly order: Int32) {}

  public canBePerformed(model: App): Throwing<boolean> {
    return ![DefaultFolderName.draft, DefaultFolderName.template].includes(
      ContainerGetterFeature.get.forceCast(model).getCurrentContainer().name,
    )
  }

  public events(): EventusEvent[] {
    return [
      Eventus.messageListEvents.openMessageActions(this.order, fakeMid()),
      Eventus.messageListEvents.replyMessage(this.order, fakeMid()),
    ]
  }

  public getActionType(): string {
    return ShortSwipeContextMenuOpenReplyComposeAction.type
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    const modelContextMenu = ContextMenuFeature.get.forceCast(model)
    const appContextMenu = ContextMenuFeature.get.forceCast(application)
    modelContextMenu.openFromShortSwipe(this.order)
    appContextMenu.openFromShortSwipe(this.order)
    modelContextMenu.openReplyCompose()
    appContextMenu.openReplyCompose()
    return new ComposeComponent()
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      MessageListDisplayFeature.get.included(modelFeatures) &&
      ContextMenuFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      ComposeFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public tostring(): string {
    return `${ShortSwipeContextMenuOpenReplyComposeAction.type}`
  }
}

export class ShortSwipeContextMenuOpenForwardComposeAction implements MBTAction {
  public static readonly type: MBTActionType = 'ShortSwipeContextMenuOpenForwardComposeAction'

  public constructor(private readonly order: Int32) {}

  public canBePerformed(model: App): Throwing<boolean> {
    return ![DefaultFolderName.draft, DefaultFolderName.template].includes(
      ContainerGetterFeature.get.forceCast(model).getCurrentContainer().name,
    )
  }

  public events(): EventusEvent[] {
    return [
      Eventus.messageListEvents.openMessageActions(this.order, fakeMid()),
      Eventus.messageListEvents.forwardMessage(this.order, fakeMid()),
    ]
  }

  public getActionType(): string {
    return ShortSwipeContextMenuOpenReplyComposeAction.type
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    const modelContextMenu = ContextMenuFeature.get.forceCast(model)
    const appContextMenu = ContextMenuFeature.get.forceCast(application)
    modelContextMenu.openFromShortSwipe(this.order)
    appContextMenu.openFromShortSwipe(this.order)
    modelContextMenu.openForwardCompose()
    appContextMenu.openForwardCompose()
    return new ComposeComponent()
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      MessageListDisplayFeature.get.included(modelFeatures) &&
      ContextMenuFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      ComposeFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public tostring(): string {
    return `${ShortSwipeContextMenuOpenForwardComposeAction.type}`
  }
}

export class ShortSwipeContextMenuMarkAsReadAction extends BaseShortSwipeContextMenuAction {
  public static readonly type: MBTActionType = 'ShortSwipeContextMenuMarkAsReadAction'

  public constructor(order: Int32) {
    super(order, ShortSwipeContextMenuMarkAsReadAction.type)
  }

  public performImpl(modelOrApplication: ContextMenu): Throwing<void> {
    modelOrApplication.openFromShortSwipe(this.order)
    modelOrApplication.markAsRead()
  }

  public canBePerformedImpl(message: MessageView): Throwing<boolean> {
    return MarkAsRead.canMarkRead(message)
  }

  public events(): EventusEvent[] {
    return [
      Eventus.messageListEvents.openMessageActions(this.order, fakeMid()),
      Eventus.messageListEvents.markMessageAsRead(this.order, fakeMid()),
    ]
  }
}

export class ShortSwipeContextMenuMarkAsUnreadAction extends BaseShortSwipeContextMenuAction {
  public static readonly type: MBTActionType = 'ShortSwipeContextMenuMarkAsUnreadAction'

  public constructor(order: Int32) {
    super(order, ShortSwipeContextMenuMarkAsUnreadAction.type)
  }

  public performImpl(modelOrApplication: ContextMenu): Throwing<void> {
    modelOrApplication.openFromShortSwipe(this.order)
    modelOrApplication.markAsUnread()
  }

  public canBePerformedImpl(message: MessageView): Throwing<boolean> {
    return MarkAsUnread.canMarkUnread(message)
  }

  public events(): EventusEvent[] {
    return [
      Eventus.messageListEvents.openMessageActions(this.order, fakeMid()),
      Eventus.messageListEvents.markMessageAsUnread(this.order, fakeMid()),
    ]
  }
}

export class ShortSwipeContextMenuMarkAsImportantAction extends BaseShortSwipeContextMenuAction {
  public static readonly type: MBTActionType = 'ShortSwipeContextMenuMarkAsImportantAction'

  public constructor(order: Int32) {
    super(order, ShortSwipeContextMenuMarkAsImportantAction.type)
  }

  public performImpl(modelOrApplication: ContextMenu): Throwing<void> {
    modelOrApplication.openFromShortSwipe(this.order)
    modelOrApplication.markAsImportant()
  }

  public canBePerformedImpl(message: MessageView): Throwing<boolean> {
    return MarkAsImportant.canMarkImportant(message)
  }

  public events(): EventusEvent[] {
    return [
      Eventus.messageListEvents.openMessageActions(this.order, fakeMid()),
      Eventus.messageListEvents.markMessageAsImportant(this.order, fakeMid()),
    ]
  }
}

export class ShortSwipeContextMenuMarkAsUnimportantAction extends BaseShortSwipeContextMenuAction {
  public static readonly type: MBTActionType = 'ShortSwipeContextMenuMarkAsUnimportantAction'

  public constructor(order: Int32) {
    super(order, ShortSwipeContextMenuMarkAsUnimportantAction.type)
  }

  public performImpl(modelOrApplication: ContextMenu): Throwing<void> {
    modelOrApplication.openFromShortSwipe(this.order)
    modelOrApplication.markAsUnimportant()
  }

  public canBePerformedImpl(message: MessageView): Throwing<boolean> {
    return MarkAsUnimportant.canMarkUnimportant(message)
  }

  public events(): EventusEvent[] {
    return [
      Eventus.messageListEvents.openMessageActions(1, fakeMid()),
      Eventus.messageListEvents.markMessageAsNotImportant(1, fakeMid()),
    ]
  }
}

export class ShortSwipeContextMenuOpenMoveToFolderAction implements MBTAction {
  public static readonly type: MBTActionType = 'ShortSwipeContextMenuOpenMoveToFolderAction'

  public constructor(private readonly order: Int32) {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      FolderNavigatorFeature.get.included(modelFeatures) &&
      MoveToFolderFeature.get.included(modelFeatures) &&
      ContextMenuFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public events(): EventusEvent[] {
    return [Eventus.messageListEvents.openMessageActions(this.order, fakeMid())]
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    const modelContextMenu = ContextMenuFeature.get.forceCast(model)
    const appContextMenu = ContextMenuFeature.get.forceCast(application)
    modelContextMenu.openFromShortSwipe(this.order)
    appContextMenu.openFromShortSwipe(this.order)
    modelContextMenu.openMoveToFolderScreen()
    appContextMenu.openMoveToFolderScreen()
    return new MoveToFolderComponent()
  }

  public tostring(): string {
    return `${ShortSwipeContextMenuOpenMoveToFolderAction.type}${this.order}`
  }

  public getActionType(): MBTActionType {
    return ShortSwipeContextMenuOpenMoveToFolderAction.type
  }
}

export class ShortSwipeContextMenuMoveToFolderAction implements MBTAction {
  public static readonly type: MBTActionType = 'ShortSwipeContextMenuOpenMoveToFolderAction'

  public constructor(private readonly order: Int32, private readonly folderName: FolderName) {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      FolderNavigatorFeature.get.included(modelFeatures) &&
      MoveToFolderFeature.get.included(modelFeatures) &&
      ContextMenuFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const folderList = FolderNavigatorFeature.get.forceCast(model).getFoldersList()
    return folderList.has(this.folderName)
  }

  public events(): EventusEvent[] {
    return [
      Eventus.messageListEvents.openMessageActions(this.order, fakeMid()),
      Eventus.messageListEvents.moveMessageToFolder(this.order, fakeMid()),
    ]
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    const modelContextMenu = ContextMenuFeature.get.forceCast(model)
    const appContextMenu = ContextMenuFeature.get.forceCast(application)
    modelContextMenu.openFromShortSwipe(this.order)
    appContextMenu.openFromShortSwipe(this.order)
    modelContextMenu.openMoveToFolderScreen()
    appContextMenu.openMoveToFolderScreen()

    MoveToFolderFeature.get.forceCast(model).tapOnFolder(this.folderName)
    MoveToFolderFeature.get.forceCast(application).tapOnFolder(this.folderName)

    return history.currentComponent
  }

  public tostring(): string {
    return `${ShortSwipeContextMenuOpenMoveToFolderAction.type}${this.order}`
  }

  public getActionType(): MBTActionType {
    return ShortSwipeContextMenuOpenMoveToFolderAction.type
  }
}

export class ShortSwipeContextMenuArchiveAction implements MBTAction {
  public static readonly type: MBTActionType = 'ShortSwipeContextMenuArchiveAction'

  public constructor(private readonly order: Int32) {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      ContainerGetterFeature.get.included(modelFeatures) &&
      ContextMenuFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const isInArchiveFolder =
      ContainerGetterFeature.get.forceCast(model).getCurrentContainer().name === DefaultFolderName.archive
    return !isInArchiveFolder
  }

  public events(): EventusEvent[] {
    return [
      Eventus.messageListEvents.openMessageActions(this.order, fakeMid()),
      Eventus.messageListEvents.archiveMessage(this.order, fakeMid()),
    ]
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    const modelContextMenu = ContextMenuFeature.get.forceCast(model)
    const appContextMenu = ContextMenuFeature.get.forceCast(application)
    modelContextMenu.openFromShortSwipe(this.order)
    appContextMenu.openFromShortSwipe(this.order)
    modelContextMenu.archive()
    appContextMenu.archive()
    return history.currentComponent
  }

  public tostring(): string {
    return `${ShortSwipeContextMenuArchiveAction.type}${this.order}`
  }

  public getActionType(): MBTActionType {
    return ShortSwipeContextMenuArchiveAction.type
  }
}

// MessageViewContextMenuActions
export class MessageViewOpenContextMenuAction implements MBTAction {
  public static readonly type: MBTActionType = 'MessageViewOpenContextMenuAction'

  public constructor() {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return ContextMenuFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    return MessageViewerFeature.get.forceCast(model).isMessageOpened()
  }

  public events(): EventusEvent[] {
    return []
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ContextMenuFeature.get.forceCast(model).openFromMessageView()
    ContextMenuFeature.get.forceCast(application).openFromMessageView()
    return new ContextMenuComponent()
  }

  public tostring(): string {
    return `${MessageViewOpenContextMenuAction.type}`
  }

  public getActionType(): MBTActionType {
    return MessageViewOpenContextMenuAction.type
  }
}

export class MessageViewContextMenuOpenApplyLabelsAction implements MBTAction {
  public static readonly type: MBTActionType = 'MessageViewContextMenuOpenApplyLabelsAction'

  public constructor() {}

  public canBePerformed(model: App): Throwing<boolean> {
    return ![DefaultFolderName.spam, DefaultFolderName.trash].includes(
      ContainerGetterFeature.get.forceCast(model).getCurrentContainer().name,
    )
  }

  public events(): EventusEvent[] {
    return [Eventus.messageViewEvents.openMessageActions(0)]
  }

  public getActionType(): string {
    return MessageViewContextMenuOpenApplyLabelsAction.type
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    const modelContextMenu = ContextMenuFeature.get.forceCast(model)
    const appContextMenu = ContextMenuFeature.get.forceCast(application)
    modelContextMenu.openFromMessageView()
    appContextMenu.openFromMessageView()
    modelContextMenu.openApplyLabelsScreen()
    appContextMenu.openApplyLabelsScreen()
    return new ApplyLabelComponent()
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      MessageListDisplayFeature.get.included(modelFeatures) &&
      ContextMenuFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public tostring(): string {
    return `${MessageViewContextMenuOpenApplyLabelsAction.type}`
  }
}

export class MessageViewContextMenuApplyLabelsAction implements MBTAction {
  public static readonly type: MBTActionType = 'MessageViewContextMenuApplyLabelsAction'

  public constructor(private readonly labels: LabelName[]) {}

  public canBePerformed(model: App): Throwing<boolean> {
    return ![DefaultFolderName.spam, DefaultFolderName.trash].includes(
      ContainerGetterFeature.get.forceCast(model).getCurrentContainer().name,
    )
  }

  public events(): EventusEvent[] {
    return [Eventus.messageViewEvents.openMessageActions(0), Eventus.messageActionsEvents.markAs()]
  }

  public getActionType(): string {
    return MessageViewContextMenuApplyLabelsAction.type
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    const modelContextMenu = ContextMenuFeature.get.forceCast(model)
    const appContextMenu = ContextMenuFeature.get.forceCast(application)
    modelContextMenu.openFromMessageView()
    appContextMenu.openFromMessageView()
    modelContextMenu.openApplyLabelsScreen()
    appContextMenu.openApplyLabelsScreen()

    const modelApplyLabel = ApplyLabelFeature.get.forceCast(model)
    const appApplyLabel = ApplyLabelFeature.get.forceCast(application)
    modelApplyLabel.selectLabelsToAdd(this.labels)
    appApplyLabel.selectLabelsToAdd(this.labels)
    modelApplyLabel.tapOnDoneButton()
    appApplyLabel.tapOnDoneButton()

    return history.currentComponent
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      MessageListDisplayFeature.get.included(modelFeatures) &&
      ContextMenuFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      MessageViewerFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public tostring(): string {
    return `${MessageViewContextMenuApplyLabelsAction.type}`
  }
}

export class MessageViewContextMenuRemoveLabelsAction implements MBTAction {
  public static readonly type: MBTActionType = 'MessageViewContextMenuRemoveLabelsAction'

  public constructor(private readonly labels: LabelName[]) {}

  public canBePerformed(model: App): Throwing<boolean> {
    const modelApplyLabel = ApplyLabelFeature.get.forceCast(model)
    const selectedLabels = modelApplyLabel.getSelectedLabels()
    for (const label of this.labels) {
      if (!selectedLabels.includes(label)) {
        return false
      }
    }
    return ![DefaultFolderName.spam, DefaultFolderName.trash].includes(
      ContainerGetterFeature.get.forceCast(model).getCurrentContainer().name,
    )
  }

  public events(): EventusEvent[] {
    return [Eventus.messageViewEvents.openMessageActions(0), Eventus.messageActionsEvents.markAs()]
  }

  public getActionType(): string {
    return MessageViewContextMenuRemoveLabelsAction.type
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    const modelContextMenu = ContextMenuFeature.get.forceCast(model)
    const appContextMenu = ContextMenuFeature.get.forceCast(application)
    modelContextMenu.openFromMessageView()
    appContextMenu.openFromMessageView()
    modelContextMenu.openApplyLabelsScreen()
    appContextMenu.openApplyLabelsScreen()

    const modelApplyLabel = ApplyLabelFeature.get.forceCast(model)
    const appApplyLabel = ApplyLabelFeature.get.forceCast(application)
    modelApplyLabel.deselectLabelsToRemove(this.labels)
    appApplyLabel.deselectLabelsToRemove(this.labels)
    modelApplyLabel.tapOnDoneButton()
    appApplyLabel.tapOnDoneButton()

    return history.currentComponent
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      MessageListDisplayFeature.get.included(modelFeatures) &&
      ContextMenuFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      MessageViewerFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public tostring(): string {
    return `${MessageViewContextMenuRemoveLabelsAction.type}`
  }
}

export class MessageViewContextMenuOpenMoveToFolderAction implements MBTAction {
  public static readonly type: MBTActionType = 'MessageViewContextMenuOpenMoveToFolderAction'

  public constructor() {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      FolderNavigatorFeature.get.included(modelFeatures) &&
      MoveToFolderFeature.get.included(modelFeatures) &&
      ContextMenuFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public events(): EventusEvent[] {
    return [Eventus.messageViewEvents.openMessageActions(0)]
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    const modelContextMenu = ContextMenuFeature.get.forceCast(model)
    const appContextMenu = ContextMenuFeature.get.forceCast(application)
    modelContextMenu.openFromMessageView()
    appContextMenu.openFromMessageView()
    modelContextMenu.openMoveToFolderScreen()
    appContextMenu.openMoveToFolderScreen()
    return new MoveToFolderComponent()
  }

  public tostring(): string {
    return `${MessageViewContextMenuOpenMoveToFolderAction.type}`
  }

  public getActionType(): MBTActionType {
    return MessageViewContextMenuOpenMoveToFolderAction.type
  }
}

export class MessageViewContextMenuMoveToFolderAction implements MBTAction {
  public static readonly type: MBTActionType = 'MessageViewContextMenuMoveToFolderAction'

  public constructor(private readonly folderName: FolderName) {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      FolderNavigatorFeature.get.included(modelFeatures) &&
      MoveToFolderFeature.get.included(modelFeatures) &&
      ContextMenuFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const folderList = FolderNavigatorFeature.get.forceCast(model).getFoldersList()
    return folderList.has(this.folderName)
  }

  public events(): EventusEvent[] {
    return [Eventus.messageViewEvents.openMessageActions(0), Eventus.messageActionsEvents.moveToFolder()]
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    const modelContextMenu = ContextMenuFeature.get.forceCast(model)
    const appContextMenu = ContextMenuFeature.get.forceCast(application)
    modelContextMenu.openFromMessageView()
    appContextMenu.openFromMessageView()
    modelContextMenu.openMoveToFolderScreen()
    appContextMenu.openMoveToFolderScreen()

    MoveToFolderFeature.get.forceCast(model).tapOnFolder(this.folderName)
    MoveToFolderFeature.get.forceCast(application).tapOnFolder(this.folderName)

    return history.currentComponent
  }

  public tostring(): string {
    return `${MessageViewContextMenuMoveToFolderAction.type}`
  }

  public getActionType(): MBTActionType {
    return MessageViewContextMenuMoveToFolderAction.type
  }
}

export class MessageViewContextMenuDeleteAction implements MBTAction {
  public static readonly type: MBTActionType = 'MessageViewContextMenuDeleteAction'

  public constructor() {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return ContextMenuFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public events(): EventusEvent[] {
    return [Eventus.messageViewEvents.openMessageActions(0), Eventus.messageActionsEvents.delete()]
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    const modelContextMenu = ContextMenuFeature.get.forceCast(model)
    const appContextMenu = ContextMenuFeature.get.forceCast(application)
    modelContextMenu.openFromMessageView()
    appContextMenu.openFromMessageView()
    modelContextMenu.deleteMessage()
    appContextMenu.deleteMessage()
    return new MaillistComponent()
  }

  public tostring(): string {
    return `${MessageViewContextMenuDeleteAction.type}`
  }

  public getActionType(): MBTActionType {
    return MessageViewContextMenuDeleteAction.type
  }
}

export class MessageViewContextMenuMarkAsSpamAction implements MBTAction {
  public static readonly type: MBTActionType = 'MessageViewContextMenuMarkAsSpamAction'

  public constructor() {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return ContextMenuFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    return ![
      DefaultFolderName.sent,
      DefaultFolderName.spam,
      DefaultFolderName.draft,
      DefaultFolderName.template,
    ].includes(ContainerGetterFeature.get.forceCast(model).getCurrentContainer().name)
  }

  public events(): EventusEvent[] {
    return [Eventus.messageViewEvents.openMessageActions(0), Eventus.messageActionsEvents.markAsSpam()]
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    const modelContextMenu = ContextMenuFeature.get.forceCast(model)
    const appContextMenu = ContextMenuFeature.get.forceCast(application)
    modelContextMenu.openFromMessageView()
    appContextMenu.openFromMessageView()
    modelContextMenu.markAsSpam()
    appContextMenu.markAsSpam()
    return new MaillistComponent()
  }

  public tostring(): string {
    return `${MessageViewContextMenuMarkAsSpamAction.type}`
  }

  public getActionType(): MBTActionType {
    return MessageViewContextMenuMarkAsSpamAction.type
  }
}

export class MessageViewContextMenuMarkAsNotSpamAction implements MBTAction {
  public static readonly type: MBTActionType = 'MessageViewContextMenuMarkAsNotSpamAction'

  public constructor() {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return ContextMenuFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    return ContainerGetterFeature.get.forceCast(model).getCurrentContainer().name === DefaultFolderName.spam
  }

  public events(): EventusEvent[] {
    return [Eventus.messageViewEvents.openMessageActions(0), Eventus.messageActionsEvents.markAsNotSpam()]
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    const modelContextMenu = ContextMenuFeature.get.forceCast(model)
    const appContextMenu = ContextMenuFeature.get.forceCast(application)
    modelContextMenu.openFromMessageView()
    appContextMenu.openFromMessageView()
    modelContextMenu.markAsNotSpam()
    appContextMenu.markAsNotSpam()
    return new MaillistComponent()
  }

  public tostring(): string {
    return `${MessageViewContextMenuMarkAsNotSpamAction.type}`
  }

  public getActionType(): MBTActionType {
    return MessageViewContextMenuMarkAsNotSpamAction.type
  }
}

export class MessageViewContextMenuMarkAsReadAction extends BaseMailViewContextMenuAction {
  public static readonly type: MBTActionType = 'MessageViewContextMenuMarkAsReadAction'

  public constructor() {
    super(MessageViewContextMenuMarkAsReadAction.type)
  }

  public requiredFeature(): Feature<ContextMenu> {
    return ContextMenuFeature.get
  }

  public canBePerformedImpl(message: MessageView): Throwing<boolean> {
    return MarkAsRead.canMarkRead(message)
  }

  public performImpl(modelOrApplication: ContextMenu): Throwing<void> {
    modelOrApplication.openFromMessageView()
    modelOrApplication.markAsRead()
  }

  public events(): EventusEvent[] {
    return [Eventus.messageViewEvents.openMessageActions(0), Eventus.messageActionsEvents.markAsRead()]
  }
}

export class MessageViewContextMenuMarkAsUnreadAction extends BaseMailViewContextMenuAction {
  public static readonly type: MBTActionType = 'MessageViewContextMenuMarkAsUnreadAction'

  public constructor() {
    super(MessageViewContextMenuMarkAsUnreadAction.type)
  }

  public requiredFeature(): Feature<ContextMenu> {
    return ContextMenuFeature.get
  }

  public canBePerformedImpl(message: MessageView): Throwing<boolean> {
    return MarkAsUnread.canMarkUnread(message)
  }

  public performImpl(modelOrApplication: ContextMenu): Throwing<void> {
    modelOrApplication.openFromMessageView()
    modelOrApplication.markAsUnread()
  }

  public events(): EventusEvent[] {
    return [Eventus.messageViewEvents.openMessageActions(0), Eventus.messageActionsEvents.markAsUnread()]
  }
}

export class MessageViewContextMenuMarkAsImportantAction extends BaseMailViewContextMenuAction {
  public static readonly type: MBTActionType = 'MessageViewContextMenuMarkAsImportantAction'

  public constructor() {
    super(MessageViewContextMenuMarkAsImportantAction.type)
  }

  public requiredFeature(): Feature<ContextMenu> {
    return ContextMenuFeature.get
  }

  public canBePerformedImpl(message: MessageView): Throwing<boolean> {
    return MarkAsImportant.canMarkImportant(message)
  }

  public performImpl(modelOrApplication: ContextMenu): Throwing<void> {
    modelOrApplication.openFromMessageView()
    modelOrApplication.markAsImportant()
  }

  public events(): EventusEvent[] {
    return [Eventus.messageViewEvents.openMessageActions(0), Eventus.messageActionsEvents.markAsImportant()]
  }
}

export class MessageViewContextMenuMarkAsUnimportantAction extends BaseMailViewContextMenuAction {
  public static readonly type: MBTActionType = 'MessageViewContextMenuMarkAsUnimportantAction'

  public constructor() {
    super(MessageViewContextMenuMarkAsUnimportantAction.type)
  }

  public requiredFeature(): Feature<ContextMenu> {
    return ContextMenuFeature.get
  }

  public canBePerformedImpl(message: MessageView): Throwing<boolean> {
    return MarkAsUnimportant.canMarkUnimportant(message)
  }

  public performImpl(modelOrApplication: ContextMenu): Throwing<void> {
    modelOrApplication.openFromMessageView()
    modelOrApplication.markAsUnimportant()
  }

  public events(): EventusEvent[] {
    return [Eventus.messageViewEvents.openMessageActions(0), Eventus.messageActionsEvents.markAsNotImportant()]
  }
}

export class MessageViewContextMenuArchiveAction implements MBTAction {
  public static readonly type: MBTActionType = 'MessageViewContextMenuArchiveAction'

  public constructor() {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      ContainerGetterFeature.get.included(modelFeatures) &&
      ContextMenuFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const isInArchiveFolder =
      ContainerGetterFeature.get.forceCast(model).getCurrentContainer().name === DefaultFolderName.archive
    return !isInArchiveFolder
  }

  public events(): EventusEvent[] {
    return [Eventus.messageViewEvents.openMessageActions(0), Eventus.messageActionsEvents.archive()]
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    const modelContextMenu = ContextMenuFeature.get.forceCast(model)
    const appContextMenu = ContextMenuFeature.get.forceCast(application)
    modelContextMenu.openFromMessageView()
    appContextMenu.openFromMessageView()
    modelContextMenu.archive()
    appContextMenu.archive()
    return new MaillistComponent()
  }

  public tostring(): string {
    return `${MessageViewContextMenuArchiveAction.type}`
  }

  public getActionType(): MBTActionType {
    return MessageViewContextMenuArchiveAction.type
  }
}

export class MessageViewContextMenuShowTranslatorAction implements MBTAction {
  public static readonly type: MBTActionType = 'MessageViewContextMenuShowTranslatorAction'

  public constructor() {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      TranslatorBarFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      ContextMenuFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public events(): EventusEvent[] {
    return [Eventus.messageViewEvents.openMessageActions(0)]
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    const modelContextMenu = ContextMenuFeature.get.forceCast(model)
    const appContextMenu = ContextMenuFeature.get.forceCast(application)
    modelContextMenu.openFromMessageView()
    appContextMenu.openFromMessageView()
    modelContextMenu.showTranslator()
    appContextMenu.showTranslator()
    return history.currentComponent
  }

  public tostring(): string {
    return `${MessageViewContextMenuShowTranslatorAction.type}`
  }

  public getActionType(): MBTActionType {
    return MessageViewContextMenuShowTranslatorAction.type
  }
}

export class MessageViewContextMenuOpenReplyComposeAction implements MBTAction {
  public static readonly type: MBTActionType = 'MessageViewContextMenuOpenReplyComposeAction'

  public constructor() {}

  public canBePerformed(model: App): Throwing<boolean> {
    return ![DefaultFolderName.draft, DefaultFolderName.template].includes(
      ContainerGetterFeature.get.forceCast(model).getCurrentContainer().name,
    )
  }

  public events(): EventusEvent[] {
    return [Eventus.messageViewEvents.openMessageActions(0), Eventus.messageViewEvents.reply(0)]
  }

  public getActionType(): string {
    return MessageViewContextMenuOpenReplyComposeAction.type
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    const modelContextMenu = ContextMenuFeature.get.forceCast(model)
    const appContextMenu = ContextMenuFeature.get.forceCast(application)
    modelContextMenu.openFromMessageView()
    appContextMenu.openFromMessageView()
    modelContextMenu.openReplyCompose()
    appContextMenu.openReplyCompose()
    return new ComposeComponent()
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      ContextMenuFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      ComposeFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public tostring(): string {
    return `${MessageViewContextMenuOpenReplyComposeAction.type}`
  }
}

export class MessageViewContextMenuOpenForwardComposeAction implements MBTAction {
  public static readonly type: MBTActionType = 'MessageViewContextMenuOpenForwardComposeAction'

  public constructor() {}

  public canBePerformed(model: App): Throwing<boolean> {
    return ![DefaultFolderName.draft, DefaultFolderName.template].includes(
      ContainerGetterFeature.get.forceCast(model).getCurrentContainer().name,
    )
  }

  public events(): EventusEvent[] {
    return [Eventus.messageViewEvents.openMessageActions(0), Eventus.messageViewEvents.reply(0)]
  }

  public getActionType(): string {
    return MessageViewContextMenuOpenForwardComposeAction.type
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    const modelContextMenu = ContextMenuFeature.get.forceCast(model)
    const appContextMenu = ContextMenuFeature.get.forceCast(application)
    modelContextMenu.openFromMessageView()
    appContextMenu.openFromMessageView()
    modelContextMenu.openForwardCompose()
    appContextMenu.openForwardCompose()
    return new ComposeComponent()
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      ContextMenuFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      ComposeFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public tostring(): string {
    return `${MessageViewContextMenuOpenForwardComposeAction.type}`
  }
}
