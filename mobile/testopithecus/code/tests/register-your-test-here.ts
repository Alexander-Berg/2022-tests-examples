import { MailboxBuilder } from '../mail/mailbox-preparer'
import { AccountType2 } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { TestsRegistry } from '../../../testopithecus-common/code/mbt/test/tests-registry'
import { ArchiveFirstMessageTest } from './archive-message-tests'
import {
  ComposeCloseSenderSuggestTest,
  ComposeCreateYabbleWithLongEmailTest,
  ComposeCreateYabbleWithNumericEmailTest,
  ComposeDeleteYabbleByTapOnCrossTest,
  ComposeEmptyRecipientsSuggestTest,
  ComposeEnterAndDeleteTextLandscapeTest,
  ComposeForwardFromMailViewTest,
  ComposeForwardViaShortSwipeMenuTest,
  ComposeLongEmailInSuggestTest,
  ComposeMinimizeRecipientsSuggestAfterSomeActionsTest,
  ComposeReplyFromMailViewTest,
  ComposeReplyViaShortSwipeMenuTest,
  ComposeSelectSenderFromSuggestTest,
  ComposeSendEmptyMessageTest,
  ComposeSendMessageToRecipientInCCFieldTest,
  ComposeSendMessageToRecipientWithLatinAndCyrillicLettersInEmailTest,
  ComposeSendMessageWithAllFilledFieldsTest,
  ComposeSendMessageWithLongSubjectTest,
  ComposeSendMessageWithNotGeneratedValidYabbleTest,
  ComposeSuggestBehaviorWhileEnterEmailTest,
  ComposeSuggestDomainTest,
  ComposeSuggestMissingAddedRecipientsTest,
  ComposeSuggestOfManyContactTest,
  ComposeSuggestOfSomeContactTest,
  ComposeSuggestRotateTest,
} from './compose-message-tests'
import { ChangeFilterTest } from './filter-navigation-tests'
import { ValidateFiltersListPt1Test, ValidateFiltersListPt2Test } from './filters-tests'
import {
  ChangeFoldersInboxCustomTest,
  ValidateFolderListTest,
  ValidateLabelListTest,
  NewFolderFromBackTestAndroid,
  NewFolderFromBackTestIos,
  NewLabelFromBackTestAndroid,
  NewLabelFromBackTestIos,
  ChangeFoldersArchiveSpamTrashTest,
  ChangeFoldersSentDraftTest,
  LongFolderNameViewTest,
} from './folder-navigation-tests'
import {
  CanOpenMessageAfterGroupActionTest,
  GroupDeleteMessagesTest,
  GroupMarkAsReadDifferentMessagesTest,
  GroupMarkAsReadMessagesTest,
  GroupModeAddRemoveLabelInCompactModeTest,
  GroupModeArchiveMessageFromSearchInCompactModeTest,
  GroupModeArchiveMessagesTest,
  GroupModeArchiveThreadTest,
  GroupModeCancelSelectionTest,
  GroupModeDeleteFromTrashTest,
  GroupModeDeleteMessageInCompactModeTest,
  GroupModeDeleteThreadTest,
  GroupModeExitByTapOnSelectedMessagesTest,
  GroupModeInitialSelectInCompactModeTest,
  GroupModeInitialSelectTest,
  GroupModeMarkAsSpamNotSpamMessageInCompactModeTest,
  GroupModeMarkImportantTest,
  GroupModeMarkImportantUnimportantMessageInCompactModeTest,
  GroupModeMarkLabelTest,
  GroupModeMarkReadMessageTest,
  GroupModeMarkReadThreadTest,
  GroupModeMarkSpamTest,
  GroupModeMarkSpamThreadTest,
  GroupModeMarkUnreadMessageTest,
  GroupModeMarkUnreadThreadTest,
  GroupModeMoveMessageToUserFolderFromInboxTest,
  GroupModeMoveThreadsToInboxTest,
  GroupModeSelectAllTest,
  GroupModeUndoArchiveMessagesAndThreadsTest,
  GroupModeUndoArchiveMessageTest,
  GroupModeUndoArchiveThreadTest,
  GroupModeUndoDeleteMessagesAndThreadsTest,
  GroupModeUndoDeleteMessageTest,
  GroupModeUndoDeleteThreadTest,
  GroupModeUndoSpamMessageTest,
  GroupModeUndoSpamMessagesAndThreadsTest,
  GroupModeUndoSpamThreadTest,
  GroupModeUnmarkImportantTest,
  GroupModeUnmarkLabelTest,
  GroupModeUnmarkSpamTest,
  GroupModeMoveMessageToSpamFromSocialTabTest,
  GroupModeApplyLabelToMessageInMailingListTabTest,
  GroupModeMoveMessageFromTabInboxToUserFolderTest,
  GroupModeSelectAllMessagesInSearchTest,
  GroupModeMardMessagesAsRead2paneTest,
  GroupModeDeleteMessage2paneTest,
} from './group-mode-tests'
import { LabelAllThreadMessagesImportantByLabellingMainMessageTest, MarkAsImportantTest } from './importance-tests'
import { InboxTopBarDisplayTest } from './inbox-top-bar-display-test'
import { LabelViewLoadingTest, LongLabelNameViewTest } from './label-navigation-tests'
import { ChoseAccountFromAccountsListTest, SwitchAccountTest } from './login-tests'
import {
  LongSwipeArchiveMessageAtSearchTest,
  LongSwipeDeleteMessageAtSearch2paneTest,
  LongSwipeDeleteMessageAtSearchTest,
  LongSwipeDeleteMessageAtTabMailingListsTest,
  LongSwipeDeleteThread2paneTest,
  LongSwipeDeleteThreadLandscapeTest,
  LongSwipeLongSwipeToDeleteFromSearchInCompactModeTest,
  LongSwipeToArchiveFromArchiveThreadTest,
  LongSwipeToArchiveInCompactModeTest,
  LongSwipeToArchiveMessageTest,
  LongSwipeToArchiveThreadTest,
  LongSwipeToDeleteFromTrashTest,
  LongSwipeToDeleteMessageTest,
  LongSwipeToDeleteThreadTest,
  LongSwipeUndoArchiveMessageTest,
  LongSwipeUndoArchiveThreadTest,
  LongSwipeUndoDeleteMessageAtSearchTest,
  LongSwipeUndoDeleteMessageTest,
  LongSwipeUndoDeleteThreadTest,
  LongSwipeToDeleteFromImportantTest,
  LongSwipeToDeleteFromTrashUndoTest,
  LongSwipeToDeleteDraftInCompactModeTest,
  LongSwipeToArchiveMsgFromLabelInCompactModeTest,
  LongSwipeToDeleteThreadInCompactModeTest,
  LongSwipeToArchiveMessageInLandscapeTest,
  LongSwipeToUndoDeleteMessageLandscapeTest,
  LongSwipeToUndoArchiveMessageLandscapeTest,
  LongSwipeToArchiveTemplateTest,
  LongSwipeToArchiveThreadWithUndoTest,
} from './long-swipe-tests'
import {
  AddLabelsFromMessageView,
  ArchiveMessageFromThreadFromMessageView,
  ArchiveSingleMessageFromMessageView,
  MarkAsNotSpamFromMessageViewAndroid,
  DeleteLabelsFromMessageView,
  DeleteMessageByTapOnIconTest,
  DeleteMessageByTapOnTopBarTest,
  DeleteMessageFromMessageViewThreadModeOff,
  DeleteMessageInSearchByTapOnTopBarTest,
  DeleteSingleMessageFromMessageViewTest,
  DeleteSingleMessageFromThreadFromMessageViewTest,
  LablesViewFromMessageView,
  MarkAsNotSpamFromMessageView,
  MarkAsReadByExpandThreadTest,
  MarkAsReadByOpeningMessageViewTest,
  MarkAsReadFromMessageViewTest,
  MarkAsSpamFromMessageView,
  MarkAsUnreadFromMessageViewTest,
  MarkImportantFromMessageView,
  MarkUnimportantFromMessageView,
  MoveMessageFromThreadToTrashFromMailViewTest,
  MoveMessageOfThreadToArchiveFromMailViewTest,
  MoveMessageToSpamFromMailViewFromSearchTest,
  MoveMessageToSpamFromMailViewTest,
  MoveMessageToTabFromMailViewTest,
  MoveMessageToTrashFromMailViewFromSearchTest,
  MoveMessageToTrashFromMailViewTest,
  MoveMessageToUsersFolderFromMessageView,
  MoveMessageToUsersFolderFromThreadFromMessageView,
  MarkAsSpamMessageFromThreadFromMessageView,
  UndoMessageDeleteFromMessageView,
  MarkImportantMessageFromThreadFromMessageView,
  AddAndDeleteLabelsFromMessageView,
  DeleteMessageByTapOnIcon2PaneTest,
  ViewOperationsInInboxFolderWithLabeledMsg,
  ViewOperationsInInboxFolder,
  ViewOperationsInSentFolder,
  ViewOperationsInArchiveFolderWithLabeledMsg,
  ViewOperationsInUserFolder,
  MarkLabelInMessageView,
  UndoMessageDeleteFromMessage2paneView,
  MessageViewCreateAndMarkLabelTest,
  MailViewArchiveMessageByTapOnTopBarTest,
  MailViewArchiveThreadByTapOnTopBarTest,
  MailViewDeleteThreadByTapOnTopBarTest,
  MailViewMarkAsSpamOneMessageInThreadLandscapeTest,
  MailView2paneUndoMessageDeleteInSearchTest,
  MailView2paneUndoDeleteOneMessageInThreadTest,
  MailView2paneUndoArchiveOneMessageInThreadTest,
  MailView2paneUndoSpamMessageTest,
  MailView2paneUndoSpamMessageInSearchTest,
  MailViewMarkAsSpamMessageFromThread2paneTest,
} from './mail-view-tests'
import {
  GoToTabByTabsNotification,
  MailListViewInCompactMode,
  MarkAsImportantMessageInTabSubscriptionTest,
  TabNotificationInEmptyMessageList,
  TabNotificationInTheFirstInMailList,
  TabNotificationInTheMiddleMailList,
  ThreadModeTurnOffAndThenOnTest,
  TabNotificationInCompactMode,
  TabsNotificationOnlyInInbox,
  TabsNotificationInLandscape,
  DisplayTabsNotificationAfterMoveMessage,
  GroupModeWithTabNotification,
  MailListViewInThreadFolder,
  MailListViewInThreadLessFolder,
  MailListViewInThreadFolderLandscape,
  MailListViewInThreadLessFolderLandscape,
  MailListViewInUnreadLabel,
  MailListViewInUnreadLabelLandscape,
  MailListViewInImportantLabel,
  MailListViewInUserLabel,
  MailListViewInThreadFolderTab,
  MailListViewInThreadLessFolderTab,
  MailListViewInImportantLabelTab,
  MailListViewNavigateByArrowsTab,
} from './maillist-view-tests'
import {
  ManageFoldersAddNewFolderInRootTest,
  ManageFoldersDeleteFolderWithSubfoldersByLongSwipeTest,
  ManageFoldersEditFolderTest,
  ManageFoldersValidateAddFolderViewTest,
  ManageFoldersValidateEditFolderViewTest,
  ManageFoldersValidateFolderLocationViewTest,
  ManageFoldersValidateViewTest,
  ManageFoldersDeleteFolderTest,
} from './manage-folders-tests'
import {
  ManageLabelsAddNewLabelTest,
  ManageLabelsDeleteOpenedLabelByLongSwipeTest,
  ManageLabelsEditLabelTest,
  ManageLabelsValidateAddLabelViewTest,
  ManageLabelsValidateEditLabelViewTest,
  ManageLabelsValidateViewTest,
} from './manage-labels-tests'
import { MoveToFolderTest } from './move-to-folder-tests'
import { ChangePinTest, ResetPinTest, TurnOffPinTest, TurnOnPinTest } from './pin-tests'
import {
  MarkAllThreadMessagesReadByMarkingMainMessageTest,
  MarkUnreadAfterReadTest,
  ReadMessageAfterOpeningTest,
} from './read-unread-tests'
import {
  MarkAsReadInSearch,
  SearchMessageListViewInCompactModeTest,
  SearchAndOpenMessage,
  SearchAndOpenMessageIn2Pane,
} from './search-tests'
import {
  ChangingActionOnSwipeTest,
  ClearCacheTest,
  OpenAboutSettingsTest,
  OpenAccountSettingsTest,
  TurningOnCompactModeTest,
  TurningOffCompactModeTest,
  ValidateAccountSettingsTest,
  ValidateGeneralSettingsTest,
  ValidateRootSettingsTest,
  DefaultActionOnSwipeTest,
  ViewAccountSettingsInLandscapeTest,
  ViewSettingsTestTab,
  ViewSettingsInLandscapeTest,
  UndoClearCacheTest,
} from './settings-tests'
import {
  MarkMessageImportantInMailingListTabByShortSwipeMenuTest,
  MarkMessageUnimportantInSocialNetworksTabByShortSwipeMenuTest,
  MoveMessageFromInboxTabToMailingListTabByShortSwipeMenuTest,
  SearchAndDeleteMessageByShortSwipeMenuTest,
  SearchAndMarkMessageReadByShortSwipeMenuTest,
  SearchAndMoveMessageToAnotherFolderByShortSwipeMenuTest,
  ShortSwipeMenuAddLabelInCompactModeTest,
  ShortSwipeMenuApplyLabelToMessageTest,
  ShortSwipeMenuApplyLabelToThreadTest,
  ShortSwipeMenuArchiveMessageFromSpamTest,
  ShortSwipeMenuArchiveMessageInCompactModeTest,
  ShortSwipeMenuArchiveThreadFromUserFolderTest,
  ShortSwipeMenuDeleteFromTrashTest,
  ShortSwipeMenuDeleteMessageFromTrashInCompactModeTest,
  ShortSwipeMenuDeleteMessageTest,
  ShortSwipeMenuDeleteThreadTest,
  ShortSwipeMenuImportantMessageTest,
  ShortSwipeMenuImportantThreadTest,
  ShortSwipeMenuMarkAsNotSpamMessageTest,
  ShortSwipeMenuMarkAsReadMessageTest,
  ShortSwipeMenuMarkAsReadThreadTest,
  ShortSwipeMenuMarkAsSpamThreadTest,
  ShortSwipeMenuMarkAsUnreadMessageTest,
  ShortSwipeMenuMarkAsUnreadThreadTest,
  ShortSwipeMenuMarkReadUnreadInCompactModeTest,
  ShortSwipeMenuMarkUnmarkAsSpamInCompactModeTest,
  ShortSwipeMenuMoveToFolderMessageTest,
  ShortSwipeMenuMoveToFolderThreadTest,
  ShortSwipeMenuMoveToInboxFromTrashTest,
  ShortSwipeMenuRemoveLabelFromMessageTest,
  ShortSwipeMenuRemoveLabelFromThreadTest,
  ShortSwipeMenuUndoArchiveMessageTest,
  ShortSwipeMenuUndoArchiveThreadTest,
  ShortSwipeMenuUndoDeleteMessageTest,
  ShortSwipeMenuUndoDeleteThreadTest,
  ShortSwipeMenuUndoSpamMessageTest,
  ShortSwipeMenuUndoSpamThreadTest,
  ShortSwipeMenuUnImportantMessageTest,
  ShortSwipeMenuUnImportantThreadTest,
  UnmarkLabelInInboxTabByShortSwipeMenuTest,
} from './short-swipe-menu-tests'
import {
  ShortSwipeDeleteMessageAtSearchTest,
  ShortSwipeDeleteThreadLandscapeTest,
  ShortSwipeMarkMessageAsReadAtSearchTest,
  ShortSwipeToArchiveFromArchiveThreadTest,
  ShortSwipeToArchiveInCompactModeTest,
  ShortSwipeToArchiveMessageTest,
  ShortSwipeToArchiveThreadTest,
  ShortSwipeToDeleteFromTrashTest,
  ShortSwipeToDeleteInCompactModeTest,
  ShortSwipeToDeleteMessageTest,
  ShortSwipeToDeleteThreadTest,
  ShortSwipeUndoArchiveMessageTest,
  ShortSwipeUndoArchiveThreadTest,
  ShortSwipeUndoDeleteMessageTest,
  ShortSwipeUndoDeleteThreadTest,
  SwipeToReadMessageInCompactModeTest,
  SwipeToReadMessageTest,
  SwipeToReadThreadTest,
  SwipeToUnreadMessageTest,
  SwipeToUnreadThreadTest,
  ShortSwipeDeleteMessageAtSearch2paneTest,
  ShortSwipeArchiveMessageAtSearch2paneTest,
  ShortSwipeMarkMessageAsReadAtSearch2paneTest,
  ShortSwipeDeleteMessage2paneTest,
  ShortSwipeArchiveMessage2paneTest,
  ShortSwipeMarkMessageAsRead2paneTest,
  ShortSwipeUndoDeleteMessage2paneTest,
  ShortSwipeUndoArchiveMessage2paneTest,
  SwipeToReadMessageFromInboxTest,
  SwipeToDeleteMessageFromTrashTest,
} from './short-swipe-tests'
import { MoveToSpamFirstMessageTest } from './spamable-tests'
import { StoriesDifferentAccountTest, StoriesHideTest, StoriesRotationTest } from './story-test'
import {
  AbsentOfTranslatorBarIfSourceLanguageIsEqualToTargetLanguageTests,
  AddLanguageToIgnoredLanguagesListAndDeleteTests,
  AddLanguageToRecentLanguagesListTests,
  ChangeDefaultTranslateLanguageTest,
  HideTranslatorBarForAutoLanguageTests,
  HideTranslatorBarTest,
  ResetTranslateAfterReopenMessageTests,
  RevertToOriginalMessageLanguageTests,
  TranslateMessage2paneTests,
} from './translator-tests'
import { FormatTextTest } from './wysiwyg-tests'
import {
  RotateDeviceInZeroSuggestTest,
  SaveQueryToZeroSuggestTest,
  SearchMessagesViaZeroSuggestIn2PaneTest,
  SearchMessagesViaZeroSuggestTest,
  SearchUnreadMessagesViaZeroSuggestTest,
} from './zero-suggest-tests'
import {
  SearchAndMoveToSpamMessage,
  SearchAndDeleteMessageFromUserFolder,
  SearchAndDeleteMessageShortSwipeFromTemplates,
  SearchAndGroupDeleteMessageTestFromTemplates,
  SearchAndGroupDeleteMessageTestFromDraft,
  SearchAndMarkImportantMessageShortSwipe,
  SearchAndMarkMessageRead,
  SearchAndMarkMessageReadFromUserFolder,
  SearchAndMarkMessageUnreadFromUserFolder,
  SearchAndAddLabelMessageFromUserFolder,
  SearchAndAddLabelMessageFromArchive,
  SearchAndAddLabelMessage,
  SearchAndMarkMessageUnreadBySwipeFromSent,
  SearchAndMarkMessageUnreadFromSpam,
  SearchAndDeleteMessageFromSpam,
} from './search-actions-tests'
import {
  QuickReplyToMessageTests,
  QuickReplyOpenFilledComposeTests,
  QuickReplyRotateTests,
  QuickReplyIsTextFieldExpandedTests,
  SmartReplyMissingIfSettingsDisabledTest,
  SmartReplyShowIfSettingsEnabledTest,
} from './quick-reply-tests'
import { ClearSpamTest, CancelClearSpamTest, ClearTrashTest, CancelClearTrashTest } from './clear-folder-tests'
import {
  TabBarOpenCalendarTest,
  TabBarOpenTelemostTest,
  TabBarOpenMoreTabTest,
  TabBarItemsYandexLandscapeTest,
  TabBarCalendarDateLabelTest,
  TabBarShownInMailListTest,
  TabBarNotShownInMailViewTest,
  TabBarNotShownInSearchTest,
  TabBarNotShownInComposeTest,
  TabBarNotShownInFolderListTest,
  TabBarNotShownInSettingsTest,
  TabBarHideInGroupModeTest,
  ShtorkaCloseByTapOverTest,
  ShtorkaCloseBySwipeTest,
  ShtorkaOpenNotesTest,
  ShtorkaOpenDiskTest,
  TabBarOpenDocumentsTest,
} from './tab-bar-tests'
import {
  MoveThreadFromMailingListToSocialNetworksTest,
  MoveThreadFromSocialNetworksTabToUserFolderTest,
  MoveThreadFromUserFolderToInboxTest,
  UndoMoveMessageToArchiveFromInboxTabTest,
  UndoMoveMessageToSpamTest,
  DeleteThreadInTabTest,
  DeleteMessageByTapOnTopBarInTabTest,
  TurnOffTabsTest,
  TurnOnTabsTest,
  UndoDeleteMessageFromTabTest,
} from './tabs-tests'

