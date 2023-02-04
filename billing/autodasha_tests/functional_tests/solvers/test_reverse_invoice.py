# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import StringIO
import pytest
import mock
import functools
import datetime as dt
from sqlalchemy import orm

import balance.muzzle_util as ut
from balance import mapper
from balance.xls_export import get_excel_document

from autodasha.solver_cl import ReverseInvoice, ParseException
from autodasha.core.api.tracker import IssueReport
from autodasha.core.api.tracker import IssueTransitions
from autodasha.db import mapper as a_mapper
from autodasha.utils.solver_utils import D
from autodasha.utils.xls_reader import xls_reader_contents

from tests.autodasha_tests.functional_tests import case_utils
from tests.autodasha_tests.functional_tests import mock_utils
from tests.autodasha_tests.common import db_utils
from tests.autodasha_tests.common import staff_utils


COMMENTS = {
    'already_solved':
        'Эта задача уже была выполнена. Направьте новый запрос через '
        '((https://wiki.yandex-team.ru/otdelsoprovozhdeniya/dokument/avtomatformi/ '
        'формы)). Если не найдёте подходящую, заполните, пожалуйста, общую форму.',
    'invoice_not_found':
        'Счет {} не найден в биллинге. Заполни, пожалуйста, '
        'форму еще раз, указав корректный номер счета.',
    'order_not_found':
        'Заказ {} не найден в биллинге. Заполни, пожалуйста, '
        'форму еще раз, указав корректный номер заказа.',
    'wrong_invoice_format':
        'Счет {} указан некорректно. '
        'Данная сточка обработана не будет.',
    'wrong_order_format':
        'Заказ {} указан некорректно. '
        'Данная строчка обработана не будет.',
    'wrong_sum_to_return':
        'Сумма {} указана некорректно. Счет: {}, Заказ: {}. '
        'Данная строчка обработана не будет.',
    'invoice_with_sum':
        'По счету {} указана сумма для возврата, но не указан номер заказа. '
        'Данная строчка обработана не будет.',
    'no_file':
        'Не приложен файл. Заполни, пожалуйста, '
        'форму еще раз, приложив корректный файл.',
    'many_files':
        'Приложено больше одного файла. Заполни, пожалуйста, '
        'форму еще раз, приложив корректный файл.',
    'incorrect_file_extension':
        'Расширение файла должно быть xls или xlsx. Заполни, пожалуйста, '
        'форму еще раз, приложив корректный файл.',
    'empty_file':
        'Файл пустой. Заполни, пожалуйста, '
        'форму еще раз, приложив корректный файл.',
    'max_file_len_exceeded':
        'Превышен максимально допустимый размер файла. '
        'В файле должно быть не больше {} строк. Заполни, пожалуйста, '
        'форму еще раз, разбив файл на несколько файлов поменьше.',
    'incorrect_caption':
        'В приложенном файле неверно указаны поля шапки. Заполни, пожалуйста, '
        'форму еще раз, приложив корректный файл.',
    'parsed_no_invoice':
        'Не удалось определить номер счета. Заполни, пожалуйста, '
        'форму еще раз, указав корректный номер счета.',
    'parsed_no_order':
        'Не удалось определить номер заказа. Заполни, пожалуйста, '
        'форму еще раз, указав корректный номер заказа.',
    'invoice_transfer_failed':
        'Произошла ошибка во время трасфера. Счет: {}. '
        'Задачу нужно передать разработчикам.',
    'order_transfer_failed':
        'Произошла ошибка во время трасфера. Счет: {}, Заказ: {}. '
        'Задачу нужно передать разработчикам.',
    'done':
        'Средства возвращены. Файл с подробной информацией во вложении.',
    'nothing_to_return':
        'По указанным данным нет свободных средств для возврата.',
    'file_is_damaged': 'Файл поврежден или некорректен. Заполни, пожалуйста, '
                       'форму еще раз, приложив корректный файл.'
}


@pytest.fixture(autouse=True)
def use_core_free_funds(request, config):
    config._items['USE_OVERACT_BY_CLIENTS'] = True


def _fake_init(self, *args, **kwargs):
    super(ReverseInvoice, self).__init__(*args, **kwargs)
    self.ro_session = self.session


