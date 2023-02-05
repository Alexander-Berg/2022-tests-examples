/* eslint-disable @typescript-eslint/unbound-method */
import { Log } from '../../../code/logging/logger'
import { MockLogger } from '../../__helpers__/mock-patches'

describe(Log, () => {
  afterEach(() => Log.reset())

  it('should log', () => {
    const nonDefaultLogger = MockLogger()
    Log.registerLogger('non-default', nonDefaultLogger)

    Log.info('info')
    expect(nonDefaultLogger.info).not.toBeCalled()

    Log.warn('warn')
    expect(nonDefaultLogger.warn).not.toBeCalled()

    Log.error('error')
    expect(nonDefaultLogger.error).not.toBeCalled()

    const defaultLogger = MockLogger()
    Log.registerDefaultLogger(defaultLogger)

    Log.info('info')
    expect(defaultLogger.info).toBeCalledWith('info')

    Log.warn('warn')
    expect(defaultLogger.warn).toBeCalledWith('warn')

    Log.error('error')
    expect(defaultLogger.error).toBeCalledWith('error')
  })
})
