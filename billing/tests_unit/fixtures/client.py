# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import allure

from tests import object_builder as ob
from balance import constants as cst, mapper

from brest.core.tests import utils as test_utils


@pytest.fixture(name='client')
@allure.step('create client')
def create_client(passport=None, **kwargs):
    session = test_utils.get_test_session()
    params = {'name': 'test client %s' % ob.get_big_number()}
    params.update(**kwargs)
    client = ob.ClientBuilder.construct(session, **params)
    if passport:
        passport.link_to_client(client)
    return client


@pytest.fixture(name='agency')
@allure.step('create agency')
def create_agency():
    session = test_utils.get_test_session()
    return ob.ClientBuilder(
        name='test_client_agency',
        is_agency=True,
    ).build(session).obj


@pytest.fixture(name='client_with_intercompany')
@allure.step('create client with intercompany')
def create_client_with_intercompany():
    session = test_utils.get_test_session()
    intercompany = session.query(mapper.Intercompany).first()
    client = ob.ClientBuilder(
        name='test_client',
        intercompany=intercompany.flex_value,
    ).build(session).obj
    return client


@pytest.fixture(name='manager')
@allure.step('create single manager')
def create_manager(client=None, w_mv=False, **kwargs):
    session = test_utils.get_test_session()
    manager = ob.SingleManagerBuilder.construct(
        session,
        email=kwargs.pop('email', 'snout_manager@email.com'),
        **kwargs  # noqa:
    )
    if w_mv:
        assert client is not None
        q = """
        insert into bo.mv_client_manager(client_id, client_name, manager_code, manager_name, manager_hidden)
        values(:client_id, :client_name, :manager_code, :manager_name, :manager_hidden)
        """
        params = {
            'client_id': client.id,
            'client_name': client.name,
            'manager_code': manager.manager_code,
            'manager_name': manager.name,
            'manager_hidden': 0,
        }
        session.execute(q, params)
    session.flush()
    return manager


@pytest.fixture(name='role_client')
def create_role_client(client=None):
    session = test_utils.get_test_session()
    client = client or create_client()
    return ob.RoleClientBuilder.construct(session, client=client)


@pytest.fixture(name='role_client')
def create_role_client_group(clients=None):
    session = test_utils.get_test_session()
    clients = clients or [create_client()]
    return ob.RoleClientGroupBuilder.construct(session, clients=clients)


def create_client_service_data(client, service_id=cst.ServiceId.DIRECT, **kwargs):
    client_service_data = ob.create_client_service_data(**kwargs)
    client.service_data[service_id] = client_service_data
    return client_service_data
