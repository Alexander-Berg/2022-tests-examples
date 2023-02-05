import {
  NetworkStatus,
  NetworkStatusCode,
  networkStatusFromJSONItem,
} from '../../../../../code/api/entities/status/network-status'
import { JSONItemFromJSON } from '../../../../../../common/__tests__/__helpers__/json-helpers'

describe(NetworkStatus, () => {
  it('should be deserializable from JSON for success', () => {
    const result = networkStatusFromJSONItem(
      JSONItemFromJSON({
        status: 1,
      }),
    )
    expect(result).toStrictEqual(new NetworkStatus(NetworkStatusCode.ok))
  })
  it('should be deserializable from JSON for authentication failure', () => {
    const result = networkStatusFromJSONItem(
      JSONItemFromJSON({
        status: 2001,
        trace: 'Oops',
        phrase: 'Hello world',
      }),
    )
    expect(result).toStrictEqual(new NetworkStatus(NetworkStatusCode.authenticationError, 'Oops', 'Hello world'))
  })
  it('should be deserializable from JSON for temporary failure', () => {
    const result = networkStatusFromJSONItem(
      JSONItemFromJSON({
        status: 2,
        trace: 'Oops',
        phrase: 'Hello world',
      }),
    )
    expect(result).toStrictEqual(new NetworkStatus(NetworkStatusCode.temporaryError, 'Oops', 'Hello world'))
  })
  it('should be deserializable from JSON for permanent failure', () => {
    const result = networkStatusFromJSONItem(
      JSONItemFromJSON({
        status: 3,
        phrase: 'Hello world',
      }),
    )
    expect(result).toStrictEqual(new NetworkStatus(NetworkStatusCode.permanentError, null, 'Hello world'))
  })
  it('should treat unknown status codes as temporary errors', () => {
    const result = networkStatusFromJSONItem(
      JSONItemFromJSON({
        status: 10,
        phrase: 'Hello world',
      }),
    )
    expect(result).toStrictEqual(new NetworkStatus(NetworkStatusCode.temporaryError, null, 'Hello world'))
  })
  it('should return null if status is not an map-like object', () => {
    const result = networkStatusFromJSONItem(
      JSONItemFromJSON([
        {
          status: 10,
          phrase: 'Hello world',
        },
      ]),
    )
    expect(result).toBeNull()
  })
  it('should return correct description', () => {
    expect(new NetworkStatus(NetworkStatusCode.ok, null, null).getErrorMessage()).toBe('No errors, status is OK')
    // tslint:disable-next-line:max-line-length
    expect(
      new NetworkStatus(NetworkStatusCode.authenticationError, null, 'PERM_FAIL folder_list: 2001').getErrorMessage(),
    ).toBe('Authentication error; Phrase = PERM_FAIL folder_list: 2001; Trace = ;')
    expect(
      new NetworkStatus(NetworkStatusCode.permanentError, null, 'PERM_FAIL folder_list: 2001').getErrorMessage(),
    ).toBe('Authentication error; Phrase = PERM_FAIL folder_list: 2001; Trace = ;')
    expect(new NetworkStatus(NetworkStatusCode.permanentError, '<trace>', '<phrase>').getErrorMessage()).toBe(
      'PERM error; Phrase = <phrase>; Trace = <trace>;',
    )
    expect(new NetworkStatus(NetworkStatusCode.temporaryError, '<trace>', '<phrase>').getErrorMessage()).toBe(
      'TEMP error; Phrase = <phrase>; Trace = <trace>;',
    )
  })
})
