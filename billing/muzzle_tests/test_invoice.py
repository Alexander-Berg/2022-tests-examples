# -*- coding: utf-8 -*-
import datetime
import decimal

import pytest
import mock
import hamcrest as hm

import butils
from balance import exc
from balance import muzzle_util as ut
from balance import mapper
from balance import constants as cst
from balance.actions.invoice_turnon import InvoiceTurnOn
from balance.actions.consumption import reverse_consume
from balance.providers import personal_acc_manager as pam
from balance.utils.xml2json import xml2json_auto
from balance.corba_buffers import StateBuffer
from muzzle.api import invoice as invoice_api
from muzzle import xreport

from tests import object_builder as ob
from tests.matchers import string_as_number_equals_to

D = decimal.Decimal


@pytest.fixture(name='client')
def create_client(session):
    return ob.ClientBuilder().build(session).obj


@pytest.fixture(name='person')
def create_person(session, client):
    return ob.PersonBuilder(client=client, person_type='ur').build(session).obj


@pytest.fixture
def contract(session, client, person):
    contract = ob.ContractBuilder(
        client=client,
        person=person,
        commission=0,
        firm=1,
        postpay=1,
        personal_account=1,
        personal_account_fictive=1,
        payment_type=3,
        payment_term=30,
        credit=3,
        credit_limit_single='9' * 20,
        services={7, 11, 35},
        is_signed=datetime.datetime.now()
    ).build(session).obj

    return contract


@pytest.fixture(name='order')
def create_order(client, session):
    return ob.OrderBuilder(client=client).build(session).obj


@pytest.fixture(name='invoice')
def create_invoice(session, client, person=None, order=None, firm_id=cst.FirmId.YANDEX_OOO, service_id=cst.ServiceId.DIRECT):
    person = person or ob.PersonBuilder.construct(session, client=client)
    order = order or ob.OrderBuilder(client=client, service_id=service_id)
    request = ob.RequestBuilder(
        firm_id=firm_id,
        basket=ob.BasketBuilder(
            client=client,
            rows=[ob.BasketItemBuilder(order=order, quantity=1)]
        )
    ).build(session).obj

    invoice = ob.InvoiceBuilder(
        request=request,
        person=person
    ).build(session).obj
    return invoice


@pytest.fixture(name='view_inv_role')
def create_view_inv_role(session):
    return ob.create_role(
        session,
        (cst.PermissionCode.VIEW_INVOICES, {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None}),
    )


@pytest.fixture(name='view_client_role')
def create_view_client_role(session):
    return ob.create_role(session, (cst.PermissionCode.VIEW_CLIENTS, {cst.ConstraintTypes.client_batch_id: None}))


@pytest.mark.permissions
@mock.patch('butils.passport.passport_admsubscribe')
class TestGetInvoicesPermissions(object):
    role_map = {
        'client': lambda s: ob.Getter(mapper.Role, cst.RoleName.CLIENT).build(s).obj,
        'view_invoices': lambda s: ob.create_role(
            s,
            (cst.PermissionCode.VIEW_INVOICES, {cst.ConstraintTypes.firm_id: None}),
        ),
    }

    @pytest.mark.parametrize(
        'link_client, roles, count',
        [
            (False, ('client',), 0),  # без привязанного клиента - ничего
            (True, ('client',), 1),  # только свой счёт
            (True, ('client', 'view_invoices'), 2),  # под админом получаем все счета
            (False, ('client', 'view_invoices'), 2),  # под админом получаем все счета

        ],
    )
    def test_from_client_ui(self, _mock_pas_adm, session, muzzle_logic, roles, count, link_client):
        """Доступны только счета, которе принадлежат клиенту"""
        firm_id = cst.FirmId.YANDEX_OOO
        min_dt = session.now()

        client1 = create_client(session)
        client2 = create_client(session)

        if link_client:
            session.passport.link_to_client(client1)
        ob.set_roles(session, session.passport, [self.role_map[role_name](session) for role_name in roles])

        order1 = create_order(client1, session)
        invoice1 = create_invoice(session, client1, ob.PersonBuilder(client=client1), order1, firm_id)

        order2 = create_order(client2, session)
        invoice2 = create_invoice(session, client2, ob.PersonBuilder(client=client2), order2)

        session.flush()
        max_dt = session.now()

        state_obj = StateBuffer(
            params={
                'req_date_type': '1',  # invoice
                'req_dt_from': min_dt.strftime('%Y-%m-%dT%H:%M:%S'),
                'req_dt_to': max_dt.strftime('%Y-%m-%dT%H:%M:%S'),
                'req_firm_id': str(firm_id),
            }
        )
        response = muzzle_logic.get_user_invoices(session, state_obj, {})
        response_json = xml2json_auto(response, 'entries/entry')
        assert response_json['total_row_count'] == str(count)
        hm.assert_that(
            response_json.get('entry', []),
            hm.contains_inanyorder(*[
                hm.has_entries({'invoice_eid': inv.external_id, 'client_id': str(c.id)})
                for inv, c in [(invoice1, client1), (invoice2, client2)][:count]
            ]),
        )


