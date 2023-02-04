# -*- coding: utf-8 -*-

import datetime
from collections import namedtuple

import pytest
from enum import Enum
from hamcrest import equal_to

import btestlib.matchers as matchers
import btestlib.reporter as reporter
from balance import balance_db as db
from balance import balance_steps as steps
from balance import balance_web as web
from balance.features import Features
from btestlib import utils as utils
from btestlib.constants import PersonTypes, Paysyses, ContractPaymentType, ContractCommissionType, Services, Firms, Nds, \
    Currencies
from btestlib.data import person_defaults
from btestlib.data.defaults import Date
from btestlib.matchers import equal_to_casted_dict, contains_dicts_with_entries
from export_commons import Locators, read_attr_values, get_oebs_party_id, \
    get_oebs_person_cust_account_id, get_oebs_party_site_id, \
    get_oebs_location_id, get_oebs_party_site_use, get_oebs_cust_acct_site_id, create_contract, create_contract_rsya, \
    attrs_list_to_dict
from temp.igogor.balance_objects import Contexts
from btestlib.data.partner_contexts import (
    TOLOKA_CONTEXT,
    TAXI_MLU_EUROPE_ROMANIA_USD_CONTEXT_SPENDABLE,
)
import btestlib.config as balance_config

NOW = datetime.datetime.now().replace(hour=1)
'''
Посмотреть в коде какие атрибуты, как и куда выгружаются можно тут balance/processors/oebs/person.py

Договоренности:
  - пока смотрим только первоначальную выгрузку.
  Если понадобится, то будем смотреть перевыгрузку отдельных полей. Перевыгрузку всего пока точно не нужно делать.
'''

# todo-blubimov сейчас есть проблемы из-за того что мы создаем плательщиков по xmlrpc:
#  - при этом можем передавать лишние (не соответствующие типу плательщика) поля. Значения кладутся в БД. Грузятся они при этом или нет?
#  - для селектов можем передать не существующее значение (можем?)

#  todo-blubimov Еще вопросы по созданию плательщиков:
#  - иногда при создании плательщиков есть селект в зависимости от которого заполняются те или иные поля (адрес по а\я и по фиас).
#     -- Видимо для таких случаев нужно делать отдельные тесты для нестандартного выбора селекта

# todo-blubimov Тесты на плательщиков, которые не должны выгружаться будем писать?


pytestmark = [reporter.feature(Features.OEBS, Features.PERSON),
              pytest.mark.docpath('https://wiki.yandex-team.ru/balance/docs/process/oebs')]

# ============================================ ATTRIBUTES ============================================

PERSON_IS_NOT_PARTNER = 'PERSON_IS_NOT_PARTNER'
NO_SIGNER = 'NO_SIGNER'


