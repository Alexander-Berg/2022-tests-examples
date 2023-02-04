# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import uuid

import pytest

from autodasha.solver_cl.settings_creation.service_solver import CreateSettingsService
from autodasha.core.api.tracker import IssueTransitions

from tests.autodasha_tests.functional_tests import case_utils


class RequiredResult(object):
    __slots__ = ('comments', 'transition', 'assignee', 'delay', 'commit', 'summonees')

    def __init__(self, comments, delay=False, commit=True, transition=IssueTransitions.none, assignee='autodasha',
                 summonees=None):
        self.comments = comments
        self.transition = transition
        self.assignee = assignee
        self.delay = delay
        self.commit = commit
        self.summonees = summonees or []


class AbstractDBTestCase(case_utils.AbstractDBTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    summary = 'Заявка на заведение сервиса в Балансе: Доставка стульев вечером'

    _parameters = {
        'name': 'Краткое наименование сервиса: %s',
        'display_name': 'Полное наименование сервиса: %s',
        'notification_protocol': 'Тип нотификаций которые будут направляться подключаемому сервису: %s',
        'prod_url': 'Продовый URL подключаемого сервиса, на который следует направлять нотификации: %s',
        'test_url': 'Тестовый URL подключаемого сервиса, на который следует направлять нотификации: %s',
        'unilateral': 'Односторонние документы (документы не требующие подписи контрагента для признания): %s',
        'contract_needed': 'Выставление для агентств только по договорам при наличии действующих договоров: %s',
        'restrict_client': 'Выставление для прямых клиентов только по договорам при наличии действующих договоров: %s',
        'is_fiscal_agent': 'Являются ли все юрлица группы компаний Яндекс, от лица которых сервисом '
                           'будут оказываться услуги, агентом при приеме денег по выставляемым счетам: %s',
        'fiscal_url': 'URL который будет в чеке: %s',
        'fiscal_email': 'Email от лица которого будет вестись отправка чеков клиентам: %s',
        'broken_line': 'Говнокод к трону говнокода: %s',
    }
    _pp_parameters = {
        'firm': 'Юридическое лицо группы компаний Яндекс, которое будет оказывать услуги - %s',
        'currency': 'Валюта выставления счета - %s',
        'legal_entity': 'Тип плательщика который может выставить счет - %s',
        'payment_methods': 'Способы оплаты - %s',
        'is_non_resident': 'Является ли плательщик нерезидентом страны на территории которой зарегистрировано '
                           'юрлицо группы компаний Яндекс, которое будет оказывать услуги - %s',
    }

    def get_description(self, session):
        params = self._get_data(session)
        pay_policies = params.pop('pay_policies', None)

        lines = []
        for param, value in params.iteritems():
            lines.append(self._parameters[param] % value)
        if pay_policies:
            lines.append('Настройки оплат:')
            for pp in pay_policies:
                lines.append('Настройки оплат')
                for param, value in pp.iteritems():
                    lines.append(self._pp_parameters[param] % value)
        return '<#<pre wrap>%s</pre>#>' % '\n'.join(lines)


class BaseServicePayPolicyCase(AbstractDBTestCase):
    _representation = 'base_service_pay_policy'

    def _get_data(self, session):
        return {
            'name': 'Сервис',
            'display_name': 'Офигенный сервис',
            'notification_protocol': 'JSON',
            'prod_url': 'service.yandex',
            'test_url': 'test.service.yandex',
            'unilateral': 'Да',
            'contract_needed': 'Да',
            'restrict_client': 'Нет',
            'is_fiscal_agent': 'Да',
            'fiscal_url': 'fiscal.yandex',
            'fiscal_email': 'fiscal@service.yandex',
            'pay_policies': [
                {
                    'firm': 'ООО «Яндекс» (1)',
                    'currency': 'RUB',
                    'legal_entity': 'Любой',
                    'payment_methods': 'Яндекс.Деньги, Qiwi',
                },
                {
                    'firm': 'Yandex Europe AG',
                    'currency': 'EUR',
                    'legal_entity': 'Физлицо',
                    'payment_methods': 'Банк, Карта',
                }
            ]
        }

    def get_result(self):
        return RequiredResult(
            transition=IssueTransitions.none,
            assignee='mscnad7',
            comments='''
Надо создать следующие объекты:
Группа сервисов:
* ID: <service_id>
* Буквенный код: <service_cc>
* Наименование: Сервис
<{dml
%%(sql)
INSERT INTO meta.t_service_group (id, group_code, name, show, show_to_extern, intern)
VALUES (:service_id, :service_cc, 'Сервис', 1, 1, 0)
%%}>
Основные параметры сервиса:
* ID: <service_id>
* Буквенный код: <service_cc>
* Краткое наименование: Сервис
* Полное наименование: Офигенный сервис
* Группа сервисов: <service_id>
<{dml
%%(sql)
INSERT INTO meta.t_service (id, dt, cc, name, display_name, service_group_id)
VALUES (:service_id, sysdate, :service_cc, 'Сервис', 'Офигенный сервис', :service_id)
%%}>
Балансовые параметры сервиса:
* ID: <service_id>
* Выставление для агентств только по договорам: Да
* Выставление для клиентов только по договорам: Нет
* Односторонние документы: Да
<{dml
%%(sql)
INSERT INTO meta.t_balance_service (id, contract_needed, restrict_client, unilateral, catalog_id, intern, extra_pay, version, show_to_user, show_to_external_user, send_invoices, in_contract, is_auto_completion, shipment_auto, direct_payment, client_only)
VALUES (:service_id, 1, 0, 1, 0, 0, 1, 4, 1, 1, 0, 1, 1, 0, 0, 0)
%%}>
Настройки нотификаций сервиса:
* ID: <service_id>
* Продовый URL: service.yandex
* Тестовый URL: test.service.yandex
* Протокол: json-rest
<{dml
%%(sql)
INSERT INTO meta.t_service_notify_params (service_id, hidden, url, test_url, protocol, version, iface_version, path)
VALUES (:service_id, 0, 'service.yandex', 'test.service.yandex', 'json-rest', 0, 3, 'Yandex/Balance/XMLRPCProxy/Service3.id')
%%}>
Настройки чеков сервиса:
* ID: <service_id>
* Тип агентства: agent
* EMail: fiscal@service.yandex
* URL: fiscal.yandex
<{dml
%%(sql)
INSERT INTO meta.t_fiscal_service (id, fiscal_agent_type, email, url)
VALUES (:service_id, 'agent', 'fiscal@service.yandex', 'fiscal.yandex')
%%}>
Платежная политика:
* Регион: Россия
* Сервис: Сервис
* Фирма: ООО «Яндекс»
* Валюта: RUB
* Способы оплаты: qiwi_wallet, yamoney_wallet
<{dml
%%(sql)
INSERT ALL
  INTO bo.t_pay_policy_part (id, firm_id, category, legal_entity, description)
    VALUES (bo.s_pay_policy_part_id.nextval, 1, null, null, 'qiwi_wallet|RUB|0%yamoney_wallet|RUB|0')
  INTO bo.t_pay_policy_routing (service_id, region_id, pay_policy_part_id)
    VALUES (:service_id, 225, bo.s_pay_policy_part_id.nextval)
  INTO bo.t_pay_policy_paymethod (pay_policy_part_id, payment_method_id, iso_currency)
    VALUES (bo.s_pay_policy_part_id.nextval, 1203, 'RUB')
  INTO bo.t_pay_policy_paymethod (pay_policy_part_id, payment_method_id, iso_currency)
    VALUES (bo.s_pay_policy_part_id.nextval, 1201, 'RUB')
SELECT * FROM dual
%%}>
Платежная политика:
* Регион: Швейцария
* Сервис: Сервис
* Фирма: Yandex Europe AG
* Валюта: EUR
* Юрлицо: Нет
* Способы оплаты: bank, card
<{dml
%%(sql)
INSERT ALL
  INTO bo.t_pay_policy_part (id, firm_id, category, legal_entity, description)
    VALUES (bo.s_pay_policy_part_id.nextval, 7, null, 0, 'bank|EUR|0%card|EUR|0')
  INTO bo.t_pay_policy_routing (service_id, region_id, pay_policy_part_id)
    VALUES (:service_id, 126, bo.s_pay_policy_part_id.nextval)
  INTO bo.t_pay_policy_paymethod (pay_policy_part_id, payment_method_id, iso_currency)
    VALUES (bo.s_pay_policy_part_id.nextval, 1001, 'EUR')
  INTO bo.t_pay_policy_paymethod (pay_policy_part_id, payment_method_id, iso_currency)
    VALUES (bo.s_pay_policy_part_id.nextval, 1101, 'EUR')
SELECT * FROM dual
%%}>
'''.lstrip())


class NonResidentPayPolicyCase(AbstractDBTestCase):
    _representation = 'non_resident_pay_policy'

    def _get_data(self, session):
        return {
            'name': 'Сервис',
            'display_name': 'Офигенный сервис',
            'notification_protocol': 'JSON',
            'prod_url': 'service.yandex',
            'test_url': 'test.service.yandex',
            'unilateral': 'Да',
            'contract_needed': 'Нет',
            'restrict_client': 'Да',
            'pay_policies': [
                {
                    'firm': 'ООО «Яндекс» (1)',
                    'currency': 'RUB',
                    'legal_entity': 'Юрлицо',
                    'payment_methods': 'Яндекс.Деньги, Qiwi',
                },
                {
                    'firm': 'Yandex Europe AG (7)',
                    'currency': 'EUR',
                    'legal_entity': 'Физлицо',
                    'payment_methods': 'Банк, Карта',
                    'is_non_resident': 'Да',
                }
            ]
        }

    def get_result(self):
        return RequiredResult(
            transition=IssueTransitions.none,
            assignee='mscnad7',
            comments='''
Надо создать следующие объекты:
Группа сервисов:
* ID: <service_id>
* Буквенный код: <service_cc>
* Наименование: Сервис
<{dml
%%(sql)
INSERT INTO meta.t_service_group (id, group_code, name, show, show_to_extern, intern)
VALUES (:service_id, :service_cc, 'Сервис', 1, 1, 0)
%%}>
Основные параметры сервиса:
* ID: <service_id>
* Буквенный код: <service_cc>
* Краткое наименование: Сервис
* Полное наименование: Офигенный сервис
* Группа сервисов: <service_id>
<{dml
%%(sql)
INSERT INTO meta.t_service (id, dt, cc, name, display_name, service_group_id)
VALUES (:service_id, sysdate, :service_cc, 'Сервис', 'Офигенный сервис', :service_id)
%%}>
Балансовые параметры сервиса:
* ID: <service_id>
* Выставление для агентств только по договорам: Нет
* Выставление для клиентов только по договорам: Да
* Односторонние документы: Да
<{dml
%%(sql)
INSERT INTO meta.t_balance_service (id, contract_needed, restrict_client, unilateral, catalog_id, intern, extra_pay, version, show_to_user, show_to_external_user, send_invoices, in_contract, is_auto_completion, shipment_auto, direct_payment, client_only)
VALUES (:service_id, 0, 1, 1, 0, 0, 1, 4, 1, 1, 0, 1, 1, 0, 0, 0)
%%}>
Настройки нотификаций сервиса:
* ID: <service_id>
* Продовый URL: service.yandex
* Тестовый URL: test.service.yandex
* Протокол: json-rest
<{dml
%%(sql)
INSERT INTO meta.t_service_notify_params (service_id, hidden, url, test_url, protocol, version, iface_version, path)
VALUES (:service_id, 0, 'service.yandex', 'test.service.yandex', 'json-rest', 0, 3, 'Yandex/Balance/XMLRPCProxy/Service3.id')
%%}>
Платежная политика:
* Регион: Россия
* Сервис: Сервис
* Фирма: ООО «Яндекс»
* Валюта: RUB
* Юрлицо: Да
* Способы оплаты: qiwi_wallet, yamoney_wallet
<{dml
%%(sql)
INSERT ALL
  INTO bo.t_pay_policy_part (id, firm_id, category, legal_entity, description)
    VALUES (bo.s_pay_policy_part_id.nextval, 1, null, 1, 'qiwi_wallet|RUB|0%yamoney_wallet|RUB|0')
  INTO bo.t_pay_policy_routing (service_id, region_id, pay_policy_part_id)
    VALUES (:service_id, 225, bo.s_pay_policy_part_id.nextval)
  INTO bo.t_pay_policy_paymethod (pay_policy_part_id, payment_method_id, iso_currency)
    VALUES (bo.s_pay_policy_part_id.nextval, 1203, 'RUB')
  INTO bo.t_pay_policy_paymethod (pay_policy_part_id, payment_method_id, iso_currency)
    VALUES (bo.s_pay_policy_part_id.nextval, 1201, 'RUB')
SELECT * FROM dual
%%}>

Также обратите внимание, что следующие настройки нужно дополнительно обработать вручную:
* Платежные политики для нерезидентов;
'''.lstrip())


class OtherPaymentMethodPayPolicyCase(AbstractDBTestCase):
    _representation = 'other_payment_method'

    def _get_data(self, session):
        return {
            'name': 'Сервис',
            'display_name': 'Офигенный сервис',
            'notification_protocol': 'JSON',
            'prod_url': 'service.yandex',
            'test_url': 'test.service.yandex',
            'unilateral': 'Да',
            'contract_needed': 'Нет',
            'restrict_client': 'Да',
            'pay_policies': [
                {
                    'firm': 'ООО «Яндекс» (1)',
                    'currency': 'RUB',
                    'legal_entity': 'Юрлицо',
                    'payment_methods': 'Яндекс.Деньги, Qiwi, Другое',
                },
                {
                    'firm': 'Yandex Europe AG (7)',
                    'currency': 'EUR',
                    'legal_entity': 'Физлицо',
                    'payment_methods': 'Банк, Карта',
                    'is_non_resident': 'Да',
                }
            ]
        }

    def get_result(self):
        return RequiredResult(
            transition=IssueTransitions.none,
            assignee='mscnad7',
            comments='''
Надо создать следующие объекты:
Группа сервисов:
* ID: <service_id>
* Буквенный код: <service_cc>
* Наименование: Сервис
<{dml
%%(sql)
INSERT INTO meta.t_service_group (id, group_code, name, show, show_to_extern, intern)
VALUES (:service_id, :service_cc, 'Сервис', 1, 1, 0)
%%}>
Основные параметры сервиса:
* ID: <service_id>
* Буквенный код: <service_cc>
* Краткое наименование: Сервис
* Полное наименование: Офигенный сервис
* Группа сервисов: <service_id>
<{dml
%%(sql)
INSERT INTO meta.t_service (id, dt, cc, name, display_name, service_group_id)
VALUES (:service_id, sysdate, :service_cc, 'Сервис', 'Офигенный сервис', :service_id)
%%}>
Балансовые параметры сервиса:
* ID: <service_id>
* Выставление для агентств только по договорам: Нет
* Выставление для клиентов только по договорам: Да
* Односторонние документы: Да
<{dml
%%(sql)
INSERT INTO meta.t_balance_service (id, contract_needed, restrict_client, unilateral, catalog_id, intern, extra_pay, version, show_to_user, show_to_external_user, send_invoices, in_contract, is_auto_completion, shipment_auto, direct_payment, client_only)
VALUES (:service_id, 0, 1, 1, 0, 0, 1, 4, 1, 1, 0, 1, 1, 0, 0, 0)
%%}>
Настройки нотификаций сервиса:
* ID: <service_id>
* Продовый URL: service.yandex
* Тестовый URL: test.service.yandex
* Протокол: json-rest
<{dml
%%(sql)
INSERT INTO meta.t_service_notify_params (service_id, hidden, url, test_url, protocol, version, iface_version, path)
VALUES (:service_id, 0, 'service.yandex', 'test.service.yandex', 'json-rest', 0, 3, 'Yandex/Balance/XMLRPCProxy/Service3.id')
%%}>
Платежная политика:
* Регион: Россия
* Сервис: Сервис
* Фирма: ООО «Яндекс»
* Валюта: RUB
* Юрлицо: Да
* Способы оплаты: qiwi_wallet, yamoney_wallet
<{dml
%%(sql)
INSERT ALL
  INTO bo.t_pay_policy_part (id, firm_id, category, legal_entity, description)
    VALUES (bo.s_pay_policy_part_id.nextval, 1, null, 1, 'qiwi_wallet|RUB|0%yamoney_wallet|RUB|0')
  INTO bo.t_pay_policy_routing (service_id, region_id, pay_policy_part_id)
    VALUES (:service_id, 225, bo.s_pay_policy_part_id.nextval)
  INTO bo.t_pay_policy_paymethod (pay_policy_part_id, payment_method_id, iso_currency)
    VALUES (bo.s_pay_policy_part_id.nextval, 1203, 'RUB')
  INTO bo.t_pay_policy_paymethod (pay_policy_part_id, payment_method_id, iso_currency)
    VALUES (bo.s_pay_policy_part_id.nextval, 1201, 'RUB')
SELECT * FROM dual
%%}>

Также обратите внимание, что следующие настройки нужно дополнительно обработать вручную:
* Платежные политики для нерезидентов;
* Платежные политики, где указан метод оплаты 'Другой';
'''.lstrip())


class OnlyOtherPaymentMethodsCase(AbstractDBTestCase):
    _representation = 'only_other_payment_method'

    def _get_data(self, session):
        return {
            'name': 'Сервис',
            'display_name': 'Офигенный сервис',
            'notification_protocol': 'JSON',
            'prod_url': 'service.yandex',
            'test_url': 'test.service.yandex',
            'pay_policies': [
                {
                    'firm': 'ООО «Яндекс» (1)',
                    'currency': 'RUB',
                    'legal_entity': 'Юрлицо',
                    'payment_methods': 'Другое',
                },
            ]
        }

    def get_result(self):
        return RequiredResult(
            transition=IssueTransitions.none,
            assignee='mscnad7',
            comments='''
Надо создать следующие объекты:
Группа сервисов:
* ID: <service_id>
* Буквенный код: <service_cc>
* Наименование: Сервис
<{dml
%%(sql)
INSERT INTO meta.t_service_group (id, group_code, name, show, show_to_extern, intern)
VALUES (:service_id, :service_cc, 'Сервис', 1, 1, 0)
%%}>
Основные параметры сервиса:
* ID: <service_id>
* Буквенный код: <service_cc>
* Краткое наименование: Сервис
* Полное наименование: Офигенный сервис
* Группа сервисов: <service_id>
<{dml
%%(sql)
INSERT INTO meta.t_service (id, dt, cc, name, display_name, service_group_id)
VALUES (:service_id, sysdate, :service_cc, 'Сервис', 'Офигенный сервис', :service_id)
%%}>
Балансовые параметры сервиса:
* ID: <service_id>
* Выставление для агентств только по договорам: Нет
* Выставление для клиентов только по договорам: Нет
* Односторонние документы: Нет
<{dml
%%(sql)
INSERT INTO meta.t_balance_service (id, contract_needed, restrict_client, unilateral, catalog_id, intern, extra_pay, version, show_to_user, show_to_external_user, send_invoices, in_contract, is_auto_completion, shipment_auto, direct_payment, client_only)
VALUES (:service_id, 0, 0, 0, 0, 0, 1, 4, 1, 1, 0, 1, 1, 0, 0, 0)
%%}>
Настройки нотификаций сервиса:
* ID: <service_id>
* Продовый URL: service.yandex
* Тестовый URL: test.service.yandex
* Протокол: json-rest
<{dml
%%(sql)
INSERT INTO meta.t_service_notify_params (service_id, hidden, url, test_url, protocol, version, iface_version, path)
VALUES (:service_id, 0, 'service.yandex', 'test.service.yandex', 'json-rest', 0, 3, 'Yandex/Balance/XMLRPCProxy/Service3.id')
%%}>

Также обратите внимание, что следующие настройки нужно дополнительно обработать вручную:
* Платежные политики, где указан метод оплаты 'Другой';
'''.lstrip())


class NoPaymentMethods(AbstractDBTestCase):
    _representation = 'no_payment_methods'

    def _get_data(self, session):
        return {
            'name': 'Сервис',
            'display_name': 'Офигенный сервис',
            'notification_protocol': 'JSON',
            'prod_url': 'service.yandex',
            'test_url': 'test.service.yandex',
            'pay_policies': [
                {
                    'firm': 'ООО «Яндекс» (1)',
                    'currency': 'RUB',
                    'legal_entity': 'Юрлицо',
                    'payment_methods': '',
                },
            ]
        }

    def get_result(self):
        return RequiredResult(
            transition=IssueTransitions.wont_fix,
            assignee='autodasha',
            comments='Не указаны методы оплаты для платежной политики.'
        )


class IncorrectFirmCase(AbstractDBTestCase):
    _representation = 'incorrect_firm'

    def __init__(self):
        self.firm_name = None

    def _get_data(self, session):
        self.firm_name = uuid.uuid4().get_hex()

        return {
            'name': 'Сервис',
            'display_name': 'Офигенный сервис',
            'notification_protocol': 'JSON',
            'prod_url': 'service.yandex',
            'test_url': 'test.service.yandex',
            'unilateral': 'Да',
            'contract_needed': 'Нет',
            'restrict_client': 'Да',
            'pay_policies': [
                {
                    'firm': self.firm_name,
                    'currency': 'RUB',
                    'legal_entity': 'Юрлицо',
                    'payment_methods': 'Банк',
                },
            ]
        }

    def get_result(self):
        return RequiredResult(
            transition=IssueTransitions.wont_fix,
            assignee='autodasha',
            comments="Указана фирма '{}', отсутствующая в справочнике.".format(self.firm_name))


class RequiredParametersServiceCase(AbstractDBTestCase):
    _representation = 'required_parameters_service'

    def _get_data(self, session):
        return {
            'name': 'Сервис',
            'notification_protocol': 'JSON',
            'prod_url': 'service.yandex',
        }

    def get_result(self):
        return RequiredResult(
            transition=IssueTransitions.wont_fix,
            assignee='autodasha',
            comments="Не указаны обязательные параметры сервиса: "
                     "Полное наименование сервиса, "
                     "Тестовый URL подключаемого сервиса, на который следует направлять нотификации. "
                     "Заполните, пожалуйста, форму ещё раз.")


class RequiredParametersFiscalCase(AbstractDBTestCase):
    _representation = 'required_fiscal_parameters'

    def _get_data(self, session):
        return {
            'name': 'Сервис',
            'display_name': 'Офигенный сервис',
            'notification_protocol': 'JSON',
            'prod_url': 'service.yandex',
            'test_url': 'test.service.yandex',
            'unilateral': 'Да',
            'contract_needed': 'Нет',
            'restrict_client': 'Да',
            'fiscal_url': 'fiscal.yandex',
        }

    def get_result(self):
        return RequiredResult(
            transition=IssueTransitions.wont_fix,
            assignee='autodasha',
            comments="Не указаны обязательные фискальные настройки сервиса: "
                     "Являются ли все юрлица группы компаний Яндекс, от лица которых сервисом будут оказываться услуги, "
                     "агентом при приеме денег по выставляемым счетам, "
                     "Email от лица которого будет вестись отправка чеков клиентам. "
                     "Заполните, пожалуйста, форму ещё раз.")


class RequiredParametersPayPolicyCase(AbstractDBTestCase):
    _representation = 'required_pay_policy_parameters'

    def _get_data(self, session):
        return {
            'name': 'Сервис',
            'display_name': 'Офигенный сервис',
            'notification_protocol': 'JSON',
            'prod_url': 'service.yandex',
            'test_url': 'test.service.yandex',
            'unilateral': 'Да',
            'contract_needed': 'Нет',
            'restrict_client': 'Да',
            'pay_policies': [
                {
                    'currency': 'RUB',
                    'legal_entity': 'Юрлицо',
                },
            ]
        }

    def get_result(self):
        return RequiredResult(
            transition=IssueTransitions.wont_fix,
            assignee='autodasha',
            comments="Не указаны обязательные настройки платёжной политики: "
                     "Юридическое лицо группы компаний Яндекс, которое будет оказывать услуги, "
                     "Способы оплаты. "
                     "Заполните, пожалуйста, форму ещё раз.")


class AlreadyHandedOverCase(AbstractDBTestCase):
    _representation = 'already_handed_over'

    def get_comments(self):
        return [('autodasha', 'Надо создать следующие объекты: Мир во всем мире.')]

    def _get_data(self, session):
        return {
            'name': 'Сервис',
            'display_name': 'Офигенный сервис',
            'notification_protocol': 'JSON',
            'prod_url': 'service.yandex',
            'test_url': 'test.service.yandex',
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
            'name': 'Сервис',
            'display_name': 'Офигенный сервис',
            'notification_protocol': 'JSON',
            'prod_url': 'service.yandex',
            'test_url': 'test.service.yandex',
            'broken_line': 'Привет!'
        }

    def get_result(self):
        return RequiredResult(
            transition=IssueTransitions.wont_fix,
            assignee='autodasha',
            delay=False,
            commit=True,
            comments="Формат формы некорректен возле строки 'Говнокод к трону говнокода: Привет!'. "
                     "Заполните, пожалуйста, форму ещё раз."
        )


@pytest.mark.parametrize('issue_data',
                         [case() for case in AbstractDBTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['issue_data'])
def test_db(session, issue_data):
    queue_object, st_issue, case = issue_data

    solver = CreateSettingsService(queue_object, st_issue)
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
