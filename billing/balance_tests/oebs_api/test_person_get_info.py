# -*- coding: utf-8 -*-

import datetime
import pytest
from balance import constants as cst
from balance.processors.oebs_api.wrappers.person import PersonWrapper
from tests.balance_tests.oebs_api.conftest import create_person_category, create_firm
from tests import object_builder as ob


def test_entity_type(session, firm):
    person = ob.PersonBuilder.construct(session)
    info = PersonWrapper(person).get_info([firm])
    assert info['entity_type'] == 'CUSTOMER'


@pytest.mark.parametrize('ur', [0, 1])
def test_customer_type(session, firm, ur):
    person_category = create_person_category(session, ur=ur)
    person = ob.PersonBuilder.construct(session, type=person_category.category)
    info = PersonWrapper(person).get_info([firm])
    if ur:
        assert info['customer_type'] == 'ORGANIZATION'
    else:
        assert info['customer_type'] == 'PERSON'


def test_customer_subtype_ph(session, firm):
    person = ob.PersonBuilder.construct(session, type='ph')
    info = PersonWrapper(person).get_info([firm])
    assert info['customer_subtype'] == 'PERSON'


@pytest.mark.parametrize('inn_length, ownership_type', [(12, 'SELFEMPLOYED'),
                                                        (10, None),
                                                        (12, None)])
def test_customer_subtype_ur(session, firm, inn_length, ownership_type):
    person = ob.PersonBuilder.construct(session,
                                          type='ur',
                                          ownership_type=ownership_type,
                                          inn=str(ob.generate_int(inn_length)))
    info = PersonWrapper(person).get_info([firm])
    if ownership_type:
        assert info['customer_subtype'] == 'SELFEMPLOYED'
    elif inn_length == 12:
        assert info['customer_subtype'] == 'INDIVIDUAL'
    else:
        assert info['customer_subtype'] == 'ORGANIZATION'


def test_customer_subtype(session, firm, person_category):
    person = ob.PersonBuilder.construct(session, type=person_category.category)
    info = PersonWrapper(person).get_info([firm])
    assert 'customer_subtype' not in info


def test_entity_id(session, firm):
    person = ob.PersonBuilder.construct(session)
    info = PersonWrapper(person).get_info([firm])
    assert info['entity_id'] == 'P{}'.format(person.id)


def test_oe_code_oe_code(session):
    firms = [create_firm(session, firm_id=firm_id) for firm_id in [1, 4]]
    person = ob.PersonBuilder.construct(session)
    info = PersonWrapper(person).get_info(firms)
    assert info['oe_code'] == [{'oe_code': 'YARU', 'resident_type': 'REZ'},
                               {'oe_code': 'YAUS', 'resident_type': 'REZ'}]


@pytest.mark.parametrize('resident', [1, 0])
def test_oe_code_resident_type(session, resident):
    firm = create_firm(session, firm_id=1)
    person_category = create_person_category(session, resident=resident)
    person = ob.PersonBuilder.construct(session, type=person_category.category)
    info = PersonWrapper(person).get_info([firm])
    assert info['oe_code'] == [{'oe_code': 'YARU', 'resident_type': 'REZ' if resident else 'NEREZ'}]


@pytest.mark.parametrize('vat_payer', [1, 0, None])
def test_oe_code_vat_payer(session, vat_payer):
    firm = create_firm(session, firm_id=1)
    person = ob.PersonBuilder.construct(session, vat_payer=vat_payer)
    info = PersonWrapper(person).get_info([firm])
    if vat_payer == 1:
        assert info['oe_code'] == [{'resident_type': 'REZ', 'oe_code': 'YARU', 'vat_payer': 'Y'}]
    elif vat_payer == 0:
        assert info['oe_code'] == [{'resident_type': 'REZ', 'oe_code': 'YARU', 'vat_payer': 'N'}]
    else:
        assert info['oe_code'] == [{'oe_code': 'YARU', 'resident_type': 'REZ'}]


@pytest.mark.parametrize('person_type', ['ph', 'ur', 'yt'])
def test_inn(session, firm, person_type):
    person = ob.PersonBuilder.construct(session, type=person_type, inn=str(ob.generate_int(10)))
    info = PersonWrapper(person).get_info([firm])
    if person_type == 'yt':
        assert 'inn' not in info
    else:
        assert info['inn'] == person.inn


@pytest.mark.parametrize('person_type', ['kzu', 'yt_kzu', 'yt_kzp', 'kzp'])
def test_inn_kz(session, firm, person_type):
    person = ob.PersonBuilder.construct(session,
                                          type=person_type,
                                          inn=str(ob.generate_int(10)),
                                          kz_in=str(ob.generate_int(10)))
    info = PersonWrapper(person).get_info([firm])
    assert info['inn'] == person.kz_in


@pytest.mark.parametrize('person_type', ['ur', 'ph', 'trp'])
def test_inn_doc_details(session, firm, person_type):
    person = ob.PersonBuilder.construct(session,
                                          type=person_type,
                                          inn_doc_details=str(ob.generate_int(10)))
    info = PersonWrapper(person).get_info([firm])
    if person_type == 'trp':
        assert 'inn_doc_details' not in info
    else:
        assert info['inn_doc_details'] == person.inn_doc_details


@pytest.mark.parametrize('local_name', [None, 'local_cat'])
@pytest.mark.parametrize('name', [None, u'Котик'])
def test_customer_name(session, firm, local_name, name, person_category):
    person = ob.PersonBuilder.construct(session,
                                          type=person_category.category,
                                          name=name,
                                          local_name=local_name)
    info = PersonWrapper(person).get_info([firm])
    if name:
        if local_name:
            assert info['customer_name'] == [{'customer_name': u'Котик',
                                              'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                              'translations': [{'customer_name': 'local_cat',
                                                                'lang': 'LOCAL'}]}]
        else:
            assert info['customer_name'] == [{'customer_name': u'Котик',
                                              'date_from': datetime.datetime(1970, 1, 1, 0, 0)}]
    else:
        if local_name:
            assert info['customer_name'] == [{'customer_name': u'',
                                              'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                              'translations': [{'customer_name': 'local_cat',
                                                                'lang': 'LOCAL'}]}]
        else:
            assert 'customer_name' not in info


@pytest.mark.parametrize('local_name', [None, 'local_cat'])
@pytest.mark.parametrize('name', [None, u'Котик'])
@pytest.mark.parametrize('local_longname', [None, 'local_full_cat'])
@pytest.mark.parametrize('longname', [None, u'Полный котик'])
def test_full_name_endbuyer_ur(session, firm, local_name, name, local_longname, longname):
    person = ob.PersonBuilder.construct(session,
                                          type='endbuyer_ur',
                                          name=name,
                                          local_name=local_name,
                                          longname=longname,
                                          local_longname=local_longname)
    info = PersonWrapper(person).get_info([firm])
    if name:
        if local_name:
            assert info['full_name'] == [{'full_name': u'Котик',
                                          'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                          'translations': [{'full_name': 'local_cat',
                                                            'lang': 'LOCAL'}]}]
        else:
            assert info['full_name'] == [{'full_name': u'Котик',
                                          'date_from': datetime.datetime(1970, 1, 1, 0, 0)}]
    else:
        if local_name:
            assert info['full_name'] == [{'full_name': u'',
                                          'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                          'translations': [{'full_name': 'local_cat',
                                                            'lang': 'LOCAL'}]}]
        else:
            assert 'full_name' not in info


@pytest.mark.parametrize('local_name', [None, 'local_cat'])
@pytest.mark.parametrize('name', [None, u'Котик'])
@pytest.mark.parametrize('local_longname', [None, 'local_full_cat'])
@pytest.mark.parametrize('longname', [None, u'Полный котик'])
def test_full_name_ur(session, firm, local_name, name, local_longname, longname):
    person = ob.PersonBuilder.construct(session,
                                          type='ur',
                                          name=name,
                                          local_name=local_name,
                                          longname=longname,
                                          local_longname=local_longname)
    info = PersonWrapper(person).get_info([firm])
    if longname:
        if local_longname:
            assert info['full_name'] == [{'full_name': u'Полный котик',
                                          'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                          'translations': [{'full_name': 'local_full_cat',
                                                            'lang': 'LOCAL'}]}]
        else:
            assert info['full_name'] == [{'full_name': u'Полный котик',
                                          'date_from': datetime.datetime(1970, 1, 1, 0, 0)}]
    else:
        if local_longname:
            assert info['full_name'] == [{'full_name': u'',
                                          'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                          'translations': [{'full_name': 'local_full_cat',
                                                            'lang': 'LOCAL'}]}]
        else:
            assert 'full_name' not in info


