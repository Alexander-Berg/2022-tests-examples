# -*- coding: utf-8 -*-

import datetime
import json
import os

import pytest
from enum import Enum

import btestlib.reporter as reporter
from balance import balance_db as db
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils as utils
from btestlib.constants import PersonTypes, Firms
from btestlib.data import person_defaults
from btestlib.matchers import equal_to_casted_dict
from export_commons import (get_oebs_party_site_use_wo_site_use_type, Locators, read_attr_values, get_oebs_party_id,
                            get_oebs_person_cust_account_id, get_oebs_party_site_id,
                            get_oebs_location_id, get_oebs_cust_acct_site_id, attrs_list_to_dict, get_oebs_party_id_new,
                            get_oebs_party_site_id_new)
from btestlib import config as balance_config

try:
    import balance_contracts
    from balance_contracts.oebs.person import replace_mask
    from balance_contracts.contract_utils import utils as contract_utils
    json_contracts_repo_path = os.path.dirname(os.path.abspath(balance_contracts.__file__))
except ImportError as err:
    json_contracts_repo_path = ''

JSON_OEBS_PATH = '/oebs/person/'

W_API = True

NOW = datetime.datetime.now().replace(hour=1)

pytestmark = [reporter.feature(Features.OEBS, Features.PERSON),
              pytest.mark.docpath('https://wiki.yandex-team.ru/balance/docs/process/oebs')]

# ============================================ ATTRIBUTES ============================================


balance_str_locators = set()
oebs_str_locators = set()


def balance_locator(locator):
    def deco(f):
        balance_str_locators.add(locator)
        return f

    return deco


class NoSuchRowInOEBS(object):
    msg = 'NON EXISTED VALUE'


