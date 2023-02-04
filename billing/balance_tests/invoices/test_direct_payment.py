# -*- coding: utf-8 -*-

from sqlalchemy import orm

from balance import mapper
from balance import exc
from balance import constants as cst
from balance.actions.invoice_turnon import InvoiceTurnOn
from balance.providers.pay_policy import PayPolicyRoutingManager

from tests.base import BalanceTest
from tests.object_builder import *
from tests.balance_tests.pay_policy.pay_policy_common import create_pay_policy


class BaseTest(BalanceTest):
    def setUp(self):
        super(BaseTest, self).setUp()
        self.client = ClientBuilder().build(self.session).obj

        self.country1 = CountryBuilder().build(self.session).obj
        TaxPolicyBuilder(region_id=self.country1.region_id).build(self.session)
        TaxPolicyBuilder(region_id=self.country1.region_id, resident=0).build(self.session)

        self.country2 = CountryBuilder().build(self.session).obj
        TaxPolicyBuilder(region_id=self.country2.region_id).build(self.session)
        TaxPolicyBuilder(region_id=self.country2.region_id, resident=0).build(self.session)

        self.country3 = CountryBuilder().build(self.session).obj
        self.country4 = CountryBuilder().build(self.session).obj
        self.country5 = CountryBuilder().build(self.session).obj

        self.firm1 = FirmBuilder(country=self.country1).build(self.session).obj
        self.firm2 = FirmBuilder(country=self.country2).build(self.session).obj

        self.cat_cnt1_res_ph = PersonCategoryBuilder(
            country=self.country1, resident=1, ur=0, cc='c1_p'
        ).build(self.session).obj
        self.cat_cnt1_res_ur = PersonCategoryBuilder(
            country=self.country1, resident=1, ur=1, cc='c1_u'
        ).build(self.session).obj
        self.cat_cnt1_nonres = PersonCategoryBuilder(
            country=self.country1, resident=0, cc='c1_y'
        ).build(self.session).obj
        self.cat_cnt2_res = PersonCategoryBuilder(
            country=self.country2, resident=1, cc='c2_y'
        ).build(self.session).obj
        self.cat_cnt2_nonres = PersonCategoryBuilder(
            country=self.country2, resident=0, cc='c2_y'
        ).build(self.session).obj
        self.cat_cnt2_nonres_magic = PersonCategoryBuilder(
            country=self.country2, resident=0, is_default=0, cc='c2_ym'
        ).build(self.session).obj
        self.cat_cnt2_nonres_atypical = PersonCategoryBuilder(
            country=self.country2, resident=0, ur=0, cc='c2_ya'
        ).build(self.session).obj

    def cr_person(self, category):
        return PersonBuilder(person_category=category, client=self.client).build(self.session).obj

    def cr_request(self, service_id=7):
        order = OrderBuilder(
            client=self.client,
            service=Getter(mapper.Service, service_id)
        )
        basket = BasketBuilder(
            client=self.client,
            rows=[BasketItemBuilder(quantity=1, order=order)]
        )
        request = RequestBuilder(basket=basket).build(self.session).obj
        return request

    def cr_invoice(self, person, service_id):
        invoice = InvoiceBuilder(
            person=person,
            request=self.cr_request(service_id)
        ).build(self.session).obj
        InvoiceTurnOn(invoice, manual=True).do()
        return invoice


