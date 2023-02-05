import { JSONSerializer } from '../../../common/code/json/json-serializer'
import { ArrayJSONItem, MapJSONItem } from '../../../common/code/json/json-types'
import { Log } from '../../../common/code/logging/logger'
import { Int32, int64, Int64, Nullable, range, Throwing, YSError } from '../../../../common/ys'
import { SyncNetwork } from '../client/network/sync-network'
import { MBTPlatform } from '../mbt/test/mbt-test'
import { fail } from './error-thrower'
import { TestpalmTrustedNetworkRequest } from './testpalm-network-request'

export function filterByOrders<T>(array: T[], byOrders: Set<Int32>): T[] {
  const result: T[] = []
  for (const i of byOrders.values()) {
    result.push(array[i])
  }
  return result
}

export function valuesArray<K, V>(iterable: Map<K, V>): V[] {
  const result: V[] = []
  for (const element of iterable.values()) {
    result.push(element)
  }
  return result
}

export function keysArray<K, V>(iterable: Map<K, V>): K[] {
  const result: K[] = []
  for (const element of iterable.keys()) {
    result.push(element)
  }
  return result
}

export function currentTimeMs(): Int64 {
  return int64(Date.now())
}

export function copyArray<T>(array: readonly T[]): T[] {
  const result: T[] = []
  for (const element of array) {
    result.push(element)
  }
  return result
}

export function copySet<T>(set: Set<T>): Set<T> {
  const result: Set<T> = new Set<T>()
  for (const element of set.values()) {
    result.add(element)
  }
  return result
}

export function copyMap<T, S>(map: Map<T, S>): Map<T, S> {
  const result: Map<T, S> = new Map<T, S>()
  map.forEach((value, key) => {
    result.set(key, value)
  })
  return result
}

export function requireNonNull<T>(obj: Nullable<T>, message: string): T {
  if (obj === null) {
    fail(message)
  }
  return obj!
}

export function getSliceIndexesForBuckets(total: Int32, bucketsTotal: Int32): Int32[] {
  const buckets: Int32[] = []
  const bucketsSliceStartIndexes: Int32[] = []
  const remainder: Int32 = total % bucketsTotal
  const intPart: Int32 = (total - remainder) / bucketsTotal
  let currentBucketStartIndex: Int32 = 0

  for (const _ of range(0, bucketsTotal)) {
    buckets.push(intPart)
  }
  for (const i of range(0, remainder)) {
    buckets[i] += 1
  }

  for (const i of range(0, bucketsTotal)) {
    bucketsSliceStartIndexes.push(currentBucketStartIndex)
    currentBucketStartIndex += buckets[i]
  }

  bucketsSliceStartIndexes.push(currentBucketStartIndex)

  Log.info(`All buckets distribution ${buckets}`)
  Log.info(`All buckets slice start indexes ${bucketsSliceStartIndexes}`)

  // for (const i of range(0, bucketIndex)) {
  //   currentBucketStartIndex += buckets[i]
  // }

  return bucketsSliceStartIndexes
}

export class TestopithecusConstants {
  public static readonly SWIFT_CONSTRUCTOR_VARIABLE_WORKAROUND_TITLE: string =
    'Ignore this param. This is a workaround for constructor override bug in our Swift Generator. See: https://st.yandex-team.ru/SSP-156'
}

export function getTrustedCases(
  platform: MBTPlatform,
  testPalmToken: string,
  network: SyncNetwork,
  jsonSerializer: JSONSerializer,
): Throwing<Int32[]> {
  const ids: Int32[] = []
  if (testPalmToken === '') {
    Log.info(`No token for testpalm! Can't get trusted tests!`)
    return []
  }
  const response: string = network
    .syncExecuteWithRetries(
      3,
      'https://testpalm-api.yandex-team.ru',
      new TestpalmTrustedNetworkRequest(platform),
      testPalmToken,
    )
    .tryGetValue()
  const json: ArrayJSONItem = jsonSerializer.deserialize(response).getValue() as ArrayJSONItem
  json.asArray().forEach((tpCase) => {
    const id = (tpCase as MapJSONItem).getInt32('id')
    if (id !== null) {
      ids.push(id)
    }
  })
  return ids
}

export function extractErrorMessage(e: any): string {
  return e instanceof YSError ? (e as YSError).message : `${e}`
}

export function getYSError(e: any): YSError {
  return e instanceof YSError ? (e as YSError) : new YSError(`${e}`)
}
