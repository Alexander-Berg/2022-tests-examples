import * as assert from 'assert'
import { range } from '../../../../../common/ys'
import { PseudoRandomProvider } from '../../../code/utils/pseudo-random'

describe('pseudo random', () => {
  it('should gemerate random numbers infinitely', (done) => {
    const random = PseudoRandomProvider.INSTANCE
    for (const i of range(1, 100000)) {
      assert.ok(random.generate(i) >= 0)
    }
    done()
  })
})
