# -*- coding: utf-8 -*-

import unittest
import sys
import os
import cStringIO
import json

path = os.path.abspath(__file__)
current = '/'.join(path.split('/')[:-2])
sys.path = [current] + sys.path

os.environ['INDEXER_PROXY_SETTINGS'] = os.path.join(current, 'config.py.development')

import indexer_proxy

import indexer_proxy_lib.utils as utils
import indexer_proxy_lib.errors as errors


ERROR_TEMPLATE = """<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 3.2 Final//EN
<title>%(status_message)s</title>
<h1>%(status_message)s</h1>
<p>%(message)s</p>
"""


def send_message(*args, **kwargs):
    pass


def fake_services(*args, **kwargs):
    return {'eaa562ae975fed2c3a18096a8c7982d8': 'bar'}


class FakeSocket(object):
    def __init__(self, *args, **kwargs):
        pass

    def sendall(self, *args, **kwargs):
        pass

    def recv(self, *args, **kwargs):
        pass

    def close(self):
        pass


def get_sock(*args, **kwargs):
    return FakeSocket()


class IndexerProxyTestCase(unittest.TestCase):

    def setUp(self):
        self.app = indexer_proxy.app.test_client()
        self.app.application.get_services = fake_services

        utils.get_sock = get_sock
        utils.send_message = send_message

    def tearDown(self):
        del self.app
        reload(utils)

    def test_bad_content_type(self):
        response = self.app.post('/service/eaa562ae975fed2c3a18096a8c7982d8', data={},
            content_type='text/xml')

        self.assertEqual(response.status_code, 415)
        self.assertEqual(response.data, ERROR_TEMPLATE % dict(
            status_message="415 Unsupported Media Type",
            message='Content-Type must be "multipart/form-data;".'))

    def test_bad_service(self):
        response = self.app.post('/service/blah',
            data=dict(json_message='{"send_type": "modify", "language": "ru", "url": "jshd73uyasgsddahdj", "charset": "utf8", "mime_type": "text/xml"}'),
            content_type='multipart/form-data')

        self.assertEqual(response.status_code, 400)
        self.assertEqual(response.data, ERROR_TEMPLATE % dict(
            status_message="400 Bad request",
            message='Service name blah has invalid value.'))

    def test_empty_json_message(self):
        response = self.app.post('/service/eaa562ae975fed2c3a18096a8c7982d8', data='',
            content_type='multipart/form-data')

        self.assertEqual(response.status_code, 400)
        self.assertEqual(response.data, ERROR_TEMPLATE % dict(
            status_message="400 Bad request",
            message='Field json_message required, can not be empty.'))

    def test_bad_json_message(self):
        json_message = u'"send_type": "modify", "language": "ru", "url": "jshd73uyasgsddahdj", "charset": "utf8", "mime_type": "text/xml"}]'

        response = self.app.post('/service/eaa562ae975fed2c3a18096a8c7982d8',
            data=dict(json_message=json_message),
            content_type='multipart/form-data')

        self.assertEqual(response.status_code, 400)
        self.assertEqual(response.data, ERROR_TEMPLATE % dict(
            status_message="400 Bad request",
            message='Send bad field json_message: %r.' % json_message))

    def test_empty_document(self):
        response = self.app.post('/service/eaa562ae975fed2c3a18096a8c7982d8',
            data=dict(json_message='{"send_type": "modify", "language": "ru", "url": "jshd73uyasgsddahdj", "charset": "utf8", "mime_type": "text/xml"}'),
            content_type='multipart/form-data')

        self.assertEqual(response.status_code, 400)
        self.assertEqual(response.data, ERROR_TEMPLATE % dict(
            status_message="400 Bad request",
            message='Sould be one of file-field or data-filed with name="document".'))

    def test_document_in_form(self):
        response = self.app.post('/service/eaa562ae975fed2c3a18096a8c7982d8',
            data=dict(
            json_message='{"send_type": "modify", "language": "ru", "url": "jshd73uyasgsddahdj", "charset": "utf8", "mime_type": "text/xml"}',
            document='some document'),
            content_type='multipart/form-data')

        self.assertEqual(response.status_code, 200)

    def test_document_in_file(self):
        response = self.app.post('/service/eaa562ae975fed2c3a18096a8c7982d8',
            data=dict(
            json_message='{"send_type": "modify", "language": "ru", "url": "jshd73uyasgsddahdj", "charset": "utf8", "mime_type": "text/xml"}',
            document=cStringIO.StringIO('some document')),
            content_type='multipart/form-data')

        self.assertEqual(response.status_code, 200)

    def test_json_message_invalid_send_type(self):
        response = self.app.post('/service/eaa562ae975fed2c3a18096a8c7982d8',
            data=dict(
            json_message=json.dumps({
                'send_type': 'del',
            }),
            document=cStringIO.StringIO('some document')),
            content_type='multipart/form-data')

        self.assertEqual(response.status_code, 400)
        self.assertEqual(response.data, ERROR_TEMPLATE % dict(
            status_message="400 Bad Request",
            message='Argument send_type must be one of %s.' % utils.SEND_TYPES.keys()))

    def test_json_message_empty_url(self):
        response = self.app.post('/service/eaa562ae975fed2c3a18096a8c7982d8',
            data=dict(
                json_message=json.dumps({
                    'send_type': 'modify',
                    'url': '',
                }),
                document=cStringIO.StringIO('some document')),
            content_type='multipart/form-data')

        self.assertEqual(response.status_code, 400)
        self.assertEqual(response.data, ERROR_TEMPLATE % dict(
            status_message="400 Bad request",
            message='Argument url is required.'))

    def test_json_message_bad_keyprefix(self):
        response = self.app.post('/service/eaa562ae975fed2c3a18096a8c7982d8',
            data=dict(
                json_message=json.dumps({
                    'send_type': 'modify',
                    'url': 'url',
                    'keyprefix': 'jjjj',
                }),
                document=cStringIO.StringIO('some document')),
            content_type='multipart/form-data')

        self.assertEqual(response.status_code, 400)
        self.assertEqual(response.data, ERROR_TEMPLATE % dict(
            status_message="400 Bad request",
            message='Argument keyprefix must be an int, not unicode.'))

    def test_json_message_empty_mime_type(self):
        response = self.app.post('/service/eaa562ae975fed2c3a18096a8c7982d8',
            data=dict(
                json_message=json.dumps({
                    'send_type': 'modify',
                    'url': 'url',
                    'keyprefix': 11,
                    'mime_type': '',
                }),
                document=cStringIO.StringIO('some document')),
            content_type='multipart/form-data')

        self.assertEqual(response.status_code, 400)
        self.assertEqual(response.data, ERROR_TEMPLATE % dict(
            status_message="400 Bad Request",
            message="Argument mime_type is required."))

    def test_json_message_bad_mime_type(self):
        response = self.app.post('/service/eaa562ae975fed2c3a18096a8c7982d8',
            data=dict(
                json_message=json.dumps({
                    'send_type': 'modify',
                    'url': 'url',
                    'keyprefix': 11,
                    'mime_type': 'text/xmlfake',
                }),
                document=cStringIO.StringIO('some document')),
            content_type='multipart/form-data')

        self.assertEqual(response.status_code, 400)
        self.assertEqual(response.data, ERROR_TEMPLATE % dict(
            status_message="400 Bad Request",
            message='Argument mime_type must be one of %s.' % utils.MIME_TYPES))

    def test_json_message_empty_charset(self):
        response = self.app.post('/service/eaa562ae975fed2c3a18096a8c7982d8',
            data=dict(
                json_message=json.dumps({
                    'send_type': 'modify',
                    'url': 'url',
                    'keyprefix': 11,
                    'mime_type': 'text/xml',
                    'charset': '',
                }),
                document=cStringIO.StringIO('<?xml version="1.0" encoding="utf-8"?><widgets><widget id ="1"><name>Яндекс.Деньги</name></widget></widgets>')),
            content_type='multipart/form-data')

        self.assertEqual(response.status_code, 400)
        self.assertEqual(response.data, ERROR_TEMPLATE % dict(
            status_message="400 Bad request",
            message='Argument charset is required.'))

    def test_json_message_bad_charset(self):
        response = self.app.post('/service/eaa562ae975fed2c3a18096a8c7982d8',
            data=dict(
                json_message=json.dumps({
                    'send_type': 'modify',
                    'url': 'url',
                    'keyprefix': 11,
                    'mime_type': 'text/xml',
                    'charset': 0,
                }),
                document=cStringIO.StringIO('<?xml version="1.0" encoding="utf-8"?><widgets><widget id ="1"><name>Яндекс.Деньги</name></widget></widgets>')),
            content_type='multipart/form-data')

        self.assertEqual(response.status_code, 400)
        self.assertEqual(response.data, ERROR_TEMPLATE % dict(
            status_message="400 Bad request",
            message='Argument charset is required.'))

    def test_json_message_languages(self):
        response = self.app.post('/service/eaa562ae975fed2c3a18096a8c7982d8',
            data=dict(
                json_message=json.dumps({
                    'send_type': 'modify',
                    'url': 'url',
                    'keyprefix': 11,
                    'mime_type': 'text/xml',
                    'charset': 'utf8',
                    'language': 'en',
                    'language2': 'ru',
                }),
                document='<?xml version="1.0" encoding="utf-8"?><widgets><widget id ="1"><name>Яндекс.Деньги</name></widget></widgets>'),
            content_type='multipart/form-data')

        self.assertEqual(response.status_code, 200)

    def test_json_message_empty_languages(self):
        response = self.app.post('/service/eaa562ae975fed2c3a18096a8c7982d8',
            data=dict(
                json_message=json.dumps({
                    'send_type': 'modify',
                    'url': 'url',
                    'keyprefix': 11,
                    'mime_type': 'text/xml',
                    'charset': 'utf8',
                    'language': '',
                    'language2': '',
                }),
                document='<?xml version="1.0" encoding="utf-8"?><widgets><widget id ="1"><name>Яндекс.Деньги</name></widget></widgets>'),
            content_type='multipart/form-data')

        self.assertEqual(response.status_code, 200)

    def test_json_message_bad_modification_timestamp(self):
        response = self.app.post('/service/eaa562ae975fed2c3a18096a8c7982d8',
            data=dict(
                json_message=json.dumps({
                    'send_type': 'modify',
                    'url': 'url',
                    'keyprefix': 11,
                    'mime_type': 'text/xml',
                    'charset': 'utf8',
                    'language': '',
                    'language2': '',
                    'modification_timestamp': 'ssss',
                }),
                document='<?xml version="1.0" encoding="utf-8"?><widgets><widget id ="1"><name>Яндекс.Деньги</name></widget></widgets>'),
            content_type='multipart/form-data')

        self.assertEqual(response.status_code, 400)
        self.assertEqual(response.data, ERROR_TEMPLATE % dict(
            status_message="400 Bad request",
            message='Argument modification_timestamp must be an int, not unicode.'))

    def test_json_message_search_attributes(self):
        response = self.app.post('/service/eaa562ae975fed2c3a18096a8c7982d8',
            data=dict(
                json_message=json.dumps({
                    'send_type': 'modify',
                    'url': 'url',
                    'keyprefix': 11,
                    'mime_type': 'text/xml',
                    'charset': 'utf8',
                    'language': '',
                    'language2': '',
                    'modification_timestamp': 1317734484.2903559,
                    'search_attributes': [{'name': 'attr1', 'value': 'str_value', 'type': 'literal'}, {'name': 'attr2', 'value': 123456, 'type': 'integer'}],
                }),
                document='<?xml version="1.0" encoding="utf-8"?><widgets><widget id ="1"><name>Яндекс.Деньги</name></widget></widgets>'),
            content_type='multipart/form-data')

        self.assertEqual(response.status_code, 200)

    def test_json_message_bad_search_attributes(self):
        response = self.app.post('/service/eaa562ae975fed2c3a18096a8c7982d8',
            data=dict(
                json_message=json.dumps({
                    'send_type': 'modify',
                    'url': 'url',
                    'keyprefix': 11,
                    'mime_type': 'text/xml',
                    'charset': 'utf8',
                    'language': '',
                    'language2': '',
                    'modification_timestamp': 1317734484.2903559,
                    'search_attributes': [{'name': 'attr1'}, {'name': 'attr2', 'value': 123456}],
                }),
                document='<?xml version="1.0" encoding="utf-8"?><widgets><widget id ="1"><name>Яндекс.Деньги</name></widget></widgets>'),
            content_type='multipart/form-data')

        self.assertEqual(response.status_code, 400)
        self.assertEqual(response.data, ERROR_TEMPLATE % dict(
            status_message="400 Bad request",
            message='Argument value in search_attributes is required.'))

    def test_json_message_grouping_attributes(self):
        response = self.app.post('/service/eaa562ae975fed2c3a18096a8c7982d8',
            data=dict(
                json_message=json.dumps({
                    'send_type': 'modify',
                    'url': 'url',
                    'keyprefix': 11,
                    'mime_type': 'text/xml',
                    'charset': 'utf8',
                    'language': '',
                    'language2': '',
                    'modification_timestamp': 1317734484.2903559,
                    'grouping_attributes': [{'name': 'attr1', 'value': 'str_value', 'type': 'literal'}, {'name': 'attr2', 'value': 123456, 'type': 'literal'}],
                }),
                document='<?xml version="1.0" encoding="utf-8"?><widgets><widget id ="1"><name>Яндекс.Деньги</name></widget></widgets>'),
            content_type='multipart/form-data')

        self.assertEqual(response.status_code, 200)

    def test_json_message_bad_grouping_attributes(self):
        response = self.app.post('/service/eaa562ae975fed2c3a18096a8c7982d8',
            data=dict(
                json_message=json.dumps({
                    'send_type': 'modify',
                    'url': 'url',
                    'keyprefix': 11,
                    'mime_type': 'text/xml',
                    'charset': 'utf8',
                    'language': '',
                    'language2': '',
                    'modification_timestamp': 1317734484.2903559,
                    'grouping_attributes': [{'name': 'attr2', 'value': 'str_value'}],
                }),
                document='<?xml version="1.0" encoding="utf-8"?><widgets><widget id ="1"><name>Яндекс.Деньги</name></widget></widgets>'),
            content_type='multipart/form-data')

        self.assertEqual(response.status_code, 400)
        self.assertEqual(response.data, ERROR_TEMPLATE % dict(
            status_message="400 Bad Request",
            message="Argument type in grouping_attributes is required."))

    def test_json_message_document_attributes(self):
        response = self.app.post('/service/eaa562ae975fed2c3a18096a8c7982d8',
            data=dict(
                json_message=json.dumps({
                    'send_type': 'modify',
                    'url': 'url',
                    'keyprefix': 11,
                    'mime_type': 'text/xml',
                    'charset': 'utf8',
                    'language': '',
                    'language2': '',
                    'modification_timestamp': 1317734484.2903559,
                    'document_attributes': [{'name': 'attr1', 'value': 'str_value'}, {'name': 'attr2', 'value': 123456763476432645235452763}],
                }),
                document='<?xml version="1.0" encoding="utf-8"?><widgets><widget id ="1"><name>Яндекс.Деньги</name></widget></widgets>'),
            content_type='multipart/form-data')

        self.assertEqual(response.status_code, 200)

    def test_json_message_bad_document_attributes(self):
        response = self.app.post('/service/eaa562ae975fed2c3a18096a8c7982d8',
            data=dict(
                json_message=json.dumps({
                    'send_type': 'modify',
                    'url': 'url',
                    'keyprefix': 11,
                    'mime_type': 'text/xml',
                    'charset': 'utf8',
                    'language': '',
                    'language2': '',
                    'modification_timestamp': 1317734484.2903559,
                    'document_attributes': [{'name': 'attr1'}, {'name': 'attr2', 'value': 12345600734545454}],
                }),
                document='<?xml version="1.0" encoding="utf-8"?><widgets><widget id ="1"><name>Яндекс.Деньги</name></widget></widgets>'),
            content_type='multipart/form-data')

        self.assertEqual(response.status_code, 400)
        self.assertEqual(response.data, ERROR_TEMPLATE % dict(
            status_message="400 Bad request",
            message='Argument value in document_attributes is required.'))

    def test_incorrect_document(self):

        def fake_send_message(*args, **kwargs):
            raise errors.IncorrectDocumentError('failed indexing message,'
                ' because message is bad: not document, bad service or not keyprefix')

        utils.send_message = fake_send_message

        response = self.app.post('/service/eaa562ae975fed2c3a18096a8c7982d8',
            data=dict(
                json_message=json.dumps({
                    'send_type': 'modify',
                    'url': 'url',
                    'keyprefix': 11,
                    'mime_type': 'text/xml',
                    'charset': 'utf8',
                    'language': '',
                    'language2': '',
                    'modification_timestamp': 1317734484.2903559,
                }),
                document='<?xml version="1.0" encoding="utf-8"?><widgets><widget id ="1"><name>Яндекс.Деньги</name></widget></widgets>'),
            content_type='multipart/form-data')

        self.assertEqual(response.status_code, 400)
        self.assertEqual(response.data, ERROR_TEMPLATE % dict(
            status_message="400 Bad request",
            message='Bad sending message: message does not contain any document'
            ' or the document contains an invalid or does not keyprefix or bad service_name.'))

    def test_internal_error(self):

        def fake_send_message(*args, **kwargs):
            raise errors.InternalError('message has not been indexed,'
                ' so that there was an internal error on the rtyserver side')

        utils.send_message = fake_send_message

        response = self.app.post('/service/eaa562ae975fed2c3a18096a8c7982d8',
            data=dict(
                json_message=json.dumps({
                    'send_type': 'modify',
                    'url': 'url',
                    'keyprefix': 11,
                    'mime_type': 'text/xml',
                    'charset': 'utf8',
                    'language': '',
                    'language2': '',
                    'modification_timestamp': 1317734484.2903559,
                }),
                document='<?xml version="1.0" encoding="utf-8"?><widgets><widget id ="1"><name>Яндекс.Деньги</name></widget></widgets>'),
            content_type='multipart/form-data')

        self.assertEqual(response.status_code, 502)
        self.assertEqual(response.data, ERROR_TEMPLATE % dict(
            status_message="502 Bad Gateway",
            message='Message has not been indexed, so that there was an internal error on the rtyserver.'))

    def test_closed_connection(self):

        def fake_send_message(*args, **kwargs):
            raise errors.ClosedConnection('Empty response, connection is closed.')

        utils.send_message = fake_send_message

        response = self.app.post('/service/eaa562ae975fed2c3a18096a8c7982d8',
            data=dict(
                json_message=json.dumps({
                    'send_type': 'modify',
                    'url': 'url',
                    'keyprefix': 11,
                    'mime_type': 'text/xml',
                    'charset': 'utf8',
                    'language': '',
                    'language2': '',
                    'modification_timestamp': 1317734484.2903559,
                }),
                document='<?xml version="1.0" encoding="utf-8"?><widgets><widget id ="1"><name>Яндекс.Деньги</name></widget></widgets>'),
            content_type='multipart/form-data')

        self.assertEqual(response.status_code, 502)
        self.assertEqual(response.data, ERROR_TEMPLATE % dict(
            status_message="502 Bad Gateway",
            message='Failed send message to indexing, because we have error from rtyserver.'))

    def test_retry_count(self):

        def fake_send_message(*args, **kwargs):
            raise errors.IndexerError('failed indexing message')

        utils.send_message = fake_send_message

        response = self.app.post('/service/eaa562ae975fed2c3a18096a8c7982d8',
            data=dict(
                json_message=json.dumps({
                    'send_type': 'modify',
                    'url': 'url',
                    'keyprefix': 11,
                    'mime_type': 'text/xml',
                    'charset': 'utf8',
                    'language': '',
                    'language2': '',
                    'modification_timestamp': 1317734484.2903559,
                }),
                document='<?xml version="1.0" encoding="utf-8"?><widgets><widget id ="1"><name>Яндекс.Деньги</name></widget></widgets>'),
            content_type='multipart/form-data')

        self.assertEqual(response.status_code, 502)
        self.assertEqual(response.data, ERROR_TEMPLATE % dict(
            status_message="502 Bad Gateway",
            message='Failed send message to indexing, because we have error from rtyserver.'))

    def test_json_message_factors(self):
        response = self.app.post('/service/eaa562ae975fed2c3a18096a8c7982d8',
            data=dict(
                json_message=json.dumps({
                    'send_type': 'modify',
                    'url': 'url',
                    'keyprefix': 11,
                    'mime_type': 'text/xml',
                    'charset': 'utf8',
                    'language': '',
                    'language2': '',
                    'modification_timestamp': 1317734484.2903559,
                    'factors': [{'name': 'attr1', 'value': 'приветттттттттт', 'type': 'literal'}, {'name': 'attr2', 'value': 123456, 'type': 'integer'}],
                }),
                document='<?xml version="1.0" encoding="utf-8"?><widgets><widget id ="1"><name>Яндекс.Деньги</name></widget></widgets>'),
            content_type='multipart/form-data')

        self.assertEqual(response.status_code, 200)

    def test_json_message_bad_factors(self):
        response = self.app.post('/service/eaa562ae975fed2c3a18096a8c7982d8',
            data=dict(
                json_message=json.dumps({
                    'send_type': 'modify',
                    'url': 'url',
                    'keyprefix': 11,
                    'mime_type': 'text/xml',
                    'charset': 'utf8',
                    'language': '',
                    'language2': '',
                    'modification_timestamp': 1317734484.2903559,
                    'factors': [{'value': 'str_value'}, {'name': 'attr2', 'value': 123456}],
                }),
                document='<?xml version="1.0" encoding="utf-8"?><widgets><widget id ="1"><name>Яндекс.Деньги</name></widget></widgets>'),
            content_type='multipart/form-data')

        self.assertEqual(response.status_code, 400)
        self.assertEqual(response.data, ERROR_TEMPLATE % dict(
            status_message="400 Bad request",
            message='Argument name in factors is required.'))

if __name__ == '__main__':
    unittest.main()
