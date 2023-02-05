/* eslint-disable @typescript-eslint/no-var-requires */
import { AnalyticsInfo } from '../../code/scenario/analytics/analytics-info'
import { SemanticFrame, semanticFrameDescription } from '../../code/scenario/semantic-frame'
import * as assert from 'assert'

const scenarioRequest = require('../../code/proto/alice/megamind/protos/scenarios/request_pb')
const scenarioResponse = require('../../code/proto/alice/megamind/protos/scenarios/response_pb')
const semanticFrame = require('../../code/proto/alice/megamind/protos/common/frame_pb')

export function protobufRunRequest(responseBuffer: Buffer, frame: SemanticFrame): Buffer {
  const response = scenarioResponse.TScenarioRunResponse.deserializeBinary(responseBuffer)
  const responseBody = response.getResponsebody()
  const baseRequest = new scenarioRequest.TScenarioBaseRequest()
  if (responseBody.hasState()) {
    baseRequest.setState(responseBody.getState())
  }
  const input = new scenarioRequest.TInput()
  input.setSemanticframesList([new semanticFrame.TSemanticFrame().setName(frame)])
  if (responseBody.hasLayout() && responseBody.getLayout().getCardsList().length > 0) {
    const card = responseBody.getLayout().getCardsList()[0]
    const text = new scenarioRequest.TInput.TText().setRawutterance(card.getText())
    input.setText(text)
  }
  const request = new scenarioRequest.TScenarioRunRequest()
  request.setBaserequest(baseRequest)
  request.setInput(input)
  const serializeBinary = request.serializeBinary()
  return Buffer.from(serializeBinary)
}

export function shouldHasCorrectAnalyticsInfo(responseBuffer: Buffer, analyticsInfo: AnalyticsInfo): void {
  const response = scenarioResponse.TScenarioRunResponse.deserializeBinary(responseBuffer)
  const info = response.getResponsebody().getAnalyticsinfo()
  assert.deepStrictEqual(info.getIntent(), analyticsInfo.intent)
}

export function shouldCommitAfterRunRequest(responseBuffer: Buffer): boolean {
  const response = scenarioResponse.TScenarioRunResponse.deserializeBinary(responseBuffer)
  return response.hasCommitcandidate()
}

export function isProtobufRunRequestIrrelevant(responseBuffer: Buffer): boolean {
  const response = scenarioResponse.TScenarioRunResponse.deserializeBinary(responseBuffer)
  if (response.hasFeatures()) {
    return response.getFeatures().getIsirrelevant()
  }
  return false
}

export function parseErrorRunResponse(responseBuffer: Buffer): string | null {
  const response = scenarioResponse.TScenarioRunResponse.deserializeBinary(responseBuffer)
  if (response.hasError()) {
    return response.getError().getMessage()
  }
  return null
}

export function parseFrameActionRunResponse(responseBuffer: Buffer, frame: SemanticFrame): string | null {
  const response = scenarioResponse.TScenarioRunResponse.deserializeBinary(responseBuffer)
  const frameactionsMap = response.getResponsebody().getFrameactionsMap()
  const frameActionsID = `mail.scenario.type.text.${semanticFrameDescription(frame)}`
  const frameaction = frameactionsMap.get(frameActionsID)
  if (frameaction === null) {
    return null
  }
  const directives = frameaction.getDirectives().getListList()
  if (directives.length === 0) {
    return null
  }
  const textTypeDirective = directives[0].getTypetextdirective()
  return textTypeDirective.getText()
}

export function isCommitResponseSuccess(responseBuffer: Buffer): boolean {
  const response = scenarioResponse.TScenarioCommitResponse.deserializeBinary(responseBuffer)
  return response.hasSuccess()
}

export function parseErrorCommitResponse(responseBuffer: Buffer): string | null {
  const response = scenarioResponse.TScenarioCommitResponse.deserializeBinary(responseBuffer)
  if (response.hasError()) {
    return response.getError().getMessage()
  }
  return null
}

export function hasProtobufRunRequestBody(responseBuffer: Buffer): boolean {
  const response = scenarioResponse.TScenarioRunResponse.deserializeBinary(responseBuffer)
  return response.hasResponsebody()
}
