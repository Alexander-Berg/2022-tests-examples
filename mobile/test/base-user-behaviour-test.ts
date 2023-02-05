import { Logger } from '../../../../common/code/logging/logger'
import { Int32, Nullable, YSError } from '../../../../../common/ys'
import { fail } from '../../utils/error-thrower'
import { PseudoRandomProvider } from '../../utils/pseudo-random'
import { FeatureID, MBTComponent } from '../mbt-abstractions'
import { StateMachine } from '../state-machine'
import { UserBehaviour } from '../walk/behaviour/user-behaviour'
import { MultiRunner } from '../walk/dfs-walk-strategy'
import { AppModel, TestPlan } from '../walk/fixed-scenario-strategy'
import { ActionLimitsStrategy } from '../walk/limits/action-limits-strategy'
import { RandomActionChooser, UserBehaviourWalkStrategy } from '../walk/user-behaviour-walk-strategy'
import { UserAccount } from '../../users/user-pool'
import { AccountType2, MBTTest, TestSuite } from './mbt-test'

export abstract class BaseUserBehaviourTest<T> extends MBTTest<T> {
  public constructor(
    description: string,
    protected readonly startComponent: MBTComponent,
    protected readonly pathLength: Int32,
    protected readonly logger: Logger,
    protected readonly seed: Int32,
  ) {
    super(description, [TestSuite.Random])
  }

  public scenario(accounts: UserAccount[], model: Nullable<AppModel>, supportedFeatures: FeatureID[]): TestPlan {
    if (model === null) {
      return TestPlan.empty()
    }
    const random = new PseudoRandomProvider(this.seed)
    const behaviour = this.getUserBehaviour(accounts)
    const walkStrategy = new UserBehaviourWalkStrategy(behaviour, new RandomActionChooser(random), this.pathLength)
    // eslint-disable-next-line prefer-const
    let applicationModel = model.copy()
    applicationModel.supportedFeatures = supportedFeatures
    const stateMachine = new StateMachine(model, applicationModel, walkStrategy, this.logger)
    try {
      stateMachine.go(this.startComponent)
    } catch (e) {
      if (e instanceof YSError) {
        fail((e as YSError).message)
      } else {
        throw new Error('Only YSError supported!')
      }
    }
    return TestPlan.empty().thenChain(walkStrategy.history)
  }

  public abstract requiredAccounts(): AccountType2[]

  public abstract prepareAccounts(preparers: T[]): void

  public abstract getUserBehaviour(accounts: UserAccount[]): UserBehaviour
}

export abstract class FullCoverageBaseTest<T> extends MBTTest<T> {
  public constructor(description: string, private startComponent: MBTComponent, private logger: Logger) {
    super(description, [TestSuite.FullCoverage])
  }

  public scenario(accounts: UserAccount[], model: Nullable<AppModel>, supportedFeatures: FeatureID[]): TestPlan {
    if (model === null) {
      return TestPlan.empty()
    }
    const behaviour = this.getUserBehaviour(accounts)
    const limits = this.getActionLimits()
    const runner = new MultiRunner(this.startComponent, behaviour, limits, supportedFeatures, this.logger)
    const path = runner.preparePath(model)
    const pathLength = path.length
    this.logger.info(`Optimal path length: ${pathLength}`)
    return TestPlan.empty().thenChain(path)
  }

  public abstract requiredAccounts(): AccountType2[]

  public abstract prepareAccounts(builders: T[]): void

  public abstract getUserBehaviour(userAccounts: UserAccount[]): UserBehaviour

  public abstract getActionLimits(): ActionLimitsStrategy
}
