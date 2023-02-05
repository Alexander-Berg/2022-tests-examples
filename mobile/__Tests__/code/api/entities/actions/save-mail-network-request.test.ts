import { JsonRequestEncoding, NetworkMethod } from '../../../../../../common/code/network/network-request'
import { SaveMailNetworkRequest } from '../../../../../code/api/entities/actions/save-mail-network-request'
import { MailSendRequest, MailSendRequestBuilder } from '../../../../../code/api/entities/draft/mail-send-request'
import { JSONItemFromJSON } from '../../../../../../common/__tests__/__helpers__/json-helpers'
import { NetworkAPIVersions } from '../../../../../code/api/mail-network-request'

describe(SaveMailNetworkRequest, () => {
  it('should build Store Mail request', () => {
    const mailSendRequest: MailSendRequest = new MailSendRequestBuilder()
      .setComposeCheck('composeCheck')
      .setCc('second_friend@yandex.ru')
      .setBcc('hidden_frient@yandex.ru')
      .setBody('Hello! This is Body')
      .setInReplyTo('in reply to mail')
      .setParts(['part1', 'part2'])
      .setReply('reply')
      .setForward('forward')
      .setAttachIds(['attachId1', 'attachId2'])
      .setSubject('Very important subject')
      .setTo('friend@yandex.ru')
      .setFromName('Me')
      .setFromMailbox('me@yandex.ru')
      .build()
    const request = new SaveMailNetworkRequest(mailSendRequest)
    expect(request.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        compose_check: 'composeCheck',
        subj: 'Very important subject',
        to: 'friend@yandex.ru',
        cc: 'second_friend@yandex.ru',
        bcc: 'hidden_frient@yandex.ru',
        from_name: 'Me',
        from_mailbox: 'me@yandex.ru',
        send: 'Hello! This is Body',
        inreplyto: 'in reply to mail',
        parts: ['part1', 'part2'],
        reply: 'reply',
        forward: 'forward',
        att_ids: ['attachId1', 'attachId2'],
        ttype: 'html',
      }),
    )
    expect(request.path()).toBe('store')
    expect(request.urlExtra().asMap().size).toBe(0)
    expect(request.version()).toStrictEqual(NetworkAPIVersions.v1)
  })
})
