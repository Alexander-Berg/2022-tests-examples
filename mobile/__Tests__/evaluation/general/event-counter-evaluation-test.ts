import { int64 } from '../../../../common/ys'
import { EventNames } from '../../../../xpackages/eventus/code/events/event-names'
import { Eventus } from '../../../../xpackages/eventus/code/events/eventus'
import { AnalyticsRunner } from '../../../code/analytics-runner'
import { EventCounterEvaluation } from '../../../code/evaluations/general-evaluations/one-value/counter/event-counter-evaluation'
import { Scenario } from '../../../code/scenario'
import { checkEvaluationsResults } from '../../utils/utils'

describe('Event counter evaluation', () => {
  it('should be correct for scenario without target events', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.startEvents.startWithMessageListShow())
      .thenEvent(Eventus.messageListEvents.markMessageAsRead(0, int64(1)))
      .thenEvent(Eventus.messageListEvents.openMessage(0, int64(1)))
      .thenEvent(Eventus.messageListEvents.deleteMessage(0, int64(2)))

    const evaluations = [new EventCounterEvaluation(EventNames.LIST_MESSAGE_MARK_AS_UNREAD, 'mark-read-count')]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [0])
    done()
  })

  it('should be correct for empty scenario', (done) => {
    const session = new Scenario()

    const evaluations = [new EventCounterEvaluation(EventNames.LIST_MESSAGE_MARK_AS_UNREAD, 'mark-unread-count')]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [0])
    done()
  })

  it('should be correct for one event scenario', (done) => {
    const session = new Scenario().thenEvent(Eventus.messageListEvents.writeNewMessage())

    const evaluations = [new EventCounterEvaluation(EventNames.LIST_MESSAGE_WRITE_NEW_MESSAGE, 'write-message-count')]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [1])
    done()
  })

  it('should be correct for multiple event scenario and metrics', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.messageListEvents.markMessageAsUnread(0, int64(1)))
      .thenEvent(Eventus.messageViewEvents.backToMailList())
      .thenEvent(Eventus.messageViewEvents.backToMailList())
      .thenEvent(Eventus.messageListEvents.markMessageAsUnread(0, int64(1)))
      .thenEvent(Eventus.messageViewEvents.backToMailList())
      .thenEvent(Eventus.messageViewEvents.backToMailList())
      .thenEvent(Eventus.messageActionsEvents.replyAll())

    const evaluations = [
      new EventCounterEvaluation(EventNames.MESSAGE_VIEW_BACK, 'message-back-count'),
      new EventCounterEvaluation(EventNames.LIST_MESSAGE_MARK_AS_UNREAD, 'mark-unread-count'),
      new EventCounterEvaluation(EventNames.MESSAGE_ACTION_REPLY, 'message-action-reply-count'),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [4, 2, 0])
    done()
  })
})
