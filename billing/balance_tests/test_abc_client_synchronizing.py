# -*- coding: utf-8 -*-
import re
import datetime
import pytest
import mock
import httpretty
import json
import itertools
import hamcrest as hm

from balance import constants as cst, exc, mapper
from balance.api.abc import ABCConnection
from tests import object_builder as ob
from tests.tutils import has_exact_entries

from cluster_tools.abc_client_synchronizing import (
    OBSOLETE_RANGE,
    clear_groups,
    enqueue,
    clean_role_client,
    clean_hidden_role_clients,
    set_groups_obsolete_dt,
)

PERM = 'Permissions'
NOW = datetime.datetime.now()
DAY_AGO = NOW - datetime.timedelta(days=1)


def make_response(abc_api, add_clients=None, del_clients=None, url=None, next_=None, results=None):
    if results is None:
        results = []
        for state, clients in [('granted', add_clients), ('deprived', del_clients)]:
            for client in (clients or []):
                results.append({
                    'state': state,
                    'modified_at': (datetime.datetime.now() + datetime.timedelta(seconds=10)).strftime('%Y-%m-%dT%H:%M:%S.%fZ'),
                    'resource': {'attributes': [{'name': 'client_id', 'value': str(client.id)}]},
                })

    abc_api._type_id = 666
    httpretty.register_uri(
        httpretty.GET,
        url or abc_api.data_url,
        status=200,
        content_type="text/json",
        body=json.dumps({
            'previous': None,
            'next': next_,
            'results': results,
        }),
    )


def assert_response(group_id, fill=False, page_size=None):
    assert httpretty.has_request() is True
    params = httpretty.last_request().querystring
    required_params = {
        u'state': hm.contains_inanyorder(u'granted', u'deprived'),
        u'type': hm.contains(u'666'),
        u'service': hm.contains(unicode(group_id)),
        u'fields': hm.contains(u'id,state,modified_at,resource.id,resource.attributes'),
    }
    if not fill:
        required_params[u'modified_at__gt'] = hm.contains(DAY_AGO.strftime(u'%Y-%m-%dT%H:%M:%S'))
    if page_size:
        required_params['page_size'] = [unicode(page_size)]
    hm.assert_that(
        params,
        has_exact_entries(required_params),
    )


@pytest.fixture(name='role')
def create_role(session):
    return ob.create_role(session, PERM)


@pytest.fixture(name='client')
def create_client(session):
    return ob.ClientBuilder.construct(session)


def test_make_groups_obsolete(session, role):
    group_1 = ob.RoleClientGroupBuilder.construct(session)
    group_2 = ob.RoleClientGroupBuilder.construct(session)
    group_3 = ob.RoleClientGroupBuilder.construct(session, obsolete_dt=session.now())
    for gr in [group_1, group_3]:
        mapper.RoleGroup.set_roles(
            session,
            gr.external_id,
            [(role, {cst.ConstraintTypes.client_batch_id: gr.client_batch_id})],
        )
    session.flush()

    set_groups_obsolete_dt(session)
    session.refresh(group_1), session.refresh(group_2), session.refresh(group_3)
    hm.assert_that(group_1, hm.has_properties(obsolete_dt=hm.none()))
    hm.assert_that(group_2, hm.has_properties(obsolete_dt=hm.not_none()))
    hm.assert_that(group_3, hm.has_properties(obsolete_dt=hm.none()))


