import { undefinedToNull } from '../../../../common/ys'
import { Registry } from '../../code/registry'
import { ILinkedBlockingDeque, LinkedBlockingDeque } from '../../code/service/linked-blocking-deque'
import { ServiceLocatorItems } from '../../code/utils/service-locator'

export type TestLinkedBlockingDeque = ILinkedBlockingDeque & { _items: File[] }

export function registerLinkedBlockingDeque() {
  const deque: TestLinkedBlockingDeque = {
    _items: [],

    contains: jest.fn().mockImplementation((item, isEqual) => Boolean(deque._items.find((i) => isEqual(i, item)))),
    isEmpty: jest.fn().mockImplementation(() => deque._items.length === 0),
    offer: jest.fn().mockImplementation((item) => {
      deque._items.push(item)
      return true
    }),
    peek: jest.fn().mockImplementation(() => undefinedToNull(deque._items[0])),
    poll: jest.fn().mockImplementation(() => undefinedToNull(deque._items.shift())),
    clear: jest.fn().mockImplementation(() => (deque._items = [])),
    offerFirst: jest.fn().mockImplementation((item) => deque._items.unshift(item)),
    toArray: jest.fn().mockImplementation(() => deque._items),
  }
  Registry.registerServiceLocatorItems(new Map([[ServiceLocatorItems.blockingDeque, () => deque]]))
}

export function unregisterLinkedBlockingDeque() {
  ;(Registry.getServiceLocator().locate(ServiceLocatorItems.blockingDeque) as LinkedBlockingDeque<File>).clear()
}