class AbstractMockTestCase(case_utils.AbstractMockTestCase):
    _default_lines = {
        'invoice_summary': 'Снять свободные средства по счету',
        'order_summary': 'Снять свободные средства с заказа по счету',
        'file_summary': 'Снять свободные средства по списку',
        'invoice_invoice': 'Возврат средств в рамках одного счета: %s',
        'order_invoice': '№ счета: %s',
        'order': '№ заказа: %s',
        'sum_to_return': 'Сумма к возврату: %s'
    }


class AbstractParseFailTestCase(AbstractMockTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    def __init__(self):
        super(AbstractParseFailTestCase, self).__init__()
        self.comments = []

    def add_result_row(self, comment_id, *args, **kwargs):
        self.comments.append(COMMENTS[comment_id].format(*args, **kwargs))

    def prepare_result(self):
        raise NotImplementedError

    def get_result(self):
        req_result = self.prepare_result()
        return req_result, self.comments


class InvoiceNotParsedInvoiceTestCase(AbstractParseFailTestCase):
    _representation = 'invoice_not_parsed_invoice'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(invoice_invoice='ВыДумалиЯСчет?')
            ]
        return self._get_default_line('invoice_summary'), lines

    def prepare_result(self):
        self.add_result_row('parsed_no_invoice')


class InvoiceNotFoundInvoiceTestCase(AbstractParseFailTestCase):
    _representation = 'invoice_not_found_invoice'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(invoice_invoice='Б-666-13')
        ]
        return self._get_default_line('invoice_summary'), lines

    def prepare_result(self):
        self.add_result_row('invoice_not_found', 'Б-666-13')


class InvoiceNotParsedOrderTestCase(AbstractParseFailTestCase):
    _representation = 'invoice_not_parsed_order'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(order_invoice='МойСчет!')
            ]
        return self._get_default_line('order_summary'), lines

    def prepare_result(self):
        self.add_result_row('parsed_no_invoice')


class OrderNotParsedOrderTestCase(AbstractParseFailTestCase):
    _representation = 'order_not_parsed'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(order_invoice='Б-666-13'),
            self._get_default_line(order='ДаЭтоЖеНеЗаказ!')
            ]
        return self._get_default_line('order_summary'), lines

    def prepare_result(self):
        self.add_result_row('parsed_no_order')


class InvoiceNotFoundOrderTestCase(AbstractParseFailTestCase):
    _representation = 'invoice_not_found_order'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(order_invoice='Б-666-13'),
            self._get_default_line(order='13-666666')
        ]
        return self._get_default_line('order_summary'), lines

    def prepare_result(self):
        self.add_result_row('invoice_not_found', 'Б-666-13')


class OrderNotFoundTestCase(AbstractParseFailTestCase):
    _representation = 'order_not_found'

    def get_data(self, mock_manager):
        invoice = mock_utils.create_invoice(mock_manager, external_id='Б-666-13')

        lines = [
            self._get_default_line(order_invoice='Б-666-13'),
            self._get_default_line(order='13-666666')
        ]
        return self._get_default_line('order_summary'), lines

    def prepare_result(self):
        self.add_result_row('order_not_found', '13-666666')


class SumNotParsedOrderTestCase(AbstractParseFailTestCase):
    _representation = 'sum_to_return_not_parsed'

    def get_data(self, mock_manager):
        invoice = mock_utils.create_invoice(mock_manager, external_id='Б-666-13')
        order = mock_utils.create_order(mock_manager, service_order_id='666666')

        lines = [
            self._get_default_line(order_invoice='Б-666-13'),
            self._get_default_line(order='7-666666'),
            self._get_default_line(sum_to_return='666praise_billing666')
            ]
        return self._get_default_line('order_summary'), lines

    def prepare_result(self):
        self.add_result_row('wrong_sum_to_return', '666praise_billing666', 'Б-666-13', '7-666666')


