# -*- coding: utf-8 -*-

import pytest
import hamcrest
from tests import object_builder as ob

from balance import mapper


def create_client(session, **kwargs):
    res = ob.ClientBuilder(**kwargs).build(session).obj
    return res


CODE_SUCCESS = 0
CODE_CLIENT_NOT_FOUND = 1003


@pytest.mark.parametrize('number_of_client', [1, 1001])
def test_get_client_by_id_batch(session, xmlrpcserver, number_of_client):
    q = session.query(mapper.Client).limit(number_of_client)
    cl_ids = []
    expected_result = []
    for c in q:
        cl_ids.append(c.id)
        expected_result.append(c.xmlrpc_hash())
    res = xmlrpcserver.GetClientByIdBatch(cl_ids)
    assert res[0] == CODE_SUCCESS
    assert sorted(res[1]) == sorted(expected_result)


def test_get_client_by_id_attrs(session, xmlrpcserver):
    client = create_client(session, city=u'Москва', fax='322', region_id=225, name=u'Имя_клиента')
    session.flush()
    res = xmlrpcserver.GetClientByIdBatch([client.id])
    assert res[0] == CODE_SUCCESS
    hamcrest.assert_that(res[1], hamcrest.equal_to([{'CITY': u'Москва',
                                                     'FAX': '322',
                                                     'REGION_ID': 225,
                                                     'NAME': u'Имя_клиента',
                                                     'URL': '',
                                                     'CLIENT_TYPE_ID': 0,
                                                     'IS_AGENCY': 0,
                                                     'PHONE': '',
                                                     'AGENCY_ID': 0,
                                                     'CLIENT_ID': client.id,
                                                     'SERVICE_DATA': {},
                                                     'EMAIL': 'test@test.ru',
                                                     'SINGLE_ACCOUNT_NUMBER': ''}]))
