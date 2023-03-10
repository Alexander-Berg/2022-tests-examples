// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM tests/long-swipe-tests.ts >>>

import Foundation

open class LongSwipeToDeleteMessageTest: RegularYandexMailTestBase {
  public init() {
    super.init("LongSwipe. Удаление письма из папки Отправленные")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(24).androidCase(6194)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.switchFolder(DefaultFolderName.sent).nextMessage("subj")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.sent)).then(DeleteMessageByLongSwipeAction(0)).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.trash))
  }

}

open class LongSwipeToDeleteThreadTest: RegularYandexMailTestBase {
  public init() {
    super.init("LongSwipe. Удаление треда из папки Входящие")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(6219).androidCase(6474)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.nextThread("thread1", 2).nextThread("thread2", 3).nextThread("thread3", 4)
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(DeleteMessageByLongSwipeAction(2)).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.trash))
  }

}

open class LongSwipeToArchiveMessageTest: RegularYandexMailTestBase {
  public init() {
    super.init("LongSwipe. Архивация письма из пользовательской папки")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(6225).androidCase(6479)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.createFolder("UserFolder").switchFolder("UserFolder").nextMessage("subj").nextMessage("subj2")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(OpenSettingsAction()).then(OpenGeneralSettingsAction()).then(SetActionOnSwipe(ActionOnSwipe.archive)).then(CloseGeneralSettingsAction()).then(CloseRootSettings()).then(GoToFolderAction("UserFolder")).then(ArchiveMessageByLongSwipeAction(0)).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.archive))
  }

}

open class LongSwipeToArchiveThreadTest: RegularYandexMailTestBase {
  public init() {
    super.init("LongSwipe. Архивация треда из папки Входящие")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(6226).androidCase(6480)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.nextThread("thread1", 2).nextThread("thread2", 3).nextThread("thread3", 4)
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(OpenSettingsAction()).then(OpenGeneralSettingsAction()).then(SetActionOnSwipe(ActionOnSwipe.archive)).then(CloseGeneralSettingsAction()).then(CloseRootSettings()).then(GoToFolderAction(DefaultFolderName.inbox)).then(ArchiveMessageByLongSwipeAction(2)).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.archive))
  }

}

open class LongSwipeToArchiveFromArchiveThreadTest: RegularYandexMailTestBase {
  public init() {
    super.init("LongSwipe. Удаление письма из Архива при действии по свайпу Архивировать")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(6613).androidCase(6623)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.switchFolder(DefaultFolderName.archive).nextMessage("subj").nextMessage("subj2")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(OpenSettingsAction()).then(OpenGeneralSettingsAction()).then(SetActionOnSwipe(ActionOnSwipe.archive)).then(CloseGeneralSettingsAction()).then(CloseRootSettings()).then(GoToFolderAction(DefaultFolderName.archive)).then(DeleteMessageByLongSwipeAction(1))
  }

}

open class LongSwipeToDeleteFromTrashTest: RegularYandexMailTestBase {
  public init() {
    super.init("LongSwipe. Удаление письма длинным свайпом из папки Удаленные")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(6612).androidCase(6624)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.switchFolder(DefaultFolderName.trash).nextMessage("subj").nextMessage("subj2")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.trash)).then(DeleteMessageByLongSwipeAction(1))
  }

}

open class LongSwipeToArchiveInCompactModeTest: RegularYandexMailTestBase {
  public init() {
    super.init("LongSwipe. Архивация письма длинным свайпом в компактном режиме")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(628).androidCase(10164)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.switchFolder(DefaultFolderName.spam).nextMessage("subj1").nextMessage("subj2").nextMessage("subj3").nextMessage("subj4")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(OpenSettingsAction()).then(OpenGeneralSettingsAction()).then(TurnOnCompactMode()).then(SetActionOnSwipe(ActionOnSwipe.archive)).then(CloseGeneralSettingsAction()).then(CloseRootSettings()).then(GoToFolderAction(DefaultFolderName.spam)).then(ArchiveMessageByLongSwipeAction(1)).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.archive))
  }

}

open class LongSwipeLongSwipeToDeleteFromSearchInCompactModeTest: RegularYandexMailTestBase {
  public init() {
    super.init("LongSwipe. Удаление письма длинным свайпом из поиска в компактном режиме")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(626).androidCase(10163)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.nextMessage("subj1").nextMessage("subj2").nextMessage("subj3").nextMessage("subj4")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(OpenSettingsAction()).then(OpenGeneralSettingsAction()).then(TurnOnCompactMode()).then(CloseGeneralSettingsAction()).then(CloseRootSettings()).then(GoToFolderAction(DefaultFolderName.inbox)).then(OpenSearchAction()).then(SearchAllMessagesAction()).then(DeleteMessageByLongSwipeAction(1))
  }

}

