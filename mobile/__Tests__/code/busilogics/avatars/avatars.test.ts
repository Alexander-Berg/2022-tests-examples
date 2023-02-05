import { resolve } from '../../../../../../common/xpromise-support'
import { MockNetwork } from '../../../../../common/__tests__/__helpers__/mock-patches'
import { Ava, Avatar, AvaType } from '../../../../../mapi/code/api/entities/avatars/avatar'
import { Avatars } from '../../../../code/busilogics/avatars/avatars'
import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { clone } from '../../../../../common/__tests__/__helpers__/utils'
import sample from '../../../../../mapi/__tests__/code/api/entities/avatars/sample.json'

describe(Avatars, () => {
  describe('fetchtURL', () => {
    describe('corporate account', () => {
      it('should build URL for account', (done) => {
        const network = MockNetwork({
          resolveURL: jest.fn().mockReturnValue('RESOLVED'),
        })
        const avatars = new Avatars(network)
        avatars.fetchtURL('comfly@yandex-team.ru', '').then((res) => {
          expect(res.first).toBe('RESOLVED')
          expect(res.second).toBeNull()
          done()
        })
      })
      it('should fail if url cannot be built for email', (done) => {
        const network = MockNetwork({
          resolveURL: jest.fn().mockReturnValue(null),
        })
        const avatars = new Avatars(network)
        avatars.fetchtURL('comfly@yandex-team.ru', '').failed((e) => {
          expect(e.message).toBe('No avatar URL for email: comfly@yandex-team.ru')
          done()
        })
      })
    })
    describe('non-corporate account', () => {
      it('should build URL for account', (done) => {
        const network = MockNetwork({
          execute: jest.fn().mockReturnValue(resolve(JSONItemFromJSON(sample))),
        })
        const avatars = new Avatars(network)
        avatars.fetchtURL('dzcomfly@yandex.ru', 'Dmitry Zakharov').then((res) => {
          expect(res.first).toBe(
            'https://avatars.mds.yandex.net/get-yapic/61207/LTPrMwv9ZFXNby39TL4a4r8hCk-1/islands-200',
          )
          expect(res.second).toStrictEqual(
            new Avatar(
              'dzcomfly',
              null,
              'dzcomfly@yandex.ru',
              'Dmitry Zakharov',
              'DZ',
              '#f39718',
              new Ava(
                AvaType.avatar,
                'https://avatars.mds.yandex.net/get-yapic/61207/LTPrMwv9ZFXNby39TL4a4r8hCk-1/islands-200',
              ),
              true,
              true,
            ),
          )
          done()
        })
      })
      it('should fail if response is malformed', (done) => {
        const network = MockNetwork({
          execute: jest.fn().mockReturnValue(resolve(JSONItemFromJSON([sample]))),
        })
        const avatars = new Avatars(network)
        avatars.fetchtURL('dzcomfly@yandex.ru', '').failed((e) => {
          expect(e.message).toBe('JSON Item parsing failed for entity AvatarResponse')
          done()
        })
      })
      it('should fail if response is missing avatar URLs', (done) => {
        const copy = clone(sample)
        delete copy.catdog
        const network = MockNetwork({
          execute: jest.fn().mockReturnValue(resolve(JSONItemFromJSON(copy))),
        })
        const avatars = new Avatars(network)
        avatars.fetchtURL('dzcomfly@yandex.ru', '').failed((e) => {
          expect(e.message).toBe('No avatar URL for email: dzcomfly@yandex.ru')
          done()
        })
      })
      it('should fail if response signifies failure', (done) => {
        const copy = clone(sample)
        copy.status.status = 3
        const network = MockNetwork({
          execute: jest.fn().mockReturnValue(resolve(JSONItemFromJSON(copy))),
        })
        const avatars = new Avatars(network)
        avatars.fetchtURL('dzcomfly@yandex.ru', '').failed((e) => {
          expect(e.message).toBe('PERM error; Phrase = ; Trace = ;')
          done()
        })
      })
      it('should fail if it has no entry with requested email in response', (done) => {
        const copy = clone(sample)
        delete copy.catdog['dzcomfly@yandex.ru']
        const network = MockNetwork({
          execute: jest.fn().mockReturnValue(resolve(JSONItemFromJSON(copy))),
        })
        const avatars = new Avatars(network)
        avatars.fetchtURL('dzcomfly@yandex.ru', '').failed((e) => {
          expect(e.message).toBe('No avatar URL for email: dzcomfly@yandex.ru')
          done()
        })
      })
      it('should fail if it has empty entry with requested email in response', (done) => {
        const copy = clone(sample)
        copy.catdog['dzcomfly@yandex.ru'] = []
        const network = MockNetwork({
          execute: jest.fn().mockReturnValue(resolve(JSONItemFromJSON(copy))),
        })
        const avatars = new Avatars(network)
        avatars.fetchtURL('dzcomfly@yandex.ru', '').failed((e) => {
          expect(e.message).toBe('No avatar URL for email: dzcomfly@yandex.ru')
          done()
        })
      })
      it('should return null in url part if no ava.url found in response entry', (done) => {
        const copy = clone(sample)
        delete copy.catdog['dzcomfly@yandex.ru'][0].ava
        const network = MockNetwork({
          execute: jest.fn().mockReturnValue(resolve(JSONItemFromJSON(copy))),
        })
        const avatars = new Avatars(network)
        avatars.fetchtURL('dzcomfly@yandex.ru', '').then((res) => {
          expect(res.first).toBeNull()
          expect(res.second).toStrictEqual(
            new Avatar('dzcomfly', null, 'dzcomfly@yandex.ru', 'Dmitry Zakharov', 'DZ', '#f39718', null, true, true),
          )
          done()
        })
      })
    })
  })
})
