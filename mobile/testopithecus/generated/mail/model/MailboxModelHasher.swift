// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/model/mailbox-model-hasher.ts >>>

import Foundation

open class MailboxModelHasher {
  @discardableResult
  private class func getSelectedMessagesHash(_ selectedMessages: YSSet<Int32>) -> Int64 {
    let hashBuilder: HashBuilder = HashBuilder()
    hashBuilder.addInt(11)
    for v in selectedMessages.values() {
      hashBuilder.addInt64(int64(v))
    }
    return hashBuilder.build()
  }

  @discardableResult
  open func getMailboxModelHash(_ model: MailboxModel) -> Int64 {
    let hashBuilder: HashBuilder = HashBuilder().addBoolean(model.rotatable.landscape).addInt64(model.messageNavigator.openedMessage)
    let selectedMessages = model.groupMode.selectedOrders
    if selectedMessages != nil {
      hashBuilder.addInt64(MailboxModelHasher.getSelectedMessagesHash(selectedMessages))
    } else {
      hashBuilder.addBoolean(true)
    }
    hashBuilder.addInt(29)
    for message in model.readOnlyExpandableThreads.expanded.values() {
      hashBuilder.addInt64(message)
    }
    hashBuilder.addInt(23)
    if !model.mailAppModelHandler.hasCurrentAccount() {
      return hashBuilder.build()
    }
    let messagesDB = model.messageListDisplay.accountDataHandler.getCurrentAccount().messagesDB
    messagesDB.setMailDBHash(hashBuilder)
    hashBuilder.addString(model.containerGetter.getCurrentContainer().name)
    return hashBuilder.build()
  }

}

