import { assertBooleanEquals, assertInt32Equals } from '../../../../testopithecus-common/code/utils/assert'
import { Int32, Throwing } from '../../../../../common/ys'
import {
  App,
  MBTAction,
  MBTComponent,
  MBTComponentType,
} from '../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { MBTComponentActions } from '../../../../testopithecus-common/code/mbt/walk/behaviour/user-behaviour'
import { keysArray } from '../../../../testopithecus-common/code/utils/utils'
import { resolveThrow } from '../../utils/mail-utils'
import { RotatableAction } from '../actions/general/rotatable-actions'
import {
  GroupModeDeleteAction,
  GroupModeMarkAsReadAction,
  GroupModeMarkAsUnreadAction,
  GroupModeMarkImportantAction,
  GroupModeMarkNotSpamAction,
  GroupModeMarkSpamAction,
  GroupModeMarkUnimportantAction,
  GroupModeMoveToFolderAction,
  GroupModeUnselectAllAction,
  GroupModeUnselectMessageAction,
} from '../actions/messages-list/group-mode-actions'
import { FolderName, FolderNavigatorFeature } from '../feature/folder-list-features'
import { GroupModeFeature } from '../feature/message-list/group-mode-feature'
import { TabBarComponent } from './tab-bar-component'

export class GroupOperationsComponent implements MBTComponent {
  public static readonly type: MBTComponentType = 'GroupOperationsComponent'

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    const groupModeModel = GroupModeFeature.get.castIfSupported(model)
    const groupModeApplication = GroupModeFeature.get.castIfSupported(application)

    if (groupModeModel !== null && groupModeApplication !== null) {
      const selectedMessagesOrders = groupModeModel.getSelectedMessages().values()
      for (const selectedMessageOrder of selectedMessagesOrders) {
        assertBooleanEquals(
          true,
          groupModeApplication.getSelectedMessages().has(selectedMessageOrder),
          `Message with order ${selectedMessageOrder} is not selected`,
        )
      }

      assertBooleanEquals(
        groupModeModel.isInGroupMode(),
        groupModeApplication.isInGroupMode(),
        'Group mode status is incorrect',
      )

      assertInt32Equals(
        groupModeModel.getNumberOfSelectedMessages(),
        groupModeApplication.getNumberOfSelectedMessages(),
        'Number of selected messages is incorrect',
      )
    }

    await new TabBarComponent().assertMatches(model, application)
  }

  public tostring(): string {
    return this.getComponentType()
  }

  public getComponentType(): MBTComponentType {
    return GroupOperationsComponent.type
  }
}

export class AllGroupOperationsActions implements MBTComponentActions {
  public getActions(_model: App): MBTAction[] {
    const actions: MBTAction[] = []
    actions.push(new GroupModeMarkAsReadAction())
    actions.push(new GroupModeMarkAsUnreadAction())
    RotatableAction.addActions(actions)
    return actions
  }
}

export class NotImplementedInClientsActions implements MBTComponentActions {
  public getActions(model: App): MBTAction[] {
    const actions: MBTAction[] = []
    actions.push(new GroupModeDeleteAction())
    actions.push(new GroupModeMarkImportantAction())
    actions.push(new GroupModeMarkUnimportantAction())
    actions.push(new GroupModeMarkSpamAction())
    actions.push(new GroupModeMarkNotSpamAction())
    FolderNavigatorFeature.get.performIfSupported(model, (mailboxModel) => {
      let folders: FolderName[]
      try {
        folders = keysArray(mailboxModel.getFoldersList())
      } catch (e) {
        folders = []
      }
      for (const folder of folders) {
        actions.push(new GroupModeMoveToFolderAction(folder))
      }
    })
    const groupMode = GroupModeFeature.get.castIfSupported(model)
    if (groupMode !== null) {
      const selectedMessages: Set<Int32> = resolveThrow(() => groupMode.getSelectedMessages(), new Set<Int32>())
      if (selectedMessages.size > 0) {
        for (const i of selectedMessages.values()) {
          actions.push(new GroupModeUnselectMessageAction(i))
        }
      }
    }
    actions.push(new GroupModeUnselectAllAction())
    return actions
  }
}
