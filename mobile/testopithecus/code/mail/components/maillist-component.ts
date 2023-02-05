import { Int32, range, Throwing } from '../../../../../common/ys'
import { maxInt32, minInt32 } from '../../../../common/code/utils/math'
import { Log } from '../../../../common/code/logging/logger'
import {
  App,
  MBTAction,
  MBTComponent,
  MBTComponentType,
} from '../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { MBTComponentActions } from '../../../../testopithecus-common/code/mbt/walk/behaviour/user-behaviour'
import { assertBooleanEquals, assertInt32Equals, assertTrue } from '../../../../testopithecus-common/code/utils/assert'
import { resolveThrow } from '../../utils/mail-utils'
import { ArchiveMessageAction } from '../actions/base-actions/archive-message-action'
import { DeleteMessageAction } from '../actions/base-actions/delete-message-action'
import { MarkAsImportant, MarkAsUnimportant } from '../actions/base-actions/labeled-actions'
import { MarkAsRead, MarkAsUnread } from '../actions/base-actions/markable-actions'
import { MoveToSpamAction } from '../actions/base-actions/spamable-actions'
import { RotatableAction } from '../actions/general/rotatable-actions'
import { GoToFolderAction } from '../actions/left-column/folder-navigator-actions'
import {
  ShortSwipeContextMenuMarkAsImportantAction,
  ShortSwipeContextMenuMarkAsReadAction,
  ShortSwipeContextMenuMarkAsUnimportantAction,
  ShortSwipeContextMenuMarkAsUnreadAction,
} from '../actions/messages-list/context-menu-actions'
import { GroupModeSelectAction } from '../actions/messages-list/group-mode-actions'
import {
  ArchiveMessageByShortSwipeAction,
  DeleteMessageByShortSwipeAction,
} from '../actions/messages-list/short-swipe-actions'
import {
  CollapseThreadAction,
  ExpandThreadAction,
  MarkAsReadExpandedAction,
  MarkAsUnreadExpandedAction,
} from '../actions/messages-list/thread-markable-actions'
import { OpenMessageAction } from '../actions/opened-message/message-actions'
import { ClearCacheAction } from '../actions/settings/general-settings-actions'
import { HideStoriesBlockAction, OpenStoryFromBlockAction } from '../actions/stories/stories-block-actions'
import { FolderNavigatorFeature } from '../feature/folder-list-features'
import { MessageView } from '../feature/mail-view-features'
import { ExpandableThreadsModelFeature } from '../feature/message-list/expandable-threads-feature'
import { GroupModeFeature } from '../feature/message-list/group-mode-feature'
import { MessageListDisplayFeature } from '../feature/message-list/message-list-display-feature'
import { StoriesBlock, StoriesBlockFeature } from '../feature/message-list/stories-block-feature'
import { UndoFeature, UndoState } from '../feature/message-list/undo-feature'
import { GeneralSettingsFeature } from '../feature/settings/general-settings-feature'
import { TabsFeature } from '../feature/tabs-feature'
import { DefaultFolderName } from '../model/folder-data-model'
import { Message } from '../model/mail-model'
import { TabBarComponent } from './tab-bar-component'

export class MaillistComponent implements MBTComponent {
  public static readonly type: MBTComponentType = 'MaillistComponent'

  public constructor() {}

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    Log.info('Model and application comparison started')

    const undoModel = UndoFeature.get.castIfSupported(model)
    const undoApplication = UndoFeature.get.castIfSupported(application)