class TestGetInvoicesPagination(object):
    @pytest.fixture(params=[True, False], ids=['rownum', 'offset'], autouse=True)
    def paginator_type_switch(self, request):
        base_parser = xreport.parse_xreport

        def patched_parser(attr_name):
            res = base_parser(attr_name)
            rownum_elem = res.find('pager/use_rownum')
            rownum_elem.text = str(int(request.param))
            return res

        with mock.patch('muzzle.xreport.parse_xreport', patched_parser):
            yield

    @pytest.mark.parametrize(
        'pn, ps, res_slice',
        [
            (1, 3, slice(3)),
            (2, 3, slice(3, 6)),
            (1, 666, slice(10)),
            (5, 3, None)
        ]
    )
    def test_pagination(self, session, muzzle_logic, client, person, pn, ps, res_slice):
        invoices = [create_invoice(session, client, person) for i in range(10)]
        for idx, invoice in enumerate(invoices):
            invoice.dt -= datetime.timedelta(idx)
        session.flush()

        state_obj = StateBuffer(
            params={
                'req_client_id': str(client.id),
                'req_pn': str(pn),
                'req_ps': str(ps),
            }
        )
        response = muzzle_logic.get_invoices(session, state_obj)
        response_json = xml2json_auto(response, 'entries/entry')
        assert response_json['total_row_count'] == '10'
        if res_slice:
            assert [e['invoice_id'] for e in response_json['entry']] == [str(i.id) for i in invoices[res_slice]]
        else:
            assert 'entry' not in response_json


