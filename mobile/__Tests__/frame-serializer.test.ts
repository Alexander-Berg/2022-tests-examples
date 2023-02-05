import * as assert from 'assert'
import { ScenarioSerializer } from '../code/scenario/scenario-serializer'
import { SemanticFrame } from '../code/scenario/semantic-frame'
import { StateSerializer } from '../code/state-serializer'

describe('FrameSerializer', () => {
  it('serialize/de semantic frame', () => {
    const serializer = new StateSerializer(new ScenarioSerializer())
    for (const frame of Object.values(SemanticFrame)) {
      const json = serializer.serialize(frame, [])
      const result = serializer.deserializeFrame(json)
      assert.strictEqual(frame, result)
    }
  })
})
