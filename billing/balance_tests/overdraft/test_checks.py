# -*- coding: utf-8 -*-

import datetime

import pytest

from balance.overdraft.checks import (
    ActCheck,
    BannedAliases,
    CheckResidentPayers,
    ClientCategory,
    DomainCheck,
    ExpiredInvoices,
    PostPayContract,
)
from balance import muzzle_util as ut
from balance import core
from balance import mapper
from billing.contract_iface import ContractTypeId
from balance import scheme
from balance.actions import acts as a_a
from balance.actions import single_account
from balance.constants import (
    ServiceId,
    PREPAY_PAYMENT_TYPE,
    POSTPAY_PAYMENT_TYPE,
    FirmId,
    NUM_CODE_RUR,
)

from tests import object_builder as ob
from tests.balance_tests.overdraft.common import generate_invoices
from tests.object_builder import ProductBuilder, RequestBuilder, BasketBuilder, OrderBuilder, BasketItemBuilder

TODAY = ut.trunc_date(datetime.datetime.now())
TOMORROW = TODAY + datetime.timedelta(1)

CHECK_CLIENT = 'client'
CHECK_ALIAS = 'alias'
CHECK_BRAND = 'brand'


def create_client(client, client_type, is_agency=0):
    if client_type == CHECK_CLIENT:
        return client
    elif client_type == CHECK_ALIAS:
        obj_client = ob.ClientBuilder.construct(client.session, is_agency=is_agency)
        obj_client.make_equivalent(client)
        client.session.flush()
        return obj_client
    elif client_type == CHECK_BRAND:
        obj_client = ob.ClientBuilder.construct(client.session, is_agency=is_agency)
        ob.create_brand(client.session, [(TODAY, [client, obj_client])], TOMORROW)
        return obj_client


@pytest.fixture
def firm_params(session):
    return mapper.ServiceFirmOverdraftParams.get(session, ServiceId.DIRECT)


class TestClientCategory(object):

    def test_base(self, session, firm_params):
        client = ob.ClientBuilder.construct(session)
        assert ClientCategory(session).check(client, ServiceId.DIRECT, firm_params) is None

    @pytest.mark.linked_clients
    def test_brands(self, session, firm_params):
        obj_client = ob.ClientBuilder.construct(session, is_agency=1)
        client = create_client(obj_client, CHECK_BRAND)
        assert ClientCategory(session).check(client, ServiceId.DIRECT, firm_params) is not None


class TestPostPayContract(object):
    def create_contract(self, client, payment_type):
        return ob.ContractBuilder(
            client=client,
            person=ob.PersonBuilder(client=client, type='ur'),
            commission=ContractTypeId.NON_AGENCY,
            firm=FirmId.YANDEX_OOO,
            payment_type=payment_type,
            personal_account=1,
            personal_account_fictive=1,
            services={ServiceId.DIRECT},
            is_signed=datetime.datetime.now(),
            currency=NUM_CODE_RUR
        ).build(client.session).obj

    @pytest.mark.linked_clients
    @pytest.mark.parametrize(
        'payment_type, is_ok',
        [
            (PREPAY_PAYMENT_TYPE, True),
            (POSTPAY_PAYMENT_TYPE, False),
        ]
    )
    @pytest.mark.parametrize('check_type', [CHECK_CLIENT, CHECK_ALIAS, CHECK_BRAND])
    @pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
    def test_base(self, session, firm_params, client, payment_type, is_ok, check_type):
        obj_client = create_client(client, check_type, client.is_agency)
        contract = self.create_contract(obj_client, payment_type)

        res = PostPayContract(session).check(client, ServiceId.DIRECT, firm_params)
        if is_ok:
            assert res is None
        else:
            assert res == 'Postpay contract %s for service_id=7' % contract.id


class TestBannedAliases(object):

    @pytest.mark.linked_clients
    @pytest.mark.parametrize(
        'params, is_ok', [
            pytest.param({}, True, id='ok'),
            pytest.param({'overdraft_ban': 1}, False, id='overdraft_ban'),
            pytest.param({'manual_suspect': 1}, False, id='manual_suspect'),
            pytest.param({'deny_overdraft': 1}, False, id='deny_overdraft'),
        ]
    )
    @pytest.mark.parametrize('check_type', [CHECK_CLIENT, CHECK_ALIAS, CHECK_BRAND])
    @pytest.mark.parametrize('is_agency', [0, 1], ids=['client', 'agency'])
    def test_base(self, session, firm_params, params, is_ok, check_type, is_agency):
        obj_client = ob.ClientBuilder.construct(session, is_agency=is_agency, **params)
        client = create_client(obj_client, check_type, is_agency)

        res = BannedAliases(session).check(client, ServiceId.DIRECT, firm_params)
        if is_ok:
            assert res is None
        else:
            assert res == 'Banned or suspected client %s' % obj_client.id


