import * as assert from 'assert'
import { MessageBody } from '../code/model/message-body'
import { CreateNewMailScenario } from '../code/scenario/mails/create-new-mail'
import { GoThroughMailsScenario } from '../code/scenario/mails/go-through-mails'
import { MailProcessingScenario } from '../code/scenario/mails/mail-processing'
import { ScenarioSerializer } from '../code/scenario/scenario-serializer'
import { RequestPerformerMock } from './__mock__/request-performer.mock'
import { buildMessage, defaultMid, messageBodies, messagesList, scenariosStack } from './utils.test'

describe('ScenarioSerializer', () => {
  it('serialize/de GoThroughMailsScenario', () => {
    const requestPerformer = new RequestPerformerMock()
    const serializer = new ScenarioSerializer()
    const scenario = GoThroughMailsScenario.build(
      requestPerformer,
      messagesList(defaultMid, 'Asya', 'New Year'),
      1,
      false,
    )
    const json = serializer.serializeGoThroughMailsScenario(scenario)
    const result = serializer.deserializeGoThroughMailsScenario(json, requestPerformer)
    assert.deepStrictEqual(scenario, result)
  })

  it('serialize/de MailProcessingScenario', () => {
    const requestPerformer = new RequestPerformerMock()
    const serializer = new ScenarioSerializer()
    const scenario = MailProcessingScenario.build(
      defaultMid,
      buildMessage(defaultMid, 'Asya', 'Bad Santa'),
      messageBodies('text'),
      false,
      false,
      requestPerformer,
    )
    const json = serializer.serializeMailProcessingScenario(scenario)
    const result = serializer.deserializeMailProcessingScenario(json, requestPerformer)
    assert.deepStrictEqual(scenario, result)
  })

  it('serialize/de CreateNewMailScenario', () => {
    const requestPerformer = new RequestPerformerMock()
    const serializer = new ScenarioSerializer()
    const scenario = CreateNewMailScenario.build(
      CreateNewMailScenario.State.waitingForBody,
      defaultMid,
      buildMessage(defaultMid, 'Asya', 'Ho-ho-ho'),
      new MessageBody('content', 'rfcId'),
      'Anna',
      'Christmas',
      'text',
      [],
      requestPerformer,
    )
    const json = serializer.serializeCreateNewMailScenario(scenario)
    const result = serializer.deserializeCreateNewMailScenario(json, requestPerformer)
    assert.deepStrictEqual(scenario, result)
  })

  it('serialize/deserialize', () => {
    const requestPerformer = new RequestPerformerMock()
    const serializer = new ScenarioSerializer()
    const scenarios = scenariosStack(requestPerformer)

    const json = serializer.serialize(scenarios)
    const result = serializer.deserialize(json, requestPerformer)
    assert.deepStrictEqual(scenarios, result)
  })
})
