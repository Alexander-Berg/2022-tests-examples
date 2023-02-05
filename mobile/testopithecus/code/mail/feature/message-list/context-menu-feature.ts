import { Int32, Throwing } from '../../../../../../common/ys'
import { Feature } from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'

export type MessageActionName = string

export class ContextMenuFeature extends Feature<ContextMenu> {
  public static get: ContextMenuFeature = new ContextMenuFeature()

  private constructor() {
    super('ContextMenu', 'Меню действий с письмом, открываемое через short swipe или из просмотра письма')
  }
}

export interface ContextMenu {
  openFromShortSwipe(order: Int32): Throwing<void>

  openFromMessageView(): Throwing<void>

  close(): Throwing<void>

  getAvailableActions(): Throwing<MessageActionName[]>

  openReplyCompose(): Throwing<void>

  openReplyAllCompose(): Throwing<void>

  openForwardCompose(): Throwing<void>

  deleteMessage(): Throwing<void>

  markAsSpam(): Throwing<void>

  markAsNotSpam(): Throwing<void>

  openApplyLabelsScreen(): Throwing<void>

  markAsRead(): Throwing<void>

  markAsUnread(): Throwing<void>

  markAsImportant(): Throwing<void>

  markAsUnimportant(): Throwing<void>

  openMoveToFolderScreen(): Throwing<void>

  archive(): Throwing<void>

  showTranslator(): Throwing<void>
}