@mock.patch('autodasha.solver_cl.reverse_invoice.ReverseInvoice.__init__', _fake_init)
@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in AbstractParseFailTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_parse_failing(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data
    ri = IssueReport()

    req_result, req_comment = case.get_result()

    solver = ReverseInvoice

    solver_res = solver(mock_queue_object, issue)
    if not req_result:
        with pytest.raises(ParseException) as exc:
            solver_res.parse_issue(ri)
        assert req_comment[0] in exc.value.message
    else:
        solver_res.parse_issue(ri)
        comments = [text.get('text') for text in ri.comments]
        comments = ' '.join(comments)
        assert req_comment[0] == comments


class AbstractParseSuccessTestCase(AbstractMockTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class ItAllGoodManInvoiceTestCase(AbstractParseSuccessTestCase):
    _representation = 'success_invoice_parse'

    def get_data(self, mock_manager):
        self.invoice = mock_utils.create_invoice(mock_manager, external_id='Б-666-13')

        lines = [
            self._get_default_line(invoice_invoice='Б-666-13')
        ]
        return self._get_default_line('invoice_summary'), lines

    def get_result(self):
        return ut.Struct(invoice=self.invoice)


class ItAllGoodManOrderWOSumTestCase(AbstractParseSuccessTestCase):
    _representation = 'success_order_wo_sum_parse'

    def get_data(self, mock_manager):
        self.invoice = mock_utils.create_invoice(mock_manager, external_id='Б-666-13')
        self.order = mock_utils.create_order(mock_manager, service_order_id='666666')

        lines = [
            self._get_default_line(order_invoice='Б-666-13'),
            self._get_default_line(order='7-666666')
            ]
        return self._get_default_line('order_summary'), lines

    def get_result(self):
        return ut.Struct(invoice=self.invoice,
                         order=self.order,
                         sum_to_return=None)


class ItAllGoodManOrderWithSumTestCase(AbstractParseSuccessTestCase):
    _representation = 'success_order_with_sum_parse'

    def get_data(self, mock_manager):
        self.invoice = mock_utils.create_invoice(mock_manager, external_id='Б-666-13')
        self.order = mock_utils.create_order(mock_manager, service_order_id='666666')

        lines = [
            self._get_default_line(order_invoice='Б-666-13'),
            self._get_default_line(order='7-666666'),
            self._get_default_line(sum_to_return='666')
            ]
        return self._get_default_line('order_summary'), lines

    def get_result(self):
        return ut.Struct(invoice=self.invoice,
                         order=self.order,
                         sum_to_return=D(666))


@mock.patch('autodasha.solver_cl.reverse_invoice.ReverseInvoice.__init__',
            _fake_init)
@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in AbstractParseSuccessTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_parse_good(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data
    ri = IssueReport()
    req_comment = case.get_result()

    solver = ReverseInvoice

    solver_res = solver(mock_queue_object, issue)
    parsed_data = solver_res.parse_issue(ri)

    assert req_comment == parsed_data


class RequiredResult(case_utils.RequiredResult):
    def __init__(self, **kwargs):
        self.commit = kwargs.get('commit')
        self.assignee = 'autodasha'
        self.file_diff = kwargs.get('file_diff', False)
        super(RequiredResult, self).__init__(**kwargs)

    @staticmethod
    def get_comment(key, *args, **kwargs):
        return COMMENTS.get(key).format(*args, **kwargs)


class AbstractDBInvoiceTestCase(case_utils.AbstractDBTestCase):
    summary = 'Снять свободные средства по счету'
    _description = '''
    Возврат средств в рамках одного счета: {invoice_eid}
    Причина возврата: {reason}
    Снятие средств подтверждаю под свою ответственность: {responsibility}
    '''.strip()

    issue_key = 'test_reverse_invoice'

    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    def __init__(self):
        super(AbstractDBInvoiceTestCase, self).__init__()
        self.config = None
        self._temp_closed = 0

    def _get_data(self, session):
        raise NotImplementedError


class AbstractDBOrderTestCase(case_utils.AbstractDBTestCase):
    summary = 'Снять свободные средства с заказа по счету'
    _description = '''
    № счета: {invoice_eid}
    № заказа: {order_eid}
    Сумма к возврату: {sum_to_return}
    Причина возврата: {reason}
    Снятие средств подтверждаю под свою ответственность: {responsibility}
    '''.strip()

    issue_key = 'test_reverse_order'

    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    def __init__(self):
        super(AbstractDBOrderTestCase, self).__init__()
        self.config = None
        self._temp_closed = 0

    def _get_data(self, session):
        raise NotImplementedError


class AbstractDBFileTestCase(case_utils.AbstractDBTestCase):
    summary = 'Снять свободные средства по списку'
    _description = '''
    Причина возврата: {reason}
    Снятие средств подтверждаю под свою ответственность: {responsibility}
    '''.strip()

    issue_key = 'test_reverse_file'

    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    def __init__(self):
        super(AbstractDBFileTestCase, self).__init__()
        self.config = None
        self._temp_closed = 0

    @staticmethod
    def _get_file(titles, rows):
        buff = StringIO.StringIO()

        def get_row(md):
            return md.get('invoice_eid'), md.get('order_eid'), md.get('sum_to_return')

        buff.write(get_excel_document(titles, map(get_row, rows), 0))
        return buff.getvalue()

    def _get_data(self, session):
        raise NotImplementedError


class SimpleDBInvoiceTestCase(AbstractDBInvoiceTestCase):
    _representation = 'simple_invoice'
    _qty = 666

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client)
        consume_qty = 666

        self.invoice = db_utils.create_invoice(
            session,
            client,
            [(order, consume_qty)]
        )
        self.invoice.transfer(order, mapper.TransferMode.dst, self._qty, skip_check=True)

        return {
            'invoice_eid': self.invoice.external_id,
            'reason': 'My pink pet crab told me to!',
            'responsibility': 'Responsiwhat?'
        }

    def get_result(self):
        res = RequiredResult(commit=True)
        res.transition = IssueTransitions.fixed
        res.add_message(COMMENTS['done'].format(self.invoice.external_id))
        return res


class ZeroSumConsumeDBInvoiceTestCase(SimpleDBInvoiceTestCase):
    _representation = 'zero_sum_consume'
    _qty = D('0.0001')

    def get_result(self):
        res = RequiredResult(commit=True)
        res.transition = IssueTransitions.fixed
        res.add_message(COMMENTS['nothing_to_return'])
        return res


class NoFileDBFileTestCase(AbstractDBFileTestCase):
    _representation = 'no_file'

    def _get_data(self, session):
        return {
            'reason': 'Choose Life. Choose a job. Choose a career. Choose a family.',
            'responsibility': 'I won\'t be held responsible for my actions'
        }

    def get_result(self):
        res = RequiredResult(commit=True)
        res.transition = IssueTransitions.wont_fix
        res.add_message(COMMENTS['no_file'])
        return res


class ManyFilesDBFileTestCase(AbstractDBFileTestCase):
    _representation = 'many_files'

    def get_attachments(self):
        return [('data0.xlsx', 'Мне файл'), ('data2.xlsx', 'И моему сыну тоже')]

    def _get_data(self, session):
        return {
            'reason': 'Choose a fkng big television, choose washing machines, cars, '
                      'compact disc players and electrical tin openers.',
            'responsibility': 'I won\'t be held responsible for my actions'
        }

    def get_result(self):
        res = RequiredResult(commit=True)
        res.transition = IssueTransitions.wont_fix
        res.add_message(COMMENTS['many_files'])
        return res


class WrongExtensionDBFileTestCase(AbstractDBFileTestCase):
    _representation = 'incorrect_file_extension'

    def get_attachments(self):
        return [('i_am_an_excel_file.exe', 'Definitely not a virus')]

    def _get_data(self, session):
        return {
            'reason': 'Choose good health, low cholesterol, and dental insurance.',
            'responsibility': 'I won\'t be held responsible for my actions'
        }

    def get_result(self):
        res = RequiredResult(commit=True)
        res.transition = IssueTransitions.wont_fix
        res.add_message(COMMENTS['incorrect_file_extension'])
        return res


class EmptyFileDBFileTestCase(AbstractDBFileTestCase):
    _representation = 'empty_file'

    def get_attachments(self):
        titles = ['Счет', 'Заказ']
        rows = []
        return [('data0.xlsx', self._get_file(titles, rows))]

    def _get_data(self, session):
        return {
            'reason': 'Choose fixed interest mortgage repayments. '
                      'Choose a starter home. Choose your friends.',
            'responsibility': 'I won\'t be held responsible for my actions'
        }

    def get_result(self):
        res = RequiredResult(commit=True)
        res.transition = IssueTransitions.wont_fix
        res.add_message(COMMENTS['empty_file'])
        return res


class MaxFileLenExceededDBFileTestCase(AbstractDBFileTestCase):
    _representation = 'max_file_len_exceeded'

    def setup_config(self, session, config):
        self.config = config
        config['REVERSE_INVOICE_MAX_FILE_LENGTHS'] = 2

    def get_attachments(self):
        titles = ['Счет', 'Заказ']
        rows = [{'invoice_eid': 'Б-666-13', 'order_eid': '13-666666'},
                {'invoice_eid': 'Б-666-13', 'order_eid': '13-666666'},
                {'invoice_eid': 'Б-666-13', 'order_eid': '13-666666'}]

        return [('data0.xlsx', self._get_file(titles, rows))]

    def _get_data(self, session):
        return {
            'reason': 'Choose leisurewear and matching luggage. '
                      'Choose a three-piece suit on hire purchase '
                      'in a range of fkng fabrics.',
            'responsibility': 'I won\'t be held responsible for my actions'
        }

    def get_result(self):
        res = RequiredResult(commit=True)
        res.transition = IssueTransitions.wont_fix
        res.add_message(COMMENTS['max_file_len_exceeded'].format('2'))
        return res


class WrongCaptionsDBFileTestCase(AbstractDBFileTestCase):
    _representation = 'incorrect_caption'

    def get_attachments(self):
        titles = ['Счет', 'Заказ', 'Кровь девственниц', 'Слеза единорога', 'Бог']
        rows = [{'invoice_eid': 'Б-666-13', 'order_eid': '13-666666'}]
        return [('data0.xlsx', self._get_file(titles, rows))]

    def _get_data(self, session):
        return {
            'reason': 'Choose leisurewear and matching luggage. '
                      'Choose a three-piece suit on hire purchase '
                      'in a range of fkng fabrics.',
            'responsibility': 'I won\'t be held responsible for my actions'
        }

    def get_result(self):
        res = RequiredResult(commit=True)
        res.transition = IssueTransitions.wont_fix
        res.add_message(COMMENTS['incorrect_caption'])
        return res


class WrongInvoiceDBFileTestCase(AbstractDBFileTestCase):
    _representation = 'incorrect_invoice'

    def get_attachments(self):
        titles = ['Счет']
        rows = [{'invoice_eid': 'Ну вот этот вот счет'}]
        return [('data0.xlsx', self._get_file(titles, rows))]

    def _get_data(self, session):
        return {
            'reason': 'Choose DIY and wondering who the fk you are on Sunday morning.',
            'responsibility': 'I won\'t be held responsible for my actions'
        }

    def get_result(self):
        res = RequiredResult(commit=True)
        res.transition = IssueTransitions.wont_fix
        res.add_message(COMMENTS['wrong_invoice_format'].format('Ну вот этот вот счет'))
        return res


class InvoiceNotFoundDBFileTestCase(AbstractDBFileTestCase):
    _representation = 'invoice_not_found_file'

    def get_attachments(self):
        titles = ['Счет']
        rows = [{'invoice_eid': 'Б-666-13'}]
        return [('data0.xlsx', self._get_file(titles, rows))]

    def _get_data(self, session):
        return {
            'reason': 'Choose sitting on that couch watching mind-numbing, '
                      'spirit-crushing game shows, stuffing fkng junk food '
                      'into your mouth.',
            'responsibility': 'I won\'t be held responsible for my actions'
        }

    def get_result(self):
        res = RequiredResult(commit=True)
        res.transition = IssueTransitions.wont_fix
        res.add_message(COMMENTS['invoice_not_found'].format('Б-666-13'))
        return res


class WrongOrderDBFileTestCase(AbstractDBFileTestCase):
    _representation = 'incorrect_order'

    def get_attachments(self):
        titles = ['Счет', 'Заказ']
        rows = [{'invoice_eid': self.invoice.external_id,
                 'order_eid': 'Со всех заказов, пожалуйста'}]
        return [('data0.xlsx', self._get_file(titles, rows))]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client)
        consume_qty = 666

        self.invoice = db_utils.create_invoice(
            session,
            client,
            [(order, consume_qty)]
        )

        return {
            'reason': 'Choose rotting away at the end of it all, '
                      'pissing your last in a miserable home, nothing more '
                      'than an embarrassment to the selfish, '
                      'fked up brats you spawned to replace yourselves.',
            'responsibility': 'I won\'t be held responsible for my actions'
        }

    def get_result(self):
        res = RequiredResult(commit=True)
        res.transition = IssueTransitions.wont_fix
        res.add_message(COMMENTS['wrong_order_format'].
                        format('Со всех заказов, пожалуйста'))
        return res


class OrderIsNumberDBFileTestCase(AbstractDBFileTestCase):
    _representation = 'order_is_number'

    def get_attachments(self):
        titles = ['Счет', 'Заказ']
        rows = [{'invoice_eid': self.invoice.external_id,
                 'order_eid': 4815162342}]
        return [('data0.xlsx', self._get_file(titles, rows))]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client)
        consume_qty = 666

        self.invoice = db_utils.create_invoice(
            session,
            client,
            [(order, consume_qty)]
        )

        return {
            'reason': 'Choose rotting away at the end of it all, '
                      'pissing your last in a miserable home, nothing more '
                      'than an embarrassment to the selfish, '
                      'fked up brats you spawned to replace yourselves.',
            'responsibility': 'I won\'t be held responsible for my actions'
        }

    def get_result(self):
        res = RequiredResult(commit=True)
        res.transition = IssueTransitions.wont_fix
        res.add_message(COMMENTS['wrong_order_format'].
                        format('4815162342'))
        return res


