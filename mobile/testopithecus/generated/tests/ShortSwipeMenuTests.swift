// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM tests/short-swipe-menu-tests.ts >>>

import Foundation

open class ShortSwipeMenuMarkAsReadMessageTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Пометка письма прочитанным")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(6217).androidCase(6472)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.nextMessage("message1").nextMessage("message2").nextMessage("message3")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(ShortSwipeContextMenuMarkAsReadAction(1))
  }

}

open class ShortSwipeMenuMarkAsReadThreadTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Пометка треда прочитанным")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(6618).androidCase(6631)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.nextThread("thread1", 1).nextThread("thread2", 2).nextThread("thread3", 3)
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(ShortSwipeContextMenuMarkAsReadAction(1))
  }

}

open class ShortSwipeMenuMarkAsUnreadMessageTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Пометка письма непрочитанным")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(6218).androidCase(6473)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.createFolder("UserFolder").switchFolder("UserFolder").nextMessage("message1").nextMessage("message2").nextMessage("message3")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(GoToFolderAction("UserFolder")).then(MarkAsRead(0)).then(ShortSwipeContextMenuMarkAsUnreadAction(0))
  }

}

open class ShortSwipeMenuMarkAsUnreadThreadTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Пометка треда непрочитанным")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(6619).androidCase(6632)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.nextThread("thread1", 3).nextThread("thread2", 2).nextThread("thread3", 3)
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(MarkAsRead(0)).then(ShortSwipeContextMenuMarkAsUnreadAction(0))
  }

}

open class ShortSwipeMenuDeleteMessageTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Удаление письма")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(6221).androidCase(6476)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.createFolder("UserSubfolder", YSArray("UserFolder")).switchFolder("UserSubfolder", YSArray("UserFolder")).nextMessage("message1").nextMessage("message2").nextMessage("message3")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(GoToFolderAction("UserSubfolder", YSArray("UserFolder"))).then(ShortSwipeContextMenuDeleteAction(1)).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.trash))
  }

}

open class ShortSwipeMenuDeleteThreadTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Удаление треда")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(6224).androidCase(6478)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.nextThread("thread1", 2).nextThread("thread2", 3).nextThread("thread3", 4)
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(ShortSwipeContextMenuDeleteAction(1)).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.trash))
  }

}

open class ShortSwipeMenuMarkAsSpamThreadTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Пометка треда спамом")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(6236).androidCase(6490)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.createFolder("UserSubfolder", YSArray("UserFolder")).switchFolder("UserSubfolder", YSArray("UserFolder")).nextThread("thread1", 2).nextThread("thread2", 3).nextThread("thread3", 4)
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(GoToFolderAction("UserSubfolder", YSArray("UserFolder"))).then(ShortSwipeContextMenuMarkAsSpamAction(1)).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.spam))
  }

}

open class ShortSwipeMenuMarkAsSpamMessageTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Пометка письма спамом")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(6235).androidCase(6489)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.nextMessage("message1").nextMessage("message2").nextMessage("message3")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(GoToFilterUnreadAction()).then(ShortSwipeContextMenuMarkAsSpamAction(1)).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.spam)).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.inbox))
  }

}

open class ShortSwipeMenuMarkAsNotSpamMessageTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Пометка письма не спамом")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(6621).androidCase(6633)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.switchFolder(DefaultFolderName.spam).nextMessage("message1").nextMessage("message2").nextMessage("message3")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.spam)).then(ShortSwipeContextMenuMarkAsNotSpamAction(1)).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.inbox))
  }

}

open class ShortSwipeMenuImportantMessageTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Пометка письма важным")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(6239).androidCase(6493)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.switchFolder(DefaultFolderName.archive).nextMessage("message1").nextMessage("message2").nextMessage("message3")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.archive)).then(ShortSwipeContextMenuMarkAsImportantAction(2)).then(OpenFolderListAction()).then(GoToFilterImportantAction())
  }

}

open class ShortSwipeMenuImportantThreadTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Пометка треда важным")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(6240).androidCase(6494)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.nextThread("thread1", 2).nextThread("thread2", 3).nextThread("thread3", 4)
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(ShortSwipeContextMenuMarkAsImportantAction(1)).then(OpenFolderListAction()).then(GoToFilterImportantAction())
  }

}

open class ShortSwipeMenuUnImportantMessageTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Снятие метки Важное с письма")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(6241).androidCase(6495)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.nextThread("message1", 2).nextThread("message2", 2).nextThread("message3", 2)
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(ShortSwipeContextMenuMarkAsImportantAction(1)).then(AssertAction()).then(OpenFolderListAction()).then(GoToFilterImportantAction()).then(ShortSwipeContextMenuMarkAsUnimportantAction(1)).then(AssertAction()).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.inbox))
  }

}

open class ShortSwipeMenuUnImportantThreadTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Снятие метки Важное с треда")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(6242).androidCase(6496)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.nextThread("thread1", 2).nextThread("thread2", 3).nextThread("thread3", 4)
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(ShortSwipeContextMenuMarkAsImportantAction(1)).then(OpenFolderListAction()).then(GoToFilterImportantAction()).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.inbox)).then(ShortSwipeContextMenuMarkAsUnimportantAction(1)).then(OpenFolderListAction()).then(GoToFilterImportantAction())
  }

}

open class ShortSwipeMenuMoveToFolderMessageTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Перемещение письма в другую папку")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(6244).androidCase(6498)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.nextMessage("message1").nextMessage("message2").nextMessage("message3").createFolder("UserFolder")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(ShortSwipeContextMenuMoveToFolderAction(1, "UserFolder")).then(OpenFolderListAction()).then(GoToFolderAction("UserFolder"))
  }

}

open class ShortSwipeMenuMoveToFolderThreadTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Перемещение треда в другую папку")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(6615).androidCase(6635)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.nextThread("thread1", 2).nextThread("thread2", 3).nextThread("thread3", 4).createFolder("UserFolder")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(ShortSwipeContextMenuMoveToFolderAction(1, "UserFolder")).then(OpenFolderListAction()).then(GoToFolderAction("UserFolder"))
  }

}

open class ShortSwipeMenuMoveToInboxFromTrashTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Перемещение письма из папки Удаленные в папку Входящие")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(26).androidCase(6193)
  }

  open override func prepareAccount(_ builder: MailboxBuilder) -> Void {
    builder.switchFolder(DefaultFolderName.trash).nextMessage("AutoTestSubj")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.trash)).then(ShortSwipeContextMenuMoveToFolderAction(0, DefaultFolderName.inbox)).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.inbox))
  }

}

open class ShortSwipeMenuDeleteFromTrashTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Удаление письма из папки Удаленные")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(6620).androidCase(7618).ignoreOn(MBTPlatform.Android)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.switchFolder("Trash").nextMessage("subject1").nextMessage("subject2")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(GoToFolderAction("Trash")).then(ShortSwipeContextMenuDeleteAction(1))
  }

}

open class ShortSwipeMenuApplyLabelToThreadTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Добавление пользовательской метки на тред")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(6616).androidCase(6636)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.nextThread("thread1", 2).nextThread("thread2", 3).nextThread("thread3", 4).createLabel(LabelData("label1"))
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(ShortSwipeContextMenuApplyLabelsAction(1, YSArray("label1"))).then(OpenFolderListAction()).then(GoToLabelAction("label1"))
  }

}

open class ShortSwipeMenuApplyLabelToMessageTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Добавление пользовательской метки на письмо")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(6246).androidCase(6500)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.createFolder("UserFolder").switchFolder("UserFolder").nextMessage("message1").nextMessage("message2").nextMessage("message3").createLabel(LabelData("label1"))
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(GoToFolderAction("UserFolder")).then(ShortSwipeContextMenuApplyLabelsAction(1, YSArray("label1"))).then(OpenFolderListAction()).then(GoToLabelAction("label1"))
  }

}