export class AllMailTests {
  private constructor() {}

  private readonly registry: TestsRegistry<MailboxBuilder> = new TestsRegistry<MailboxBuilder>()

  private registerAllTestsAndGetThem(): TestsRegistry<MailboxBuilder> {
    this.registerStoriesTests()
    this.registerShortSwipeMenuTests()
    this.registerGroupModeTests()
    this.registerShortSwipeTests()
    this.registerLongSwipeTests()
    this.registerManageFoldersTests()
    this.registerManageLabelsTests()
    this.registerTabsTests()
    this.registerTabbarTests()
    this.registerClearFolderTests()
    this.registerQuickReplyTests()
    this.registerSettingTests()
    this.registerTranslatorTests()
    this.registerMessageListViewTests()
    this.registerMailViewTests()
    this.registerFolderListTests()
    this.registerComposeTests()
    this.registerOtherTests()
    this.registerAuthorizationTests()
    this.registerPinTests()
    this.registerSearchTests()
    this.registerFiltersTests()
    return this.registry
  }

  private registerShortSwipeMenuTests(): void {
    this.registry
      .regular(new ShortSwipeMenuMarkAsReadMessageTest())
      .regular(new ShortSwipeMenuMarkAsUnreadMessageTest())
      .regular(new ShortSwipeMenuDeleteMessageTest())
      .regular(new ShortSwipeMenuDeleteThreadTest())
      .regular(new ShortSwipeMenuImportantMessageTest())
      .regular(new ShortSwipeMenuImportantThreadTest())
      .regular(new ShortSwipeMenuUnImportantThreadTest())
      .regular(new ShortSwipeMenuMoveToFolderMessageTest())
      .regular(new ShortSwipeMenuMoveToInboxFromTrashTest())
      .regular(new ShortSwipeMenuMarkAsReadThreadTest())
      .regular(new ShortSwipeMenuMarkAsUnreadThreadTest())
      .regular(new ShortSwipeMenuMarkAsSpamThreadTest())
      .regular(new ShortSwipeMenuMarkAsNotSpamMessageTest())
      .regular(new ShortSwipeMenuUnImportantMessageTest())
      .regular(new ShortSwipeMenuMoveToFolderThreadTest())
      .regular(new ShortSwipeMenuApplyLabelToThreadTest())
      .regular(new ShortSwipeMenuApplyLabelToMessageTest())
      .regular(new ShortSwipeMenuRemoveLabelFromMessageTest())
      .regular(new ShortSwipeMenuRemoveLabelFromThreadTest())
      .regular(new ShortSwipeMenuArchiveMessageFromSpamTest())
      .regular(new ShortSwipeMenuArchiveThreadFromUserFolderTest())
      .regular(new ShortSwipeMenuUndoDeleteThreadTest())
      .regular(new ShortSwipeMenuUndoArchiveMessageTest())
      .regular(new ShortSwipeMenuUndoDeleteMessageTest())
      .regular(new ShortSwipeMenuUndoArchiveThreadTest())
      .regular(new ShortSwipeMenuUndoSpamMessageTest())
      .regular(new ShortSwipeMenuUndoSpamThreadTest())
      .regular(new ShortSwipeMenuDeleteFromTrashTest())
      .regular(new ShortSwipeMenuMarkReadUnreadInCompactModeTest())
      .regular(new ShortSwipeMenuAddLabelInCompactModeTest())
      .regular(new ShortSwipeMenuMarkUnmarkAsSpamInCompactModeTest())
      .regular(new ShortSwipeMenuDeleteMessageFromTrashInCompactModeTest())
      .regular(new ShortSwipeMenuArchiveMessageInCompactModeTest())
      .regular(new SearchAndMarkMessageReadByShortSwipeMenuTest())
      .regular(new SearchAndDeleteMessageByShortSwipeMenuTest())
      .regular(new SearchAndMoveMessageToAnotherFolderByShortSwipeMenuTest())
      .regular(new MoveMessageFromInboxTabToMailingListTabByShortSwipeMenuTest())
      .regular(new MarkMessageImportantInMailingListTabByShortSwipeMenuTest())
      .regular(new MarkMessageUnimportantInSocialNetworksTabByShortSwipeMenuTest())
      .regular(new UnmarkLabelInInboxTabByShortSwipeMenuTest())
  }

