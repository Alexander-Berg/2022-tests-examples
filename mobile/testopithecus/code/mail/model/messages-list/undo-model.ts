import { currentTimeMs } from '../../../../../testopithecus-common/code/utils/utils'
import { Int64, Throwing } from '../../../../../../common/ys'
import { Undo, UndoState } from '../../feature/message-list/undo-feature'
import { ArchiveMessageModel } from '../base-models/archive-message-model'
import { DeleteMessageModel } from '../base-models/delete-message-model'
import { SpamableModel } from '../base-models/spamable-model'
import { DefaultFolderName } from '../folder-data-model'
import { MailAppModelHandler } from '../mail-model'
import { MessageListDatabaseFilter } from '../supplementary/message-list-database'
import { MessageListDisplayModel } from './message-list-display-model'

export class UndoModel implements Undo {
  public constructor(
    private deleteModel: DeleteMessageModel,
    private archiveModel: ArchiveMessageModel,
    private spamModel: SpamableModel,
    private accHandler: MailAppModelHandler,
    private messageListDisplayModel: MessageListDisplayModel,
  ) {}

  public resetUndoShowing(): void {
    this.deleteModel.resetLastDeleteMessageTime()
    this.archiveModel.resetLastArchiveMessageTime()
    this.spamModel.resetLastSpamMessageTime()
  }

  public isUndoArchiveToastShown(): Throwing<UndoState> {
    const lastArchiveMessageTime = this.archiveModel.getLastArchiveMessageTime()
    if (lastArchiveMessageTime === null) {
      return UndoState.notShown
    }
    const duration = currentTimeMs() - lastArchiveMessageTime!
    return this.getUndoToastShowingState(duration)
  }

  public isUndoDeleteToastShown(): Throwing<UndoState> {
    const lastDeleteMessageTime = this.deleteModel.getLastDeleteMessageTime()
    if (
      lastDeleteMessageTime === null ||
      this.messageListDisplayModel.getCurrentContainer().name === DefaultFolderName.trash
    ) {
      return UndoState.notShown
    }
    const duration = currentTimeMs() - lastDeleteMessageTime!
    return this.getUndoToastShowingState(duration)
  }

  public isUndoSpamToastShown(): Throwing<UndoState> {
    const lastSpamMessageTime = this.spamModel.getLastSpamMessageTime()
    if (lastSpamMessageTime === null) {
      return UndoState.notShown
    }
    const duration = currentTimeMs() - lastSpamMessageTime!
    return this.getUndoToastShowingState(duration)
  }

  public isUndoSendingToastShown(): Throwing<UndoState> {
    return UndoState.notShown // TODO: need to implement
  }

  public undoArchive(): Throwing<void> {
    const archivedMessageIdToFolder = this.archiveModel.getArchivedMessageIdToFolder()
    archivedMessageIdToFolder.forEach((folderName, mid) => {
      this.accHandler.getCurrentAccount().messagesDB.moveMessageToFolder(mid, folderName)
    })
    this.archiveModel.resetLastArchiveMessageTime()
  }

  public undoDelete(): Throwing<void> {
    const deletedMessageIdToFolder = this.deleteModel.getDeletedMessageIdToFolder()
    deletedMessageIdToFolder.forEach((folderName, mid) => {
      if (folderName !== DefaultFolderName.trash) {
        this.accHandler.getCurrentAccount().messagesDB.moveMessageToFolder(mid, folderName)
      }
    })
    this.deleteModel.resetLastDeleteMessageTime()
  }

  public undoSpam(): Throwing<void> {
    const spammedMessageIdToFolder = this.spamModel.getSpammedMessageIdToFolder()
    const midToReadStatus = this.spamModel.getMidToReadStatus()
    const notSpamMessages = this.accHandler
      .getCurrentAccount()
      .messagesDB.getMessageIdList(new MessageListDatabaseFilter().withExcludedFolders([DefaultFolderName.spam]))

    spammedMessageIdToFolder.forEach((folderName, mid) => {
      this.accHandler.getCurrentAccount().messagesDB.storedMessage(mid).mutableHead.read = midToReadStatus.get(mid)!
      this.accHandler.getCurrentAccount().messagesDB.moveMessageToFolder(mid, folderName)
      this.spamModel.addThreadCounter(notSpamMessages, mid)
      notSpamMessages.push(mid)
    })
    this.spamModel.resetLastSpamMessageTime()
  }

  public undoSending(): Throwing<void> {
    // TODO: need to implement
  }

  private getUndoToastShowingState(duration: Int64): UndoState {
    const SHOWING_DURATION = 5_000
    const TIMING_MARGIN = 10_000

    if (duration <= SHOWING_DURATION) {
      return UndoState.shown
    } else if (duration > SHOWING_DURATION && duration <= TIMING_MARGIN) {
      return UndoState.undefined
    } else {
      return UndoState.notShown
    }
  }
}
