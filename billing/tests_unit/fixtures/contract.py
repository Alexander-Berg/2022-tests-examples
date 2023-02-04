# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import datetime
import pytest
import allure

from billing.contract_iface.cmeta import general
from balance import constants as cst
from tests import object_builder as ob

from brest.core.tests import utils as test_utils


def create_contract(
    ctype='GENERAL',
    firm_id=None,
    client=None,
    w_ui=False,
    ui_services=None,
    ui_agency=False,
    **kwargs
):
    session = test_utils.get_test_session()
    client = client or ob.ClientBuilder.construct(session)
    contract = ob.ContractBuilder.construct(
        session,
        ctype=ctype,
        client=client,
        firm=firm_id,
        **kwargs  # noqa: C815
    )

    if w_ui:
        q = """
            insert into mv_ui_contract(id, contract_eid, dt, commission, is_signed, services, contract_id, agency_id, client_id, firm)
            values(:id, :contract_eid, :dt, :commission, :is_signed, :services, :contract_id, :agency_id, :client_id, :firm)
            """
        params = dict(
            id=contract.id,
            contract_eid=contract.external_id,
            dt=contract.col0.dt,
            commission=contract.commission,
            is_signed=contract.col0.is_signed,
            services=ui_services or str(contract.col0.services),
            contract_id=contract.id,
            agency_id=contract.client_id if ui_agency else None,
            client_id=contract.client_id if not ui_agency else None,
            firm=firm_id,
        )
        session.execute(q, params)
    session.flush()

    return contract


@pytest.fixture(name='general_contract')
@allure.step('create contract')
def create_general_contract(
    firm_id=None,
    client=None,
    w_ui=False,
    ui_services=None,
    ui_agency=False,
    **kwargs
):
    return create_contract(
        'GENERAL',
        firm_id=firm_id,
        client=client,
        w_ui=w_ui,
        ui_services=ui_services,
        ui_agency=ui_agency,
        **kwargs
    )


@pytest.fixture(name='spendable_contract')
@allure.step('create contract')
def create_spendable_contract(
    firm_id=None,
    client=None,
    w_ui=False,
    ui_services=None,
    ui_agency=False,
    **kwargs
):
    return create_contract(
        'SPENDABLE',
        firm_id=firm_id,
        client=client,
        w_ui=w_ui,
        ui_services=ui_services,
        ui_agency=ui_agency,
        **kwargs
    )


@pytest.fixture(name='distribution_contract')
@allure.step('create contract')
def create_distribution_contract(
    firm_id=None,
    is_faxed=None,
    client=None,
    w_ui=False,
    doc_set=1,
    platform_type=None,
    **kwargs  # noqa: C816
):
    session = test_utils.get_test_session()
    client = client or ob.ClientBuilder.construct(session)
    contract = ob.ContractBuilder.construct(
        session,
        ctype='DISTRIBUTION',
        client=client,
        firm=firm_id,
        platform_type=platform_type,
        is_faxed=is_faxed,
        **kwargs  # noqa: C815
    )
    if w_ui:
        q = """
        insert into mv_ui_partner_contract(id, contract_class, contract_eid, dt, is_signed, is_faxed, services, contract_id, client_id, firm, payment_type, platform_type, doc_set)
        values(:id, :contract_class, :contract_eid, :dt, :is_signed, :is_faxed, :services, :contract_id, :client_id, :firm, :payment_type, :platform_type, :doc_set)
        """
        params = dict(
            contract_class='DISTRIBUTION',
            id=contract.id,
            contract_eid=contract.external_id,
            dt=contract.col0.dt,
            is_signed=contract.col0.is_signed,
            is_faxed=is_faxed,
            services=str(contract.col0.services),
            contract_id=contract.id,
            client_id=contract.client_id,
            firm=firm_id,
            payment_type=1,
            platform_type=platform_type,
            doc_set=doc_set,
        )
        session.execute(q, params)
    session.flush()
    return contract


@pytest.fixture(name='partner_contract')
def create_partner_contract(
    eid=None,
    client=None,
    is_faxed=None,
    firm_id=cst.FirmId.YANDEX_OOO,
    services=None,
    doc_set=1,
    w_ui=True,
    **kwargs  # noqa: C816
):
    session = test_utils.get_test_session()
    client = client or ob.ClientBuilder.construct(session)
    contract = ob.ContractBuilder.construct(
        session,
        external_id=eid,
        ctype='PARTNERS',
        client=client,
        firm=firm_id,
        is_faxed=is_faxed,
        services=services,
        **kwargs  # noqa: C815
    )
    if w_ui:
        q = """
        insert into mv_ui_partner_contract(id, contract_class, contract_eid, dt, is_signed, is_faxed, services, contract_id, client_id, firm, payment_type, doc_set)
        values(:id, :contract_class, :contract_eid, :dt, :is_signed, :is_faxed, :services, :contract_id, :client_id, :firm, :payment_type, :doc_set)
        """
        params = dict(
            contract_class='PARTNERS',
            id=contract.id,
            contract_eid=contract.external_id,
            dt=contract.col0.dt,
            is_signed=contract.col0.is_signed,
            is_faxed=is_faxed,
            services=str(contract.col0.services),
            contract_id=contract.id,
            client_id=contract.client_id,
            firm=firm_id,
            payment_type=1,
            doc_set=doc_set,
        )
        session.execute(q, params)
    session.flush()
    return contract


def create_credit_contract(client=None, person=None, person_type='ur', **kwargs):
    session = test_utils.get_test_session()

    client = client or ob.ClientBuilder.construct(session)
    person = person or ob.PersonBuilder.construct(session, client=client, type=person_type)

    params = dict(
        client=client,
        person=person,
        commission=cst.ContractTypeId.NON_AGENCY,
        firm=cst.FirmId.YANDEX_OOO,
        payment_type=cst.POSTPAY_PAYMENT_TYPE,
        personal_account=1,
        personal_account_fictive=1,
        services={cst.ServiceId.DIRECT},
        is_signed=datetime.datetime.now(),
        currency=cst.NUM_CODE_RUR,
    )
    params.update(kwargs)
    return ob.ContractBuilder.construct(session, **params)


def create_collateral(contract, **kwargs):
    session = test_utils.get_test_session()
    return ob.CollateralBuilder.construct(
        session,
        contract=contract,
        collateral_type=kwargs.pop('collateral_type', general.collateral_types[1003]),
        memo=kwargs.pop('memo', ''),
        **kwargs  # noqa: C815
    )
