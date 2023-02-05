import { Nullable } from '../../../../../common/ys'
import { UserAccount } from '../../users/user-pool'
import { FeatureID } from '../mbt-abstractions'
import { AppModel, TestPlan } from '../walk/fixed-scenario-strategy'
import { AccountType2, MBTTest, TestSuite } from './mbt-test'

export abstract class RegularTestBase<T> extends MBTTest<T> {
  protected constructor(
    description: string,
    public readonly accountType: AccountType2,
    suite: TestSuite[] = [TestSuite.Fixed],
  ) {
    super(description, suite)
  }

  public requiredAccounts(): AccountType2[] {
    return [this.accountType]
  }

  public prepareAccounts(preparers: T[]): void {
    if (preparers.length !== 1) {
      throw new Error('Тесты на базе RegularTestBase должны наливать ровно один аккаунт!')
    }
    this.prepareAccount(preparers[0])
  }

  public scenario(accounts: UserAccount[], _model: Nullable<AppModel>, _supportedFeatures: FeatureID[]): TestPlan {
    if (accounts.length !== 1) {
      throw new Error('Тесты на базе RegularTestBase должны использовать ровно один аккаунт!')
    }
    return this.regularScenario(accounts[0])
  }

  public abstract regularScenario(account: UserAccount): TestPlan

  public abstract prepareAccount(preparer: T): void
}