def test_full_name_ph(session, firm):
    person = ob.PersonBuilder.construct(session,
                                          type='ph',
                                          local_name='local_cat',
                                          local_longname='local_full_cat',
                                          name=u'Котик',
                                          longname=u'Полный котик')
    info = PersonWrapper(person).get_info([firm])
    assert 'full_name' not in info


@pytest.mark.parametrize('local_name', [None, 'local_cat'])
@pytest.mark.parametrize('name', [None, u'Котик'])
@pytest.mark.parametrize('local_longname', [None, 'local_full_cat'])
@pytest.mark.parametrize('longname', [None, u'Полный котик'])
def test_full_name_non_res(session, firm, local_name, name, local_longname, longname):
    person = ob.PersonBuilder.construct(session,
                                          type='yt',
                                          name=name,
                                          local_name=local_name,
                                          longname=longname,
                                          local_longname=local_longname)
    info = PersonWrapper(person).get_info([firm])
    if longname:
        if local_longname:
            assert info['full_name'] == [{'full_name': u'Полный котик',
                                          'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                          'translations': [{'full_name': 'local_full_cat',
                                                            'lang': 'LOCAL'}]}]
        elif local_name:
            assert info['full_name'] == [{'full_name': u'Полный котик',
                                          'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                          'translations': [{'full_name': 'local_cat',
                                                            'lang': 'LOCAL'}]}]
        else:
            assert info['full_name'] == [{'full_name': u'Полный котик',
                                          'date_from': datetime.datetime(1970, 1, 1, 0, 0)}]
    else:
        if name:
            if local_longname:
                assert info['full_name'] == [{'full_name': u'Котик',
                                              'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                              'translations': [{'full_name': 'local_full_cat',
                                                                'lang': 'LOCAL'}]}]
            elif local_name:
                assert info['full_name'] == [{'full_name': u'Котик',
                                              'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                              'translations': [{'full_name': 'local_cat',
                                                                'lang': 'LOCAL'}]}]
            else:
                assert info['full_name'] == [{'full_name': u'Котик',
                                              'date_from': datetime.datetime(1970, 1, 1, 0, 0)}]
        else:
            if local_longname:
                assert info['full_name'] == [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                              'translations': [{'lang': 'LOCAL',
                                                                'full_name': 'local_full_cat'}],
                                              'full_name': ''}]
            elif local_name:
                assert info['full_name'] == [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                              'translations': [{'lang': 'LOCAL',
                                                                'full_name': 'local_cat'}],
                                              'full_name': ''}]
            else:
                assert 'full_name' not in info


@pytest.mark.parametrize('birthday', [None, datetime.datetime.now()])
def test_date_of_birth(session, firm, birthday, person_category):
    person = ob.PersonBuilder.construct(session,
                                          type=person_category.category,
                                          birthday=birthday)
    info = PersonWrapper(person).get_info([firm])
    if not birthday:
        assert 'date_of_birth' not in info
    else:
        assert info['date_of_birth'] == person.birthday


@pytest.mark.parametrize('passport_s', [None, '123'])
def test_passport_ser(session, firm, passport_s, person_category):
    person = ob.PersonBuilder.construct(session,
                                          type=person_category.category,
                                          passport_s=passport_s)
    info = PersonWrapper(person).get_info([firm])
    if not passport_s:
        assert 'passport_ser' not in info
    else:
        assert info['passport_ser'] == [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                         'passport_ser': person.passport_s}]


@pytest.mark.parametrize('passport_n', [None, '123'])
def test_passport_num(session, firm, passport_n, person_category):
    person = ob.PersonBuilder.construct(session,
                                          type=person_category.category,
                                          passport_n=passport_n)
    info = PersonWrapper(person).get_info([firm])
    if not passport_n:
        assert 'passport_num' not in info
    else:
        assert info['passport_num'] == [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                         'passport_num': person.passport_n}]


@pytest.mark.parametrize('passport_d', [None, '2011-10-11'])
def test_passport_when(session, firm, passport_d, person_category):
    person = ob.PersonBuilder.construct(session, type=person_category.category,
                                          passport_d=passport_d)
    info = PersonWrapper(person).get_info([firm])
    if not passport_d:
        assert 'passport_when' not in info
    else:
        assert info['passport_when'] == [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                          'passport_when': datetime.datetime(2011, 10, 11, 0, 0)}]


@pytest.mark.parametrize('passport_e', [None, u'Ахтубинским ОВД гор. Нижнекамска'])
def test_passport_whom(session, firm, passport_e, person_category):
    person = ob.PersonBuilder.construct(session,
                                          type=person_category.category,
                                          passport_e=passport_e)
    info = PersonWrapper(person).get_info([firm])
    if not passport_e:
        assert 'passport_whom' not in info
    else:
        assert info['passport_whom'] == [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                          'passport_whom': passport_e}]


@pytest.mark.parametrize('passport_code', [None, '123214'])
def test_passport_dept(session, firm, passport_code, person_category):
    person = ob.PersonBuilder.construct(session,
                                          type=person_category.category,
                                          passport_code=passport_code)
    info = PersonWrapper(person).get_info([firm])
    if not passport_code:
        assert 'passport_dept' not in info
    else:
        assert info['passport_dept'] == [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                          'passport_dept': passport_code}]


@pytest.mark.parametrize('pfr', [None, '123214'])
def test_pfr_number(session, firm, pfr, person_category):
    person = ob.PersonBuilder.construct(session,
                                          type=person_category.category,
                                          pfr=pfr)
    info = PersonWrapper(person).get_info([firm])
    if not pfr:
        assert 'pfr_number' not in info
    else:
        assert info['pfr_number'] == [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                       'pfr_number': pfr}]


@pytest.mark.parametrize('bank_type', [cst.BankType.YANDEX_MONEY,
                                       cst.BankType.WEBMONEY,
                                       cst.BankType.PAYPAL,
                                       cst.BankType.PAYONEER,
                                       cst.BankType.PINGPONG,
                                       666])
def test_wallet(session, firm, bank_type, person_category):
    person = ob.PersonBuilder.construct(session,
                                          type=person_category.category,
                                          bank_type=bank_type,
                                          yamoney_wallet=ob.generate_int(5),
                                          webmoney_wallet=ob.generate_int(6),
                                          paypal_wallet=ob.generate_int(4),
                                          payoneer_wallet=ob.generate_int(7),
                                          pingpong_wallet=ob.generate_int(8))
    info = PersonWrapper(person).get_info([firm])
    if bank_type == cst.BankType.YANDEX_MONEY:
        assert info['wallet'] == person.yamoney_wallet
    elif bank_type == cst.BankType.WEBMONEY:
        assert info['wallet'] == person.webmoney_wallet
    elif bank_type == cst.BankType.PAYPAL:
        assert info['wallet'] == person.paypal_wallet
    elif bank_type == cst.BankType.PAYONEER:
        assert info['wallet'] == person.payoneer_wallet
    elif bank_type == cst.BankType.PINGPONG:
        assert info['wallet'] == person.pingpong_wallet
    else:
        assert 'wallet' not in info


@pytest.mark.parametrize('fname', [None, u'Имя'])
def test_first_name(session, firm, fname, person_category):
    person = ob.PersonBuilder.construct(session,
                                          type=person_category.category,
                                          fname=fname)
    info = PersonWrapper(person).get_info([firm])
    if not fname:
        assert 'first_name' not in info
    else:
        assert info['first_name'] == [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                       'first_name': fname}]


@pytest.mark.parametrize('person_type', ['yt', 'ph', 'ur'])
@pytest.mark.parametrize('mname', [None, u'Отчество'])
def test_middle_name(session, firm, mname, person_type):
    person = ob.PersonBuilder.construct(session,
                                          type=person_type,
                                          mname=mname)
    info = PersonWrapper(person).get_info([firm])
    if person_type == 'yt':
        assert 'middle_name' not in info
    else:
        if not mname:
            assert 'middle_name' not in info
        else:
            assert info['middle_name'] == [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                            'middle_name': mname}]


@pytest.mark.parametrize('lname', [None, u'Фамилия'])
def test_last_name(session, firm, lname, person_category):
    person = ob.PersonBuilder.construct(session,
                                          type=person_category.category,
                                          lname=lname)
    info = PersonWrapper(person).get_info([firm])
    if not lname:
        assert 'last_name' not in info
    else:
        assert info['last_name'] == [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                      'last_name': lname}]


@pytest.mark.parametrize('account', [None, 124142342342])
def test_account(session, firm, account, person_category):
    person = ob.PersonBuilder.construct(session,
                                          type=person_category.category,
                                          account=account)
    info = PersonWrapper(person).get_info([firm])
    if not account:
        assert 'account' not in info
    else:
        assert info['account'] == [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                    'account': account}]