class ATTRS(object):
    class HZ_CUST_ACCOUNTS(Enum):
        account_number = \
            Locators(balance=lambda b: 'P' + str(b['t_person.id']),
                     oebs=lambda o: o['hz_cust_accounts.account_number'])

        hold_bill_flag = \
            Locators(balance=lambda b: b.get('t_extprops.person.invalid_bankprops', 0),
                     oebs=lambda o: 1 if o['hz_cust_accounts.hold_bill_flag'] == 'Y' else 0)

        account_name = \
            Locators(balance=lambda b: get_person_name(b),
                     oebs=lambda o: o['hz_cust_accounts.account_name'])

        attribute1 = \
            Locators(balance=lambda b: live_signature_to_oebs_format(b['t_person.live_signature']) if
            b['t_person.type'] != 'kzu' else 'LIVE',
                     oebs=lambda o: o['hz_cust_accounts.attribute1'])

        attribute2 = \
            Locators(balance=lambda b: get_resident_value(b),
                     oebs=lambda o: o['hz_cust_accounts.attribute2'])

        attribute3 = \
            Locators(
                balance=lambda b: get_delivery_type(b),
                oebs=lambda o: o['hz_cust_accounts.attribute3'])

        attribute4 = \
            Locators(balance=lambda b: get_longname(b),
                     oebs=lambda o: o['hz_cust_accounts.attribute4'])

        attribute5 = \
            Locators(balance=lambda b: b['t_person.account'],
                     oebs=lambda o: o['hz_cust_accounts.attribute5'])

        attribute6 = \
            Locators(
                balance=lambda b: str(b['t_extprops.person.vip']) if b.get('t_extprops.person.vip', False) else '0',
                oebs=lambda o: o['hz_cust_accounts.attribute6'])

        attribute7 = \
            Locators(balance=lambda b: get_wallet_number(b),
                     oebs=lambda o: o['hz_cust_accounts.attribute7'])

        attribute8 = \
            Locators(balance=lambda b: get_bik(b),
                     oebs=lambda o: o['hz_cust_accounts.attribute8'])

        attribute11 = \
            Locators(balance=lambda b: get_person_kpp(b),
                     oebs=lambda o: o['hz_cust_accounts.attribute11'])

        attribute13 = \
            Locators(balance=lambda b: get_person_account(b),
                     oebs=lambda o: o['hz_cust_accounts.attribute13'])

        attribute14 = \
            Locators(balance=lambda b: get_bank_type(b),
                     oebs=lambda o: o['hz_cust_accounts.attribute14'])

        attribute16 = \
            Locators(balance=lambda b: get_intercompany(b),
                     oebs=lambda o: o['hz_cust_accounts.attribute16'])

        attribute17 = \
            Locators(balance=lambda b: get_early_docs(b),
                     oebs=lambda o: o['hz_cust_accounts.attribute17'])

    class XXAR_EDO_OPER_HIST_TBL(Enum):
        # список значений
        values = \
            Locators(balance=lambda b: get_edo_offers(b),
                     oebs=lambda o: o['xxar_edo_oper_hist_tbl.values'])

    class XXAR_CUSTOMER_ATTRIBUTES(Enum):
        bank_address = \
            Locators(balance=lambda b: get_bankcity(b),
                     oebs=lambda o: o['xxar_customer_attributes.bank_address'])

        ben_account = \
            Locators(balance=lambda b: b.get('t_extprops.person.ben_account', None),
                     oebs=lambda o: o['xxar_customer_attributes.account_name_eng'])

        kbk = \
            Locators(balance=lambda b: get_kbk(b),
                     oebs=lambda o: o['xxar_customer_attributes.kbk_code'])

        oktmo = \
            Locators(balance=lambda b: get_oktmo(b),
                     oebs=lambda o: o['xxar_customer_attributes.oktmo_code'])

        payment_purpose = \
            Locators(balance=lambda b: b.get('t_contract_attributes.payment_purpose'),
                     oebs=lambda o: o['xxar_customer_attributes.added_pay_purpose'])

        iban = \
            Locators(balance=lambda b: get_iban(b),
                     oebs=lambda o: o['xxar_customer_attributes.iban_number'])

        swift = \
            Locators(balance=lambda b: get_swift(b),
                     oebs=lambda o: o['xxar_customer_attributes.swift_code'])

        ben_bank = \
            Locators(
                balance=lambda b: get_ben_bank(b),
                oebs=lambda o: o['xxar_customer_attributes.bank_name_eng'])

        beneficiar_code = \
            Locators(balance=lambda b: get_beneficiar_code(b),
                     oebs=lambda o: o['xxar_customer_attributes.beneficiar_code'])

        tax_reg_number = \
            Locators(balance=lambda b: get_tax_reg_number(b),
                     oebs=lambda o: o['xxar_customer_attributes.tax_reg_number'])

        bank_type = \
            Locators(balance=lambda b: get_bank_type(b),
                     oebs=lambda o: o['xxar_customers.attribute14'])

        corr_swift = \
            Locators(balance=lambda b: b.get('t_extprops.person.corr_swift', None),
                     oebs=lambda o: o['xxar_customer_attributes.correspondent_swift'])

        ownership_type = \
            Locators(balance=lambda b: get_ownership_type(b),
                     oebs=lambda o: o['xxar_customer_attributes.customer_type'])

        other = \
            Locators(balance=lambda b: b.get('t_extprops.person.other', None),
                     oebs=lambda o: o['xxar_customer_attributes.additional_payment_details'])

    class HZ_PARTIES_CP(Enum):
        repr_person_last_name = \
            Locators(balance=lambda b: get_repr_person_last_name(b),
                     oebs=lambda o: o['hz_parties.cp.person_last_name'])

    class HZ_PARTIES_DP(Enum):
        sign_person_last_name = \
            Locators(balance=lambda b: get_sign_person_last_name(b),
                     oebs=lambda o: o.get('hz_parties.dp.person_last_name'))

    class HZ_PARTIES_I(Enum):
        party_name = \
            Locators(balance=lambda b: get_ur_party_name(b),
                     oebs=lambda o: o['hz_parties.i.party_name'])

        party_type = \
            Locators(balance=lambda b: get_ur_party_type(b),
                     oebs=lambda o: o['hz_parties.i.party_type'])

        jgzz_fiscal_code = \
            Locators(balance=lambda b: get_ur_person_inn(b),
                     oebs=lambda o: o['hz_parties.i.jgzz_fiscal_code'])
        tax_reference = \
            Locators(balance=lambda b: get_person_inn_details(b),
                     oebs=lambda o: o['hz_parties.i.tax_reference'])

    class HZ_PARTIES_P(Enum):
        party_name = \
            Locators(balance=lambda b: get_non_ur_party_name(b),
                     oebs=lambda o: o['hz_parties.p.party_name'])

        party_type = \
            Locators(balance=lambda b: get_non_ur_party_type(b),
                     oebs=lambda o: o['hz_parties.p.party_type'])

        jgzz_fiscal_code = \
            Locators(balance=lambda b: get_non_ur_person_inn(b),
                     oebs=lambda o: o['hz_parties.p.jgzz_fiscal_code'])

        person_first_name = \
            Locators(balance=lambda b: get_fname(b),
                     oebs=lambda o: o['hz_parties.p.person_first_name'])

        person_middle_name = \
            Locators(balance=lambda b: get_mname(b),
                     oebs=lambda o: o['hz_parties.p.person_middle_name'])

        person_last_name = \
            Locators(balance=lambda b: get_lname(b),
                     oebs=lambda o: o['hz_parties.p.person_last_name'])

    class HZ_ORG_CONTACTS_REPR_PC(Enum):
        job_title_code = \
            Locators(balance=lambda b: get_job_title_code(b),
                     oebs=lambda o: o['hz_org_contacts.pc.job_title_code'])
        job_title = \
            Locators(balance=lambda b: get_job_title(b),
                     oebs=lambda o: o['hz_org_contacts.pc.job_title'])

    class HZ_RELATIONSHIPS_CP(Enum):
        subject_type = \
            Locators(balance=lambda b: get_subject_type(b),
                     oebs=lambda o: o['hz_relationships.cp.subject_type'])

        subject_table_name = \
            Locators(balance=lambda b: get_subject_table_name(b),
                     oebs=lambda o: o['hz_relationships.cp.subject_table_name'])

        object_type = \
            Locators(balance=lambda b: get_object_type(b),
                     oebs=lambda o: o['hz_relationships.cp.object_type'])

        object_table_name = \
            Locators(balance=lambda b: get_object_table_name(b),
                     oebs=lambda o: o['hz_relationships.cp.object_table_name'])

        relationship_code = \
            Locators(balance=lambda b: get_relationship_code(b),
                     oebs=lambda o: o['hz_relationships.cp.relationship_code'])

        relationship_type = \
            Locators(balance=lambda b: get_relationship_type(b),
                     oebs=lambda o: o['hz_relationships.cp.relationship_type'])

        object_id = \
            Locators(balance=lambda b: get_object_id(b),
                     oebs=lambda o: o['hz_relationships.cp.object_id'])

    class HZ_RELATIONSHIPS_DP(Enum):
        subject_type = \
            Locators(balance=lambda b: get_subject_type_signer(b),
                     oebs=lambda o: o['hz_relationships.dp.subject_type'])

        subject_table_name = \
            Locators(balance=lambda b: get_subject_table_name_signer(b),
                     oebs=lambda o: o['hz_relationships.dp.subject_table_name'])

        object_type = \
            Locators(balance=lambda b: get_object_type_signer(b),
                     oebs=lambda o: o['hz_relationships.dp.object_type'])

        object_table_name = \
            Locators(balance=lambda b: get_object_table_name_signer(b),
                     oebs=lambda o: o['hz_relationships.dp.object_table_name'])

        relationship_code = \
            Locators(balance=lambda b: get_relationship_code_signer(b),
                     oebs=lambda o: o['hz_relationships.dp.relationship_code'])

        relationship_type = \
            Locators(balance=lambda b: get_relationship_type_signer(b),
                     oebs=lambda o: o['hz_relationships.dp.relationship_type'])

        object_id = \
            Locators(balance=lambda b: get_object_id_signer(b),
                     oebs=lambda o: o['hz_relationships.dp.object_id'])

    class HZ_ORG_CONTACTS_SIGNER_PD(Enum):
        job_title_code = \
            Locators(balance=lambda b: get_job_title_code_signer(b),
                     oebs=lambda o: o['hz_org_contacts.pd.job_title_code'])

        job_title = \
            Locators(balance=lambda b: get_job_title_signer(b),
                     oebs=lambda o: o['hz_org_contacts.pd.job_title'])

    class HZ_CUST_ACCOUNT_ROLES_PC(Enum):
        party_id = \
            Locators(balance=lambda b: get_party_id_pc(b),
                     oebs=lambda o: o['hz_cust_account_roles.pc.party_id'])

        cust_account_id = \
            Locators(balance=lambda b: get_cust_account_id(b),
                     oebs=lambda o: o['hz_cust_account_roles.pc.cust_account_id'])

        role_type = \
            Locators(balance=lambda b: get_role_type(b),
                     oebs=lambda o: o['hz_cust_account_roles.pc.role_type'])

    class HZ_CUST_ACCOUNT_ROLES_PD(Enum):
        party_id = \
            Locators(balance=lambda b: get_party_id_signer(b),
                     oebs=lambda o: o['hz_cust_account_roles.pd.party_id'])

        cust_account_id = \
            Locators(balance=lambda b: get_cust_account_id_signer(b),
                     oebs=lambda o: o['hz_cust_account_roles.pd.cust_account_id'])

        role_type = \
            Locators(balance=lambda b: get_role_type_signer(b),
                     oebs=lambda o: o['hz_cust_account_roles.pd.role_type'])

        attribute1 = \
            Locators(balance=lambda b: get_position_name_signer(b),
                     oebs=lambda o: o['hz_cust_account_roles.pd.attribute1'])

        attribute2 = \
            Locators(balance=lambda b: get_authority_doc_type(b),
                     oebs=lambda o: o['hz_cust_account_roles.pd.attribute2'])

        attribute3 = \
            Locators(balance=lambda b: get_signer_gender(b),
                     oebs=lambda o: o['hz_cust_account_roles.pd.attribute3'])

        attribute5 = \
            Locators(balance=lambda b: get_authority_doc_details(b),
                     oebs=lambda o: o['hz_cust_account_roles.pd.attribute5'])

    class HZ_CONTACT_POINTS_CP(Enum):
        owner_table_name = \
            Locators(balance=lambda b: get_owner_table_name_phone(b, code='cp'),
                     oebs=lambda o: o['hz_contact_points.cp.owner_table_name'])

        owner_table_id = \
            Locators(balance=lambda b: get_owner_table_id_phone(b, code='cp'),
                     oebs=lambda o: o['hz_contact_points.cp.owner_table_id'])

        raw_phone_number = \
            Locators(balance=lambda b: get_raw_phone_number_phone(b, code='cp'),
                     oebs=lambda o: o['hz_contact_points.cp.raw_phone_number'])

        phone_number = \
            Locators(balance=lambda b: get_phone_number_phone(b, code='cp'),
                     oebs=lambda o: o['hz_contact_points.cp.phone_number'])

        phone_area_code = \
            Locators(balance=lambda b: get_phone_area_code_phone(b, code='cp'),
                     oebs=lambda o: o['hz_contact_points.cp.phone_area_code'])

        phone_country_code = \
            Locators(balance=lambda b: get_phone_country_code_phone(b, code='cp'),
                     oebs=lambda o: o['hz_contact_points.cp.phone_country_code'])

        phone_line_type = \
            Locators(balance=lambda b: get_phone_line_type_phone(b, code='cp'),
                     oebs=lambda o: o['hz_contact_points.cp.phone_line_type'])

        contact_point_type = \
            Locators(balance=lambda b: get_contact_point_type_hz_contact_points(b, code='cp'),
                     oebs=lambda o: o['hz_contact_points.cp.contact_point_type'])

    class HZ_CONTACT_POINTS_P(Enum):
        owner_table_name = \
            Locators(balance=lambda b: get_owner_table_name_phone(b, code='p'),
                     oebs=lambda o: o['hz_contact_points.p.owner_table_name'])

        owner_table_id = \
            Locators(balance=lambda b: get_owner_table_id_phone(b, code='p'),
                     oebs=lambda o: o['hz_contact_points.p.owner_table_id'])

        raw_phone_number = \
            Locators(balance=lambda b: get_raw_phone_number_phone(b, code='p'),
                     oebs=lambda o: o['hz_contact_points.p.raw_phone_number'])

        phone_number = \
            Locators(balance=lambda b: get_phone_number_phone(b, code='p'),
                     oebs=lambda o: o['hz_contact_points.p.phone_number'])

        phone_area_code = \
            Locators(balance=lambda b: get_phone_area_code_phone(b, code='p'),
                     oebs=lambda o: o['hz_contact_points.p.phone_area_code'])

        phone_country_code = \
            Locators(balance=lambda b: get_phone_country_code_phone(b, code='p'),
                     oebs=lambda o: o['hz_contact_points.p.phone_country_code'])

        phone_line_type = \
            Locators(balance=lambda b: get_phone_line_type_phone(b, code='p'),
                     oebs=lambda o: o['hz_contact_points.p.phone_line_type'])

        contact_point_type = \
            Locators(balance=lambda b: get_contact_point_type_hz_contact_points(b, code='p'),
                     oebs=lambda o: o['hz_contact_points.p.contact_point_type'])

    class HZ_CONTACT_POINTS_CF(Enum):
        owner_table_name = \
            Locators(balance=lambda b: get_owner_table_name_hz_contact_points_cf(b),
                     oebs=lambda o: o['hz_contact_points.cf.owner_table_name'])

        owner_table_id = \
            Locators(balance=lambda b: get_owner_table_id_hz_contact_points_cf(b),
                     oebs=lambda o: o['hz_contact_points.cf.owner_table_id'])

        raw_phone_number = \
            Locators(balance=lambda b: get_raw_phone_number_hz_contact_points_cf(b),
                     oebs=lambda o: o['hz_contact_points.cf.raw_phone_number'])

        phone_line_type = \
            Locators(balance=lambda b: get_phone_line_type_hz_contact_points_cf(b),
                     oebs=lambda o: o['hz_contact_points.cf.phone_line_type'])

        phone_country_code = \
            Locators(balance=lambda b: get_phone_country_code_fax(b, code='cf'),
                     oebs=lambda o: o['hz_contact_points.cf.phone_country_code'])

        phone_area_code = \
            Locators(balance=lambda b: get_phone_area_code_fax(b, code='cf'),
                     oebs=lambda o: o['hz_contact_points.cf.phone_area_code'])

        phone_number = \
            Locators(balance=lambda b: get_phone_number_fax(b, code='cf'),
                     oebs=lambda o: o['hz_contact_points.cf.phone_number'])

        contact_point_type = \
            Locators(balance=lambda b: get_contact_point_type_hz_contact_points_cf(b),
                     oebs=lambda o: o['hz_contact_points.cf.contact_point_type'])

    class HZ_CONTACT_POINTS_F(Enum):
        owner_table_name = \
            Locators(balance=lambda b: get_owner_table_name_hz_contact_points_f(b),
                     oebs=lambda o: o['hz_contact_points.f.owner_table_name'])

        owner_table_id = \
            Locators(balance=lambda b: get_owner_table_id_hz_contact_points_f(b),
                     oebs=lambda o: o['hz_contact_points.f.owner_table_id'])

        raw_phone_number = \
            Locators(balance=lambda b: get_raw_phone_number_hz_contact_points_f(b),
                     oebs=lambda o: o['hz_contact_points.f.raw_phone_number'])

        phone_line_type = \
            Locators(balance=lambda b: get_phone_line_type_hz_contact_points_f(b),
                     oebs=lambda o: o['hz_contact_points.f.phone_line_type'])

        phone_country_code = \
            Locators(balance=lambda b: get_phone_country_code_fax(b, code='f'),
                     oebs=lambda o: o['hz_contact_points.f.phone_country_code'])

        phone_area_code = \
            Locators(balance=lambda b: get_phone_area_code_fax(b, code='f'),
                     oebs=lambda o: o['hz_contact_points.f.phone_area_code'])

        phone_number = \
            Locators(balance=lambda b: get_phone_number_fax(b, code='f'),
                     oebs=lambda o: o['hz_contact_points.f.phone_number'])

        contact_point_type = \
            Locators(balance=lambda b: get_contact_point_type_hz_contact_points_f(b),
                     oebs=lambda o: o['hz_contact_points.f.contact_point_type'])

    class HZ_CONTACT_POINTS_CE(Enum):
        owner_table_name = \
            Locators(balance=lambda b: get_owner_table_name_email(b, code='ce'),
                     oebs=lambda o: o['hz_contact_points.ce.owner_table_name'])

        owner_table_id = \
            Locators(balance=lambda b: get_owner_table_id_hz_contact_points_ce(b),
                     oebs=lambda o: o['hz_contact_points.ce.owner_table_id'])

        email_address = \
            Locators(balance=lambda b: get_email_address_hz_contact_points_ce(b),
                     oebs=lambda o: o['hz_contact_points.ce.email_address'])

        contact_point_type = \
            Locators(balance=lambda b: get_contact_point_type_hz_contact_points_ce(b),
                     oebs=lambda o: o['hz_contact_points.ce.contact_point_type'])

    class HZ_CONTACT_POINTS_E(Enum):
        owner_table_name = \
            Locators(balance=lambda b: get_owner_table_name_email(b, code='e'),
                     oebs=lambda o: o['hz_contact_points.e.owner_table_name'])

        owner_table_id = \
            Locators(balance=lambda b: get_owner_table_id_hz_contact_points_e(b),
                     oebs=lambda o: o['hz_contact_points.e.owner_table_id'])

        email_address = \
            Locators(balance=lambda b: get_email_address_hz_contact_points_e(b),
                     oebs=lambda o: o['hz_contact_points.e.email_address'])

        contact_point_type = \
            Locators(balance=lambda b: get_contact_point_type_hz_contact_points_e(b),
                     oebs=lambda o: o['hz_contact_points.e.contact_point_type'])

    class HZ_LOCATIONS_B(Enum):
        city = \
            Locators(balance=lambda b: get_address_city(b),
                     oebs=lambda o: o['hz_locations.b.city'])

        postal_code = \
            Locators(balance=lambda b: get_address_postcode(b),
                     oebs=lambda o: o['hz_locations.b.postal_code'])

        country = \
            Locators(balance=lambda b: get_address_country(b),
                     oebs=lambda o: o['hz_locations.b.country'])

        # address1 = \
        #     Locators(balance=lambda b: get_address_b(b),
        #              oebs=lambda o: o['hz_locations.b.address1'])

        address3 = \
            Locators(balance=lambda b: get_address_kladr(b),
                     oebs=lambda o: o['hz_locations.b.address3'])

        # address4 = \
        #     Locators(
        #         balance=lambda b: get_street_b(b, slice(0, 50)),
        #         oebs=lambda o: o['hz_locations.b.address4'])

        # attribute1 = \
        #     Locators(
        #         balance=lambda b: get_street_b(b, slice(0, 50)),
        #         oebs=lambda o: o['hz_locations.b.attribute1'])

    class HZ_LOCATIONS_S(Enum):
        country = \
            Locators(balance=lambda b: get_shipment_country(b),
                     oebs=lambda o: o['hz_locations.s.country'])

        postal_code = \
            Locators(balance=lambda b: get_shipment_postcode(b),
                     oebs=lambda o: o['hz_locations.s.postal_code'])

        address3 = \
            Locators(balance=lambda b: get_shipment_kladr_code(b),
                     oebs=lambda o: o['hz_locations.s.address3'])

        # street = \
        #     Locators(balance=lambda b: get_shipment_street(b),
        #              oebs=lambda o: o['hz_locations.s.address4'])

        # street_first_50 = \
        #     Locators(balance=lambda b: get_shipment_street(b),
        #              oebs=lambda o: o['hz_locations.s.attribute1'])

        city = \
            Locators(balance=lambda b: get_shipment_city(b),
                     oebs=lambda o: o['hz_locations.s.city'])

        address1 = \
            Locators(balance=lambda b: get_shipment_address(b),
                     oebs=lambda o: o['hz_locations.s.address1'])

    class HZ_LOCATIONS_L(Enum):
        country = \
            Locators(balance=lambda b: get_legal_country(b),
                     oebs=lambda o: o['hz_locations.l.country'])

        postal_code = \
            Locators(balance=lambda b: get_legal_postcode(b),
                     oebs=lambda o: o['hz_locations.l.postal_code'])

        address1 = \
            Locators(balance=lambda b: get_legal_address(b),
                     oebs=lambda o: o['hz_locations.l.address1'])

        address3 = \
            Locators(balance=lambda b: get_legal_kladr_code(b),
                     oebs=lambda o: o['hz_locations.l.address3'])

        # address4 = \
        #     Locators(balance=lambda b: get_legal_street(b),
        #              oebs=lambda o: o['hz_locations.l.address4'])
        #
        # attribute1 = \
        #     Locators(balance=lambda b: get_legal_street_first_50(b),
        #              oebs=lambda o: o['hz_locations.l.attribute1'])

        city = \
            Locators(balance=lambda b: get_legal_city(b),
                     oebs=lambda o: o['hz_locations.l.city'])

    class HZ_PARTY_SITES_B(Enum):
        location_id = \
            Locators(balance=lambda b: get_address_location_id(b),
                     oebs=lambda o: o['hz_party_sites.b.location_id'])

        party_id = \
            Locators(balance=lambda b: get_address_party_id(b),
                     oebs=lambda o: o['hz_party_sites.b.party_id'])

    class HZ_PARTY_SITES_S(Enum):
        location_id = \
            Locators(balance=lambda b: get_shipment_address_location_id(b),
                     oebs=lambda o: o['hz_party_sites.s.location_id'])

        party_id = \
            Locators(balance=lambda b: get_shipment_address_party_id(b),
                     oebs=lambda o: o['hz_party_sites.s.party_id'])

    class HZ_PARTY_SITES_L(Enum):
        location_id = \
            Locators(balance=lambda b: get_legal_address_location_id(b),
                     oebs=lambda o: o['hz_party_sites.l.location_id'])

        party_id = \
            Locators(balance=lambda b: get_legal_address_party_id(b),
                     oebs=lambda o: o['hz_party_sites.l.party_id'])

    class HZ_PARTY_SITE_USES_B(Enum):
        site_use_type = \
            Locators(balance=lambda b: 'BILL_TO',
                     oebs=lambda o: o['hz_party_site_uses.b.site_use_type'])

        party_site_id = \
            Locators(balance=lambda b: get_oebs_party_site_id(b['t_firm.id'], 'P{0}_B'.format(b['t_person.id'])),
                     oebs=lambda o: o['hz_party_site_uses.b.party_site_id'])

    class HZ_PARTY_SITE_USES_S(Enum):
        site_use_type = \
            Locators(balance=lambda b: get_shipment_site_use_type(b),
                     oebs=lambda o: o['hz_party_site_uses.s.site_use_type'])

        party_site_id = \
            Locators(balance=lambda b: get_shipment_party_side_id(b),
                     oebs=lambda o: o['hz_party_site_uses.s.party_site_id'])

    class HZ_PARTY_SITE_USES_L(Enum):
        site_use_type = \
            Locators(balance=lambda b: get_legal_site_use_type(b),
                     oebs=lambda o: o['hz_party_site_uses.l.site_use_type'])

        party_site_id = \
            Locators(balance=lambda b: get_legal_party_site_id(b),
                     oebs=lambda o: o['hz_party_site_uses.l.party_site_id'])

    class HZ_CUST_ACCT_SITES_B(Enum):
        cust_account_id = \
            Locators(balance=lambda b: get_oebs_person_cust_account_id(b['t_firm.id'], b['t_person.id']),
                     oebs=lambda o: o['hz_cust_acct_sites.b.cust_account_id'])

        territory = \
            Locators(balance=lambda b: get_address_geo_territory_code(b),
                     oebs=lambda o: o['hz_cust_acct_sites.b.territory'])

        party_site_id = \
            Locators(balance=lambda b: get_oebs_party_site_id(b['t_firm.id'], 'P{0}_B'.format(b['t_person.id'])),
                     oebs=lambda o: o['hz_cust_acct_sites.b.party_site_id'])

        attribute1 = \
            Locators(balance=lambda b: get_invalid_address(b),
                     oebs=lambda o: o['hz_cust_acct_sites.b.attribute1'])

        attribute2 = \
            Locators(balance=lambda b: get_vat_payer(b),
                     oebs=lambda o: o['hz_cust_acct_sites.b.attribute2'])

        attribute9 = \
            Locators(balance=lambda b: get_addr_resident_value(b),
                     oebs=lambda o: o['hz_cust_acct_sites.b.attribute9'])

    class HZ_CUST_ACCT_SITES_S(Enum):
        cust_account_id = \
            Locators(balance=lambda b: get_shipment_cust_account_id(b),
                     oebs=lambda o: o['hz_cust_acct_sites.s.cust_account_id'])

        territory = \
            Locators(balance=lambda b: get_shipment_territory(b),
                     oebs=lambda o: o['hz_cust_acct_sites.s.territory'])

        party_site_id = \
            Locators(balance=lambda b: get_shipment_party_side_id(b),
                     oebs=lambda o: o['hz_cust_acct_sites.s.party_site_id'])

        attribute1 = \
            Locators(balance=lambda b: get_shipment_invalid_address(b),
                     oebs=lambda o: o['hz_cust_acct_sites.s.attribute1'])

        attribute2 = \
            Locators(balance=lambda b: get_shipment_vat_payer(b),
                     oebs=lambda o: o['hz_cust_acct_sites.s.attribute2'])
        attribute9 = \
            Locators(balance=lambda b: get_shipment_person_resident(b),
                     oebs=lambda o: o['hz_cust_acct_sites.s.attribute9'])

    class HZ_CUST_ACCT_SITES_L(Enum):
        cust_account_id = \
            Locators(balance=lambda b: get_legal_cust_account_id(b),
                     oebs=lambda o: o['hz_cust_acct_sites.l.cust_account_id'])

        territory = \
            Locators(balance=lambda b: get_legal_territory(b),
                     oebs=lambda o: o['hz_cust_acct_sites.l.territory'])

        party_site_id = \
            Locators(balance=lambda b: get_legal_party_site_id(b),
                     oebs=lambda o: o['hz_cust_acct_sites.l.party_site_id'])

        attribute1 = \
            Locators(balance=lambda b: get_legal_invalid_address(b),
                     oebs=lambda o: o['hz_cust_acct_sites.l.attribute1'])

        attribute2 = \
            Locators(balance=lambda b: get_legal_vat_payer(b),
                     oebs=lambda o: o['hz_cust_acct_sites.l.attribute2'])

        attribute9 = \
            Locators(balance=lambda b: get_legal_person_resident(b),
                     oebs=lambda o: o['hz_cust_acct_sites.l.attribute9'])

    class HZ_CUST_SITE_USES_B(Enum):
        site_use_code = \
            Locators(balance=lambda b: 'BILL_TO',
                     oebs=lambda o: o['hz_cust_site_uses.b.site_use_code'])

        location = \
            Locators(balance=lambda b: 'P{0}_B'.format(b['t_person.id']),
                     oebs=lambda o: o['hz_cust_site_uses.b.location'])

        cust_acct_site_id = \
            Locators(balance=lambda b: get_oebs_cust_acct_site_id(b['t_firm.id'], '{0}_B'.format(b['t_person.id'])),
                     oebs=lambda o: o['hz_cust_site_uses.b.cust_acct_site_id'])

    class HZ_CUST_SITE_USES_S(Enum):
        site_use_code = \
            Locators(balance=lambda b: get_shipment_site_use_code(b),
                     oebs=lambda o: o['hz_cust_site_uses.s.site_use_code'])

        location = \
            Locators(balance=lambda b: get_shipment_address_location(b),
                     oebs=lambda o: o['hz_cust_site_uses.s.location'])

        cust_acct_site_id = \
            Locators(balance=lambda b: get_shipment_cust_acct_site_id(b),
                     oebs=lambda o: o['hz_cust_site_uses.s.cust_acct_site_id'])

    class HZ_CUST_SITE_USES_L(Enum):
        site_use_code = \
            Locators(balance=lambda b: get_legal_site_use_code(b),
                     oebs=lambda o: o['hz_cust_site_uses.l.site_use_code'])

        location = \
            Locators(balance=lambda b: get_legal_address_location(b),
                     oebs=lambda o: o['hz_cust_site_uses.l.location'])

        cust_acct_site_id = \
            Locators(balance=lambda b: get_legal_cust_acct_site_id(b),
                     oebs=lambda o: o['hz_cust_site_uses.l.cust_acct_site_id'])


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