  private registerStoriesTests(): void {
    this.registry
      .regular(new StoriesDifferentAccountTest())
      .regular(new StoriesHideTest())
      .regular(new StoriesRotationTest())
  }

  private registerGroupModeTests(): void {
    this.registry
      .regular(new CanOpenMessageAfterGroupActionTest())
      .regular(new GroupModeCancelSelectionTest())
      .regular(new GroupModeUnmarkImportantTest())
      .regular(new GroupModeMarkLabelTest())
      .regular(new GroupModeUnmarkLabelTest())
      .regular(new GroupModeMarkReadMessageTest())
      .regular(new GroupModeMarkUnreadMessageTest())
      .regular(new GroupModeArchiveMessagesTest())
      .regular(new GroupModeMarkImportantTest())
      .regular(new GroupModeMarkSpamTest())
      .regular(new GroupModeArchiveThreadTest())
      .regular(new GroupModeSelectAllTest())
      .regular(new GroupModeMarkReadThreadTest())
      .regular(new GroupModeMarkUnreadThreadTest())
      .regular(new GroupModeDeleteThreadTest())
      .regular(new GroupModeMarkSpamThreadTest())
      .regular(new GroupModeUnmarkSpamTest())
      .regular(new GroupModeMoveThreadsToInboxTest())
      .regular(new GroupModeMoveMessageToUserFolderFromInboxTest())
      .regular(new GroupModeInitialSelectTest())
      .regular(new GroupModeExitByTapOnSelectedMessagesTest())
      .regular(new GroupModeDeleteFromTrashTest())
      .regular(new GroupModeInitialSelectInCompactModeTest())
      .regular(new GroupModeUndoDeleteThreadTest())
      .regular(new GroupModeUndoSpamMessagesAndThreadsTest())
      .regular(new GroupModeUndoArchiveThreadTest())
      .regular(new GroupModeUndoSpamMessageTest())
      .regular(new GroupModeUndoArchiveMessageTest())
      .regular(new GroupModeUndoArchiveMessagesAndThreadsTest())
      .regular(new GroupModeUndoSpamThreadTest())
      .regular(new GroupModeUndoDeleteMessageTest())
      .regular(new GroupModeUndoDeleteMessagesAndThreadsTest())
      .regular(new GroupModeDeleteMessageInCompactModeTest())
      .regular(new GroupModeMarkAsSpamNotSpamMessageInCompactModeTest())
      .regular(new GroupModeArchiveMessageFromSearchInCompactModeTest())
      .regular(new GroupModeMarkImportantUnimportantMessageInCompactModeTest())
      .regular(new GroupModeAddRemoveLabelInCompactModeTest())
      .regular(new GroupModeMardMessagesAsRead2paneTest())
      .regular(new GroupModeDeleteMessage2paneTest())
      .regular(new GroupModeSelectAllMessagesInSearchTest())
      .regular(new GroupModeMoveMessageFromTabInboxToUserFolderTest())
      .regular(new GroupModeMoveMessageToSpamFromSocialTabTest())
      .regular(new GroupModeApplyLabelToMessageInMailingListTabTest())
      .regular(new GroupMarkAsReadDifferentMessagesTest())
      .regular(new GroupMarkAsReadMessagesTest())
      .regular(new GroupDeleteMessagesTest())
  }