@pytest.mark.parametrize('person_type', ['yt', 'ph', 'ur'])
@pytest.mark.parametrize('bankcity', [None, u'Москва'])
def test_bank_address(session, firm, bankcity, person_type):
    person = ob.PersonBuilder.construct(session,
                                          type=person_type,
                                          bankcity=bankcity)
    info = PersonWrapper(person).get_info([firm])
    if person_type == 'ph':
        assert 'bank_address' not in info
    else:
        if not bankcity:
            assert 'bank_address' not in info
        else:
            assert info['bank_address'] == [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                             'bank_address': bankcity}]


@pytest.mark.parametrize('person_type', ['yt', 'ph', 'ur'])
@pytest.mark.parametrize('bank_type', [None, 2])
def test_bank_type(session, firm, bank_type, person_type):
    person = ob.PersonBuilder.construct(session,
                                          type=person_type,
                                          bank_type=bank_type)
    info = PersonWrapper(person).get_info([firm])
    if person_type == 'ur':
        assert 'bank_type' not in info
    else:
        if not bank_type:
            assert 'bank_type' not in info
        else:
            assert info['bank_type'] == [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                          'bank_type': bank_type}]


@pytest.mark.parametrize('ben_bank', [None, u'ВТБ'])
@pytest.mark.parametrize('local_ben_bank', [None, u'VTB'])
def test_bank_name(session, firm, ben_bank, local_ben_bank, person_category):
    person = ob.PersonBuilder.construct(session,
                                          type=person_category.category,
                                          bank=u'Альфа банк',
                                          local_bank=u'alpha bank',
                                          ben_bank=ben_bank,
                                          local_ben_bank=local_ben_bank)
    info = PersonWrapper(person).get_info([firm])
    if ben_bank:
        if local_ben_bank:
            assert info['bank_name'] == [{'bank_name': u'ВТБ',
                                          'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                          'translations': [{'lang': 'LOCAL',
                                                            'bank_name': u'VTB'}]}]
        else:
            assert info['bank_name'] == [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                          'bank_name': u'ВТБ'}]
    else:
        if local_ben_bank:
            assert info['bank_name'] == [{'bank_name': '',
                                          'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                          'translations': [{'lang': 'LOCAL',
                                                            'bank_name': u'VTB'}]}]
        else:
            assert 'bank_name' not in info


@pytest.mark.parametrize('bank', [None, u'Альфа банк'])
@pytest.mark.parametrize('local_bank', [None, u'alpha bank'])
def test_bank_name_kzu(session, firm, bank, local_bank):
    person = ob.PersonBuilder.construct(session,
                                          type='kzu',
                                          bank=bank,
                                          local_bank=local_bank,
                                          ben_bank=u'ВТБ',
                                          local_ben_bank=u'VTB')
    info = PersonWrapper(person).get_info([firm])
    if bank:
        if local_bank:
            assert info['bank_name'] == [{'bank_name': u'Альфа банк',
                                          'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                          'translations': [{'lang': 'LOCAL',
                                                            'bank_name': u'alpha bank'}]}]
        else:
            assert info['bank_name'] == [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                          'bank_name': u'Альфа банк'}]
    else:
        if local_bank:
            assert info['bank_name'] == [{'bank_name': '',
                                          'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                          'translations': [{'lang': 'LOCAL',
                                                            'bank_name': u'alpha bank'}]}]
        else:
            assert 'bank_name' not in info


@pytest.mark.parametrize('person_type', ['yt', 'ph', 'ur'])
@pytest.mark.parametrize('rnn', [None, 3242352])
def test_tax_reg_number(session, firm, rnn, person_type):
    person = ob.PersonBuilder.construct(session,
                                          type=person_type,
                                          rnn=rnn)
    info = PersonWrapper(person).get_info([firm])
    if rnn and person_type == 'yt':
        assert info['tax_reg_number'] == rnn
    else:

        assert 'tax_reg_number' not in info


@pytest.mark.parametrize('ben_account', [None, 3242352])
def test_account_name(session, firm, ben_account, person_category):
    person = ob.PersonBuilder.construct(session,
                                          type=person_category.category,
                                          ben_account=ben_account)
    info = PersonWrapper(person).get_info([firm])
    if ben_account:
        assert info['account_name'] == ben_account
    else:

        assert 'account_name' not in info


@pytest.mark.parametrize('corr_swift', [None, 3242352])
def test_correspondent_swift(session, firm, corr_swift, person_category):
    person = ob.PersonBuilder.construct(session,
                                          type=person_category.category,
                                          corr_swift=corr_swift)
    info = PersonWrapper(person).get_info([firm])
    if corr_swift:
        assert info['correspondent_swift'] == [{'correspondent_swift': corr_swift,
                                                'date_from': datetime.datetime(1970, 1, 1, 0, 0)}]
    else:

        assert 'correspondent_swift' not in info


@pytest.mark.parametrize('person_type', ['yt', 'ph', 'ur', 'kzu'])
@pytest.mark.parametrize('iik', [None, 45])
@pytest.mark.parametrize('iban', [None, 3242352])
def test_iban_number(session, firm, iban, iik, person_type):
    person = ob.PersonBuilder.construct(session,
                                          type=person_type,
                                          iban=iban,
                                          iik=iik)
    info = PersonWrapper(person).get_info([firm])
    if person_type == 'kzu':
        if iik:
            assert info['iban_number'] == iik
        else:
            assert 'iban_number' not in info
    else:
        if iban:
            assert info['iban_number'] == iban
        else:
            assert 'iban_number' not in info


@pytest.mark.parametrize('person_type', ['ur'])
@pytest.mark.parametrize('ogrn', ['6666777766666', None])
def test_ogrn(session, firm, ogrn, person_type):
    person = ob.PersonBuilder.construct(session,
                                          type=person_type,
                                          ogrn=ogrn)
    info = PersonWrapper(person).get_info([firm])
    if ogrn:
        assert info['ogrn'] == ogrn
    else:
        assert ogrn not in info


@pytest.mark.parametrize('person_type', ['yt', 'ph', 'ur', 'kzu'])
@pytest.mark.parametrize('bik', [None, 445])
@pytest.mark.parametrize('swift', [None, u'ЦERFER542'])
def test_swift_code(session, firm, swift, bik, person_type):
    person = ob.PersonBuilder.construct(session,
                                          type=person_type,
                                          swift=swift,
                                          bik=bik)
    info = PersonWrapper(person).get_info([firm])
    if person_type == 'kzu':
        if bik:
            assert info['swift_code'] == bik
        else:
            assert 'swift_code' not in info
    else:
        if swift:
            assert info['swift_code'] == swift
        else:
            assert 'swift_code' not in info


@pytest.mark.parametrize('person_type', ['yt', 'ph', 'ur'])
@pytest.mark.parametrize('bik', [None, 243])
def test_bik(session, firm, bik, person_type):
    person = ob.PersonBuilder.construct(session,
                                          type=person_type,
                                          bik=bik)
    info = PersonWrapper(person).get_info([firm])
    if person_type == 'yt':
        assert 'bik' not in info
    else:
        if not bik:
            assert 'bik' not in info
        else:
            assert info['bik'] == [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                    'bik': str(bik)}]


@pytest.mark.parametrize('person_type', ['yt', 'ph', 'ur', 'kzu'])
@pytest.mark.parametrize('kpp', [None, 445])
def test_kpp(session, firm, kpp, person_type):
    person = ob.PersonBuilder.construct(session,
                                          type=person_type,
                                          kpp=kpp)
    info = PersonWrapper(person).get_info([firm])
    if person_type in ('ph', 'yt', 'kzu'):
        assert 'kpp' not in info
    else:
        if kpp:
            assert info['kpp'] == kpp
        else:
            assert 'kpp' not in info


@pytest.mark.parametrize('person_type', ['yt', 'ph', 'ur', 'kzu'])
@pytest.mark.parametrize('kbk', [None, 445])
def test_kbk_code(session, firm, kbk, person_type):
    person = ob.PersonBuilder.construct(session,
                                          type=person_type,
                                          kbk=kbk)
    info = PersonWrapper(person).get_info([firm])
    if person_type in ('ph', 'yt', 'kzu'):
        assert 'kbk_code' not in info
    else:
        if kbk:
            assert info['kbk_code'] == kbk
        else:
            assert 'kbk_code' not in info


@pytest.mark.parametrize('invalid_bankprops', [1, 0, None])
def test_invalid_bankprops(session, firm, invalid_bankprops):
    person = ob.PersonBuilder.construct(session,
                                          type=create_person_category(session).category,
                                          invalid_bankprops=invalid_bankprops)
    info = PersonWrapper(person).get_info([firm])
    if invalid_bankprops == 1:
        assert info['invalid_bankprops'] == 'Y'
    elif invalid_bankprops == 0:
        assert info['invalid_bankprops'] == 'N'
    else:
        assert 'invalid_bankprops' not in info


