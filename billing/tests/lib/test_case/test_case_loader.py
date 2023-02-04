import typing as tp
from os import path

import yaml

from billing.hot.tests.lib.test_case.config import TestConfig, TestCase, SystemTestBundleConfig

from library.python import resource


class TestConfigLoader:
    CONFIG_FILE_PATH = "config"

    @classmethod
    def load_testing_namespaces(cls) -> dict[str, list[str]]:
        cfg = resource.find(cls.CONFIG_FILE_PATH).decode()
        cfg_dct = yaml.load(cfg, yaml.Loader)
        return cfg_dct.get('testing_namespaces', {})

    @classmethod
    def load_baseline_cases(cls) -> list[tuple[str, tp.Optional[str], TestCase]]:
        testing_namespaces = cls.load_testing_namespaces()
        cases = []
        for namespace in testing_namespaces.get('baseline'):
            for case in NamespaceTestLoader(namespace).cases:
                cases.append((case.namespace, case.description, case))
        return cases

    @classmethod
    def load_system_cases(cls) -> list[tuple[str, tp.Optional[str], TestCase]]:
        testing_namespaces = cls.load_testing_namespaces()
        cases = []
        for namespace in testing_namespaces.get('baseline'):
            for case in NamespaceTestLoader(namespace).cases:
                cases.append((case.namespace, case.description, case))
        return cases


class NamespaceTestLoader:
    FIXTURES_FOLDER = "fixtures"
    CONSTS_FILE_PATH = path.join(FIXTURES_FOLDER, "000-consts.yml")

    def __init__(self, namespace: str):
        self._namespace = namespace
        self._config = self._load_config(namespace)

    def _load_config(self, namespace: str) -> TestConfig:
        cases_spec = path.join(self.FIXTURES_FOLDER, f"001-{namespace}.yml")

        consts = resource.find(self.CONSTS_FILE_PATH).decode()
        cases = resource.find(cases_spec).decode()

        dct = yaml.load("{}\n{}".format(consts, cases), yaml.Loader)
        dct["namespace"] = self._namespace

        return TestConfig.from_dict(dct=dct)

    @property
    def system_test_bundle(self) -> SystemTestBundleConfig:
        return self._config.system

    @property
    def cases(self) -> list[TestCase]:
        cases = []
        for endpoint_cases in self._config.cases.values():
            cases += endpoint_cases
        return cases