def test_clear_groups(session):
    groups = [
        ob.RoleClientGroupBuilder.construct(session, obsolete_dt=session.now() - datetime.timedelta(days=OBSOLETE_RANGE + 1))
        for _i in range(3)
    ]
    group_ids = [gr.external_id for gr in groups]
    client_batch_ids = [gr.client_batch_id for gr in groups]

    active_group = ob.RoleClientGroupBuilder.construct(
        session,
        obsolete_dt=session.now() - datetime.timedelta(days=OBSOLETE_RANGE - 1),
        clients=[create_client(session)],
    )
    session.flush()

    clear_groups(session)
    assert session.query(mapper.RoleClientGroup).filter(mapper.RoleClientGroup.external_id.in_(group_ids)).count() == 0
    assert session.query(mapper.RoleClient).filter(mapper.RoleClient.client_batch_id.in_(client_batch_ids)).count() == 0
    assert session.query(mapper.RoleClientGroup).filter_by(external_id=active_group.external_id).count() == 1
    assert session.query(mapper.RoleClient).filter_by(client_batch_id=active_group.client_batch_id).count() == 1


def test_clean_role_client(session):
    group_client_batch_id = ob.RoleClientGroupBuilder.construct(session, clients=[create_client(session)]).client_batch_id
    user_client_batch_id = ob.RoleClientBuilder.construct(session).client_batch_id
    ob.set_roles(session, session.passport, [(ob.create_role(session), {cst.ConstraintTypes.client_batch_id: user_client_batch_id})])
    client_batch_ids = [ob.RoleClientBuilder.construct(session).client_batch_id for _i in range(2)]

    clean_role_client(session)
    session.flush()
    role_clients = (
        session.query(mapper.RoleClient.client_batch_id)
        .filter(mapper.RoleClient.client_batch_id.in_([group_client_batch_id, user_client_batch_id] + client_batch_ids))
        .all()
    )
    assert role_clients == [(group_client_batch_id,), (user_client_batch_id,)]


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_type_id(app, session):
    abc_api = ABCConnection(app, session)
    abc_api._type_id = 666
    httpretty.register_uri(
        httpretty.GET,
        abc_api.type_url,
        status=200,
        json={
            "previous": None,
            "results": [
                {
                    "is_enabled": None,
                    "supplier": {
                        "id": 333,
                        "slug": "direct"
                    },
                    "id": 666,
                    "name": {
                        "ru": "Клиент Директа",
                        "en": "Клиент Директа"
                    }
                }
            ],
            "next": None,
        },
    )
    assert abc_api.get_type_id() == 666


@pytest.mark.parametrize(
    'is_exist',
    [True, False],
)
@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_update_group_add(app, session, is_exist):
    clients = [create_client(session) for _i in range(2)]
    new_client = create_client(session)
    res_clients = clients + [new_client]
    if is_exist:
        clients.append(new_client)

    group = ob.RoleClientGroupBuilder.construct(session, refresh_dt=DAY_AGO, clients=clients)
    session.flush()

    abc_api = ABCConnection(app, session)
    make_response(abc_api, add_clients=[new_client])
    abc_api.update_clients()
    assert_response(group.external_id)

    session.refresh(group)
    hm.assert_that(
        group.clients,
        hm.contains_inanyorder(*res_clients),
    )


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_update_group_add_several_row(app, session):
    clients = [create_client(session) for _i in range(2)]
    new_clients = [create_client(session) for _i in range(2)]
    res_clients = clients + new_clients

    group = ob.RoleClientGroupBuilder.construct(session, refresh_dt=DAY_AGO, clients=clients)
    session.flush()

    abc_api = ABCConnection(app, session)
    make_response(abc_api, add_clients=new_clients)
    abc_api.update_clients()
    assert_response(group.external_id)

    session.refresh(group)
    hm.assert_that(
        group.clients,
        hm.contains_inanyorder(*res_clients),
    )


