import { Logger, Log } from '../../code/logging/logger'

export class ConsoleLog implements Logger {
  public static LOGGER = new ConsoleLog()

  private constructor() {}

  public info(message: string): void {
    console.log(message)
  }

  public warn(message: string): void {
    console.warn(message)
  }

  public error(message: string): void {
    console.error(message)
  }

  public static setup(): void {
    Log.registerDefaultLogger(ConsoleLog.LOGGER)
  }
}