@pytest.mark.parametrize('person_type', ['yt', 'ph', 'ur'])
@pytest.mark.parametrize('person_account', [None, 445])
def test_person_account(session, firm, person_account, person_type):
    person = ob.PersonBuilder.construct(session,
                                          type=person_type,
                                          person_account=person_account)
    info = PersonWrapper(person).get_info([firm])
    if person_type in ('ur', 'yt'):
        assert 'person_account' not in info
    else:
        if person_account:
            assert info['person_account'] == person_account
        else:
            assert 'person_account' not in info


@pytest.mark.parametrize('payment_purpose', [None, 3242352])
def test_payment_purpose(session, firm, payment_purpose):
    person = ob.PersonBuilder.construct(session,
                                          type=create_person_category(session).category,
                                          payment_purpose=payment_purpose)
    info = PersonWrapper(person).get_info([firm])
    if payment_purpose:
        assert info['payment_purpose'] == payment_purpose
    else:

        assert 'payment_purpose' not in info


@pytest.mark.parametrize('kbe', [None, 3242352])
def test_beneficiar_code(session, firm, kbe):
    person = ob.PersonBuilder.construct(session,
                                          type=create_person_category(session).category,
                                          kbe=kbe)
    info = PersonWrapper(person).get_info([firm])
    if kbe:
        assert info['beneficiar_code'] == str(kbe)
    else:
        assert 'beneficiar_code' not in info


@pytest.mark.parametrize('other', [None, u'Всякое'])
@pytest.mark.parametrize('local_other', [None, u'local всякое'])
def test_additional_payment_details(session, firm, other, local_other):
    person = ob.PersonBuilder.construct(session,
                                          type=create_person_category(session).category,
                                          other=other,
                                          local_other=local_other)
    info = PersonWrapper(person).get_info([firm])
    if other:
        if local_other:
            assert info['additional_payment_details'] == [{'additional_payment_details': u'Всякое',
                                                           'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                           'translations': [{'lang': 'LOCAL',
                                                                             'additional_payment_details': u'local всякое'}]}]
        else:
            assert info['additional_payment_details'] == [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                           'additional_payment_details': u'Всякое'}]
    else:
        if local_other:
            assert info['additional_payment_details'] == [{'additional_payment_details': '',
                                                           'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                           'translations': [{'lang': 'LOCAL',
                                                                             'additional_payment_details': u'local всякое'}]}]
        else:
            assert 'additional_payment_details' not in info


@pytest.mark.parametrize('person_type', ['yt', 'ph', 'ur'])
@pytest.mark.parametrize('oktmo', [None, 445])
def test_oktmo_code(session, firm, oktmo, person_type):
    person = ob.PersonBuilder.construct(session,
                                          type=person_type,
                                          oktmo=oktmo)
    info = PersonWrapper(person).get_info([firm])
    if person_type in ('ph', 'yt'):
        assert 'oktmo_code' not in info
    else:
        if oktmo:
            assert info['oktmo_code'] == oktmo
        else:
            assert 'oktmo_code' not in info


@pytest.mark.parametrize('person_type', ['yt', 'endbuyer_yt'])
@pytest.mark.parametrize('address', [None, u'д. Столбино'])
def test_address_post_address(session, firm, person_type, address, patch_get_application):
    person = ob.PersonBuilder.construct(session,
                                          type=person_type,
                                          address=address)

    with patch_get_application as app:
        app.return_value.geolookup.parents.return_value = [1]
        app.return_value.geolookup.regionById.return_value.type = 3
        app.return_value.geolookup.regionById.return_value.short_ename = 'FU'

        info = PersonWrapper(person).get_info([firm])

    if address:
        assert info['address'] == [{'state': [{'state': 'FU',
                                               'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                    'address': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                 'address': u'д. Столбино'}],
                                    'address_type': 'POST'}]

    else:
        assert info['address'] == []


@pytest.mark.parametrize('fias_guid', [None, u'13a5bcdb-9187-4d0f-b027-45f8ec29591a'])
@pytest.mark.parametrize('postaddress', [None, u'Какой-то другой город'])
@pytest.mark.parametrize('local_postaddress', [None, u'derevnya'])
def test_address_post_address(session, firm, fias_guid, patch_get_application, person_category, postaddress,
                              local_postaddress):
    person = ob.PersonBuilder.construct(session,
                                          type=person_category.category,
                                          fias_guid=fias_guid,
                                          postaddress=postaddress,
                                          local_postaddress=local_postaddress)

    with patch_get_application:
        info = PersonWrapper(person).get_info([firm])

    if fias_guid:
        if local_postaddress:
            assert info['address'] == [{'state': [{'state': person.person_category.oebs_country_code,
                                                   'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                        'address': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                     'translations': [{'lang': 'LOCAL', 'address': u'derevnya'}],
                                                     'address': u'Коми Респ, Усть-Вымский р-н, пгт Жешарт'}],
                                        'fias_guid': [{'fias_guid': u'13a5bcdb-9187-4d0f-b027-45f8ec29591a',
                                                       'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                        'validity_flag': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                           'validity_flag': 'Y'}],
                                        'address_type': 'POST'}]
        else:
            assert info['address'] == [{'state': [{'state': person.person_category.oebs_country_code,
                                                   'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                        'address': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                     'address': u'Коми Респ, Усть-Вымский р-н, пгт Жешарт'}],
                                        'fias_guid': [{'fias_guid': u'13a5bcdb-9187-4d0f-b027-45f8ec29591a',
                                                       'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                        'validity_flag': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                           'validity_flag': 'Y'}],
                                        'address_type': 'POST'}]

    else:
        if postaddress:
            if local_postaddress:
                assert info['address'] == [{'state': [{'state': person.person_category.oebs_country_code,
                                                       'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                            'address': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                         'translations': [{'lang': 'LOCAL',
                                                                           'address': u'derevnya'}],
                                                         'address': u'Какой-то другой город'}],
                                            'validity_flag': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                               'validity_flag': 'Y'}],
                                            'address_type': 'POST'}]
            else:
                assert info['address'] == [{'state': [{'state': person.person_category.oebs_country_code,
                                                       'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                            'address': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                         'address': u'Какой-то другой город'}],
                                            'validity_flag': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                               'validity_flag': 'Y'}],
                                            'address_type': 'POST'}]
        else:
            if local_postaddress:
                assert info['address'] == [{'state': [{'state': person.person_category.oebs_country_code,
                                                       'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                            'validity_flag': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                               'validity_flag': 'Y'}],
                                            'address': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                         'translations': [{'lang': 'LOCAL',
                                                                           'address': u'derevnya'}],
                                                         'address': ''}],
                                            'address_type': 'POST'}]
            else:
                assert 'address' not in info


@pytest.mark.parametrize('person_type', ['yt', 'ur', 'usu'])
@pytest.mark.parametrize('local_city', [None, u'Локальное имя города'])
def test_address_post_address_city(session, firm, patch_get_application, person_type, local_city):
    person = ob.PersonBuilder.construct(session,
                                          type=person_type,
                                          local_city=local_city,
                                          city=u'Локальный город')

    with patch_get_application:
        info = PersonWrapper(person).get_info([firm])

    if local_city:
        if person_type == 'usu':
            assert info['address'] == [
                {'city': [{'city': u'Локальный город', 'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                           'translations': [{'lang': 'LOCAL',
                                             'city': u'Локальное имя города'}]}],
                 'state': [
                     {'state': person.person_category.oebs_country_code,
                      'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                 'validity_flag': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                    'validity_flag': 'Y'}],
                 'address_type': 'POST'}]
        else:

            assert info['address'] == [{'city': [{'city': '', 'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                  'translations': [{'lang': 'LOCAL',
                                                                    'city': u'Локальное имя города'}]}],
                                        'state': [{
                                            'state': person.person_category.oebs_country_code if person.person_category.resident else 'NU',
                                            'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                        'validity_flag': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                           'validity_flag': 'Y'}],
                                        'address_type': 'POST'}]
    else:
        if person_type == 'usu':
            assert info['address'] == [{'address_type': 'POST',
                                        'city': [{'city': u'Локальный город',
                                                  'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                        'validity_flag': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                           'validity_flag': 'Y'}],
                                        'state': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                   'state': person.person_category.oebs_country_code}]}]
        else:
            assert 'address' not in info


@pytest.mark.parametrize('is_partner', [1, 0])
@pytest.mark.parametrize('city', [None, u'Имя города'])
@pytest.mark.parametrize('local_city', [None, u'Локальное имя города'])
def test_address_post_address_city_ph(session, firm, patch_get_application, is_partner, city, local_city):
    person = ob.PersonBuilder.construct(session,
                                          type='ph',
                                          is_partner=is_partner,
                                          city=city,
                                          local_city=local_city)

    with patch_get_application:
        info = PersonWrapper(person).get_info([firm])

    if is_partner:
        if local_city:
            assert info['address'] == [
                {'city': [{'city': u'', 'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                           'translations': [{'lang': 'LOCAL',
                                             'city': u'Локальное имя города'}]}],
                 'validity_flag': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                    'validity_flag': 'Y'}],
                 'state': [{'state': person.person_category.oebs_country_code,
                            'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                 'address_type': 'POST'}]
        else:

            assert 'address' not in info
    else:
        if local_city:
            if city:
                assert info['address'] == [{'address_type': 'POST',
                                            'city': [{'city': u'Имя города',
                                                      'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                      'translations': [{'city': u'Локальное имя города',
                                                                        'lang': 'LOCAL'}]}],
                                            'validity_flag': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                               'validity_flag': 'Y'}],
                                            'state': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                       'state': 'RU'}]}]
            else:
                assert info['address'] == [{'address_type': 'POST',
                                            'city': [{'city': '',
                                                      'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                      'translations': [{'city': u'Локальное имя города',
                                                                        'lang': 'LOCAL'}]}],
                                            'validity_flag': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                               'validity_flag': 'Y'}],
                                            'state': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                       'state': 'RU'}]}]
        else:
            if city:
                assert info['address'] == [{'address_type': 'POST',
                                            'city': [{'city': u'Имя города',
                                                      'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                            'validity_flag': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                               'validity_flag': 'Y'}],
                                            'state': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                       'state': 'RU'}]}]
            else:
                assert 'address' not in info


