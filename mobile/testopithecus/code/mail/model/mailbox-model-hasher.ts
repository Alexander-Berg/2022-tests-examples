import { Int32, int64, Int64 } from '../../../../../common/ys'
import { HashBuilder } from '../../../../testopithecus-common/code/mbt/walk/hash/hash-builder'
import { MailboxModel } from './mail-model'

export class MailboxModelHasher {
  private static getSelectedMessagesHash(selectedMessages: Set<Int32>): Int64 {
    const hashBuilder: HashBuilder = new HashBuilder()
    hashBuilder.addInt(11)
    for (const v of selectedMessages.values()) {
      hashBuilder.addInt64(int64(v))
    }
    return hashBuilder.build()
  }
  public getMailboxModelHash(model: MailboxModel): Int64 {
    const hashBuilder: HashBuilder = new HashBuilder()
      .addBoolean(model.rotatable.landscape)
      .addInt64(model.messageNavigator.openedMessage)

    const selectedMessages = model.groupMode.selectedOrders
    if (selectedMessages !== null) {
      hashBuilder.addInt64(MailboxModelHasher.getSelectedMessagesHash(selectedMessages))
    } else {
      hashBuilder.addBoolean(true)
    }

    hashBuilder.addInt(29)
    for (const message of model.readOnlyExpandableThreads.expanded.values()) {
      hashBuilder.addInt64(message)
    }

    hashBuilder.addInt(23)
    if (!model.mailAppModelHandler.hasCurrentAccount()) {
      return hashBuilder.build()
    }

    const messagesDB = model.messageListDisplay.accountDataHandler.getCurrentAccount().messagesDB

    messagesDB.setMailDBHash(hashBuilder)

    hashBuilder.addString(model.containerGetter.getCurrentContainer().name)

    return hashBuilder.build()
  }
}