open class ShortSwipeMenuRemoveLabelFromMessageTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Снятие пользовательской метки с письма")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(6247).androidCase(6501)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.switchFolder(DefaultFolderName.sent).nextCustomMessage(MessageSpecBuilder().withDefaults().addLabels(YSArray(LabelData("label1"))).withSubject("subj1")).nextCustomMessage(MessageSpecBuilder().withDefaults().addLabels(YSArray(LabelData("label1"))).withSubject("subj2"))
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.sent)).then(ShortSwipeContextMenuRemoveLabelsAction(1, YSArray("label1"))).then(OpenFolderListAction()).then(GoToLabelAction("label1"))
  }

}

open class ShortSwipeMenuRemoveLabelFromThreadTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Снятие пользовательской метки с треда")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(6617).androidCase(6637)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.nextThread("thread1", 2).nextThread("thread2", 3).nextThread("thread3", 4).createLabel(LabelData("label1"))
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(ShortSwipeContextMenuApplyLabelsAction(1, YSArray("label1"))).then(OpenFolderListAction()).then(GoToLabelAction("label1")).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.inbox)).then(ShortSwipeContextMenuRemoveLabelsAction(1, YSArray("label1"))).then(OpenFolderListAction()).then(GoToLabelAction("label1"))
  }

}

open class ShortSwipeMenuReplyOnMessageTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Ответ на письмо")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(6196).androidCase(6072)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.nextMessage("message1").nextMessage("message2").nextMessage("message3")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(ShortSwipeContextMenuOpenReplyComposeAction(1))
  }

}

open class ShortSwipeMenuArchiveMessageFromSpamTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Архивация одиночного письма из папки Спам")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(6231).androidCase(6485)
  }

  open override func prepareAccount(_ builder: MailboxBuilder) -> Void {
    builder.switchFolder(DefaultFolderName.spam).nextMessage("AutoTestSubj1").nextMessage("AutoTestSubj2")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.spam)).then(ShortSwipeContextMenuArchiveAction(0)).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.archive))
  }

}

open class ShortSwipeMenuArchiveThreadFromUserFolderTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Архивация треда письма из Пользовательской папки")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(6232).androidCase(6486)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.createFolder("UserFolder").switchFolder("UserFolder").nextThread("AutoTestSubj1", 2)
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(GoToFolderAction("UserFolder")).then(ShortSwipeContextMenuArchiveAction(0)).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.archive))
  }

}

open class ShortSwipeMenuMarkReadUnreadInCompactModeTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Пометка письма прочитанным-непрочитанным в compact режиме")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(632).androidCase(10535)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.createFolder("UserFolder").switchFolder("UserFolder").nextMessage("message1").nextMessage("message2").nextMessage("message3")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(OpenSettingsAction()).then(OpenGeneralSettingsAction()).then(TurnOnCompactMode()).then(CloseGeneralSettingsAction()).then(CloseRootSettings()).then(GoToFolderAction("UserFolder")).then(MarkAsRead(0)).then(ShortSwipeContextMenuMarkAsReadAction(1)).then(ShortSwipeContextMenuMarkAsUnreadAction(0)).then(OpenFolderListAction()).then(GoToFilterUnreadAction())
  }

}

open class ShortSwipeMenuAddLabelInCompactModeTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Пометка письма важным-неважным в компактном режиме")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(635).androidCase(10537)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.nextCustomMessage(MessageSpecBuilder().withDefaults().addLabels(YSArray(LabelData("label1"), LabelData("label2"))).withSubject("inbox_subj1")).switchFolder(DefaultFolderName.sent).nextMessage("subj1").nextMessage("subj2").nextMessage("subj3").createLabel(LabelData("label1"))
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(OpenSettingsAction()).then(OpenGeneralSettingsAction()).then(TurnOnCompactMode()).then(CloseGeneralSettingsAction()).then(CloseRootSettings()).then(GoToFolderAction(DefaultFolderName.sent)).then(ShortSwipeContextMenuApplyLabelsAction(1, YSArray("label1"))).then(OpenFolderListAction()).then(GoToLabelAction("label1")).then(OpenFolderListAction()).then(GoToLabelAction("label2")).then(ShortSwipeContextMenuRemoveLabelsAction(0, YSArray("label2"))).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.inbox))
  }

}

