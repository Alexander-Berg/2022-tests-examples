import { Int32, int64, Throwing } from '../../../../../../common/ys'
import { Eventus } from '../../../../../eventus/code/events/eventus'
import { EventusEvent } from '../../../../../eventus-common/code/eventus-event'
import {
  App,
  FeatureID,
  MBTAction,
  MBTActionType,
  MBTComponent,
  MBTHistory,
} from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { ArchiveMessageFeature } from '../../feature/base-action-features'
import { ContainerGetterFeature, MessageContainerType } from '../../feature/message-list/container-getter-feature'
import { MessageListDisplayFeature } from '../../feature/message-list/message-list-display-feature'
import { DefaultFolderName } from '../../model/folder-data-model'

export class ArchiveMessageAction implements MBTAction {
  public static readonly type: MBTActionType = 'ArchiveMessage'

  public constructor(private order: Int32) {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      MessageListDisplayFeature.get.included(modelFeatures) &&
      ContainerGetterFeature.get.included(modelFeatures) &&
      ArchiveMessageFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const messageListDisplayModel = MessageListDisplayFeature.get.forceCast(model)
    const messages = messageListDisplayModel.getMessageList(this.order + 1)
    const currentContainer = ContainerGetterFeature.get.forceCast(model).getCurrentContainer()
    return (
      currentContainer.type === MessageContainerType.folder &&
      currentContainer.name !== DefaultFolderName.archive &&
      this.order < messages.length
    )
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ArchiveMessageFeature.get.forceCast(model).archiveMessage(this.order)
    ArchiveMessageFeature.get.forceCast(application).archiveMessage(this.order)
    return history.currentComponent
  }

  public getActionType(): MBTActionType {
    return ArchiveMessageAction.type
  }

  public tostring(): string {
    return `MoveToArchive(#${this.order})`
  }

  public events(): EventusEvent[] {
    return [
      Eventus.messageListEvents.openMessageActions(this.order, int64(-1)),
      Eventus.messageListEvents.archiveMessage(this.order, int64(-1)),
    ]
  }
}
