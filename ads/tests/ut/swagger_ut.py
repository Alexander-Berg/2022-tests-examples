import json

import pytest
from jsonschema import RefResolver
from swagger_spec_validator import validator20

import ads.watchman.timeline.api.tests.helpers as helpers
import ads.watchman.timeline.api.lib.app as app


@pytest.fixture(scope='module')
def flask_app_client():
    config = helpers.model_generators.TestingConfig()
    return app.make_app(config).test_client()


@pytest.fixture(scope='module')
def swagger_config(flask_app_client):
    raw_swagger_spec = flask_app_client.get('/swagger.json').data
    return json.loads(raw_swagger_spec.decode('utf-8'))


def test_that_swagger_spec_valid(swagger_config):
    assert isinstance(validator20.validate_spec(swagger_config), RefResolver)


def test_that_swagger_has_valid_number_of_namespaces(swagger_config):
    expected_namespaces = sorted(['events', 'tickets'])
    assert sorted(tag['name'] for tag in swagger_config['tags']) == expected_namespaces


def test_that_swagger_has_valid_version(swagger_config):
    assert swagger_config['info']['version'] == helpers.model_generators.TestingConfig.DEPLOY_VERSION


def test_that_swagger_has_valid_description(swagger_config):
    assert swagger_config['info']['description'] == helpers.model_generators.TestingConfig.DEPLOY_DESCRIPTION
