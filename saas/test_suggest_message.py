# -*- coding: utf-8 -*-

import unittest
import sys
import os
import hashlib
import json

path = os.path.abspath(__file__)
current = '/'.join(path.split('/')[:-2])
sys.path = [current] + sys.path

import suggest_message as smessage


class TestSuggestMessage(unittest.TestCase):
    def setUp(self):
        pass

    def tearDown(self):
        pass

    def test_suggest_message(self):
        text = u'RuRufH'
        keyprefix = 123
        query = {'hhhhh': 'jjj', 'utf8': '1'},
        send_type = 'modify',
        timestamp = 1317931200

        sm = smessage.SuggestMessage(
            keyprefix=keyprefix,
            text=text,
            query=query,
            send_type=send_type,
            timestamp=timestamp)

        self.assertEqual(sm.keyprefix, keyprefix)
        self.assertEqual(sm.text, text)
        self.assertEqual(sm.query, query)
        self.assertEqual(sm.timestamp, timestamp)
        self.assertEqual(sm.ltext, text.lower())
        self.assertEqual(sm.url, hashlib.md5('%s %s' % (text.lower(), keyprefix)).hexdigest())
        self.assertEqual(sm.body, 'someqtrnvhkly ' + text.lower())

    def test_create_proto_message(self):
        sm = smessage.SuggestMessage(
            keyprefix=123456,
            text=u'Blah!!!',
            query={'utf8': '1', 'length': '200'},
            send_type='modify',
            timestamp=1234567)

        self.assertEqual(sm.create_proto_message(), '\xbb\x01\x08\x01\x12\xb4\x01\n 92c3dd03bddbf1d706d67f2bdcaaf11b\x12\ntext/plain\x1a\x05utf-82\x15someqtrnvhkly blah!!!:\x16\n\torig_text\x12\x07blah!!!\x18\x00B\x1a\n\rreq_timestamp\x12\x071234567\x18\x01J\t\n\x04utf8\x12\x011J\r\n\x06length\x12\x03200J\x14\n\torig_text\x12\x07Blah!!!P\xc0\xc4\x07(\x01')

    def test_http_request_body_headers(self):
        def encode_field(data, field_name):
            return ('--' + boundary,
                    'Content-Disposition: form-data; name="%s"' % field_name,
                    '', data[field_name])

        keyprefix = 123456
        text = u'Blah!!!'
        query = {'utf8': '1', 'length': '200'}
        send_type = 'modify'
        timestamp = 1234567

        sm = smessage.SuggestMessage(
            keyprefix=keyprefix,
            text=text,
            query=query,
            send_type=send_type,
            timestamp=timestamp)

        body, headers = sm.create_http_request_body_headers()
        boundary = headers.get('content-type').split('boundary=')[-1]

        json_message = json.dumps({
            'send_type': send_type,
            'url': hashlib.md5('%s %s' % (text.lower(), keyprefix)).hexdigest(),
            'mime_type': 'text/plain',
            'charset': 'utf8',
            'document_attributes': [{'name': '%s' % k, 'value': '%s' % v} \
                for k, v in query.items()] + [{'name': 'orig_text', 'value': text}],
            'grouping_attributes': [{'name': 'req_timestamp',
                'value': timestamp, 'type': 'integer'}],
            'search_attributes': [{'name': 'orig_text', 'value': text.lower(),
                'type': 'literal'}],
            'keyprefix': keyprefix,
            })
        data = {'document': 'someqtrnvhkly ' + text.lower(),
            'json_message': json_message}
        lines = []
        for name in data:
            lines.extend(encode_field(data, name))
        lines.extend(('--%s--' % boundary, ''))
        result = '\r\n'.join(lines)

        self.assertEqual(body, result)

if __name__ == '__main__':
    unittest.main()