class ATTRS(object):
    class CUSTOM(Enum):
        # в common заносить?
        intercompany = \
            Locators(balance=lambda b: b['t_extprops.client.intercompany'],
                     oebs=lambda o: o['hz_cust_accounts.attribute16'])

    class PH_NAME(Enum):
        party_name = \
            Locators(balance=lambda b: u'{0} {1}'.format(b['t_person.fname'], b['t_person.lname']),
                     oebs=lambda o: o['hz_parties.p.party_name'])

        fname = Locators(balance=lambda b: b['t_person.fname'],
                         oebs=lambda o: o['hz_parties.p.person_first_name'])

        mname = Locators(balance=lambda b: b['t_person.mname'],
                         oebs=lambda o: o['hz_parties.p.person_middle_name'])

        lname = Locators(balance=lambda b: b['t_person.lname'],
                         oebs=lambda o: o['hz_parties.p.person_last_name'])

    class COMMON_ACCOUNT(Enum):
        account_number = \
            Locators(balance=lambda b: 'P' + str(b['t_person.id']),
                     oebs=lambda o: o['hz_cust_accounts.account_number'])

        sign_type = \
            Locators(balance=lambda b: live_signature_to_oebs_format(b['t_person.live_signature']) if
            b['t_person.type'] != 'kzu' else 'LIVE',
                     oebs=lambda o: o['hz_cust_accounts.attribute1'])

        delivery_type = \
            Locators(
                balance=lambda b: merge_delivery_type_and_city(b['t_person.delivery_type'], b['t_person.delivery_city'],
                                                               resident=b['t_person_category.resident']),
                oebs=lambda o: o['hz_cust_accounts.attribute3'])

        invalid_bankprops = \
            Locators(balance=lambda b: b.get('t_extprops.person.invalid_bankprops', 0),
                     oebs=lambda o: 1 if o['hz_cust_accounts.hold_bill_flag'] == 'Y' else 0)

        account = \
            Locators(balance=lambda b: b['t_person.account'],
                     oebs=lambda o: o['hz_cust_accounts.attribute5'])

        vip = \
            Locators(balance=lambda b: b['t_extprops.person.vip'] if b.get('t_extprops.person.vip', False) else '0',
                     oebs=lambda o: o['hz_cust_accounts.attribute6'])

        early_docs = \
            Locators(balance=lambda b: early_docs_to_oebs_format(b['t_extprops.person.early_docs']) if b.get(
                't_extprops.person.early_docs', False) else 'N',
                     oebs=lambda o: o['hz_cust_accounts.attribute17'])

        bankcity = \
            Locators(balance=lambda b: b['t_person.bankcity'],
                     oebs=lambda o: o['xxar_customer_attributes.bank_address'])

        ben_account = \
            Locators(balance=lambda b: b['t_extprops.person.ben_account'] if b.get('t_extprops.person.ben_account',
                                                                                   False) else '',
                     oebs=lambda o: o['xxar_customer_attributes.account_name_eng'])

        kbk = \
            Locators(balance=lambda b: b.get('t_contract_attributes.kbk', ''),
                     oebs=lambda o: o['xxar_customer_attributes.kbk_code'])

        oktmo = \
            Locators(balance=lambda b: b.get('t_contract_attributes.oktmo', ''),
                     oebs=lambda o: o['xxar_customer_attributes.oktmo_code'])

        payment_purpose = \
            Locators(balance=lambda b: b.get('t_contract_attributes.payment_purpose', ''),
                     oebs=lambda o: o['xxar_customer_attributes.added_pay_purpose'])

    class UR_ACCOUNT(Enum):
        account_name = \
            Locators(balance=lambda b: b['t_person.name'],
                     oebs=lambda o: o['hz_cust_accounts.account_name'])

        kpp = Locators(balance=lambda b: b['t_person.kpp'],
                       oebs=lambda o: o['hz_cust_accounts.attribute11'])

        longname = \
            Locators(
                balance=lambda b: b['t_person.name'] if b['t_person.type'] == 'endbuyer_ur' else b['t_person.longname'],
                oebs=lambda o: o['hz_cust_accounts.attribute4'])

        # longname_local = \
        #     Locators(
        #         balance=lambda b: b['t_extprops.person.local_longname'],
        #         oebs=lambda o: o['xxfin_translations.a.val'])

        resident = \
            Locators(balance=lambda b: '10',
                     oebs=lambda o: o['hz_cust_accounts.attribute2'])

        bik = \
            Locators(balance=lambda b: b['t_person.bik'],
                     oebs=lambda o: o['hz_cust_accounts.attribute8'])

        iban = \
            Locators(balance=lambda b: b.get('t_extprops.person.iban', ''),
                     oebs=lambda o: o['xxar_customer_attributes.iban_number'])

        swift = \
            Locators(balance=lambda b: b.get('t_extprops.person.swift', ''),
                     oebs=lambda o: o['xxar_customer_attributes.swift_code'])

        ben_bank = \
            Locators(
                balance=lambda b: b['t_extprops.person.ben_bank'] if b.get('t_extprops.person.ben_bank', False) else '',
                oebs=lambda o: o['xxar_customer_attributes.bank_name_eng'])

    class PH_ACCOUNT(Enum):
        account_name = \
            Locators(balance=lambda b: b['t_person.lname'],
                     oebs=lambda o: o['hz_cust_accounts.account_name'])

        kpp = Locators(balance=lambda b: '',
                       oebs=lambda o: o['hz_cust_accounts.attribute11'])

        longname = \
            Locators(
                balance=lambda b: b['t_person.name'] if b['t_person.type'] == 'endbuyer_ur' else b['t_person.longname'],
                oebs=lambda o: o['hz_cust_accounts.attribute4'])

        resident = \
            Locators(balance=lambda b: '10' if b['t_person_category.resident'] == 1 else '20',
                     oebs=lambda o: o['hz_cust_accounts.attribute2'])

        bik = \
            Locators(balance=lambda b: b['t_person.bik'],
                     oebs=lambda o: o['hz_cust_accounts.attribute8'])

        iban = \
            Locators(balance=lambda b: b['t_extprops.person.iban'] if b.get('t_extprops.person.iban', False) else '',
                     oebs=lambda o: o['xxar_customer_attributes.iban_number'])

        swift = \
            Locators(balance=lambda b: u'PAYSYSTEM01' if b.get('t_contract_attributes.bank_type') == 7
            else b['t_extprops.person.swift'] if b.get('t_extprops.person.swift', False) else '',
                     oebs=lambda o: o['xxar_customer_attributes.swift_code'])

        ben_bank = \
            Locators(
                balance=lambda b: b['t_extprops.person.ben_bank'] if b.get('t_extprops.person.ben_bank', False) else '',
                oebs=lambda o: o['xxar_customer_attributes.bank_name_eng'])

        beneficiar_code = \
            Locators(balance=lambda b: b.get('t_extprops.person.kz_kbe', '') if b['t_person.type'] != 'kzp' else '19',
                     oebs=lambda o: o['xxar_customer_attributes.beneficiar_code'])

    class BANK_TYPES(Enum):
        bank_type = \
            Locators(balance=lambda b: b.get('t_contract_attributes.bank_type', ''),
                     oebs=lambda o: o['hz_cust_accounts.attribute14'])

        yamoney_wallet = \
            Locators(balance=lambda b: b.get('t_extprops.person.yamoney_wallet', ''),
                     oebs=lambda o: o['hz_cust_accounts.attribute7'] if o['hz_cust_accounts.attribute14'] == '3'
                     else '')

        webmoney_wallet = \
            Locators(balance=lambda b: b.get('t_contract_attributes.webmoney_wallet', ''),
                     oebs=lambda o: o['hz_cust_accounts.attribute7'] if o['hz_cust_accounts.attribute14'] == '4'
                     else '')

        paypal_wallet = \
            Locators(balance=lambda b: b.get('t_contract_attributes.paypal_wallet', ''),
                     oebs=lambda o: o['hz_cust_accounts.attribute7'] if o['hz_cust_accounts.attribute14'] == '5'
                     else '')

        payoneer_wallet = \
            Locators(balance=lambda b: b.get('t_extprops.person.payoneer_wallet', ''),
                     oebs=lambda o: o['hz_cust_accounts.attribute7'] if o['hz_cust_accounts.attribute14'] == '7'
                     else '')

        pingpong_wallet = \
            Locators(balance=lambda b: b.get('t_contract_attributes.pingpong_wallet', ''),
                     oebs=lambda o: o['hz_cust_accounts.attribute7'] if o['hz_cust_accounts.attribute14'] == '8'
                     else '')

    class YT_ACCOUNT(Enum):
        longname = \
            Locators(balance=lambda b: b['t_person.longname'] or b['t_person.name'],
                     oebs=lambda o: o['hz_cust_accounts.attribute4'])

        resident = \
            Locators(balance=lambda b: '20' if b['t_person.type'] != 'kzu' else '10',
                     oebs=lambda o: o['hz_cust_accounts.attribute2'])

        bik = \
            Locators(balance=lambda b: '',
                     oebs=lambda o: o['hz_cust_accounts.attribute8'])

        iban = \
            Locators(balance=lambda b: b.get('t_extprops.person.iban', '') if b['t_person.type'] != 'kzu' else b.get(
                't_person.iik', ''),
                     oebs=lambda o: o['xxar_customer_attributes.iban_number'])

        swift = \
            Locators(balance=lambda b: u'PAYSYSTEM01' if
            b.get('t_extprops.person.payoneer_wallet', '') else b.get('t_extprops.person.swift', '') if
            b['t_person.type'] != 'kzu' else b.get('t_person.bik', ''),
                     oebs=lambda o: o['xxar_customer_attributes.swift_code'])

        ben_bank = \
            Locators(
                balance=lambda b: b.get('t_extprops.person.ben_bank', '') if b['t_person.type'] != 'kzu' else b.get(
                    't_person.bank', ''),
                oebs=lambda o: o['xxar_customer_attributes.bank_name_eng'])

        beneficiar_code = \
            Locators(balance=lambda b: b.get('t_extprops.person.kz_kbe', ''),
                     oebs=lambda o: o['xxar_customer_attributes.beneficiar_code'])

        bank_type = \
            Locators(balance=lambda b: str(b.get('t_person.bank_type', '')),
                     oebs=lambda o: o['xxar_customers.attribute14'])

    class UR_ORGANIZATION(Enum):
        party_name = \
            Locators(balance=lambda b: b['t_person.name'],
                     oebs=lambda o: o['hz_parties.i.party_name'])

        # party_name_local = \
        #     Locators(balance=lambda b: b['t_extprops.person.local_name'],
        #              oebs=lambda o: o['xxfin_translations.p.val'])

        party_type = \
            Locators(balance=lambda b: 'ORGANIZATION',
                     oebs=lambda o: o['hz_parties.i.party_type'])

        inn = \
            Locators(balance=lambda b: b['t_person.inn'],
                     oebs=lambda o: o['hz_parties.i.jgzz_fiscal_code'])

        inn_doc_details = \
            Locators(balance=lambda b: b['t_person.inn_doc_details'],
                     oebs=lambda o: o['hz_parties.i.tax_reference'])

    class UA_ORGANIZATION(Enum):
        party_name = \
            Locators(balance=lambda b: b['t_person.name'],
                     oebs=lambda o: o['hz_parties.i.party_name'])

        party_type = \
            Locators(balance=lambda b: 'ORGANIZATION',
                     oebs=lambda o: o['hz_parties.i.party_type'])

        inn = \
            Locators(balance=lambda b: b['t_person.inn'],
                     oebs=lambda o: o['hz_parties.i.jgzz_fiscal_code'])

        kpp = \
            Locators(balance=lambda b: b['t_person.kpp'],
                     oebs=lambda o: o['hz_parties.i.tax_reference'])

    class YT_ORGANIZATION(Enum):
        party_name = \
            Locators(balance=lambda b: b['t_person.name'],
                     oebs=lambda o: o['hz_parties.p.party_name'])

        party_type = \
            Locators(balance=lambda b: 'ORGANIZATION',
                     oebs=lambda o: o['hz_parties.p.party_type'])

        inn = \
            Locators(balance=lambda b: b.get('t_extprops.person.kz_in', '') if
            b['t_person.type'] in ('kzu', 'yt_kzu', 'yt_kzp') else b['t_person.inn'] if
            b['t_person.type'] in ('sw_ur', 'sw_yt', 'eu_yt', 'hk_ur', 'hk_yt') else '',
                     oebs=lambda o: o['hz_parties.p.jgzz_fiscal_code'])

    class PH_PERSON(Enum):
        party_type = \
            Locators(balance=lambda b: 'PERSON',
                     oebs=lambda o: o['hz_parties.p.party_type'])

        fname = \
            Locators(balance=lambda b: b['t_person.fname'],
                     oebs=lambda o: o['hz_parties.p.person_first_name'])

        mname = \
            Locators(balance=lambda b: b['t_person.mname'],
                     oebs=lambda o: o['hz_parties.p.person_middle_name'])

        lname = \
            Locators(balance=lambda b: b['t_person.lname'],
                     oebs=lambda o: o['hz_parties.p.person_last_name'])

        inn = \
            Locators(balance=lambda b: b['t_person.inn'] if b['t_person.type'] not in ('kzp',) else b.get(
                't_extprops.person.kz_in', ''),
                     oebs=lambda o: o['hz_parties.p.jgzz_fiscal_code'])

        corr_swift = \
            Locators(balance=lambda b: b['t_extprops.person.corr_swift'] if b['t_person.type'] == 'sw_ytph'
                                                                            and b['t_person.is_partner'] == 1 else '',
                     oebs=lambda o: o['xxar_customer_attributes.correspondent_swift'])

    class UR_PERSON(Enum):
        signer_person_name = \
            Locators(balance=lambda b: b['t_person.signer_person_name'] if
            b['t_person.signer_person_name'] is not None else NO_SIGNER,
                     oebs=lambda o: o.get('hz_parties.dp.person_last_name', NO_SIGNER))

        representative = \
            Locators(balance=lambda b: b['t_person.representative'],
                     oebs=lambda o: o['hz_parties.cp.person_last_name'])

        corr_swift = \
            Locators(balance=lambda b: b['t_extprops.person.corr_swift'] if
            b['t_person.type'] in ('sw_yt', 'kzu', 'eu_yt')
            or (b['t_person.type'] == 'yt' and b['t_person.is_partner'] == 1)
            else '',
                     oebs=lambda o: o['xxar_customer_attributes.correspondent_swift'])

        ownership_type = \
            Locators(balance=lambda b: 'SELFEMPLOYED' if b.get('t_extprops.person.ownership_type') is not None
            else 'ORGANIZATION' if b['t_person.type'] == 'ur' else '',
                     oebs=lambda o: o['xxar_customer_attributes.customer_type'])

    class COMMON_ORG_CONTACT(Enum):
        job_title_code = \
            Locators(balance=lambda b: 'REPRESENTATIVE',
                     oebs=lambda o: o['hz_org_contacts.pc.job_title_code'])

        job_title_code_director = \
            Locators(balance=lambda b: 'DIRECTOR' if b['t_person.signer_person_name'] is not None else NO_SIGNER,
                     oebs=lambda o: o.get('hz_org_contacts.pd.job_title_code', NO_SIGNER))

        job_title = \
            Locators(balance=lambda b: u'Представитель',
                     oebs=lambda o: o['hz_org_contacts.pc.job_title'])

        job_title_director = \
            Locators(balance=lambda b: u'Директор' if b['t_person.signer_person_name'] is not None else NO_SIGNER,
                     oebs=lambda o: o.get('hz_org_contacts.pd.job_title', NO_SIGNER))

        subject_type = \
            Locators(balance=lambda b: 'PERSON',
                     oebs=lambda o: o['hz_relationships.cp.subject_type'])

        subject_type_director = \
            Locators(balance=lambda b: 'PERSON' if b['t_person.signer_person_name'] is not None else NO_SIGNER,
                     oebs=lambda o: o.get('hz_relationships.dp.subject_type', NO_SIGNER))

        subject_table_name = \
            Locators(balance=lambda b: 'HZ_PARTIES',
                     oebs=lambda o: o['hz_relationships.cp.subject_table_name'])

        subject_table_name_director = \
            Locators(balance=lambda b: 'HZ_PARTIES' if b['t_person.signer_person_name'] is not None else NO_SIGNER,
                     oebs=lambda o: o.get('hz_relationships.dp.subject_table_name', NO_SIGNER))

        object_type = \
            Locators(balance=lambda b: 'ORGANIZATION',
                     oebs=lambda o: o['hz_relationships.cp.object_type'])

        object_type_director = \
            Locators(balance=lambda b: 'ORGANIZATION' if b['t_person.signer_person_name'] is not None else NO_SIGNER,
                     oebs=lambda o: o.get('hz_relationships.dp.object_type', NO_SIGNER))

        object_table_name = \
            Locators(balance=lambda b: 'HZ_PARTIES',
                     oebs=lambda o: o['hz_relationships.cp.object_table_name'])

        object_table_name_director = \
            Locators(balance=lambda b: 'HZ_PARTIES' if b['t_person.signer_person_name'] is not None else NO_SIGNER,
                     oebs=lambda o: o.get('hz_relationships.dp.object_table_name', NO_SIGNER))

        relationship_code = \
            Locators(balance=lambda b: 'CONTACT_OF',
                     oebs=lambda o: o['hz_relationships.cp.relationship_code'])

        relationship_code_director = \
            Locators(balance=lambda b: 'CONTACT_OF' if b['t_person.signer_person_name'] is not None else NO_SIGNER,
                     oebs=lambda o: o.get('hz_relationships.dp.relationship_code', NO_SIGNER))

        relationship_type = \
            Locators(balance=lambda b: 'CONTACT',
                     oebs=lambda o: o['hz_relationships.cp.relationship_type'])

        relationship_type_director = \
            Locators(balance=lambda b: 'CONTACT' if b['t_person.signer_person_name'] is not None else NO_SIGNER,
                     oebs=lambda o: o.get('hz_relationships.dp.relationship_type', NO_SIGNER))

    class UR_ORG_CONTACT(Enum):
        object_id = \
            Locators(balance=lambda b: get_oebs_party_id(b['t_firm.id'], 'I{0}'.format(b['t_person.inn'] or '')),
                     oebs=lambda o: o['hz_relationships.cp.object_id'])

        object_id_director = \
            Locators(balance=lambda b: get_oebs_party_id(b['t_firm.id'], 'I{0}'.format(b['t_person.inn'] or '')) \
                if b['t_person.signer_person_name'] is not None else NO_SIGNER,
                     oebs=lambda o: o.get('hz_relationships.dp.object_id', NO_SIGNER))

    class YT_ORG_CONTACT(Enum):
        object_id = \
            Locators(balance=lambda b: get_oebs_party_id(b['t_firm.id'], 'P{0}'.format(b['t_person.id'])),
                     oebs=lambda o: o['hz_relationships.cp.object_id'])

        object_id_director = \
            Locators(balance=lambda b: get_oebs_party_id(b['t_firm.id'], 'P{0}'.format(b['t_person.id'] or '')) \
                if b['t_person.signer_person_name'] is not None else NO_SIGNER,
                     oebs=lambda o: o.get('hz_relationships.dp.object_id', NO_SIGNER))

    class COMMON_CUST_ACCOUNT_ROLE(Enum):
        party_id = \
            Locators(balance=lambda b: get_oebs_party_id(b['t_firm.id'], 'CP{0}_'.format(b['t_person.id'])),
                     oebs=lambda o: o['hz_cust_account_roles.pc.party_id'])

        party_id_director = \
            Locators(balance=lambda b: get_oebs_party_id(b['t_firm.id'], 'DP{0}_'.format(b['t_person.id'])) if
            b['t_person.signer_person_name'] is not None else NO_SIGNER,
                     oebs=lambda o: o.get('hz_cust_account_roles.pd.party_id', NO_SIGNER))

        cust_account_id = \
            Locators(balance=lambda b: get_oebs_person_cust_account_id(b['t_firm.id'], b['t_person.id']),
                     oebs=lambda o: o['hz_cust_account_roles.pc.cust_account_id'])

        cust_account_id_director = \
            Locators(balance=lambda b: get_oebs_person_cust_account_id(b['t_firm.id'], b['t_person.id']) if
            b['t_person.signer_person_name'] is not None else NO_SIGNER,
                     oebs=lambda o: o.get('hz_cust_account_roles.pd.cust_account_id', NO_SIGNER))

        role_type = \
            Locators(balance=lambda b: 'CONTACT',
                     oebs=lambda o: o['hz_cust_account_roles.pc.role_type'])

        role_type_director = \
            Locators(balance=lambda b: 'CONTACT' if b['t_person.signer_person_name'] is not None else NO_SIGNER,
                     oebs=lambda o: o.get('hz_cust_account_roles.pd.role_type', NO_SIGNER))

        signer_position_name = \
            Locators(balance=lambda b: b['t_person.signer_position_name'] if
            b['t_person.signer_person_name'] is not None else NO_SIGNER,
                     oebs=lambda o: o.get('hz_cust_account_roles.pd.attribute1', NO_SIGNER))

        authority_doc_type = \
            Locators(balance=lambda b: get_oebs_doc_type(b['t_person.authority_doc_type']) if
            b['t_person.signer_person_name'] is not None else NO_SIGNER,
                     oebs=lambda o: o.get('hz_cust_account_roles.pd.attribute2', NO_SIGNER))

        authority_doc_details = \
            Locators(balance=lambda b: b['t_person.authority_doc_details'] if
            b['t_person.signer_person_name'] is not None else NO_SIGNER,
                     oebs=lambda o: o.get('hz_cust_account_roles.pd.attribute5', NO_SIGNER))

        signer_person_gender = \
            Locators(balance=lambda b: get_oebs_gender(b['t_person.signer_person_gender']) if
            b['t_person.signer_person_name'] is not None else NO_SIGNER,
                     oebs=lambda o: o.get('hz_cust_account_roles.pd.attribute3', NO_SIGNER))

    class UR_PHONE_CONTACT(Enum):
        owner_table_name = \
            Locators(balance=lambda b: 'HZ_PARTIES',
                     oebs=lambda o: o['hz_contact_points.cp.owner_table_name'])

        owner_table_id = \
            Locators(balance=lambda b: get_oebs_party_id(b['t_firm.id'], 'CP{0}_'.format(b['t_person.id'])),
                     oebs=lambda o: o['hz_contact_points.cp.owner_table_id'])

        raw_phone_number = \
            Locators(balance=lambda b: b['t_person.phone'],
                     oebs=lambda o: o['hz_contact_points.cp.raw_phone_number'])

        phone = \
            Locators(balance=lambda b: phone_to_oebs_format(b['t_person.phone']),
                     oebs=lambda o: merge_oebs_phone_attrs(
                         o['hz_contact_points.cp.phone_country_code'],
                         o['hz_contact_points.cp.phone_area_code'],
                         o['hz_contact_points.cp.phone_number']))

        phone_line_type = \
            Locators(balance=lambda b: 'GEN',
                     oebs=lambda o: o['hz_contact_points.cp.phone_line_type'])

        phone_contact_point_type = \
            Locators(balance=lambda b: 'PHONE',
                     oebs=lambda o: o[
                         'hz_contact_points.cp.contact_point_type'])

    class PH_PHONE_CONTACT(Enum):
        owner_table_name = \
            Locators(balance=lambda b: 'HZ_PARTIES',
                     oebs=lambda o: o['hz_contact_points.p.owner_table_name'])

        owner_table_id = \
            Locators(balance=lambda b: get_oebs_party_id(b['t_firm.id'], 'P{0}'.format(b['t_person.id'])),
                     oebs=lambda o: o['hz_contact_points.p.owner_table_id'])

        raw_phone_number = \
            Locators(balance=lambda b: b['t_person.phone'],
                     oebs=lambda o: o['hz_contact_points.p.raw_phone_number'])

        phone = \
            Locators(balance=lambda b: phone_to_oebs_format(b['t_person.phone']),
                     oebs=lambda o: merge_oebs_phone_attrs(
                         o['hz_contact_points.p.phone_country_code'],
                         o['hz_contact_points.p.phone_area_code'],
                         o['hz_contact_points.p.phone_number']))

        phone_line_type = \
            Locators(balance=lambda b: 'GEN',
                     oebs=lambda o: o['hz_contact_points.p.phone_line_type'])

        phone_contact_point_type = \
            Locators(balance=lambda b: 'PHONE',
                     oebs=lambda o: o['hz_contact_points.p.contact_point_type'])

    class UR_FAX_CONTACT(Enum):
        owner_table_name = \
            Locators(balance=lambda b: 'HZ_PARTIES',
                     oebs=lambda o: o['hz_contact_points.cf.owner_table_name'])

        owner_table_id = \
            Locators(balance=lambda b: get_oebs_party_id(b['t_firm.id'], 'CP{0}_'.format(b['t_person.id'])),
                     oebs=lambda o: o['hz_contact_points.cf.owner_table_id'])

        raw_phone_number = \
            Locators(balance=lambda b: b['t_person.fax'],
                     oebs=lambda o: o['hz_contact_points.cf.raw_phone_number'])

        phone_line_type = \
            Locators(balance=lambda b: 'FAX',
                     oebs=lambda o: o['hz_contact_points.cf.phone_line_type'])

        phone = \
            Locators(balance=lambda b: phone_to_oebs_format(b['t_person.fax']),
                     oebs=lambda o: merge_oebs_phone_attrs(
                         o['hz_contact_points.cf.phone_country_code'],
                         o['hz_contact_points.cf.phone_area_code'],
                         o['hz_contact_points.cf.phone_number']))

        phone_contact_point_type = \
            Locators(balance=lambda b: 'PHONE',
                     oebs=lambda o: o['hz_contact_points.cf.contact_point_type'])

    class PH_FAX_CONTACT(Enum):
        owner_table_name = \
            Locators(balance=lambda b: 'HZ_PARTIES',
                     oebs=lambda o: o['hz_contact_points.f.owner_table_name'])

        owner_table_id = \
            Locators(balance=lambda b: get_oebs_party_id(b['t_firm.id'], 'P{0}'.format(b['t_person.id'])),
                     oebs=lambda o: o['hz_contact_points.f.owner_table_id'])

        raw_phone_number = \
            Locators(balance=lambda b: b['t_person.fax'],
                     oebs=lambda o: o['hz_contact_points.f.raw_phone_number'])

        phone_line_type = \
            Locators(balance=lambda b: 'FAX',
                     oebs=lambda o: o['hz_contact_points.f.phone_line_type'])

        phone = \
            Locators(balance=lambda b: phone_to_oebs_format(b['t_person.fax']),
                     oebs=lambda o: merge_oebs_phone_attrs(
                         o['hz_contact_points.f.phone_country_code'],
                         o['hz_contact_points.f.phone_area_code'],
                         o['hz_contact_points.f.phone_number']))

        phone_contact_point_type = \
            Locators(balance=lambda b: 'PHONE',
                     oebs=lambda o: o['hz_contact_points.f.contact_point_type'])

    class UR_EMAIL_CONTACT(Enum):
        owner_table_name = \
            Locators(balance=lambda b: 'HZ_PARTIES',
                     oebs=lambda o: o['hz_contact_points.ce.owner_table_name'])

        owner_table_id = \
            Locators(balance=lambda b: get_oebs_party_id(b['t_firm.id'], 'CP{0}_'.format(b['t_person.id'])),
                     oebs=lambda o: o['hz_contact_points.ce.owner_table_id'])

        raw_phone_number = \
            Locators(balance=lambda b: b['t_person.email'],
                     oebs=lambda o: o['hz_contact_points.ce.email_address'])

        phone_contact_point_type = \
            Locators(balance=lambda b: 'EMAIL',
                     oebs=lambda o: o['hz_contact_points.ce.contact_point_type'])

    class PH_EMAIL_CONTACT(Enum):
        owner_table_name = \
            Locators(balance=lambda b: 'HZ_PARTIES',
                     oebs=lambda o: o['hz_contact_points.e.owner_table_name'])

        owner_table_id = \
            Locators(balance=lambda b: get_oebs_party_id(b['t_firm.id'], 'P{0}'.format(b['t_person.id'])),
                     oebs=lambda o: o['hz_contact_points.e.owner_table_id'])

        raw_phone_number = \
            Locators(balance=lambda b: b['t_person.email'],
                     oebs=lambda o: o['hz_contact_points.e.email_address'])

        phone_contact_point_type = \
            Locators(balance=lambda b: 'EMAIL',
                     oebs=lambda o: o['hz_contact_points.e.contact_point_type'])

    class COMMON_LOCATION_B(Enum):
        country = \
            Locators(balance=lambda b: b['t_person_category.oebs_country_code'],
                     oebs=lambda o: o['hz_locations.b.country'])

        kladr_code = \
            Locators(balance=lambda b: b['t_person.kladr_code'] if b['t_person.fias_guid'] else '',
                     oebs=lambda o: o['hz_locations.b.address3'])

    class COMMON_LOCATION_S(Enum):
        country = \
            Locators(balance=lambda b: b['t_person_category.oebs_country_code'],
                     oebs=lambda o: o['hz_locations.s.country'])

        postcode = \
            Locators(balance=lambda b: '',
                     oebs=lambda o: o['hz_locations.s.postal_code'])

        kladr_code = \
            Locators(balance=lambda b: '',
                     oebs=lambda o: o['hz_locations.s.address3'])

        street = \
            Locators(balance=lambda b: '',
                     oebs=lambda o: o['hz_locations.s.address4'])

        street_first_50 = \
            Locators(balance=lambda b: '',
                     oebs=lambda o: o['hz_locations.s.attribute1'])

        city = \
            Locators(balance=lambda b: '',
                     oebs=lambda o: o['hz_locations.s.city'])

    class UR_LOCATION_B(Enum):
        address = \
            Locators(balance=lambda b: get_oebs_address(b['t_person.fias_guid'], b['t_person.postsuffix']) if
            b['t_person.fias_guid'] else b['t_person.postaddress'],
                     oebs=lambda o: o['hz_locations.b.address1'])

        city = \
            Locators(balance=lambda b: '' if b['t_person.type'] == 'ur' else b['t_person.city'],
                     oebs=lambda o: o['hz_locations.b.city'])

        postcode = \
            Locators(balance=lambda b: b['t_person.postcode'],
                     oebs=lambda o: o['hz_locations.b.postal_code'])

        street = \
            Locators(
                balance=lambda b: ', '.join([i for i in (b['t_person.street'], b['t_person.postsuffix']) if i])[:50],
                oebs=lambda o: o['hz_locations.b.address4'])

        street_first_50 = \
            Locators(
                balance=lambda b: ', '.join([i for i in (b['t_person.street'], b['t_person.postsuffix']) if i])[:50],
                oebs=lambda o: o['hz_locations.b.attribute1'])

    class PH_LOCATION_B(Enum):
        address = \
            Locators(balance=lambda b: get_oebs_address(b['t_person.fias_guid'], b['t_person.postsuffix']) if
            b['t_person.fias_guid'] else b['t_person.postaddress'],
                     oebs=lambda o: o['hz_locations.b.address1'])

        city = \
            Locators(balance=lambda b: '' if b['t_person.is_partner'] == 1 else b['t_person.city'],
                     oebs=lambda o: o['hz_locations.b.city'])

        postcode = \
            Locators(balance=lambda b: b['t_person.postcode'],
                     oebs=lambda o: o['hz_locations.b.postal_code'])

        street = \
            Locators(
                balance=lambda b: ', '.join([i for i in (b['t_person.street'], b['t_person.postsuffix']) if i])[:50],
                oebs=lambda o: o['hz_locations.b.address4'])

        street_first_50 = \
            Locators(
                balance=lambda b: ', '.join([i for i in (b['t_person.street'], b['t_person.postsuffix']) if i])[:50],
                oebs=lambda o: o['hz_locations.b.attribute1'])

    class UR_LOCATION_S(Enum):
        address = \
            Locators(balance=lambda b: b['t_person.address'],
                     oebs=lambda o: o['hz_locations.s.address1'])

    class YT_LOCATION_B(Enum):
        address = \
            Locators(balance=lambda b: b['t_person.postaddress']
            if b['t_person.type'] == 'eu_yt' else b['t_person.address'],
                     oebs=lambda o: o['hz_locations.b.address1'])

        city = \
            Locators(balance=lambda b: '',
                     oebs=lambda o: o['hz_locations.b.city'])

        postcode = \
            Locators(balance=lambda b: '' if b['t_person.type'] in ('yt', 'eu_yt') else b['t_person.postcode'],
                     oebs=lambda o: o['hz_locations.b.postal_code'])

        street = \
            Locators(
                balance=lambda b: '' if b['t_person.type'] == 'yt' else
                ', '.join([i for i in (b['t_person.street'], b['t_person.postsuffix']) if i])[:50],
                oebs=lambda o: o['hz_locations.b.address4'])

        street_first_50 = \
            Locators(
                balance=lambda b: '' if b['t_person.type'] == 'yt' else
                ', '.join([i for i in (b['t_person.street'], b['t_person.postsuffix']) if i])[:50],
                oebs=lambda o: o['hz_locations.b.attribute1'])

    class COMMON_LOCATION_L(Enum):
        country = \
            Locators(balance=lambda b: b['t_person_category.oebs_country_code'],
                     oebs=lambda o: o['hz_locations.l.country'])

        postcode = \
            Locators(balance=lambda b: '',
                     oebs=lambda o: o['hz_locations.l.postal_code'])

        kladr_code = \
            Locators(balance=lambda b: '',
                     oebs=lambda o: o['hz_locations.l.address3'])

        street = \
            Locators(balance=lambda b: '',
                     oebs=lambda o: o['hz_locations.l.address4'])

        street_first_50 = \
            Locators(balance=lambda b: '',
                     oebs=lambda o: o['hz_locations.l.attribute1'])

        city = \
            Locators(balance=lambda b: '',
                     oebs=lambda o: o['hz_locations.l.city'])

    class PH_LOCATION_L(Enum):
        country = \
            Locators(balance=lambda b: b['t_person_category.oebs_country_code'] if
            b['t_person.is_partner'] else PERSON_IS_NOT_PARTNER,
                     oebs=lambda o: o.get('hz_locations.l.country', PERSON_IS_NOT_PARTNER))

        postcode = \
            Locators(balance=lambda b: '' if b['t_person.is_partner'] else PERSON_IS_NOT_PARTNER,
                     oebs=lambda o: o.get('hz_locations.l.postal_code', PERSON_IS_NOT_PARTNER))

        kladr_code = \
            Locators(balance=lambda b: '' if b['t_person.is_partner'] else PERSON_IS_NOT_PARTNER,
                     oebs=lambda o: o.get('hz_locations.l.address3', PERSON_IS_NOT_PARTNER))

        street = \
            Locators(balance=lambda b: '' if b['t_person.is_partner'] else PERSON_IS_NOT_PARTNER,
                     oebs=lambda o: o.get('hz_locations.l.address4', PERSON_IS_NOT_PARTNER))

        street_first_50 = \
            Locators(balance=lambda b: '' if b['t_person.is_partner'] else PERSON_IS_NOT_PARTNER,
                     oebs=lambda o: o.get('hz_locations.l.attribute1', PERSON_IS_NOT_PARTNER))

        city = \
            Locators(balance=lambda b: '' if b['t_person.is_partner'] else PERSON_IS_NOT_PARTNER,
                     oebs=lambda o: o.get('hz_locations.l.city', PERSON_IS_NOT_PARTNER))

        address = \
            Locators(balance=lambda b: b['t_person.legaladdress'] if b['t_person.is_partner'] else
            PERSON_IS_NOT_PARTNER,
                     oebs=lambda o: o.get('hz_locations.l.address1', PERSON_IS_NOT_PARTNER))

    class UR_LOCATION_L(Enum):
        address = \
            Locators(balance=lambda b: b['t_person.legaladdress'],
                     oebs=lambda o: o['hz_locations.l.address1'])

    class YT_LOCATION_L(Enum):
        address = \
            Locators(balance=lambda b: b['t_person.legaladdress'],
                     oebs=lambda o: o['hz_locations.l.address1'])

    class UR_PARTY_USE_B(Enum):
        location_id = \
            Locators(balance=lambda b: get_oebs_location_id(b['t_firm.id'], 'P{0}_B'.format(b['t_person.id'])),
                     oebs=lambda o: o['hz_party_sites.b.location_id'])

        party_id = \
            Locators(balance=lambda b: get_oebs_party_id(b['t_firm.id'], 'I{0}'.format(b['t_person.inn'])),
                     oebs=lambda o: o['hz_party_sites.b.party_id'])

    class UR_PARTY_USE_S(Enum):
        location_id = \
            Locators(balance=lambda b: get_oebs_location_id(b['t_firm.id'], 'P{0}_S'.format(b['t_person.id'])),
                     oebs=lambda o: o['hz_party_sites.s.location_id'])

        party_id = \
            Locators(balance=lambda b: get_oebs_party_id(b['t_firm.id'], 'I{0}'.format(b['t_person.inn'])),
                     oebs=lambda o: o['hz_party_sites.s.party_id'])

    class UR_PARTY_USE_L(Enum):
        location_id = \
            Locators(balance=lambda b: get_oebs_location_id(b['t_firm.id'], 'P{0}_L'.format(b['t_person.id'])),
                     oebs=lambda o: o['hz_party_sites.l.location_id'])

        party_id = \
            Locators(balance=lambda b: get_oebs_party_id(b['t_firm.id'], 'I{0}'.format(b['t_person.inn'])),
                     oebs=lambda o: o['hz_party_sites.l.party_id'])

    class PH_PARTY_USE_L(Enum):
        location_id = \
            Locators(balance=lambda b: get_oebs_location_id(b['t_firm.id'], 'P{0}_L'.format(b['t_person.id'])) if
            b['t_person.is_partner'] else PERSON_IS_NOT_PARTNER,
                     oebs=lambda o: o.get('hz_party_sites.l.location_id', PERSON_IS_NOT_PARTNER))

        party_id = \
            Locators(balance=lambda b: get_oebs_party_id(b['t_firm.id'], 'P{0}'.format(b['t_person.id'])) if
            b['t_person.is_partner'] else PERSON_IS_NOT_PARTNER,
                     oebs=lambda o: o.get('hz_party_sites.l.party_id', PERSON_IS_NOT_PARTNER))

    class YT_PARTY_USE_B(Enum):
        location_id = \
            Locators(balance=lambda b: get_oebs_location_id(b['t_firm.id'], 'P{0}_B'.format(b['t_person.id'])),
                     oebs=lambda o: o['hz_party_sites.b.location_id'])

        party_id = \
            Locators(balance=lambda b: get_oebs_party_id(b['t_firm.id'], 'P{0}'.format(b['t_person.id'])),
                     oebs=lambda o: o['hz_party_sites.b.party_id'])

    class YT_PARTY_USE_L(Enum):
        location_id = \
            Locators(balance=lambda b: get_oebs_location_id(b['t_firm.id'], 'P{0}_L'.format(b['t_person.id'])),
                     oebs=lambda o: o['hz_party_sites.l.location_id'])

        party_id = \
            Locators(balance=lambda b: get_oebs_party_id(b['t_firm.id'], 'P{0}'.format(b['t_person.id'])),
                     oebs=lambda o: o['hz_party_sites.l.party_id'])

    class COMMON_PARTY_USE_SITE_B(Enum):
        site_use_type = \
            Locators(balance=lambda b: 'BILL_TO',
                     oebs=lambda o: o['hz_party_site_uses.b.site_use_type'])

        party_site_id = \
            Locators(balance=lambda b: get_oebs_party_site_id(b['t_firm.id'], 'P{0}_B'.format(b['t_person.id'])),
                     oebs=lambda o: o['hz_party_site_uses.b.party_site_id'])

    class COMMON_PARTY_USE_SITE_S(Enum):
        site_use_type = \
            Locators(balance=lambda b: 'SHIP_TO',
                     oebs=lambda o: o['hz_party_site_uses.s.site_use_type'])

        party_site_id = \
            Locators(balance=lambda b: get_oebs_party_site_id(b['t_firm.id'], 'P{0}_S'.format(b['t_person.id'])),
                     oebs=lambda o: o['hz_party_site_uses.s.party_site_id'])

    class COMMON_PARTY_USE_SITE_L(Enum):
        site_use_type = \
            Locators(balance=lambda b: 'LEGAL',
                     oebs=lambda o: o['hz_party_site_uses.l.site_use_type'])

        party_site_id = \
            Locators(balance=lambda b: get_oebs_party_site_id(b['t_firm.id'], 'P{0}_L'.format(b['t_person.id'])),
                     oebs=lambda o: o['hz_party_site_uses.l.party_site_id'])

    class PH_PARTY_USE_SITE_L(Enum):
        site_use_type = \
            Locators(balance=lambda b: 'LEGAL' if b['t_person.is_partner'] else PERSON_IS_NOT_PARTNER,
                     oebs=lambda o: o.get('hz_party_site_uses.l.site_use_type', PERSON_IS_NOT_PARTNER))

        party_site_id = \
            Locators(balance=lambda b: get_oebs_party_site_id(b['t_firm.id'], 'P{0}_L'.format(b['t_person.id'])) if
            b['t_person.is_partner'] else PERSON_IS_NOT_PARTNER,
                     oebs=lambda o: o.get('hz_party_site_uses.l.party_site_id', PERSON_IS_NOT_PARTNER))

    class COMMON_CUST_ACCT_SITE_B(Enum):
        cust_account_id = \
            Locators(balance=lambda b: get_oebs_person_cust_account_id(b['t_firm.id'], b['t_person.id']),
                     oebs=lambda o: o['hz_cust_acct_sites.b.cust_account_id'])

        territory = \
            Locators(balance=lambda b: get_geo_territory_code(b['t_person_category.oebs_country_code'],
                                                              b['t_person_category.resident'],
                                                              b.get('t_extprops.person.region', '')),
                     oebs=lambda o: o['hz_cust_acct_sites.b.territory'])

        party_site_id = \
            Locators(balance=lambda b: get_oebs_party_site_id(b['t_firm.id'], 'P{0}_B'.format(b['t_person.id'])),
                     oebs=lambda o: o['hz_cust_acct_sites.b.party_site_id'])

        attribute1 = \
            Locators(balance=lambda b: 'I' if b['t_person.invalid_address'] else 'A',
                     oebs=lambda o: o['hz_cust_acct_sites.b.attribute1'])

        attribute2 = \
            Locators(balance=lambda b: b.get('t_extprops.person.vat_payer', ''),
                     oebs=lambda o: o['hz_cust_acct_sites.b.attribute2'])

        attribute9 = \
            Locators(balance=lambda b: 10 if b['t_person_category.resident'] and
                                             b['t_person_category.region_id'] == b['t_firm.region_id'] else 20,
                     oebs=lambda o: o['hz_cust_acct_sites.b.attribute9'])

    class COMMON_CUST_ACCT_SITE_S(Enum):
        cust_account_id = \
            Locators(balance=lambda b: get_oebs_person_cust_account_id(b['t_firm.id'], b['t_person.id']),
                     oebs=lambda o: o['hz_cust_acct_sites.s.cust_account_id'])

        territory = \
            Locators(balance=lambda b: get_geo_territory_code(b['t_person_category.oebs_country_code'],
                                                              b['t_person_category.resident'],
                                                              b.get('t_extprops.person.region', '')),
                     oebs=lambda o: o['hz_cust_acct_sites.s.territory'])

        party_site_id = \
            Locators(balance=lambda b: get_oebs_party_site_id(b['t_firm.id'], 'P{0}_S'.format(b['t_person.id'])),
                     oebs=lambda o: o['hz_cust_acct_sites.s.party_site_id'])

        attribute1 = \
            Locators(balance=lambda b: 'I' if b['t_person.invalid_address'] else 'A',
                     oebs=lambda o: o['hz_cust_acct_sites.s.attribute1'])

        attribute2 = \
            Locators(balance=lambda b: b.get('t_extprops.person.vat_payer', ''),
                     oebs=lambda o: o['hz_cust_acct_sites.s.attribute2'])
        attribute9 = \
            Locators(balance=lambda b: 10 if b['t_person_category.resident'] and
                                             b['t_person_category.region_id'] == b['t_firm.region_id'] else 20,
                     oebs=lambda o: o['hz_cust_acct_sites.s.attribute9'])

    class COMMON_CUST_ACCT_SITE_L(Enum):
        cust_account_id = \
            Locators(balance=lambda b: get_oebs_person_cust_account_id(b['t_firm.id'], b['t_person.id']),
                     oebs=lambda o: o['hz_cust_acct_sites.l.cust_account_id'])

        territory = \
            Locators(balance=lambda b: get_geo_territory_code(b['t_person_category.oebs_country_code'],
                                                              b['t_person_category.resident'],
                                                              b.get('t_extprops.person.region', '')),
                     oebs=lambda o: o['hz_cust_acct_sites.l.territory'])

        party_site_id = \
            Locators(balance=lambda b: get_oebs_party_site_id(b['t_firm.id'], 'P{0}_L'.format(b['t_person.id'])),
                     oebs=lambda o: o['hz_cust_acct_sites.l.party_site_id'])

        attribute1 = \
            Locators(balance=lambda b: 'I' if b['t_person.invalid_address'] else 'A',
                     oebs=lambda o: o['hz_cust_acct_sites.l.attribute1'])

        attribute2 = \
            Locators(balance=lambda b: b.get('t_extprops.person.vat_payer', ''),
                     oebs=lambda o: o['hz_cust_acct_sites.l.attribute2'])
        attribute9 = \
            Locators(balance=lambda b: 10 if b['t_person_category.resident'] and
                                             b['t_person_category.region_id'] == b['t_firm.region_id'] else 20,
                     oebs=lambda o: o['hz_cust_acct_sites.l.attribute9'])

    class PH_CUST_ACCT_SITE_L(Enum):
        cust_account_id = \
            Locators(balance=lambda b: get_oebs_person_cust_account_id(b['t_firm.id'], b['t_person.id']) if b[
                't_person.is_partner'] else PERSON_IS_NOT_PARTNER,
                     oebs=lambda o: o.get('hz_cust_acct_sites.l.cust_account_id', PERSON_IS_NOT_PARTNER))

        territory = \
            Locators(balance=lambda b: get_geo_territory_code(b['t_person_category.oebs_country_code'],
                                                              b['t_person_category.resident'],
                                                              b.get('t_extprops.person.region', '')) if
            b['t_person.is_partner'] else PERSON_IS_NOT_PARTNER,
                     oebs=lambda o: o.get('hz_cust_acct_sites.l.territory', PERSON_IS_NOT_PARTNER))

        party_site_id = \
            Locators(balance=lambda b: get_oebs_party_site_id(b['t_firm.id'], 'P{0}_L'.format(b['t_person.id'])) if
            b['t_person.is_partner'] else PERSON_IS_NOT_PARTNER,
                     oebs=lambda o: o.get('hz_cust_acct_sites.l.party_site_id', PERSON_IS_NOT_PARTNER))

        attribute1 = \
            Locators(balance=lambda b: 'I' if b['t_person.invalid_address'] else 'A' if
            b['t_person.is_partner'] else PERSON_IS_NOT_PARTNER,
                     oebs=lambda o: o.get('hz_cust_acct_sites.l.attribute1', PERSON_IS_NOT_PARTNER))

        attribute2 = \
            Locators(balance=lambda b: b.get('t_extprops.person.vat_payer', '') if
            b['t_person.is_partner'] else PERSON_IS_NOT_PARTNER,
                     oebs=lambda o: o.get('hz_cust_acct_sites.l.attribute2', PERSON_IS_NOT_PARTNER))

        attribute9 = \
            Locators(balance=lambda b: PERSON_IS_NOT_PARTNER if not b['t_person.is_partner'] else 10 if
            b['t_person_category.resident'] and
            b['t_person_category.region_id'] == b['t_firm.region_id'] else 20,
                     oebs=lambda o: o.get('hz_cust_acct_sites.l.attribute9', PERSON_IS_NOT_PARTNER))

    class COMMON_CUST_SITE_USE_B(Enum):
        site_use_code = \
            Locators(balance=lambda b: 'BILL_TO',
                     oebs=lambda o: o['hz_cust_site_uses.b.site_use_code'])

        location = \
            Locators(balance=lambda b: 'P{0}_B'.format(b['t_person.id']),
                     oebs=lambda o: o['hz_cust_site_uses.b.location'])

        cust_acct_site_id = \
            Locators(balance=lambda b: get_oebs_cust_acct_site_id(b['t_firm.id'], '{0}_B'.format(b['t_person.id'])),
                     oebs=lambda o: o['hz_cust_site_uses.b.cust_acct_site_id'])

    class COMMON_CUST_SITE_USE_S(Enum):
        site_use_code = \
            Locators(balance=lambda b: 'SHIP_TO',
                     oebs=lambda o: o['hz_cust_site_uses.s.site_use_code'])

        location = \
            Locators(balance=lambda b: 'P{0}_S'.format(b['t_person.id']),
                     oebs=lambda o: o['hz_cust_site_uses.s.location'])

        cust_acct_site_id = \
            Locators(balance=lambda b: get_oebs_cust_acct_site_id(b['t_firm.id'], '{0}_S'.format(b['t_person.id'])),
                     oebs=lambda o: o['hz_cust_site_uses.s.cust_acct_site_id'])

    class COMMON_CUST_SITE_USE_L(Enum):
        site_use_code = \
            Locators(balance=lambda b: 'LEGAL',
                     oebs=lambda o: o['hz_cust_site_uses.l.site_use_code'])

        location = \
            Locators(balance=lambda b: 'P{0}_L'.format(b['t_person.id']),
                     oebs=lambda o: o['hz_cust_site_uses.l.location'])

        cust_acct_site_id = \
            Locators(balance=lambda b: get_oebs_cust_acct_site_id(b['t_firm.id'], '{0}_L'.format(b['t_person.id'])),
                     oebs=lambda o: o['hz_cust_site_uses.l.cust_acct_site_id'])

    class PH_CUST_SITE_USE_L(Enum):
        site_use_code = \
            Locators(balance=lambda b: 'LEGAL' if b['t_person.is_partner'] else PERSON_IS_NOT_PARTNER,
                     oebs=lambda o: o.get('hz_cust_site_uses.l.site_use_code', PERSON_IS_NOT_PARTNER))

        location = \
            Locators(balance=lambda b: 'P{0}_L'.format(b['t_person.id']) if
            b['t_person.is_partner'] else PERSON_IS_NOT_PARTNER,
                     oebs=lambda o: o.get('hz_cust_site_uses.l.location', PERSON_IS_NOT_PARTNER))

        cust_acct_site_id = \
            Locators(balance=lambda b: get_oebs_cust_acct_site_id(b['t_firm.id'],
                                                                  '{0}_L'.format(b['t_person.id'])) if
            b['t_person.is_partner'] else PERSON_IS_NOT_PARTNER,
                     oebs=lambda o: o.get('hz_cust_site_uses.l.cust_acct_site_id', PERSON_IS_NOT_PARTNER))


