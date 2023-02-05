import { Nullable, Throwing } from '../../../../../common/ys'
import { Feature } from '../../../../testopithecus-common/code/mbt/mbt-abstractions'

export class TabBarFeature extends Feature<TabBar> {
  public static get: TabBarFeature = new TabBarFeature()

  private constructor() {
    super('TabBar', 'Таббар')
  }
}

export interface TabBar {
  isShown(): Throwing<boolean>

  getCurrentItem(): Throwing<TabBarItem>

  tapOnItem(item: TabBarItem): Throwing<void>
}

export class TabBarIOSFeature extends Feature<TabBarIOS> {
  public static get: TabBarIOSFeature = new TabBarIOSFeature()

  private constructor() {
    super('TabBarIOS', 'Специфичные для таббара на iOS методы')
  }
}

export interface TabBarIOS {
  getItems(): Throwing<TabBarItem[]>

  getCalendarIconDate(): Throwing<string>
}

export class TabBarAndroidFeature extends Feature<TabBarAndroid> {
  public static get: TabBarAndroidFeature = new TabBarAndroidFeature()

  private constructor() {
    super('TabBarAndroid', 'Специфичные для таббара на Android методы')
  }
}

export interface TabBarAndroid {
  getItems(): Throwing<TabBarItem[]>
}

export class ShtorkaFeature extends Feature<Shtorka> {
  public static get: ShtorkaFeature = new ShtorkaFeature()

  private constructor() {
    super('Shtorka', 'Шторка с сервисами, которая открывается по тапу на More в таббаре')
  }
}

export interface Shtorka {
  closeBySwipe(): Throwing<void>

  closeByTapOver(): Throwing<void>

  getShownBannerType(): Throwing<Nullable<ShtorkaBannerType>>

  tapOnBanner(): Throwing<void>

  closeBanner(): Throwing<void>

  tapOnItem(item: TabBarItem): Throwing<void>
}

export class ShtorkaAndroidFeature extends Feature<ShtorkaAndroid> {
  public static get: ShtorkaAndroidFeature = new ShtorkaAndroidFeature()

  private constructor() {
    super('ShtorkaAndroid', 'Специфичные для шторки на Android методы')
  }
}

export interface ShtorkaAndroid {
  getItems(): Throwing<TabBarItem[]>
}

export class ShtorkaIOSFeature extends Feature<ShtorkaIOS> {
  public static get: ShtorkaIOSFeature = new ShtorkaIOSFeature()

  private constructor() {
    super('ShtorkaIOS', 'Специфичные для шторки на iOS методы')
  }
}

export interface ShtorkaIOS {
  getItems(): Throwing<TabBarItem[]>
}

export enum ShtorkaBannerType {
  docs,
  scanner,
  mail360,
}

export enum TabBarItem {
  mail = 'Mail',
  calendar = 'Calendar',
  documents = 'Documents',
  telemost = 'Telemost',
  contacts = 'Contacts',
  more = 'More',
  disk = 'Disk',
  notes = 'Notes',
  scanner = 'Scanner',
  subscriptions = 'Subscriptions',
}
