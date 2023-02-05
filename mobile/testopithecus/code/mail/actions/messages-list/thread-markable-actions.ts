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
import {
  ExpandableThreads,
  ExpandableThreadsFeature,
  ExpandableThreadsModelFeature,
  ReadOnlyExpandableThreads,
} from '../../feature/message-list/expandable-threads-feature'

export abstract class AbstractExpandableThreadsAction implements MBTAction {
  public constructor(protected threadOrder: Int32, private type: MBTActionType) {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      ExpandableThreadsModelFeature.get.included(modelFeatures) &&
      ExpandableThreadsFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public canBePerformed(model: App): Throwing<boolean> {
    return this.canBePerformedImpl(ExpandableThreadsModelFeature.get.forceCast(model))
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    this.performImpl(ExpandableThreadsFeature.get.forceCast(model))
    this.performImpl(ExpandableThreadsFeature.get.forceCast(application))
    return history.currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }

  public getActionType(): MBTActionType {
    return this.type
  }

  public abstract canBePerformedImpl(model: ReadOnlyExpandableThreads): Throwing<boolean>

  public abstract performImpl(modelOrApplication: ExpandableThreads): Throwing<void>

  public abstract tostring(): string
}

export class ExpandThreadAction extends AbstractExpandableThreadsAction {
  public static readonly type: MBTActionType = 'ExpandThread'

  public constructor(threadOrder: number) {
    super(threadOrder, ExpandThreadAction.type)
  }

  public canBePerformedImpl(model: ReadOnlyExpandableThreads): Throwing<boolean> {
    const isExpanded = model.isExpanded(this.threadOrder)
    const threadLength = model.getMessagesInThread(this.threadOrder).length
    return !isExpanded && threadLength > 1
  }

  public performImpl(modelOrApplication: ExpandableThreads): Throwing<void> {
    modelOrApplication.expandThread(this.threadOrder)
  }

  public tostring(): string {
    return `ExpandThreadAction(${this.threadOrder})`
  }
}

export class CollapseThreadAction extends AbstractExpandableThreadsAction {
  public static readonly type: MBTActionType = 'CollapseThread'

  public constructor(threadOrder: number) {
    super(threadOrder, CollapseThreadAction.type)
  }

  public canBePerformedImpl(model: ReadOnlyExpandableThreads): Throwing<boolean> {
    return model.isExpanded(this.threadOrder)
  }

  public performImpl(modelOrApplication: ExpandableThreads): Throwing<void> {
    modelOrApplication.collapseThread(this.threadOrder)
  }

  public tostring(): string {
    return `CollapseThreadAction(${this.threadOrder})`
  }
}

export class MarkAsReadExpandedAction extends AbstractExpandableThreadsAction {
  public static readonly type: MBTActionType = 'MarkAsReadExpanded'

  public constructor(threadOrder: Int32, private messageOrder: Int32) {
    super(threadOrder, MarkAsReadExpandedAction.type)
  }

  public canBePerformedImpl(model: ReadOnlyExpandableThreads): Throwing<boolean> {
    const isRead = model.isRead(this.threadOrder, this.messageOrder)
    return model.isExpanded(this.threadOrder) && !isRead
  }

  public performImpl(modelOrApplication: ExpandableThreads): Throwing<void> {
    modelOrApplication.markThreadMessageAsRead(this.threadOrder, this.messageOrder)
  }

  public tostring(): string {
    return `MarkAsReadExpandedAction(${this.threadOrder}, ${this.messageOrder})`
  }
}

export class MarkAsUnreadExpandedAction extends AbstractExpandableThreadsAction {
  public static readonly type: MBTActionType = 'MarkAsUnreadExpanded'

  public constructor(threadOrder: Int32, private messageOrder: Int32) {
    super(threadOrder, MarkAsUnreadExpandedAction.type)
  }

  public canBePerformedImpl(model: ReadOnlyExpandableThreads): Throwing<boolean> {
    const isRead = model.isRead(this.threadOrder, this.messageOrder)
    return model.isExpanded(this.threadOrder) && isRead
  }

  public performImpl(modelOrApplication: ExpandableThreads): Throwing<void> {
    modelOrApplication.markThreadMessageAsUnRead(this.threadOrder, this.messageOrder)
  }

  public tostring(): string {
    return `MarkAsUnreadExpandedAction(${this.threadOrder}, ${this.messageOrder})`
  }
}
