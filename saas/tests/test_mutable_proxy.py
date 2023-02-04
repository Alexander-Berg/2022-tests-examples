# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import copy
import mock
import unittest

from faker import Faker

from saas.library.python.token_store import PersistentTokenStore

from saas.library.python.nanny_rest import NannyServiceBase
from saas.library.python.nanny_rest.tests.fake import Provider as NannyRestFakeProvider

fake = Faker()
fake.add_provider(NannyRestFakeProvider)


class TestMutableProxy(unittest.TestCase):
    FAKE_RESOURCE_METHOD = {
        'static_files': fake.static_file_resource,
        'sandbox_files': fake.sandbox_file_resource
    }

    @staticmethod
    def get_clean_runtime_attrs():
        runtime_attrs = fake.runtime_attributes()
        runtime_attrs['content']['resources'] = {
            'url_files': [],
            'sandbox_files': [],
            'static_files': [],
            'template_set_files': [],
            'l7_fast_balancer_config_files': [],
            'services_balancer_config_files': [],
        }
        return runtime_attrs

    @classmethod
    def add_resources(cls, runtime_attrs, kind='static_files', names=None):
        if not names:
            names = fake.words()
        resource_list = runtime_attrs['content']['resources'][kind]
        for name in names:
            resource = cls.FAKE_RESOURCE_METHOD[kind](name)
            resource_list.append(resource.dict())
        return runtime_attrs

    @classmethod
    def setUpClass(cls):
        PersistentTokenStore.add_tokens_from_env()

    def setUp(self):
        self.assertEqual(PersistentTokenStore.get_token('nanny'), 'fake_nanny_token')
        self.nanny_service = NannyServiceBase('test_service')

    @mock.patch.object(NannyServiceBase, '_NANNY', autospec=True)
    def test_set_static_resource_new(self, nanny_mock):
        test_runtime_attrs = self.get_clean_runtime_attrs()
        self.add_resources(test_runtime_attrs)
        nanny_mock.get_runtime_attrs = mock.MagicMock(return_value=test_runtime_attrs)
        nanny_mock.put_runtime_attrs = mock.MagicMock(return_value={'content': mock.sentinel.put_attrs})

        new_resource = fake.static_file_resource(fake.word())
        with self.nanny_service.runtime_attrs_transaction(fake.sentence()) as nanny_service:
            nanny_service.set_resource(new_resource)

        self.assertIs(self.nanny_service._runtime_attrs['content'], mock.sentinel.put_attrs)
        expected_content = copy.deepcopy(test_runtime_attrs)['content']
        expected_content['resources']['static_files'].append(new_resource.dict())

        nanny_mock.put_runtime_attrs.assert_called_once()
        call_args = nanny_mock.put_runtime_attrs.call_args
        self.assertEqual(call_args.args[0], self.nanny_service.name)
        self.assertDictEqual(call_args.args[1]['content'], expected_content)

    @mock.patch.object(NannyServiceBase, '_NANNY', autospec=True)
    def test_set_static_resource_existing(self, nanny_mock):
        test_runtime_attrs = self.get_clean_runtime_attrs()
        self.add_resources(test_runtime_attrs)
        nanny_mock.get_runtime_attrs = mock.MagicMock(return_value=test_runtime_attrs)
        nanny_mock.put_runtime_attrs = mock.MagicMock(return_value={'content': mock.sentinel.put_attrs})

        new_resource = fake.static_file_resource(
            test_runtime_attrs['content']['resources']['static_files'][-1]['local_path']
        )

        with self.nanny_service.runtime_attrs_transaction(fake.sentence()) as nanny_service:
            nanny_service.set_resource(new_resource)
        self.assertIs(self.nanny_service._runtime_attrs['content'], mock.sentinel.put_attrs)

        expected_content = copy.deepcopy(test_runtime_attrs)['content']
        expected_content['resources']['static_files'][-1] = new_resource.dict()

        nanny_mock.put_runtime_attrs.assert_called_once()
        call_args = nanny_mock.put_runtime_attrs.call_args
        self.assertEqual(call_args.args[0], self.nanny_service.name)
        self.assertDictEqual(call_args.args[1]['content'], expected_content)

    @mock.patch.object(NannyServiceBase, '_NANNY', autospec=True)
    def test_set_sandbox_resource_new(self, nanny_mock):
        test_runtime_attrs = self.get_clean_runtime_attrs()
        self.add_resources(test_runtime_attrs, 'sandbox_files')
        nanny_mock.get_runtime_attrs = mock.MagicMock(return_value=test_runtime_attrs)
        nanny_mock.put_runtime_attrs = mock.MagicMock(return_value={'content': mock.sentinel.put_attrs})

        new_resource = fake.sandbox_file_resource(fake.word())
        with self.nanny_service.runtime_attrs_transaction(fake.sentence()) as nanny_service:
            nanny_service.set_resource(new_resource)

        self.assertIs(self.nanny_service._runtime_attrs['content'], mock.sentinel.put_attrs)
        expected_content = copy.deepcopy(test_runtime_attrs)['content']
        expected_content['resources']['sandbox_files'].append(new_resource.dict())

        nanny_mock.put_runtime_attrs.assert_called_once()
        call_args = nanny_mock.put_runtime_attrs.call_args
        self.assertEqual(call_args.args[0], self.nanny_service.name)
        self.assertDictEqual(call_args.args[1]['content'], expected_content)

    @mock.patch.object(NannyServiceBase, '_NANNY', autospec=True)
    def test_set_sandbox_resource_existing(self, nanny_mock):
        test_runtime_attrs = self.get_clean_runtime_attrs()
        self.add_resources(test_runtime_attrs, 'sandbox_files')
        nanny_mock.get_runtime_attrs = mock.MagicMock(return_value=test_runtime_attrs)
        nanny_mock.put_runtime_attrs = mock.MagicMock(return_value={'content': mock.sentinel.put_attrs})

        new_resource = fake.sandbox_file_resource(
            test_runtime_attrs['content']['resources']['sandbox_files'][-1]['local_path']
        )

        with self.nanny_service.runtime_attrs_transaction(fake.sentence()) as nanny_service:
            nanny_service.set_resource(new_resource)
        self.assertIs(self.nanny_service._runtime_attrs['content'], mock.sentinel.put_attrs)

        expected_content = copy.deepcopy(test_runtime_attrs)['content']
        expected_content['resources']['sandbox_files'][-1] = new_resource.dict()

        nanny_mock.put_runtime_attrs.assert_called_once()
        call_args = nanny_mock.put_runtime_attrs.call_args
        self.assertEqual(call_args.args[0], self.nanny_service.name)
        self.assertDictEqual(call_args.args[1]['content'], expected_content)

    @mock.patch.object(NannyServiceBase, '_NANNY', autospec=True)
    def test_set_sandbox_resource_change_type(self, nanny_mock):
        test_runtime_attrs = self.get_clean_runtime_attrs()
        self.add_resources(test_runtime_attrs, 'static_files')
        self.add_resources(test_runtime_attrs, 'sandbox_files')
        nanny_mock.get_runtime_attrs = mock.MagicMock(return_value=test_runtime_attrs)
        nanny_mock.put_runtime_attrs = mock.MagicMock(return_value={'content': mock.sentinel.put_attrs})

        new_resource = fake.static_file_resource(
            test_runtime_attrs['content']['resources']['sandbox_files'][-1]['local_path']
        )

        with self.nanny_service.runtime_attrs_transaction(fake.sentence()) as nanny_service:
            nanny_service.set_resource(new_resource)
        self.assertIs(self.nanny_service._runtime_attrs['content'], mock.sentinel.put_attrs)

        expected_content = copy.deepcopy(test_runtime_attrs)['content']
        expected_content['resources']['static_files'].append(new_resource.dict())
        del expected_content['resources']['sandbox_files'][-1]

        nanny_mock.put_runtime_attrs.assert_called_once()
        call_args = nanny_mock.put_runtime_attrs.call_args
        self.assertEqual(call_args.args[0], self.nanny_service.name)
        self.assertDictEqual(call_args.args[1]['content'], expected_content)

    @mock.patch.object(NannyServiceBase, '_NANNY', autospec=True)
    def test_set_sandbox_resource_first_updated(self, nanny_mock):
        test_runtime_attrs = self.get_clean_runtime_attrs()
        self.add_resources(test_runtime_attrs, 'sandbox_files')
        nanny_mock.get_runtime_attrs = mock.MagicMock(return_value=test_runtime_attrs)
        nanny_mock.put_runtime_attrs = mock.MagicMock(return_value={'content': mock.sentinel.put_attrs})

        new_resource = fake.sandbox_file_resource(
            test_runtime_attrs['content']['resources']['sandbox_files'][0]['local_path']
        )

        with self.nanny_service.runtime_attrs_transaction(fake.sentence()) as nanny_service:
            nanny_service.set_resource(new_resource)
        self.assertIs(self.nanny_service._runtime_attrs['content'], mock.sentinel.put_attrs)

        expected_content = copy.deepcopy(test_runtime_attrs)['content']
        expected_content['resources']['sandbox_files'][0] = new_resource.dict()

        nanny_mock.put_runtime_attrs.assert_called_once()
        call_args = nanny_mock.put_runtime_attrs.call_args
        self.assertEqual(call_args.args[0], self.nanny_service.name)
        self.assertDictEqual(call_args.args[1]['content'], expected_content)
