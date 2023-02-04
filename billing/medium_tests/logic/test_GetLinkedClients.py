# -*- coding: utf-8 -*-


import datetime
import pytest
from tests import object_builder as ob


def new_client(session, **attrs):
    res = ob.ClientBuilder(**attrs).build(session).obj
    return res


def csv_answer_to_list(answer):
    answer = answer.split('\n')[1:-1]
    answer_list = []

    for i, i_val in enumerate(answer):
        l_ = i_val.split('\t')
        answer_list.append({'group_id': l_[0], 'client_id': l_[1], 'link_type': l_[2], 'brand_client_id': l_[3]})

    return answer_list


@pytest.mark.parametrize(
    'params',
    [
        {},
        {'LinkTypes': [7]},
        {'LinkTypes': [0]},
        {'LinkTypes': [0, 7, 77, 99]},
        {'LinkTypes': [0, 7, 77, 99], 'DT': datetime.datetime(2019, 1, 1)},
    ]
)
def test_equal_clients(session, xmlrpcserver, params):
    eq_client_1 = new_client(session)
    eq_client_2 = new_client(session)
    eq_client_2.make_equivalent(eq_client_1)

    session.flush()
    session.expire_all()

    res = xmlrpcserver.GetLinkedClients(params)
    answer = csv_answer_to_list(res)

    link_types = params.get('LinkTypes', [])

    eq_client_1_dict = {
        'group_id': str(eq_client_1.id),
        'client_id': str(eq_client_1.id),
        'link_type': '0',
        'brand_client_id': ''
    }

    eq_client_2_dict = {
        'group_id': str(eq_client_1.id),
        'client_id': str(eq_client_2.id),
        'link_type': '0',
        'brand_client_id': ''
    }

    if not link_types or 0 in link_types:
        assert eq_client_1_dict in answer
        assert eq_client_2_dict in answer
    else:
        assert eq_client_1_dict not in answer
        assert eq_client_2_dict not in answer


@pytest.mark.parametrize(
    'params',
    [
        {},
        {'LinkTypes': [7]},
        {'LinkTypes': [0]},
        {'LinkTypes': [0, 7, 77, 99]},
        {'LinkTypes': [0, 7, 77, 99], 'DT': datetime.datetime.now() + datetime.timedelta(-11)},
        {'LinkTypes': [0, 7, 77, 99], 'DT': datetime.datetime.now() + datetime.timedelta(-1)},
        {'LinkTypes': [0, 7, 77, 99], 'DT': datetime.datetime.now() + datetime.timedelta(21)},
    ]
)
def test_brand_clients(session, xmlrpcserver, params):
    contract_from_dt = datetime.datetime.now() + datetime.timedelta(-10)
    contract_finish_dt = datetime.datetime.now() + datetime.timedelta(20)

    client_brand_1 = new_client(session)
    client_brand_2 = new_client(session)

    contract = ob.create_brand(
        session,
        [(
            contract_from_dt,
            [client_brand_1, client_brand_2]
        )],
        contract_finish_dt
    )
    session.expire_all()

    res = xmlrpcserver.GetLinkedClients(params)
    answer = csv_answer_to_list(res)

    link_types = params.get('LinkTypes', [])
    dt = params.get('DT', datetime.datetime.now())

    client_brand_1_dict = {
        'group_id': str(contract.id),
        'client_id': str(client_brand_1.id),
        'link_type': '7',
        'brand_client_id': str(client_brand_1.id)
    }

    client_brand_2_dict = {
        'group_id': str(contract.id),
        'client_id': str(client_brand_2.id),
        'link_type': '7',
        'brand_client_id': str(client_brand_1.id)
    }

    if (not link_types or 7 in link_types) and (contract_from_dt < dt < contract_finish_dt):
        assert client_brand_1_dict in answer
        assert client_brand_2_dict in answer
    else:
        assert client_brand_1_dict not in answer
        assert client_brand_2_dict not in answer


@pytest.mark.parametrize(
    'params',
    [
        {},
        {'LinkTypes': [7]},
        {'LinkTypes': [0]},
        {'LinkTypes': [0, 7, 77, 99]},
        {'LinkTypes': [0, 7, 77, 99], 'DT': datetime.datetime.now() + datetime.timedelta(-11)},
        {'LinkTypes': [0, 7, 77, 99], 'DT': datetime.datetime.now() + datetime.timedelta(-1)},
        {'LinkTypes': [0, 7, 77, 99], 'DT': datetime.datetime.now() + datetime.timedelta(21)},
    ]
)
def test_equal_and_brand_clients(session, xmlrpcserver, params):
    contract_from_dt = datetime.datetime.now() + datetime.timedelta(-10)
    contract_finish_dt = datetime.datetime.now() + datetime.timedelta(20)

    client_brand_1 = new_client(session)
    eq_client_1 = new_client(session)
    eq_client_1.make_equivalent(client_brand_1)
    client_brand_2 = new_client(session)

    session.flush()
    session.expire_all()

    contract = ob.create_brand(
        session,
        [(
            contract_from_dt,
            [client_brand_1, client_brand_2]
        )],
        contract_finish_dt
    )
    session.expire_all()

    res = xmlrpcserver.GetLinkedClients(params)
    answer = csv_answer_to_list(res)

    link_types = params.get('LinkTypes', [])
    dt = params.get('DT', datetime.datetime.now())

    client_brand_1_brand_dict = {
        'group_id': str(contract.id),
        'client_id': str(client_brand_1.id),
        'link_type': '7',
        'brand_client_id': str(client_brand_1.id)
    }
    client_brand_2_brand_dict = {
        'group_id': str(contract.id),
        'client_id': str(client_brand_2.id),
        'link_type': '7',
        'brand_client_id': str(client_brand_1.id)
    }
    eq_client_1_brand_dict = {
        'group_id': str(contract.id),
        'client_id': str(eq_client_1.id),
        'link_type': '7',
        'brand_client_id': str(client_brand_1.id)
    }

    eq_client_1_equal_dict = {
        'group_id': str(client_brand_1.id),
        'client_id': str(eq_client_1.id),
        'link_type': '0',
        'brand_client_id': ''
    }
    client_brand_1_equal_dict = {
        'group_id': str(client_brand_1.id),
        'client_id': str(client_brand_1.id),
        'link_type': '0',
        'brand_client_id': ''
    }

    if not link_types or 0 in link_types:
        assert eq_client_1_equal_dict in answer
        assert client_brand_1_equal_dict in answer
    else:
        assert eq_client_1_equal_dict not in answer
        assert client_brand_1_equal_dict not in answer

    if (not link_types or 7 in link_types) and (contract_from_dt < dt < contract_finish_dt):
        assert client_brand_1_brand_dict in answer
        assert client_brand_2_brand_dict in answer
        assert eq_client_1_brand_dict in answer
    else:
        assert client_brand_1_brand_dict not in answer
        assert client_brand_2_brand_dict not in answer
        assert eq_client_1_brand_dict not in answer
