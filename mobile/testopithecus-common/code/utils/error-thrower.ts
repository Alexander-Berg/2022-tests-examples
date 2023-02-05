import { Registry } from '../../../testopithecus-common/code/utils/registry'

export interface ErrorThrower {
  fail(message: string): void
}

export function fail(message: string): void {
  const errorThrower = Registry.get().errorThrower
  if (errorThrower === null) {
    throw new Error(message)
  }
  errorThrower.fail(message)
}
