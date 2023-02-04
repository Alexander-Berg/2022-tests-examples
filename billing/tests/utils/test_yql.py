import unittest
from unittest import mock
import xml.etree.ElementTree as et
from io import StringIO

from yql.api.v1.client import YqlClient


from agency_rewards.rewards.utils.yql_crutches import (
    get_token,
    get_proxy_url,
    get_proxy_url_clear,
    run_yql,
)
from . import config_sample


class TestXmlConfigGetter(unittest.TestCase):
    def setUp(self):
        self.cfg = et.parse(StringIO(config_sample))

    def test_get_token(self):
        self.assertEqual(get_token(self.cfg, 'yql'), 'YQL_TOKEN')
        self.assertEqual(get_token(self.cfg, 'yt'), 'YT_TOKEN')

    def test_get_proxy_url(self):
        self.assertEqual(get_proxy_url(self.cfg, 'yql'), 'hahn.yql.com')
        self.assertEqual(get_proxy_url(self.cfg, 'yt'), 'hahn.yt.com')

    def test_get_proxy_url_clear(self):
        self.assertEqual(get_proxy_url_clear(self.cfg), 'hahn')
        self.assertEqual(get_proxy_url_clear(self.cfg, 'yql'), 'hahn')
        self.assertEqual(get_proxy_url_clear(self.cfg, 'yt'), 'hahn')


class TestYQLRun(unittest.TestCase):
    @mock.patch('agency_rewards.rewards.utils.yql_crutches.Config')
    @mock.patch('yql.client.operation.YqlSqlOperationRequest.run', mock.MagicMock())
    def test_SQLlib_attachment(self, ConfigMock):
        """
        Тест на подключение sql бибилотеки из аркадии к запросу
        :param ConfigMock: замена метода run YqlSqlOperationRequest,
                чтобы не делать запросы в yt
        :return:
        """
        from agency_rewards.rewards.utils.bunker import ArcadiaLib
        from agency_rewards.rewards.config import Config

        ConfigMock.clusters = [Config.Service('FREUD', 'freud.yt.yandex.net')]

        query = """
                PRAGMA Library("yql_lib_test.sql");

                IMPORT yql_lib_test SYMBOLS $Test_function;
                select $Test_function(0.02) as reward;
            """

        libs = [
            ArcadiaLib(path='test/path_in/arcadia/1', revision='11', alias='lib1.sql'),
            ArcadiaLib(path='test/path_in/arcadia/2', revision='666', alias='lib2.sql'),
        ]
        yql_request = run_yql(query, YqlClient(), 'hahn', libs=libs)

        result = [
            {'name': 'lib1.sql', 'data': 'arc://test/path_in/arcadia/1?rev=11', 'type': 'URL'},
            {'name': 'lib2.sql', 'data': 'arc://test/path_in/arcadia/2?rev=666', 'type': 'URL'},
        ]
        self.assertListEqual(result, yql_request.attached_files)
