import logging
import requests
from urllib.parse import urlencode
import yaml

from .parametrization import RequestsProvider
from .tester import Config


class AmmoGenerator():
    def __init__(self, config):
        assert isinstance(config, Config)
        self._config = config
        self._logger = logging.getLogger()
        self.ammo_file = 'ammofile.txt'
        self.config_file = 'load.yaml'

    def create_ammo_file(self):
        f = open(self.ammo_file, 'w')
        for _ in range(int(self._config.duration_s * self._config.rps)):
            r = self._config.requests_provider.next_request(self._config.settings)
            f.write(f"{r['url'].partition(self._config.settings['backend'])[-1]}?{urlencode(r['params'])}\n")
        f.close()
        self._logger.info(f'Requests were saved to {self.ammo_file}.')

    def create_config_yaml(self):
        data = {
            'phantom': {
                'address': self._config.settings['backend'],
                'ammo_type': 'uri',
                'ammofile': self.ammo_file,
                'load_profile': {
                    'load_type': 'rps',
                    'schedule': f'const({self._config.rps}, {self._config.duration_s}s)'
                }
            }
        }
        with open(self.config_file, 'w') as f:
            yaml.dump(data, f, encoding='UTF-8', allow_unicode=True, sort_keys=False)
        self._logger.info(f'Lunapark config was generated to {self.config_file}.')
