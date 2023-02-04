# -*- coding: utf-8 -*-

import unittest
import sys
import os
import cStringIO

path = os.path.abspath(__file__)
current = '/'.join(path.split('/')[:-2])
sys.path = [current] + sys.path

import indexer_proxy_lib.rtyserver_pb2 as rtyserver
import indexer_proxy_lib.utils as utils
import indexer_proxy_lib.errors as errors
import indexer_proxy_lib.message as message


class FakeSocketWithClosedConnection(object):
    def __init__(self, *args, **kwargs):
        pass

    def sendall(self, *args, **kwargs):
        pass

    def recv(self, *args, **kwargs):
        return ''

    def close(self):
        pass


class Test(unittest.TestCase):
    def setUp(self):
        pass

    def tearDown(self):
        pass

    def test_check_param_required(self):
        self.assertRaises(
            errors.HttpIndexerProxyError,
            utils.check_param,
            'some_param', None, int, required=True)

    def test_check_param_not_required(self):
        self.assertEqual(
            utils.check_param('some_param', None, int),
            None)

    def test_check_param_bad_type(self):
        self.assertRaises(
            errors.HttpIndexerProxyError,
            utils.check_param,
            'some_param', 'some_str', int)

    def test_check_param_good_type(self):
        self.assertEqual(
            utils.check_param('some_param', '1666', int, required=True),
            1666)

    def test_check_spesial_params_good_special_params(self):
        self.assertEqual(
            utils.check_special_params('send_type', 'modify', ['modify', 'delete'], required=True),
            'modify')

    def test_check_spesial_params_bad_special_params(self):
        self.assertRaises(
            errors.HttpIndexerProxyError,
            utils.check_special_params, 'send_type', 'add', ['modify', 'delete'])

    def test_check_spesial_params_required(self):
        self.assertRaises(
            errors.HttpIndexerProxyError,
            utils.check_special_params, 'send_type', None, ['modify', 'delete'], required=True)

    def test_check_spesial_params_not_required(self):
        self.assertEqual(
            utils.check_special_params('send_type', None, ['modify', 'delete']),
            None)

    def test_create_attributes_without_type(self):
        attrs = [{'name': 'some_attr3', 'value': 22222}]
        self.assertEqual(
            utils.create_attributes('search attrs', attrs, with_type=False),
            [message.Attribute(name='some_attr3', value='22222', type=None)])

    def test_create_attributes_bad_attrs(self):
        attrs = [{'name': 'some_attr2'}]
        self.assertRaises(
            errors.HttpIndexerProxyError,
            utils.create_attributes, 'search attrs', attrs)

    def test_create_attributes_bad_attrs2(self):
        attrs = [{'name': 'some_attr2', 'value': '\x0101011'}]

        self.assertRaises(
            errors.HttpIndexerProxyError,
            utils.create_attributes, 'search attrs', attrs)

    def test_create_attributes_bad_attrs3(self):
        attrs = [{'name': 'some_attr2', 'type': 'literal'}]
        self.assertRaises(
            errors.HttpIndexerProxyError,
            utils.create_attributes, 'search attrs', attrs)

    def test_create_attributes_bad_attrs4(self):
        attrs = [{'name': 'some_attr2', 'value': '\x0101011', 'type': 'fake type'}]
        self.assertRaises(
            errors.HttpIndexerProxyError,
            utils.create_attributes, 'search attrs', attrs)

    def test_create_attributes_good_attrs(self):
        attrs = [{'name': 'some_attr1', 'value': 'some_value', 'type': 'literal'},
            {'name': 'some_attr2', 'value': 2222, 'type': 'integer'}]

        self.assertEqual(
            sorted(utils.create_attributes('search attrs', attrs), key=lambda x: (x.name, x.value, x.type), reverse=True),
            sorted([
                message.Attribute(name='some_attr2', value='2222', type=utils.ATTRIBUTE_TYPES.get('integer')),
                message.Attribute(name='some_attr1', value='some_value', type=utils.ATTRIBUTE_TYPES.get('literal')),
            ], key=lambda x: (x.name, x.value, x.type), reverse=True),
        )

    def test_create_attributes_zero_value(self):
        attrs = [{'name': 'some_attr1', 'value': 0, 'type': 'integer'}]

        self.assertEqual(
            utils.create_attributes('search attrs', attrs),
            [
                message.Attribute(name='some_attr1', value='0', type=utils.ATTRIBUTE_TYPES.get('integer')),
            ],
        )

    def test_create_attributes_empty_value(self):
        attrs = [{'name': 'some_attr1', 'value': '', 'type': 'literal'}]
        self.assertRaises(
            errors.HttpIndexerProxyError,
            utils.create_attributes, 'search attrs', attrs)

    def test_create_attributes_good_attrs_with_empty_type(self):
        attrs = [{'name': 'some_attr1', 'value': 0, 'type': 'jjjj'}]
        self.assertEqual(
            utils.create_attributes('document properties', attrs, with_type=False),
            [
                message.Attribute(name='some_attr1', value='0', type=None),
            ],
        )

    def test_parse_data(self):
        send_type = u'add'
        url = u'fake_url'
        keyprefix = 123456
        mime_type = u'text/plain'
        charset = u'utf-8'
        language = u'en'
        language2 = u'ru'
        modification_timestamp = 1317814869.8177731
        search_attributes = [{u'name': u'attr1', u'value': 'value1', 'type': 'literal'},
            {u'name': u'attr2', u'value': 123456, 'type': 'integer'}]
        grouping_attributes = [{u'name': u'attr3', u'value': 'value3', 'type': 'literal'},
            {u'name': u'attr4', u'value': 123456, 'type': 'integer'}]
        document_attributes = [{u'name': u'attr5', u'value': 'value5'},
            {u'name': u'attr6', u'value': 123456}]
        factors = [{u'name': u'attr7', u'value': 'value7', 'type': 'literal'},
            {u'name': u'attr8', u'value': 1234569, 'type': 'literal'}]

        valid_data = {
            u'send_type': send_type,
            u'url': url,
            u'keyprefix': int(keyprefix),
            u'mime_type': mime_type,
            u'charset': charset,
            u'language': language,
            u'language2': language2,
            u'modification_timestamp': modification_timestamp,
            u'search_attributes': search_attributes,
            u'grouping_attributes': grouping_attributes,
            u'document_attributes': document_attributes,
            u'factors': factors,
        }

        valid_document = u'1some text document hhh'

        m = message.Message(
            send_type=send_type,
            url=url,
            document=valid_document,
            keyprefix=keyprefix,
            mime_type=mime_type,
            charset=charset,
            language=language,
            language2=language2,
            modification_timestamp=int(modification_timestamp),
            search_attributes=utils.create_attributes('search_attributes', search_attributes),
            grouping_attributes=utils.create_attributes('grouping_attributes', grouping_attributes),
            document_attributes=utils.create_attributes('document_attributes', document_attributes, with_type=False),
            factors=utils.create_attributes('factors', factors),
        )

        self.assertEqual(
            utils.parse_data(valid_data, valid_document).get_dicpatcher_proto_message('fake_service', with_response=True),
            m.get_dicpatcher_proto_message('fake_service', with_response=True))

    def test_check_not_now_response(self):
        response = rtyserver.TReply()
        response.MessageId = 1
        response.Status = rtyserver.TReply.NOTNOW

        self.assertRaises(
            errors.IndexerError,
            utils.check_response, response)

    def test_check_incorrect_document_response(self):
        response = rtyserver.TReply()
        response.MessageId = 1
        response.Status = rtyserver.TReply.INCORRECT_DOCUMENT

        self.assertRaises(
            errors.IncorrectDocumentError,
            utils.check_response, response)

    def test_check_strange_response(self):
        response = rtyserver.TReply()
        response.MessageId = 1
        response.Status = 3222

        self.assertRaises(
            errors.IndexerError,
            utils.check_response, response)

    def test_check_send_failed_response(self):
        response = rtyserver.TReply()
        response.MessageId = 1
        response.Status = rtyserver.TReply.SEND_FAILED

        self.assertRaises(
            errors.IndexerError,
            utils.check_response, response)

    def test_check_read_failed_response(self):
        response = rtyserver.TReply()
        response.MessageId = 1
        response.Status = rtyserver.TReply.READ_FAILED

        self.assertRaises(
            errors.IndexerError,
            utils.check_response, response)

    def test_check_store_failed_response(self):
        response = rtyserver.TReply()
        response.MessageId = 1
        response.Status = rtyserver.TReply.STORE_FAILED

        self.assertRaises(
            errors.IndexerError,
            utils.check_response, response)

    def test_check_bad_message_id_response(self):
        response = rtyserver.TReply()
        response.MessageId = 22324
        response.Status = rtyserver.TReply.OK

        self.assertRaises(
            errors.IndexerError,
            utils.check_response, response)

    def test_check_internal_error(self):
        response = rtyserver.TReply()
        response.MessageId = 1
        response.Status = rtyserver.TReply.INTERNAL_ERROR

        self.assertRaises(
            errors.InternalError,
            utils.check_response, response)

    def test_check_not_response(self):
        self.assertRaises(
            errors.IndexerError,
            utils.check_response, None)

    def test_check_empty_response(self):
        self.assertRaises(
            errors.IndexerError,
            utils.check_response, '')

    def test_parse_searchmap_with_empty_str(self):
        self.assertEqual(
            utils.parse_searchmap(cStringIO.StringIO('')),
            [])

    def test_parse_searchmap(self):
        self.assertEqual(
            utils.parse_searchmap(cStringIO.StringIO('   service some_service\n# test some_test\nblahblah')),
            ['service'])

    def test_closed_connection(self):
        self.assertRaises(
            errors.ClosedConnection,
            utils.send_message, FakeSocketWithClosedConnection(), 'test message')


if __name__ == '__main__':
    unittest.main()
