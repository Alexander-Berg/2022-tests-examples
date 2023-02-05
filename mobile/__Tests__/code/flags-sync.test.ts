/* eslint-disable @typescript-eslint/unbound-method */
import { PlatformType } from '../../../common/code/network/platform'
import { resolve } from '../../../../common/xpromise-support'
import { YSError } from '../../../../common/ys'
import { MockNetwork } from '../../../common/__tests__/__helpers__/mock-patches'
import { getVoid } from '../../../common/code/result/result'
import { FlagsRequest } from '../../code/api/flags-request'
import { FlagsSync } from '../../code/flags-sync'
import { JSONItemFromJSON } from '../../../common/__tests__/__helpers__/json-helpers'
import response from './sample.json'
import { MockFlagConfigurationsStore } from './flag-configurations-store.test'

describe(FlagsSync, () => {
  describe('fetchFlags', () => {
    it('should fail if flags response is malformed', (done) => {
      const execute = jest.fn().mockReturnValue(resolve(JSONItemFromJSON([])))
      const network = MockNetwork({ execute })
      const flagsStore = MockFlagConfigurationsStore()
      const model = new FlagsSync(network, flagsStore)

      expect.assertions(2)
      model.fetchFlags('MOBMAIL', '', { type: PlatformType.android, isTablet: false }).failed((error) => {
        expect(error).toStrictEqual(
          new YSError(
            'JSON Item parsing failed for entity FlagsResponseWithRawItems:\n<JSONItem kind: array, value: []>',
          ),
        )
        expect(flagsStore.storeRawConfigurations).not.toBeCalled()
        done()
      })
    })
    it('should fetch new flags', (done) => {
      const execute = jest.fn().mockReturnValue(resolve(JSONItemFromJSON(response)))
      const network = MockNetwork({ execute })
      const flagsStore = MockFlagConfigurationsStore({
        storeRawConfigurations: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const model = new FlagsSync(network, flagsStore)

      expect.assertions(2)
      model
        .fetchFlags('MOBMAIL', '', {
          type: PlatformType.android,
          isTablet: false,
        })
        .then(() => {
          expect(network.execute).toBeCalledWith(
            new FlagsRequest('MOBMAIL', '', {
              type: PlatformType.android,
              isTablet: false,
            }),
          )
          expect(flagsStore.storeRawConfigurations).toBeCalledWith(JSONItemFromJSON(response))
          done()
        })
    })
  })
})
