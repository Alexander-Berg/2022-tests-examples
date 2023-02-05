import { resolve } from '../../../../../../common/xpromise-support'
import { int64 } from '../../../../../../common/ys'
import { EntityKind } from '../../../../../mapi/code/api/entities/entity-kind'
import {
  getLastAutoincrementedId,
  queryPlaceholders,
  queryValuesFromIds,
} from '../../../../code/api/storage/query-helpers'
import { MockCursorWithArray, MockStorage } from '../../../__helpers__/mock-patches'
import { TestIDSupport } from '../../../__helpers__/test-id-support'

describe(queryPlaceholders, () => {
  it('should build placeholders', () => {
    expect(queryPlaceholders([])).toBe('')
    expect(queryPlaceholders([1])).toBe('?')
    expect(queryPlaceholders([1, 2, 3])).toBe('?, ?, ?')
    expect(queryPlaceholders(['a', 'b', 'c'])).toBe('?, ?, ?')
  })
})

describe(queryValuesFromIds, () => {
  it('should build query values', () => {
    expect(queryValuesFromIds([], new TestIDSupport(), false)).toBe('')
    expect(queryValuesFromIds([int64(1)], new TestIDSupport(), false)).toBe('1')
    expect(queryValuesFromIds([int64(1), int64(2), int64(3)], new TestIDSupport(), false)).toBe('1, 2, 3')

    expect(queryValuesFromIds([], new TestIDSupport(), true)).toBe('')
    expect(queryValuesFromIds([int64(1)], new TestIDSupport(), true)).toBe('"1"')
    expect(queryValuesFromIds([int64(1), int64(2), int64(3)], new TestIDSupport(), true)).toBe('"1", "2", "3"')
  })
})

describe(getLastAutoincrementedId, () => {
  it('should return value if it exists', (done) => {
    const mockRunQuery = jest.fn().mockReturnValue(resolve(MockCursorWithArray([[int64(555)]])))
    const storage = MockStorage({
      runQuery: mockRunQuery,
    })

    expect.assertions(2)
    getLastAutoincrementedId(storage, EntityKind.draft_attach).then((id) => {
      expect(id).toBe(int64(555))
      expect(mockRunQuery).toBeCalledWith('SELECT seq FROM sqlite_sequence WHERE name = "draft_attach"', [])
      done()
    })
  })

  it('should return null if it does not exist', (done) => {
    const mockRunQuery = jest.fn().mockReturnValue(resolve(MockCursorWithArray([])))
    const storage = MockStorage({
      runQuery: mockRunQuery,
    })

    expect.assertions(2)
    getLastAutoincrementedId(storage, EntityKind.draft_attach).then((id) => {
      expect(id).toBeNull()
      expect(mockRunQuery).toBeCalledWith('SELECT seq FROM sqlite_sequence WHERE name = "draft_attach"', [])
      done()
    })
  })
})
