import { Result } from '../../code/result/result'

import JestMock from 'jest-mock'

export function objectToMap(obj: any): Map<string, any> {
  const result = new Map<string, any>()
  for (const key of Object.getOwnPropertyNames(obj)) {
    result.set(key, innerObjectToMap(obj[key]))
  }
  return result

  function innerObjectToMap(item: any): any {
    switch (typeof item) {
      case 'bigint':
      case 'boolean':
      case 'number':
      case 'string':
      case 'symbol':
      case 'undefined':
        return item
      case 'object':
        if (item === null) {
          return item
        } else if (Array.isArray(item)) {
          return item.map(innerObjectToMap)
        } else {
          return objectToMap(item)
        }
        break
      case 'function':
        throw new Error('Functions are not supported as values')
    }
  }
}

export type Writable<T> = {
  -readonly [P in keyof T]: T[P]
}

export function clone(obj: any): any {
  switch (typeof obj) {
    case 'bigint':
    case 'boolean':
    case 'number':
    case 'undefined':
    case 'symbol':
    case 'string':
      return obj
    case 'object':
      if (obj === null) {
        return obj
      }
      if (Array.isArray(obj)) {
        return obj.map(clone)
      }
      const result: { [name: string]: any } = {}
      for (const prop of Object.getOwnPropertyNames(obj)) {
        result[prop] = clone(obj[prop])
      }
      return result
    default:
      throw new Error(`Unsupported type of object to clone: ${typeof obj}`)
  }
}

export function tryCatchResult<T>(execute: () => T): Result<T> {
  try {
    return new Result<T>(execute(), null)
  } catch (e) {
    return new Result<T>(null, e)
  }
}

export type ClassType<T> = new (...args: any[]) => T

export function createMockInstance<T>(classConstructor: ClassType<T>, patch: Partial<T> = {}): T {
  const MockedClassConstructor = JestMock.generateFromMetadata(JestMock.getMetadata(classConstructor)!)!
  const instance = new MockedClassConstructor()
  return Object.assign(instance as any, patch)
}
