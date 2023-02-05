import * as assert from 'assert'
import { PseudoRandomProvider } from '../../code/utils/pseudo-random'
import { Fuzzer } from '../../code/fuzzing/fuzzer'

describe('fuzzer', () => {
  it(' should generate random email', (done) => {
    const fuzzer = new Fuzzer(PseudoRandomProvider.INSTANCE)
    const generated = fuzzer.naughtyString(10)
    console.log(generated)
    assert.ok(generated.length >= 10)
    done()
  })
})
