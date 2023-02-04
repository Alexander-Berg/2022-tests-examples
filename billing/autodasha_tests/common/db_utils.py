# -*- coding: utf-8 -*-

import datetime as dt
import uuid
import bson
import hashlib
import random

import sqlalchemy as sa

import balance.mapper as mapper
from balance.providers import personal_acc_manager as pam
import balance.scheme as b_scheme
import balance.muzzle_util as ut
from balance.actions import consumption
from balance.actions.invoice_create import InvoiceFactory, generate_split_invoices
from balance.providers import personal_acc_manager as pam
import balance.actions.acts as a_a
from balance.constants import OrderLogTariffState


def create_client(session, name='test_autodasha', create_agency=False, is_agency=False, agency=None):
    client = mapper.Client(name=name)
    session.add(client)

    if create_agency:
        agency = mapper.Client(name='agency_%s' % name, is_agency=1)
        session.add(agency)
        session.flush()

    client.assign_agency_status(is_agency, agency)

    session.flush()

    return client


def create_client_person(session, client_name='test_autodasha', person_name='test_autodasha',
                         create_agency=False, is_agency=False, agency=None, person_type='ph'):
    client = create_client(session, client_name, create_agency, is_agency, agency)
    person = create_person(session, client, person_name, person_type)

    return client, person


def create_person(session, client=None, person_name='test_autodasha', person_type='ph'):
    if client is None:
        client = create_client(session)

    person = mapper.Person(client.agency or client, person_type, name=person_name, skip_category_check=True)
    session.add(person)
    session.flush()

    return person


def create_order(session, client=None, product=None, product_id=None, agency=None,
                 real_sequence=False, service_id=None, main_order=None, group_order_id=None, is_log_tariff=False):
    if client is None:
        client = create_client(session)

    if product is None:
        product = session.query(mapper.Product).getone(product_id or 1475)

    text = None
    if main_order:
        text = 'Общий счет'

    if not service_id:
        service_id = product.engine_id

    order = mapper.Order(client=client, service_id=service_id, product=product
                         , agency=agency, main_order=main_order, group_order_id=group_order_id, text=text)

    if order.service_id == 7 and real_sequence:
        sequence = sa.Sequence('s_test_service_order_id_7')
        sql_query = sa.func.next_value(sequence)
        order.service_order_id = session.execute(sql_query).scalar()

    session.add(order)
    session.flush()

    return order


def create_invoice_simple(session, order, consumes_qtys, paysys_id=1003, person=None, **kwargs):
    request_client = order.agency or order.client
    paysys = session.query(mapper.Paysys).getone(paysys_id)
    firm = paysys.firm

    rows = [mapper.BasketItem(qty, order) for qty in consumes_qtys]
    request = mapper.Request(session.oper_id, mapper.Basket(request_client, rows))
    session.add(request)
    session.flush()
    invoice = InvoiceFactory.create(request, paysys, person,
                                    status_id=0, credit=0, temporary=False, firm=firm, **kwargs)
    session.add(invoice)
    session.flush()

    invoice.create_receipt(invoice.effective_sum)
    invoice.turn_on_rows()
    session.flush()

    return invoice


def create_invoice(session, client, orders_qtys, paysys_id=1000, person=None, turn_on=False, **kwargs):
    paysys = session.query(mapper.Paysys).getone(paysys_id)
    firm = paysys.firm

    rows = [mapper.BasketItem(qty, order) for order, qty in orders_qtys]
    request = mapper.Request(session.oper_id, mapper.Basket(client, rows))
    session.add(request)
    session.flush()
    invoice = InvoiceFactory.create(request, paysys, person,
                                    status_id=0, credit=0, temporary=False, firm=firm, **kwargs)
    session.add(invoice)
    session.flush()

    if turn_on:
        invoice.create_receipt(invoice.effective_sum)
        invoice.turn_on_rows()
        session.flush()

    return invoice


def create_charge_note(session, charge_invoice, orders_qtys=None, **kwargs):
    orders_qtys = orders_qtys or [(create_order(session, client=charge_invoice.client), 1)]
    rows = [mapper.BasketItem(qty, order) for order, qty in orders_qtys]
    basket = mapper.Basket(charge_invoice.client, rows)
    request = mapper.Request(session.oper_id, basket, turn_on_rows=1)
    session.add(request)
    session.flush()

    invoice = InvoiceFactory.create(
        request,
        charge_invoice.paysys,
        charge_invoice.person,
        status_id=0,
        credit=0,
        temporary=False,
        firm=charge_invoice.firm,
        type='charge_note',
        **kwargs
    )

    invoice.charge_invoice = charge_invoice
    session.add(invoice)
    session.flush()
    return invoice


