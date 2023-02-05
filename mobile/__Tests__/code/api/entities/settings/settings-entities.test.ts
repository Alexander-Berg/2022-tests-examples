import { MapJSONItem } from '../../../../../../common/code/json/json-types'
import { Email } from '../../../../../code/api/entities/recipient/email'
import {
  AccountInformation,
  int32ToSignaturePlace,
  SettingsResponse,
  settingsResponseFromJSONItem,
  SettingsSetup,
  SignaturePlace,
  signaturePlaceToInt32,
  StatusResponsePayload,
  UserParameters,
} from '../../../../../code/api/entities/settings/settings-entities'
import { SettingsRequest } from '../../../../../code/api/entities/settings/settings-request'
import { NetworkStatus, NetworkStatusCode } from '../../../../../code/api/entities/status/network-status'
import { NetworkMethod, RequestEncodingKind } from '../../../../../../common/code/network/network-request'
import { JSONItemFromJSON } from '../../../../../../common/__tests__/__helpers__/json-helpers'
import { NetworkAPIVersions } from '../../../../../code/api/mail-network-request'
import { clone } from '../../../../../../common/__tests__/__helpers__/utils'
import sampleItem from './sample.json'

describe(SettingsRequest, () => {
  it('should provide request attributes', () => {
    const request = new SettingsRequest()
    expect(request.method()).toBe(NetworkMethod.get)
    expect(request.params().asMap().size).toBe(0)
    expect(request.path()).toBe('settings')
    expect(request.version()).toBe(NetworkAPIVersions.v1)
    expect(request.encoding().kind).toBe(RequestEncodingKind.url)
    expect(request.urlExtra()).toStrictEqual(new MapJSONItem())
    expect(request.headersExtra()).toStrictEqual(new MapJSONItem())
  })
})

describe('SignaturePlace', () => {
  it('should convert Int32 to SignaturePlace', () => {
    expect(int32ToSignaturePlace(0)).toBe(SignaturePlace.none)
    expect(int32ToSignaturePlace(1)).toBe(SignaturePlace.atEnd)
    expect(int32ToSignaturePlace(2)).toBe(SignaturePlace.afterReply)
    expect(int32ToSignaturePlace(3)).toBe(SignaturePlace.none)
  })
  it('should convert SignaturePlace to Int32', () => {
    expect(signaturePlaceToInt32(SignaturePlace.none)).toBe(0)
    expect(signaturePlaceToInt32(SignaturePlace.atEnd)).toBe(1)
    expect(signaturePlaceToInt32(SignaturePlace.afterReply)).toBe(2)
  })
})

describe(Email, () => {
  it('should build Email from string', () => {
    expect(Email.fromString('')).toBeNull()
    expect(Email.fromString('hello')).toBeNull()
    expect(Email.fromString('hello@')).toStrictEqual(new Email('hello', ''))
    expect(Email.fromString('@')).toStrictEqual(new Email('', ''))
    expect(Email.fromString('hello@domain')).toStrictEqual(new Email('hello', 'domain'))
  })
})

