# -*- coding: utf-8 -*-

from decimal import Decimal as D
import datetime
import mock
import pytest
import sqlalchemy as sa
from butils import decimal_unit

from balance import constants as const
from balance import contractpage, mapper
from balance import muzzle_util as ut
from tests import object_builder as ob


DU = decimal_unit.DecimalUnit
CONTRACT_DT = datetime.datetime(2022, 1, 1)


@pytest.mark.parametrize('tax_policy_params, expected_data', [
    pytest.param(
        None,
        {
            'YANDEX_SERVICE': {'paysys_id': 1301003, 'nds': 1, 'nds_pct': DU('20', '%')},
            'DEPOSITION': {'paysys_id': 1301006, 'nds': 1, 'nds_pct': DU('20', '%')},
            'AGENT_REWARD':{'paysys_id': 1301003, 'nds': 1, 'nds_pct': DU('20', '%')},
        },
        id='NO_TAX_POLICY_PARAMS'
    ),
    pytest.param(
        {
            'service_code': 'DEPOSITION',
            'firm_id': 13,
            'country': 225,
            'service_id': 124,
            'currency': 'RUB',
            'person_category': None,
            'tax_policy_mdh_id': '0ad5f71b-f374-4efb-8f4d-9e67d41ff86f'
        },
        {
            'YANDEX_SERVICE': {'paysys_id': 1301003, 'nds': 1, 'nds_pct': DU('20', '%')},
            'DEPOSITION': {'paysys_id': 1301006, 'nds': 1, 'nds_pct': DU('7.7', '%')},
            'AGENT_REWARD': {'paysys_id': 1301003, 'nds': 1, 'nds_pct': DU('20', '%')},
        },
        id='SUITABLE_TAX_POLICY_PARAMS',
    ),
    pytest.param(
        {
            'service_code': 'DEPOSITION',
            'firm_id': 13,
            'country': None,
            'service_id': 124,
            'currency': 'RUB',
            'person_category': 'ur',
            'tax_policy_mdh_id': '0ad5f71b-f374-4efb-8f4d-9e67d41ff86f'
        },
        {
            'YANDEX_SERVICE': {'paysys_id': 1301003, 'nds': 1, 'nds_pct': DU('20', '%')},
            'DEPOSITION': {'paysys_id': 1301006, 'nds': 1, 'nds_pct': DU('7.7', '%')},
            'AGENT_REWARD': {'paysys_id': 1301003, 'nds': 1, 'nds_pct': DU('20', '%')},
        },
        id='SUITABLE_TAX_POLICY_PARAMS_WIDE_COUNTRY',
    ),
    pytest.param(
        {
            'service_code': 'DEPOSITION',
            'firm_id': 13,
            'country': 225,
            'service_id': 124,
            'currency': 'RUB',
            'person_category': None,
            'tax_policy_mdh_id': '0ad5f71b-f374-4efb-8f4d-9e67d41ff86f'
        },
        {
            'YANDEX_SERVICE': {'paysys_id': 1301003, 'nds': 1, 'nds_pct': DU('20', '%')},
            'DEPOSITION': {'paysys_id': 1301006, 'nds': 1, 'nds_pct': DU('7.7', '%')},
            'AGENT_REWARD': {'paysys_id': 1301003, 'nds': 1, 'nds_pct': DU('20', '%')},
        },
        id='SUITABLE_TAX_POLICY_PARAMS_WIDE_CATEGORY',
    ),
    pytest.param(
        {
            'service_code': 'DEPOSITION',
            'firm_id': 13,
            'country': None,
            'service_id': 124,
            'currency': 'RUB',
            'person_category': None,
            'tax_policy_mdh_id': '0ad5f71b-f374-4efb-8f4d-9e67d41ff86f'
        },
        {
            'YANDEX_SERVICE': {'paysys_id': 1301003, 'nds': 1, 'nds_pct': DU('20', '%')},
            'DEPOSITION': {'paysys_id': 1301006, 'nds': 1, 'nds_pct': DU('7.7', '%')},
            'AGENT_REWARD': {'paysys_id': 1301003, 'nds': 1, 'nds_pct': DU('20', '%')},
        },
        id='SUITABLE_TAX_POLICY_PARAMS_WIDE_CATEGORY_COUNTRY',
    ),
    pytest.param(
        {
            'service_code': 'DEPOSITION',
            'firm_id': 13,
            'country': 225,
            'service_id': 125,
            'currency': 'RUB',
            'person_category': None,
            'tax_policy_mdh_id': '0ad5f71b-f374-4efb-8f4d-9e67d41ff86f'
        },
        {
            'YANDEX_SERVICE': {'paysys_id': 1301003, 'nds': 1, 'nds_pct': DU('20', '%')},
            'DEPOSITION': {'paysys_id': 1301006, 'nds': 1, 'nds_pct': DU('20', '%')},
            'AGENT_REWARD': {'paysys_id': 1301003, 'nds': 1, 'nds_pct': DU('20', '%')},
        },
        id='UNSUITABLE_TAX_POLICY_PARAMS',
    ),
    pytest.param(
        {
            'service_code': 'DEPOSITION',
            'firm_id': 13,
            'country': 226,
            'service_id': 124,
            'currency': 'RUB',
            'person_category': None,
            'tax_policy_mdh_id': '0ad5f71b-f374-4efb-8f4d-9e67d41ff86f'
        },
        {
            'YANDEX_SERVICE': {'paysys_id': 1301003, 'nds': 1, 'nds_pct': DU('20', '%')},
            'DEPOSITION': {'paysys_id': 1301006, 'nds': 1, 'nds_pct': DU('20', '%')},
            'AGENT_REWARD': {'paysys_id': 1301003, 'nds': 1, 'nds_pct': DU('20', '%')},
        },
        id='UNSUITABLE_TAX_POLICY_PARAMS_COUNTRY',
    ),
    pytest.param(
        {
            'service_code': 'DEPOSITION',
            'firm_id': 13,
            'country': 225,
            'service_id': 124,
            'currency': 'RUB',
            'person_category': 'ph',
            'tax_policy_mdh_id': '0ad5f71b-f374-4efb-8f4d-9e67d41ff86f'
        },
        {
            'YANDEX_SERVICE': {'paysys_id': 1301003, 'nds': 1, 'nds_pct': DU('20', '%')},
            'DEPOSITION': {'paysys_id': 1301006, 'nds': 1, 'nds_pct': DU('20', '%')},
            'AGENT_REWARD': {'paysys_id': 1301003, 'nds': 1, 'nds_pct': DU('20', '%')},
        },
        id='UNSUITABLE_TAX_POLICY_PARAMS_CATEGORY',
    ),


])
def test_custom_nds_pct(session, tax_policy_params, expected_data):
    from billing.contract_iface import contract_meta

    contract = mapper.Contract(ctype=contract_meta.ContractTypes(type="GENERAL"))
    session.add(contract)
    contract.client = ob.ClientBuilder().build(session).obj
    contract.person = (
        ob.PersonBuilder(client=contract.client, type="ur").build(session).obj
    )
    contract.col0.dt = CONTRACT_DT

    contract.col0.firm = 13
    contract.col0.country = 225
    contract.col0.manager_code = 1122
    contract.col0.commission = 0
    contract.col0.payment_type = const.PREPAY_PAYMENT_TYPE
    contract.col0.personal_account = 1
    contract.col0.currency = 810
    contract.external_id = contract.create_new_eid()
    contract.col0.is_signed = CONTRACT_DT

    contract.col0.services = {const.ServiceId.TAXI_CASH, const.ServiceId.TAXI_PAYMENT}

    if tax_policy_params:
        session.add(mapper.TaxPolicyForServiceCode(**tax_policy_params))
        session.flush()

    cp = contractpage.ContractPage(session, contract.id)
    cp.create_personal_accounts()
    session.flush()
    invoices = contract.invoices
    assert len(invoices) == 3, 'Count of created invoices mismatch. Expected 3, got {}'.format(len(invoices))
    for service_code in expected_data:
        invoice, = [i for i in invoices if i.service_code == service_code]
        for attr in expected_data[service_code]:
            assert getattr(invoice, attr) == expected_data[service_code][attr], \
                'Mismatch attr {} value for service_code {}: expected {}, got {}'.format(
                    attr, service_code, expected_data[service_code][attr], getattr(invoice, attr)
                )
