import { Nullable, Throwing, undefinedToNull, YSError } from '../../../../common/ys'
import { JSONSerializer } from '../../../common/code/json/json-serializer'
import { JSONItem } from '../../../common/code/json/json-types'
import { HttpRequest } from '../../../common/code/network/http-layer'
import { Result, resultError, resultValue } from '../../../common/code/result/result'

export function getHttpOAuth(request: HttpRequest): Nullable<string> {
  const auth = getRequestHeader(request.headers, 'Authorization')
  if (auth === null) {
    return null
  }
  const parts = auth!.split(' ')
  if (parts.length !== 2) {
    return null
  }
  return parts[1]
}

export function getRequestHeader(headers: ReadonlyMap<string, string>, header: string): Nullable<string> {
  const headerToCompare = header.toLowerCase()
  for (const key of headers.keys()) {
    if (key.toLowerCase() === headerToCompare) {
      return undefinedToNull(headers.get(key))
    }
  }
  return null
}

export function tryGetRequestHeader(headers: ReadonlyMap<string, string>, header: string): Throwing<string> {
  const result = getRequestHeader(headers, header)
  if (result === null) {
    throw new YSError(`Failed to query request headers for key "${header}"`)
  }
  return result!
}

export function extractMockRequest<T>(
  bodyString: string,
  serializer: JSONSerializer,
  parse: (item: JSONItem) => Result<T>,
): Result<T> {
  const deserialized = serializer.deserialize(bodyString)
  if (deserialized.isError()) {
    return resultError(deserialized.getError())
  }
  const json = deserialized.getValue()
  const parsed = parse(json)
  if (parsed.isError()) {
    return resultError(parsed.getError())
  }

  return resultValue(parsed.getValue())
}