@pytest.mark.parametrize(
    'is_exist',
    [True, False],
)
@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_update_group_remove(app, session, is_exist):
    clients = [create_client(session) for _i in range(2)]
    res_clients = [clients[0]] if is_exist else clients

    group = ob.RoleClientGroupBuilder.construct(session, refresh_dt=DAY_AGO, clients=clients)
    session.flush()

    abc_api = ABCConnection(app, session)
    make_response(abc_api, del_clients=[clients[1]] if is_exist else [create_client(session)])
    abc_api.update_clients()
    assert_response(group.external_id)

    session.refresh(group)
    hm.assert_that(
        group.clients,
        hm.contains_inanyorder(*res_clients),
    )


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_update_group_remove_several_row(app, session):
    clients = [create_client(session) for _i in range(2)]
    group = ob.RoleClientGroupBuilder.construct(session, refresh_dt=DAY_AGO, clients=clients)
    session.flush()

    abc_api = ABCConnection(app, session)
    make_response(abc_api, del_clients=clients)
    abc_api.update_clients()
    assert_response(group.external_id)

    session.refresh(group)
    hm.assert_that(
        group.clients,
        hm.empty(),
    )


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_update_group_add_and_remove(app, session):
    clients = [create_client(session) for _i in range(4)]
    new_clients = [create_client(session) for _i in range(2)]
    group = ob.RoleClientGroupBuilder.construct(session, refresh_dt=DAY_AGO, clients=clients)
    session.flush()

    abc_api = ABCConnection(app, session)
    make_response(abc_api, add_clients=new_clients, del_clients=clients[2:])
    abc_api.update_clients()
    assert_response(group.external_id)

    session.refresh(group)
    hm.assert_that(
        group.clients,
        hm.contains_inanyorder(*(clients[:2] + new_clients)),
    )
    role_clients = session.query(mapper.RoleClient).filter_by(client_batch_id=group.client_batch_id).all()
    hm.assert_that(
        role_clients,
        hm.contains_inanyorder(*[
            hm.has_properties({
                'client_batch_id': group.client_batch_id,
                'client_id': client.id,
                'update_dt': hm.is_not(None),
                'hidden': hm.is_(hid),
            })
            for hid, client in itertools.chain(zip([True] * 2, clients[2:]), zip([None] * 4, clients[:2] + new_clients))
        ]),
    )


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_update_next(app, session):
    clients = [create_client(session) for _i in range(2)]
    new_clients = [create_client(session) for _i in range(4)]
    group = ob.RoleClientGroupBuilder.construct(session, refresh_dt=DAY_AGO, clients=clients)
    session.flush()

    abc_api = ABCConnection(app, session)
    make_response(abc_api, add_clients=new_clients[:2], next_=abc_api.data_url + 'test/?next_unique_id=666')
    make_response(abc_api, add_clients=new_clients[2:], url=abc_api.data_url + 'test/?next_unique_id=666', next_=None)
    abc_api.update_clients()

    assert httpretty.has_request() is True
    params = httpretty.last_request().querystring
    hm.assert_that(params, hm.has_entries({u'next_unique_id': hm.contains(u'666')}))

    session.refresh(group)
    hm.assert_that(
        group.clients,
        hm.contains_inanyorder(*(clients + new_clients)),
    )


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_fill_new_group(app, session):
    new_clients = [create_client(session) for _i in range(2)]
    group = ob.RoleClientGroupBuilder.construct(session)
    session.flush()

    abc_api = ABCConnection(app, session)
    make_response(abc_api, add_clients=new_clients)
    abc_api.fill_new_groups()
    assert_response(group.external_id, fill=True)

    session.refresh(group)
    hm.assert_that(
        group.clients,
        hm.contains_inanyorder(*new_clients),
    )


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_fill_new_group_next(app, session):
    new_clients = [create_client(session) for _i in range(6)]
    group = ob.RoleClientGroupBuilder.construct(session)
    session.flush()

    abc_api = ABCConnection(app, session)
    make_response(abc_api, add_clients=new_clients[:3], next_=abc_api.data_url + 'test/?next_unique_id=666')
    make_response(abc_api, add_clients=new_clients[3:], url=abc_api.data_url + 'test/?next_unique_id=666', next_=None)
    abc_api.fill_new_groups()

    session.refresh(group)
    hm.assert_that(
        group.clients,
        hm.contains_inanyorder(*new_clients),
    )


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_fill_new_group_w_same_clients(app, session):
    clients = [create_client(session) for _i in range(2)]
    group = ob.RoleClientGroupBuilder.construct(session, clients=clients)
    session.flush()

    abc_api = ABCConnection(app, session)
    make_response(abc_api, add_clients=clients)
    abc_api.fill_new_groups()
    assert_response(group.external_id, fill=True)

    session.refresh(group)
    hm.assert_that(
        group.clients,
        hm.contains_inanyorder(*clients),
    )


