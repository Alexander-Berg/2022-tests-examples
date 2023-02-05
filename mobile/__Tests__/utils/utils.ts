import * as assert from 'assert'
import { Int32, Int64, int64 } from '../../../common/ys'
import { JSONItemFromJSON } from '../../../xpackages/common/__tests__/__helpers__/json-helpers'
import { JSONItem } from '../../../xpackages/common/code/json/json-types'
import { EventusEvent, LoggingEvent, EventusConstants } from '../../../xpackages/eventus-common/code/eventus-event'
import { EventusRegistry } from '../../../xpackages/eventus-common/code/eventus-registry'
import { NativeTimeProvider, TimeProvider } from '../../../xpackages/eventus-common/code/time-provider'
import { Evaluation } from '../../code/evaluations/evaluation'
import { ScenarioSplitter } from '../../code/evaluations/scenario-splitting/scenario-splitter'
import { Scenario, ScenarioAttributes } from '../../code/scenario'
import { TestStackReporter } from '../reporting/test-stack-reporter'
import { Eventus } from '../../../xpackages/eventus/code/events/eventus'
import { EventIdProvider } from '../../../xpackages/eventus-common/code/event-id-provider'

export function setIncrementalTimeline(scenario: Scenario): void {
  const times = []
  for (let i = 0; i < scenario.events.length; i++) {
    times.push(i)
  }
  setTimeline(scenario, times)
}

export function setTimeline(scenario: Scenario, times: Int32[]): void {
  if (scenario.events.length !== times.length) {
    throw Error('Timeline size must have same size as scenario')
  }
  EventusRegistry.timeProvider = new MockTimeProvider(times.map((t) => int64(t)))
  const reporter = new TestStackReporter()
  EventusRegistry.setEventReporter(reporter)
  scenario.events.forEach((e, _) => {
    e.report()
  })
  EventusRegistry.timeProvider = new NativeTimeProvider()
  scenario.events.splice(0, scenario.events.length)
  reporter.events.map((e) => parseEvent(e)).forEach((e) => scenario.thenEvent(e))
}

export function checkScenario(scenario: Scenario, expected: Scenario): void {
  refreshProvider()
  const reporter = new TestStackReporter()
  for (const event of scenario.events) {
    event.reportVia(reporter)
  }
  const actualEvents = reporter.events.map((e) => {
    assert.strictEqual(e.attributes.has('timestamp'), true)
    e.attributes.delete('timestamp')
    e.attributes.delete('version')
    return parseEvent(e)
  })
  assert.deepStrictEqual(actualEvents, expected.events)
}

export function checkEvaluationsResults<T, C>(
  evaluations: Evaluation<T, C>[],
  actual: Map<string, any>,
  expected: any[],
): void {
  const results = collectResults(evaluations, actual)
  assert.deepStrictEqual(results, expected)
}

export function checkSplitterEvaluationResults<C>(
  evaluation: ScenarioSplitter<C>,
  actual: Map<string, ScenarioAttributes[]>,
  expected: any[][],
): void {
  const scenarioAttributes = actual.get(evaluation.name())!
  const evaluations = evaluation.getEvaluations()
  const result = []
  for (const scenarioAttribute of scenarioAttributes) {
    result.push(collectResults(evaluations, scenarioAttribute.attributes))
  }
  assert.deepStrictEqual(result, expected)
}

export function refreshProvider(): void {
  Eventus.setup()
  EventusRegistry.eventIdProvider = new ZeroEventIdProvider()
}

function collectResults<T, C>(
  evaluations: readonly Evaluation<T, C>[],
  actual: ReadonlyMap<string, any>,
): readonly T[] {
  return evaluations.map((ev) => actual.get(ev.name()))
}

export function parseEvent(event: LoggingEvent): EventusEvent {
  const attributes = new Map<string, JSONItem>()
  event.attributes.forEach((v, k) => {
    const parsedValue = JSONItemFromJSON(v)
    attributes.set(k, parsedValue)
  })
  return EventusEvent.fromMap(event.name.replace(EventusConstants.PREFIX, ''), attributes)
}

export class MockTimeProvider implements TimeProvider {
  private position: Int32 = 0

  constructor(private times: Int64[]) {}

  public getCurrentTimeMs(): Int64 {
    return this.times[this.position++]
  }
}

export class ZeroEventIdProvider implements EventIdProvider {
  getId(): Int64 {
    return int64(0)
  }
}
