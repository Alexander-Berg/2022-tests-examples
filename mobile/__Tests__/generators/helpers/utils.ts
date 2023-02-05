import { removeDuplicates, zip } from '../../../src/generators/__helpers__/utils'

describe('Utils function', () => {
  it('`removeDuplicates` should remove duplicates by specific key', () => {
    const person1 = { name: 'name1', age: 1, city: 'city1' }
    const person2 = { name: 'name2', age: 1, city: 'city1' }
    const person3 = { name: 'name3', age: 1, city: 'city2' }
    const person4 = { name: 'name4', age: 1, city: 'city2' }
    const persons = [person1, person2, person3, person4]

    const resultByName = removeDuplicates(persons, 'name')
    const resultByAge = removeDuplicates(persons, 'age')
    const resultByCity = removeDuplicates(persons, 'city')

    const expextedByName = [person1, person2, person3, person4]
    const expextedByAge = [person1]
    const expextedByCity = [person1, person3]

    expect(resultByName).toEqual(expextedByName)
    expect(resultByAge).toEqual(expextedByAge)
    expect(resultByCity).toEqual(expextedByCity)
  })

  it('`zip` correctly merges arrays', () => {
    expect(zip(['foo', 'bar'], ['apples', 'grapes'])).toEqual([
      ['foo', 'apples'],
      ['bar', 'grapes'],
    ])
    expect(zip(['foo', 'bar'], ['apples', 'grapes'], ['a', 'b', 'c'])).toEqual([
      ['foo', 'apples', 'a'],
      ['bar', 'grapes', 'b'],
      [undefined, undefined, 'c'],
    ])
  })
})
