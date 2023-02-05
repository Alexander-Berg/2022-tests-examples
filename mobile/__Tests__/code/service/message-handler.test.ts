import { resolve } from '../../../../../common/xpromise-support'
import { Int32 } from '../../../../../common/ys'
import { XPromise } from '../../../../common/code/promise/xpromise'
import { getVoid } from '../../../../common/code/result/result'
import { Registry } from '../../../../xmail/code/registry'
import { IHandler, MessageHandler } from '../../../code/service/message-handler'

class MockMessageHandler implements IHandler {
  private isDestroyedState = false
  public post(toExecute: () => void): XPromise<void> {
    toExecute()
    return resolve<void>(getVoid())
  }
  public hasMessages(messageType: Int32): boolean {
    return false
  }
  public destroy(): void {
    this.isDestroyedState = true
  }

  public isDestroyed(): boolean {
    return this.isDestroyedState
  }
}

describe(MessageHandler, () => {
  it('should be creatable', () => {
    jest.mock('../../../code/registry')
    const mockHandler = new MockMessageHandler()
    const mockLocateFunction = jest.fn().mockReturnValue({
      locate: jest.fn().mockReturnValue(mockHandler),
    })
    Registry.getServiceLocator = mockLocateFunction.bind(Registry)
    const mockPost = jest.spyOn(mockHandler, 'post')
    const messageHandler = new MessageHandler('someName')
    messageHandler.post(() => {
      return getVoid()
    })
    expect(mockPost).toBeCalledTimes(1)
    expect(messageHandler.hasMessages(0)).toBe(false)
    messageHandler.destroy()
    expect(mockHandler.isDestroyed()).toBe(true)
  })
})