@pytest.mark.parametrize('person_type', ['yt', 'ur', 'ph'])
@pytest.mark.parametrize('postcode', [None, 123455])
def test_address_post_postal_code(session, firm, person_type, postcode, patch_get_application):
    person = ob.PersonBuilder.construct(session,
                                          type=person_type,
                                          postcode=postcode)

    with patch_get_application:
        info = PersonWrapper(person).get_info([firm])
    state = person.person_category.oebs_country_code if person.person_category.resident else 'NU'
    if postcode:
        if person_type == 'yt':
            assert 'address' not in info
        else:
            assert info['address'] == [{'state': [{'state': state,
                                                   'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                        'postal_code': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                         'postal_code': 123455}],
                                        'validity_flag': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                           'validity_flag': 'Y'}],
                                        'address_type': 'POST'}]
    else:
        assert 'address' not in info


def test_address_post_address_suffix_yt(session, firm, patch_get_application):
    person = ob.PersonBuilder.construct(session,
                                          type='yt',
                                          street=u'ул. Прямая',
                                          postsuffix=u'д. 12',
                                          fias_guid=None)

    with patch_get_application:
        info = PersonWrapper(person).get_info([firm])
    assert 'address' not in info


@pytest.mark.parametrize('fias_guid', [None, u'13a5bcdb-9187-4d0f-b027-45f8ec29591a'])
@pytest.mark.parametrize('postsuffix', [None, u'д. 12'])
def test_address_post_address_suffix(session, firm, person_category, postsuffix, patch_get_application,
                                     fias_guid):
    person = ob.PersonBuilder.construct(session,
                                          type=person_category.category,
                                          street=u'ул. Прямая',
                                          postsuffix=postsuffix,
                                          fias_guid=fias_guid)

    with patch_get_application:
        info = PersonWrapper(person).get_info([firm])
    if fias_guid:
        if postsuffix:
            assert info['address'] == [{'address_suffix': [{'address_suffix': u'д. 12',
                                                            'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                        'state': [{'state': person.person_category.oebs_country_code,
                                                   'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                        'validity_flag': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                           'validity_flag': 'Y'}],
                                        'address': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                     'address': u'Коми Респ, Усть-Вымский р-н, пгт Жешарт, ул. Прямая'}],
                                        'fias_guid': [{'fias_guid': u'13a5bcdb-9187-4d0f-b027-45f8ec29591a',
                                                       'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                        'address_type': 'POST'}]
        else:
            assert info['address'] == [{'state': [{'state': person.person_category.oebs_country_code,
                                                   'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                        'validity_flag': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                           'validity_flag': 'Y'}],
                                        'address': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                     'address': u'Коми Респ, Усть-Вымский р-н, пгт Жешарт, ул. Прямая'}],
                                        'fias_guid': [{'fias_guid': u'13a5bcdb-9187-4d0f-b027-45f8ec29591a',
                                                       'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                        'address_type': 'POST'}]

    else:
        if postsuffix:
            assert info['address'] == [{'state': [{'state': person.person_category.oebs_country_code,
                                                   'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                        'validity_flag': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                           'validity_flag': 'Y'}],
                                        'address': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                     'address': u'д. 12'}],
                                        'address_type': 'POST'}]
        else:
            assert 'address' not in info


@pytest.mark.parametrize('kladr_code', [None, 0002323])
@pytest.mark.parametrize('fias_guid', [None, u'13a5bcdb-9187-4d0f-b027-45f8ec29591a'])
def test_address_post_kladr_code(session, firm, kladr_code, patch_get_application, fias_guid, person_category):
    person = ob.PersonBuilder.construct(session,
                                          type=person_category.category,
                                          kladr_code=kladr_code,
                                          fias_guid=fias_guid)

    with patch_get_application:
        info = PersonWrapper(person).get_info([firm])
    if fias_guid:
        assert info['address'] == [{'state': [{'state': person.person_category.oebs_country_code,
                                               'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                    'address': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                 'address': u'Коми Респ, Усть-Вымский р-н, пгт Жешарт'}],
                                    'fias_guid': [{'fias_guid': u'13a5bcdb-9187-4d0f-b027-45f8ec29591a',
                                                   'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                    'validity_flag': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                       'validity_flag': 'Y'}],
                                    'address_type': 'POST'}]
    else:
        if kladr_code:

            assert info['address'] == [{'kladr_code': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                        'kladr_code': 1235}],
                                        'state': [{'state': person.person_category.oebs_country_code,
                                                   'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                        'validity_flag': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                           'validity_flag': 'Y'}],
                                        'address_type': 'POST'}]
        else:
            assert 'address' not in info


@pytest.mark.parametrize('fias_guid', [None, u'13a5bcdb-9187-4d0f-b027-45f8ec29591a'])
def test_address_post_fias_guid(session, firm, patch_get_application, fias_guid, person_category):
    person = ob.PersonBuilder.construct(session,
                                          type=person_category.category,
                                          fias_guid=fias_guid)

    with patch_get_application:
        info = PersonWrapper(person).get_info([firm])
    if fias_guid:
        assert info['address'] == [{'state': [{'state': person.person_category.oebs_country_code,
                                               'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                    'address': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                 'address': u'Коми Респ, Усть-Вымский р-н, пгт Жешарт'}],
                                    'fias_guid': [{'fias_guid': u'13a5bcdb-9187-4d0f-b027-45f8ec29591a',
                                                   'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                    'validity_flag': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                       'validity_flag': 'Y'}],
                                    'address_type': 'POST'}]
    else:
        assert 'address' not in info


@pytest.mark.parametrize('is_partner', [1, 0])
@pytest.mark.parametrize('legaladdress', [None, u'юридический адрес строкой'])
@pytest.mark.parametrize('local_legaladdress', [None, u'local_legaladdress'])
def test_address_legal_address_ph(session, firm, patch_get_application, is_partner, legaladdress,
                                  local_legaladdress):
    person = ob.PersonBuilder.construct(session,
                                          type='ph',
                                          is_partner=is_partner,
                                          legaladdress=legaladdress,
                                          local_legaladdress=local_legaladdress)

    with patch_get_application:
        info = PersonWrapper(person).get_info([firm])

    if not is_partner:
        assert 'address' not in info
    else:
        if legaladdress:
            if local_legaladdress:
                assert info['address'] == [{'address_type': 'LEGAL',
                                            'address': [{'address': u'юридический адрес строкой',
                                                         'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                         'translations': [{'address': u'local_legaladdress',
                                                                           'lang': 'LOCAL'}]}],
                                            'validity_flag': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                               'validity_flag': 'Y'}],
                                            'state': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                       'state': 'RU'}]}]
            else:
                assert info['address'] == [{'address_type': 'LEGAL',
                                            'address': [{'address': u'юридический адрес строкой',
                                                         'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                            'validity_flag': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                               'validity_flag': 'Y'}],
                                            'state': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                       'state': 'RU'}]}]
        else:
            if local_legaladdress:
                assert info['address'] == [{'address': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                         'translations': [{'lang': 'LOCAL',
                                                                           'address': u'local_legaladdress'}],
                                                         'address': ''}],
                                            'validity_flag': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                               'validity_flag': 'Y'}],
                                            'address_type': 'LEGAL',
                                            'state': [{'state': u'RU',
                                                       'date_from': datetime.datetime(1970, 1, 1, 0, 0)}]}]
            else:
                assert 'address' not in info


@pytest.mark.parametrize('legal_fias_guid', [None, u'13a5bcdb-9187-4d0f-b027-45f8ec29591a'])
@pytest.mark.parametrize('legaladdress', [None, u'юридический адрес строкой'])
@pytest.mark.parametrize('local_legaladdress', [None, u'local_legaladdress'])
def test_address_legal_address(session, firm, patch_get_application, legal_fias_guid, legaladdress,
                               local_legaladdress, person_category):
    person = ob.PersonBuilder.construct(session,
                                          type=person_category.category,
                                          legal_fias_guid=legal_fias_guid,
                                          legaladdress=legaladdress,
                                          local_legaladdress=local_legaladdress)

    with patch_get_application:
        info = PersonWrapper(person).get_info([firm])

    if legal_fias_guid:
        if local_legaladdress:
            assert info['address'] == [{'address': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                     'translations': [{'lang': 'LOCAL',
                                                                       'address': u'local_legaladdress'}],
                                                     'address': u'Коми Респ, Усть-Вымский р-н, пгт Жешарт'}],
                                        'fias_guid': [{'fias_guid': u'13a5bcdb-9187-4d0f-b027-45f8ec29591a',
                                                       'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                        'address_type': 'LEGAL',
                                        'validity_flag': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                           'validity_flag': 'Y'}],
                                        'state': [{'state': person.person_category.oebs_country_code,
                                                   'date_from': datetime.datetime(1970, 1, 1, 0, 0)}]}]
        else:
            assert info['address'] == [{'address': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                     'address': u'Коми Респ, Усть-Вымский р-н, пгт Жешарт'}],
                                        'fias_guid': [{'fias_guid': u'13a5bcdb-9187-4d0f-b027-45f8ec29591a',
                                                       'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                        'address_type': 'LEGAL',
                                        'validity_flag': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                           'validity_flag': 'Y'}],
                                        'state': [{'state': person.person_category.oebs_country_code,
                                                   'date_from': datetime.datetime(1970, 1, 1, 0, 0)}]}]

    else:
        if legaladdress:
            if local_legaladdress:
                assert info['address'] == [{'address_type': 'LEGAL',
                                            'address': [{'address': u'юридический адрес строкой',
                                                         'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                         'translations': [{'address': u'local_legaladdress',
                                                                           'lang': 'LOCAL'}]}],
                                            'validity_flag': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                               'validity_flag': 'Y'}],
                                            'state': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                       'state': person.person_category.oebs_country_code}]}]
            else:
                assert info['address'] == [{'address_type': 'LEGAL',
                                            'address': [{'address': u'юридический адрес строкой',
                                                         'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                            'validity_flag': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                               'validity_flag': 'Y'}],
                                            'state': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                       'state': person.person_category.oebs_country_code}]}]
        else:
            if local_legaladdress:
                assert info['address'] == [{'address': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                         'translations': [{'lang': 'LOCAL',
                                                                           'address': u'local_legaladdress'}],
                                                         'address': ''}],
                                            'validity_flag': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                               'validity_flag': 'Y'}],
                                            'address_type': 'LEGAL',
                                            'state': [{'state': person.person_category.oebs_country_code,
                                                       'date_from': datetime.datetime(1970, 1, 1, 0, 0)}]}]
            else:
                assert 'address' not in info


