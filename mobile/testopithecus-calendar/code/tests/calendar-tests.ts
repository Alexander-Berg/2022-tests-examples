import { Nullable } from '../../../common/ys'
import { FeatureID } from '../../../xpackages/testopithecus-common/code/mbt/mbt-abstractions'
import { AccountType2, MBTTest, TestSettings } from '../../../xpackages/testopithecus-common/code/mbt/test/mbt-test'
import { TestsRegistry } from '../../../xpackages/testopithecus-common/code/mbt/test/tests-registry'
import { AppModel, TestPlan } from '../../../xpackages/testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../../../xpackages/testopithecus-common/code/users/user-pool'
import { CreateEventAction } from '../model/calendar-actions'
import { CalendarEvent, CalendarUser } from '../model/calendar-features'
import { CalendarPreparer } from '../model/calendar-preparer'
import { Timestamps } from '../model/timestamps'

export class DemoTest extends MBTTest<CalendarPreparer> {
  public constructor() {
    super('should create events')
  }

  public setupSettings(_: TestSettings): void {}

  public requiredAccounts(): AccountType2[] {
    return [AccountType2.YandexTeam]
  }

  public prepareAccounts(preparers: CalendarPreparer[]): void {
    preparers[0].addEvent(new CalendarEvent('start7', Timestamps.getDate(0), Timestamps.getDate(1)))
  }

  public scenario(accounts: UserAccount[], _: Nullable<AppModel>, __: FeatureID[]): TestPlan {
    const user = CalendarUser.from(accounts[0])
    return TestPlan.empty().then(
      new CreateEventAction(user, new CalendarEvent('pizza6', Timestamps.getDate(1), Timestamps.getDate(2))),
    )
  }
}

export class AllCalendarTests {
  public static get: TestsRegistry<CalendarPreparer> = new TestsRegistry().regular(new DemoTest())
}
