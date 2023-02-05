import { Eventus } from '../../../../../eventus/code/events/eventus'
import { Int32, Throwing } from '../../../../../../common/ys'
import { EventusEvent } from '../../../../../eventus-common/code/eventus-event'
import {
  App,
  FeatureID,
  MBTAction,
  MBTActionType,
  MBTComponent,
  MBTHistory,
} from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { fakeMid } from '../../../utils/mail-utils'
import { ContainerGetterFeature, MessageContainerType } from '../../feature/message-list/container-getter-feature'
import { MessageListDisplayFeature } from '../../feature/message-list/message-list-display-feature'
import { ShortSwipeFeature } from '../../feature/message-list/short-swipe-feature'
import { ActionOnSwipe, GeneralSettingsFeature } from '../../feature/settings/general-settings-feature'
import { DefaultFolderName } from '../../model/folder-data-model'

export class DeleteMessageByShortSwipeAction implements MBTAction {
  public static readonly type: MBTActionType = 'DeleteMessageByShortSwipeAction'
  public constructor(protected order: Int32) {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return ShortSwipeFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const messageListModel = MessageListDisplayFeature.get.forceCast(model)
    const messages = messageListModel.getMessageList(10)
    const actionOnSwipe = GeneralSettingsFeature.get.forceCast(model).getActionOnSwipe()
    const currentContainer = ContainerGetterFeature.get.forceCast(model).getCurrentContainer()
    return (
      this.order < messages.length &&
      (actionOnSwipe === ActionOnSwipe.delete || currentContainer.name === DefaultFolderName.archive)
    )
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ShortSwipeFeature.get.forceCast(model).deleteMessageByShortSwipe(this.order)
    ShortSwipeFeature.get.forceCast(application).deleteMessageByShortSwipe(this.order)
    return history.currentComponent
  }

  public events(): EventusEvent[] {
    return [Eventus.messageListEvents.deleteMessage(this.order, fakeMid())]
  }

  public getActionType(): MBTActionType {
    return DeleteMessageByShortSwipeAction.type
  }

  public tostring(): string {
    return `DeleteMessageByShortSwipeAction(${this.order})`
  }
}

export class ArchiveMessageByShortSwipeAction implements MBTAction {
  public static readonly type: MBTActionType = 'ArchiveMessageByShortSwipeAction'
  public constructor(protected order: Int32) {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return ShortSwipeFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const messageListModel = MessageListDisplayFeature.get.forceCast(model)
    const messages = messageListModel.getMessageList(10)
    const actionOnSwipe = GeneralSettingsFeature.get.forceCast(model).getActionOnSwipe()
    const currentContainer = ContainerGetterFeature.get.forceCast(model).getCurrentContainer()
    return (
      this.order < messages.length &&
      actionOnSwipe === ActionOnSwipe.archive &&
      (currentContainer.type === MessageContainerType.folder ||
        currentContainer.type === MessageContainerType.search) &&
      currentContainer.name !== DefaultFolderName.archive
    )
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ShortSwipeFeature.get.forceCast(model).archiveMessageByShortSwipe(this.order)
    ShortSwipeFeature.get.forceCast(application).archiveMessageByShortSwipe(this.order)
    return history.currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }

  public getActionType(): MBTActionType {
    return ArchiveMessageByShortSwipeAction.type
  }

  public tostring(): string {
    return `ArchiveMessageByShortSwipeAction(${this.order})`
  }
}

export class MarkAsUnreadFromShortSwipeAction implements MBTAction {
  public static readonly type: MBTActionType = 'MarkAsUnreadFromShortSwipeAction'
  public constructor(protected order: Int32) {}

  public canBePerformed(model: App): Throwing<boolean> {
    const messageListModel = MessageListDisplayFeature.get.forceCast(model)
    const messages = messageListModel.getMessageList(10)
    return this.order < messages.length && messages[this.order].read
  }

  public events(): EventusEvent[] {
    return [Eventus.messageListEvents.markMessageAsUnread(this.order, fakeMid())]
  }

  public getActionType(): MBTActionType {
    return MarkAsUnreadFromShortSwipeAction.type
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ShortSwipeFeature.get.forceCast(model).markAsUnread(this.order)
    ShortSwipeFeature.get.forceCast(application).markAsUnread(this.order)
    return history.currentComponent
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return ShortSwipeFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public tostring(): string {
    return `MarkAsUnreadFromShortSwipeAction(${this.order})`
  }
}

export class MarkAsReadFromShortSwipeAction implements MBTAction {
  public static readonly type: MBTActionType = 'MarkAsReadFromShortSwipeAction'
  public constructor(protected order: Int32) {}

  public canBePerformed(model: App): Throwing<boolean> {
    const messageListModel = MessageListDisplayFeature.get.forceCast(model)
    const messages = messageListModel.getMessageList(10)
    return this.order < messages.length && !messages[this.order].read
  }

  public events(): EventusEvent[] {
    return [Eventus.messageListEvents.markMessageAsRead(this.order, fakeMid())]
  }

  public getActionType(): MBTActionType {
    return MarkAsReadFromShortSwipeAction.type
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ShortSwipeFeature.get.forceCast(model).markAsRead(this.order)
    ShortSwipeFeature.get.forceCast(application).markAsRead(this.order)
    return history.currentComponent
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return ShortSwipeFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public tostring(): string {
    return `MarkAsReadFromShortSwipeAction(${this.order})`
  }
}
