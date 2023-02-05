import * as assert from 'assert'
import { getFormByNumber, WordForm } from '../../code/utils/word-form'

describe('WordForm', () => {
  it('should return one form', () => {
    const numbers: number[] = [1, 21, 31, 41, 51, 61, 71, 81, 91, 101, 401, 1001, 4261, 7671]
    for (const n of numbers) {
      const form = getFormByNumber(n)
      assert.strictEqual(WordForm.one, form)
    }
  })

  it('should return few form', () => {
    const numbers: number[] = [2, 3, 4, 22, 34, 32, 52, 64, 72, 83, 92, 94, 122, 403, 1024, 5662, 103674]
    for (const n of numbers) {
      const form = getFormByNumber(n)
      assert.strictEqual(WordForm.few, form)
    }
  })

  it('should return many form', () => {
    const numbers: number[] = [
      0,
      5,
      6,
      7,
      8,
      9,
      10,
      11,
      12,
      13,
      14,
      15,
      16,
      17,
      18,
      19,
      20,
      28,
      37,
      89,
      129,
      2811,
      3900,
    ]
    for (const n of numbers) {
      const form = getFormByNumber(n)
      assert.strictEqual(WordForm.many, form)
    }
  })
})