class TestGetInvoice(object):
    @pytest.fixture
    def invoice(self, session, client, person):
        orders = [ob.OrderBuilder(client=client).build(session).obj for _ in xrange(3)]

        request = ob.RequestBuilder(
            basket=ob.BasketBuilder(
                client=client,
                rows=[
                    ob.BasketItemBuilder(order=o, quantity=1)
                    for o in orders
                ]
            )
        ).build(session).obj

        invoice = ob.InvoiceBuilder(
            request=request,
            person=person
        ).build(session).obj
        InvoiceTurnOn(invoice, invoice.effective_sum, manual=True).do()
        session.flush()
        session.expire_all()
        return invoice

    @staticmethod
    def _get_base_invoice_matcher(invoice):
        matchers = {
            'id': str(invoice.id),
            'external-id': invoice.external_id,
            'type': invoice.type,
            'currency': 'RUR',
            'client': hm.has_entries(id=str(invoice.client_id), name=invoice.client.name),
            'person': hm.has_entries(id=str(invoice.person_id), name=invoice.person.name),
            'paysys': hm.has_entries(id=str(invoice.paysys_id), name=invoice.paysys.name),
            'consume-sum': format(invoice.consume_sum.as_decimal(), 'f'),
            'receipt-sum': format(invoice.receipt_sum.as_decimal(), 'f'),
            'receipt-sum-1c': format(invoice.receipt_sum_1c.as_decimal(), 'f'),
            'total-act-sum': format(invoice.total_act_sum.as_decimal(), 'f'),
        }
        if invoice.contract:
            matchers['contract'] = hm.has_entries({
                'id': str(invoice.contract_id),
                'external-id': invoice.contract.external_id
            })
        return hm.has_entries(matchers)

    @staticmethod
    def _get_consumes_matcher(invoice):
        return hm.contains(*[
            hm.has_entries({
                'id': str(co.id),
                'consume-qty': format(co.consume_qty.as_decimal(), 'f'),
                'current-qty': format(co.current_qty.as_decimal(), 'f'),
                'completion-qty': format(co.completion_qty.as_decimal(), 'f'),
                'act-qty': format(co.act_qty.as_decimal(), 'f'),
                'consume-sum': format(co.consume_sum.as_decimal(), 'f'),
                'current-sum': format(co.current_sum.as_decimal(), 'f'),
                'completion-sum': format(co.completion_sum.as_decimal(), 'f'),
                'act-sum': format(co.act_sum.as_decimal(), 'f'),
                'order': hm.has_entries({'id': str(co.parent_order_id)})
            })
            for co in invoice.consumes
        ])

    def test_prepay_wo_consumes(self, session, muzzle_logic, invoice):
        res = muzzle_logic.get_invoice_without_consumes(session, None, None, invoice.id, True, ut.Struct(lang='ru'))
        json_res = xml2json_auto(res)
        hm.assert_that(
            json_res,
            hm.all_of(
                self._get_base_invoice_matcher(invoice),
                hm.is_not(
                    hm.has_key('consumes')
                )
            )
        )

    def test_personal_account(self, session, contract, order, muzzle_logic):
        pa = (
            pam.PersonalAccountManager(session)
                .for_contract(contract)
                .for_paysys(ob.Getter(mapper.Paysys, 1003).build(session).obj)
                .get()
        )
        pa.create_receipt(666)
        pa.transfer(order)
        order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: D('3.21')})
        session.flush()
        act1, = pa.generate_act(force=1, backdate=datetime.datetime.now())
        session.flush()
        order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: D('6.66')})
        session.flush()
        act2, = pa.generate_act(force=1, backdate=datetime.datetime.now())
        session.flush()

        act1.invoice.create_receipt(14)
        act2.invoice.create_receipt(654)
        session.flush()
        session.expire_all()

        res = muzzle_logic.get_invoice_without_consumes(session, None, None, pa.id, True, ut.Struct(lang='ru'))
        json_res = xml2json_auto(res)
        hm.assert_that(
            json_res,
            hm.all_of(
                self._get_base_invoice_matcher(pa),
                hm.is_not(
                    hm.has_key('consumes')
                ),
                hm.has_entries({
                    'repayment-invoices': hm.has_entries({
                        'totals': hm.has_entries({
                            'receipt-sum': string_as_number_equals_to(668),
                            'amount': string_as_number_equals_to(668),
                        }),
                        'invoice': hm.contains(*[
                            hm.has_entries({
                                'id': str(ri.id),
                                'external-id': ri.external_id,
                                'currency': ri.currency,
                                'amount': str(ri.receipt_sum.as_decimal()),
                                'receipt-sum': str(ri.receipt_sum.as_decimal()),
                            })
                            for ri in pa.repayments
                        ])
                    })
                })
            )
        )


class TestGetInvoiceOperations(object):
    @pytest.fixture
    def operations(self, session, invoice):
        order = invoice.invoice_orders[0].order
        alt_order = ob.OrderBuilder(
            client=order.client,
            product=order.product
        ).build(session).obj

        operations = []
        for idx in range(16):
            operation = mapper.Operation(
                cst.OperationTypeIDs.support,
                dt=datetime.datetime.now() - datetime.timedelta(17 - idx)
            )
            if idx % 4 == 0:
                invoice.create_receipt(666, operation)
            elif idx % 4 == 1:
                invoice.transfer(order, 2, 100, operation=operation, skip_check=True)
            elif idx % 4 == 2:
                consume = invoice.consumes[-1]
                reverse_consume(consume, operation, 1)
            elif idx % 4 == 3:
                invoice.create_receipt(666, operation)
                consume = invoice.consumes[-1]
                reverse_consume(consume, operation, 1)
                invoice.transfer(alt_order, 2, 100, operation=operation, skip_check=True)

            operations.append(operation)
        return operations

    def test_all(self, session, muzzle_logic, invoice, operations):
        res = muzzle_logic.get_invoice_operations(session, invoice.id, 0, 1000)
        json_res = xml2json_auto(res)

        hm.assert_that(
            json_res,
            hm.has_entries({
                'operation': hm.contains(*[
                    hm.has_entries({
                        'id': str(op.id),
                        'external-type-id': str(op.display_type_id),
                        'type-id': str(op.type_id)
                    })
                    for op in operations
                ])
            })
        )

    def test_limit_offset(self, session, muzzle_logic, invoice, operations):
        res = muzzle_logic.get_invoice_operations(session, invoice.id, 6, 6)
        json_res = xml2json_auto(res)

        hm.assert_that(
            json_res,
            hm.has_entries({
                'operation': hm.contains(*[
                    hm.has_entries({
                        'id': str(op.id),
                        'external-type-id': str(op.display_type_id),
                        'type-id': str(op.type_id)
                    })
                    for op in operations[6:12]
                ])
            })
        )

    def test_null_operations(self, session, muzzle_logic, invoice, operations):
        alt_order = ob.OrderBuilder(
            client=invoice.client,
            product=invoice.invoice_orders[0].order.product
        ).build(session).obj

        invoice.create_receipt(666)
        res = invoice.transfer(alt_order, 2, 16, skip_check=True)
        res.consume.operation = None
        reverse_consume(alt_order.consumes[-1], None, 7)

        res = muzzle_logic.get_invoice_operations(session, invoice.id, 6, 14)
        json_res = xml2json_auto(res)

        matchers = [
            hm.has_entries({
                'id': str(op.id),
                'external-type-id': str(op.display_type_id),
                'type-id': str(op.type_id)
            })
            for op in operations[6:]
        ]
        matchers = matchers + [
            hm.has_entries({
                'id': None,
                'external-type-id': '1',
                'type-id': str('2')
            }),
            hm.has_entries({
                'id': None,
                'external-type-id': '5',
                'type-id': str('2')
            }),
            hm.has_entries({
                'id': None,
                'external-type-id': '6',
                'type-id': str('2')
            })
        ]

        hm.assert_that(
            json_res,
            hm.has_entries({
                'operation': hm.contains_inanyorder(*matchers)
            })
        )


