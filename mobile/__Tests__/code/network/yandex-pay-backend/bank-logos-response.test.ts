import { JSONParsingError } from '../../../../../common/code/json/json-types'
import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { BankLogo } from '../../../../code/models/bank-logo'
import { BankLogosResponse } from '../../../../code/network/yandex-pay-backend/bank-logos-response'
import { bankLogo } from '../../../__helpers__/bank-logo-helper'

describe(BankLogosResponse, () => {
  it('should be deserializable from JSON response', () => {
    const response = BankLogosResponse.fromJSONItem(
      JSONItemFromJSON({
        AK_BARS: {
          FULL: {
            LIGHT: 'ak-bars-full-light',
            DARK: 'ak-bars-full-dark',
            MONO: 'ak-bars-full-mono',
          },
          SHORT: {
            LIGHT: 'ak-bars-short-light',
          },
        },
        ALFABANK: {
          FULL: {
            DARK: 'alfa-full-dark',
          },
          SHORT: {
            LIGHT: 'alfa-short-light',
            DARK: 'alfa-short-dark',
            MONO: 'alfa-short-mono',
          },
        },
      }),
    )
    expect(response.isError()).toBe(false)
    expect(response.getValue()).toStrictEqual(
      new BankLogosResponse(
        'success',
        200,
        new Map<string, BankLogo>([
          [
            'AK_BARS',
            bankLogo({
              full: { light: 'ak-bars-full-light', dark: 'ak-bars-full-dark', mono: 'ak-bars-full-mono' },
              short: { light: 'ak-bars-short-light' },
            }),
          ],
          [
            'ALFABANK',
            bankLogo({
              full: { dark: 'alfa-full-dark' },
              short: { light: 'alfa-short-light', dark: 'alfa-short-dark', mono: 'alfa-short-mono' },
            }),
          ],
        ]),
      ),
    )
  })
  it('should deserialize error if status is not success', () => {
    const response = BankLogosResponse.fromJSONItem(JSONItemFromJSON([]))
    expect(response.isError()).toBe(true)
    expect(response.getError()).toBeInstanceOf(JSONParsingError)
  })
  it('should skip improperly formatted values', () => {
    const response = BankLogosResponse.fromJSONItem(
      JSONItemFromJSON({
        A: 10,
        B: true,
        C: {
          D: 'hello',
        },
        D: {
          SHORT: {
            DARK: true,
          },
        },
        E: {
          FULL: {
            Z: 'Z',
          },
        },
        F: {
          FULL: {
            LIGHT: 'F-FULL-LIGHT',
          },
          SHORT: {
            DARK: 'F-SHORT-DARK',
          },
        },
      }),
    )
    expect(response.isError()).toBe(false)
    expect(response.getValue()).toStrictEqual(
      new BankLogosResponse(
        'success',
        200,
        new Map<string, BankLogo>([
          ['C', bankLogo({})],
          ['D', bankLogo({ short: {} })],
          ['E', bankLogo({ full: {} })],
          ['F', bankLogo({ full: { light: 'F-FULL-LIGHT' }, short: { dark: 'F-SHORT-DARK' } })],
        ]),
      ),
    )
  })
})
