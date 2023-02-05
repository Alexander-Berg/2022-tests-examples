import { MapJSONItem } from '../../../common/code/json/json-types'
import { EventNames } from '../../../eventus/code/events/event-names'
import { MessageViewContextMenuDeleteAction } from '../mail/actions/messages-list/context-menu-actions'
import { DeleteMessageByLongSwipeAction } from '../mail/actions/messages-list/long-swipe-actions'
import { RefreshMessageListAction } from '../mail/actions/messages-list/message-list-actions'
import { MessageViewBackToMailListAction, OpenMessageAction } from '../mail/actions/opened-message/message-actions'
import { MBTAction } from '../../../testopithecus-common/code/mbt/mbt-abstractions'

export class ActionParser {
  public parseActionFromJson(json: MapJSONItem): MBTAction {
    switch (json.getString('event_name')) {
      // message list actions
      case EventNames.LIST_MESSAGE_OPEN:
        return new OpenMessageAction(json.getInt32('order')!)
      case EventNames.LIST_MESSAGE_DELETE:
        return new DeleteMessageByLongSwipeAction(json.getInt32('order')!)
      case EventNames.LIST_MESSAGE_REFRESH:
        return new RefreshMessageListAction()

      // message actions
      case EventNames.MESSAGE_VIEW_BACK:
        return new MessageViewBackToMailListAction()
      case EventNames.MESSAGE_VIEW_DELETE:
        return new MessageViewContextMenuDeleteAction()

      default:
        throw new Error(`Unknown action ${json.getString('event_name')}`)
    }
  }

  public parseActionFromString(extrasString: string): MBTAction {
    return this.parseActionFromJson(this.parse(extrasString))
  }

  // noinspection JSMethodCanBeStatic, JSUnusedLocalSymbols
  public parse(_extrasString: string): MapJSONItem {
    // TODO
    return new MapJSONItem()
  }
}