@pytest.mark.permissions
class TestGetClientUnusedFunds(object):
    @pytest.mark.parametrize(
        'role_firm_ids, unused_rur_sum, unused_fish',
        [
            pytest.param(cst.SENTINEL, '0.00', '0.00',
                         id='wo role'),
            pytest.param((None,), '577.00', '19.23',
                         id='role wo constraints'),
            pytest.param((cst.FirmId.YANDEX_OOO,), '566.00', '18.87',
                         id='role constraints matches 1 firm_id'),
            pytest.param((cst.FirmId.YANDEX_OOO, cst.FirmId.CLOUD), '577.00', '19.23',
                         id='role constraints matches 2 firm_id'),
        ],
    )
    def test_permissions(self, session, muzzle_logic, client, view_inv_role, role_firm_ids, unused_rur_sum, unused_fish):
        roles = []
        if role_firm_ids is not cst.SENTINEL:
            roles.extend([
                (view_inv_role, {cst.ConstraintTypes.firm_id: role_firm_id})
                for role_firm_id in role_firm_ids
            ])
        ob.set_roles(session, session.passport, roles)

        invoice1 = create_invoice(session, client=client, firm_id=cst.FirmId.YANDEX_OOO)
        invoice2 = create_invoice(session, client=client, firm_id=cst.FirmId.CLOUD)

        invoice1.manual_turn_on(D('666'))
        invoice2.manual_turn_on(D('111'))\

        res = muzzle_logic.get_client_unused_funds(session, client.id)
        hm.assert_that(
            res.attrib,
            hm.has_entries({
                'unused-rur-sum': unused_rur_sum,
                'unused-fish': unused_fish,
            }),
        )


class TestOverdrafts(object):
    @pytest.mark.parametrize(
        'allow',
        [True, False],
    )
    def test_get_expired_overdrafts(self, session, muzzle_logic, client, view_inv_role, allow):
        passport = session.passport
        client_batch_id = ob.RoleClientBuilder.construct(session, client=client).client_batch_id if allow else 666
        roles = [(view_inv_role, {cst.ConstraintTypes.client_batch_id: client_batch_id})]
        ob.set_roles(session, passport, roles)
        session.flush()

        invoice = ob.InvoiceBuilder.construct(session, client=client, overdraft=True)
        invoice.turn_on_rows()

        if allow:
            res = muzzle_logic.get_expired_overdrafts(session, client.id, None)
            res_invoices = res.find('invoices').getchildren()
            assert len(res_invoices) == 1
            res_invoice = res_invoices[0]
            assert res_invoice.find('invoice-id').text == str(invoice.id)

        else:
            with pytest.raises(exc.PERMISSION_DENIED):
                muzzle_logic.get_expired_overdrafts(session, client.id, None)
