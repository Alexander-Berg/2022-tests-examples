import { Int32, Throwing } from '../../../../../common/ys'
import { Feature } from '../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { FolderName } from './folder-list-features'

export class TabsFeature extends Feature<Tabs> {
  public static get: TabsFeature = new TabsFeature()

  private constructor() {
    super('Табы', 'Фича Табов. Реализовано включение, выключение, проверка сосотояния, а так же переход в таб')
  }
}

export interface Tabs {
  SwitchOffTabs(): Throwing<void>

  SwitchOnTabs(): Throwing<void>

  isEnableTabs(): Throwing<boolean>

  isDisplayNotificationTabs(tabsName: FolderName): Throwing<boolean>

  isUnreadNotificationTabs(tabsName: FolderName): Throwing<boolean>

  getPositionTabsNotification(tabsName: FolderName): Throwing<Int32>

  goToTabByNotification(tabsName: FolderName): Throwing<void>
}
