# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import datetime
import http.client as http
import pytest
import mock
import itertools
from decimal import Decimal as D
from flask.helpers import url_quote
from hamcrest import (
    assert_that,
    equal_to,
    empty,
    has_entry,
    has_entries,
    has_item,
    has_items,
    contains,
    contains_string,
    contains_inanyorder,
    not_,
)

from balance import constants as cst, mapper, core
from tests import object_builder as ob
from tests.balance_tests.invoices.invoice_common import (
    create_credit_contract,
)

from brest.core.tests import security
from yb_snout_api.resources import enums as common_enums
from yb_snout_api.utils import clean_dict
from yb_snout_api.utils.ma_fields import DT_FMT
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
from yb_snout_api.resources import enums
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client, create_role_client_group, create_role_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, get_client_role, create_role
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.firm import create_firm
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.service import create_service
from yb_snout_api.tests_unit.fixtures.common import create_paysys
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import (
    create_invoice,
    create_custom_invoice,
    create_invoice_with_endbuyer,
    create_overdraft_invoice,
)

BANK_PAYSYS_ID = 1003


@pytest.fixture(name='view_inv_role')
def create_view_inv_role():
    return create_role(
        (
            cst.PermissionCode.VIEW_INVOICES,
            {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
        ),
    )


def create_contract(
        session,
        limit,
        personal_account=None,
        personal_account_fictive=None,
        client_limits=None,
        firm=cst.FirmId.YANDEX_OOO,
        client=None,
):
    client = client or ob.ClientBuilder.construct(session)
    person = ob.PersonBuilder.construct(session, client=client, type='ur')
    return create_credit_contract(
        session,
        commission=0,
        commission_type=None,
        credit_type=2,
        credit_limit_single=limit,
        personal_account=personal_account,
        personal_account_fictive=personal_account_fictive,
        client_limits=client_limits,
        firm=firm,
        client=client,
        person=person,
    )


def create_request(session, client, qty, agency=None):
    return ob.RequestBuilder(
        basket=ob.BasketBuilder(
            client=agency or client,
            rows=[ob.BasketItemBuilder(
                quantity=qty,
                order=ob.OrderBuilder(
                    client=client,
                    agency=agency,
                    product=ob.Getter(mapper.Product, cst.DIRECT_PRODUCT_RUB_ID),
                ),
            )],
        ),
    ).build(session).obj


def pay_on_credit(contract, request, paysys_id=BANK_PAYSYS_ID):
    invoice, = core.Core(contract.session).pay_on_credit(request.id, paysys_id, contract.person_id, contract.id)
    return invoice


@pytest.mark.smoke
class TestCaseInvoiceList(TestCaseApiAppBase):
    BASE_API = '/v1/invoice/list'

    @pytest.mark.slow
    def test_invoice_list(self, invoice_with_endbuyer):
        order = invoice_with_endbuyer.consumes[0].order

        params = {
            'invoice_eid': invoice_with_endbuyer.external_id,
            'service_id': order.service_id,
            'service_order_id': order.service_order_id,
            'manager_subordinate': 'true',
        }

        response = self.test_client.get(self.BASE_API, params)
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        data = response.get_json().get('data')
        assert_that(
            data,
            has_entries({
                'total_row_count': 1,
                'items': contains(has_entries({
                    'invoice_eid_exact': invoice_with_endbuyer.external_id,
                    'endbuyer': has_entries({'id': invoice_with_endbuyer.endbuyer_id}),
                })),
            }),
        )

    def test_get_invoices_by_client(self, invoice):
        res = self.test_client.get(self.BASE_API, {'client_id': invoice.client.id})
        assert_that(res.status_code, equal_to(http.OK))
        assert_that(res.get_json()['data']['items'], contains(has_entry('invoice_id', invoice.id)))

    def test_get_invoices_by_eid_substr(self, invoice):
        res = self.test_client.get(self.BASE_API, {'invoice_eid': invoice.external_id})
        assert_that(res.status_code, equal_to(http.OK))
        assert_that(res.get_json()['data']['items'], contains(has_entry('invoice_id', invoice.id)))

    def test_get_invoices_by_eid_request(self, invoice):
        res = self.test_client.get(self.BASE_API, {'invoice_eid': invoice.request.id})
        assert_that(res.status_code, equal_to(http.OK))
        assert_that(res.get_json()['data']['items'], contains(has_entry('invoice_id', invoice.id)))

    def test_get_invoices_by_order(self, invoice):
        invoice.turn_on_rows()
        order = invoice.consumes[0].order
        res = self.test_client.get(self.BASE_API, {'service_order_id': order.service_order_id})
        assert_that(res.status_code, equal_to(http.OK))
        assert_that(res.get_json()['data']['items'], contains(has_entry('invoice_id', invoice.id)))

    @pytest.mark.parametrize(
        'advance_invoice',
        ['123', '456', '789']
    )
    def test_get_invoices_by_advance_invoice(self, invoice, advance_invoice):
        session = invoice.session
        another_invoice = create_invoice()
        invoice.turn_on_rows()
        order = invoice.consumes[0].order
        ob.OebsCashPaymentFactBuilder.construct(session, invoice=invoice, acc_number='123', amount=100),
        ob.OebsCashPaymentFactBuilder.construct(session, invoice=another_invoice, acc_number='456', amount=100)
        session.flush()

        exp_res = {'123': invoice.id, '456': another_invoice.id}
        res = self.test_client.get(self.BASE_API, {'advance_invoice': advance_invoice})
        assert_that(res.status_code, equal_to(http.OK))
        items = res.get_json()['data']['items']
        assert_that(
            items,
            contains(has_entry('invoice_id', exp_res[advance_invoice])) if advance_invoice in exp_res else empty()
        )

    def test_invoices_totals(self, invoice_with_endbuyer):

        params = {
            'invoice_eid': invoice_with_endbuyer.external_id,
            'show_totals': 'true',
        }

        response = self.test_client.get(self.BASE_API, params)
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')
        data = response.get_json().get('data')

        assert_that(
            data,
            has_items('totals', 'gtotals'),
            'Data should contains totals',
        )

        expected_keys = [
            'act_total_sum',
            'payment_sum',
            'receipt_sum_1c',
            'row_count',
            'total_sum',
        ]

        assert_that(
            data['totals'],
            contains_inanyorder(*expected_keys),
            'Data totals contains expected keys',
        )

        assert_that(
            data['gtotals'],
            contains_inanyorder(*expected_keys),
            'Data gtotals contains expected keys',
        )

    def test_invoices_sorting(self, invoice_with_endbuyer, overdraft_invoice):
        min_dt = min([invoice_with_endbuyer.dt, overdraft_invoice.dt]) + datetime.timedelta(minutes=-1)
        max_dt = datetime.datetime.now()

        params = {
            'sort_key': 'INVOICE_EID',
            'sort_order': 'ASC',
            'from_dt': min_dt.strftime('%Y-%m-%dT%H:%M:%S'),
            'to_dt': max_dt.strftime('%Y-%m-%dT%H:%M:%S'),
        }

        response = self.test_client.get(self.BASE_API, params)
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        items = response.get_json().get('data')['items']
        asc_sorted_items = sorted(items, key=lambda x: x['invoice_eid'])

        # Проверяем, что результат упорядочен по возрастанию
        assert_that(items, contains(*[has_entries(item) for item in asc_sorted_items]), 'Incorrect order')

        params['sort_order'] = 'DESC'
        response = self.test_client.get(self.BASE_API, params)
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        items = response.get_json().get('data')['items']
        desc_sorted_items = sorted(items, key=lambda x: x['invoice_eid'], reverse=True)

        # Проверяем, что результат упорядочен по убыванию
        assert_that(items, contains(*[has_entries(item) for item in desc_sorted_items]), 'Incorrect order')

    def test_filter_by_paysys_list(self, invoice_with_endbuyer):
        invoice = invoice_with_endbuyer

        params = {
            'paysys_list': invoice.paysys.id,
            'invoice_eid': invoice.external_id,
        }

        response = self.test_client.get(self.BASE_API, params)
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')
        data = response.get_json().get('data')
        assert_that(
            data.get('items', []),
            contains(has_entries({'invoice_id': invoice.id})),
        )

        some_else_id = (
            self.test_session.query(mapper.Paysys.id)
            .filter(mapper.Paysys.id != invoice.paysys.id)
            .first().id
        )

        params['paysys_list'] = some_else_id

        response = self.test_client.get(self.BASE_API, params)
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')
        data = response.get_json().get('data')
        assert_that(data['total_row_count'], equal_to(0), 'Invoice with this type of paysys does not exist')

    @pytest.mark.slow
    def test_filter_by_payment_status(self, client):
        invoices = {
            'unpaid': create_invoice(client=client),
            'paid': create_invoice(client=client),
        }
        invoices['paid'].manual_turn_on(D('100'))

        status_map = [
            (common_enums.InvoicePaymentStatus.ALL.name, ['unpaid', 'paid']),
            (common_enums.InvoicePaymentStatus.TURN_OFF.name, ['unpaid']),
            (common_enums.InvoicePaymentStatus.TURN_ON.name, ['paid']),
        ]

        for payment_status, invoice_names in status_map:
            response = self.test_client.get(
                self.BASE_API,
                {'client_id': client.id, 'payment_status': payment_status},
            )
            assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

            data = response.get_json().get('data')
            assert_that(
                data,
                has_entries({
                    'total_row_count': len(invoice_names),
                    'items': contains_inanyorder(*[
                        has_entries({'invoice_eid': invoices[inv_name].external_id})
                        for inv_name in invoice_names
                    ]),
                }),
            )

    @pytest.mark.parametrize('config_on', [True, False])
    def test_response(self, client, config_on):
        self.test_session.config.__dict__['USE_OLD_SERIALIZER_FOR_SNOUT_INVOICE_LIST'] = config_on
        invoice1 = create_invoice(client=client)
        invoice2 = create_invoice(client=client)
        res = self.test_client.get(
            self.BASE_API,
            {
                'invoice_eid': invoice1.external_id,
                'client_id': invoice1.client_id,
                'show_totals': True,
                'ps': 1,
            },
        )
        assert_that(res.status_code, equal_to(http.OK))

        data = res.get_json()['data']
        assert_that(
            data,
            has_entries({
                'total_row_count': 1,
                'gtotals': has_entries({
                    'payment_sum': '0.00',
                    'act_total_sum': '0.00',
                    'total_sum': '3000.00',
                    'receipt_sum_1c': '0.00',
                    'row_count': 1,
                }),
                'items': contains(
                    has_entries({
                        'act_total_sum': '0.00',
                        'receipt_dt': None,
                        'manager_info': None,
                        'firm_title': 'ООО «Яндекс»',
                        'currency': 'RUR',
                        'manager': None,
                        'person_inn': None,
                        'receipt_sum_1c': '0.00',
                        'postpay': 0,
                        'contract_id': None,
                        'credit': 0,
                        'overdraft': False,
                        'manager_code': None,
                        'invoice_eid_exact': invoice1.external_id,
                        'can_manual_turn_on': True,
                        'suspect': 0,
                        'person_id': invoice1.person_id,
                        'payment_sum': '0.00',
                        'hidden': False,
                        'type': 'prepayment',
                        'receipt_sum': '0.00',
                        'paysys_instant': 0,
                        'person_name': None,
                        'contract_eid': None,
                        'paysys_cc': 'ph',
                        'firm_id': 1,
                        'client_id': invoice1.client_id,
                        'invoice_dt': invoice1.dt.strftime(DT_FMT),
                        'fast_payment': 0,
                        'paysys_id': 1001,
                        'paysys_name': 'Банк для физических лиц',
                        'service_code': None,
                        'invoice_id': invoice1.id,
                        'total_sum': '3000.00',
                        'invoice_eid': invoice1.external_id,
                        'client': client.name,
                        'request_id': invoice1.request_id,
                    }),
                ),
               'totals': has_entries({
                   'payment_sum': '0.00',
                   'act_total_sum': '0.00',
                   'total_sum': '3000.00',
                   'receipt_sum_1c': '0.00',
                   'row_count': 1,
               }),
            }),
        )

    @pytest.mark.parametrize(
        'is_admin',
        [False, True],
    )
    def test_ci_fictive_invoice_filter(self, client, is_admin):
        if not is_admin:
            security.set_roles([])
            security.set_passport_client(client)

        invoice = create_invoice(client=client)
        contract = create_contract(
            session=self.test_session,
            limit=100500,
            client=client,
        )
        request = create_request(self.test_session, client, 10)
        credit_invoice = pay_on_credit(contract, request)

        res = self.test_client.get(
            self.BASE_API,
            {'client_id': client.id} if is_admin else {},
            is_admin=is_admin,
        )
        assert_that(res.status_code, equal_to(http.OK))

        invoice_matches = [has_entries({'invoice_id': invoice.id})]
        if is_admin:
            invoice_matches.append(has_entries({'invoice_id': credit_invoice.id}))

        data = res.get_json()['data']
        assert_that(
            data,
            has_entries({
                'total_row_count': len(invoice_matches),
                'items': contains_inanyorder(*invoice_matches),
            }),
        )

    def test_overdraft_expired(self, client):
        self.test_session.__dict__['OVERDRAFT_EXCEEDED_DELAY'] = 10
        now = self.test_session.now()
        nearly_expired_dt = now - datetime.timedelta(days=15)
        expired_dt = now - datetime.timedelta(days=45)

        security.set_roles([])
        security.set_passport_client(client)

        common_params = dict(client=client, overdraft=True, turn_on=True)
        expired_invoice = create_invoice(dt=expired_dt, **common_params)
        nearly_expired_invoice = create_invoice(dt=nearly_expired_dt, **common_params)
        _ok_invoice = create_invoice(dt=now, **common_params)

        for trouble_type, inv in [('OVERDRAFT_NEARLY_EXPIRED', nearly_expired_invoice), ('OVERDRAFT_EXPIRED', expired_invoice)]:
            res = self.test_client.get(self.BASE_API, {'trouble_type': trouble_type}, is_admin=False)
            assert_that(res.status_code, equal_to(http.OK), 'Fetching %s' % trouble_type)

            data = res.get_json()['data']
            assert_that(
                data,
                has_entries({
                    'total_row_count': 1,
                    'items': contains(has_entries({'invoice_id': inv.id})),
                }),
                'Fetching %s' % trouble_type,
            )

    @mock.patch('balance.mncloselib.get_task_last_status', return_value='resolved')  # не падать первого числа
    @pytest.mark.parametrize(
        'expired_type, expired_days',
        [
            pytest.param('CREDIT_NEARLY_EXPIRED', 2, id='nearly_expired'),
            pytest.param('CREDIT_EXPIRED', -1, id='expired'),
        ],
    )
    def test_credits_expired(self, _mock_mnclose, expired_type, expired_days):
        agency = create_client(is_agency=True)
        agency.set_currency(cst.ServiceId.DIRECT, 'RUB', self.test_session.now(), cst.CONVERT_TYPE_COPY)
        client = create_client(agency=agency)
        firm = create_firm(postpay=1, default_currency='RUR')
        paysys = create_paysys(firm.id)

        security.set_roles([])
        security.set_passport_client(agency)

        exp_dt = self.test_session.now() + datetime.timedelta(days=expired_days)

        contract = create_contract(
            session=self.test_session,
            limit=100500,
            client=agency,
            firm=firm.id,
            personal_account=1,
            personal_account_fictive=1,
        )
        request = create_request(self.test_session, client, 10, agency=agency)
        credit_invoice = pay_on_credit(contract, request, paysys_id=paysys.id)
        credit_invoice.close_invoice(exp_dt)

        exp_inv = filter(lambda i: isinstance(i, mapper.YInvoice), contract.invoices)[0]
        self.test_session.flush()

        res = self.test_client.get(self.BASE_API, {'trouble_type': expired_type}, is_admin=False)
        assert_that(res.status_code, equal_to(http.OK))

        data = res.get_json()['data']
        assert_that(
            data,
            has_entries({
                'total_row_count': 1,
                'items': contains(has_entries({'invoice_id': exp_inv.id})),
            }),
        )

    def test_credit_expired_exception(self):
        res = self.test_client.get(self.BASE_API, {'trouble_type': 'CREDIT_EXPIRED'})
        assert_that(res.status_code, equal_to(http.BAD_REQUEST))

        assert_that(
            res.get_json(),
            has_entries({'error': 'FILTER_NEEDS_CLIENT'}),
        )


@pytest.mark.permissions
@pytest.mark.slow
@pytest.mark.smoke
class TestCaseInvoiceListPermissions(TestCaseApiAppBase):
    BASE_API = '/v1/invoice/list'
    role_map = {
        'client': get_client_role,
        'view_invoices': create_view_inv_role,
    }

    @pytest.mark.parametrize(
        'inv_firm_ids, role_firm_ids',
        (
            ((cst.FirmId.YANDEX_OOO, cst.FirmId.MARKET),
             cst.SENTINEL),
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
    def test_filtering_by_user_constraints_firm_id(
            self,
            client,
            admin_role,
            view_inv_role,
            inv_firm_ids,
            role_firm_ids,
    ):
        """Фильтр счетов по фирме указанной в роли"""
        session = self.test_session
        invoices = []
        required_invoices = []

        if role_firm_ids is cst.SENTINEL:
            roles = []  # нет роли вообще
        elif not role_firm_ids:
            roles = [view_inv_role]  # роль без ограничений
        else:
            roles = [  # роль с ограничениями
                (view_inv_role, {cst.ConstraintTypes.firm_id: firm_id})
                for firm_id in role_firm_ids
            ]
        roles.append(admin_role)
        security.set_roles(roles)

        for firm_id in inv_firm_ids:
            order = ob.OrderBuilder(client=client)
            invoice = create_custom_invoice({order: D('50')}, client, firm_id)
            invoices.append(invoice)
            if role_firm_ids is cst.SENTINEL:
                continue
            elif (
                    not role_firm_ids  # нет ограничения по фирме
                    or firm_id in role_firm_ids
            ):
                required_invoices.append(invoice)
        session.flush()

        response = self.test_client.get(self.BASE_API, {'client_id': client.id})
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        data = response.get_json().get('data')
        assert_that(data['total_row_count'], equal_to(len(required_invoices)))
        assert_that(
            data['items'],
            contains_inanyorder(*[
                has_entries({'invoice_eid': inv.external_id, 'firm_id': inv.firm_id})
                for inv in required_invoices
            ]),
        )

    @pytest.mark.slow
    def test_filtering_by_user_constraint_client_id_and_firm_id(
            self,
            admin_role,
            view_inv_role,
            service,
    ):
        firms = [create_firm() for _i in range(3)]
        clients = [
            [
                create_client()
                for _j in range(2)
            ]
            for _i in firms
        ]
        client_batch_ids = [
            create_role_client_group(clients=client_group).client_batch_id
            for client_group in clients
        ]
        roles = [
            admin_role,
            (
                view_inv_role,  # role1
                {cst.ConstraintTypes.firm_id: firms[0].id, cst.ConstraintTypes.client_batch_id: client_batch_ids[0]},
            ),
            (
                view_inv_role,  # role2
                {cst.ConstraintTypes.firm_id: firms[1].id, cst.ConstraintTypes.client_batch_id: client_batch_ids[1]},
            ),
            (
                view_inv_role,  # role3: разрешена любая фирма
                {cst.ConstraintTypes.client_batch_id: client_batch_ids[2]},
            ),
        ]
        security.set_roles(roles)

        required_invoices = [
            # role1
            create_invoice(client=clients[0][0], firm_id=firms[0].id, service_id=service.id),
            create_invoice(client=clients[0][1], firm_id=firms[0].id, service_id=service.id),
            # role2
            create_invoice(client=clients[1][0], firm_id=firms[1].id, service_id=service.id),
            create_invoice(client=clients[1][1], firm_id=firms[1].id, service_id=service.id),
            # role3
            create_invoice(client=clients[2][0], firm_id=firms[1].id, service_id=service.id),
            create_invoice(client=clients[2][1], firm_id=firms[2].id, service_id=service.id),
        ]
        invoices = [
            create_invoice(client=clients[1][0], firm_id=firms[0].id, service_id=service.id),
            create_invoice(client=clients[0][0], firm_id=firms[1].id, service_id=service.id),
            create_invoice(client=clients[0][0], firm_id=firms[2].id, service_id=service.id),
            create_invoice(client=create_client(), firm_id=firms[2].id, service_id=service.id),
        ]
        for inv in itertools.chain(required_invoices, invoices):
            inv.turn_on_rows()
        self.test_session.flush()

        response = self.test_client.get(self.BASE_API, {'service_id': service.id})
        assert_that(response.status_code, equal_to(http.OK))

        data = response.get_json().get('data')
        assert_that(data['total_row_count'], equal_to(len(required_invoices)))
        assert_that(
            data['items'],
            contains_inanyorder(*[
                has_entries({
                    'invoice_eid': inv.external_id,
                    'firm_id': inv.firm_id,
                    'client_id': inv.client_id,
                })
                for inv in required_invoices
            ]),
        )

    @pytest.mark.parametrize(
        'role_builders, res_count',
        [
            ([role_map['client']], 1),
            ([role_map['client'], role_map['view_invoices']], 2),  # с админскими правами можно получить не свой счёт

        ],
    )
    def test_client_owns_invoice(self, role_builders, res_count):
        """Получаем только счета, которые принадлежат клиенту из паспорта"""
        session = self.test_session
        firm_id = cst.FirmId.YANDEX_OOO
        from_dt = session.now()

        client1 = create_client()
        client2 = create_client()

        roles = [builder() for builder in role_builders]
        security.set_passport_client(client1)
        security.set_roles(roles)

        order1 = ob.OrderBuilder(client=client1)
        invoice1 = create_custom_invoice({order1: D('50')}, client1, firm_id)
        order2 = ob.OrderBuilder(client=client2)
        invoice2 = create_custom_invoice({order2: D('50')}, client2, firm_id)
        session.flush()
        to_dt = session.now()

        response = self.test_client.get(
            self.BASE_API,
            {
                'from_dt': from_dt.strftime('%Y-%m-%dT%H:%M:%S'),
                'to_dt': to_dt.strftime('%Y-%m-%dT%H:%M:%S'),
                'firm_id': firm_id,
            },
            is_admin=False,
        )
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        data = response.get_json().get('data')
        assert_that(data['total_row_count'], equal_to(res_count))
        assert_that(
            data['items'],
            contains_inanyorder(*[
                has_entries({'invoice_eid': inv.external_id, 'client_id': c.id})
                for c, inv in [(client1, invoice1), (client2, invoice2)][:res_count]
            ]),
        )

    def test_client_ui_admin_user(self, admin_role, view_inv_role):
        """В КИ ищем счета под админом. Да, так можно."""
        session = self.test_session
        roles = [
            admin_role,
            (view_inv_role, {cst.ConstraintTypes.firm_id: cst.FirmId.YANDEX_OOO}),
        ]
        security.set_roles(roles)

        from_dt = session.now()
        invoice1 = create_custom_invoice(firm_id=cst.FirmId.YANDEX_OOO)
        invoice2 = create_custom_invoice(firm_id=cst.FirmId.CLOUD)
        session.flush()
        to_dt = session.now()

        response = self.test_client.get(
            self.BASE_API,
            {
                'from_dt': from_dt.strftime('%Y-%m-%dT%H:%M:%S'),
                'to_dt': to_dt.strftime('%Y-%m-%dT%H:%M:%S'),
            },
            is_admin=False,
        )
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        data = response.get_json().get('data')
        assert_that(data['total_row_count'], equal_to(1))
        assert_that(data['items'], has_item(has_entry('invoice_eid', invoice1.external_id)))
        assert_that(data['items'], not_(has_item(has_entry('invoice_eid', invoice2.external_id))))

    @pytest.mark.parametrize(
        'hidden',
        [True, None],
    )
    def test_hidden_role_client(self, admin_role, view_inv_role, hidden):
        session = self.test_session
        role_client = create_role_client()
        role_client.hidden = hidden
        roles = [
            admin_role,
            (view_inv_role, {cst.ConstraintTypes.client_batch_id: role_client.client_batch_id}),
        ]
        security.set_roles(roles)

        invoice = create_custom_invoice(client=role_client.client)
        session.flush()
        res = self.test_client.get(
            self.BASE_API,
            params={'client_id': role_client.client.id},
        )
        assert_that(res.status_code, equal_to(http.OK))

        data = res.get_json().get('data', {})
        res_match = contains(has_entry('invoice_eid', invoice.external_id)) if hidden is None else empty()
        assert_that(data.get('items', []), res_match)


@pytest.mark.smoke
class TestCaseXLSInvoiceList(TestCaseApiAppBase):
    BASE_API = '/v1/invoice/list/xls'

    @pytest.mark.parametrize(
        'test_fname, expected_fname_in_context',
        [
            ('тестовое_имя_файла.xls', 'filename*=UTF-8\'\'' + url_quote('тестовое_имя_файла.xls')),
            ('тестовое_имя_файла', 'filename*=UTF-8\'\'' + url_quote('тестовое_имя_файла.xls')),
            ('test_name.xls', 'filename=test_name.xls'),
            ('test_name', 'filename=test_name.xls'),
            (None, 'filename=invoices.xls'),
        ],
    )
    def test_load_xls_file(self, mocker, invoice_with_endbuyer, test_fname, expected_fname_in_context):
        invoice = invoice_with_endbuyer

        params = clean_dict({
            'invoice_eid': invoice.external_id.encode('utf-8'),
            'filename': test_fname and test_fname.encode('utf-8'),
        })

        response = self.test_client.get(self.BASE_API, params)
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')
        headers = response.headers
        assert_that(response.content_type, equal_to(enums.Mimetype.XLS.value))
        assert_that(
            headers,
            has_items(
                contains(
                    'Content-Disposition',
                    contains_string(expected_fname_in_context),
                ),
                contains('Content-Type', enums.Mimetype.XLS.value),
            ),
        )
