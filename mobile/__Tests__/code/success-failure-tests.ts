import { MockReporter } from '../__helpers__/mock-reporter'
import { EventusConstants, EventusEvent } from '../../code/eventus-event'
import { ValueMapBuilder } from '../../code/value-map-builder'
import { resolve, take, reject } from '../../../../common/xpromise-support'
import { EventusRegistry } from '../../code/eventus-registry'

describe('Testopithecus events', () => {
  afterEach(jest.restoreAllMocks)

  it('should connect eventus id', () => {
    const reporter = new MockReporter()
    const event = EventusEvent.newClientEvent('sample_event', ValueMapBuilder.userEvent())
    const eventId = event.getInt64(EventusConstants.EVENTUS_ID)
    event.reportVia(reporter)
    event.success().reportVia(reporter)
    expect(reporter.lastEvent!.name).toMatch(/.*sample_event_success$/)
    expect(reporter.lastEvent!.attributes.get(EventusConstants.ORIGIN_EVENTUS_ID)).toBe(eventId)
    expect(reporter.lastEvent!.attributes.get(EventusConstants.EVENTUS_ID)).not.toBe(eventId)
  })

  it('should trace promise success execution', async () => {
    const reportSpy = jest.spyOn(EventusRegistry.eventReporter(), 'report')
    const event = EventusEvent.newClientEvent('sample_event', ValueMapBuilder.userEvent().addDebug())

    await take(event.traceExecution(resolve('value')))

    expect(reportSpy).toBeCalledTimes(2)

    const startEvent = reportSpy.mock.calls[0][0]
    expect(startEvent!.name).toMatch(/.*sample_event$/)

    const lastEvent = reportSpy.mock.calls[1][0]
    expect(lastEvent!.name).toMatch(/.*sample_event_success$/)
    expect(lastEvent!.attributes.get('timespan')).not.toBeUndefined()
    expect(lastEvent!.attributes.get('debug')).toBe(true)
  })

  it('should trace promise failure execution', async () => {
    const reportSpy = jest.spyOn(EventusRegistry.eventReporter(), 'report')
    const event = EventusEvent.newClientEvent('sample_event', ValueMapBuilder.userEvent().addDebug())

    await take(event.traceExecution(reject({ message: 'ERROR' }))).catch(() => {})

    expect(reportSpy).toBeCalledTimes(2)

    const startEvent = reportSpy.mock.calls[0][0]
    expect(startEvent!.name).toMatch(/.*sample_event$/)

    const lastEvent = reportSpy.mock.calls[1][0]
    expect(lastEvent!.name).toMatch(/.*sample_event_failure$/)
    expect(lastEvent!.attributes.get('timespan')).not.toBeUndefined()
    expect(lastEvent!.attributes.get('debug')).toBe(true)
    expect(lastEvent!.attributes.get('error')).toBe(true)
    expect(lastEvent!.attributes.get('reason')).toBe('ERROR')
  })
})
