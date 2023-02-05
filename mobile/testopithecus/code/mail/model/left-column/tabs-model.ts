import { Int32, Throwing } from '../../../../../../common/ys'
import { formatFolderName } from '../../../utils/mail-utils'
import { FolderName } from '../../feature/folder-list-features'
import { MessageView } from '../../feature/mail-view-features'
import { MessageContainer, MessageContainerType } from '../../feature/message-list/container-getter-feature'
import { Tabs } from '../../feature/tabs-feature'
import { DefaultFolderName } from '../folder-data-model'
import { MailAppModelHandler } from '../mail-model'
import { MessageListDisplayModel } from '../messages-list/message-list-display-model'
import { UndoModel } from '../messages-list/undo-model'
import { MessageListDatabaseFilter } from '../supplementary/message-list-database'

export class TabsModel implements Tabs {
  public constructor(
    public model: MessageListDisplayModel,
    private accHandler: MailAppModelHandler,
    private undoModel: UndoModel,
  ) {}

  public isEnableTabs(): Throwing<boolean> {
    return this.accHandler.getCurrentAccount().accountSettings.sortingEmailsByCategoryEnabled
  }

  public SwitchOffTabs(): Throwing<void> {
    this.accHandler.getCurrentAccount().accountSettings.sortingEmailsByCategoryEnabled = false
  }

  public SwitchOnTabs(): Throwing<void> {
    this.accHandler.getCurrentAccount().accountSettings.sortingEmailsByCategoryEnabled = true
  }

  public isDisplayNotificationTabs(tabsName: FolderName): Throwing<boolean> {
    return (
      this.model.getCurrentContainer().name === DefaultFolderName.inbox &&
      this.accHandler.getCurrentAccount().messagesDB.getTabsToMessage(tabsName).size !== 0
    )
  }

  public isUnreadNotificationTabs(tabsName: FolderName): Throwing<boolean> {
    const messageListInTab = this.accHandler
      .getCurrentAccount()
      .messagesDB.getMessageList(new MessageListDatabaseFilter().withFolder(tabsName))

    for (const msg of messageListInTab) {
      if (!msg.read) {
        return true
      }
    }
    return false
  }

  private isFirstTabNotificationInMessageList(
    firstMessage: MessageView,
    firstMessageInOtherTabs: MessageView,
  ): Throwing<boolean> {
    return firstMessage.timestamp > firstMessageInOtherTabs.timestamp
  }

  public getPositionTabsNotification(tabsName: FolderName): Throwing<Int32> {
    const messageList = this.model.getMessageList(10)
    const msgListInMailingLists = this.accHandler
      .getCurrentAccount()
      .messagesDB.getMessageList(new MessageListDatabaseFilter().withFolder(DefaultFolderName.mailingLists))

    const msgListInSocialNetworks = this.accHandler
      .getCurrentAccount()
      .messagesDB.getMessageList(new MessageListDatabaseFilter().withFolder(DefaultFolderName.socialNetworks))

    let position: Int32 = 0
    let firstMessage: MessageView
    let firstMessageinOtherTabs: MessageView | null

    if (tabsName === DefaultFolderName.mailingLists) {
      firstMessage = msgListInMailingLists[0]
      firstMessageinOtherTabs = msgListInSocialNetworks.length !== 0 ? msgListInSocialNetworks[0] : null
    } else {
      firstMessage = msgListInSocialNetworks[0]
      firstMessageinOtherTabs = msgListInMailingLists.length !== 0 ? msgListInMailingLists[0] : null
    }

    for (const msg of messageList) {
      if (msg.timestamp < firstMessage.timestamp) {
        break
      }
      position = position + 1
    }

    if (firstMessageinOtherTabs !== null) {
      position = this.isFirstTabNotificationInMessageList(firstMessage, firstMessageinOtherTabs)
        ? position
        : position + 1
    }

    return position
  }

  public goToTabByNotification(tabsName: FolderName): Throwing<void> {
    this.model.setCurrentContainer(new MessageContainer(formatFolderName(tabsName), MessageContainerType.folder))
    this.undoModel.resetUndoShowing()
  }
}
