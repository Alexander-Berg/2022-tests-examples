import { AccountSettingsKeys } from '../../../../code/busilogics/settings/settings-saver'
import { MailboxRevisionManager } from '../../../../code/busilogics/sync/mailbox-revision-manager'
import { MockSharedPreferences } from '../../../../../common/__tests__/__helpers__/preferences-mock'

describe(MailboxRevisionManager, () => {
  it('should be able to load mailbox revision', () => {
    const manager = new MailboxRevisionManager(
      new MockSharedPreferences(new Map([[AccountSettingsKeys.mailboxRevision.toString(), 1000]])),
    )
    expect(manager.load()).toBe(1000)
  })
  it('should return 0 if mailbox revision is not present', () => {
    const manager = new MailboxRevisionManager(new MockSharedPreferences(new Map()))
    expect(manager.load()).toBe(0)
  })
  it('should save mailbox revision', () => {
    const prefs = new Map()
    const manager = new MailboxRevisionManager(new MockSharedPreferences(prefs))
    manager.save(1000)
    expect(prefs.get(AccountSettingsKeys.mailboxRevision.toString())).toBe(1000)
  })
})