open class ShortSwipeMenuMarkUnmarkAsSpamInCompactModeTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Пометка письма спамом-не спамом в компактном режиме")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(636).androidCase(10538)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.nextMessage("subj1").nextMessage("subj2").nextMessage("subj3")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(OpenSettingsAction()).then(OpenGeneralSettingsAction()).then(TurnOnCompactMode()).then(CloseGeneralSettingsAction()).then(CloseRootSettings()).then(GoToFilterUnreadAction()).then(ShortSwipeContextMenuMarkAsSpamAction(1)).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.spam)).then(ShortSwipeContextMenuMarkAsNotSpamAction(0)).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.inbox))
  }

}

open class ShortSwipeMenuDeleteMessageFromTrashInCompactModeTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Удаление письма из папки Удаленные в компактном режиме")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(637).androidCase(10539)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.switchFolder(DefaultFolderName.trash).nextMessage("subj1").nextMessage("subj2").nextMessage("subj3")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(OpenSettingsAction()).then(OpenGeneralSettingsAction()).then(TurnOnCompactMode()).then(CloseGeneralSettingsAction()).then(CloseRootSettings()).then(GoToFolderAction(DefaultFolderName.trash)).then(ShortSwipeContextMenuDeleteAction(1))
  }

}

open class ShortSwipeMenuArchiveMessageInCompactModeTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Архивация письма в компактном режиме")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(638).androidCase(10540)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.nextMessage("subj1").nextMessage("subj2").nextMessage("subj3")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(OpenSettingsAction()).then(OpenGeneralSettingsAction()).then(TurnOnCompactMode()).then(CloseGeneralSettingsAction()).then(CloseRootSettings()).then(GoToFolderAction(DefaultFolderName.inbox)).then(GroupModeInitialSelectAction(0)).then(GroupModeSelectAction(1)).then(GroupModeMarkImportantAction()).then(OpenFolderListAction()).then(GoToFilterImportantAction()).then(ShortSwipeContextMenuArchiveAction(1)).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.archive))
  }

}

open class ShortSwipeMenuUndoSpamMessageTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Отмена отправки в спам письма")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(8625).androidCase(10545)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.switchFolder(DefaultFolderName.trash).nextMessage("subj1").nextMessage("subj2").nextMessage("subj3")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.trash)).then(ShortSwipeContextMenuMarkAsSpamAction(1)).then(UndoSpamAction())
  }

}

open class ShortSwipeMenuUndoSpamThreadTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Отмена отправки в спам треда")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(8632).androidCase(10548)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.createFolder("UserFolder").switchFolder("UserFolder").nextThread("subj1", 2).nextThread("subj2", 3).nextThread("subj3", 4)
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(GoToFolderAction("UserFolder")).then(ShortSwipeContextMenuMarkAsSpamAction(1)).then(UndoSpamAction())
  }

}

open class ShortSwipeMenuUndoDeleteThreadTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Отмена удаления треда")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(8627).androidCase(10514)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.nextThread("subj1", 2).nextThread("subj2", 3).nextThread("subj3", 4)
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(ShortSwipeContextMenuDeleteAction(1)).then(UndoDeleteAction())
  }

}

open class ShortSwipeMenuUndoDeleteMessageTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Отмена удаления письма")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(8630).androidCase(10547)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.nextMessage("subj1").nextMessage("subj2").nextMessage("subj3")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(GoToFilterUnreadAction()).then(ShortSwipeContextMenuDeleteAction(1)).then(UndoDeleteAction())
  }

}

open class ShortSwipeMenuUndoArchiveThreadTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Отмена архивирования треда")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(8629).androidCase(10546)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.nextThread("subj1", 2).nextThread("subj2", 3).nextThread("subj3", 4)
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(ShortSwipeContextMenuArchiveAction(0)).then(UndoArchiveAction())
  }

}

