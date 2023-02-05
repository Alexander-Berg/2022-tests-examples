import { Int32 } from '../../../../../../common/ys'
import { MBTAction } from '../../mbt-abstractions'
import { Stack } from '../data-structures/stack'
import { ActionLimitsStrategy } from './action-limits-strategy'

export class TotalActionLimits implements ActionLimitsStrategy {
  public constructor(private totalLimit: Int32) {}

  public check(actions: Stack<MBTAction>): boolean {
    return this.totalLimit > actions.size()
  }
}
