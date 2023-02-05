import { MockFileSystem } from '../../../../../common/__tests__/__helpers__/mock-patches'
import { MimeType } from '../../../../code/busilogics/mimetype/mime-type'
import { Registry } from '../../../../code/registry'

describe(MimeType, () => {
  beforeAll(() => Registry.registerFileSystem(MockFileSystem()))
  afterAll(Registry.drop)
  describe('fromFileName', () => {
    it("should return null if extension doesn't match any known type", () => {
      expect(MimeType.fromFileName('hello')).toBeNull()
      expect(MimeType.fromFileName('file.hello')).toBeNull()
    })
    it('should return proper mime type if extension is of known type', () => {
      expect(MimeType.fromFileName('file.png')).toStrictEqual(new MimeType('image', 'png'))
    })
  })
  describe('fromString', () => {
    it('should return null subtype if mimetype has no second part', () => {
      expect(MimeType.fromString('image')).toStrictEqual(new MimeType('image', null))
    })
    it('should return empty type and null subtype on empty string', () => {
      expect(MimeType.fromString('')).toStrictEqual(new MimeType('', null))
    })
    it('should return proper mime type from type and subtype string', () => {
      expect(MimeType.fromString('image/png')).toStrictEqual(new MimeType('image', 'png'))
    })
  })
  describe('fullType', () => {
    it('should return full mime type separated with "/" if both type and subtype are present', () => {
      expect(new MimeType('first', 'second').fullType()).toBe('first/second')
    })
    it('should return only type mime type if only type part is present', () => {
      expect(new MimeType('first', null).fullType()).toBe('first')
      expect(new MimeType('first', '').fullType()).toBe('first')
    })
  })
})
