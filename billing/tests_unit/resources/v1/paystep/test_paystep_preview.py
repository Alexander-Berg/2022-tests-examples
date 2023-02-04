# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import datetime
import pytest
import mock
import contextlib
import hamcrest as hm
import http.client as http
from decimal import Decimal as D

from butils.decimal_unit import DecimalUnit as DU
from balance import constants as cst, mapper, core
from balance.discounts.bases import DiscountProof
from balance.actions import promocodes as promo_actions
from tests import object_builder as ob

from brest.core.tests import security

from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client, create_agency, create_role_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_role
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.person import create_person
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import create_request, DEFAULT_ORDER_QTY, create_invoice, create_overdraft_invoice
from yb_snout_api.tests_unit.fixtures.contract import create_credit_contract
from yb_snout_api.tests_unit.fixtures.promocode import create_legacy_promocode
from yb_snout_api.tests_unit.fixtures.common import create_paysys


@pytest.fixture(name='view_inv_role')
def create_view_inv_role():
    return create_role(
        (
            cst.PermissionCode.VIEW_INVOICES,
            {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
        ),
    )


@pytest.fixture(name='issue_inv_role')
def create_issue_inv_role():
    return create_role(
        (
            cst.PermissionCode.ISSUE_INVOICES,
            {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
        ),
    )


@pytest.fixture(name='alter_inv_role')
def create_alter_inv_role():
    return create_role(
        (
            cst.PermissionCode.ALTER_INVOICE_PAYSYS,
            {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
        ),
    )


def create_credit_invoice(
    session,
    contract,
    client,
    paysys,
    qty,
    closed=False,
):
    request = ob.RequestBuilder.construct(
        session,
        basket=ob.BasketBuilder(
            client=contract.client,
            rows=[ob.BasketItemBuilder(
                quantity=qty,
                order=ob.OrderBuilder(
                    client=client,
                    agency=contract.client,
                    product=ob.Getter(mapper.Product, cst.DIRECT_PRODUCT_RUB_ID),
                ),
            )],
        ),
    )
    inv, = core.Core(session).pay_on_credit(request.id, paysys.id, contract.person_id, contract.id)
    if closed:
        inv.close_invoice(session.now())
    return inv


@contextlib.contextmanager
def _patch_discounts(discount_proofs):
    from balance import discounts
    old_func = discounts.calc_from_ns
    discounts.calc_from_ns = lambda ns: discount_proofs

    yield

    discounts.calc_from_ns = old_func


def assert_base_ur_case(data, request_, person_id):
    hm.assert_that(
        data,
        hm.has_entries({
            'request': hm.has_entries({
                'id': request_.id,
                'client': hm.has_entries({
                    'id': request_.client.id,
                    'name': request_.client.name,
                }),
                'is_available_promo_codes': True,
                'direct_payment': request_.direct_payment,
                'total_qty': '30',
                'is_unmoderated': False,
            }),
            'invoice': None,
            'rows': hm.contains_inanyorder(*[
                hm.has_entries({
                    'order': hm.has_entries({
                        'id': r.order.id,
                        'service_id': r.order.service_id,
                        'service_order_id': r.order.service_order_id,
                        'is_unmoderated': None,
                    }),
                    'qty': '10',
                    'discount_pct': '0.00',
                    'sum': '1000.00',
                    'sum_wo_discount': '1000.00',
                    'qty_wo_discount': '10',
                    'product_type_cc': 'Bucks',
                    'unit_name': u'у.е.',
                    'discounts': hm.empty(),
                    'type_rate': '1',
                })
                for r in request_.request_orders
            ]),
            'payment_totals': hm.has_entries({
                'nds_sum': '500.01',
                'nds_pct': '20',
                'sum': '3000.00',
                'discount_pct': '0',
                'qty': '30',
                'sum_wo_discount': '3000.00',
                'iso_currency': 'RUB',
                'qty_w_discount': '30',
            }),
            'payment_params': hm.has_entries({
                'postpay': False,
                'credit': 0,
                'dt': hm.not_none(),
                'is_for_single_account': False,
                'money_product': False,
            }),
            'promocode_info': None,
            'payment_choice': hm.has_entries({
                'payment_method': hm.has_entries({
                    'payment_limit': None,
                    'is_trust_api': False,
                    'code': 'bank',
                    'id': 1001,
                    'name': 'Bank Payment',
                }),
                'contract': None,
                'paysys_group_id': 0,
                'firm_id': 1,
                'person': hm.has_entries({
                    'id': person_id,
                    'type': 'ur',
                    'type_name': 'ID_Legal_entity_or_Indiv_entrepr',
                    'region_id': 225,
                    'inn': None,
                    'invalid_address': False,
                }),
                'iso_currency': 'RUB',
                'trust_paymethods': None,
                'offer_id': 38,
            }),
            'is_auto_overdraft_available': None,
            'overdraft_info': hm.has_entries({
                'available_sum': '0.00',
                'spent_sum': '0.00',
                'iso_currency': 'RUB',
                'is_available': False,
                'expired_sum': '0.00',
            }),
            'credit_info': hm.has_entries({
                'available_sum': '0',
                'spent_sum': '0',
                'is_available': False,
                'expired_sum': '0',
            }),
            'need_ticket_id': False,
        }),
    )


@pytest.mark.smoke
class TestPaystepPreview(TestCaseApiAppBase):
    BASE_API = '/v1/paystep/preview'
    domain = 'snout.core.yandex.com'

    def test_get_request(self, client):
        security.set_roles([])
        security.set_passport_client(client)
        request_ = create_request(client=client)
        res = self.test_client.get(
            self.BASE_API,
            {
                'request_id': request_.id,
                'firm_id': cst.FirmId.YANDEX_OOO,
                'iso_currency': 'RUB',
                'payment_method_id': cst.PaymentMethodIDs.bank,
                'paysys_group_id': cst.PaysysGroupIDs.default,
            },
            is_admin=False,
            headers={'HOST': self.domain},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'request': hm.has_entries({
                    'id': request_.id,
                    'client': hm.has_entries({
                        'id': request_.client.id,
                        'name': request_.client.name,
                    }),
                    'is_available_promo_codes': True,
                    'is_unmoderated': False,
                    'direct_payment': request_.direct_payment,
                    'total_qty': '30',
                }),
                'rows': hm.contains_inanyorder(*[
                    hm.has_entries({
                        'order': hm.has_entries({
                            'id': r.order.id,
                            'service_id': r.order.service_id,
                            'service_order_id': r.order.service_order_id,
                            'product_iso_currency': None,
                        }),
                        'qty': '10',
                        'qty_wo_discount': '10',
                        'sum': '0',
                        'sum_wo_discount': '0',
                        'discount_pct': '0',
                        'unit_name': u'у.е.',
                        'product_id': r.order.product.id,
                        'text': 'Test Product',
                        'product_type_cc': 'Bucks',
                        'type_rate': '1',
                    })
                    for r in request_.request_orders
                ]),
            }),
        )

    def test_preview_w_payment(self, client):
        security.set_roles([])
        security.set_passport_client(client)
        request_ = create_request(client=client)
        person = create_person(client=request_.client, type='ur')
        res = self.test_client.get(
            self.BASE_API,
            {
                'request_id': request_.id,
                'person_id': person.id,
                'firm_id': cst.FirmId.YANDEX_OOO,
                'iso_currency': 'RUB',
                'payment_method_id': cst.PaymentMethodIDs.bank,
                'paysys_group_id': cst.PaysysGroupIDs.default,
            },
            headers={'HOST': self.domain},
            is_admin=False,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        assert_base_ur_case(data, request_, person.id)

    def test_preview_w_payment_wo_person(self, client):
        security.set_roles([])
        security.set_passport_client(client)
        request_ = create_request(client=client)
        res = self.test_client.get(
            self.BASE_API,
            {
                'request_id': request_.id,
                'person_type': 'ur',
                'firm_id': cst.FirmId.YANDEX_OOO,
                'iso_currency': 'RUB',
                'payment_method_id': cst.PaymentMethodIDs.bank,
                'paysys_group_id': cst.PaysysGroupIDs.default,
            },
            headers={'HOST': self.domain},
            is_admin=False,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        assert_base_ur_case(data, request_, None)

    def test_fixed_discount(self, agency):
        session = self.test_session
        security.set_roles([])
        security.set_passport_client(agency)

        pc = ob.PayOnCreditCase(session)
        prod = pc.get_product_hierarchy(media_discount=1)
        prod[1]._other.price.b.tax, prod[1]._other.price.b.price = 0, 1000
        person = create_person(client=agency, type='ur')
        contract = pc.get_contract(
            client=agency,
            person=person,
            commission=7,
            payment_type=3,
            credit_type=1,
            payment_term=30,
            services={cst.ServiceId.MKB},
            is_signed=True,
            firm=cst.FirmId.YANDEX_OOO,
            discount_fixed=15,
            turnover_forecast={
                prod[0].activity_type.id: 235000000,
                prod[1].activity_type.id: 235000000,
            },
        )
        order = ob.OrderBuilder.construct(
            session,
            product=prod[1],
            client=contract.client,
            agency=agency,
            service=ob.Getter(mapper.Service, cst.ServiceId.MKB),
        )
        request = ob.RequestBuilder.construct(
            session,
            basket=ob.BasketBuilder(
                rows=[
                    ob.BasketItemBuilder(
                        order=order,
                        quantity=10,
                    ),
                ],
            ),
        )

        res = self.test_client.get(
            self.BASE_API,
            {
                'request_id': request.id,
                'person_id': person.id,
                'contract_id': contract.id,
                'firm_id': cst.FirmId.YANDEX_OOO,
                'iso_currency': 'RUB',
                'payment_method_id': cst.PaymentMethodIDs.credit_card,
                'paysys_group_id': cst.PaysysGroupIDs.default,
            },
            headers={'HOST': self.domain},
            is_admin=False,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'request': hm.has_entries({
                    'id': request.id,
                    'total_qty': None,
                    'client': hm.has_entries({'id': agency.id, 'name': agency.name}),
                    'direct_payment': False,
                }),
                'rows': hm.contains(
                    hm.has_entries({
                        'order': hm.has_entries({'id': order.id}),
                        'product_id': prod[1].id,
                        'product_type_cc': 'Bucks',
                        'qty': '10',
                        'sum': '10200.00',
                        'sum_wo_discount': '12000.00',
                        'discount_pct': '15.00',
                        'discounts': hm.contains(
                            hm.has_entries({
                                'type': 'base',
                                'pct': '15.00',
                            }),
                        ),
                    }),
                ),
                'promocode_info': None,
                'payment_totals': hm.has_entries({
                    'nds_pct': '20',
                    'discount_pct': '15',
                    'agency_discount_proof': hm.has_entries({
                        'type_rate': '1',
                        'name': 'discount_fixed',
                        'classname': 'DiscountFixed',
                        'discount': '15',
                        'first_invoice': None,
                        'adjust_quantity': False,
                        'dt': hm.not_none(),
                        'type': 'fixed',
                        'without_taxes': True,
                    }),
                    'currency_rate': u'1',
                    'iso_currency': 'RUB',
                    'client_discount_proofs': hm.empty(),
                }),
                'payment_params': hm.has_entries({
                    'postpay': False,
                    'credit': 0,
                    'dt': hm.not_none(),
                }),
            }),
        )

    def test_budget_discount(self, request_):
        client = request_.client
        person = create_person(client=request_.client)

        discount_proofs = [
            None,
            DiscountProof(
                'mock',
                adjust_quantity=False,
                budget=DU('100', 'RUR'),
                budget_discount=D('10', '%'),
                classname='MockClass',
                currency='RUR',
                discount=D('10', '%'),
                name='mock_name',
                next_budget=DU('500', 'RUR'),
                next_discount=D('50', '%'),
                without_taxes=True,
                first_invoice=True,
            ),
            None,
        ]

        with _patch_discounts(discount_proofs):
            res = self.test_client.get(
                self.BASE_API,
                {
                    'request_id': request_.id,
                    'person_id': person.id,
                    'firm_id': cst.FirmId.YANDEX_OOO,
                    'iso_currency': 'RUB',
                    'payment_method_id': cst.PaymentMethodIDs.bank,
                    'paysys_group_id': cst.PaysysGroupIDs.default,
                },
                headers={'HOST': self.domain},
                is_admin=False,
            )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'request': hm.has_entries({
                    'id': request_.id,
                    'total_qty': '30',
                    'client': hm.has_entries({'id': client.id, 'name': client.name}),
                    'direct_payment': True,
                }),
                'rows': hm.contains(*[
                    hm.has_entries({
                        'order': hm.has_entries({'id': r.order.id}),
                        'product_type_cc': 'Bucks',
                        'qty': '10',
                        'sum': '900.00',
                        'sum_wo_discount': '1000.00',
                        'discount_pct': '10.00',
                        'discounts': hm.contains(
                            hm.has_entries({
                                'type': 'base',
                                'pct': '10.00',
                            }),
                        ),
                    })
                    for r in request_.request_orders
                ]),
                'promocode_info': None,
                'payment_totals': hm.has_entries({
                    'nds_pct': '20',
                    'discount_pct': '10',
                    'agency_discount_proof': hm.has_entries({
                        'name': 'mock_name',
                        'classname': 'MockClass',
                        'adjust_quantity': False,
                        'type': 'mock',
                        'without_taxes': True,
                        'next_budget': '500',
                        'budget': '100',
                        'budget_discount': '10',
                        'discount': '10',
                        'next_discount': '50',
                        'iso_currency': 'RUB',
                        'first_invoice': True,
                    }),
                    'currency_rate': u'1',
                    'iso_currency': 'RUB',
                    'client_discount_proofs': hm.empty(),
                }),
                'payment_params': hm.has_entries({
                    'postpay': False,
                    'credit': 0,
                    'dt': hm.not_none(),
                }),
            }),
        )

    def test_client_discount_proof(self, client):
        security.set_roles([])
        security.set_passport_client(client)
        request_ = create_request(client=client)
        person = create_person(client=client)

        discount_proofs = [
            DiscountProof('mock', discount=D('33'), adjust_quantity=False),
            None, None
        ]

        with _patch_discounts(discount_proofs):
            res = self.test_client.get(
                self.BASE_API,
                {
                    'request_id': request_.id,
                    'person_id': person.id,
                    'firm_id': cst.FirmId.YANDEX_OOO,
                    'iso_currency': 'RUB',
                    'payment_method_id': cst.PaymentMethodIDs.bank,
                    'paysys_group_id': cst.PaysysGroupIDs.default,
                },
                headers={'HOST': self.domain},
                is_admin=False,
            )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'request': hm.has_entries({
                    'id': request_.id,
                    'total_qty': '30',
                    'direct_payment': True,
                }),
                'rows': hm.contains(*[
                    hm.has_entries({
                        'qty': '10',
                        'sum': '670.00',
                        'sum_wo_discount': '1000.00',
                        'discount_pct': '33.00',
                        'discounts': hm.contains(
                            hm.has_entries({
                                'type': 'base',
                                'pct': '33.00',
                            }),
                        ),
                    })
                    for _i in range(3)
                ]),
                'promocode_info': None,
                'payment_totals': hm.has_entries({
                    'nds_pct': '20',
                    'discount_pct': '0',
                    'agency_discount_proof': None,
                    'currency_rate': u'1',
                    'iso_currency': 'RUB',
                    'client_discount_proofs': hm.contains(
                        hm.has_entries({
                            'discount_proof': hm.has_entries({
                                'adjust_quantity': False,
                                'discount': '33',
                                'type': 'mock',
                            }),
                            'client': hm.has_entries({
                                'id': request_.client.id,
                                'name': request_.client.name,
                            }),
                        }),
                    ),
                }),
                'payment_params': hm.has_entries({
                    'postpay': False,
                    'credit': 0,
                    'dt': hm.not_none(),
                }),
            }),
        )

    def test_create_credit_info_invoice(self):
        agency = create_agency()
        client = create_client(agency=agency)
        person = create_person(client=agency)

        contract = create_credit_contract(
            client=agency,
            person=person,
            credit_limit_single=1000000,
            personal_account_fictive=False,
            personal_account=True,
        )

        # кредитный счет по этому договору уже есть
        paysys = create_paysys(contract.firm.id)
        create_credit_invoice(self.test_session, contract, client, paysys, 1200, closed=True)
        create_credit_invoice(self.test_session, contract, client, paysys, 250000)

        request = create_request(agency=agency, client=client, firm_id=contract.col0.firm)
        request_params = {
            'request_id': request.id,
            'person_id': person.id,
            'contract_id': contract.id,
            'firm_id': request.firm_id,
            'iso_currency': 'RUB',
            'payment_method_id': cst.PaymentMethodIDs.bank,
            'paysys_group_id': cst.PaysysGroupIDs.default
        }
        res = self.test_client.get(self.BASE_API, request_params, headers={'HOST': self.domain})
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'request': hm.has_entries({
                    'id': request.id,
                    'total_qty': '30',
                    'direct_payment': True,
                }),
                'payment_totals': hm.has_entries({
                    'nds_sum': '500.01',
                    'nds_pct': '20',
                    'sum': '3000.00',
                    'discount_pct': '0',
                    'qty': '30',
                    'sum_wo_discount': '3000.00',
                    'currency_rate': u'1',
                    'iso_currency': 'RUB',
                    'qty_w_discount': '30',
                    'agency_discount_proof': None,
                    'client_discount_proofs': hm.empty(),
                }),
                'payment_params': hm.has_entries({
                    'postpay': False,
                    'payment_term_dt': None,
                    'credit': 2,
                    'is_for_single_account': False,
                    'dt': hm.not_none(),
                    'money_product': False,
                }),
                'credit_info': hm.has_entries({
                    'available_sum': '750000.00',
                    'credit_sum': '1000000.00',
                    'spent_sum': '1200.00',
                    'iso_currency': 'RUB',
                    'is_available': True,
                    'expired_sum': '0',
                    'nearly_expired_sum': '1200.00',
                    'current_limit_expired_sum': '0',
                    'credit_only': True,
                }),
                'overdraft_info': None,
                'promocode_info': None,
            }),
        )

    @pytest.mark.parametrize(
        'apply_on_create',
        [True, False],
    )
    def test_credit_w_promocode(self, client, person, apply_on_create):
        legacy_promocode = create_legacy_promocode(
            calc_params={'adjust_quantity': True, 'discount_pct': '66', 'minimal_qty': 11},
            minimal_amounts={'RUB': '6.66'},
            apply_on_create=apply_on_create,
        )
        promo_actions.reserve_promo_code(client, legacy_promocode)

        contract = create_credit_contract(
            client=client,
            person=person,
            credit_limit_single=1000000,
            personal_account_fictive=False,
            personal_account=True,
        )
        request = create_request(client=contract.client, firm_id=contract.col0.firm)
        request_params = {
            'request_id': request.id,
            'person_id': contract.person.id,
            'contract_id': contract.id,
            'firm_id': request.firm_id,
            'iso_currency': 'RUB',
            'payment_method_id': cst.PaymentMethodIDs.bank,
            'paysys_group_id': cst.PaysysGroupIDs.default
        }
        res = self.test_client.get(self.BASE_API, request_params, headers={'HOST': self.domain})
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'credit_info': hm.has_entries({
                    'available_sum': '1000000.00',
                    'spent_sum': '0',
                    'iso_currency': 'RUB',
                    'is_available': True,
                    'expired_sum': '0',
                }),
                'promocode_info': hm.has_entries({
                    'promocode': hm.has_entries({
                        'code': legacy_promocode.code,
                        'apply_type': cst.PromocodeApplyTypes.ON_CREATE if apply_on_create else cst.PromocodeApplyTypes.ON_TURN_ON,
                        'id': legacy_promocode.id,
                    }),
                    'reservation': hm.has_entries({
                        'client_id': request.client.id,
                    }),
                    'deny_reason': hm.has_entries({
                        'code': 400,
                        'error': 'INVALID_PC_WITH_CREDIT',
                        # 'description': 'Invalid promo code: ID_PC_WITH_CREDIT',
                    }),
                }),
            }),
        )

    @pytest.mark.parametrize(
        'person_type, has_role, region_config, skip_sms',
        [
            pytest.param('ph', True, 0, 'skip_by_permission', id='ph w permission'),
            pytest.param('ur', True, 0, 'skip_by_permission', id='w permission'),
            pytest.param('ph', False, 0, None, id='ph config off'),
            pytest.param('ur', False, 0, 'skip_by_region', id='config off'),
            pytest.param('ur', False, 1, None, id='config on'),
            pytest.param('ur', False, [cst.RegionId.RUSSIA], None, id='region in config'),
            pytest.param('ur', False, [cst.RegionId.ARMENIA], 'skip_by_region', id='region not in config'),
        ],
    )
    def test_skip_sms_notification_for_credit(
        self,
        issue_inv_role,
        client,
        person_type,
        has_role,
        region_config,
        skip_sms,
    ):
        roles = []
        if has_role:
            roles = [issue_inv_role]
        security.set_roles(roles)
        security.set_passport_client(client)

        client.region_id = cst.RegionId.RUSSIA
        self.test_session.config.__dict__['SAUTH_REQUIRED_REGIONS'] = region_config

        contract = create_credit_contract(
            client=client,
            person=create_person(client=client, type=person_type),
            credit_limit_single=1000000,
            personal_account_fictive=False,
            personal_account=True,
        )
        request = create_request(client=contract.client, firm_id=contract.col0.firm)
        request_params = {
            'request_id': request.id,
            'person_id': contract.person.id,
            'contract_id': contract.id,
            'firm_id': request.firm_id,
            'iso_currency': 'RUB',
            'payment_method_id': cst.PaymentMethodIDs.bank,
            'paysys_group_id': cst.PaysysGroupIDs.default
        }
        res = self.test_client.get(self.BASE_API, request_params, headers={'HOST': self.domain}, is_admin=False)
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'credit_info': hm.has_entries({
                    'available_sum': '1000000.00',
                    'spent_sum': '0',
                    'iso_currency': 'RUB',
                    'is_available': True,
                    'expired_sum': '0',
                    'skip_sms_notification': skip_sms,
                }),
            }),
        )

    @pytest.mark.parametrize(
        'is_available',
        [True, False],
    )
    def test_create_overdraft_invoice(self, client, is_available):
        client.set_currency(cst.ServiceId.DIRECT, 'RUB', self.test_session.now(), cst.CONVERT_TYPE_COPY)
        client.set_overdraft_limit(cst.ServiceId.DIRECT, cst.FirmId.YANDEX_OOO, 3000 if is_available else 1000, 'RUB')
        person = create_person(client)
        request = create_request(client=client, firm_id=cst.FirmId.YANDEX_OOO)
        request_params = {
            'request_id': request.id,
            'person_id': person.id,
            'firm_id': request.firm_id,
            'iso_currency': 'RUB',
            'payment_method_id': cst.PaymentMethodIDs.credit_card,
            'paysys_group_id': cst.PaysysGroupIDs.default,
        }
        res = self.test_client.get(self.BASE_API, request_params, headers={'HOST': self.domain})
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'request': hm.has_entries({
                    'id': request.id,
                    'total_qty': '30',
                    'direct_payment': True,
                }),
                'payment_totals': hm.has_entries({
                    'nds_sum': '500.01',
                    'nds_pct': '20',
                    'sum': '3000.00',
                    'discount_pct': '0',
                    'qty': '30',
                    'sum_wo_discount': '3000.00',
                    'currency_rate': u'1',
                    'iso_currency': 'RUB',
                    'qty_w_discount': '30',
                }),
                'payment_params': hm.has_entries({
                    'postpay': False,
                    'payment_term_dt': None,
                    'credit': 0,
                    'is_for_single_account': False,
                    'dt': hm.not_none(),
                    'money_product': False,
                }),
                'credit_info': hm.has_entries({
                    'available_sum': '0',
                    'spent_sum': '0',
                    'is_available': False,
                    'expired_sum': '0',
                }),
                'overdraft_info': hm.has_entries({
                    'available_sum': '3000.00' if is_available else '1000.00',
                    'spent_sum': '0.00',
                    'iso_currency': 'RUB',
                    'is_available': is_available,
                    'expired_sum': '0.00',
                }),
            }),
        )

    def test_second_overdraft(self, client):
        client.set_currency(cst.ServiceId.DIRECT, 'RUB', self.test_session.now(), cst.CONVERT_TYPE_COPY)
        client.set_overdraft_limit(cst.ServiceId.DIRECT, cst.FirmId.YANDEX_OOO, 100500, 'RUB')
        person = create_person(client)
        inv = create_overdraft_invoice(client=client, person=person)
        inv.turn_on_rows()
        request = create_request(client=client, firm_id=cst.FirmId.YANDEX_OOO)
        request_params = {
            'request_id': request.id,
            'person_id': person.id,
            'firm_id': request.firm_id,
            'iso_currency': 'RUB',
            'payment_method_id': cst.PaymentMethodIDs.credit_card,
            'paysys_group_id': cst.PaysysGroupIDs.default,
        }
        res = self.test_client.get(self.BASE_API, request_params, headers={'HOST': self.domain})
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'overdraft_info': hm.has_entries({
                    'skip_sms_notification': 'skip_by_permission',
                    'is_present': True,
                    'spent_sum': '1000.00',
                    'overdraft_sum': '100500.00',
                    'is_available': False,
                    'available_sum': '99500.00',
                    'iso_currency': 'RUB',
                    'expired_sum': '1000.00',
                }),
            }),
        )

    @pytest.mark.parametrize(
        'apply_on_create',
        [True, False],
    )
    def test_overdraft_w_promocode(self, client, apply_on_create):
        legacy_promocode = create_legacy_promocode(
            calc_params={'adjust_quantity': True, 'discount_pct': '66', 'minimal_qty': 11},
            minimal_amounts={'RUB': '6.66'},
            apply_on_create=apply_on_create,
        )
        promo_actions.reserve_promo_code(client, legacy_promocode)

        client.set_currency(cst.ServiceId.DIRECT, 'RUB', self.test_session.now(), cst.CONVERT_TYPE_COPY)
        client.set_overdraft_limit(cst.ServiceId.DIRECT, cst.FirmId.YANDEX_OOO, 3000, 'RUB')
        person = create_person(client)
        request = create_request(client=client, firm_id=cst.FirmId.YANDEX_OOO)
        request_params = {
            'request_id': request.id,
            'person_id': person.id,
            'firm_id': request.firm_id,
            'iso_currency': 'RUB',
            'payment_method_id': cst.PaymentMethodIDs.credit_card,
            'paysys_group_id': cst.PaysysGroupIDs.default,
        }
        res = self.test_client.get(self.BASE_API, request_params, headers={'HOST': self.domain})
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'promocode_info': hm.has_entries({
                    'promocode': hm.has_entries({
                        'code': legacy_promocode.code,
                        'apply_type': cst.PromocodeApplyTypes.ON_CREATE if apply_on_create else cst.PromocodeApplyTypes.ON_TURN_ON,
                        'id': legacy_promocode.id,
                    }),
                    'reservation': hm.has_entries({
                        'begin_dt': hm.not_none(),
                        'end_dt': hm.not_none(),
                        'client_id': request.client.id,
                    }),
                }),
                'overdraft_info': hm.has_entries({
                    'is_present': False,
                    'is_available': False,
                    'available_sum': '0',
                    'spent_sum': '0',
                }),
            }),
        )

    @pytest.mark.parametrize(
        'person_type, has_role, region_config, skip_sms',
        [
            pytest.param('ph', False, 1, None, id='ph'),
            pytest.param('ph', True, 0, 'skip_by_permission', id='ph w permission'),
            pytest.param('ur', True, 0, 'skip_by_permission', id='w permission'),
            pytest.param('ph', False, 0, None, id='ph config off'),
            pytest.param('ur', False, 0, 'skip_by_region', id='config off'),
            pytest.param('ur', False, 1, None, id='config on'),
            pytest.param('ur', False, [cst.RegionId.RUSSIA], None, id='region in config'),
            pytest.param('ur', False, [cst.RegionId.ARMENIA], 'skip_by_region', id='region not in config'),
        ],
    )
    def test_skip_sms_notification_for_overdraft(
        self,
        issue_inv_role,
        person_type,
        has_role,
        region_config,
        skip_sms,
    ):
        client = create_client(region_id=cst.RegionId.RUSSIA)
        client.set_currency(cst.ServiceId.DIRECT, 'RUB', self.test_session.now(), cst.CONVERT_TYPE_COPY)
        client.set_overdraft_limit(cst.ServiceId.DIRECT, cst.FirmId.YANDEX_OOO, 3000, 'RUB')
        person = create_person(client, type=person_type)
        request = create_request(client=client, firm_id=cst.FirmId.YANDEX_OOO)

        roles = []
        if has_role:
            roles.append(issue_inv_role)
        security.set_passport_client(client)
        security.set_roles(roles)

        self.test_session.config.__dict__['SAUTH_REQUIRED_REGIONS'] = region_config

        request_params = {
            'request_id': request.id,
            'person_id': person.id,
            'firm_id': request.firm_id,
            'iso_currency': 'RUB',
            'payment_method_id': cst.PaymentMethodIDs.credit_card,
            'paysys_group_id': cst.PaysysGroupIDs.default,
        }
        res = self.test_client.get(
            self.BASE_API,
            request_params,
            headers={'HOST': self.domain},
            is_admin=False,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'request': hm.has_entries({
                    'id': request.id,
                    'total_qty': '30',
                    'direct_payment': True,
                }),
                'overdraft_info': hm.has_entries({
                    'available_sum': '3000.00',
                    'spent_sum': '0.00',
                    'iso_currency': 'RUB',
                    'is_available': True,
                    'expired_sum': '0.00',
                    'skip_sms_notification': skip_sms,
                }),
            }),
        )

    @pytest.mark.parametrize(
        'cookie_val, is_valid',
        [
            ('123.666.valid_cookie', True),
            ('456.777.invalid_cookie', False),
        ],
    )
    def test_overdraft_cookie_and_verification_code(self, client, cookie_val, is_valid):
        security.set_roles([])
        security.set_passport_client(client)

        person = create_person(client, type='ph')
        request = create_request(client=client)

        self.test_client.set_cookie('', b'yb_paystep', cookie_val)
        with mock.patch('muzzle.security.sauth._get_secret_key', return_value='123.666.valid_cookie'):
            request_params = {
                'request_id': request.id,
                'person_id': person.id,
                'firm_id': request.firm_id,
                'iso_currency': 'RUB',
                'payment_method_id': cst.PaymentMethodIDs.credit_card,
                'paysys_group_id': cst.PaysysGroupIDs.default,
            }
            res = self.test_client.get(
                self.BASE_API,
                request_params,
                headers={'HOST': self.domain},
                is_admin=False,
            )
            hm.assert_that(res.status_code, hm.equal_to(http.OK))
        cond = hm.has_item(
            hm.contains(
                'Set-Cookie',
                hm.contains_string(
                    'yb_paystep=; Domain=.core.yandex.com; Expires=Thu, 01-Jan-1970 00:00:00 GMT; Secure; Path=/'),
            ),
        )
        if is_valid:
            cond = hm.not_(cond)
        hm.assert_that(res.headers, cond)

    @pytest.mark.promo_code
    @pytest.mark.parametrize(
        'apply_on_create',
        [True, False],
    )
    @pytest.mark.parametrize(
        'direct_requested',
        [True, False],
    )
    def test_promocode(self, client, apply_on_create, direct_requested):
        legacy_promocode = create_legacy_promocode(
            calc_params={'adjust_quantity': True, 'discount_pct': '66', 'minimal_qty': 11},
            minimal_amounts={'RUB': '6.66'},
            apply_on_create=apply_on_create,
        )
        promo_actions.reserve_promo_code(client, legacy_promocode)
        person = create_person(client)
        request = create_request(client=client, firm_id=cst.FirmId.YANDEX_OOO)
        request_params = {
            'request_id': request.id,
            'person_id': person.id,
            'firm_id': cst.FirmId.YANDEX_OOO,
            'iso_currency': 'RUB',
            'payment_method_id': cst.PaymentMethodIDs.credit_card,
            'paysys_group_id': cst.PaysysGroupIDs.default,
        }
        if direct_requested:
            request_params['promocode'] = legacy_promocode.code

        res = self.test_client.get(self.BASE_API, request_params, headers={'HOST': self.domain})
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'request': hm.has_entries({
                    'id': request.id,
                    'total_qty': '30',
                    'direct_payment': True,
                }),
                'rows': hm.contains_inanyorder(*[
                    hm.has_entries({
                        'order': hm.has_entries({'id': r.order.id}),
                        'qty': '29.411765',
                        'sum': '1000.00',
                        'sum_wo_discount': '2941.18',
                        'qty_wo_discount': '10',
                        'discount_pct': '66.00',
                        'discounts': hm.contains(hm.has_entries({'type': 'promocode', 'pct': '66.00'})),
                    })
                    for r in request.request_orders
                ]),
                'payment_totals': hm.has_entries({
                    'nds_sum': '500.01',
                    'nds_pct': '20',
                    'sum': '3000.00',
                    'qty': '30',
                    'sum_wo_discount': '8823.54',
                    'currency_rate': u'1',
                    'iso_currency': 'RUB',
                    'qty_w_discount': '88.235295',
                    'discount_pct': '0',
                }),
                'promocode_info': hm.has_entries({
                    'promocode': hm.has_entries({
                        'service_ids': hm.contains(cst.ServiceId.DIRECT),
                        'code': legacy_promocode.code,
                        'apply_type': cst.PromocodeApplyTypes.ON_CREATE if apply_on_create else cst.PromocodeApplyTypes.ON_TURN_ON,
                        'firm_id': cst.FirmId.YANDEX_OOO,
                        'params': hm.has_entries({'apply_on_create': apply_on_create, 'discount_pct': '66'}),
                        'adjust_quantity': True,
                        'type': 'FixedDiscountPromoCodeGroup',
                        'id': legacy_promocode.id,
                    }),
                    'reservation': hm.has_entries({
                        'begin_dt': hm.not_none(),
                        'end_dt': hm.not_none(),
                        'client_id': request.client.id,
                    }),
                    'bonus': None,
                    'minimal_money': '7.99',
                }),
            }),
        )

    @pytest.mark.promo_code
    def test_no_promocode(self, client):
        person = create_person(client)
        request = create_request(client=client, firm_id=cst.FirmId.YANDEX_OOO)
        request_params = {
            'request_id': request.id,
            'person_id': person.id,
            'firm_id': cst.FirmId.YANDEX_OOO,
            'iso_currency': 'RUB',
            'payment_method_id': cst.PaymentMethodIDs.credit_card,
            'paysys_group_id': cst.PaysysGroupIDs.default,
            'promocode': 'AAAA-BBBB-CCCC-DDDD',
        }
        res = self.test_client.get(self.BASE_API, request_params, headers={'HOST': self.domain})
        hm.assert_that(res.status_code, hm.equal_to(http.BAD_REQUEST))

        data = res.get_json()
        hm.assert_that(
            data,
            hm.has_entries({
                'error': 'PROMOCODE_NOT_FOUND',
                'description': 'Invalid promo code: ID_PC_UNKNOWN',
            }),
        )

    @pytest.mark.promo_code
    def test_promocode_wo_reservation(self, client):
        legacy_promocode = create_legacy_promocode()
        person = create_person(client)
        request = create_request(client=client, firm_id=cst.FirmId.YANDEX_OOO)

        request_params = {
            'request_id': request.id,
            'person_id': person.id,
            'firm_id': cst.FirmId.YANDEX_OOO,
            'iso_currency': 'RUB',
            'payment_method_id': cst.PaymentMethodIDs.credit_card,
            'paysys_group_id': cst.PaysysGroupIDs.default,
            'promocode': legacy_promocode.code,
        }
        res = self.test_client.get(self.BASE_API, request_params, headers={'HOST': self.domain})
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'promocode_info': hm.has_entries({
                    'promocode': hm.has_entries({
                        'id': legacy_promocode.id,
                        'code': legacy_promocode.code,
                        'type': 'FixedDiscountPromoCodeGroup',
                    }),
                    'reservation': None,
                    'bonus': None,
                    'deny_reason': hm.has_entries({
                        'code': 400,
                        'error': 'INVALID_PC_NON_RESERVED',
                        'description': 'Invalid promo code: ID_PC_NON_RESERVED',
                    }),
                }),
            }),
        )

    def test_already_reserved_promocode(self, client):
        legacy_promocode = create_legacy_promocode(
            calc_params={'adjust_quantity': True, 'discount_pct': '66', 'minimal_qty': 11},
            minimal_amounts={'RUB': '6.66'},
            apply_on_create=True,
        )
        promo_actions.reserve_promo_code(client, legacy_promocode)
        person = create_person(client)
        request = create_request(client=client, firm_id=cst.FirmId.YANDEX_OOO)

        request_params = {
            'request_id': request.id,
            'person_id': person.id,
            'firm_id': cst.FirmId.YANDEX_OOO,
            'iso_currency': 'RUB',
            'payment_method_id': cst.PaymentMethodIDs.credit_card,
            'paysys_group_id': cst.PaysysGroupIDs.default,
        }
        res = self.test_client.get(self.BASE_API, request_params, headers={'HOST': self.domain})
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'promocode_info': hm.has_entries({
                    'promocode': hm.has_entries({
                        'id': legacy_promocode.id,
                        'code': legacy_promocode.code,
                        'type': 'FixedDiscountPromoCodeGroup',
                    }),
                    'reservation': hm.has_entries({
                        'begin_dt': hm.not_none(),
                        'end_dt': hm.not_none(),
                        'client_id': client.id,
                    }),
                    'bonus': None,
                    'deny_reason': None,
                    'minimal_money': '7.99',
                }),
                'payment_totals': hm.has_entries({
                    'qty': '30',
                    'qty_w_discount': '88.235295',
                }),
            }),
        )

    def test_mindblow_w_2_promocodes(self, client):
        """Создаем и привязываем к клиенту 2 промокода.
        В запросе передаем тот, что к счету примениться не сможет из-за неправильной фирмы.
        В ответе должен быть другой промокод, который не был явно передан в запросе,
        но который применится к счету при создании.
        """
        legacy_promocode_1 = create_legacy_promocode(
            calc_params={'adjust_quantity': True, 'discount_pct': '66', 'minimal_qty': 11},
            minimal_amounts={'RUB': '6.66'},
            apply_on_create=True,
        )
        legacy_promocode_2 = create_legacy_promocode(
            calc_params={'adjust_quantity': True, 'discount_pct': '66', 'minimal_qty': 11},
            minimal_amounts={'RUB': '6.66'},
            apply_on_create=True,
            firm_id=cst.FirmId.MARKET,
        )
        promo_actions.reserve_promo_code(client, legacy_promocode_1)
        promo_actions.reserve_promo_code(client, legacy_promocode_2)
        person = create_person(client)
        request = create_request(client=client, firm_id=cst.FirmId.YANDEX_OOO)

        request_params = {
            'request_id': request.id,
            'person_id': person.id,
            'firm_id': cst.FirmId.YANDEX_OOO,
            'iso_currency': 'RUB',
            'payment_method_id': cst.PaymentMethodIDs.credit_card,
            'paysys_group_id': cst.PaysysGroupIDs.default,
            'promocode': legacy_promocode_2.code,
        }
        res = self.test_client.get(self.BASE_API, request_params, headers={'HOST': self.domain})
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'promocode_info': hm.has_entries({
                    'promocode': hm.has_entries({
                        'id': legacy_promocode_1.id,
                        'code': legacy_promocode_1.code,
                        'type': 'FixedDiscountPromoCodeGroup',
                    }),
                    'reservation': hm.has_entries({
                        'begin_dt': hm.not_none(),
                        'end_dt': hm.not_none(),
                        'client_id': client.id,
                    }),
                    'bonus': None,
                    'deny_reason': None,
                    'minimal_money': '7.99',
                }),
                'payment_totals': hm.has_entries({
                    'qty': '30',
                    'qty_w_discount': '88.235295',
                }),
            }),
        )

    @pytest.mark.promo_code
    def test_promocode_invalid(self, client):
        legacy_promocode = create_legacy_promocode(
            calc_params={'adjust_quantity': True, 'discount_pct': '66'},
            apply_on_create=True,
            firm_id=cst.FirmId.MARKET,
        )
        promo_actions.reserve_promo_code(client, legacy_promocode)
        person = create_person(client)
        request = create_request(client=client, firm_id=cst.FirmId.YANDEX_OOO)
        request_params = {
            'request_id': request.id,
            'person_id': person.id,
            'firm_id': cst.FirmId.YANDEX_OOO,
            'iso_currency': 'RUB',
            'payment_method_id': cst.PaymentMethodIDs.credit_card,
            'paysys_group_id': cst.PaysysGroupIDs.default,
            'promocode': legacy_promocode.code,
        }
        res = self.test_client.get(self.BASE_API, request_params, headers={'HOST': self.domain})
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'promocode_info': hm.has_entries({
                    'promocode': hm.has_entries({
                        'id': legacy_promocode.id,
                        'code': legacy_promocode.code,
                        'type': 'FixedDiscountPromoCodeGroup',
                    }),
                    'reservation': hm.has_entries({'client_id': client.id}),
                    'bonus': None,
                    'deny_reason': hm.has_entries({
                        'code': 400,
                        'error': 'INVALID_PC_FIRM_MISMATCH',
                        'description': 'Invalid promo code: ID_PC_FIRM_MISMATCH',
                    }),
                }),
                'payment_totals': hm.has_entries({
                    'discount_pct': '0',
                    'qty': '30',
                    'qty_w_discount': '30',
                }),
            }),
        )

    def test_legacy_promocode(self, client):
        now = self.test_session.now()
        legacy_promocode = create_legacy_promocode(
            calc_class_name='LegacyPromoCodeGroup',
            calc_params={
                "bonus1": "500",
                "service_ids": [7],
                "middle_dt": (now - datetime.timedelta(days=5)).isoformat(),
                "bonus2": "500",
                "multicurrency_bonuses": {"RUB": {"bonus1": 15000, "bonus2": 15000}},
                "discount_pct": "0",
            },
            start_dt=(now - datetime.timedelta(days=5)),
            end_dt=(now + datetime.timedelta(days=5)),
            service_ids=[cst.ServiceId.DIRECT],
        )
        promo_actions.reserve_promo_code(client, legacy_promocode)
        person = create_person(client)
        request = create_request(client=client, firm_id=cst.FirmId.YANDEX_OOO)
        request_params = {
            'request_id': request.id,
            'person_id': person.id,
            'firm_id': cst.FirmId.YANDEX_OOO,
            'iso_currency': 'RUB',
            'payment_method_id': cst.PaymentMethodIDs.credit_card,
            'paysys_group_id': cst.PaysysGroupIDs.default,
            'promocode': legacy_promocode.code,
        }
        res = self.test_client.get(self.BASE_API, request_params, headers={'HOST': self.domain})
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'promocode_info': hm.has_entries({
                    'promocode': hm.has_entries({
                        'service_ids': hm.contains(cst.ServiceId.DIRECT),
                        'code': legacy_promocode.code,
                        'apply_type': cst.PromocodeApplyTypes.ON_TURN_ON,
                        'firm_id': cst.FirmId.YANDEX_OOO,
                        'params': hm.has_entries({
                            "bonus1": "500",
                            "service_ids": [7],
                            "middle_dt": hm.not_none(),
                            "bonus2": "500",
                            "multicurrency_bonuses": hm.has_entries({"RUB": hm.has_entries({"bonus1": 15000, "bonus2": 15000})}),
                            "discount_pct": "0",
                        }),
                        'adjust_quantity': True,
                        'type': 'LegacyPromoCodeGroup',
                        'id': legacy_promocode.id,
                    }),
                    'reservation': hm.has_entries({
                        'begin_dt': hm.not_none(),
                        'end_dt': hm.not_none(),
                        'client_id': request.client.id,
                    }),
                    'bonus': '50000.00',
                    'minimal_money': '0',
                }),
            }),
        )

    def test_alter_invoice(self):
        invoice = create_invoice(dt=datetime.datetime(2021, 9, 1, 9, 30, 0), order_count=1, turn_on=True)
        order = invoice.consumes[0].order
        order.calculate_consumption(
            dt=datetime.datetime.today() - datetime.timedelta(days=1),
            stop=0,
            shipment_info={'Bucks': 1},
        )
        act, = invoice.generate_act(force=True)
        person = create_person(client=invoice.client)

        res = self.test_client.get(
            self.BASE_API,
            {
                'invoice_id': invoice.id,
                'person_id': person.id,
                'firm_id': cst.FirmId.YANDEX_OOO,
                'iso_currency': 'RUB',
                'payment_method_id': cst.PaymentMethodIDs.bank,
                'paysys_group_id': cst.PaysysGroupIDs.default,
            },
            headers={'HOST': self.domain},
            is_admin=False,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'invoice': hm.has_entries({
                    'id': invoice.id,
                    'postpay': False,
                    'external_id': invoice.external_id,
                }),
                'request': hm.has_entries({
                    'id': invoice.request_id,
                    'total_qty': '10',
                    'client': hm.has_entries({'id': invoice.client.id}),
                }),
                'payment_choice': hm.has_entries({
                    'person': hm.has_entries({'id': person.id}),
                }),
                'payment_params': hm.has_entries({
                    'postpay': False,
                    'payment_term_dt': None,
                    'credit': 0,
                    'is_for_single_account': False,
                    'dt': '2021-09-01T09:30:00',
                    'money_product': False,
                }),
                'payment_totals': hm.has_entries({
                    'nds_sum': '166.67',
                    'nds_pct': '20',
                    'sum': '1000.00',
                    'qty': '10',
                    'iso_currency': 'RUB',
                    'currency_rate': u'1',
                }),
                'need_ticket_id': True,
            }),
        )

    def test_alter_y_invoice(self, client, view_inv_role, issue_inv_role, alter_inv_role):
        session = self.test_session
        security.set_roles([view_inv_role, issue_inv_role, alter_inv_role])

        contract = create_credit_contract(
            client=client,
            commission=cst.ContractTypeId.NON_AGENCY,
            commission_type=None,
            credit_type=2,
            credit_limit_single=666666,
            personal_account=1,
            personal_account_fictive=1,
            client_limits=None,
            firm=cst.FirmId.YANDEX_OOO,
        )
        request = create_request(client)
        inv, = core.Core(session).pay_on_credit(request.id, 1003, contract.person_id, contract.id)
        inv.close_invoice(session.now())
        y_invoice = filter(lambda i: isinstance(i, mapper.YInvoice), contract.invoices)[0]

        res = self.test_client.get(
            self.BASE_API,
            {
                'invoice_id': y_invoice.id,
                'person_id': y_invoice.person.id,
                'contract_id': contract.id,
                'firm_id': cst.FirmId.YANDEX_OOO,
                'iso_currency': 'RUB',
                'payment_method_id': cst.PaymentMethodIDs.credit_card,
                'paysys_group_id': 0,
            },
            is_admin=False,
            headers={'HOST': self.domain},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'invoice': hm.has_entries({
                    'id': y_invoice.id,
                    'postpay': False,
                    'external_id': y_invoice.external_id,
                }),
                'request': hm.has_entries({
                    'id': y_invoice.request_id,
                    'total_qty': '0',
                    'client': hm.has_entries({'id': client.id}),
                }),
                'payment_choice': hm.has_entries({
                    'person': hm.has_entries({'id': y_invoice.person.id}),
                    'payment_method': hm.has_entries({'code': 'card'}),
                }),
                'payment_params': hm.has_entries({
                    'postpay': False,
                    'credit': 1,
                }),
                'payment_totals': hm.has_entries({
                    'nds_sum': '500.01',
                    'nds_pct': '20',
                    'sum': '3000.00',
                    'qty': '30',
                    'iso_currency': 'RUB',
                    'currency_rate': u'1',
                }),
                'rows': hm.contains_inanyorder(*[
                    hm.has_entries({
                        'sum': '1000.00',
                        'precision': 6,
                        'qty': '10',
                        # 'order': hm.has_entries({'id': io.order_id}),
                    })
                    for io in y_invoice.invoice_orders
                ]),
            }),
        )

    @pytest.mark.parametrize(
        'qty, res_qty',
        [
            ('10', '10'),
            ('10.555', '10.555'),
            ('10.5555555', '10.555556'),
            ('9.9999999', '10'),
        ],
    )
    def test_precision(self,qty, res_qty):
        request = create_request(order_qty=[D(qty)])
        person = create_person(client=request.client, type='ur')
        res = self.test_client.get(
            self.BASE_API,
            {
                'request_id': request.id,
                'person_id': person.id,
                'firm_id': cst.FirmId.YANDEX_OOO,
                'iso_currency': 'RUB',
                'payment_method_id': cst.PaymentMethodIDs.bank,
                'paysys_group_id': cst.PaysysGroupIDs.default,
            },
            is_admin=False,
            headers={'HOST': self.domain},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'request': hm.has_entries({
                    'id': request.id,
                    'total_qty': res_qty,
                }),
                'payment_totals': hm.has_entries({
                    'qty': res_qty,
                    'qty_w_discount': res_qty,
                }),
                'rows': hm.contains_inanyorder(
                    hm.has_entries({
                        'precision': 6,
                        'qty': res_qty,
                    }),
                ),
            }),
        )

    def test_different_precisions(self, client):
        session = self.test_session
        orders = [
            ob.OrderBuilder.construct(session, client=client, product_id=cst.DIRECT_PRODUCT_RUB_ID),
            ob.OrderBuilder.construct(session, client=client, product_id=cst.DIRECT_PRODUCT_PRECISION),
        ]
        rows = [
            ob.BasketItemBuilder(order=orders[0], quantity=D('5.55555')),
            ob.BasketItemBuilder(order=orders[1], quantity=D('56788.67')),
        ]
        request = ob.RequestBuilder.construct(
            session,
            basket=ob.BasketBuilder(rows=rows),
            firm_id=cst.FirmId.YANDEX_OOO,
        )
        person = create_person(client=request.client, type='ur')
        res = self.test_client.get(
            self.BASE_API,
            {
                'request_id': request.id,
                'person_id': person.id,
                'firm_id': cst.FirmId.YANDEX_OOO,
                'iso_currency': 'RUB',
                'payment_method_id': cst.PaymentMethodIDs.bank,
                'paysys_group_id': cst.PaysysGroupIDs.default,
            },
            is_admin=False,
            headers={'HOST': self.domain},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'request': hm.has_entries({
                    'id': request.id,
                    'total_qty': '62.3442',
                }),
                'payment_totals': hm.has_entries({
                    'qty': '56794.5556',
                    'qty_w_discount': '56794.5556',
                    'sum': '81781.72',
                    'sum_wo_discount': '81781.72',
                    'nds_pct': '20',
                    'iso_currency': 'RUB',
                    'currency_rate': u'1',
                }),
                'rows': hm.contains_inanyorder(
                    hm.has_entries({
                        'precision': 4,
                        'qty': '5.5556',
                        'sum': '5.56'
                    }),
                    hm.has_entries({
                        'precision': 3,
                        'qty': '56.789',
                        'sum': '81776.16',
                    }),
                ),
            }),
        )

    def test_different_inits(self, client):
        session = self.test_session
        orders = [
            ob.OrderBuilder.construct(session, client=client, product_id=cst.DIRECT_PRODUCT_RUB_ID),
            ob.OrderBuilder.construct(session, client=client, product_id=cst.DIRECT_PRODUCT_ID),
        ]
        rows = [
            ob.BasketItemBuilder(order=orders[0], quantity=D('10')),
            ob.BasketItemBuilder(order=orders[1], quantity=D('10')),
        ]
        request = ob.RequestBuilder.construct(
            session,
            basket=ob.BasketBuilder(rows=rows),
            firm_id=cst.FirmId.YANDEX_OOO,
        )
        person = create_person(client=request.client, type='ur')
        res = self.test_client.get(
            self.BASE_API,
            {
                'request_id': request.id,
                'person_id': person.id,
                'firm_id': cst.FirmId.YANDEX_OOO,
                'iso_currency': 'RUB',
                'payment_method_id': cst.PaymentMethodIDs.bank,
                'paysys_group_id': cst.PaysysGroupIDs.default,
            },
            is_admin=False,
            headers={'HOST': self.domain},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'request': hm.has_entries({'id': request.id}),
                'rows': hm.contains_inanyorder(
                    hm.has_entries({
                        'type_rate': '1',
                        'unit_name': u'деньги',
                        'product_type_cc': 'Money',
                        'sum': '10.00',
                        'precision': 4,
                        'qty': '10',
                        'iso_currency': 'RUB',
                    }),
                    hm.has_entries({
                        'type_rate': '1',
                        'unit_name': u'у.е.',
                        'product_type_cc': 'Bucks',
                        'sum': '300.00',
                        'precision': 6,
                        'qty': '10',
                        'iso_currency': None,
                    }),
                ),
                'payment_totals': hm.has_entries({
                    'nds_sum': '51.67',
                    'nds_pct': '20',
                    'sum': '310.00',
                    'qty': '20',
                    'iso_currency': 'RUB',
                    'currency_rate': u'1',
                }),
            }),
        )


