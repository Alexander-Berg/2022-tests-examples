import { EventReporter } from '../../code/event-reporter'
import { LoggingEvent } from '../../code/eventus-event'
import { Nullable } from '../../../../common/ys'

export class MockReporter implements EventReporter {
  public lastEvent: Nullable<LoggingEvent> = null

  public report(event: LoggingEvent): void {
    this.lastEvent = event
  }
}
