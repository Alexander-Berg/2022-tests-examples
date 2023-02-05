import { Throwing } from '../../../../../../common/ys'
import { MessageContainerType } from '../../feature/message-list/container-getter-feature'
import { ZeroSuggest } from '../../feature/search-features'
import { MailAppModelHandler } from '../mail-model'
import { MessageListDisplayModel } from '../messages-list/message-list-display-model'
import { SearchModel } from './search-model'

export class ZeroSuggestModel implements ZeroSuggest {
  public constructor(
    private mailAppModelHandler: MailAppModelHandler,
    private searchModel: SearchModel,
    private messageList: MessageListDisplayModel,
  ) {}

  public getZeroSuggest(): Throwing<string[]> {
    return this.mailAppModelHandler.getCurrentAccount().zeroSuggest
  }

  public isShown(): Throwing<boolean> {
    const isInSearch = this.searchModel.isInSearch()
    return this.messageList.getCurrentContainer().type !== MessageContainerType.search && isInSearch
  }

  public searchByZeroSuggest(suggest: string): Throwing<void> {
    this.searchModel.searchByQuery(suggest)
  }
}
