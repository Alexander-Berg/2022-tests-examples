import { int64ToString, undefinedToNull } from '../../../../common/ys'
import {
  ArrayJSONItem,
  BooleanJSONItem,
  DoubleJSONItem,
  IntegerJSONItem,
  JSONItem,
  JSONItemKind,
  JSONItemToDouble,
  JSONItemToInt32,
  JSONItemToInt64,
  MapJSONItem,
  NullJSONItem,
  StringJSONItem,
} from '../../../common/code/json/json-types'

export function JSONItemFromJSON(object: any): JSONItem {
  switch (typeof object) {
    case 'undefined':
      return new NullJSONItem()
    case 'boolean':
      return new BooleanJSONItem(object)
    case 'number':
      if (Number.isInteger(object)) {
        return object < 2 ** 32 - 1 && object > -(2 ** 32)
          ? IntegerJSONItem.fromInt32(object)
          : IntegerJSONItem.fromInt64(BigInt(object))
      } else {
        return new DoubleJSONItem(object)
      }
    case 'bigint':
      return IntegerJSONItem.fromInt64(object)
    case 'string':
      return new StringJSONItem(object)
    case 'object':
      if (undefinedToNull(object) === null) {
        return new NullJSONItem()
      } else if (Array.isArray(object)) {
        return arrayJSONItemFromArray(object)
      } else if (object instanceof Map) {
        return mapJSONItemFromMap(object as Map<string, any>)
      } else {
        return mapJSONItemFromObject(object)
      }
    default:
      throw new Error(`Unsupported type for converting to JSONItem: ${typeof object}`)
  }
}

export function arrayJSONItemFromArray(object: readonly any[]): ArrayJSONItem {
  const result = new ArrayJSONItem()
  for (const item of object) {
    result.add(JSONItemFromJSON(item))
  }
  return result
}

export function mapJSONItemFromMap(map: Map<string, any>): MapJSONItem {
  const result = new MapJSONItem()
  map.forEach((v, k) => {
    result.put(k, JSONItemFromJSON(v))
  })
  return result
}

export function mapJSONItemFromObject(object: any): MapJSONItem {
  const result = new MapJSONItem()
  for (const name of Object.getOwnPropertyNames(object)) {
    result.put(name, JSONItemFromJSON(object[name]))
  }
  return result
}

export function objectFromJSONItem(jsonItem: JSONItem): any {
  switch (jsonItem.kind) {
    case JSONItemKind.array:
      return (jsonItem as ArrayJSONItem).asArray().map(objectFromJSONItem)
    case JSONItemKind.map:
      return Object.fromEntries(
        Array.from((jsonItem as MapJSONItem).asMap().entries()).map(([key, value]) => [key, objectFromJSONItem(value)]),
      )
    case JSONItemKind.double:
      return JSONItemToDouble(jsonItem)
    case JSONItemKind.integer:
      return (jsonItem as IntegerJSONItem).isInt64 ? JSONItemToInt64(jsonItem) : JSONItemToInt32(jsonItem)
    case JSONItemKind.nullItem:
      return null
    case JSONItemKind.string:
      return (jsonItem as StringJSONItem).value
    case JSONItemKind.boolean:
      return (jsonItem as BooleanJSONItem).value
  }
}

export function JSONItemToJSONString(jsonItem: JSONItem, skipNulls = true, int64AsString = true): string {
  return JSON.stringify(objectFromJSONItem(transformJSONItemToSerializable(jsonItem, skipNulls, int64AsString)))
}

export function transformJSONItemToSerializable(jsonItem: JSONItem, skipNulls = true, int64AsString = true): JSONItem {
  switch (jsonItem.kind) {
    case JSONItemKind.array:
      return new ArrayJSONItem(
        (jsonItem as ArrayJSONItem)
          .asArray()
          .map((item) => transformJSONItemToSerializable(item, skipNulls, int64AsString)),
      )
    case JSONItemKind.map:
      const transformed = new MapJSONItem()
      for (const [key, value] of (jsonItem as MapJSONItem).asMap().entries()) {
        const newValue = transformJSONItemToSerializable(value, skipNulls, int64AsString)
        if (!(skipNulls && (value as JSONItem).kind === JSONItemKind.nullItem)) {
          transformed.put(key, newValue)
        }
      }
      return transformed
    case JSONItemKind.double:
      return jsonItem
    case JSONItemKind.integer:
      return (jsonItem as IntegerJSONItem).isInt64 && int64AsString
        ? new StringJSONItem(int64ToString(JSONItemToInt64(jsonItem)!))
        : jsonItem
    case JSONItemKind.nullItem:
      return jsonItem
    case JSONItemKind.string:
      return jsonItem
    case JSONItemKind.boolean:
      return jsonItem
  }
}

export function JSONItemFromJSONString(value: string): JSONItem {
  return JSONItemFromJSON(JSON.parse(value))
}

export function MapJsonByPath(item: MapJSONItem, path: string[]): MapJSONItem {
  let current: MapJSONItem = item
  for (const key of path) {
    current = current.get(key) as MapJSONItem
  }
  return current
}
