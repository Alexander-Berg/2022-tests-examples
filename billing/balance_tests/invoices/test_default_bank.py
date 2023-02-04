# -*- coding: utf-8 -*-

import datetime

import pytest
import mock

from balance import mapper
import balance.actions.acts as a_a
from balance import default_bank_rules
from balance.constants import (
    FirmId,
    PaymentMethodIDs,
    DIRECT_PRODUCT_ID,
    DIRECT_PRODUCT_RUB_ID,
)

from tests import object_builder as ob


def create_firm(session):
    return ob.FirmBuilder(region_id=225).build(session).obj


def create_paysys(session, firm, category, currency='RUR', payment_method_id=PaymentMethodIDs.credit_card):
    return ob.PaysysBuilder(
        firm=firm,
        category=category,
        currency=currency,
        iso_currency=mapper.Currency.fix_iso_code(currency),
        payment_method_id=payment_method_id
    ).build(session).obj


def create_bank(session):
    return ob.PaymentBankBuilder().build(session).obj


def create_bank_details(session, firm, bank=None, currency='RUR'):
    return ob.BankDetailsBuilder(
        payment_bank=bank or create_bank(session),
        firm=firm,
        currency=currency,
        iso_currency=mapper.Currency.fix_iso_code(currency)
    ).build(session).obj


def create_invoice(session, client, person_type, firm_id=FirmId.YANDEX_OOO):
    person = ob.PersonBuilder(client=client, type=person_type).build(session).obj
    paysys = (
        session.query(mapper.Paysys)
            .filter_by(firm_id=firm_id,
                       category=person_type,
                       payment_method_id=PaymentMethodIDs.credit_card,
                       currency='RUR'
                       )
            .first()
    )
    product = ob.Getter(mapper.Product, DIRECT_PRODUCT_RUB_ID)
    order = ob.OrderBuilder(product=product, client=client)
    request = ob.RequestBuilder(
        firm_id=firm_id,
        basket=ob.BasketBuilder(
            client=client,
            rows=[ob.BasketItemBuilder(quantity=1, order=order)]
        )
    )
    invoice = ob.InvoiceBuilder(
        client=client,
        person=person,
        paysys=paysys,
        request=request,
    ).build(session).obj
    session.expire_all()
    return invoice


def create_client_bank(session, client, bank, firm, person_category='ur'):
    return ob.ClientBankBuilder(
        client=client,
        person_category=person_category,
        payment_bank=bank,
        firm=firm,
    ).build(session).obj


@pytest.fixture
def rules():
    old_rules = default_bank_rules.PARSER.rules
    new_rules = []

    default_bank_rules.PARSER.rules = new_rules
    yield new_rules
    default_bank_rules.PARSER.rules = old_rules


@pytest.fixture
def firm(session):
    return create_firm(session)


@pytest.fixture
def paysys_ph(session, firm):
    return create_paysys(session, firm, 'ph')


@pytest.fixture
def paysys_ur(session, firm):
    return create_paysys(session, firm, 'ur')


@pytest.fixture
def client(session):
    return ob.ClientBuilder().build(session).obj


@pytest.fixture
def client_aliases(session, client):
    res = []
    for idx in range(3):
        res.append(ob.ClientBuilder.construct(session))
        res[-1].make_equivalent(client)
    return res


def test_no_bank_details(session, client, firm, paysys_ur):
    i = create_invoice(session, client, 'ur', firm.id)
    assert i.bank_id is None
    assert i.bank_details_id is None
    assert not client.banks


def test_no_rules_single_bank(session, client, firm, paysys_ur):
    bd = create_bank_details(session, firm)
    create_bank_details(session, firm, currency='USD')
    create_bank_details(session, create_firm(session))
    i = create_invoice(session, client, 'ur', firm.id)

    assert i.bank_details_id == bd.id
    client_bank, = client.banks
    assert client_bank.bank_id == bd.bank_id
    assert client_bank.firm_id == firm.id
    assert client_bank.person_category == 'ur'


def test_no_rules_multiple_banks(session, client, firm, paysys_ur):
    create_bank_details(session, firm)
    bd = create_bank_details(session, firm)

    with mock.patch('balance.mapper.invoices.random.choice', return_value=bd.bank_id):
        i = create_invoice(session, client, 'ur', firm.id)

    assert i.bank_details_id == bd.id
    client_bank, = client.banks
    assert client_bank.bank_id == bd.bank_id
    assert client_bank.firm_id == firm.id
    assert client_bank.person_category == 'ur'


