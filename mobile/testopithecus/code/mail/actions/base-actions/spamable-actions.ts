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
import { Spamable, SpamableFeature } from '../../feature/base-action-features'
import { ContainerGetterFeature, MessageContainerType } from '../../feature/message-list/container-getter-feature'
import { MessageListDisplayFeature } from '../../feature/message-list/message-list-display-feature'
import { DefaultFolderName } from '../../model/folder-data-model'

export abstract class BaseSpamAction implements MBTAction {
  protected constructor(protected order: Int32, private type: MBTActionType) {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      MessageListDisplayFeature.get.included(modelFeatures) &&
      ContainerGetterFeature.get.included(modelFeatures) &&
      SpamableFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const messageListDisplayModel = MessageListDisplayFeature.get.forceCast(model)
    const messages = messageListDisplayModel.getMessageList(this.order + 1)
    const canPerform = this.canBePerformedImpl(model)
    return messages.length > this.order && canPerform
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    this.performImpl(SpamableFeature.get.forceCast(model))
    this.performImpl(SpamableFeature.get.forceCast(application))
    return history.currentComponent
  }

  public events(): EventusEvent[] {
    return [Eventus.messageListEvents.openMessageActions(this.order, int64(-1))]
  }

  public getActionType(): MBTActionType {
    return this.type
  }

  public abstract performImpl(modelOrApplication: Spamable): Throwing<void>

  public abstract canBePerformedImpl(model: App): Throwing<boolean>

  public abstract tostring(): string
}

export class MoveFromSpamAction extends BaseSpamAction {
  public static readonly type: MBTActionType = 'MoveFromSpam'

  public constructor(order: Int32) {
    super(order, MoveFromSpamAction.type)
  }

  public getActionType(): MBTActionType {
    return MoveFromSpamAction.type
  }

  public performImpl(modelOrApplication: Spamable): Throwing<void> {
    modelOrApplication.moveFromSpam(this.order)
  }

  public tostring(): string {
    return `${MoveFromSpamAction.type}(#${this.order})`
  }

  public events(): EventusEvent[] {
    const events = super.events()
    events.push(Eventus.messageListEvents.markMessageAsSpam(this.order, int64(-1)))
    return events
  }

  public canBePerformedImpl(model: App): Throwing<boolean> {
    const currentContainer = ContainerGetterFeature.get.forceCast(model).getCurrentContainer()
    return currentContainer.type === MessageContainerType.folder && currentContainer.name === DefaultFolderName.spam
  }
}

export class MoveToSpamAction extends BaseSpamAction {
  public static readonly type: MBTActionType = 'MoveToSpam'

  public constructor(order: Int32) {
    super(order, MoveToSpamAction.type)
  }

  public getActionType(): MBTActionType {
    return MoveToSpamAction.type
  }

  public performImpl(modelOrApplication: Spamable): Throwing<void> {
    modelOrApplication.moveToSpam(this.order)
  }

  public tostring(): string {
    return `${MoveToSpamAction.type}(#${this.order})`
  }

  public events(): EventusEvent[] {
    const events = super.events()
    events.push(Eventus.messageListEvents.markMessageAsNotSpam(this.order, int64(-1)))
    return events
  }

  public canBePerformedImpl(model: App): Throwing<boolean> {
    const currentContainer = ContainerGetterFeature.get.forceCast(model).getCurrentContainer()
    return currentContainer.type === MessageContainerType.folder && currentContainer.name !== DefaultFolderName.spam
  }
}
