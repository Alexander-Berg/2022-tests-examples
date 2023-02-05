import * as assert from 'assert'
import { getVoid } from '../../../xpackages/common/code/result/result'
import { Contact } from '../../../xpackages/mapi/code/api/entities/contact/contact'
import { MessageBody } from '../../code/model/message-body'
import { MilesError } from '../../code/scenario/miles-error'
import { CreateNewMailScenario } from '../../code/scenario/mails/create-new-mail'
import { IrrelevantScenarioResponse, RegularScenarioResponse } from '../../code/scenario/scenario-response'
import { SemanticFrame } from '../../code/scenario/semantic-frame'
import { RequestPerformerMock } from '../__mock__/request-performer.mock'
import { assertStrictEqualScenarioResponseToValue, buildMessage, defaultMid } from '../utils.test'

describe('CreateNewMail', () => {
  const knownFrames = [SemanticFrame.sendMail, SemanticFrame.saveDraft, SemanticFrame.discardChanges]
  const unknownFrames = Object.values(SemanticFrame).filter((element) => !knownFrames.includes(element))

  const defaultTo = 'Anna'
  const defaultSubject = 'hi'
  const defaultBody = 'text'
  const defaultMessageBody = new MessageBody('content', 'rfcId')

  // MARK :- init

  it('init without message -> message/mid is null', () => {
    const scenario = new CreateNewMailScenario(new RequestPerformerMock())
    assert.strictEqual(scenario.messageMeta(), null)
    assert.strictEqual(scenario.messageMID(), null)
    assert.strictEqual(scenario.messageTo(), null)
    assert.strictEqual(scenario.messageSubject(), null)
  })

  it('init with mid -> message is null', () => {
    const scenario = CreateNewMailScenario.buildWithMID(defaultMid, new RequestPerformerMock())
    assert.strictEqual(scenario.messageMeta(), null)
    assert.strictEqual(scenario.messageMID(), defaultMid)
    assert.strictEqual(scenario.messageTo(), null)
    assert.strictEqual(scenario.messageSubject(), null)
  })

  it('init with message -> message/mid not null', () => {
    const message = buildMessage(defaultMid, 'Anna', 'hi')
    const scenario = CreateNewMailScenario.buildWithPreloadedMessage(message, null, new RequestPerformerMock())
    assert.strictEqual(scenario.messageMeta(), message)
    assert.strictEqual(scenario.messageMID(), message.mid)
    assert.strictEqual(scenario.messageTo(), message.sender)
    assert.strictEqual(scenario.messageSubject(), message.subject)
  })

  // MARK :- run

  it('run new mail success -> return immediately', async () => {
    const scenario = new CreateNewMailScenario(new RequestPerformerMock())

    assert.strictEqual(scenario.messageMeta(), null)
    assert.strictEqual(scenario.messageMID(), null)
    assert.strictEqual(scenario.currentState(), CreateNewMailScenario.State.idle)

    try {
      const response = await scenario.run()
      assert.strictEqual(scenario.messageMeta(), null)
      assert.strictEqual(scenario.messageMID(), null)
      assert.strictEqual(scenario.currentState(), CreateNewMailScenario.State.waitingForTo)
      assertStrictEqualScenarioResponseToValue(response, CreateNewMailScenario.toText())
    } catch (e) {
      assert.fail('CreateNewMail scenario has failed')
    }
  })

  it('run with mid success -> return immediately', async () => {
    const message = buildMessage(defaultMid, 'Anna', 'hi')
    const performer = new RequestPerformerMock()
    performer.loadMessagePromise = (mid) => {
      assert.strictEqual(mid, defaultMid)
      return Promise.resolve(message)
    }
    const scenario = CreateNewMailScenario.buildWithMID(defaultMid, performer)

    assert.strictEqual(scenario.messageMeta(), null)
    assert.strictEqual(scenario.messageMID(), defaultMid)
    assert.strictEqual(scenario.currentState(), CreateNewMailScenario.State.idle)

    try {
      const response = await scenario.run()
      assert.strictEqual(scenario.messageMeta(), message)
      assert.strictEqual(scenario.messageMID(), defaultMid)
      assert.strictEqual(scenario.messageTo(), message.sender)
      assert.strictEqual(scenario.messageSubject(), message.subject)
      assert.strictEqual(scenario.currentState(), CreateNewMailScenario.State.waitingForBody)
      assertStrictEqualScenarioResponseToValue(response, CreateNewMailScenario.bodyText())
    } catch (e) {
      assert.fail('CreateNewMail scenario has failed')
    }
  })

  it('run with message success -> return immediately', async () => {
    const message = buildMessage(defaultMid, 'Anna', 'hi')
    const scenario = CreateNewMailScenario.buildWithPreloadedMessage(message, null, new RequestPerformerMock())

    assert.strictEqual(scenario.messageMeta(), message)
    assert.strictEqual(scenario.messageMID(), message.mid)
    assert.strictEqual(scenario.currentState(), CreateNewMailScenario.State.idle)

    try {
      const response = await scenario.run()
      assert.strictEqual(scenario.messageMeta(), message)
      assert.strictEqual(scenario.messageMID(), message.mid)
      assert.strictEqual(scenario.messageTo(), message.sender)
      assert.strictEqual(scenario.messageSubject(), message.subject)
      assert.strictEqual(scenario.currentState(), CreateNewMailScenario.State.waitingForBody)
      assertStrictEqualScenarioResponseToValue(response, CreateNewMailScenario.bodyText())
    } catch (e) {
      assert.fail('CreateNewMail scenario has failed')
    }
  })

  it('run fail -> error', async () => {
    const performer = new RequestPerformerMock()
    performer.loadMessagePromise = () => {
      return Promise.reject('500')
    }
    const scenario = CreateNewMailScenario.buildWithMID(defaultMid, performer)
    assert.strictEqual(scenario.currentState(), CreateNewMailScenario.State.idle)

    try {
      await scenario.run()
      assert.fail('CreateNewMail scenario should fail')
    } catch (reason) {
      assert.strictEqual(MilesError.dataLoadingFailed, reason)
      assert.strictEqual(scenario.currentState(), CreateNewMailScenario.State.idle)
    }
  })

  // MARK :- states changing

  it('state from idle to waitingForTo', async () => {
    const performer = new RequestPerformerMock()
    performer.loadContactPromise = (name) => {
      return Promise.resolve([new Contact(name, name)])
    }
    const scenario = new CreateNewMailScenario(performer)

    assert.strictEqual(scenario.currentState(), CreateNewMailScenario.State.idle)

    try {
      await scenario.run()
      assert.strictEqual(scenario.currentState(), CreateNewMailScenario.State.waitingForTo)
      await scenario.handle(SemanticFrame.createNewMail, defaultTo)
      assert.strictEqual(scenario.messageTo(), defaultTo)
      assert.strictEqual(scenario.currentState(), CreateNewMailScenario.State.waitingForSubject)
      await scenario.handle(SemanticFrame.createNewMail, defaultSubject)
      assert.strictEqual(scenario.messageSubject(), defaultSubject)
      assert.strictEqual(scenario.currentState(), CreateNewMailScenario.State.waitingForBody)
      await scenario.handle(SemanticFrame.createNewMail, defaultBody)
      assert.strictEqual(scenario.messageBody(), defaultBody)
      assert.strictEqual(scenario.currentState(), CreateNewMailScenario.State.idle)
    } catch (e) {
      assert.fail('CreateNewMail scenario has failed')
    }
  })

  // MARK :- handle unknown frames

  it('handle unknown frames without forwarding -> irrelevant response', async () => {
    for (const frame of unknownFrames) {
      const scenario = new CreateNewMailScenario(new RequestPerformerMock())

      try {
        const response = await scenario.handle(frame)
        assert.ok(response instanceof IrrelevantScenarioResponse)
      } catch (e) {
        assert.fail('CreateNewMail scenario has failed')
      }
    }
  })

  it('handle unknown frames with forwarding -> custom response', async () => {
    for (const frame of unknownFrames) {
      const scenario = new CreateNewMailScenario(new RequestPerformerMock())
      const unknownFrame = 'unknown_frame'
      scenario.forwardInvocation = () => {
        return Promise.resolve(new RegularScenarioResponse(unknownFrame))
      }

      try {
        const response = await scenario.handle(frame)
        assertStrictEqualScenarioResponseToValue(response, unknownFrame)
      } catch (e) {
        assert.fail('CreateNewMail scenario has failed')
      }
    }
  })

  // MARK :- load contact

  it('load contacts get none -> re-ask', async () => {
    const performer = new RequestPerformerMock()
    performer.loadContactPromise = () => {
      return Promise.resolve([])
    }
    const scenario = new CreateNewMailScenario(performer)

    try {
      await scenario.run()
      const response = await scenario.handle(SemanticFrame.createNewMail, defaultTo)
      assertStrictEqualScenarioResponseToValue(response, CreateNewMailScenario.emptyToText())
    } catch (e) {
      assert.fail('CreateNewMail scenario has failed')
    }
  })

  it('load contacts get none -> discard', async () => {
    const performer = new RequestPerformerMock()
    performer.loadContactPromise = () => {
      return Promise.resolve([])
    }
    const scenario = new CreateNewMailScenario(performer)

    try {
      await scenario.run()
      let response = await scenario.handle(SemanticFrame.createNewMail, defaultTo)
      assertStrictEqualScenarioResponseToValue(response, CreateNewMailScenario.emptyToText())
      response = await scenario.handle(SemanticFrame.discardChanges)
      assert.strictEqual(scenario.currentState(), CreateNewMailScenario.State.idle)
      assertStrictEqualScenarioResponseToValue(response, CreateNewMailScenario.discardText())
    } catch (e) {
      assert.fail('CreateNewMail scenario has failed')
    }
  })

  it('load contacts get one -> save and ask for subject', async () => {
    const performer = new RequestPerformerMock()
    performer.loadContactPromise = (name) => {
      return Promise.resolve([new Contact(name, name)])
    }
    const scenario = new CreateNewMailScenario(performer)

    try {
      await scenario.run()
      const response = await scenario.handle(SemanticFrame.createNewMail, defaultTo)
      assert.strictEqual(scenario.messageTo(), defaultTo)
      assert.strictEqual(scenario.currentState(), CreateNewMailScenario.State.waitingForSubject)
      assertStrictEqualScenarioResponseToValue(response, CreateNewMailScenario.subjectText())
    } catch (e) {
      assert.fail('CreateNewMail scenario has failed')
    }
  })

  it('load contacts get many -> select', async () => {
    const otherName = 'Alice'
    const performer = new RequestPerformerMock()
    performer.loadContactPromise = (name) => {
      return Promise.resolve([new Contact(name, name), new Contact(otherName, otherName)])
    }
    const scenario = new CreateNewMailScenario(performer)
    try {
      await scenario.run()
      const response = await scenario.handle(SemanticFrame.createNewMail, defaultTo)
      assert.strictEqual(scenario.messageTo(), null)
      assert.strictEqual(scenario.currentState(), CreateNewMailScenario.State.waitingForTo)
      const buildNames = [`${defaultTo} ${defaultTo}`, `${otherName} ${otherName}`]
      assertStrictEqualScenarioResponseToValue(response, CreateNewMailScenario.manyToText(buildNames))
    } catch (e) {
      assert.fail('CreateNewMail scenario has failed')
    }
  })

  // MARK :- handle known frames

  it('handle send without to -> irrelevant scenario', async () => {
    const scenario = new CreateNewMailScenario(new RequestPerformerMock())
    assert.strictEqual(scenario.messageTo(), null)
    assert.strictEqual(scenario.currentState(), CreateNewMailScenario.State.idle)

    try {
      const response = await scenario.handle(SemanticFrame.sendMail)
      assert.ok(response instanceof IrrelevantScenarioResponse)
    } catch (reason) {
      assert.fail('CreateNewMail scenario should not fail')
    }
  })

  it('handle send with to -> success text without request', async () => {
    const message = buildMessage(defaultMid, 'Anna', 'hi')
    const scenario = CreateNewMailScenario.buildWithPreloadedMessage(message, null, new RequestPerformerMock())
    assert.strictEqual(scenario.currentState(), CreateNewMailScenario.State.idle)

    try {
      const response = await scenario.handle(SemanticFrame.sendMail)
      assertStrictEqualScenarioResponseToValue(response, CreateNewMailScenario.sendText())
    } catch (e) {
      assert.fail('CreateNewMail scenario has failed')
    }
  })

  it('handle safe draft -> success text without request', async () => {
    const message = buildMessage(defaultMid, '', '')
    const scenario = CreateNewMailScenario.buildWithPreloadedMessage(message, null, new RequestPerformerMock())

    try {
      const response = await scenario.handle(SemanticFrame.saveDraft)
      assertStrictEqualScenarioResponseToValue(response, CreateNewMailScenario.saveDraftText())
    } catch (e) {
      assert.fail('CreateNewMail scenario has failed')
    }
  })

  it('handle discard -> success', async () => {
    const message = buildMessage(defaultMid, '', '')
    const scenario = CreateNewMailScenario.buildWithPreloadedMessage(message, null, new RequestPerformerMock())
    try {
      const response = await scenario.handle(SemanticFrame.discardChanges)
      assertStrictEqualScenarioResponseToValue(response, CreateNewMailScenario.discardText())
      assert.strictEqual(scenario.currentState(), CreateNewMailScenario.State.idle)
    } catch (e) {
      assert.fail('CreateNewMail scenario has failed')
    }
  })

  // MARK :- commit

  it('commit send with to -> success', async () => {
    const message = buildMessage(defaultMid, 'Anna', 'hi')
    const performer = new RequestPerformerMock()
    performer.sendMessagePromise = (to, subj, body) => {
      assert.strictEqual(to, message.sender)
      assert.strictEqual(subj, message.subject)
      assert.strictEqual(body, '')
      return Promise.resolve(getVoid())
    }
    performer.loadMessageBodiesPromise = (mid) => {
      assert.strictEqual(mid, defaultMid)
      return Promise.resolve(defaultMessageBody)
    }
    const scenario = CreateNewMailScenario.buildWithPreloadedMessage(message, null, performer)
    assert.strictEqual(scenario.currentState(), CreateNewMailScenario.State.idle)

    try {
      await scenario.handle(SemanticFrame.sendMail)
      assert.strictEqual(scenario.currentState(), CreateNewMailScenario.State.idle)
    } catch (e) {
      assert.fail('CreateNewMail scenario has failed')
    }
  })

  it('commit send with to -> failed', async () => {
    const message = buildMessage(defaultMid, 'Anna', 'hi')
    const performer = new RequestPerformerMock()
    performer.sendMessagePromise = () => {
      return Promise.reject('500')
    }
    performer.loadMessageBodiesPromise = (_) => {
      return Promise.resolve(defaultMessageBody)
    }
    const scenario = CreateNewMailScenario.buildWithPreloadedMessage(message, null, performer)

    try {
      await scenario.commit(SemanticFrame.sendMail)
      assert.fail('CreateNewMail scenario should fail')
    } catch (reason) {
      assert.strictEqual(MilesError.dataLoadingFailed, reason)
    }
  })

  it('commit safe draft -> success', async () => {
    const message = buildMessage(defaultMid, '', '')
    const performer = new RequestPerformerMock()
    performer.saveMessageDraftPromise = () => {
      return Promise.resolve(getVoid())
    }
    performer.loadMessageBodiesPromise = (_) => {
      return Promise.resolve(defaultMessageBody)
    }
    const scenario = CreateNewMailScenario.buildWithPreloadedMessage(message, null, performer)

    try {
      await scenario.commit(SemanticFrame.saveDraft)
      assert.strictEqual(scenario.currentState(), CreateNewMailScenario.State.idle)
    } catch (e) {
      assert.fail('CreateNewMail scenario has failed')
    }
  })

  it('commit safe draft -> fail', async () => {
    const message = buildMessage(defaultMid, '', '')
    const performer = new RequestPerformerMock()
    performer.saveMessageDraftPromise = () => {
      return Promise.reject('500')
    }
    performer.loadMessageBodiesPromise = (_) => {
      return Promise.resolve(defaultMessageBody)
    }
    const scenario = CreateNewMailScenario.buildWithPreloadedMessage(message, null, performer)

    try {
      await scenario.commit(SemanticFrame.saveDraft)
      assert.fail('CreateNewMail scenario should fail')
    } catch (reason) {
      assert.strictEqual(MilesError.dataLoadingFailed, reason)
    }
  })

  // MARK :- cancel scenario

  it('handle send with to -> cancel scenario', (done) => {
    const message = buildMessage(defaultMid, '', '')
    const performer = new RequestPerformerMock()
    performer.sendMessagePromise = () => {
      return Promise.resolve(getVoid())
    }
    performer.loadMessageBodiesPromise = () => {
      return Promise.resolve(defaultMessageBody)
    }
    const scenario = CreateNewMailScenario.buildWithPreloadedMessage(message, null, performer)
    scenario.cancellation = () => {
      return done()
    }

    scenario.handle(SemanticFrame.sendMail).catch((e) => {
      assert.fail(e)
      done()
    })
  })

  it('handle safe draft -> cancel scenario', (done) => {
    const message = buildMessage(defaultMid, '', '')
    const performer = new RequestPerformerMock()
    performer.saveMessageDraftPromise = () => {
      return Promise.resolve(getVoid())
    }
    performer.loadMessageBodiesPromise = (_) => {
      return Promise.resolve(defaultMessageBody)
    }
    const scenario = CreateNewMailScenario.buildWithPreloadedMessage(message, null, performer)
    scenario.cancellation = () => {
      return done()
    }

    scenario.handle(SemanticFrame.saveDraft).catch(() => {
      assert.fail('CreateNewMail scenario has failed')
      done()
    })
  })

  it('handle discard -> cancel scenario', (done) => {
    const message = buildMessage(defaultMid, '', '')
    const scenario = CreateNewMailScenario.buildWithPreloadedMessage(message, null, new RequestPerformerMock())
    scenario.cancellation = () => {
      return done()
    }

    scenario.handle(SemanticFrame.discardChanges).catch(() => {
      assert.fail('CreateNewMail scenario has failed')
      done()
    })
  })
})
