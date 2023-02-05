import { reject, resolve } from '../../../../../../common/xpromise-support'
import { YSError } from '../../../../../../common/ys'
import { MockNetwork } from '../../../../../common/__tests__/__helpers__/mock-patches'
import { JSONItem } from '../../../../../common/code/json/json-types'
import { getVoid } from '../../../../../common/code/result/result'
import { settingsResponseFromJSONItem } from '../../../../../mapi/code/api/entities/settings/settings-entities'
import { SettingsRequest } from '../../../../../mapi/code/api/entities/settings/settings-request'
import { NetworkStatus, NetworkStatusCode } from '../../../../../mapi/code/api/entities/status/network-status'
import { MailApiTempError } from '../../../../../mapi/code/api/mail-api-error'
import { Network } from '../../../../../common/code/network/network'
import { SettingsSaver, SettingsSync } from '../../../../code/busilogics/settings/settings-sync'
import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import sampleResponse from './sample.json'

const saver: SettingsSaver = { save: jest.fn() }

describe(SettingsSync, () => {
  it('should execute request for settings', (done) => {
    // Just respond with rejection. Actual result doesn't matter: we test network request
    const execute: Network['execute'] = jest.fn().mockReturnValue(reject<JSONItem>(new YSError("Doesn't matter")))
    const network = MockNetwork({ execute })
    const settingsSync = new SettingsSync(network, saver)
    expect.assertions(1)
    settingsSync.synchronize().failed(() => {
      expect(execute).toBeCalledWith(new SettingsRequest())
      done()
    })
  })
  it('should fail if request for settings fails', (done) => {
    const execute: Network['execute'] = jest.fn().mockReturnValue(reject<JSONItem>(new YSError('NETWORK ERROR')))
    const network = MockNetwork({ execute })
    const settingsSync = new SettingsSync(network, saver)
    expect.assertions(1)
    settingsSync.synchronize().failed((error) => {
      expect(error).toStrictEqual(new YSError('NETWORK ERROR'))
      done()
    })
  })
  it('should fail if JSONItem is malformed', (done) => {
    const execute: Network['execute'] = jest.fn().mockReturnValue(resolve(JSONItemFromJSON([sampleResponse])))
    const network = MockNetwork({ execute })
    const settingsSync = new SettingsSync(network, saver)
    expect.assertions(1)
    settingsSync.synchronize().failed((error) => {
      expect(error).toStrictEqual(new YSError('JSON Item parsing failed for entity SettingsResponse (settings)'))
      done()
    })
  })
  it('should fail if status of response is not ok', (done) => {
    const jsonItem = JSONItemFromJSON({ ...sampleResponse, status: { status: 2 } })
    const execute: Network['execute'] = jest.fn().mockReturnValue(resolve(jsonItem))
    const network = MockNetwork({ execute })
    const save = jest.fn().mockReturnValue(resolve(getVoid()))
    const settingsSync = new SettingsSync(network, { save })
    expect.assertions(1)
    settingsSync.synchronize().failed((error) => {
      expect(error).toStrictEqual(new MailApiTempError(new NetworkStatus(NetworkStatusCode.temporaryError)))
      done()
    })
  })
  it('should call save if status of response is ok', async () => {
    const jsonItem = JSONItemFromJSON(sampleResponse)
    const settingsResponse = settingsResponseFromJSONItem(jsonItem)!
    const execute: Network['execute'] = jest.fn().mockReturnValue(resolve(jsonItem))
    const network = MockNetwork({ execute })
    const save = jest.fn().mockReturnValue(resolve(getVoid()))
    const settingsSync = new SettingsSync(network, { save })

    const result = await settingsSync.synchronize()
    expect(save).toBeCalledWith(settingsResponse)
    expect(result).toStrictEqual(settingsResponse)
  })
  it('should call save and return failure if it failed', (done) => {
    const jsonItem = JSONItemFromJSON(sampleResponse)
    const execute: Network['execute'] = jest.fn().mockReturnValue(resolve(jsonItem))
    const network = MockNetwork({ execute })
    const save = jest.fn().mockReturnValue(reject(new YSError('SAVE ERROR')))
    const settingsSync = new SettingsSync(network, { save })

    expect.assertions(1)
    settingsSync.synchronize().failed((error) => {
      expect(error).toStrictEqual(new YSError('SAVE ERROR'))
      done()
    })
  })
})