def test_rules_single_allowed(session, client, rules, firm, paysys_ur):
    create_bank_details(session, firm)
    bd = create_bank_details(session, firm)
    rules.append({'firm_id': [firm.id], 'bank_ids': {bd.bank_id: None}})

    i = create_invoice(session, client, 'ur', firm.id)

    assert i.bank_details_id == bd.id
    client_bank, = client.banks
    assert client_bank.bank_id == bd.bank_id
    assert client_bank.firm_id == firm.id
    assert client_bank.person_category == 'ur'


def test_rules_multiple_allowed(session, client, rules, firm, paysys_ur):
    bd1 = create_bank_details(session, firm)
    bd2 = create_bank_details(session, firm)
    bd3 = create_bank_details(session, firm)
    rules.append({'firm_id': [firm.id], 'bank_ids': {bd1.bank_id: None, bd2.bank_id: None, bd3.bank_id: None}})

    with mock.patch('balance.mapper.invoices.random.choice', return_value=bd1.bank_id):
        i = create_invoice(session, client, 'ur', firm.id)

    assert i.bank_details_id == bd1.id
    client_bank, = client.banks
    assert client_bank.bank_id == bd1.bank_id
    assert client_bank.firm_id == firm.id
    assert client_bank.person_category == 'ur'


def test_rules_multiple_allowed_weighted(session, client, rules, firm, paysys_ur):
    bd1 = create_bank_details(session, firm)
    bd2 = create_bank_details(session, firm)
    bd3 = create_bank_details(session, firm)
    rules.append({'firm_id': [firm.id], 'bank_ids': {bd1.bank_id: 6, bd2.bank_id: 66, bd3.bank_id: 666}})

    with mock.patch('balance.muzzle_util.weighted_choice', return_value=bd1.bank_id):
        i = create_invoice(session, client, 'ur', firm.id)

    assert i.bank_details_id == bd1.id
    client_bank, = client.banks
    assert client_bank.bank_id == bd1.bank_id
    assert client_bank.firm_id == firm.id
    assert client_bank.person_category == 'ur'


def test_cache_matching_no_rules(session, client, firm, paysys_ur):
    create_bank_details(session, firm)
    bd = create_bank_details(session, firm)

    client_bank = create_client_bank(session, client, bd.payment_bank, firm)

    i = create_invoice(session, client, 'ur', firm.id)

    assert i.bank_details_id == bd.id
    assert client.banks == [client_bank]


def test_cache_matching_rules(session, client, rules, firm, paysys_ur):
    bd = create_bank_details(session, firm)
    bd_alt = create_bank_details(session, firm)
    create_bank_details(session, firm)

    client_bank = create_client_bank(session, client, bd.payment_bank, firm)
    rules.append({'firm_id': [firm.id], 'bank_ids': {bd.bank_id: None, bd_alt.bank_id: None}})

    i = create_invoice(session, client, 'ur', firm.id)

    assert i.bank_details_id == bd.id
    assert client.banks == [client_bank]


def test_cache_unmatching_no_rules(session, client, firm, paysys_ur):
    bd = create_bank_details(session, firm)
    alt_bank = create_bank(session)

    client_bank = create_client_bank(session, client, alt_bank, firm)

    i = create_invoice(session, client, 'ur', firm.id)

    assert i.bank_details_id == bd.id
    assert client.banks == [client_bank]
    assert client_bank.bank_id == bd.bank_id


def test_cache_unmatching_rules(session, client, rules, firm, paysys_ur):
    bd = create_bank_details(session, firm)
    bd_alt = create_bank_details(session, firm)

    client_bank = create_client_bank(session, client, bd_alt.payment_bank, firm)
    rules.append({'firm_id': [firm.id], 'bank_ids': {bd.bank_id: None}})

    i = create_invoice(session, client, 'ur', firm.id)

    assert i.bank_details_id == bd.id
    assert client.banks == [client_bank]
    assert client_bank.bank_id == bd.bank_id


def test_cache_w_aliases_matched(session, client, client_aliases, firm, paysys_ur):
    bd = create_bank_details(session, firm)
    create_bank_details(session, firm)
    client_banks = []
    for alias in client_aliases:
        client_banks.append(create_client_bank(session, alias, bd.payment_bank, firm))

    i = create_invoice(session, client, 'ur', firm.id)

    assert i.bank_details_id == bd.id
    assert all(cb.client in client_aliases for cb in client.banks)


def test_cache_w_aliases_unmatched(session, client, client_aliases, firm, paysys_ur):
    bd = create_bank_details(session, firm)
    alt_bank = create_bank(session)
    client_banks = []
    for alias in client_aliases:
        client_banks.append(create_client_bank(session, alias, alt_bank, firm))

    i = create_invoice(session, client, 'ur', firm.id)

    client_banks.sort(key=lambda cb: cb not in session)
    assert i.bank_details_id == bd.id
    assert client_banks[0].client_id == client.id
    assert client_banks[0].bank_id == bd.bank_id
    assert all(cb not in session for cb in client_banks[1:])


