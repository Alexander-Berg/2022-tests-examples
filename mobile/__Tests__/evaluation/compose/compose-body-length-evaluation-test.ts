import { Eventus } from '../../../../xpackages/eventus/code/events/eventus'
import { AnalyticsRunner } from '../../../code/analytics-runner'
import { ComposeBodyLengthEvaluation } from '../../../code/mail/scenarios/compose/compose-body-length-evaluation'
import { Scenario } from '../../../code/scenario'
import { checkEvaluationsResults } from '../../utils/utils'

describe('Compose body length evaluation', () => {
  it('should be correct for edit text scenario', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.messageListEvents.writeNewMessage())
      .thenEvent(Eventus.composeEvents.editBody(10))
      .thenEvent(Eventus.composeEvents.editBody(44))
      .thenEvent(Eventus.composeEvents.sendMessage())

    const evaluations = [new ComposeBodyLengthEvaluation()]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [44])
    done()
  })

  it('should be correct for unsuccessful scenario', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.messageListEvents.writeNewMessage())
      .thenEvent(Eventus.composeEvents.addAttachments(1))
      .thenEvent(Eventus.composeEvents.pressBack(false))

    const evaluations = [new ComposeBodyLengthEvaluation()]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [null])
    done()
  })

  it('should be correct for empty scenario', (done) => {
    const session = new Scenario()

    const evaluations = [new ComposeBodyLengthEvaluation()]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [null])
    done()
  })

  it('should be false for scenario', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.messageListEvents.writeNewMessage())
      .thenEvent(Eventus.composeEvents.editBody())
      .thenEvent(Eventus.composeEvents.editBody())
      .thenEvent(Eventus.composeEvents.pressBack(false))

    const evaluations = [new ComposeBodyLengthEvaluation()]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [null])
    done()
  })
})