class Table(object):
    def __init__(self, prefix, options, query, link_type='direct', firm_id=None, single_row=True):
        self.prefix = prefix
        self.options = options
        self.query = query
        self.link_type = link_type
        self.firm_id = firm_id
        self.single_row = single_row

    def is_mentioned_in_locators(self, locators):
        return is_prefix_in_locators(self.prefix, locators)

    def get_object_id(self, object_id):
        if self.link_type == 'direct':
            return object_id
        if self.link_type == 'party_id':
            return get_oebs_party_id_new(self.firm_id, object_id)
        if self.link_type == 'party_site_id':
            party_site_id = get_oebs_party_site_id_new(self.firm_id, object_id)
            if not party_site_id:
                return
            return get_oebs_party_site_use_wo_site_use_type(self.firm_id, party_site_id)

    def get_result(self, firm_id, locators):
        result = {}
        for option, object_id in self.options.iteritems():
            object_id = self.get_object_id(object_id)
            prefix_w_option = self.prefix.format(option)
            if is_prefix_in_locators(prefix_w_option, locators):
                # условие с object_id != 'I', потому что в оебсе есть такая строка, заполненная чем-то по дефолту
                if object_id and object_id != 'I':
                    option_result = db.oebs().execute_oebs(firm_id,
                                                           self.query,
                                                           {'object_id': object_id},
                                                           single_row=self.single_row)
                    if not option_result:
                        option_result = {str_locator.replace(prefix_w_option, ''): NoSuchRowInOEBS.msg
                                         for str_locator in locators if prefix_w_option in str_locator}

                else:
                    option_result = {str_locator.replace(prefix_w_option, ''): NoSuchRowInOEBS.msg
                                     for str_locator in locators if prefix_w_option in str_locator}
                result.update(utils.add_key_prefix(option_result, prefix_w_option))
        return result


def get_oebs_person_data(balance_data, firm_id, oebs_str_locators):
    person = {}
    person_id = balance_data['t_person.id']

    hz_cust_accounts_table = \
        Table(prefix='hz_cust_accounts.',
              options={'': 'P{0}'.format(person_id)},
              query="SELECT * FROM apps.hz_cust_accounts WHERE account_number = :object_id")

    xxar_customer_attributes_table = \
        Table(prefix='xxar_customer_attributes.',
              options={'': 'P{0}'.format(person_id)},
              query="SELECT * FROM apps.XXAR_CUSTOMER_ATTRIBUTES "
                    "WHERE CUSTOMER_ID = (SELECT ac.CUST_ACCOUNT_ID "
                    "FROM apps.HZ_CUST_ACCOUNTS ac "
                    "WHERE ac.ACCOUNT_NUMBER = :object_id)")

    hz_cust_account_roles_table = \
        Table(prefix='hz_cust_account_roles.{0}.',
              options={'pd': 'P{0}_D'.format(person_id),
                       'pc': 'P{0}_C'.format(person_id)},
              query="SELECT * FROM apps.hz_cust_account_roles WHERE orig_system_reference  = :object_id")

    hz_org_contacts_table = \
        Table(prefix='hz_org_contacts.{0}.',
              options={'pc': 'P{0}_C'.format(person_id),
                       'pd': 'P{0}_D'.format(person_id)},
              query="SELECT * FROM apps.hz_org_contacts WHERE orig_system_reference  = :object_id")

    hz_relationships_table = \
        Table(prefix='hz_relationships.{0}.',
              options={'cp': 'CP{0}'.format(person_id),
                       'dp': 'DP{0}'.format(person_id)},
              query="SELECT * FROM apps.hz_relationships WHERE subject_id  = :object_id",
              link_type='party_id',
              firm_id=firm_id)

    if balance_data['t_person.type'] == PersonTypes.UA.code:
        inn_kpp = balance_data['t_person.kpp'] or balance_data['t_person.inn'] or ''
    else:
        inn_kpp = balance_data['t_person.inn'] or ''

    hz_parties_table = \
        Table(prefix='hz_parties.{0}.',
              options={'p': 'P{0}'.format(person_id),
                       'cp': 'CP{0}'.format(person_id),
                       'dp': 'DP{0}'.format(person_id),
                       'i': 'I{0}'.format(inn_kpp),
                       },
              query="SELECT * FROM apps.hz_parties WHERE orig_system_reference  = :object_id")

    hz_locations_table = \
        Table(prefix='hz_locations.{0}.',
              options={'b': 'P{0}_B'.format(person_id),
                       'l': 'P{0}_L'.format(person_id),
                       's': 'P{0}_S'.format(person_id)},
              query="SELECT * FROM apps.hz_locations WHERE orig_system_reference  = :object_id")

    hz_party_sites_table = \
        Table(prefix='hz_party_sites.{0}.',
              options={'b': 'P{0}_B'.format(person_id),
                       'l': 'P{0}_L'.format(person_id),
                       's': 'P{0}_S'.format(person_id)},
              query="SELECT * FROM apps.hz_party_sites WHERE orig_system_reference  = :object_id")

    hz_party_site_uses_table = \
        Table(prefix='hz_party_site_uses.{0}.',
              options={'b': 'P{0}_B'.format(person_id),
                       'l': 'P{0}_L'.format(person_id),
                       's': 'P{0}_S'.format(person_id)},
              query="SELECT * FROM apps.hz_party_site_uses WHERE party_site_use_id  = :object_id",
              firm_id=firm_id,
              link_type='party_site_id')

    hz_cust_acct_sites_table = \
        Table(prefix='hz_cust_acct_sites.{0}.',
              options={'b': 'P{0}_B'.format(person_id),
                       'l': 'P{0}_L'.format(person_id),
                       's': 'P{0}_S'.format(person_id)},
              query="SELECT * FROM apps.hz_cust_acct_sites WHERE orig_system_reference  = :object_id")

    hz_cust_site_uses_table = \
        Table(prefix='hz_cust_site_uses.{0}.',
              options={'b': 'P{0}_B'.format(person_id),
                       'l': 'P{0}_L'.format(person_id),
                       's': 'P{0}_S'.format(person_id)},
              query="SELECT * FROM apps.hz_cust_site_uses WHERE orig_system_reference  = :object_id")

    hz_contact_points_table = \
        Table(prefix='hz_contact_points.{0}.',
              options={'e': 'P{0}_E'.format(person_id),
                       'ce': 'P{0}_CE'.format(person_id),
                       'p': 'P{0}_P'.format(person_id),
                       'cp': 'P{0}_CP'.format(person_id),
                       'f': 'P{0}_F'.format(person_id),
                       'cf': 'P{0}_CF'.format(person_id)},
              query="SELECT owner_table_name, owner_table_id, raw_phone_number, phone_country_code, "
                    "phone_area_code, "
                    "phone_number, phone_line_type, contact_point_type, email_address "
                    "FROM apps.hz_contact_points WHERE orig_system_reference  = :object_id")

    xxar_customers_table = \
        Table(prefix='xxar_customers.',
              options={'': 'P{0}'.format(person_id)},
              query="SELECT * FROM apps.xxar_customers WHERE orig_system_reference = :object_id")

    xxar_edo_oper_hist_table = \
        Table(prefix='xxar_edo_oper_hist_tbl.',
              options={'': 'P{0}'.format(person_id)},
              query="SELECT * FROM apps.xxar_edo_oper_hist_tbl WHERE CUSTOMER_ID = \
                      (SELECT CUST_ACCOUNT_ID FROM hz_cust_accounts WHERE ACCOUNT_NUMBER = :object_id)"
                    "order by date_start",
              single_row=False)

    ALL_TABLES = [
        hz_cust_accounts_table,
        xxar_customer_attributes_table,
        hz_cust_account_roles_table,
        hz_org_contacts_table,
        hz_relationships_table,
        hz_parties_table,
        hz_locations_table,
        hz_party_sites_table,
        hz_party_site_uses_table,
        hz_cust_acct_sites_table,
        hz_cust_site_uses_table,
        hz_contact_points_table,
        xxar_customers_table,
        xxar_edo_oper_hist_table
    ]

    for table in ALL_TABLES:
        person.update(table.get_result(firm_id, oebs_str_locators))

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

    for name, locators in attrs_list_to_dict(attrs).items():
        balance_str_locators.update(get_str_constants(locators.balance))
        oebs_str_locators.update(get_str_constants(locators.oebs))
    return balance_str_locators, oebs_str_locators


def is_prefix_in_locators(prefix, db_locators):
    return any(locator.startswith(prefix) for locator in db_locators)


def get_kbk(data):
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return None
    if data['t_person.type'] in YT_LIKE_PERSONS:
        return ''
    else:
        return data.get('t_contract_attributes.kbk', None)


def get_oktmo(data):
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return None
    if data['t_person.type'] in YT_LIKE_PERSONS:
        return ''
    else:
        return data.get('t_contract_attributes.oktmo', None)


def get_longname(data):
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return None
    if data['t_person.type'] in YT_LIKE_PERSONS:
        return data['t_person.longname'] or data['t_person.name']
    else:
        if data['t_person.type'] == 'endbuyer_ur':
            return data['t_person.name']
        return data['t_person.longname'] if data['t_person.longname'] else ''


def get_non_ur_party_name(data):
    if data['t_person.type'] in UR_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    if data['t_person.type'] in YT_LIKE_PERSONS:
        return data['t_person.name']
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return ' '.join([data[x] for x in ['t_person.fname', 't_person.lname'] if data[x]])


def get_ur_party_name(data):
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    if data['t_person.type'] in YT_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    return data['t_person.name']


def get_ur_party_type(data):
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    if data['t_person.type'] in YT_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    return 'ORGANIZATION'


def get_non_ur_party_type(data):
    if data['t_person.type'] in UR_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return 'PERSON'
    else:
        return 'ORGANIZATION'


def get_ur_person_inn(data):
    if data['t_person.type'] in UR_LIKE_PERSONS:
        return data['t_person.inn']
    else:
        return NoSuchRowInOEBS.msg


def get_non_ur_person_inn(data):
    if W_API:
        # 'endbuyer_yt', 'usu' потому что у них и не бывает инн
        if data['t_person.type'] in ['yt']:
            return ''
    else:
        if data['t_person.type'] in ['yt', 'endbuyer_yt', 'usu']:
            return ''

    if data['t_person.type'] in UR_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    if data['t_person.type'] in ('kzu', 'yt_kzu', 'yt_kzp', 'kzp'):
        return data.get('t_extprops.person.kz_in', '')
    return data['t_person.inn'] or ''


def get_person_inn_details(data):
    if data['t_person.type'] in YT_LIKE_PERSONS or data['t_person.type'] in PH_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    if data['t_person.kpp'] in UA_LIKE_PERSONS:
        return data['t_person.kpp']
    return data['t_person.inn_doc_details']


def get_sign_person_last_name(data):
    if data['t_person.type'] in PH_LIKE_PERSONS or data['t_person.type'] == 'yt_kzp':
        return NoSuchRowInOEBS.msg
    return data['t_person.signer_person_name'] or NoSuchRowInOEBS.msg


def get_repr_person_last_name(data):
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    if data['t_person.type'] == 'yt_kzp':
        return ''
    return data['t_person.representative'] or '-'


def get_fname(data):
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return data['t_person.fname'] or ''
    if data['t_person.type'] in YT_LIKE_PERSONS:
        return None
    return NoSuchRowInOEBS.msg


def get_lname(data):
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return data['t_person.lname'] or ''
    if data['t_person.type'] in YT_LIKE_PERSONS:
        return None
    return NoSuchRowInOEBS.msg


def get_job_title_code(data):
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    else:
        return 'REPRESENTATIVE'


def get_job_title_code_signer(data):
    if data['t_person.type'] in PH_LIKE_PERSONS or data['t_person.type'] == 'yt_kzp':
        return NoSuchRowInOEBS.msg
    if data['t_person.signer_person_name'] is not None:
        return 'DIRECTOR'
    else:
        return NoSuchRowInOEBS.msg


def get_job_title_signer(data):
    if data['t_person.type'] in PH_LIKE_PERSONS or data['t_person.type'] == 'yt_kzp':
        return NoSuchRowInOEBS.msg
    if data['t_person.signer_person_name'] is not None:
        return u'Директор'
    else:
        return NoSuchRowInOEBS.msg


def get_cust_account_id(data):
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    return get_oebs_person_cust_account_id(data['t_firm.id'], data['t_person.id'])


def get_role_type(data):
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    return 'CONTACT'


def get_party_id_pc(data):
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    return get_oebs_party_id(data['t_firm.id'], 'CP{0}_'.format(data['t_person.id']))


def get_party_id_signer(data):
    if data['t_person.type'] in PH_LIKE_PERSONS or data['t_person.type'] == 'yt_kzp':
        return NoSuchRowInOEBS.msg
    if data['t_person.signer_person_name']:
        return get_oebs_party_id(data['t_firm.id'], 'DP{0}_'.format(data['t_person.id']))
    return NoSuchRowInOEBS.msg


def get_cust_account_id_signer(data):
    if data['t_person.type'] in PH_LIKE_PERSONS or data['t_person.type'] == 'yt_kzp':
        return NoSuchRowInOEBS.msg
    if data['t_person.signer_person_name']:
        return get_oebs_person_cust_account_id(data['t_firm.id'], data['t_person.id'])
    return NoSuchRowInOEBS.msg


def get_role_type_signer(data):
    if data['t_person.type'] in PH_LIKE_PERSONS or data['t_person.type'] == 'yt_kzp':
        return NoSuchRowInOEBS.msg
    if data['t_person.signer_person_name']:
        return 'CONTACT'
    return NoSuchRowInOEBS.msg


def get_position_name_signer(data):
    if data['t_person.type'] in PH_LIKE_PERSONS or data['t_person.type'] == 'yt_kzp':
        return NoSuchRowInOEBS.msg
    if data['t_person.signer_person_name']:
        return data['t_person.signer_position_name']
    else:
        return NoSuchRowInOEBS.msg


def get_authority_doc_details(data):
    if data['t_person.type'] in PH_LIKE_PERSONS or data['t_person.type'] == 'yt_kzp':
        return NoSuchRowInOEBS.msg
    if data['t_person.signer_person_name']:
        return data['t_person.authority_doc_details']
    else:
        return NoSuchRowInOEBS.msg


def get_authority_doc_type(data):
    if data['t_person.type'] in PH_LIKE_PERSONS or data['t_person.type'] == 'yt_kzp':
        return NoSuchRowInOEBS.msg
    if data['t_person.signer_person_name']:
        return get_oebs_doc_type(data['t_person.authority_doc_type'])
    return NoSuchRowInOEBS.msg


def get_signer_gender(data):
    if data['t_person.type'] in PH_LIKE_PERSONS or data['t_person.type'] == 'yt_kzp':
        return NoSuchRowInOEBS.msg
    if data['t_person.signer_person_name']:
        return get_oebs_gender(data['t_person.signer_person_gender'])
    return NoSuchRowInOEBS.msg


def get_job_title(data):
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    else:
        return u'Представитель'


