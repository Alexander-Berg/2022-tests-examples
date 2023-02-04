# -*- coding: utf-8 -*-
import json
import responses

from django.test import TestCase

from events.common_app.sender.client import SenderClient, Address, Attachment


class TestSenderClient(TestCase):
    @responses.activate
    def test_send_email(self):
        responses.add(
            responses.POST,
            'https://test.sender.yandex-team.ru/api/0/forms/transactional/campaign/send',
            json={
                'result': {'status': 'OK', 'message_id': '123', 'task_id': 'abcd'},
            },
        )
        subject = 'Test it'
        body = 'This is test'
        sender = SenderClient()
        sender.add_address(Address('user1@yandex.ru'))
        response = sender.send_email('campaign', {'subject': subject, 'body': body})

        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(responses.calls), 1)

        req = responses.calls[0].request
        self.assertEqual(req.headers['User-Agent'], SenderClient.default_user_agent)

        data = json.loads(req.body.decode())
        self.assertTrue(data['async'])
        self.assertEqual(data['args']['subject'], subject)
        self.assertEqual(data['args']['body'], body)
        self.assertListEqual(data['to'], [{'email': 'user1@yandex.ru'}])
        self.assertTrue(data['ignore_empty_email'])
        self.assertTrue(data['has_ugc'])

        from_address = Address(SenderClient.default_email, SenderClient.default_name)
        self.assertEqual(data['from_email'], from_address.email)
        self.assertEqual(data['from_name'], from_address.name)

    @responses.activate
    def test_send_email_with_from(self):
        responses.add(
            responses.POST,
            'https://test.sender.yandex-team.ru/api/0/forms/transactional/campaign/send',
            json={
                'result': {'status': 'OK', 'message_id': '123', 'task_id': 'abcd'},
            },
        )
        subject = 'Test it'
        body = 'This is test'
        from_address = Address('admin@yandex.ru', 'Admin')
        sender = SenderClient()
        sender.add_address(Address('user1@yandex.ru'))
        sender.set_from_address(from_address)
        response = sender.send_email('campaign', {'subject': subject, 'body': body})

        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(responses.calls), 1)

        req = responses.calls[0].request
        data = json.loads(req.body.decode())
        self.assertEqual(data['from_email'], from_address.email)
        self.assertEqual(data['from_name'], from_address.name)

    @responses.activate
    def test_send_email_with_to(self):
        responses.add(
            responses.POST,
            'https://test.sender.yandex-team.ru/api/0/forms/transactional/campaign/send',
            json={
                'result': {'status': 'OK', 'message_id': '123', 'task_id': 'abcd'},
            },
        )
        subject = 'Test it'
        body = 'This is test'
        sender = SenderClient()
        addr1 = Address('user1@yandex.ru')
        addr2 = Address('user2@yandex.ru', 'User2')
        sender.add_address(addr1)
        sender.add_address(addr2)
        response = sender.send_email('campaign', {'subject': subject, 'body': body})

        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(responses.calls), 1)

        req = responses.calls[0].request
        data = json.loads(req.body.decode())
        self.assertListEqual(data['to'], [
            {'email': addr1.email},
            {'email': addr2.email, 'name': addr2.name},
        ])

    @responses.activate
    def test_send_email_with_headers(self):
        responses.add(
            responses.POST,
            'https://test.sender.yandex-team.ru/api/0/forms/transactional/campaign/send',
            json={
                'result': {'status': 'OK', 'message_id': '123', 'task_id': 'abcd'},
            },
        )
        subject = 'Test it'
        body = 'This is test'
        sender = SenderClient()
        sender.add_address(Address('user1@yandex.ru'))
        sender.add_header('X-Test', '1234')
        sender.add_header('X-Service-ID', 'abcd')
        response = sender.send_email('campaign', {'subject': subject, 'body': body})

        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(responses.calls), 1)

        req = responses.calls[0].request
        self.assertEqual(req.headers['X-Test'], '1234')
        self.assertEqual(req.headers['X-Service-ID'], 'abcd')

    @responses.activate
    def test_send_email_with_attachments(self):
        responses.add(
            responses.POST,
            'https://test.sender.yandex-team.ru/api/0/forms/transactional/campaign/send',
            json={
                'result': {'status': 'OK', 'message_id': '123', 'task_id': 'abcd'},
            },
        )
        subject = 'Test it'
        body = 'This is test'
        attach1 = Attachment('readme.txt', b'tech doc')
        attach2 = Attachment('face.jpg', b'your avatar')
        attach3 = Attachment('xmlfile', b'<xml></xml>', mime_type='text/xml')
        sender = SenderClient()
        sender.add_address(Address('user1@yandex.ru'))
        sender.add_attachment(attach1)
        sender.add_attachment(attach2)
        sender.add_attachment(attach3)
        response = sender.send_email('campaign', {'subject': subject, 'body': body})

        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(responses.calls), 1)

        req = responses.calls[0].request
        data = json.loads(req.body.decode())
        attachments = data['attachments']
        self.assertEqual(len(attachments), 3)
        self.assertEqual(attachments[0]['filename'], 'readme.txt')
        self.assertEqual(attachments[0]['data'], attach1.binary_content)
        self.assertEqual(attachments[0]['mime_type'], 'text/plain;charset=utf-8')
        self.assertEqual(attachments[1]['filename'], 'face.jpg')
        self.assertEqual(attachments[1]['data'], attach2.binary_content)
        self.assertEqual(attachments[1]['mime_type'], 'image/jpeg')
        self.assertEqual(attachments[2]['filename'], 'xmlfile')
        self.assertEqual(attachments[2]['data'], attach3.binary_content)
        self.assertEqual(attachments[2]['mime_type'], 'text/xml')

    @responses.activate
    def test_send_email_with_replyto(self):
        responses.add(
            responses.POST,
            'https://test.sender.yandex-team.ru/api/0/forms/transactional/campaign/send',
            json={
                'result': {'status': 'OK', 'message_id': '123', 'task_id': 'abcd'},
            },
        )
        subject = 'Test it'
        body = 'This is test'
        reply_to = Address('user2@yandex.ru')
        sender = SenderClient()
        sender.add_address(Address('user1@yandex.ru'))
        sender.set_reply_to(reply_to)
        response = sender.send_email('campaign', {'subject': subject, 'body': body})

        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(responses.calls), 1)

        req = responses.calls[0].request
        self.assertEqual(req.headers['Reply-To'], reply_to.email)

    @responses.activate
    def test_send_email_with_replyto_name(self):
        responses.add(
            responses.POST,
            'https://test.sender.yandex-team.ru/api/0/forms/transactional/campaign/send',
            json={
                'result': {'status': 'OK', 'message_id': '123', 'task_id': 'abcd'},
            },
        )
        subject = 'Test it'
        body = 'This is test'
        reply_to = Address('user2@yandex.ru', 'User2')
        sender = SenderClient()
        sender.add_address(Address('user1@yandex.ru'))
        sender.set_reply_to(reply_to)
        response = sender.send_email('campaign', {'subject': subject, 'body': body})

        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(responses.calls), 1)

        req = responses.calls[0].request
        self.assertEqual(req.headers['Reply-To'], f'{reply_to.name} <{reply_to.email}>')


