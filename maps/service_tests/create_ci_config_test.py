import typing as tp
from dataclasses import dataclass
from textwrap import indent

import pytest

from maps.infra.sedem.cli.lib.ci import CiConfigRenderer
from maps.infra.sedem.cli.lib.service import Service
from maps.infra.sedem.cli.tests.typing import ServiceFactory


@pytest.fixture(scope='function')
def service(service_factory: ServiceFactory) -> ServiceFactory:
    def factory(service_type: str,
                use_testing_machine: bool = False) -> Service:
        config = {
            'main': {
                'name': 'mock',
                'abc_service': 'maps-core-mock',
                'use_testing_machine': use_testing_machine,
            },
            'deploy': {
                'type': service_type,
            },
            'resources': {'stable': {}},
        }
        if service_type == 'sandbox':
            config['deploy'] |= {'sandbox': {
                'owner': 'MAPS-CI',
            }}
        return service_factory(config=config)
    return factory


EXPECTED_CONFIG_TEMPLATE = """\
title: {title}
service: maps-core-mock

ci:
  secret: sec-XXXX  # https://docs.yandex-team.ru/ci/quick-start-guide#get-token
  runtime:
    sandbox:
      owner: MAPS-CI
      # Uncomment to enable notifications
      # notifications:
      #   - statuses: [FAILURE, EXCEPTION]
      #     transport: email
      #     recipients:
      #       - <login>

  flows:
    build:
      title: {title} build flow
      jobs:
        build:
          title: {title} build job
          task: {task}
          input:{params}

  actions:
    build:
      flow: build
      triggers:
        - on: commit
          into: trunk
          filters:
            - discovery: dir
              abs-paths:
                - {observed_path}/**
                # Add more paths to trigger build on if needed
              not-abs-paths:
                - maps/**/tests/**
                - maps/**/README.md
                # Add more paths to exclude from build triggers
"""


@dataclass
class RenderingTestCase:
    name: str
    service_type: tp.Literal['rtc', 'garden', 'sandbox']
    expected_title: str
    expected_task: str
    expected_params: dict[str, str]
    expected_observed_path: str

    def __str__(self) -> str:
        return self.name


RENDERING_TEST_CASES = [
    RenderingTestCase(
        name='nanny',
        service_type='rtc',
        expected_title='Nanny service maps-core-mock',
        expected_task='projects/maps/common/maps_build_docker',
        expected_params={
            'target': 'fake-path/docker/pkg.json',
            'sedem_service_name': 'maps-core-mock',
        },
        expected_observed_path='fake-path/docker',
    ),
    RenderingTestCase(
        name='garden',
        service_type='garden',
        expected_title='Garden module mock',
        expected_task='projects/maps/common/build_garden_module',
        expected_params={
            'target': 'fake-path/bin',
            'module_name': 'mock',
        },
        expected_observed_path='fake-path',
    ),
    RenderingTestCase(
        name='sandbox',
        service_type='sandbox',
        expected_title='Sandbox task mock',
        expected_task='projects/maps/common/build_binary_task/stable',
        expected_params={
            'target': 'fake-path/task',
            'sedem_service_name': 'maps-core-mock',
        },
        expected_observed_path='fake-path/task',
    ),
]


@pytest.mark.parametrize('case', RENDERING_TEST_CASES, ids=str)
@pytest.mark.parametrize('use_testing_sedem', (False, True))
def test_config_rendering(monkeypatch,
                          service: ServiceFactory,
                          case: RenderingTestCase,
                          use_testing_sedem: bool) -> None:
    service = service(service_type=case.service_type,
                      use_testing_machine=use_testing_sedem)
    renderer = CiConfigRenderer()
    actual_config = renderer.render(service)
    expected_params = case.expected_params
    if use_testing_sedem:
        expected_params['use_testing_sedem'] = 'true'
    expected_config = EXPECTED_CONFIG_TEMPLATE.format(
        title=case.expected_title,
        task=case.expected_task,
        params=indent(
            text='\n' + ('\n'.join(
                f'{k}: {v}'
                for k, v in expected_params.items()
            )),
            prefix=' ' * 12,
        ),
        observed_path=case.expected_observed_path,
    )
    assert expected_config == actual_config


def test_config_rendering_fails_for_meta(monkeypatch, service: ServiceFactory) -> None:
    service = service('meta')
    renderer = CiConfigRenderer()
    with pytest.raises(Exception, match=r'is not supported'):
        renderer.render(service)