  private registerShortSwipeTests(): void {
    this.registry
      .regular(new ShortSwipeToDeleteThreadTest())
      .regular(new ShortSwipeToDeleteMessageTest())
      .regular(new ShortSwipeToArchiveMessageTest())
      .regular(new ShortSwipeToArchiveThreadTest())
      .regular(new ShortSwipeToDeleteFromTrashTest())
      .regular(new ShortSwipeToArchiveFromArchiveThreadTest())
      .regular(new ShortSwipeUndoDeleteThreadTest())
      .regular(new ShortSwipeUndoArchiveMessageTest())
      .regular(new ShortSwipeUndoDeleteMessageTest())
      .regular(new ShortSwipeUndoArchiveThreadTest())
      .regular(new SwipeToReadMessageInCompactModeTest())
      .regular(new ShortSwipeToDeleteInCompactModeTest())
      .regular(new ShortSwipeToArchiveInCompactModeTest())
      .regular(new ShortSwipeDeleteMessageAtSearch2paneTest())
      .regular(new ShortSwipeArchiveMessageAtSearch2paneTest())
      .regular(new ShortSwipeMarkMessageAsReadAtSearch2paneTest())
      .regular(new ShortSwipeDeleteMessage2paneTest())
      .regular(new ShortSwipeArchiveMessage2paneTest())
      .regular(new ShortSwipeMarkMessageAsRead2paneTest())
      .regular(new ShortSwipeUndoDeleteMessage2paneTest())
      .regular(new ShortSwipeUndoArchiveMessage2paneTest())
      .regular(new SwipeToReadMessageTest())
      .regular(new SwipeToUnreadMessageTest())
      .regular(new SwipeToReadThreadTest())
      .regular(new SwipeToUnreadThreadTest())
      .regular(new ShortSwipeDeleteMessageAtSearchTest())
      .regular(new ShortSwipeDeleteThreadLandscapeTest())
      .regular(new ShortSwipeMarkMessageAsReadAtSearchTest())
      .regular(new SwipeToReadMessageFromInboxTest())
      .regular(new SwipeToDeleteMessageFromTrashTest())
  }