class OrderNotFoundDBFileTestCase(AbstractDBFileTestCase):
    _representation = 'order_not_found_file'

    def get_attachments(self):
        titles = ['Счет', 'Заказ']
        rows = [{'invoice_eid': self.invoice.external_id, 'order_eid': '13-666666'}]
        return [('data0.xlsx', self._get_file(titles, rows))]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client)
        consume_qty = 666

        self.invoice = db_utils.create_invoice(
            session,
            client,
            [(order, consume_qty)]
        )

        return {
            'reason': 'Choose your future. Choose life...',
            'responsibility': 'I won\'t be held responsible for my actions'
        }

    def get_result(self):
        res = RequiredResult(commit=True)
        res.transition = IssueTransitions.wont_fix
        res.add_message(COMMENTS['order_not_found'].
                        format('13-666666'))
        return res


class WrongSumDBFileTestCase(AbstractDBFileTestCase):
    _representation = 'incorrect_sum_to_return'

    def get_attachments(self):
        titles = ['Счет', 'Заказ', 'Сумма']
        rows = [{'invoice_eid': self.invoice.external_id,
                 'order_eid': '%s-%s' %
                              (self.order.service_id, self.order.service_order_id),
                 'sum_to_return': 'Снимите все, по-братски'}]
        return [('data0.xlsx', self._get_file(titles, rows))]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.order = db_utils.create_order(session, client)
        consume_qty = 666

        self.invoice = db_utils.create_invoice(
            session,
            client,
            [(self.order, consume_qty)]
        )

        return {
            'reason': 'But why would I want to do a thing like that? '
                      'I chose not to choose life. I chose something else.',
            'responsibility': 'I won\'t be held responsible for my actions'
        }

    def get_result(self):
        res = RequiredResult(commit=True)
        res.transition = IssueTransitions.wont_fix
        res.add_message(COMMENTS['wrong_sum_to_return'].
                        format('Снимите все, по-братски',
                               self.invoice.external_id,
                               '%s-%s' %
                               (self.order.service_id, self.order.service_order_id)))
        return res


