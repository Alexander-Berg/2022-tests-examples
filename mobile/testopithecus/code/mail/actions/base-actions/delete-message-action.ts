import { Eventus } from '../../../../../eventus/code/events/eventus'
import { EventusEvent } from '../../../../../eventus-common/code/eventus-event'
import { Int32, Throwing } from '../../../../../../common/ys'
import {
  App,
  FeatureID,
  MBTAction,
  MBTActionType,
  MBTComponent,
  MBTHistory,
} from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { fakeMid } from '../../../utils/mail-utils'
import { MaillistComponent } from '../../components/maillist-component'
import { DeleteMessage, DeleteMessageFeature } from '../../feature/base-action-features'
import { ContainerGetterFeature } from '../../feature/message-list/container-getter-feature'
import { LongSwipeFeature } from '../../feature/message-list/long-swipe-feature'
import { MessageListDisplayFeature } from '../../feature/message-list/message-list-display-feature'
import { ActionOnSwipe, GeneralSettingsFeature } from '../../feature/settings/general-settings-feature'
import { DefaultFolderName } from '../../model/folder-data-model'

export abstract class BaseDeleteMessageAction implements MBTAction {
  protected constructor(protected order: Int32) {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      MessageListDisplayFeature.get.included(modelFeatures) &&
      ContainerGetterFeature.get.included(modelFeatures) &&
      DeleteMessageFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      LongSwipeFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
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
    DeleteMessageFeature.get.forceCast(model).deleteMessage(this.order)
    DeleteMessageFeature.get.forceCast(application).deleteMessage(this.order)
    return new MaillistComponent()
  }

  public events(): EventusEvent[] {
    return [Eventus.messageListEvents.deleteMessage(this.order, fakeMid())]
  }

  public tostring(): string {
    return `DeleteMessageByLongSwipe(#${this.order})`
  }

  public abstract getActionType(): MBTActionType
}

export class DeleteMessageAction extends BaseDeleteMessageAction {
  public static readonly type: MBTActionType = 'DeleteMessage'

  public constructor(order: Int32) {
    super(order)
  }

  public performImpl(modelOrApplication: DeleteMessage): Throwing<void> {
    return modelOrApplication.deleteMessage(this.order)
  }

  public getActionType(): MBTActionType {
    return DeleteMessageAction.type
  }
}