open class LongSwipeUndoDeleteMessageTest: RegularYandexMailTestBase {
  public init() {
    super.init("LongSwipe. Отмена удаления письма из папки Входящие")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(6195).androidCase(6066)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.nextMessage("subj1").nextMessage("subj2").nextMessage("subj3")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(DeleteMessageByLongSwipeAction(1)).then(UndoDeleteAction())
  }

}

open class LongSwipeUndoDeleteThreadTest: RegularYandexMailTestBase {
  public init() {
    super.init("LongSwipe. Отмена удаления треда из Пользовательской папки")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(8628).androidCase(9843)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.createFolder("UserFolder").switchFolder("UserFolder").nextThread("subj1", 3).nextThread("subj2", 4)
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(GoToFolderAction("UserFolder")).then(DeleteMessageByLongSwipeAction(0)).then(UndoDeleteAction())
  }

}

open class LongSwipeUndoArchiveThreadTest: RegularYandexMailTestBase {
  public init() {
    super.init("LongSwipe. Отмена архивирования треда из папки Входящие")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.androidCase(10591)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.nextThread("subj1", 3).nextThread("subj2", 2).nextMessage("subj3")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(OpenSettingsAction()).then(OpenGeneralSettingsAction()).then(SetActionOnSwipe(ActionOnSwipe.archive)).then(CloseGeneralSettingsAction()).then(CloseRootSettings()).then(GoToFolderAction(DefaultFolderName.inbox)).then(ArchiveMessageByLongSwipeAction(1)).then(UndoArchiveAction())
  }

}

open class LongSwipeUndoArchiveMessageTest: RegularYandexMailTestBase {
  public init() {
    super.init("LongSwipe. Отмена архивирования письма из папки Спам")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(8624).androidCase(9851)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.switchFolder(DefaultFolderName.spam).nextMessage("subj1").nextMessage("subj2").nextMessage("subj3")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(OpenSettingsAction()).then(OpenGeneralSettingsAction()).then(SetActionOnSwipe(ActionOnSwipe.archive)).then(CloseGeneralSettingsAction()).then(CloseRootSettings()).then(GoToFolderAction(DefaultFolderName.spam)).then(ArchiveMessageByLongSwipeAction(1)).then(UndoArchiveAction())
  }

}

open class LongSwipeUndoDeleteMessageAtSearchTest: RegularYandexMailTestBase {
  public init() {
    super.init("LongSwipe. Отмена удаления письма в поиске")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(8856).androidCase(10159)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.nextMessage("subj1").nextMessage("subj2").nextMessage("subj3")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenSearchAction()).then(SearchAllMessagesAction()).then(DeleteMessageByLongSwipeAction(1)).then(UndoDeleteAction())
  }

}

open class LongSwipeDeleteMessageAtSearchTest: RegularYandexMailTestBase {
  public init() {
    super.init("LongSwipe. Удаление письма в поиске")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(8852).androidCase(10157)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.nextMessage("subj1").nextMessage("subj2").nextMessage("subj3")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenSearchAction()).then(SearchAllMessagesAction()).then(DeleteMessageByLongSwipeAction(1)).then(AssertAction()).then(CloseSearchAction()).then(AssertAction()).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.trash))
  }

}

open class LongSwipeArchiveMessageAtSearchTest: RegularYandexMailTestBase {
  public init() {
    super.init("LongSwipe. Архивация письма в поиске")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(8853).androidCase(10568)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.nextMessage("subj1").nextMessage("subj2").nextMessage("subj3")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(OpenSettingsAction()).then(OpenGeneralSettingsAction()).then(SetActionOnSwipe(ActionOnSwipe.archive)).then(CloseGeneralSettingsAction()).then(CloseRootSettings()).then(GoToFolderAction(DefaultFolderName.inbox)).then(OpenSearchAction()).then(SearchAllMessagesAction()).then(ArchiveMessageByLongSwipeAction(2)).then(AssertAction()).then(CloseSearchAction()).then(AssertAction()).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.archive))
  }

}