    if (undoModel !== null && undoApplication !== null) {
      const isUndoArchiveToastShownApplication = undoApplication.isUndoArchiveToastShown() === UndoState.shown
      if (isUndoArchiveToastShownApplication) {
        assertBooleanEquals(
          undoModel.isUndoArchiveToastShown() !== UndoState.notShown,
          isUndoArchiveToastShownApplication,
          'Undo archive toast state is unexpected',
        )
      } else {
        assertBooleanEquals(
          undoModel.isUndoArchiveToastShown() === UndoState.shown,
          isUndoArchiveToastShownApplication,
          'Undo archive toast state is unexpected',
        )
      }

      const isUndoDeleteToastShownApplication = undoApplication.isUndoDeleteToastShown() === UndoState.shown
      if (isUndoDeleteToastShownApplication) {
        assertBooleanEquals(
          undoModel.isUndoDeleteToastShown() !== UndoState.notShown,
          isUndoDeleteToastShownApplication,
          'Undo delete toast state is unexpected',
        )
      } else {
        assertBooleanEquals(
          undoModel.isUndoDeleteToastShown() === UndoState.shown,
          isUndoDeleteToastShownApplication,
          'Undo delete toast state is unexpected',
        )
      }

      const isUndoSpamToastShownApplication = undoApplication.isUndoSpamToastShown() === UndoState.shown
      if (isUndoSpamToastShownApplication) {
        assertBooleanEquals(
          undoModel.isUndoSpamToastShown() !== UndoState.notShown,
          isUndoSpamToastShownApplication,
          'Undo spam toast state is unexpected',
        )
      } else {
        assertBooleanEquals(
          undoModel.isUndoSpamToastShown() === UndoState.shown,
          isUndoSpamToastShownApplication,
          'Undo spam toast state is unexpected',
        )
      }
    }
    const messageListModel = MessageListDisplayFeature.get.castIfSupported(model)
    const messageListApplication = MessageListDisplayFeature.get.castIfSupported(application)
    if (messageListModel !== null && messageListApplication !== null) {
      let comparedMessages = 0
      const actualMessages = messageListApplication.getMessageList(10)
      const expectedMessages = messageListModel.getMessageList(10)
      const isCompactModeModel = GeneralSettingsFeature.get.forceCast(model).isCompactModeEnabled()

      for (const i of range(0, minInt32(maxInt32(actualMessages.length, 1), expectedMessages.length))) {
        assertTrue(i < actualMessages.length, `There is expected to be message at position ${i} but there was not`)
        const actual = actualMessages[i]
        const expected = expectedMessages[i]
        Log.info(`№${i}: expected=${expected.tostring()} actual=${actual.tostring()}`)

        assertTrue(Message.matches(expected, actual, isCompactModeModel), `Messages are different at position ${i}`)
        comparedMessages += 1
      }
      Log.info(`Message view is ok, compared: ${comparedMessages}`)
      // const actualUnreadCounter = messageListApplication.unreadCounter();
      // const expectedUnreadCounter = messageListModel.unreadCounter();
      // assertInt32Equals(expectedUnreadCounter, actualUnreadCounter, `Number of unread messages are different`)

      let comparedThreads = 0
      const expandableThreadsModel = ExpandableThreadsModelFeature.get.castIfSupported(model)
      const expandableThreadsApplication = ExpandableThreadsModelFeature.get.castIfSupported(application)
      if (expandableThreadsModel !== null && expandableThreadsApplication !== null) {
        const expectedMessagesCount = messageListModel.getMessageList(10).length
        for (const threadOrder of range(0, expectedMessagesCount)) {
          if (expandableThreadsModel.isExpanded(threadOrder)) {
            comparedThreads += 1
            const modelMessagesInThread = expandableThreadsModel.getMessagesInThread(threadOrder)
            const appMessagesInThread = expandableThreadsApplication.getMessagesInThread(threadOrder)
            for (const messageInThreadOrder of range(0, modelMessagesInThread.length)) {
              const expected = modelMessagesInThread[messageInThreadOrder]
              const actual = appMessagesInThread[messageInThreadOrder]
              assertBooleanEquals(
                expected.read,
                actual.read,
                `Messages are different at thread position ${threadOrder}, message position ${messageInThreadOrder}`,
              )
            }
          }
        }
      }

      const folderNavigatorModel = FolderNavigatorFeature.get.castIfSupported(model)
      const folderNavigatorApplication = FolderNavigatorFeature.get.castIfSupported(application)
      const tabsModel = TabsFeature.get.castIfSupported(model)
      const tabsApplication = TabsFeature.get.castIfSupported(application)
      if (
        folderNavigatorModel !== null &&
        folderNavigatorApplication !== null &&
        tabsModel !== null &&
        tabsApplication !== null
      ) {
        if (folderNavigatorModel.isInTabsMode()) {
          const isDisplayNotificationMailingListsTabsModel = tabsModel.isDisplayNotificationTabs(
            DefaultFolderName.mailingLists,
          )
          const isDisplayNotificationSocialNetworksTabsModel = tabsModel.isDisplayNotificationTabs(
            DefaultFolderName.socialNetworks,
          )
          assertBooleanEquals(
            isDisplayNotificationMailingListsTabsModel,
            tabsApplication.isDisplayNotificationTabs(DefaultFolderName.mailingLists),
            `Different status notification of ${DefaultFolderName.mailingLists} tab`,
          )
          assertBooleanEquals(
            isDisplayNotificationSocialNetworksTabsModel,
            tabsApplication.isDisplayNotificationTabs(DefaultFolderName.socialNetworks),
            `Different status notification of ${DefaultFolderName.socialNetworks} tab`,
          )
          if (isDisplayNotificationMailingListsTabsModel) {
            assertInt32Equals(
              tabsModel.getPositionTabsNotification(DefaultFolderName.mailingLists),
              tabsApplication.getPositionTabsNotification(DefaultFolderName.mailingLists),
              `Different position notification of ${DefaultFolderName.mailingLists} tab in Message list`,
            )

            assertBooleanEquals(
              tabsModel.isUnreadNotificationTabs(DefaultFolderName.mailingLists),
              tabsApplication.isUnreadNotificationTabs(DefaultFolderName.mailingLists),
              `Different notification's status read/unread of ${DefaultFolderName.mailingLists} tab in Message list`,
            )
          }
          if (isDisplayNotificationSocialNetworksTabsModel) {
            assertInt32Equals(
              tabsModel.getPositionTabsNotification(DefaultFolderName.socialNetworks),
              tabsApplication.getPositionTabsNotification(DefaultFolderName.socialNetworks),
              `Different position notification of ${DefaultFolderName.socialNetworks} tab in Message list`,
            )

            assertBooleanEquals(
              tabsModel.isUnreadNotificationTabs(DefaultFolderName.socialNetworks),
              tabsApplication.isUnreadNotificationTabs(DefaultFolderName.socialNetworks),
              `Different notification's status read/unread of ${DefaultFolderName.socialNetworks} tab in Message list`,
            )
          }
        }
      }

      Log.info(`Model and app are equal, compared ${comparedMessages} messages and ${comparedThreads} expanded threads`)
    }

