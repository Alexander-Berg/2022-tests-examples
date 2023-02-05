import { YSError } from '../../../../../../common/ys'
import { mockLogger } from '../../../../../common/__tests__/__helpers__/mock-patches'
import { Log } from '../../../../../common/code/logging/logger'
import { reject, resolve } from '../../../../../../common/xpromise-support'
import { XPromise } from '../../../../../common/code/promise/xpromise'
import { getVoid } from '../../../../../common/code/result/result'
import { EntityKind } from '../../../../../mapi/code/api/entities/entity-kind'
import { Email } from '../../../../../mapi/code/api/entities/recipient/email'
import {
  AccountInformation,
  settingsResponseFromJSONItem,
  SettingsSetup,
  signaturePlaceToInt32,
  UserParameters,
} from '../../../../../mapi/code/api/entities/settings/settings-entities'
import { DefaultSettingsSaver } from '../../../../code/busilogics/settings/settings-saver'
import { Registry } from '../../../../code/registry'
import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { MockStorage } from '../../../__helpers__/mock-patches'
import { MockSharedPreferences } from '../../../../../common/__tests__/__helpers__/preferences-mock'
import { objectToMap, Writable } from '../../../../../common/__tests__/__helpers__/utils'

const testEntityObject = {
  status: {
    status: 1,
  },
  account_information: {
    'account-information': {
      uid: '12345',
      suid: '54321',
      emails: {
        email: [
          {
            login: 'sample',
            domain: 'ya.ru',
          },
          {
            login: 'sample',
            domain: 'yandex.com',
          },
        ],
      },
      'compose-check': 'c0mp0$3-Ch3ck',
    },
  },
  get_user_parameters: {
    body: {
      'seasons-modifier': '',
      mobile_open_from_web: 1,
      can_read_tabs: 'on',
    },
  },
  settings_setup: {
    body: {
      color_scheme: 'nemo',
      from_name: '',
      default_email: 'sample@yandex.ru',
      folder_thread_view: 'on',
      quotation_char: '>',
      mobile_sign: 'rty123123',
      signature_top: 'on',
      reply_to: {
        item: [
          'ald00@yandex.ru',
          {
            $t: '',
            checked: '',
          },
        ],
      },
    },
  },
}

