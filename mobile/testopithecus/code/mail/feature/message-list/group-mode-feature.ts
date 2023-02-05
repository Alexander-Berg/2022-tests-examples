import { Int32, Throwing } from '../../../../../../common/ys'
import { Feature } from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'

export class GroupModeFeature extends Feature<GroupMode> {
  public static get: GroupModeFeature = new GroupModeFeature()

  private constructor() {
    super(
      'GroupMode',
      'Действия с письмами в режиме групповых операций.' +
        'InitialSelectMessage переводит в компонент GroupMode и производится по лонг тапу.' +
        'SelectMessage выделяет письма, если мы уже в режиме групповых операций по обычному тапу.',
    )
  }
}

export interface GroupMode {
  getNumberOfSelectedMessages(): Throwing<Int32>

  isInGroupMode(): Throwing<boolean>

  selectMessage(byOrder: Int32): Throwing<void>

  selectAllMessages(): Throwing<void>

  initialMessageSelect(byOrder: Int32): Throwing<void>

  getSelectedMessages(): Throwing<Set<Int32>>

  markAsRead(): Throwing<void>

  markAsUnread(): Throwing<void>

  delete(): Throwing<void>

  openApplyLabelsScreen(): Throwing<void>

  markAsImportant(): Throwing<void>

  markAsUnimportant(): Throwing<void>

  markAsSpam(): Throwing<void>

  markAsNotSpam(): Throwing<void>

  openMoveToFolderScreen(): Throwing<void>

  archive(): Throwing<void>

  unselectMessage(byOrder: Int32): Throwing<void>

  unselectAllMessages(): Throwing<void>
}
