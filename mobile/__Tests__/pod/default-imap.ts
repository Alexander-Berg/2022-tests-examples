import Connection, { MailBoxes } from 'imap'
import { promise } from '../../../../common/xpromise-support'
import { Nullable, range, undefinedToNull, YSError } from '../../../../common/ys'
import { XPromise } from '../../../common/code/promise/xpromise'
import { FolderName } from '../../code/mail/feature/folder-list-features'
import { Imap, ImapFolderDisplay, ImapFolderInfo, ImapProvider } from '../../code/mail/imap'
import { AttachmentSpec, MailAccountSpec, MessageSpec, UserSpec } from '../../code/mail/mailbox-preparer'
import { DefaultFolderName, FolderBackendName } from '../../code/mail/model/folder-data-model'

export class DefaultImap implements Imap {
  private connection: Connection = new Connection({ user: '', password: '' })

  public constructor(account: MailAccountSpec) {
    this.connection = new Connection({
      authTimeout: 60000,
      autotls: 'always',
      connTimeout: 60000,
      debug: console.log,
      host: account.host,
      password: account.password,
      port: 993,
      tls: true,
      user: account.login,
    })
  }

  public connect(cb: (error: Nullable<YSError>) => void): void {
    this.connection.connect()
    this.connection.on('ready', this.convertError(cb))
    this.connection.on('error', this.convertError(cb))
  }

  public appendMessage(folder: string, message: MessageSpec, cb: (error: Nullable<YSError>) => void): void {
    const messageData = this.serialize(message)
    this.connection.append(messageData, { mailbox: folder, date: message.timestamp }, this.convertError(cb))
  }

  public deleteFolder(folder: string, cb: (error: Nullable<YSError>) => void): void {
    this.connection.delBox(folder, this.convertError(cb))
  }

  public deleteMessages(folder: string, messageCount: number, cb: (error: Nullable<YSError>) => void): void {
    this.connection.seq.addFlags(`1:${messageCount}`, '\\Deleted', this.convertError(cb))
  }

  public expungeFolder(folder: string, cb: (error: Nullable<YSError>) => void): void {
    this.connection.closeBox(true, this.convertError(cb))
  }

  public fetchAllFolders(): XPromise<ImapFolderDisplay[]> {
    return promise((resolve, reject) => {
      this.connection.getBoxes((err, mailboxes) => {
        if (err !== undefined) {
          reject(new YSError(err.message))
          return
        }
        const result = []
        // eslint-disable-next-line
        for (const box in mailboxes) {
          result.push(new ImapFolderDisplay(box))
        }
        if (this.isTemplateFolderExists(mailboxes)) {
          result.push(new ImapFolderDisplay(FolderBackendName.templates))
        }
        resolve(result)
      })
    })
  }

  private isTemplateFolderExists(mailboxes: MailBoxes): boolean {
    return (
      undefinedToNull(mailboxes[DefaultFolderName.draft]) !== null &&
      mailboxes[DefaultFolderName.draft].children !== null &&
      undefinedToNull(mailboxes[DefaultFolderName.draft].children.template) !== null
    )
  }

  public disconnect(cb: (error: Nullable<Error>) => void): void {
    this.connection.end()
    cb(null)
  }

  public createFolder(folder: FolderName, cb: (error: Nullable<YSError>) => void): void {
    this.connection.addBox(folder, this.convertError(cb))
  }

  public openFolder(folder: FolderName): XPromise<ImapFolderInfo> {
    return promise((resolve, reject) => {
      this.connection.openBox(folder, (err, mailbox) => {
        if (undefinedToNull(err) !== null) {
          reject(err)
        } else {
          console.log(`[OPEN] Opened folder ${folder}`)
          resolve(new ImapFolderInfo(mailbox.messages.total))
        }
      })
    })
  }

  private convertError(cb: (error: Nullable<YSError>) => void): (error: Error | undefined) => void {
    return (error) => {
      if (error !== undefined) {
        cb(new YSError(error.message))
      } else {
        cb(null)
      }
    }
  }

  private serialize(message: MessageSpec): string {
    let s = ''
    s += this.serializeFrom(message.sender)
    s += this.serializeSubject(message.subject)
    s += 'Content-Type: multipart/mixed; boundary="--boundary_text_string"\r\n\r\n'
    s += this.serializeToReceivers(message.toReceivers)
    s += '----boundary_text_string\r\n'
    s += this.serializeBody(message.textBody)
    s += this.serializeAttachments(message.attachments)
    s += '----boundary_text_string--\r\n'
    return s
  }

  private serializeFrom(from: UserSpec): string {
    return `From: ${from.name} ` + '<' + `${from.email}` + '>\r\n'
  }

  private serializeSubject(subject: string): string {
    return `Subject: ${subject}\r\n`
  }

  private serializeToReceivers(toRecievers: UserSpec[]): string {
    let toMessage = `To: `
    for (const i of range(0, toRecievers.length)) {
      const userInfo: UserSpec = toRecievers[i]
      toMessage += `${userInfo.name} ` + '<' + `${userInfo.email}` + '>'
      if (i !== toRecievers.length - 1) {
        toMessage += `, `
      }
    }
    return `${toMessage}\r\n`
  }

  private serializeAttachments(attachments: AttachmentSpec[]): string {
    let toMessage = ''
    for (const i of range(0, attachments.length)) {
      const attachment: AttachmentSpec = attachments[i]

      toMessage += '\r\n----boundary_text_string\r\n'
      toMessage += `Content-Disposition: attachment; filename="${attachment.title}"\r\n`
      toMessage += `Content-Transfer-Encoding: base64\r\n`
      toMessage += `Content-Type: ${attachment.contentType}; name="${attachment.title}"\r\n\r\n`
      toMessage += `${attachment.contentBase64}\r\n`
    }
    return toMessage
  }

  private serializeBody(messageBody: string): string {
    let s = 'Content-Type: text/html; charset=UTF-8\r\n'
    s += `\r\n${messageBody}\r\n`
    return s
  }
}

export class DefaultImapProvider implements ImapProvider {
  public provide(account: MailAccountSpec): Imap {
    return new DefaultImap(account)
  }
}
