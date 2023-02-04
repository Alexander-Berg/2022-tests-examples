# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import bson
from decimal import Decimal as D
import functools
import datetime as dt
from billing.contract_iface import contract_meta
from balance import contractpage, mapper
import balance.constants as const
from balance import reverse_partners as rp

import tests.object_builder as ob
from tests.balance_tests.rev_partners.common import generate_acts

OBJECTS_DT = dt.datetime(2022, 1, 1)
AMOUNT = D('110')


def create_product(session, engine_id, id=None, name='Test product', product_group_id=1,
                   unit_id=666, mdh_id=None, service_code='YANDEX_SERVICE'):
    id = id or session.execute('select nvl(max(id), 0) + 10 id from bo.t_product').fetchone()['id']
    mdh_id = mdh_id or str(bson.ObjectId())
    product = mapper.Product(id=id, engine_id=engine_id, name=name, product_group_id=product_group_id,
                             activity_type_id=None, main_product_id=None,
                             unit_id=unit_id, mdh_id=mdh_id, service_code=service_code)
    session.add(product)
    session.flush()
    return product


def create_tax_policy(session, id=None, firm_id=1, name='Плоти нологи!', hidden=0,
                      resident=1, region_id=225, mdh_id=None):
    id = id or session.execute('select nvl(max(id), 0) + 10 id from bo.t_tax_policy').fetchone()['id']
    mdh_id = mdh_id or str(bson.ObjectId())
    tax_policy = mapper.TaxPolicy(id=id, firm_id=firm_id, name=name, hidden=hidden,
                                  resident=resident, region_id=region_id, mdh_id=mdh_id)
    session.add(tax_policy)
    session.flush()
    return tax_policy


def create_tax_policy_pct(session, dt, tax_policy_id, nds_pct, id=None,
                          nsp_pct=0, hidden=0, mdh_id=None):
    id = id or session.execute('select nvl(max(id), 0) + 10 id from bo.t_tax_policy_pct').fetchone()['id']
    mdh_id = mdh_id or str(bson.ObjectId())
    tax_policy_pct = mapper.TaxPolicyPct(id=id, dt=dt, tax_policy_id=tax_policy_id,
                                         nds_pct=nds_pct, nsp_pct=nsp_pct,
                                         hidden=hidden, mdh_id=mdh_id)
    session.add(tax_policy_pct)
    session.flush()
    return tax_policy_pct


def create_tax(session, dt, id=None, currency_id=810, product_id=0, hidden=0,
               iso_currency='RUB', tax_policy_id=None, mdh_id=None):
    id = id or session.execute('select nvl(max(id), 0) + 10 id from bo.t_tax').fetchone()['id']
    mdh_id = mdh_id or str(bson.ObjectId())

    tax = mapper.Tax(id=id, dt=dt, currency_id=currency_id,
                     product_id=product_id, hidden=hidden, iso_currency=iso_currency,
                     tax_policy_id=tax_policy_id, mdh_id=mdh_id)
    session.add(tax)
    session.flush()
    return tax


def create_partner_product_compls(session, dt, service_id, product_mdh_id, amount, nds_pct=None,
                                  contract_id=None, client_id=None, personal_account=None):
    """
    -- dt (string) - период обилливания, формат %Y-%m-%d, часовой пояс MSK.                 -- dt
    -- service_id (int64) - id сервиса в балансе                                            -- service_id
    -- contract_id (int64) - id договора в балансе                                          -- contract_id
    -- product_mdh_id (string) - mdh_id продукта с соответствующей ставкой налога           -- type
    -- amount (string) - сумма, включающая налог                                            -- amount
    -- nds_pct (string) - ставка налога, % (необязательно, но желательно для проверки)      -- commission_sum
    -- client_id (int64) - id клиента (необязательно, если работаем в разрезе contract_id)  -- client_id
    -- personal_account - лицевой счет (необязательно, но можно для проверки)               -- transaction_type
    """
    session.execute("""
        INSERT INTO T_PARTNER_PRODUCT_COMPLETION
          (DT,  SERVICE_ID,  CONTRACT_ID,  TYPE,            AMOUNT,  COMMISSION_SUM, CLIENT_ID,  TRANSACTION_TYPE)
        VALUES
          (:dt, :service_id, :contract_id, :product_mdh_id, :amount, :nds_pct,       :client_id, :personal_account)
    """,
        dict(dt=dt, service_id=service_id, product_mdh_id=product_mdh_id, amount=amount, nds_pct=nds_pct,
             contract_id=contract_id, client_id=client_id, personal_account=personal_account)
    )


