import * as assert from 'assert'
import { findAllOccurrences, removeTag } from '../code/utils/string-utils'

describe('Find all occurrences', () => {
  it('when there is no occurrences', () => {
    assert.deepStrictEqual(findAllOccurrences('hello', 'lle'), [])
  })

  it('when there is one occurrence', () => {
    assert.deepStrictEqual(findAllOccurrences('t<div>ta', '<div>'), [1])
  })

  it('when there is two occurrences', () => {
    assert.deepStrictEqual(findAllOccurrences('t<div>ta<div>', '<div>'), [1, 8])
    assert.deepStrictEqual(findAllOccurrences('t<div><div><div', '<div>'), [1, 6])
  })
})

describe('Removes all tags', () => {
  it('when there is no tags', () => {
    assert.deepStrictEqual(removeTag('hello', 'div'), 'hello')
  })

  it('when there is one tag', () => {
    assert.deepStrictEqual(removeTag('a<div>b</div>c', 'div'), 'ac')
  })

  it('when there are two parallel tags', () => {
    assert.deepStrictEqual(removeTag('a<div>b</div>c<div>d</div>e', 'div'), 'ace')
  })

  it('when there are recursive tags', () => {
    assert.deepStrictEqual(removeTag('a<div>b<div>c</div>d</div>e', 'div'), 'ae')
  })

  it('when there are recursive and parallel tags', () => {
    assert.deepStrictEqual(removeTag('a<div>b<div>c</div>d</div>e<div>f<div>g</div>h</div>i', 'div'), 'aei')
  })
})
