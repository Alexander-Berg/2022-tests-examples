import { int64 } from '../../../../common/ys'
import { Eventus } from '../../../../xpackages/eventus/code/events/eventus'
import { AnalyticsRunner } from '../../../code/analytics-runner'
import { Evaluation } from '../../../code/evaluations/evaluation'
import { MailContextApplier } from '../../../code/mail/mail-context-applier'
import { ComposeScenarioSplitter } from '../../../code/mail/scenarios/compose/compose-scenario-splitter'
import { ComposeSrIndexUsedEvaluation } from '../../../code/mail/scenarios/compose/smart_replies/compose-sr-index-used-evaluation'
import { ComposeSrItemsCountEvaluation } from '../../../code/mail/scenarios/compose/smart_replies/compose-sr-items-count-evaluation'
import { ComposeSrUsedEvaluation } from '../../../code/mail/scenarios/compose/smart_replies/compose-sr-used-evaluation'
import { ComposeSrUsedExactEvaluation } from '../../../code/mail/scenarios/compose/smart_replies/compose-sr-used-exact-evaluation'
import { Scenario } from '../../../code/scenario'
import { checkEvaluationsResults, checkSplitterEvaluationResults } from '../../utils/utils'

describe('Compose smart replies evaluations', () => {
  it('should be correct for scenario without smart replies', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.messageListEvents.writeNewMessage())
      .thenEvent(Eventus.composeEvents.editBody(10))
      .thenEvent(Eventus.composeEvents.editBody(44))
      .thenEvent(Eventus.composeEvents.sendMessage())

    const evaluations: Evaluation<any, any>[] = [
      new ComposeSrUsedEvaluation(),
      new ComposeSrItemsCountEvaluation(),
      new ComposeSrIndexUsedEvaluation(),
      new ComposeSrUsedExactEvaluation(),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluateWithContext(session, evaluations, new MailContextApplier())

    checkEvaluationsResults(evaluations, results, [false, null, null, null])
    done()
  })

  it('should be correct for scenario with not used smart replies', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.pushEvents.replyMessagePushClicked(int64(0), int64(0), int64(0), 3))
      .thenEvent(Eventus.composeEvents.editBody(10))
      .thenEvent(Eventus.composeEvents.sendMessage())

    const evaluations: Evaluation<any, any>[] = [
      new ComposeSrUsedEvaluation(),
      new ComposeSrItemsCountEvaluation(),
      new ComposeSrIndexUsedEvaluation(),
      new ComposeSrUsedExactEvaluation(),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluateWithContext(session, evaluations, new MailContextApplier())

    checkEvaluationsResults(evaluations, results, [false, 3, null, null])
    done()
  })

  it('should be correct for scenario with zero smart replies', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.pushEvents.replyMessagePushClicked(int64(0), int64(0), int64(0), 0))
      .thenEvent(Eventus.composeEvents.editBody(10))
      .thenEvent(Eventus.composeEvents.sendMessage())

    const evaluations: Evaluation<any, any>[] = [
      new ComposeSrUsedEvaluation(),
      new ComposeSrItemsCountEvaluation(),
      new ComposeSrIndexUsedEvaluation(),
      new ComposeSrUsedExactEvaluation(),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluateWithContext(session, evaluations, new MailContextApplier())

    checkEvaluationsResults(evaluations, results, [false, 0, null, null])
    done()
  })

  it('should be correct for scenario with edited smart reply', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.pushEvents.smartReplyMessagePushClicked(int64(0), int64(0), int64(0), 1, 3))
      .thenEvent(Eventus.composeEvents.editBody(10))
      .thenEvent(Eventus.composeEvents.sendMessage())

    const evaluations: Evaluation<any, any>[] = [
      new ComposeSrUsedEvaluation(),
      new ComposeSrItemsCountEvaluation(),
      new ComposeSrIndexUsedEvaluation(),
      new ComposeSrUsedExactEvaluation(),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluateWithContext(session, evaluations, new MailContextApplier())

    checkEvaluationsResults(evaluations, results, [true, 3, 1, false])
    done()
  })

  it('should be correct for scenario with smart reply as is', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.pushEvents.smartReplyMessagePushClicked(int64(0), int64(0), int64(0), 1, 3))
      .thenEvent(Eventus.composeEvents.addAttachments(1))
      .thenEvent(Eventus.composeEvents.sendMessage())

    const evaluations: Evaluation<any, any>[] = [
      new ComposeSrUsedEvaluation(),
      new ComposeSrItemsCountEvaluation(),
      new ComposeSrIndexUsedEvaluation(),
      new ComposeSrUsedExactEvaluation(),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluateWithContext(session, evaluations, new MailContextApplier())

    checkEvaluationsResults(evaluations, results, [true, 3, 1, true])
    done()
  })

  it('should be correct for scenario with pushes', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.pushEvents.messagesReceivedPushShown(int64(0), int64(0), [int64(1), int64(2)], [3, 2]))
      .thenEvent(Eventus.pushEvents.singleMessagePushClicked(int64(0), int64(1), int64(0)))
      .thenEvent(Eventus.messageViewEvents.reply(0))
      .thenEvent(Eventus.composeEvents.sendMessage())

    const evaluations = [
      new ComposeScenarioSplitter([
        (): any => new ComposeSrUsedEvaluation(),
        (): any => new ComposeSrItemsCountEvaluation(),
        (): any => new ComposeSrIndexUsedEvaluation(),
        (): any => new ComposeSrUsedExactEvaluation(),
      ]),
    ]

    const runner = new AnalyticsRunner()
    const results = runner.evaluateWithContext(session, evaluations, new MailContextApplier())

    checkSplitterEvaluationResults(evaluations[0], results, [[false, 3, null, null]])
    done()
  })

  it('should be correct for scenario with pushes but no quick replies provided', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.pushEvents.messagesReceivedPushShown(int64(0), int64(0), [int64(1), int64(2)]))
      .thenEvent(Eventus.pushEvents.singleMessagePushClicked(int64(0), int64(1), int64(0)))
      .thenEvent(Eventus.messageViewEvents.reply(0))
      .thenEvent(Eventus.composeEvents.sendMessage())

    const evaluations = [
      new ComposeScenarioSplitter([
        (): any => new ComposeSrUsedEvaluation(),
        (): any => new ComposeSrItemsCountEvaluation(),
        (): any => new ComposeSrIndexUsedEvaluation(),
        (): any => new ComposeSrUsedExactEvaluation(),
      ]),
    ]

    const runner = new AnalyticsRunner()
    const results = runner.evaluateWithContext(session, evaluations, new MailContextApplier())

    checkSplitterEvaluationResults(evaluations[0], results, [[false, null, null, null]])
    done()
  })
})