@pytest.mark.parametrize('person_type', ['ur', 'ph'])
@pytest.mark.parametrize('is_partner', [1, 0])
@pytest.mark.parametrize('legal_address_postcode', [None, 34231])
def test_address_legal_postal_code(session, firm, legal_address_postcode, patch_get_application,
                                   is_partner, person_type):
    person = ob.PersonBuilder.construct(session,
                                          type=person_type,
                                          is_partner=is_partner,
                                          legal_address_postcode=legal_address_postcode)

    with patch_get_application:
        info = PersonWrapper(person).get_info([firm])
    if not is_partner and person_type == 'ph':
        assert 'address' not in info
    else:
        if legal_address_postcode:
            assert info['address'] == [{'postal_code': [{'postal_code': 34231,
                                                         'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                        'validity_flag': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                           'validity_flag': 'Y'}],
                                        'state': [{'state': u'RU',
                                                   'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                        'address_type': 'LEGAL'}]
        else:
            assert 'address' not in info


@pytest.mark.parametrize('person_type', ['yt', 'ur'])
@pytest.mark.parametrize('legal_address_home', [None, u'д. 12'])
@pytest.mark.parametrize('legal_fias_guid', [None, u'13a5bcdb-9187-4d0f-b027-45f8ec29591a'])
def test_address_legal_address_suffix(session, firm, person_type, legal_address_home,
                                      patch_get_application, legal_fias_guid):
    person = ob.PersonBuilder.construct(session,
                                          type=person_type,
                                          legal_address_street=u'ул. Прямая',
                                          legal_address_home=legal_address_home,
                                          legal_fias_guid=legal_fias_guid)

    with patch_get_application:
        info = PersonWrapper(person).get_info([firm])
    person_state = person.person_category.oebs_country_code if person.person_category.resident else u'NU'
    if legal_fias_guid:
        if legal_address_home:
            assert info['address'] == [{'address_suffix': [{'address_suffix': u'д. 12',
                                                            'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                        'address': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                     'address': u'Коми Респ, Усть-Вымский р-н, пгт Жешарт, ул. Прямая'}],
                                        'fias_guid': [{'fias_guid': u'13a5bcdb-9187-4d0f-b027-45f8ec29591a',
                                                       'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                        'address_type': 'LEGAL',
                                        'state': [{'state': person_state,
                                                   'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                        'validity_flag': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                           'validity_flag': 'Y'}]}]
        else:
            assert info['address'] == [{'address': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                     'address': u'Коми Респ, Усть-Вымский р-н, пгт Жешарт, ул. Прямая'}],
                                        'fias_guid': [{'fias_guid': u'13a5bcdb-9187-4d0f-b027-45f8ec29591a',
                                                       'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                        'address_type': 'LEGAL',
                                        'state': [{'state': person_state,
                                                   'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                        'validity_flag': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                           'validity_flag': 'Y'}]}]

    else:
        assert 'address' not in info


@pytest.mark.parametrize('is_partner', [1, 0])
@pytest.mark.parametrize('legal_fias_guid', [None, u'13a5bcdb-9187-4d0f-b027-45f8ec29591a'])
def test_address_legal_address_suffix_ph(session, firm, patch_get_application, is_partner, legal_fias_guid):
    person = ob.PersonBuilder.construct(session,
                                          type='ph',
                                          is_partner=is_partner,
                                          legal_address_street=u'ул. Прямая',
                                          legal_address_home=u'д. 12',
                                          legal_fias_guid=legal_fias_guid)

    with patch_get_application:
        info = PersonWrapper(person).get_info([firm])
    if legal_fias_guid:
        if not is_partner:
            assert 'address' not in info
        else:
            assert info['address'] == [{'address_suffix': [{'address_suffix': u'д. 12',
                                                            'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                        'address': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                     'address': u'Коми Респ, Усть-Вымский р-н, пгт Жешарт, ул. Прямая'}],
                                        'fias_guid': [{'fias_guid': u'13a5bcdb-9187-4d0f-b027-45f8ec29591a',
                                                       'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                        'address_type': 'LEGAL',
                                        'state': [{'state': u'RU',
                                                   'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                        'validity_flag': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                           'validity_flag': 'Y'}]}]
    else:
        assert 'address' not in info


@pytest.mark.parametrize('is_partner', [1, 0])
@pytest.mark.parametrize('person_type', ['ph', 'ur'])
@pytest.mark.parametrize('legal_fias_guid', [None, u'13a5bcdb-9187-4d0f-b027-45f8ec29591a'])
def test_address_legal_fias_guid(session, firm, patch_get_application, legal_fias_guid, person_type,
                                 is_partner):
    person = ob.PersonBuilder.construct(session,
                                          type=person_type,
                                          is_partner=is_partner,
                                          legal_fias_guid=legal_fias_guid)

    with patch_get_application:
        info = PersonWrapper(person).get_info([firm])
    if legal_fias_guid:
        if person_type == 'ph':
            if is_partner:
                assert info['address'] == [{'address': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                         'address': u'Коми Респ, Усть-Вымский р-н, пгт Жешарт'}],
                                            'fias_guid': [{'fias_guid': u'13a5bcdb-9187-4d0f-b027-45f8ec29591a',
                                                           'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                            'address_type': 'LEGAL',
                                            'state': [{'state': u'RU',
                                                       'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                            'validity_flag': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                               'validity_flag': 'Y'}]}]
            else:
                assert 'address' not in info
        else:
            if is_partner:
                assert info['address'] == [{'address': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                         'address': u'Коми Респ, Усть-Вымский р-н, пгт Жешарт'}],
                                            'fias_guid': [{'fias_guid': u'13a5bcdb-9187-4d0f-b027-45f8ec29591a',
                                                           'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                            'address_type': 'LEGAL',
                                            'state': [{'state': u'RU',
                                                       'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                            'validity_flag': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                               'validity_flag': 'Y'}]}]
            else:
                assert info['address'] == [{'address': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                         'address': u'Коми Респ, Усть-Вымский р-н, пгт Жешарт'}],
                                            'fias_guid': [{'fias_guid': u'13a5bcdb-9187-4d0f-b027-45f8ec29591a',
                                                           'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                            'address_type': 'LEGAL',
                                            'state': [{'state': u'RU',
                                                       'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                            'validity_flag': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                               'validity_flag': 'Y'}]}]
    else:
        assert 'address' not in info


@pytest.mark.parametrize('person_type', ['ph', 'ur', 'ytph'])
@pytest.mark.parametrize('address', [None, u'Адрес строкой'])
def test_address_sklad_address(session, firm, patch_get_application, address, person_type):
    person = ob.PersonBuilder.construct(session,
                                          type=person_type,
                                          address=address)

    with patch_get_application:
        info = PersonWrapper(person).get_info([firm])
    if address and person_type == 'ur':
        assert info['address'] == [
            {'state': [
                {'state': person.person_category.oebs_country_code if person.person_category.resident else u'NU',
                 'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                'address': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                             'address': u'Адрес строкой'}],
                'validity_flag': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                   'validity_flag': 'Y'}],
                'address_type': 'SKLAD'}]
    else:
        assert 'address' not in info


@pytest.mark.parametrize('invalid_address', [1, 0, None])
def test_validity_flag(session, firm, invalid_address, person_category, patch_get_application):
    person = ob.PersonBuilder.construct(session,
                                          type=person_category.category,
                                          postaddress=u'Почтовый адрес',
                                          invalid_address=invalid_address)

    with patch_get_application:
        info = PersonWrapper(person).get_info([firm])
    if invalid_address == 1:
        assert info['address'] == [{'state': [{'state': person.person_category.oebs_country_code,
                                               'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                    'address': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                 'address': u'Почтовый адрес'}],
                                    'validity_flag': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                       'validity_flag': 'N'}],
                                    'address_type': 'POST'}]
    elif invalid_address == 0:
        assert info['address'] == [{'state': [{'state': person.person_category.oebs_country_code,
                                               'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                    'address': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                 'address': u'Почтовый адрес'}],
                                    'validity_flag': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                       'validity_flag': 'Y'}],
                                    'address_type': 'POST'}]
    else:
        assert info['address'] == [{'state': [{'state': person.person_category.oebs_country_code,
                                               'date_from': datetime.datetime(1970, 1, 1, 0, 0)}],
                                    'address': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                 'address': u'Почтовый адрес'}],
                                    'address_type': 'POST'}]


@pytest.mark.parametrize('person_type', ['ur', 'ytph'])
@pytest.mark.parametrize('region', [None, 123])
def test_address_state(session, firm, patch_get_application, person_type, region):
    person = ob.PersonBuilder.construct(session,
                                          type=person_type,
                                          region=region,
                                          postaddress=u'Почтовый адрес')

    with patch_get_application as app:
        app.return_value.geolookup.parents.return_value = [1]
        app.return_value.geolookup.regionById.return_value.type = 3
        region_code = ob.generate_character_string(2)
        app.return_value.geolookup.regionById.return_value.short_ename = region_code

        info = PersonWrapper(person).get_info([firm])

    if person_type == 'ytph':
        if region:
            assert info['address'][0]['state'] == [{'state': person.person_category.oebs_country_code,
                                                    'date_from': datetime.datetime(1970, 1, 1, 0, 0)}]
        else:
            assert info['address'][0]['state'] == [{'state': person.person_category.oebs_country_code,
                                                    'date_from': datetime.datetime(1970, 1, 1, 0, 0)}]
    else:
        assert info['address'][0]['state'] == [{'state': person.person_category.oebs_country_code,
                                                'date_from': datetime.datetime(1970, 1, 1, 0, 0)}]


@pytest.mark.parametrize('email', ['email@email.ru', None])
def test_contact_email(session, firm, person_category, email):
    person = ob.PersonBuilder.construct(session,
                                          type=person_category.category,
                                          email=email)

    info = PersonWrapper(person).get_info([firm])
    if email:
        assert info['contact'] == [{'contact': 'email@email.ru', 'contact_type': 'EMAIL'}]
    else:
        assert 'contact' not in info


@pytest.mark.parametrize('fax', ['+7 (234) 2324433', None])
def test_contact_fax(session, firm, person_category, fax):
    person = ob.PersonBuilder.construct(session,
                                          type=person_category.category,
                                          fax=fax)

    info = PersonWrapper(person).get_info([firm])
    if fax:
        assert info['contact'] == [{'contact': '+7 (234) 2324433', 'contact_type': 'FAX'}]
    else:
        assert 'contact' not in info


@pytest.mark.parametrize('phone', ['+7 (234) 2324433', None])
def test_contact_phone(session, firm, person_category, phone):
    person = ob.PersonBuilder.construct(session,
                                          type=person_category.category,
                                          phone=phone)

    info = PersonWrapper(person).get_info([firm])
    if phone:
        assert info['contact'] == [{'contact': '+7 (234) 2324433', 'contact_type': 'PHONE'}]
    else:
        assert 'contact' not in info


@pytest.mark.parametrize('ur', [1, 0])
@pytest.mark.parametrize('representative', [u'ФИО контактого лица', None])
@pytest.mark.parametrize('local_representative', ['local_name_pepr', None])
def test_representatives_representative(session, firm, ur, representative, local_representative):
    person_category = create_person_category(session, ur=ur)
    person = ob.PersonBuilder.construct(session,
                                          type=person_category.category,
                                          representative=representative,
                                          local_representative=local_representative)

    info = PersonWrapper(person).get_info([firm])
    if ur:
        if representative:
            if local_representative:
                assert info['representative'] == [{'last_name': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                                  'last_name': u'ФИО контактого лица',
                                                                  'translations': [{'lang': 'LOCAL',
                                                                                    'last_name': 'local_name_pepr'}]}],
                                                   'type': 'REPRESENTATIVE'}]
            else:
                assert info['representative'] == [{'last_name': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                                  'last_name': u'ФИО контактого лица'}],
                                                   'type': 'REPRESENTATIVE'}]

        else:
            if local_representative:
                assert info['representative'] == [{'last_name': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                                  'last_name': u'',
                                                                  'translations': [{'lang': 'LOCAL',
                                                                                    'last_name': 'local_name_pepr'}]}],
                                                   'type': 'REPRESENTATIVE'}]
            else:
                assert 'representative' not in info
    else:
        assert 'representative' not in info


@pytest.mark.parametrize('ur', [1, 0])
@pytest.mark.parametrize('signer_person_name', [u'ФИО подписанта', None])
@pytest.mark.parametrize('local_signer_person_name', ['local_name_signer', None])
def test_representatives_signer(session, firm, ur, signer_person_name, local_signer_person_name):
    person_category = create_person_category(session, ur=ur)
    person = ob.PersonBuilder.construct(session,
                                          type=person_category.category,
                                          signer_person_name=signer_person_name,
                                          local_signer_person_name=local_signer_person_name)

    info = PersonWrapper(person).get_info([firm])
    if ur:
        if signer_person_name:
            if local_signer_person_name:
                assert info['representative'] == [{'last_name': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                                  'last_name': u'ФИО подписанта',
                                                                  'translations': [{'lang': 'LOCAL',
                                                                                    'last_name': 'local_name_signer'}]}],
                                                   'type': 'DIRECTOR'}]
            else:
                assert info['representative'] == [{'last_name': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                                  'last_name': u'ФИО подписанта'}],
                                                   'type': 'DIRECTOR'}]

        else:
            if local_signer_person_name:
                assert info['representative'] == [{'last_name': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                                  'last_name': u'',
                                                                  'translations': [{'lang': 'LOCAL',
                                                                                    'last_name': 'local_name_signer'}]}],
                                                   'type': 'DIRECTOR'}]
            else:
                assert 'representative' not in info
    else:
        assert 'representative' not in info


@pytest.mark.parametrize('signer_person_gender', ['M', u'м', u'М', u'ж', u'Ж', 'W', 'X', None])
def test_representatives_signer_sex(session, firm, signer_person_gender):
    person = ob.PersonBuilder.construct(session,
                                          type=create_person_category(session, ur=1).category,
                                          signer_person_name=u'ФИО подписанта',
                                          signer_person_gender=signer_person_gender)

    info = PersonWrapper(person).get_info([firm])
    if signer_person_gender in ['M', u'м', u'М']:
        assert info['representative'] == [{'last_name': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                          'last_name': u'ФИО подписанта'}],
                                           'type': 'DIRECTOR', 'sex': 'M'}]

    elif signer_person_gender in [u'ж', u'Ж', 'W', ]:
        assert info['representative'] == [{'last_name': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                          'last_name': u'ФИО подписанта'}],
                                           'type': 'DIRECTOR', 'sex': 'F'}]
    else:
        assert info['representative'] == [{'last_name': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                          'last_name': u'ФИО подписанта'}],
                                           'type': 'DIRECTOR'}]


@pytest.mark.parametrize('authority_doc_type', [u'Доверенность', None])
@pytest.mark.parametrize('authority_doc_details', [None, '1244'])
@pytest.mark.parametrize('local_authority_doc_details', [None, '124444'])
def test_representatives_signer_basis(session, firm, authority_doc_type, authority_doc_details,
                                      local_authority_doc_details):
    person = ob.PersonBuilder.construct(session,
                                          type=create_person_category(session, ur=1).category,
                                          signer_person_name=u'ФИО подписанта',
                                          authority_doc_type=authority_doc_type,
                                          authority_doc_details=authority_doc_details,
                                          local_authority_doc_details=local_authority_doc_details
                                          )

    info = PersonWrapper(person).get_info([firm])
    if authority_doc_type:
        if authority_doc_details:
            if local_authority_doc_details:
                assert info['representative'] == [{'basis': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                              'translations': [{'lang': 'LOCAL',
                                                                                'basis': u'Доверенность 124444'}],
                                                              'basis': u'Доверенность 1244'}],
                                                   'last_name': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                                  'last_name': u'ФИО подписанта'}],
                                                   'type': 'DIRECTOR'}]
            else:
                assert info['representative'] == [{'basis': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                              'translations': [{'lang': 'LOCAL',
                                                                                'basis': u'Доверенность'}],
                                                              'basis': u'Доверенность 1244'}],
                                                   'last_name': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                                  'last_name': u'ФИО подписанта'}],
                                                   'type': 'DIRECTOR'}]
        else:
            if local_authority_doc_details:
                assert info['representative'] == [{'basis': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                              'translations': [{'lang': 'LOCAL',
                                                                                'basis': u'Доверенность 124444'}],
                                                              'basis': u'Доверенность'}],
                                                   'last_name': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                                  'last_name': u'ФИО подписанта'}],
                                                   'type': 'DIRECTOR'}]
            else:
                assert info['representative'] == [{'basis': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                              'translations': [{'lang': 'LOCAL',
                                                                                'basis': u'Доверенность'}],
                                                              'basis': u'Доверенность'}],
                                                   'last_name': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                                  'last_name': u'ФИО подписанта'}],
                                                   'type': 'DIRECTOR'}]
    else:
        if authority_doc_details:
            if local_authority_doc_details:
                assert info['representative'] == [{'basis': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                              'translations': [{'lang': 'LOCAL',
                                                                                'basis': '124444'}],
                                                              'basis': '1244'}],
                                                   'last_name': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                                  'last_name': u'ФИО подписанта'}],
                                                   'type': 'DIRECTOR'}]
            else:
                assert info['representative'] == [{'basis': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                              'basis': '1244'}],
                                                   'last_name': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                                  'last_name': u'ФИО подписанта'}],
                                                   'type': 'DIRECTOR'}]
        else:
            if local_authority_doc_details:
                assert info['representative'] == [{'basis': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                              'translations': [{'lang': 'LOCAL',
                                                                                'basis': '124444'}],
                                                              'basis': ''}],
                                                   'last_name': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                                  'last_name': u'ФИО подписанта'}],
                                                   'type': 'DIRECTOR'}]
            else:
                assert info['representative'] == [{'last_name': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                                  'last_name': u'ФИО подписанта'}],
                                                   'type': 'DIRECTOR'}]


