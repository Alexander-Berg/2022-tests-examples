import { int64 } from '../../../../common/ys'
import { Eventus } from '../../../../xpackages/eventus/code/events/eventus'
import { EcomailService } from '../../../../xpackages/eventus-common/code/objects/ecomail'
import { AnalyticsRunner } from '../../../code/analytics-runner'
import { FullScenarioEvaluation } from '../../../code/evaluations/general-evaluations/default/full-scenario-evaluation'
import { StartScenarioSplitter } from '../../../code/evaluations/scenario-splitting/start-scenario-splitter'
import { MailContextApplier } from '../../../code/mail/mail-context-applier'
import { Scenario } from '../../../code/scenario'
import { checkSplitterEvaluationResults, setTimeline } from '../../utils/utils'

describe('Global splitting should split correctly', () => {
  it('for one start event in scenario', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.startEvents.startWithMessageListShow())
      .thenEvent(Eventus.messageListEvents.openMessage(0, int64(1)))
      .thenEvent(Eventus.messageListEvents.openMessage(0, int64(1)))
      .thenEvent(Eventus.messageListEvents.openMessage(0, int64(1)))
      .thenEvent(Eventus.messageListEvents.openMessage(0, int64(1)))
    setTimeline(session, [10, 20, 30, 40, 50])

    const evaluations = [
      new StartScenarioSplitter([(): FullScenarioEvaluation<unknown> => new FullScenarioEvaluation()]),
    ]
    const runner = new AnalyticsRunner()
    const result = runner.evaluateWithContext(session, evaluations, new MailContextApplier())

    checkSplitterEvaluationResults(evaluations[0], result, [[session]])
    done()
  })

  it('for one event', (done) => {
    const session = new Scenario().thenEvent(Eventus.startEvents.startFromMessageNotification())
    setTimeline(session, [10])

    const evaluations = [
      new StartScenarioSplitter([(): FullScenarioEvaluation<unknown> => new FullScenarioEvaluation()]),
    ]
    const runner = new AnalyticsRunner()
    const result = runner.evaluateWithContext(session, evaluations, new MailContextApplier())

    checkSplitterEvaluationResults(evaluations[0], result, [[session]])
    done()
  })

  it('for session without start event', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.messageListEvents.openMessage(0, int64(1)))
      .thenEvent(Eventus.messageListEvents.openMessage(0, int64(1)))
      .thenEvent(Eventus.messageListEvents.openMessage(0, int64(1)))
      .thenEvent(Eventus.messageListEvents.openMessage(0, int64(1)))
    setTimeline(session, [10, 20, 30, 40])

    const evaluations = [
      new StartScenarioSplitter([(): FullScenarioEvaluation<unknown> => new FullScenarioEvaluation()]),
    ]
    const runner = new AnalyticsRunner()
    const result = runner.evaluateWithContext(session, evaluations, new MailContextApplier())

    checkSplitterEvaluationResults(evaluations[0], result, [])
    done()
  })

  it('for scenario interaction by time limit', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.startEvents.startWithMessageListShow())
      .thenEvent(Eventus.messageListEvents.refreshMessageList())
      .thenEvent(Eventus.messageListEvents.markMessageAsRead(0, int64(0)))
      .thenEvent(Eventus.messageListEvents.refreshMessageList())
      .thenEvent(Eventus.messageListEvents.markMessageAsRead(0, int64(0)))
      .thenEvent(Eventus.ecomailEvents.openEcomailService(EcomailService.Mail))
      .thenEvent(Eventus.messageListEvents.openMessage(0, int64(1)))
    setTimeline(session, [10, 100, 1000, 1000000, 1000100, 1000200, 1001000])

    const evaluations = [
      new StartScenarioSplitter([(): FullScenarioEvaluation<unknown> => new FullScenarioEvaluation()]),
    ]
    const runner = new AnalyticsRunner()
    const result = runner.evaluateWithContext(session, evaluations, new MailContextApplier())

    const evaluations1 = [
      new StartScenarioSplitter([(): FullScenarioEvaluation<unknown> => new FullScenarioEvaluation()]),
    ]
    const part1 = new Scenario()
      .thenEvent(Eventus.startEvents.startWithMessageListShow())
      .thenEvent(Eventus.messageListEvents.refreshMessageList())
      .thenEvent(Eventus.messageListEvents.markMessageAsRead(0, int64(0)))
    setTimeline(part1, [10, 100, 1000])
    runner.evaluateWithContext(part1, evaluations1, new MailContextApplier())

    const evaluations2 = [
      new StartScenarioSplitter([(): FullScenarioEvaluation<unknown> => new FullScenarioEvaluation()]),
    ]
    const part2 = new Scenario()
      .thenEvent(Eventus.ecomailEvents.openEcomailService(EcomailService.Mail))
      .thenEvent(Eventus.messageListEvents.openMessage(0, int64(1)))
    setTimeline(part2, [1000200, 1001000])
    runner.evaluateWithContext(part2, evaluations2, new MailContextApplier())

    checkSplitterEvaluationResults(evaluations[0], result, [[part1], [part2]])
    done()
  })
})