def test_multi_nds(session):

    tax_policy0 = create_tax_policy(session, name='НДС0')
    tax_policy_pct0 = create_tax_policy_pct(session, dt=OBJECTS_DT,
                                            tax_policy_id=tax_policy0.id, nds_pct=0)
    tax_policy10 = create_tax_policy(session, name='НДС10')
    tax_policy_pct10 = create_tax_policy_pct(session, dt=OBJECTS_DT,
                                            tax_policy_id=tax_policy10.id, nds_pct=10)
    tax_policy20 = create_tax_policy(session, name='НДС20')
    tax_policy_pct20 = create_tax_policy_pct(session, dt=OBJECTS_DT,
                                            tax_policy_id=tax_policy20.id, nds_pct=20)
    tax_policy30 = create_tax_policy(session, name='НДС30')
    tax_policy_pct30 = create_tax_policy_pct(session, dt=OBJECTS_DT,
                                            tax_policy_id=tax_policy30.id, nds_pct=30)

    service = ob.ServiceBuilder(partner_income=1).build(session).obj

    product0 = create_product(session, engine_id=service.id, unit_id=850)
    tax0 = create_tax(session, dt=OBJECTS_DT,
                              product_id=product0.id,
                              tax_policy_id=tax_policy0.id)
    product10 = create_product(session, engine_id=service.id, unit_id=850)
    tax10 = create_tax(session, dt=OBJECTS_DT,
                              product_id=product10.id,
                              tax_policy_id=tax_policy10.id)
    product10_1 = create_product(session, engine_id=service.id, unit_id=850)
    tax10_1 = create_tax(session, dt=OBJECTS_DT,
                              product_id=product10_1.id,
                              tax_policy_id=tax_policy10.id)
    product20 = create_product(session, engine_id=service.id, unit_id=850)
    tax20 = create_tax(session, dt=OBJECTS_DT,
                              product_id=product20.id,
                              tax_policy_id=tax_policy20.id)
    product30 = create_product(session, engine_id=service.id, unit_id=850)
    tax20 = create_tax(session, dt=OBJECTS_DT,
                              product_id=product30.id,
                              tax_policy_id=tax_policy30.id)

    contract = mapper.Contract(ctype=contract_meta.ContractTypes(type="GENERAL"))
    session.add(contract)
    contract.client = ob.ClientBuilder().build(session).obj
    contract.person = (
        ob.PersonBuilder(client=contract.client, type="ur").build(session).obj
    )
    contract.col0.dt = OBJECTS_DT

    contract.col0.firm = 1
    contract.col0.manager_code = 1122
    contract.col0.commission = 0
    contract.col0.payment_type = const.PREPAY_PAYMENT_TYPE
    contract.col0.personal_account = 1
    contract.col0.currency = 810
    contract.external_id = contract.create_new_eid()
    contract.col0.is_signed = OBJECTS_DT

    contract.col0.services = {service.id}
    tax_policy_params = [
        {
            'service_code': 'YANDEX_SERVICE',
            'firm_id': 1,
            'country': None,
            'service_id': service.id,
            'currency': 'RUB',
            'tax_policy_mdh_id': tax_policy0.mdh_id
        },
        {
            'service_code': 'YANDEX_SERVICE',
            'firm_id': 1,
            'country': None,
            'service_id': service.id,
            'currency': 'RUB',
            'tax_policy_mdh_id': tax_policy10.mdh_id
        },
        {
            'service_code': 'YANDEX_SERVICE',
            'firm_id': 1,
            'country': None,
            'service_id': service.id,
            'currency': 'RUB',
            'tax_policy_mdh_id': tax_policy20.mdh_id
        },
    ]

    for tp in tax_policy_params:
        session.add(mapper.TaxPolicyForServiceCode(**tp))
        session.flush()

    cp = contractpage.ContractPage(session, contract.id)
    cp.create_personal_accounts()
    session.flush()

    a_m = mapper.ActMonth(for_month=OBJECTS_DT)
    rp.ReversePartnerCalc._processors_map.update({
        (rp.IS_PREPAY, service.id): rp.ReversePartnersMultiNdsInvoiceProcessor,
        (rp.IS_NOT_PREPAY, service.id): rp.ReversePartnersMultiNdsInvoiceProcessor,

    })
    rp.compl_map[service.id] = functools.partial(rp.partner_product_multinds_completions, service_ids=service.id)

    create_partner_product_compls(session, dt=OBJECTS_DT, service_id=service.id, product_mdh_id=product0.mdh_id,
                                  amount=AMOUNT, nds_pct=tax_policy_pct0.nds_pct.as_decimal(),
                                  contract_id=contract.id, client_id=None, personal_account=None)
    create_partner_product_compls(session, dt=OBJECTS_DT, service_id=service.id, product_mdh_id=product10.mdh_id,
                                  amount=AMOUNT, nds_pct=tax_policy_pct10.nds_pct.as_decimal(),
                                  contract_id=contract.id, client_id=None, personal_account=None)
    create_partner_product_compls(session, dt=OBJECTS_DT, service_id=service.id, product_mdh_id=product10_1.mdh_id,
                                  amount=AMOUNT, nds_pct=tax_policy_pct10.nds_pct.as_decimal(),
                                  contract_id=contract.id, client_id=None, personal_account=None)
    create_partner_product_compls(session, dt=OBJECTS_DT, service_id=service.id, product_mdh_id=product20.mdh_id,
                                  amount=AMOUNT, nds_pct=tax_policy_pct20.nds_pct.as_decimal(),
                                  contract_id=contract.id, client_id=None, personal_account=None)

    rpc = rp.ReversePartnerCalc(contract, [service.id], a_m)
    res = rpc.process_and_enqueue_act()
    assert len(res[1]) == 3
    acts = generate_acts(contract, a_m, dps=res[0], invoices=res[1])
    assert len(acts) == 3
