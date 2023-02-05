import { SignaturePlace } from '../../../../../mapi/code/api/entities/settings/settings-entities'
import { Int32, Nullable, range, Throwing } from '../../../../../../common/ys'
import { AccountsManager } from '../../../../../testopithecus-common/code/users/accounts-manager'
import { requireNonNull } from '../../../../../testopithecus-common/code/utils/utils'
import { FolderName } from '../../feature/folder-list-features'
import {
  AccountSettings,
  AndroidAccountSettings,
  IosAccountSettings,
  NotificationOption,
  NotificationSound,
} from '../../feature/settings/account-settings-feature'
import { DefaultFolderName } from '../folder-data-model'
import { AccountSettingsModel, MailAppModelHandler } from '../mail-model'
import { MessageListDatabaseFilter } from '../supplementary/message-list-database'

export class AccountSettingModel implements AccountSettings, AndroidAccountSettings, IosAccountSettings {
  public constructor(
    public readonly accountsSettings: AccountSettingsModel[],
    public readonly accountsManager: AccountsManager,
    private accHandler: MailAppModelHandler,
    private openedAccount: Nullable<Int32> = null,
  ) {}

  private syncCalendar: boolean = false
  private filterScreenOpenCounter: Int32 = 0

  public openAccountSettings(accountIndex: Int32): Throwing<void> {
    this.openedAccount = this.accountsManager.indexesOfLoggedInAccounts[accountIndex]
  }

  public closeAccountSettings(): Throwing<void> {
    this.openedAccount = null
  }

  private getOpenedAccountSettings(): AccountSettingsModel {
    return this.accountsSettings[this.demandRequiredAccountIndex()]
  }

  public isGroupBySubjectEnabled(): Throwing<boolean> {
    return this.getOpenedAccountSettings().groupBySubjectEnabled
  }

  public getFolderToNotificationOption(): Throwing<Map<FolderName, NotificationOption>> {
    return this.getOpenedAccountSettings().folderToNotificationOption
  }

  public switchGroupBySubject(): Throwing<void> {
    this.getOpenedAccountSettings().groupBySubjectEnabled = !this.getOpenedAccountSettings().groupBySubjectEnabled
  }

  public changePhoneNumber(newPhoneNumber: string): Throwing<void> {
    this.getOpenedAccountSettings().phoneNumber = newPhoneNumber
  }

  public changeSignature(newSignature: string): Throwing<void> {
    this.getOpenedAccountSettings().signature = newSignature
  }

  public getNotificationOptionForFolder(folder: FolderName): Throwing<NotificationOption> {
    return this.getOpenedAccountSettings().folderToNotificationOption.get(folder)!
  }

  public getPlaceForSignature(): Throwing<SignaturePlace> {
    return this.getOpenedAccountSettings().placeForSignature
  }

  public getPushNotificationSound(): Throwing<NotificationSound> {
    return this.getOpenedAccountSettings().notificationSound
  }

  public getSignature(): Throwing<string> {
    return this.getOpenedAccountSettings().signature
  }

  public isAccountUsingEnabled(): Throwing<boolean> {
    return this.getOpenedAccountSettings().accountUsingEnabled
  }

  public isPushNotificationForAllEnabled(): Throwing<boolean> {
    return this.getOpenedAccountSettings().pushNotificationForAllEnabled
  }

  public isSortingEmailsByCategoryEnabled(): Throwing<boolean> {
    return this.getOpenedAccountSettings().sortingEmailsByCategoryEnabled
  }

  public isThemeEnabled(): Throwing<boolean> {
    return this.getOpenedAccountSettings().themeEnabled
  }

  public openFolderManager(): Throwing<void> {}

  public openLabelManager(): Throwing<void> {}

  public openMailingListsManager(): Throwing<void> {}

  public openPassport(): Throwing<void> {}

  public openFilters(): Throwing<void> {
    this.filterScreenOpenCounter += 1
  }

  public getFilterScreenOpenCounter(): Int32 {
    return this.filterScreenOpenCounter
  }

  public isSyncCalendarEnabled(): Throwing<boolean> {
    return this.syncCalendar
  }