describe(SettingsResponse, () => {
  it('should be deserializable from JSONItem', () => {
    const response = settingsResponseFromJSONItem(JSONItemFromJSON(sampleItem))
    expect(response).not.toBeNull()
    expect(response!.networkStatus()).toStrictEqual(new NetworkStatus(NetworkStatusCode.ok))
    expect(response!).toStrictEqual(
      new SettingsResponse(
        new NetworkStatus(NetworkStatusCode.ok),
        new StatusResponsePayload(
          new AccountInformation(
            sampleItem.account_information['account-information'].uid,
            sampleItem.account_information['account-information'].suid,
            sampleItem.account_information['account-information'].emails.email.map(
              ({ login, domain }) => new Email(login, domain),
            ),
            sampleItem.account_information['account-information']['compose-check'],
          ),
          new UserParameters(
            sampleItem.get_user_parameters.body['seasons-modifier'],
            sampleItem.get_user_parameters.body.can_read_tabs === 'on',
            sampleItem.get_user_parameters.body.show_folders_tabs === 'on',
          ),
          new SettingsSetup(
            sampleItem.settings_setup.body.color_scheme,
            sampleItem.settings_setup.body.from_name,
            sampleItem.settings_setup.body.default_email,
            true,
            '>',
            sampleItem.settings_setup.body.mobile_sign,
            SignaturePlace.afterReply,
            [sampleItem.settings_setup.body.reply_to.item[0] as string],
            false,
          ),
        ),
      ),
    )
  })
  it('should be deserializable into Nulled object with bad status', () => {
    const response = settingsResponseFromJSONItem(
      JSONItemFromJSON({
        ...sampleItem,
        status: { status: 2 },
      }),
    )
    expect(response).not.toBeNull()
    expect(response!.networkStatus()).toStrictEqual(new NetworkStatus(NetworkStatusCode.temporaryError))
    expect(response!.payload).toBeNull()
  })
  it('should be null if wrong type of response', () => {
    const response = settingsResponseFromJSONItem(JSONItemFromJSON([sampleItem]))
    expect(response).toBeNull()
  })
  it('should deserialize signature top', () => {
    const copy = Object.assign({}, sampleItem)
    copy.settings_setup.body.signature_top = 'off'
    expect(settingsResponseFromJSONItem(JSONItemFromJSON(copy))).toStrictEqual(
      new SettingsResponse(
        new NetworkStatus(NetworkStatusCode.ok),
        new StatusResponsePayload(
          new AccountInformation(
            sampleItem.account_information['account-information'].uid,
            sampleItem.account_information['account-information'].suid,
            sampleItem.account_information['account-information'].emails.email.map(
              ({ login, domain }) => new Email(login, domain),
            ),
            sampleItem.account_information['account-information']['compose-check'],
          ),
          new UserParameters(
            sampleItem.get_user_parameters.body['seasons-modifier'],
            sampleItem.get_user_parameters.body.can_read_tabs === 'on',
            sampleItem.get_user_parameters.body.show_folders_tabs === 'on',
          ),
          new SettingsSetup(
            sampleItem.settings_setup.body.color_scheme,
            sampleItem.settings_setup.body.from_name,
            sampleItem.settings_setup.body.default_email,
            true,
            '>',
            sampleItem.settings_setup.body.mobile_sign,
            SignaturePlace.atEnd,
            [sampleItem.settings_setup.body.reply_to.item[0] as string],
            false,
          ),
        ),
      ),
    )

    delete copy.settings_setup.body.signature_top
    expect(settingsResponseFromJSONItem(JSONItemFromJSON(copy))).toStrictEqual(
      new SettingsResponse(
        new NetworkStatus(NetworkStatusCode.ok),
        new StatusResponsePayload(
          new AccountInformation(
            sampleItem.account_information['account-information'].uid,
            sampleItem.account_information['account-information'].suid,
            sampleItem.account_information['account-information'].emails.email.map(
              ({ login, domain }) => new Email(login, domain),
            ),
            sampleItem.account_information['account-information']['compose-check'],
          ),
          new UserParameters(
            sampleItem.get_user_parameters.body['seasons-modifier'],
            sampleItem.get_user_parameters.body.can_read_tabs === 'on',
            sampleItem.get_user_parameters.body.show_folders_tabs === 'on',
          ),
          new SettingsSetup(
            sampleItem.settings_setup.body.color_scheme,
            sampleItem.settings_setup.body.from_name,
            sampleItem.settings_setup.body.default_email,
            true,
            '>',
            sampleItem.settings_setup.body.mobile_sign,
            SignaturePlace.none,
            [sampleItem.settings_setup.body.reply_to.item[0] as string],
            false,
          ),
        ),
      ),
    )
  })
  it('should skip non-array "reply-to" value', () => {
    const copy = clone(sampleItem)
    copy.settings_setup.body.reply_to.item = { $t: '', checked: '' } as any
    expect(settingsResponseFromJSONItem(JSONItemFromJSON(copy))).toStrictEqual(
      new SettingsResponse(
        new NetworkStatus(NetworkStatusCode.ok),
        new StatusResponsePayload(
          new AccountInformation(
            sampleItem.account_information['account-information'].uid,
            sampleItem.account_information['account-information'].suid,
            sampleItem.account_information['account-information'].emails.email.map(
              ({ login, domain }) => new Email(login, domain),
            ),
            sampleItem.account_information['account-information']['compose-check'],
          ),
          new UserParameters(
            sampleItem.get_user_parameters.body['seasons-modifier'],
            sampleItem.get_user_parameters.body.can_read_tabs === 'on',
            sampleItem.get_user_parameters.body.show_folders_tabs === 'on',
          ),
          new SettingsSetup(
            sampleItem.settings_setup.body.color_scheme,
            sampleItem.settings_setup.body.from_name,
            sampleItem.settings_setup.body.default_email,
            true,
            '>',
            sampleItem.settings_setup.body.mobile_sign,
            SignaturePlace.none,
            [],
            false,
          ),
        ),
      ),
    )
  })
})