def create_fictive_invoice(session, contract, orders_qtys, paysys_id=1000, turn_on=True, **kwargs):
    paysys = session.query(mapper.Paysys).getone(paysys_id)

    rows = [mapper.BasketItem(qty, order) for order, qty in orders_qtys]
    request = mapper.Request(session.oper_id, mapper.Basket(contract.client, rows))
    session.add(request)
    session.flush()

    basket = mapper.Basket(contract.client, [ro.basket_item() for ro in request.request_orders])

    invoice_kwargs = dict(credit=2,
                          temporary=False,
                          paysys=paysys,
                          person=contract.person,
                          contract=contract,
                          pay_on_credit=True)
    invoice_kwargs.update(kwargs)
    invoices = generate_split_invoices(session, basket, invoice_kwargs, request=request)
    invoice,  = invoices
    session.flush()

    if turn_on:
        invoice.create_receipt(invoice.effective_sum)
        invoice.turn_on_rows()
        session.flush()

    return invoice


def create_general_contract(session, client, person, on_dt=None, **kwargs):
    ctype = mapper.contract_meta.ContractTypes('GENERAL')
    on_dt = on_dt or ut.trunc_date(dt.datetime.now())
    contract = mapper.Contract(ctype, client_id=client.id, person_id=person.id)

    col0 = contract.col0
    col0.commission = kwargs.get('commission', 0)
    col0.dt = on_dt
    col0.firm = kwargs.get('firm_id', 1)
    col0.currency = kwargs.get('currency', 810)
    col0.bank_details_id = kwargs.get('bank_details_id', 1)
    col0.payment_type = kwargs.get('payment_type', 2)
    col0.services = kwargs.get('services', {7, 35})
    col0.is_signed = on_dt
    col0.nds = kwargs.get('nds', 0)
    col0.memo = '123333333'
    col0.personal_account = kwargs.get('personal_account', 0)

    for k, v in kwargs.iteritems():
        setattr(col0, k, v)

    session.add(contract)
    contract.external_id = kwargs.get('external_id', contract.create_new_eid())

    session.flush()
    return contract


def create_acquiring_contract(session, client, person, on_dt=None, **kwargs):
    ctype = mapper.contract_meta.ContractTypes('ACQUIRING')
    on_dt = on_dt or ut.trunc_date(dt.datetime.now())
    contract = mapper.Contract(ctype, client_id=client.id, person_id=person.id)

    col0 = contract.col0
    col0.dt = on_dt
    col0.firm = kwargs.get('firm_id', 1)
    col0.is_signed = on_dt

    for k, v in kwargs.iteritems():
        setattr(col0, k, v)

    session.add(contract)
    contract.external_id = kwargs.get('external_id', None)
    if not contract.external_id:
        contract.external_id = contract.create_new_eid()

    session.flush()
    return contract


def create_preferred_deal_contract(session, client, person, on_dt=None, **kwargs):
    ctype = mapper.contract_meta.ContractTypes('PREFERRED_DEAL')
    on_dt = on_dt or ut.trunc_date(dt.datetime.now())
    contract = mapper.Contract(ctype, client_id=client.id, person_id=person.id)

    col0 = contract.col0
    col0.dt = on_dt
    col0.firm = kwargs.get('firm_id', 1)
    col0.is_signed = on_dt

    for k, v in kwargs.iteritems():
        setattr(col0, k, v)

    session.add(contract)
    contract.external_id = kwargs.get('external_id', contract.create_new_eid())

    session.flush()
    return contract


def get_contract_information_for_support_approve(session, contract):
    contract_type = contract.type

    firm_id = contract.firm.id
    if firm_id:
        firm_name = session.query(mapper.Firm).getone(firm_id).title
    else:
        firm_name = u'Без фирмы'

    service_ids = getattr(contract.current, 'services', None)

    if service_ids:
        service_names = session.query(mapper.Service.name).\
            filter(mapper.Service.id.in_(service_ids)).\
            order_by(mapper.Service.name).\
            all()
        service_names = ', '.join(map(lambda s: s[0], service_names))
    else:
        service_names = u'Без сервисов'

    return contract_type, firm_name, service_names