def test_wrong_data_from_abc(app, session):
    new_clients = [create_client(session) for _i in range(2)]
    del_clients = [create_client(session) for _i in range(1)]
    clients = [create_client(session) for _i in range(2)]
    session.flush()

    dt = NOW.strftime('%Y-%m-%dT%H:%M:%S.%fZ')
    data = {
        'results': [
            {
                'state': 'granted',
                'modified_at': dt,
                'resource': {'attributes': [{'name': 'client_id', 'value': str(new_clients[0].id)}]},
            },
            {
                'state': 'granted',
                'modified_at': dt,
                'resource': {'attributes': [{'name': 'client_id', 'value': str(new_clients[1].id)}]},
            },
            {
                'state': 'deprived',
                'modified_at': dt,
                'resource': {'attributes': [{'name': 'client_id', 'value': str(del_clients[0].id)}]},
            },
            {
                'state': 'wrong',
                'modified_at': dt,
                'resource': {'attributes': [{'name': 'client_id', 'value': str(clients[0].id)}]},
            },
            {
                'state': 'granted',
                'modified_at': dt,
                'resource': {'attributes': [{'name': 'wrong', 'value': str(clients[1].id)}]},
            },
            {
                'state': 'granted',
                'modified_at': dt,
                'resource': {'attributes': [{'name': 'client_id', 'value': '-666'}]},
            },
        ],
    }
    abc_api = ABCConnection(app, session)
    clients = abc_api.parse_client_ids(data)
    hm.assert_that(
        clients.values(),
        hm.contains_inanyorder(
            hm.has_properties(id=new_clients[0].id, state='granted', modified_dt=NOW),
            hm.has_properties(id=new_clients[1].id, state='granted', modified_dt=NOW),
            hm.has_properties(id=del_clients[0].id, state='deprived', modified_dt=NOW),
        ),
    )


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_empty_response(app, session):
    group = ob.RoleClientGroupBuilder.construct(session, refresh_dt=DAY_AGO)

    abc_api = ABCConnection(app, session)
    make_response(abc_api)
    abc_api.update_clients()
    assert_response(group.external_id)

    session.refresh(group)
    hm.assert_that(
        group.clients,
        hm.empty(),
    )


@pytest.mark.parametrize(
    'func_name',
    ['update_clients', 'fill_new_groups'],
)
@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_empty_groups(app, session, func_name):
    group = ob.RoleClientGroupBuilder.construct(session, refresh_dt=DAY_AGO if func_name != 'update_clients' else None)
    abc_api = ABCConnection(app, session)
    make_response(abc_api)
    getattr(abc_api, func_name)()
    assert httpretty.has_request() is False

    session.refresh(group)
    hm.assert_that(
        group.clients,
        hm.empty(),
    )


@pytest.mark.parametrize(
    'func_name',
    ['update_clients', 'get_type_id'],
)
@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_api_error_w_status_code(app, session, func_name):
    _group = ob.RoleClientGroupBuilder.construct(session, refresh_dt=DAY_AGO)

    abc_api = ABCConnection(app, session)
    body = 'TEST_BODY'
    httpretty.register_uri(
        httpretty.GET,
        re.compile(abc_api.base_url + '.+'),
        status=400,
        body=body,
    )
    with pytest.raises(exc.ABC_API_ERROR) as exc_info:
        getattr(abc_api, func_name)()
    assert exc_info.value.msg == u'Abc api error: status_code=400, text=%s' % body