def get_mname(data):
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return data['t_person.mname'] or ''
    if data['t_person.type'] in YT_LIKE_PERSONS:
        return ''
    return NoSuchRowInOEBS.msg


def get_owner_table_name_email(data, code):
    if W_API:
        if not data['t_person.email']:
            return NoSuchRowInOEBS.msg
    if (code == 'e' and data['t_person.type'] in PH_LIKE_PERSONS
            or code == 'ce' and data['t_person.type'] not in PH_LIKE_PERSONS):
        return 'HZ_PARTIES'
    return NoSuchRowInOEBS.msg


def get_owner_table_name_phone(data, code):
    if W_API:
        if not data['t_person.phone']:
            return NoSuchRowInOEBS.msg
    if (code == 'p' and data['t_person.type'] in PH_LIKE_PERSONS
            or code == 'cp' and data['t_person.type'] not in PH_LIKE_PERSONS):
        return 'HZ_PARTIES'
    return NoSuchRowInOEBS.msg


def get_owner_table_id_hz_contact_points_e(data):
    if W_API:
        if not data['t_person.email']:
            return NoSuchRowInOEBS.msg
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return get_oebs_party_id(data['t_firm.id'], 'P{0}'.format(data['t_person.id']))
    return NoSuchRowInOEBS.msg


def get_owner_table_id_hz_contact_points_ce(data):
    if W_API:
        if not data['t_person.email']:
            return NoSuchRowInOEBS.msg
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    return get_oebs_party_id(data['t_firm.id'], 'CP{0}_'.format(data['t_person.id']))


def get_owner_table_id_hz_contact_points_f(data):
    if W_API:
        if not data['t_person.fax']:
            return NoSuchRowInOEBS.msg
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return get_oebs_party_id(data['t_firm.id'], 'P{0}'.format(data['t_person.id']))
    return NoSuchRowInOEBS.msg


def get_owner_table_id_hz_contact_points_cf(data):
    if W_API:
        if not data['t_person.email']:
            return NoSuchRowInOEBS.msg
    if data['t_person.type'] not in PH_LIKE_PERSONS:
        return get_oebs_party_id(data['t_firm.id'], 'CP{0}_'.format(data['t_person.id']))
    return NoSuchRowInOEBS.msg


def get_owner_table_id_phone(data, code):
    if W_API:
        if not data['t_person.phone']:
            return NoSuchRowInOEBS.msg
    if code == 'p':
        if data['t_person.type'] in PH_LIKE_PERSONS:
            return get_oebs_party_id(data['t_firm.id'], 'P{0}'.format(data['t_person.id']))
    else:
        if data['t_person.type'] not in PH_LIKE_PERSONS:
            return get_oebs_party_id(data['t_firm.id'], 'CP{0}_'.format(data['t_person.id']))
    return NoSuchRowInOEBS.msg


def get_phone_country_code_fax(data, code):
    if W_API:
        if not data['t_person.fax']:
            return NoSuchRowInOEBS.msg
    if code == 'f':
        if data['t_person.type'] in PH_LIKE_PERSONS:
            return get_country_code(data['t_person.fax']) or None
    else:
        if data['t_person.type'] not in PH_LIKE_PERSONS:
            return get_country_code(data['t_person.fax']) or None
    return NoSuchRowInOEBS.msg


def get_phone_country_code_phone(data, code):
    if W_API:
        if not data['t_person.phone']:
            return NoSuchRowInOEBS.msg
    if code == 'p':
        if data['t_person.type'] in PH_LIKE_PERSONS:
            return get_country_code(data['t_person.phone']) or None
    else:
        if data['t_person.type'] not in PH_LIKE_PERSONS:
            return get_country_code(data['t_person.phone']) or None
    return NoSuchRowInOEBS.msg


def get_raw_phone_number_hz_contact_points_f(data):
    if W_API:
        if not data['t_person.fax']:
            return NoSuchRowInOEBS.msg
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return data['t_person.fax'] or '-'
    return NoSuchRowInOEBS.msg


def get_raw_phone_number_hz_contact_points_cf(data):
    if W_API:
        if not data['t_person.fax']:
            return NoSuchRowInOEBS.msg
    if data['t_person.type'] not in PH_LIKE_PERSONS:
        return data['t_person.fax'] or '-'
    return NoSuchRowInOEBS.msg


def get_raw_phone_number_phone(data, code):
    if W_API:
        if not data['t_person.phone']:
            return NoSuchRowInOEBS.msg

    if code == 'p':
        if data['t_person.type'] in PH_LIKE_PERSONS:
            return data['t_person.phone'] or '-'
    else:
        if data['t_person.type'] not in PH_LIKE_PERSONS:
            return data['t_person.phone'] or '-'
    return NoSuchRowInOEBS.msg


def get_phone_line_type_hz_contact_points_f(data):
    if W_API:
        if not data['t_person.fax']:
            return NoSuchRowInOEBS.msg
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return 'FAX'
    return NoSuchRowInOEBS.msg


def get_phone_line_type_hz_contact_points_cf(data):
    if W_API:
        if not data['t_person.fax']:
            return NoSuchRowInOEBS.msg
    if data['t_person.type'] not in PH_LIKE_PERSONS:
        return 'FAX'
    return NoSuchRowInOEBS.msg


def get_phone_line_type_phone(data, code):
    if W_API:
        if not data['t_person.phone']:
            return NoSuchRowInOEBS.msg
    if (code == 'p' and data['t_person.type'] in PH_LIKE_PERSONS
            or code == 'cp' and data['t_person.type'] not in PH_LIKE_PERSONS):
        return 'GEN'
    return NoSuchRowInOEBS.msg


def get_phone_area_code_fax(data, code):
    if W_API:
        if not data['t_person.fax']:
            return NoSuchRowInOEBS.msg
    if code == 'f':
        if data['t_person.type'] in PH_LIKE_PERSONS:
            return
    else:
        if data['t_person.type'] not in PH_LIKE_PERSONS:
            return
    return NoSuchRowInOEBS.msg


def get_phone_area_code_phone(data, code):
    if W_API:
        if not data['t_person.phone']:
            return NoSuchRowInOEBS.msg
    if (code == 'p' and data['t_person.type'] in PH_LIKE_PERSONS
            or code == 'cp' and data['t_person.type'] not in PH_LIKE_PERSONS):
        return get_phone_area_code(data['t_person.phone']) or None
    return NoSuchRowInOEBS.msg


def get_phone_number_fax(data, code):
    if W_API:
        if not data['t_person.fax']:
            return NoSuchRowInOEBS.msg
    fax = phone_to_oebs_format(data['t_person.fax'])
    fax_code = get_country_code(fax)
    if code == 'f':
        if data['t_person.type'] in PH_LIKE_PERSONS:
            if not fax:
                return None
            return fax and fax[len(fax_code):] if fax_code else fax
    else:
        if data['t_person.type'] not in PH_LIKE_PERSONS:
            if not fax:
                return None
            return fax and fax[len(fax_code):] if fax_code else fax
    return NoSuchRowInOEBS.msg


def get_phone_number_phone(data, code):
    if W_API:
        if not data['t_person.phone']:
            return NoSuchRowInOEBS.msg
    phone = phone_to_oebs_format(data['t_person.phone'])
    phone_code = get_country_code(phone)
    area_code = get_phone_area_code(phone)
    if code == 'p':
        if data['t_person.type'] in PH_LIKE_PERSONS:
            if not phone:
                return None
            return phone[len(phone_code) + len(area_code):]
    else:
        if data['t_person.type'] not in PH_LIKE_PERSONS:
            if not phone:
                return None
            return phone[len(phone_code) + len(area_code):]
    return NoSuchRowInOEBS.msg


def get_contact_point_type_hz_contact_points_f(data):
    if W_API:
        if not data['t_person.fax']:
            return NoSuchRowInOEBS.msg
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return 'PHONE'
    return NoSuchRowInOEBS.msg


def get_contact_point_type_hz_contact_points_cf(data):
    if W_API:
        if not data['t_person.fax']:
            return NoSuchRowInOEBS.msg
    if data['t_person.type'] not in PH_LIKE_PERSONS:
        return 'PHONE'
    return NoSuchRowInOEBS.msg


def get_contact_point_type_hz_contact_points(data, code):
    if W_API:
        if not data['t_person.phone']:
            return NoSuchRowInOEBS.msg
    if code == 'p':
        if data['t_person.type'] in PH_LIKE_PERSONS:
            return 'PHONE'
    else:
        if data['t_person.type'] not in PH_LIKE_PERSONS:
            return 'PHONE'
    return NoSuchRowInOEBS.msg


def get_owner_table_name_hz_contact_points_f(data):
    if W_API:
        if not data['t_person.fax']:
            return NoSuchRowInOEBS.msg
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return 'HZ_PARTIES'
    return NoSuchRowInOEBS.msg


def get_owner_table_name_hz_contact_points_cf(data):
    if W_API:
        if not data['t_person.email']:
            return NoSuchRowInOEBS.msg
    if data['t_person.type'] not in PH_LIKE_PERSONS:
        return 'HZ_PARTIES'
    return NoSuchRowInOEBS.msg


def get_email_address_hz_contact_points_e(data):
    if W_API:
        if not data['t_person.email']:
            return NoSuchRowInOEBS.msg
    if data['t_person.type'] in PH_LIKE_PERSONS:
        if data['t_person.email']:
            return data['t_person.email']
        else:
            return '-'
    return NoSuchRowInOEBS.msg


def get_email_address_hz_contact_points_ce(data):
    if W_API:
        if not data['t_person.email']:
            return NoSuchRowInOEBS.msg
    if data['t_person.type'] not in PH_LIKE_PERSONS:
        return data['t_person.email'] or '-'
    return NoSuchRowInOEBS.msg


def get_contact_point_type_hz_contact_points_e(data):
    if W_API:
        if not data['t_person.email']:
            return NoSuchRowInOEBS.msg
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return 'EMAIL'
    return NoSuchRowInOEBS.msg


def get_contact_point_type_hz_contact_points_ce(data):
    if W_API:
        if not data['t_person.email']:
            return NoSuchRowInOEBS.msg
    if data['t_person.type'] not in PH_LIKE_PERSONS:
        return 'EMAIL'
    return NoSuchRowInOEBS.msg


def get_country_code(phone):
    phone = phone_to_oebs_format(phone)
    if phone:
        for code in ['7', '90', '41', '1', '375', '972']:
            if phone.startswith(code):
                return code
    return ''


def get_phone_area_code(phone):
    phone = phone_to_oebs_format(phone)
    phone_wo_code = phone[len(get_country_code(phone)):]
    if phone_wo_code.startswith('650'):
        return '650'
    return ''


def get_address_geo_territory_code(data):
    code = data['t_person_category.oebs_country_code']
    resident = data['t_person_category.resident']
    person_type = data['t_person.type']
    return get_geo_territory_code(code, resident, person_type)


# код территории из геобазы
def get_geo_territory_code(code, resident, person_type):
    if resident:
        if code == 'NU':
            code = 'CV'
        elif code == 'HK':
            if W_API:
                return code
            code = 'CN'
        return code
    else:
        if person_type in ('yt_kzp', 'yt_kzu'):
            return 'KZ'
        return 'NU'



def get_party_id(object_id, firm_id=1):
    query = "select party_id from apps.hz_parties where orig_system_reference = '{0}'".format(object_id)
    try:
        return db.oebs().execute_oebs(firm_id, query, {}, single_row=True)['party_id']
    except KeyError:
        return {}


def get_cust_account(object_id, firm_id=1):
    query = "select cust_account_id from apps.hz_cust_accounts where orig_system_reference = '{0}'".format(
        object_id)
    try:
        return db.oebs().execute_oebs(firm_id, query, {}, single_row=True)['cust_account_id']
    except KeyError:
        return {}


def phone_to_oebs_format(phone):
    return ''.join(ch for ch in phone if ch not in ' +()-') if phone else ''


def live_signature_to_oebs_format(signature):
    return 'FAX' if signature == 0 else 'LIVE'


FIAS_MAP = {'13a5bcdb-9187-4d0f-b027-45f8ec29591a': u"Коми Респ, Усть-Вымский р-н, пгт Жешарт",
            'f8437a42-f896-4c74-9b33-4f5f8fd55418': u'Челябинская обл, г Челябинск',
            '3265a9dd-8983-4861-b77f-20ac3be4b815': u"Свердловская обл, Алапаевский р-н, пгт Махнёво",
            '5c0ecc6f-134f-4be6-834c-e49ca43d63ea': u"обл Московская, г Жуковский"}


def get_address_b(data):
    if W_API:
        post_suffix = data['t_person.postsuffix']
        street = data['t_person.street']
        if data['t_person.type'] == 'yt':
            return data['t_person.address']
        elif data['t_person.type'] == 'endbuyer_yt':
            addr = data['t_person.address']
            return u', '.join([x for x in [addr, street, post_suffix] if x])
        else:
            fias_guid = data['t_person.fias_guid']
            if fias_guid:
                addr = FIAS_MAP[fias_guid]
                return u', '.join([addr, street, post_suffix])

            return data['t_person.postaddress'] or '-'

    if data['t_person.type'] in YT_LIKE_PERSONS_WO_ADDRESS:
        if data['t_person.type'] in ['yt', 'endbuyer_yt']:
            return data['t_person.address']
        return data['t_person.postaddress']

    fias_guid = data['t_person.fias_guid']
    if fias_guid:
        post_suffix = data['t_person.postsuffix']
        street = data['t_person.street']
        return u', '.join([FIAS_MAP[fias_guid], street, post_suffix])
    return data['t_person.postaddress'] or '-'


def get_address_by_fias(fias, street, post_suffix):
    address_parts = get_fias_address_parts(fias, [])
    address_parts[0], address_parts[1] = address_parts[1], address_parts[0]
    address_parts.reverse()
    for i in range(1, len(address_parts) - 1, 2):
        address_parts[i] += ','
    if street:
        address_parts[-1] += ','
        address_parts.append(street)
    if post_suffix:
        address_parts[-1] += ','
        address_parts.append(post_suffix)
    return ' '.join(address_parts)


def get_address_city(data):
    if data['t_person.type'] in UR_LIKE_PERSONS:
        if data['t_person.type'] == 'ur':
            return ''
        return data['t_person.city'] or ''

    if data['t_person.type'] in YT_LIKE_PERSONS_WO_ADDRESS:
        return ''

    if data['t_person.type'] in YT_LIKE_PERSONS_W_ADDRESS:
        return data['t_person.city'] or ''

    if data['t_person.type'] in PH_LIKE_PERSONS:
        if data['t_person.is_partner'] == 1:
            return ''
    return data['t_person.city'] or ''


def get_legal_country(data):
    if W_API:
        if not data['t_person.legaladdress']:
            return NoSuchRowInOEBS.msg
        if data['t_person.type'] in PH_LIKE_PERSONS:
            if not data['t_person.is_partner']:
                return NoSuchRowInOEBS.msg
        if not data.get('t_extprops.person.legal_fias_guid') and not data['t_person.legaladdress']:
            return NoSuchRowInOEBS.msg
        if not data['t_person_category.resident']:
            return get_geo_territory_code(None, 0, data['t_person.type'])

    if data['t_person.type'] in PH_LIKE_PERSONS:
        if not data['t_person.is_partner']:
            return NoSuchRowInOEBS.msg

    return data['t_person_category.oebs_country_code']


def get_shipment_country(data):
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    if data['t_person.type'] in YT_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    if W_API:
        if not data['t_person.address']:
            return NoSuchRowInOEBS.msg
    return data['t_person_category.oebs_country_code']


