import { EventReporter } from '../../eventus-common/code/event-reporter'
import { LoggingEvent } from '../../eventus-common/code/eventus-event'
import { EventusRegistry } from '../../eventus-common/code/eventus-registry'

export class TestStackReporter implements EventReporter {

  public events: LoggingEvent[] = []

  public report(event: LoggingEvent): void {
    this.events.push(event)
  }

}

export function initStackReporterInRegistry(): TestStackReporter {
  const reporter = new TestStackReporter()
  EventusRegistry.setEventReporter(reporter)
  return reporter
}
