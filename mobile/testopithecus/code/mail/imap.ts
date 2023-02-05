import { Int32, Nullable, YSError } from '../../../../common/ys'
import { XPromise } from '../../../common/code/promise/xpromise'
import { MailAccountSpec, MessageSpec } from './mailbox-preparer'

export interface Imap {
  connect(cb: (error: Nullable<YSError>) => void): void
  appendMessage(folder: string, message: MessageSpec, cb: (error: Nullable<YSError>) => void): void
  createFolder(folder: string, cb: (error: Nullable<YSError>) => void): void
  deleteMessages(folder: string, messageCount: Int32, cb: (error: Nullable<YSError>) => void): void
  deleteFolder(folder: string, cb: (error: Nullable<YSError>) => void): void

  openFolder(folder: string): XPromise<ImapFolderInfo>
  expungeFolder(folder: string, cb: (error: Nullable<YSError>) => void): void

  fetchAllFolders(): XPromise<ImapFolderDisplay[]>

  disconnect(cb: (error: Nullable<YSError>) => void): void
}

export class ImapFolderInfo {
  public constructor(public readonly messageCount: Int32) {}
}

export class ImapFolderDisplay {
  public constructor(public readonly name: string) {}
}

export interface ImapProvider {
  provide(account: MailAccountSpec): Imap
}
