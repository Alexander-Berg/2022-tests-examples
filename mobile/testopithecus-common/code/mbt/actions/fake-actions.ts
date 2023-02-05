import { MBTActionType } from '../mbt-abstractions'
import { AssertAction } from './assert-action'
import { DebugDumpAction } from './debug-dump-action'
import { PingAccountLockAction } from './ping-account-lock-action'

export function fakeActions(): MBTActionType[] {
  return [AssertAction.type, DebugDumpAction.type, PingAccountLockAction.type]
}