def add_subclients_collateral(session, contract, subclients):
    on_dt = ut.trunc_date(dt.datetime.now())
    subclient_params = {
        'client_credit_type': 2,
        'client_limit': 666,
        'client_limit_currency': u'RUR',
        'client_payment_term': 45
    }

    collateral_attributes = {
        'print_form_type': 0,
        'is_signed': on_dt,
        'client_limits': {subclient.id: subclient_params.copy() for subclient in subclients},
    }
    col_type = mapper.contract_meta.collateral_types['GENERAL'][1035]
    col = contract.append_collateral(on_dt, col_type, **collateral_attributes)

    session.add(col)
    session.flush()

    return col


def create_partners_contract(session, client, person, on_dt=None, **kwargs):
    ctype = mapper.contract_meta.ContractTypes('PARTNERS')
    on_dt = on_dt or ut.trunc_date(dt.datetime.now())
    contract = mapper.Contract(ctype, client_id=client.id, person_id=person.id)

    col0 = contract.col0
    col0.dt = on_dt
    col0.contract_type = kwargs.get('contract_type', 3)
    col0.firm = kwargs.get('firm_id', 1)
    col0.doc_set = kwargs.get('doc_set', 4)
    col0.currency = kwargs.get('currency', 643)
    col0.pay_to = kwargs.get('pay_to', 1)
    col0.payment_type = kwargs.get('payment_type', 1)
    col0.is_signed = on_dt
    col0.nds = kwargs.get('nds', 0)

    for k, v in kwargs.iteritems():
        setattr(col0, k, v)

    session.add(contract)
    contract.external_id = contract.create_new_eid()
    session.flush()
    return contract


def create_distr_contract(session, client, person, on_dt=None, **kwargs):
    ctype = mapper.contract_meta.ContractTypes('DISTRIBUTION')
    on_dt = on_dt or ut.trunc_date(dt.datetime.now())
    contract = mapper.Contract(ctype, client_id=client.id, person_id=person.id)

    col0 = contract.col0
    col0.dt = on_dt
    col0.contract_type = kwargs.get('contract_type', 3)
    col0.firm = kwargs.get('firm_id', 1)
    col0.currency = kwargs.get('currency', 643)
    col0.tail_time = kwargs.get('tail_time', 0)
    col0.service_start_dt = kwargs.get('service_start_dt', on_dt)
    col0.is_signed = on_dt
    col0.nds = kwargs.get('nds', 0)

    for k, v in kwargs.iteritems():
        setattr(col0, k, v)

    session.add(contract)
    contract.external_id = contract.create_new_eid()
    session.flush()
    return contract


def add_collateral(contract, collateral_type_id=None, dt_=None, **kwargs):
    if collateral_type_id is None:
        default_contract_coltypes = {
            'GENERAL': 1003,
            'PARTNERS': 2040,
            'DISTRIBUTION': 3040,
            'SPENDABLE': 7020
        }
        collateral_type_id = default_contract_coltypes.get(contract.type)

    session = contract.session
    on_dt = dt_ or ut.trunc_date(dt.datetime.now())

    default_parameters = {
        'print_form_type': 0,
        'memo': 'test'
    }
    kwargs.update(default_parameters)

    col_type = mapper.contract_meta.collateral_types[contract.type][collateral_type_id]
    col = contract.append_collateral(on_dt, col_type, **kwargs)

    session.add(col)
    session.flush()

    return col


def create_personal_account(session, agency=None, person=None, contract=None, subclient=None,
                            personal_account_fictive=1, dt_=None, paysys_id=None):
    if contract is None:
        contract_attributes = {
            'commission': 61,
            'payment_type': 2,
            'personal_account': 1,
            'personal_account_fictive': personal_account_fictive,
            'wholesale_agent_premium_awards_scale_type': 1,
            'credit_type': 2,
            'payment_term': 10,
            'calc_defermant': 0,
            'lift_credit_on_payment': 1,
            'credit_limit_single': 666
        }
        contract = create_general_contract(session, agency, person, **contract_attributes)

    if subclient:
        add_subclients_collateral(session, contract, [subclient])

    paysys = session.query(mapper.Paysys).getone(paysys_id or 1003)
    pa = pam.PersonalAccountManager(session).\
        for_contract(contract, subclient and [subclient] or None).\
        for_paysys(paysys).\
        get(dt_)

    return pa


