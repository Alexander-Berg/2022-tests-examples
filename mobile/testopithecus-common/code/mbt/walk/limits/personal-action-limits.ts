import { Int32, range } from '../../../../../../common/ys'
import { MBTAction, MBTActionType } from '../../mbt-abstractions'
import { Stack } from '../data-structures/stack'
import { ActionLimitsStrategy } from './action-limits-strategy'

export class PersonalActionLimits implements ActionLimitsStrategy {
  private personalLimits: Map<MBTActionType, Int32> = new Map<MBTActionType, Int32>()

  public constructor(private totalLimit: Int32) {}

  public setLimit(action: MBTActionType, limit: Int32): PersonalActionLimits {
    this.personalLimits.set(action, limit)
    return this
  }

  public check(actions: Stack<MBTAction>): boolean {
    if (this.totalLimit <= actions.size()) {
      return false
    }
    for (const actionType of this.personalLimits.keys()) {
      let count: Int32 = 0
      for (const i of range(0, actions.size())) {
        if (actions.get(i).getActionType() === actionType) {
          count += 1
        }
      }
      if (count >= this.personalLimits.get(actionType)!) {
        return false
      }
    }
    return true
  }
}
