import { int64 } from '../../../common/ys'
import { Eventus } from '../../../xpackages/eventus/code/events/eventus'
import { AnalyticsRunner } from '../../code/analytics-runner'
import { FirstEventEvaluation } from '../../code/evaluations/general-evaluations/default/first-event-evaluation'
import { SessionLengthEvaluation } from '../../code/evaluations/general-evaluations/default/session-length-evaluation'
import { MailContextApplier } from '../../code/mail/mail-context-applier'
import { QuickReplyScenarioSplitter } from '../../code/mail/scenarios/compose/quick-reply-scenario-splitter'
import { Scenario } from '../../code/scenario'
import { checkSplitterEvaluationResults, setTimeline } from '../utils/utils'

describe('Quick reply scenario length and first event', () => {
  it('should be correct for one scenario session', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.startEvents.startWithMessageListShow())
      .thenEvent(Eventus.messageListEvents.openMessage(0, int64(0)))
      .thenEvent(Eventus.quickReplyEvents.clicked())
      .thenEvent(Eventus.messageViewEvents.backToMailList())

    setTimeline(session, [10, 100, 1000, 2000])

    const scenarioEvaluation = [
      new QuickReplyScenarioSplitter([(): any => new SessionLengthEvaluation(), (): any => new FirstEventEvaluation()]),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluateWithContext(session, scenarioEvaluation, new MailContextApplier())

    checkSplitterEvaluationResults(scenarioEvaluation[0], results, [[int64(1000), int64(1000)]])
    done()
  })

  it('should be correct for unfinished scenario session', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.startEvents.startWithMessageListShow())
      .thenEvent(Eventus.messageListEvents.openMessage(0, int64(0)))
      .thenEvent(Eventus.quickReplyEvents.smartReplyMessageClicked(0))
      .thenEvent(Eventus.quickReplyEvents.editBody(200))
      .thenEvent(Eventus.quickReplyEvents.editBody(200))
    setTimeline(session, [10, 100, 1000, 10000, 10100])

    const scenarioEvaluation = [
      new QuickReplyScenarioSplitter([(): any => new SessionLengthEvaluation(), (): any => new FirstEventEvaluation()]),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluateWithContext(session, scenarioEvaluation, new MailContextApplier())

    checkSplitterEvaluationResults(scenarioEvaluation[0], results, [[int64(9100), int64(9000)]])
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
      new QuickReplyScenarioSplitter([(): any => new SessionLengthEvaluation(), (): any => new FirstEventEvaluation()]),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluateWithContext(session, scenarioEvaluation, new MailContextApplier())

    checkSplitterEvaluationResults(scenarioEvaluation[0], results, [])
    done()
  })

  it('should be correct for two scenario session', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.startEvents.startWithMessageListShow())
      .thenEvent(Eventus.messageListEvents.openMessage(0, int64(1)))
      .thenEvent(Eventus.quickReplyEvents.smartReplyMessageClicked(1))
      .thenEvent(Eventus.quickReplyEvents.openCompose())
      .thenEvent(Eventus.composeEvents.sendMessage())
      .thenEvent(Eventus.messageListEvents.openMessage(1, int64(2)))
      .thenEvent(Eventus.quickReplyEvents.clicked())
      .thenEvent(Eventus.quickReplyEvents.sendMessage())
    setTimeline(session, [1, 3, 6, 10, 30, 60, 100, 300])

    const scenarioEvaluation = [
      new QuickReplyScenarioSplitter([(): any => new SessionLengthEvaluation(), (): any => new FirstEventEvaluation()]),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluateWithContext(session, scenarioEvaluation, new MailContextApplier())

    checkSplitterEvaluationResults(scenarioEvaluation[0], results, [
      [int64(4), int64(4)],
      [int64(200), int64(200)],
    ])
    done()
  })
})
