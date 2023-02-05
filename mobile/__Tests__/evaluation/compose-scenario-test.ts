import { int64 } from '../../../common/ys'
import { Eventus } from '../../../xpackages/eventus/code/events/eventus'
import { AnalyticsRunner } from '../../code/analytics-runner'
import { FirstEventEvaluation } from '../../code/evaluations/general-evaluations/default/first-event-evaluation'
import { SessionLengthEvaluation } from '../../code/evaluations/general-evaluations/default/session-length-evaluation'
import { MailContextApplier } from '../../code/mail/mail-context-applier'
import { ComposeScenarioSplitter } from '../../code/mail/scenarios/compose/compose-scenario-splitter'
import { Scenario } from '../../code/scenario'
import { checkSplitterEvaluationResults, setTimeline } from '../utils/utils'

describe('Compose scenario length and first event', () => {
  it('should be correct for one scenario session', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.startEvents.startWithMessageListShow())
      .thenEvent(Eventus.messageListEvents.writeNewMessage())
      .thenEvent(Eventus.composeEvents.pressBack(false))

    setTimeline(session, [10, 100, 1000])

    const scenarioEvaluation = [
      new ComposeScenarioSplitter([(): any => new SessionLengthEvaluation(), (): any => new FirstEventEvaluation()]),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluateWithContext(session, scenarioEvaluation, new MailContextApplier())

    checkSplitterEvaluationResults(scenarioEvaluation[0], results, [[int64(900), int64(900)]])
    done()
  })

  it('should be correct for unfinished scenario session', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.startEvents.startWithMessageListShow())
      .thenEvent(Eventus.messageListEvents.writeNewMessage())
      .thenEvent(Eventus.composeEvents.editBody(100))
      .thenEvent(Eventus.composeEvents.editBody(200))
    setTimeline(session, [10, 100, 1000, 10000])

    const scenarioEvaluation = [
      new ComposeScenarioSplitter([(): any => new SessionLengthEvaluation(), (): any => new FirstEventEvaluation()]),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluateWithContext(session, scenarioEvaluation, new MailContextApplier())

    checkSplitterEvaluationResults(scenarioEvaluation[0], results, [[int64(9900), int64(900)]])
    done()
  })

  it('should be correct for zero scenario session', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.startEvents.startWithMessageListShow())
      .thenEvent(Eventus.messageListEvents.openMessage(0, int64(1)))
      .thenEvent(Eventus.messageViewEvents.backToMailList())
      .thenEvent(Eventus.messageListEvents.deleteMessage(0, int64(2)))
    setTimeline(session, [10, 100, 1000, 10000])

    const scenarioEvaluation = [
      new ComposeScenarioSplitter([(): any => new SessionLengthEvaluation(), (): any => new FirstEventEvaluation()]),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluateWithContext(session, scenarioEvaluation, new MailContextApplier())

    checkSplitterEvaluationResults(scenarioEvaluation[0], results, [])
    done()
  })

  it('should be correct for two scenario session', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.startEvents.startWithMessageListShow())
      .thenEvent(Eventus.messageListEvents.writeNewMessage())
      .thenEvent(Eventus.composeEvents.pressBack(false))
      .thenEvent(Eventus.messageListEvents.openMessage(0, int64(1)))
      .thenEvent(Eventus.messageViewEvents.reply(0))
      .thenEvent(Eventus.composeEvents.editBody(100))
      .thenEvent(Eventus.composeEvents.sendMessage())
      .thenEvent(Eventus.messageListEvents.deleteMessage(0, int64(2)))
    setTimeline(session, [1, 3, 6, 10, 30, 60, 100, 300])

    const scenarioEvaluation = [
      new ComposeScenarioSplitter([(): any => new SessionLengthEvaluation(), (): any => new FirstEventEvaluation()]),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluateWithContext(session, scenarioEvaluation, new MailContextApplier())

    checkSplitterEvaluationResults(scenarioEvaluation[0], results, [
      [int64(3), int64(3)],
      [int64(70), int64(30)],
    ])
    done()
  })
})
