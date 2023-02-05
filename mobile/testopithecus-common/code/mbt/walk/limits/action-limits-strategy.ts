import { MBTAction } from '../../mbt-abstractions'
import { Stack } from '../data-structures/stack'

export interface ActionLimitsStrategy {
  check(actions: Stack<MBTAction>): boolean
}
