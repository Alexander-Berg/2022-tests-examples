# -*- coding: utf-8 -*-
import datetime
import decimal
import mock
import pytest
import hamcrest as hm

import butils

from balance import mapper
from balance import constants as cst
from balance.utils.xml2json import xml2json_auto
from balance.corba_buffers import StateBuffer

from tests import object_builder as ob

D = decimal.Decimal


def create_orders(client, services):
    return [ob.OrderBuilder(client=client, service=service) for service in services]


def insert_into_mv_contract_signed_attr(session, params):
    session.execute('insert into mv_contract_signed_attr(collateral_id, code, value_num, update_dt)'
                    ' values(:collateral_id, :code, :value_num, sysdate)', params)


@pytest.fixture(name='client')
def create_client(session):
    return ob.ClientBuilder().build(session).obj


@pytest.fixture(name='person')
def create_person(session, client):
    return ob.PersonBuilder(client=client, person_type='ur').build(session).obj


@pytest.fixture
def service_groups(session):
    return session.query(mapper.ServiceGroup) \
            .filter(mapper.ServiceGroup.show_to_extern == 1) \
            .order_by(mapper.ServiceGroup.id).all()


@pytest.fixture(name='contract')
def create_contract(session, client, person, firm_id=cst.FirmId.YANDEX_OOO):
    contract = ob.ContractBuilder(
        client=client,
        person=person,
        commission=0,
        firm=firm_id,
        postpay=1,
        personal_account=1,
        personal_account_fictive=1,
        payment_type=3,
        payment_term=30,
        credit=3,
        credit_limit_single='9' * 20,
        services={7, 11, 35},
        is_signed=session.now(),
    ).build(session).obj

    return contract


@pytest.fixture(name='invoice')
def create_invoice(session, client=None, person=None, contract=None, orders=None,
                   firm_id=cst.FirmId.YANDEX_OOO, service_id=cst.ServiceId.DIRECT):
    client = client or ob.ClientBuilder()
    person = person or create_person(session, client)
    orders = orders or [ob.OrderBuilder(client=client, service_id=service_id).build(session).obj]
    request = ob.RequestBuilder(
        firm_id=firm_id,
        basket=ob.BasketBuilder(
            client=client,
            rows=[ob.BasketItemBuilder(order=order, quantity=D('666')) for order in orders]
        )
    ).build(session).obj

    invoice = ob.InvoiceBuilder(
        request=request,
        person=person,
        contract=contract,
    ).build(session).obj
    return invoice


class TestGetConsumesHistoryServiceGroupForUser(object):

    @pytest.mark.parametrize(
        'is_admin', [False, True]
    )
    def test_user(self, session, muzzle_logic, client, service_groups, is_admin):
        services = [ob.Getter(mapper.Service, cst.ServiceId.DIRECT).build(session).obj,
                    ob.Getter(mapper.Service, cst.ServiceId.ADFOX).build(session).obj]
        session.passport.link_to_client(client)
        _orders = create_orders(client, services)
        _inv = create_invoice(session, client=client, orders=_orders)

        if not is_admin:
            with mock.patch('butils.passport.passport_admsubscribe'):
                ob.set_roles(session, session.passport, [])
                session.oper_perms = []
                session.flush()
            service_groups = [service.group for service in services]

        res = muzzle_logic.get_consumes_history_service_groups_for_user(session)
        res = xml2json_auto(res)

        hm.assert_that(
            res['service-group'],
            hm.contains_inanyorder(*[
                hm.has_entries({
                    'group-code': s_group.group_code,
                    'id': str(s_group.id),
                    'name': s_group.name,
                })
                for s_group in service_groups
            ]),
        )