def create_promocode(session, start_dt, end_dt, bonus=0, min_qty=0, discount_pct=0,
                     code=u'PROMOCODEFORPROMOCODEGOD666', client_id=None,
                     calc_class_name=None, **kwargs):
    if hasattr(mapper.PromoCode, 'group'):
        group = mapper.PromoCodeGroup.create_promocodes(
            session,
            calc_class_name=calc_class_name,  # Defaults to LegacyPromoCodeGroup
            promocode_info_list=[{'code': code, 'client_id': client_id}],
            start_dt=start_dt,
            end_dt=end_dt,
            calc_params=dict(
                middle_dt=start_dt + dt.timedelta(seconds=1) if discount_pct == 0 else end_dt,
                bonus1=bonus,
                bonus2=bonus,
                minimal_qty=min_qty,
                multicurrency_bonuses={
                    'RUB': {
                        'bonus1': bonus * 30,
                        'bonus2': bonus * 30,
                        'minimal_qty': min_qty * 30
                    }
                },
                currency_bonuses={
                    'RUB': bonus * 30
                },
                service_ids=[7, 70, 11, 35],
                discount_pct=discount_pct,
                **kwargs
            ),
            checks=dict(
                new_clients_only=1,
                valid_until_paid=0,
                is_global_unique=1,
                need_unique_urls=1,
                minimal_amounts={'RUB': min_qty * 30}
            ),
            firm_id=1,
            reservation_days=30,
        )
        pc = group.promocodes[0]
    else:
        pc = mapper.PromoCode(code=code,
                              start_dt=start_dt,
                              middle_dt=start_dt + dt.timedelta(seconds=1) if discount_pct == 0 else end_dt,
                              end_dt=end_dt,
                              bonus1=bonus,
                              bonus2=bonus,
                              series=2010,
                              new_clients_only=1,
                              valid_until_paid=0,
                              any_same_series=0,
                              is_global_unique=1,
                              need_unique_urls=1,
                              firm_id=1,
                              event_id=1,
                              reservation_days=30,
                              aux_minimal_qty=min_qty,
                              multicurrency_bonuses={
                                  'RUB': {
                                      'bonus1': bonus * 30,
                                      'bonus2': bonus * 30,
                                      'minimal_qty': min_qty * 30
                                  }
                              })

        session.add(pc)
        pc.service_ids = {7, 70, 11, 35}
    session.flush()
    return pc


def create_promocode_reservation(session, client, promocode=None, start_dt=None, end_dt=None,
                                 bonus=0, min_qty=0, discount_pct=0):
    if promocode is None:
        promocode = create_promocode(session, start_dt, end_dt, bonus=bonus, min_qty=min_qty, discount_pct=discount_pct)

    pcr = mapper.PromoCodeReservation(client=client, promocode=promocode, begin_dt=start_dt, end_dt=end_dt)
    session.add(pcr)
    session.flush()

    return pcr


def generate_acts(client, month=None, backdate=None, invoices=None, dps=None):
    invoices = invoices or []
    dps = dps or []

    acc = a_a.ActAccounter(client, month, backdate, dps, invoices, force=True)
    return acc.do()


def consume_order(invoice, order, qty, discount_pct=0, discount_obj=None):
    session = invoice.session
    price_obj = invoice.internal_price(
        order=order,
        product=order.product,
        dt=order.begin_dt or dt.datetime.now()
    )
    price = invoice.effective_price(price_obj, discount_pct)

    q = consumption.cr_consume(invoice, order, None,
                               price_obj, qty * price, qty, discount_pct, discount_pct, discount_obj=discount_obj)
    session.add(q)
    session.flush()
    return q


def cut_agava(client, month, dps):
    old_fr = client.full_repayment

    client.full_repayment = 0
    client.session.flush()

    acc = a_a.ActAccounter(client, month, force=True)
    acc.cut_agava(dps=dps)

    client.full_repayment = old_fr
    client.session.flush()


def create_passport(session, login='superuniquelogin2017pro', uid=2130000022965343):
    passport_info = {
        'login': login,
        'uid': uid,
        'fio': 'Вахтанг Саркисович Хачаридзе',
        'email': 'vah@tang.hach',
        'avatar': '0/0-0',
    }
    p = mapper.Passport(passport_info)
    session.add(p)
    session.flush()
    return p


