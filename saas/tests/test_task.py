# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import mock
import unittest

from faker import Faker

from sandbox.common.rest import Client

from saas.library.python.sandbox import SandboxTask
from saas.library.python.sandbox import SandboxResource


fake = Faker()


class TestSandboxTask(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        pass

    def setUp(self):
        pass

    @staticmethod
    def get_resource_mock(**kwargs):
        resource_mock = mock.MagicMock(spec=SandboxResource)
        resource_mock.configure_mock(**kwargs)
        return resource_mock

    @mock.patch('saas.library.python.sandbox.SandboxTask.resources', new_callable=mock.PropertyMock)
    def test_get_single_resource(self, resources_mock):
        resources_mock.return_value = [self.get_resource_mock(attr_value=i) for i in range(0, 5)]
        task = SandboxTask(mock.MagicMock(spec=Client), fake.random.randint(0, 999999999))
        resource = task.get_single_resource(lambda r: r.attr_value == 3)
        self.assertEqual(resource, task.resources[3])