class TestCheckResidentPayers(object):

    @pytest.mark.linked_clients
    @pytest.mark.parametrize(
        'person_type, msg',
        [
            pytest.param(None, "Client doesn't have resident payers", id='empty'),
            pytest.param('ur', None, id='ur'),
            pytest.param('sw_ur', "Client doesn't have resident payers in acceptable regions", id='sw_ur'),
            pytest.param('yt', "Client doesn't have resident payers", id='yt'),
        ]
    )
    @pytest.mark.parametrize('check_type', [CHECK_CLIENT, CHECK_ALIAS, CHECK_BRAND])
    @pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
    def test(self, session, firm_params, client, check_type, person_type, msg):
        obj_client = create_client(client, check_type, client.is_agency)

        if person_type:
            ob.PersonBuilder.construct(session, client=obj_client, type=person_type)

        res = CheckResidentPayers(session).check(client, ServiceId.DIRECT, firm_params)
        assert res == msg


class TestActCheck(object):
    @pytest.mark.linked_clients
    @pytest.mark.parametrize(
        'term_delta, msg',
        [
            (6, None),
            (5, 'no act in needed previous months'),
        ]
    )
    @pytest.mark.parametrize('check_type', [CHECK_CLIENT, CHECK_ALIAS, CHECK_BRAND])
    @pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
    def test_dt(self, session, firm_params, client, check_type, term_delta, msg):
        obj_client = create_client(client, check_type, client.is_agency)
        generate_invoices(obj_client, [(ut.add_months_to_date(TODAY, -term_delta), 750)])

        assert ActCheck(session).check(client, ServiceId.DIRECT, firm_params) == msg


class TestExpiredInvoices(object):

    @pytest.mark.linked_clients
    @pytest.mark.parametrize(
        'is_paid, is_ok',
        [
            (True, True),
            (False, False),
        ]
    )
    @pytest.mark.parametrize('check_type', [CHECK_CLIENT, CHECK_ALIAS, CHECK_BRAND])
    @pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
    def test_prepayment(self, session, client, firm_params, check_type, is_paid, is_ok):
        obj_client = create_client(client, check_type, client.is_agency)

        invoice = ob.InvoiceBuilder.construct(
            session,
            client=obj_client,
            person=ob.PersonBuilder(client=obj_client)
        )
        invoice.turn_on_rows()
        invoice.close_invoice(datetime.datetime.now())
        if is_paid:
            invoice.receipt_sum_1c = invoice.total_sum
        invoice.turn_on_dt = TODAY - datetime.timedelta(666)
        session.flush()

        res = ExpiredInvoices(session).check(client, ServiceId.DIRECT, firm_params)
        if is_ok:
            assert res is None
        else:
            assert res == 'Expired pay invoices: [(%s,)]' % invoice.id

    @pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
    def test_overdraft(self, session, client, firm_params):
        invoice = ob.InvoiceBuilder.construct(
            session,
            client=client,
            person=ob.PersonBuilder(client=client),
            overdraft=1
        )
        invoice.turn_on_rows()
        invoice.close_invoice(datetime.datetime.now())
        invoice.turn_on_dt = TODAY - datetime.timedelta(666)
        session.flush()

        res = ExpiredInvoices(session).check(client, ServiceId.DIRECT, firm_params)
        assert res is None

    @pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
    def test_personal_account(self, session, client, firm_params):
        contract = ob.create_credit_contract(
            session,
            client,
            commission=ContractTypeId.NON_AGENCY,
            personal_account_fictive=0,
        )
        request = ob.RequestBuilder.construct(session, basket=ob.BasketBuilder(client=client))

        invoice, = core.Core(session).pay_on_credit(request.id, 1003, contract.person_id, contract.id)
        q, = invoice.consumes
        act_dt = datetime.datetime.now() - datetime.timedelta(666)
        q.order.calculate_consumption(act_dt, {q.order.shipment_type: q.current_qty})
        session.flush()
        a_a.ActAccounter(client, mapper.ActMonth(for_month=act_dt), force=1).do()

        res = ExpiredInvoices(session).check(client, ServiceId.DIRECT, firm_params)
        assert res == 'Expired pay invoices: [(%s,)]' % invoice.id

    @pytest.mark.parametrize('invoice_type', ['charge_note', 'charge_note_register'])
    def test_single_account(self, session, firm_params, invoice_type):
        client = ob.ClientBuilder(with_single_account=True).build(session).obj
        person = ob.PersonBuilder(client=client, type='ur').build(session).obj
        single_account.prepare.process_client(client)

        request = RequestBuilder(
            basket=BasketBuilder(
                rows=[
                    BasketItemBuilder(
                        quantity=1,
                        order=OrderBuilder(
                            client=client,
                            product=ProductBuilder(
                                price=1,
                                engine_id=ServiceId.DIRECT
                            ),
                            service_id=ServiceId.DIRECT
                        )
                    )
                ]
            )
        ).build(session).obj

        invoice = ob.InvoiceBuilder(
            dt=TODAY - datetime.timedelta(333),
            person=person,
            paysys=ob.Getter(mapper.Paysys, 1003).build(session).obj,
            request=request,
            single_account_number=client.single_account_number,
            type=invoice_type
        ).build(session).obj
        invoice.turn_on_rows()
        invoice.close_invoice(datetime.datetime.now())
        invoice.turn_on_dt = TODAY - datetime.timedelta(333)
        session.flush()

        assert invoice.type == invoice_type
        res = ExpiredInvoices(session).check(client, ServiceId.DIRECT, firm_params)
        assert res is None