# ====================================================================================================


def get_balance_person_data(person_id, firm_id, str_locators):
    person_data = {}

    # t_person
    t_person_prefix = 't_person.'
    query = "SELECT * FROM v_person WHERE id = :person_id"
    person_data = db.balance().execute(query, {'person_id': person_id}, single_row=True)
    person_data.update(utils.add_key_prefix(person_data, t_person_prefix))

    # t_contract_attributes
    t_contract_attributes_prefix = 't_contract_attributes.'
    query = "SELECT * FROM t_contract_attributes WHERE ATTRIBUTE_BATCH_ID = (select ATTRIBUTE_BATCH_ID from " \
            "t_person where id = :person_id)"
    person_attrs = db.balance().execute(query, {'person_id': person_id}, single_row=False)
    person_data.update(
        utils.add_key_prefix(contract_attributes_to_attr_dict(person_attrs), t_contract_attributes_prefix))

    # t_person_category
    t_person_category_prefix = 't_person_category.'
    if is_prefix_in_locators(t_person_category_prefix, str_locators):
        query = "SELECT * FROM t_person_category WHERE category = :person_type"
        category = db.balance().execute(query, {'person_type': person_data['t_person.type']}, single_row=True)
        person_data.update(utils.add_key_prefix(category, t_person_category_prefix))

    # t_invoice
    t_invoice_prefix = 't_invoice.'
    if is_prefix_in_locators(t_invoice_prefix, str_locators):
        query = "SELECT * FROM t_invoice WHERE person_id = :person_id"
        invoice = db.balance().execute(query, {'person_id': person_data['t_person.id']}, single_row=True)
        if invoice:
            person_data.update(utils.add_key_prefix(invoice, t_invoice_prefix))

    # t_firm
    t_firm_prefix = 't_firm.'
    if is_prefix_in_locators(t_firm_prefix, str_locators):
        query = "SELECT * FROM t_firm WHERE id = :firm_id"
        firm = db.balance().execute(query, {'firm_id': firm_id}, single_row=True)
        person_data.update(utils.add_key_prefix(firm, t_firm_prefix))

    # t_extprops for persons
    t_extprops_person_prefix = 't_extprops.person.'
    if is_prefix_in_locators(t_extprops_person_prefix, str_locators):
        extprops = db.balance().get_extprops_by_object_id('Person', person_id)
        person_data.update(utils.add_key_prefix(extprops_to_attr_dict(extprops), t_extprops_person_prefix))

    # t_extprops for client (for 'intercompany' attribute)
    t_extprops_client_prefix = 't_extprops.client.'
    if is_prefix_in_locators(t_extprops_client_prefix, str_locators):
        client_id = person_data['t_person.client_id']
        extprops = db.balance().get_extprops_by_object_id('Client', client_id)
        prefixed_extprops_attrs = utils.add_key_prefix(extprops_to_attr_dict(extprops), t_extprops_client_prefix)
        person_data.update(prefixed_extprops_attrs)

    return person_data


