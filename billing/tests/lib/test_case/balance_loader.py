from os import path

import yaml

from billing.hot.tests.lib.test_case.config import BalancesConfig, AccountConfig

from library.python import resource


class BalancesLoader:
    FIXTURES_FOLDER = "fixtures"
    BALANCES_FILE_PATH = path.join(FIXTURES_FOLDER, "000-balances.yml")

    @classmethod
    def load_balances(cls) -> dict[str, list[AccountConfig]]:
        balances_cfg = cls._load_balances_cfg()
        return balances_cfg.balances

    @classmethod
    def _load_balances_cfg(cls) -> BalancesConfig:
        balances = resource.find(cls.BALANCES_FILE_PATH).decode()

        dct = yaml.load(balances, yaml.Loader)

        return BalancesConfig.from_dict(dct=dct.get('balances'))
