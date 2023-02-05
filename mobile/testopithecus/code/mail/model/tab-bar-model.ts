import { int32ToString, Nullable, Throwing } from '../../../../../common/ys'
import { AccountType2, DeviceType } from '../../../../testopithecus-common/code/mbt/test/mbt-test'
import { MessageContainerType } from '../feature/message-list/container-getter-feature'
import {
  Shtorka,
  ShtorkaAndroid,
  ShtorkaBannerType,
  ShtorkaIOS,
  TabBar,
  TabBarAndroid,
  TabBarIOS,
  TabBarItem,
} from '../feature/tab-bar-feature'
import { ComposeModel } from './compose/compose-model'
import { FolderNavigatorModel } from './left-column/folder-navigator-model'
import { DeviceTypeModel, MailAppModelHandler } from './mail-model'
import { GroupModeModel } from './messages-list/group-mode-model'
import { MessageListDisplayModel } from './messages-list/message-list-display-model'
import { OpenMessageModel } from './opened-message/open-message-model'
import { ZeroSuggestModel } from './search/zero-suggest-model'
import { RootSettingsModel } from './settings/root-settings-model'

export class TabBarModel implements TabBar {
  public constructor(
    private readonly messageListDisplayModel: MessageListDisplayModel,
    private readonly openMessageModel: OpenMessageModel,
    private readonly groupModeModel: GroupModeModel,
    private readonly zeroSuggestModel: ZeroSuggestModel,
    private readonly rootSettingsModel: RootSettingsModel,
    private readonly composeModel: ComposeModel,
    private readonly folderNavigatorModel: FolderNavigatorModel,
    private readonly deviceTypeModel: DeviceTypeModel,
  ) {}

  private currentItem: TabBarItem = TabBarItem.mail

  public getCurrentItem(): Throwing<TabBarItem> {
    return this.currentItem
  }

  public setCurrentItem(item: TabBarItem): void {
    this.currentItem = item
  }

  public isShown(): Throwing<boolean> {
    const inGroupMode = this.groupModeModel.isInGroupMode()
    const isZeroSuggestOpened = this.zeroSuggestModel.isShown()
    const isSettingsOpened = this.rootSettingsModel.isOpened()
    const isComposeOpened = this.composeModel.isComposeOpened()
    const isFolderListOpened = this.folderNavigatorModel.isOpened()
    const isTab = this.deviceTypeModel.getDeviceType() === DeviceType.Tab
    return (
      this.messageListDisplayModel.getCurrentContainer().type !== MessageContainerType.search &&
      (!this.openMessageModel.isMessageOpened() || isTab) &&
      !inGroupMode &&
      !isZeroSuggestOpened &&
      !isSettingsOpened &&
      !isComposeOpened &&
      !isFolderListOpened
    )
  }

  public tapOnItem(item: TabBarItem): Throwing<void> {
    if (item !== TabBarItem.more) {
      this.currentItem = item
    }
  }
}

export class TabBarIOSModel implements TabBarIOS {
  public constructor(private readonly mailAppModelHandler: MailAppModelHandler) {}

  public getItems(): Throwing<TabBarItem[]> {
    const accountType = this.mailAppModelHandler.getCurrentAccountType()
    const yandexTabBarItems = [
      TabBarItem.mail,
      TabBarItem.calendar,
      TabBarItem.documents,
      TabBarItem.telemost,
      TabBarItem.more,
    ]
    const yandexTeamTabBarItems = [TabBarItem.mail, TabBarItem.calendar]
    const otherMailTabBarItems = [TabBarItem.mail, TabBarItem.telemost]

    switch (accountType) {
      case AccountType2.Yandex:
        return yandexTabBarItems
      case AccountType2.YandexTeam:
        return yandexTeamTabBarItems
      default:
        return otherMailTabBarItems
    }
  }

  public getCalendarIconDate(): Throwing<string> {
    const accountType = this.mailAppModelHandler.getCurrentAccountType()
    const currentDate = int32ToString(new Date().getDate())
    switch (accountType) {
      case AccountType2.Yandex:
        return currentDate
      case AccountType2.YandexTeam:
        return currentDate
      default:
        return ''
    }
  }
}

export class TabBarAndroidModel implements TabBarAndroid {
  public constructor(private readonly mailAppModelHandler: MailAppModelHandler) {}

  public getItems(): Throwing<TabBarItem[]> {
    const accountType = this.mailAppModelHandler.getCurrentAccountType()
    const yandexTabBarItems = [
      TabBarItem.mail,
      TabBarItem.contacts,
      TabBarItem.documents,
      TabBarItem.telemost,
      TabBarItem.more,
    ]
    const yandexTeamTabBarItems = [TabBarItem.mail, TabBarItem.contacts, TabBarItem.calendar]
    const otherMailTabBarItems = [TabBarItem.mail, TabBarItem.telemost]

    switch (accountType) {
      case AccountType2.Yandex:
        return yandexTabBarItems
      case AccountType2.YandexTeam:
        return yandexTeamTabBarItems
      default:
        return otherMailTabBarItems
    }
  }
}

export class ShtorkaModel implements Shtorka {
  public constructor(
    private readonly mailAppModelHandler: MailAppModelHandler,
    private readonly tabBarModel: TabBarModel,
  ) {}

  private isBannerClosedInCurrentShtorkaSession: boolean = false
  private bannersToShow: ShtorkaBannerType[] = [
    ShtorkaBannerType.docs,
    ShtorkaBannerType.scanner,
    ShtorkaBannerType.mail360,
  ]

  public closeBySwipe(): Throwing<void> {
    this.close()
  }

  public closeByTapOver(): Throwing<void> {
    this.close()
  }

  private close(): void {
    this.isBannerClosedInCurrentShtorkaSession = false
  }

  public tapOnItem(item: TabBarItem): Throwing<void> {
    switch (item) {
      case TabBarItem.notes:
        this.tabBarModel.setCurrentItem(TabBarItem.more)
        break
      case TabBarItem.disk:
        this.tabBarModel.setCurrentItem(TabBarItem.more)
        break
      case TabBarItem.subscriptions:
        break
      case TabBarItem.scanner:
        break
      default:
        this.tabBarModel.setCurrentItem(item)
    }
    this.close()
  }

  public closeBanner(): Throwing<void> {
    this.bannersToShow.splice(0, 1)
    this.isBannerClosedInCurrentShtorkaSession = true
  }

  public getShownBannerType(): Throwing<Nullable<ShtorkaBannerType>> {
    if (this.bannersToShow.length === 0 || this.isBannerClosedInCurrentShtorkaSession) {
      return null
    }
    const currentBanner = this.bannersToShow[0]
    if (currentBanner === ShtorkaBannerType.mail360) {
      const promoteMail360 = !this.mailAppModelHandler.getCurrentAccount().promoteMail360
      return promoteMail360 ? currentBanner : null
    }
    return currentBanner
  }

  public tapOnBanner(): Throwing<void> {
    this.close()
  }
}

export class ShtorkaIOSModel implements ShtorkaIOS {
  public constructor() {}

  public getItems(): Throwing<TabBarItem[]> {
    return [TabBarItem.disk, TabBarItem.notes, TabBarItem.scanner, TabBarItem.subscriptions]
  }
}

export class ShtorkaAndroidModel implements ShtorkaAndroid {
  public constructor() {}

  public getItems(): Throwing<TabBarItem[]> {
    return [TabBarItem.contacts, TabBarItem.disk, TabBarItem.notes, TabBarItem.scanner, TabBarItem.subscriptions]
  }
}