class TestDomainCheck(object):
    def create_url_client(self, client):
        session = client.session
        url_client = ob.ClientBuilder.construct(session)
        ins = scheme.active_client_same_urls.insert().values(src_client_id=client.id, dst_client_id=url_client.id)
        session.execute(ins)
        return url_client

    @pytest.mark.linked_clients
    @pytest.mark.parametrize('check_type', [CHECK_CLIENT, CHECK_ALIAS, CHECK_BRAND])
    @pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
    def test_ban(self, session, client, firm_params, check_type):
        obj_client = create_client(client, check_type, client.is_agency)
        url_client = self.create_url_client(obj_client)
        invoice = ob.InvoiceBuilder.construct(
            session,
            client=url_client,
            person=ob.PersonBuilder(client=url_client),
            overdraft=1,
        )
        invoice.turn_on_rows()
        invoice.payment_term_dt = TODAY - datetime.timedelta(666)
        session.flush()

        res = DomainCheck(session).check(client, ServiceId.DIRECT, firm_params)
        assert 'Url debts' in res
        assert client.domain_check_status == mapper.AUTO_BANNED_DOMAIN

    @pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
    def test_unban(self, session, client, firm_params):
        client.domain_check_status = mapper.AUTO_BANNED_DOMAIN
        session.flush()

        res = DomainCheck(session).check(client, ServiceId.DIRECT, firm_params)
        assert res is None
        assert client.domain_check_status == mapper.NOT_BANNED_DOMAIN

    @pytest.mark.linked_clients
    @pytest.mark.parametrize(
        'check_type, main_status, link_status, is_ok',
        [
            (CHECK_CLIENT, mapper.TRUSTED_DOMAIN, mapper.TRUSTED_DOMAIN, True),
            (CHECK_ALIAS, mapper.TRUSTED_DOMAIN, mapper.NOT_BANNED_DOMAIN, True),
            (CHECK_ALIAS, mapper.NOT_BANNED_DOMAIN, mapper.TRUSTED_DOMAIN, False),
            (CHECK_ALIAS, mapper.TRUSTED_DOMAIN, mapper.TRUSTED_DOMAIN, True),
            (CHECK_BRAND, mapper.TRUSTED_DOMAIN, mapper.NOT_BANNED_DOMAIN, False),
            (CHECK_BRAND, mapper.NOT_BANNED_DOMAIN, mapper.TRUSTED_DOMAIN, False),
            (CHECK_BRAND, mapper.TRUSTED_DOMAIN, mapper.TRUSTED_DOMAIN, True),
        ]
    )
    @pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
    def test_trusted(self, session, client, firm_params, check_type, main_status, link_status, is_ok):
        obj_client = create_client(client, check_type, client.is_agency)
        client.domain_check_status = main_status
        obj_client.domain_check_status = link_status

        url_client = self.create_url_client(obj_client)
        invoice = ob.InvoiceBuilder.construct(
            session,
            client=url_client,
            person=ob.PersonBuilder(client=url_client),
            overdraft=1,
        )
        invoice.turn_on_rows()
        invoice.payment_term_dt = TODAY - datetime.timedelta(666)
        session.flush()

        res = DomainCheck(session).check(client, ServiceId.DIRECT, firm_params)
        if is_ok:
            assert res is None
        else:
            assert res is not None