def get_legal_address(data):
    if W_API:
        if data['t_person.type'] in YT_LIKE_PERSONS and not data['t_person.legaladdress']:
            return NoSuchRowInOEBS.msg

        if not data.get('t_extprops.person.legal_fias_guid') and not data['t_person.legaladdress']:
            return NoSuchRowInOEBS.msg

        if data['t_person.type'] in PH_LIKE_PERSONS:
            if not data['t_person.is_partner']:
                return NoSuchRowInOEBS.msg

        legal_fias_guid = data.get('t_extprops.person.legal_fias_guid')
        if legal_fias_guid:
            post_suffix = data['t_person.legal_address_home']
            street = data['t_person.legal_address_street']
            return u', '.join([FIAS_MAP[legal_fias_guid], street, post_suffix])

    if data['t_person.type'] in PH_LIKE_PERSONS:
        if not data['t_person.is_partner']:
            return NoSuchRowInOEBS.msg

    if data['t_person.type'] not in YT_LIKE_PERSONS:
        legal_fias_guid = data.get('t_extprops.person.legal_fias_guid')
        if legal_fias_guid:
            post_suffix = data['t_person.legal_address_home']
            street = data['t_person.legal_address_street']
            return u', '.join([FIAS_MAP[legal_fias_guid], street, post_suffix])

    return data['t_person.legaladdress'] or '-'


def get_shipment_address(data):
    if data['t_person.type'] in PH_LIKE_PERSONS or data['t_person.type'] in YT_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    if W_API:
        if not data['t_person.address']:
            return NoSuchRowInOEBS.msg
    return data['t_person.address'] or '-'


def get_legal_postcode(data):
    if W_API:
        if data['t_person.type'] in YT_LIKE_PERSONS and not data['t_person.legaladdress']:
            return NoSuchRowInOEBS.msg
        if not data.get('t_person.legal_fias_guid') and not data['t_person.legaladdress']:
            return NoSuchRowInOEBS.msg

    if data['t_person.type'] in PH_LIKE_PERSONS:
        if not data['t_person.is_partner']:
            return NoSuchRowInOEBS.msg
    return ''


def get_shipment_postcode(data):
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    if data['t_person.type'] in YT_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    if W_API:
        if not data['t_person.address']:
            return NoSuchRowInOEBS.msg
    return ''


def get_legal_city(data):
    if W_API:
        if not data.get('t_extprops.person.legal_fias_guid') and not data['t_person.legaladdress']:
            return NoSuchRowInOEBS.msg
        if data['t_person.type'] in YT_LIKE_PERSONS and not data['t_person.legaladdress']:
            return NoSuchRowInOEBS.msg

    if data['t_person.type'] in PH_LIKE_PERSONS:
        if not data['t_person.is_partner']:
            return NoSuchRowInOEBS.msg

    return ''


def get_address_location_id(data):
    return get_oebs_location_id(data['t_firm.id'], 'P{0}_B'.format(data['t_person.id']))


def get_shipment_address_location_id(data):
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    if data['t_person.type'] in YT_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    if W_API:
        if not data['t_person.address']:
            return NoSuchRowInOEBS.msg
    return get_oebs_location_id(data['t_firm.id'], 'P{0}_S'.format(data['t_person.id']))


def get_legal_address_location_id(data):
    if W_API:
        if data['t_person.type'] in YT_LIKE_PERSONS and not data['t_person.legaladdress']:
            return NoSuchRowInOEBS.msg
        if not data.get('t_extprops.person.legal_fias_guid') and not data['t_person.legaladdress']:
            return NoSuchRowInOEBS.msg

    if data['t_person.type'] in PH_LIKE_PERSONS:
        if not data['t_person.is_partner']:
            return NoSuchRowInOEBS.msg

    return get_oebs_location_id(data['t_firm.id'], 'P{0}_L'.format(data['t_person.id']))


def get_shipment_address_party_id(data):
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    if data['t_person.type'] in YT_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    if W_API:
        if not data['t_person.address']:
            return NoSuchRowInOEBS.msg
    if data['t_person.type'] in UR_LIKE_PERSONS:
        return get_oebs_party_id(data['t_firm.id'], 'I{0}'.format(data['t_person.inn']))
    return get_oebs_party_id(data['t_firm.id'], 'P{0}'.format(data['t_person.id']))


def get_shipment_site_use_type(data):
    if W_API:
        if not data.get('t_person.address'):
            return NoSuchRowInOEBS.msg
    if data['t_person.type'] in YT_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    return 'SHIP_TO'


def get_shipment_party_side_id(data):
    if data['t_person.type'] in YT_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    if W_API:
        if not data['t_person.address']:
            return NoSuchRowInOEBS.msg
    return get_oebs_party_site_id(data['t_firm.id'], 'P{0}_S'.format(data['t_person.id']))


def get_address_party_id(data):
    if data['t_person.type'] in UR_LIKE_PERSONS:
        return get_oebs_party_id(data['t_firm.id'], 'I{0}'.format(data['t_person.inn']))
    return get_oebs_party_id(data['t_firm.id'], 'P{0}'.format(data['t_person.id']))


def get_legal_address_party_id(data):
    if W_API:
        if data['t_person.type'] in YT_LIKE_PERSONS and not data['t_person.legaladdress']:
            return NoSuchRowInOEBS.msg
        if not data.get('t_extprops.person.legal_fias_guid') and not data['t_person.legaladdress']:
            return NoSuchRowInOEBS.msg

    if data['t_person.type'] in UR_LIKE_PERSONS:
        return get_oebs_party_id(data['t_firm.id'], 'I{0}'.format(data['t_person.inn']))
    if data['t_person.type'] in PH_LIKE_PERSONS:
        if not data['t_person.is_partner']:
            return NoSuchRowInOEBS.msg
    return get_oebs_party_id(data['t_firm.id'], 'P{0}'.format(data['t_person.id']))


def get_legal_site_use_type(data):
    if W_API:
        if data['t_person.type'] in YT_LIKE_PERSONS_WO_ADDRESS and not data['t_person.legaladdress']:
            return NoSuchRowInOEBS.msg
        if not data.get('t_person.legal_fias_guid') and not data['t_person.legaladdress']:
            return NoSuchRowInOEBS.msg

    if data['t_person.type'] in PH_LIKE_PERSONS:
        if not data['t_person.is_partner']:
            return NoSuchRowInOEBS.msg
    return 'LEGAL'


def get_legal_invalid_address(data):
    if W_API and not data['t_person.legaladdress']:
        return NoSuchRowInOEBS.msg
    if data['t_person.type'] in PH_LIKE_PERSONS:
        if not data['t_person.is_partner']:
            return NoSuchRowInOEBS.msg

    if data['t_person.type'] in YT_LIKE_PERSONS_WO_ADDRESS and W_API and not data['t_person.legaladdress']:
        return NoSuchRowInOEBS.msg
    return 'I' if data['t_person.invalid_address'] else 'A'


def get_shipment_invalid_address(data):
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    if data['t_person.type'] in YT_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    if W_API:
        if not data['t_person.address']:
            return NoSuchRowInOEBS.msg
    return 'I' if data['t_person.invalid_address'] else 'A'


def get_invalid_address(data):
    if W_API:
        if not (data['t_person.fias_guid'] or data['t_person.postaddress']):
            return 'I'
    return 'I' if data['t_person.invalid_address'] else 'A'


def get_vat_payer(data):
    if W_API:
        if data.get('t_extprops.person.vat_payer', None) is not None:
            return str(data['t_extprops.person.vat_payer'])
        return ''
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return ''
    if data['t_person.type'] in YT_LIKE_PERSONS:
        return ''
    if data.get('t_extprops.person.vat_payer', None) is not None:
        return str(data['t_extprops.person.vat_payer'])
    return ''


def get_legal_cust_account_id(data):
    if W_API and not data['t_person.legaladdress']:
        return NoSuchRowInOEBS.msg
    if data['t_person.type'] in PH_LIKE_PERSONS:
        if not data['t_person.is_partner']:
            return NoSuchRowInOEBS.msg
    return get_oebs_person_cust_account_id(data['t_firm.id'], data['t_person.id'])


def get_shipment_cust_account_id(data):
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    if data['t_person.type'] in YT_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    if W_API:
        if not data['t_person.address']:
            return NoSuchRowInOEBS.msg
    return get_oebs_person_cust_account_id(data['t_firm.id'], data['t_person.id'])


def get_legal_territory(data):
    if W_API and not data['t_person.legaladdress']:
        return NoSuchRowInOEBS.msg
    if data['t_person.type'] in PH_LIKE_PERSONS:
        if not data['t_person.is_partner']:
            return NoSuchRowInOEBS.msg
    return get_geo_territory_code(data['t_person_category.oebs_country_code'],
                                  data['t_person_category.resident'],
                                  data['t_person.type'])


def get_shipment_territory(data):
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    if data['t_person.type'] in YT_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    if W_API:
        if not data['t_person.address']:
            return NoSuchRowInOEBS.msg
    return get_geo_territory_code(data['t_person_category.oebs_country_code'],
                                  data['t_person_category.resident'],
                                  data['t_person.type'])


def get_legal_vat_payer(data):
    if W_API:
        if not data['t_person.legaladdress']:
            return NoSuchRowInOEBS.msg
        if data['t_person.type'] in PH_LIKE_PERSONS:
            if not data['t_person.is_partner']:
                return NoSuchRowInOEBS.msg
        if data.get('t_extprops.person.vat_payer') is not None:
            return str(data['t_extprops.person.vat_payer'])
        return ''

    if data['t_person.type'] in PH_LIKE_PERSONS:
        if not data['t_person.is_partner']:
            return NoSuchRowInOEBS.msg
        else:
            return ''
    if data['t_person.type'] in YT_LIKE_PERSONS:
        return ''
    if data.get('t_extprops.person.vat_payer') is not None:
        return str(data['t_extprops.person.vat_payer'])
    return ''


def get_shipment_vat_payer(data):
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    if data['t_person.type'] in YT_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    if W_API:
        if not data['t_person.address']:
            return NoSuchRowInOEBS.msg
    if data.get('t_extprops.person.vat_payer', None) is not None:
        return str(data['t_extprops.person.vat_payer'])
    return ''


def get_shipment_person_resident(data):
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    if data['t_person.type'] in YT_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    if W_API:
        if not data['t_person.address']:
            return NoSuchRowInOEBS.msg
    if data['t_person_category.resident'] and data['t_person_category.region_id'] == data['t_firm.region_id']:
        return '10'
    else:
        return '20'


def get_legal_person_resident(data):
    if W_API:
        if not data['t_person.legaladdress']:
            return NoSuchRowInOEBS.msg
        if data['t_person.type'] in PH_LIKE_PERSONS:
            if not data['t_person.is_partner']:
                return NoSuchRowInOEBS.msg
        return '10' if data['t_person_category.resident'] else '20'

    if data['t_person.type'] in PH_LIKE_PERSONS:
        if not data['t_person.is_partner']:
            return NoSuchRowInOEBS.msg
    if data['t_person_category.resident'] and data['t_person_category.region_id'] == data['t_firm.region_id']:
        return '10'
    else:
        return '20'


def get_legal_site_use_code(data):
    if W_API:
        if data['t_person.type'] in YT_LIKE_PERSONS and not data['t_person.legaladdress']:
            return NoSuchRowInOEBS.msg
        if not data['t_person.legaladdress'] or not data['t_extprops.person.legal_fias_guid']:
            return NoSuchRowInOEBS.msg
    if data['t_person.type'] in PH_LIKE_PERSONS:
        if not data['t_person.is_partner']:
            return NoSuchRowInOEBS.msg
    return 'LEGAL'


def get_shipment_site_use_code(data):
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    if data['t_person.type'] in YT_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    if W_API:
        if not data['t_person.address']:
            return NoSuchRowInOEBS.msg
    return 'SHIP_TO'


@balance_locator('t_extprops.person.legal_fias_guid')
def get_legal_address_location(data):
    if W_API:
        if data['t_person.type'] in YT_LIKE_PERSONS and not data['t_person.legaladdress']:
            return NoSuchRowInOEBS.msg

        if not data.get('t_extprops.person.legal_fias_guid') and not data['t_person.legaladdress']:
            return NoSuchRowInOEBS.msg
    if data['t_person.type'] in PH_LIKE_PERSONS:
        if not data['t_person.is_partner']:
            return NoSuchRowInOEBS.msg

    return 'P{0}_L'.format(data['t_person.id'])


def get_shipment_address_location(data):
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    if data['t_person.type'] in YT_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    if W_API:
        if not data['t_person.address']:
            return NoSuchRowInOEBS.msg
    return 'P{0}_S'.format(data['t_person.id'])


def get_legal_cust_acct_site_id(data):
    if W_API:
        if data['t_person.type'] in YT_LIKE_PERSONS and not data['t_person.legaladdress']:
            return NoSuchRowInOEBS.msg
        if not data['t_person.legaladdress'] or not data.get('t_extprops.person.legal_fias_guid'):
            return NoSuchRowInOEBS.msg

    if data['t_person.type'] in PH_LIKE_PERSONS:
        if not data['t_person.is_partner']:
            return NoSuchRowInOEBS.msg
    return get_oebs_cust_acct_site_id(data['t_firm.id'], '{0}_L'.format(data['t_person.id']))


def get_shipment_cust_acct_site_id(data):
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    if data['t_person.type'] in YT_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    if W_API:
        if not data['t_person.address']:
            return NoSuchRowInOEBS.msg
    return get_oebs_cust_acct_site_id(data['t_firm.id'], '{0}_S'.format(data['t_person.id']))


def get_legal_party_site_id(data):
    if W_API and not data['t_person.legaladdress']:
        return NoSuchRowInOEBS.msg
    if data['t_person.type'] in PH_LIKE_PERSONS:
        if not data['t_person.is_partner']:
            return NoSuchRowInOEBS.msg
    return get_oebs_party_site_id(data['t_firm.id'], 'P{0}_L'.format(data['t_person.id']))


def get_shipment_party_site_id(data):
    if data['t_person.type'] in PH_LIKE_PERSONS:
        if not data['t_person.is_partner']:
            return NoSuchRowInOEBS.msg
    return get_oebs_party_site_id(data['t_firm.id'], 'P{0}_S'.format(data['t_person.id']))


def get_shipment_city(data):
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    if data['t_person.type'] in YT_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    if W_API:
        if not data['t_person.address']:
            return NoSuchRowInOEBS.msg
    return ''


def get_legal_kladr_code(data):
    if W_API:
        if data['t_person.type'] in YT_LIKE_PERSONS and not data['t_person.legaladdress']:
            return NoSuchRowInOEBS.msg
        if not data.get('t_extprops.person.legal_fias_guid') and not data['t_person.legaladdress']:
            return NoSuchRowInOEBS.msg

    if data['t_person.type'] in PH_LIKE_PERSONS:
        if not data['t_person.is_partner']:
            return NoSuchRowInOEBS.msg
        return ''
    return None


def get_shipment_kladr_code(data):
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    if data['t_person.type'] in YT_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    if W_API:
        if not data['t_person.address']:
            return NoSuchRowInOEBS.msg
    return None


def get_legal_street(data):
    if W_API:
        if not data.get('t_extprops.person.legal_fias_guid') and not data['t_person.legaladdress']:
            return NoSuchRowInOEBS.msg

        if data['t_person.type'] in YT_LIKE_PERSONS:
            if not data['t_person.legaladdress']:
                return NoSuchRowInOEBS.msg
            else:
                post_suffix = data['t_person.legal_address_home']
                street = data['t_person.legal_address_street']
                return ', '.join([i for i in [street, post_suffix] if i])

    if data['t_person.type'] in PH_LIKE_PERSONS:
        if not data['t_person.is_partner']:
            return NoSuchRowInOEBS.msg
        if not data['t_person.legaladdress']:
            return ''
        else:
            post_suffix = data['t_person.legal_address_home']
            street = data['t_person.legal_address_street']
            return ', '.join([i for i in [street, post_suffix] if i])
    if data['t_person.type'] in UR_LIKE_PERSONS:
        post_suffix = data['t_person.legal_address_home']
        street = data['t_person.legal_address_street']
        return ', '.join([i for i in [street, post_suffix] if i])


