import logging
import typing as tp

import pytest
import google.protobuf
from click.testing import CliRunner

from maps.infra.quotateka.config_uploader import main as uploader
from maps.infra.quotateka.client.fixture import QuotatekaFixture
from maps.infra.quotateka.proto import configuration_pb2 as config_pb2
from yandex.maps.proto.quotateka import quotas_pb2
from maps.pylibs.fixtures.matchers import Match


logger = logging.getLogger(__name__)


def assert_click_result(function: tp.Callable[[], None], args: tp.List[str]) -> None:
    runner = CliRunner()
    result = runner.invoke(function, args)
    if result.exception:
        raise result.exception
    logger.info(result.output)
    assert result.exit_code == 0


def test_config_uploader(fixture_factory, configs_root):
    quotateka_fixture: QuotatekaFixture = fixture_factory(QuotatekaFixture)

    test_provider_id = 'maps-router'
    test_staging = 'stable'
    test_client_abc = 'maps-mobile-proxy'

    assert_click_result(uploader, [test_provider_id,
                                   '--token', 'XXX',
                                   '--staging', test_staging,
                                   '--configs-root', configs_root])

    provider = quotateka_fixture.provider(test_provider_id)
    assert provider.id == test_provider_id

    client = quotateka_fixture.client(test_client_abc)
    assert client.abc_slug == test_client_abc

    # Check proto update message contents
    provider_update_proto = quotateka_fixture.quotateka_storage.providers.get(test_provider_id)
    assert provider_update_proto == google.protobuf.json_format.Parse('''{
        "abc_slug": "maps-core-router",
        "abcd_id": "dcaf0290-e1e7-4e57-a7c4-d574b55dda64",
        "tvm_ids": [2010296],
        "localized_name": [{"lang": "en", "value": "router"}],
        "scope_support": "AccountSlugAsScope",
        "resources": [
            {
                "id": "general",
                "localized_name": [{"lang": "en", "value": "General api"}],
                "endpoints": [
                    {"path": "/route", "cost": 3},
                    {"path": "/uri", "cost": 5}
                ],
                "default_limit": 10,
                "anonym_limit": 15
            },
            {
                "id": "heavy",
                "type": "PerDayLimit",
                "localized_name": [{"lang": "en", "value": "Heavy api"}]
            }
        ]
    }''', config_pb2.UpdateProviderRequest())


def test_tvms_check_in_uploader(fixture_factory, colliding_tvm_configs_root):
    fixture_factory(QuotatekaFixture)  # init quotateka

    with pytest.raises(Exception,
                       match='Providers maps-router/stable.yaml, maps-teapot/stable.yaml have the same tvm_id=2010296'):
        assert_click_result(uploader,
                            ['maps-router',
                             '--token', 'XXX',
                             '--staging', 'stable',
                             '--configs-root', colliding_tvm_configs_root])


def test_config_uploader_delete_client(fixture_factory, configs_root, deleted_client_configs_root):
    quotateka_fixture: QuotatekaFixture = fixture_factory(QuotatekaFixture)

    test_staging = 'stable'
    test_client_abc = 'maps-mobile-proxy'

    # Initial upload
    assert_click_result(uploader, ['maps-router',
                                   '--token', 'XXX',
                                   '--staging', test_staging,
                                   '--configs-root', configs_root])

    client = quotateka_fixture.client(test_client_abc)
    assert client.abc_slug == test_client_abc
    router_quotas: quotas_pb2.ClientProfile.ProviderQuota = client.quotas[0]
    total_resources = [{'id': quota_res.resource.id, 'limit': quota_res.limit}
                       for quota_res in router_quotas.total]
    assert total_resources == Match.Contains(
        {'id': 'general', 'limit': 1000},
        {'id': 'heavy', 'limit': 100}
    )

    # Upload with deleted maps-mobile-proxy
    assert_click_result(uploader, ['maps-router',
                                   '--token', 'XXX',
                                   '--staging', test_staging,
                                   '--configs-root', deleted_client_configs_root])
    client = quotateka_fixture.client(test_client_abc)
    assert client.abc_slug == test_client_abc
    router_quotas = client.quotas[0]
    assert len(router_quotas.total) == 0


def test_config_uploader_invalid(fixture_factory, invalid_configs_root):
    fixture_factory(QuotatekaFixture)

    test_staging = 'stable'

    with pytest.raises(Exception):
        assert_click_result(uploader, ['maps-router',
                                       '--token', 'XXX',
                                       '--staging', test_staging,
                                       '--configs-root', invalid_configs_root])
