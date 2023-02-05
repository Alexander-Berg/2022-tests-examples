import { EventNames } from '../../../../xpackages/eventus/code/events/event-names'
import { Eventus } from '../../../../xpackages/eventus/code/events/eventus'
import { AnalyticsRunner } from '../../../code/analytics-runner'
import { Evaluation } from '../../../code/evaluations/evaluation'
import { ComposeCompletedEvaluation } from '../../../code/mail/scenarios/compose/compose-completed-evaluation'
import { ComposeFinalizerEvaluation } from '../../../code/mail/scenarios/compose/compose-finalizer-evaluation'
import { ComposeQualityTypeEvaluation } from '../../../code/mail/scenarios/compose/compose-quality-type-evaluation'
import { Scenario } from '../../../code/scenario'
import { checkEvaluationsResults } from '../../utils/utils'

describe('Compose finalizer evaluation', () => {
  it('should be correct for empty scenario', (done) => {
    const session = new Scenario()

    const evaluations: Evaluation<any, null>[] = [
      new ComposeFinalizerEvaluation(),
      new ComposeCompletedEvaluation(),
      new ComposeQualityTypeEvaluation(),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [null, false, 'incomplete'])
    done()
  })

  it('should be correct for finished with back scenario', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.messageListEvents.writeNewMessage())
      .thenEvent(Eventus.composeEvents.editBody())
      .thenEvent(Eventus.composeEvents.pressBack(false))

    const evaluations: Evaluation<any, null>[] = [
      new ComposeFinalizerEvaluation(),
      new ComposeCompletedEvaluation(),
      new ComposeQualityTypeEvaluation(),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [EventNames.COMPOSE_BACK, true, 'success'])
    done()
  })

  it('should be correct for finished with send scenario', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.messageListEvents.writeNewMessage())
      .thenEvent(Eventus.composeEvents.addAttachments(10))
      .thenEvent(Eventus.composeEvents.sendMessage())

    const evaluations: Evaluation<any, null>[] = [
      new ComposeFinalizerEvaluation(),
      new ComposeCompletedEvaluation(),
      new ComposeQualityTypeEvaluation(),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [EventNames.COMPOSE_SEND_MESSAGE, true, 'success'])
    done()
  })

  it('should be correct for unfinished scenario', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.messageListEvents.writeNewMessage())
      .thenEvent(Eventus.composeEvents.addAttachments(10))
      .thenEvent(Eventus.composeEvents.editBody())

    const evaluations: Evaluation<any, null>[] = [
      new ComposeFinalizerEvaluation(),
      new ComposeCompletedEvaluation(),
      new ComposeQualityTypeEvaluation(),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [null, false, 'incomplete'])
    done()
  })
})
