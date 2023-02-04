# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import datetime
from dateutil.relativedelta import relativedelta
import StringIO

import mock
import pytest

from balance import mapper
from balance import muzzle_util as ut
from balance.xls_export import get_excel_document

from autodasha.core.api.tracker import IssueTransitions
from autodasha.solver_cl import (
    ContractDetalisation,
    TAXI_COMMISSION_SERVICES,
    FOOD_COMMISSION_SERVICES,
    BUS_TT_SERVICES,
    MEDIA_TT_SPECIAL_SERVICES
)
from autodasha.utils.transliterator import transliterate
from autodasha.utils.xls_reader import xls_reader_contents

from tests.autodasha_tests.common import db_utils
from tests.autodasha_tests.functional_tests import case_utils


COMMENTS = {
    'already_solved': 'Эта задача уже была выполнена. Направьте новый запрос через '
    '((https://wiki.yandex-team.ru/otdelsoprovozhdeniya/dokument/avtomatformi/ формы)). '
    'Если не найдёте подходящую, заполните, пожалуйста, общую форму.',
    'invalid_form': 'Задача создана некорректно.\n'
    '{}'
    'Пожалуйста, создай новую задачу через форму.',
    'invalid_contracts': 'Поле "Список договоров" не заполнено.\n',
    'invalid_begin_dt': 'Поле "С какой даты" не пусто и не соответствует формату "ГГГГ-ММ-ДД".\n',
    'invalid_end_dt': 'Поле "По какую дату" не пусто и не соответствует формату "ГГГГ-ММ-ДД".\n',
    'invalid_dates': 'Дата начала детализации должна быть строго меньше даты конца.\n',
    'invalid_trunc_months': 'Поле "Округлить период до полных месяцев" не заполнено или'
    ' имеет неожидаемое значение.\n',
    'invalid_split_months': 'Поле "Каждый месяц в отдельном файле" не заполнено или'
    ' имеет неожидаемое значение.\n',
    'contract_not_found': 'Договор {} не найден в Биллинге.\n',
    'contract_not_signed': 'Договор {} не подписан в Биллинге.\n',
    'comment_template': 'Договор {obj.contract.external_id} (id {obj.contract.id}), {obj.report_type},'
    ' c {obj.begin_dt:%Y-%m-%d} по {obj.end_dt:%Y-%m-%d}:\n{obj.comment}.\n',
    'unprocessable_contract': 'для сервисов данного договора отсутствуют обработчики для создания детализации',
    'success_report': 'выгрузка в файле %%{filename}%%',
    'fail_report': 'для указанного периода по договору нет данных',
}


def __init__(self, *args, **kwargs):
    super(ContractDetalisation, self).__init__(*args, **kwargs)
    self.ro_session = self.session


class AbstractMockTestCase(case_utils.AbstractMockTestCase):
    _default_lines = {
        'summary': 'Детализация платежей и комиссии по договорам',
        'contracts': 'Список договоров: %s',
        'begin_dt': 'С какой даты: %s',
        'end_dt': 'По какую дату: %s',
        'trunc_months': 'Округлить период до полных месяцев: %s',
        'split_months': 'Каждый месяц в отдельном файле: %s',
        #'comment': 'Комментарий: -'
    }


