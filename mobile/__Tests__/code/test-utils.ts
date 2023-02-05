import { ConsoleLog } from '../../../common/__tests__/__helpers__/console-log'

export function onProcessStart(): void {
  // eslint-disable-next-line
  require('log-timestamp')

  process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0'
  process.on('unhandledRejection', (e) => {
    throw e
  })
  printEnv()
  ConsoleLog.setup()
}

export function printEnv(): void {
  // eslint-disable-next-line
  for (const k in process.env) {
    console.log(`${k}=${process.env[k]}`)
  }
}