class RoutingTest(BaseTest):

    def setUp(self):
        super(RoutingTest, self).setUp()

        self.manager = PayPolicyRoutingManager(self.session)

        self.pm_bank = Getter(mapper.PaymentMethod, 1001).build(self.session).obj
        self.pm_card = Getter(mapper.PaymentMethod, 1101).build(self.session).obj

        part_f1_r1 = self.cr_pay_policy_service(cst.ServiceId.DIRECT, self.firm1,
                                                [('RUB', self.pm_bank), ('RUB', self.pm_card)], None)
        part_f1_r2 = self.cr_pay_policy_service(cst.ServiceId.DIRECT, self.firm1, [('RUB', self.pm_bank)])
        part_f1_r3_ag = self.cr_pay_policy_service(cst.ServiceId.DIRECT, self.firm1, [('RUB', self.pm_bank)])
        part_f1_r3_nonag = self.cr_pay_policy_service(cst.ServiceId.DIRECT, self.firm1, [('EUR', self.pm_bank)])

        part_f2_r1 = self.cr_pay_policy_service(cst.ServiceId.MARKET, self.firm2, [('USD', self.pm_bank)])
        part_f2_r2 = self.cr_pay_policy_service(cst.ServiceId.MARKET, self.firm2,
                                                [('USD', self.pm_bank), ('USD', self.pm_card)])
        part_f2_r3_nonag = self.cr_pay_policy_service(cst.ServiceId.DIRECT, self.firm2, [('EUR', self.pm_bank)])
        part_f2_r3_contract = self.cr_pay_policy_service(cst.ServiceId.DIRECT, self.firm2, [('BYN', self.pm_bank)])
        part_f2_r4_m = self.cr_pay_policy_service(cst.ServiceId.DIRECT, self.firm2, [('UAH', self.pm_bank)],
                                                  category=self.cat_cnt2_nonres_magic)

        part_f2_r4_a = self.cr_pay_policy_service(cst.ServiceId.MARKET, self.firm2, [('RUB', self.pm_bank)],
                                                  category=self.cat_cnt2_nonres_atypical, is_atypical=1)

        def _fmt_ppr(pay_policy_service_id, country, is_agency=None, is_contract=None):
            return {
                'r_id': country.region_id,
                'is_agency': is_agency, 'is_contract': is_contract,
                'pay_policy_service_id': pay_policy_service_id
            }

        ppr_rows = [
            _fmt_ppr(part_f1_r1, self.country1),
            _fmt_ppr(part_f1_r2, self.country2),
            _fmt_ppr(part_f1_r3_nonag, self.country3, 0),
            _fmt_ppr(part_f1_r3_ag, self.country3, 1),
            _fmt_ppr(part_f2_r1, self.country1),
            _fmt_ppr(part_f2_r2, self.country2),
            _fmt_ppr(part_f2_r3_nonag, self.country3, 0),
            _fmt_ppr(part_f2_r3_contract, self.country3, 1, 1),
            _fmt_ppr(part_f2_r4_m, self.country4),
            _fmt_ppr(part_f2_r4_a, self.country5),
        ]

        self.cr_pay_policy_region(ppr_rows)

    def cr_pay_policy_service(self, service_id, firm, rows, legal_entity=1, category=None, is_atypical=0):
        pay_policy_service_id = get_big_number()
        self.session.execute('''
           insert into bo.t_pay_policy_service (id, service_id, firm_id, legal_entity, category, is_atypical)
             values (:id, :service_id, :firm_id, :le, :cat, :at)
        ''', {
            'id': pay_policy_service_id, 'service_id': service_id, 'firm_id': firm.id,
            'le': category and category.ur or legal_entity,
            'cat': category and category.category,
            'at': is_atypical,
        })
        for currency, pm in rows:
            pay_policy_payment_method_id = get_big_number()
            self.session.add(mapper.PayPolicyPaymentMethod(
                id=pay_policy_payment_method_id,
                pay_policy_service_id=pay_policy_service_id,
                iso_currency=currency,
                payment_method=pm,
                paysys_group_id=0
            ))

        self.session.flush()
        return pay_policy_service_id

    def cr_pay_policy_region(self, rows):
        for row in rows:
            row['id'] = get_big_number()
        self.session.execute('''
            insert into bo.t_pay_policy_region (id, region_id, is_agency, is_contract, pay_policy_service_id)
              values (:id, :r_id, :is_agency, :is_contract, :pay_policy_service_id)
        ''', rows)


class TestRequestDirectPaymentCases(RoutingTest):
    def setUp(self):
        super(TestRequestDirectPaymentCases, self).setUp()
        self.session.config.__dict__['DIRECT_PAYMENT_SERVICE_REGIONS'] = [[7, self.country1.region_id]]

    def test_client_region_ok_service(self):
        self.client.region_id = self.country1.region_id
        request = self.cr_request(7)
        self.assertTrue(request.direct_payment)

    def test_person_region_ok_service(self):
        self.cr_person(self.cat_cnt2_nonres)
        request = self.cr_request(7)
        self.assertTrue(request.direct_payment)

    def test_person_region_bad_service(self):
        self.cr_person(self.cat_cnt1_res_ph)
        request = self.cr_request(11)
        self.assertFalse(request.direct_payment)

    def test_bad_client_region_ok_service(self):
        self.client.region_id = self.country3.region_id
        request = self.cr_request(7)
        self.assertFalse(request.direct_payment)

    def test_bad_person_region_ok_service(self):
        self.cr_person(self.cat_cnt1_nonres)
        request = self.cr_request(7)
        self.assertFalse(request.direct_payment)


class TestInvoiceDirectPaymentCases(RoutingTest):
    def setUp(self):
        super(TestInvoiceDirectPaymentCases, self).setUp()
        self.session.config.__dict__['DIRECT_PAYMENT_SERVICE_REGIONS'] = [[7, self.country1.region_id]]

    def test_person_region_ok_service(self):
        person = self.cr_person(self.cat_cnt2_nonres)
        invoice = self.cr_invoice(person, 7)
        self.assertTrue(invoice.direct_payment)

    def test_person_region_bad_service(self):
        person = self.cr_person(self.cat_cnt2_res)
        invoice = self.cr_invoice(person, 11)
        self.assertFalse(invoice.direct_payment)

    def test_bad_person_region_ok_service(self):
        person = self.cr_person(self.cat_cnt1_nonres)
        invoice = self.cr_invoice(person, 7)
        self.assertFalse(invoice.direct_payment)