open class LongSwipeDeleteMessageAtTabMailingListsTest: RegularYandexMailTestBase {
  public init() {
    super.init("LongSwipe. Удаление письма из таба Рассылки")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(7590).androidCase(9835).ignoreOn(MBTPlatform.Android)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.turnOnTab().switchFolder(FolderBackendName.mailingLists).nextMessage("subj1").nextMessage("subj2").nextMessage("subj3")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.mailingLists)).then(DeleteMessageByLongSwipeAction(1)).then(AssertAction()).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.trash))
  }

}

open class LongSwipeDeleteThreadLandscapeTest: RegularYandexMailTestBase {
  public init() {
    super.init("LongSwipe. Удаление треда в лендскейпе")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(8864).androidCase(10343)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.nextThread("subj1", 3)
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(RotateToLandscape()).then(DeleteMessageByLongSwipeAction(0)).then(AssertAction()).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.trash))
  }

}

open class LongSwipeDeleteMessageAtSearch2paneTest: RegularYandexMailTestBase {
  public init() {
    super.init("LongSwipe. Удаление письма в поиске в 2pane")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(8858).androidCase(10341).setTags(YSArray(DeviceType.Tab))
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.nextMessage("subj1").nextMessage("subj2").nextMessage("subj3")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(RotateToLandscape()).then(OpenSearchAction()).then(SearchAllMessagesAction()).then(OpenMessageAction(1)).then(SwitchContextToAction(MaillistComponent())).then(DeleteMessageByLongSwipeAction(1)).then(AssertAction()).then(CloseSearchAction()).then(AssertAction()).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.trash))
  }

}

open class LongSwipeDeleteThread2paneTest: RegularYandexMailTestBase {
  public init() {
    super.init("LongSwipe. Удаление треда в 2pane")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(8861).androidCase(10342).setTags(YSArray(DeviceType.Tab))
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.nextThread("subj1", 3)
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(RotateToLandscape()).then(OpenMessageAction(0)).then(SwitchContextToAction(MaillistComponent())).then(DeleteMessageByLongSwipeAction(0)).then(AssertAction()).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.trash))
  }

}

open class LongSwipeToDeleteFromImportantTest: RegularYandexMailTestBase {
  public init() {
    super.init("LongSwipe. Удаление письма длинным свайпом из метки Важные")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(10358).androidCase(9836)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.nextMessage("subj").nextMessage("subj2")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(ShortSwipeContextMenuMarkAsImportantAction(0)).then(OpenFolderListAction()).then(GoToFilterImportantAction()).then(DeleteMessageByLongSwipeAction(0))
  }

}

open class LongSwipeToDeleteFromTrashUndoTest: RegularYandexMailTestBase {
  public init() {
    super.init("LongSwipe. Отмена удаления письма длинным свайпом из папки Удаленные")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.iosCase(10364).androidCase(9838)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.switchFolder(DefaultFolderName.trash).nextMessage("subj").nextMessage("subj2")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.trash)).then(DeleteMessageByLongSwipeAction(0, false))
  }

}

open class LongSwipeToDeleteDraftInCompactModeTest: RegularYandexMailTestBase {
  public init() {
    super.init("LongSwipe. Удаление черновика длинным свайпом в compact режиме в Черновиках")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.androidCase(9844).iosCase(10375)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.switchFolder(DefaultFolderName.draft).nextMessage("draft1").nextMessage("draft2")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(OpenSettingsAction()).then(OpenGeneralSettingsAction()).then(TurnOnCompactMode()).then(CloseGeneralSettingsAction()).then(CloseRootSettings()).then(GoToFolderAction(DefaultFolderName.draft)).then(DeleteMessageByLongSwipeAction(0)).then(AssertAction()).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.trash))
  }

}

open class LongSwipeToArchiveMsgFromLabelInCompactModeTest: RegularYandexMailTestBase {
  public init() {
    super.init("LongSwipe. Архивация письма длинным свайпом в пользовательской метке")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.androidCase(9848)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.nextCustomMessage(MessageSpecBuilder().withDefaults().addLabels(YSArray(LabelData("label1"))).withSubject("subj1")).nextCustomMessage(MessageSpecBuilder().withDefaults().addLabels(YSArray(LabelData("label1"))).withSubject("subj2")).createLabel(LabelData("label1"))
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(OpenSettingsAction()).then(OpenGeneralSettingsAction()).then(SetActionOnSwipe(ActionOnSwipe.archive)).then(CloseGeneralSettingsAction()).then(CloseRootSettings()).then(GoToLabelAction("label1")).then(ArchiveMessageByLongSwipeAction(0)).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.archive))
  }

}

