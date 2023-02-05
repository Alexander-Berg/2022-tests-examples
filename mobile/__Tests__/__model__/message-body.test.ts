import * as assert from 'assert'
import {
  MessageBodyInfo,
  MessageBodyPart,
  MessageBodyPayload,
} from '../../../xpackages/mapi/code/api/entities/body/message-body'
import { MessageBody } from '../../code/model/message-body'
import { defaultMid } from '../utils.test'

describe('Message body build', () => {
  it('removes exact string if no html', () => {
    const text = 'Hello there my dear friend!'
    test(text, text)
  })

  it('removes one html tag', () => {
    const text = 'Hello there <div>my</div> dear friend!'
    const expected = 'Hello there my dear friend!'
    test(text, expected)
  })

  it('removes reqursive html tag', () => {
    const text = 'Hello there <div>my <tr>dear</tr></div> friend!'
    const expected = 'Hello there my dear friend!'
    test(text, expected)
  })

  it('removes spaces from html', () => {
    const text = `<br>
               <br>
                      <br>
                              Hello there my dear friend
                      </br>
               </br>
    <br>`
    const expected = 'Hello there my dear friend'
    test(text, expected)
  })

  it('removes unpaired html tag', () => {
    const text = 'Hello there <br>my dear friend!'
    const expected = 'Hello there my dear friend!'
    test(text, expected)
  })

  it('removes html tag with parameters', () => {
    const text = 'Hello there <div class="my-test-class">my</div> dear friend!'
    const expected = 'Hello there my dear friend!'
    test(text, expected)
  })

  it('removes quote', () => {
    const text =
      '<div>Hello there my dear friend!</div><div> </div><blockquote><br />some text<br /><br /><br />--<br />Sent from Yandex.Mail for mobile</blockquote>'
    const expected = 'Hello there my dear friend!'
    test(text, expected)
  })

  it('removes quote with line break', () => {
    const text = 'Hello there my dear <blockquote>\n some text there</blockquote>friend!'
    const expected = 'Hello there my dear friend!'
    test(text, expected)
  })

  it('removes parallel quote with', () => {
    const text =
      'Hello there my<blockquote> some text there</blockquote> dear <blockquote>\n some text there</blockquote>friend!'
    const expected = 'Hello there my dear friend!'
    test(text, expected)
  })

  it('removes parallel quote with linebreak', () => {
    const text =
      'Hello there my<blockquote>\n some text there</blockquote>\ndear <blockquote>\n some text there</blockquote>friend!'
    const expected = 'Hello there my\ndear friend!'
    test(text, expected)
  })

  it('removes recursive quotes', () => {
    const text = 'Hello there my dear <blockquote> and <blockquote>some text </blockquote>there</blockquote>friend!'
    const expected = 'Hello there my dear friend!'
    test(text, expected)
  })

  it('removes inline', () => {
    const text =
      '<div>Hello there my </div><div> </div><inline-image>127891582630795@161d47f95e63.qloud-c.yandex.net</inline-image>dear friend!'
    const expected = 'Hello there my dear friend!'
    test(text, expected)
  })

  it('removes multiple inline-image', () => {
    const text = 'Hello there <inline-image> and </inline-image>my dear <inline-image>there</inline-image>friend!'
    const expected = 'Hello there my dear friend!'
    test(text, expected)
  })

  it('removes quotes with inline-image', () => {
    const text =
      'Hello<inline-image>remove this</inline-image> there <blockquote> and <inline-image>mm </inline-image><blockquote></blockquote>there</blockquote>my dear friend!'
    const expected = 'Hello there my dear friend!'
    test(text, expected)
  })

  it('stacks text blocks', () => {
    const text1 = 'Hello there '
    const text2 = 'my dear friend!'
    const expected = 'Hello there my dear friend!'
    testMany([text1, text2], expected)
  })
})

function test(html: string, expected: string): void {
  testMany([html], expected)
}

function testMany(htmls: string[], expected: string): void {
  const payload = new MessageBodyPayload(
    new MessageBodyInfo(defaultMid, [], [], 'rfcId', 'ref'),
    htmls.map((html) => createMessageBodyPart(html)),
    'contentType',
    'lang',
    false,
    [],
  )
  assert.equal(MessageBody.build(payload).content, expected)
}

function createMessageBodyPart(text: string): MessageBodyPart {
  return new MessageBodyPart('hid1', text, 'text', 'en')
}
