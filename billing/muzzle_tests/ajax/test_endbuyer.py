# -*- coding: utf-8 -*-
import pytest

from billing.contract_iface.constants import ContractTypeId

from balance.utils.xml2json import xml2json_auto
from balance.corba_buffers import StateBuffer, RequestBuffer
from balance import constants as cst

from muzzle.ajax import endbuyer

from tests import object_builder as ob


class TestGetEndbuyerPersons(object):
    @pytest.mark.parametrize(
        'inn_or_name, limit',
        [
            [None, endbuyer.ENDBUYERS_LIMIT],
            [u'Конечный покупатель', endbuyer.ENDBUYERS_LIMIT],
            [u'Конечный покупатель 1', 2]
        ]
    )
    def test_limit(self, session, inn_or_name, limit):
        client = ob.ClientBuilder(is_agency=True).build(session).obj
        contract = ob.ContractBuilder(commission=ContractTypeId.COMMISSION, client=client).build(session).obj

        for i in range(endbuyer.ENDBUYERS_LIMIT+1):
            name = u'Конечный покупатель {}'.format(i)
            ob.PersonBuilder(client=client, type='endbuyer_ph', name=name).build(session)

        endbuyers = endbuyer.get_endbuyer_persons(
            session,
            agency_id=client.id,
            request_id=None,
            invoice_id=None,
            inn_or_name=inn_or_name,
            contract_id=contract.id,
            state_obj=StateBuffer(params={'prot_method': 'GET'}),
            request_obj=RequestBuffer(params=([], [('X-Requested-With', 'XMLHttpRequest')], []))
        )
        endbuyers_json = xml2json_auto(endbuyers)
        persons = [i['id'] for i in endbuyers_json['person'] if i['id'][0]]

        assert len(persons) == limit