  private registerLongSwipeTests(): void {
    this.registry
      .regular(new LongSwipeToDeleteMessageTest())
      .regular(new LongSwipeToDeleteThreadTest())
      .regular(new LongSwipeToArchiveFromArchiveThreadTest())
      .regular(new LongSwipeToArchiveThreadTest())
      .regular(new LongSwipeToArchiveMessageTest())
      .regular(new LongSwipeToDeleteFromTrashTest())
      .regular(new LongSwipeUndoDeleteMessageTest())
      .regular(new LongSwipeUndoArchiveThreadTest())
      .regular(new LongSwipeUndoDeleteThreadTest())
      .regular(new LongSwipeUndoArchiveMessageTest())
      .regular(new LongSwipeToArchiveInCompactModeTest())
      .regular(new LongSwipeLongSwipeToDeleteFromSearchInCompactModeTest())
      .regular(new LongSwipeUndoDeleteMessageAtSearchTest())
      .regular(new LongSwipeDeleteMessageAtSearchTest())
      .regular(new LongSwipeArchiveMessageAtSearchTest())
      .regular(new LongSwipeDeleteMessageAtTabMailingListsTest())
      .regular(new LongSwipeDeleteThreadLandscapeTest())
      .regular(new LongSwipeDeleteThread2paneTest())
      .regular(new LongSwipeDeleteMessageAtSearch2paneTest())
      .regular(new LongSwipeToDeleteFromImportantTest())
      .regular(new LongSwipeToDeleteFromTrashUndoTest())
      .regular(new LongSwipeToDeleteDraftInCompactModeTest())
      .regular(new LongSwipeToArchiveMsgFromLabelInCompactModeTest())
      .regular(new LongSwipeToDeleteThreadInCompactModeTest())
      .regular(new LongSwipeToArchiveMessageInLandscapeTest())
      .regular(new LongSwipeToUndoDeleteMessageLandscapeTest())
      .regular(new LongSwipeToUndoArchiveMessageLandscapeTest())
      .regular(new LongSwipeToArchiveTemplateTest())
      .regular(new LongSwipeToArchiveThreadWithUndoTest())
  }