class AbstractParseFailTestCase(AbstractMockTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class AbstractParseSuccessTestCase(AbstractMockTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class InvalidContracts1Case(AbstractParseFailTestCase):
    _representation = 'invalid_contracts_1'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(contracts=''),
            self._get_default_line(begin_dt='2020-01-01'),
            self._get_default_line(end_dt='2020-02-01'),
            self._get_default_line(trunc_months='False'),
            self._get_default_line(split_months='False'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_form'].format(COMMENTS['invalid_contracts'])


class InvalidContracts2Case(AbstractParseFailTestCase):
    _representation = 'invalid_contracts_2'

    def get_data(self, mock_manager):
        contracts = '''
           ,   ;,
        ;   ,
        '''
        lines = [
            self._get_default_line(contracts=contracts),
            self._get_default_line(begin_dt='2020-01-01'),
            self._get_default_line(end_dt='2020-02-01'),
            self._get_default_line(trunc_months='False'),
            self._get_default_line(split_months='False'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_form'].format(COMMENTS['invalid_contracts'])


class InvalidBeginDt1Case(AbstractParseFailTestCase):
    _representation = 'invalid_begin_dt_1'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(contracts='TEST'),
            self._get_default_line(begin_dt='20.02.01'),
            self._get_default_line(end_dt='2020-02-01'),
            self._get_default_line(trunc_months='False'),
            self._get_default_line(split_months='False'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_form'].format(COMMENTS['invalid_begin_dt'])


class InvalidEndDt1Case(AbstractParseFailTestCase):
    _representation = 'invalid_end_dt_1'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(contracts='TEST'),
            self._get_default_line(begin_dt='2020-01-01'),
            self._get_default_line(end_dt='20.02.01'),
            self._get_default_line(trunc_months='False'),
            self._get_default_line(split_months='False'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_form'].format(COMMENTS['invalid_end_dt'])


class InvalidBeginDt2Case(AbstractParseFailTestCase):
    _representation = 'invalid_begin_dt_2'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(contracts='TEST'),
            self._get_default_line(begin_dt='2020-02-01'),
            self._get_default_line(end_dt='2020-02-01'),
            self._get_default_line(trunc_months='False'),
            self._get_default_line(split_months='False'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_form'].format(COMMENTS['invalid_dates'])


class InvalidBeginDt4Case(AbstractParseFailTestCase):
    _representation = 'invalid_begin_dt_4'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(contracts='TEST'),
            self._get_default_line(begin_dt='2020-03-01'),
            self._get_default_line(end_dt='2020-02-01'),
            self._get_default_line(trunc_months='False'),
            self._get_default_line(split_months='False'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_form'].format(COMMENTS['invalid_dates'])


class InvalidDatesCase(AbstractParseFailTestCase):
    _representation = 'invalid_dates'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(contracts='TEST'),
            self._get_default_line(begin_dt='2020-01-01'),
            self._get_default_line(end_dt='2019-12-01'),
            self._get_default_line(trunc_months='False'),
            self._get_default_line(split_months='False'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_form'].format(COMMENTS['invalid_dates'])


@mock.patch(
    'autodasha.solver_cl.contract_detalisation.ContractDetalisation.__init__', __init__
)
@pytest.mark.parametrize(
    'mock_issue_data',
    [case() for case in AbstractParseFailTestCase._cases],
    ids=lambda case: str(case),
    indirect=['mock_issue_data'],
)
def test_parse_fail_cases(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data
    required_res = case.get_result()
    solver = ContractDetalisation(mock_queue_object, issue)
    with pytest.raises(Exception) as e:
        solver.parse_issue()
    assert required_res == e.value.message


class ValidContracts1Case(AbstractParseSuccessTestCase):
    _representation = 'valid_contracts_1'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(contracts='TEST'),
            self._get_default_line(begin_dt='2020-01-01'),
            self._get_default_line(end_dt='2020-02-01'),
            self._get_default_line(trunc_months='False'),
            self._get_default_line(split_months='False'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return (
            ['TEST'],
            [(datetime.datetime(2020, 1, 1), datetime.datetime(2020, 2, 1))],
        )


class ValidContracts2Case(AbstractParseSuccessTestCase):
    _representation = 'valid_contracts_2'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(contracts='TEST, TEST/19 ; TEST/20'),
            self._get_default_line(begin_dt='2020-01-01'),
            self._get_default_line(end_dt='2020-02-01'),
            self._get_default_line(trunc_months='False'),
            self._get_default_line(split_months='False'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return (
            ['TEST', 'TEST/19', 'TEST/20'],
            [(datetime.datetime(2020, 1, 1), datetime.datetime(2020, 2, 1))],
        )


class ValidContracts3Case(AbstractParseSuccessTestCase):
    _representation = 'valid_contracts_3'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(contracts='TEST, TEST/19 ; TEST/20 TEST/19 TEST'),
            self._get_default_line(begin_dt='2020-01-01'),
            self._get_default_line(end_dt='2020-02-01'),
            self._get_default_line(trunc_months='False'),
            self._get_default_line(split_months='False'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return (
            ['TEST', 'TEST/19', 'TEST/20'],
            [(datetime.datetime(2020, 1, 1), datetime.datetime(2020, 2, 1))],
        )


class ValidDates1Case(AbstractParseSuccessTestCase):
    _representation = 'valid_dates_1'

    def get_data(self, mock_manager):
        self.end_dt = ut.month_first_day(datetime.datetime.now())
        lines = [
            self._get_default_line(contracts='TEST, TEST/19 ; TEST/20 TEST/19 TEST'),
            self._get_default_line(begin_dt=''),
            self._get_default_line(end_dt=self.end_dt.strftime('%Y-%m-%d')),
            self._get_default_line(trunc_months='False'),
            self._get_default_line(split_months='False'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return (
            ['TEST', 'TEST/19', 'TEST/20'],
            [(self.end_dt - relativedelta(months=1), self.end_dt)],
        )


class ValidDates2Case(AbstractParseSuccessTestCase):
    _representation = 'valid_dates_2'

    def get_data(self, mock_manager):
        self.end_dt = ut.month_first_day(datetime.datetime.now()) + relativedelta(
            days=10
        )
        lines = [
            self._get_default_line(contracts='TEST, TEST/19 ; TEST/20 TEST/19 TEST'),
            self._get_default_line(begin_dt=''),
            self._get_default_line(end_dt=self.end_dt.strftime('%Y-%m-%d')),
            self._get_default_line(trunc_months='False'),
            self._get_default_line(split_months='False'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return (
            ['TEST', 'TEST/19', 'TEST/20'],
            [(ut.month_first_day(datetime.datetime.now()), self.end_dt)],
        )


class ValidDates3Case(AbstractParseSuccessTestCase):
    _representation = 'valid_dates_3'

    def get_data(self, mock_manager):
        self.begin_dt = ut.month_first_day(datetime.datetime.now())
        lines = [
            self._get_default_line(contracts='TEST, TEST/19 ; TEST/20 TEST/19 TEST'),
            self._get_default_line(begin_dt=self.begin_dt.strftime('%Y-%m-%d')),
            self._get_default_line(end_dt=''),
            self._get_default_line(trunc_months='False'),
            self._get_default_line(split_months='False'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return (
            ['TEST', 'TEST/19', 'TEST/20'],
            [(self.begin_dt, self.begin_dt + relativedelta(months=1))],
        )


class ValidDates4Case(AbstractParseSuccessTestCase):
    _representation = 'valid_dates_4'

    def get_data(self, mock_manager):
        self.begin_dt = ut.trunc_date(datetime.datetime.now())
        lines = [
            self._get_default_line(contracts='TEST, TEST/19 ; TEST/20 TEST/19 TEST'),
            self._get_default_line(begin_dt=self.begin_dt.strftime('%Y-%m-%d')),
            self._get_default_line(end_dt=''),
            self._get_default_line(trunc_months='False'),
            self._get_default_line(split_months='False'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return (
            ['TEST', 'TEST/19', 'TEST/20'],
            [
                (
                    self.begin_dt,
                    ut.month_first_day(self.begin_dt) + relativedelta(months=1),
                )
            ],
        )


class ValidDates5Case(AbstractParseSuccessTestCase):
    _representation = 'valid_dates_5'

    def get_data(self, mock_manager):
        self.begin_dt = datetime.datetime(2019, 12, 5)
        self.end_dt = datetime.datetime(2020, 1, 20)
        lines = [
            self._get_default_line(contracts='TEST, TEST/19 ; TEST/20 TEST/19 TEST'),
            self._get_default_line(begin_dt=self.begin_dt.strftime('%Y-%m-%d')),
            self._get_default_line(end_dt=self.end_dt.strftime('%Y-%m-%d')),
            self._get_default_line(trunc_months='False'),
            self._get_default_line(split_months='False'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return ['TEST', 'TEST/19', 'TEST/20'], [(self.begin_dt, self.end_dt)]


class ValidDates6Case(AbstractParseSuccessTestCase):
    _representation = 'valid_dates_6'

    def get_data(self, mock_manager):
        self.begin_dt = mapper.ActMonth().begin_dt
        self.end_dt = mapper.ActMonth().end_dt
        lines = [
            self._get_default_line(contracts='TEST, TEST/19 ; TEST/20 TEST/19 TEST'),
            self._get_default_line(begin_dt=''),
            self._get_default_line(end_dt=''),
            self._get_default_line(trunc_months='False'),
            self._get_default_line(split_months='False'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return ['TEST', 'TEST/19', 'TEST/20'], [(self.begin_dt, self.end_dt)]


class ValidDates7Case(AbstractParseSuccessTestCase):
    _representation = 'valid_dates_7'

    def get_data(self, mock_manager):
        self.begin_dt = mapper.ActMonth().begin_dt
        self.end_dt = mapper.ActMonth().end_dt
        lines = [
            self._get_default_line(contracts='TEST, TEST/19 ; TEST/20 TEST/19 TEST'),
            self._get_default_line(begin_dt='None'),
            self._get_default_line(end_dt='None'),
            self._get_default_line(trunc_months='False'),
            self._get_default_line(split_months='False'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return ['TEST', 'TEST/19', 'TEST/20'], [(self.begin_dt, self.end_dt)]


class ValidDatesTrunc1Case(AbstractParseSuccessTestCase):
    _representation = 'valid_dates_trunc_1'

    def get_data(self, mock_manager):
        self.end_dt = ut.month_first_day(datetime.datetime.now())
        lines = [
            self._get_default_line(contracts='TEST, TEST/19 ; TEST/20 TEST/19 TEST'),
            self._get_default_line(begin_dt=''),
            self._get_default_line(end_dt=self.end_dt.strftime('%Y-%m-%d')),
            self._get_default_line(trunc_months='True'),
            self._get_default_line(split_months='False'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return (
            ['TEST', 'TEST/19', 'TEST/20'],
            [(self.end_dt - relativedelta(months=1), self.end_dt)],
        )


class ValidDatesTrunc2Case(AbstractParseSuccessTestCase):
    _representation = 'valid_dates_trunc_2'

    def get_data(self, mock_manager):
        self.end_dt = ut.month_first_day(datetime.datetime.now()) + relativedelta(
            days=10
        )
        lines = [
            self._get_default_line(contracts='TEST, TEST/19 ; TEST/20 TEST/19 TEST'),
            self._get_default_line(begin_dt=''),
            self._get_default_line(end_dt=self.end_dt.strftime('%Y-%m-%d')),
            self._get_default_line(trunc_months='True'),
            self._get_default_line(split_months='False'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return (
            ['TEST', 'TEST/19', 'TEST/20'],
            [
                (
                    ut.month_first_day(datetime.datetime.now()),
                    ut.month_first_day(self.end_dt + relativedelta(months=1)),
                )
            ],
        )


class ValidDatesTrunc3Case(AbstractParseSuccessTestCase):
    _representation = 'valid_dates_trunc_3'

    def get_data(self, mock_manager):
        self.begin_dt = ut.month_first_day(datetime.datetime.now())
        lines = [
            self._get_default_line(contracts='TEST, TEST/19 ; TEST/20 TEST/19 TEST'),
            self._get_default_line(begin_dt=self.begin_dt.strftime('%Y-%m-%d')),
            self._get_default_line(end_dt=''),
            self._get_default_line(trunc_months='True'),
            self._get_default_line(split_months='False'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return (
            ['TEST', 'TEST/19', 'TEST/20'],
            [(self.begin_dt, self.begin_dt + relativedelta(months=1))],
        )


class ValidDatesTrunc4Case(AbstractParseSuccessTestCase):
    _representation = 'valid_dates_trunc_4'

    def get_data(self, mock_manager):
        self.begin_dt = ut.trunc_date(datetime.datetime.now())
        lines = [
            self._get_default_line(contracts='TEST, TEST/19 ; TEST/20 TEST/19 TEST'),
            self._get_default_line(begin_dt=self.begin_dt.strftime('%Y-%m-%d')),
            self._get_default_line(end_dt=''),
            self._get_default_line(trunc_months='True'),
            self._get_default_line(split_months='False'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return (
            ['TEST', 'TEST/19', 'TEST/20'],
            [
                (
                    ut.month_first_day(self.begin_dt),
                    ut.month_first_day(self.begin_dt) + relativedelta(months=1),
                )
            ],
        )


class ValidDatesTrunc5Case(AbstractParseSuccessTestCase):
    _representation = 'valid_dates_trunc_5'

    def get_data(self, mock_manager):
        self.begin_dt = datetime.datetime(2020, 1, 5)
        self.end_dt = datetime.datetime(2020, 1, 20)
        lines = [
            self._get_default_line(contracts='TEST, TEST/19 ; TEST/20 TEST/19 TEST'),
            self._get_default_line(begin_dt=self.begin_dt.strftime('%Y-%m-%d')),
            self._get_default_line(end_dt=self.end_dt.strftime('%Y-%m-%d')),
            self._get_default_line(trunc_months='True'),
            self._get_default_line(split_months='False'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return (
            ['TEST', 'TEST/19', 'TEST/20'],
            [
                (
                    ut.month_first_day(self.begin_dt),
                    ut.month_first_day(self.end_dt) + relativedelta(months=1),
                )
            ],
        )


class ValidDatesTrunc6Case(AbstractParseSuccessTestCase):
    _representation = 'valid_dates_trunc_6'

    def get_data(self, mock_manager):
        self.begin_dt = datetime.datetime(2019, 12, 5)
        self.end_dt = datetime.datetime(2020, 1, 20)
        lines = [
            self._get_default_line(contracts='TEST, TEST/19 ; TEST/20 TEST/19 TEST'),
            self._get_default_line(begin_dt=self.begin_dt.strftime('%Y-%m-%d')),
            self._get_default_line(end_dt=self.end_dt.strftime('%Y-%m-%d')),
            self._get_default_line(trunc_months='True'),
            self._get_default_line(split_months='False'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return (
            ['TEST', 'TEST/19', 'TEST/20'],
            [
                (
                    ut.month_first_day(self.begin_dt),
                    ut.month_first_day(self.end_dt) + relativedelta(months=1),
                )
            ],
        )


class ValidDatesSplit1Case(AbstractParseSuccessTestCase):
    _representation = 'valid_dates_split_1'

    def get_data(self, mock_manager):
        self.end_dt = ut.month_first_day(datetime.datetime.now())
        lines = [
            self._get_default_line(contracts='TEST, TEST/19 ; TEST/20 TEST/19 TEST'),
            self._get_default_line(begin_dt=''),
            self._get_default_line(end_dt=self.end_dt.strftime('%Y-%m-%d')),
            self._get_default_line(trunc_months='False'),
            self._get_default_line(split_months='True'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return (
            ['TEST', 'TEST/19', 'TEST/20'],
            [(self.end_dt - relativedelta(months=1), self.end_dt)],
        )


class ValidDatesSplit2Case(AbstractParseSuccessTestCase):
    _representation = 'valid_dates_split_2'

    def get_data(self, mock_manager):
        self.end_dt = ut.month_first_day(datetime.datetime.now()) + relativedelta(
            days=10
        )
        lines = [
            self._get_default_line(contracts='TEST, TEST/19 ; TEST/20 TEST/19 TEST'),
            self._get_default_line(begin_dt=''),
            self._get_default_line(end_dt=self.end_dt.strftime('%Y-%m-%d')),
            self._get_default_line(trunc_months='False'),
            self._get_default_line(split_months='True'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return (
            ['TEST', 'TEST/19', 'TEST/20'],
            [(ut.month_first_day(datetime.datetime.now()), self.end_dt)],
        )


class ValidDatesSplit3Case(AbstractParseSuccessTestCase):
    _representation = 'valid_dates_split_3'

    def get_data(self, mock_manager):
        self.begin_dt = ut.month_first_day(datetime.datetime.now())
        lines = [
            self._get_default_line(contracts='TEST, TEST/19 ; TEST/20 TEST/19 TEST'),
            self._get_default_line(begin_dt=self.begin_dt.strftime('%Y-%m-%d')),
            self._get_default_line(end_dt=''),
            self._get_default_line(trunc_months='False'),
            self._get_default_line(split_months='True'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return (
            ['TEST', 'TEST/19', 'TEST/20'],
            [(self.begin_dt, self.begin_dt + relativedelta(months=1))],
        )


class ValidDatesSplit4Case(AbstractParseSuccessTestCase):
    _representation = 'valid_dates_split_4'

    def get_data(self, mock_manager):
        self.begin_dt = ut.trunc_date(datetime.datetime.now())
        lines = [
            self._get_default_line(contracts='TEST, TEST/19 ; TEST/20 TEST/19 TEST'),
            self._get_default_line(begin_dt=self.begin_dt.strftime('%Y-%m-%d')),
            self._get_default_line(end_dt=''),
            self._get_default_line(trunc_months='False'),
            self._get_default_line(split_months='True'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return (
            ['TEST', 'TEST/19', 'TEST/20'],
            [
                (
                    self.begin_dt,
                    ut.month_first_day(self.begin_dt) + relativedelta(months=1),
                )
            ],
        )


class ValidDatesSplit5Case(AbstractParseSuccessTestCase):
    _representation = 'valid_dates_split_5'

    def get_data(self, mock_manager):
        self.begin_dt = datetime.datetime(2020, 1, 5)
        self.end_dt = datetime.datetime(2020, 1, 20)
        lines = [
            self._get_default_line(contracts='TEST, TEST/19 ; TEST/20 TEST/19 TEST'),
            self._get_default_line(begin_dt=self.begin_dt.strftime('%Y-%m-%d')),
            self._get_default_line(end_dt=self.end_dt.strftime('%Y-%m-%d')),
            self._get_default_line(trunc_months='False'),
            self._get_default_line(split_months='True'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return ['TEST', 'TEST/19', 'TEST/20'], [(self.begin_dt, self.end_dt)]


class ValidDatesSplit6Case(AbstractParseSuccessTestCase):
    _representation = 'valid_dates_split_6'

    def get_data(self, mock_manager):
        self.begin_dt = datetime.datetime(2019, 12, 5)
        self.end_dt = datetime.datetime(2020, 1, 20)
        lines = [
            self._get_default_line(contracts='TEST, TEST/19 ; TEST/20 TEST/19 TEST'),
            self._get_default_line(begin_dt=self.begin_dt.strftime('%Y-%m-%d')),
            self._get_default_line(end_dt=self.end_dt.strftime('%Y-%m-%d')),
            self._get_default_line(trunc_months='False'),
            self._get_default_line(split_months='True'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return (
            ['TEST', 'TEST/19', 'TEST/20'],
            [
                (self.begin_dt, datetime.datetime(2020, 1, 1)),
                (datetime.datetime(2020, 1, 1), self.end_dt),
            ],
        )


class ValidDatesTruncSplit1Case(AbstractParseSuccessTestCase):
    _representation = 'valid_dates_trunc_split_1'

    def get_data(self, mock_manager):
        self.end_dt = ut.month_first_day(datetime.datetime.now())
        lines = [
            self._get_default_line(contracts='TEST, TEST/19 ; TEST/20 TEST/19 TEST'),
            self._get_default_line(begin_dt=''),
            self._get_default_line(end_dt=self.end_dt.strftime('%Y-%m-%d')),
            self._get_default_line(trunc_months='True'),
            self._get_default_line(split_months='True'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return (
            ['TEST', 'TEST/19', 'TEST/20'],
            [(self.end_dt - relativedelta(months=1), self.end_dt)],
        )


class ValidDatesTruncSplit2Case(AbstractParseSuccessTestCase):
    _representation = 'valid_dates_trunc_split_2'

    def get_data(self, mock_manager):
        self.end_dt = ut.month_first_day(datetime.datetime.now()) + relativedelta(
            days=10
        )
        lines = [
            self._get_default_line(contracts='TEST, TEST/19 ; TEST/20 TEST/19 TEST'),
            self._get_default_line(begin_dt=''),
            self._get_default_line(end_dt=self.end_dt.strftime('%Y-%m-%d')),
            self._get_default_line(trunc_months='True'),
            self._get_default_line(split_months='True'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return (
            ['TEST', 'TEST/19', 'TEST/20'],
            [
                (
                    ut.month_first_day(datetime.datetime.now()),
                    ut.month_first_day(datetime.datetime.now())
                    + relativedelta(months=1),
                )
            ],
        )


class ValidDatesTruncSplit3Case(AbstractParseSuccessTestCase):
    _representation = 'valid_dates_trunc_split_3'

    def get_data(self, mock_manager):
        self.begin_dt = ut.month_first_day(datetime.datetime.now())
        lines = [
            self._get_default_line(contracts='TEST, TEST/19 ; TEST/20 TEST/19 TEST'),
            self._get_default_line(begin_dt=self.begin_dt.strftime('%Y-%m-%d')),
            self._get_default_line(end_dt=''),
            self._get_default_line(trunc_months='True'),
            self._get_default_line(split_months='True'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return (
            ['TEST', 'TEST/19', 'TEST/20'],
            [(self.begin_dt, self.begin_dt + relativedelta(months=1))],
        )


class ValidDatesTruncSplit4Case(AbstractParseSuccessTestCase):
    _representation = 'valid_dates_trunc_split_4'

    def get_data(self, mock_manager):
        self.begin_dt = ut.trunc_date(datetime.datetime.now())
        lines = [
            self._get_default_line(contracts='TEST, TEST/19 ; TEST/20 TEST/19 TEST'),
            self._get_default_line(begin_dt=self.begin_dt.strftime('%Y-%m-%d')),
            self._get_default_line(end_dt=''),
            self._get_default_line(trunc_months='True'),
            self._get_default_line(split_months='True'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return (
            ['TEST', 'TEST/19', 'TEST/20'],
            [
                (
                    ut.month_first_day(self.begin_dt),
                    ut.month_first_day(self.begin_dt) + relativedelta(months=1),
                )
            ],
        )


class ValidDatesTruncSplit5Case(AbstractParseSuccessTestCase):
    _representation = 'valid_dates_trunc_split_5'

    def get_data(self, mock_manager):
        self.begin_dt = datetime.datetime(2020, 1, 5)
        self.end_dt = datetime.datetime(2020, 1, 20)
        lines = [
            self._get_default_line(contracts='TEST, TEST/19 ; TEST/20 TEST/19 TEST'),
            self._get_default_line(begin_dt=self.begin_dt.strftime('%Y-%m-%d')),
            self._get_default_line(end_dt=self.end_dt.strftime('%Y-%m-%d')),
            self._get_default_line(trunc_months='True'),
            self._get_default_line(split_months='True'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return (
            ['TEST', 'TEST/19', 'TEST/20'],
            [
                (
                    ut.month_first_day(self.begin_dt),
                    ut.month_first_day(self.end_dt) + relativedelta(months=1),
                )
            ],
        )


class ValidDatesTruncSplit6Case(AbstractParseSuccessTestCase):
    _representation = 'valid_dates_trunc_split_6'

    def get_data(self, mock_manager):
        self.begin_dt = datetime.datetime(2019, 12, 5)
        self.end_dt = datetime.datetime(2020, 1, 20)
        lines = [
            self._get_default_line(contracts='TEST, TEST/19 ; TEST/20 TEST/19 TEST'),
            self._get_default_line(begin_dt=self.begin_dt.strftime('%Y-%m-%d')),
            self._get_default_line(end_dt=self.end_dt.strftime('%Y-%m-%d')),
            self._get_default_line(trunc_months='True'),
            self._get_default_line(split_months='True'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return (
            ['TEST', 'TEST/19', 'TEST/20'],
            [
                (ut.month_first_day(self.begin_dt), datetime.datetime(2020, 1, 1)),
                (datetime.datetime(2020, 1, 1), datetime.datetime(2020, 2, 1)),
            ],
        )


class ValidDatesTruncSplit7Case(AbstractParseSuccessTestCase):
    _representation = 'valid_dates_trunc_split_7'

    def get_data(self, mock_manager):
        self.begin_dt = datetime.datetime(2019, 12, 5)
        self.end_dt = datetime.datetime(2020, 2, 1)
        lines = [
            self._get_default_line(contracts='TEST, TEST/19 ; TEST/20 TEST/19 TEST'),
            self._get_default_line(begin_dt=self.begin_dt.strftime('%Y-%m-%d')),
            self._get_default_line(end_dt=self.end_dt.strftime('%Y-%m-%d')),
            self._get_default_line(trunc_months='True'),
            self._get_default_line(split_months='True'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return (
            ['TEST', 'TEST/19', 'TEST/20'],
            [
                (ut.month_first_day(self.begin_dt), datetime.datetime(2020, 1, 1)),
                (datetime.datetime(2020, 1, 1), datetime.datetime(2020, 2, 1)),
            ],
        )


class ValidDatesTruncSplit8Case(AbstractParseSuccessTestCase):
    _representation = 'valid_dates_trunc_split_8'

    def get_data(self, mock_manager):
        self.begin_dt = datetime.datetime(2019, 12, 1)
        self.end_dt = datetime.datetime(2020, 2, 1)
        lines = [
            self._get_default_line(contracts='TEST, TEST/19 ; TEST/20 TEST/19 TEST'),
            self._get_default_line(begin_dt=self.begin_dt.strftime('%Y-%m-%d')),
            self._get_default_line(end_dt=self.end_dt.strftime('%Y-%m-%d')),
            self._get_default_line(trunc_months='True'),
            self._get_default_line(split_months='True'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return (
            ['TEST', 'TEST/19', 'TEST/20'],
            [
                (self.begin_dt, datetime.datetime(2020, 1, 1)),
                (datetime.datetime(2020, 1, 1), datetime.datetime(2020, 2, 1)),
            ],
        )


@mock.patch(
    'autodasha.solver_cl.contract_detalisation.ContractDetalisation.__init__', __init__
)
@pytest.mark.parametrize(
    'mock_issue_data',
    [case() for case in AbstractParseSuccessTestCase._cases],
    ids=lambda case: str(case),
    indirect=['mock_issue_data'],
)
def test_parse_success_cases(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data
    required_res = case.get_result()
    solver = ContractDetalisation(mock_queue_object, issue)
    assert required_res == solver.parse_issue()


class AbstractDBTestCase(case_utils.AbstractDBTestCase):
    __metaclass__ = case_utils.TestCaseMeta

    _summary = 'Детализация платежей и комиссии по договорам'
    _description = '''
Список договоров: {contract_eids}
С какой даты: {begin_dt}
По какую дату: {end_dt}
Округлить период до полных месяцев: {trunc_months}
Каждый месяц в отдельном файле: {split_months}
        '''
    filename_template = (
        '{self.report_type}_{self.contract.external_id}_{self.contract.id}'
        '__{self.begin_dt:%Y-%m-%d}__{self.end_dt:%Y-%m-%d}'
    )
    _cases = []

    def __init__(self):
        super(AbstractDBTestCase, self).__init__()
        self.config = None
        self.filenames = set()

    def get_description(self, session):
        (
            contracts,
            contract_eids,
            begin_dt,
            end_dt,
            trunc_months,
            split_months,
        ) = self._get_data(session)
        return self._description.format(
            contract_eids=', '.join(contract_eids),
            begin_dt=datetime.datetime.strftime(begin_dt, '%Y-%m-%d')
            if begin_dt
            else '',
            end_dt=datetime.datetime.strftime(end_dt, '%Y-%m-%d') if end_dt else '',
            trunc_months=unicode(trunc_months),
            split_months=unicode(split_months),
        )

    def get_filename(self, contract, begin_dt, end_dt, report_type):
        report = ut.Struct(
            contract=contract, begin_dt=begin_dt, end_dt=end_dt, report_type=report_type
        )
        filename = self.filename_template.format(self=report)
        for ch in ('/', '\\'):
            filename = filename.replace(ch, '-')
        num = 1
        prev_filename = filename
        while prev_filename in self.filenames:
            prev_filename = filename + '({})'.format(num)
            num += 1
        filename = prev_filename
        self.filenames.add(filename)
        return transliterate(filename + '.xls')

    def get_success_message(self, contract, begin_dt, end_dt, report_type):
        filename = self.get_filename(contract, begin_dt, end_dt, report_type)
        report = ut.Struct(
            contract=contract,
            report_type=report_type,
            filename=filename,
            comment=COMMENTS['success_report'].format(filename=filename),
            begin_dt=begin_dt,
            end_dt=end_dt,
        )
        return ut.Struct(
            text=COMMENTS['comment_template'].format(obj=report), filename=filename
        )

    def get_single_contract_reports(
        self, contract, begin_dt, end_dt, trunc_months, split_months
    ):
        tt_services = set(self.config['DETALISATION_ALLOWED_THIRDPARTY_SERVICES'])
        c_services = set(contract.col0.services)
        service_ids = c_services & tt_services
        if trunc_months:
            begin_dt = ut.month_first_day(begin_dt)
            if end_dt != ut.month_first_day(end_dt):
                end_dt = ut.month_first_day(end_dt + relativedelta(months=1))
        min_begin_dt = begin_dt
        max_end_dt = end_dt
        if split_months:
            periods = []
            dt = begin_dt
            while dt < end_dt:
                period_begin_dt = dt
                period_end_dt = min(
                    ut.month_first_day(dt + relativedelta(months=1)), end_dt
                )
                dt = period_end_dt
                periods.append((period_begin_dt, period_end_dt))
        else:
            periods = [(begin_dt, end_dt)]

        patch_data = []
        messages = []
        attachments = {}
        if service_ids:
            for begin_dt, end_dt in periods:
                report_type = 'детализация транзакций'
                success_comment = self.get_success_message(
                    contract, begin_dt, end_dt, report_type
                )
                messages.append(success_comment.text)
                query = self.union_all_queries(
                    self,
                    [
                        self.gen_thirdparty_rows(
                            self, contract.id, service_id, begin_dt, end_dt
                        ).format(services=service_id)
                        for service_id in service_ids
                    ],
                )
                params = ut.Struct(
                    contract_id=contract.id, begin_dt=begin_dt, end_dt=end_dt
                )
                attachments[success_comment.filename] = (query, params)
            service_ids.add(6666)
            query = self.union_all_queries(
                self,
                [
                    self.gen_thirdparty_rows(
                        self,
                        contract.id,
                        service_id,
                        min_begin_dt - relativedelta(months=1),
                        max_end_dt + relativedelta(months=1),
                    )
                    for service_id in service_ids
                ],
            )
            patch_data.append(
                ut.Struct(
                    obj='autodasha.solver_cl.contract_detalisation.TTReportConstructor.get_query',
                    value=query,
                )
            )
        if c_services & TAXI_COMMISSION_SERVICES:
            for begin_dt, end_dt in periods:
                report_type = 'детализация комиссии'
                success_comment = self.get_success_message(
                    contract, begin_dt, end_dt, report_type
                )
                messages.append(success_comment.text)
                query = self.gen_taxi_commission_rows(
                    self, contract.id, begin_dt, end_dt
                )
                params = ut.Struct(
                    contract_id=contract.id, begin_dt=begin_dt, end_dt=end_dt
                )
                attachments[success_comment.filename] = (query, params)
            query = self.gen_taxi_commission_rows(
                self,
                contract.id,
                min_begin_dt - relativedelta(months=1),
                max_end_dt + relativedelta(months=1),
            )
            patch_data.append(
                ut.Struct(
                    obj='autodasha.solver_cl.contract_detalisation.TaxiCommissionReportConstructor.get_query',
                    value=query,
                )
            )
        if c_services & FOOD_COMMISSION_SERVICES:
            service_ids = c_services & FOOD_COMMISSION_SERVICES
            for begin_dt, end_dt in periods:
                report_type = 'детализация комиссии'
                success_comment = self.get_success_message(
                    contract, begin_dt, end_dt, report_type
                )
                messages.append(success_comment.text)
                query = self.union_all_queries(
                    self,
                    [
                        self.gen_food_commission_rows(
                            self, contract.id, service_id, begin_dt, end_dt, 'goods'
                        ).format(services=service_id)
                        for service_id in service_ids,
                        self.gen_food_commission_rows(
                            self, contract.id, service_id, begin_dt, end_dt, 'pickup'
                        ).format(services=service_id)
                        for service_id in service_ids
                    ],
                )
                params = ut.Struct(
                    contract_id=contract.id, begin_dt=begin_dt, end_dt=end_dt
                )
                attachments[success_comment.filename] = (query, params)
            service_ids.add(6666)
            query = self.union_all_queries(
                self,
                [
                    self.gen_food_commission_rows(
                        self,
                        contract.id,
                        service_id,
                        min_begin_dt - relativedelta(months=1),
                        max_end_dt + relativedelta(months=1),
                        'order'
                    )
                    for service_id in service_ids
                ],
            )
            patch_data.append(
                ut.Struct(
                    obj='autodasha.solver_cl.contract_detalisation.FoodCommissionReportConstructor.get_query',
                    value=query,
                )
            )
        if c_services & BUS_TT_SERVICES:
            service_ids = c_services & BUS_TT_SERVICES
            for begin_dt, end_dt in periods:
                report_type = 'детализация транзакций'
                success_comment = self.get_success_message(
                    contract, begin_dt, end_dt, report_type
                )
                messages.append(success_comment.text)
                query = self.union_all_queries(
                    self,
                    [
                        self.gen_thirdparty_rows(
                            self, contract.id, service_id, begin_dt, end_dt
                        ).format(services=service_id)
                        for service_id in service_ids
                    ],
                )
                params = ut.Struct(
                    contract_id=contract.id, begin_dt=begin_dt, end_dt=end_dt
                )
                attachments[success_comment.filename] = (query, params)
            service_ids.add(6666)
            query = self.union_all_queries(
                self,
                [
                    self.gen_thirdparty_rows(
                        self,
                        contract.id,
                        service_id,
                        min_begin_dt - relativedelta(months=1),
                        max_end_dt + relativedelta(months=1),
                    )
                    for service_id in service_ids
                ],
            )
            patch_data.append(
                ut.Struct(
                    obj='autodasha.solver_cl.contract_detalisation.BusTTReportConstructor.get_query',
                    value=query,
                )
            )
        if c_services & MEDIA_TT_SPECIAL_SERVICES:
            service_ids = c_services & MEDIA_TT_SPECIAL_SERVICES
            for begin_dt, end_dt in periods:
                report_type = 'детализация транзакций'
                success_comment = self.get_success_message(
                    contract, begin_dt, end_dt, report_type
                )
                messages.append(success_comment.text)
                query = self.union_all_queries(
                    self,
                    [
                        self.gen_thirdparty_rows(
                            self, contract.id, service_id, begin_dt, end_dt
                        ).format(services=service_id)
                        for service_id in service_ids
                    ],
                )
                params = ut.Struct(
                    contract_id=contract.id, begin_dt=begin_dt, end_dt=end_dt
                )
                attachments[success_comment.filename] = (query, params)
            service_ids.add(6666)
            query = self.union_all_queries(
                self,
                [
                    self.gen_thirdparty_rows(
                        self,
                        contract.id,
                        service_id,
                        min_begin_dt - relativedelta(months=1),
                        max_end_dt + relativedelta(months=1),
                    )
                    for service_id in service_ids
                ],
            )
            patch_data.append(
                ut.Struct(
                    obj='autodasha.solver_cl.contract_detalisation.MediaTTReportConstructor.get_query',
                    value=query,
                )
            )
        return '\n'.join(messages), attachments, patch_data

    @staticmethod
    def gen_taxi_commission_rows(cls, contract_id, begin_dt, end_dt):
        subquery_row = """
select
  date'{dt:%Y-%m-%d}' dt,
  {contract_id} contract_id,
  '{external_id}' external_id,
  '{payment_type}' payment_type,
  {commission_sum} commission_sum,
  {promocode_sum} promocode_sum,
  {subsidy_sum} subsidy_sum,
  '{currency}' currency
from dual
            """
        subquery_rows = []
        subquery_params = {
            'contract_id': contract_id,
            'external_id': 'external_id',
            'payment_type': 'payment_type',
            'currency': 'currency',
            'commission_sum': 1,
            'promocode_sum': 2,
            'subsidy_sum': 3,
        }
        dt = begin_dt
        while dt < end_dt:
            subquery_rows.append(subquery_row.format(dt=dt, **subquery_params))
            dt += datetime.timedelta(days=1)
        subquery_rows = cls.union_all_queries(cls, subquery_rows)
        query = """
select dt "Дата",
    external_id "Договор",
    payment_type "Тип платежа",
    commission_sum "Комиссия",
    promocode_sum "Промокоды",
    subsidy_sum "Субсидии",
    currency "Валюта"
from
  (select * from ({subquery})
  where contract_id = :contract_id
    and dt >= :begin_dt
    and dt < :end_dt)
            """
        return query.format(subquery=subquery_rows)

    @staticmethod
    def gen_food_commission_rows(cls, contract_id, service_id, begin_dt, end_dt, type_):
        subquery_row = """
select
  {client_id} client_id,
  {contract_id} contract_id,
  '{external_id}' external_id,
  date'{dt:%Y-%m-%d}' dt,
  {commission_sum} commission_sum,
  '{currency_chr}' currency_chr,
  {service_id} service_id,
  '{type}' type
from dual
            """
        subquery_rows = []
        subquery_params = {
            'client_id': contract_id,
            'contract_id': contract_id,
            'external_id': 'external_id',
            'currency_chr': 'currency_chr',
            'commission_sum': 1,
            'service_id': service_id,
            'type': type_
        }
        dt = begin_dt
        while dt < end_dt:
            subquery_rows.append(subquery_row.format(dt=dt, **subquery_params))
            dt += datetime.timedelta(days=1)
        subquery_rows = cls.union_all_queries(cls, subquery_rows)
        query = """
select
    client_id "ID клиента",
    external_id "Договор",
    dt "Дата",
    sum(commission_sum) "Комиссия",
    currency_chr "Валюта",
    type "Тип"
from
  (select * from ({subquery})
  where contract_id = :contract_id
  and dt >= :begin_dt
  and dt < :end_dt
  and service_id in ({{services}}))
group by client_id, external_id, dt, currency_chr, type
            """
        return query.format(subquery=subquery_rows)

    @staticmethod
    def gen_thirdparty_rows(cls, contract_id, service_id, begin_dt, end_dt):
        subquery_row = """
select
    date'{dt:%Y-%m-%d}' dt,
    '{external_id}' external_id,
    {contract_id} contract_id,
    {service_id} service_id,
    '{name}' name,
    '{service_order_id_str}' service_order_id_str,
    '{developer_payload}' developer_payload,
    '{trust_id}' trust_id,
    '{trust_payment_id}' trust_payment_id,
    '{payment_type}' payment_type,
    '{transaction_type}' transaction_type,
    '{partner_currency}' partner_currency,
    {amount} amount,
    {row_paysys_commission_sum} row_paysys_commission_sum,
    {yandex_reward} yandex_reward,
    '{oebs_exportable}' oebs_exportable,
    '{oebs_exported}' oebs_exported,
    date'{payment_date:%Y-%m-%d}' payment_date,
    {payment_number} payment_number,
    '{payment_details}' payment_details,
    {invoice_amount} invoice_amount,
    '{invoice_status}' invoice_status,
    '{invoice_state}' invoice_state
from dual
            """
        subquery_rows = []
        subquery_params = {
            'contract_id': contract_id,
            'service_id': service_id,
            'external_id': 'external_id',
            'name': 'name',
            'service_order_id_str': 'service_order_id_str',
            'developer_payload': 'developer_payload',
            'trust_id': 'trust_id',
            'trust_payment_id': 'trust_payment_id',
            'payment_type': 'payment_type',
            'transaction_type': 'transaction_type',
            'partner_currency': 'partner_currency',
            'amount': 1,
            'row_paysys_commission_sum': 2,
            'yandex_reward': 3,
            'oebs_exportable': 'oebs_exportable',
            'oebs_exported': 'oebs_exported',
            'payment_date': begin_dt,
            'payment_number': 0,
            'payment_details': 'payment_details',
            'invoice_amount': 1,
            'invoice_status': 'invoice_status',
            'invoice_state': 'invoice_state',
        }
        dt = begin_dt
        while dt < end_dt:
            subquery_rows.append(subquery_row.format(dt=dt, **subquery_params))
            dt += datetime.timedelta(days=1)
        subquery_rows = cls.union_all_queries(cls, subquery_rows)
        query = """
select
     dt "Дата",
     external_id "Договор",
     name "Сервис",
     service_order_id_str "Заказ",
     developer_payload "Доп. сведения (payload)",
     trust_id "Транзакция",
     trust_payment_id "Транз. платежа",
     payment_type "Тип платежа",
     transaction_type "Тип транзакции",
     partner_currency "Валюта",
     amount "Сумма",
     row_paysys_commission_sum "Комиссия пл.с.",
     yandex_reward "Ком. Яндекса",
     oebs_exportable "Выгружаемо в OEBS",
     oebs_exported "Выгружено в OEBS",
     payment_date "Дата п/п",
     payment_number "Номер п/п",
     payment_details "п/п",
     invoice_amount "Сумма п/п",
     invoice_status "Статус п/п",
     invoice_state "Состояние п/п"
from (select * from ({subquery})
  where contract_id = :contract_id
  and dt >= :begin_dt
  and dt < :end_dt
  and service_id in ({{services}}))
            """
        return query.format(subquery=subquery_rows)

    @staticmethod
    def union_all_queries(cls, queries):
        return '\nunion all\n'.join(queries)

    def setup_config(self, session, config):
        config['OEBS_INVOICE_DATA_TABLE'] = 'bo.mv_oebs_agent_invoice_data_daily_f'
        config['THIRDPARTY_PAYMENTS_QUERY_PARALLEL_HINT'] = ''
        config['DETALISATION_ALLOWED_THIRDPARTY_SERVICES'] = [
            s.id for s in session.query(mapper.ThirdPartyService) if s.id not in BUS_TT_SERVICES
        ]

        config['DETALISATION_REPORT_TYPES'] = {
            "детализация транзакций": [666]
        }
        self.config = config


class RequiredResult(case_utils.RequiredResult):
    def __init__(self, **kwargs):
        self.patch_data = []
        self.attachments = dict()

        super(RequiredResult, self).__init__(**kwargs)


class SingleNotFoundCase(AbstractDBTestCase):
    _representation = 'single_not_found'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        contract = db_utils.create_general_contract(
            session,
            client,
            person,
            on_dt=datetime.datetime(2019, 1, 1),
            services={111, 124, 128},
        )
        self.contracts = [contract]
        self.begin_dt = datetime.datetime(2019, 12, 1)
        self.end_dt = datetime.datetime(2020, 1, 1)
        self.trunc_months = True
        self.split_months = True

        return (
            self.contracts,
            ['TEST/0'],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )

    def get_result(self):
        res = RequiredResult(
            transition=IssueTransitions.fixed,
            messages=[COMMENTS['contract_not_found'].format('TEST/0')],
        )
        return res


class SingleNotSignedCase(AbstractDBTestCase):
    _representation = 'single_not_signed'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        contract = db_utils.create_general_contract(
            session,
            client,
            person,
            on_dt=datetime.datetime(2019, 1, 1),
            services={111, 124, 128},
        )
        contract.col0.is_signed = None
        session.flush()
        self.contracts = [contract]
        self.begin_dt = datetime.datetime(2019, 12, 1)
        self.end_dt = datetime.datetime(2020, 1, 1)
        self.trunc_months = True
        self.split_months = True

        return (
            self.contracts,
            [c.external_id for c in self.contracts],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )

    def get_result(self):
        res = RequiredResult(
            transition=IssueTransitions.fixed,
            messages=[
                COMMENTS['contract_not_signed'].format(self.contracts[0].external_id)
            ],
        )
        return res


class SingleUnprocessableContractCase(AbstractDBTestCase):
    _representation = 'single_unprocessable_contract'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        contract = db_utils.create_partners_contract(
            session,
            client,
            person,
            on_dt=datetime.datetime(2019, 1, 1),
            services={111, 124, 128},
        )
        self.begin_dt = datetime.datetime(2019, 12, 1)
        self.end_dt = datetime.datetime(2020, 1, 1)
        self.contracts = [contract]
        self.trunc_months = False
        self.split_months = False

        return (
            self.contracts,
            [c.external_id for c in self.contracts],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )

    def get_result(self):
        report = ut.Struct(
            contract=self.contracts[0],
            report_type='детализация невозможна',
            comment=COMMENTS['unprocessable_contract'],
            begin_dt=self.begin_dt,
            end_dt=self.end_dt,
        )
        messages = [COMMENTS['comment_template'].format(obj=report)]
        res = RequiredResult(transition=IssueTransitions.fixed, messages=messages)
        return res


class SingleEmptyThirdPartyDetalisationCase(AbstractDBTestCase):
    _representation = 'single_empty_thirdparty_detalisation'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        contract = db_utils.create_general_contract(
            session, client, person, on_dt=datetime.datetime(2019, 1, 1), services={124}
        )
        self.begin_dt = datetime.datetime(2019, 12, 1)
        self.end_dt = datetime.datetime(2020, 1, 1)
        self.contracts = [contract]
        self.trunc_months = False
        self.split_months = False

        return (
            self.contracts,
            [c.external_id for c in self.contracts],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )

    def get_result(self):
        report = ut.Struct(
            contract=self.contracts[0],
            report_type='детализация транзакций',
            comment=COMMENTS['fail_report'],
            begin_dt=self.begin_dt,
            end_dt=self.end_dt,
        )
        messages = [COMMENTS['comment_template'].format(obj=report)]
        res = RequiredResult(transition=IssueTransitions.fixed, messages=messages)
        return res


class SingleEmptyCommissionDetalisationCase(AbstractDBTestCase):
    _representation = 'single_empty_commission_detalisation'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        contract = db_utils.create_general_contract(
            session, client, person, on_dt=datetime.datetime(2019, 1, 1), services={111}
        )
        self.begin_dt = datetime.datetime(2019, 12, 1)
        self.end_dt = datetime.datetime(2020, 1, 1)
        self.contracts = [contract]
        self.trunc_months = False
        self.split_months = False

        return (
            self.contracts,
            [c.external_id for c in self.contracts],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )

    def get_result(self):
        report = ut.Struct(
            contract=self.contracts[0],
            report_type='детализация комиссии',
            comment=COMMENTS['fail_report'],
            begin_dt=self.begin_dt,
            end_dt=self.end_dt,
        )
        messages = [COMMENTS['comment_template'].format(obj=report)]
        res = RequiredResult(transition=IssueTransitions.fixed, messages=messages)
        return res


class SingleEmptyMixedDetalisationCase(AbstractDBTestCase):
    _representation = 'single_empty_mixed_detalisation'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        contract = db_utils.create_general_contract(
            session,
            client,
            person,
            on_dt=datetime.datetime(2019, 1, 1),
            services={111, 124, 128},
        )
        self.begin_dt = datetime.datetime(2019, 12, 1)
        self.end_dt = datetime.datetime(2020, 1, 1)
        self.contracts = [contract]
        self.trunc_months = False
        self.split_months = False

        return (
            self.contracts,
            [c.external_id for c in self.contracts],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )

    def get_result(self):
        reports = [
            ut.Struct(
                contract=self.contracts[0],
                report_type='детализация транзакций',
                comment=COMMENTS['fail_report'],
                begin_dt=self.begin_dt,
                end_dt=self.end_dt,
            ),
            ut.Struct(
                contract=self.contracts[0],
                report_type='детализация комиссии',
                comment=COMMENTS['fail_report'],
                begin_dt=self.begin_dt,
                end_dt=self.end_dt,
            ),
        ]
        messages = [
            '\n'.join(
                [COMMENTS['comment_template'].format(obj=report) for report in reports]
            )
        ]
        res = RequiredResult(transition=IssueTransitions.fixed, messages=messages)
        return res


class SingleNonEmptyThirdPartyDetalisation1MCase(AbstractDBTestCase):
    _representation = 'single_nonempty_thirdparty_detalisation_1m'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        contract = db_utils.create_general_contract(
            session, client, person, on_dt=datetime.datetime(2019, 1, 1), services={124}
        )
        self.begin_dt = datetime.datetime(2019, 12, 15)
        self.end_dt = datetime.datetime(2019, 12, 20)
        self.contracts = [contract]
        self.trunc_months = False
        self.split_months = False

        return (
            self.contracts,
            [c.external_id for c in self.contracts],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )

    def get_result(self):
        message, attachments, patch_data = self.get_single_contract_reports(
            self.contracts[0],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )
        res = RequiredResult(transition=IssueTransitions.fixed, messages=[message])
        res.patch_data = patch_data
        res.attachments = attachments
        return res


class SingleNonEmptyThirdPartyDetalisation1MTCase(AbstractDBTestCase):
    _representation = 'single_nonempty_thirdparty_detalisation_1m_t'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        contract = db_utils.create_general_contract(
            session, client, person, on_dt=datetime.datetime(2019, 1, 1), services={124}
        )
        self.begin_dt = datetime.datetime(2019, 12, 15)
        self.end_dt = datetime.datetime(2019, 12, 20)
        self.contracts = [contract]
        self.trunc_months = True
        self.split_months = False

        return (
            self.contracts,
            [c.external_id for c in self.contracts],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )

    def get_result(self):
        message, attachments, patch_data = self.get_single_contract_reports(
            self.contracts[0],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )
        res = RequiredResult(transition=IssueTransitions.fixed, messages=[message])
        res.patch_data = patch_data
        res.attachments = attachments
        return res


class SingleNonEmptyThirdPartyDetalisation1MSCase(AbstractDBTestCase):
    _representation = 'single_nonempty_thirdparty_detalisation_1m_s'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        contract = db_utils.create_general_contract(
            session, client, person, on_dt=datetime.datetime(2019, 1, 1), services={124}
        )
        self.begin_dt = datetime.datetime(2019, 12, 15)
        self.end_dt = datetime.datetime(2019, 12, 20)
        self.contracts = [contract]
        self.trunc_months = False
        self.split_months = True

        return (
            self.contracts,
            [c.external_id for c in self.contracts],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )

    def get_result(self):
        message, attachments, patch_data = self.get_single_contract_reports(
            self.contracts[0],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )
        res = RequiredResult(transition=IssueTransitions.fixed, messages=[message])
        res.patch_data = patch_data
        res.attachments = attachments
        return res


class SingleNonEmptyThirdPartyDetalisation1MTSCase(AbstractDBTestCase):
    _representation = 'single_nonempty_thirdparty_detalisation_1m_t_s'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        contract = db_utils.create_general_contract(
            session, client, person, on_dt=datetime.datetime(2019, 1, 1), services={124}
        )
        self.begin_dt = datetime.datetime(2019, 12, 15)
        self.end_dt = datetime.datetime(2019, 12, 20)
        self.contracts = [contract]
        self.trunc_months = True
        self.split_months = True

        return (
            self.contracts,
            [c.external_id for c in self.contracts],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )

    def get_result(self):
        message, attachments, patch_data = self.get_single_contract_reports(
            self.contracts[0],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )
        res = RequiredResult(transition=IssueTransitions.fixed, messages=[message])
        res.patch_data = patch_data
        res.attachments = attachments
        return res


class SingleNonEmptyThirdPartyDetalisation2MCase(AbstractDBTestCase):
    _representation = 'single_nonempty_thirdparty_detalisation_2m'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        contract = db_utils.create_general_contract(
            session, client, person, on_dt=datetime.datetime(2019, 1, 1), services={124}
        )
        self.begin_dt = datetime.datetime(2019, 12, 15)
        self.end_dt = datetime.datetime(2020, 1, 20)
        self.contracts = [contract]
        self.trunc_months = False
        self.split_months = False

        return (
            self.contracts,
            [c.external_id for c in self.contracts],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )

    def get_result(self):
        message, attachments, patch_data = self.get_single_contract_reports(
            self.contracts[0],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )
        res = RequiredResult(transition=IssueTransitions.fixed, messages=[message])
        res.patch_data = patch_data
        res.attachments = attachments
        return res


class SingleNonEmptyThirdPartyDetalisation2MTCase(AbstractDBTestCase):
    _representation = 'single_nonempty_thirdparty_detalisation_2m_t'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        contract = db_utils.create_general_contract(
            session, client, person, on_dt=datetime.datetime(2019, 1, 1), services={124}
        )
        self.begin_dt = datetime.datetime(2019, 12, 15)
        self.end_dt = datetime.datetime(2020, 1, 20)
        self.contracts = [contract]
        self.trunc_months = True
        self.split_months = False

        return (
            self.contracts,
            [c.external_id for c in self.contracts],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )

    def get_result(self):
        message, attachments, patch_data = self.get_single_contract_reports(
            self.contracts[0],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )
        res = RequiredResult(transition=IssueTransitions.fixed, messages=[message])
        res.patch_data = patch_data
        res.attachments = attachments
        return res


class SingleNonEmptyThirdPartyDetalisation2MSCase(AbstractDBTestCase):
    _representation = 'single_nonempty_thirdparty_detalisation_2m_s'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        contract = db_utils.create_general_contract(
            session, client, person, on_dt=datetime.datetime(2019, 1, 1), services={124}
        )
        self.begin_dt = datetime.datetime(2019, 12, 15)
        self.end_dt = datetime.datetime(2020, 1, 20)
        self.contracts = [contract]
        self.trunc_months = False
        self.split_months = True

        return (
            self.contracts,
            [c.external_id for c in self.contracts],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )

    def get_result(self):
        message, attachments, patch_data = self.get_single_contract_reports(
            self.contracts[0],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )
        res = RequiredResult(transition=IssueTransitions.fixed, messages=[message])
        res.patch_data = patch_data
        res.attachments = attachments
        return res


class SingleNonEmptyThirdPartyDetalisation2MTSCase(AbstractDBTestCase):
    _representation = 'single_nonempty_thirdparty_detalisation_2m_t_s'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        contract = db_utils.create_general_contract(
            session, client, person, on_dt=datetime.datetime(2019, 1, 1), services={124}
        )
        self.begin_dt = datetime.datetime(2019, 12, 15)
        self.end_dt = datetime.datetime(2020, 1, 20)
        self.contracts = [contract]
        self.trunc_months = True
        self.split_months = True

        return (
            self.contracts,
            [c.external_id for c in self.contracts],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )

    def get_result(self):
        message, attachments, patch_data = self.get_single_contract_reports(
            self.contracts[0],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )
        res = RequiredResult(transition=IssueTransitions.fixed, messages=[message])
        res.patch_data = patch_data
        res.attachments = attachments
        return res


class SingleNonEmptyCommissionDetalisation1MCase(AbstractDBTestCase):
    _representation = 'single_nonempty_commission_detalisation_1m'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        contract = db_utils.create_general_contract(
            session, client, person, on_dt=datetime.datetime(2019, 1, 1), services={111}
        )
        self.begin_dt = datetime.datetime(2019, 12, 15)
        self.end_dt = datetime.datetime(2019, 12, 20)
        self.contracts = [contract]
        self.trunc_months = False
        self.split_months = False

        return (
            self.contracts,
            [c.external_id for c in self.contracts],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )

    def get_result(self):
        message, attachments, patch_data = self.get_single_contract_reports(
            self.contracts[0],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )
        res = RequiredResult(transition=IssueTransitions.fixed, messages=[message])
        res.patch_data = patch_data
        res.attachments = attachments
        return res


class SingleNonEmptyCommissionDetalisation1MTCase(AbstractDBTestCase):
    _representation = 'single_nonempty_commission_detalisation_1m_t'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        contract = db_utils.create_general_contract(
            session, client, person, on_dt=datetime.datetime(2019, 1, 1), services={111}
        )
        self.begin_dt = datetime.datetime(2019, 12, 15)
        self.end_dt = datetime.datetime(2019, 12, 20)
        self.contracts = [contract]
        self.trunc_months = True
        self.split_months = False

        return (
            self.contracts,
            [c.external_id for c in self.contracts],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )

    def get_result(self):
        message, attachments, patch_data = self.get_single_contract_reports(
            self.contracts[0],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )
        res = RequiredResult(transition=IssueTransitions.fixed, messages=[message])
        res.patch_data = patch_data
        res.attachments = attachments
        return res


class SingleNonEmptyCommissionDetalisation1MSCase(AbstractDBTestCase):
    _representation = 'single_nonempty_commission_detalisation_1m_s'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        contract = db_utils.create_general_contract(
            session, client, person, on_dt=datetime.datetime(2019, 1, 1), services={111}
        )
        self.begin_dt = datetime.datetime(2019, 12, 15)
        self.end_dt = datetime.datetime(2019, 12, 20)
        self.contracts = [contract]
        self.trunc_months = False
        self.split_months = True

        return (
            self.contracts,
            [c.external_id for c in self.contracts],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )

    def get_result(self):
        message, attachments, patch_data = self.get_single_contract_reports(
            self.contracts[0],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )
        res = RequiredResult(transition=IssueTransitions.fixed, messages=[message])
        res.patch_data = patch_data
        res.attachments = attachments
        return res


class SingleNonEmptyCommissionDetalisation1MTSCase(AbstractDBTestCase):
    _representation = 'single_nonempty_commission_detalisation_1m_t_s'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        contract = db_utils.create_general_contract(
            session, client, person, on_dt=datetime.datetime(2019, 1, 1), services={111}
        )
        self.begin_dt = datetime.datetime(2019, 12, 15)
        self.end_dt = datetime.datetime(2019, 12, 20)
        self.contracts = [contract]
        self.trunc_months = True
        self.split_months = True

        return (
            self.contracts,
            [c.external_id for c in self.contracts],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )

    def get_result(self):
        message, attachments, patch_data = self.get_single_contract_reports(
            self.contracts[0],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )
        res = RequiredResult(transition=IssueTransitions.fixed, messages=[message])
        res.patch_data = patch_data
        res.attachments = attachments
        return res


class SingleNonEmptyCommissionDetalisation2MCase(AbstractDBTestCase):
    _representation = 'single_nonempty_commission_detalisation_2m'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        contract = db_utils.create_general_contract(
            session, client, person, on_dt=datetime.datetime(2019, 1, 1), services={111}
        )
        self.begin_dt = datetime.datetime(2019, 12, 15)
        self.end_dt = datetime.datetime(2020, 1, 20)
        self.contracts = [contract]
        self.trunc_months = False
        self.split_months = False

        return (
            self.contracts,
            [c.external_id for c in self.contracts],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )

    def get_result(self):
        message, attachments, patch_data = self.get_single_contract_reports(
            self.contracts[0],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )
        res = RequiredResult(transition=IssueTransitions.fixed, messages=[message])
        res.patch_data = patch_data
        res.attachments = attachments
        return res


class SingleNonEmptyCommissionDetalisation2MTCase(AbstractDBTestCase):
    _representation = 'single_nonempty_commission_detalisation_2m_t'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        contract = db_utils.create_general_contract(
            session, client, person, on_dt=datetime.datetime(2019, 1, 1), services={111}
        )
        self.begin_dt = datetime.datetime(2019, 12, 15)
        self.end_dt = datetime.datetime(2020, 1, 20)
        self.contracts = [contract]
        self.trunc_months = True
        self.split_months = False

        return (
            self.contracts,
            [c.external_id for c in self.contracts],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )

    def get_result(self):
        message, attachments, patch_data = self.get_single_contract_reports(
            self.contracts[0],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )
        res = RequiredResult(transition=IssueTransitions.fixed, messages=[message])
        res.patch_data = patch_data
        res.attachments = attachments
        return res


class SingleNonEmptyCommissionDetalisation2MSCase(AbstractDBTestCase):
    _representation = 'single_nonempty_commission_detalisation_2m_s'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        contract = db_utils.create_general_contract(
            session, client, person, on_dt=datetime.datetime(2019, 1, 1), services={111}
        )
        self.begin_dt = datetime.datetime(2019, 12, 15)
        self.end_dt = datetime.datetime(2020, 1, 20)
        self.contracts = [contract]
        self.trunc_months = False
        self.split_months = True

        return (
            self.contracts,
            [c.external_id for c in self.contracts],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )

    def get_result(self):
        message, attachments, patch_data = self.get_single_contract_reports(
            self.contracts[0],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )
        res = RequiredResult(transition=IssueTransitions.fixed, messages=[message])
        res.patch_data = patch_data
        res.attachments = attachments
        return res


class SingleNonEmptyCommissionDetalisation2MTSCase(AbstractDBTestCase):
    _representation = 'single_nonempty_commission_detalisation_2m_t_s'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        contract = db_utils.create_general_contract(
            session, client, person, on_dt=datetime.datetime(2019, 1, 1), services={111}
        )
        self.begin_dt = datetime.datetime(2019, 12, 15)
        self.end_dt = datetime.datetime(2020, 1, 20)
        self.contracts = [contract]
        self.trunc_months = True
        self.split_months = True

        return (
            self.contracts,
            [c.external_id for c in self.contracts],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )

    def get_result(self):
        message, attachments, patch_data = self.get_single_contract_reports(
            self.contracts[0],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )
        res = RequiredResult(transition=IssueTransitions.fixed, messages=[message])
        res.patch_data = patch_data
        res.attachments = attachments
        return res


class SingleNonEmptyMixedDetalisation1MCase(AbstractDBTestCase):
    _representation = 'single_nonempty_mixed_detalisation_1m'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        contract = db_utils.create_general_contract(
            session,
            client,
            person,
            on_dt=datetime.datetime(2019, 1, 1),
            services={111, 124, 128},
        )
        self.begin_dt = datetime.datetime(2019, 12, 15)
        self.end_dt = datetime.datetime(2019, 12, 20)
        self.contracts = [contract]
        self.trunc_months = False
        self.split_months = False

        return (
            self.contracts,
            [c.external_id for c in self.contracts],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )

    def get_result(self):
        message, attachments, patch_data = self.get_single_contract_reports(
            self.contracts[0],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )
        res = RequiredResult(transition=IssueTransitions.fixed, messages=[message])
        res.patch_data = patch_data
        res.attachments = attachments
        return res


class SingleNonEmptyMixedDetalisation1MTCase(AbstractDBTestCase):
    _representation = 'single_nonempty_mixed_detalisation_1m_t'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        contract = db_utils.create_general_contract(
            session,
            client,
            person,
            on_dt=datetime.datetime(2019, 1, 1),
            services={111, 124, 128},
        )
        self.begin_dt = datetime.datetime(2019, 12, 15)
        self.end_dt = datetime.datetime(2019, 12, 20)
        self.contracts = [contract]
        self.trunc_months = True
        self.split_months = False

        return (
            self.contracts,
            [c.external_id for c in self.contracts],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )

    def get_result(self):
        message, attachments, patch_data = self.get_single_contract_reports(
            self.contracts[0],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )
        res = RequiredResult(transition=IssueTransitions.fixed, messages=[message])
        res.patch_data = patch_data
        res.attachments = attachments
        return res


class SingleNonEmptyMixedDetalisation1MSCase(AbstractDBTestCase):
    _representation = 'single_nonempty_mixed_detalisation_1m_s'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        contract = db_utils.create_general_contract(
            session,
            client,
            person,
            on_dt=datetime.datetime(2019, 1, 1),
            services={111, 124, 128},
        )
        self.begin_dt = datetime.datetime(2019, 12, 15)
        self.end_dt = datetime.datetime(2019, 12, 20)
        self.contracts = [contract]
        self.trunc_months = False
        self.split_months = True

        return (
            self.contracts,
            [c.external_id for c in self.contracts],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )

    def get_result(self):
        message, attachments, patch_data = self.get_single_contract_reports(
            self.contracts[0],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )
        res = RequiredResult(transition=IssueTransitions.fixed, messages=[message])
        res.patch_data = patch_data
        res.attachments = attachments
        return res


class SingleNonEmptyMixedDetalisation1MTSCase(AbstractDBTestCase):
    _representation = 'single_nonempty_mixed_detalisation_1m_t_s'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        contract = db_utils.create_general_contract(
            session,
            client,
            person,
            on_dt=datetime.datetime(2019, 1, 1),
            services={111, 124, 128},
        )
        self.begin_dt = datetime.datetime(2019, 12, 15)
        self.end_dt = datetime.datetime(2019, 12, 20)
        self.contracts = [contract]
        self.trunc_months = True
        self.split_months = True

        return (
            self.contracts,
            [c.external_id for c in self.contracts],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )

    def get_result(self):
        message, attachments, patch_data = self.get_single_contract_reports(
            self.contracts[0],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )
        res = RequiredResult(transition=IssueTransitions.fixed, messages=[message])
        res.patch_data = patch_data
        res.attachments = attachments
        return res


class SingleNonEmptyMixedDetalisation2MCase(AbstractDBTestCase):
    _representation = 'single_nonempty_mixed_detalisation_2m'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        contract = db_utils.create_general_contract(
            session,
            client,
            person,
            on_dt=datetime.datetime(2019, 1, 1),
            services={111, 124, 128},
        )
        self.begin_dt = datetime.datetime(2019, 12, 15)
        self.end_dt = datetime.datetime(2020, 1, 20)
        self.contracts = [contract]
        self.trunc_months = False
        self.split_months = False

        return (
            self.contracts,
            [c.external_id for c in self.contracts],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )

    def get_result(self):
        message, attachments, patch_data = self.get_single_contract_reports(
            self.contracts[0],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )
        res = RequiredResult(transition=IssueTransitions.fixed, messages=[message])
        res.patch_data = patch_data
        res.attachments = attachments
        return res


class SingleNonEmptyMixedDetalisation2MTCase(AbstractDBTestCase):
    _representation = 'single_nonempty_mixed_detalisation_2m_t'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        contract = db_utils.create_general_contract(
            session,
            client,
            person,
            on_dt=datetime.datetime(2019, 1, 1),
            services={111, 124, 128},
        )
        self.begin_dt = datetime.datetime(2019, 12, 15)
        self.end_dt = datetime.datetime(2020, 1, 20)
        self.contracts = [contract]
        self.trunc_months = True
        self.split_months = False

        return (
            self.contracts,
            [c.external_id for c in self.contracts],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )

    def get_result(self):
        message, attachments, patch_data = self.get_single_contract_reports(
            self.contracts[0],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )
        res = RequiredResult(transition=IssueTransitions.fixed, messages=[message])
        res.patch_data = patch_data
        res.attachments = attachments
        return res


class SingleNonEmptyMixedDetalisation2MSCase(AbstractDBTestCase):
    _representation = 'single_nonempty_mixed_detalisation_2m_s'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        contract = db_utils.create_general_contract(
            session,
            client,
            person,
            on_dt=datetime.datetime(2019, 1, 1),
            services={111, 124, 128},
        )
        self.begin_dt = datetime.datetime(2019, 12, 15)
        self.end_dt = datetime.datetime(2020, 1, 20)
        self.contracts = [contract]
        self.trunc_months = False
        self.split_months = True

        return (
            self.contracts,
            [c.external_id for c in self.contracts],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )

    def get_result(self):
        message, attachments, patch_data = self.get_single_contract_reports(
            self.contracts[0],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )
        res = RequiredResult(transition=IssueTransitions.fixed, messages=[message])
        res.patch_data = patch_data
        res.attachments = attachments
        return res


class SingleNonEmptyMixedDetalisation2MTSCase(AbstractDBTestCase):
    _representation = 'single_nonempty_mixed_detalisation_2m_t_s'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        contract = db_utils.create_general_contract(
            session,
            client,
            person,
            on_dt=datetime.datetime(2019, 1, 1),
            services={111, 124, 128},
        )
        self.begin_dt = datetime.datetime(2019, 12, 15)
        self.end_dt = datetime.datetime(2020, 1, 20)
        self.contracts = [contract]
        self.trunc_months = True
        self.split_months = True

        return (
            self.contracts,
            [c.external_id for c in self.contracts],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )

    def get_result(self):
        message, attachments, patch_data = self.get_single_contract_reports(
            self.contracts[0],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )
        res = RequiredResult(transition=IssueTransitions.fixed, messages=[message])
        res.patch_data = patch_data
        res.attachments = attachments
        return res


class MultipleAllInOneMixedDetalisation2MTSCase(AbstractDBTestCase):
    _representation = 'multiple_all_in_one_mixed_detalisation_2m_t_s'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contracts = []
        external_id = 'TEST/0'  # not found
        self.contracts.append(  # not signed
            db_utils.create_general_contract(
                session,
                client,
                person,
                on_dt=datetime.datetime(2019, 1, 1),
                services={124, 125},
            )
        )
        self.contracts[0].col0.is_signed = None
        self.contracts[0].session.flush()
        self.contracts.append(  # unprocessable
            db_utils.create_partners_contract(
                session, client, person, on_dt=datetime.datetime(2019, 1, 1)
            )
        )
        self.contracts.append(  # empty thirdparty
            db_utils.create_general_contract(
                session,
                client,
                person,
                on_dt=datetime.datetime(2019, 1, 1),
                services={124, 125},
            )
        )
        self.contracts.append(  # nonempty thirdparty
            db_utils.create_general_contract(
                session,
                client,
                person,
                on_dt=datetime.datetime(2019, 1, 1),
                services={124, 125},
            )
        )
        self.contracts.append(  # empty commission
            db_utils.create_general_contract(
                session,
                client,
                person,
                on_dt=datetime.datetime(2019, 1, 1),
                services={111, 128},
            )
        )
        self.contracts.append(  # nonempty commission
            db_utils.create_general_contract(
                session,
                client,
                person,
                on_dt=datetime.datetime(2019, 1, 1),
                services={111, 128},
            )
        )
        self.contracts.append(  # empty mixed
            db_utils.create_general_contract(
                session,
                client,
                person,
                on_dt=datetime.datetime(2019, 1, 1),
                services={111, 124, 128, 125},
            )
        )
        self.contracts.append(  # nonempty mixed
            db_utils.create_general_contract(
                session,
                client,
                person,
                on_dt=datetime.datetime(2019, 1, 1),
                services={111, 124, 128, 125, 628, 629, 645, 646},
            )
        )
        self.contracts.append(  # nonempty mixed 2
            db_utils.create_general_contract(
                session,
                client,
                person,
                on_dt=datetime.datetime(2019, 1, 1),
                services={111, 124, 128, 125, 628, 629, 645, 646, 151, 601, 602},
            )
        )
        self.contracts.append(  # nonempty mixed 3
            db_utils.create_general_contract(
                session,
                client,
                person,
                on_dt=datetime.datetime(2019, 1, 1),
                services={111, 124, 128, 125, 628, 629, 645, 646, 151, 601, 602, 126, 118, 638},
            )
        )
        self.contracts.append(  # nonempty mixed 4
            db_utils.create_general_contract(
                session,
                client,
                person,
                on_dt=datetime.datetime(2019, 1, 1),
                services={111, 124, 128, 125, 628, 629, 645, 646, 661, 662},
            )
        )
        self.begin_dt = datetime.datetime(2019, 12, 15)
        self.end_dt = datetime.datetime(2020, 1, 20)
        self.trunc_months = True
        self.split_months = True

        return (
            self.contracts,
            [external_id] + [c.external_id for c in self.contracts],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed)
        patchers = []
        messages = []

        messages.append(COMMENTS['contract_not_found'].format('TEST/0'))

        messages.append(
            COMMENTS['contract_not_signed'].format(self.contracts[0].external_id)
        )

        begin_dt = ut.month_first_day(self.begin_dt)
        middle_dt = datetime.datetime(2020, 1, 1)
        end_dt = ut.month_first_day(self.end_dt) + relativedelta(months=1)

        report = ut.Struct(
            contract=self.contracts[1],
            report_type='детализация невозможна',
            comment=COMMENTS['unprocessable_contract'],
            begin_dt=begin_dt,
            end_dt=end_dt,
        )
        messages.append(COMMENTS['comment_template'].format(obj=report))

        report = ut.Struct(
            contract=self.contracts[2],
            report_type='детализация транзакций',
            comment=COMMENTS['fail_report'],
            begin_dt=begin_dt,
            end_dt=middle_dt,
        )
        messages.append(COMMENTS['comment_template'].format(obj=report))
        report = ut.Struct(
            contract=self.contracts[2],
            report_type='детализация транзакций',
            comment=COMMENTS['fail_report'],
            begin_dt=middle_dt,
            end_dt=end_dt,
        )
        messages.append(COMMENTS['comment_template'].format(obj=report))

        message, attachments, patch_data = self.get_single_contract_reports(
            self.contracts[3],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )
        messages.append(message)
        res.attachments.update(attachments)
        patchers.extend(patch_data)

        report = ut.Struct(
            contract=self.contracts[4],
            report_type='детализация комиссии',
            comment=COMMENTS['fail_report'],
            begin_dt=begin_dt,
            end_dt=middle_dt,
        )
        messages.append(COMMENTS['comment_template'].format(obj=report))
        report = ut.Struct(
            contract=self.contracts[4],
            report_type='детализация комиссии',
            comment=COMMENTS['fail_report'],
            begin_dt=middle_dt,
            end_dt=end_dt,
        )
        messages.append(COMMENTS['comment_template'].format(obj=report))

        message, attachments, patch_data = self.get_single_contract_reports(
            self.contracts[5],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )
        messages.append(message)
        res.attachments.update(attachments)
        patchers.extend(patch_data)

        report = ut.Struct(
            contract=self.contracts[6],
            report_type='детализация транзакций',
            comment=COMMENTS['fail_report'],
            begin_dt=begin_dt,
            end_dt=middle_dt,
        )
        messages.append(COMMENTS['comment_template'].format(obj=report))
        report = ut.Struct(
            contract=self.contracts[6],
            report_type='детализация транзакций',
            comment=COMMENTS['fail_report'],
            begin_dt=middle_dt,
            end_dt=end_dt,
        )
        messages.append(COMMENTS['comment_template'].format(obj=report))
        report = ut.Struct(
            contract=self.contracts[6],
            report_type='детализация комиссии',
            comment=COMMENTS['fail_report'],
            begin_dt=begin_dt,
            end_dt=middle_dt,
        )
        messages.append(COMMENTS['comment_template'].format(obj=report))
        report = ut.Struct(
            contract=self.contracts[6],
            report_type='детализация комиссии',
            comment=COMMENTS['fail_report'],
            begin_dt=middle_dt,
            end_dt=end_dt,
        )
        messages.append(COMMENTS['comment_template'].format(obj=report))

        message, attachments, patch_data = self.get_single_contract_reports(
            self.contracts[7],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )
        messages.append(message)
        res.attachments.update(attachments)
        patchers.extend(patch_data)

        message, attachments, patch_data = self.get_single_contract_reports(
            self.contracts[8],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )
        messages.append(message)
        res.attachments.update(attachments)
        patchers.extend(patch_data)

        message, attachments, patch_data = self.get_single_contract_reports(
            self.contracts[9],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )
        messages.append(message)
        res.attachments.update(attachments)
        patchers.extend(patch_data)

        message, attachments, patch_data = self.get_single_contract_reports(
            self.contracts[10],
            self.begin_dt,
            self.end_dt,
            self.trunc_months,
            self.split_months,
        )
        messages.append(message)
        res.attachments.update(attachments)
        patchers.extend(patch_data)

        merged_patch_data = []
        for obj, obj_patchers in ut.groupby(patchers, key=lambda patcher: patcher.obj):
            merged_patch_data.append(
                ut.Struct(
                    obj=obj,
                    value=self.union_all_queries(
                        self, [patch_data.value for patch_data in obj_patchers]
                    ),
                )
            )
        res.patch_data = merged_patch_data
        res.add_message('\n'.join(messages))
        return res


@mock.patch(
    'autodasha.solver_cl.contract_detalisation.ContractDetalisation.__init__', __init__
)
@pytest.mark.parametrize(
    'issue_data',
    [case() for case in AbstractDBTestCase._cases],
    ids=lambda case: str(case),
    indirect=['issue_data'],
)
def test_db(session, issue_data):
    queue_object, st_issue, case = issue_data

    req_res = case.get_result()

    patchers = []
    if req_res and req_res.patch_data:
        for p in req_res.patch_data:
            patchers.append(mock.patch(p.obj, return_value=p.value))

    for patcher in patchers:
        patcher.start()

    solver = ContractDetalisation(queue_object, st_issue)
    res = solver.solve()
    session.flush()

    for patcher in patchers:
        patcher.stop()

    if req_res is None:
        assert res.commit is False
        assert res.delay is True
        assert res.issue_report is None
        return

    assert res.commit is True
    assert res.delay is False

    report = res.issue_report
    assert report.transition == req_res.transition
    assert report.assignee == req_res.assignee

    comments = report.comments
    assert len(comments) == 1
    (comment,) = comments
    lines = comment.text.rstrip('\n').split('\n\n')
    req_comments = req_res.comments
    assert len(req_comments) == 1
    (req_comment,) = req_comments
    req_lines = req_comment.rstrip('\n').split('\n\n')
    assert set(lines) == set(req_lines)
    for filename, (query, params) in req_res.attachments.items():
        query_result = session.execute(query, params).fetchall()
        assert query_result
        header = query_result[0].keys()
        excel_doc = get_excel_document(header, query_result, 0)
        attachment = filter(lambda a: a.name == filename, comment.attachments)
        assert len(attachment) == 1
        (attachment,) = attachment
        assert attachment.name == filename
        data = map(tuple, xls_reader_contents(attachment.getvalue()))
        req_data = map(
            tuple, xls_reader_contents(StringIO.StringIO(excel_doc).getvalue())
        )
        assert set(data) == set(req_data)