open class LongSwipeToDeleteThreadInCompactModeTest: RegularYandexMailTestBase {
  public init() {
    super.init("LongSwipe. Удаление треда длинным свайпом в компактном режиме")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.androidCase(10350).iosCase(8865)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.nextThread("thread1", 2).nextThread("thread2", 3)
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(OpenSettingsAction()).then(OpenGeneralSettingsAction()).then(TurnOnCompactMode()).then(CloseGeneralSettingsAction()).then(CloseRootSettings()).then(GoToFolderAction(DefaultFolderName.inbox)).then(DeleteMessageByLongSwipeAction(1)).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.trash))
  }

}

open class LongSwipeToArchiveMessageInLandscapeTest: RegularYandexMailTestBase {
  public init() {
    super.init("LongSwipe. Архивация письма в пользовательской папке 2pane")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.androidCase(10348).iosCase(8862).setTags(YSArray(DeviceType.Tab))
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.createFolder("UserFolder").switchFolder("UserFolder").nextMessage("subj").nextMessage("subj2")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(OpenSettingsAction()).then(OpenGeneralSettingsAction()).then(SetActionOnSwipe(ActionOnSwipe.archive)).then(CloseGeneralSettingsAction()).then(CloseRootSettings()).then(RotateToLandscape()).then(GoToFolderAction("UserFolder")).then(ArchiveMessageByLongSwipeAction(0)).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.archive))
  }

}

open class LongSwipeToUndoDeleteMessageLandscapeTest: RegularYandexMailTestBase {
  public init() {
    super.init("LongSwipe. Отмена удаления письма из папки Отправленные 2pane")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.androidCase(10349).iosCase(8863).setTags(YSArray(DeviceType.Tab))
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.switchFolder(DefaultFolderName.sent).nextMessage("subj")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(RotateToLandscape()).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.sent)).then(DeleteMessageByLongSwipeAction(0)).then(UndoDeleteAction())
  }

}

open class LongSwipeToUndoArchiveMessageLandscapeTest: RegularYandexMailTestBase {
  public init() {
    super.init("LongSwipe. Отмена архивирования письма из пользовательской папки 2pane")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.androidCase(10352).iosCase(9163).setTags(YSArray(DeviceType.Tab))
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.createFolder("UserFolder").switchFolder("UserFolder").nextMessage("subj").nextMessage("subj2")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(RotateToLandscape()).then(OpenFolderListAction()).then(OpenSettingsAction()).then(OpenGeneralSettingsAction()).then(SetActionOnSwipe(ActionOnSwipe.archive)).then(CloseGeneralSettingsAction()).then(CloseRootSettings()).then(GoToFolderAction("UserFolder")).then(ArchiveMessageByLongSwipeAction(0)).then(UndoArchiveAction())
  }

}

open class LongSwipeToArchiveTemplateTest: RegularYandexMailTestBase {
  public init() {
    super.init("LongSwipe. Архивирование шаблона в компактном режиме")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.androidCase(9853)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.switchFolder(DefaultFolderName.template).nextMessage("subj1")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(OpenSettingsAction()).then(OpenGeneralSettingsAction()).then(SetActionOnSwipe(ActionOnSwipe.archive)).then(TurnOnCompactMode()).then(CloseGeneralSettingsAction()).then(CloseRootSettings()).then(GoToFolderAction(DefaultFolderName.template)).then(ArchiveMessageByLongSwipeAction(0)).then(AssertAction()).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.archive))
  }

}

open class LongSwipeToArchiveThreadWithUndoTest: RegularYandexMailTestBase {
  public init() {
    super.init("LongSwipe. Архивация треда с Отменой (письма в разных папках)")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.androidCase(9852).iosCase(8626)
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    mailbox.nextMessage("thread1").switchFolder(DefaultFolderName.sent).nextMessage("thread1").createFolder("subfolder", YSArray("folder")).switchFolder("subfolder", YSArray("folder")).nextMessage("thread1").createFolder("folder2").switchFolder("folder2").nextMessage("thread1")
  }

  @discardableResult
  open override func testScenario(_ account: UserAccount) -> TestPlan {
    return self.yandexLogin(account).then(OpenFolderListAction()).then(OpenSettingsAction()).then(OpenGeneralSettingsAction()).then(SetActionOnSwipe(ActionOnSwipe.archive)).then(CloseGeneralSettingsAction()).then(CloseRootSettings()).then(GoToFolderAction("subfolder", YSArray("folder"))).then(AssertAction()).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.inbox)).then(ArchiveMessageByLongSwipeAction(0)).then(UndoArchiveAction())
  }

}

