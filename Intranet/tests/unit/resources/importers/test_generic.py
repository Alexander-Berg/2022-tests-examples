import pretend
import pytest

from plan.resources.importers import base
from plan.resources.importers.generic import GenericPlugin
from common import factories
from utils import vcr_test

pytestmark = pytest.mark.django_db


@pytest.fixture
def data(db):
    service1 = factories.ServiceFactory(id=9910, slug='slug9910')
    service2 = factories.ServiceFactory(id=9911, slug='slug9911')
    resource_type = factories.ResourceTypeFactory(
        import_plugin='generic',
        import_link='http://yandex.ru/?consumer=abc',
    )
    fixture = pretend.stub(
        resource_type=resource_type,
        service1=service1,
        service2=service2,
    )
    return fixture


def test_get_plugin(data):
    plugin_class = base.Plugin.get_plugin_class(data.resource_type.import_plugin)
    assert plugin_class == GenericPlugin


def test_fetch_resources(data):
    plugin = GenericPlugin(resource_type=data.resource_type)

    cassette_name = 'resources/generic_importer.json'
    with vcr_test().use_cassette(cassette_name):
        result = plugin.fetch()

    assert len(result) == 2

    plugin_data = {rec['service'].slug: rec for rec in result}

    assert len(plugin_data['slug9910']['resources']) == 1
    assert plugin_data['slug9910']['resources'][0]['name'] == 'Passport-test'

    assert len(plugin_data['slug9911']['resources']) == 1
    assert plugin_data['slug9911']['resources'][0]['name'] == 'Passport-test2'


def test_fetch_resources_without_meta(data):
    plugin = GenericPlugin(resource_type=data.resource_type)

    cassette_name = 'resources/generic_importer_without_meta.json'
    with vcr_test().use_cassette(cassette_name):
        result = plugin.fetch()

    assert len(result) == 2

    plugin_data = {rec['service'].slug: rec for rec in result}

    assert len(plugin_data['slug9910']['resources']) == 1
    assert plugin_data['slug9910']['resources'][0]['name'] == 'Passport-test'

    assert len(plugin_data['slug9911']['resources']) == 1
    assert plugin_data['slug9911']['resources'][0]['name'] == 'Passport-test2'
