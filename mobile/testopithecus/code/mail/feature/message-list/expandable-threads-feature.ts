import { Int32, Throwing } from '../../../../../../common/ys'
import { Feature } from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { MessageView } from '../mail-view-features'

export class ExpandableThreadsModelFeature extends Feature<ReadOnlyExpandableThreads> {
  public static get: ExpandableThreadsModelFeature = new ExpandableThreadsModelFeature()

  private constructor() {
    super('ReadOnlyExpandableThreads', 'TODO: добрый человек, напиши тут, про что эта фича')
  }
}

export interface ReadOnlyExpandableThreads {
  isExpanded(threadOrder: Int32): Throwing<boolean>

  isRead(threadOrder: Int32, messageOrder: Int32): Throwing<boolean>

  getMessagesInThread(threadOrder: Int32): Throwing<MessageView[]>
}

export class ExpandableThreadsFeature extends Feature<ExpandableThreads> {
  public static get: ExpandableThreadsFeature = new ExpandableThreadsFeature()

  private constructor() {
    super('ExpandableThreads', 'TODO: добрый человек, напиши тут, про что эта фича')
  }
}

export interface ExpandableThreads {
  markThreadMessageAsRead(threadOrder: Int32, messageOrder: Int32): Throwing<void>

  markThreadMessageAsUnRead(threadOrder: Int32, messageOrder: Int32): Throwing<void>

  expandThread(order: Int32): Throwing<void>

  collapseThread(order: Int32): Throwing<void>
}