def get_oebs_person_data(balance_data, firm_id, oebs_str_locators):
    person = {}
    person_id = balance_data['t_person.id']
    firm_id = firm_id

    # hz_cust_accounts
    hz_cust_accounts_prefix = 'hz_cust_accounts.'
    if is_prefix_in_locators(hz_cust_accounts_prefix, oebs_str_locators):
        object_id = 'P{0}'.format(person_id)
        query = "SELECT * FROM apps.hz_cust_accounts WHERE account_number = :object_id"
        result = db.oebs().execute_oebs(firm_id, query, {'object_id': object_id}, single_row=True)
        person.update(utils.add_key_prefix(result, hz_cust_accounts_prefix))

    # xxar_customer_attributes
    xxar_customer_attributes_prefix = 'xxar_customer_attributes.'
    if is_prefix_in_locators(xxar_customer_attributes_prefix, oebs_str_locators):
        object_id = 'P{0}'.format(person_id)
        query = "SELECT * FROM apps.XXAR_CUSTOMER_ATTRIBUTES WHERE CUSTOMER_ID =" \
                "(SELECT ac.CUST_ACCOUNT_ID FROM apps.HZ_CUST_ACCOUNTS ac WHERE ac.ACCOUNT_NUMBER = :object_id)"
        result = db.oebs().execute_oebs(firm_id, query, {'object_id': object_id}, single_row=True)
        person.update(utils.add_key_prefix(result, xxar_customer_attributes_prefix))

    # hz_cust_account_roles
    object_id_options = {'pd': 'P{0}_D'.format(person_id),
                         'pc': 'P{0}_C'.format(person_id)}
    for option, object_id in object_id_options.iteritems():
        hz_cust_account_roles_prefix = 'hz_cust_account_roles.{0}.'.format(option)
        if is_prefix_in_locators(hz_cust_account_roles_prefix, oebs_str_locators):
            query = "SELECT * FROM apps.hz_cust_account_roles WHERE orig_system_reference  = :object_id"
            result = db.oebs().execute_oebs(firm_id, query, {'object_id': object_id}, single_row=True)
            person.update(utils.add_key_prefix(result, hz_cust_account_roles_prefix))

    # hz_org_contacts
    object_id_options = {'pc': 'P{0}_C'.format(person_id),
                         'pd': 'P{0}_D'.format(person_id)}
    for option, object_id in object_id_options.iteritems():
        hz_org_contacts_prefix = 'hz_org_contacts.{0}.'.format(option)
        if is_prefix_in_locators(hz_org_contacts_prefix, oebs_str_locators):
            query = "SELECT * FROM apps.hz_org_contacts WHERE orig_system_reference  = :object_id"
            result = db.oebs().execute_oebs(firm_id, query, {'object_id': object_id}, single_row=True)
            person.update(utils.add_key_prefix(result, hz_org_contacts_prefix))

    # hz_relationships
    object_id_options = {'cp': 'CP' + str(person_id),
                         'dp': 'DP' + str(person_id)}
    for option, object_id in object_id_options.iteritems():
        hz_relationships_prefix = 'hz_relationships.{0}.'.format(option)
        if is_prefix_in_locators(hz_relationships_prefix, oebs_str_locators):
            try:
                object_id = get_oebs_party_id(balance_data['t_firm.id'], object_id)
                query = "SELECT * FROM apps.hz_relationships WHERE subject_id  = :object_id"
                result = db.oebs().execute_oebs(firm_id, query, {'object_id': object_id}, single_row=True)
                person.update(utils.add_key_prefix(result, hz_relationships_prefix))
            except KeyError:
                pass

    # hz_parties

    # для украинских юриков ЕГРПОУ хранится в поле kpp, и, если оно непустое,
    # то его значение передается в orig_system_reference вместо inn
    # если kpp=null, то в orig_system_reference передается inn
    # если пусты оба значения, то в orig_system_reference передается только I
    if balance_data['t_person.type'] == PersonTypes.UA.code:
        inn_kpp = balance_data['t_person.kpp'] or balance_data['t_person.inn'] or ''
    else:
        inn_kpp = balance_data['t_person.inn'] or ''

    object_id_options = {'p': 'P{0}'.format(person_id),
                         'cp': 'CP{0}'.format(person_id),
                         'dp': 'DP{0}'.format(person_id),
                         'i': 'I{0}'.format(inn_kpp)}
    for option, object_id in object_id_options.iteritems():
        hz_parties_prefix = 'hz_parties.{0}.'.format(option)
        if is_prefix_in_locators(hz_parties_prefix, oebs_str_locators):
            query = "SELECT * FROM apps.hz_parties WHERE orig_system_reference  = :object_id"
            result = db.oebs().execute_oebs(firm_id, query, {'object_id': object_id}, single_row=True)
            person.update(utils.add_key_prefix(result, hz_parties_prefix))

    # # xxfin_translations
    #
    # object_id_options = {
    #     'p': [get_oebs_party_id(firm_id, 'I{0}'.format(balance_data['t_person.inn'] or '')), 'PARTY_NAME'],
    #     'a': [get_oebs_person_cust_account_id(firm_id, person_id), 'ATTRIBUTE4']
    #     }
    # for option, params in object_id_options.iteritems():
    #     xxfin_translations_prefix = 'xxfin_translations.{0}.'.format(option)
    #     if is_prefix_in_locators(xxfin_translations_prefix, oebs_str_locators):
    #         query = "SELECT * FROM apps.xxfin_translations WHERE OWNER_TABLE_ID  = :object_id AND COLUMN_NAME = :column_name"
    #         result = db.oebs().execute_oebs(firm_id, query, {'object_id': params[0], 'column_name': params[1]},
    #                                         single_row=True)
    #         print 'result_xxfin', result
    #         person.update(utils.add_key_prefix(result, xxfin_translations_prefix))

    # hz_locations
    object_id_template_options = {'b': 'P{0}_B',
                                  'l': 'P{0}_L',
                                  's': 'P{0}_S'}
    for option, object_id_template in object_id_template_options.iteritems():
        hz_locations_prefix = 'hz_locations.{0}.'.format(option)
        if is_prefix_in_locators(hz_locations_prefix, oebs_str_locators):
            query = "SELECT * FROM apps.hz_locations WHERE orig_system_reference  = :object_id"
            result = db.oebs().execute_oebs(firm_id, query, {'object_id': object_id_template.format(person_id)},
                                            single_row=True)
            person.update(utils.add_key_prefix(result, hz_locations_prefix))

    # hz_party_sites
    object_id_template_options = {'b': 'P{0}_B',
                                  'l': 'P{0}_L',
                                  's': 'P{0}_S'}
    for option, object_id_template in object_id_template_options.iteritems():
        hz_party_sites_prefix = 'hz_party_sites.{0}.'.format(option)
        if is_prefix_in_locators(hz_party_sites_prefix, oebs_str_locators):
            query = "SELECT * FROM apps.hz_party_sites WHERE orig_system_reference  = :object_id"
            result = db.oebs().execute_oebs(firm_id, query, {'object_id': object_id_template.format(person_id)},
                                            single_row=True)
            person.update(utils.add_key_prefix(result, hz_party_sites_prefix))

    # hz_party_site_uses
    object_id_template_options = {'b': ['BILL_TO', 'P{0}_B'],
                                  's': ['SHIP_TO', 'P{0}_S'],
                                  'l': ['LEGAL', 'P{0}_L']}
    for option, object_id_template in object_id_template_options.iteritems():
        hz_party_site_uses_prefix = 'hz_party_site_uses.{0}.'.format(option)
        if is_prefix_in_locators(hz_party_site_uses_prefix, oebs_str_locators):
            try:
                party_site_id = get_oebs_party_site_id(firm_id, object_id_template[1].format(person_id))
                object_id = get_oebs_party_site_use(firm_id, object_id_template[0], party_site_id)
                query = "SELECT * FROM apps.hz_party_site_uses WHERE party_site_use_id  = :object_id"
                result = db.oebs().execute_oebs(firm_id, query, {'object_id': object_id},
                                                single_row=True)
                person.update(utils.add_key_prefix(result, hz_party_site_uses_prefix))
            except KeyError:
                pass

    # hz_cust_acct_sites
    object_id_template_options = {'b': 'P{0}_B',
                                  's': 'P{0}_S',
                                  'l': 'P{0}_L'}
    for option, object_id_template in object_id_template_options.iteritems():
        hz_cust_acct_sites_prefix = 'hz_cust_acct_sites.{0}.'.format(option)
        if is_prefix_in_locators(hz_cust_acct_sites_prefix, oebs_str_locators):
            query = "SELECT * FROM apps.hz_cust_acct_sites WHERE orig_system_reference  = :object_id"
            result = db.oebs().execute_oebs(firm_id, query, {'object_id': object_id_template.format(person_id)},
                                            single_row=True)
            person.update(utils.add_key_prefix(result, hz_cust_acct_sites_prefix))

    # hz_cust_site_uses
    object_id_template_options = {'b': 'P{0}_B',
                                  's': 'P{0}_S',
                                  'l': 'P{0}_L'}
    for option, object_id_template in object_id_template_options.iteritems():
        hz_cust_site_uses_prefix = 'hz_cust_site_uses.{0}.'.format(option)
        if is_prefix_in_locators(hz_cust_site_uses_prefix, oebs_str_locators):
            query = "SELECT * FROM apps.hz_cust_site_uses WHERE orig_system_reference  = :object_id"
            result = db.oebs().execute_oebs(firm_id, query, {'object_id': object_id_template.format(person_id)},
                                            single_row=True)
            person.update(utils.add_key_prefix(result, hz_cust_site_uses_prefix))

    # hz_contact_points
    object_id_template_options = {'e': 'P{0}_E',
                                  'ce': 'P{0}_CE',
                                  'p': 'P{0}_P',
                                  'cp': 'P{0}_CP',
                                  'f': 'P{0}_F',
                                  'cf': 'P{0}_CF'}
    for option, object_id_template in object_id_template_options.iteritems():
        hz_contact_points_prefix = 'hz_contact_points.{0}.'.format(option)
        if is_prefix_in_locators(hz_contact_points_prefix, oebs_str_locators):
            # query = "SELECT * FROM apps.hz_contact_points WHERE orig_system_reference  = :object_id"
            # В столбце transposed_phone_number OEBS может передавать необрабатываемое значение
            query = "SELECT owner_table_name, owner_table_id, raw_phone_number, phone_country_code, phone_area_code, " \
                    "phone_number, phone_line_type, contact_point_type, email_address " \
                    "FROM apps.hz_contact_points WHERE orig_system_reference  = :object_id"
            result = db.oebs().execute_oebs(firm_id, query,
                                            {'object_id': object_id_template.format(person_id)}, single_row=True)
            person.update(utils.add_key_prefix(result, hz_contact_points_prefix))

    # xxar_customers
    xxar_customers_prefix = 'xxar_customers.'
    if is_prefix_in_locators(xxar_customers_prefix, oebs_str_locators):
        object_id = 'P{0}'.format(person_id)
        query = "SELECT * FROM apps.xxar_customers WHERE orig_system_reference = :object_id"
        result = db.oebs().execute_oebs(firm_id, query, {'object_id': object_id}, single_row=True)
        person.update(utils.add_key_prefix(result, xxar_customers_prefix))

    return person