class InvoiceWithSumDBFileTestCase(AbstractDBFileTestCase):
    _representation = 'invoice_with_sum'

    def get_attachments(self):
        titles = ['Счет', 'Заказ', 'Сумма']
        rows = [{'invoice_eid': self.invoice.external_id,
                 'order_eid': '',
                 'sum_to_return': D('666')}]
        return [('data0.xlsx', self._get_file(titles, rows))]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.order = db_utils.create_order(session, client)
        consume_qty = 666

        self.invoice = db_utils.create_invoice(
            session,
            client,
            [(self.order, consume_qty)]
        )

        return {
            'reason': 'And the reasons? There are no reasons. '
                      'Who needs reasons when you\'ve got heroin?',
            'responsibility': 'I won\'t be held responsible for my actions'
        }

    def get_result(self):
        res = RequiredResult(commit=True)
        res.transition = IssueTransitions.wont_fix
        res.add_message(COMMENTS['invoice_with_sum'].format(self.invoice.external_id))
        return res


class InvoiceFineReverseDBFileTestCase(AbstractDBFileTestCase):
    _representation = 'invoice_receipt_less_reverse_sum'

    def get_attachments(self):
        titles = ['Счет', 'Заказ', 'Сумма']
        rows = [{'invoice_eid': self.invoice.external_id,
                 'order_eid': '%s-%s' %
                              (self.order.service_id, self.order.service_order_id),
                 'sum_to_return': D('666')}]
        return [('data0.xlsx', self._get_file(titles, rows))]

    def _get_data(self, session):
        contract_dt = ut.trunc_date(dt.datetime.now())
        client, person = db_utils.create_client_person(session)
        contract = db_utils.create_general_contract(
            session, client, person, contract_dt, payment_type=3
        )
        self.order = db_utils.create_order(session, client, product_id=503162)
        consume_qty = 666

        self.invoice = db_utils.create_fictive_invoice(
            session,
            contract,
            [(self.order, consume_qty)]
        )
        self.invoice.create_receipt(-665)

        return {
            'reason': 'And the reasons? There are no reasons. '
                      'Who needs reasons when you\'ve got heroin?',
            'responsibility': 'I won\'t be held responsible for my actions'
        }

    def get_result(self):
        res = RequiredResult(commit=True)
        res.transition = IssueTransitions.fixed
        res.add_message(COMMENTS['done'].format(self.invoice.external_id))
        return res


