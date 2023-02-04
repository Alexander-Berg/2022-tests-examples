import os
import pytest

from maps.infra.baseimage.template_generator.lib import template_generator
from maps.infra.baseimage.template_generator.lib.environment import cpu_guarantee_env, load_environment

from unittest import mock

Environment = template_generator.Environment


ASSERT_MAPPINGS = [
    dict(name="stable", type="stable", legacy_type="production"),
    dict(name="prestable", type="stable", legacy_type="production"),
    dict(name="testing", type="testing", legacy_type="testing"),
    dict(name="load", type="testing", legacy_type="testing"),
    dict(name="unstable", type="testing", legacy_type="testing"),
    dict(name="datatesting", type="stable", legacy_type="production"),
    dict(name="datavalidation", type="stable", legacy_type="production"),
    dict(name="dataprestable", type="stable", legacy_type="production"),
    dict(name="experiments", type="stable", legacy_type="production"),
    dict(name="compattesting", type="testing", legacy_type="testing"),
    dict(name="datatestingload", type="stable", legacy_type="production"),
    dict(name="testingdatatesting", type="testing", legacy_type="testing"),
]


@pytest.fixture(params=ASSERT_MAPPINGS)
def mapping(request):
    return request.param


def test_mappings(mapping, monkeypatch):
    monkeypatch.setenv("NANNY_SERVICE_ID", f'maps_core_teapot_{mapping["name"]}')

    loaded_config = template_generator.load_environment()

    expected = dict(name=loaded_config[Environment.Name],
                    type=loaded_config[Environment.Type],
                    legacy_type=loaded_config[Environment.TypeSynonym])

    assert expected == mapping


def test_cpu_guarantee_env():
    assert cpu_guarantee_env({'CPU_GUARANTEE': '2'}) == 2


@mock.patch.dict(os.environ, {}, clear=True)
def test_load_environment_empty():
    assert load_environment() == {
        'ENV': {},
        'ENVIRONMENT_NAME': 'development',
        'ENVIRONMENT_TYPE': 'development',
        'ENVIRONMENT_TYPE_SYNONYM': 'development',
        'LIMITS': {
            'CPU': 1
        },
        'app': {
            'name': None
        }
    }


@mock.patch.dict(os.environ, {
    'CPU_GUARANTEE': '2',
    'ENVIRONMENT_NAME': 'prestable',
    'DEPLOY_PROJECT_ID': 'maps-core-bar'
}, clear=True)
def test_load_environment():
    assert load_environment() == {
        'ENV': {
            'CPU_GUARANTEE': '2',
            'ENVIRONMENT_NAME': 'prestable',
            'DEPLOY_PROJECT_ID': 'maps-core-bar'
        },
        'ENVIRONMENT_NAME': 'prestable',
        'ENVIRONMENT_TYPE': 'stable',
        'ENVIRONMENT_TYPE_SYNONYM': 'production',
        'LIMITS': {
            'CPU': 2
        },
        'app': {
            'name': 'maps-core-bar'
        }
    }