# todo-blubimov методы кандидаты на вынос из теста в steps/utils и тп.
# ====================================================================================================================
def extprops_to_attr_dict(extprops_list):
    attr_dict = {}
    for extprop in extprops_list:
        non_none_value_keys = [k for k in extprop.keys() if k[:5] == 'value' and extprop[k] is not None]
        if len(non_none_value_keys) > 1:
            raise utils.TestsError(
                u"Expected one non-none value in extprops but got {}".format(len(non_none_value_keys)))
        elif len(non_none_value_keys) == 0:
            attr_dict[extprop['attrname']] = ''
        else:
            attr_dict[extprop['attrname']] = extprop[non_none_value_keys[0]]
    return attr_dict


def contract_attributes_to_attr_dict(contract_attributes_list):
    value_fields = [
        'value_clob',
        'value_dt',
        'value_str',
        'value_num',
    ]

    attr_dict = {}
    for contract_attributes in contract_attributes_list:
        values_iter = (contract_attributes[value_field] for value_field in value_fields)
        value = next((value for value in values_iter if value is not None), None)
        attr_dict[contract_attributes['code'].lower()] = value
    return attr_dict


# строковые значения локаторов из атрибутов (например, 't_person.inn')
# на самом деле здесь еще возвращаются строки типа 'kzu','10', но они не мешают и фильтровать их нет смысла
def get_attrs_str_locators(attrs):
    def get_str_constants(locator_func):
        return {c for c in locator_func.func_code.co_consts if isinstance(c, str)}

    balance_str_locators = set()
    oebs_str_locators = set()
    for locators in attrs_list_to_dict(attrs).values():
        balance_str_locators.update(get_str_constants(locators.balance))
        oebs_str_locators.update(get_str_constants(locators.oebs))
    return balance_str_locators, oebs_str_locators


def is_prefix_in_locators(prefix, db_locators):
    return any(locator.startswith(prefix) for locator in db_locators)


def ph_name_to_oebs_format(fname, lname):
    return u'{0}{1}'.format(fname + u' ' if fname is not None else u'', lname or u'')


# код территории из геобазы
def get_geo_territory_code(code, resident, region):
    if resident:
        if code == 'NU':
            code = 'CV'
        elif code == 'HK':
            code = 'CN'
        return code
    else:
        territory_short_name_map = {21326: 'CV',
                                    21199: 'ST',
                                    21595: 'TC',
                                    21580: 'FM',
                                    21553: 'VI',
                                    20856: 'AG',
                                    206: 'LV',
                                    }
        return territory_short_name_map[region] if region else ''


def get_party_id(object_id, firm_id=1):
    query = "select party_id from apps.hz_parties where orig_system_reference = '{0}'".format(object_id)
    try:
        return db.oebs().execute_oebs(firm_id, query, {}, single_row=True)['party_id']
    except KeyError:
        return {}


def get_cust_account(object_id, firm_id=1):
    query = "select cust_account_id from apps.hz_cust_accounts where orig_system_reference = '{0}'".format(object_id)
    try:
        return db.oebs().execute_oebs(firm_id, query, {}, single_row=True)['cust_account_id']
    except KeyError:
        return {}


def phone_to_oebs_format(phone):
    return ''.join(ch for ch in phone if ch not in ' +()-') if phone else phone


def live_signature_to_oebs_format(signature):
    return 'FAX' if signature == 0 else 'LIVE'


def early_docs_to_oebs_format(early_docs):
    return 'Y' if early_docs else 'N'


def get_oebs_address(fias, post_suffix):
    address_parts = get_fias_address_parts(fias, [])
    address_parts[0], address_parts[1] = address_parts[1], address_parts[0]
    address_parts.reverse()
    for i in range(1, len(address_parts) - 1, 2):
        address_parts[i] += ','
    if post_suffix:
        address_parts[-1] += ','
        address_parts.append(post_suffix)
    return ' '.join(address_parts)


def get_fias_address_parts(fias, parts):
    if fias:
        fias_address = db.balance().execute('''SELECT * FROM t_fias WHERE guid = :fias''', {'fias': fias},
                                            single_row=True)
        if fias_address:
            parts.append(fias_address['short_name'])
            parts.append(fias_address['formal_name'])
            return get_fias_address_parts(fias_address['parent_guid'], parts)
    return parts


def merge_oebs_phone_attrs(country_code, area_code, phone_number):
    return '{0}{1}{2}'.format(country_code or '', area_code or '', phone_number or '')


def get_oebs_doc_type(bill_doc_type):
    doc_type_map = {
        u'Устав': 1,
        u'Доверенность': 2,
        u'Приказ': 3,
        u'Распоряжение': 4,
        u'Положение о филиале': 5,
        u'Свидетельство о регистрации': 6,
        u'Договор': 7,
        u'Протокол': 8,
        u'Решение': 9,
        u'устав': 1,
        u'доверенность': 2,
        u'Св-во о регистрации': 6,
        u'Доверенности': 2,
        u'Устава': 1,
        u'Приказа': 3,
        u'Распоряжения': 4,
        u'Положения о филиале': 5,
        u'Свидетельства о регистрации': 6,
        u'Договора': 7,
        u'Протокола': 8,
        None: None}
    return doc_type_map[bill_doc_type]


def get_oebs_gender(bill_gender):
    gender_map = {
        'M': 'M',
        'W': 'F',
        'X': None,
        u'м': 'M',
        None: None}
    return gender_map[bill_gender]


def merge_delivery_type_and_city(delivery_type, delivery_city, resident):
    delivery_type_map = {
        0: 'PR',
        1: 'PR',
        2: 'CY',
        3: 'CC',
        4: 'VIP',
        5: 'EDO',
    }
    return '_'.join(
        [delivery_type_map[delivery_type], delivery_city]) if delivery_city and delivery_type != 5 and resident else \
        delivery_type_map[delivery_type]


# ====================================================================================================================

# todo-blubimov excluded_attrs нужен? если да - поддержать в коде
def check_attrs(person_id, attrs, firm_id, excluded_attrs=None):
    balance_str_locators, oebs_str_locators = get_attrs_str_locators(attrs)

    with reporter.step(u'Считываем данные из баланса'):
        balance_data = get_balance_person_data(person_id, firm_id, balance_str_locators)
    with reporter.step(u'Считываем данные из ОЕБС'):
        oebs_data = get_oebs_person_data(balance_data, firm_id, oebs_str_locators)

    balance_values, oebs_values = read_attr_values(attrs, balance_data, oebs_data)

    utils.check_that(oebs_values, equal_to_casted_dict(balance_values),
                     step=u'Проверяем корректность данных плательщика в ОЕБС')


def create_simple_invoice(client_id, person_id, context, contract_id=None, endbuyer_id=None):
    campaigns_list = [
        {'client_id': client_id, 'service_id': context.service.id, 'product_id': context.product.id, 'qty': 100}
    ]
    invoice_id, _, _, _ = steps.InvoiceSteps.create_force_invoice(client_id=client_id,
                                                                  person_id=person_id,
                                                                  campaigns_list=campaigns_list,
                                                                  paysys_id=context.paysys.id,
                                                                  contract_id=contract_id,
                                                                  endbuyer_id=endbuyer_id,
                                                                  prevent_oebs_export=True)
    if balance_config.ENABLE_SINGLE_ACCOUNT:
        invoice_id = db.get_invoice_by_charge_note_id(invoice_id)[0]['id']
    if endbuyer_id:
        firm_id = db.get_invoice_by_id(invoice_id)[0]['firm_id']
        db.balance().execute("""insert into t_person_firm (person_id, firm_id) values (:person_id, :firm_id)""",
                             {'person_id': endbuyer_id, 'firm_id': firm_id})

    return invoice_id