@pytest.mark.parametrize(
    'func_name',
    ['update_clients', 'get_type_id'],
)
@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_api_error_w_res_body(app, session, func_name):
    _group = ob.RoleClientGroupBuilder.construct(session, refresh_dt=DAY_AGO)

    abc_api = ABCConnection(app, session)
    body = json.dumps({u'error': {u'message': u'Service is not found', u'code': u'not_found'}})
    httpretty.register_uri(
        httpretty.GET,
        re.compile(abc_api.base_url + '.+'),
        status=200,
        content_type="text/json",
        body=body,
    )
    with pytest.raises(exc.ABC_API_ERROR) as exc_info:
        getattr(abc_api, func_name)()
    assert exc_info.value.msg == u'Abc api error: status_code=200, text=%s' % body


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_page_size(app, session):
    group = ob.RoleClientGroupBuilder.construct(session, refresh_dt=DAY_AGO)
    page_size = 4
    session.config.__dict__['ABC_API_PAGE_SIZE'] = page_size

    abc_api = ABCConnection(app, session)
    make_response(abc_api)
    abc_api.update_clients()
    assert_response(group.external_id, page_size=page_size)


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_update_group_add_unexisted_client(app, session):
    clients = [create_client(session) for _i in range(2)]
    real_new_client = create_client(session)
    fake_new_client = mock.MagicMock(id=-666)
    res_clients = clients + [real_new_client]

    group = ob.RoleClientGroupBuilder.construct(session, refresh_dt=DAY_AGO, clients=clients)
    session.flush()

    abc_api = ABCConnection(app, session)
    make_response(abc_api, add_clients=[real_new_client, fake_new_client])
    abc_api.update_clients()
    assert_response(group.external_id)

    session.refresh(group)
    hm.assert_that(
        group.clients,
        hm.contains_inanyorder(*res_clients),
    )


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_update_group_remove_unexisted_client(app, session):
    clients = [create_client(session) for _i in range(3)]
    real_new_client = clients[-1]
    fake_new_client = mock.MagicMock(id=-666)
    res_clients = clients[:-1]

    group = ob.RoleClientGroupBuilder.construct(session, refresh_dt=DAY_AGO, clients=clients)
    session.flush()

    abc_api = ABCConnection(app, session)
    make_response(abc_api, del_clients=[real_new_client, fake_new_client])
    abc_api.update_clients()
    assert_response(group.external_id)

    session.refresh(group)
    hm.assert_that(
        group.clients,
        hm.contains_inanyorder(*res_clients),
    )


@pytest.mark.parametrize(
    'add',
    [
        pytest.param(False, id='add'),
        pytest.param(True, id='remove'),
    ],
)
@pytest.mark.parametrize(
    'in_different_batches',
    [
        pytest.param(True, id='in different batches'),
        pytest.param(False, id='in one batch'),
    ],
)
@pytest.mark.parametrize(
    'w_client',
    [
        pytest.param(True, id='w client'),
        pytest.param(False, id='wo client'),
    ],
)
@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_several_status_for_client_id(app, session, client, add, in_different_batches, w_client):
    group = ob.RoleClientGroupBuilder.construct(session, refresh_dt=DAY_AGO, clients=[client] if w_client else [])
    session.flush()
    dt1 = session.now()
    dt2 = session.now() + datetime.timedelta(days=1)

    abc_api = ABCConnection(app, session)
    abc_api._type_id = 666
    params = [
        {
            'state': 'deprived',
            'modified_at': (dt1 if add else dt2).strftime('%Y-%m-%dT%H:%M:%S.%fZ'),
            'resource': {'attributes': [{'name': 'client_id', 'value': str(client.id)}]},
        },
        {
            'state': 'granted',
            'modified_at': (dt2 if add else dt1).strftime('%Y-%m-%dT%H:%M:%SZ'),
            'resource': {'attributes': [{'name': 'client_id', 'value': str(client.id)}]},
        },
    ]
    parametrize = [
        [
            (abc_api.data_url, abc_api.data_url + 'test/?next_unique_id=666', [params[0]]),
            (abc_api.data_url + 'test/?next_unique_id=666', None, [params[1]]),
        ],
        [
            (abc_api.data_url, None, params),
        ],
    ]
    for url, next_, item_params in parametrize[0 if in_different_batches else 1]:
        httpretty.register_uri(
            httpretty.GET,
            url,
            status=200,
            content_type="text/json",
            body=json.dumps({
                'previous': None,
                'next': next_,
                'results': item_params,
            }),
        )

    abc_api.update_clients()

    assert httpretty.has_request() is True

    session.refresh(group)
    hm.assert_that(
        group.clients,
        hm.contains_inanyorder(*([client] if add else [])),
    )

    role_client = session.query(mapper.RoleClient).filter_by(client_batch_id=group.client_batch_id, client_id=client.id).one()
    assert role_client.update_dt.strftime('%Y-%m-%dT%H:%M:%S') == dt2.strftime('%Y-%m-%dT%H:%M:%S')
    assert role_client.hidden is not add


