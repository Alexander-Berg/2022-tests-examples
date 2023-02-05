import { Registry } from '../../../code/registry'
import { IMimeTypeMap, MimeTypeMap } from '../../../code/service/mime-type-map'
import { ServiceLocatorItems } from '../../../code/utils/service-locator'

describe(MimeTypeMap, () => {
  const mimeTypeMapStub: IMimeTypeMap = {
    getMimeType: jest.fn().mockReturnValue('image'),
  }
  beforeAll(() => {
    Registry.registerServiceLocatorItems(
      new Map<ServiceLocatorItems.mimeTypeMap, () => any>([[ServiceLocatorItems.mimeTypeMap, () => mimeTypeMapStub]]),
    )
  })
  it('should forward "getMimeType" method call', () => {
    const result = MimeTypeMap.getMimeType('path')
    expect(result).toBe('image')
  })
})