def create_endbuyer(endbuyer_context):
    client_id = steps.ClientSteps.create(params={'IS_AGENCY': 1}, prevent_oebs_export=True)
    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code, inn_type=person_defaults.InnType.RANDOM)
    contract_id, _ = create_contract(client_id=client_id, person_id=person_id,
                                     contract_type=ContractCommissionType.COMMISS)
    endbuyer_id = steps.PersonSteps.create(client_id, endbuyer_context.endbuyer_type.code, full=True,
                                           inn_type=person_defaults.InnType.UNIQUE)
    invoice_id = create_simple_invoice(client_id, person_id, UR_CONTEXT, contract_id, endbuyer_id)
    try:
        steps.ExportSteps.export_oebs(invoice_id=invoice_id)
    except Exception:
        pass
    steps.ExportSteps.export_oebs(client_id=client_id, endbuyer_id=endbuyer_id)
    return endbuyer_id, invoice_id


UR_CONTEXT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(person_type=PersonTypes.UR, firm=Firms.YANDEX_1)
PH_CONTEXT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(person_type=PersonTypes.PH, paysys=Paysyses.BANK_PH_RUB)
YT_CONTEXT = Contexts.DIRECT_FISH_YT_RUB_CONTEXT.new(person_type=PersonTypes.YT)
# todo-blubimov нужно использовать paysys=1060 или 11101060 ?
# todo-blubimov должны выгружаться в фирму 1 или 111 ? (в t_person_category фирма 1)
YT_KZU_CONTEXT = Contexts.MARKET_RUB_CONTEXT.new(person_type=PersonTypes.YT_KZU, paysys=Paysyses.BANK_YTUR_KZT)
YT_KZP_CONTEXT = Contexts.MARKET_RUB_CONTEXT.new(person_type=PersonTypes.YT_KZP, paysys=Paysyses.BANK_YTPH_KZT)
UA_UR_CONTEXT = Contexts.DIRECT_FISH_UAH_CONTEXT.new(person_type=PersonTypes.UA)
UA_PH_CONTEXT = Contexts.DIRECT_FISH_UAH_CONTEXT.new(person_type=PersonTypes.PU, paysys=Paysyses.BANK_UA_PH_UAH)
USA_UR_CONTEXT = Contexts.DIRECT_FISH_USD_CONTEXT.new(person_type=PersonTypes.USU)
USA_PH_CONTEXT = Contexts.DIRECT_FISH_USD_CONTEXT.new(person_type=PersonTypes.USP, paysys=Paysyses.BANK_US_PH_USD)
SW_UR_CONTEXT = Contexts.DIRECT_FISH_SW_EUR_CONTEXT.new(person_type=PersonTypes.SW_UR)
SW_PH_CONTEXT = Contexts.DIRECT_FISH_SW_EUR_CONTEXT.new(person_type=PersonTypes.SW_PH, paysys=Paysyses.BANK_SW_PH_EUR)
SW_YT_CONTEXT = Contexts.DIRECT_FISH_SW_EUR_CONTEXT.new(person_type=PersonTypes.SW_YT, paysys=Paysyses.BANK_SW_YT_EUR)
EU_YT_CONTEXT = Contexts.DIRECT_FISH_SW_EUR_CONTEXT.new(person_type=PersonTypes.EU_YT, paysys=Paysyses.BANK_SW_YT_EUR)
SW_YTPH_CONTEXT = Contexts.DIRECT_FISH_SW_EUR_CONTEXT.new(person_type=PersonTypes.SW_YTPH,
                                                          paysys=Paysyses.BANK_SW_YTPH_EUR)
BY_YTPH_CONTEXT = Contexts.DIRECT_FISH_SW_EUR_CONTEXT.new(person_type=PersonTypes.BY_YTPH,
                                                          paysys=Paysyses.CC_BY_YTPH_RUB)
TR_UR_CONTEXT = Contexts.DIRECT_FISH_TRY_CONTEXT.new(person_type=PersonTypes.TRU)
TR_PH_CONTEXT = Contexts.DIRECT_FISH_TRY_CONTEXT.new(person_type=PersonTypes.TRP, paysys=Paysyses.BANK_TR_PH_TRY)
AM_UR_CONTEXT = Contexts.TAXI_FISH_RUB_CONTEXT.new(person_type=PersonTypes.AM_UR)
AM_PH_CONTEXT = Contexts.TAXI_FISH_RUB_CONTEXT.new(person_type=PersonTypes.AM_PH)
KZ_UR_CONTEXT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(person_type=PersonTypes.KZU, paysys=Paysyses.BANK_KZ_UR_TG)
KZ_PH_CONTEXT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(person_type=PersonTypes.KZP, paysys=Paysyses.BANK_KZ_PH_TG)

BY_PH_CONTEXT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(person_type=PersonTypes.BYP, paysys=Paysyses.BANK_BY_PH_BYN,
                                                     service=Services.GEO)
BY_UR_CONTEXT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(person_type=PersonTypes.BYU, paysys=Paysyses.BANK_BY_UR_BYN)

PROCEDURE_ATTRS_PERSON_TYPE_MAP = {'UR': {'CREATE_ORGANIZATION': [ATTRS.UR_ORGANIZATION],
                                          'CREATE_CUST_ACCOUNT': [ATTRS.COMMON_ACCOUNT,
                                                                  ATTRS.UR_ACCOUNT],
                                          'CREATE_PERSON': [ATTRS.UR_PERSON],
                                          'CREATE_ORG_CONTACT': [ATTRS.COMMON_ORG_CONTACT,
                                                                 ATTRS.UR_ORG_CONTACT],
                                          'CREATE_CUST_ACCOUNT_ROLE': [ATTRS.COMMON_CUST_ACCOUNT_ROLE],
                                          'CREATE_PHONE_CONTACT_POINT': [ATTRS.UR_PHONE_CONTACT,
                                                                         ATTRS.UR_FAX_CONTACT,
                                                                         ATTRS.UR_EMAIL_CONTACT],
                                          'CREATE_LOCATION': [ATTRS.COMMON_LOCATION_B,
                                                              ATTRS.UR_LOCATION_B,
                                                              ATTRS.COMMON_LOCATION_S,
                                                              ATTRS.UR_LOCATION_S,
                                                              ATTRS.COMMON_LOCATION_L,
                                                              ATTRS.UR_LOCATION_L],
                                          'CREATE_PARTY_SITE': [ATTRS.UR_PARTY_USE_B,
                                                                ATTRS.UR_PARTY_USE_S,
                                                                ATTRS.UR_PARTY_USE_L],
                                          'CREATE_PARTY_SITE_USE': [ATTRS.COMMON_PARTY_USE_SITE_B,
                                                                    ATTRS.COMMON_PARTY_USE_SITE_S,
                                                                    ATTRS.COMMON_PARTY_USE_SITE_L],
                                          'CREATE_CUST_ACCT_SITE': [ATTRS.COMMON_CUST_ACCT_SITE_B,
                                                                    ATTRS.COMMON_CUST_ACCT_SITE_S,
                                                                    ATTRS.COMMON_CUST_ACCT_SITE_L],
                                          'CREATE_CUST_SITE_USE': [ATTRS.COMMON_CUST_SITE_USE_B,
                                                                   ATTRS.COMMON_CUST_SITE_USE_S,
                                                                   ATTRS.COMMON_CUST_SITE_USE_L]
                                          },
                                   'UA': {'CREATE_ORGANIZATION': [ATTRS.UA_ORGANIZATION],
                                          'CREATE_CUST_ACCOUNT': [ATTRS.COMMON_ACCOUNT,
                                                                  ATTRS.UR_ACCOUNT],
                                          'CREATE_PERSON': [ATTRS.UR_PERSON],
                                          'CREATE_ORG_CONTACT': [ATTRS.COMMON_ORG_CONTACT],
                                          'CREATE_LOCATION': [ATTRS.COMMON_LOCATION_B]
                                          },
                                   'PH': {'CREATE_PERSON': [ATTRS.PH_PERSON],
                                          'CREATE_CUST_ACCOUNT': [ATTRS.COMMON_ACCOUNT,
                                                                  ATTRS.PH_ACCOUNT,
                                                                  ATTRS.BANK_TYPES],
                                          'CREATE_ORG_CONTACT': [],
                                          'CREATE_PHONE_CONTACT_POINT': [ATTRS.PH_PHONE_CONTACT,
                                                                         ATTRS.PH_FAX_CONTACT,
                                                                         ATTRS.PH_EMAIL_CONTACT
                                                                         ],
                                          'CREATE_LOCATION': [ATTRS.COMMON_LOCATION_B,
                                                              ATTRS.PH_LOCATION_B,
                                                              ATTRS.PH_LOCATION_L],
                                          'CREATE_PARTY_SITE': [ATTRS.YT_PARTY_USE_B,
                                                                ATTRS.PH_PARTY_USE_L],
                                          'CREATE_PARTY_SITE_USE': [ATTRS.COMMON_PARTY_USE_SITE_B,
                                                                    ATTRS.PH_PARTY_USE_SITE_L],
                                          'CREATE_CUST_ACCT_SITE': [ATTRS.COMMON_CUST_ACCT_SITE_B,
                                                                    ATTRS.PH_CUST_ACCT_SITE_L],
                                          'CREATE_CUST_SITE_USE': [ATTRS.COMMON_CUST_SITE_USE_B,
                                                                   ATTRS.PH_CUST_SITE_USE_L]
                                          },
                                   'YT': {'CREATE_ORGANIZATION': [ATTRS.YT_ORGANIZATION],
                                          'CREATE_CUST_ACCOUNT': [ATTRS.COMMON_ACCOUNT,
                                                                  ATTRS.YT_ACCOUNT],
                                          'CREATE_PERSON': [ATTRS.UR_PERSON],
                                          'CREATE_ORG_CONTACT': [ATTRS.COMMON_ORG_CONTACT,
                                                                 ATTRS.YT_ORG_CONTACT],
                                          'CREATE_CUST_ACCOUNT_ROLE': [ATTRS.COMMON_CUST_ACCOUNT_ROLE],
                                          'CREATE_PHONE_CONTACT_POINT': [ATTRS.UR_PHONE_CONTACT,
                                                                         ATTRS.UR_FAX_CONTACT,
                                                                         ATTRS.UR_EMAIL_CONTACT],
                                          'CREATE_LOCATION': [ATTRS.COMMON_LOCATION_B,
                                                              ATTRS.YT_LOCATION_B,
                                                              ATTRS.COMMON_LOCATION_L,
                                                              ATTRS.YT_LOCATION_L],
                                          'CREATE_PARTY_SITE': [ATTRS.YT_PARTY_USE_B,
                                                                ATTRS.YT_PARTY_USE_L],
                                          'CREATE_PARTY_SITE_USE': [ATTRS.COMMON_PARTY_USE_SITE_B,
                                                                    ATTRS.COMMON_PARTY_USE_SITE_L],
                                          'CREATE_CUST_ACCT_SITE': [ATTRS.COMMON_CUST_ACCT_SITE_B,
                                                                    ATTRS.COMMON_CUST_ACCT_SITE_L],
                                          'CREATE_CUST_SITE_USE': [ATTRS.COMMON_CUST_SITE_USE_B,
                                                                   ATTRS.COMMON_CUST_SITE_USE_L]
                                          },
                                   'EXT_YT': {'CREATE_ORGANIZATION': [ATTRS.YT_ORGANIZATION],
                                              'CREATE_CUST_ACCOUNT': [ATTRS.COMMON_ACCOUNT,
                                                                      ATTRS.YT_ACCOUNT,
                                                                      ATTRS.BANK_TYPES],
                                              'CREATE_PERSON': [ATTRS.UR_PERSON],
                                              'CREATE_ORG_CONTACT': [ATTRS.COMMON_ORG_CONTACT,
                                                                     ATTRS.YT_ORG_CONTACT],
                                              'CREATE_CUST_ACCOUNT_ROLE': [ATTRS.COMMON_CUST_ACCOUNT_ROLE],
                                              'CREATE_PHONE_CONTACT_POINT': [ATTRS.UR_PHONE_CONTACT,
                                                                             ATTRS.UR_FAX_CONTACT,
                                                                             ATTRS.UR_EMAIL_CONTACT],
                                              'CREATE_LOCATION': [ATTRS.COMMON_LOCATION_B,
                                                                  ATTRS.UR_LOCATION_B,
                                                                  ATTRS.COMMON_LOCATION_L,
                                                                  ATTRS.UR_LOCATION_L],
                                              'CREATE_PARTY_SITE': [ATTRS.YT_PARTY_USE_B,
                                                                    ATTRS.YT_PARTY_USE_L],
                                              'CREATE_PARTY_SITE_USE': [ATTRS.COMMON_PARTY_USE_SITE_B,
                                                                        ATTRS.COMMON_PARTY_USE_SITE_L],
                                              'CREATE_CUST_ACCT_SITE': [ATTRS.COMMON_CUST_ACCT_SITE_B,
                                                                        ATTRS.COMMON_CUST_ACCT_SITE_L],
                                              'CREATE_CUST_SITE_USE': [ATTRS.COMMON_CUST_SITE_USE_B,
                                                                       ATTRS.COMMON_CUST_SITE_USE_L]
                                              },
                                   }


def create_attrs_list(person_type):
    common_atts_list = []
    for procedure_name, attrs_list in PROCEDURE_ATTRS_PERSON_TYPE_MAP[person_type].iteritems():
        common_atts_list.extend(attrs_list)
    return common_atts_list


@pytest.mark.parametrize('context, attrs', [
    (UR_CONTEXT, create_attrs_list('UR')),
    (TR_UR_CONTEXT, create_attrs_list('UR')),
    (BY_UR_CONTEXT, create_attrs_list('UR')),

    # физики
    (PH_CONTEXT, create_attrs_list('PH')),
    (USA_PH_CONTEXT, create_attrs_list('PH')),
    (SW_PH_CONTEXT, create_attrs_list('PH')),
    (SW_YTPH_CONTEXT, create_attrs_list('PH')),
    (BY_YTPH_CONTEXT, create_attrs_list('PH')),
    (TR_PH_CONTEXT, create_attrs_list('PH')),
    (KZ_PH_CONTEXT, create_attrs_list('PH')),
    # после включения карт в беларуси
    # (BY_PH_CONTEXT, create_attrs_list('PH')),

    # нерезиденты
    (YT_CONTEXT, create_attrs_list('YT')),
    (USA_UR_CONTEXT, create_attrs_list('EXT_YT')),
    (YT_KZP_CONTEXT, create_attrs_list('EXT_YT')),
    (YT_KZU_CONTEXT, create_attrs_list('EXT_YT')),
    (KZ_UR_CONTEXT, create_attrs_list('EXT_YT')),
    (SW_UR_CONTEXT, create_attrs_list('EXT_YT')),
    (SW_YT_CONTEXT, create_attrs_list('EXT_YT'))

], ids=lambda context, attrs: context.person_type.code)
def export_person(context, attrs):
    client_id = steps.ClientSteps.create(prevent_oebs_export=True)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code, full=True,
                                         inn_type=person_defaults.InnType.UNIQUE)
    invoice_id = create_simple_invoice(client_id, person_id, context)
    steps.ExportSteps.export_oebs(client_id=client_id, person_id=person_id)
    invoice_firm_id = db.get_invoice_by_id(invoice_id)[0]['firm_id']
    check_attrs(person_id, attrs, firm_id=invoice_firm_id)
    steps.ExportSteps.export_oebs(person_id=person_id)