def test_cache_wo_firm_matched(session, client, client_aliases, firm, paysys_ur):
    bd = create_bank_details(session, firm)

    client_bank = create_client_bank(session, client, bd.payment_bank, None)

    i = create_invoice(session, client, 'ur', firm.id)

    assert i.bank_details_id == bd.id
    assert client.banks == [client_bank]
    assert client_bank.firm_id == firm.id


def test_cache_wo_firm_unmatched(session, client, client_aliases, firm, paysys_ur):
    bd = create_bank_details(session, firm)
    alt_bank = create_bank(session)

    client_bank = create_client_bank(session, client, alt_bank, None)

    i = create_invoice(session, client, 'ur', firm.id)

    assert i.bank_details_id == bd.id
    assert client.banks == [client_bank]
    assert client_bank.bank_id == bd.bank_id
    assert client_bank.firm_id == firm.id


def test_cache_wo_firm_unmatched_multiple(session, client, client_aliases, firm, paysys_ur):
    """
    У клиента существует 2 ClientBank, и оба с неправильным банком.
    После вызова Invoice.update_bank_id должен остаться один из них (случайный).
    Ему должен проставится правильный bank_id и фирма.
    Второй ClientBank (лишний) должен быть удален.
    """
    bd = create_bank_details(session, firm)
    alt_bank1 = create_bank(session)
    alt_bank2 = create_bank(session)

    client_bank = create_client_bank(session, client, alt_bank1, firm)
    client_bank_alt = create_client_bank(session, client, alt_bank2, None)

    i = create_invoice(session, client, 'ur', firm.id)

    assert i.bank_details_id == bd.id
    assert len(client.banks) == 1
    chosen_client_bank = client.banks[0]
    assert chosen_client_bank in (client_bank, client_bank_alt)
    assert chosen_client_bank.bank_id == bd.bank_id
    assert chosen_client_bank.firm_id == firm.id
    excess_client_bank = client_bank_alt if chosen_client_bank is client_bank else client_bank
    assert excess_client_bank not in session


def test_yinvoice_new_bank(session, rules):
    bank_details = (
        session.query(mapper.BankDetails)
            .filter_by(currency='RUR', firm_id=1)
            .order_by(mapper.BankDetails.id)
            .first()
    )
    rules.append({'person_type': 'ur', 'bank_ids': {bank_details.bank_id: None}})

    client = ob.ClientBuilder(is_agency=True).build(session).obj
    subclient = ob.ClientBuilder(agency=client).build(session).obj
    contract = ob.ContractBuilder(
        dt=datetime.datetime.now() - datetime.timedelta(days=66),
        client=client,
        person=ob.PersonBuilder(client=client, type='ur'),
        commission=1,
        payment_type=3,
        credit_type=1,
        payment_term=30,
        payment_term_max=60,
        personal_account=1,
        personal_account_fictive=1,
        currency=810,
        lift_credit_on_payment=1,
        commission_type=57,
        repayment_on_consume=1,
        credit_limit_single=1666666,
        services={7},
        is_signed=datetime.datetime.now(),
        firm=1,
    ).build(session).obj
    session.flush()

    order = ob.OrderBuilder(
        product=ob.Getter(mapper.Product, DIRECT_PRODUCT_ID),
        client=subclient, agency=client,
    ).build(session).obj
    basket = ob.BasketBuilder(
        client=contract.client,
        rows=[ob.BasketItemBuilder(quantity=6666, order=order)]
    )

    paysys = session.query(mapper.Paysys).filter_by(firm_id=FirmId.YANDEX_OOO).getone(cc='ur')
    pa = ob.PayOnCreditCase(session).pay_on_credit(basket, contract, paysys)[0]
    # симулируем случай когда закешированных банков нет
    session.delete(client.banks[0])
    session.flush()
    session.expire(client, ['banks'])

    order.calculate_consumption(datetime.datetime.now() - datetime.timedelta(days=32), {order.shipment_type: 100})
    act_accounter = a_a.ActAccounter(
        contract.client, a_a.ActMonth(for_month=datetime.datetime.now()),
        force=True, dps=[], invoices=[pa.id]
    )
    act, = act_accounter.do(skip_cut_agava=True)
    invoice = act.invoice

    assert invoice.bank_details_id == bank_details.id
    assert [cb.bank_id for cb in client.banks] == [bank_details.bank_id]
