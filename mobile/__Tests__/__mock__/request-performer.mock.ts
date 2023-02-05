import { ID } from '../../../xpackages/mapi/code/api/common/id'
import { Contact } from '../../../xpackages/mapi/code/api/entities/contact/contact'
import { RequestPerformer } from '../../code/mobapi/request-performer'
import { MessageBody } from '../../code/model/message-body'
import { MessageHeader } from '../../code/model/message-header'

export class RequestPerformerMock implements RequestPerformer {
  public deleteMessagePromise: (mid: ID) => Promise<void> = () =>
    Promise.reject('`deleteMessage` should not be called accidentally')
  public loadLastUnreadMessagesPromise: () => Promise<readonly MessageHeader[]> = () =>
    Promise.reject('`loadLastUnreadMessages` should not be called accidentally')
  public loadMessagePromise: (mid: ID) => Promise<MessageHeader> = () =>
    Promise.reject('`loadMessage` should not be called accidentally')
  public loadMessageBodiesPromise: (mid: ID) => Promise<MessageBody> = () =>
    Promise.reject('`loadMessageBodies` should not be called accidentally')
  public markAsImportantMessagePromise: (mid: ID) => Promise<void> = () =>
    Promise.reject('`markAsImportantMessage` should not be called accidentally')
  public markAsReadMessagePromise: (mid: ID) => Promise<void> = () =>
    Promise.reject('`markAsReadMessage` should not be called accidentally')
  public sendMessagePromise: (
    to: string,
    subject: string,
    body: string,
    replyMid: ID | null,
    references: string | null,
  ) => Promise<void> = () => Promise.reject('`sendMessage` should not be called accidentally')
  public saveMessageDraftPromise: (
    to: string,
    subject: string,
    body: string,
    replyMid: ID | null,
    references: string | null,
  ) => Promise<void> = () => Promise.reject('`sendMessageDraft` should not be called accidentally')
  public loadContactPromise: (name: string) => Promise<readonly Contact[]> = () =>
    Promise.reject('`loadContact` should not be called accidentally')

  public deleteMessage(mid: ID): Promise<void> {
    return this.deleteMessagePromise(mid)
  }

  public loadLastUnreadMessages(_first: number, _last: number): Promise<readonly MessageHeader[]> {
    return this.loadLastUnreadMessagesPromise()
  }

  public loadMessage(mid: ID): Promise<MessageHeader> {
    return this.loadMessagePromise(mid)
  }

  public loadMessageBodies(mid: ID): Promise<MessageBody> {
    return this.loadMessageBodiesPromise(mid)
  }

  public markAsImportantMessage(mid: ID): Promise<void> {
    return this.markAsImportantMessagePromise(mid)
  }

  public markAsReadMessage(mid: ID): Promise<void> {
    return this.markAsReadMessagePromise(mid)
  }

  public loadContact(name: string): Promise<readonly Contact[]> {
    return this.loadContactPromise(name)
  }

  public sendMessage(
    to: string,
    subject: string,
    body: string,
    replyMid: ID | null,
    references: string | null,
  ): Promise<void> {
    return this.sendMessagePromise(to, subject, body, replyMid, references)
  }

  public saveMessageDraft(
    to: string,
    subject: string,
    body: string,
    replyMid: ID | null,
    references: string | null,
  ): Promise<void> {
    return this.saveMessageDraftPromise(to, subject, body, replyMid, references)
  }
}