@pytest.mark.parametrize('context, attrs', [
    (AM_UR_CONTEXT.new(contract_type=ContractCommissionType.NO_AGENCY,
                       contract_params={
                           'SERVICES': [Services.TAXI.id],
                           'FIRM': Firms.TAXI_AM_26.id,
                           'PAYMENT_TYPE': ContractPaymentType.PREPAY
                       }), create_attrs_list('UR')),

    (AM_PH_CONTEXT.new(contract_type=ContractCommissionType.NO_AGENCY,
                       contract_params={
                           'SERVICES': [Services.TAXI.id],
                           'FIRM': Firms.TAXI_AM_26.id,
                           'PAYMENT_TYPE': ContractPaymentType.PREPAY
                       }), create_attrs_list('PH'))
], ids=lambda context, attrs: context.person_type.code)
def export_person_with_contract(context, attrs):
    client_id = steps.ClientSteps.create(prevent_oebs_export=True)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code,
                                         full=True,
                                         inn_type=person_defaults.InnType.UNIQUE)
    contract_id, _ = create_contract(client_id, person_id, context.contract_type, context.contract_params)
    steps.ExportSteps.export_oebs(client_id=client_id,
                                  person_id=person_id)
    check_attrs(person_id, attrs, firm_id=context.contract_params['FIRM'])


class EndbuyerContext(object):
    _EndbuyerContext = namedtuple('_EndbuyerContext', 'endbuyer_type')
    UR = _EndbuyerContext(PersonTypes.ENDBUYER_UR)
    PH = _EndbuyerContext(PersonTypes.ENDBUYER_PH)
    YT = _EndbuyerContext(PersonTypes.ENDBUYER_YT)


@pytest.mark.parametrize('endbuyer_context, attrs', [
    (EndbuyerContext.UR, create_attrs_list('UR')),
    (EndbuyerContext.PH, create_attrs_list('PH')),
    (EndbuyerContext.YT, create_attrs_list('YT')),
], ids=lambda endbuyer_context, attrs: endbuyer_context.endbuyer_type.code)
def export_endbuyer(endbuyer_context, attrs):
    endbuyer_id, invoice_id = create_endbuyer(endbuyer_context)

    invoice_firm_id = db.get_invoice_by_id(invoice_id)[0]['firm_id']
    check_attrs(endbuyer_id, attrs, firm_id=invoice_firm_id)


@pytest.mark.parametrize('context, attrs', [
    (utils.aDict(person_type=PersonTypes.UR, firm=Firms.YANDEX_1, ), create_attrs_list('UR')),
    (utils.aDict(person_type=PersonTypes.PH, firm=Firms.YANDEX_1), create_attrs_list('PH')),
    (utils.aDict(person_type=PersonTypes.YT, firm=Firms.YANDEX_1), create_attrs_list('YT')),
    (utils.aDict(person_type=PersonTypes.SW_YTPH, firm=Firms.EUROPE_AG_7), create_attrs_list('PH')),
    (utils.aDict(person_type=PersonTypes.SW_YT, firm=Firms.EUROPE_AG_7), create_attrs_list('EXT_YT')),
    (utils.aDict(person_type=PersonTypes.SW_UR, firm=Firms.EUROPE_AG_7), create_attrs_list('EXT_YT')),
    (utils.aDict(person_type=PersonTypes.YT_KZU, firm=Firms.YANDEX_1), create_attrs_list('EXT_YT')),
    (utils.aDict(person_type=PersonTypes.EU_YT, firm=Firms.TAXI_BV_22), create_attrs_list('YT')),
], ids=lambda context, attrs: context.person_type.code)
def export_person_rsya(context, attrs):
    client_id = steps.ClientSteps.create(prevent_oebs_export=True)
    person_id = steps.PersonSteps.create_partner(client_id, context.person_type.code, full=True,
                                                 inn_type=person_defaults.InnType.UNIQUE,
                                                 name_type=person_defaults.NameType.RANDOM)

    if context.person_type == PersonTypes.EU_YT:
        contract_id, _ = create_contract(client_id, person_id, 'spendable_corp_clients',
                                         {'FIRM': context.firm.id,
                                          'SERVICES': [Services.TAXI_CORP.id, Services.TAXI_CORP_PARTNERS.id]})
    else:
        contract_id, _ = create_contract_rsya(client_id, person_id, 'rsya_universal', {'FIRM': context.firm.id})

    steps.ExportSteps.export_oebs(client_id=client_id,
                                  person_id=person_id,
                                  w_person_log=True)

    check_attrs(person_id, attrs, firm_id=context.firm.id)


@pytest.mark.skip(reason='TODO')
@reporter.feature(Features.UI)
def test_export_intercompany_ur():
    # Создаём клиента, с заполненным параметром интеркомпани и счётом,
    # так как плательщик выгружается только вместе с счётом\договором\актом
    INTERCOMPANY_VALUE = 'RU10'
    client_id = None or steps.ClientSteps.create(prevent_oebs_export=True)
    steps.CommonSteps.set_extprops('Client', client_id, 'intercompany',
                                   {'value_str': INTERCOMPANY_VALUE})
    person_id = None or steps.PersonSteps.create(client_id, UR_CONTEXT.person_type.code,
                                                 inn_type=person_defaults.InnType.UNIQUE)
    create_simple_invoice(client_id, person_id, UR_CONTEXT)
    check_attrs(person_id, ['intercompany'])

    OTHER_INTERCOMPANY_VALUE = 'RU20'
    with web.Driver() as driver:
        page = web.AdminClientEditPage.open(client_id, driver)
        intercompany_option = driver.find_element(
            *web.AdminClientEditPage.INTERCOMPANY_DROPDOWN_FILTER(
                intercompany=OTHER_INTERCOMPANY_VALUE))
        intercompany_option._element.click()
        save_button = driver.find_element(*web.AdminClientEditPage.SAVE_BUTTON)
        save_button.click()

    query = "SELECT state FROM t_export WHERE type = 'OEBS' AND classname = 'Person' AND object_id = :object_id"
    steps.CommonSteps.wait_for(query, {'object_id': person_id})
    # TODO: check for person in export
    # TODO: wait for export
    check_attrs(person_id, [ATTRS.CUSTOM.intercompany])


# проверяем перевыгрузку признака невалидные банковские реквизиты в каждой процедуре выгрузки
@pytest.mark.parametrize('context', [
    UR_CONTEXT,
    YT_CONTEXT,
    SW_UR_CONTEXT,
], ids=lambda context: context.person_type.code)
def test_reexport_invalid_bankprops(context):
    client_id = None or steps.ClientSteps.create(prevent_oebs_export=True)
    person_id = None or steps.PersonSteps.create(client_id, context.person_type.code,
                                                 params={'invalid-bankprops': '1'},
                                                 inn_type=person_defaults.InnType.UNIQUE)

    extprops = steps.CommonSteps.get_extprops('Person', person_id, 'invalid_bankprops')
    utils.check_that(extprops, contains_dicts_with_entries([{'attrname': 'invalid_bankprops', 'value_num': 1}]))

    invoice_id = create_simple_invoice(client_id, person_id, context)
    steps.ExportSteps.export_oebs(client_id=client_id, person_id=person_id)
    invoice_firm_id = db.get_invoice_by_id(invoice_id)[0]['firm_id']

    check_attrs(person_id, [ATTRS.COMMON_ACCOUNT.invalid_bankprops], invoice_firm_id)

    steps.CommonSteps.set_extprops('Person', person_id, 'invalid_bankprops', {'value_num': 0})
    steps.ExportSteps.export_oebs(person_id=person_id)

    check_attrs(person_id, [ATTRS.COMMON_ACCOUNT.invalid_bankprops], invoice_firm_id)


@pytest.mark.parametrize('context', [
    (utils.aDict(person_type=PersonTypes.SW_YTPH, firm=Firms.EUROPE_AG_7)),
], ids=lambda context: context.person_type.code)
def test_reexport_invalid_bankprops_partner(context):
    client_id = steps.ClientSteps.create(prevent_oebs_export=True)
    person_id = steps.PersonSteps.create_partner(client_id, context.person_type.code,
                                                 params={'invalid-bankprops': '1'},
                                                 inn_type=person_defaults.InnType.UNIQUE)

    extprops = steps.CommonSteps.get_extprops('Person', person_id, 'invalid_bankprops')
    utils.check_that(extprops, contains_dicts_with_entries([{'attrname': 'invalid_bankprops', 'value_num': 1}]))

    contract_id, _ = create_contract_rsya(client_id, person_id, 'rsya_universal', {'FIRM': context.firm.id})

    steps.ExportSteps.export_oebs(client_id=client_id, person_id=person_id, contract_id=contract_id)

    check_attrs(person_id, [ATTRS.COMMON_ACCOUNT.invalid_bankprops], firm_id=context.firm.id)

    steps.CommonSteps.set_extprops('Person', person_id, 'invalid_bankprops', {'value_num': 0})
    steps.ExportSteps.export_oebs(person_id=person_id)

    check_attrs(person_id, [ATTRS.COMMON_ACCOUNT.invalid_bankprops], firm_id=context.firm.id)


@pytest.mark.parametrize('context, attrs', [
    (AM_UR_CONTEXT.new(contract_type=ContractCommissionType.NO_AGENCY,
                       contract_params={
                           'SERVICES': [Services.TAXI.id],
                           'FIRM': Firms.TAXI_AM_26.id,
                           'PAYMENT_TYPE': ContractPaymentType.PREPAY
                       },
                       additional_person_params={'local_name': 'local_name համաձայանգրով սահմանված ծառայություններ',
                                                 'local_longname': ' local_longnameհամաձայանգրով սահմանված ծառայություններ',
                                                 'local_postaddress': 'local_postaddress համաձայանգրով սահմանված ծառայություններ Жоғары',
                                                 'local_ben_bank': 'local_ben_bank жігіт համաձայանգրով սահմանված ծառայություններ',
                                                 'local_legaladdress': 'local_legaladdress համաձայանգրով սահմանված ծառայություններ Жоғары',
                                                 'local_city': ' local_city жігіт համաձայանգրով սահմանված ծառայություններ',
                                                 'local_representative': 'local_representative համաձայանգրով սահմանված ծառայություններ Жоғары'}
                       ), create_attrs_list('UR'))],
                         ids=lambda context, attrs: context.person_type.code)
def export_person_with_contract_duo_language(context, attrs):
    client_id = steps.ClientSteps.create(prevent_oebs_export=True)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code,
                                         params=context.additional_person_params,
                                         full=True,
                                         inn_type=person_defaults.InnType.UNIQUE)
    contract_id, _ = create_contract(client_id, person_id, context.contract_type, context.contract_params)
    steps.ExportSteps.export_oebs(client_id=client_id,
                                  person_id=person_id,
                                  contract_id=contract_id)
    check_attrs(person_id, attrs, firm_id=context.contract_params['FIRM'])


@pytest.mark.parametrize('context, attrs', [
    (KZ_UR_CONTEXT.new(additional_person_params={'local_name': 'жігіт Жоғары',
                                                 'local_city': 'жігіт Жоғары',
                                                 'local_postaddress': 'жігіт Жоғары',
                                                 'local_legaladdress': 'жігіт Жоғары',
                                                 'local_bank': 'жігіт Жоғары',
                                                 'local_signer_person_name': 'жігіт Жоғары',
                                                 'local_signer_position_name': 'жігіт Жоғары',
                                                 'local_authority_doc_details': 'жігіт Жоғары'}),
     create_attrs_list('EXT_YT')),
], ids=lambda context, attrs: context.person_type.code)
def export_person_duo_language(context, attrs):
    client_id = steps.ClientSteps.create(prevent_oebs_export=True)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code, params=context.additional_person_params,
                                         full=True,
                                         inn_type=person_defaults.InnType.UNIQUE)
    invoice_id = create_simple_invoice(client_id, person_id, context)
    steps.ExportSteps.export_oebs(client_id=client_id, person_id=person_id)
    invoice_firm_id = db.get_invoice_by_id(invoice_id)[0]['firm_id']
    check_attrs(person_id, attrs, firm_id=invoice_firm_id)
    steps.ExportSteps.export_oebs(person_id=person_id)


EU_YT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(person_type=PersonTypes.EU_YT, contract_params={
    'FIRM': Firms.TAXI_BV_22.id,
    'CURRENCY': Currencies.USD.num_code,
    'COUNTRY': 10077,
    'NDS': Nds.NOT_RESIDENT,
    'SERVICES': [Services.TAXI_CORP.id, Services.TAXI_CORP_PARTNERS.id]},
                                             contract_type='spendable_corp_clients',
                                             additional_person_params={'local_name': 'local_nameжігіт Жоғары',
                                                                       'local_representative': 'local_representativeжігіт Жоғары',
                                                                       'local_postaddress': 'local_postaddressжігіт Жоғары',
                                                                       'local_ben_bank': 'local_ben_bankжігіт Жоғары',
                                                                       'local_other': 'local_other Жоғары'})


@pytest.mark.parametrize('context', [EU_YT])
def test_get_client_contracts_spendable(context):
    client_id = steps.ClientSteps.create()
    person_params = context.additional_person_params
    person_id = steps.PersonSteps.create(client_id, context.person_type.code, params=person_params)
    contract_id, _ = create_contract(client_id, person_id, context.contract_type, context.contract_params)
    steps.ExportSteps.export_oebs(client_id=client_id,
                                  person_id=person_id,
                                  contract_id=contract_id)


