import * as assert from 'assert'
import { getVoid } from '../../../xpackages/common/code/result/result'
import { MessageHeader } from '../../code/model/message-header'
import { MilesError } from '../../code/scenario/miles-error'
import { MailProcessingScenario } from '../../code/scenario/mails/mail-processing'
import {
  InternalScenarioResponse,
  IrrelevantScenarioResponse,
  RegularScenarioResponse,
} from '../../code/scenario/scenario-response'
import { SemanticFrame } from '../../code/scenario/semantic-frame'
import { RequestPerformerMock } from '../__mock__/request-performer.mock'
import { RunnerMock } from '../__mock__/runner.mock'
import { assertStrictEqualScenarioResponseToValue, buildMessage, defaultMid, messageBodies } from '../utils.test'

describe('MailProcessingScenario', () => {
  const knownFrames = [
    SemanticFrame.readMail,
    SemanticFrame.replyMail,
    SemanticFrame.deleteMail,
    SemanticFrame.markAsImportantMail,
    SemanticFrame.markAsReadMail,
  ]
  const unknownFrames = Object.values(SemanticFrame).filter((element) => !knownFrames.includes(element))

  // MARK :- init

  it('init with mid -> message is null', () => {
    const scenario = new MailProcessingScenario(defaultMid, new RequestPerformerMock())
    assert.strictEqual(scenario.messageMeta(), null)
  })

  it('init with message -> message not null', () => {
    const message = buildMessage()
    const scenario = MailProcessingScenario.buildWithPreloadedMessage(message, new RequestPerformerMock())
    assert.strictEqual(scenario.messageMeta(), message)
  })

  // MARK :- run

  it('run with mid success -> load message', async () => {
    const scenario = new MailProcessingScenario(defaultMid, new RequestPerformerMock())

    try {
      const response = await scenario.run()
      assert.ok(response instanceof InternalScenarioResponse)
    } catch (e) {
      assert.fail('MailProcessing scenario has failed')
    }
  })

  it('run with message success -> load message', async () => {
    const message = buildMessage()
    const scenario = MailProcessingScenario.buildWithPreloadedMessage(message, new RequestPerformerMock())

    assert.strictEqual(scenario.messageMeta(), message)
    try {
      const response = await scenario.run()
      assert.ok(response instanceof InternalScenarioResponse)
    } catch (e) {
      assert.fail('MailProcessing scenario has failed')
    }
  })

  // MARK :- handle unknown frames

  it('handle unknown frame without forwarding -> irrelevant response', async () => {
    for (const frame of unknownFrames) {
      const message = buildMessage()
      const scenario = MailProcessingScenario.buildWithPreloadedMessage(message, new RequestPerformerMock())

      try {
        const response = await scenario.handle(frame)
        assert.ok(response instanceof IrrelevantScenarioResponse)
      } catch (e) {
        assert.fail('MailProcessing scenario has failed')
      }
    }
  })

  it('forward unknown frame -> custom response', async () => {
    for (const frame of unknownFrames) {
      const message = buildMessage()
      const scenario = MailProcessingScenario.buildWithPreloadedMessage(message, new RequestPerformerMock())
      const unknownFrame = 'unknown_frame'
      scenario.forwardInvocation = () => {
        return Promise.resolve(new RegularScenarioResponse(unknownFrame))
      }

      try {
        const response = await scenario.handle(frame)
        assertStrictEqualScenarioResponseToValue(response, unknownFrame)
      } catch (e) {
        assert.fail('MailProcessing scenario has failed')
      }
    }
  })

  // MARK :- handle known frame (request succeed)

  it('handle readMail frame -> message body context', async () => {
    const message = buildMessage(defaultMid)
    const context = 'message_body'
    const performer = new RequestPerformerMock()
    performer.loadMessageBodiesPromise = (mid) => {
      assert.strictEqual(mid, defaultMid)
      return Promise.resolve(messageBodies(context))
    }
    const scenario = MailProcessingScenario.buildWithPreloadedMessage(message, performer)

    try {
      const response = await scenario.handle(SemanticFrame.readMail)
      assertStrictEqualScenarioResponseToValue(response, context)
    } catch (e) {
      assert.fail('MailProcessing scenario has failed')
    }
  })

  it('handle reply frame -> run newMail scenario with params', async () => {
    const message = buildMessage(defaultMid, 'sender', 'subject')
    const newMailResponse = 'run_new_mail_scenario'
    const runner = new RunnerMock()
    runner.newMailScenarioPromise = (mes: MessageHeader | null) => {
      assert.strictEqual(mes, message)
      return Promise.resolve(new RegularScenarioResponse(newMailResponse))
    }
    const scenario = MailProcessingScenario.buildWithPreloadedMessage(message, new RequestPerformerMock())
    scenario.runner = runner

    try {
      const response = await scenario.handle(SemanticFrame.replyMail)
      assertStrictEqualScenarioResponseToValue(response, newMailResponse)
    } catch (e) {
      assert.fail('MailProcessing scenario has failed')
    }
  })

  it('handle delete frame -> delete text without request', async () => {
    const message = buildMessage(defaultMid)
    const scenario = MailProcessingScenario.buildWithPreloadedMessage(message, new RequestPerformerMock())

    try {
      const response = await scenario.handle(SemanticFrame.deleteMail)
      assertStrictEqualScenarioResponseToValue(response, MailProcessingScenario.deleteMailText())
    } catch (e) {
      assert.fail('MailProcessing scenario has failed')
    }
  })

  it('handle markAsRead frame -> mark text without request', async () => {
    const message = buildMessage(defaultMid)
    const scenario = MailProcessingScenario.buildWithPreloadedMessage(message, new RequestPerformerMock())

    try {
      const response = await scenario.handle(SemanticFrame.markAsReadMail)
      assertStrictEqualScenarioResponseToValue(response, MailProcessingScenario.markAsReadMailText())
    } catch (e) {
      assert.fail('MailProcessing scenario has failed')
    }
  })

  it('handle markAsImportant frame -> mark text without request', async () => {
    const message = buildMessage(defaultMid)
    const scenario = MailProcessingScenario.buildWithPreloadedMessage(message, new RequestPerformerMock())

    try {
      const response = await scenario.handle(SemanticFrame.markAsImportantMail)
      assertStrictEqualScenarioResponseToValue(response, MailProcessingScenario.markAsImportantMailText())
    } catch (e) {
      assert.fail('MailProcessing scenario has failed')
    }
  })

  // MARK :- handle known frame (request failed)

  it('fail commited known frame -> fail', async () => {
    const frames = [SemanticFrame.deleteMail, SemanticFrame.markAsReadMail, SemanticFrame.markAsImportantMail]
    for (const frame of frames) {
      const scenario = new MailProcessingScenario(defaultMid, new RequestPerformerMock())

      try {
        await scenario.commit(frame)
        assert.fail('MailProcessing scenario should fail')
      } catch (reason) {
        assert.strictEqual(MilesError.dataLoadingFailed, reason)
      }
    }
  })

  // MARK :- commit

  it('commit delete frame -> delete', async () => {
    const message = buildMessage(defaultMid)
    const performer = new RequestPerformerMock()
    performer.deleteMessagePromise = (mid) => {
      assert.strictEqual(mid, defaultMid)
      return Promise.resolve(getVoid())
    }
    const scenario = MailProcessingScenario.buildWithPreloadedMessage(message, performer)

    try {
      await scenario.commit(SemanticFrame.deleteMail)
    } catch (e) {
      assert.fail('MailProcessing scenario has failed')
    }
  })

  it('commit markAsRead frame -> mark', async () => {
    const message = buildMessage(defaultMid)
    const performer = new RequestPerformerMock()
    performer.markAsReadMessagePromise = (mid) => {
      assert.strictEqual(mid, defaultMid)
      return Promise.resolve(getVoid())
    }
    const scenario = MailProcessingScenario.buildWithPreloadedMessage(message, performer)

    try {
      await scenario.commit(SemanticFrame.markAsReadMail)
    } catch (e) {
      assert.fail('MailProcessing scenario has failed')
    }
  })

  it('commit markAsImportant frame -> mark', async () => {
    const message = buildMessage(defaultMid)
    const performer = new RequestPerformerMock()
    performer.markAsImportantMessagePromise = (mid) => {
      assert.strictEqual(mid, defaultMid)
      return Promise.resolve(getVoid())
    }
    const scenario = MailProcessingScenario.buildWithPreloadedMessage(message, performer)

    try {
      await scenario.commit(SemanticFrame.markAsImportantMail)
    } catch (e) {
      assert.fail('MailProcessing scenario has failed')
    }
  })

  // MARK :- cancel

  it('handle delete frame -> cancel scenario', (done) => {
    const message = buildMessage(defaultMid)
    const performer = new RequestPerformerMock()
    performer.deleteMessagePromise = (mid) => {
      assert.strictEqual(mid, defaultMid)
      return Promise.resolve(getVoid())
    }
    const scenario = MailProcessingScenario.buildWithPreloadedMessage(message, performer)
    scenario.cancellation = () => {
      done()
    }

    scenario.handle(SemanticFrame.deleteMail).catch(() => {
      assert.fail('MailProcessing scenario has failed')
      done()
    })
  })

  it('fail delete frame -> cancel not called', async () => {
    const message = buildMessage(defaultMid)
    const performer = new RequestPerformerMock()
    performer.deleteMessagePromise = (mid) => {
      assert.strictEqual(mid, defaultMid)
      return Promise.reject('500')
    }
    const scenario = MailProcessingScenario.buildWithPreloadedMessage(message, performer)
    scenario.cancellation = () => {
      assert.fail('Cancellation should not be called if deletion was failed')
    }

    try {
      await scenario.commit(SemanticFrame.deleteMail)
      assert.fail('MailProcessing scenario should fail')
    } catch (reason) {
      assert.strictEqual(MilesError.dataLoadingFailed, reason)
    }
  })

  // MARK : - save message & bodies

  it('handle readMail for loaded message body -> message body saved', async () => {
    const message = buildMessage(defaultMid)
    const bodies = messageBodies()
    const performer = new RequestPerformerMock()
    performer.loadMessageBodiesPromise = (mid) => {
      assert.strictEqual(mid, defaultMid)
      return Promise.resolve(bodies)
    }
    const scenario = MailProcessingScenario.buildWithPreloadedMessage(message, performer)

    assert.strictEqual(scenario.messageBody(), null)

    try {
      await scenario.handle(SemanticFrame.readMail)
      assert.strictEqual(scenario.messageBody(), bodies)
      performer.loadMessageBodiesPromise = (mid) => {
        assert.strictEqual(mid, defaultMid)
        return Promise.reject('500')
      }
      await scenario.handle(SemanticFrame.readMail)
      assert.strictEqual(scenario.messageBody(), bodies)
    } catch (e) {
      assert.fail('MailProcessing scenario has failed')
    }
  })

  it('handle replyMail for loaded message -> message body saved', async () => {
    const message = buildMessage(defaultMid, 'sender', 'subject')
    const runner = new RunnerMock()
    runner.newMailScenarioPromise = (mes: MessageHeader | null) => {
      assert.strictEqual(mes, message)
      return Promise.resolve(new RegularScenarioResponse(''))
    }
    const performer = new RequestPerformerMock()
    performer.loadMessagePromise = (mid) => {
      assert.strictEqual(mid, defaultMid)
      return Promise.resolve(message)
    }
    const scenario = new MailProcessingScenario(defaultMid, performer)
    scenario.runner = runner

    assert.strictEqual(scenario.messageMeta(), null)

    try {
      await scenario.handle(SemanticFrame.replyMail)
      assert.strictEqual(scenario.messageMeta(), message)
      performer.loadMessageBodiesPromise = (mid) => {
        assert.strictEqual(mid, defaultMid)
        return Promise.reject('500')
      }
      await scenario.handle(SemanticFrame.replyMail)
      assert.strictEqual(scenario.messageMeta(), message)
    } catch (e) {
      assert.fail('MailProcessing scenario has failed')
    }
  })
})
