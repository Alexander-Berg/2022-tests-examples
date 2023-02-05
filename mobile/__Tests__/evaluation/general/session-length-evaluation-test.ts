import { int64 } from '../../../../common/ys'
import { Eventus } from '../../../../xpackages/eventus/code/events/eventus'
import { AnalyticsRunner } from '../../../code/analytics-runner'
import { SessionLengthEvaluation } from '../../../code/evaluations/general-evaluations/default/session-length-evaluation'
import { Scenario } from '../../../code/scenario'
import { checkEvaluationsResults, setTimeline } from '../../utils/utils'

describe('Session length evaluation', () => {
  it('should be correct for usual scenario', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.startEvents.startWithMessageListShow())
      .thenEvent(Eventus.messageListEvents.markMessageAsRead(0, int64(1)))
      .thenEvent(Eventus.messageListEvents.deleteMessage(0, int64(2)))
    setTimeline(session, [10, 100, 1000])

    const evaluations = [new SessionLengthEvaluation()]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [int64(990)])
    done()
  })

  it('should be correct for one event scenario', (done) => {
    const session = new Scenario().thenEvent(Eventus.startEvents.startWithMessageListShow())
    setTimeline(session, [10])

    const evaluations = [new SessionLengthEvaluation()]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [int64(0)])
    done()
  })
})