def get_shipment_street(data):
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    if data['t_person.type'] in YT_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    if W_API:
        if not data['t_person.address']:
            return NoSuchRowInOEBS.msg
    return None


def get_legal_street_first_50(data):
    if W_API:
        if data['t_person.type'] in YT_LIKE_PERSONS and not data['t_person.legaladdress']:
            return NoSuchRowInOEBS.msg

    if data['t_person.type'] in PH_LIKE_PERSONS:
        if not data['t_person.is_partner']:
            return NoSuchRowInOEBS.msg
        return ''
    if data['t_person.type'] in UR_LIKE_PERSONS:
        post_suffix = data['t_person.legal_address_home']
        street = data['t_person.legal_address_street']
        return ', '.join([i for i in [street, post_suffix] if i])


def get_address_postcode(data):
    if data['t_person.type'] in YT_LIKE_PERSONS_WO_ADDRESS:
        return ''
    return data['t_person.postcode'] or ''


def get_subject_type_signer(data):
    if data['t_person.type'] in PH_LIKE_PERSONS or data['t_person.type'] == 'yt_kzp':
        return NoSuchRowInOEBS.msg
    if data['t_person.signer_person_name'] is not None:
        return 'PERSON'
    return NoSuchRowInOEBS.msg


def get_subject_type(data):
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    return 'PERSON'


def get_object_type(data):
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    return 'ORGANIZATION'


def get_object_type_signer(data):
    if data['t_person.type'] in PH_LIKE_PERSONS or data['t_person.type'] == 'yt_kzp':
        return NoSuchRowInOEBS.msg
    if data['t_person.signer_person_name']:
        return 'ORGANIZATION'
    return NoSuchRowInOEBS.msg


def get_object_table_name(data):
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    return 'HZ_PARTIES'


def get_subject_table_name(data):
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    return 'HZ_PARTIES'


def get_subject_table_name_signer(data):
    if data['t_person.type'] in PH_LIKE_PERSONS or data['t_person.type'] == 'yt_kzp':
        return NoSuchRowInOEBS.msg
    if data['t_person.signer_person_name']:
        return 'HZ_PARTIES'
    return NoSuchRowInOEBS.msg


def get_object_table_name_signer(data):
    if data['t_person.type'] in PH_LIKE_PERSONS or data['t_person.type'] == 'yt_kzp':
        return NoSuchRowInOEBS.msg
    if data['t_person.signer_person_name'] is not None:
        return 'HZ_PARTIES'
    return NoSuchRowInOEBS.msg


def get_relationship_code(data):
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    return 'CONTACT_OF'


def get_relationship_code_signer(data):
    if data['t_person.type'] in PH_LIKE_PERSONS or data['t_person.type'] == 'yt_kzp':
        return NoSuchRowInOEBS.msg
    if data['t_person.signer_person_name'] is not None:
        return 'CONTACT_OF'
    return NoSuchRowInOEBS.msg


def get_relationship_type(data):
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return NoSuchRowInOEBS.msg
    return 'CONTACT'


def get_relationship_type_signer(data):
    if data['t_person.type'] in PH_LIKE_PERSONS or data['t_person.type'] == 'yt_kzp':
        return NoSuchRowInOEBS.msg
    if data['t_person.signer_person_name']:
        return 'CONTACT'
    return NoSuchRowInOEBS.msg


def get_object_id(data):
    if data['t_person.type'] in YT_LIKE_PERSONS:
        return get_oebs_party_id(data['t_firm.id'], 'P{0}'.format(data['t_person.id']))
    if data['t_person.type'] in UR_LIKE_PERSONS:
        if data['t_person.inn']:
            return get_oebs_party_id(data['t_firm.id'], 'I{0}'.format(data['t_person.inn']))
    return NoSuchRowInOEBS.msg


def get_object_id_signer(data):
    if data['t_person.type'] in PH_LIKE_PERSONS or data['t_person.type'] == 'yt_kzp':
        return NoSuchRowInOEBS.msg
    if data['t_person.signer_person_name']:
        if data['t_person.type'] in YT_LIKE_PERSONS:
            return get_oebs_party_id(data['t_firm.id'], 'P{0}'.format(data['t_person.id']))
        if data['t_person.type'] in UR_LIKE_PERSONS:
            if data['t_person.inn']:
                return get_oebs_party_id(data['t_firm.id'], 'I{0}'.format(data['t_person.inn']))
    else:
        return NoSuchRowInOEBS.msg


def get_street_b(data, slice_):
    if W_API:
        if data['t_person.type'] == 'yt':
            return None
    else:
        if data['t_person.type'] in YT_LIKE_PERSONS_WO_ADDRESS:
            return None
    street = data['t_person.street']
    postsuffix = data['t_person.postsuffix']
    street_b = ', '.join([i for i in [street, postsuffix] if i])[slice_]
    if street_b:
        return street_b


def get_address_country(data):
    if data['t_person_category.resident'] or not W_API:
        return data['t_person_category.oebs_country_code']
    return get_geo_territory_code(None, 0, data['t_person.type'])


def get_address_kladr(data):
    if data['t_person.type'] in YT_LIKE_PERSONS:
        return ''
    if data['t_person.fias_guid']:
        return ''
    return data['t_person.kladr_code'] or None


def get_fias_address_parts(fias, parts):
    if fias:
        fias_address = db.balance().execute('''SELECT * FROM t_fias WHERE guid = :fias''', {'fias': fias},
                                            single_row=True)
        if fias_address:
            parts.append(fias_address['short_name'])
            parts.append(fias_address['formal_name'])
            return get_fias_address_parts(fias_address['parent_guid'], parts)
    return parts


def get_oebs_doc_type(bill_doc_type):
    doc_type_map = {
        u'Устав': '1',
        u'Доверенность': '2',
        u'Приказ': '3',
        u'Распоряжение': '4',
        u'Положение о филиале': '5',
        u'Свидетельство о регистрации': '6',
        u'Договор': '7',
        u'Протокол': '8',
        u'Решение': '9',
        u'устав': '1',
        u'доверенность': '2',
        u'Св-во о регистрации': '6',
        u'Доверенности': '2',
        u'Устава': '1',
        u'Приказа': '3',
        u'Распоряжения': '4',
        u'Положения о филиале': '5',
        u'Свидетельства о регистрации': '6',
        u'Договора': '7',
        u'Протокола': '8',
        None: None}
    return doc_type_map[bill_doc_type]


@balance_locator('t_person_category.resident')
def get_resident_value(data):
    if W_API:
        return '10' if data['t_person_category.resident'] else '20'

    if data['t_person.type'] in YT_LIKE_PERSONS:
        if data['t_person.type'] == 'kzu':
            return '10'
        return '20'
    if data['t_person.type'] in UR_LIKE_PERSONS:
        return '10'
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return '10' if data['t_person_category.resident'] else '20'


def get_addr_resident_value(data):
    if not W_API:
        if (data['t_person_category.resident']
                and data['t_person_category.region_id'] == data['t_firm.region_id']):
            return '10'
        return '20'

    return '10' if data['t_person_category.resident'] else '20'


def get_delivery_type(data):
    if W_API:
        if data['t_person.type'] in ('ur', 'ph') and data['t_person.delivery_type'] != 5:
            return '_'.join([value for value in [delivery_type_map[data['t_person.delivery_type']],
                                                 data['t_person.delivery_city']] if value])
        return delivery_type_map[data['t_person.delivery_type']]

    if data['t_person.type'] in YT_LIKE_PERSONS:
        return delivery_type_map[data['t_person.delivery_type']]
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return '_'.join([value for value in [delivery_type_map[data['t_person.delivery_type']],
                                             data['t_person.delivery_city']] if value])
    return merge_delivery_type_and_city(data['t_person.delivery_type'],
                                        data['t_person.delivery_city'],
                                        resident=get_resident_value(data))


def get_bik(data):
    if data['t_person.type'] in YT_LIKE_PERSONS:
        return None
    return data['t_person.bik']


def get_wallet_number(data):
    wallet_type = data.get('t_contract_attributes.bank_type', '')
    if not wallet_type:
        return None
    if wallet_type == '3':
        return data.get('t_extprops.person.yamoney_wallet', '')
    if wallet_type == '4':
        return data.get('t_extprops.person.webmoney_wallet', '')
    if wallet_type == '5':
        return data.get('t_extprops.person.paypal_wallet', '')
    if wallet_type == '7':
        return data.get('t_extprops.person.payoneer_wallet', '')
    if wallet_type == '8':
        return data.get('t_extprops.person.pingpong_wallet', '')


def get_intercompany(data):
    return data.get('t_extprops.client.intercompany')


def get_iban(data):
    if data['t_person.type'] == 'kzu':
        return data.get('t_person.iik', '')
    return data.get('t_extprops.person.iban', None)


def get_swift(data):
    if data['t_person.type'] == 'kzu':
        return data.get('t_contract_attributes.bik', None)
    return data.get('t_extprops.person.swift', None)


def get_early_docs(data):
    if data.get('t_extprops.person.early_docs'):
        return 'Y'
    return 'N'


def get_bankcity(data):
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return ''
    return data['t_person.bankcity']


@balance_locator('t_firm.id')
def get_edo_offers(data):
    edo_info = steps.PersonSteps.get_edo_by_firm(data['t_person.id'], data['t_firm.id'])
    org_id = \
        db.balance().execute(
            '''SELECT OEBS_ORG_ID FROM T_FIRM_EXPORT WHERE EXPORT_TYPE = 'OEBS' AND firm_id = :firm_id''',
            {'firm_id': data['t_firm.id']})[0]['oebs_org_id']
    result = {}
    for edo in edo_info:
        from_dt_nullified = utils.Date.nullify_time_of_date(edo['from_dt'])
        if result.get(from_dt_nullified):
            if result[from_dt_nullified]['from_dt'] < edo['from_dt']:
                result[from_dt_nullified] = {'date_start': from_dt_nullified,
                                             'from_dt': edo['from_dt'],
                                             'edo_type': edo['offer_type'],
                                             'org_id': org_id,
                                             'edo_operator': '2BE' if edo['edo_type_id'] else None,
                                             'customer_id': get_oebs_person_cust_account_id(data['t_firm.id'],
                                                                                            data['t_person.id'])}
        else:
            result[from_dt_nullified] = {'date_start': from_dt_nullified,
                                         'from_dt': edo['from_dt'],
                                         'edo_type': edo['offer_type'],
                                         'org_id': org_id,
                                         'edo_operator': '2BE' if edo['edo_type_id'] else None,
                                         'customer_id': get_oebs_person_cust_account_id(data['t_firm.id'],
                                                                                        data['t_person.id'])}
    edo_list = []

    dates = sorted(result.keys())
    for k, v in result.iteritems():
        del v['from_dt']
        date_pos = dates.index(v['date_start'])
        if date_pos != len(dates) - 1:
            v['date_end'] = dates[date_pos + 1] - datetime.timedelta(days=1)
        else:
            v['date_end'] = None
        edo_list.append(v)

    return edo_list or NoSuchRowInOEBS.msg


def get_ben_bank(data):
    if data['t_person.type'] == 'kzu':
        return data.get('t_person.bank', '')
    if data.get('t_extprops.person.ben_bank', False):
        return data['t_extprops.person.ben_bank']


def get_beneficiar_code(data):
    if data['t_person.type'] == 'kzp':
        return '19'
    return data.get('t_extprops.person.kz_kbe', None)


def get_tax_reg_number(data):
    if data['t_person.type'] in UR_LIKE_PERSONS:
        return ''
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return ''
    return data.get('t_contract_attributes.rnn', '')


def get_bank_type(data):
    if data['t_person.type'] in UR_LIKE_PERSONS:
        return None
    bank_type = data.get('t_contract_attributes.bank_type', '0')
    return str(bank_type)


def get_person_account(data):
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return data.get('t_contract_attributes.person_account', '')
    return ''


def get_ownership_type(data):
    if data['t_person.type'] in PH_LIKE_PERSONS:
        if data['t_person.type'] == 'ph':
            return 'PERSON'
        else:
            return data.get('t_extprops.person.ownership_type')
    if data.get('t_extprops.person.ownership_type') is not None:
        return data.get('t_extprops.person.ownership_type')
    else:
        if data['t_person.type'] == 'ur':
            return 'ORGANIZATION'


def get_person_name(data):
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return data['t_person.lname']
    return data['t_person.name']


def get_person_kpp(data):
    if data['t_person.type'] in PH_LIKE_PERSONS:
        return None
    if data['t_person.type'] in YT_LIKE_PERSONS:
        return ''
    return data['t_person.kpp']


def get_oebs_gender(bill_gender):
    gender_map = {
        'M': 'M',
        'W': 'F',
        'X': None,
        u'м': 'M',
        None: None}
    return gender_map[bill_gender]


delivery_type_map = {
    0: 'PR',
    1: 'PR',
    2: 'CY',
    3: 'CC',
    4: 'VIP',
    5: 'EDO',
}


def merge_delivery_type_and_city(delivery_type, delivery_city, resident):
    if delivery_city and delivery_type != 5 and resident == '10':
        return '_'.join([delivery_type_map[delivery_type], delivery_city])
    else:
        return delivery_type_map[delivery_type]

    # ====================================================================================================================


def delete_all_extprops(person_id):
    db.balance().execute("""delete from t_extprops where classname = 'Person' and object_id = :object_id""",
                         {'object_id': person_id})


def delete_all_attributes(person_id):
    attribute_batch_id = db.get_person_by_id(person_id)[0]['attribute_batch_id']
    db.balance().execute("""delete from t_contract_attributes where attribute_batch_id=:attribute_batch_id""",
                         {'attribute_batch_id': attribute_batch_id})


def check_attrs(person_id, attrs, firm_id, excluded_attrs=None, person_type=None):
    balance_str_locators, oebs_str_locators = get_attrs_str_locators(attrs)

    with reporter.step(u'Считываем данные из баланса'):
        balance_data = get_balance_person_data(person_id, firm_id, balance_str_locators)
    with reporter.step(u'Считываем данные из ОЕБС'):
        oebs_data = get_oebs_person_data(balance_data, firm_id, oebs_str_locators)
        oebs_data.update({'person_type': person_type})

    balance_values, oebs_values = read_attr_values(attrs, balance_data, oebs_data)

    utils.check_that(oebs_values, equal_to_casted_dict(balance_values),
                     step=u'Проверяем корректность данных плательщика в ОЕБС')


def create_person(person_type):
    client_id = steps.ClientSteps.create(prevent_oebs_export=True)
    person_id = steps.PersonSteps.create(client_id, PersonTypes.PH.code, full=False,
                                         inn_type=person_defaults.InnType.UNIQUE)
    db.balance().execute('''update t_person set type = :person_type where id = :person_id''',
                         {'person_type': person_type,
                          'person_id': person_id})
    return person_id


def export(firm_id, person_id, W_API):
    try:
        db.balance().execute("""insert into t_person_firm (person_id, firm_id) values (:person_id, :firm_id)""",
                             {'person_id': person_id, 'firm_id': firm_id})
    except Exception:
        pass

    steps.ExportSteps.export_oebs(person_id=person_id, firm_id=firm_id)


