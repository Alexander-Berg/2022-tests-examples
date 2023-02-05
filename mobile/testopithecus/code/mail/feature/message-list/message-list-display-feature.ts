import { Int32, Throwing } from '../../../../../../common/ys'
import { Feature } from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { MessageView } from '../mail-view-features'

export class MessageListDisplayFeature extends Feature<MessageListDisplay> {
  public static get: MessageListDisplayFeature = new MessageListDisplayFeature()

  private constructor() {
    super('MessageListDisplay', 'TODO: добрый человек, напиши тут, про что эта фича')
  }
}

// TODO: разделить на действия и условия (ModelFeature и Feature)
export interface MessageListDisplay {
  /**
   * Этот метод должен возвращать список писем в папке, которые сейчас видит пользователь.
   * То есть, например, если мы находимся в тредовальном режиме, то мы вернем список первых писем из тредов
   * Если мы находимся не в тредовальном режиме, то мы вернем просто список писем в папке
   *
   * @param limit - сколько писем надо вернуть
   */
  getMessageList(limit: Int32): Throwing<MessageView[]>

  refreshMessageList(): Throwing<void>

  unreadCounter(): Throwing<Int32>

  swipeDownMessageList(): Throwing<void>
}