class CertReceiptDeleteDBFileTestCase(AbstractDBFileTestCase):
    _representation = 'cert_receipt_delete'

    def get_attachments(self):
        titles = ['Счет', 'Заказ', 'Сумма']
        rows = [{'invoice_eid': self.invoice.external_id,
                 'order_eid': '%s-%s' %
                              (self.order.service_id, self.order.service_order_id),
                 'sum_to_return': D('666')}]
        return [('data0.xlsx', self._get_file(titles, rows))]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.order = db_utils.create_order(session, client, product_id=503162)
        consume_qty = 666

        self.invoice = db_utils.create_invoice(
            session,
            client,
            [(self.order, consume_qty)],
            paysys_id=1006,
            turn_on=True
        )

        return {
            'reason': 'And the reasons? There are no reasons. '
                      'Who needs reasons when you\'ve got heroin?',
            'responsibility': 'I won\'t be held responsible for my actions'
        }

    def get_result(self):
        assert self.invoice.receipt_sum == D('0')
        res = RequiredResult(commit=True)
        res.transition = IssueTransitions.fixed
        res.add_message(COMMENTS['done'].format(self.invoice.external_id))
        return res


class TwoConsumesOneOrderDBFileTestCase(AbstractDBFileTestCase):
    _representation = 'two_consumes_one_order'

    def get_attachments(self):
        titles = ['Счет', 'Заказ', 'Сумма']
        rows = [{'invoice_eid': self.invoice.external_id,
                 'order_eid': '%s-%s' %
                              (self.order.service_id, self.order.service_order_id),
                 'sum_to_return': D('1443')}]
        return [('data0.xlsx', self._get_file(titles, rows))]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.order = db_utils.create_order(session, client, product_id=503162)
        consume_qty = 666

        self.invoice = db_utils.create_invoice(
            session,
            client,
            [(self.order, consume_qty)],
            turn_on=True
        )

        db_utils.consume_order(self.invoice, self.order, 777)

        return {
            'reason': 'And the reasons? There are no reasons. '
                      'Who needs reasons when you\'ve got heroin?',
            'responsibility': 'I won\'t be held responsible for my actions'
        }

    def get_result(self):
        res = RequiredResult(commit=True, file_diff=True)
        res.transition = IssueTransitions.fixed
        res.add_message(COMMENTS['done'].format(self.invoice.external_id))
        return res