@pytest.mark.parametrize('context, edo_offers, edo_offers_oebs', [

    (UR_CONTEXT, [{'firm_id': Firms.YANDEX_1, 'from_dt': NOW, 'offer_type': 1}],
     [{'firm_id': Firms.YANDEX_1, 'rows': [{'date_start': utils.Date.nullify_time_of_date(NOW),
                                            'date_end': None,
                                            'edo_operator': '2BE',
                                            'edo_type': 1}]
       }]),

    (UR_CONTEXT, [{'firm_id': Firms.YANDEX_1, 'from_dt': NOW, 'offer_type': 2}],
     [{'firm_id': Firms.YANDEX_1, 'rows': [{'date_start': utils.Date.nullify_time_of_date(NOW),
                                            'date_end': None,
                                            'edo_operator': '2BE',
                                            'edo_type': 2}]
       }]),

    (UR_CONTEXT, [{'firm_id': Firms.YANDEX_1, 'from_dt': NOW, 'offer_type': 3}],
     [{'firm_id': Firms.YANDEX_1, 'rows': [{'date_start': utils.Date.nullify_time_of_date(NOW),
                                            'date_end': None,
                                            'edo_operator': '2BE',
                                            'edo_type': 3}]
       }]),

    (UR_CONTEXT, [{'firm_id': Firms.YANDEX_1, 'from_dt': NOW},
                  {'firm_id': Firms.VERTICAL_12, 'from_dt': NOW}],
     [{'firm_id': Firms.YANDEX_1, 'rows': [{'date_start': utils.Date.nullify_time_of_date(NOW),
                                            'date_end': None,
                                            'edo_operator': '2BE'}
                                           ]},
      {'firm_id': Firms.VERTICAL_12, 'rows': [{'date_start': utils.Date.nullify_time_of_date(NOW),
                                               'date_end': None,
                                               'edo_operator': '2BE'}
                                              ]}
      ]),

    (UR_CONTEXT, [{'firm_id': Firms.YANDEX_1, 'from_dt': NOW},
                  {'firm_id': Firms.YANDEX_1, 'from_dt': NOW + datetime.timedelta(hours=1)}],
     [{'firm_id': Firms.YANDEX_1, 'rows': [{'date_start': utils.Date.nullify_time_of_date(NOW),
                                            'date_end': None,
                                            'edo_operator': '2BE'}
                                           ]}
      ]),

    (UR_CONTEXT, [{'firm_id': Firms.YANDEX_1, 'from_dt': NOW},
                  {'firm_id': Firms.YANDEX_1, 'from_dt': NOW - datetime.timedelta(days=1)}],
     [{'firm_id': Firms.YANDEX_1,
       'rows': [{'date_start': utils.Date.nullify_time_of_date(NOW - datetime.timedelta(days=1)),
                 'date_end': utils.Date.nullify_time_of_date(NOW - datetime.timedelta(days=1)),
                 'edo_operator': '2BE'},
                {'date_start': utils.Date.nullify_time_of_date(NOW),
                 'date_end': None,
                 'edo_operator': '2BE'}
                ]}
      ]),
    (UR_CONTEXT, [{'firm_id': Firms.YANDEX_1, 'from_dt': NOW},
                  {'firm_id': Firms.YANDEX_1, 'from_dt': NOW + datetime.timedelta(hours=1), 'edo_type': None}],
     [{'firm_id': Firms.YANDEX_1,
       'rows': [{'date_start': utils.Date.nullify_time_of_date(NOW),
                 'date_end': None,
                 'edo_operator': None}
                ]}
      ])
])
def get_export_person_edo_offer(context, edo_offers, edo_offers_oebs):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code,
                                         inn_type=person_defaults.InnType.UNIQUE)
    person = db.get_person_by_id(person_id)[0]
    person_inn = person['inn']
    person_kpp = person['kpp']
    db.balance().execute('''DELETE FROM t_edo_offer_cal WHERE PERSON_INN = :person_inn AND PERSON_KPP = :person_kpp''',
                         {'person_inn': person_inn, 'person_kpp': person_kpp})
    for offer in edo_offers:
        steps.PersonSteps.accept_edo(firm_id=offer['firm_id'].id, person_id=person_id, from_dt=offer['from_dt'],
                                     edo_type_id=offer.get('edo_type', 1), offer_type=offer.get('offer_type', 1))
    invoice_id = create_simple_invoice(client_id, person_id, context)
    if balance_config.ENABLE_SINGLE_ACCOUNT:
        invoice_id = db.get_invoice_by_charge_note_id(invoice_id)[0]['id']
    steps.ExportSteps.export_oebs(client_id=client_id,
                                  person_id=person_id,
                                  invoice_id=invoice_id)
    for offer_oebs in edo_offers_oebs:
        query = '''SELECT * FROM apps.xxar_edo_oper_hist_tbl WHERE CUSTOMER_ID =
                  (SELECT CUST_ACCOUNT_ID FROM hz_cust_accounts WHERE ACCOUNT_NUMBER = :person_id)'''
        offer_oebs_db = db.oebs().execute_oebs(offer_oebs['firm_id'].id, query, {'person_id': 'P' + str(person_id)},
                                               single_row=False)
        org_id = \
            db.balance().execute(
                '''SELECT OEBS_ORG_ID FROM T_FIRM_EXPORT WHERE EXPORT_TYPE = 'OEBS' AND firm_id = :firm_id''',
                {'firm_id': offer_oebs['firm_id'].id})[0]['oebs_org_id']
        for row in offer_oebs['rows']:
            row['org_id'] = org_id
        utils.check_that(offer_oebs_db, matchers.contains_dicts_with_entries(offer_oebs['rows'],
                                                                             same_length=True))
    person_id = steps.PersonSteps.create(client_id, context.person_type.code, {'person_id': person_id,
                                                                               'inn': person_inn,
                                                                               'kpp': str(int(person_kpp) + 1)})
    steps.ExportSteps.export_oebs(person_id=person_id)
    for offer_oebs in edo_offers_oebs:
        query = '''SELECT * FROM apps.xxar_edo_oper_hist_tbl WHERE CUSTOMER_ID =
                  (SELECT CUST_ACCOUNT_ID FROM hz_cust_accounts WHERE ACCOUNT_NUMBER = :person_id)'''
    offer_oebs_db = db.oebs().execute_oebs(offer_oebs['firm_id'].id, query, {'person_id': 'P' + str(person_id)},
                                           single_row=False)
    org_id = \
        db.balance().execute(
            '''SELECT OEBS_ORG_ID FROM T_FIRM_EXPORT WHERE EXPORT_TYPE = 'OEBS' AND firm_id = :firm_id''',
            {'firm_id': offer_oebs['firm_id'].id})[0]['oebs_org_id']
    for row in offer_oebs['rows']:
        row['org_id'] = org_id
    utils.check_that(offer_oebs_db, matchers.contains_dicts_with_entries([],
                                                                         same_length=True))


@pytest.mark.parametrize('context, edo_offers, edo_offers_oebs', [

    (UR_CONTEXT, [],
     [{'firm_id': Firms.YANDEX_1, 'rows': [{'date_start': utils.Date.nullify_time_of_date(NOW),
                                            'date_end': None,
                                            'edo_operator': '2BE',
                                            'edo_type': 1}]
       }])
])
def test_get_export_person_without_offer(context, edo_offers, edo_offers_oebs):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code,
                                         inn_type=person_defaults.InnType.UNIQUE)
    person = db.get_person_by_id(person_id)[0]
    person_inn = person['inn']
    person_kpp = person['kpp']
    db.balance().execute(
        '''DELETE FROM t_edo_offer_cal WHERE PERSON_INN = :person_inn AND PERSON_KPP = :person_kpp''',
        {'person_inn': person_inn, 'person_kpp': person_kpp})
    invoice_id = create_simple_invoice(client_id, person_id, context)
    steps.ExportSteps.export_oebs(client_id=client_id,
                                  person_id=person_id,
                                  invoice_id=invoice_id)
    for offer_oebs in edo_offers_oebs:
        query = '''SELECT * FROM apps.xxar_edo_oper_hist_tbl WHERE CUSTOMER_ID =
                 (SELECT CUST_ACCOUNT_ID FROM hz_cust_accounts WHERE ACCOUNT_NUMBER = :person_id)'''
        offer_oebs_db = db.oebs().execute_oebs(offer_oebs['firm_id'].id, query, {'person_id': 'P' + str(person_id)},
                                               single_row=False)
        org_id = \
            db.balance().execute(
                '''SELECT OEBS_ORG_ID FROM T_FIRM_EXPORT WHERE EXPORT_TYPE = 'OEBS' AND firm_id = :firm_id''',
                {'firm_id': offer_oebs['firm_id'].id})[0]['oebs_org_id']
        for row in offer_oebs['rows']:
            row['org_id'] = org_id
        utils.check_that(offer_oebs_db, matchers.equal_to([]))
    person_id = steps.PersonSteps.create(client_id, context.person_type.code, {'person_id': person_id,
                                                                               'inn': person_inn,
                                                                               'kpp': str(int(person_kpp) + 1)})

    print steps.ExportSteps.get_export_data(person_id, classname='Person', queue_type='OEBS_API')['input']
    query = '''SELECT * FROM apps.xxar_edo_oper_hist_tbl WHERE CUSTOMER_ID =
                         (SELECT CUST_ACCOUNT_ID FROM hz_cust_accounts WHERE ACCOUNT_NUMBER = :person_id)'''

    offer_oebs_db = db.oebs().execute_oebs(offer_oebs['firm_id'].id, query, {'person_id': 'P' + str(person_id)},
                                           single_row=False)


@pytest.mark.parametrize('context, attrs', [
    (TR_UR_CONTEXT, create_attrs_list('UR'))], ids=lambda context, attrs: context.person_type.code)
def test_export_persons_non_unique_inn(context, attrs):
    client_id = steps.ClientSteps.create(prevent_oebs_export=True)
    person_params = {'inn_doc_details': '24234'}
    person_id = steps.PersonSteps.create(client_id, context.person_type.code, params=person_params,
                                         full=True,
                                         inn_type=person_defaults.InnType.UNIQUE)
    invoice_id = create_simple_invoice(client_id, person_id, context)
    invoice_firm_id = db.get_invoice_by_id(invoice_id)[0]['firm_id']
    # check_attrs(person_id, attrs, firm_id=invoice_firm_id)
    steps.ExportSteps.export_oebs(person_id=person_id)
    person = db.get_person_by_id(person_id)[0]

    client_id = steps.ClientSteps.create(prevent_oebs_export=True)
    person_params = person_defaults.get_details(context.person_type.code, full=True)
    person_params['inn'] = person['inn']
    person_params['inn_doc_details'] = str(int(person['inn_doc_details']) + 1)

    person_id = steps.PersonSteps.create(client_id, context.person_type.code, params=person_params)
    invoice_id = create_simple_invoice(client_id, person_id, context)
    invoice_firm_id = db.get_invoice_by_id(invoice_id)[0]['firm_id']
    # check_attrs(person_id, attrs, firm_id=invoice_firm_id)
    steps.ExportSteps.export_oebs(person_id=person_id)


@pytest.mark.parametrize('person_type', ['ph', 'ur', 'yt'])
@pytest.mark.tickets('BALANCE-28899')
def first_person_export(person_type):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, person_type)
    steps.ExportSteps.export_oebs(person_id=person_id)


@pytest.mark.tickets('BALANCE-27813')
def export_person_to_close_firm():
    with reporter.step(u'Проверяем, что фирма Авто.ру закрыта'):
        active = db.balance().execute("SELECT ACTIVE FROM v_firm_export WHERE FIRM_ID = 10")[0]['active']
        utils.check_that(active, equal_to(0))

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')

    query = "insert into T_PERSON_FIRM (PERSON_ID, FIRM_ID) values (:person_id, 10)"
    db.balance().execute(query, {'person_id': person_id})

    steps.ExportSteps.export_oebs(client_id=client_id, person_id=person_id)
    re_output = db.balance().execute("SELECT OUTPUT FROM T_EXPORT "
                                     "WHERE CLASSNAME = 'Person' AND OBJECT_ID = :person_id",
                                     {'person_id': person_id})[0]['output']
    utils.check_that(re_output, equal_to('No firms'), step=u'Проверяем выгрузку плательщика')


@pytest.mark.parametrize('context, attrs', [
    (UR_CONTEXT, create_attrs_list('UR')),
], ids=lambda context, attrs: context.person_type.code)
def export_selfemployed_person(context, attrs):
    client_id = steps.ClientSteps.create(prevent_oebs_export=True)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code, full=True,
                                         inn_type=person_defaults.InnType.UNIQUE,
                                         params={'ownership_type': 'SELFEMPLOYED'}
                                         )
    invoice_id = create_simple_invoice(client_id, person_id, context)
    steps.ExportSteps.export_oebs(client_id=client_id, person_id=person_id)
    if balance_config.ENABLE_SINGLE_ACCOUNT:
        invoice_id = db.get_invoice_by_charge_note_id(invoice_id)[0]['id']
    invoice_firm_id = db.get_invoice_by_id(invoice_id)[0]['firm_id']
    check_attrs(person_id, attrs, firm_id=invoice_firm_id)
    steps.ExportSteps.export_oebs(person_id=person_id)


@pytest.mark.parametrize('context, attrs', [
    (utils.aDict(person_type=PersonTypes.UR, firm=Firms.YANDEX_1, ), create_attrs_list('UR')),
], ids=lambda context, attrs: context.person_type.code)
def export_selfemployed_person_rsya(context, attrs):
    client_id = steps.ClientSteps.create(prevent_oebs_export=True)
    person_id = steps.PersonSteps.create_partner(client_id, context.person_type.code, full=True,
                                                 inn_type=person_defaults.InnType.UNIQUE,
                                                 name_type=person_defaults.NameType.RANDOM,
                                                 params={'ownership_type': 'SELFEMPLOYED'}
                                                 )

    contract_id, _ = create_contract_rsya(client_id, person_id, 'rsya_universal', {'FIRM': context.firm.id})

    steps.ExportSteps.export_oebs(client_id=client_id,
                                  person_id=person_id)
    check_attrs(person_id, attrs, firm_id=context.firm.id)
    steps.ExportSteps.export_oebs(person_id=person_id)


@pytest.mark.parametrize('bank_type_context', [
    utils.aDict(bank_type=3, wallet_name='yamoney-wallet'),
    utils.aDict(bank_type=4, wallet_name='webmoney-wallet'),
    utils.aDict(bank_type=5, wallet_name='paypal-wallet'),
    utils.aDict(bank_type=7, wallet_name='payoneer-wallet'),
    utils.aDict(bank_type=8, wallet_name='pingpong-wallet'),
], ids=lambda bank_type_context: bank_type_context.wallet_name)
@pytest.mark.parametrize('context, attrs', [
    (TOLOKA_CONTEXT.new(is_partner='1'), create_attrs_list('PH')),
], ids=lambda context, attrs: '%s-%s' % (context.name, context.is_partner))
def _export_bank_type(bank_type_context, context, attrs):
    _, _, _, _, second_month_start_dt, _ = utils.Date.previous_three_months_start_end_dates()

    client_id = steps.ClientSteps.create(prevent_oebs_export=True)
    person_id = steps.PersonSteps.create(
        client_id,
        context.person_type.code,
        full=True,
        params={
            'is-partner': context.is_partner,
            'bank-type': str(bank_type_context.bank_type),
            bank_type_context.wallet_name: '123456789',
        }
    )

    client_id, person_id, contract_id, _ = \
        steps.ContractSteps.create_partner_contract(
            context,
            client_id=client_id,
            person_id=person_id,
            additional_params={'start_dt': second_month_start_dt}
        )

    steps.ExportSteps.export_oebs(client_id=client_id, person_id=person_id,contract_id=contract_id)
    check_attrs(person_id, attrs, firm_id=context.firm.id)
