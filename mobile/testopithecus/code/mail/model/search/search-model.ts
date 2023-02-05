import { Throwing } from '../../../../../../common/ys'
import { MessageContainer, MessageContainerType } from '../../feature/message-list/container-getter-feature'
import { Search } from '../../feature/search-features'
import { MessageListDisplayModel } from '../messages-list/message-list-display-model'
import { OpenMessageModel } from '../opened-message/open-message-model'

export class SearchModel implements Search {
  private isSearchOpened: boolean
  private lastContainer: MessageContainer

  public constructor(
    private messageList: MessageListDisplayModel,
    private readonly openMessageModel: OpenMessageModel,
  ) {
    this.isSearchOpened = false
    this.lastContainer = this.messageList.getCurrentContainer()
  }

  public searchAllMessages(): Throwing<void> {
    this.searchByQuery('yandex')
  }

  public searchByQuery(query: string): Throwing<void> {
    this.messageList.setCurrentContainer(new MessageContainer(query, MessageContainerType.search))
  }

  public closeSearch(): Throwing<void> {
    this.isSearchOpened = false
    this.messageList.setCurrentContainer(this.lastContainer)
    this.openMessageModel.closeMessage()
  }

  public clearTextField(): Throwing<void> {
    this.messageList.setCurrentContainer(this.lastContainer)
  }

  public isInSearch(): Throwing<boolean> {
    return this.isSearchOpened
  }

  public isSearchedForMessages(): Throwing<boolean> {
    return this.messageList.getCurrentContainer().type === MessageContainerType.search
  }

  public openSearch(): Throwing<void> {
    this.lastContainer = this.messageList.getCurrentContainer()
    this.isSearchOpened = true
  }
}