ALL_OEBS_TABLES = [ATTRS.HZ_PARTIES_P,
                   ATTRS.HZ_PARTIES_CP,
                   ATTRS.HZ_PARTIES_DP,
                   ATTRS.HZ_PARTIES_I,

                   ATTRS.HZ_CUST_ACCOUNTS,

                   ATTRS.XXAR_CUSTOMER_ATTRIBUTES,

                   ATTRS.XXAR_EDO_OPER_HIST_TBL,

                   ATTRS.HZ_ORG_CONTACTS_REPR_PC,
                   ATTRS.HZ_ORG_CONTACTS_SIGNER_PD,

                   ATTRS.HZ_RELATIONSHIPS_CP,
                   ATTRS.HZ_RELATIONSHIPS_DP,

                   ATTRS.HZ_CUST_ACCOUNT_ROLES_PC,
                   ATTRS.HZ_CUST_ACCOUNT_ROLES_PD,

                   ATTRS.HZ_CONTACT_POINTS_CP,
                   ATTRS.HZ_CONTACT_POINTS_CF,
                   ATTRS.HZ_CONTACT_POINTS_CE,
                   ATTRS.HZ_CONTACT_POINTS_P,
                   ATTRS.HZ_CONTACT_POINTS_F,
                   ATTRS.HZ_CONTACT_POINTS_E,

                   ATTRS.HZ_LOCATIONS_B,
                   ATTRS.HZ_LOCATIONS_S,
                   ATTRS.HZ_LOCATIONS_L,

                   ATTRS.HZ_PARTY_SITES_B,
                   ATTRS.HZ_PARTY_SITES_S,
                   ATTRS.HZ_PARTY_SITES_L,

                   ATTRS.HZ_PARTY_SITE_USES_B,
                   ATTRS.HZ_PARTY_SITE_USES_S,
                   ATTRS.HZ_PARTY_SITE_USES_L,

                   ATTRS.HZ_CUST_ACCT_SITES_B,
                   ATTRS.HZ_CUST_ACCT_SITES_S,
                   ATTRS.HZ_CUST_ACCT_SITES_L,

                   ATTRS.HZ_CUST_SITE_USES_B,
                   ATTRS.HZ_CUST_SITE_USES_S,
                   ATTRS.HZ_CUST_SITE_USES_L
                   ]

PH_LIKE_PERSONS = [PersonTypes.PH.code,
                   PersonTypes.ENDBUYER_PH.code,
                   PersonTypes.PH_AUTORU.code,
                   PersonTypes.USP.code,
                   PersonTypes.SW_PH.code,
                   PersonTypes.SW_YTPH.code,
                   PersonTypes.BY_YTPH.code,
                   PersonTypes.TRP.code,
                   PersonTypes.KZP.code,
                   PersonTypes.BYP.code,
                   PersonTypes.AM_PH.code,
                   PersonTypes.YTPH.code]

UA_LIKE_PERSONS = [PersonTypes.UA.code]

YT_LIKE_PERSONS_WO_ADDRESS = [PersonTypes.YT.code,
                              PersonTypes.EU_YT.code,
                              PersonTypes.IL_UR.code,
                              PersonTypes.AZ_UR.code,
                              PersonTypes.ENDBUYER_YT.code]

YT_LIKE_PERSONS_W_ADDRESS = [PersonTypes.HK_YT.code,
                             PersonTypes.USU.code,
                             PersonTypes.YT_KZP.code,
                             PersonTypes.YT_KZU.code,
                             PersonTypes.KZU.code,
                             PersonTypes.SW_UR.code,
                             PersonTypes.SW_YT.code,
                             PersonTypes.HK_UR.code]

YT_LIKE_PERSONS = YT_LIKE_PERSONS_WO_ADDRESS + YT_LIKE_PERSONS_W_ADDRESS

UR_LIKE_PERSONS = [PersonTypes.UR.code,
                   PersonTypes.ENDBUYER_UR.code,
                   PersonTypes.UR_AUTORU.code,
                   PersonTypes.TRU.code,
                   PersonTypes.BYU.code,
                   PersonTypes.AM_UR.code]


class Extprop(object):

    def __init__(self, attrname, value_num=None, value_str=None):
        self.attrname = attrname
        self.value_num = value_num
        self.value_str = value_str

    def set(self, person_id):
        value_num = self.value_num if not isinstance(self.value_num, list) else self.value_num[0]
        value_str = self.value_str if not isinstance(self.value_str, list) else self.value_str[0]
        db.balance().execute('''insert into T_EXTPROPS (id, classname, object_id, attrname, value_str, value_num) 
        values (S_EXTPROPS.nextval, 'Person', :person_id,  :attrname, :value_str, :value_num)''',
                             {'value_num': value_num,
                              'value_str': value_str,
                              'person_id': person_id,
                              'attrname': self.attrname,
                              })

    def update(self, person_id):
        value_num = self.value_num if not isinstance(self.value_num, list) else self.value_num[1]
        value_str = self.value_str if not isinstance(self.value_str, list) else self.value_str[1]
        db.balance().execute('''insert into T_EXTPROPS (id, classname, object_id, attrname, value_str, value_num) 
        values (S_EXTPROPS.nextval, 'Person', :person_id,  :attrname, :value_str, :value_num)''',
                             {'value_num': value_num,
                              'value_str': value_str,
                              'person_id': person_id,
                              'attrname': self.attrname,
                              })


class Attribute(object):
    def __init__(self, code, value_str=None, value_num=None, value_dt=None):
        self.code = code
        self.value_str = value_str
        self.value_num = value_num
        self.value_dt = value_dt

    def set(self, attribute_batch_id):
        value_num = self.value_num if not isinstance(self.value_num, list) else self.value_num[0]
        value_str = self.value_str if not isinstance(self.value_str, list) else self.value_str[0]
        value_dt = self.value_dt if not isinstance(self.value_dt, list) else self.value_dt[0]
        db.balance().execute('''INSERT INTO BO.T_CONTRACT_ATTRIBUTES (ID, RELATED_OBJECT_TABLE, 
        ATTRIBUTE_BATCH_ID, CODE, VALUE_STR, VALUE_NUM, VALUE_DT) VALUES (S_CONTRACT_ATTRIBUTES_ID.nextval, 'T_PERSON',
         :batch_id, :code, :value_str, :value_num, :value_dt)''', {'value_num': value_num,
                                                                   'value_str': value_str,
                                                                   'batch_id': attribute_batch_id,
                                                                   'code': self.code,
                                                                   "value_dt": value_dt
                                                                   })

    def update(self, attribute_batch_id):
        value_num = self.value_num if not isinstance(self.value_num, list) else self.value_num[1]
        value_str = self.value_str if not isinstance(self.value_str, list) else self.value_str[1]
        db.balance().execute('''INSERT INTO BO.T_CONTRACT_ATTRIBUTES (ID, RELATED_OBJECT_TABLE, 
        ATTRIBUTE_BATCH_ID, CODE, VALUE_STR, VALUE_NUM) VALUES (S_CONTRACT_ATTRIBUTES_ID.nextval, 'T_PERSON',
         :batch_id, :code, :value_str, :value_num)''', {'value_num': value_num,
                                                        'value_str': value_str,
                                                        'batch_id': attribute_batch_id,
                                                        'code': self.code,
                                                        })

    def delete(self, attribute_batch_id):
        db.balance().execute('''DELETE FROM BO.T_CONTRACT_ATTRIBUTES WHERE ATTRIBUTE_BATCH_ID=:batch_id
        AND CODE = :code''', {'batch_id': attribute_batch_id,
                              'code': self.code,
                              })


ALL_EXTPROPS = [Extprop('ben_account', value_str=['PINCHUK YEVHEN OLEXANDROVICH',
                                                  'Marushchenko Olena Oleksiyivna']),
                Extprop('ben_bank', value_str=['AS Expobank', 'BANK HAPOALIM B.M']),
                Extprop('corr_swift', value_str=['CNOVRUMMXXX', 'RZBAATWWXXX']),
                Extprop('early_docs', value_num=[1, 0]),
                Extprop('iban', value_str=['CY6913200001 0001 5011 6276 0010', 'LV61UNLA0050020493428']),
                Extprop('invalid_bankprops', value_num=[1, 0]),
                Extprop('legal_fias_guid', value_str=['f8437a42-f896-4c74-9b33-4f5f8fd55418',
                                                      '3265a9dd-8983-4861-b77f-20ac3be4b815']),
                Extprop('local_authority_doc_details', value_str=['dfvdvf', 'б/н от 13.03.2018 г.']),
                Extprop('local_bank', value_str='vdfvdfv'),
                Extprop('local_ben_bank', value_str='lkmkmk'),
                Extprop('local_city', value_str='rer'),
                Extprop('local_legaladdress', value_str='address'),
                Extprop('local_longname', value_str='local_longname'),
                Extprop('local_name', value_str=';k;lkl'),
                Extprop('local_other', value_str=';lk;lk'),
                Extprop('local_postaddress', value_str='eferf'),
                Extprop('local_representative', value_str='poipoipoi'),
                Extprop('local_signer_person_name', value_str='ppp'),
                Extprop('local_signer_position_name', value_str='ouu'),
                Extprop('other', value_str=['popoo', '123']),
                Extprop('ownership_type', value_str=['PERSON', 'SELFEMPLOYED']),
                Extprop('payoneer_wallet', value_str=['123456789', '3453']),
                Extprop('region', value_num=[21580, 10318]),
                Extprop('swift', value_str=['ANIKAM22', 'BYLADEM1001']),
                Extprop('vat_payer', value_num=[1, 0]),
                Extprop('vip', value_num=[1, 0]),
                ]

ALL_PERSON_ATTRS = [Attribute('ACCOUNT', value_str=['40802810300000514037', '40802810200000520192']),
                    Attribute('ADDRESS', value_str=['225710 Брестская обл., г. Пинск пр-д Калиновского, д. 2',
                                                    'Belize Offshore Centre, 3-d Floor, Barrack Road, P.O Box 1777 , Belize City, Belize']),
                    Attribute('ADDRESS_CODE', value_str=['500000440810034', '010000010030039']),
                    Attribute('AUTHORITY_DOC_DETAILS', value_str=['Записи о регистрации в ЕГРИП № 31821305',
                                                                  'Устава и Договора передачи полномочий']),
                    Attribute('AUTHORITY_DOC_TYPE', value_str=['Свидетельство о регистрации', 'Устав']),
                    Attribute('BANKCITY', value_str=['Ростов-на-Дону', 'Г.САНКТ-ПЕТЕРБУРГ']),
                    Attribute('BANK_TYPE', value_num=[3, 0]),
                    Attribute('BIK', value_str=['044030786', '044525600']),
                    Attribute('BIRTHDAY', value_dt=[datetime.datetime.now(), datetime.datetime.now().replace(day=1)]),
                    Attribute('CITY', value_str=['Санкт-Петербург', 'fdff']),
                    Attribute('DELIVERY_CITY', value_str=['KZN', 'ODS']),
                    Attribute('DELIVERY_TYPE', value_num=[0, 5]),
                    Attribute('EMAIL', value_str=['ehpanova@ya.ru', 'rrefe44']),
                    Attribute('FAX', value_str=['+7 812 5389048', '+7(3952)220162']),
                    Attribute('FIAS_GUID', value_str=['13a5bcdb-9187-4d0f-b027-45f8ec29591a',
                                                      '5c0ecc6f-134f-4be6-834c-e49ca43d63ea']),
                    Attribute('FNAME', value_str=['Котик', 'Пес']),
                    Attribute('IIK', value_str=['KZ838560000000463517', 'KZ899470398991054707']),
                    Attribute('INN_DOC_DETAILS', value_str=['200050068', '200050068455']),
                    Attribute('INVALID_ADDRESS', value_num=['1', '0']),
                    Attribute('KBK', value_str=['18210202103081013160', '1821020333013160']),
                    Attribute('KLADR_CODE', value_str=['7800000000000', '234234234234']),
                    Attribute('KPP', value_str=['500701001', '5007010013']),
                    Attribute('LIVE_SIGNATURE', value_num=[1, 0]),
                    Attribute('LEGALADDRESS', value_str=['Москва', 'Ямайка']),
                    Attribute('LEGAL_ADDRESS_STREET', value_str=['Льва толстого', 'Свердлова']),
                    Attribute('LEGAL_ADDRESS_HOME', value_str=['д. 16', 'д 23/55']),
                    Attribute('LNAME', value_str=['Котиков', 'Собакин']),
                    Attribute('LONGNAME', value_str=['Индивидуальный предприниматель Котляров Павел Олегович',
                                                     'милый котик']),
                    Attribute('MNAME', value_str=['Котикович', 'Собакович']),
                    Attribute('NAME', value_str='Короткое имя организации'),
                    Attribute('OKTMO', value_str=['43443435', '343443']),
                    Attribute('PFR', value_str=['066-713-612 70', '103-053-652 97']),
                    Attribute('PASSPORT_D', value_str=['2004-01-14', '2011-10-11']),
                    Attribute('PASSPORT_E', value_str=['Ахтубинским ОВД гор. Нижнекамска республики Татарстан',
                                                       'УВД г. Балаково и Балаковского р-на Саратовской области']),
                    Attribute('PASSPORT_N', value_str=['627813', '313787']),
                    Attribute('PASSPORT_S', value_str=['6301', '63014']),

                    Attribute('PASSPORT_CODE', value_str=['242002', '2420023']),

                    Attribute('PAYMENT_PURPOSE', value_str=['Оплата услуг Яндекс', 'бяк']),
                    Attribute('PERSON_ACCOUNT', value_str=['34345', 'r34234234']),
                    Attribute('PHONE', value_str=["+1 650 5173548", '+7(3952)220162']),
                    Attribute('POSTADDRESS', value_str=['620000, Екатеринбург, Антона Валека, 12',
                                                        'Деревушкино']),
                    Attribute('POSTCODE', value_str=['123456', '23424']),
                    Attribute('POSTSUFFIX',
                              value_str=["а/я уапоапукщпшоукпщшоукпщшуокпщшуокпщушкпоукщшпоукщшпоукщшпоукщшп32",
                                         "кв 23"]),
                    Attribute('REPRESENTATIVE', value_str=['Ахметов Виталий Тахирович',
                                                           'Пулькин А.А']),
                    Attribute('RNN', value_str=['123456789012', '123489012']),
                    Attribute('SIGNER_PERSON_GENDER', value_str=['M', 'W']),
                    Attribute('SIGNER_PERSON_NAME', value_str=['Хапаев Аскер Умарович', 'Капуль И.Е']),
                    Attribute('SIGNER_POSITION_NAME', value_str=['Индивидуальный предприниматель', 'Босс']),
                    Attribute('STREET', value_str=['улица Плакучая', 'Свердлова']),
                    ]

ALL_PERSONS = [
    # юрики
    (PersonTypes.UR, Firms.YANDEX_1, 'ur'),
    (PersonTypes.ENDBUYER_UR, Firms.YANDEX_1, 'ur_endbuyer'),
    (PersonTypes.UR_AUTORU, Firms.YANDEX_1, 'ur_autoru'),
    (PersonTypes.TRU, Firms.YANDEX_TURKEY_8, 'tru'),
    (PersonTypes.BYU, Firms.REKLAMA_BEL_27, 'byu'),
    (PersonTypes.AM_UR, Firms.TAXI_CORP_ARM_122, 'am_ur'),
    # (PersonTypes.UA, Firms.YANDEX_UA_2, ''),

    # физики
    (PersonTypes.PH, Firms.YANDEX_1, 'ph'),
    (PersonTypes.ENDBUYER_PH, Firms.YANDEX_1, 'endbuyer_ph'),
    (PersonTypes.PH_AUTORU, Firms.YANDEX_1, 'ph_autoru'),
    (PersonTypes.USP, Firms.YANDEX_INC_4, 'usp'),
    (PersonTypes.SW_PH, Firms.EUROPE_AG_7, 'sw_ph'),
    (PersonTypes.SW_YTPH, Firms.EUROPE_AG_7, 'sw_ytph'),
    (PersonTypes.BY_YTPH, Firms.EUROPE_AG_7, 'by_ytph'),
    (PersonTypes.TRP, Firms.YANDEX_TURKEY_8, 'trp'),
    (PersonTypes.KZP, Firms.KZ_25, 'kzp'),
    (PersonTypes.BYP, Firms.REKLAMA_BEL_27, 'byp'),
    (PersonTypes.AM_PH, Firms.TAXI_CORP_ARM_122, 'am_ph'),
    (PersonTypes.YTPH, Firms.YANDEX_1, 'ytph'),

    # нерезиденты
    (PersonTypes.YT, Firms.YANDEX_1, 'yt'),
    (PersonTypes.EU_YT, Firms.YANDEX_1, 'eu_yt'),
    (PersonTypes.IL_UR, Firms.YANDEX_GO_ISRAEL_35, 'il_ur'),
    (PersonTypes.AZ_UR, Firms.UBER_AZ_116, 'az_ur'),
    (PersonTypes.ENDBUYER_YT, Firms.YANDEX_1, 'endbuyer_yt'),

    # нерезиденты с подробными адресами
    (PersonTypes.HK_YT, Firms.UBER_AZ_116, 'hk_yt'),
    (PersonTypes.USU, Firms.YANDEX_INC_4, 'usu'),
    (PersonTypes.YT_KZP, Firms.YANDEX_1, 'yt_kzp'),
    (PersonTypes.YT_KZU, Firms.YANDEX_1, 'yt_kzu'),
    (PersonTypes.KZU, Firms.KZ_25, 'kzu'),
    (PersonTypes.SW_UR, Firms.EUROPE_AG_7, 'sw_ur'),
    (PersonTypes.SW_YT, Firms.EUROPE_AG_7, 'sw_yt'),
    (PersonTypes.HK_UR, Firms.HK_ECOMMERCE_33, 'hk_ur'),

]


