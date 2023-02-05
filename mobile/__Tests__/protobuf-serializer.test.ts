import * as assert from 'assert'
import { CommitSerializer, RunSerializer } from '../code/protobuf-serializer'
import { AnalyticsInfo } from '../code/scenario/analytics/analytics-info'
import { ProtobufAnalyticsSerializer } from '../code/scenario/analytics/protobuf-analytics-serializer'
import {
  InternalScenarioResponse,
  IrrelevantScenarioResponse,
  RegularScenarioResponse,
} from '../code/scenario/scenario-response'
import { ScenarioSerializer } from '../code/scenario/scenario-serializer'
import { SemanticFrame, semanticFrameDescription } from '../code/scenario/semantic-frame'
import { StateSerializer } from '../code/state-serializer'
import { AuthorizationError } from '../code/tvm-client'
import {
  hasProtobufRunRequestBody,
  isCommitResponseSuccess,
  isProtobufRunRequestIrrelevant,
  parseErrorCommitResponse,
  parseErrorRunResponse,
  parseFrameActionRunResponse,
  protobufRunRequest,
  shouldCommitAfterRunRequest,
  shouldHasCorrectAnalyticsInfo,
} from './__mock__/protobuf.bulder.mock'
import { RequestPerformerMock } from './__mock__/request-performer.mock'
import { scenariosStack } from './utils.test'