@pytest.mark.parametrize(
    'reversed_',
    [False, True],
)
@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_microsecond(app, session, reversed_):
    group = ob.RoleClientGroupBuilder.construct(session, refresh_dt=datetime.datetime(2020, 12, 1), clients=[])
    client = create_client(session)

    results = []
    dates = [
        '2020-12-08T23:07:55.1234Z',
        '2020-12-08T23:07:55.1235Z',
    ]
    if reversed_:
        dates = dates[::-1]

    for state, dt in zip(['granted', 'deprived'], dates):
        results.append({
            'state': state,
            'modified_at': dt,
            'resource': {'attributes': [{'name': 'client_id', 'value': str(client.id)}]},
        })

    abc_api = ABCConnection(app, session)
    abc_api._type_id = 666
    make_response(abc_api, results=results)
    abc_api.update_clients()

    session.refresh(group)
    if reversed_:
        match_res = hm.contains(
            hm.has_properties(
                id=client.id,
            ),
        )
    else:
        match_res = hm.empty()
    hm.assert_that(
        group.clients,
        match_res,
    )


def test_delete_hidden_rows(session):
    client_batch_ids = [
        ob.RoleClientBuilder.construct(session, client=create_client(session), hidden=True).client_batch_id,
        ob.RoleClientBuilder.construct(session, client=create_client(session)).client_batch_id,
    ]
    clean_hidden_role_clients(session)
    session.flush()

    assert session.query(mapper.RoleClient.client_batch_id).filter(mapper.RoleClient.client_batch_id.in_(client_batch_ids)).all() == [(client_batch_ids[1],)]


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_enqueue(app, session):
    clients = [create_client(session) for _i in range(4)]
    new_clients = [create_client(session) for _i in range(2)]
    group = ob.RoleClientGroupBuilder.construct(session, refresh_dt=DAY_AGO, clients=clients)
    session.flush()

    abc_api = ABCConnection(app, session)
    make_response(abc_api, add_clients=new_clients, del_clients=clients[2:])
    with mock.patch('balance.api.abc.ABCConnection.get_type_id', return_value=666):
        enqueue(app, session)
    assert_response(group.external_id)

    session.refresh(group)
    hm.assert_that(
        group.clients,
        hm.contains_inanyorder(*(clients[:2] + new_clients)),
    )
    role_clients = session.query(mapper.RoleClient).filter_by(client_batch_id=group.client_batch_id).all()
    hm.assert_that(
        role_clients,
        hm.contains_inanyorder(*[
            hm.has_properties({
                'client_batch_id': group.client_batch_id,
                'client_id': client.id,
                'update_dt': hm.is_not(None),
                'hidden': hm.is_(None),
            })
            for client in clients[:2] + new_clients
        ]),
    )