# @pytest.mark.parametrize('partner', [
#     True,
#     False
# ])
# @pytest.mark.parametrize('person_type, firm, json_file', ALL_PERSONS, ids=lambda person_type, firm, json_file: person_type.code)
# def export_empty_person(person_type, firm, json_file, partner):
#     config_dict = json.loads(db.balance().execute("""SELECT *
#                                 FROM t_Config
#                                 WHERE ITEM = 'CLASSNAMES_EXPORTED_WITH_OEBS_API'""")[0]['value_json'])
#     person_conf = config_dict.get('Person')
#     if person_conf == 1:
#         W_API = True
#     else:
#         W_API = False
#     person_id = create_person(person_type.code)
#
#     delete_all_extprops(person_id)
#     delete_all_attributes(person_id)
#     attribute_batch_id = db.get_person_by_id(person_id)[0]['attribute_batch_id']
#
#     if person_type.code in UR_LIKE_PERSONS:
#         inn = Attribute('INN',
#                         value_str=person_defaults.get_inn_params(person_type='ur',
#                                                                  inn_type=person_defaults.InnType.UNIQUE)['inn'])
#         inn.set(attribute_batch_id)
#
#     if person_type.code in UR_LIKE_PERSONS or (person_type.code in YT_LIKE_PERSONS):
#         Attribute('NAME', value_str='Короткое имя организации').set(attribute_batch_id)
#     else:
#         Attribute('FNAME', value_str='Птичка').set(attribute_batch_id)
#
#     if partner:
#         Attribute('IS_PARTNER', value_num=1).set(attribute_batch_id)
#
#     export(firm.id, person_id, W_API)
#     check_attrs(person_id, ALL_OEBS_TABLES, firm_id=firm.id, person_type=person_type.code)

def check_json_contract(person_id, firm, json_file, partner=False):
    try:
        db.balance().execute("""insert into t_person_firm (person_id, firm_id) values (:person_id, :firm_id)""",
                             {'person_id': person_id, 'firm_id': firm.id})
    except Exception:
        pass

    steps.ExportSteps.init_oebs_api_export('Person', person_id)
    actual_json_data = steps.ExportSteps.get_json_data('Person', person_id)

    partner_sufix = '_partner' if partner else ''
    json_file = json_file + partner_sufix + '.json'

    steps.ExportSteps.log_json_contract_actions(json_contracts_repo_path,
                                                JSON_OEBS_PATH,
                                                json_file,
                                                balance_config.FIX_CURRENT_JSON_CONTRACT)

    contract_utils.process_json_contract(json_contracts_repo_path,
                                         JSON_OEBS_PATH,
                                         json_file,
                                         actual_json_data,
                                         replace_mask,
                                         balance_config.FIX_CURRENT_JSON_CONTRACT)


@pytest.mark.parametrize('partner', [
    True,
    False
])
@pytest.mark.parametrize('person_type, firm, json_file', ALL_PERSONS, ids=lambda person_type, firm, json_file: person_type.code)
def test_export_person(person_type, firm, json_file, partner):
    config_dict = json.loads(db.balance().execute("""SELECT *
                                FROM t_Config
                                WHERE ITEM = 'CLASSNAMES_EXPORTED_WITH_OEBS_API'""")[0]['value_json'])
    person_conf = config_dict.get('Person')
    if person_conf == 1:
        W_API = True
    else:
        W_API = False
    person_id = create_person(person_type.code)
    attribute_batch_id = db.get_person_by_id(person_id)[0]['attribute_batch_id']
    delete_all_extprops(person_id)
    for extprop in ALL_EXTPROPS:
        extprop.set(person_id)
    kz_in = Extprop('kz_in',
                    value_str=person_defaults.get_inn_params(person_type='ur',
                                                             inn_type=person_defaults.InnType.UNIQUE)['inn'])
    kz_in.set(person_id)

    delete_all_attributes(person_id)

    for attr in ALL_PERSON_ATTRS:
        attr.set(attribute_batch_id)

    if person_type.code not in PH_LIKE_PERSONS:
        inn = Attribute('INN',
                        value_str=person_defaults.get_inn_params(person_type='ur',
                                                                 inn_type=person_defaults.InnType.UNIQUE)['inn'])
        inn.set(attribute_batch_id)

    if partner:
        Attribute('IS_PARTNER', value_num=1).set(attribute_batch_id)

    if balance_config.JSON_CONTRACT_OEBS and json_file:
        check_json_contract(person_id, firm, json_file, partner)

    else:
        export(firm.id, person_id, W_API)
        check_attrs(person_id, ALL_OEBS_TABLES, firm_id=firm.id, person_type=person_type.code)


# @pytest.mark.parametrize('partner', [
#     True,
#     False
# ])
# @pytest.mark.parametrize('person_type, firm, json_file', ALL_PERSONS, ids=lambda person_type, firm, json_file: person_type.code)
# def update_person(person_type, firm, json_file, partner):
#     config_dict = json.loads(db.balance().execute("""SELECT *
#                                 FROM t_Config
#                                 WHERE ITEM = 'CLASSNAMES_EXPORTED_WITH_OEBS_API'""")[0]['value_json'])
#     person_conf = config_dict.get('Person')
#     if person_conf == 1:
#         W_API = True
#     else:
#         W_API = False
#     person_id = create_person(person_type.code)
#     attribute_batch_id = db.get_person_by_id(person_id)[0]['attribute_batch_id']
#     delete_all_extprops(person_id)
#     for extprop in ALL_EXTPROPS:
#         extprop.set(person_id)
#     kz_in_value = person_defaults.get_inn_params(person_type='ur',
#                                                  inn_type=person_defaults.InnType.UNIQUE)['inn']
#     Extprop('kz_in', value_str=kz_in_value).set(person_id)
#
#     delete_all_attributes(person_id)
#
#     for attr in ALL_PERSON_ATTRS:
#         attr.set(attribute_batch_id)
#
#     inn_value = person_defaults.get_inn_params(person_type='ur',
#                                                inn_type=person_defaults.InnType.UNIQUE)['inn']
#     if person_type.code not in PH_LIKE_PERSONS:
#         Attribute('INN', value_str=inn_value).set(attribute_batch_id)
#
#     if partner:
#         Attribute('IS_PARTNER', value_num=1).set(attribute_batch_id)
#
#     export(firm.id, person_id, W_API)
#     delete_all_extprops(person_id)
#     for extprop in ALL_EXTPROPS:
#         extprop.update(person_id)
#     Extprop('kz_in', value_str=kz_in_value).set(person_id)
#
#     delete_all_attributes(person_id)
#
#     for attr in ALL_PERSON_ATTRS:
#         attr.update(attribute_batch_id)
#     if person_type.code not in PH_LIKE_PERSONS:
#         Attribute('INN', value_str=inn_value).set(attribute_batch_id)
#     if partner:
#         Attribute('IS_PARTNER', value_num=1).set(attribute_batch_id)
#     export(firm.id, person_id, W_API)
#     check_attrs(person_id, ALL_OEBS_TABLES, firm_id=firm.id, person_type=person_type.code)


#
# @pytest.mark.parametrize('partner', [
#     True,
#     False
# ])
# @pytest.mark.parametrize('person_type, firm, json_file', ALL_PERSONS, ids=lambda person_type, firm, json_file: person_type.code)
# def test_delete_attrs_person(person_type, firm, json_file, partner):
#     person_id = create_person(person_type.code)
#     attribute_batch_id = db.get_person_by_id(person_id)[0]['attribute_batch_id']
#     delete_all_extprops(person_id)
#     for extprop in ALL_EXTPROPS:
#         extprop.set(person_id)
#     kz_in = Extprop('kz_in',
#                     value_str=person_defaults.get_inn_params(person_type='ur',
#                                                              inn_type=person_defaults.InnType.UNIQUE)['inn'])
#     kz_in.set(person_id)
#
#     delete_all_attributes(person_id)
#
#     for attr in ALL_PERSON_ATTRS:
#         attr.set(attribute_batch_id)
#
#     if person_type.code in UR_LIKE_PERSONS:
#         inn = Attribute('INN',
#                         value_str=person_defaults.get_inn_params(person_type='ur',
#                                                                  inn_type=person_defaults.InnType.UNIQUE)['inn'])
#         inn.set(attribute_batch_id)
#
#     if partner:
#         Attribute('IS_PARTNER', value_num=1).set(attribute_batch_id)
#
#     export(firm.id, person_id, W_API)
#
#     delete_all_extprops(person_id)
#     delete_all_attributes(person_id)
#
#     attribute_batch_id = db.get_person_by_id(person_id)[0]['attribute_batch_id']
#
#     if person_type.code in UR_LIKE_PERSONS:
#         inn.set(attribute_batch_id)
#
#     if person_type.code in UR_LIKE_PERSONS or (person_type.code in YT_LIKE_PERSONS and person_type.code != 'yt_kzp'):
#         Attribute('NAME', value_str='Короткое имя организации').set(attribute_batch_id)
#     else:
#         Attribute('FNAME', value_str='Птичка').set(attribute_batch_id)
#
#     if partner:
#         Attribute('IS_PARTNER', value_num=1).set(attribute_batch_id)
#     export(firm.id, person_id, W_API)
#     check_attrs(person_id, ALL_OEBS_TABLES, firm_id=firm.id, person_type=person_type.code)

# Медведь закопан, тест отключен.
@pytest.mark.parametrize('person_type, firm, edo_offers, edo_offers_oebs, json_file', [

    (PersonTypes.UR, Firms.YANDEX_1,

     [{'firm_id': Firms.YANDEX_1, 'from_dt': NOW, 'offer_type': 1}],

     [{'firm_id': Firms.YANDEX_1, 'rows': [{'date_start': utils.Date.nullify_time_of_date(NOW),
                                            'date_end': None,
                                            'edo_operator': '2BE',
                                            'edo_type': 1}]
       }], 'ur_edo_offer_1'),

    (PersonTypes.UR, Firms.YANDEX_1,
     [{'firm_id': Firms.YANDEX_1, 'from_dt': NOW, 'offer_type': 2}],

     [{'firm_id': Firms.YANDEX_1, 'rows': [{'date_start': utils.Date.nullify_time_of_date(NOW),
                                            'date_end': None,
                                            'edo_operator': '2BE',
                                            'edo_type': 2}]
       }], 'ur_edo_offer_2'),

    (PersonTypes.UR, Firms.YANDEX_1,
     [{'firm_id': Firms.YANDEX_1, 'from_dt': NOW, 'offer_type': 3}],

     [{'firm_id': Firms.YANDEX_1, 'rows': [{'date_start': utils.Date.nullify_time_of_date(NOW),
                                            'date_end': None,
                                            'edo_operator': '2BE',
                                            'edo_type': 3}]
       }], 'ur_edo_offer_3'),

    (PersonTypes.UR, Firms.YANDEX_1,

     [{'firm_id': Firms.YANDEX_1, 'from_dt': NOW},
      {'firm_id': Firms.VERTICAL_12, 'from_dt': NOW}],

     [{'firm_id': Firms.YANDEX_1, 'rows': [{'date_start': utils.Date.nullify_time_of_date(NOW),
                                            'date_end': None,
                                            'edo_operator': '2BE'}
                                           ]},
      {'firm_id': Firms.VERTICAL_12, 'rows': [{'date_start': utils.Date.nullify_time_of_date(NOW),
                                               'date_end': None,
                                               'edo_operator': '2BE'}
                                              ]}
      ], 'ur_edo_offer_several_firms'),

    (PersonTypes.UR, Firms.YANDEX_1,

     [{'firm_id': Firms.YANDEX_1, 'from_dt': NOW},

      {'firm_id': Firms.YANDEX_1, 'from_dt': NOW + datetime.timedelta(hours=1)}],

     [{'firm_id': Firms.YANDEX_1, 'rows': [{'date_start': utils.Date.nullify_time_of_date(NOW),
                                            'date_end': None,
                                            'edo_operator': '2BE'}
                                           ]}
      ], 'ur_edo_offer_several_from_dt'),

    (PersonTypes.UR, Firms.YANDEX_1, [{'firm_id': Firms.YANDEX_1, 'from_dt': NOW},
                                      {'firm_id': Firms.YANDEX_1, 'from_dt': NOW - datetime.timedelta(days=2)}],
     [{'firm_id': Firms.YANDEX_1,
       'rows': [{'date_start': utils.Date.nullify_time_of_date(NOW - datetime.timedelta(days=1)),
                 'date_end': utils.Date.nullify_time_of_date(NOW - datetime.timedelta(days=1)),
                 'edo_operator': '2BE'},

                {'date_start': utils.Date.nullify_time_of_date(NOW),
                 'date_end': None,
                 'edo_operator': '2BE'}
                ]}
      ], 'ur_edo_offer_several_rows'),

    (PersonTypes.UR, Firms.YANDEX_1,

     [{'firm_id': Firms.YANDEX_1, 'from_dt': NOW},
      {'firm_id': Firms.YANDEX_1, 'from_dt': NOW + datetime.timedelta(hours=1),
       'edo_type': None}],

     [{'firm_id': Firms.YANDEX_1,
       'rows': [{'date_start': utils.Date.nullify_time_of_date(NOW),
                 'date_end': None,
                 'edo_operator': None}
                ]}
      ], 'ur_edo_offer_operator_none')
])
def get_export_person_edo_offer(person_type, firm, edo_offers, edo_offers_oebs, json_file):
    config_dict = json.loads(db.balance().execute("""SELECT *
                                FROM t_Config
                                WHERE ITEM = 'CLASSNAMES_EXPORTED_WITH_OEBS_API'""")[0]['value_json'])
    person_conf = config_dict.get('Person')
    if person_conf == 1:
        W_API = True
    else:
        W_API = False
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, person_type.code,
                                         inn_type=person_defaults.InnType.UNIQUE)
    steps.PersonSteps.clean_up_edo(person_id)
    for offer in edo_offers:
        steps.PersonSteps.accept_edo(firm_id=offer['firm_id'].id,
                                     person_id=person_id,
                                     from_dt=offer['from_dt'],
                                     edo_type_id=offer.get('edo_type', 1),
                                     offer_type=offer.get('offer_type', 1))

    if balance_config.JSON_CONTRACT_OEBS and json_file:
        check_json_contract(person_id, firm, json_file, partner=False)

    else:
        export(firm.id, person_id, W_API)

        check_attrs(person_id, [ATTRS.XXAR_EDO_OPER_HIST_TBL], firm_id=Firms.YANDEX_1.id, person_type=person_type.code)
        check_attrs(person_id, [ATTRS.XXAR_EDO_OPER_HIST_TBL], firm_id=Firms.VERTICAL_12.id, person_type=person_type.code)