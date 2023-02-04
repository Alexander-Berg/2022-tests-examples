import unittest
import yatest.common

from agency_rewards.rewards.config import Config


class TestConfig(unittest.TestCase):
    @unittest.mock.patch.object(Config, "_Config__init_clusters", spec=Config._Config__init_clusters)
    @unittest.mock.patch.object(Config, "_Config__init_queries", spec=Config._Config__init_queries)
    @unittest.mock.patch.object(Config, "_Config__init_config", spec=Config._Config__init_config)
    def setUp(
        self, config_init_config_method_mocked, config_init_queries_method_mocked, config_init_clusters_method_mocked
    ):
        config_path = 'billing/agency_rewards/deployment/configs/calculate.cfg.local.xml'
        config_path_real = yatest.common.source_path(config_path)

        # Копия конфига, чтобы не аффектить другие тесты
        ConfigCopy = type('ConfigCopy', Config.__bases__, dict(Config.__dict__))
        with unittest.mock.patch.dict(
            'os.environ', {'YANDEX_XML_CONFIG': config_path_real, 'LOGGING_TYPE': 'NONE'}, clear=True
        ):
            ConfigCopy.init()
        self.ConfigCopy = ConfigCopy

    def test_all_clusters(self):
        tests = [('hahn', 'hahn.yt.yandex.net'), ('freud', 'freud.yt.yandex.net')]
        for cluster, test in zip([Config.DEFAULT_CLUSTER, Config.Service('freud', 'freud.yt.yandex.net')], tests):
            self.assertEqual(cluster.name, test[0])
            self.assertEqual(cluster.proxy, test[1])

    def test_yql_tokens(self):
        self.assertEqual(len(self.ConfigCopy.YQL_TOKENS), 1)
        self.assertEqual(self.ConfigCopy.YQL_TOKENS['balance_ar'], 'XXX')

    def test_yt_tokens(self):
        self.assertEqual(len(self.ConfigCopy.YT_TOKENS), 1)
        self.assertEqual(self.ConfigCopy.YT_TOKENS['balance_ar'], 'XXX')

    def test_env_type(self):
        self.assertEqual(self.ConfigCopy._env_type, 'dev')

    def test_startrek_host(self):
        self.assertEqual(self.ConfigCopy.STARTREK_HOST, 'https://st-api.test.yandex-team.ru')
        self.assertEqual(self.ConfigCopy.STARTREK_URL, 'https://st.test.yandex-team.ru')
