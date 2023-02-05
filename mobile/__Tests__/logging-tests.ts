import * as assert from 'assert'
import { Nullable } from '../../../common/ys'
import { EventReporter } from '../../eventus-common/code/event-reporter'
import { Eventus } from '../code/events/eventus'
import { LoggingEvent } from '../../eventus-common/code/eventus-event'
import { EventusRegistry } from '../../eventus-common/code/eventus-registry'

class MockReporter implements EventReporter {
  public lastEvent: Nullable<LoggingEvent> = null

  public report(event: LoggingEvent): void {
    this.lastEvent = event
  }
}

describe('Testopithecus events', () => {
  it('should log timestamp in ms', (done) => {
    const currentTimeInMs = Date.now()
    const reporter = new MockReporter()
    Eventus.startEvents.startWithMessageListShow().reportVia(reporter)
    const timestamp = reporter.lastEvent!.attributes.get('timestamp')
    assert.ok(timestamp >= currentTimeInMs)
    done()
  })

  it('should log version', (done) => {
    const reporter = new MockReporter()
    Eventus.startEvents.startWithMessageListShow().reportVia(reporter)
    const timestamp = reporter.lastEvent!.attributes.get('version')
    assert.ok(timestamp === EventusRegistry.version)
    done()
  })
})
