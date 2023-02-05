import { mockLogger } from '../../../../common/__tests__/__helpers__/mock-patches'
import { Log } from '../../../../common/code/logging/logger'
import { Rfc822Tokenizer, IRfc822Tokenizer, Rfc822Token } from '../../../code/service/rfc822-tokenizer'
import { Registry } from '../../../code/registry'
import { ServiceLocatorItems } from '../../../code/utils/service-locator'

describe(Rfc822Tokenizer, () => {
  describe(Rfc822Tokenizer.tokenize, () => {
    const tokenizer: IRfc822Tokenizer = {
      tokenize: jest.fn((arg) => [new Rfc822Token(`NAME ${arg}`, `EMAIL ${arg}`, `COMMENT ${arg}`)]),
    }
    beforeAll(() => {
      Registry.registerServiceLocatorItems(new Map([[ServiceLocatorItems.rfc822Tokenizer, () => tokenizer]]))
    })
    afterAll(() => Registry.drop())
    it('should call native IRfc822Tokenizer.tokenize if tokenize is called', () => {
      const result = Rfc822Tokenizer.tokenize('a@b.com')
      expect(result).toStrictEqual([new Rfc822Token('NAME a@b.com', 'EMAIL a@b.com', 'COMMENT a@b.com')])
      expect(tokenizer.tokenize).toBeCalledWith('a@b.com')
    })
  })
  describe(Rfc822Tokenizer.tokenize, () => {
    beforeAll(() => {
      Registry.registerServiceLocatorItems(new Map([]))
      mockLogger()
    })
    afterAll(() => Registry.drop())
    it('should throw if IRfc822Tokenizer is not registered with service locator', () => {
      expect(() => {
        Rfc822Tokenizer.tokenize('a@b.com')
      }).toThrowError('rfc822Tokenizer must be registered before use')
      expect(Log.getDefaultLogger()!.warn).toBeCalledWith(
        'Unregistered Service Locator Item requested: rfc822Tokenizer',
      )
    })
  })
})