@pytest.mark.parametrize('signer_position_name', [u'Главный', None])
@pytest.mark.parametrize('local_signer_position_name', ['Glavnyy', None])
def test_representatives_representative_position(session, firm, signer_position_name, local_signer_position_name):
    person_category = create_person_category(session, ur=1)
    person = ob.PersonBuilder.construct(session,
                                          type=person_category.category,
                                          signer_person_name=u'ФИО подписанта',
                                          signer_position_name=signer_position_name,
                                          local_signer_position_name=local_signer_position_name
                                          )

    info = PersonWrapper(person).get_info([firm])
    if signer_position_name:
        if local_signer_position_name:
            assert info['representative'] == [
                {'representative_position': [{'representative_position': u'Главный',
                                              'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                              'translations': [{'lang': 'LOCAL',
                                                                'representative_position': 'Glavnyy'}]}],
                 'last_name': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                'last_name': u'ФИО подписанта'}],
                 'type': 'DIRECTOR'}]
        else:
            assert info['representative'] == [{'representative_position': [{'representative_position': u'Главный',
                                                                            'date_from': datetime.datetime(1970, 1, 1,
                                                                                                           0, 0)}],
                                               'last_name': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                              'last_name': u'ФИО подписанта'}],
                                               'type': 'DIRECTOR'}]

    else:
        if local_signer_position_name:
            assert info['representative'] == [{'representative_position': [{'representative_position': '',
                                                                            'date_from': datetime.datetime(1970, 1, 1,
                                                                                                           0, 0),
                                                                            'translations': [{'lang': 'LOCAL',
                                                                                              'representative_position': 'Glavnyy'}]}],
                                               'last_name': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                              'last_name': u'ФИО подписанта'}],
                                               'type': 'DIRECTOR'}]
        else:
            assert info['representative'] == [{'last_name': [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                                              'last_name': u'ФИО подписанта'}],
                                               'type': 'DIRECTOR'}]


