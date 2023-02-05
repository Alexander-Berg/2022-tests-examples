import { App, MBTComponent, MBTComponentType } from '../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { assertInt32Equals, assertTrue } from '../../../../testopithecus-common/code/utils/assert'
import { Throwing } from '../../../../../common/ys'
import { ContextMenuFeature } from '../feature/message-list/context-menu-feature'
import { MessageActionItem } from '../model/messages-list/context-menu-model'

export class ContextMenuComponent implements MBTComponent {
  public static readonly type: MBTComponentType = 'ContextMenuComponent'

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    const modelContextMenu = ContextMenuFeature.get.castIfSupported(model)
    const appContextMenu = ContextMenuFeature.get.castIfSupported(application)

    if (modelContextMenu === null || appContextMenu === null) {
      return
    }

    const modelAvailableActions = modelContextMenu.getAvailableActions()
    const appAvailableActions = appContextMenu.getAvailableActions()

    // Android. Reply all and Spam always shown
    if (
      appAvailableActions.includes(MessageActionItem.replyAll) &&
      !modelAvailableActions.includes(MessageActionItem.replyAll)
    ) {
      appAvailableActions.splice(appAvailableActions.lastIndexOf(MessageActionItem.replyAll), 1)
    }
    if (
      appAvailableActions.includes(MessageActionItem.spam) &&
      !modelAvailableActions.includes(MessageActionItem.spam)
    ) {
      appAvailableActions.splice(appAvailableActions.lastIndexOf(MessageActionItem.spam), 1)
    }

    // iOS. "Print" is shown only in experiment
    if (
      !appAvailableActions.includes(MessageActionItem.print) &&
      modelAvailableActions.includes(MessageActionItem.print)
    ) {
      modelAvailableActions.splice(modelAvailableActions.lastIndexOf(MessageActionItem.print), 1)
    }

    assertInt32Equals(
      modelAvailableActions.length,
      appAvailableActions.length,
      `Incorrect number of operations, model=${modelAvailableActions}, app=${appAvailableActions}`,
    )

    for (const modelAction of modelAvailableActions) {
      assertTrue(appAvailableActions.includes(modelAction), `There is no action ${modelAction}`)
    }
  }

  public tostring(): string {
    return this.getComponentType()
  }

  public getComponentType(): MBTComponentType {
    return ContextMenuComponent.type
  }
}
