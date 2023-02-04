import xmlrpclib

import datetime
import pytest

from balance import constants as cst
from tests import object_builder as ob
from tests import tutils as tut

TWO_MONTH_BEFORE = datetime.datetime.now() - datetime.timedelta(days=60)


def test_accept_taxi_offer(session, medium_xmlrpc):
    contract = ob.ContractBuilder.construct(session, currency=810)
    contract.person.is_partner = 1
    main_contract = ob.ContractBuilder.construct(session,
                                 ctype='SPENDABLE',
                                 client=contract.client,
                                 contract_type=87,
                                 dt=TWO_MONTH_BEFORE,
                                 is_signed=TWO_MONTH_BEFORE,
                                 services={cst.ServiceId.TAXI_PROMO},
                                 firm=1,
                                 link_contract_id=contract.id
                                 )
    session.flush()
    with pytest.raises(xmlrpclib.Fault) as exc_info:
        medium_xmlrpc.AcceptTaxiOffer(session.oper_id, {'contract_id': contract.id,
                                                        'person_id': contract.person_id})
    assert tut.get_exception_code(exc_info.value, 'msg') == 'Invalid parameter for function: Client {} already has contract {} with link_contract_id={}'.format(contract.client.id, main_contract.id, contract.id)
