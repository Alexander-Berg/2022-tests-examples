import { SignaturePlace } from '../../../../../mapi/code/api/entities/settings/settings-entities'
import { Int32, Throwing } from '../../../../../../common/ys'
import { Feature } from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { FolderName } from '../folder-list-features'

export interface AccountSettings {
  openAccountSettings(accountIndex: Int32): Throwing<void>

  closeAccountSettings(): Throwing<void>

  isGroupBySubjectEnabled(): Throwing<boolean>

  switchGroupBySubject(): Throwing<void>

  switchSortingEmailsByCategory(): Throwing<void>

  isSortingEmailsByCategoryEnabled(): Throwing<boolean>

  openMailingListsManager(): Throwing<void>

  getSignature(): Throwing<string>

  changeSignature(newSignature: string): Throwing<void>

  switchTheme(): Throwing<void>

  isThemeEnabled(): Throwing<boolean>

  getFolderToNotificationOption(): Throwing<Map<FolderName, NotificationOption>>

  setNotificationOptionForFolder(folder: FolderName, option: NotificationOption): Throwing<void>

  getNotificationOptionForFolder(folder: FolderName): Throwing<NotificationOption>

  openFilters(): Throwing<void>
}

export class AccountSettingsFeature extends Feature<AccountSettings> {
  public static get: AccountSettingsFeature = new AccountSettingsFeature()

  private constructor() {
    super(
      'AccountSettings',
      'Общие для iOS и Android настройки аккаунта пользователя. И в iOS, и в Android открываются с экрана Root Settings',
    )
  }
}

export interface IosAccountSettings {
  changePhoneNumber(newPhoneNumber: string): Throwing<void>

  getPushNotificationSound(): Throwing<NotificationSound>

  setPushNotificationSound(sound: NotificationSound): Throwing<void>

  switchPushNotification(): Throwing<void>

  isPushNotificationForAllEnabled(): Throwing<boolean>
}

export class IosAccountSettingsFeature extends Feature<IosAccountSettings> {
  public static get: IosAccountSettingsFeature = new IosAccountSettingsFeature()

  private constructor() {
    super('IosAccountSettings', 'Специфичные для iOS настройки аккаунта пользователя.')
  }
}

export interface AndroidAccountSettings {
  switchUseAccountSetting(): Throwing<void>

  isAccountUsingEnabled(): Throwing<boolean>

  openFolderManager(): Throwing<void>

  openLabelManager(): Throwing<void>

  openPassport(): Throwing<void>

  setPlaceForSignature(place: SignaturePlace): Throwing<void>

  getPlaceForSignature(): Throwing<SignaturePlace>

  isSyncCalendarEnabled(): Throwing<boolean>

  switchSyncCalendar(): Throwing<void>
}

export class AndroidAccountSettingsFeature extends Feature<AndroidAccountSettings> {
  public static get: AndroidAccountSettingsFeature = new AndroidAccountSettingsFeature()

  private constructor() {
    super('AndroidAccountSettings', 'Специфичные для Android настройки аккаунта пользователя.')
  }
}

export enum NotificationSound {
  standard = 'Standard',
  yandexMail = 'Yandex Mail',
}

export enum NotificationOption {
  doNotSync = 'Do not sync',
  syncWithoutNotification = 'Sync without notification',
  syncAndNotifyMe = 'Sync and notify me',
}
