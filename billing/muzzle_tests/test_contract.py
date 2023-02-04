# -*- coding: utf-8 -*-
import pytest
import mock
import re
import datetime
import hamcrest as hm
import xml.etree.ElementTree as et
from decimal import Decimal as D

from billing.contract_iface.cmeta import general

from balance import (
    core,
    constants as cst,
    exc,
    mapper,
)
from billing.contract_iface import ContractTypeId
from balance.corba_buffers import StateBuffer, RequestBuffer
from balance.utils.xml2json import xml2json_auto
from muzzle.ajax import contract as contract_ajax

from tests.tutils import mock_transactions, has_exact_entries
from tests import object_builder as ob
from tests.balance_tests.invoices.invoice_common import (
    create_credit_contract,
)


CONTRACT_FIRM_ID = cst.FirmId.YANDEX_OOO


def create_passport(session, roles, client=None, patch_session=True, **kwargs):
    passport = ob.create_passport(session, *roles, patch_session=patch_session, client=client, **kwargs)
    return passport


@pytest.fixture(name='client_role')
def get_client_role(session):
    return ob.Getter(mapper.Role, cst.RoleName.CLIENT).build(session).obj


@pytest.fixture(name='view_contract_role')
def create_view_contract_role(session):
    return ob.create_role(
        session,
        (
            cst.PermissionCode.VIEW_CONTRACTS,
            {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
        ),
    )


@pytest.fixture(name='view_invoice_role')
def create_view_invoice_role(session):
    return ob.create_role(
        session,
        (
            cst.PermissionCode.VIEW_INVOICES,
            {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
        ),
    )


@pytest.fixture(name='edit_contract_role')
def create_edit_contract_role(session):
    return ob.create_role(
        session,
        (
            cst.PermissionCode.EDIT_CONTRACTS,
            {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
        ),
    )


@pytest.fixture(name='deal_passport_role')
def create_deal_passport_role(session):
    return ob.create_role(
        session,
        (
            cst.PermissionCode.DEAL_PASSPORT,
            {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
        ),
    )


@pytest.fixture(name='print_tmpl_role')
def create_print_tmpl_role(session):
    return ob.create_role(
        session,
        (
            cst.PermissionCode.ALTER_PRINT_TEMPLATE,
            {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
        ),
    )


@pytest.fixture(name='role_client')
def create_role_client(session, client=None):
    return ob.RoleClientBuilder.construct(
        session,
        client=client or create_client(session),
    )


@pytest.fixture(name='firm')
def create_firm(session):
    return ob.FirmBuilder.construct(session)


@pytest.fixture(name='client')
def create_client(session):
    return ob.ClientBuilder.construct(session)


@pytest.fixture(name='contract')
def create_contract(
        session,
        client=None,
        firm_id=CONTRACT_FIRM_ID,
        services=None,
        new_comm=False,
        contract_type=None,
        w_ui=True,
        ui_agency=False,
        **kwargs
):
    client = client or ob.ClientBuilder.construct(session)
    contract = ob.ContractBuilder.construct(
        session,
        client=client,
        commission=contract_type,
        firm=firm_id,
        services=services,
        new_commissioner_report=new_comm,
        **kwargs
    )
    if w_ui:
        q = """insert into mv_ui_contract(id, contract_eid, dt, commission, is_signed, services, contract_id, client_id, firm)
        values(:id, :contract_eid, :dt, :commission, :is_signed, :services, :contract_id, :client_id, :firm)"""
        params = dict(
            id=contract.id,
            contract_eid=contract.external_id,
            dt=contract.col0.dt,
            commission=contract.commission,
            is_signed=contract.col0.is_signed,
            services=str(contract.col0.services),
            contract_id=contract.id,
            agency_id=contract.client_id if ui_agency else None,
            client_id=contract.client_id if not ui_agency else None,
            firm=firm_id,
        )
        session.execute(q, params)
    session.flush()
    return contract


@pytest.fixture(name='partner_contract')
def create_partner_contract(session, client=None, person=None, firm_id=CONTRACT_FIRM_ID, services=None, w_ui=True, ui_agency=False,):
    client = client or ob.ClientBuilder.construct(session)
    contract = ob.ContractBuilder.construct(
        session,
        ctype='PARTNERS',
        client=client,
        person=person,
        firm=firm_id,
        services=services,
    )
    if w_ui:
        q = """insert into mv_ui_partner_contract(id, contract_class, contract_eid, dt, is_signed, services, contract_id, client_id, firm)
        values(:id, :contract_class, :contract_eid, :dt, :is_signed, :services, :contract_id, :client_id, :firm)"""
        params = dict(
            contract_class='PARTNERS',
            id=contract.id,
            contract_eid=contract.external_id,
            dt=contract.col0.dt,
            is_signed=contract.col0.is_signed,
            services=str(contract.col0.services),
            contract_id=contract.id,
            client_id=contract.client_id,
            firm=firm_id,
        )
        session.execute(q, params)
    session.flush()
    return contract


@pytest.fixture(name='distribution_contract')
def create_distribution_contract(session, client=None, firm_id=CONTRACT_FIRM_ID, **params):
    client = client or ob.ClientBuilder.construct(session)
    return ob.ContractBuilder.construct(
        session,
        ctype='DISTRIBUTION',
        client=client,
        firm=firm_id,
        **params
    )


@pytest.mark.permissions
class TestGetContractPermissions(object):
    def test_nobody(self, session, client_role, muzzle_logic):
        ob.set_roles(session, session.passport, [client_role])
        contract = create_contract(session)
        with pytest.raises(exc.PERMISSION_DENIED):
            muzzle_logic.get_actual_contract(session, contract.id)

    def test_client_contract(self, session, muzzle_logic, client_role, contract):
        create_passport(session, [client_role], client=contract.client)
        res = muzzle_logic.get_actual_contract(session, contract.id)
        assert res.find('id').text == str(contract.id)

    def test_right_firm_constraint(self, session, muzzle_logic, view_contract_role, contract):
        create_passport(session, [(view_contract_role, CONTRACT_FIRM_ID)])
        res = muzzle_logic.get_actual_contract(session, contract.id)
        assert res.find('id').text == str(contract.id)

    def test_wrong_firm_constraint(self, session, muzzle_logic, view_contract_role):
        ob.set_roles(session, session.passport, [(view_contract_role, {cst.ConstraintTypes.firm_id: cst.FirmId.CLOUD})])
        contract = create_contract(session)
        with pytest.raises(exc.PERMISSION_DENIED):
            muzzle_logic.get_actual_contract(session, contract.id)

    def test_right_client_constraint(self, session, muzzle_logic, view_contract_role, contract):
        role_client = create_role_client(session, client=contract.client)
        create_passport(session, [(view_contract_role, None, role_client.client_batch_id)])
        res = muzzle_logic.get_actual_contract(session, contract.id)
        assert res.find('id').text == str(contract.id)

    def test_wrong_client_constraint(self, session, muzzle_logic, view_contract_role, contract, role_client):
        create_passport(session, [(view_contract_role, None, role_client.client_batch_id)])
        with pytest.raises(exc.PERMISSION_DENIED):
            muzzle_logic.get_actual_contract(session, contract.id)

    def test_right_client_firm_constraint(self, session, muzzle_logic, view_contract_role, contract):
        role_client = create_role_client(session, client=contract.client)
        create_passport(session, [(view_contract_role, CONTRACT_FIRM_ID, role_client.client_batch_id)])
        res = muzzle_logic.get_actual_contract(session, contract.id)
        assert res.find('id').text == str(contract.id)

    def test_wrong_client_firm_constraint(self, session, muzzle_logic, view_contract_role, contract):
        role_client = create_role_client(session, client=contract.client)
        create_passport(
            session,
            [
                (view_contract_role, CONTRACT_FIRM_ID, create_role_client(session).client_batch_id),
                (view_contract_role, cst.FirmId.AUTORU, role_client.client_batch_id),
            ],
        )
        with pytest.raises(exc.PERMISSION_DENIED):
            muzzle_logic.get_actual_contract(session, contract.id)

    @pytest.mark.slow
    def test_contract_attributes(self, session, muzzle_logic):
        client = ob.ClientBuilder.construct(session)
        person = ob.PersonBuilder.construct(session, client=client)
        contract = create_contract(
            session,
            client=client,
            person=person,
            postpay=0,
            firm_id=cst.FirmId.YANDEX_EU_AG,  # доступны electronic_reports
            currency=978,  # 'EUR'
            services={cst.ServiceId.DIRECT: 1},
        )
        finish_dt = session.now() + datetime.timedelta(days=7)
        collaterals = [
            ob.CollateralBuilder.construct(session, contract=contract, num='02', collateral_type=general.collateral_types[1003], dt=session.now(), is_signed=session.now(), memo=u'Test memооо'),
            ob.CollateralBuilder.construct(session, contract=contract, num='03', collateral_type=general.collateral_types[80], dt=session.now(), finish_dt=finish_dt, is_signed=session.now()),
        ]
        periods = [
            ob.CRPaymentReportBuilder.construct(session, contract=contract),
            ob.CRPaymentReportBuilder.construct(session, contract=contract),
        ]
        invoice = ob.InvoiceBuilder.construct(
            session,
            person=contract.person,
            request=ob.RequestBuilder(
                basket=ob.BasketBuilder(
                    client=contract.client,
                    rows=[
                        ob.BasketItemBuilder(order=ob.OrderBuilder(service_id=cst.ServiceId.DIRECT, client=contract.client), quantity=D('10'))
                    ],
                ),
                firm_id=cst.FirmId.YANDEX_EU_AG,
            ),
            contract=contract,
            postpay=0,
        )
        invoice.manual_turn_on(D('1000'))
        order = invoice.consumes[0].order
        order.calculate_consumption(
            dt=datetime.datetime.today() - datetime.timedelta(days=1),
            stop=0,
            shipment_info={'Bucks': D('10')},
        )
        invoice.generate_act(force=True)
        session.flush()

        res = muzzle_logic.get_actual_contract(session, contract.id, show_collaterals=1)
        assert res.findtext('show-collaterals') == '1'
        assert res.findtext('collaterals-count') == '2'

        assert res.findtext('id') == str(contract.id)
        assert res.findtext('external-id') == str(contract.external_id)
        assert res.findtext('type') == 'GENERAL'
        assert res.findtext('oebs-exportable') == '1'
        assert res.find('client').findtext('id') == str(client.id)
        assert res.find('person').findtext('id') == str(person.id)

        res_periods = [xml2json_auto(col) for col in res.find('periods').getchildren()]
        hm.assert_that(
            res_periods,
            hm.contains(*[
                hm.has_entries({
                    'id': str(p.id),
                    'avans-amount': str(p.avans_amount),
                    'pretty-dt': hm.not_none(),
                    'nach-amount': '1.23',
                    'db-status': '0',
                    'perech-amount': '6.66',
                    'factura-id': None,
                    'start-dt': hm.not_none(),
                    'end-dt': hm.not_none(),
                })
                for p in periods
            ])
        )

        hm.assert_that(
            res.find('electronic-reports').getchildren(),
            hm.contains(
                hm.has_properties({
                    'text': hm.not_none(),
                    'attrib': hm.has_key('dt'),
                }),
            ),
        )

        attr_groups = res.find('attributes').getchildren()
        assert len(attr_groups) == 3
        grp1, grp2, grp3 = attr_groups
        hm.assert_that(
            grp1.getchildren(),
            hm.contains(
                hm.has_properties({'text': u'Yandex Europe AG', 'attrib': hm.has_entries({'name': u'firm'})}),
                hm.has_properties({'text': u'EUR', 'attrib': hm.has_entries({'name': u'currency'})}),
                hm.has_properties({'text': str(contract.col0.manager_code), 'attrib': hm.has_entries({'name': u'manager_code'})}),
                hm.has_properties({'text': hm.not_none(), 'attrib': hm.has_entries({'name': u'dt'})}),
                hm.has_properties({'text': hm.not_none(), 'attrib': hm.has_entries({'name': u'finish_dt'})}),
                hm.has_properties({'text': u'постоплата', 'attrib': hm.has_entries({'name': u'payment_type'})}),
                hm.has_properties({'text': u'Директ: Рекламные кампании', 'attrib': hm.has_entries({'name': u'services'})}),
            ),
        )
        hm.assert_that(
            grp2.getchildren(),
            hm.empty(),
        )
        hm.assert_that(
            grp3.getchildren(),
            hm.contains(
                hm.has_properties({'text': hm.not_none(), 'attrib': hm.has_entries({'name': u'is_signed'})}),
            ),
        )

        res_collaterals = [xml2json_auto(col) for col in res.find('collaterals').getchildren()]
        collaterals_match = [
            hm.has_entries({'id': str(col.id), 'num': col.num, 'dt': hm.not_none(), 'memo': col.get('memo', ''), 'collateral-pn': str(idx + 1), 'is-signed': hm.not_none()})
            for idx, col in enumerate(collaterals[::-1])
        ]
        hm.assert_that(
            res_collaterals,
            hm.contains(*collaterals_match),
        )

    @pytest.mark.parametrize(
        'all_collaterals',
        [0, 1],
    )
    def test_w_collaterals(self, session, muzzle_logic, contract, all_collaterals):
        finish_dt = session.now() + datetime.timedelta(days=1)
        _collaterals = [
            ob.CollateralBuilder.construct(session, contract=contract, num='04', collateral_type=general.collateral_types[80], dt=session.now(), finish_dt=finish_dt, is_cancelled=session.now(), memo='Aaaa'),
            ob.CollateralBuilder.construct(session, contract=contract, num='01', collateral_type=general.collateral_types[1001], dt=session.now(), services={11: True}, is_booked=1, is_booked_dt=session.now()),
            ob.CollateralBuilder.construct(session, contract=contract, num='02', collateral_type=general.collateral_types[1003], dt=session.now(), is_signed=session.now(), memo=u'Test memооо'),
            ob.CollateralBuilder.construct(session, contract=contract, num='03', collateral_type=general.collateral_types[80], dt=session.now(), finish_dt=finish_dt, is_signed=session.now(), is_cancelled=session.now()),
        ]
        session.flush()
        res = muzzle_logic.get_actual_contract(session, contract.id, show_collaterals=1, all_collaterals=all_collaterals)
        assert res.findtext('show-collaterals') == '1'
        assert res.findtext('all-collaterals') == str(all_collaterals)
        assert res.findtext('collaterals-count') == '4' if all_collaterals else '3'
        res_collaterals = [xml2json_auto(col) for col in res.find('collaterals').getchildren()]
        collaterals_match = [
            hm.has_entries({'id': hm.not_none(), 'num': '03', 'dt': hm.not_none(), 'collateral-type': u'продление договора','finish-dt': hm.not_none(), 'memo': u'', 'collateral-pn': '1', 'is-cancelled': hm.not_none(), 'is-signed': hm.not_none()}),
            hm.has_entries({'id': hm.not_none(), 'num': '02', 'dt': hm.not_none(), 'collateral-type': u'прочее', 'finish-dt': hm.not_none(), 'memo': u'Test memооо', 'collateral-pn': '2', 'is-signed': hm.not_none(), 'is-cancelled': ''}),
            hm.has_entries({'id': hm.not_none(), 'num': '01', 'dt': hm.not_none(), 'collateral-type': u'изменение сервисов', 'finish-dt': hm.not_none(), 'memo': u'', 'collateral-pn': '3', 'is-booked': hm.not_none(), 'is-signed': ''}),
            hm.has_entries({'id': hm.not_none(), 'num': '04', 'dt': hm.not_none(), 'collateral-type': u'продление договора', 'finish-dt': hm.not_none(), 'memo': u'Aaaa', 'collateral-pn': '4', 'is-cancelled': hm.not_none(), 'is-signed': ''}),
        ]
        if not all_collaterals:
            del collaterals_match[-1]
        hm.assert_that(
            res_collaterals,
            hm.contains(*collaterals_match),
        )


@pytest.mark.permissions
class TestGetDistributionLinkedContracts(object):
    def test_linked_contract(self, session, muzzle_logic):
        contract = create_distribution_contract(session, contract_type=cst.DistributionContractType.GROUP_OFFER, external_id='unique 123')
        linked = create_distribution_contract(session, parent_contract_id=contract.id, external_id='1', contract_type=cst.DistributionContractType.UNIVERSAL, supplements={1: 1, 3: 1})
        session.flush()

        res = muzzle_logic.get_distribution_linked_contracts(session, contract.id)
        res_linked, = res.getchildren()
        assert res_linked.findtext('id') == str(linked.id)
        assert res_linked.findtext('external-id') == '1'

        attributes = res_linked.findall('attribute')
        attributes = [{'attrib': attr.attrib, 'text': attr.text, 'value': attr.find('value')} for attr in attributes]
        hm.assert_that(
            attributes,
            hm.contains(
                hm.has_entries({
                    'attrib': hm.has_entries({'name': 'contract_type', 'type': 'refselect', 'caption': u'Тип договора', 'highlighted': 1}),
                    'text': u'Универсальный',
                    'value': None,
                }),
                hm.has_entries({
                    'attrib': hm.has_entries({'name': 'supplements', 'type': 'checkboxes', 'caption': u'Приложения договора', 'highlighted': 0}),
                    'text': u'Разделение доходов, Поиски',
                    'value': None,
                }),
            ),
        )

    def test_right_permissions(self, session, muzzle_logic, view_contract_role):
        """У пользователя есть роль ViewContracts с firm_id_1 и firm_id_2.
        Договор с firm_id_1 и client_1 => его можно просматривать.
        Можно посмотреть только те связанные договора, которые удовлетворяют выданным правам.
        """
        role_clients = [create_role_client(session) for _i in range(4)]
        firm_ids = [create_firm(session).id for _i in range(4)]

        create_passport(
            session,
            [
                (view_contract_role, firm_ids[0], role_clients[0].client_batch_id),
                (view_contract_role, firm_ids[1], None),
                (view_contract_role, None, role_clients[1].client_batch_id),
                (view_contract_role, firm_ids[2], role_clients[2].client_batch_id),
            ],
        )

        contract = create_distribution_contract(session, firm_id=firm_ids[0], client=role_clients[0].client, contract_type=7)

        requred_linked_contracts = [
            create_distribution_contract(session, firm_id=firm_ids[1], client=role_clients[3].client, parent_contract_id=contract.id),
            create_distribution_contract(session, firm_id=firm_ids[3], client=role_clients[1].client, parent_contract_id=contract.id),
            create_distribution_contract(session, firm_id=firm_ids[2], client=role_clients[2].client, parent_contract_id=contract.id),
            create_distribution_contract(session, firm_id=None, client=role_clients[2].client, parent_contract_id=contract.id),
            create_distribution_contract(session, firm_id=None, client=role_clients[3].client, parent_contract_id=contract.id),
        ]
        create_distribution_contract(session, firm_id=firm_ids[2], client=role_clients[0].client, parent_contract_id=contract.id)
        create_distribution_contract(session, firm_id=firm_ids[0], client=role_clients[2].client, parent_contract_id=contract.id)
        create_distribution_contract(session, firm_id=firm_ids[3], client=role_clients[3].client, parent_contract_id=contract.id)

        session.flush()

        res = muzzle_logic.get_distribution_linked_contracts(session, contract.id)
        linked_contracts = xml2json_auto(res).get('linked-contract', [])
        assert len(linked_contracts) == len(requred_linked_contracts)
        hm.assert_that(
            linked_contracts,
            hm.contains_inanyorder(*[
                hm.has_entry('id', str(linked_contract.id))
                for linked_contract in requred_linked_contracts
            ]),
        )

    def test_wrong_permission(self, session, muzzle_logic, view_contract_role):
        ob.set_roles(session, session.passport, [(view_contract_role, {cst.ConstraintTypes.firm_id: cst.FirmId.CLOUD})])
        contract = create_contract(session)
        with pytest.raises(exc.PERMISSION_DENIED):
            muzzle_logic.get_distribution_linked_contracts(session, contract.id)


@pytest.mark.permissions
@pytest.mark.parametrize(
    'method_name, create_func',
    [
        pytest.param('get_contracts', create_contract, id='general'),
        pytest.param('get_partner_contracts', create_partner_contract, id='partner'),
    ],
)
class TestGetContracts(object):
    @staticmethod
    def set_passport_roles(session, roles_w_constraints):
        roles = []
        for role, role_firm_ids in roles_w_constraints:
            if role_firm_ids is cst.SENTINEL:
                continue
            elif not role_firm_ids:
                roles.append(role)
            else:
                roles.extend([
                    (role, {cst.ConstraintTypes.firm_id: firm_id})
                    for firm_id in role_firm_ids
                ])
        ob.set_roles(session, session.passport, roles)

    @pytest.mark.parametrize(
        'contract_firm_ids, role_firm_ids',
        (
            pytest.param([None], cst.SENTINEL,
                        id='wo firm - wo role'),
            pytest.param([None], [],
                        id='wo firm - w role'),
            pytest.param([None], [cst.FirmId.YANDEX_OOO],
                         id='wo firm - w role w constraints'),
            pytest.param([cst.FirmId.YANDEX_OOO], [],
                        id='contracts w firm - w role wo constraints'),
            pytest.param(
                [cst.FirmId.YANDEX_OOO, cst.FirmId.YANDEX_OOO, cst.FirmId.DRIVE, cst.FirmId.CLOUD, None],
                [cst.FirmId.YANDEX_OOO, cst.FirmId.DRIVE, cst.FirmId.BUS],
                id='contracts w firms - w role w constraints',
            ),
        ),
    )
    def test_filtering_by_user_constraints_firm_id(self, session, muzzle_logic, view_contract_role,
                                                   method_name, create_func, contract_firm_ids, role_firm_ids):
        """Фильтрация по праву ViewContracts и фирме"""
        self.set_passport_roles(session, [(view_contract_role, role_firm_ids)])
        service = ob.ServiceBuilder.construct(session)
        required_contracts = []
        for firm_id in contract_firm_ids:
            contract = create_func(session, services=[service.id], firm_id=firm_id, w_ui=True)
            if (
                    role_firm_ids is not cst.SENTINEL
                    and
                    (firm_id is None or not role_firm_ids or firm_id in role_firm_ids)
            ):
                required_contracts.append(contract)

        state_obj = StateBuffer(
            params={
                'req_service_id': str(service.id),
            }
        )
        args = [session, state_obj]
        if method_name == 'get_partner_contracts':
            args.extend([{}, 'xml', None])
        res = getattr(muzzle_logic, method_name)(*args)
        assert res.find('total_row_count').text == str(len(required_contracts))
        res_ids = [item.find('contract_id').text for item in res.findall('entry')]
        hm.assert_that(
            res_ids,
            hm.contains_inanyorder(*[str(c.id) for c in required_contracts]),
        )

    @pytest.mark.parametrize(
        'match_client, firm_id, ans',
        (
            pytest.param(True, CONTRACT_FIRM_ID, True, id='w client w firm'),
            pytest.param(True, None, True, id='w client wo firm'),
            pytest.param(False, CONTRACT_FIRM_ID, False, id='wo client w firm'),
            pytest.param(True, cst.FirmId.DRIVE, False, id='w client w wrong firm'),
        ),
    )
    def test_filtering_by_user_constraints_client(
            self,
            session,
            muzzle_logic,
            view_contract_role,
            method_name,
            create_func,
            match_client,
            firm_id,
            ans,
    ):
        """Фильтрация по праву ViewContracts и клиенту"""
        service = ob.ServiceBuilder.construct(session)
        role_client = create_role_client(session)

        ob.set_roles(
            session,
            session.passport,
            [
                (view_contract_role, {
                    cst.ConstraintTypes.firm_id: CONTRACT_FIRM_ID,
                    cst.ConstraintTypes.client_batch_id: role_client.client_batch_id,
                }),
            ],
        )
        contract = create_func(session, services=[service.id], firm_id=firm_id, w_ui=True, client=role_client.client if match_client else None)

        state_obj = StateBuffer(
            params={
                'req_service_id': str(service.id),
            }
        )
        args = [session, state_obj]
        if method_name == 'get_partner_contracts':
            args.extend([{}, 'xml', None])
        res = getattr(muzzle_logic, method_name)(*args)
        res_ids = [item.find('contract_id').text for item in res.findall('entry')]
        assert [str(contract.id)] if ans else [] == res_ids

    @pytest.mark.parametrize(
        'match_client',
        [True, False],
    )
    def test_perm_for_agency(
            self,
            session,
            muzzle_logic,
            view_contract_role,
            method_name,
            create_func,
            match_client,
            client,
    ):
        """По agency_id тоже фильтруем"""
        service = ob.ServiceBuilder.construct(session)
        role_client = create_role_client(session, client=client if match_client else create_client(session))
        ob.set_roles(
            session,
            session.passport,
            [
                (view_contract_role, {
                    cst.ConstraintTypes.client_batch_id: role_client.client_batch_id,
                }),
            ],
        )

        contract = create_func(session, services=[service.id], client=client, w_ui=True, ui_agency=True)
        state_obj = StateBuffer(
            params={
                'req_service_id': str(service.id),
            }
        )
        args = [session, state_obj]
        if method_name == 'get_partner_contracts':
            args.extend([{}, 'xml', None])
        res = getattr(muzzle_logic, method_name)(*args)

        res_ids = [item.find('contract_id').text for item in res.findall('entry')]
        assert [str(contract.id)] if match_client else [] == res_ids


@pytest.mark.permissions
class TestGetContract2GETPermissions(object):
    """Тесты get"""

    def test_nobody(self, session, client_role, muzzle_logic):
        ob.set_roles(session, session.passport, [client_role])
        contract = create_contract(session)
        with mock_transactions():
            with pytest.raises(exc.PERMISSION_DENIED):
                muzzle_logic.get_contract2(session, contract.id, passport_id=session.passport.passport_id)

    def test_client(self, session, muzzle_logic, client_role, contract):
        """Доступ для клиента заказа"""
        create_passport(session, [client_role], client=contract.client)
        res = muzzle_logic.get_contract2(session, contract.id, passport_id=session.passport.passport_id)
        res_json = xml2json_auto(res)
        assert res_json['xf:model']['xf:instance']['data']['id'][0] == str(contract.id)

    @pytest.mark.parametrize(
        'contract_firm_id, role_firm_ids, ans',
        (
            pytest.param(None, cst.SENTINEL, False,
                        id='wo firm - wo role'),
            pytest.param(None, [CONTRACT_FIRM_ID], True,
                         id='wo firm - w role w constraints'),
            pytest.param(CONTRACT_FIRM_ID, [], True,
                        id='contracts w firm - w role wo constraints'),
            pytest.param(CONTRACT_FIRM_ID, [CONTRACT_FIRM_ID], True,
                         id='contracts firm match role firm'),
            pytest.param(CONTRACT_FIRM_ID, [cst.FirmId.CLOUD], False,
                         id='contracts firm does\'n match role firm'),
        ),
    )
    def test_firm_constraint(self, session, muzzle_logic, view_contract_role, contract_firm_id, role_firm_ids, ans):
        """Доступ по праву ViewContracts и фирме"""
        if role_firm_ids is cst.SENTINEL:
            roles = []
        elif not role_firm_ids:
            roles = [view_contract_role]
        else:
            roles = [
                (view_contract_role, {cst.ConstraintTypes.firm_id: firm_id})
                for firm_id in role_firm_ids
            ]
        ob.set_roles(session, session.passport, roles)

        contract = create_contract(session, firm_id=contract_firm_id)
        passport_id = session.passport.passport_id
        if ans:
            res = muzzle_logic.get_contract2(session, contract.id, passport_id=session.passport.passport_id)
            res_json = xml2json_auto(res)
            assert res_json['xf:model']['xf:instance']['data']['id'][0] == str(contract.id)
        else:
            with pytest.raises(exc.PERMISSION_DENIED) as exc_info:
                muzzle_logic.get_contract2(session, contract.id, passport_id=session.passport.passport_id)
            error_msg = 'User %s has no permission %s.' % (passport_id, cst.PermissionCode.VIEW_CONTRACTS)
            assert exc_info.value.msg == error_msg

    @pytest.mark.parametrize(
        'match_client, firm_id, ans',
        (
            pytest.param(True, CONTRACT_FIRM_ID, True, id='w client w firm'),
            pytest.param(True, None, True, id='w client wo firm'),
            pytest.param(False, CONTRACT_FIRM_ID, False, id='wo client w firm'),
            pytest.param(True, cst.FirmId.DRIVE, False, id='w client w wrong firm'),
        ),
    )
    def test_client_constraint(
            self,
            session,
            muzzle_logic,
            view_contract_role,
            match_client,
            firm_id,
            ans,
    ):
        """Доступ по праву ViewContracts, firm и client"""
        role_client = create_role_client(session)
        ob.set_roles(
            session,
            session.passport,
            [
                (view_contract_role, {
                    cst.ConstraintTypes.firm_id: CONTRACT_FIRM_ID,
                    cst.ConstraintTypes.client_batch_id: role_client.client_batch_id,
                }),
            ],
        )
        contract = create_contract(session, firm_id=firm_id, client=role_client.client if match_client else None)

        passport_id = session.passport.passport_id
        if ans:
            res = muzzle_logic.get_contract2(session, contract.id, passport_id=passport_id)
            res_json = xml2json_auto(res)
            assert res_json['xf:model']['xf:instance']['data']['id'][0] == str(contract.id)
        else:
            with pytest.raises(exc.PERMISSION_DENIED) as exc_info:
                muzzle_logic.get_contract2(session, contract.id, passport_id=passport_id)
            error_msg = 'User %s has no permission %s.' % (passport_id, cst.PermissionCode.VIEW_CONTRACTS)
            assert exc_info.value.msg == error_msg

    @pytest.mark.parametrize(
        'role_firm_ids, ans',
        [
            pytest.param(cst.SENTINEL, False, id='wo role'),
            pytest.param([], True, id='wo constraints'),
            pytest.param([cst.FirmId.CLOUD], True, id='w constraints'),
        ],
    )
    def test_empty_form(self, session, muzzle_logic, edit_contract_role, role_firm_ids, ans):
        """Получить пустую форму"""
        if role_firm_ids is cst.SENTINEL:
            roles = []
        elif not role_firm_ids:
            roles = [edit_contract_role]
        else:
            roles = [
                (edit_contract_role, {cst.ConstraintTypes.firm_id: firm_id})
                for firm_id in role_firm_ids
            ]
        ob.set_roles(session, session.passport, roles)

        if ans:
            _res = muzzle_logic.get_contract2(session, 0, passport_id=session.passport.passport_id)
        else:
            with pytest.raises(exc.PERMISSION_DENIED) as exc_info:
                muzzle_logic.get_contract2(session, 0, passport_id=session.passport.passport_id)
            assert exc_info.value.msg.endswith('has no permission %s.' % cst.PermissionCode.EDIT_CONTRACTS)


@pytest.mark.permissions
class TestGetContract2POSTPermissions(object):
    """Тесты post"""

    @pytest.mark.parametrize(
        'contract_firm_id, role_firm_ids, ans',
        (
            pytest.param(None, cst.SENTINEL, False,
                        id='wo firm - wo role'),
            pytest.param(CONTRACT_FIRM_ID, [], True,
                        id='contracts w firm - w role wo constraints'),
            pytest.param(CONTRACT_FIRM_ID, [CONTRACT_FIRM_ID], True,
                         id='contracts firm match role firm'),
            pytest.param(CONTRACT_FIRM_ID, [cst.FirmId.CLOUD], False,
                         id='contracts firm does\'n match role firm'),
        ),
    )
    def test_get_existing_contract_w_firm_constraint(
            self,
            session,
            muzzle_logic,
            edit_contract_role,
            contract_firm_id,
            role_firm_ids,
            ans,
    ):
        if role_firm_ids is cst.SENTINEL:
            roles = []
        elif not role_firm_ids:
            roles = [edit_contract_role]
        else:
            roles = [
                (edit_contract_role, {cst.ConstraintTypes.firm_id: firm_id})
                for firm_id in role_firm_ids
            ]
        ob.set_roles(session, session.passport, roles)

        contract = create_contract(session, firm_id=contract_firm_id)
        contract.col0.is_signed = session.now()
        session.flush()

        state_obj = StateBuffer(
            params={
                'req_id': str(contract.id),
                'req_client-id': str(contract.client_id),
            },
        )
        if ans:
            res = muzzle_logic.get_contract2(session, contract.id, passport_id=session.passport.passport_id, state_obj=state_obj)
            hm.assert_that(res.attrib,  hm.has_entries({'contract-id': str(contract.id)}))
        else:
            with pytest.raises(exc.PERMISSION_DENIED) as exc_info, mock_transactions():
                muzzle_logic.get_contract2(session, contract.id, passport_id=session.passport.passport_id, state_obj=state_obj)
            assert exc_info.value.msg == u'User 0 has no permission SomePermission.'

    @pytest.mark.parametrize(
        'role_func',
        [
            pytest.param(create_edit_contract_role, id=cst.PermissionCode.EDIT_CONTRACTS),
            pytest.param(create_deal_passport_role, id=cst.PermissionCode.DEAL_PASSPORT),
        ],
    )
    @pytest.mark.parametrize(
        'match_client, firm_id, ans',
        (
            pytest.param(True, CONTRACT_FIRM_ID, True, id='w client w firm'),
            pytest.param(True, None, True, id='w client wo firm'),
            pytest.param(False, CONTRACT_FIRM_ID, False, id='wo client w firm'),
            pytest.param(True, cst.FirmId.DRIVE, False, id='w client w wrong firm'),
        ),
    )
    def test_get_existing_contract_w_client_constraint(
            self,
            session,
            muzzle_logic,
            role_func,
            match_client,
            firm_id,
            ans,
    ):
        role_client = create_role_client(session)
        ob.set_roles(
            session,
            session.passport,
            [
                (role_func(session), {
                    cst.ConstraintTypes.firm_id: CONTRACT_FIRM_ID,
                    cst.ConstraintTypes.client_batch_id: role_client.client_batch_id,
                }),
            ],
        )

        contract = create_contract(session, firm_id=firm_id, client=role_client.client if match_client else None)
        contract.col0.is_signed = session.now()
        session.flush()

        state_obj = StateBuffer(
            params={'req_id': str(contract.id), 'req_client-id': str(contract.client_id)}
        )
        if ans:
            res = muzzle_logic.get_contract2(session, contract.id, passport_id=session.passport.passport_id, state_obj=state_obj)
            hm.assert_that(res.attrib, hm.has_entries({'contract-id': str(contract.id)}))
        else:
            with pytest.raises(exc.PERMISSION_DENIED) as exc_info, mock_transactions():
                muzzle_logic.get_contract2(session, contract.id, passport_id=session.passport.passport_id, state_obj=state_obj)
            assert exc_info.value.msg == u'User 0 has no permission SomePermission.'

    def test_get_existing_contract_wo_firm(self, session, muzzle_logic, edit_contract_role):
        """Для договора без фирмы фирма в роли не влияет"""
        contract = create_contract(
            session,
            firm_id=None,
            contract_type=ContractTypeId.ADVERTISING_BRAND,
            brand_type=cst.ClientLinkType.DIRECT,
        )
        ob.set_roles(session, session.passport, [(edit_contract_role, {cst.ConstraintTypes.firm_id: cst.FirmId.DRIVE})])
        state_obj = StateBuffer(
            params={'req_id': str(contract.id), 'req_client-id': str(contract.client_id)}
        )
        res = muzzle_logic.get_contract2(session, contract.id, passport_id=session.passport.passport_id, state_obj=state_obj)
        hm.assert_that(res.attrib, hm.has_entries({'contract-id': str(contract.id)}))


@pytest.mark.now
@pytest.mark.slow
class TestGetContract2(object):
    def _get_data(self, res_xml):
        data_path = ['xf:model', 'xf:instance', 'xf:instance', 'data']
        data = res_xml
        for tag in data_path:
            data = next(data.iter(tag))
        return data

    def _get_form_col_ids(self, data):
        return sorted(list(set(re.findall(r'col-(\d+)-num', et.tostring(data)))), reverse=True)

    def test_get_contract_w_pagination(self, session, muzzle_logic, contract):
        finish_dt = session.now() + datetime.timedelta(days=1)

        collaterals = []
        for i in range(1, 6):
            collaterals.append(
                ob.CollateralBuilder.construct(session, contract=contract, num='0%s' % i,
                                               collateral_type=general.collateral_types[80], dt=session.now(),
                                               finish_dt=finish_dt, is_signed=session.now(), memo='Sunny day #%s' % i),
            )
        session.flush()

        res = muzzle_logic.get_contract2(session, contract.id, collateral_pn=2, collateral_ps=2)

        pagination_params = xml2json_auto(res.find('collateral-pagination'))
        hm.assert_that(
            pagination_params,
            hm.has_entries({
                'pn': '2',
                'ps': '2',
                'total-count': '5',
                'total-pages': '3',
            }),
        )

        data = self._get_data(res)

        # проверяем нужный ли договор
        assert data.findtext('external-id') == contract.external_id
        assert data.findtext('id') == str(contract.id)

        # проверяем, что есть форма для создания нового ДС
        assert data.findtext('col-new-num') == '06'

        # проверяем, что есть только нужные допники
        match_cols = collaterals[::-1][2:4]
        res_col_ids = self._get_form_col_ids(data)
        hm.assert_that(res_col_ids, hm.contains(*[str(c.id) for c in match_cols]))
        for col_id, idx in zip(res_col_ids, [3, 2]):
            assert data.findtext('col-%s-num' % col_id) == '0%s' % idx
            assert data.findtext('col-%s-memo' % col_id) == 'Sunny day #%s' % idx

    @pytest.mark.parametrize(
        'pn',
        [1, 2],
    )
    def test_pagination_w_col0(self, session, muzzle_logic, contract, pn):
        """Проверяем, что вне зависимости от пагинации col0 будет подтягивать именно актуальные данные"""
        col_type = general.collateral_types[1001]  # изменение сервиса
        idx = pn - 1
        services = [7, 11]
        nums = ['01', '02']
        collaterals = [
            ob.CollateralBuilder.construct(session, contract=contract, num=num, collateral_type=col_type, dt=session.now(), services={s: True}, is_signed=session.now())
            for num, s in zip(nums, services)
        ]
        session.flush()

        res = muzzle_logic.get_contract2(session, contract.id, collateral_pn=pn, collateral_ps=1)
        data = self._get_data(res)
        col = collaterals[::-1][idx]

        assert data.findtext('id') == str(contract.id)
        assert data.find('services/item').attrib['key'] == '11'  # всегда в col0 значения последнего ДС

        assert data.findtext('col-%s-collateral-type' % col.id) == '1001'
        assert data.findtext('col-%s-num' % col.id) == nums[::-1][idx]

        col_services = data.findall('col-%s-group02-grp-1001-services/item' % col.id)
        checked_services = [i for i in col_services if i.text == '1']
        hm.assert_that(
            checked_services,
            hm.contains(
                hm.has_property('attrib', hm.has_entry('key', str(services[::-1][idx]))),
            ),
        )

    def test_change_col0(self, session, muzzle_logic):
        contract = create_contract(session, is_signed=None, services={cst.ServiceId.DIRECT: True})
        session.flush()

        state_obj = StateBuffer(
            params={
                'req_id': str(contract.id),
                'services': '1',
                'services-%s' % cst.ServiceId.MARKET: str(cst.ServiceId.MARKET),
                'services-%s' % cst.ServiceId.DRIVE: str(cst.ServiceId.DRIVE),
                'req_is-signed': '',
                'req_is-signed-dt': session.now().strftime('%Y-%m-%dT%H:%M:%S'),
                'req_payment-term': '10',  # срок оплаты 10 дней
                'req_is-signed-checkpassed': '1',
            },
        )
        res = muzzle_logic.get_contract2(session, contract.id, state_obj=state_obj)
        assert res.tag == 'redirect-back'
        hm.assert_that(res.attrib, hm.has_entries({'contract-id': str(contract.id)}))

        current = contract.current_state()
        assert current.payment_term == 10
        assert sorted(list(current.services)) == sorted(current.services)
        assert current.is_signed is not None

    def test_create_new_col(self, session, muzzle_logic):
        contract = create_contract(session, is_signed=None, services={cst.ServiceId.DIRECT: True})
        col_old = ob.CollateralBuilder.construct(session, contract=contract, num='01',
                                                 collateral_type=general.collateral_types[1001], dt=(session.now() + datetime.timedelta(days=1)),
                                                 services={cst.ServiceId.DRIVE: True})
        state_obj = StateBuffer(
            params={
                'req_id': str(contract.id),
                'req_col-new-collateral-form': '',
                'req_col-new-num': '001',
                'req_col-new-collateral-type': '1001',
                # создаем договор датой начала ранее col_old,
                # таким образом он окажется на второй странице, а не на первой
                'req_col-new-dt': session.now().strftime('%Y-%m-%dT%H:%M:%S'),
                'req_col-new-is-signed-checkpassed': '1',
                'req_col-new-is-signed-dt': session.now().strftime('%Y-%m-%dT%H:%M:%S'),
                'req_col-new-sent-dt-checkpassed': '1',
                'req_payment-term': '10',  # срок оплаты 10 дней
                'req_col-new-group02-grp-1001-services': '1',
                'req_col-new-group02-grp-1001-services-%s' % cst.ServiceId.DRIVE: str(cst.ServiceId.DRIVE),
                'req_col-new-group02-grp-1001-services-%s' % cst.ServiceId.BUSES: str(cst.ServiceId.BUSES),
                'req_col-new-collateral-pn': '1',
                'req_col-new-collateral-ps': '1',
            },
        )
        res = muzzle_logic.get_contract2(session, contract.id, state_obj=state_obj)
        assert res.tag == 'redirect-back'
        hm.assert_that(
            res.attrib,
            hm.has_entries({
                'contract-id': str(contract.id),
                'all-collaterals': '0',
                'collateral-pn': '2',
                'collateral-ps': '1',
            }),
        )

        assert len(contract.collaterals) == 3
        col0, cols = contract.collaterals[0], contract.collaterals[1:]
        col = cols[1]
        current = contract.current_state()

        assert col.num == '001'
        assert col.collateral_type.id == 1001

        # текущее состояние - это те сервисы, которые мы указали в ДС
        assert sorted(list(current.services)) == sorted([cst.ServiceId.DRIVE, cst.ServiceId.BUSES])

        # начальное состояние - это 1 сервис
        hm.assert_that(
            col0.services,
            hm.has_entries({cst.ServiceId.DIRECT: 1}),
        )
        # а в допнике фиксируется, что со старого сервиса мы галку сняли и добавили новых 2
        hm.assert_that(
            col.services,
            has_exact_entries({
                cst.ServiceId.DIRECT: None,
                cst.ServiceId.DRIVE: 1,
                cst.ServiceId.BUSES: 1,
            }),
        )

    def test_create_new_selfemployed_col(self, session, muzzle_logic):
        client = ob.ClientBuilder.construct(session)
        person = ob.PersonBuilder.construct(session, client=client, type='ur')
        contract = create_partner_contract(session, client=client, person=person, services={cst.ServiceId.RNY: True})
        state_obj = StateBuffer(
            params={
                'req_id': str(contract.id),
                'req_col-new-collateral-form': '',
                'req_col-new-num': 'testtest',
                'req_col-new-collateral-type': '2160',
                'col_new_group02_grp_2160_selfemployed': '1',
                'req_col-new-dt': session.now().strftime('%Y-%m-%dT%H:%M:%S'),
                'req_col-new-is-signed-checkpassed': '1',
                'req_col-new-is-signed-dt': session.now().strftime('%Y-%m-%dT%H:%M:%S'),
                'req_col-new-sent-dt-checkpassed': '1',
                'req_col-new-collateral-pn': '1',
                'req_col-new-collateral-ps': None,
            },
        )
        res = muzzle_logic.get_contract2(session, contract.id, state_obj=state_obj)

        assert len(contract.collaterals) == 2
        col0, col = contract.collaterals

        current = contract.current_state()

        assert col.num == 'testtest'
        assert col.collateral_type.id == 2160

    def test_change_col(self, session, muzzle_logic):
        contract = create_contract(session, services={cst.ServiceId.DIRECT: True})
        col = ob.CollateralBuilder.construct(session, contract=contract, num='01', collateral_type=general.collateral_types[1001], dt=session.now(), services={cst.ServiceId.DRIVE: True})
        session.flush()

        assert list(contract.current_state().services) == [cst.ServiceId.DRIVE]

        state_obj = StateBuffer(
            params={
                'req_id': str(contract.id),
                'req_col-%s-collateral-form' % col.id: '',
                'req_col-%s-num' % col.id: '002',
                'req_col-%s-collateral-type' % col.id: '1001',
                'req_col-%s-dt' % col.id: session.now().strftime('%Y-%m-%dT%H:%M:%S'),
                'req_col-%s-is-signed-checkpassed' % col.id: '1',
                'req_col-%s-is-signed-dt' % col.id: session.now().strftime('%Y-%m-%dT%H:%M:%S'),
                'req_col-%s-sent-dt-checkpassed' % col.id: '1',
                'req_payment-term': '10',  # срок оплаты 10 дней
                'req_col-%s-group02-grp-1001-services' % col.id: '1',
                'req_col-%s-group02-grp-1001-services-%s' % (col.id, cst.ServiceId.BUSES): str(cst.ServiceId.BUSES),
                'req_col-%s-collateral-pn' % col.id: 1,
                'req_col-%s-collateral-ps' % col.id: 3,
                'req_col-%s-all-collaterals' % col.id: 1,
            },
        )
        res = muzzle_logic.get_contract2(session, contract.id, state_obj=state_obj)
        assert res.tag == 'redirect-back'
        hm.assert_that(
            res.attrib,
            hm.has_entries({
                'contract-id': str(contract.id),
                'all-collaterals': '1',
                'collateral-pn': '1',
                'collateral-ps': '3',
            }),
        )

        session.refresh(contract)
        session.refresh(col)

        current = contract.current_state()
        assert list(current.services) == [cst.ServiceId.BUSES]

        assert col.num == '002'
        hm.assert_that(
            col.services,
            has_exact_entries({
                cst.ServiceId.DIRECT: None,
                cst.ServiceId.DRIVE: None,
                cst.ServiceId.BUSES: 1,
            }),
        )

    def test_invalid_col_for_changing(self, session, muzzle_logic, contract):
        col = ob.CollateralBuilder.construct(session, contract=contract, num='01', collateral_type=general.collateral_types[80], dt=session.now(), memo='Aaaa')
        session.flush()

        invalid_col_id = col.id + 1
        state_obj = StateBuffer(
            params={
                'req_id': str(contract.id),
                'req_col-%s-collateral-form' % invalid_col_id: '',
                'req_col-%s-num' % invalid_col_id: '002',
                'req_col-%s-collateral-type' % invalid_col_id: '80',
                'req_col-%s-dt' % invalid_col_id: session.now().strftime('%Y-%m-%dT%H:%M:%S'),
                'req_col-%s-is-signed-checkpassed' % invalid_col_id: '1',
                'req_col-%s-is-signed-dt' % invalid_col_id: session.now().strftime('%Y-%m-%dT%H:%M:%S'),
                'req_col-%s-sent-dt-checkpassed' % col.id: '1',
                'req_payment-term': '10',  # срок оплаты 10 дней
            },
        )
        with pytest.raises(exc.NOT_FOUND) as exc_info:
            muzzle_logic.get_contract2(session, contract.id, state_obj=state_obj)
        assert exc_info.value.msg == 'Object not found: Collateral id=%s not found' % invalid_col_id


class TestCredits(object):
    def _create_clients(self, session):
        agency = ob.ClientBuilder.construct(session)
        agency.set_currency(cst.ServiceId.DIRECT, 'RUB', session.now(), cst.CONVERT_TYPE_COPY)
        agency.assign_agency_status(is_agency=True)
        client = ob.ClientBuilder.construct(session, agency=agency)
        return agency, client

    def _create_credit_contracts(self, session, agency, client, firms):
        common_contract_params = dict(
            commission=0,
            commission_type=None,
            credit_type=2,
            credit_limit_single=666,
            personal_account=1,
            personal_account_fictive=1,
            client_limits=None,
            client=agency,
            person=ob.PersonBuilder(client=agency, type='ur'),
        )

        contracts = []
        for firm in firms:
            contract = create_credit_contract(session, firm=firm.id, **common_contract_params)
            contracts.append(contract)

        for contract in contracts:
            paysys = ob.PaysysBuilder.construct(
                session,
                firm_id=contract.firm.id,
                payment_method_id=cst.PaymentMethodIDs.bank,
                iso_currency='RUB',
                currency=mapper.fix_crate_base_cc('RUB'),
                extern=1,
            )
            request = ob.RequestBuilder.construct(
                session,
                basket=ob.BasketBuilder(
                    client=agency,
                    rows=[ob.BasketItemBuilder(
                        quantity=100,
                        order=ob.OrderBuilder(
                            client=client,
                            agency=agency,
                            product=ob.Getter(mapper.Product, cst.DIRECT_PRODUCT_RUB_ID),
                        ),
                    )],
                ),
            )
            invoice, = core.Core(session).pay_on_credit(request.id, paysys.id, contract.person_id, contract.id)
            invoice.close_invoice(session.now())

        return contracts

    @pytest.mark.permissions
    @pytest.mark.parametrize(
        'allow',
        [0, 1],
    )
    def test_get_expired_credits_permission(self, session, muzzle_logic, view_invoice_role, view_contract_role, allow):
        agency, client = self._create_clients(session)
        firm = ob.FirmBuilder.construct(session, postpay=1, default_currency='RUR')

        ob.set_roles(
            session,
            session.passport,
            [
                (view_invoice_role, {cst.ConstraintTypes.firm_id: firm.id if allow else cst.FirmId.YANDEX_OOO}),
            ],
        )

        contract, = self._create_credit_contracts(session, agency, client, [firm])
        invoice = filter(lambda i: isinstance(i, mapper.YInvoice), contract.invoices)[0]

        if allow:
            res = muzzle_logic.get_expired_credits(session, contract.id, expired=-1)
            res_json = xml2json_auto(res)
            hm.assert_that(
                res_json.get('invoices', {}).get('invoice', {}),
                hm.has_entries({
                    'iso-currency': 'RUB',
                    'invoice-eid': invoice.external_id,
                    'invoice-id': str(invoice.id),
                    'time-to-live': u'-1',
                }),
            )
        else:
            with pytest.raises(exc.PERMISSION_DENIED) as exc_info:
                muzzle_logic.get_expired_credits(session, contract.id, expired=-1)
            assert exc_info.value.msg == u'User %s has no permission ViewInvoices.' % session.oper_id


class MockWiki(object):
    @staticmethod
    def url_join(*parts):
        return 'https://wiki.yandex.net/api/test/'

    def delete_page(self, instance_url):
        pass

    def make_url_relative(self, wiki_tpl):
        pass

    def check_page_exists(self, page_url):
        return True

    def raw(self, page_url):
        return '{% contract %}'.decode('utf-8')

    def create_or_modify_page(self, page_body, page_url, title):
        return 'https://url.ru/'

    def html(self, data):
        return ''


class MockMdsClient(object):
    def __init__(self, **kw):
        pass

    def delete(self, mds_key):
        assert mds_key == 'mds_key'


class TestContractAjax(object):
    ajax_kw = {
        'state_obj': StateBuffer(params={'prot_method': 'POST'}),
        'request_obj': RequestBuffer(params=([], [('X-Requested-With', 'XMLHttpRequest')], []))
    }

    @pytest.mark.parametrize(
        'match_client, firm_id, ans',
        (
                pytest.param(True, CONTRACT_FIRM_ID, True, id='w client w firm'),
                pytest.param(True, None, True, id='w client wo firm'),
                pytest.param(False, CONTRACT_FIRM_ID, False, id='wo client w firm'),
                pytest.param(True, cst.FirmId.DRIVE, False, id='w client w wrong firm'),
        ),
    )
    @mock.patch('balance.publisher.wiki_api.Wiki', MockWiki)
    @mock.patch('balance.publisher.wiki_handler.EasyMDS', MockMdsClient)
    def test_erase_print_form_wiki_instance(self, session, client, print_tmpl_role, match_client, firm_id, ans):
        batch_id = create_role_client(session, client=client if match_client else False).client_batch_id
        ob.set_roles(
            session,
            session.passport,
            [(print_tmpl_role, {cst.ConstraintTypes.firm_id: CONTRACT_FIRM_ID, cst.ConstraintTypes.client_batch_id: batch_id})],
        )
        contract = create_contract(session, client=client, firm_id=firm_id)
        contract.col0.print_template = cst.PrintTplEmail.ADDRESS_DOCUMENTS
        contract.col0.print_tpl_mds_key = 'mds_key'
        contract.col0.print_tpl_barcode = 666
        session.flush()

        if ans:
            contract_ajax.erase_print_form_wiki_instance(session, object_id=contract.id, object_type='contract', **self.ajax_kw)
            assert contract.col0.print_tpl_mds_key is None
        else:
            with pytest.raises(exc.PERMISSION_DENIED) as exc_info:
                contract_ajax.erase_print_form_wiki_instance(session, object_id=contract.id, object_type='contract',
                                                             **self.ajax_kw)
            assert exc_info.value.msg == u'User %s has no permission AlterPrintTemplate.' % session.oper_id