  public switchSyncCalendar(): Throwing<void> {
    this.syncCalendar = !this.syncCalendar
  }

  public setNotificationOptionForFolder(folder: FolderName, option: NotificationOption): Throwing<void> {
    this.getOpenedAccountSettings().folderToNotificationOption.set(folder, option)
  }

  public setPlaceForSignature(place: SignaturePlace): Throwing<void> {
    this.getOpenedAccountSettings().placeForSignature = place
  }

  public setPushNotificationSound(sound: NotificationSound): Throwing<void> {
    this.getOpenedAccountSettings().notificationSound = sound
  }

  public switchPushNotification(): Throwing<void> {
    const currentAccountSettings: AccountSettingsModel = this.getOpenedAccountSettings()
    currentAccountSettings.pushNotificationForAllEnabled = !currentAccountSettings.pushNotificationForAllEnabled
  }

  public switchSortingEmailsByCategory(): Throwing<void> {
    const currentAccountSettings: AccountSettingsModel = this.getOpenedAccountSettings()
    currentAccountSettings.sortingEmailsByCategoryEnabled = !currentAccountSettings.sortingEmailsByCategoryEnabled

    if (currentAccountSettings.sortingEmailsByCategoryEnabled) {
      this.accHandler.getCurrentAccount().messagesDB.createFolder(DefaultFolderName.mailingLists)
      this.accHandler.getCurrentAccount().messagesDB.createFolder(DefaultFolderName.socialNetworks)

      const msgs = this.accHandler
        .getCurrentAccount()
        .messagesDB.getMessageIdList(new MessageListDatabaseFilter().withFolder(DefaultFolderName.inbox))
      const msgInSocialMedia = this.accHandler
        .getCurrentAccount()
        .messagesDB.getTabsToMessage(DefaultFolderName.socialNetworks)
      const msgInSubscriptions = this.accHandler
        .getCurrentAccount()
        .messagesDB.getTabsToMessage(DefaultFolderName.mailingLists)

      for (const index of range(0, msgs.length)) {
        if (msgInSocialMedia.has(msgs[index])) {
          this.accHandler
            .getCurrentAccount()
            .messagesDB.moveMessageToFolder(msgs[index], DefaultFolderName.socialNetworks)
        }
        if (msgInSubscriptions.has(msgs[index])) {
          this.accHandler
            .getCurrentAccount()
            .messagesDB.moveMessageToFolder(msgs[index], DefaultFolderName.mailingLists)
        }
      }
    } else {
      let msgs = this.accHandler
        .getCurrentAccount()
        .messagesDB.getMessageIdList(new MessageListDatabaseFilter().withFolder(DefaultFolderName.socialNetworks))
      for (const index of range(0, msgs.length)) {
        this.accHandler.getCurrentAccount().messagesDB.moveMessageToFolder(msgs[index], DefaultFolderName.inbox, false)
      }

      msgs = this.accHandler
        .getCurrentAccount()
        .messagesDB.getMessageIdList(new MessageListDatabaseFilter().withFolder(DefaultFolderName.mailingLists))
      for (const index of range(0, msgs.length)) {
        this.accHandler.getCurrentAccount().messagesDB.moveMessageToFolder(msgs[index], DefaultFolderName.inbox, false)
      }
      this.accHandler.getCurrentAccount().messagesDB.removeFolder(DefaultFolderName.mailingLists)
      this.accHandler.getCurrentAccount().messagesDB.removeFolder(DefaultFolderName.socialNetworks)
    }
  }

  public switchTheme(): Throwing<void> {
    const currentAccountSettings: AccountSettingsModel = this.getOpenedAccountSettings()
    currentAccountSettings.themeEnabled = !currentAccountSettings.themeEnabled
  }

  public switchUseAccountSetting(): Throwing<void> {
    const currentAccountSettings: AccountSettingsModel = this.getOpenedAccountSettings()
    currentAccountSettings.accountUsingEnabled = !currentAccountSettings.accountUsingEnabled
  }

  private demandRequiredAccountIndex(): Int32 {
    return requireNonNull(this.openedAccount, 'Необходимо зайти в настройки аккаунта')
  }
}
