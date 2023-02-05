import { Double, Int32, Throwing, YSError } from '../../../../common/ys'

export function assertBooleanEquals(expected: boolean, actual: boolean, message: string): Throwing<void> {
  if (expected !== actual) {
    throw new YSError(`${message}: expected=${expected}, but actual=${actual}`)
  }
}

export function assertTrue(condition: boolean, message: string): Throwing<void> {
  if (!condition) {
    throw new YSError(`${message}: ${condition} is not true`)
  }
}

export function assertInt32Equals(expected: Int32, actual: Int32, message: string): Throwing<void> {
  if (expected !== actual) {
    throw new YSError(`${message}: expected=${expected}, but actual=${actual}`)
  }
}

export function assertDoubleEquals(expected: Double, actual: Double, message: string): Throwing<void> {
  if (expected !== actual) {
    throw new YSError(`${message}: expected=${expected}, but actual=${actual}`)
  }
}

export function assertStringEquals(expected: string, actual: string, message: string): Throwing<void> {
  if (expected !== actual) {
    throw new YSError(`${message}: expected=${expected}, but actual=${actual}`)
  }
}
