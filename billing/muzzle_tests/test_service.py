# -*- coding: utf-8 -*-

import pytest
import mock

from balance.constants import ServiceId
from muzzle.api import service as service_api

from tests import object_builder as ob


def test_service(session):
    service = service_api.get(session, ServiceId.DIRECT)
    assert service.id == ServiceId.DIRECT


def test_get_services(session, muzzle_logic):
    res = muzzle_logic.get_services(session)
    service = res[0]
    service_attrib = {'in_contract', 'service_cc', 'service_id', 'service_name', 'service_url_orders'}
    assert res.tag == 'services'
    assert service.tag == 'service'
    assert set(service.attrib.keys()) == service_attrib


@pytest.mark.parametrize(
    'test_env, env_type, is_ok',
    [
        (0, 'prod', True),
        (0, 'test', True),
        (1, 'prod', False),
        (1, 'test', True),
        (1, 'dev', True),
    ]
)
def test_with_test_env(session, app, test_env, env_type, is_ok):
    service = ob.ServiceBuilder.construct(session)
    service.balance_service.test_env = test_env
    service.balance_service.show_to_user = 1
    session.flush()

    with mock.patch.object(app, 'get_current_env_type', return_value=env_type):
        services = service_api.get(session)

    services_ids = {s.id for s in services}
    if is_ok:
        assert service.id in services_ids
    else:
        assert service.id not in services_ids
