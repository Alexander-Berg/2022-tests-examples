# -*- coding: utf-8 -*-


import random
import datetime as dt
import decimal
import pytest
from tests import object_builder as ob

from balance import mapper, muzzle_util as ut
from balance import constants as cnst


def new_client(session, **attrs):
    res = ob.ClientBuilder(**attrs).build(session).obj
    return res


CODE_SUCCESS = 0
CODE_CLIENT_NOT_FOUND = 1003


def test_get_equal_clients(session, xmlrpcserver):
    def check_response(client_list):
        assert len(res) == 3
        assert (res[0], res[1]) == (CODE_SUCCESS, 'SUCCESS')
        assert list(client_['CLIENT_ID'] for client_ in res[2]) == client_list

    client = new_client(session)
    res = xmlrpcserver.GetEqualClients(client.id)
    check_response([client.id])
    eq_client = new_client(session)
    eq_client.make_equivalent(client)
    res = xmlrpcserver.GetEqualClients(client.id)
    check_response([client.id, eq_client.id])
    res = xmlrpcserver.GetEqualClients(eq_client.id)
    check_response([client.id, eq_client.id])


def test_get_equal_clients_errors(xmlrpcserver):
    non_existent_client_id = ob.get_big_number()
    res = xmlrpcserver.GetEqualClients(non_existent_client_id)
    assert (res[0], res[1]) == (CODE_CLIENT_NOT_FOUND,
                                'Client with ID {client_id} not found in DB'.format(client_id=non_existent_client_id))