  private registerManageFoldersTests(): void {
    this.registry
      .regular(new ManageFoldersAddNewFolderInRootTest())
      .regular(new ManageFoldersDeleteFolderWithSubfoldersByLongSwipeTest())
      .regular(new ManageFoldersEditFolderTest())
      .regular(new ManageFoldersValidateViewTest())
      .regular(new ManageFoldersValidateEditFolderViewTest())
      .regular(new ManageFoldersValidateAddFolderViewTest())
      .regular(new ManageFoldersValidateFolderLocationViewTest())
      .regular(new ManageFoldersDeleteFolderTest())
  }

  private registerManageLabelsTests(): void {
    this.registry
      .regular(new ManageLabelsAddNewLabelTest())
      .regular(new ManageLabelsEditLabelTest())
      .regular(new ManageLabelsDeleteOpenedLabelByLongSwipeTest())
      .regular(new ManageLabelsValidateViewTest())
      .regular(new ManageLabelsValidateAddLabelViewTest())
      .regular(new ManageLabelsValidateEditLabelViewTest())
  }

  private registerSearchTests(): void {
    this.registry
      .regular(new SearchAndMoveToSpamMessage())
      .regular(new SearchAndDeleteMessageFromUserFolder())
      .regular(new SearchAndDeleteMessageShortSwipeFromTemplates())
      .regular(new SearchAndGroupDeleteMessageTestFromTemplates())
      .regular(new SearchAndGroupDeleteMessageTestFromDraft())
      .regular(new SearchAndMarkImportantMessageShortSwipe())
      .regular(new SearchAndMarkMessageRead())
      .regular(new SearchAndMarkMessageReadFromUserFolder())
      .regular(new SearchAndMarkMessageUnreadFromUserFolder())
      .regular(new SearchAndAddLabelMessageFromUserFolder())
      .regular(new SearchAndAddLabelMessageFromArchive())
      .regular(new SearchAndAddLabelMessage())
      .regular(new SearchAndMarkMessageUnreadBySwipeFromSent())
      .regular(new SearchAndMarkMessageUnreadFromSpam())
      .regular(new SearchAndDeleteMessageFromSpam())
      .regular(new MarkAsReadInSearch())
      .regular(new SearchMessageListViewInCompactModeTest())
      .regular(new SearchAndOpenMessage())
      .regular(new SearchAndOpenMessageIn2Pane())
      .regular(new RotateDeviceInZeroSuggestTest())
      .regular(new SaveQueryToZeroSuggestTest())
      .regular(new SearchMessagesViaZeroSuggestIn2PaneTest())
      .regular(new SearchMessagesViaZeroSuggestTest())
      .regular(new SearchUnreadMessagesViaZeroSuggestTest())
  }