def create_spendable_contract(session, client, person, services=None,
                              contract_type=None, on_dt=None, is_offer=None, **kwargs):
    ctype = mapper.contract_meta.ContractTypes('SPENDABLE')
    on_dt = on_dt or ut.trunc_date(dt.datetime.now())
    contract = mapper.Contract(ctype, client_id=client.id, person_id=person.id)

    col0 = contract.col0
    col0.dt = on_dt
    col0.contract_type = contract_type
    col0.services = services
    col0.firm = kwargs.get('firm_id', 13)
    col0.is_signed = on_dt
    if is_offer is None:
        col0.is_offer = int(col0.services and col0.services.get(137) or 0)
    else:
        col0.is_offer = is_offer

    for k, v in kwargs.iteritems():
        setattr(col0, k, v)

    session.add(contract)
    session.flush()
    contract.external_id = kwargs.get('external_id') or contract.create_new_eid()

    session.flush()
    return contract


def create_partner_taxi_stat(session, client, **kwargs):
    dt_now = dt.datetime.now()
    insert_data = ut.Struct(
        dt=dt_now,
        client_id=client.id,
        order_text=unicode(uuid.uuid1()).replace(u'-', u''),
        order_price=300,
        finished=1,
        clid=None,
        datasource_id=None,
        # promocode_sum=0, # в схеме на данный момент нет
        payment_type='cash',
        commission_sum=25,
        currency='RUB',
        tariffication_dt_utc=dt_now,
        tariffication_dt_offset=3
    )
    insert_data.update(kwargs)
    insert = b_scheme.partner_taxi_stat.insert(insert_data)
    session.execute(insert)
    session.flush()


def create_thirdparty_transaction(session, contract, service_id, **kwargs):
    dt_now = dt.datetime.now()
    tt_id = session.execute('select s_request_order_id.nextval from dual').scalar()
    trust_id = unicode(uuid.uuid4()).replace('-', '')
    params = ut.Struct(id=tt_id, contract_id=contract.id, partner_id=contract.client.id,
                       person_id=contract.person.id, dt=dt_now, trust_id=trust_id,
                       trust_payment_id=trust_id, payment_type='card',
                       transaction_type='payment', amount=300,
                       row_paysys_commission_sum=0,
                       yandex_reward=1,
                       currency='RUR',
                       partner_currency='RUR',
                       service_id=service_id)
    params.update(kwargs)
    tt = mapper.ThirdPartyTransaction(**params)
    session.add(tt)
    session.flush()


def create_markup(meta_session, code='test_markup', description='Test markup'):
    markup = mapper.Markup(code=code, description=description)
    meta_session.add(markup)
    meta_session.flush()
    return markup


def create_product_type(meta_session, id=13, cc='Money', name='Money'):
    product_type = mapper.ProductType(id=id, cc=cc, name=name)
    meta_session.add(product_type)
    meta_session.flush()
    return product_type


def create_product_unit(meta_session, id=666, name='сол', englishname='sol',
                        type_rate=1, product_type_id=0, precision=0):
    product_unit = mapper.ProductUnit(id=id, name=name, type_rate=type_rate,
                                      englishname=englishname,
                                      product_type_id=product_type_id,
                                      precision=precision)
    meta_session.add(product_unit)
    meta_session.flush()
    return product_unit


def create_product(meta_session, id=500000, name='Test product', product_group_id=1,
                   unit_id=666):
    product = mapper.Product(id=id, name=name, product_group_id=product_group_id,
                             activity_type_id=None, main_product_id=None,
                             unit_id=unit_id)
    meta_session.add(product)
    meta_session.flush()
    return product


def create_product_markup(meta_session, dt, product, markup, pct=100):
    product_markup = mapper.ProductMarkup(dt=dt, product=product, markup=markup, pct=pct)
    meta_session.add(product_markup)
    meta_session.flush()
    return product_markup


def create_tax_policy(meta_session, id=13, firm_id=1, name='Плоти нологи!', hidden=0,
                      resident=1, region_id=225):
    tax_policy = mapper.TaxPolicy(id=id, firm_id=firm_id, name=name, hidden=hidden,
                                  resident=resident, region_id=region_id)
    meta_session.add(tax_policy)
    meta_session.flush()
    return tax_policy


