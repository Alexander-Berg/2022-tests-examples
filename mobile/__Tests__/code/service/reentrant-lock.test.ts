import { resolve } from '../../../../../common/xpromise-support'
import { XPromise } from '../../../../common/code/promise/xpromise'
import { getVoid } from '../../../../common/code/result/result'
import { Registry } from '../../../../xmail/code/registry'
import { ILock, ReentrantLock } from '../../../code/service/reentrant-lock'

class MockedLock implements ILock {
  public executeInLock(toWrap: () => void): XPromise<void> {
    toWrap()
    return resolve<void>(getVoid())
  }
}

describe(ReentrantLock, () => {
  it('should execute code block with lock', () => {
    jest.mock('../../../code/registry')
    const mockLock = new MockedLock()
    const mockLocateFunction = jest.fn().mockReturnValue({
      locate: jest.fn().mockReturnValue(mockLock),
    })
    const spyExecuteInLock = jest.spyOn(mockLock, 'executeInLock')
    Registry.getServiceLocator = mockLocateFunction.bind(Registry)
    const reentrantLock = new ReentrantLock()
    reentrantLock.executeInLock(() => {
      return getVoid()
    })
    expect(spyExecuteInLock).toBeCalledTimes(1)
  })
})