  private registerComposeTests(): void {
    this.registry
      .regular(new ComposeForwardViaShortSwipeMenuTest())
      .regular(new ComposeReplyViaShortSwipeMenuTest())
      .regular(new ComposeSelectSenderFromSuggestTest())
      .regular(new ComposeEmptyRecipientsSuggestTest())
      .regular(new ComposeEnterAndDeleteTextLandscapeTest())
      .regular(new ComposeCloseSenderSuggestTest())
      .regular(new ComposeMinimizeRecipientsSuggestAfterSomeActionsTest())
      .regular(new ComposeSuggestOfManyContactTest())
      .regular(new ComposeCreateYabbleWithLongEmailTest())
      .regular(new ComposeReplyFromMailViewTest())
      .regular(new ComposeForwardFromMailViewTest())
      .regular(new ComposeSuggestOfSomeContactTest())
      .regular(new ComposeLongEmailInSuggestTest())
      .regular(new ComposeCreateYabbleWithNumericEmailTest())
      .regular(new ComposeDeleteYabbleByTapOnCrossTest())
      .regular(new ComposeSendMessageWithLongSubjectTest())
      .regular(new ComposeSendMessageWithNotGeneratedValidYabbleTest())
      .regular(new ComposeSendMessageToRecipientWithLatinAndCyrillicLettersInEmailTest())
      .regular(new ComposeSendEmptyMessageTest())
      .regular(new ComposeSendMessageToRecipientInCCFieldTest())
      .regular(new ComposeSendMessageWithAllFilledFieldsTest())
      .regular(new ComposeSuggestMissingAddedRecipientsTest())
      .regular(new ComposeSuggestDomainTest())
      .regular(new ComposeSuggestRotateTest())
      .regular(new ComposeSuggestBehaviorWhileEnterEmailTest())
  }

  private registerOtherTests(): void {
    this.registry
      .regular(new FormatTextTest())
      .regular(new MoveToSpamFirstMessageTest())
      .regular(new MoveToFolderTest())
      .regular(new MarkAllThreadMessagesReadByMarkingMainMessageTest())
      .regular(new MarkUnreadAfterReadTest())
      .regular(new ReadMessageAfterOpeningTest())
      .regular(new ArchiveFirstMessageTest())
      .regular(new LabelAllThreadMessagesImportantByLabellingMainMessageTest())
      .regular(new MarkAsImportantTest())
  }

  private registerAuthorizationTests(): void {
    this.registry.regular(new ChoseAccountFromAccountsListTest()).regular(new SwitchAccountTest())
  }

  private registerPinTests(): void {
    this.registry
      .regular(new ChangePinTest())
      .regular(new ResetPinTest())
      .regular(new TurnOffPinTest())
      .regular(new TurnOnPinTest())
  }

  private registerFolderListTests(): void {
    this.registry
      .regular(new ChangeFoldersInboxCustomTest())
      .regular(new ValidateFolderListTest())
      .regular(new ValidateLabelListTest())
      .regular(new NewFolderFromBackTestAndroid(), false)
      .regular(new NewFolderFromBackTestIos(), false)
      .regular(new NewLabelFromBackTestAndroid(), false)
      .regular(new NewLabelFromBackTestIos(), false)
      .regular(new ChangeFoldersArchiveSpamTrashTest())
      .regular(new ChangeFoldersSentDraftTest())
      .regular(new LongFolderNameViewTest())
      .regular(new LongLabelNameViewTest())
  }

  private registerMailViewTests(): void {
    this.registry
      .regular(new AddLabelsFromMessageView())
      .regular(new ArchiveMessageFromThreadFromMessageView())
      .regular(new ArchiveSingleMessageFromMessageView())
      .regular(new MarkAsNotSpamFromMessageViewAndroid())
      .regular(new DeleteLabelsFromMessageView())
      .regular(new DeleteMessageByTapOnIconTest())
      .regular(new DeleteMessageByTapOnTopBarTest())
      .regular(new DeleteMessageFromMessageViewThreadModeOff())
      .regular(new DeleteMessageInSearchByTapOnTopBarTest())
      .regular(new DeleteSingleMessageFromMessageViewTest())
      .regular(new DeleteSingleMessageFromThreadFromMessageViewTest())
      .regular(new LablesViewFromMessageView())
      .regular(new MarkAsNotSpamFromMessageView())
      .regular(new MarkAsReadByExpandThreadTest())
      .regular(new MarkAsReadByOpeningMessageViewTest())
      .regular(new MarkAsReadFromMessageViewTest())
      .regular(new MarkAsSpamFromMessageView())
      .regular(new MarkAsUnreadFromMessageViewTest())
      .regular(new MarkImportantFromMessageView())
      .regular(new MarkUnimportantFromMessageView())
      .regular(new MoveMessageFromThreadToTrashFromMailViewTest())
      .regular(new MoveMessageOfThreadToArchiveFromMailViewTest())
      .regular(new MoveMessageToSpamFromMailViewFromSearchTest())
      .regular(new MoveMessageToSpamFromMailViewTest())
      .regular(new MoveMessageToTabFromMailViewTest())
      .regular(new MoveMessageToTrashFromMailViewFromSearchTest())
      .regular(new MoveMessageToTrashFromMailViewTest())
      .regular(new MoveMessageToUsersFolderFromMessageView())
      .regular(new MoveMessageToUsersFolderFromThreadFromMessageView())
      .regular(new MarkAsSpamMessageFromThreadFromMessageView())
      .regular(new UndoMessageDeleteFromMessageView())
      .regular(new MarkImportantMessageFromThreadFromMessageView())
      .regular(new AddAndDeleteLabelsFromMessageView())
      .regular(new DeleteMessageByTapOnIcon2PaneTest())
      .regular(new ViewOperationsInInboxFolderWithLabeledMsg())
      .regular(new ViewOperationsInInboxFolder())
      .regular(new ViewOperationsInSentFolder())
      .regular(new ViewOperationsInArchiveFolderWithLabeledMsg())
      .regular(new ViewOperationsInUserFolder())
      .regular(new MarkLabelInMessageView())
      .regular(new UndoMessageDeleteFromMessage2paneView())
      .regular(new MessageViewCreateAndMarkLabelTest())
      .regular(new MailViewArchiveMessageByTapOnTopBarTest())
      .regular(new MailViewArchiveThreadByTapOnTopBarTest())
      .regular(new MailViewDeleteThreadByTapOnTopBarTest())
      .regular(new MailViewMarkAsSpamOneMessageInThreadLandscapeTest())
      .regular(new MailView2paneUndoMessageDeleteInSearchTest())
      .regular(new MailView2paneUndoDeleteOneMessageInThreadTest())
      .regular(new MailView2paneUndoArchiveOneMessageInThreadTest())
      .regular(new MailView2paneUndoSpamMessageTest())
      .regular(new MailView2paneUndoSpamMessageInSearchTest())
      .regular(new MailViewMarkAsSpamMessageFromThread2paneTest())
  }