class TwoOrdersOneInvoiceDBFileTestCase(AbstractDBFileTestCase):
    _representation = 'two_orders_one_invoice'

    def get_attachments(self):
        titles = ['Счет']
        rows = [{'invoice_eid': self.invoice.external_id}]
        return [('data0.xlsx', self._get_file(titles, rows))]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order1 = db_utils.create_order(session, client, product_id=503162)
        order2 = db_utils.create_order(session, client, product_id=503162)

        consume_qty1 = 666
        consume_qty2 = 777

        self.invoice = db_utils.create_invoice(
            session,
            client,
            [(order1, consume_qty1), (order2, consume_qty2)],
            turn_on=True
        )

        return {
            'reason': 'And the reasons? There are no reasons. '
                      'Who needs reasons when you\'ve got heroin?',
            'responsibility': 'I won\'t be held responsible for my actions'
        }

    def get_result(self):
        res = RequiredResult(commit=True)
        res.transition = IssueTransitions.fixed
        res.add_message(COMMENTS['done'].format(self.invoice.external_id))
        return res


class TaxiPersonalAccountDBFileTestCase(AbstractDBFileTestCase):
    _representation = 'taxi_personal_account'

    def get_attachments(self):
        titles = ['Счет']
        rows = [{'invoice_eid': self.invoice.external_id}]
        return [('data0.xlsx', self._get_file(titles, rows))]

    def _get_data(self, session):
        contract_dt = ut.trunc_date(dt.datetime.now())
        client, person = db_utils.create_client_person(session)

        contract = db_utils.create_general_contract(
            session, client, person, contract_dt, payment_type=3, services={111, 128}
        )
        self.order = db_utils.create_order(session, client, product_id=503162, service_id=111)

        invoice = db_utils.create_personal_account(
            session,
            person=person,
            contract=contract,
            dt_=contract_dt
        )
        external_num = invoice.external_id.split('-')
        external_num[0] = 'ЛСПРЛСПРРР'
        invoice.external_id = '-'.join(external_num)

        self.invoice = invoice
        self.invoice.transfer(self.order, 2, 30, skip_check=True)

        return {
            'reason': 'And the reasons? There are no reasons. '
                      'Who needs reasons when you\'ve got heroin?',
            'responsibility': 'I won\'t be held responsible for my actions'
        }

    def get_result(self):
        res = RequiredResult(commit=True)
        res.transition = IssueTransitions.fixed
        res.add_message(COMMENTS['done'].format(self.invoice.external_id))
        return res


