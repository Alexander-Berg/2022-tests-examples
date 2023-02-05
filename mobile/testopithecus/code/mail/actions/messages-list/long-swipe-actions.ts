import { Int32, Throwing } from '../../../../../../common/ys'
import { EventusEvent } from '../../../../../eventus-common/code/eventus-event'
import { Eventus } from '../../../../../eventus/code/events/eventus'
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
import { LongSwipeFeature } from '../../feature/message-list/long-swipe-feature'
import { MessageListDisplayFeature } from '../../feature/message-list/message-list-display-feature'
import { ActionOnSwipe, GeneralSettingsFeature } from '../../feature/settings/general-settings-feature'
import { DefaultFolderName } from '../../model/folder-data-model'

export class DeleteMessageByLongSwipeAction implements MBTAction {
  public static readonly type: MBTActionType = 'DeleteMessageByLongSwipeAction'
  public constructor(protected order: Int32, protected confirmDeletionIfNeeded: boolean = true) {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return LongSwipeFeature.get.includedAll(modelFeatures, applicationFeatures)
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
    LongSwipeFeature.get.forceCast(model).deleteMessageByLongSwipe(this.order, this.confirmDeletionIfNeeded)
    LongSwipeFeature.get.forceCast(application).deleteMessageByLongSwipe(this.order, this.confirmDeletionIfNeeded)
    return history.currentComponent
  }

  public events(): EventusEvent[] {
    return [Eventus.messageListEvents.deleteMessage(this.order, fakeMid())]
  }

  public getActionType(): MBTActionType {
    return DeleteMessageByLongSwipeAction.type
  }

  public tostring(): string {
    return `DeleteMessageByLongSwipeAction(${this.order})`
  }
}

export class ArchiveMessageByLongSwipeAction implements MBTAction {
  public static readonly type: MBTActionType = 'ArchiveMessageByLongSwipeAction'
  public constructor(protected order: Int32) {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return LongSwipeFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const messageListModel = MessageListDisplayFeature.get.forceCast(model)
    const messages = messageListModel.getMessageList(10)
    const actionOnSwipe = GeneralSettingsFeature.get.forceCast(model).getActionOnSwipe()
    const currentContainer = ContainerGetterFeature.get.forceCast(model).getCurrentContainer()
    return (
      this.order < messages.length &&
      actionOnSwipe === ActionOnSwipe.archive &&
      [MessageContainerType.folder, MessageContainerType.label, MessageContainerType.search].includes(
        currentContainer.type,
      ) &&
      currentContainer.name !== DefaultFolderName.archive
    )
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    LongSwipeFeature.get.forceCast(model).archiveMessageByLongSwipe(this.order)
    LongSwipeFeature.get.forceCast(application).archiveMessageByLongSwipe(this.order)
    return history.currentComponent
  }

  public events(): EventusEvent[] {
    return [] // TODO: add archive
  }

  public getActionType(): MBTActionType {
    return ArchiveMessageByLongSwipeAction.type
  }

  public tostring(): string {
    return `ArchiveMessageByLongSwipeAction(${this.order})`
  }
}
