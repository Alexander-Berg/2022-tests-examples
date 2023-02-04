import xml.etree.ElementTree as et
import yatest.common


class MockApplication(object):

    LOCAL_CONFIG_PATH = 'billing/agency_rewards/deployment/configs/calculate.cfg.local.xml'

    def __init__(self, cfg_path=LOCAL_CONFIG_PATH):
        cfg_path = yatest.common.source_path(self.LOCAL_CONFIG_PATH)
        with open(cfg_path) as config:
            self.cfg = et.parse(config)

    def get_current_env_type(self):
        return 'dev'
