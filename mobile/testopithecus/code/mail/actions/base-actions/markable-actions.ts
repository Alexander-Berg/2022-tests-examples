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
import { MarkableRead, MarkableReadFeature } from '../../feature/base-action-features'
import { MessageView } from '../../feature/mail-view-features'
import { MessageListDisplayFeature } from '../../feature/message-list/message-list-display-feature'

export abstract class BaseMarkAction implements MBTAction {
  protected constructor(protected order: Int32) {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      MessageListDisplayFeature.get.included(modelFeatures) &&
      MarkableReadFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const messageListModel = MessageListDisplayFeature.get.forceCast(model)
    const messages = messageListModel.getMessageList(10)
    const canPerform = this.canBePerformedImpl(messages[this.order])
    return this.order < messages.length && canPerform
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    this.performImpl(MarkableReadFeature.get.forceCast(model))
    this.performImpl(MarkableReadFeature.get.forceCast(application))
    return history.currentComponent
  }

  public abstract events(): EventusEvent[]

  public abstract canBePerformedImpl(message: MessageView): Throwing<boolean>

  public abstract performImpl(modelOrApplication: MarkableRead): Throwing<void>

  public abstract tostring(): string

  public abstract getActionType(): MBTActionType
}

export class MarkAsRead extends BaseMarkAction {
  public static readonly type: MBTActionType = 'MarkAsRead'

  public constructor(order: Int32) {
    super(order)
  }

  public static canMarkRead(message: MessageView): boolean {
    return !message.read
  }

  public canBePerformedImpl(message: MessageView): Throwing<boolean> {
    return MarkAsRead.canMarkRead(message)
  }

  public performImpl(modelOrApplication: MarkableRead): Throwing<void> {
    return modelOrApplication.markAsRead(this.order)
  }

  public events(): EventusEvent[] {
    return [Eventus.messageListEvents.markMessageAsRead(this.order, int64(-1))]
  }

  public tostring(): string {
    return `MarkAsRead(#${this.order})`
  }

  public getActionType(): MBTActionType {
    return MarkAsRead.type
  }
}

export class MarkAsUnread extends BaseMarkAction {
  public static readonly type: MBTActionType = 'MarkAsUnread'

  public constructor(order: Int32) {
    super(order)
  }

  public static canMarkUnread(message: MessageView): boolean {
    return message.read
  }

  public canBePerformedImpl(message: MessageView): Throwing<boolean> {
    return MarkAsUnread.canMarkUnread(message)
  }

  public events(): EventusEvent[] {
    return [Eventus.messageListEvents.markMessageAsUnread(this.order, int64(-1))]
  }

  public performImpl(modelOrApplication: MarkableRead): Throwing<void> {
    return modelOrApplication.markAsUnread(this.order)
  }

  public tostring(): string {
    return `MarkAsUnread(#${this.order})`
  }

  public getActionType(): MBTActionType {
    return MarkAsUnread.type
  }
}