@pytest.mark.parametrize('vip', [1, 0, None])
def test_vip(session, firm, person_category, vip):
    person = ob.PersonBuilder.construct(session,
                                          type=person_category.category,
                                          vip=vip)
    info = PersonWrapper(person).get_info([firm])
    if vip == 1:
        assert info['vip_flag'] == 'Y'
    else:
        assert info['vip_flag'] == 'N'


@pytest.mark.parametrize('intercompany', ['RU10', None])
def test_intercompany(session, firm, person_category, intercompany):
    person = ob.PersonBuilder.construct(session,
                                          type=person_category.category)
    person.client.intercompany = intercompany
    info = PersonWrapper(person).get_info([firm])
    if intercompany:
        assert info['intercompany'] == 'RU10'
    else:
        assert 'intercompany' not in info


@pytest.mark.parametrize('person_type', ['ur', 'kzu'])
@pytest.mark.parametrize('live_signature', [1, 0, 2])
def test_sign_type(session, firm, person_category, live_signature, person_type):
    person = ob.PersonBuilder.construct(session,
                                          type=person_type,
                                          live_signature=live_signature)
    if live_signature in [1, 0] or person_type == 'kzu':
        info = PersonWrapper(person).get_info([firm])

        if live_signature == 1 or person_type == 'kzu':
            assert info['sign_type'] == 'LIVE'
        else:
            assert info['sign_type'] == 'FAX'
    else:
        with pytest.raises(ValueError):
            PersonWrapper(person).get_info([firm])


@pytest.mark.parametrize('person_type', ['ur', 'ph', 'yt'])
@pytest.mark.parametrize('delivery_type', [0, 1, 2, 3, 4, 5])
@pytest.mark.parametrize('delivery_city', [None, u'Москва'])
def test_delivery_type(session, firm, person_category, delivery_type, person_type, delivery_city):
    delivery_type_map = {
        0: 'PR',
        1: 'PR',
        2: 'CY',
        3: 'CC',
        4: 'VIP',
        5: 'EDO',
    }
    person = ob.PersonBuilder.construct(session,
                                          delivery_city=delivery_city,
                                          delivery_type=delivery_type,
                                          type=person_type
                                          )
    info = PersonWrapper(person).get_info([firm])
    if person_type in ('ur', 'ph') and delivery_type != 5 and delivery_city:
        assert info['delivery_type'] == [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                          'delivery_type': u'{}_Москва'.format(delivery_type_map[delivery_type])}]
    else:
        assert info['delivery_type'] == [{'date_from': datetime.datetime(1970, 1, 1, 0, 0),
                                          'delivery_type': delivery_type_map[delivery_type]}]


@pytest.mark.parametrize('early_docs', [1, 0, None])
def test_early_docs(session, firm, person_category, early_docs):
    person = ob.PersonBuilder.construct(session,
                                          type=person_category.category,
                                          early_docs=early_docs)
    info = PersonWrapper(person).get_info([firm])
    if early_docs == 1:
        assert info['early_docs'] == 'Y'
    else:
        assert info['early_docs'] == 'N'
