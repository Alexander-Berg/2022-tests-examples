import { Eventus } from '../../../../xpackages/eventus/code/events/eventus'
import { AnalyticsRunner } from '../../../code/analytics-runner'
import { Evaluation } from '../../../code/evaluations/evaluation'
import { ComposeAttachmentsCountEvaluation } from '../../../code/mail/scenarios/compose/attachments/compose-attachments-count-evaluation'
import { ComposeBlankScansCountEvaluation } from '../../../code/mail/scenarios/compose/attachments/compose-blank-scans-count-evaluation'
import { ComposeHasAttachmentsEvaluation } from '../../../code/mail/scenarios/compose/attachments/compose-has-attachments-evaluation'
import { ComposeIsPhotomailEvaluation } from '../../../code/mail/scenarios/compose/attachments/compose-is-photomail-evaluation'
import { ComposePhotomailLengthEvaluation } from '../../../code/mail/scenarios/compose/attachments/compose-photomail-length-evaluation'
import { ComposeScansCountEvaluation } from '../../../code/mail/scenarios/compose/attachments/compose-scans-count-evaluation'
import { ComposeBodyLengthEvaluation } from '../../../code/mail/scenarios/compose/compose-body-length-evaluation'
import { ComposeEditBodyEvaluation } from '../../../code/mail/scenarios/compose/compose-edit-body-evaluation'
import { Scenario } from '../../../code/scenario'
import { checkEvaluationsResults } from '../../utils/utils'

describe('Compose attachment evaluations', () => {
  it('should be correct for scenario without attachments', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.messageListEvents.writeNewMessage())
      .thenEvent(Eventus.composeEvents.editBody(10))
      .thenEvent(Eventus.composeEvents.editBody(44))
      .thenEvent(Eventus.composeEvents.sendMessage())

    const evaluations: Evaluation<any, null>[] = [
      new ComposeAttachmentsCountEvaluation(),
      new ComposeHasAttachmentsEvaluation(),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [0, false])
    done()
  })

  it('should be correct for scenario with attachments', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.messageListEvents.writeNewMessage())
      .thenEvent(Eventus.composeEvents.addAttachments(2))
      .thenEvent(Eventus.composeEvents.addAttachments(1))
      .thenEvent(Eventus.composeEvents.pressBack(false))

    const evaluations: Evaluation<any, null>[] = [
      new ComposeAttachmentsCountEvaluation(),
      new ComposeHasAttachmentsEvaluation(),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [3, true])
    done()
  })

  it('should be correct for unfinished scenario', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.messageListEvents.writeNewMessage())
      .thenEvent(Eventus.composeEvents.addAttachments(2))
      .thenEvent(Eventus.composeEvents.addAttachments(1))

    const evaluations: Evaluation<any, null>[] = [
      new ComposeAttachmentsCountEvaluation(),
      new ComposeHasAttachmentsEvaluation(),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [3, true])
    done()
  })

  it('should be correct for scenario when not all attachments were removed', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.messageListEvents.writeNewMessage())
      .thenEvent(Eventus.composeEvents.addAttachments(2))
      .thenEvent(Eventus.composeEvents.removeAttachment())
      .thenEvent(Eventus.composeEvents.removeAttachment())
      .thenEvent(Eventus.composeEvents.addAttachments(1))

    const evaluations: Evaluation<any, null>[] = [
      new ComposeAttachmentsCountEvaluation(),
      new ComposeHasAttachmentsEvaluation(),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [1, true])
    done()
  })

  it('should be correct for scenario with removed attachments', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.messageListEvents.writeNewMessage())
      .thenEvent(Eventus.composeEvents.addAttachments(2))
      .thenEvent(Eventus.composeEvents.removeAttachment())
      .thenEvent(Eventus.composeEvents.removeAttachment())
      .thenEvent(Eventus.composeEvents.addAttachments(1))
      .thenEvent(Eventus.composeEvents.removeAttachment())
      .thenEvent(Eventus.composeEvents.editBody())
      .thenEvent(Eventus.composeEvents.sendMessage())

    const evaluations: Evaluation<any, null>[] = [
      new ComposeAttachmentsCountEvaluation(),
      new ComposeHasAttachmentsEvaluation(),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [0, false])
    done()
  })

  it('should be correct for scenario with scan attachments', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.messageListEvents.writeNewMessage())
      .thenEvent(Eventus.composeEvents.addAttachments(2))
      .thenEvent(Eventus.composeEvents.removeAttachment())
      .thenEvent(Eventus.composeEvents.addScans(10))
      .thenEvent(Eventus.composeEvents.addSelectedScans(2, 3))
      .thenEvent(Eventus.composeEvents.removeAttachment())
      .thenEvent(Eventus.composeEvents.addAttachments(1))
      .thenEvent(Eventus.composeEvents.editBody())
      .thenEvent(Eventus.composeEvents.sendMessage())

    const evaluations: Evaluation<any, null>[] = [
      new ComposeAttachmentsCountEvaluation(),
      new ComposeHasAttachmentsEvaluation(),
      new ComposeScansCountEvaluation(),
      new ComposeBlankScansCountEvaluation(),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [3, true, 2, 3])
    done()
  })

  it('should be correct for scenario with photomail', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.messageListEvents.writeNewMessage())
      .thenEvent(Eventus.composeEvents.addPhotoMail(234))
      .thenEvent(Eventus.composeEvents.removeAttachment())
      .thenEvent(Eventus.composeEvents.sendMessage())

    const evaluations: Evaluation<any, null>[] = [
      new ComposeAttachmentsCountEvaluation(),
      new ComposeHasAttachmentsEvaluation(),
      new ComposeIsPhotomailEvaluation(),
      new ComposePhotomailLengthEvaluation(),
      new ComposeBodyLengthEvaluation(),
      new ComposeEditBodyEvaluation(),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [0, false, true, 234, 234, false])
    done()
  })

  it('should be correct for scenario with corrected photomail', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.messageListEvents.writeNewMessage())
      .thenEvent(Eventus.composeEvents.addPhotoMail(3849))
      .thenEvent(Eventus.composeEvents.editBody(3455))
      .thenEvent(Eventus.composeEvents.sendMessage())

    const evaluations: Evaluation<any, null>[] = [
      new ComposeAttachmentsCountEvaluation(),
      new ComposeHasAttachmentsEvaluation(),
      new ComposeIsPhotomailEvaluation(),
      new ComposePhotomailLengthEvaluation(),
      new ComposeBodyLengthEvaluation(),
      new ComposeEditBodyEvaluation(),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [1, true, true, 3849, 3455, true])
    done()
  })
})
