import { Throwing, YSError } from '../../../../../common/ys'
import { EventusEvent } from '../../../../eventus-common/code/eventus-event'
import { BaseSimpleAction } from '../../../../testopithecus-common/code/mbt/base-simple-action'
import { Feature, MBTActionType, MBTComponent } from '../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { CalendarMailComponent } from '../components/calendar-mail-component'
import { ContactsListComponent } from '../components/contacts-list-component'
import { DiskWebViewComponent } from '../components/disk-web-view-component'
import { DocumentsMailComponent } from '../components/documents-component'
import { MaillistComponent } from '../components/maillist-component'
import { NotesWebViewComponent } from '../components/notes-web-view-component'
import { ShtorkaComponent } from '../components/shtorka-component'
import { SubscriptionsComponent } from '../components/subscriptions-component'
import { TelemostComponent } from '../components/telemost-component'
import { Shtorka, ShtorkaFeature, TabBar, TabBarFeature, TabBarItem } from '../feature/tab-bar-feature'

export class TabBarTapOnItemAction extends BaseSimpleAction<TabBar, MBTComponent> {
  public static readonly type: MBTActionType = 'TabBarTapOnItemAction'

  public constructor(private readonly item: TabBarItem) {
    super(TabBarTapOnItemAction.type)
  }

  public requiredFeature(): Feature<TabBar> {
    return TabBarFeature.get
  }

  public canBePerformedImpl(model: TabBar): Throwing<boolean> {
    return model.isShown()
  }

  public performImpl(modelOrApplication: TabBar, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.tapOnItem(this.item)
    switch (this.item) {
      case TabBarItem.mail:
        return new MaillistComponent()
      case TabBarItem.calendar:
        return new CalendarMailComponent()
      case TabBarItem.documents:
        return new DocumentsMailComponent()
      case TabBarItem.contacts:
        return new ContactsListComponent()
      case TabBarItem.more:
        return new ShtorkaComponent()
      case TabBarItem.telemost:
        return new TelemostComponent()
      default:
        throw new YSError(`Unknown tabbar item ${this.item}`)
    }
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return `${TabBarTapOnItemAction.type}(item=${this.item})`
  }
}

export class ShtorkaTapOnItemAction extends BaseSimpleAction<Shtorka, MBTComponent> {
  public static readonly type: MBTActionType = 'ShtorkaTapOnItemAction'

  public constructor(private readonly item: TabBarItem) {
    super(ShtorkaTapOnItemAction.type)
  }

  public requiredFeature(): Feature<Shtorka> {
    return ShtorkaFeature.get
  }

  public performImpl(modelOrApplication: Shtorka, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.tapOnItem(this.item)
    switch (this.item) {
      case TabBarItem.mail:
        return new MaillistComponent()
      case TabBarItem.calendar:
        return new CalendarMailComponent()
      case TabBarItem.documents:
        return new DocumentsMailComponent()
      case TabBarItem.contacts:
        return new ContactsListComponent()
      case TabBarItem.disk:
        return new DiskWebViewComponent()
      case TabBarItem.notes:
        return new NotesWebViewComponent()
      case TabBarItem.telemost:
        return new TelemostComponent()
      case TabBarItem.subscriptions:
        return new SubscriptionsComponent()
      default:
        throw new YSError(`Unknown shtorka item ${this.item}`)
    }
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return `${ShtorkaTapOnItemAction.type}(item=${this.item})`
  }
}

export class ShtorkaTapOnTryItAction extends BaseSimpleAction<Shtorka, MBTComponent> {
  public static readonly type: MBTActionType = 'ShtorkaTapOnTryItAction'
  public constructor() {
    super(ShtorkaTapOnTryItAction.type)
  }

  public requiredFeature(): Feature<Shtorka> {
    return ShtorkaFeature.get
  }

  public canBePerformedImpl(model: Shtorka): Throwing<boolean> {
    return model.getShownBannerType() !== null
  }

  public performImpl(modelOrApplication: Shtorka, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.tapOnBanner()
    return new SubscriptionsComponent()
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class ShtorkaCloseBannerAction extends BaseSimpleAction<Shtorka, MBTComponent> {
  public static readonly type: MBTActionType = 'ShtorkaCloseBannerAction'
  public constructor() {
    super(ShtorkaCloseBannerAction.type)
  }

  public requiredFeature(): Feature<Shtorka> {
    return ShtorkaFeature.get
  }

  public canBePerformedImpl(model: Shtorka): Throwing<boolean> {
    return model.getShownBannerType() !== null
  }

  public performImpl(modelOrApplication: Shtorka, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.closeBanner()
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class ShtorkaCloseBySwipeAction extends BaseSimpleAction<Shtorka, MBTComponent> {
  public static readonly type: MBTActionType = 'ShtorkaCloseBySwipeAction'
  public constructor() {
    super(ShtorkaCloseBySwipeAction.type)
  }

  public requiredFeature(): Feature<Shtorka> {
    return ShtorkaFeature.get
  }

  public performImpl(modelOrApplication: Shtorka, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.closeBySwipe()
    return new MaillistComponent()
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class ShtorkaCloseByTapOverAction extends BaseSimpleAction<Shtorka, MBTComponent> {
  public static readonly type: MBTActionType = 'ShtorkaCloseByTapOverAction'
  public constructor() {
    super(ShtorkaCloseByTapOverAction.type)
  }

  public requiredFeature(): Feature<Shtorka> {
    return ShtorkaFeature.get
  }

  public performImpl(modelOrApplication: Shtorka, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.closeByTapOver()
    return new MaillistComponent()
  }

  public events(): EventusEvent[] {
    return []
  }
}
