import { Throwing } from '../../../../../../common/ys'
import { FolderName, LabelName } from '../../feature/folder-list-features'
import { AdvancedSearch } from '../../feature/search-features'
import { MessageListDisplayModel } from './message-list-display-model'

export class AdvancedSearchModel implements AdvancedSearch {
  public constructor(private messageList: MessageListDisplayModel) {}

  public addFolderToSearch(folderName: FolderName): Throwing<void> {
    this.messageList.messageListFilter.withFolder(folderName)
  }

  public addLabelToSearch(labelName: LabelName): Throwing<void> {
    this.messageList.messageListFilter.withLabel(labelName)
  }

  public searchOnlyImportant(): Throwing<void> {
    this.messageList.messageListFilter.withIsImportantOnly()
  }
}