class TestAddress(TestCase):
    def test_address_without_name(self):
        email = 'user@yandex.ru'
        address = Address(email)
        self.assertEqual(address.email, email)
        self.assertEqual(address.name, '')
        self.assertDictEqual(address.getvalue(), {'email': email})

    def test_address_with_punnycode(self):
        email = 'продажи@диваны.рф'
        punnycode_email = 'продажи@xn--80adfq1a8f.xn--p1ai'
        address = Address(email)
        self.assertEqual(address.email, punnycode_email)
        self.assertEqual(address.name, '')
        self.assertDictEqual(address.getvalue(), {'email': punnycode_email})

    def test_address_with_name(self):
        email = 'user@yandex.ru'
        name = 'User1'
        encoded_name = '=?utf-8?B?VXNlcjE=?='
        address = Address(email, name)
        self.assertEqual(address.email, email)
        self.assertEqual(address.name, encoded_name)
        self.assertDictEqual(address.getvalue(), {'email': email, 'name': encoded_name})


class TestAttachment(TestCase):
    def test_attach_without_name_and_type(self):
        content = b'some content'
        attach = Attachment('newfile', content)
        self.assertEqual(attach.attach_name, 'newfile')
        self.assertEqual(attach.binary_content, 'c29tZSBjb250ZW50')
        self.assertEqual(attach.mime_type, 'application/octet-stream')
        self.assertDictEqual(attach.getvalue(), {
            'filename': attach.attach_name,
            'data': attach.binary_content,
            'mime_type': attach.mime_type,
        })

    def test_attach_text_file(self):
        content = b'some content'
        attach = Attachment('readme.txt', content)
        self.assertEqual(attach.attach_name, 'readme.txt')
        self.assertEqual(attach.binary_content, 'c29tZSBjb250ZW50')
        self.assertEqual(attach.mime_type, 'text/plain;charset=utf-8')
        self.assertDictEqual(attach.getvalue(), {
            'filename': attach.attach_name,
            'data': attach.binary_content,
            'mime_type': attach.mime_type,
        })

    def test_attach_jpeg_file(self):
        content = b'some content'
        attach = Attachment('face.jpg', content)
        self.assertEqual(attach.attach_name, 'face.jpg')
        self.assertEqual(attach.binary_content, 'c29tZSBjb250ZW50')
        self.assertEqual(attach.mime_type, 'image/jpeg')
        self.assertDictEqual(attach.getvalue(), {
            'filename': attach.attach_name,
            'data': attach.binary_content,
            'mime_type': attach.mime_type,
        })

    def test_attach_with_type(self):
        content = b'some content'
        attach = Attachment('newfile', content, 'text/xml')
        self.assertEqual(attach.attach_name, 'newfile')
        self.assertEqual(attach.binary_content, 'c29tZSBjb250ZW50')
        self.assertEqual(attach.mime_type, 'text/xml')
        self.assertDictEqual(attach.getvalue(), {
            'filename': attach.attach_name,
            'data': attach.binary_content,
            'mime_type': attach.mime_type,
        })