describe(DefaultSettingsSaver, () => {
  describe('SharedPreferences saving', () => {
    it('should save new settings with SharedPreferences', () => {
      const prefs = new MockSharedPreferences()
      const saver = new DefaultSettingsSaver(
        prefs,
        MockStorage({
          withinTransaction: jest.fn().mockReturnValue(resolve(getVoid())),
          notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
        }),
        'DEFAULT SIGNATURE',
      )
      const entity = settingsResponseFromJSONItem(JSONItemFromJSON(testEntityObject))!
      expect.assertions(2)
      expect(saver.save(entity)).resolves.toBe(getVoid())

      const payload = entity.payload!
      expect(prefs.map).toStrictEqual(
        objectToMap({
          can_read_tabs: payload.userParameters.canReadTabs,
          compose_check: payload.accountInformation.composeCheck,
          suid: payload.accountInformation.suid,
          uid: payload.accountInformation.uid,
          default_email: payload.settingsSetup.defaultEmail,
          default_name: payload.settingsSetup.fromName,
          thread_mode: payload.settingsSetup.folderThreadView,
          push_notification_enabled: true,
          signature_place: signaturePlaceToInt32(payload.settingsSetup.signatureTop),
          theme_enabled: true,
          sync_enabled: true,
          quotation_char: payload.settingsSetup.quotationChar,
          is_synced: true,
          signature: payload.settingsSetup.mobileSign,
          use_default_signature: false,
        }),
      )
    })
    describe('themes', () => {
      it('should not extract theme is scheme is unsupported', async () => {
        const prefs = new MockSharedPreferences()
        const saver = new DefaultSettingsSaver(
          prefs,
          MockStorage({
            withinTransaction: jest.fn().mockReturnValue(resolve(getVoid())),
            notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
          }),
          'DEFAULT SIGNATURE',
        )
        const entity = settingsResponseFromJSONItem(JSONItemFromJSON(testEntityObject))!
        const settingsSetup: Writable<SettingsSetup> = entity.payload!.settingsSetup
        settingsSetup.colorScheme = 'unsupported'
        await saver.save(entity)
        expect(prefs.map.has('theme')).toBe(false)
      })
      it('should extract color scheme as theme if present among supported (non-seasons)', async () => {
        const prefs = new MockSharedPreferences()
        const saver = new DefaultSettingsSaver(
          prefs,
          MockStorage({
            withinTransaction: jest.fn().mockReturnValue(resolve(getVoid())),
            notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
          }),
          'DEFAULT SIGNATURE',
        )
        const entity = settingsResponseFromJSONItem(JSONItemFromJSON(testEntityObject))!
        const settingsSetup: Writable<SettingsSetup> = entity.payload!.settingsSetup
        settingsSetup.colorScheme = 'bears'
        await saver.save(entity)
        expect(prefs.map.get('theme')).toBe('bears')
      })
      it('should extract seasons theme if of type seasons', async () => {
        const prefs = new MockSharedPreferences()
        const saver = new DefaultSettingsSaver(
          prefs,
          MockStorage({
            withinTransaction: jest.fn().mockReturnValue(resolve(getVoid())),
            notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
          }),
          'DEFAULT SIGNATURE',
        )
        const entity = settingsResponseFromJSONItem(JSONItemFromJSON(testEntityObject))!
        const settingsSetup: Writable<SettingsSetup> = entity.payload!.settingsSetup
        settingsSetup.colorScheme = 'seasons'
        const userParameters: Writable<UserParameters> = entity.payload!.userParameters
        userParameters.seasonsModifier = 'autumn'
        await saver.save(entity)
        expect(prefs.map.get('theme')).toBe('seasons_autumn')
      })
      it('should return null if seasons modifier is unsupported', async () => {
        mockLogger()
        const prefs = new MockSharedPreferences()
        const saver = new DefaultSettingsSaver(
          prefs,
          MockStorage({
            withinTransaction: jest.fn().mockReturnValue(resolve(getVoid())),
            notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
          }),
          'DEFAULT SIGNATURE',
        )
        const entity = settingsResponseFromJSONItem(JSONItemFromJSON(testEntityObject))!
        const settingsSetup: Writable<SettingsSetup> = entity.payload!.settingsSetup
        settingsSetup.colorScheme = 'seasons'
        const userParameters: Writable<UserParameters> = entity.payload!.userParameters
        userParameters.seasonsModifier = 'unsupported'
        await saver.save(entity)
        expect(Log.getDefaultLogger()!.error).toBeCalledWith('Unexpected seasons theme modifier: unsupported')
        expect(prefs.map.has('theme')).toBe(false)
        Registry.drop()
      })
      it('should return winter theme for winter months', async () => {
        const dateSpy = jest.spyOn(Date, 'now').mockReturnValue(new Date(2019, 1, 1).valueOf())
        const prefs = new MockSharedPreferences()
        const saver = new DefaultSettingsSaver(
          prefs,
          MockStorage({
            withinTransaction: jest.fn().mockReturnValue(resolve(getVoid())),
            notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
          }),
          'DEFAULT SIGNATURE',
        )
        const entity = settingsResponseFromJSONItem(JSONItemFromJSON(testEntityObject))!
        const settingsSetup: Writable<SettingsSetup> = entity.payload!.settingsSetup
        settingsSetup.colorScheme = 'seasons'
        await saver.save(entity)
        expect(prefs.map.get('theme')).toBe('seasons_winter')
        dateSpy.mockRestore()
      })
      it('should return spring theme for spring months', async () => {
        const dateSpy = jest.spyOn(Date, 'now').mockReturnValue(new Date(2019, 4, 1).valueOf())
        const prefs = new MockSharedPreferences()
        const saver = new DefaultSettingsSaver(
          prefs,
          MockStorage({
            withinTransaction: jest.fn().mockReturnValue(resolve(getVoid())),
            notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
          }),
          'DEFAULT SIGNATURE',
        )
        const entity = settingsResponseFromJSONItem(JSONItemFromJSON(testEntityObject))!
        const settingsSetup: Writable<SettingsSetup> = entity.payload!.settingsSetup
        settingsSetup.colorScheme = 'seasons'
        await saver.save(entity)
        expect(prefs.map.get('theme')).toBe('seasons_spring')
        dateSpy.mockRestore()
      })
      it('should return summer theme for summer months', async () => {
        const dateSpy = jest.spyOn(Date, 'now').mockReturnValue(new Date(2019, 7, 1).valueOf())
        const prefs = new MockSharedPreferences()
        const saver = new DefaultSettingsSaver(
          prefs,
          MockStorage({
            withinTransaction: jest.fn().mockReturnValue(resolve(getVoid())),
            notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
          }),
          'DEFAULT SIGNATURE',
        )
        const entity = settingsResponseFromJSONItem(JSONItemFromJSON(testEntityObject))!
        const settingsSetup: Writable<SettingsSetup> = entity.payload!.settingsSetup
        settingsSetup.colorScheme = 'seasons'
        await saver.save(entity)
        expect(prefs.map.get('theme')).toBe('seasons_summer')
        dateSpy.mockRestore()
      })
      it('should return autumn theme for autumn months', async () => {
        const dateSpy = jest.spyOn(Date, 'now').mockReturnValue(new Date(2019, 10, 1).valueOf())
        const prefs = new MockSharedPreferences()
        const saver = new DefaultSettingsSaver(
          prefs,
          MockStorage({
            withinTransaction: jest.fn().mockReturnValue(resolve(getVoid())),
            notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
          }),
          'DEFAULT SIGNATURE',
        )
        const entity = settingsResponseFromJSONItem(JSONItemFromJSON(testEntityObject))!
        const settingsSetup: Writable<SettingsSetup> = entity.payload!.settingsSetup
        settingsSetup.colorScheme = 'seasons'
        await saver.save(entity)
        expect(prefs.map.get('theme')).toBe('seasons_autumn')
        dateSpy.mockRestore()
      })
      it('should return winter theme for december', async () => {
        const dateSpy = jest.spyOn(Date, 'now').mockReturnValue(new Date(2019, 11, 1).valueOf())
        const prefs = new MockSharedPreferences()
        const saver = new DefaultSettingsSaver(
          prefs,
          MockStorage({
            withinTransaction: jest.fn().mockReturnValue(resolve(getVoid())),
            notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
          }),
          'DEFAULT SIGNATURE',
        )
        const entity = settingsResponseFromJSONItem(JSONItemFromJSON(testEntityObject))!
        const settingsSetup: Writable<SettingsSetup> = entity.payload!.settingsSetup
        settingsSetup.colorScheme = 'seasons'
        await saver.save(entity)
        expect(prefs.map.get('theme')).toBe('seasons_winter')
        dateSpy.mockRestore()
      })
    })
    describe('signatures', () => {
      it('should save default signature if not synced before and new signature is not present', async () => {
        const prefs = new MockSharedPreferences()
        const saver = new DefaultSettingsSaver(
          prefs,
          MockStorage({
            withinTransaction: jest.fn().mockReturnValue(resolve(getVoid())),
            notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
          }),
          'DEFAULT SIGNATURE',
        )
        const entity = settingsResponseFromJSONItem(JSONItemFromJSON(testEntityObject))!
        const settingsSetup: Writable<SettingsSetup> = entity.payload!.settingsSetup
        settingsSetup.mobileSign = ''
        await saver.save(entity)
        expect(prefs.map.get('signature')).toBe('DEFAULT SIGNATURE')
        expect(prefs.map.get('use_default_signature')).toBe(true)
      })
      it('should save default signature if was synced before and new signature is not present', async () => {
        const prefs = new MockSharedPreferences(objectToMap({ is_synced: true }))
        const saver = new DefaultSettingsSaver(
          prefs,
          MockStorage({
            withinTransaction: jest.fn().mockReturnValue(resolve(getVoid())),
            notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
          }),
          'DEFAULT SIGNATURE',
        )
        const entity = settingsResponseFromJSONItem(JSONItemFromJSON(testEntityObject))!
        const settingsSetup: Writable<SettingsSetup> = entity.payload!.settingsSetup
        settingsSetup.mobileSign = ''
        await saver.save(entity)
        expect(prefs.map.get('signature')).toBe('DEFAULT SIGNATURE')
        expect(prefs.map.get('use_default_signature')).toBe(true)
      })
      // tslint:disable-next-line: max-line-length
      it('should save default signature if was synced before, new signature is present but default signature is preferrable', async () => {
        const prefs = new MockSharedPreferences(objectToMap({ is_synced: true, use_default_signature: true }))
        const saver = new DefaultSettingsSaver(
          prefs,
          MockStorage({
            withinTransaction: jest.fn().mockReturnValue(resolve(getVoid())),
            notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
          }),
          'DEFAULT SIGNATURE',
        )
        const entity = settingsResponseFromJSONItem(JSONItemFromJSON(testEntityObject))!
        const settingsSetup: Writable<SettingsSetup> = entity.payload!.settingsSetup
        settingsSetup.mobileSign = ''
        await saver.save(entity)
        expect(prefs.map.get('signature')).toBe('DEFAULT SIGNATURE')
        expect(prefs.map.get('use_default_signature')).toBe(true)
      })
      // tslint:disable-next-line: max-line-length
      it('should save new signature if was synced before, new signature is present but default signature is not preferrable', async () => {
        const prefs = new MockSharedPreferences(objectToMap({ is_synced: true, use_default_signature: false }))
        const saver = new DefaultSettingsSaver(
          prefs,
          MockStorage({
            withinTransaction: jest.fn().mockReturnValue(resolve(getVoid())),
            notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
          }),
          'DEFAULT SIGNATURE',
        )
        const entity = settingsResponseFromJSONItem(JSONItemFromJSON(testEntityObject))!
        const settingsSetup: Writable<SettingsSetup> = entity.payload!.settingsSetup
        settingsSetup.mobileSign = 'NEW SIGN'
        await saver.save(entity)
        expect(prefs.map.get('signature')).toBe('NEW SIGN')
        expect(prefs.map.get('use_default_signature')).toBe(false)
      })
      // tslint:disable-next-line: max-line-length
      it('should save new signature if was synced before, new signature is present', async () => {
        const prefs = new MockSharedPreferences(objectToMap({ is_synced: true, use_default_signature: true }))
        const saver = new DefaultSettingsSaver(
          prefs,
          MockStorage({
            withinTransaction: jest.fn().mockReturnValue(resolve(getVoid())),
            notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
          }),
          'DEFAULT SIGNATURE',
        )
        const entity = settingsResponseFromJSONItem(JSONItemFromJSON(testEntityObject))!
        const settingsSetup: Writable<SettingsSetup> = entity.payload!.settingsSetup
        settingsSetup.mobileSign = 'NEW SIGN'
        await saver.save(entity)
        expect(prefs.map.get('signature')).toBe('NEW SIGN')
        expect(prefs.map.get('use_default_signature')).toBe(false)
      })
    })
  })
  describe('saving to database', () => {
    it('should start within transaction', (done) => {
      const prefs = new MockSharedPreferences()
      const withinTransaction = jest.fn().mockReturnValue(reject(new YSError('NO MATTER')))
      const saver = new DefaultSettingsSaver(
        prefs,
        MockStorage({
          withinTransaction,
        }),
        'DEFAULT SIGNATURE',
      )
      const entity = settingsResponseFromJSONItem(JSONItemFromJSON(testEntityObject))!
      const accountInfo: Writable<AccountInformation> = entity.payload!.accountInformation
      accountInfo.emails = [new Email('first', 'yandex.ru'), new Email('second', 'yandex.ru')]
      const settingsSetup: Writable<SettingsSetup> = entity.payload!.settingsSetup
      settingsSetup.defaultEmail = 'third@yandex.ru'
      settingsSetup.replyTo = ['fourth@yandex.ru', 'fifth@yandex.ru']
      expect.assertions(1)
      saver.save(entity).failed((_) => {
        expect(withinTransaction).toBeCalledWith(true, expect.any(Function))
        done()
      })
    })
    it('should fail if transaction fails', (done) => {
      const prefs = new MockSharedPreferences()
      const withinTransaction = jest.fn().mockReturnValue(reject(new YSError('NO MATTER')))
      const saver = new DefaultSettingsSaver(
        prefs,
        MockStorage({
          withinTransaction,
        }),
        'DEFAULT SIGNATURE',
      )
      const entity = settingsResponseFromJSONItem(JSONItemFromJSON(testEntityObject))!
      const accountInfo: Writable<AccountInformation> = entity.payload!.accountInformation
      accountInfo.emails = [new Email('first', 'yandex.ru'), new Email('second', 'yandex.ru')]
      const settingsSetup: Writable<SettingsSetup> = entity.payload!.settingsSetup
      settingsSetup.defaultEmail = 'third@yandex.ru'
      settingsSetup.replyTo = ['fourth@yandex.ru', 'fifth@yandex.ru']
      expect.assertions(1)
      saver.save(entity).failed((e) => {
        expect(e).toStrictEqual(new YSError('NO MATTER'))
        done()
      })
    })
    it('should delete old emails before writing', (done) => {
      const prefs = new MockSharedPreferences()
      const runStatement = jest.fn().mockReturnValue(reject(new YSError('NO MATTER')))
      const saver = new DefaultSettingsSaver(
        prefs,
        MockStorage({
          withinTransaction: jest.fn<XPromise<any>, [boolean, any]>((_, block) => block()),
          runStatement,
        }),
        'DEFAULT SIGNATURE',
      )
      const entity = settingsResponseFromJSONItem(JSONItemFromJSON(testEntityObject))!
      const accountInfo: Writable<AccountInformation> = entity.payload!.accountInformation
      accountInfo.emails = [new Email('first', 'yandex.ru'), new Email('second', 'yandex.ru')]
      const settingsSetup: Writable<SettingsSetup> = entity.payload!.settingsSetup
      settingsSetup.defaultEmail = 'third@yandex.ru'
      settingsSetup.replyTo = ['fourth@yandex.ru', 'fifth@yandex.ru']
      expect.assertions(1)
      saver.save(entity).failed((_) => {
        expect(runStatement).toBeCalledWith(`DELETE FROM ${EntityKind.email};`)
        done()
      })
    })
    it('should fail if old emails deletion fails', (done) => {
      const prefs = new MockSharedPreferences()
      const runStatement = jest.fn().mockReturnValue(reject(new YSError('NO MATTER')))
      const saver = new DefaultSettingsSaver(
        prefs,
        MockStorage({
          withinTransaction: jest.fn<XPromise<any>, [boolean, any]>((_, block) => block()),
          runStatement,
        }),
        'DEFAULT SIGNATURE',
      )
      const entity = settingsResponseFromJSONItem(JSONItemFromJSON(testEntityObject))!
      const accountInfo: Writable<AccountInformation> = entity.payload!.accountInformation
      accountInfo.emails = [new Email('first', 'yandex.ru'), new Email('second', 'yandex.ru')]
      const settingsSetup: Writable<SettingsSetup> = entity.payload!.settingsSetup
      settingsSetup.defaultEmail = 'third@yandex.ru'
      settingsSetup.replyTo = ['fourth@yandex.ru', 'fifth@yandex.ru']
      expect.assertions(1)
      saver.save(entity).failed((e) => {
        expect(e).toStrictEqual(new YSError('NO MATTER'))
        done()
      })
    })
    it('should insert new emails if old emails deletion succeeds', (done) => {
      const prefs = new MockSharedPreferences()
      const runStatement = jest.fn().mockReturnValue(resolve(getVoid()))
      const prepareStatement = jest.fn().mockReturnValue(reject(new YSError('NO MATTER')))
      const saver = new DefaultSettingsSaver(
        prefs,
        MockStorage({
          withinTransaction: jest.fn<XPromise<any>, [boolean, any]>((_, block) => block()),
          runStatement,
          prepareStatement,
        }),
        'DEFAULT SIGNATURE',
      )
      const entity = settingsResponseFromJSONItem(JSONItemFromJSON(testEntityObject))!
      const accountInfo: Writable<AccountInformation> = entity.payload!.accountInformation
      accountInfo.emails = [new Email('first', 'yandex.ru'), new Email('second', 'yandex.ru')]
      const settingsSetup: Writable<SettingsSetup> = entity.payload!.settingsSetup
      settingsSetup.defaultEmail = 'third@yandex.ru'
      settingsSetup.replyTo = ['fourth@yandex.ru', 'fifth@yandex.ru']
      expect.assertions(1)
      saver.save(entity).failed((_) => {
        expect(prepareStatement).toBeCalledWith(`INSERT INTO ${EntityKind.email} (login, domain) VALUES (?, ?);`)
        done()
      })
    })
    it('should fail insertion statement preparation fails', (done) => {
      const prefs = new MockSharedPreferences()
      const runStatement = jest.fn().mockReturnValue(resolve(getVoid()))
      const prepareStatement = jest.fn().mockReturnValue(reject(new YSError('NO MATTER')))
      const saver = new DefaultSettingsSaver(
        prefs,
        MockStorage({
          withinTransaction: jest.fn<XPromise<any>, [boolean, any]>((_, block) => block()),
          runStatement,
          prepareStatement,
        }),
        'DEFAULT SIGNATURE',
      )
      const entity = settingsResponseFromJSONItem(JSONItemFromJSON(testEntityObject))!
      const accountInfo: Writable<AccountInformation> = entity.payload!.accountInformation
      accountInfo.emails = [new Email('first', 'yandex.ru'), new Email('second', 'yandex.ru')]
      const settingsSetup: Writable<SettingsSetup> = entity.payload!.settingsSetup
      settingsSetup.defaultEmail = 'third@yandex.ru'
      settingsSetup.replyTo = ['fourth@yandex.ru', 'fifth@yandex.ru']
      expect.assertions(1)
      saver.save(entity).failed((e) => {
        expect(e).toStrictEqual(new YSError('NO MATTER'))
        done()
      })
    })
    it('should store all emails with insertion statement (all specified)', async () => {
      const prefs = new MockSharedPreferences()

      const execute = jest.fn().mockReturnValue(resolve(getVoid()))
      const runStatement = jest.fn().mockReturnValue(resolve(getVoid()))
      const prepareStatement = jest.fn().mockReturnValue(
        resolve({
          execute,
          close: jest.fn(),
        }),
      )
      const saver = new DefaultSettingsSaver(
        prefs,
        MockStorage({
          withinTransaction: jest.fn<XPromise<any>, [boolean, any]>((_, block) => block()),
          runStatement,
          prepareStatement,
          notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
        }),
        'DEFAULT SIGNATURE',
      )
      const entity = settingsResponseFromJSONItem(JSONItemFromJSON(testEntityObject))!
      const accountInfo: Writable<AccountInformation> = entity.payload!.accountInformation
      accountInfo.emails = [new Email('first', 'yandex.ru'), new Email('second', 'yandex.ru')]
      const settingsSetup: Writable<SettingsSetup> = entity.payload!.settingsSetup
      settingsSetup.defaultEmail = 'third@yandex.ru'
      settingsSetup.replyTo = ['fourth@yandex.ru', 'fifth@yandex.ru']
      await saver.save(entity)
      expect(execute).toBeCalledTimes(5)
      expect(execute.mock.calls[0][0]).toStrictEqual(['first', 'yandex.ru'])
      expect(execute.mock.calls[1][0]).toStrictEqual(['second', 'yandex.ru'])
      expect(execute.mock.calls[2][0]).toStrictEqual(['third', 'yandex.ru'])
      expect(execute.mock.calls[3][0]).toStrictEqual(['fourth', 'yandex.ru'])
      expect(execute.mock.calls[4][0]).toStrictEqual(['fifth', 'yandex.ru'])
    })
    it('should store all emails with insertion statement (only default)', async () => {
      const prefs = new MockSharedPreferences()

      const execute = jest.fn().mockReturnValue(resolve(getVoid()))
      const runStatement = jest.fn().mockReturnValue(resolve(getVoid()))
      const prepareStatement = jest.fn().mockReturnValue(
        resolve({
          execute,
          close: jest.fn(),
        }),
      )
      const saver = new DefaultSettingsSaver(
        prefs,
        MockStorage({
          withinTransaction: jest.fn<XPromise<any>, [boolean, any]>((_, block) => block()),
          runStatement,
          prepareStatement,
          notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
        }),
        'DEFAULT SIGNATURE',
      )
      const entity = settingsResponseFromJSONItem(JSONItemFromJSON(testEntityObject))!
      const accountInfo: Writable<AccountInformation> = entity.payload!.accountInformation
      accountInfo.emails = []
      const settingsSetup: Writable<SettingsSetup> = entity.payload!.settingsSetup
      settingsSetup.defaultEmail = 'only@yandex.ru'
      settingsSetup.replyTo = []
      await saver.save(entity)
      expect(execute).toBeCalledTimes(1)
      expect(execute).toBeCalledWith(['only', 'yandex.ru'])
    })
    it('should skip incorrect emails when saving', async () => {
      const prefs = new MockSharedPreferences()

      const execute = jest.fn().mockReturnValue(resolve(getVoid()))
      const runStatement = jest.fn().mockReturnValue(resolve(getVoid()))
      const prepareStatement = jest.fn().mockReturnValue(
        resolve({
          execute,
          close: jest.fn(),
        }),
      )
      const saver = new DefaultSettingsSaver(
        prefs,
        MockStorage({
          withinTransaction: jest.fn<XPromise<any>, [boolean, any]>((_, block) => block()),
          runStatement,
          prepareStatement,
          notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
        }),
        'DEFAULT SIGNATURE',
      )
      const entity = settingsResponseFromJSONItem(JSONItemFromJSON(testEntityObject))!
      const accountInfo: Writable<AccountInformation> = entity.payload!.accountInformation
      accountInfo.emails = []
      const settingsSetup: Writable<SettingsSetup> = entity.payload!.settingsSetup
      settingsSetup.defaultEmail = 'incorrect'
      settingsSetup.replyTo = ['correct@yandex.ru', 'bad']
      await saver.save(entity)
      expect(execute).toBeCalledTimes(1)
      expect(execute).toBeCalledWith(['correct', 'yandex.ru'])
    })
    it('should call close if successfull', async () => {
      const prefs = new MockSharedPreferences()
      const execute = jest.fn().mockReturnValue(resolve(getVoid()))
      const runStatement = jest.fn().mockReturnValue(resolve(getVoid()))
      const close = jest.fn().mockReturnValue(resolve(getVoid()))
      const prepareStatement = jest.fn().mockReturnValue(
        resolve({
          execute,
          close,
        }),
      )
      const saver = new DefaultSettingsSaver(
        prefs,
        MockStorage({
          withinTransaction: jest.fn<XPromise<any>, [boolean, any]>((_, block) => block()),
          runStatement,
          prepareStatement,
          notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
        }),
        'DEFAULT SIGNATURE',
      )
      const entity = settingsResponseFromJSONItem(JSONItemFromJSON(testEntityObject))!
      const accountInfo: Writable<AccountInformation> = entity.payload!.accountInformation
      accountInfo.emails = [new Email('first', 'yandex.ru'), new Email('second', 'yandex.ru')]
      const settingsSetup: Writable<SettingsSetup> = entity.payload!.settingsSetup
      settingsSetup.defaultEmail = 'third@yandex.ru'
      settingsSetup.replyTo = ['fourth@yandex.ru', 'fifth@yandex.ru']
      await saver.save(entity)
      expect(close).toBeCalled()
    })
    it('should notify about changes', async () => {
      const prefs = new MockSharedPreferences()
      const execute = jest.fn().mockReturnValue(resolve(getVoid()))
      const runStatement = jest.fn().mockReturnValue(resolve(getVoid()))
      const close = jest.fn().mockReturnValue(resolve(getVoid()))
      const prepareStatement = jest.fn().mockReturnValue(
        resolve({
          execute,
          close,
        }),
      )
      const storage = MockStorage({
        withinTransaction: jest.fn<XPromise<any>, [boolean, any]>((_, block) => block()),
        runStatement,
        prepareStatement,
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const saver = new DefaultSettingsSaver(prefs, storage, 'DEFAULT SIGNATURE')
      const entity = settingsResponseFromJSONItem(JSONItemFromJSON(testEntityObject))!
      const accountInfo: Writable<AccountInformation> = entity.payload!.accountInformation
      accountInfo.emails = [new Email('first', 'yandex.ru'), new Email('second', 'yandex.ru')]
      const settingsSetup: Writable<SettingsSetup> = entity.payload!.settingsSetup
      settingsSetup.defaultEmail = 'third@yandex.ru'
      settingsSetup.replyTo = ['fourth@yandex.ru', 'fifth@yandex.ru']
      await saver.save(entity)
      expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.email])
    })
    it('should call close if failed', (done) => {
      const prefs = new MockSharedPreferences()
      const execute = jest
        .fn()
        .mockReturnValueOnce(resolve(getVoid()))
        .mockReturnValueOnce(reject(new YSError('EXECUTE ERROR')))
        .mockReturnValue(resolve(getVoid()))
      const runStatement = jest.fn().mockReturnValue(resolve(getVoid()))
      const close = jest.fn().mockReturnValue(resolve(getVoid()))
      const prepareStatement = jest.fn().mockReturnValue(
        resolve({
          execute,
          close,
        }),
      )
      const saver = new DefaultSettingsSaver(
        prefs,
        MockStorage({
          withinTransaction: jest.fn<XPromise<any>, [boolean, any]>((_, block) => block()),
          runStatement,
          prepareStatement,
        }),
        'DEFAULT SIGNATURE',
      )
      const entity = settingsResponseFromJSONItem(JSONItemFromJSON(testEntityObject))!
      const accountInfo: Writable<AccountInformation> = entity.payload!.accountInformation
      accountInfo.emails = [new Email('first', 'yandex.ru'), new Email('second', 'yandex.ru')]
      const settingsSetup: Writable<SettingsSetup> = entity.payload!.settingsSetup
      settingsSetup.defaultEmail = 'third@yandex.ru'
      settingsSetup.replyTo = ['fourth@yandex.ru', 'fifth@yandex.ru']
      expect.assertions(1)
      saver.save(entity).failed((_) => {
        expect(close).toBeCalled()
        done()
      })
    })
    it('should fail if any of executions fails', (done) => {
      const prefs = new MockSharedPreferences()
      const execute = jest
        .fn()
        .mockReturnValueOnce(resolve(getVoid()))
        .mockReturnValueOnce(resolve(getVoid()))
        .mockReturnValueOnce(reject(new YSError('EXECUTE ERROR')))
        .mockReturnValue(resolve(getVoid()))
      const runStatement = jest.fn().mockReturnValue(resolve(getVoid()))
      const close = jest.fn().mockReturnValue(resolve(getVoid()))
      const prepareStatement = jest.fn().mockReturnValue(
        resolve({
          execute,
          close,
        }),
      )
      const saver = new DefaultSettingsSaver(
        prefs,
        MockStorage({
          withinTransaction: jest.fn<XPromise<any>, [boolean, any]>((_, block) => block()),
          runStatement,
          prepareStatement,
        }),
        'DEFAULT SIGNATURE',
      )
      const entity = settingsResponseFromJSONItem(JSONItemFromJSON(testEntityObject))!
      const accountInfo: Writable<AccountInformation> = entity.payload!.accountInformation
      accountInfo.emails = [new Email('first', 'yandex.ru'), new Email('second', 'yandex.ru')]
      const settingsSetup: Writable<SettingsSetup> = entity.payload!.settingsSetup
      settingsSetup.defaultEmail = 'third@yandex.ru'
      settingsSetup.replyTo = ['fourth@yandex.ru', 'fifth@yandex.ru']
      expect.assertions(1)
      saver.save(entity).failed((e) => {
        expect(e).toStrictEqual(new YSError('EXECUTE ERROR'))
        done()
      })
    })
  })
})
