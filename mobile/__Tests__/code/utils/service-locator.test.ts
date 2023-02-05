import { mockLogger } from '../../../../common/__tests__/__helpers__/mock-patches'
import { Log } from '../../../../common/code/logging/logger'
import { Registry } from '../../../code/registry'
import { ServiceLocator, ServiceLocatorItems } from '../../../code/utils/service-locator'

describe(ServiceLocator, () => {
  beforeAll(() => mockLogger())
  afterAll(Registry.drop)

  it('should locate by ServiceLocatorItem if registered', () => {
    const item = {}
    const locator = new ServiceLocator(
      new Map<ServiceLocatorItems, () => any>([[ServiceLocatorItems.blockingDeque, () => item]]),
    )
    expect(locator.locate(ServiceLocatorItems.blockingDeque)).toBe(item)
  })
  it('should return null if the item is not registered with ServiceLocator', () => {
    const locator = new ServiceLocator(new Map<ServiceLocatorItems, () => any>())
    expect(locator.locate(ServiceLocatorItems.blockingDeque)).toBeNull()
    expect(Log.getDefaultLogger()!.warn).toBeCalledWith(
      `Unregistered Service Locator Item requested: ${ServiceLocatorItems.blockingDeque}`,
    )
  })
})