  private registerMessageListViewTests(): void {
    this.registry
      .regular(new GoToTabByTabsNotification())
      .regular(new MailListViewInCompactMode())
      .regular(new MarkAsImportantMessageInTabSubscriptionTest())
      .regular(new TabNotificationInEmptyMessageList())
      .regular(new TabNotificationInTheFirstInMailList())
      .regular(new TabNotificationInTheMiddleMailList())
      .regular(new ThreadModeTurnOffAndThenOnTest())
      .regular(new TabNotificationInCompactMode())
      .regular(new TabsNotificationOnlyInInbox())
      .regular(new TabsNotificationInLandscape())
      .regular(new DisplayTabsNotificationAfterMoveMessage())
      .regular(new GroupModeWithTabNotification())
      .regular(new MailListViewInThreadFolder())
      .regular(new MailListViewInThreadLessFolder())
      .regular(new MailListViewInThreadFolderLandscape())
      .regular(new MailListViewInThreadLessFolderLandscape())
      .regular(new MailListViewInUnreadLabel())
      .regular(new MailListViewInUnreadLabelLandscape())
      .regular(new MailListViewInImportantLabel())
      .regular(new MailListViewInUserLabel())
      .regular(new MailListViewInThreadFolderTab())
      .regular(new MailListViewInThreadLessFolderTab())
      .regular(new MailListViewInImportantLabelTab())
      .regular(new MailListViewNavigateByArrowsTab())
      .regular(new ChangeFilterTest())
      .regular(new LabelViewLoadingTest())
      .regular(new InboxTopBarDisplayTest(AccountType2.Yandex))
      .regular(new InboxTopBarDisplayTest(AccountType2.YandexTeam))
  }

  private registerTranslatorTests(): void {
    this.registry
      .regular(new AbsentOfTranslatorBarIfSourceLanguageIsEqualToTargetLanguageTests())
      .regular(new AddLanguageToIgnoredLanguagesListAndDeleteTests())
      .regular(new AddLanguageToRecentLanguagesListTests())
      .regular(new ChangeDefaultTranslateLanguageTest())
      .regular(new HideTranslatorBarForAutoLanguageTests())
      .regular(new HideTranslatorBarTest())
      .regular(new ResetTranslateAfterReopenMessageTests())
      .regular(new RevertToOriginalMessageLanguageTests())
      .regular(new TranslateMessage2paneTests())
  }

  private registerSettingTests(): void {
    this.registry
      .regular(new ChangingActionOnSwipeTest())
      .regular(new ClearCacheTest())
      .regular(new OpenAboutSettingsTest())
      .regular(new OpenAccountSettingsTest())
      .regular(new TurningOnCompactModeTest())
      .regular(new TurningOffCompactModeTest())
      .regular(new ValidateAccountSettingsTest())
      .regular(new ValidateGeneralSettingsTest())
      .regular(new ValidateRootSettingsTest())
      .regular(new DefaultActionOnSwipeTest())
      .regular(new ViewAccountSettingsInLandscapeTest())
      .regular(new ViewSettingsTestTab())
      .regular(new ViewSettingsInLandscapeTest())
      .regular(new UndoClearCacheTest())
  }

  private registerClearFolderTests(): void {
    this.registry
      .regular(new ClearSpamTest())
      .regular(new CancelClearSpamTest())
      .regular(new ClearTrashTest())
      .regular(new CancelClearTrashTest())
  }

  private registerQuickReplyTests(): void {
    this.registry
      .regular(new QuickReplyToMessageTests())
      .regular(new QuickReplyOpenFilledComposeTests())
      .regular(new QuickReplyRotateTests())
      .regular(new QuickReplyIsTextFieldExpandedTests())
      .regular(new SmartReplyMissingIfSettingsDisabledTest())
      .regular(new SmartReplyShowIfSettingsEnabledTest())
  }

  private registerTabbarTests(): void {
    this.registry
      .regular(new TabBarOpenCalendarTest())
      .regular(new TabBarOpenTelemostTest())
      .regular(new TabBarOpenDocumentsTest())
      .regular(new TabBarOpenMoreTabTest())
      .regular(new TabBarItemsYandexLandscapeTest())
      .regular(new TabBarCalendarDateLabelTest())
      .regular(new TabBarShownInMailListTest())
      .regular(new TabBarNotShownInMailViewTest())
      .regular(new TabBarNotShownInSearchTest())
      .regular(new TabBarNotShownInComposeTest())
      .regular(new TabBarNotShownInFolderListTest())
      .regular(new TabBarNotShownInSettingsTest())
      .regular(new TabBarHideInGroupModeTest())
      .regular(new ShtorkaCloseByTapOverTest())
      .regular(new ShtorkaCloseBySwipeTest())
      .regular(new ShtorkaOpenNotesTest())
      .regular(new ShtorkaOpenDiskTest())
  }

  private registerTabsTests(): void {
    this.registry
      .regular(new MoveThreadFromMailingListToSocialNetworksTest())
      .regular(new MoveThreadFromSocialNetworksTabToUserFolderTest())
      .regular(new MoveThreadFromUserFolderToInboxTest())
      .regular(new UndoMoveMessageToArchiveFromInboxTabTest())
      .regular(new UndoMoveMessageToSpamTest())
      .regular(new DeleteThreadInTabTest())
      .regular(new DeleteMessageByTapOnTopBarInTabTest())
      .regular(new TurnOffTabsTest())
      .regular(new TurnOnTabsTest())
      .regular(new UndoDeleteMessageFromTabTest())
  }

  private registerFiltersTests(): void {
    this.registry.regular(new ValidateFiltersListPt1Test()).regular(new ValidateFiltersListPt2Test())
  }

  public static readonly get: TestsRegistry<MailboxBuilder> = new AllMailTests().registerAllTestsAndGetThem()
}
