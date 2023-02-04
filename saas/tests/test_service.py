# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import json
import mock
import unittest
import yatest

from faker import Faker

from saas.library.python.nanny_rest import NannyServiceBase
from saas.library.python.nanny_rest.enums import AllocationType, ServiceSummaryStatus, SnapshotStatus
from saas.library.python.nanny_rest.errors import NannyApiError
from saas.library.python.nanny_rest.resource import SandboxFile, StaticFile, UrlFile, TemplateSetFile
from saas.library.python.nanny_rest.tests.fake import Provider as NannyRestFakeProvider

fake = Faker()
fake.add_provider(NannyRestFakeProvider)


class TestNannyService(unittest.TestCase):

    def setUp(self):
        with mock.patch('saas.library.python.nanny_rest.service.PersistentTokenStore', autospec=True):
            self.nanny_service = NannyServiceBase('test_service')

    def test_runtime_attrs_getters(self):
        fake_attrs = fake.runtime_attributes()
        self.nanny_service._NANNY.get_runtime_attrs = mock.Mock(return_value=fake_attrs)
        self.assertDictEqual(self.nanny_service.runtime_attrs, fake_attrs['content'])
        self.assertDictEqual(self.nanny_service.get_runtime_attrs(), fake_attrs['content'])

    def test_runtime_attrs_setter(self):
        old_fake_attrs = fake.runtime_attributes()
        test_attrs = fake.runtime_attributes(parent=old_fake_attrs)
        self.nanny_service._runtime_attrs = old_fake_attrs

        test_comment = fake.sentence()
        self.nanny_service._NANNY.put_runtime_attrs = mock.Mock(return_value=test_attrs)
        self.nanny_service.set_runtime_attrs(test_attrs['content'], test_comment)
        self.nanny_service._NANNY.put_runtime_attrs.assert_called_once_with(self.nanny_service.name, {
            'comment': test_comment, 'snapshot_id': old_fake_attrs['_id'], 'content': test_attrs['content'],
        })

    def test_info_attrs_getters(self):
        fake_attrs = fake.info_attributes()
        self.nanny_service._NANNY.get_info_attrs = mock.Mock(return_value=fake_attrs)
        self.assertDictEqual(self.nanny_service.info_attrs, fake_attrs['content'])
        self.assertDictEqual(self.nanny_service.get_info_attrs(), fake_attrs['content'])

    def test_info_attrs_setter(self):
        old_fake_attrs = fake.info_attributes()
        test_attrs = fake.info_attributes(parent=old_fake_attrs)
        self.nanny_service._info_attrs = old_fake_attrs

        test_comment = fake.sentence()
        self.nanny_service._NANNY.put_info_attrs = mock.Mock(return_value=test_attrs)
        self.nanny_service.set_info_attrs(test_attrs['content'], test_comment)
        self.nanny_service._NANNY.put_info_attrs.assert_called_once_with(self.nanny_service.name, {
            'comment': test_comment, 'snapshot_id': old_fake_attrs['_id'], 'content': test_attrs['content'],
        })

    def test_auth_attrs_getters(self):
        fake_attrs = fake.auth_attributes()
        self.nanny_service._NANNY.get_auth_attrs = mock.Mock(return_value=fake_attrs)
        self.assertDictEqual(self.nanny_service.auth_attrs, fake_attrs['content'])
        self.assertDictEqual(self.nanny_service.get_auth_attrs(), fake_attrs['content'])

    def test_auth_attrs_setter(self):
        old_fake_attrs = fake.auth_attributes()
        test_attrs = fake.auth_attributes(parent=old_fake_attrs)
        self.nanny_service._auth_attrs = old_fake_attrs

        test_comment = fake.sentence()
        self.nanny_service._NANNY.put_auth_attrs = mock.Mock(return_value=test_attrs)
        self.nanny_service.set_auth_attrs(test_attrs['content'], test_comment)
        self.nanny_service._NANNY.put_auth_attrs.assert_called_once_with(self.nanny_service.name, {
            'comment': test_comment, 'snapshot_id': old_fake_attrs['_id'], 'content': test_attrs['content'],
        })

    @mock.patch('saas.library.python.nanny_rest.service.ServiceRepoClient', autospec=True)
    def test_equality(self, *args):
        service_name = fake.nanny_service_name()
        other_service_name = fake.nanny_service_name()
        with mock.patch('saas.library.python.nanny_rest.service.PersistentTokenStore', autospec=True):
            s1 = NannyServiceBase(service_name)
            s2 = NannyServiceBase(service_name)
            s3 = NannyServiceBase(other_service_name)
            self.assertEqual(s1, s2, 'NannyServiceBase with same name are not equal')
            self.assertEqual(s1.__hash__(), s2.__hash__())

            self.assertNotEqual(s1, s3, 'NannyServiceBase with different name are equal')
            self.assertNotEqual(s1.__hash__(), s3.__hash__())

            self.assertNotEqual(s2, s3, 'NannyServiceBase with different name are equal')
            self.assertNotEqual(s2.__hash__(), s3.__hash__())

            self.assertEqual(s3, s3, 'NannyServiceBase is not equal to itself')
            self.assertEqual(s3.__hash__(), s3.__hash__())

    def test_allocation_type(self):
        gencfg_fake_attrs = fake.runtime_attributes(instance_allocation='EXTENDED_GENCFG_GROUPS')
        gencfg_allocated_service = NannyServiceBase(fake.nanny_service_name(), runtime_attrs=gencfg_fake_attrs)
        self.assertIs(gencfg_allocated_service.allocation_type, AllocationType.gencfg)

        yp_fake_attrs = fake.runtime_attributes(instance_allocation='YP_POD_IDS')
        yp_allocated_service = NannyServiceBase(fake.nanny_service_name(), runtime_attrs=yp_fake_attrs)
        self.assertIs(yp_allocated_service.allocation_type, AllocationType.yp_lite)

        invalid_fake_attrs = fake.runtime_attributes(instance_allocation='INSTANCE_LIST')
        somehow_allocated_service = NannyServiceBase(fake.nanny_service_name(), runtime_attrs=invalid_fake_attrs)
        with self.assertRaises(NannyApiError):
            somehow_allocated_service.allocation_type

    @mock.patch.object(NannyServiceBase, '_NANNY', autospec=True)
    def test_resources(self, nanny_mock):
        nanny_mock.get_runtime_attrs = mock.MagicMock(return_value=fake.runtime_attributes())
        fake_service = NannyServiceBase(fake.nanny_service_name())
        for sandbox_file in fake_service.sandbox_files.values():
            self.assertIsInstance(sandbox_file, SandboxFile)
        for static_file in fake_service.static_files.values():
            self.assertIsInstance(static_file, StaticFile)
        for url_file in fake_service.url_files.values():
            self.assertIsInstance(url_file, UrlFile)
        for template_set_file in fake_service.template_set_files.values():
            self.assertIsInstance(template_set_file, TemplateSetFile)

        nanny_mock.get_runtime_attrs.assert_called_once_with(fake_service.name)

    @mock.patch.object(NannyServiceBase, '_NANNY', autospec=True)
    def test_cms_snapshots_info(self, nanny_mock):
        with open(
            yatest.common.source_path('saas/library/python/nanny_rest/tests/data/current_state.json'), 'rb'
        ) as f:
            nanny_mock.get_current_state = mock.MagicMock(return_value=json.load(f))

        fake_service = NannyServiceBase(fake.nanny_service_name())
        self.assertEqual(len(fake_service.cms_snapshots_info), 5)
        self.assertEqual(fake_service.cms_snapshots_info[0].snapshot_id, '30694cb3bec1b99100fc1c73df8b9e0b2afa31eb')
        self.assertEqual(fake_service.cms_snapshots_info[0].state, SnapshotStatus.ACTIVATING)
        self.assertIsNone(fake_service.active_snapshot_id)
        self.assertFalse(fake_service.is_online)
        nanny_mock.get_current_state.assert_called_with(fake_service.name)

    @mock.patch.object(NannyServiceBase, '_NANNY', autospec=True)
    def test_current_state_summary_status(self, nanny_mock):
        with open(
            yatest.common.source_path('saas/library/python/nanny_rest/tests/data/current_state.json'), 'rb'
        ) as f:
            nanny_mock.get_current_state = mock.MagicMock(return_value=json.load(f))

        fake_service = NannyServiceBase(fake.nanny_service_name())
        self.assertEqual(fake_service.current_state_summary_status, ServiceSummaryStatus.UPDATING)
        nanny_mock.get_current_state.assert_called_once_with(fake_service.name)


if __name__ == '__main__':
    unittest.main()
