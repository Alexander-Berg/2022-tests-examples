# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import datetime as dt

import pytest
from sqlalchemy import orm

import balance.mapper as mapper
import balance.muzzle_util as ut

from autodasha.comments.change_person import ChangePersonCommentsManager
from autodasha.db import mapper as a_mapper
from autodasha.core.api.tracker import IssueTransitions, IssueStatuses
from autodasha.solver_cl import ChangePerson

from tests.autodasha_tests.functional_tests import case_utils
from tests.autodasha_tests.functional_tests import mock_utils
from tests.autodasha_tests.common import db_utils


@pytest.fixture
def comment_manager():
    return ChangePersonCommentsManager()


class AbstractMockTestCase(case_utils.AbstractMockTestCase):
    _default_lines = {
        'summary': 'Изменение плательщика в счетах',
        'invoices': '№ счета: %s',
        'person': 'Название плательщика (на которого надо поменять) и его ID: %s (ID: %s)',
        'paysys': 'Способ оплат: %s',
        'reason': 'Причина изменения: %s'
    }


class AbstractChangeMockTestCase(AbstractMockTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class InvalidFormCase(AbstractChangeMockTestCase):
    _representation = 'invalid_form'

    def get_data(self, mock_manager):
        client = mock_utils.create_client(mock_manager)
        person = mock_utils.create_person(mock_manager, client)
        invoice = mock_utils.create_invoice(mock_manager, client)
        paysys = mock_utils.create_paysys(mock_manager)

        lines = [
            self._get_default_line(invoices=invoice.external_id),
            'Назвааа ВО ИМЯ САТАНЫ: %s ID: %s' % (person.name, person.id),
            self._get_default_line(paysys=paysys.name),
            self._get_default_line(reason='тапочки')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Поля заполнены некорректно. Уточни данные и заполни форму еще раз.',\
               IssueTransitions.wont_fix


class InvalidPersonIdCase(AbstractChangeMockTestCase):
    _representation = 'invalid_person_id'

    def get_data(self, mock_manager):
        client = mock_utils.create_client(mock_manager)
        person = mock_utils.create_person(mock_manager, client, id_='траляля')
        invoice = mock_utils.create_invoice(mock_manager, client)
        paysys = mock_utils.create_paysys(mock_manager)

        lines = [
            self._get_default_line(invoices=invoice.external_id),
            self._get_default_line(person=(person.name, person.id)),
            self._get_default_line(paysys=paysys.name),
            self._get_default_line(reason='мыло')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Некорректно указан ID плательщика. Пожалуйста, заполни форму с верными данными еще раз.',\
               IssueTransitions.wont_fix


class NonexistentPersonCase(AbstractChangeMockTestCase):
    _representation = 'nonexistent_person'

    def get_data(self, mock_manager):
        client = mock_utils.create_client(mock_manager)
        person = mock_utils.create_person(mock_manager, client)
        invoice = mock_utils.create_invoice(mock_manager, client)
        paysys = mock_utils.create_paysys(mock_manager)

        lines = [
            self._get_default_line(invoices=invoice.external_id),
            self._get_default_line(person=(person.name, 667)),
            self._get_default_line(paysys=paysys.name),
            self._get_default_line(reason='полотенцо')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Некорректно указан ID плательщика. Пожалуйста, заполни форму с верными данными еще раз.',\
               IssueTransitions.wont_fix


class IsPartnerPersonCase(AbstractChangeMockTestCase):
    _representation = 'person_is_partner'

    def __init__(self):
        super(IsPartnerPersonCase, self).__init__()
        self._person = None

    def get_data(self, mock_manager):
        client = mock_utils.create_client(mock_manager)
        self._person = mock_utils.create_person(mock_manager, client, is_partner=1)
        invoice = mock_utils.create_invoice(mock_manager, client, person=self._person)
        paysys = mock_utils.create_paysys(mock_manager)

        lines = [
            self._get_default_line(invoices=invoice.external_id),
            self._get_default_line(person=(self._person.name, self._person.id)),
            self._get_default_line(paysys=paysys.name),
            self._get_default_line(reason='верёвка')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Провести изменения плательщика на %s ID %s не можем.' \
               ' Плательщик с типом РСЯ не может быть указан в данном счете.' \
               ' Поправь id плательщика и перепиши задачу через форму' % (self._person.name, self._person.id),\
               IssueTransitions.wont_fix


class InvalidInvoiceEIDCase(AbstractChangeMockTestCase):
    _representation = 'invalid_eid'

    def get_data(self, mock_manager):
        client = mock_utils.create_client(mock_manager)
        person = mock_utils.create_person(mock_manager, client)
        invoice = mock_utils.create_invoice(mock_manager, client, 'Б-ЛЯЛЯЛЯ')
        paysys = mock_utils.create_paysys(mock_manager)

        lines = [
            self._get_default_line(invoices=invoice.external_id),
            self._get_default_line(person=(person.name, person.id)),
            self._get_default_line(paysys=paysys.name),
            self._get_default_line(reason='водка')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Не указано ни одного счета', IssueTransitions.wont_fix


class NoInvoicesCase(AbstractChangeMockTestCase):
    _representation = 'no_invoices'

    def get_data(self, mock_manager):
        client = mock_utils.create_client(mock_manager)
        person = mock_utils.create_person(mock_manager, client)
        paysys = mock_utils.create_paysys(mock_manager)

        lines = [
            self._get_default_line(invoices=''),
            self._get_default_line(person=(person.name, person.id)),
            self._get_default_line(paysys=paysys.name),
            self._get_default_line(reason='сигареты')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Не указано ни одного счета', IssueTransitions.wont_fix


class NonexistentInvoiceCase(AbstractChangeMockTestCase):
    _representation = 'nonexistent_invoice'

    def get_data(self, mock_manager):
        client = mock_utils.create_client(mock_manager)
        person = mock_utils.create_person(mock_manager, client)
        invoice = mock_utils.create_invoice(mock_manager, client, 'Б-66666-6')
        paysys = mock_utils.create_paysys(mock_manager)

        lines = [
            self._get_default_line(invoices='Б-66666-7'),
            self._get_default_line(person=(person.name, person.id)),
            self._get_default_line(paysys=paysys.name),
            self._get_default_line(reason='спички')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Некорректно указан № счета (Б-66666-7). Пожалуйста, заполни форму с верными данными еще раз',\
               IssueTransitions.wont_fix


class MultipleFirmsInvoicesCase(AbstractChangeMockTestCase):
    _representation = 'multiple_firms'

    def get_data(self, mock_manager):
        client = mock_utils.create_client(mock_manager)
        person = mock_utils.create_person(mock_manager, client)
        invoice1 = mock_utils.create_invoice(mock_manager, client, 'Б-66666-1', firm_id=1)
        invoice2 = mock_utils.create_invoice(mock_manager, client, 'Б-66666-2', firm_id=2)
        paysys = mock_utils.create_paysys(mock_manager)

        lines = [
            self._get_default_line(invoices='%s, %s' % (invoice1.external_id, invoice2.external_id)),
            self._get_default_line(person=(person.name, person.id)),
            self._get_default_line(paysys=paysys.name),
            self._get_default_line(reason='патроны')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Счета относятся к разным фирмам. Пожалуйста, сгруппируй счета по фирме' \
               ' и заполни форму для каждой фирмы отдельно',\
               IssueTransitions.wont_fix


class IncorrectPersonClientCase(AbstractChangeMockTestCase):
    _representation = 'incorrect_person_client'

    def __init__(self):
        super(IncorrectPersonClientCase, self).__init__()
        self._person = None
        self._invoice = None

    def get_data(self, mock_manager):
        i_client = mock_utils.create_client(mock_manager, id_=666)
        i_person = mock_utils.create_person(mock_manager, i_client, id_=666666)
        p_client = mock_utils.create_client(mock_manager, id_=667)
        self._person = mock_utils.create_person(mock_manager, p_client, id_=6666667)
        self._invoice = mock_utils.create_invoice(mock_manager, i_client, person=i_person)
        paysys = mock_utils.create_paysys(mock_manager)

        lines = [
            self._get_default_line(invoices=self._invoice.external_id),
            self._get_default_line(person=(self._person.name, self._person.id)),
            self._get_default_line(paysys=paysys.name),
            self._get_default_line(reason='бинты')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        client = self._invoice.client
        fmt_data = (self._person.name, self._person.id, client.name, client.id, self._invoice.external_id)
        return 'Плательщик %s ID %s не привязан к клиенту %s ID %s из счета %s.' \
               ' Перепиши пожалуйста задачу через форму с корректными данными' % fmt_data,\
               IssueTransitions.wont_fix


class InvoiceTypeCase(AbstractChangeMockTestCase):
    _type = ''

    def __init__(self):
        super(InvoiceTypeCase, self).__init__()
        self._invoice = None

    def get_data(self, mock_manager):
        client = mock_utils.create_client(mock_manager)
        person = mock_utils.create_person(mock_manager, client)
        self._invoice = mock_utils.create_invoice(mock_manager, client, type=self._type)
        paysys = mock_utils.create_paysys(mock_manager)

        lines = [
            self._get_default_line(invoices=self._invoice.external_id),
            self._get_default_line(person=(person.name, person.id)),
            self._get_default_line(paysys=paysys.name),
            self._get_default_line(reason='Великий Яндекс')
        ]
        return self._get_default_line('summary'), lines


class PersonalAccountCase(InvoiceTypeCase):
    _representation = 'personal_account'
    _type = 'personal_account'

    def get_result(self):
        return (
                   'В лицевых счетах ({}) изменение плательщика автоматически не производится. '
                   'Если по лицевому счету не было оплат (поступлений) и клиент готов в дальнейшем '
                   'оплачивать только указанным способом, обратитесь в поддержку '
                   'через ((https://wiki.yandex-team.ru/otdelsoprovozhdeniya/dokument/avtomatformi/ общую форму)).'
               ).format(self._invoice.external_id), IssueTransitions.wont_fix


class FictivePersonalAccountCase(PersonalAccountCase):
    _representation = 'fictive_personal_account'
    _type = 'fictive_personal_account'


class YInvoiceCase(InvoiceTypeCase):
    _representation = 'y_invoice'
    _type = 'y_invoice'

    def get_result(self):
        return (
                   'Тип счета {} не обрабатывается автоматически. '
                   'Для изменения плательщика в счете обратитесь в поддержку '
                   'через ((https://wiki.yandex-team.ru/otdelsoprovozhdeniya/dokument/avtomatformi/ общую форму)).'
               ).format(self._invoice.external_id), IssueTransitions.wont_fix


class CompensationCase(AbstractChangeMockTestCase):
    _representation = 'compensation'

    def __init__(self):
        super(CompensationCase, self).__init__()
        self._invoice = None

    def get_data(self, mock_manager):
        client = mock_utils.create_client(mock_manager)
        person = mock_utils.create_person(mock_manager, client)
        invoice_paysys = mock_utils.create_paysys(mock_manager, id_=1006, certificate=1)
        self._invoice = mock_utils.create_invoice(mock_manager, client, paysys=invoice_paysys)
        paysys = mock_utils.create_paysys(mock_manager)

        lines = [
            self._get_default_line(invoices=self._invoice.external_id),
            self._get_default_line(person=(person.name, person.id)),
            self._get_default_line(paysys=paysys.name),
            self._get_default_line(reason='альма матер')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return (
                   'Тип счета {} не обрабатывается автоматически. '
                   'Для изменения плательщика в счете обратитесь в поддержку '
                   'через ((https://wiki.yandex-team.ru/otdelsoprovozhdeniya/dokument/avtomatformi/ общую форму)).'
               ).format(self._invoice.external_id), IssueTransitions.wont_fix


class ContractCase(AbstractChangeMockTestCase):
    _representation = 'has_contract'

    def __init__(self):
        super(ContractCase, self).__init__()
        self._invoice = None

    def get_data(self, mock_manager):
        client = mock_utils.create_client(mock_manager)
        person = mock_utils.create_person(mock_manager, client)
        contract = mock_manager.create_object(mapper.Contract, id=666, external_id='666/66')
        self._invoice = mock_utils.create_invoice(mock_manager, client, contract=contract)
        paysys = mock_utils.create_paysys(mock_manager)

        lines = [
            self._get_default_line(invoices=self._invoice.external_id),
            self._get_default_line(person=(person.name, person.id)),
            self._get_default_line(paysys=paysys.name),
            self._get_default_line(reason='распил и откат')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return (
                   'Счет {} привязан к договору. В автоматическом режиме произвести изменения невозможно. '
                   'Для изменения плательщика в счете обратитесь в поддержку '
                   'через ((https://wiki.yandex-team.ru/otdelsoprovozhdeniya/dokument/avtomatformi/ общую форму)).'
               ).format(self._invoice.external_id), IssueTransitions.wont_fix


class ExportEnqueuedCase(AbstractChangeMockTestCase):
    _representation = 'export_enqueued'

    def __init__(self):
        super(ExportEnqueuedCase, self).__init__()
        self._invoice = None

    def get_data(self, mock_manager):
        client = mock_utils.create_client(mock_manager)
        person = mock_utils.create_person(mock_manager, client)
        self._invoice = mock_utils.create_invoice(mock_manager, client)
        paysys = mock_utils.create_paysys(mock_manager)

        lines = [
            self._get_default_line(invoices=self._invoice.external_id),
            self._get_default_line(person=(person.name, person.id)),
            self._get_default_line(paysys=paysys.name),
            self._get_default_line(reason='распил и откат')
        ]
        return self._get_default_line('summary'), lines

    def get_comments(self):
        msg = 'Плательщик в счете %s изменен на Вася Пупкин (666) - Натурой. ' \
              'Изменённые счета и акты добавлены в очередь на выгрузку в ОЕБС.' % self._invoice.external_id
        return [('autodasha', msg)]

    def get_result(self):
        return None


class AlreadySolvedCase(AbstractChangeMockTestCase):
    _representation = 'already_solved'

    last_resolved = dt.datetime.now() - dt.timedelta(hours=1)

    def __init__(self):
        super(AlreadySolvedCase, self).__init__()
        self._invoice = None

    def get_data(self, mock_manager):
        client = mock_utils.create_client(mock_manager)
        person = mock_utils.create_person(mock_manager, client)
        self._invoice = mock_utils.create_invoice(mock_manager, client)
        paysys = mock_utils.create_paysys(mock_manager)

        lines = [
            self._get_default_line(invoices=self._invoice.external_id),
            self._get_default_line(person=(person.name, person.id)),
            self._get_default_line(paysys=paysys.name),
            self._get_default_line(reason='распил и откат')
        ]
        return self._get_default_line('summary'), lines

    def get_comments(self):
        msg = 'Плательщик в счете %s изменен на Вася Пупкин (666) - Натурой. ' \
              'Изменённые счета и акты добавлены в очередь на выгрузку в ОЕБС.' % self._invoice.external_id
        return [('autodasha', msg), ('autodasha', 'Всё выгрузилось, слава КПСС!')]

    def get_result(self):
        return (
            'Эта задача уже была выполнена. Направьте новый запрос через '
            '((https://wiki.yandex-team.ru/otdelsoprovozhdeniya/dokument/avtomatformi/ формы)). '
            'Если не найдёте подходящую, заполните, пожалуйста, общую форму.'
        ), IssueTransitions.fixed, False


@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in AbstractChangeMockTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_simple(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data

    req_res = case.get_result()

    solver = ChangePerson(mock_queue_object, issue)
    res = solver.solve()

    if req_res is None:
        assert res.commit is False
        assert res.delay is False
        assert res.issue_report is None
    else:
        if len(req_res) == 3:
            req_comment, req_transition, req_commit = req_res
        else:
            req_comment, req_transition = req_res
            req_commit = True

        assert res.commit is req_commit
        assert res.delay is False
        report = res.issue_report

        assert req_comment in report.comment
        assert report.transition == req_transition
        assert report.assignee == 'autodasha'


class ChangeRequiredResult(case_utils.RequiredResult):
    def __init__(self, **kwargs):
        self.invoices_states = []
        self.reexported_invoices = []
        self.delay = kwargs.get('frozen', False)
        super(ChangeRequiredResult, self).__init__(**kwargs)

    def set_object_states(self, unchanged=[], **kwargs):
        for invoice in unchanged:
            self.add_unchanged_state(invoice)

    def add_unchanged_state(self, invoice):
        self.invoices_states.append((invoice, invoice.person_id, invoice.paysys_id, invoice.bank_details.bank_id,
                                     invoice.total_act_sum, {a.id for a in invoice.acts}))

    def add_changed_person_state(self, invoice, person, paysys, sum_=None, bank_ids=None):
        self.invoices_states.append((invoice, person.id, paysys.id, bank_ids or [invoice.bank_details.bank_id], sum_ or invoice.total_act_sum, set()))
        self.reexported_invoices.append(invoice)

    def add_changed_paysys_state(self, invoice, paysys):
        self.invoices_states.append((invoice, invoice.person_id, paysys.id, invoice.bank_details.bank_id,
                                     invoice.total_act_sum, {a.id for a in invoice.acts}))

    def set_messages(self, changed_paysys=[], changed_person=[], enqueue=False, frozen=False, **kwargs):
        for invoice, paysys in changed_paysys:
            self.add_changed_paysys_message(invoice, paysys)

        for row in changed_person:
            self.add_changed_person_message(*row)

        if enqueue:
            self.add_enq_message()

        if frozen:
            self.add_frozen_message()

    def add_changed_paysys_message(self, invoice, paysys):
        self.add_message('Способ оплат в счете %s изменен на %s.' % (invoice.external_id, paysys.name))
        self.add_changed_paysys_state(invoice, paysys)

    def add_changed_person_message(self, invoice, person, paysys, *args):
        txt = 'Плательщик в счете %s изменен на %s (%s) - %s.' \
              % (invoice.external_id, person.name, person.id, paysys.name)
        self.add_message(txt)
        self.add_changed_person_state(invoice, person, paysys, *args)

    def add_person_paysys_mismatch_message(self, paysys, person):
        txt = 'Способ оплат - %s не соответствует указанному плательщику %s (ID %s). ' \
              'Пожалуйста заполни форму с верными данными еще раз.' \
              % (paysys.name, person.name, person.id)
        self.add_message(txt)

    def add_check_receipt_sum_message(self, invoice):
        txt = (
            'Счет %s не обрабатывается автоматически. '
            'Для изменения плательщика в счете обратитесь в поддержку '
            'через ((https://wiki.yandex-team.ru/otdelsoprovozhdeniya/dokument/avtomatformi/ общую форму)).'
               % invoice.external_id)
        self.add_message(txt)
        self.add_unchanged_state(invoice)

    def add_check_acts_sum_1c_message(self, invoice):
        txt = ('По счету %s нет актов и платежей - изменение плательщика можно произвести самостоятельно, '
               'либо обратившись в бэк-офис, написав на рассылку payment-invoice@.'
               % invoice.external_id)
        self.add_message(txt)
        self.add_unchanged_state(invoice)

    def add_check_contract_message(self, invoice):
        txt = (
                'Счет %s привязан к договору. В автоматическом режиме произвести изменения невозможно. '
                'Для изменения плательщика в счете обратитесь в поддержку '
                'через ((https://wiki.yandex-team.ru/otdelsoprovozhdeniya/dokument/avtomatformi/ общую форму)).'
                    % invoice.external_id)
        self.add_message(txt)
        self.add_unchanged_state(invoice)

    def add_check_payments_message(self, invoice):
        txt = (
                  'По счету %s есть оплаты. Для изменения плательщика необходимо сторнировать поступление средств. '
                  'Для этого обратитесь в поддержку '
                  'через ((https://wiki.yandex-team.ru/otdelsoprovozhdeniya/dokument/avtomatformi/ общую форму)).'
              ) % invoice.external_id
        self.add_message(txt)
        self.add_unchanged_state(invoice)

    def add_check_change_currency_message(self, invoice):
        txt = 'Не можем внести изменения в счёт %s, так как по счёту изменится валюта.' % invoice.external_id
        self.add_message(txt)
        self.add_unchanged_state(invoice)

    def add_check_change_nds_message(self, invoice):
        txt = 'Не можем внести изменения в счёт %s, так как по счёту изменится НДС.' % invoice.external_id
        self.add_message(txt)
        self.add_unchanged_state(invoice)

    def add_check_paysys_amount_message(self, invoice):
        txt = 'Не можем внести изменения в счёт %s,' \
              ' так как сумма счёта превышает доступную сумму для указанного способа оплаты.' % invoice.external_id
        self.add_message(txt)
        self.add_unchanged_state(invoice)

    def add_closed_act_message(self, invoice):
        txt = ('Акт по счету %s находится в закрытом периоде. '
               'Изменение плательщика в закрытом периоде невозможно. '
               'Для решения вопроса обратитесь в Отдел по работе '
               'с платежами и задолженностями на payment-invoice@.' % invoice.external_id)
        self.add_message(txt)
        self.add_unchanged_state(invoice)

    def add_frozen_message(self):
        self.add_message('Ждем разморозки выручки.')

    def add_enq_message(self):
        self.add_message('Изменённые счета и акты добавлены в очередь на выгрузку в ОЕБС.')


class AbstractDBChangePersonTestCase(case_utils.AbstractDBTestCase):
    _summary = 'Изменение плательщика в счетах'
    issue_key = 'test_change_person'

    _description = '''
№ счета: {invoices}\r\t
Название плательщика (на которого надо поменять) и его ID: {person.name} (ID: {person.id})\r\t
Способ оплат: {paysys.name}\r\t
Причина изменения: патамучта гладиолус
'''.strip()

    def get_description(self, session):
        invoices, person, paysys = self._get_data(session)
        return self._description.format(invoices='\n'.join(i.external_id for i in invoices),
                                        person=person, paysys=paysys)


class AbstractDBChangeTestCase(AbstractDBChangePersonTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class InvalidPaysysCase(AbstractDBChangeTestCase):
    _representation = 'incorrect_paysys'

    def __init__(self):
        super(InvalidPaysysCase, self).__init__()
        self._person = None
        self._paysys = None
        self._invoice = None

    def _get_data(self, session):
        client, self._person = db_utils.create_client_person(session, person_type='ph')

        self._paysys = ut.Struct(id=6666, name='Борзыми щенками')

        product = session.query(mapper.Product).getone(1475)
        o = mapper.Order(client=client, service_id=7, product=product)
        self._invoice = db_utils.create_invoice(session, client, [(o, 1)], person=self._person)

        return [self._invoice], self._person, self._paysys

    def get_result(self):
        res = ChangeRequiredResult(transition=IssueTransitions.wont_fix, unchanged=[self._invoice])
        res.add_message('Некорректно указан способ оплаты. Пожалуйста, заполни форму с верными данными еще раз.')
        return res


class InvalidPaysysFirmCase(AbstractDBChangeTestCase):
    _representation = 'incorrect_paysys_firm'

    def __init__(self):
        super(InvalidPaysysFirmCase, self).__init__()
        self._person = None
        self._paysys = None
        self._invoice = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='ph')

        self._person = db_utils.create_person(session, client, person_type='pu')
        self._paysys = session.query(mapper.Paysys).getone(1018)

        product = session.query(mapper.Product).getone(1475)
        o = mapper.Order(client=client, service_id=7, product=product)
        self._invoice = db_utils.create_invoice(session, client, [(o, 1)], person=person)

        return [self._invoice], self._person, self._paysys

    def get_result(self):
        res = ChangeRequiredResult(transition=IssueTransitions.wont_fix, unchanged=[self._invoice])
        res.add_message('Некорректно указан способ оплаты. Пожалуйста, заполни форму с верными данными еще раз.')
        return res


class InvalidPaysysCategoryCase(AbstractDBChangeTestCase):
    _representation = 'incorrect_paysys_category'

    def __init__(self):
        super(InvalidPaysysCategoryCase, self).__init__()
        self._person = None
        self._paysys = None
        self._invoice = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='ph')

        self._person = db_utils.create_person(session, client, person_type='kzp')
        self._paysys = session.query(mapper.Paysys).getone(1000)

        product = session.query(mapper.Product).getone(1475)
        o = mapper.Order(client=client, service_id=7, product=product)
        self._invoice = db_utils.create_invoice(session, client, [(o, 1)], person=person)

        return [self._invoice], self._person, self._paysys

    def get_result(self):
        res = ChangeRequiredResult(transition=IssueTransitions.wont_fix, unchanged=[self._invoice])
        res.add_person_paysys_mismatch_message(self._paysys, self._person)
        return res


class IncorrectPersonCategoryCase(AbstractDBChangeTestCase):
    _representation = 'incorrect person category'

    def __init__(self):
        super(IncorrectPersonCategoryCase, self).__init__()
        self._person = None
        self._paysys = None
        self._invoice = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='ph')

        self._person = db_utils.create_person(session, client, person_type='sw_ur')
        self._paysys = session.query(mapper.Paysys).getone(1000)

        product = session.query(mapper.Product).getone(1475)
        o = mapper.Order(client=client, service_id=7, product=product)
        self._invoice = db_utils.create_invoice(session, client, [(o, 1)], person=person)

        return [self._invoice], self._person, self._paysys

    def get_result(self):
        res = ChangeRequiredResult(transition=IssueTransitions.wont_fix, unchanged=[self._invoice])
        res.add_person_paysys_mismatch_message(self._paysys, self._person)
        return res


class AbstractPaymentsCase(AbstractDBChangeTestCase):

    def __init__(self, base_paysys_id, new_paysys_id):
        super(AbstractPaymentsCase, self).__init__()
        self._base_paysys_id = base_paysys_id
        self._new_paysys_id = new_paysys_id
        self._invoice = None

    def _get_data(self, session):
        client, person_from = db_utils.create_client_person(session, person_type='ph')

        person_to = db_utils.create_person(session, client, person_type='ph')
        paysys = session.query(mapper.Paysys).getone(self._new_paysys_id)

        product = session.query(mapper.Product).getone(1475)
        o = mapper.Order(client=client, service_id=7, product=product)
        self._invoice = db_utils.create_invoice(session, client, [(o, 1)], self._base_paysys_id,
                                                person=person_from)

        return [self._invoice], person_to, paysys

    def get_result(self):
        res = ChangeRequiredResult(transition=IssueTransitions.wont_fix)
        res.add_check_payments_message(self._invoice)
        return res


class OEBSPaymentsCase(AbstractPaymentsCase):
    _representation = 'oebs payments'

    def __init__(self):
        super(OEBSPaymentsCase, self).__init__(1003, 1000)

    def _get_data(self, session):
        res = super(OEBSPaymentsCase, self)._get_data(session)

        self._invoice.receipt_sum_1c = 666

        return res


class InstantPaymentsCase(AbstractPaymentsCase):
    _representation = 'instant_payments'

    def __init__(self):
        super(InstantPaymentsCase, self).__init__(1002, 1000)

    def _get_data(self, session):
        res = super(InstantPaymentsCase, self)._get_data(session)

        payment = mapper.RBSPayment(self._invoice)
        payment.mdorder = 'trololo'
        session.add(payment)
        session.flush()

        payment.set_state('paid', 'by_my_will', 100)
        payment.mark_paid(dt.datetime.now())

        return res


class HasNoPaymentsAndActsEmptyCaseCheckReceiptSum(AbstractDBChangeTestCase):
    _representation = 'no payments and acts - empty, check_receipt_sum'

    def __init__(self):
        super(HasNoPaymentsAndActsEmptyCaseCheckReceiptSum, self).__init__()
        self._invoice = None

    def setup_config(self, session, config):
        config['CHANGE_PERSON_OLD_POLICY'] = 1

    def _get_data(self, session):
        client, person_from = db_utils.create_client_person(session)

        person_to = db_utils.create_person(session, client)
        paysys = session.query(mapper.Paysys).getone(1000)

        product = session.query(mapper.Product).getone(1475)
        o = mapper.Order(client=client, service_id=7, product=product)
        self._invoice = db_utils.create_invoice(session, client, [(o, 1)], person=person_from)

        return [self._invoice], person_to, paysys

    def get_result(self):
        res = ChangeRequiredResult(transition=IssueTransitions.wont_fix)
        res.add_check_acts_sum_1c_message(self._invoice)
        return res


class HasNoPaymentsAndActsOverdractCaseCheckReceiptSum(HasNoPaymentsAndActsEmptyCaseCheckReceiptSum):
    _representation = 'no payments and acts - overdraft, check_receipt_sum'

    def _get_data(self, session):
        client, person_from = db_utils.create_client_person(session)

        person_to = db_utils.create_person(session, client)
        paysys = session.query(mapper.Paysys).getone(1000)

        product = session.query(mapper.Product).getone(1475)
        o = mapper.Order(client=client, service_id=7, product=product)
        self._invoice = db_utils.create_invoice(session, client, [(o, 1)], person=person_from, overdraft=1)
        self._invoice.turn_on_rows()

        return [self._invoice], person_to, paysys


class HasReceiptSumCase(AbstractDBChangeTestCase):
    _representation = 'receipt_sum'

    def __init__(self):
        super(HasReceiptSumCase, self).__init__()
        self._invoice = None

    def setup_config(self, session, config):
        config['CHANGE_PERSON_OLD_POLICY'] = 1

    def _get_data(self, session):
        client, person_from = db_utils.create_client_person(session)

        person_to = db_utils.create_person(session, client)
        paysys = session.query(mapper.Paysys).getone(1000)

        product = session.query(mapper.Product).getone(1475)
        o = mapper.Order(client=client, service_id=7, product=product)
        self._invoice = db_utils.create_invoice(session, client, [(o, 1)], person=person_from, turn_on=True)

        return [self._invoice], person_to, paysys

    def get_result(self):
        res = ChangeRequiredResult(transition=IssueTransitions.wont_fix)
        res.add_check_receipt_sum_message(self._invoice)
        return res


class CurrencyChangeCase(AbstractDBChangeTestCase):
    _representation = 'currency_change'

    def __init__(self):
        super(CurrencyChangeCase, self).__init__()
        self._invoice = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='ur')

        paysys = session.query(mapper.Paysys).getone(1003)

        product = session.query(mapper.Product).getone(1475)
        o = mapper.Order(client=client, service_id=7, product=product)
        self._invoice = db_utils.create_invoice(session, client, [(o, 1)], 1011, person=person)
        self._invoice.turn_on_rows()
        self._invoice.total_act_sum = 666

        return [self._invoice], person, paysys

    def get_result(self):
        res = ChangeRequiredResult(transition=IssueTransitions.wont_fix)
        res.add_check_change_currency_message(self._invoice)
        return res


class NDSChangeCase(AbstractDBChangeTestCase):
    _representation = 'nds_change'

    def __init__(self):
        super(NDSChangeCase, self).__init__()
        self._invoice = None

    def _get_data(self, session):
        client, person_from = db_utils.create_client_person(session, person_type='yt')
        person_to = db_utils.create_person(session, client, person_type='ph')

        paysys = session.query(mapper.Paysys).getone(1001)

        product = session.query(mapper.Product).getone(1475)
        o = mapper.Order(client=client, service_id=7, product=product)
        self._invoice = db_utils.create_invoice(session, client, [(o, 1)], 1014, person=person_from)
        self._invoice.turn_on_rows()
        self._invoice.total_act_sum = 666

        return [self._invoice], person_to, paysys

    def get_result(self):
        res = ChangeRequiredResult(transition=IssueTransitions.wont_fix)
        res.add_check_change_nds_message(self._invoice)
        return res


class PaysysAmountCase(AbstractDBChangeTestCase):
    _representation = 'paysys_amount'

    def __init__(self):
        super(PaysysAmountCase, self).__init__()
        self._invoice = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)

        paysys = session.query(mapper.Paysys).getone(1002)

        product = session.query(mapper.Product).getone(1475)
        o = mapper.Order(client=client, service_id=7, product=product)
        self._invoice = db_utils.create_invoice(session, client, [(o, 4000)], person=person)
        self._invoice.turn_on_rows()
        self._invoice.total_act_sum = 666

        return [self._invoice], person, paysys

    def get_result(self):
        res = ChangeRequiredResult(transition=IssueTransitions.wont_fix)
        res.add_check_paysys_amount_message(self._invoice)
        return res


class MultipleChecksCase(AbstractDBChangeTestCase):
    _representation = 'multiple_checks'

    def __init__(self):
        super(MultipleChecksCase, self).__init__()
        self._invoice_receipt = None
        self._invoice_empty = None
        self._invoice_contract = None

    def _get_data(self, session):
        client, person_from = db_utils.create_client_person(session)

        person_to = db_utils.create_person(session, client)
        paysys = session.query(mapper.Paysys).getone(1000)

        o = db_utils.create_order(session, client)
        contract = db_utils.create_general_contract(session, client, person_from)

        product = session.query(mapper.Product).getone(1475)
        o = mapper.Order(client=client, service_id=7, product=product)
        self._invoice_receipt_sum_1c = db_utils.create_invoice(session, client, [(o, 1)], 1002,
                                                person=person_from)
        self._invoice_receipt_sum_1c.receipt_sum_1c = 666
        self._invoice_contract = db_utils.create_invoice(session, client, [(o, 1)],
                                                         person=person_from, contract=contract)

        return [self._invoice_receipt_sum_1c, self._invoice_contract], person_to, paysys

    def get_result(self):
        res = ChangeRequiredResult(transition=IssueTransitions.wont_fix)
        res.add_check_payments_message(self._invoice_receipt_sum_1c)
        res.add_check_contract_message(self._invoice_contract)
        return res


class AbstractChangeDataCase(AbstractDBChangeTestCase):
    def __init__(self, temp_closed, latest_closed):
        super(AbstractChangeDataCase, self).__init__()
        self._temp_closed = temp_closed
        self._latest_closed = latest_closed

    def setup_config(self, session, config):
        config['LATEST_CLOSED'] = self._latest_closed
        config['TEMP_CLOSED'] = self._temp_closed
        config['changeable_acts_dt'] = self._latest_closed
        config['is_temporary_closed'] = self._temp_closed

        session.execute("update autodasha.t_config set value_num = :val where item = 'TEMP_CLOSED'",
                        {'val': self._temp_closed})

        session.execute("update autodasha.t_config set value_dt = :val where item = 'LATEST_CLOSED'",
                        {'val': self._latest_closed})

    @staticmethod
    def _create_invoice(session, act_dates, overdraft=1, person_type='ph', paysys_id=1002, client=None, person=None,
                        internal_acts=0):
        if person:
            client = person.client
        elif client:
            person = db_utils.create_person(session, client, person_type=person_type)
        else:
            client, person = db_utils.create_client_person(session, person_type=person_type)

        product = session.query(mapper.Product).getone(1475)
        o = mapper.Order(client=client, service_id=7, product=product)

        # на каждый момент времени откручиваем и актим 1 qty - какая к чёрту разница?
        invoice = db_utils.create_invoice(session, client, [(o, len(act_dates) + 1)],
                                          paysys_id, person, overdraft=overdraft)

        invoice.turn_on_rows()
        invoice.internal_acts = internal_acts

        compl_qty = 0
        for dt_ in sorted(act_dates):
            compl_qty += 1
            o.calculate_consumption(dt_, {o.shipment_type: compl_qty})
            invoice.generate_act(force=True, backdate=dt_)

        return invoice


class ChangePaysysCase(AbstractChangeDataCase):
    _representation = 'change paysys'

    def __init__(self):
        dt_ = ut.trunc_date(dt.datetime.now()).replace(day=1)
        super(ChangePaysysCase, self).__init__(0, dt_)
        self._invoice = None
        self._paysys = None

    def _get_data(self, session):
        self._invoice = self._create_invoice(session, [dt.datetime.now()])

        self._paysys = session.query(mapper.Paysys).getone(1000)
        return [self._invoice], self._invoice.person, self._paysys

    def get_result(self):
        res = ChangeRequiredResult(transition=IssueTransitions.fixed, changed_paysys=[(self._invoice, self._paysys)])
        return res


class ChangePaysysBYNCase(AbstractChangeDataCase):
    _representation = 'change_BYN_paysys'

    def __init__(self):
        dt_ = ut.trunc_date(dt.datetime.now()).replace(day=1)
        super(ChangePaysysBYNCase, self).__init__(0, dt_)
        self._invoice = None
        self._paysys = None

    def _get_data(self, session):
        self._invoice = self._create_invoice(session, [dt.datetime.now()], person_type='byu', paysys_id=1125)
        self._paysys = session.query(mapper.Paysys).getone(2701101)
        return [self._invoice], self._invoice.person, self._paysys

    def get_result(self):
        res = ChangeRequiredResult(transition=IssueTransitions.fixed, changed_paysys=[(self._invoice, self._paysys)])
        return res


class ChangePersonCase(AbstractChangeDataCase):
    _representation = 'change_person'

    def __init__(self):
        dt_ = ut.trunc_date(dt.datetime.now()).replace(day=1)
        super(ChangePersonCase, self).__init__(0, dt_)
        self._invoice = None
        self._person = None
        self._paysys = None

    def _get_data(self, session):
        self._invoice = self._create_invoice(session, [dt.datetime.now()])
        self._person = db_utils.create_person(session, self._invoice.client)
        self._paysys = session.query(mapper.Paysys).getone(1000)

        return [self._invoice], self._person, self._paysys

    def get_result(self):
        res = ChangeRequiredResult(transition=IssueTransitions.none,
                                   changed_person=[(self._invoice, self._person, self._paysys)],
                                   enqueue=True)

        return res


class ChangePersonAndPaysysCase(AbstractChangeDataCase):
    _representation = 'change person + change paysys'

    def __init__(self):
        dt_ = ut.trunc_date(dt.datetime.now()).replace(day=1)
        super(ChangePersonAndPaysysCase, self).__init__(0, dt_)
        self._invoice_ps = None
        self._invoice_pr = None
        self._person = None
        self._paysys = None

    def _get_data(self, session):
        self._invoice_pr = self._create_invoice(session, [dt.datetime.now()])
        self._person = db_utils.create_person(session, self._invoice_pr.client)
        self._invoice_ps = self._create_invoice(session, [dt.datetime.now()], person=self._person)
        self._paysys = session.query(mapper.Paysys).getone(1000)

        return [self._invoice_pr, self._invoice_ps], self._person, self._paysys

    def get_result(self):
        res = ChangeRequiredResult(transition=IssueTransitions.none,
                                   changed_person=[(self._invoice_pr, self._person, self._paysys)],
                                   changed_paysys=[(self._invoice_ps, self._paysys)],
                                   enqueue=True)

        return res


class ChangePersonAndBYNPaysysCase(AbstractChangeDataCase):
    _representation = 'change person + change BYN paysys'

    def __init__(self):
        dt_ = ut.trunc_date(dt.datetime.now()).replace(day=1)
        super(ChangePersonAndBYNPaysysCase, self).__init__(0, dt_)
        self._invoice_ps = None
        self._invoice_pr = None
        self._person = None
        self._paysys = None

    def _get_data(self, session):
        self._invoice_pr = self._create_invoice(session, [dt.datetime.now()], person_type='byp', paysys_id=1126)
        self._person = db_utils.create_person(session, self._invoice_pr.client, person_type='byu')
        self._invoice_ps = self._create_invoice(session, [dt.datetime.now()], person=self._person,paysys_id=1125)
        self._paysys = session.query(mapper.Paysys).getone(2701101)

        return [self._invoice_pr, self._invoice_ps], self._person, self._paysys

    def get_result(self):
        res = ChangeRequiredResult(transition=IssueTransitions.none,
                                   changed_person=[(self._invoice_pr, self._person, self._paysys)],
                                   changed_paysys=[(self._invoice_ps, self._paysys)],
                                   enqueue=True)

        return res


class ChangePersonDifferentCategoryCase(AbstractChangeDataCase):
    _representation = 'change_person_category'

    def __init__(self):
        dt_ = ut.trunc_date(dt.datetime.now()).replace(day=1)
        super(ChangePersonDifferentCategoryCase, self).__init__(0, dt_)
        self._invoice = None
        self._person = None
        self._paysys = None

    def _get_data(self, session):
        self._invoice = self._create_invoice(session, [dt.datetime.now()])
        self._person = db_utils.create_person(session, self._invoice.client, person_type='ur')
        self._paysys = session.query(mapper.Paysys).getone(1003)

        return [self._invoice], self._person, self._paysys

    def get_result(self):
        res = ChangeRequiredResult(transition=IssueTransitions.none,
                                   changed_person=[(self._invoice, self._person, self._paysys,
                                                    self._invoice.total_act_sum, [2002, 2003, 2007])],
                                   enqueue=True)

        return res


class ChangePersonNotFoundCase(AbstractChangeDataCase):
    _representation = 'change_person_w_not_found_invoice'

    def __init__(self):
        dt_ = ut.trunc_date(dt.datetime.now()).replace(day=1)
        super(ChangePersonNotFoundCase, self).__init__(0, dt_)
        self._invoice = None
        self._person = None
        self._paysys = None

    def _get_data(self, session):
        self._invoice = self._create_invoice(session, [dt.datetime.now()])
        self._person = db_utils.create_person(session, self._invoice.client)
        self._paysys = session.query(mapper.Paysys).getone(1000)

        return [self._invoice, ut.Struct(external_id=self._invoice.external_id + '66')], self._person, self._paysys

    def get_result(self):
        res = ChangeRequiredResult(transition=IssueTransitions.none,
                                   changed_person=[(self._invoice, self._person, self._paysys)],
                                   enqueue=True)
        txt = 'Некорректно указан № счета (%s). Пожалуйста, заполни форму с верными данными еще раз.'\
              % (self._invoice.external_id + '66')
        res.add_message(txt)

        return res


class ChangePaysysAndHasReceiptCase(AbstractChangeDataCase):
    _representation = 'change_paysys_w_receipt_sum'

    def __init__(self):
        dt_ = ut.trunc_date(dt.datetime.now()).replace(day=1)
        super(ChangePaysysAndHasReceiptCase, self).__init__(0, dt_)
        self._invoice = None
        self._invoice_receipt = None
        self._paysys = None

    def _get_data(self, session):
        self._invoice = self._create_invoice(session, [dt.datetime.now()])
        self._invoice_receipt = self._create_invoice(session, [dt.datetime.now()], client=self._invoice.client,
                                                     person=self._invoice.person)

        self._invoice_receipt.create_receipt(self._invoice_receipt.effective_sum)

        self._paysys = session.query(mapper.Paysys).getone(1000)
        return [self._invoice, self._invoice_receipt], self._invoice.person, self._paysys

    def get_result(self):
        res = ChangeRequiredResult(transition=IssueTransitions.fixed,
                                   changed_paysys=[(self._invoice, self._paysys),
                                                   (self._invoice_receipt, self._paysys)]
                                   )

        return res


class ClosedMonthActCase(AbstractChangeDataCase):
    _representation = 'closed_month_act'

    def __init__(self):
        dt_ = ut.trunc_date(dt.datetime.now()).replace(day=1)
        super(ClosedMonthActCase, self).__init__(0, dt_ + dt.timedelta(days=31))
        self._invoice = None
        self._person = None
        self._paysys = None

    def _get_data(self, session):
        self._invoice = self._create_invoice(session, [dt.datetime.now()])
        self._person = db_utils.create_person(session, self._invoice.client)
        self._paysys = session.query(mapper.Paysys).getone(1000)

        return [self._invoice], self._person, self._paysys

    def get_result(self):
        res = ChangeRequiredResult(transition=IssueTransitions.wont_fix)
        res.add_closed_act_message(self._invoice)
        return res


class ClosedMonthActMultipleCase(AbstractChangeDataCase):
    _representation = 'closed_month_multiple_acts'

    def __init__(self):
        self.month_dt = ut.trunc_date(dt.datetime.now()).replace(day=1)
        super(ClosedMonthActMultipleCase, self).__init__(0, self.month_dt - dt.timedelta(days=15))
        self._invoice = None
        self._person = None
        self._paysys = None

    def _get_data(self, session):
        dates = [
            self.month_dt - dt.timedelta(days=25),
            self.month_dt - dt.timedelta(days=5)
        ]
        self._invoice = self._create_invoice(session, dates)
        self._person = db_utils.create_person(session, self._invoice.client)
        self._paysys = session.query(mapper.Paysys).getone(1000)

        return [self._invoice], self._person, self._paysys

    def get_result(self):
        res = ChangeRequiredResult(transition=IssueTransitions.wont_fix)
        res.add_closed_act_message(self._invoice)
        return res


class ChangePaysysInClosedMonthActCase(AbstractChangeDataCase):
    _representation = 'change_paysys_closed_act'

    def __init__(self):
        dt_ = ut.trunc_date(dt.datetime.now()).replace(day=1)
        super(ChangePaysysInClosedMonthActCase, self).__init__(0, dt_ + dt.timedelta(days=31))
        self._invoice = None
        self._paysys = None

    def _get_data(self, session):
        self._invoice = self._create_invoice(session, [dt.datetime.now()])
        self._paysys = session.query(mapper.Paysys).getone(1000)

        return [self._invoice], self._invoice.person, self._paysys

    def get_result(self):
        res = ChangeRequiredResult(transition=IssueTransitions.fixed, changed_paysys=[(self._invoice, self._paysys)])
        return res


class TempClosedWithoutWaitingCase(AbstractChangeDataCase):
    _representation = 'temp_closed_change_person'

    def __init__(self):
        dt_ = ut.trunc_date(dt.datetime.now()).replace(day=1)
        super(TempClosedWithoutWaitingCase, self).__init__(1, dt_ - dt.timedelta(days=31))
        self._invoice = None
        self._person = None
        self._paysys = None

    def _get_data(self, session):
        self._invoice = self._create_invoice(session, [dt.datetime.now()])
        self._person = db_utils.create_person(session, self._invoice.client)
        self._paysys = session.query(mapper.Paysys).getone(1000)

        return [self._invoice], self._person, self._paysys

    def get_result(self):
        res = ChangeRequiredResult(transition=IssueTransitions.none,
                                   changed_person=[(self._invoice, self._person, self._paysys)],
                                   enqueue=True)
        return res


class TempClosedWaitingCase(AbstractChangeDataCase):
    _representation = 'temp_closed_waiting'

    def __init__(self):
        self._month_dt = ut.trunc_date(dt.datetime.now()).replace(day=1)
        super(TempClosedWaitingCase, self).__init__(1, self._month_dt - dt.timedelta(days=31))
        self._invoice = None
        self._person = None
        self._paysys = None

    def _get_data(self, session):
        self._invoice = self._create_invoice(session, [self._month_dt - dt.timedelta(days=16)])
        self._person = db_utils.create_person(session, self._invoice.client)
        self._paysys = session.query(mapper.Paysys).getone(1000)

        return [self._invoice], self._person, self._paysys

    def get_result(self):
        res = ChangeRequiredResult(transition=IssueTransitions.waiting,
                                   unchanged=[self._invoice],
                                   frozen=True)
        return res


class TempClosedContinuingWaitingCase(AbstractChangeDataCase):
    _representation = 'temp_closed_continuing_waiting'

    status = IssueStatuses.waiting

    def __init__(self):
        self._month_dt = ut.trunc_date(dt.datetime.now()).replace(day=1)
        super(TempClosedContinuingWaitingCase, self).__init__(1, self._month_dt - dt.timedelta(days=31))
        self._invoice = None
        self._person = None
        self._paysys = None

    def _get_data(self, session):
        self._invoice = self._create_invoice(session, [self._month_dt - dt.timedelta(days=16)])
        self._person = db_utils.create_person(session, self._invoice.client)
        self._paysys = session.query(mapper.Paysys).getone(1000)

        return [self._invoice], self._person, self._paysys

    def get_result(self):
        return None


class TempClosedWaitingMultipleCase(AbstractChangeDataCase):
    _representation = 'temp_closed_waiting_multiple_acts'

    def __init__(self):
        self.month_dt = ut.trunc_date(dt.datetime.now()).replace(day=1)
        super(TempClosedWaitingMultipleCase, self).__init__(1, self.month_dt - dt.timedelta(days=31))
        self._invoice = None
        self._person = None
        self._paysys = None

    def _get_data(self, session):
        dates = [
            self.month_dt - dt.timedelta(days=5),
            dt.datetime.now()
        ]
        self._invoice = self._create_invoice(session, dates)
        self._person = db_utils.create_person(session, self._invoice.client)
        self._paysys = session.query(mapper.Paysys).getone(1000)

        return [self._invoice], self._person, self._paysys

    def get_result(self):
        res = ChangeRequiredResult(transition=IssueTransitions.waiting,
                                   unchanged=[self._invoice],
                                   frozen=True)
        return res


class ChangePersonDifferentSumCase(AbstractChangeDataCase):
    _representation = 'change_diff_sum'

    def __init__(self):
        dt_ = ut.trunc_date(dt.datetime.now()).replace(day=1)
        super(ChangePersonDifferentSumCase, self).__init__(0, dt_)
        self._invoice = None
        self._person = None
        self._paysys = None

    def _get_data(self, session):
        self._invoice = self._create_invoice(session, [dt.datetime.now()])
        order, = [io.order for io in self._invoice.rows]
        order.calculate_consumption(order.shipment.dt, {order.shipment_type: order.shipment.completion_qty / 2})

        self._person = db_utils.create_person(session, self._invoice.client)
        self._paysys = session.query(mapper.Paysys).getone(1000)

        return [self._invoice], self._person, self._paysys

    def get_result(self):
        consume, = self._invoice.consumes
        res = ChangeRequiredResult(transition=IssueTransitions.none,
                                   changed_person=[(self._invoice, self._person, self._paysys, consume.completion_sum)],
                                   enqueue=True)

        changed_sum_txt = \
            'Обращаем внимание, что до изменения плательщика по счету {invoice} было: ' \
            '1 акт на сумму {old_sum:.2f} {currency}, после смены плательщика по счету {invoice} стало: ' \
            '1 акт на сумму {new_sum:.2f} {currency}.'.format(invoice=self._invoice.external_id,
                                                              old_sum=consume.act_sum,
                                                              new_sum=consume.completion_sum,
                                                              currency=self._invoice.currency)
        res.add_message(changed_sum_txt)

        return res


class ChangePersonAndClosedCase(AbstractChangeDataCase):
    _representation = 'change_person_w_closed'

    def __init__(self):
        dt_ = ut.trunc_date(dt.datetime.now()).replace(day=1)
        super(ChangePersonAndClosedCase, self).__init__(0, dt_)
        self._invoice_closed = None
        self._invoice_change = None
        self._person = None
        self._paysys = None

    def _get_data(self, session):
        client = db_utils.create_client(session)

        dt_ = dt.datetime.now()
        self._invoice_closed = self._create_invoice(session, [dt_ - dt.timedelta(days=31)], client=client)
        self._invoice_change = self._create_invoice(session, [dt_], client=client)
        self._person = db_utils.create_person(session, client)
        self._paysys = session.query(mapper.Paysys).getone(1000)

        return [self._invoice_closed, self._invoice_change], self._person, self._paysys

    def get_result(self):
        res = ChangeRequiredResult(transition=IssueTransitions.none,
                                   changed_person=[(self._invoice_change, self._person, self._paysys)],
                                   enqueue=True)
        res.add_closed_act_message(self._invoice_closed)
        return res


class ChangePersonAndWaitingCase(AbstractChangeDataCase):
    _representation = 'change_person_w_waiting'

    def __init__(self):
        self._month_dt = ut.trunc_date(dt.datetime.now()).replace(day=1)
        super(ChangePersonAndWaitingCase, self).__init__(1, self._month_dt - dt.timedelta(days=31))
        self._invoice_waiting = None
        self._invoice_change = None
        self._person = None
        self._paysys = None

    def _get_data(self, session):
        client = db_utils.create_client(session)

        self._invoice_waiting = self._create_invoice(session, [self._month_dt - dt.timedelta(days=15)], client=client)
        self._invoice_change = self._create_invoice(session, [self._month_dt], client=client)
        self._person = db_utils.create_person(session, client)
        self._paysys = session.query(mapper.Paysys).getone(1000)

        return [self._invoice_waiting, self._invoice_change], self._person, self._paysys

    def get_result(self):
        res = ChangeRequiredResult(transition=IssueTransitions.waiting,
                                   unchanged=[self._invoice_waiting, self._invoice_change],
                                   frozen=True)
        return res


class WaitingAndClosedCase(AbstractChangeDataCase):
    _representation = 'change_waiting_w_closed'

    def __init__(self):
        self.month_dt = ut.trunc_date(dt.datetime.now()).replace(day=1)
        super(WaitingAndClosedCase, self).__init__(1, self.month_dt - dt.timedelta(days=31))
        self._invoice_closed = None
        self._invoice_waiting = None
        self._person = None
        self._paysys = None

    def _get_data(self, session):
        client = db_utils.create_client(session)

        self._invoice_closed = self._create_invoice(session, [self.month_dt - dt.timedelta(days=45)], client=client)
        self._invoice_waiting = self._create_invoice(session, [self.month_dt - dt.timedelta(days=15)], client=client)
        self._person = db_utils.create_person(session, client)
        self._paysys = session.query(mapper.Paysys).getone(1000)

        return [self._invoice_closed, self._invoice_waiting], self._person, self._paysys

    def get_result(self):
        res = ChangeRequiredResult(transition=IssueTransitions.waiting,
                                   unchanged=[self._invoice_waiting, self._invoice_closed],
                                   frozen=True)
        return res


class ChangePersonAndInWaitingPeriodCase(AbstractChangeDataCase):
    _representation = 'change_person_w_person_waiting'

    def __init__(self):
        self._month_dt = ut.trunc_date(dt.datetime.now()).replace(day=1)
        super(ChangePersonAndInWaitingPeriodCase, self).__init__(0, self._month_dt - dt.timedelta(days=31))
        self._invoices = None
        self._person = None
        self._paysys = None

    def _get_data(self, session):
        client = db_utils.create_client(session)

        self._invoices = []
        self._invoices.append(self._create_invoice(session, [self._month_dt - dt.timedelta(days=15)], client=client))
        self._invoices.append(self._create_invoice(session, [self._month_dt], client=client))
        self._person = db_utils.create_person(session, client)
        self._paysys = session.query(mapper.Paysys).getone(1000)

        return self._invoices, self._person, self._paysys

    def get_result(self):
        changed = [(i, self._person, self._paysys) for i in self._invoices]
        res = ChangeRequiredResult(transition=IssueTransitions.none,
                                   changed_person=changed,
                                   enqueue=True)
        return res


class ClosedAndInWaitingPeriodCase(AbstractChangeDataCase):
    _representation = 'change_closed_w_person_waiting'

    def __init__(self):
        self._month_dt = ut.trunc_date(dt.datetime.now()).replace(day=1)
        super(ClosedAndInWaitingPeriodCase, self).__init__(0, self._month_dt - dt.timedelta(days=31))
        self._invoice_change = None
        self._invoice_closed = None
        self._person = None
        self._paysys = None

    def _get_data(self, session):
        client = db_utils.create_client(session)

        self._invoice_change = self._create_invoice(session, [self._month_dt - dt.timedelta(days=15)], client=client)
        self._invoice_closed = self._create_invoice(session, [self._month_dt - dt.timedelta(45)], client=client)
        self._person = db_utils.create_person(session, client)
        self._paysys = session.query(mapper.Paysys).getone(1000)

        return [self._invoice_closed, self._invoice_change], self._person, self._paysys

    def get_result(self):
        res = ChangeRequiredResult(transition=IssueTransitions.none,
                                   changed_person=[(self._invoice_change, self._person, self._paysys)],
                                   enqueue=True)
        res.add_closed_act_message(self._invoice_closed)
        return res


class ChangePersonFirmCase(AbstractChangeDataCase):
    _representation = 'change_person_firm'

    def __init__(self):
        dt_ = ut.trunc_date(dt.datetime.now()).replace(day=1)
        super(ChangePersonFirmCase, self).__init__(0, dt_)
        self._invoice = None
        self._person = None
        self._paysys = None

    def _get_data(self, session):
        self._invoice = self._create_invoice(session, [dt.datetime.now()], paysys_id=1002)
        self._person = db_utils.create_person(session, self._invoice.client)
        self._paysys = session.query(mapper.Paysys).getone(1000)

        return [self._invoice], self._person, self._paysys

    def get_result(self):
        res = ChangeRequiredResult(transition=IssueTransitions.none,
                                   changed_person=[(self._invoice, self._person, self._paysys,
                                                    self._invoice.total_act_sum, [2001])],
                                   enqueue=True)

        return res


class ReceiptSum35Case(AbstractChangeDataCase):
    _representation = 'receipt_sum_35'

    def __init__(self):
        dt_ = ut.trunc_date(dt.datetime.now()).replace(day=1)
        super(ReceiptSum35Case, self).__init__(0, dt_)
        self._invoice = None
        self._paysys = None

    def _get_data(self, session):
        client, person_from = db_utils.create_client_person(session)

        product = session.query(mapper.Product).getone(506537)
        o = mapper.Order(client=client, service_id=35, product=product)
        self._invoice = db_utils.create_invoice(session, client, [(o, 1)], person=person_from, turn_on=True)
        self._invoice.close_invoice(dt.datetime.now())

        self._paysys = session.query(mapper.Paysys).getone(1001)
        return [self._invoice], self._invoice.person, self._paysys

    def get_result(self):
        res = ChangeRequiredResult(transition=IssueTransitions.fixed, changed_paysys=[(self._invoice, self._paysys)])
        return res


class ChangePersonInternalCase(AbstractChangeDataCase):
    _representation = 'change_person_internal_act'

    def __init__(self):
        dt_ = ut.trunc_date(dt.datetime.now()).replace(day=1)
        super(ChangePersonInternalCase, self).__init__(0, dt_)
        self._invoice = None
        self._person = None
        self._paysys = None

    def _get_data(self, session):
        self._invoice = self._create_invoice(session, [dt.datetime.now()], internal_acts=1)
        self._person = db_utils.create_person(session, self._invoice.client)
        self._paysys = session.query(mapper.Paysys).getone(1000)

        return [self._invoice], self._person, self._paysys

    def get_result(self):
        res = ChangeRequiredResult(transition=IssueTransitions.none,
                                   changed_person=[(self._invoice, self._person, self._paysys)],
                                   enqueue=True)
        return res


class ChangePersonLogTariffCase(AbstractChangeDataCase):
    _representation = 'change_person_log_tariff'

    def __init__(self):
        dt_ = ut.trunc_date(dt.datetime.now()).replace(day=1)
        super(ChangePersonLogTariffCase, self).__init__(0, dt_)
        self._invoice = None
        self._person = None
        self._paysys = None

    def _get_data(self, session):
        from balance.actions.act_create import ActFactory
        from balance.actions.acts.row import ActRow

        self._invoice = self._create_invoice(session, [])

        ActFactory.create_from_external(
            self._invoice,
            [ActRow(self._invoice.consumes[0], 30, 900)],
            dt.datetime.now(),
            external_id='YB-666-2'
        )

        self._person = db_utils.create_person(session, self._invoice.client)
        self._paysys = session.query(mapper.Paysys).getone(1000)

        return [self._invoice], self._person, self._paysys

    def get_result(self):
        res = ChangeRequiredResult(transition=IssueTransitions.none,
                                   changed_person=[(self._invoice, self._person, self._paysys)],
                                   enqueue=True)

        return res


class ChangePersonInvoiceHasReceiptSum(AbstractChangeDataCase):
    _representation = 'change_person_invoice_has_receipt_sum'

    def __init__(self):
        dt_ = ut.trunc_date(dt.datetime.now()).replace(day=1)
        super(ChangePersonInvoiceHasReceiptSum, self).__init__(0, dt_)
        self._invoice = None
        self._person = None
        self._paysys = None

    def _get_data(self, session):
        client, person_from = db_utils.create_client_person(session)

        self._person = db_utils.create_person(session, client)
        self._paysys = session.query(mapper.Paysys).getone(1000)

        product = session.query(mapper.Product).getone(1475)
        o = mapper.Order(client=client, service_id=7, product=product)
        self._invoice = db_utils.create_invoice(session, client, [(o, 10)], person=person_from, turn_on=True)

        return [self._invoice], self._person, self._paysys

    def get_result(self):
        assert self._invoice.receipt_sum_1c == 0
        assert self._invoice.receipt_sum.as_decimal() > 0
        res = ChangeRequiredResult(transition=IssueTransitions.none,
                                   changed_person=[(self._invoice, self._person, self._paysys)],
                                   enqueue=True)

        return res


class HasNoPaymentsAndActsEmptyCase(AbstractChangeDataCase):
    _representation = 'no_payments_and_acts'

    def __init__(self):
        dt_ = ut.trunc_date(dt.datetime.now()).replace(day=1)
        super(HasNoPaymentsAndActsEmptyCase, self).__init__(0, dt_)
        self._invoice = None
        self._person = None
        self._paysys = None

    def _get_data(self, session):
        client, person_from = db_utils.create_client_person(session)

        self._person = db_utils.create_person(session, client)
        self._paysys = session.query(mapper.Paysys).getone(1000)

        product = session.query(mapper.Product).getone(1475)
        o = mapper.Order(client=client, service_id=7, product=product)
        self._invoice = db_utils.create_invoice(session, client, [(o, 1)], person=person_from)

        return [self._invoice], self._person, self._paysys

    def get_result(self):
        res = ChangeRequiredResult(transition=IssueTransitions.none,
                                   changed_person=[(self._invoice, self._person, self._paysys)],
                                   enqueue=True)

        return res


class HasNoPaymentsAndActsOverdractCase(HasNoPaymentsAndActsEmptyCase):
    _representation = 'no_payments_and_acts_overdraft'

    def _get_data(self, session):
        client, person_from = db_utils.create_client_person(session)

        self._person = db_utils.create_person(session, client)
        self._paysys = session.query(mapper.Paysys).getone(1000)

        product = session.query(mapper.Product).getone(1475)
        o = mapper.Order(client=client, service_id=7, product=product)
        self._invoice = db_utils.create_invoice(session, client, [(o, 1)], person=person_from, overdraft=1)
        self._invoice.turn_on_rows()

        return [self._invoice], self._person, self._paysys


@pytest.mark.parametrize('issue_data',
                         [case() for case in AbstractDBChangeTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['issue_data'])
def test_db(session, issue_data):
    queue_object, st_issue, case = issue_data

    req_res = case.get_result()

    solver = ChangePerson(queue_object, st_issue)
    res = solver.solve()

    if req_res is None:
        assert res.commit is False
        assert res.delay is True
        assert res.issue_report is None
        return

    assert res.commit is True
    assert res.delay is req_res.delay
    report = res.issue_report

    comments_parts = filter(None, map(unicode.strip, report.comment.strip().split('\n')))
    assert set(req_res.comments) == set(comments_parts)
    assert len(report.comments) <= 1
    assert report.transition == req_res.transition
    assert report.assignee == req_res.assignee

    for invoice, person_id, paysys_id, bank_id, act_amount, active_acts in req_res.invoices_states:
        assert invoice.person_id == person_id
        assert invoice.paysys_id == paysys_id
        assert (isinstance(bank_id, list) and invoice.bank_details.bank_id in bank_id) \
               or (bank_id == invoice.bank_details.bank_id)
        assert invoice.total_act_sum.as_decimal() == act_amount
        assert active_acts & {a.id for a in invoice.acts if a.hidden < 4} == active_acts

    try:
        export_queue = session.query(a_mapper.QueueObject).\
            filter(a_mapper.QueueObject.issue == queue_object.issue,
                   a_mapper.QueueObject.processor == 'EXPORT_CHECK').\
            one()
    except orm.exc.NoResultFound:
        assert not req_res.reexported_invoices
    else:
        req_reexp_invoices = {obj.object_id for obj in export_queue.proxies if obj.classname == 'Invoice'}
        req_reexp_acts = {obj.object_id for obj in export_queue.proxies if obj.classname == 'Act'}
        assert req_reexp_invoices | req_reexp_acts == {obj.object_id for obj in export_queue.proxies}

        reexp_invoices = {i.id for i in req_res.reexported_invoices if i.exportable}
        reexp_acts = {act.id for invoice in req_res.reexported_invoices for act in invoice.acts if act.exportable}
        assert reexp_invoices == req_reexp_invoices
        assert reexp_acts == req_reexp_acts