def create_tax_policy_pct(meta_session, dt, id=666, tax_policy_id=0, nds_pct=20,
                          nsp_pct=0, hidden=0):
    tax_policy_pct = mapper.TaxPolicyPct(id=id, dt=dt, tax_policy_id=tax_policy_id,
                                         nds_pct=nds_pct, nsp_pct=nsp_pct,
                                         hidden=hidden)
    meta_session.add(tax_policy_pct)
    meta_session.flush()
    return tax_policy_pct


def create_tax(meta_session, dt, id=66666, currency_id=810, product_id=0, hidden=0,
               iso_currency='RUB', tax_policy_id=None):

    tax = mapper.Tax(id=id, dt=dt, currency_id=currency_id,
                     product_id=product_id, hidden=hidden, iso_currency=iso_currency,
                     tax_policy_id=tax_policy_id)
    meta_session.add(tax)
    meta_session.flush()
    return tax


def create_price(meta_session, dt, id=66666, product_id=0, price=1, tax=0,
                 currency_id=810, hidden=0, iso_currency='RUB'):
    price = mapper.Price(id=id, dt=dt, product_id=product_id, price=price, tax=tax,
                         currency_id=currency_id, hidden=hidden,
                         iso_currency=iso_currency)
    meta_session.add(price)
    meta_session.flush()
    return price


def create_payment_bank(session, name=None, swift=None, bik=None, address=None, oebs_code=None):
    bik_swift = uuid.uuid4().get_hex()
    if bik is None:
        bik = bik_swift[:16]
    if swift is None:
        swift = bik_swift[16:]
    bank = mapper.PaymentBank(
        id=uuid.uuid4().int,
        name=name or uuid.uuid4().get_hex(),
        swift=swift,
        bik=bik,
        address=address or uuid.uuid4().get_hex(),
        oebs_code=oebs_code or uuid.uuid4().get_hex(),
    )
    session.add(bank)
    session.flush()
    return bank


def create_trust_payment(session, service_id, processing_id, payment_fraud_status_afs_data=None):
    processing_terminal_map = {
        10001: 305571,  # yamoney
        10105: 55001002,  # alfa
        10181: 55713001,  # sber

    }
    terminal = session.query(mapper.Terminal).getone(processing_terminal_map[processing_id])
    trust_payment = mapper.TrustPayment(invoice=None, terminal=terminal, service_id=service_id)
    session.add(trust_payment)
    trust_payment.trust_payment_id = unicode(bson.ObjectId())
    trust_payment.purchase_token = hashlib.sha1(str(random.randint(0, 1000000))).hexdigest()

    trust_payment.phone = '111111****2222'
    trust_payment.update_status('status', 'vse ok')
    session.flush()
    if payment_fraud_status_afs_data:
        pfs = mapper.PaymentFraudStatus(payment_id=trust_payment.id, trust_payment_id=trust_payment.trust_payment_id)
        pfs.set_props(**payment_fraud_status_afs_data)
        session.add(pfs)
        trust_payment.payment_fraud_status = pfs
    session.flush()
    return trust_payment


def create_simple_payment(session, service_id, invoice=None, processing_id=None):
    processing_terminal_map = {
        10500: 55907006,  # trust web
        10501: 57000045,  # trust as proc
    }

    if processing_id:
        terminal = session.query(mapper.Terminal).getone(
            processing_terminal_map[processing_id]
        )
    else:
        terminal = None
    payment = mapper.Payment(invoice=invoice, terminal=terminal, service_id=service_id)
    session.add(payment)

    if not processing_id:
        payment.paysys_code = 'BANK'
    else:
        payment.paysys_code = 'TRUST' if processing_id != 10501 else 'TRUST_API'

    session.flush()
    return payment


def create_nirvana_mnclose_sync_row(session, task_id, status, month):
    try:
        task = session.query(mapper.NirvanaMnCloseSync) \
            .filter(mapper.NirvanaMnCloseSync.task_id == task_id) \
            .filter(mapper.NirvanaMnCloseSync.dt == month) \
            .one()

        if task.status != status:
            task_id.status = status
    except sa.orm.exc.NoResultFound:
        task = mapper.NirvanaMnCloseSync(
            task_id=task_id,
            status=status,
            dt=month
        )
        session.add(task)

    session.flush()
