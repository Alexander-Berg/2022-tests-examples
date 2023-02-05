import { int64 } from '../../../common/ys'
import { EcomailService } from '../../../xpackages/eventus-common/code/objects/ecomail'
import { Eventus } from '../../../xpackages/eventus/code/events/eventus'
import { AnalyticsRunner } from '../../code/analytics-runner'
import { FirstEventTimestampEvaluation } from '../../code/evaluations/general-evaluations/function/default/first-event-timestamp-evaluation'
import { SessionLengthEvaluation } from '../../code/evaluations/general-evaluations/default/session-length-evaluation'
import { MailContextApplier } from '../../code/mail/mail-context-applier'
import { Scenario } from '../../code/scenario'
import { checkSplitterEvaluationResults, setTimeline } from '../utils/utils'
import { EcomailScenarioSplitter } from '../../code/mail/scenarios/ecomail/ecomail-scenario-splitter'

describe('Ecomail scenario length and start timestamp', () => {
  it('should be correct for one scenario session', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.startEvents.startWithMessageListShow())
      .thenEvent(Eventus.ecomailEvents.openEcomailService(EcomailService.Telemost))
      .thenEvent(Eventus.ecomailEvents.initEcomailService(EcomailService.Telemost, 'message_list'))
      .thenEvent(Eventus.ecomailEvents.closeEcomailService(EcomailService.Telemost))

    setTimeline(session, [10, 100, 1000, 2000])

    const scenarioEvaluation = [
      new EcomailScenarioSplitter([
        (): any => new SessionLengthEvaluation(),
        (): any => new FirstEventTimestampEvaluation(),
      ]),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluateWithContext(session, scenarioEvaluation, new MailContextApplier())

    checkSplitterEvaluationResults(scenarioEvaluation[0], results, [[int64(1900), int64(100)]])
    done()
  })

  it('should be correct for unfinished scenario session', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.startEvents.startWithMessageListShow())
      .thenEvent(Eventus.ecomailEvents.openEcomailService(EcomailService.Telemost))
      .thenEvent(Eventus.ecomailEvents.initEcomailService(EcomailService.Telemost, 'message_list'))
      .thenEvent(Eventus.ecomailEvents.openEcomailService(EcomailService.Telemost))

    setTimeline(session, [10, 100, 200, 1000])

    const scenarioEvaluation = [
      new EcomailScenarioSplitter([
        (): any => new SessionLengthEvaluation(),
        (): any => new FirstEventTimestampEvaluation(),
      ]),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluateWithContext(session, scenarioEvaluation, new MailContextApplier())

    checkSplitterEvaluationResults(scenarioEvaluation[0], results, [[int64(900), int64(100)]])
    done()
  })

  it('should be correct for zero scenario session', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.startEvents.startWithMessageListShow())
      .thenEvent(Eventus.messageListEvents.openMessage(0, int64(1)))
      .thenEvent(Eventus.quickReplyEvents.openCompose())
      .thenEvent(Eventus.composeEvents.sendMessage())

    setTimeline(session, [10, 100, 1000, 10000])

    const scenarioEvaluation = [
      new EcomailScenarioSplitter([
        (): any => new SessionLengthEvaluation(),
        (): any => new FirstEventTimestampEvaluation(),
      ]),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluateWithContext(session, scenarioEvaluation, new MailContextApplier())

    checkSplitterEvaluationResults(scenarioEvaluation[0], results, [])
    done()
  })

  it('should be correct for two scenario session', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.startEvents.startWithMessageListShow())
      .thenEvent(Eventus.ecomailEvents.openEcomailService(EcomailService.Telemost))
      .thenEvent(Eventus.ecomailEvents.initEcomailService(EcomailService.Telemost, 'message_list'))
      .thenEvent(Eventus.ecomailEvents.closeEcomailService(EcomailService.Telemost))
      .thenEvent(Eventus.ecomailEvents.openEcomailService(EcomailService.Telemost))
      .thenEvent(Eventus.ecomailEvents.initEcomailService(EcomailService.Telemost, 'message_list'))
      .thenEvent(Eventus.ecomailEvents.openEcomailService(EcomailService.Telemost))
      .thenEvent(Eventus.ecomailEvents.closeEcomailService(EcomailService.Telemost))

    setTimeline(session, [1, 3, 6, 10, 30, 60, 100, 300])

    const scenarioEvaluation = [
      new EcomailScenarioSplitter([
        (): any => new SessionLengthEvaluation(),
        (): any => new FirstEventTimestampEvaluation(),
      ]),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluateWithContext(session, scenarioEvaluation, new MailContextApplier())

    checkSplitterEvaluationResults(scenarioEvaluation[0], results, [
      [int64(7), int64(3)],
      [int64(270), int64(30)],
    ])
    done()
  })
})
