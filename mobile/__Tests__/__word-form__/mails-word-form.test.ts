import * as assert from 'assert'
import { getTextByWordFormForUnreadMails, WordForm } from '../../code/utils/word-form'

describe('Unread mail scenario', () => {
  it('should return correct word form', () => {
    assert.strictEqual(getTextByWordFormForUnreadMails(WordForm.one), 'непрочитанное письмо')
    assert.strictEqual(getTextByWordFormForUnreadMails(WordForm.few), 'непрочитанных письма')
    assert.strictEqual(getTextByWordFormForUnreadMails(WordForm.many), 'непрочитанных писем')
  })
})
