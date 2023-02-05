import { int64 } from '../../../../common/ys'
import { Eventus } from '../../../../xpackages/eventus/code/events/eventus'
import { MessageDTO } from '../../../../xpackages/eventus-common/code/objects/message'
import { AnalyticsRunner } from '../../../code/analytics-runner'
import { Evaluation } from '../../../code/evaluations/evaluation'
import { MailContextApplier } from '../../../code/mail/mail-context-applier'
import { QuickReplyQualityTypeEvaluation } from '../../../code/mail/scenarios/compose/quick-reply-quality-type-evaluation'
import { ComposeQrUsedEvaluation } from '../../../code/mail/scenarios/compose/smart_replies/compose-qr-used-evaluation'
import { ComposeSrIndexUsedEvaluation } from '../../../code/mail/scenarios/compose/smart_replies/compose-sr-index-used-evaluation'
import { ComposeSrItemsCountEvaluation } from '../../../code/mail/scenarios/compose/smart_replies/compose-sr-items-count-evaluation'
import { ComposeSrUsedEvaluation } from '../../../code/mail/scenarios/compose/smart_replies/compose-sr-used-evaluation'
import { ComposeSrUsedExactEvaluation } from '../../../code/mail/scenarios/compose/smart_replies/compose-sr-used-exact-evaluation'
import { Scenario } from '../../../code/scenario'
import { checkEvaluationsResults } from '../../utils/utils'

describe('Quick replies evaluations', () => {
  it('should be correct for scenario without quick replies', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.messageListEvents.writeNewMessage())
      .thenEvent(Eventus.composeEvents.editBody(10))
      .thenEvent(Eventus.composeEvents.editBody(44))
      .thenEvent(Eventus.composeEvents.sendMessage())

    const evaluations: Evaluation<any, any>[] = [
      new ComposeQrUsedEvaluation(),
      new ComposeSrUsedEvaluation(),
      new ComposeSrItemsCountEvaluation(),
      new ComposeSrIndexUsedEvaluation(),
      new ComposeSrUsedExactEvaluation(),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluateWithContext(session, evaluations, new MailContextApplier())

    checkEvaluationsResults(evaluations, results, [false, false, null, null, null])
    done()
  })

  it('should be correct for scenario with not used smart replies', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.modelSyncEvents.updateMessageList([new MessageDTO(int64(0), int64(10), 3)]))
      .thenEvent(Eventus.messageListEvents.openMessage(0, int64(0)))
      .thenEvent(Eventus.quickReplyEvents.clicked())
      .thenEvent(Eventus.quickReplyEvents.editBody(23))
      .thenEvent(Eventus.quickReplyEvents.sendMessage())

    const evaluations: Evaluation<any, any>[] = [
      new QuickReplyQualityTypeEvaluation(),
      new ComposeQrUsedEvaluation(),
      new ComposeSrUsedEvaluation(),
      new ComposeSrItemsCountEvaluation(),
      new ComposeSrIndexUsedEvaluation(),
      new ComposeSrUsedExactEvaluation(),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluateWithContext(session, evaluations, new MailContextApplier())

    checkEvaluationsResults(evaluations, results, ['success', true, false, 3, null, null])
    done()
  })

  it('should be correct for scenario with zero smart replies', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.modelSyncEvents.updateMessageList([new MessageDTO(int64(0), int64(10), 0)]))
      .thenEvent(Eventus.messageListEvents.openMessage(0, int64(0)))
      .thenEvent(Eventus.quickReplyEvents.clicked())
      .thenEvent(Eventus.quickReplyEvents.editBody(23))
      .thenEvent(Eventus.quickReplyEvents.sendMessage())

    const evaluations: Evaluation<any, any>[] = [
      new ComposeQrUsedEvaluation(),
      new ComposeSrUsedEvaluation(),
      new ComposeSrItemsCountEvaluation(),
      new ComposeSrIndexUsedEvaluation(),
      new ComposeSrUsedExactEvaluation(),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluateWithContext(session, evaluations, new MailContextApplier())

    checkEvaluationsResults(evaluations, results, [true, false, 0, null, null])
    done()
  })

  it('should be correct for scenario with edited smart reply', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.modelSyncEvents.updateMessageList([new MessageDTO(int64(0), int64(10), 3)]))
      .thenEvent(Eventus.messageListEvents.openMessage(0, int64(0)))
      .thenEvent(Eventus.quickReplyEvents.smartReplyMessageClicked(2))
      .thenEvent(Eventus.quickReplyEvents.editBody(23))
      .thenEvent(Eventus.quickReplyEvents.sendMessage())

    const evaluations: Evaluation<any, any>[] = [
      new ComposeQrUsedEvaluation(),
      new ComposeSrUsedEvaluation(),
      new ComposeSrItemsCountEvaluation(),
      new ComposeSrIndexUsedEvaluation(),
      new ComposeSrUsedExactEvaluation(),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluateWithContext(session, evaluations, new MailContextApplier())

    checkEvaluationsResults(evaluations, results, [true, true, 3, 2, false])
    done()
  })

  it('should be correct for scenario with smart reply as is', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.modelSyncEvents.updateMessageList([new MessageDTO(int64(0), int64(10), 3)]))
      .thenEvent(Eventus.messageListEvents.openMessage(0, int64(0)))
      .thenEvent(Eventus.quickReplyEvents.smartReplyMessageClicked(1))
      .thenEvent(Eventus.quickReplyEvents.sendMessage())

    const evaluations: Evaluation<any, any>[] = [
      new ComposeQrUsedEvaluation(),
      new ComposeSrUsedEvaluation(),
      new ComposeSrItemsCountEvaluation(),
      new ComposeSrIndexUsedEvaluation(),
      new ComposeSrUsedExactEvaluation(),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluateWithContext(session, evaluations, new MailContextApplier())

    checkEvaluationsResults(evaluations, results, [true, true, 3, 1, true])
    done()
  })
})
