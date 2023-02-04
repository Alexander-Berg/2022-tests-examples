import datetime
import pytest
from xmlrpclib import Fault
from balance import exc
from tests import object_builder as ob
from tests.tutils import get_exception_code

def test_success(test_xmlrpc_srv, session):
    els_start_dt = session.config.SINGLE_ACCOUNT_MIN_CLIENT_DT
    client = ob.ClientBuilder.construct(session, with_single_account=False,
                                        creation_dt=els_start_dt - datetime.timedelta(days=1))
    response = test_xmlrpc_srv.MigrateClientToEls({'ClientID': client.id})
    assert client.single_account_number
    assert response == client.single_account_number


def test_exception(test_xmlrpc_srv, session):
    client = ob.ClientBuilder.construct(session, with_single_account=False)
    for _ in range(2):
        ob.PersonBuilder.construct(session, client=client, type='ph')
    with pytest.raises(Fault) as exc_info:
        test_xmlrpc_srv.MigrateClientToEls({'ClientID': client.id})
    assert client.single_account_number is None
    assert get_exception_code(exc=exc_info.value) == 'INVALID_PARAM'
    assert get_exception_code(exc=exc_info.value,
                              tag_name='msg') == 'Invalid parameter for function: ' \
                                 'Client with single account can have only one individual person'