open class ShortSwipeMenuUndoArchiveMessageTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Отмена архивирования письма")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(8634).androidCase(10515)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.switchFolder(DefaultFolderName.sent).nextMessage("subj1").nextMessage("subj2").nextMessage("subj3")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.sent)).then(ShortSwipeContextMenuArchiveAction(0)).then(UndoArchiveAction())
  }

}

open class SearchAndMarkMessageReadByShortSwipeMenuTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Пометка письма прочитанным в Поиске")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(8867).androidCase(10200)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.nextMessage("subj1").nextMessage("subj2").nextMessage("subj3")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenSearchAction()).then(SearchAllMessagesAction()).then(ShortSwipeContextMenuMarkAsReadAction(0)).then(AssertAction()).then(CloseSearchAction())
  }

}

open class SearchAndDeleteMessageByShortSwipeMenuTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Удаление письма в Поиске")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(8869).androidCase(7363)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.nextMessage("subj1").nextMessage("subj2").nextMessage("subj3")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenSearchAction()).then(SearchAllMessagesAction()).then(ShortSwipeContextMenuDeleteAction(1)).then(AssertAction()).then(CloseSearchAction()).then(AssertAction()).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.trash))
  }

}

open class SearchAndMoveMessageToAnotherFolderByShortSwipeMenuTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Перемещение письма в другую папку в Поиске")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(8876).androidCase(7374)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.nextMessage("subj1").nextMessage("subj2").nextMessage("subj3").createFolder("UserFolder")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenSearchAction()).then(SearchAllMessagesAction()).then(ShortSwipeContextMenuMoveToFolderAction(1, "UserFolder")).then(AssertAction()).then(CloseSearchAction()).then(AssertAction()).then(OpenFolderListAction()).then(GoToFolderAction("UserFolder"))
  }

}

open class MoveMessageFromInboxTabToMailingListTabByShortSwipeMenuTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Перемещение одиночного сообщения из таба в таб")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(7583).androidCase(540).ignoreOn(MBTPlatform.Android)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.turnOnTab().nextMessage("subj1").nextMessage("subj2").nextMessage("subj3")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(ShortSwipeContextMenuMoveToFolderAction(1, DefaultFolderName.mailingLists)).then(AssertAction()).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.mailingLists))
  }

}

open class MarkMessageImportantInMailingListTabByShortSwipeMenuTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Пометка письма Важным в табе")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(7593).androidCase(551).ignoreOn(MBTPlatform.Android)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.turnOnTab().switchFolder(FolderBackendName.mailingLists).nextMessage("subj1").nextMessage("subj2").nextMessage("subj3")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.mailingLists)).then(ShortSwipeContextMenuMarkAsImportantAction(1)).then(AssertAction()).then(OpenFolderListAction()).then(GoToFilterImportantAction())
  }

}

open class MarkMessageUnimportantInSocialNetworksTabByShortSwipeMenuTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Снятие метки Важное в табе")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(7594).androidCase(552).ignoreOn(MBTPlatform.Android)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.turnOnTab().switchFolder(FolderBackendName.socialNetworks).nextMessage("subj1").nextMessage("subj2").nextMessage("subj3")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.socialNetworks)).then(ShortSwipeContextMenuMarkAsImportantAction(1)).then(ShortSwipeContextMenuMarkAsUnimportantAction(1)).then(AssertAction()).then(OpenFolderListAction()).then(GoToFilterImportantAction())
  }

}

open class UnmarkLabelInInboxTabByShortSwipeMenuTest: RegularYandexMailTestBase {
  public init() {
    super.init("ShortSwipeMenu. Снятие пользовательской метки с письма в табе")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(7596).androidCase(554).ignoreOn(MBTPlatform.Android)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.turnOnTab().nextCustomMessage(MessageSpecBuilder().withDefaults().addLabels(YSArray(LabelData("label1"), LabelData("label2"))).withSubject("subj1")).nextMessage("subj2").nextMessage("subj3")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(ShortSwipeContextMenuRemoveLabelsAction(0, YSArray("label1"))).then(AssertAction()).then(OpenFolderListAction()).then(GoToLabelAction("label1"))
  }

}

