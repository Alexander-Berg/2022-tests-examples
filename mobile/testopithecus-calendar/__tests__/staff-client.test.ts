import assert from 'assert'
import { onProcessStart } from '../../xpackages/testopithecus-common/__tests__/code/test-utils'
import { StaffClient } from '../code/staff-client'

onProcessStart()

describe('staff client', () => {
  it('should get persons', async () => {
    const persons = await new StaffClient().persons(1, 1)
    assert.strictEqual(persons.length, 1)
  })
})