@pytest.mark.permissions
class TestPaystepPreviewPermissions(TestCaseApiAppBase):
    BASE_API = '/v1/paystep/preview'
    domain = 'snout.core.yandex.com'

    @pytest.mark.parametrize(
        'params, res_code',
        [
            pytest.param({}, http.FORBIDDEN, id='nobody'),
            pytest.param({'is_owner': True}, http.OK, id='owner'),
            pytest.param({'w_role': True, 'w_role_client': True, 'w_role_firm': True}, http.OK, id='w_role'),
            pytest.param({'w_role': True, 'w_role_client': False, 'w_role_firm': True}, http.FORBIDDEN, id='w_wrong_client'),
            pytest.param({'w_role': True, 'w_role_client': True, 'w_role_firm': False}, http.FORBIDDEN, id='w_wrong_firm'),
        ],
    )
    def test_w_request(self, request_, params, res_code, admin_role, issue_inv_role):
        if params.get('is_owner'):
            security.set_passport_client(request_.client)

        roles = []
        if params.get('w_role'):
            roles = [
                admin_role,
                (
                    issue_inv_role,
                    {
                        cst.ConstraintTypes.firm_id: cst.FirmId.YANDEX_OOO if params.get('w_role_firm') else cst.FirmId.MARKET,
                        cst.ConstraintTypes.client_batch_id: create_role_client(client=request_.client if params.get('w_role_client') else None).client_batch_id,
                    },
                ),
            ]
        security.set_roles(roles)

        person = create_person(client=request_.client, type='ur')
        res = self.test_client.get(
            self.BASE_API,
            {
                'request_id': request_.id,
                'person_id': person.id,
                'firm_id': cst.FirmId.YANDEX_OOO,
                'iso_currency': 'RUB',
                'payment_method_id': cst.PaymentMethodIDs.bank,
                'paysys_group_id': cst.PaysysGroupIDs.default,
            },
            is_admin=False,
            headers={'HOST': self.domain},
        )
        hm.assert_that(res.status_code, hm.equal_to(res_code))

        if res_code == http.OK:
            data = res.get_json()['data']
            hm.assert_that(data, hm.has_entries({'request': hm.has_entries({'id': request_.id})}))

    @pytest.mark.parametrize(
        'params, res_code',
        [
            pytest.param({}, http.FORBIDDEN, id='nobody'),
            pytest.param({'is_owner': True}, http.BAD_REQUEST, id='owner'),  # нельзя менять счет без прав, отфильтруются доступные способы оплаты
            pytest.param({'w_view_role': {'w_role_client': False, 'w_role_firm': True}}, http.FORBIDDEN, id='w_wrong_view_client'),
            pytest.param({'w_view_role': {'w_role_client': True, 'w_role_firm': False}}, http.FORBIDDEN, id='w_wrong_view_firm'),
            pytest.param({'w_view_role': True, 'w_alter_role': {'w_role_client': True, 'w_role_firm': True}}, http.OK, id='w_role'),
            pytest.param({'w_view_role': True, 'w_alter_role': {'w_role_client': False, 'w_role_firm': True}}, http.FORBIDDEN, id='w_wrong_alter_client'),
            pytest.param({'w_view_role': True, 'w_alter_role': {'w_role_client': True, 'w_role_firm': False}}, http.FORBIDDEN, id='w_wrong_alter_firm'),
        ],
    )
    def test_w_invoice(self, client, params, res_code, view_inv_role, alter_inv_role):
        if params.get('is_owner'):
            security.set_passport_client(client)

        roles = []
        if params.get('w_view_role'):
            view_role_params = params.get('w_view_role')
            if isinstance(view_role_params, dict):
                roles.append((
                    view_inv_role,
                    {
                        cst.ConstraintTypes.firm_id: cst.FirmId.YANDEX_OOO if view_role_params.get('w_role_firm') else cst.FirmId.MARKET,
                        cst.ConstraintTypes.client_batch_id: create_role_client(client=client if view_role_params.get('w_role_client') else None).client_batch_id,
                    },
                ))
            else:
                roles.append(view_inv_role)

        if params.get('w_alter_role'):
            alter_role_params = params.get('w_alter_role')
            roles.append((
                alter_inv_role,
                {
                    cst.ConstraintTypes.firm_id: cst.FirmId.YANDEX_OOO if alter_role_params.get('w_role_firm') else cst.FirmId.MARKET,
                    cst.ConstraintTypes.client_batch_id: create_role_client(client=client if alter_role_params.get('w_role_client') else None).client_batch_id,
                },
            ))
        security.set_roles(roles)

        invoice = create_invoice(
            client=client,
            firm_id=cst.FirmId.YANDEX_OOO,
            person=create_person(client=client, type='ph'),
            paysys=ob.Getter(mapper.Paysys, 1002),
        )
        res = self.test_client.get(
            self.BASE_API,
            {
                'invoice_id': invoice.id,
                'person_id': invoice.person.id,
                'firm_id': cst.FirmId.YANDEX_OOO,
                'iso_currency': 'RUB',
                'payment_method_id': cst.PaymentMethodIDs.bank,
                'paysys_group_id': cst.PaysysGroupIDs.default,
            },
            is_admin=False,
            headers={'HOST': self.domain},
        )
        hm.assert_that(res.status_code, hm.equal_to(res_code))

        if res_code == http.OK:
            data = res.get_json()['data']
            hm.assert_that(data, hm.has_entries({'invoice': hm.has_entries({'id': invoice.id})}))
