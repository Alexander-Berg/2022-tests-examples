import xmlrpclib

import datetime
import pytest

from balance import constants as cst
from tests import object_builder as ob
from tests import tutils as tut

TWO_MONTH_BEFORE = datetime.datetime.now() - datetime.timedelta(days=60)


def test_rr(session, medium_xmlrpc):
    client = ob.ClientBuilder.construct(session)
    with pytest.raises(xmlrpclib.Fault) as exc_info:
        medium_xmlrpc.CreatePersonValidate(session.oper_id, {'account': '40817810138051929363',
                                                             'bik': '044525225',
                                                             'client_id': client.id,
                                                             'type': 'ph',
                                                             'delivery_type': '4'}
                                       )
    assert 'ValidatorResult failed validation of ' in tut.get_exception_code(exc_info.value, 'msg')