def mock_staff(testfunc):
    other_dick = staff_utils.Person('pepe')
    other_boss = staff_utils.Person('other_boss')
    other_dept = staff_utils.Department('other_dept', [other_boss], [], [other_dick])

    yandex = staff_utils.Department(
        'yandex', childs=[other_dept]
    )

    staff = staff_utils.StaffMock(yandex)

    staff_path = 'autodasha.core.api.staff.Staff.%s'

    @mock.patch(
        staff_path % '_get_person_data',
        lambda s, *a, **k: staff._get_person_data(*a, **k),
    )
    @mock.patch(staff_path % '__init__', lambda *args: None)
    @functools.wraps(testfunc)
    def deco(session, issue_data):
        return testfunc(session, issue_data)

    return deco


@mock_staff
@pytest.mark.parametrize(
    'issue_data',
    [case() for case in AbstractDBFileTestCase._cases],
    ids=lambda case: str(case),
    indirect=['issue_data'],
)
def test_db(session, issue_data):
    queue_object, st_issue, case = issue_data
    solver = ReverseInvoice(queue_object, st_issue)

    solver.session = session
    solver_res = solver.solve()

    session.flush()
    session.expire_all()

    req_res = case.get_result()
    report = solver_res.issue_report

    assert solver_res.commit == req_res.commit

    report_comments = []
    for c in report.comments:
        c_text = c['text']
        c_attach = c['attachments']
        for part in c_text.strip().split('\n'):
            report_comments.append(part.strip())

        if c_attach:
            (res_attachment, ) = filter(lambda a:
                                        a.name == '%s_result.xls' % st_issue.key,
                                        c_attach)
            res_list = list(xls_reader_contents(res_attachment.getvalue()))
            res_qty = sum(map(lambda ra: ra['Qty'], res_list))
            res_sum = sum(map(lambda ra: ra['Sum'], res_list))
            res_data = map(lambda ra: tuple(ra.values()), res_list)

            (tech_attachment,) = filter(lambda a:
                                        a.name == '%s_tech_data.xls' % st_issue.key,
                                        c_attach)
            tech_list = list(xls_reader_contents(tech_attachment.getvalue()))
            tech_qty = sum(map(lambda ta: ta['Qty'], tech_list))
            tech_sum = sum(map(lambda ta: ta['Sum'], tech_list))
            tech_data = map(lambda ta: tuple(ta.values()), tech_list)

            assert res_qty == tech_qty
            assert res_sum == tech_sum

            if req_res.file_diff:
                assert len(set(tech_data)) > len(set(res_data))
            else:
                assert len(set(tech_data)) == len(set(res_data))

    req_res_comments = []
    for c in req_res.comments:
        for part in c.strip().split('\n'):
            req_res_comments.append(part.strip())

    report_comments = sorted(report_comments)
    req_res_comments = sorted(req_res_comments)

    assert len(report_comments) == len(req_res_comments)

    for i in range(len(req_res_comments)):
        assert req_res_comments[i] == report_comments[i]

    assert report.transition == req_res.transition
    assert report.assignee == req_res.assignee
