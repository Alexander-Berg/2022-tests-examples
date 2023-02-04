import os
import tempfile
import typing as tp
from dataclasses import dataclass

import pytest
import yatest

from maps.infra.environment_linker.lib import environment_linker


@pytest.fixture(scope='function')
def config_path():
    with tempfile.TemporaryDirectory(dir=yatest.common.output_path()) as tmpdir:
        def factory(name: str):
            return os.path.join(tmpdir, name)
        yield factory


@pytest.fixture(scope='function')
def create_config(config_path):
    files = []
    def factory(name: str):
        filename = config_path(name)
        with open(filename, 'w') as cfg:
            cfg.write('abcdef')
        files.append(filename)
        return filename
    yield factory
    for filename in files:
        os.unlink(filename)


@dataclass
class LinkerTestCase:
    name: str
    config_envs: list[str]
    environment: str
    expected_target: tp.Optional[str]

    def __str__(self):
        return self.name


TEST_CASES = [
    LinkerTestCase(
        name='link_exact',
        config_envs=[
            'testing',
            'default',
        ],
        environment='testing',
        expected_target='testing',
    ),
    LinkerTestCase(
        name='link_default',
        config_envs=[
            'testing',
            'default',
        ],
        environment='stable',
        expected_target='default',
    ),
    LinkerTestCase(
        name='nolink_w/o_default',
        config_envs=[
            'testing',
        ],
        environment='stable',
        expected_target=None,
    ),
]


for environment, synonyms in environment_linker.ENVIRONMENT_SYNONYMS.items():
    for synonym in synonyms:
        TEST_CASES.append(
            LinkerTestCase(
                name=f'link_{synonym}_in_{environment}',
                config_envs=[
                    synonym,
                ],
                environment=environment,
                expected_target=synonym,
            )
        )


@pytest.mark.parametrize('case', TEST_CASES, ids=str)
def test_linker(create_config, config_path, case):
    paths = []
    for environment in case.config_envs:
        paths.append(create_config(f'tst.{environment}.conf'))

    environment_linker.link_env(
        paths,
        [case.environment]
    )

    filename = config_path('tst.conf')

    if case.expected_target is not None:
        target_filename = config_path(f'tst.{case.expected_target}.conf')
        assert os.path.exists(filename)
        assert os.path.islink(filename)
        assert os.readlink(filename) == target_filename
    else:
        assert not os.path.exists(filename)