@pytest.mark.permissions
@mock.patch('butils.passport.passport_admsubscribe')
class TestConsumesHistoryPermission(object):
    role_map = {
        'client': lambda s: ob.Getter(mapper.Role, cst.RoleName.CLIENT).build(s).obj,
        'view_invoices': lambda s: ob.create_role(
            s,
            (cst.PermissionCode.VIEW_INVOICES, {cst.ConstraintTypes.firm_id: None}),
        ),
    }

    @pytest.mark.parametrize(
        'inv_firm_ids, role_firm_ids',
        (
                ((cst.FirmId.YANDEX_OOO, cst.FirmId.MARKET),
                 set()),
                ((cst.FirmId.YANDEX_OOO, cst.FirmId.MARKET),
                 (cst.FirmId.YANDEX_OOO,)),
                ((cst.FirmId.YANDEX_OOO, cst.FirmId.YANDEX_OOO, cst.FirmId.MARKET),
                 (cst.FirmId.YANDEX_OOO,)),
                ((cst.FirmId.YANDEX_OOO, cst.FirmId.MARKET, cst.FirmId.AUTORU),
                 (cst.FirmId.YANDEX_OOO, cst.FirmId.MARKET)),
        ),
    )
    def test_filter_by_constraints(self, _mock_pas_adm, session, muzzle_logic,
                                   inv_firm_ids, role_firm_ids):
        """Фильтр по фирме указанной в роли"""
        service_id = cst.ServiceId.DIRECT
        role = ob.create_role(session, (cst.PermissionCode.VIEW_INVOICES, {cst.ConstraintTypes.firm_id: None}))
        invoices = []
        required_invoices = []

        if role_firm_ids:
            ob.set_roles(session, session.passport, [
                (role, {cst.ConstraintTypes.firm_id: firm_id})
                for firm_id in role_firm_ids
            ])
        else:
            ob.set_roles(session, session.passport, role)

        for firm_id in inv_firm_ids:
            client = create_client(session)
            person = create_person(session, client)
            contract = create_contract(session, client, person, firm_id)
            insert_into_mv_contract_signed_attr(
                session,
                {'collateral_id': contract.col0.id, 'code': 'PERSONAL_ACCOUNT_FICTIVE', 'value_num': 1},
            )
            invoice = create_invoice(session, client=client, person=person, contract=contract,
                                     firm_id=firm_id, service_id=service_id)
            invoice.turn_on_rows()
            invoices.append(invoice)
            if firm_id in role_firm_ids:
                required_invoices.append(invoice)

        if not role_firm_ids:
            required_invoices = invoices  # нет ограничения по фирме

        state_obj = StateBuffer(
            params={
                # 'req_client_id': str(client.id),
                'req_service_id': str(service_id),
                'req_operation_dt_from': (session.now() - datetime.timedelta(days=1)).strftime('%Y-%m-%dT%H:%M:%S'),
            }
        )
        response = muzzle_logic.get_consumes_history(session, state_obj, {}, True, 'xml')
        response_json = xml2json_auto(response, 'entries/entry')
        assert response_json['total_row_count'] == str(len(required_invoices))
        hm.assert_that(
            response_json['entry'],
            hm.contains_inanyorder(*[
                hm.has_entries({'invoice_eid': inv.external_id})
                for inv in required_invoices
            ]),
        )

    @pytest.mark.parametrize(
        'roles',
        [
            ('client',),
            ('client', 'view_invoices'),  # даже с админскими правами нельзя получить не свой счёт

        ],
    )
    def test_client_owns_invoice(self, _mock_pas_adm, session, muzzle_logic, roles):
        firm_id = cst.FirmId.YANDEX_OOO
        min_dt = session.now()

        client1 = create_client(session)
        client2 = create_client(session)

        session.passport.link_to_client(client1)
        ob.set_roles(session, session.passport, [self.role_map[role_name](session) for role_name in roles])
        invoices = {}

        for client in [client1, client2]:
            person = create_person(session, client)
            contract = create_contract(session, client, person, firm_id)
            insert_into_mv_contract_signed_attr(
                session,
                {'collateral_id': contract.col0.id, 'code': 'PERSONAL_ACCOUNT_FICTIVE', 'value_num': 1},
            )
            invoice = create_invoice(session, client=client, person=person, contract=contract,
                                     firm_id=firm_id, service_id=cst.ServiceId.DIRECT)
            invoice.turn_on_rows()
            invoices[client] = invoice

        session.flush()
        max_dt = session.now()

        state_obj = StateBuffer(
            params={
                'req_operation_dt_from': min_dt.strftime('%Y-%m-%dT%H:%M:%S'),
                'req_operation_dt_to': max_dt.strftime('%Y-%m-%dT%H:%M:%S'),
            }
        )
        response = muzzle_logic.get_consumes_history(session, state_obj, {}, False, 'xml')
        response_json = xml2json_auto(response, 'entries/entry')
        assert response_json['total_row_count'] == '1'
        hm.assert_that(
            response_json['entry'],
            [hm.has_entries({'invoice_eid': invoices[client1].external_id, 'client_id': str(client1.id)})],
        )
