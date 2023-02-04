# -*- coding: utf-8 -*-

"test_promo_change"

from __future__ import unicode_literals

import datetime as dt
import itertools
import functools
import mock
import pytest
import StringIO
from sqlalchemy import orm

from autodasha.core.api.tracker import IssueTransitions
from autodasha.comments.promo_change import PromoChangeCommentsManager
from autodasha.db import mapper as a_mapper
from autodasha.solver_cl.promo_change import BaseSolver, PromoChangeSolver
from autodasha.utils.solver_utils import D
from autodasha.utils.xls_reader import xls_reader_contents
from balance.mapper import PromoCode
from balance.xls_export import get_excel_document

from tests.autodasha_tests.functional_tests import case_utils, mock_utils
from tests.autodasha_tests.common import db_utils, staff_utils

robot = 'autodasha'

COMMENTS = {
    'already_solved':
        'Эта задача уже была выполнена. Направьте новый запрос через '
        '((https://wiki.yandex-team.ru/otdelsoprovozhdeniya/dokument/avtomatformi/ '
        'формы)). Если не найдёте подходящую, заполните, пожалуйста, общую форму.',
    'empty_file':
        'Файл пустой. Заполни, пожалуйста, '
        'форму еще раз, приложив корректный файл.',
    'missing_code_col':
        'В файле отсутствует обязательная колонка \'code\'. Заполни, пожалуйста, '
        'форму еще раз, приложив корректный файл.',
    'cols_not_found':
        'Невозможно определить атрибуты для изменения. Заполни, пожалуйста, '
        'форму еще раз, приложив корректный файл.',
    'unknown_col':
        'В файле присутствует недопустимая колонка {}. Заполни, пожалуйста, '
        'форму еще раз, приложив корректный файл.',
    'done':
        'Готово. Отчет об измененных промокодах во вложении.',
    'failed':
        'Не удалось изменить промокоды. Отчет во вложении.',
    'unexpected_behaviour':
        'Произошла ошибка во время изменения прокодов:\n{}\n\n'
        'Задачу надо передать разработчикам.'
    }


def _fake_init(self, *args, **kwargs):
    super(PromoChangeSolver, self).__init__(*args, **kwargs)
    self.ro_session = self.session


class RequiredResult(case_utils.RequiredResult):
    def __init__(self, **kwargs):
        self.commit = kwargs.get('commit')
        self.delay = kwargs.get('delay')
        self.summonees = kwargs.get('summonees', [])
        self.assignee = 'autodasha'
        self.number_of_new_pc_groups = None
        self.error = None
        self.old_attrs = {}

        super(RequiredResult, self).__init__(**kwargs)

    @staticmethod
    def get_comment(key, *args, **kwargs):
        return COMMENTS.get(key).format(*args, **kwargs)


class AbstractDBTestCase(case_utils.AbstractDBTestCase):
    summary = 'Изменить промокод'
    _description = '''
    Вложение:\n{attachment}
    Комментарий:\n
    '''.strip()

    issue_key = 'test_promo_change'

    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    def __init__(self):
        super(AbstractDBTestCase, self).__init__()

    @staticmethod
    def _get_file(main_data):
        buff = StringIO.StringIO()

        titles = ['code', 'end_dt']

        def get_row(md):
            return md.get('code'), md.get('end_dt')

        buff.write(get_excel_document(titles, map(get_row, main_data), 0))
        return buff.getvalue()

    def _get_data(self, session):
        raise NotImplementedError