describe('ProtobufSerializer', () => {
  // MARK :- run response success

  it('serialize/de run response state', () => {
    const requestPerformer = new RequestPerformerMock()
    const protobufSerializer = new RunSerializer(
      new StateSerializer(new ScenarioSerializer()),
      new ProtobufAnalyticsSerializer(),
    )
    const scenarios = scenariosStack(requestPerformer)

    const scenarioResponse = new RegularScenarioResponse('text')
    const buffer = protobufSerializer.serializeResponse(
      SemanticFrame.goThroughMails,
      scenarios,
      null,
      scenarioResponse,
      true,
    )
    const request = protobufRunRequest(buffer, SemanticFrame.unreadCount)
    const parseScenarios = protobufSerializer.parseScenarios(request, requestPerformer)
    assert.deepStrictEqual(parseScenarios, scenarios)
  })

  it('serialize/de run response intent', () => {
    const protobufSerializer = new RunSerializer(
      new StateSerializer(new ScenarioSerializer()),
      new ProtobufAnalyticsSerializer(),
    )
    const scenarios = scenariosStack(new RequestPerformerMock())
    const frame = SemanticFrame.goThroughMails
    const text = 'If you need to convert to Base64 you could do so using Buffer'

    const scenarioResponse = new RegularScenarioResponse(text)
    const buffer = protobufSerializer.serializeResponse(
      SemanticFrame.goThroughMails,
      scenarios,
      null,
      scenarioResponse,
      true,
    )
    const request = protobufRunRequest(buffer, frame)
    const intent = protobufSerializer.parseIntent(request, null)
    assert.strictEqual(intent[0], frame)
    assert.strictEqual(intent[1], text)
  })

  it('serialize/de relevant run response', () => {
    const protobufSerializer = new RunSerializer(
      new StateSerializer(new ScenarioSerializer()),
      new ProtobufAnalyticsSerializer(),
    )
    const scenarios = scenariosStack(new RequestPerformerMock())

    const scenarioResponse = new RegularScenarioResponse('text')
    const buffer = protobufSerializer.serializeResponse(
      SemanticFrame.goThroughMails,
      scenarios,
      null,
      scenarioResponse,
      true,
    )
    assert.ok(!isProtobufRunRequestIrrelevant(buffer))
  })

  it('serialize/de relevant run response has analytics info', () => {
    const protobufSerializer = new RunSerializer(
      new StateSerializer(new ScenarioSerializer()),
      new ProtobufAnalyticsSerializer(),
    )
    const scenarios = scenariosStack(new RequestPerformerMock())

    const analyticsInfo = AnalyticsInfo.empty()
    const scenarioResponse = new RegularScenarioResponse('text', analyticsInfo)
    const buffer = protobufSerializer.serializeResponse(
      SemanticFrame.goThroughMails,
      scenarios,
      null,
      scenarioResponse,
      true,
    )
    shouldHasCorrectAnalyticsInfo(buffer, analyticsInfo)
  })

  it('serialize/de run response with commit', () => {
    const protobufSerializer = new RunSerializer(
      new StateSerializer(new ScenarioSerializer()),
      new ProtobufAnalyticsSerializer(),
    )
    const scenarios = scenariosStack(new RequestPerformerMock())

    const scenarioResponse = new RegularScenarioResponse('text', AnalyticsInfo.empty(), [], [], true)
    const buffer = protobufSerializer.serializeResponse(
      SemanticFrame.goThroughMails,
      scenarios,
      scenarios[0],
      scenarioResponse,
      true,
    )
    assert.ok(shouldCommitAfterRunRequest(buffer))
  })

  it('serialize/de run response frame action', () => {
    const protobufSerializer = new RunSerializer(
      new StateSerializer(new ScenarioSerializer()),
      new ProtobufAnalyticsSerializer(),
    )
    const scenarios = scenariosStack(new RequestPerformerMock())

    for (const frame of Object.values(SemanticFrame)) {
      const scenarioResponse = new RegularScenarioResponse('text', AnalyticsInfo.empty(), [frame])
      const buffer = protobufSerializer.serializeResponse(frame, scenarios, null, scenarioResponse, true)
      const intent = parseFrameActionRunResponse(buffer, frame)
      assert.strictEqual(intent, semanticFrameDescription(frame))
    }
  })

  // MARK :- run response irrelevant/internal

  it('serialize/de irrelevant run response', () => {
    const protobufSerializer = new RunSerializer(
      new StateSerializer(new ScenarioSerializer()),
      new ProtobufAnalyticsSerializer(),
    )
    const scenarios = scenariosStack(new RequestPerformerMock())

    const scenarioResponse = new IrrelevantScenarioResponse()
    const buffer = protobufSerializer.serializeResponse(
      SemanticFrame.goThroughMails,
      scenarios,
      null,
      scenarioResponse,
      true,
    )
    assert.ok(isProtobufRunRequestIrrelevant(buffer))
  })

  it('fail serializing for internal response', () => {
    const protobufSerializer = new RunSerializer(
      new StateSerializer(new ScenarioSerializer()),
      new ProtobufAnalyticsSerializer(),
    )
    const scenarios = scenariosStack(new RequestPerformerMock())

    assert.throws(() =>
      protobufSerializer.serializeResponse(
        SemanticFrame.goThroughMails,
        scenarios,
        null,
        new InternalScenarioResponse(),
        true,
      ),
    )
  })

  it('serialize/de error run response', () => {
    const protobufSerializer = new RunSerializer(
      new StateSerializer(new ScenarioSerializer()),
      new ProtobufAnalyticsSerializer(),
    )
    const errorMessage = 'error message'

    const buffer = protobufSerializer.serializeInvalidResponseFrom(errorMessage, SemanticFrame.createNewMail, false)
    assert.equal(parseErrorRunResponse(buffer), errorMessage)
  })

  it('serialize/de authorization error run response for root scenario', () => {
    const protobufSerializer = new RunSerializer(
      new StateSerializer(new ScenarioSerializer()),
      new ProtobufAnalyticsSerializer(),
    )
    const buffer = protobufSerializer.serializeInvalidResponseFrom(
      AuthorizationError.authorizationFailed,
      SemanticFrame.createNewMail,
      false,
    )
    assert.ok(hasProtobufRunRequestBody(buffer))
  })

  it('serialize/de authorization error run response for non root scenario', () => {
    const protobufSerializer = new RunSerializer(
      new StateSerializer(new ScenarioSerializer()),
      new ProtobufAnalyticsSerializer(),
    )

    const buffer = protobufSerializer.serializeInvalidResponseFrom(
      AuthorizationError.authorizationFailed,
      SemanticFrame.discardChanges,
      false,
    )
    assert.ok(isProtobufRunRequestIrrelevant(buffer))
  })

  // MARK :- commit response

  it('serialize/de commit response', () => {
    const protobufSerializer = new CommitSerializer(new StateSerializer(new ScenarioSerializer()))

    const buffer = protobufSerializer.serializeResponse()
    assert.ok(isCommitResponseSuccess(buffer))
  })

  it('serialize/de error commit response', () => {
    const protobufSerializer = new CommitSerializer(new StateSerializer(new ScenarioSerializer()))
    const errorMessage = 'error message'

    const buffer = protobufSerializer.serializeResponseError(errorMessage)
    assert.equal(parseErrorCommitResponse(buffer), errorMessage)
  })
})
