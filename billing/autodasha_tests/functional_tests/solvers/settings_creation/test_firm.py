# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import uuid
import contextlib

import pytest

from balance import mapper

from autodasha.solver_cl.settings_creation.firm_solver import CreateSettingsFirm
from autodasha.solver_cl.settings_creation.common_states import BankState
from autodasha.core.api.tracker import IssueTransitions

from tests.autodasha_tests.functional_tests import case_utils
from tests.autodasha_tests.common import db_utils


class RequiredResult(object):
    __slots__ = ('comments', 'transition', 'assignee', 'delay', 'commit', 'summonees', 'banks_inserted')

    def __init__(self, comments, delay=False, commit=True, transition=IssueTransitions.none, assignee='autodasha',
                 summonees=None, banks_inserted=0):
        self.comments = comments
        self.transition = transition
        self.assignee = assignee
        self.delay = delay
        self.commit = commit
        self.summonees = summonees or []
        self.banks_inserted = banks_inserted


def get_approve_message(*args, **kwargs):
    return 'Нажми кнопку "Подтвердить", сука! https://jing.yandex-team.ru/files/autodasha/approve_button_something.png'


class AbstractDBTestCase(case_utils.AbstractDBTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    summary = 'Заявка на заведение фирмы в Балансе: Рога и Копыта'

    _parameters = {
        'title': 'Наименование фирмы: %s',
        'region': 'Регион: %s',
        'inn': 'ИНН/Registration Number: %s',
        'kpp': 'КПП: %s',
        'legaladdress': 'Юридический адрес: %s',
        'phone': 'Телефон по-умолчанию в печатных формах: %s',
        'email': 'Email в рассылках по-умолчанию: %s',
        'payment_invoice_email': 'Email рассылки о недоплатах/переплатах: %s',
        'default_currency': 'Функциональная валюта юрлица: %s',
        'unilateral': 'Односторонние документы: %s',
        'pa_prefix': 'Префикс лицевых счетов: %s',
        'can_use_shop': 'Возможность выставления документов через магазин Биллинга: %s',
        'oebs_org_id': 'OEBS_ORG_ID: %s',
        'oebs_user_id': 'OEBS_USER_ID: %s',
        'oebs_perm_id': 'OEBS_PERM_ID: %s',
        'oebs_app_id': 'OEBS_APP_ID: %s',
        'oebs_tax_nds': 'Формат кода налога ОЕБС для резидентов: %s',
        'oebs_tax_free': 'Код налога ОЕБС для нерезидентов: %s',
        'oebs_tax_free_resident': 'Код налога ОЕБС для резидентов освобождённых от НДС: %s',
        'broken_line': 'Говнокод к трону говнокода: %s',
    }
    _bank_parameters = {
        'is_new_bank': 'Новый банк - %s',
        'currency': 'Валюта банковского счёта - %s',
        'bank': 'Банк - %s',
        'account': 'Расчётный счёт - %s',
        'bankaccount': 'Корреспондентский счёт - %s',
        'cor_bank': 'Банк-корреспондент - %s',
        'cor_swift': 'SWIFT банка-корреспондента - %s',
        'cor_iban': 'IBAN банка-корреспондента - %s',
        'name': 'Название банка - %s',
        'address': 'Адрес банка - %s',
        'bik': 'БИК - %s',
        'swift': 'SWIFT - %s',
        'oebs_code': 'Код ОЕБС - %s',
    }

    def __init__(self):
        self.bank_name2id = {}

    def get_description(self, session):
        params = self._get_data(session)
        bank_details = params.pop('bank_details', None)

        lines = []
        for param, value in params.iteritems():
            lines.append(self._parameters[param] % value)
        if bank_details:
            lines.append('Банковские реквизиты:')
            for bank_detail in bank_details:
                lines.append('Банковские реквизиты')
                for param, value in bank_detail.iteritems():
                    lines.append(self._bank_parameters[param] % value)
        return '\n'.join(lines)

    @contextlib.contextmanager
    def mock_bank_insertion(self):
        bank_name2id = self.bank_name2id
        old_func = BankState.get_executable_dml

        def mock_func(self):
            insert = old_func(self)
            inserted_id = uuid.uuid4().int
            bank_name2id[insert.parameters['name']] = inserted_id
            return insert.values(id=inserted_id)

        BankState.get_executable_dml = mock_func
        yield
        BankState.get_executable_dml = old_func


class FirmExportBanksCase(AbstractDBTestCase):
    _representation = 'firm_export_banks'

    def __init__(self):
        super(FirmExportBanksCase, self).__init__()
        self.bank1 = None
        self.bank2 = None

    def _get_data(self, session):
        self.bank1 = db_utils.create_payment_bank(
            session,
            address='Адрес 1',
            swift='swift1',
            bik='111111',
            oebs_code='bank1',
        )
        self.bank2 = db_utils.create_payment_bank(
            session,
            address='Адрес 2',
            swift='swift2',
            bik='111111',
            oebs_code='bank2',
        )

        return {
            'title': 'Ахалай-Махалай',
            'region': 'Россия',
            'inn': '1234567890',
            'kpp': '654321',
            'legaladdress': 'Великая прекрасная Россия, дом 42',
            'phone': '6(666)666-66-66',
            'email': 'yandex@yandex.yandex',
            'payment_invoice_email': 'payment@yandex.yandex',
            'default_currency': 'RUB',
            'unilateral': 'Нет',
            'pa_prefix': 'СЛ',
            'oebs_org_id': 6,
            'oebs_user_id': 66,
            'oebs_perm_id': 666,
            'oebs_app_id': 6666,
            'oebs_tax_nds': 'НДС 30',
            'oebs_tax_free': 'Без НДС',
            'oebs_tax_free_resident': 'Без НДС (осв)',
            'bank_details': [
                {
                    'is_new_bank': False,
                    'bank': self.bank1.name,
                    'currency': 'RUB',
                    'account': '11111111',
                    'bankaccount': '22222222',
                },
                {
                    'is_new_bank': False,
                    'bank': '%s (%s)' % (self.bank2.name, self.bank2.id),
                    'currency': 'EUR',
                    'account': '33333333',
                    'bankaccount': '44444444',
                    'cor_bank': 'Левый банк',
                    'cor_swift': 'swift_666',
                    'cor_iban': 'iban_666'
                }
            ]
        }

    def get_result(self):
        return RequiredResult(
            transition=IssueTransitions.none,
            assignee='mscnad7',
            comments='''
Надо создать следующие объекты:
Фирма:
* ID: <требуется указать>
* Название: Ахалай-Махалай
* Регион: Россия
* ИНН: 1234567890
* КПП: 654321
* Адрес: Великая прекрасная Россия, дом 42
* Телефон: 6(666)666-66-66
* Email: yandex@yandex.yandex
* Email рассылки о платежах: payment@yandex.yandex
* Валюта: RUB
* Односторонние акты: Нет
* Префикс ЛС: СЛ
* Настройки закрытия межфилиалки: {{"mnclose_close_tasks": ["monthly_close_firms"]}}
* Код источника курсов валют: cbr
<{{dml
%%(sql)
INSERT INTO meta.t_firm (id, title, region_id, inn, kpp, legaladdress, phone, email, payment_invoice_email, default_iso_currency, default_currency, unilateral, pa_prefix, config, currency_rate_src)
VALUES (:firm_id, 'Ахалай-Махалай', 225, '1234567890', '654321', 'Великая прекрасная Россия, дом 42', '6(666)666-66-66', 'yandex@yandex.yandex', 'payment@yandex.yandex', 'RUB', 'RUR', 0, 'СЛ', '{{"mnclose_close_tasks": ["monthly_close_firms"]}}', 'cbr')
%%}}>
Настройки экспорта фирмы:
* ID Фирмы: <требуется указать>
* Тип экспорта: OEBS
* OEBS_ORG_ID: 6
* OEBS_USER_ID: 66
* OEBS_PERM_ID: 666
* OEBS_APP_ID: 6666
* Пропускать суффикс организации OEBS: Да
* Код страны ОЕБС: RU
* Формат кода налога ОЕБС для резидентов: НДС 30
* Код налога ОЕБС для резидентов освобождённых от НДС: Без НДС (осв)
* Код налога ОЕБС для нерезидентов: Без НДС
* Требуется выгружать налог для ЛС: Нет
<{{dml
%%(sql)
INSERT INTO bo.t_firm_export (firm_id, export_type, oebs_org_id, oebs_user_id, oebs_perm_id, oebs_app_id, oebs_skip_org_suffix, oebs_country_code, oebs_tax_nds, oebs_tax_free_resident, oebs_tax_free, oebs_need_pa_tax)
VALUES (:firm_id, 'OEBS', 6, 66, 666, 6666, 1, 'RU', 'НДС 30', 'Без НДС (осв)', 'Без НДС', 0)
%%}}>
Банковские реквизиты:
* Банк: {bank1.name}
* Фирма: <требуется указать>
* Валюта: RUB
* Название банка: {bank1.name}
* БИК/SWIFT: 111111
* Адрес: Адрес 1
* Расчётный счёт: 11111111
* Корреспондентский счёт: 22222222
* Код ОЕБС: bank1
<{{dml
%%(sql)
INSERT INTO bo.t_bank_details (bank_id, firm_id, iso_currency, currency, bank, bankcode, bankaddress, account, bankaccount, needs_alert, oebs_code)
VALUES ({bank1.id}, :firm_id, 'RUB', 'RUR', '{bank1.name}', '111111', 'Адрес 1', '11111111', '22222222', 0, 'bank1')
%%}}>
Банковские реквизиты:
* Банк: {bank2.name}
* Фирма: <требуется указать>
* Валюта: EUR
* Название банка: {bank2.name}
* БИК/SWIFT: swift2
* Адрес: Адрес 2
* Расчётный счёт: 33333333
* Корреспондентский счёт: 44444444
* Банк-корреспондент: Левый банк
* SWIFT банка-корреспондента: swift_666
* IBAN: iban_666
* Код ОЕБС: bank2
<{{dml
%%(sql)
INSERT INTO bo.t_bank_details (bank_id, firm_id, iso_currency, currency, bank, bankcode, bankaddress, account, bankaccount, corrbank, corrbankcode, corriban, needs_alert, oebs_code)
VALUES ({bank2.id}, :firm_id, 'EUR', 'EUR', '{bank2.name}', 'swift2', 'Адрес 2', '33333333', '44444444', 'Левый банк', 'swift_666', 'iban_666', 0, 'bank2')
%%}}>
'''.lstrip().format(bank1=self.bank1, bank2=self.bank2))


class CanUseShopCase(AbstractDBTestCase):
    _representation = 'can_use_shop'

    def _get_data(self, session):
        return {
            'title': 'Ахалай-Махалай',
            'region': 'Россия (225)',
            'inn': '1234567890',
            'kpp': '654321',
            'legaladdress': 'Великая прекрасная Россия, дом 42',
            'phone': '6(666)666-66-66',
            'email': 'yandex@yandex.yandex',
            'payment_invoice_email': 'payment@yandex.yandex',
            'default_currency': 'RUB',
            'unilateral': 'Нет',
            'pa_prefix': 'СЛ',
            'can_use_shop': 'Да',
            'oebs_org_id': 6,
            'oebs_user_id': 66,
            'oebs_perm_id': 666,
            'oebs_app_id': 6666,
            'oebs_tax_nds': 'НДС 30',
            'oebs_tax_free': 'Без НДС',
            'oebs_tax_free_resident': 'Без НДС (осв)',
        }

    def get_result(self):
        return RequiredResult(
            transition=IssueTransitions.none,
            assignee='mscnad7',
            comments=[
                '* Фиксированная категория: ur',
                '* Фиксированная категория: ph',
                '* Фиксированная категория: yt',
                '* Фиксированная категория: yt_kzu',
'''
Платежная политика:
* Сервис: Разовые продажи
* Фирма: Ахалай-Махалай
* Валюта: RUB
* Юрлицо: Нет
* Фиксированная категория: ph
* Способы оплаты: bank, card, yamoney_wallet, webmoney_wallet
<{dml
%%(sql)
INSERT ALL
  INTO bo.t_pay_policy_part (id, firm_id, category, legal_entity, description)
    VALUES (bo.s_pay_policy_part_id.nextval, :firm_id, 'ph', 0, 'bank|RUB|0%card|RUB|0%yamoney_wallet|RUB|0%webmoney_wallet|RUB|0')
  INTO bo.t_pay_policy_routing (service_id, region_id, pay_policy_part_id)
    VALUES (35, null, bo.s_pay_policy_part_id.nextval)
  INTO bo.t_pay_policy_paymethod (pay_policy_part_id, payment_method_id, iso_currency)
    VALUES (bo.s_pay_policy_part_id.nextval, 1001, 'RUB')
  INTO bo.t_pay_policy_paymethod (pay_policy_part_id, payment_method_id, iso_currency)
    VALUES (bo.s_pay_policy_part_id.nextval, 1101, 'RUB')
  INTO bo.t_pay_policy_paymethod (pay_policy_part_id, payment_method_id, iso_currency)
    VALUES (bo.s_pay_policy_part_id.nextval, 1201, 'RUB')
  INTO bo.t_pay_policy_paymethod (pay_policy_part_id, payment_method_id, iso_currency)
    VALUES (bo.s_pay_policy_part_id.nextval, 1202, 'RUB')
SELECT * FROM dual
%%}>
'''.strip()])


class NewBanksRequestCase(AbstractDBTestCase):
    _representation = 'new_banks_request'

    def __init__(self):
        super(NewBanksRequestCase, self).__init__()
        self.bank_name_1 = None
        self.bank_name_2 = None

    def _get_data(self, session):
        self.bank_name_1 = '1_' + uuid.uuid4().get_hex()
        self.bank_name_2 = '2_' + uuid.uuid4().get_hex()

        return {
            'title': 'Ахалай-Махалай',
            'region': 'Россия',
            'inn': '1234567890',
            'kpp': '654321',
            'legaladdress': 'Великая прекрасная Россия, дом 42',
            'phone': '6(666)666-66-66',
            'email': 'yandex@yandex.yandex',
            'payment_invoice_email': 'payment@yandex.yandex',
            'default_currency': 'RUB',
            'unilateral': 'Нет',
            'pa_prefix': 'СЛ',
            'bank_details': [
                {
                    'is_new_bank': True,
                    'name': self.bank_name_1,
                    'address': 'Адрес 1',
                    'bik': '111111',
                    'swift': 'SWIFT1',
                    'oebs_code': 'oebs_1',
                    'currency': 'RUB',
                    'account': '11111111',
                    'bankaccount': '22222222',
                },
                {
                    'is_new_bank': True,
                    'name': self.bank_name_1,
                    'address': 'Адрес 1',
                    'bik': '111111',
                    'swift': 'SWIFT1',
                    'oebs_code': 'oebs_1',
                    'currency': 'EUR',
                    'account': '33333333',
                    'bankaccount': '44444444',
                },
                {
                    'is_new_bank': True,
                    'name': self.bank_name_2,
                    'address': 'Адрес 2',
                    'bik': '222222',
                    'swift': 'SWIFT2',
                    'oebs_code': 'oebs_2',
                    'currency': 'RUB',
                    'account': '55555555',
                    'bankaccount': '66666666',
                },
            ]
        }

    def get_result(self):
        return RequiredResult(
            transition=IssueTransitions.none,
            assignee='autodasha',
            summonees=['mscnad7'],
            delay=True,
            comments='''
кто:mscnad7, проверь, пожалуйста, и подтверди заведение общих справочников на проде.
Банк:
* ID: <из последовательности>
* Название: {bank_name_1}
* Адрес: Адрес 1
* БИК: 111111
* SWIFT: SWIFT1
* Код ОЕБС: oebs_1
Банк:
* ID: <из последовательности>
* Название: {bank_name_2}
* Адрес: Адрес 2
* БИК: 222222
* SWIFT: SWIFT2
* Код ОЕБС: oebs_2

Для подтверждения требуется нажать кнопку "Подтверждено"
https://jing.yandex-team.ru/files/autodasha/approve_button_2_ru.png
For confirmation it is necessary to press the button "Confirmed"
https://jing.yandex-team.ru/files/autodasha/approve_button_2_eng.png
'''.strip().format(bank_name_1=self.bank_name_1, bank_name_2=self.bank_name_2))


class NewBankWaitingApproveRequestCase(AbstractDBTestCase):
    _representation = 'new_banks_waiting_approve'

    def get_comments(self):
        return [('autodasha', get_approve_message())]

    def _get_data(self, session):
        bank_name = uuid.uuid4().get_hex()

        return {
            'title': 'Ахалай-Махалай',
            'region': 'Россия',
            'inn': '1234567890',
            'kpp': '654321',
            'legaladdress': 'Великая прекрасная Россия, дом 42',
            'phone': '6(666)666-66-66',
            'email': 'yandex@yandex.yandex',
            'payment_invoice_email': 'payment@yandex.yandex',
            'default_currency': 'RUB',
            'unilateral': 'Нет',
            'pa_prefix': 'СЛ',
            'bank_details': [
                {
                    'is_new_bank': True,
                    'name': bank_name,
                    'address': 'Адрес 1',
                    'bik': '111111',
                    'swift': 'SWIFT1',
                    'oebs_code': 'oebs_1',
                    'currency': 'RUB',
                    'account': '11111111',
                    'bankaccount': '22222222',
                },
            ]
        }

    def get_result(self):
        return None


class NewBanksInsertCase(AbstractDBTestCase):
    _representation = 'new_banks_approved'

    def __init__(self):
        super(NewBanksInsertCase, self).__init__()
        self.bank_name_1 = None
        self.bank_name_2 = None

    def get_comments(self):
        return [('mscnad7', 'Подтверждено')]

    def _get_data(self, session):
        self.bank_name_1 = 'a_' + uuid.uuid4().get_hex()
        self.bank_name_2 = 'b_' + uuid.uuid4().get_hex()

        return {
            'title': 'Ахалай-Махалай',
            'region': 'Россия',
            'inn': '1234567890',
            'kpp': '654321',
            'legaladdress': 'Великая прекрасная Россия, дом 42',
            'phone': '6(666)666-66-66',
            'email': 'yandex@yandex.yandex',
            'payment_invoice_email': 'payment@yandex.yandex',
            'default_currency': 'RUB',
            'unilateral': 'Нет',
            'pa_prefix': 'СЛ',
            'bank_details': [
                {
                    'is_new_bank': True,
                    'name': self.bank_name_1,
                    'address': 'Адрес 1',
                    'bik': '111111',
                    'swift': 'SWIFT1',
                    'oebs_code': 'oebs_1',
                    'currency': 'RUB',
                    'account': '11111111',
                    'bankaccount': '22222222',
                },
                {
                    'is_new_bank': True,
                    'name': self.bank_name_1,
                    'address': 'Адрес 1',
                    'bik': '111111',
                    'swift': 'SWIFT1',
                    'oebs_code': 'oebs_1',
                    'currency': 'EUR',
                    'account': '33333333',
                    'bankaccount': '44444444',
                },
                {
                    'is_new_bank': True,
                    'name': self.bank_name_2,
                    'address': 'Адрес 2',
                    'bik': '222222',
                    'swift': 'SWIFT2',
                    'oebs_code': 'oebs_2',
                    'currency': 'RUB',
                    'account': '55555555',
                    'bankaccount': '66666666',
                },
            ]
        }

    def get_result(self):
        return RequiredResult(
            transition=IssueTransitions.none,
            assignee='mscnad7',
            delay=False,
            banks_inserted=2,
            comments='''
Надо создать следующие объекты:
Фирма:
* ID: <требуется указать>
* Название: Ахалай-Махалай
* Регион: Россия
* ИНН: 1234567890
* КПП: 654321
* Адрес: Великая прекрасная Россия, дом 42
* Телефон: 6(666)666-66-66
* Email: yandex@yandex.yandex
* Email рассылки о платежах: payment@yandex.yandex
* Валюта: RUB
* Односторонние акты: Нет
* Префикс ЛС: СЛ
* Настройки закрытия межфилиалки: {{"mnclose_close_tasks": ["monthly_close_firms"]}}
* Код источника курсов валют: cbr
<{{dml
%%(sql)
INSERT INTO meta.t_firm (id, title, region_id, inn, kpp, legaladdress, phone, email, payment_invoice_email, default_iso_currency, default_currency, unilateral, pa_prefix, config, currency_rate_src)
VALUES (:firm_id, 'Ахалай-Махалай', 225, '1234567890', '654321', 'Великая прекрасная Россия, дом 42', '6(666)666-66-66', 'yandex@yandex.yandex', 'payment@yandex.yandex', 'RUB', 'RUR', 0, 'СЛ', '{{"mnclose_close_tasks": ["monthly_close_firms"]}}', 'cbr')
%%}}>
Банк:
* ID: {bank_id_1}
* Название: {bank_name_1}
* Адрес: Адрес 1
* БИК: 111111
* SWIFT: SWIFT1
* Код ОЕБС: oebs_1
<{{dml
%%(sql)
MERGE INTO bo.t_payment_bank t
USING (
  SELECT
    {bank_id_1} id,
    '{bank_name_1}' name,
    'Адрес 1' address,
    '111111' bik,
    'SWIFT1' swift,
    'oebs_1' oebs_code
  FROM dual
) d
ON (t.id = d.id)
WHEN NOT MATCHED THEN
  INSERT (id, name, address, bik, swift, oebs_code)
  VALUES (d.id, d.name, d.address, d.bik, d.swift, d.oebs_code)
WHEN MATCHED THEN
  UPDATE SET
    t.name = d.name,
    t.address = d.address,
    t.bik = d.bik,
    t.swift = d.swift,
    t.oebs_code = d.oebs_code
%%}}>
Банк:
* ID: {bank_id_2}
* Название: {bank_name_2}
* Адрес: Адрес 2
* БИК: 222222
* SWIFT: SWIFT2
* Код ОЕБС: oebs_2
<{{dml
%%(sql)
MERGE INTO bo.t_payment_bank t
USING (
  SELECT
    {bank_id_2} id,
    '{bank_name_2}' name,
    'Адрес 2' address,
    '222222' bik,
    'SWIFT2' swift,
    'oebs_2' oebs_code
  FROM dual
) d
ON (t.id = d.id)
WHEN NOT MATCHED THEN
  INSERT (id, name, address, bik, swift, oebs_code)
  VALUES (d.id, d.name, d.address, d.bik, d.swift, d.oebs_code)
WHEN MATCHED THEN
  UPDATE SET
    t.name = d.name,
    t.address = d.address,
    t.bik = d.bik,
    t.swift = d.swift,
    t.oebs_code = d.oebs_code
%%}}>
Банковские реквизиты:
* Банк: {bank_name_1}
* Фирма: <требуется указать>
* Валюта: RUB
* Название банка: {bank_name_1}
* БИК/SWIFT: 111111
* Адрес: Адрес 1
* Расчётный счёт: 11111111
* Корреспондентский счёт: 22222222
* Код ОЕБС: oebs_1
<{{dml
%%(sql)
INSERT INTO bo.t_bank_details (bank_id, firm_id, iso_currency, currency, bank, bankcode, bankaddress, account, bankaccount, needs_alert, oebs_code)
VALUES ({bank_id_1}, :firm_id, 'RUB', 'RUR', '{bank_name_1}', '111111', 'Адрес 1', '11111111', '22222222', 0, 'oebs_1')
%%}}>
Банковские реквизиты:
* Банк: {bank_name_1}
* Фирма: <требуется указать>
* Валюта: EUR
* Название банка: {bank_name_1}
* БИК/SWIFT: SWIFT1
* Адрес: Адрес 1
* Расчётный счёт: 33333333
* Корреспондентский счёт: 44444444
* Код ОЕБС: oebs_1
<{{dml
%%(sql)
INSERT INTO bo.t_bank_details (bank_id, firm_id, iso_currency, currency, bank, bankcode, bankaddress, account, bankaccount, needs_alert, oebs_code)
VALUES ({bank_id_1}, :firm_id, 'EUR', 'EUR', '{bank_name_1}', 'SWIFT1', 'Адрес 1', '33333333', '44444444', 0, 'oebs_1')
%%}}>
Банковские реквизиты:
* Банк: {bank_name_2}
* Фирма: <требуется указать>
* Валюта: RUB
* Название банка: {bank_name_2}
* БИК/SWIFT: 222222
* Адрес: Адрес 2
* Расчётный счёт: 55555555
* Корреспондентский счёт: 66666666
* Код ОЕБС: oebs_2
<{{dml
%%(sql)
INSERT INTO bo.t_bank_details (bank_id, firm_id, iso_currency, currency, bank, bankcode, bankaddress, account, bankaccount, needs_alert, oebs_code)
VALUES ({bank_id_2}, :firm_id, 'RUB', 'RUR', '{bank_name_2}', '222222', 'Адрес 2', '55555555', '66666666', 0, 'oebs_2')
%%}}>
'''.lstrip().format(
                bank_name_1=self.bank_name_1,
                bank_id_1=self.bank_name2id["'%s'" % self.bank_name_1],
                bank_name_2=self.bank_name_2,
                bank_id_2=self.bank_name2id["'%s'" % self.bank_name_2]))


class IncorrectBankCase(AbstractDBTestCase):
    _representation = 'incorrect_bank'

    def __init__(self):
        super(IncorrectBankCase, self).__init__()
        self.bank_name = None

    def _get_data(self, session):
        self.bank_name = uuid.uuid4().get_hex()

        return {
            'title': 'Ахалай-Махалай',
            'region': 'Россия',
            'inn': '1234567890',
            'kpp': '654321',
            'legaladdress': 'Великая прекрасная Россия, дом 42',
            'phone': '6(666)666-66-66',
            'email': 'yandex@yandex.yandex',
            'payment_invoice_email': 'payment@yandex.yandex',
            'default_currency': 'RUB',
            'unilateral': 'Нет',
            'pa_prefix': 'СЛ',
            'bank_details': [
                {
                    'is_new_bank': False,
                    'bank': self.bank_name,
                    'currency': 'RUB',
                    'account': '11111111',
                    'bankaccount': '22222222',
                },
            ]
        }

    def get_result(self):
        return RequiredResult(
            transition=IssueTransitions.wont_fix,
            assignee='autodasha',
            delay=False,
            comments="Указан банк '{}', отсутствующий в справочнике.".format(self.bank_name))


class IncorrectRegionCase(AbstractDBTestCase):
    _representation = 'incorrect_region'

    def __init__(self):
        super(IncorrectRegionCase, self).__init__()
        self.region_name = None

    def _get_data(self, session):
        self.region_name = uuid.uuid4().get_hex()

        return {
            'title': 'Ахалай-Махалай',
            'region': self.region_name,
            'inn': '1234567890',
            'kpp': '654321',
            'legaladdress': 'Великая прекрасная Россия, дом 42',
            'phone': '6(666)666-66-66',
            'email': 'yandex@yandex.yandex',
            'payment_invoice_email': 'payment@yandex.yandex',
            'default_currency': 'RUB',
            'unilateral': 'Нет',
            'pa_prefix': 'СЛ',
        }

    def get_result(self):
        return RequiredResult(
            transition=IssueTransitions.wont_fix,
            assignee='autodasha',
            delay=False,
            comments="Указан регион '{}', отсутствующий в справочнике.".format(self.region_name))


class NewRegionCase(AbstractDBTestCase):
    _representation = 'new_region'

    def __init__(self):
        super(NewRegionCase, self).__init__()
        self.region = None

    def _get_data(self, session):
        region_params = uuid.uuid4()
        self.region = mapper.Country(
            region_id=region_params.int,
            region_name=region_params.get_hex(),
            region_name_en=region_params.get_hex(),
        )
        session.add(self.region)
        session.flush()

        return {
            'title': 'Ахалай-Махалай',
            'region': self.region.region_name,
            'inn': '1234567890',
            'kpp': '654321',
            'legaladdress': 'Великая прекрасная Россия, дом 42',
            'phone': '6(666)666-66-66',
            'email': 'yandex@yandex.yandex',
            'payment_invoice_email': 'payment@yandex.yandex',
            'default_currency': 'RUB',
            'unilateral': 'Нет',
            'pa_prefix': 'СЛ',
        }

    def get_result(self):
        return RequiredResult(
            transition=IssueTransitions.none,
            assignee='mscnad7',
            delay=False,
            comments="Указан регион '{}', в котором нет ни одной настроенной фирмы. "
                     "Автоматическое заведение не поддерживается.".format(self.region.region_name))


class RegionWithoutExportCase(AbstractDBTestCase):
    _representation = 'region_without_export'

    def __init__(self):
        super(RegionWithoutExportCase, self).__init__()
        self.region = None

    def _get_data(self, session):
        region_params = uuid.uuid4()
        self.region = mapper.Country(
            region_id=region_params.int,
            region_name=region_params.get_hex(),
            region_name_en=region_params.get_hex(),
        )
        session.add(self.region)
        session.flush()

        firm_params = uuid.uuid4()
        firm = mapper.Firm(
            id=firm_params.int,
            region_id=self.region.region_id,
            title=firm_params.get_hex(),
            email=firm_params.get_hex(),
            phone=firm_params.get_hex(),
            payment_invoice_email=firm_params.get_hex(),
            default_currency='RUR',
            default_iso_currency='RUB',
        )
        session.add(firm)
        session.flush()

        return {
            'title': 'Ахалай-Махалай',
            'region': self.region.region_name,
            'inn': '1234567890',
            'kpp': '654321',
            'legaladdress': 'Великая прекрасная Россия, дом 42',
            'phone': '6(666)666-66-66',
            'email': 'yandex@yandex.yandex',
            'payment_invoice_email': 'payment@yandex.yandex',
            'default_currency': 'RUB',
            'unilateral': 'Нет',
            'pa_prefix': 'СЛ',
            'oebs_org_id': 6,
            'oebs_user_id': 66,
            'oebs_perm_id': 666,
            'oebs_app_id': 6666,
            'oebs_tax_nds': 'НДС 30',
            'oebs_tax_free': 'Без НДС',
            'oebs_tax_free_resident': 'Без НДС (осв)',
        }

    def get_result(self):
        return RequiredResult(
            transition=IssueTransitions.none,
            assignee='mscnad7',
            delay=False,
            comments="Указан регион '{}', в котором нет ни одной выгружаемой в ОЕБС фирмы. "
                     "Автоматическое заведение не поддерживается.".format(self.region.region_name))


class RequiredParametersFirmCase(AbstractDBTestCase):
    _representation = 'required_parameters_firm'

    def _get_data(self, session):
        return {
            'title': 'Ахалай-Махалай',
            'region': 'Россия',
            'legaladdress': 'Великая прекрасная Россия, дом 42',
            'phone': '6(666)666-66-66',
            'email': 'yandex@yandex.yandex',
            'payment_invoice_email': 'payment@yandex.yandex',
            'default_currency': 'RUB',
            'unilateral': 'Нет',
            'pa_prefix': 'СЛ',
        }

    def get_result(self):
        return RequiredResult(
            transition=IssueTransitions.wont_fix,
            assignee='autodasha',
            delay=False,
            comments="Не указаны обязательные параметры фирмы: ИНН/Registration Number. "
                     "Заполните, пожалуйста, форму ещё раз.")


class RequiredParametersExportCase(AbstractDBTestCase):
    _representation = 'required_parameters_export'

    def _get_data(self, session):
        return {
            'title': 'Ахалай-Махалай',
            'region': 'Россия',
            'inn': '1234567890',
            'kpp': '654321',
            'legaladdress': 'Великая прекрасная Россия, дом 42',
            'phone': '6(666)666-66-66',
            'email': 'yandex@yandex.yandex',
            'payment_invoice_email': 'payment@yandex.yandex',
            'default_currency': 'RUB',
            'unilateral': 'Нет',
            'pa_prefix': 'СЛ',
            'oebs_org_id': 6,
            'oebs_user_id': 66,
            'oebs_app_id': 6666,
            'oebs_tax_nds': 'НДС 30',
            'oebs_tax_free': 'Без НДС',
        }

    def get_result(self):
        return RequiredResult(
            transition=IssueTransitions.wont_fix,
            assignee='autodasha',
            delay=False,
            comments="Не указаны обязательные параметры экспорта: Код налога ОЕБС для резидентов освобождённых от НДС, "
                     "OEBS_PERM_ID. Заполните, пожалуйста, форму ещё раз.")


class RequiredParametersBankCase(AbstractDBTestCase):
    _representation = 'required_parameters_bank'

    def _get_data(self, session):
        return {
            'title': 'Ахалай-Махалай',
            'region': 'Россия',
            'inn': '1234567890',
            'kpp': '654321',
            'legaladdress': 'Великая прекрасная Россия, дом 42',
            'phone': '6(666)666-66-66',
            'email': 'yandex@yandex.yandex',
            'payment_invoice_email': 'payment@yandex.yandex',
            'default_currency': 'RUB',
            'unilateral': 'Нет',
            'pa_prefix': 'СЛ',
            'bank_details': [
                {
                    'is_new_bank': True,
                    'address': 'Адрес 1',
                    'bik': '111111',
                    'swift': 'SWIFT1',
                    'currency': 'RUB',
                    'account': '11111111',
                    'bankaccount': '22222222',
                },
            ]
        }

    def get_result(self):
        return RequiredResult(
            transition=IssueTransitions.wont_fix,
            assignee='autodasha',
            delay=False,
            comments="Не указаны обязательные параметры нового банка: Название банка, Код ОЕБС. "
                     "Заполните, пожалуйста, форму ещё раз.")


class RequiredParametersBankDetailsCase(AbstractDBTestCase):
    _representation = 'required_parameters_bank_details'

    def __init__(self):
        super(RequiredParametersBankDetailsCase, self).__init__()
        self.bank = None

    def _get_data(self, session):
        self.bank = db_utils.create_payment_bank(
            session,
            address='Адрес банка',
            swift='swift',
            bik='111111',
            oebs_code='bank',
        )

        return {
            'title': 'Ахалай-Махалай',
            'region': 'Россия',
            'inn': '1234567890',
            'kpp': '654321',
            'legaladdress': 'Великая прекрасная Россия, дом 42',
            'phone': '6(666)666-66-66',
            'email': 'yandex@yandex.yandex',
            'payment_invoice_email': 'payment@yandex.yandex',
            'default_currency': 'RUB',
            'unilateral': 'Нет',
            'pa_prefix': 'СЛ',
            'bank_details': [
                {
                    'is_new_bank': False,
                    'bank': self.bank.name,
                    'bankaccount': '11111111',
                },
            ]
        }

    def get_result(self):
        return RequiredResult(
            transition=IssueTransitions.wont_fix,
            assignee='autodasha',
            delay=False,
            comments="Не указаны обязательные параметры банковских реквизитов: "
                     "Валюта банковского счёта, Расчётный счёт. Заполните, пожалуйста, форму ещё раз.")


class AlreadyHandedOverCase(AbstractDBTestCase):
    _representation = 'already_handed_over'

    def get_comments(self):
        return [('autodasha', 'Надо создать следующие объекты: Мир во всем мире.')]

    def _get_data(self, session):
        return {
            'title': 'Ахалай-Махалай',
            'region': 'Россия',
            'inn': '1234567890',
            'kpp': '654321',
            'legaladdress': 'Великая прекрасная Россия, дом 42',
            'phone': '6(666)666-66-66',
            'email': 'yandex@yandex.yandex',
            'payment_invoice_email': 'payment@yandex.yandex',
            'default_currency': 'RUB',
            'unilateral': 'Нет',
            'pa_prefix': 'СЛ',
        }

    def get_result(self):
        return RequiredResult(
            transition=IssueTransitions.wont_fix,
            assignee='autodasha',
            delay=False,
            commit=False,
            comments=None
        )


class UnhandlableCase(AbstractDBTestCase):
    _representation = 'unhandlable_case'

    def get_comments(self):
        return [('autodasha', 'У разработки Баланса кривые руки и нет нормальных архитекторов.'
                              'Автоматическое заведение не поддерживается.')]

    def _get_data(self, session):
        return {
            'title': 'Ахалай-Махалай',
            'region': 'Россия',
            'inn': '1234567890',
            'kpp': '654321',
            'legaladdress': 'Великая прекрасная Россия, дом 42',
            'phone': '6(666)666-66-66',
            'email': 'yandex@yandex.yandex',
            'payment_invoice_email': 'payment@yandex.yandex',
            'default_currency': 'RUB',
            'unilateral': 'Нет',
            'pa_prefix': 'СЛ',
        }

    def get_result(self):
        return RequiredResult(
            transition=IssueTransitions.wont_fix,
            assignee='autodasha',
            delay=False,
            commit=False,
            comments=None
        )


class InvalidFormCase(AbstractDBTestCase):
    _representation = 'invalid_form'

    def _get_data(self, session):
        return {
            'title': 'Ахалай-Махалай',
            'region': 'Россия',
            'inn': '1234567890',
            'kpp': '654321',
            'broken_line': 'Траляля',
        }

    def get_result(self):
        return RequiredResult(
            transition=IssueTransitions.wont_fix,
            assignee='autodasha',
            delay=False,
            commit=True,
            comments="Формат формы некорректен возле строки 'Говнокод к трону говнокода: Траляля'. "
                     "Заполните, пожалуйста, форму ещё раз."
        )


@pytest.mark.parametrize('issue_data',
                         [case() for case in AbstractDBTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['issue_data'])
def test_db(session, issue_data):
    queue_object, st_issue, case = issue_data

    solver = CreateSettingsFirm(queue_object, st_issue)
    with case.mock_bank_insertion():
        res = solver.solve()

    required_res = case.get_result()
    if required_res is None:
        assert res.commit is False
        assert res.delay is True
        assert res.issue_report is None
        return

    assert res.commit is required_res.commit
    assert res.delay is required_res.delay

    report = res.issue_report
    if required_res.comments is None:
        assert report is None
        return

    if isinstance(required_res.comments, basestring):
        assert required_res.comments == report.comment
    else:
        assert all(cmt in report.comment for cmt in required_res.comments)
    assert required_res.transition == report.transition
    assert required_res.assignee == report.assignee
    assert set(required_res.summonees) == set(report.summonees or [])

    if required_res.banks_inserted:
        assert len(case.bank_name2id) == required_res.banks_inserted

        bank_mappers = (
            session.query(mapper.PaymentBank)
                .filter(mapper.PaymentBank.id.in_(case.bank_name2id.values()))
                .all()
        )
        assert len(bank_mappers) == required_res.banks_inserted
        assert all(bm.name for bm in bank_mappers)
        assert all(bm.address for bm in bank_mappers)
        assert all(bm.swift for bm in bank_mappers)
        assert all(bm.bik for bm in bank_mappers)