    const groupModeModel = GroupModeFeature.get.castIfSupported(model)
    const groupModeApplication = GroupModeFeature.get.castIfSupported(application)
    if (groupModeModel !== null && groupModeApplication !== null) {
      const modelGroupMode = groupModeModel.isInGroupMode()
      const applicationGroupMode = groupModeApplication.isInGroupMode()

      assertBooleanEquals(modelGroupMode, applicationGroupMode, 'Group mode state is incorrect')
      Log.info(`Group mode state is correct, state: ${modelGroupMode}`)
    }

    // const storiesBlockModel = StoriesBlockFeature.get.castIfSupported(model)
    // const storiesBlockApplication = StoriesBlockFeature.get.castIfSupported(application)
    // if (storiesBlockModel !== null && storiesBlockApplication !== null) {
    //   assertBooleanEquals(storiesBlockModel.isHidden(), storiesBlockApplication.isHidden(), 'Stories Block Display is not equal')
    // }
    await new TabBarComponent().assertMatches(model, application)
  }

  public getComponentType(): string {
    return MaillistComponent.type
  }

  public tostring(): string {
    return 'MailListComponent'
  }
}

export class AllMaillistActions implements MBTComponentActions {
  public getActions(model: App): MBTAction[] {
    const actions: MBTAction[] = []

    MessageListDisplayFeature.get.performIfSupported(model, (mailboxModel) => {
      const messages: MessageView[] = resolveThrow(() => mailboxModel.getMessageList(3), [])
      for (const i of range(0, messages.length)) {
        actions.push(new MarkAsRead(i))
        actions.push(new MarkAsUnread(i))
        actions.push(new OpenMessageAction(i))
        actions.push(new ExpandThreadAction(i))
        actions.push(new CollapseThreadAction(i))
        actions.push(new MarkAsReadExpandedAction(i, 0))
        actions.push(new MarkAsUnreadExpandedAction(i, 0))
        actions.push(new MarkAsImportant(i))
        actions.push(new MarkAsUnimportant(i))
        actions.push(new MoveToSpamAction(i))
        actions.push(new GroupModeSelectAction(i))
        actions.push(new ArchiveMessageAction(i))
        actions.push(new DeleteMessageByShortSwipeAction(i))
        actions.push(new ArchiveMessageByShortSwipeAction(i))
        actions.push(new ShortSwipeContextMenuMarkAsReadAction(i))
        actions.push(new ShortSwipeContextMenuMarkAsUnreadAction(i))
        actions.push(new ShortSwipeContextMenuMarkAsImportantAction(i))
        actions.push(new ShortSwipeContextMenuMarkAsUnimportantAction(i))
        actions.push(new DeleteMessageAction(i))
      }
    })
    StoriesBlockFeature.get.performIfSupported(model, (t: StoriesBlock) => {
      actions.push(new HideStoriesBlockAction())
      const six: Int32 = 6
      for (const i of range(0, six)) {
        actions.push(new OpenStoryFromBlockAction(i))
      }
    })
    FolderNavigatorFeature.get.performIfSupported(model, (_mailboxModel) => {
      // TODO mailboxModel.getFoldersList()
      const folders = [DefaultFolderName.inbox, DefaultFolderName.sent, DefaultFolderName.trash, DefaultFolderName.spam]
      for (const folder of folders) {
        actions.push(new GoToFolderAction(folder))
      }
    })
    actions.push(new ClearCacheAction())
    RotatableAction.addActions(actions)

    return actions
  }
}