class MissingCodeCol(AbstractDBTestCase):
    _representation = 'missing_code_col'

    @staticmethod
    def _get_file(main_data):
        buff = StringIO.StringIO()

        titles = ['PrOmO_КОД', 'end_dt']

        def get_row(md):
            return md.get('code'), md.get('end_dt')

        buff.write(get_excel_document(titles, map(get_row, main_data), 0))
        return buff.getvalue()

    def get_attachments(self):
        main_data = [{'code': 'ABCDEFG', 'end_dt': '666-13-13'}]
        return [('data0.xlsx', self._get_file(main_data))]

    def _get_data(self, session):
        return {
            'attachment': 'https://pls-help-me-god.xlsx'
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.wont_fix
        res.add_message(COMMENTS['missing_code_col'])
        return res


class ColsNotFound(AbstractDBTestCase):
    _representation = 'cols_not_found'

    @staticmethod
    def _get_file(main_data):
        buff = StringIO.StringIO()

        titles = ['code']

        def get_row(md):
            return md.get('code')

        buff.write(get_excel_document(titles, map(get_row, main_data), 0))
        return buff.getvalue()

    def get_attachments(self):
        main_data = [{'code': 'ABCDEFG'}]
        return [('data0.xlsx', self._get_file(main_data))]

    def _get_data(self, session):
        return {
            'attachment': 'https://pls-help-me-god.xlsx'
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.wont_fix
        res.add_message(COMMENTS['cols_not_found'])
        return res


class UnknownCol(AbstractDBTestCase):
    _representation = 'unknown_col'

    @staticmethod
    def _get_file(main_data):
        buff = StringIO.StringIO()

        titles = ['code', 'due_dt']

        def get_row(md):
            return md.get('code'), md.get('end_dt')

        buff.write(get_excel_document(titles, map(get_row, main_data), 0))
        return buff.getvalue()

    def get_attachments(self):
        main_data = [{'code': 'ABCDEFG', 'end_dt': '666-13-13'}]
        return [('data0.xlsx', self._get_file(main_data))]

    def _get_data(self, session):
        return {
            'attachment': 'https://pls-help-me-god.xlsx'
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.wont_fix
        res.add_message(COMMENTS['unknown_col'].format('due_dt'))
        return res


class EmptyFile(AbstractDBTestCase):
    _representation = 'empty_file'

    @staticmethod
    def _get_file(main_data):
        buff = StringIO.StringIO()

        titles = ['code', 'end_dt']

        def get_row(md):
            return md.get('code'), md.get('end_dt')

        buff.write(get_excel_document(titles, map(get_row, main_data), 0))
        return buff.getvalue()

    def get_attachments(self):
        main_data = []
        return [('data0.xlsx', self._get_file(main_data))]

    def _get_data(self, session):
        return {
            'attachment': 'https://pls-help-me-god.xlsx'
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.wont_fix
        res.add_message(COMMENTS['empty_file'])
        return res


class PromoCodeNotFound(AbstractDBTestCase):
    _representation = 'promo_code_not_found'

    @staticmethod
    def _get_file(main_data):
        buff = StringIO.StringIO()

        titles = ['code', 'end_dt']

        def get_row(md):
            return md.get('code'), md.get('end_dt')

        buff.write(get_excel_document(titles, map(get_row, main_data), 0))
        return buff.getvalue()

    def get_attachments(self):
        main_data = [{'code': 'MYNEWPROMOCODE', 'end_dt': '2019-12-08'}]
        return [('data0.xlsx', self._get_file(main_data))]

    def _get_data(self, session):
        return {
            'attachment': 'https://pls-help-me-god.xlsx'
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.wont_fix
        res.add_message(COMMENTS['failed'])
        res.error = 'Не удалось найти промокод в базе данных'
        return res


class PromoCodeIsUsed(AbstractDBTestCase):
    _representation = 'promo_code_is_used'

    @staticmethod
    def _get_file(main_data):
        buff = StringIO.StringIO()

        titles = ['code', 'end_dt']

        def get_row(md):
            return md.get('code'), md.get('end_dt')

        buff.write(get_excel_document(titles, map(get_row, main_data), 0))
        return buff.getvalue()

    def get_attachments(self):
        main_data = [{'code': 'MYNEWPROMOCODE', 'end_dt': '2019-12-08'}]
        return [('data0.xlsx', self._get_file(main_data))]

    def _get_data(self, session):
        start_dt = dt.datetime.now() - dt.timedelta(days=1)
        end_dt = dt.datetime.now() + dt.timedelta(days=10)
        client, person = db_utils.create_client_person(session)
        promo_code = db_utils.create_promocode(
            session, start_dt, end_dt, 30, 10, code=u'MYNEWPROMOCODE'
        )
        db_utils.create_promocode_reservation(
            session, client, promocode=promo_code, start_dt=start_dt, end_dt=end_dt
        )
        order = db_utils.create_order(session, client=client)
        invoice = db_utils.create_invoice(
            session, client, [(order, 10)],
            person=person, promo_code_id=promo_code.id
        )
        invoice.create_receipt(10 * 30)
        invoice.turn_on_rows(apply_promocode=True)
        assert promo_code.is_used, promo_code.is_used

        return {
            'attachment': 'https://pls-help-me-god.xlsx'
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.wont_fix
        res.add_message(COMMENTS['failed'])
        res.error = 'Промокод уже был использован'
        return res


class ErrorNormalizingData(AbstractDBTestCase):
    _representation = 'error_normalizing_data'

    @staticmethod
    def _get_file(main_data):
        buff = StringIO.StringIO()

        titles = ['code', 'end_dt']

        def get_row(md):
            return md.get('code'), md.get('end_dt')

        buff.write(get_excel_document(titles, map(get_row, main_data), 0))
        return buff.getvalue()

    def get_attachments(self):
        main_data = [{'code': 'MYNEWPROMOCODE', 'end_dt': '19-12-08'}]
        return [('data0.xlsx', self._get_file(main_data))]

    def _get_data(self, session):
        start_dt = dt.datetime.now() - dt.timedelta(days=1)
        end_dt = dt.datetime.now() + dt.timedelta(days=10)
        db_utils.create_promocode(
            session, start_dt, end_dt, 30, 10, code=u'MYNEWPROMOCODE'
        )

        return {
            'attachment': 'https://pls-help-me-god.xlsx'
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.wont_fix
        res.add_message(COMMENTS['failed'])
        res.error = 'Ошибка во время преобразования данных промокода'
        return res


class UnexpectedErrorSelectingPromoCode(AbstractDBTestCase):
    _representation = 'unexpected_error_selecting_promo_code'

    @staticmethod
    def _get_file(main_data):
        buff = StringIO.StringIO()

        titles = ['code', 'end_dt']

        def get_row(md):
            return md.get('code'), md.get('end_dt')

        buff.write(get_excel_document(titles, map(get_row, main_data), 0))
        return buff.getvalue()

    def get_attachments(self):
        main_data = [{'code': 'MYNEWPROMOCODE', 'end_dt': '2019-12-08'}]
        return [('data0.xlsx', self._get_file(main_data))]

    def _get_data(self, session):
        start_dt = dt.datetime.now() - dt.timedelta(days=1)
        end_dt = dt.datetime.now() + dt.timedelta(days=10)
        db_utils.create_promocode(
            session, start_dt, end_dt, 30, 10, code=u'MYNEWPROMOCODE'
        )

        return {
            'attachment': 'https://pls-help-me-god.xlsx'
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.wont_fix
        res.add_message(COMMENTS['failed'])
        res.error = 'Ошибка во время получения промокода'
        return res


class MediumStuff(AbstractDBTestCase):
    _representation = 'partial_done'

    @staticmethod
    def _get_file(main_data):
        buff = StringIO.StringIO()

        titles = ['code', 'end_dt']

        def get_row(md):
            return md.get('code'), md.get('end_dt')

        buff.write(get_excel_document(titles, map(get_row, main_data), 0))
        return buff.getvalue()

    def get_attachments(self):
        main_data = [{'code': 'MYNEWPROMOCODE', 'end_dt': '2019-12-08'},
                     {'code': 'YOUWONTFINDME', 'end_dt': '2019-12-08'}]
        return [('data0.xlsx', self._get_file(main_data))]

    def _get_data(self, session):
        start_dt = dt.datetime.now() - dt.timedelta(days=1)
        end_dt = dt.datetime.now() + dt.timedelta(days=10)
        db_utils.create_promocode(
            session, start_dt, end_dt, 30, 10, code=u'MYNEWPROMOCODE'
        )

        return {
            'attachment': 'https://pls-help-me-god.xlsx'
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.fixed
        res.add_message(COMMENTS['done'])
        res.number_of_new_pc_groups = 1
        res.error = 'Не удалось найти промокод в базе данных'
        return res


class GoodStuff(AbstractDBTestCase):
    _representation = 'all_done'

    @staticmethod
    def _get_file(main_data):
        buff = StringIO.StringIO()

        titles = ['code', 'end_dt']

        def get_row(md):
            return md.get('code'), md.get('end_dt')

        buff.write(get_excel_document(titles, map(get_row, main_data), 0))
        return buff.getvalue()

    def get_attachments(self):
        main_data = [{'code': 'MYNEWPROMOCODE1', 'end_dt': '2019-12-08'},
                     {'code': 'MYNEWPROMOCODE2', 'end_dt': '2019-12-08'},
                     {'code': 'MYNEWPROMOCODE3', 'end_dt': '2019-12-09'},
                     {'code': 'MYNEWPROMOCODE4', 'end_dt': '2019-12-08'},
                     {'code': 'MYNEWPROMOCODE5', 'end_dt': '2019-12-09'}]
        return [('data0.xlsx', self._get_file(main_data))]

    def _get_data(self, session):
        start_dt = dt.datetime.now() - dt.timedelta(days=1)
        self.end_dt = dt.datetime.now() + dt.timedelta(days=10)
        promo1 = db_utils.create_promocode(
            session, start_dt, self.end_dt, 30, 10, code=u'MYNEWPROMOCODE1'
        )
        promo2 = db_utils.create_promocode(
            session, start_dt, self.end_dt, 30, 10, code=u'MYNEWPROMOCODE2'
        )
        promo2.group_id = promo1.group_id
        promo3 = db_utils.create_promocode(
            session, start_dt, self.end_dt, 30, 10, code=u'MYNEWPROMOCODE3'
        )
        promo3.group_id = promo1.group_id
        promo4 = db_utils.create_promocode(
            session, start_dt, self.end_dt, 30, 10, code=u'MYNEWPROMOCODE4'
        )
        promo5 = db_utils.create_promocode(
            session, start_dt, self.end_dt, 30, 10, code=u'MYNEWPROMOCODE5'
        )

        return {
            'attachment': 'https://pls-help-me-god.xlsx'
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.fixed
        res.add_message(COMMENTS['done'])
        res.number_of_new_pc_groups = 4
        res.old_attrs = {'end_dt_old': self.end_dt.replace(microsecond=0)}
        return res


def assert_false():
    assert False, 'FAIL'


@pytest.mark.parametrize(
    'issue_data',
    [case() for case in AbstractDBTestCase._cases],
    ids=lambda case: str(case),
    indirect=['issue_data'],
)
def test_db(session, issue_data):
    queue_object, st_issue, case = issue_data
    solver = PromoChangeSolver(queue_object, st_issue)

    solver.session = session
    if case._representation == 'unexpected_error_selecting_promo_code':
        with mock.patch(
            'autodasha.solver_cl.promo_change.PromoChangeSolver.get_unused_promo_codes',
            assert_false
        ):
            solver_res = solver.solve()
    else:
        solver_res = solver.solve()

    session.flush()
    session.expire_all()

    req_res = case.get_result()
    report = solver_res.issue_report

    assert solver_res.commit == req_res.commit
    assert solver_res.delay == req_res.delay

    report_comments = []
    for c in report.comments:
        c_text = c['text']
        c_attach = c['attachments']

        for part in c_text.strip().split('\n'):
            if part:
                report_comments.append(part.strip())

        if c_attach:
            res_attachment = filter(lambda a:
                                    a.name == '%s_result.xls' % st_issue.key,
                                    c_attach)
            if res_attachment:
                new_pc_groups = set()
                res_list = list(xls_reader_contents(res_attachment[0].getvalue()))

                for row in res_list:
                    new_pc_groups.add(row['pc_group_id'])
                    for k, v in req_res.old_attrs.items():
                        assert row[k] == v

                assert len(new_pc_groups) == req_res.number_of_new_pc_groups

            fail_attachment = filter(lambda a:
                                     a.name == '%s_errors.xls' % st_issue.key,
                                     c_attach)
            if fail_attachment:
                fail_list = list(xls_reader_contents(fail_attachment[0].getvalue()))
                for fl in fail_list:
                    assert fl['error'] == req_res.error

    req_res_comments = []
    for c in req_res.comments:
        for part in c.strip().split('\n'):
            if part:
                req_res_comments.append(part.strip())

    report_comments = sorted(report_comments)
    req_res_comments = sorted(req_res_comments)

    assert len(report_comments) == len(req_res_comments)

    for i in range(len(req_res_comments)):
        assert req_res_comments[i] == report_comments[i]

    assert report.transition == req_res.transition
    assert report.assignee == req_res.assignee
