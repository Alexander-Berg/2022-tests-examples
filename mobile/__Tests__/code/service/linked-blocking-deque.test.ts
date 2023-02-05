import { Int32 } from '../../../../../common/ys'
import { getVoid } from '../../../../common/code/result/result'
import { Registry } from '../../../code/registry'
import { ILinkedBlockingDeque, LinkedBlockingDeque } from '../../../code/service/linked-blocking-deque'
import { ServiceLocatorItems } from '../../../code/utils/service-locator'

describe(LinkedBlockingDeque, () => {
  const serviceLocatorStub: ILinkedBlockingDeque = {
    contains: jest.fn<any, [any, (l: any, r: any) => boolean]>((item, f) => f(10, item)),
    isEmpty: jest.fn().mockReturnValue(true),
    offer: jest.fn().mockReturnValue(true),
    peek: jest.fn().mockReturnValueOnce(10).mockReturnValueOnce(null),
    poll: jest.fn().mockReturnValueOnce(10).mockReturnValueOnce(null),
    clear: jest.fn().mockReturnValue(getVoid()),
    offerFirst: jest.fn().mockReturnValue(true),
    toArray: jest.fn().mockReturnValue([1, 2, 3]),
  }
  beforeAll(() => {
    Registry.registerServiceLocatorItems(
      new Map<ServiceLocatorItems.blockingDeque, () => any>([
        [ServiceLocatorItems.blockingDeque, () => serviceLocatorStub],
      ]),
    )
  })
  it("should forward 'contains' method call", () => {
    const queue = new LinkedBlockingDeque<Int32>()
    expect(queue.contains(10, (l, r) => l === r)).toBe(true)
    expect(serviceLocatorStub.contains).toBeCalledWith(10, expect.any(Function))
    const predicate = jest.fn<boolean, [any, any]>((l, r) => l === r)
    expect(queue.contains(10, predicate)).toBe(true)
    expect(serviceLocatorStub.contains).toBeCalledWith(10, expect.any(Function))
    expect(predicate).toBeCalledWith(10, 10)
  })
  it("should forward 'isEmpty' method call", () => {
    const queue = new LinkedBlockingDeque<Int32>()
    expect(queue.isEmpty()).toBe(true)
    expect(serviceLocatorStub.isEmpty).toBeCalled()
  })
  it("should forward 'offer' method call", () => {
    const queue = new LinkedBlockingDeque<Int32>()
    expect(queue.offer(10)).toBe(true)
    expect(serviceLocatorStub.offer).toBeCalledWith(10)
  })
  it("should forward 'peek' method call", () => {
    const queue = new LinkedBlockingDeque<Int32>()
    expect(queue.peek()).toBe(10)
    expect(queue.peek()).toBeNull()
    expect(serviceLocatorStub.peek).toBeCalledTimes(2)
    expect(serviceLocatorStub.peek).toBeCalledWith()
    expect(serviceLocatorStub.peek).toBeCalledWith()
  })
  it("should forward 'poll' method call", () => {
    const queue = new LinkedBlockingDeque<Int32>()
    expect(queue.poll()).toBe(10)
    expect(queue.poll()).toBeNull()
    expect(serviceLocatorStub.poll).toBeCalledTimes(2)
    expect(serviceLocatorStub.poll).toBeCalledWith()
    expect(serviceLocatorStub.poll).toBeCalledWith()
  })
  it("should forward 'clear' method call", () => {
    const queue = new LinkedBlockingDeque<Int32>()
    queue.clear()
    expect(serviceLocatorStub.clear).toBeCalled()
  })
  it("should forward 'offerFirst' method call", () => {
    const queue = new LinkedBlockingDeque<Int32>()
    expect(queue.offerFirst(10)).toBe(true)
    expect(serviceLocatorStub.offerFirst).toBeCalledWith(10)
  })
})
