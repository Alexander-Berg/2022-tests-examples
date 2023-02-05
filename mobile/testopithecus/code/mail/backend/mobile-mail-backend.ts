import { MailboxClientHandler } from '../../client/mailbox-client'
import { App, FeatureID, FeatureRegistry } from '../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { reduced } from '../../utils/mail-utils'
import {
  ArchiveMessageFeature,
  DeleteMessageFeature,
  MarkableImportantFeature,
  MarkableReadFeature,
  MovableToFolderFeature,
  SpamableFeature,
} from '../feature/base-action-features'
import { FolderNavigatorFeature } from '../feature/folder-list-features'
import {
  CustomMailServiceLoginFeature,
  GoogleLoginFeature,
  HotmailLoginFeature,
  MailRuLoginFeature,
  MultiAccountFeature,
  OutlookLoginFeature,
  RamblerLoginFeature,
  YahooLoginFeature,
  YandexLoginFeature,
  YandexTeamLoginFeature,
} from '../feature/login-features'
import { CreatableFolderFeature } from '../feature/manageable-container-features'
import { ExpandableThreadsFeature } from '../feature/message-list/expandable-threads-feature'
import { MessageListDisplayFeature } from '../feature/message-list/message-list-display-feature'
import { Message } from '../model/mail-model'
import { ArchiveMessageBackend } from './archive-message-backend'
import { CreatableFolderBackend } from './creatable-folder-backend'
import { DeleteMessageBackend } from './delete-message-backend'
import { ExpandableThreadsBackend } from './expandable-threads-backend'
import { FolderNavigatorBackend } from './folder-navigator-backend'
import { MarkableImportantBackend } from './labeled-backend'
import { LoginBackend } from './login-backend'
import { MarkableBackend } from './markable-backend'
import { MessageListDisplayBackend } from './message-list-display-backend'
import { MovableToFolderBackend } from './movable-to-folder-backend'
import { MultiAccountBackend } from './multi-account-backend'
import { SpamableBackend } from './spamable-backend'

export class MobileMailBackend implements App {
  public static allSupportedFeatures: FeatureID[] = [
    ArchiveMessageFeature.get.name,
    MessageListDisplayFeature.get.name,
    MarkableReadFeature.get.name,
    MarkableImportantFeature.get.name,
    ExpandableThreadsFeature.get.name,
    DeleteMessageFeature.get.name,
    SpamableFeature.get.name,
    MovableToFolderFeature.get.name,
    CreatableFolderFeature.get.name,
    FolderNavigatorFeature.get.name,
    YandexLoginFeature.get.name,
    YandexTeamLoginFeature.get.name,
    MultiAccountFeature.get.name,
    MailRuLoginFeature.get.name,
    GoogleLoginFeature.get.name,
    OutlookLoginFeature.get.name,
    HotmailLoginFeature.get.name,
    RamblerLoginFeature.get.name,
    YahooLoginFeature.get.name,
  ]

  public supportedFeatures: FeatureID[] = MobileMailBackend.allSupportedFeatures

  public archive: ArchiveMessageBackend
  public messageListDisplay: MessageListDisplayBackend
  public folderNavigator: FolderNavigatorBackend
  public markable: MarkableBackend
  public markableImportant: MarkableImportantBackend
  public deleteMessage: DeleteMessageBackend
  public spamable: SpamableBackend
  public movableToFolder: MovableToFolderBackend
  public creatableFolder: CreatableFolderBackend
  public expandableThreads: ExpandableThreadsBackend
  public loginBackend: LoginBackend
  public multiAccount: MultiAccountBackend

  public constructor(public readonly clientsHandler: MailboxClientHandler) {
    this.messageListDisplay = new MessageListDisplayBackend(clientsHandler)
    this.folderNavigator = new FolderNavigatorBackend(this.messageListDisplay, clientsHandler)
    this.markable = new MarkableBackend(this.messageListDisplay, clientsHandler)
    this.markableImportant = new MarkableImportantBackend(this.messageListDisplay, clientsHandler)
    this.deleteMessage = new DeleteMessageBackend(this.messageListDisplay, clientsHandler)
    this.spamable = new SpamableBackend(this.messageListDisplay, clientsHandler)
    this.movableToFolder = new MovableToFolderBackend(this.messageListDisplay, clientsHandler)
    this.creatableFolder = new CreatableFolderBackend(clientsHandler)
    this.expandableThreads = new ExpandableThreadsBackend(this.messageListDisplay, clientsHandler)
    this.loginBackend = new LoginBackend(clientsHandler)
    this.archive = new ArchiveMessageBackend(this.messageListDisplay, clientsHandler)
    this.multiAccount = new MultiAccountBackend(clientsHandler)
  }

  public getFeature(feature: FeatureID): any {
    return new FeatureRegistry()
      .register(MessageListDisplayFeature.get, this.messageListDisplay)
      .register(MarkableReadFeature.get, this.markable)
      .register(MarkableImportantFeature.get, this.markableImportant)
      .register(ExpandableThreadsFeature.get, this.expandableThreads)
      .register(DeleteMessageFeature.get, this.deleteMessage)
      .register(SpamableFeature.get, this.spamable)
      .register(FolderNavigatorFeature.get, this.folderNavigator)
      .register(MovableToFolderFeature.get, this.movableToFolder)
      .register(CreatableFolderFeature.get, this.creatableFolder)
      .register(YandexLoginFeature.get, this.loginBackend)
      .register(YandexTeamLoginFeature.get, this.loginBackend)
      .register(ArchiveMessageFeature.get, this.archive)
      .register(MultiAccountFeature.get, this.multiAccount)
      .register(MailRuLoginFeature.get, this.loginBackend)
      .register(GoogleLoginFeature.get, this.loginBackend)
      .register(OutlookLoginFeature.get, this.loginBackend)
      .register(HotmailLoginFeature.get, this.loginBackend)
      .register(RamblerLoginFeature.get, this.loginBackend)
      .register(YahooLoginFeature.get, this.loginBackend)
      .register(CustomMailServiceLoginFeature.get, this.loginBackend)
      .get(feature)
  }

  public async dump(model: App): Promise<string> {
    let s = `${this.messageListDisplay.getCurrentFolder().name}\n`
    const threads = this.messageListDisplay.getMessageDTOList(3)
    for (const thread of threads) {
      const threadSelector = thread.threadCount !== null ? `${thread.threadCount!}v` : ''
      s += `${reduced(thread.mid)} ${thread.sender}\t${thread.unread ? '*' : 'o'}\t${
        thread.subjectText
      }\t${threadSelector}\t${thread.timestamp}\n`
      const threadSize = Message.fromMeta(thread).threadCounter
      if (threadSize !== null) {
        for (const message of this.clientsHandler.getCurrentClient().getMessagesInThread(thread.tid!, threadSize)) {
          s += `\t\t${reduced(message.mid)} ${message.sender}\t${message.unread ? '*' : 'o'}\t${message.subjectText}\t${
            message.fid
          }\t${thread.timestamp}\n`
        }
      }
    }
    return s
  }
}
