import { Throwing } from '../../../../../../common/ys'
import { AccountsManager } from '../../../../../testopithecus-common/code/users/accounts-manager'
import { MessageContainer, MessageContainerType } from '../../feature/message-list/container-getter-feature'
import { AndroidRootSettings, IOSRootSettings, RootSettings } from '../../feature/settings/root-settings-feature'
import { DefaultFolderName } from '../folder-data-model'
import { ScreenTitle } from '../mail-model'
import { MessageListDisplayModel } from '../messages-list/message-list-display-model'

export class RootSettingsModel implements RootSettings, AndroidRootSettings, IOSRootSettings {
  public constructor(
    private readonly messageListDisplay: MessageListDisplayModel,
    private readonly accountsManager: AccountsManager,
  ) {}

  private settingsOpened: boolean = false

  public isOpened(): boolean {
    return this.settingsOpened
  }

  public openRootSettings(): Throwing<void> {
    this.settingsOpened = true
  }

  public closeRootSettings(): Throwing<void> {
    this.messageListDisplay.setCurrentContainer(
      new MessageContainer(DefaultFolderName.inbox, MessageContainerType.folder),
    )
    this.settingsOpened = false
  }

  public getAccounts(): Throwing<string[]> {
    return this.accountsManager.getLoggedInAccounts().map((account) => account.login)
  }

  public getTitle(): Throwing<string> {
    return ScreenTitle.rootSettings
  }

  public isAboutCellExists(): Throwing<boolean> {
    return true
  }

  public isGeneralSettingsCellExists(): Throwing<boolean> {
    return true
  }

  public isHelpAndFeedbackCellExists(): Throwing<boolean> {
    return true
  }

  public addAccount(): Throwing<void> {}

  public isAddAccountCellExists(): Throwing<boolean> {
    return true
  }
}
