# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import datetime as dt
import uuid
from dateutil.relativedelta import relativedelta
from itertools import chain

import pytest
import mock

import balance.mapper as mapper
from balance import muzzle_util as ut

from autodasha.solver_cl import MarketPaymentDetalisation, ParseException
from autodasha.core.api.tracker import IssueTransitions
from autodasha.utils.transliterator import transliterate

from tests.autodasha_tests.functional_tests import case_utils
from tests.autodasha_tests.common import db_utils


# Используется и для мок-тестов и для дб-тестов.
# Для mock, потому что отсутствует Application, который нужен чтобы сдлеать self.get_ro_session
# Для дб-тестов, потому что тесты выполняются на тестовой базе, а солвер выполняется на ро-базе.
# В связи с этим получаются разные результаты.
def _fake_init(self, *args, **kwargs):
    super(MarketPaymentDetalisation, self).__init__(*args, **kwargs)
    self.ro_session = self.session


class AbstractMockTestCase(case_utils.AbstractMockTestCase):
    _default_lines = {
        'summary': 'Детализация платежей для сервиса Маркет',
        'contracts_eids': 'Номера договоров: %s',
        'date_from': 'С какой даты: %s',
        'date_to': 'По какую дату: %s',
        'split_period_by_months': 'Каждый месяц в отдельный файл: %s',
        'trunc_period_to_months': 'Округлить период до полного месяца: %s',
    }


class AbstractFailingMockTestCase(AbstractMockTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class InvalidFormCase(AbstractFailingMockTestCase):
    _representation = 'invalid_form'

    def get_data(self, mock_manager):
        contract = mock_manager.create_object(mapper.Contract, id=666, external_id='666/0')

        lines = [
            u'Список кабальных грамот: %s' % contract.external_id,
            self._get_default_line(date_from='2017-02-01'),
            self._get_default_line(date_to='2017-03-01'),
            self._get_default_line(trunc_period_to_months='Да'),
            self._get_default_line(split_period_by_months='Да'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return (
            'Не удалось получить список договоров из тикета. Возможно, '
            'форма заполнена некорректно. В случае ошибки обратитесь в поддержку '
            'через ((https://wiki.yandex-team.ru/otdelsoprovozhdeniya/dokument/avtomatformi/ общую форму)).'
        )


class OnlyContractsFormCase(AbstractFailingMockTestCase):
    _representation = 'only_contracts_form'

    def get_data(self, mock_manager):
        contract = mock_manager.create_object(mapper.Contract, id=666, external_id='666/0')

        lines = [
            self._get_default_line(contracts_eids=contract.external_id),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return (
            'Не удалось получить список договоров из тикета. Возможно, '
            'форма заполнена некорректно. В случае ошибки обратитесь в поддержку '
            'через ((https://wiki.yandex-team.ru/otdelsoprovozhdeniya/dokument/avtomatformi/ общую форму)).'
        )


class NoContractsCase(AbstractFailingMockTestCase):
    _representation = 'no_contracts'

    def get_data(self, mock_manager):
        lines = [
            u'Номера договоров:',
            self._get_default_line(date_from='2017-02-01'),
            self._get_default_line(date_to='2017-03-01'),
            self._get_default_line(trunc_period_to_months='Да'),
            self._get_default_line(split_period_by_months='Да'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Не указан договор для построения детализации. ' \
               'Уточните данные и заполните форму еще раз.'


class NonLettersContractsCase(AbstractFailingMockTestCase):
    _representation = 'non_letters_contracts'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(contracts_eids='\n  , \t   ;'),
            self._get_default_line(date_from='2017-02-01'),
            self._get_default_line(date_to='2017-03-01'),
            self._get_default_line(trunc_period_to_months='Да'),
            self._get_default_line(split_period_by_months='Да'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Не указан договор для построения детализации. ' \
               'Уточните данные и заполните форму еще раз.'


class NoDateFromCase(AbstractFailingMockTestCase):
    _representation = 'no_date_from'

    def get_data(self, mock_manager):
        contract = mock_manager.create_object(mapper.Contract, id=666,
                                              external_id='666/1')
        lines = [
            self._get_default_line(contracts_eids=contract.external_id),
            self._get_default_line(date_to='2017-03-01'),
            self._get_default_line(trunc_period_to_months='Да'),
            self._get_default_line(split_period_by_months='Да'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Не указана дата "С какой даты".'


class NoDateToCase(AbstractFailingMockTestCase):
    _representation = 'no_date_to'

    def get_data(self, mock_manager):
        contract = mock_manager.create_object(mapper.Contract, id=666,
                                              external_id='666/0')
        lines = [
            self._get_default_line(contracts_eids=contract.external_id),
            self._get_default_line(date_from='2017-03-01'),
            self._get_default_line(trunc_period_to_months='Да'),
            self._get_default_line(split_period_by_months='Да'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Не указана дата "По какую дату".'


class IncorrectFormatDateFromCase(AbstractFailingMockTestCase):
    _representation = 'incorrect_format_date_from'

    def get_data(self, mock_manager):
        contract = mock_manager.create_object(mapper.Contract, id=666,
                                              external_id='666/0')
        lines = [
            self._get_default_line(contracts_eids=contract.external_id),
            self._get_default_line(date_from='17-02-01'),
            self._get_default_line(date_to='2017-03-01'),
            self._get_default_line(trunc_period_to_months='Да'),
            self._get_default_line(split_period_by_months='Да'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Неверный формат даты "С какой даты".'


class IncorrectFormatDateToCase(AbstractFailingMockTestCase):
    _representation = 'incorrect_format_date_to'

    def get_data(self, mock_manager):
        contract = mock_manager.create_object(mapper.Contract, id=666,
                                              external_id='666/0')
        lines = [
            self._get_default_line(contracts_eids=contract.external_id),
            self._get_default_line(date_from='2017-02-01'),
            self._get_default_line(date_to='2017-13-01'),
            self._get_default_line(trunc_period_to_months='Да'),
            self._get_default_line(split_period_by_months='Да'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Неверный формат даты "По какую дату".'


class NoTruncPeriodCase(AbstractFailingMockTestCase):
    _representation = 'no_trunc_period_to_months'

    def get_data(self, mock_manager):
        contract = mock_manager.create_object(mapper.Contract, id=666,
                                              external_id='666/0')
        lines = [
            self._get_default_line(contracts_eids=contract.external_id),
            self._get_default_line(date_from='2017-02-01'),
            self._get_default_line(date_to='2017-04-01'),
            self._get_default_line(split_period_by_months='Да'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Не указан признак "Округлить период до полного месяца".'


class IncorrectFormatTruncPeriodCase(AbstractFailingMockTestCase):
    _representation = 'incorrect_format_trunc_period_by_months'

    def get_data(self, mock_manager):
        contract = mock_manager.create_object(mapper.Contract, id=666,
                                              external_id='666/0')
        lines = [
            self._get_default_line(contracts_eids=contract.external_id),
            self._get_default_line(date_from='2017-02-01'),
            self._get_default_line(date_to='2017-04-01'),
            self._get_default_line(trunc_period_to_months='ДаНет'),
            self._get_default_line(split_period_by_months='Да')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Признак "Округлить период до полного месяца" ' \
               'должен иметь значение Да или Нет.'


class NoSplitPeriodCase(AbstractFailingMockTestCase):
    _representation = 'no_split_period_by_months'

    def get_data(self, mock_manager):
        contract = mock_manager.create_object(mapper.Contract, id=666,
                                              external_id='666/0')
        lines = [
            self._get_default_line(contracts_eids=contract.external_id),
            self._get_default_line(date_from='2017-02-01'),
            self._get_default_line(date_to='2017-04-01'),
            self._get_default_line(trunc_period_to_months='Да'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Не указан признак "Каждый месяц в отдельный файл".'


class IncorrectFormatSplitPeriodCase(AbstractFailingMockTestCase):
    _representation = 'incorrect_format_split_period_by_months'

    def get_data(self, mock_manager):
        contract = mock_manager.create_object(mapper.Contract, id=666,
                                              external_id='666/0')
        lines = [
            self._get_default_line(contracts_eids=contract.external_id),
            self._get_default_line(date_from='2017-02-01'),
            self._get_default_line(date_to='2017-04-01'),
            self._get_default_line(trunc_period_to_months='Да'),
            self._get_default_line(split_period_by_months='ДаНет')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Признак "Каждый месяц в отдельный файл" ' \
               'должен иметь значение Да или Нет.'


@mock.patch('autodasha.solver_cl.market_payment_detalisation.MarketPaymentDetalisation.__init__', _fake_init)
@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in AbstractFailingMockTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_parse_failing(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data

    req_comment = case.get_result()

    solver = MarketPaymentDetalisation(mock_queue_object, issue)
    with pytest.raises(ParseException) as exc:
        solver.parse_issue()

    assert req_comment in exc.value.message


class AbstractGoodMockTestCase(AbstractMockTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class AllValuesFilledCase(AbstractGoodMockTestCase):
    _representation = 'all_values_filled'

    def __init__(self):
        self.contract = None

    def get_data(self, mock_manager):
        self.contract = mock_manager.create_object(mapper.Contract, id=666,
                                                   external_id='666/0')
        lines = [
            self._get_default_line(contracts_eids=self.contract.external_id),
            self._get_default_line(date_from='2017-02-01'),
            self._get_default_line(date_to='2017-04-01'),
            self._get_default_line(trunc_period_to_months='Да'),
            self._get_default_line(split_period_by_months='Да')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return {'contracts_eids': [self.contract.external_id],
                'date_from': dt.datetime(2017, 2, 1),
                'date_to': dt.datetime(2017, 4, 1),
                'trunc_period_to_months': True,
                'split_period_by_months': True}


class NoDatesFilledCase(AbstractGoodMockTestCase):
    _representation = 'no_dates_filled'

    def __init__(self):
        self.contract0 = None
        self.contract1 = None
        self.contract2 = None

    def get_data(self, mock_manager):
        self.contract0 = mock_manager.create_object(mapper.Contract, id=666,
                                                    external_id='666/0')
        self.contract1 = mock_manager.create_object(mapper.Contract, id=6666,
                                                    external_id='6666/0')
        self.contract2 = mock_manager.create_object(mapper.Contract, id=66666,
                                                    external_id='66666/0')

        contract_eids = '  ;\t{self.contract0.external_id},,;\n  {self.contract1.external_id},' \
                        '{self.contract2.external_id}; \t'.format(self=self)
        lines = [
            self._get_default_line(contracts_eids=contract_eids),
            self._get_default_line(date_from='None'),
            self._get_default_line(date_to=''),
            self._get_default_line(trunc_period_to_months='Да'),
            self._get_default_line(split_period_by_months='Нет'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        am = mapper.ActMonth()
        return {'contracts_eids': [self.contract0.external_id, self.contract1.external_id,
                                   self.contract2.external_id],
                'date_from': am.begin_dt,
                'date_to': am.end_dt,
                'trunc_period_to_months': True,
                'split_period_by_months': False}


@mock.patch('autodasha.solver_cl.market_payment_detalisation.MarketPaymentDetalisation.__init__', _fake_init)
@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in AbstractGoodMockTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_parse_good(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data

    req_res = case.get_result()

    solver = MarketPaymentDetalisation(mock_queue_object, issue)
    res = solver.parse_issue()

    assert set(req_res['contracts_eids']) == set(res['contracts_eids'])
    assert req_res['date_from'] == res['date_from']
    assert req_res['date_to'] == res['date_to']
    assert req_res['split_period_by_months'] == res['split_period_by_months']
    assert req_res['trunc_period_to_months'] == res['trunc_period_to_months']


class AbstractDBTestCase(case_utils.AbstractDBTestCase):
    _summary = 'Детализация платежей для сервиса Маркет'
    _description = '''
Номера договоров: {contracts_eids}
С какой даты: {date_from}
По какую дату: {date_to}
Каждый месяц в отдельный файл: {split_period_by_months}
Округлить период до полного месяца: {trunc_period_to_months}
'''
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    def __init__(self):
        super(AbstractDBTestCase, self).__init__()
        self.config = None

    def get_description(self, session):
        contracts, date_from, date_to, trunc_period_to_months, split_period_by_months = self._get_data(session)
        return self._description.format(
            contracts_eids=', '.join(c.external_id for c in contracts),
            date_from=dt.datetime.strftime(date_from, '%Y-%m-%d') if date_from else 'None',
            date_to=dt.datetime.strftime(date_to, '%Y-%m-%d') if date_to else 'None',
            trunc_period_to_months=unicode(trunc_period_to_months),
            split_period_by_months=unicode(split_period_by_months)
        )

    def setup_config(self, session, config):
        config['OEBS_INVOICE_DATA_TABLE'] = 'bo.mv_oebs_agent_invoice_data'
        config['THIRDPARTY_PAYMENTS_QUERY_PARALLEL_HINT'] = ' '
        self.config = config


class RequiredResult(case_utils.RequiredResult):
    def __init__(self, mode='equality', **kwargs):
        self.mode = mode
        self.patch_data = []
        self.attachments_names = []

        super(RequiredResult, self).__init__(**kwargs)

    def get_contract_line_message(self, **kwargs):
        """
        :param kwargs: contract, report_type, start_dt, end_dt, comment
        :return:
        """
        template = 'Договор {contract.external_id}, {report_type}, ' \
                   'c {date_from:%Y-%m-%d} по {date_to:%Y-%m-%d} : {comment}.'
        return template.format(**kwargs)


class ContractNotFoundCase(AbstractDBTestCase):
    _representation = 'contract_not_found'

    def __init__(self):
        self.contracts_data = []
        self.date_from = None
        self.date_to = None
        self.split_period_by_months = None
        self.trunc_period_to_months = None

    def _get_data(self, session):
        self.contracts_data.append(
            ut.Struct(contract=ut.Struct(external_id='Неуловимый/Джо'),
                      report_type='детализация платежей',
                      filename_template='payments_{contract.external_id}__{self.date_from:%Y-%m-%d}_'
                                        '_{self.date_to:%Y-%m-%d}.xls'
                      )
        )
        self.date_from = None
        self.date_to = None
        self.split_period_by_months = 'Нет'
        self.trunc_period_to_months = 'Да'

        return [row.contract for row in self.contracts_data], self.date_from, self.date_to, \
               self.trunc_period_to_months, self.split_period_by_months

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix,
                             messages=['Договор {self.contracts_data[0].contract.external_id} не найден.'
                             .format(self=self)])
        res.add_message(
            'Для данных условий детализация не найдена. В случае ошибки обратитесь в поддержку '
            'через ((https://wiki.yandex-team.ru/otdelsoprovozhdeniya/dokument/avtomatformi/ общую форму.))')
        return res


class Firm1AndNotSignedCase(AbstractDBTestCase):
    _representation = 'firm_not_market_and_not_signed_case'

    def __init__(self):
        self.contracts_data = []
        self.date_from = None
        self.date_to = None
        self.trunc_period_to_months = None
        self.split_period_by_months = None
        self.split_periods = []

    def _get_data(self, session):
        self.split_periods = [
            (dt.datetime(2017, 2, 1), dt.datetime(2017, 3, 1)),
            (dt.datetime(2017, 3, 1), dt.datetime(2017, 4, 1)), ]

        client, person = db_utils.create_client_person(session)
        general_contract0 = db_utils.create_general_contract(session, client, person,
                                                             on_dt=dt.datetime(2017, 1, 1),
                                                             firm_id=1,
                                                             services={11, 172, 609, 610, 613, 615, 620},
                                                             external_id='666-маркет',
                                                             is_offer=1)

        self.contracts_data.append(ut.Struct(contract=general_contract0,
                                             report_type='первая фирма, нет будет репорта',
                                             filename_template='вот такая печаль')
                                   )

        general_contract1 = db_utils.create_general_contract(session, client, person,
                                                             on_dt=dt.datetime(2017, 1, 1),
                                                             firm_id=111,
                                                             services={11, 172, 609, 610, 613, 615, 620},
                                                             external_id='666-маркет',
                                                             is_offer=0)
        general_contract1.col0.is_signed = None
        general_contract1.session.flush()

        self.contracts_data.append(ut.Struct(contract=general_contract1,
                                             report_type='Не подписан, не будет репорта',
                                             filename_template='вай вай')
                                   )

        self.date_from = dt.datetime(2017, 2, 1)
        self.date_to = dt.datetime(2017, 4, 1)
        self.trunc_period_to_months = 'Да'
        self.split_period_by_months = 'Да'

        return set([row.contract for row in self.contracts_data]), self.date_from, self.date_to, \
               self.trunc_period_to_months, self.split_period_by_months

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix,
                             messages=['Неверная фирма в договоре {self.contracts_data[0].contract.external_id}. '
                                       'Для данных условий детализация не найдена. '
                                       'В случае ошибки обратитесь в поддержку через '
                                       '((https://wiki.yandex-team.ru/otdelsoprovozhdeniya/dokument/avtomatformi/ общую форму)).'.format(self=self),
                                       'Договор {self.contracts_data[1].contract.external_id} не подписан. '
                                       'В случае ошибки уточните данные и заполните форму ещё раз или обратитесь в поддержку через '
                                       '((https://wiki.yandex-team.ru/otdelsoprovozhdeniya/dokument/avtomatformi/ общую форму)).'.format(self=self),
                                       ])
        res.add_message(
            'Для данных условий детализация не найдена. В случае ошибки обратитесь в поддержку '
            'через ((https://wiki.yandex-team.ru/otdelsoprovozhdeniya/dokument/avtomatformi/ общую форму.))')
        return res


class GreatSuccessCase(AbstractDBTestCase):
    _representation = 'great_success'

    def __init__(self):
        self.contracts_data = []
        self.date_from = None
        self.date_to = None
        self.trunc_period_to_months = None
        self.split_period_by_months = None
        self.split_periods = []

    def _get_data(self, session):
        self.split_periods = [(dt.datetime(2017, 2, 1), dt.datetime(2017, 3, 1)),
                              (dt.datetime(2017, 3, 1), dt.datetime(2017, 4, 1)), ]

        client, person = db_utils.create_client_person(session)
        general_contract = db_utils.create_general_contract(session, client, person,
                                                            on_dt=dt.datetime(2017, 1, 1),
                                                            firm_id=111,
                                                            services={11, 172, 609, 610, 613, 615, 620},
                                                            is_offer=0)

        self.contracts_data.append(ut.Struct(contract=general_contract,
                                             report_type='детализация платежей',
                                             filename_template='contract_{contract.external_id}__{date_from:%Y-%m-%d}__{date_to:%Y-%m-%d}.xls')
                                   )

        self.date_from = dt.datetime(2017, 2, 1)
        self.date_to = dt.datetime(2017, 4, 1)
        self.split_period_by_months = 'Да'
        self.trunc_period_to_months = 'Да'

        for split_period_start, split_period_end in self.split_periods:
            db_utils.create_thirdparty_transaction(session, general_contract, 609,
                                                   dt=split_period_start)

        return set([row.contract for row in self.contracts_data]), self.date_from, self.date_to,\
               self.trunc_period_to_months, self.split_period_by_months

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed)

        for split_period_start, split_period_end in self.split_periods:
            for row in self.contracts_data:
                attach_name = transliterate(row.filename_template.format(contract=row.contract,
                                                                         date_from=split_period_start,
                                                                         date_to=split_period_end).replace('/', '-'))
                res.attachments_names.append(attach_name)
                message = res.get_contract_line_message(contract=row.contract, report_type=row.report_type,
                                                        date_from=split_period_start, date_to=split_period_end,
                                                        comment='Выгрузка в файле ' + '%%' + attach_name + '%%')
                res.add_message(message)
            res.add_message('Успешно выгруженные примерные детализации во вложении.')

        return res


class NoDataForPeriodCase(AbstractDBTestCase):
    _representation = 'no_data_for_period'

    def __init__(self):
        self.contracts_data = []
        self.date_from = None
        self.date_to = None
        self.trunc_period_to_months = None
        self.split_period_by_months = None
        self.split_periods = []

    def _get_data(self, session):
        self.split_periods = [(dt.datetime(2017, 2, 1), dt.datetime(2017, 3, 1)),
                              (dt.datetime(2017, 3, 1), dt.datetime(2017, 4, 1)), ]

        client, person = db_utils.create_client_person(session)

        general_contract = db_utils.create_general_contract(session, client, person,
                                                            on_dt=dt.datetime(2017, 1, 1),
                                                            firm_id=111,
                                                            services={11, 172, 609, 610, 613, 615, 620},
                                                            is_offer=0)

        self.contracts_data.append(ut.Struct(contract=general_contract,
                                             report_type='детализация платежей',
                                             filename_template='contract_{contract.external_id}__{date_from:%Y-%m-%d}__'
                                                               '{date_to:%Y-%m-%d}.xls')
                                   )

        self.date_from = dt.datetime(2017, 2, 1)
        self.date_to = dt.datetime(2017, 4, 1)
        self.trunc_period_to_months = 'Да'
        self.split_period_by_months = 'Да'

        db_utils.create_thirdparty_transaction(session, general_contract, 609,
                                               dt=self.date_from)

        return set([row.contract for row in self.contracts_data]), self.date_from, self.date_to, \
               self.trunc_period_to_months, self.split_period_by_months

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed)

        row, = self.contracts_data

        attach_name = transliterate(row.filename_template.format(contract=row.contract,
                                                                 date_from=self.split_periods[0][0],
                                                                 date_to=self.split_periods[0][1]).replace('/', '-'))
        res.attachments_names.append(attach_name)
        message = res.get_contract_line_message(contract=row.contract, report_type=row.report_type,
                                                date_from=self.split_periods[0][0], date_to=self.split_periods[0][1],
                                                comment='Выгрузка в файле ' + '%%' + attach_name + '%%')
        res.add_message(message)
        message = res.get_contract_line_message(contract=row.contract, report_type=row.report_type,
                                                date_from=self.split_periods[1][0], date_to=self.split_periods[1][1],
                                                comment='Для указанного периода по договору нет данных')
        res.add_message(message)

        res.add_message('Успешно выгруженные примерные детализации во вложении.')

        return res


class InvalidContractTypeCase(AbstractDBTestCase):
    _representation = 'invalid_contract_type'

    def __init__(self):
        self.contracts_data = []
        self.date_from = None
        self.date_to = None
        self.trunc_period_to_months = None
        self.split_period_by_months = None
        self.split_periods = []

    def _get_data(self, session):
        self.split_periods = [
            (dt.datetime(2017, 2, 1), dt.datetime(2017, 3, 1)),
            (dt.datetime(2017, 3, 1), dt.datetime(2017, 4, 1)), ]

        client, person = db_utils.create_client_person(session)
        general_contract = db_utils.create_general_contract(session, client, person,
                                                            on_dt=dt.datetime(2017, 1, 1),
                                                            firm_id=111,
                                                            services={35})

        self.contracts_data.append(ut.Struct(contract=general_contract,
                                             report_type='детализация невозможна',
                                             filename_template='нового года не будет, дед мороз повесился')
                                   )

        corp_contract = db_utils.create_spendable_contract(session, client, person, 609, None,
                                                           on_dt=dt.datetime(2017, 1, 1), firm_id=111)

        self.contracts_data.append(ut.Struct(contract=corp_contract,
                                             report_type='детализация платежей',
                                             filename_template='contract_{contract.external_id}__{date_from:%Y-%m-%d}_'
                                                               '_{date_to:%Y-%m-%d}.xls')
                                   )

        self.date_from = dt.datetime(2017, 2, 1)
        self.date_to = dt.datetime(2017, 4, 1)
        self.trunc_period_to_months = 'Да'
        self.split_period_by_months = 'Да'

        for split_period_start, split_period_end in self.split_periods:
            db_utils.create_thirdparty_transaction(session, general_contract, 610, dt=split_period_start)
            db_utils.create_thirdparty_transaction(session, corp_contract, 609, dt=split_period_start)
        return set([row.contract for row in self.contracts_data]), self.date_from, self.date_to, \
               self.trunc_period_to_months, self.split_period_by_months

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed)

        left_row = None
        for split_period_start, split_period_end in self.split_periods:
            for row in self.contracts_data:
                if row.report_type == 'детализация невозможна':
                    left_row = row
                    continue
                attach_name = transliterate(row.filename_template.format(contract=row.contract,
                                                                         date_from=split_period_start,
                                                                         date_to=split_period_end).replace('/', '-'))
                res.attachments_names.append(attach_name)
                message = res.get_contract_line_message(
                    contract=row.contract, report_type=row.report_type,
                    date_from=split_period_start, date_to=split_period_end,
                    comment='Выгрузка в файле ' + '%%' + attach_name + '%%')
                res.add_message(message)
        message = res.get_contract_line_message(contract=left_row.contract, report_type=left_row.report_type,
                                                date_from=self.date_from, date_to=self.date_to,
                                                comment='Данный договор не принадлежит ни к одному типу, по которому '
                                                        'может быть сделана детализация. Возможно, в договоре не '
                                                        'указаны сервисы')
        res.add_message(message)
        res.add_message('Успешно выгруженные примерные детализации во вложении.')
        return res


class OverCountQueryFailedCase(AbstractDBTestCase):
    _representation = 'overcount_query_failed'

    def __init__(self):
        self.contracts_data = []
        self.date_from = None
        self.date_to = None
        self.trunc_period_to_months = None
        self.split_period_by_months = None
        self.split_periods = []

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        general_contract = db_utils.create_general_contract(session, client, person,
                                                            on_dt=dt.datetime(2017, 1, 1),
                                                            firm_id=111,
                                                            services={11, 172, 609, 610, 613, 615, 620},
                                                            is_offer=0)

        self.contracts_data.append(ut.Struct(contract=general_contract,
                                             report_type='детализация платежей',
                                             filename_template='contract_{contract.external_id}__{date_from:%Y-%m-%d}__{date_to:%Y-%m-%d}.xls')
                                   )

        self.date_from = dt.datetime(2017, 2, 1)
        self.date_to = dt.datetime(2017, 4, 1)
        self.trunc_period_to_months = 'Да'
        self.split_period_by_months = 'Нет'

        return set([row.contract for row in self.contracts_data]), self.date_from, self.date_to, \
               self.trunc_period_to_months, self.split_period_by_months

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix)

        for row in self.contracts_data:
            message = res.get_contract_line_message(contract=row.contract, report_type=row.report_type,
                                                    date_from=self.date_from, date_to=self.date_to,
                                                    comment='Результат выполнения данного запроса превысил 1000000 строк. '
                                                            'Уменьшите период запроса и/или установите галку "Каждый месяц в отдельный файл". '
                                                            'В случае повторения ошибки обратитесь в поддержку '
                                                            'через ((https://wiki.yandex-team.ru/otdelsoprovozhdeniya/dokument/avtomatformi/ общую форму.))')
            res.add_message(message)
        res.patch_data = (
                          ut.Struct(
                              obj='autodasha.solver_cl.MarketReportProcessor.count_rows',
                              value=(lambda *args, **kwargs: 1000001),
                          ),
        )
        res.add_message(
            'Для данных условий детализация не найдена. В случае ошибки обратитесь в поддержку '
            'через ((https://wiki.yandex-team.ru/otdelsoprovozhdeniya/dokument/avtomatformi/ общую форму.))')
        return res


class StartDtGreaterThenEndDtCase(AbstractDBTestCase):
    _representation = 'start_dt_greater_then_end_dt'

    def __init__(self):
        self.contracts_data = []
        self.date_from = None
        self.date_to = None
        self.trunc_period_to_months = None
        self.split_period_by_months = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        general_contract = db_utils.create_general_contract(session, client, person,
                                                            on_dt=dt.datetime(2017, 1, 1),
                                                            firm_id=111,
                                                            services={35},
                                                            is_offer=0)

        self.contracts_data.append(ut.Struct(contract=general_contract,
                                             report_type='детализации не будет',
                                             filename_template='ваще не будет')
                                   )
        self.date_from = dt.datetime(2017, 3, 1)
        self.date_to = dt.datetime(2017, 2, 1)
        self.trunc_period_to_months = 'Да'
        self.split_period_by_months = 'Нет'

        return [row.contract for row in self.contracts_data], self.date_from, self.date_to, \
               self.trunc_period_to_months, self.split_period_by_months

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix,
                             messages=['Дата начала детализации должна быть больше даты конца.'])
        return res


class StartDtGreaterThenNowCase(AbstractDBTestCase):
    _representation = 'start_dt_greater_then_now'

    def __init__(self):
        self.contracts_data = []
        self.date_from = None
        self.date_to = None
        self.trunc_period_to_months = None
        self.split_period_by_months = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        general_contract = db_utils.create_general_contract(session, client, person,
                                                            on_dt=dt.datetime(2017, 1, 1),
                                                            firm_id=111,
                                                            services={35})

        self.contracts_data.append(ut.Struct(contract=general_contract,
                                             report_type='детализации не будет',
                                             filename_template='ваще не будет')
                                   )
        self.date_from = dt.datetime.now() + relativedelta(days=1)
        self.date_to = dt.datetime.now() + relativedelta(days=3)
        self.trunc_period_to_months = 'Нет'
        self.split_period_by_months = 'Нет'

        return [row.contract for row in self.contracts_data], self.date_from, self.date_to, \
               self.trunc_period_to_months, self.split_period_by_months

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix,
                             messages=['Дата начала детализации должна быть меньше текущей даты.'])
        return res


class PeriodIsGreaterTherPreviousMonthCase(AbstractDBTestCase):
    _representation = 'period_is_greater_then_previous_month'

    def __init__(self):
        self.contracts_data = []
        self.date_from = None
        self.date_to = None
        self.trunc_period_to_months = None
        self.split_period_by_months = None

    def _get_data(self, session):
        am = mapper.ActMonth()
        client, person = db_utils.create_client_person(session)
        general_contract = db_utils.create_general_contract(session, client, person,
                                                            on_dt=dt.datetime(2017, 1, 1),
                                                            firm_id=111,
                                                            services={35})

        self.contracts_data.append(ut.Struct(contract=general_contract,
                                             report_type='детализации не будет',
                                             filename_template='ваще не будет')
                                   )
        self.date_from = am.end_dt
        self.date_to = am.end_dt + relativedelta(months=1)
        self.trunc_period_to_months = 'Да'
        self.split_period_by_months = 'Нет'

        return [row.contract for row in self.contracts_data], self.date_from, self.date_to, \
               self.trunc_period_to_months, self.split_period_by_months

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix,
                             messages=['Дата начала детализации должна быть меньше текущего месяца '
                                       'при выбранной опции "Округлить период до полного месяца"'])
        return res


class NoTruncPeriodCurrentMonthSuccessCase(AbstractDBTestCase):
    _representation = 'no_trunc_period_current_month'

    def __init__(self):
        self.contracts_data = []
        self.date_from = None
        self.date_to = None
        self.trunc_period_to_months = None
        self.split_period_by_months = None

    def _get_data(self, session):
        am = mapper.ActMonth()
        client, person = db_utils.create_client_person(session)
        corp_contract = db_utils.create_spendable_contract(session, client, person, 613,
                                                           on_dt=dt.datetime(2017, 1, 1), firm_id=111)

        db_utils.create_thirdparty_transaction(session, corp_contract, 613, dt=am.end_dt)
        self.date_from = am.end_dt
        self.date_to = am.end_dt + relativedelta(days=1)
        self.trunc_period_to_months = 'Нет'
        self.split_period_by_months = 'Да'

        self.contracts_data.append(
            ut.Struct(contract=corp_contract,
                      report_type='детализация платежей',
                      filename_template='contract_{contract.external_id}__{date_from:%Y-%m-%d}__{date_to:%Y-%m-%d}.xls',
                      date_from=self.date_from, date_to=self.date_to),
        )

        return [row.contract for row in self.contracts_data], self.date_from, self.date_to, \
               self.trunc_period_to_months, self.split_period_by_months

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed)

        for row in self.contracts_data:
            attach_name = transliterate(row.filename_template.format(contract=row.contract,
                                                                     date_from=row.date_from,
                                                                     date_to=row.date_to).replace('/', '-'))
            res.attachments_names.append(attach_name)
            message = res.get_contract_line_message(contract=row.contract, report_type=row.report_type,
                                                    date_from=row.date_from, date_to=row.date_to,
                                                    comment='Выгрузка в файле ' + '%%' + attach_name + '%%')
            res.add_message(message)
        res.add_message('Успешно выгруженные примерные детализации во вложении.')

        return res


@mock.patch('autodasha.solver_cl.market_payment_detalisation.MarketPaymentDetalisation.__init__', _fake_init)
@pytest.mark.parametrize('issue_data',
                         [case() for case in AbstractDBTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['issue_data'])
def test_db(session, issue_data):
    queue_object, st_issue, case = issue_data

    req_res = case.get_result()

    patchers = []
    if req_res and req_res.patch_data:
        patchers = [mock.patch(p.obj, new=p.value) for p in req_res.patch_data]

    for patcher in patchers:
        patcher.start()

    solver = MarketPaymentDetalisation(queue_object, st_issue)
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
    assert res.delay is (req_res.transition == IssueTransitions.none)
    report = res.issue_report

    assert req_res.mode in ('equality', 'like'), 'Yo, that test is fucked up'
    if req_res.mode == 'equality':
        lines = map(unicode.strip, report.comment.strip().split('\n'))
        assert set([line for line in lines if line]) == set(req_res.comments)
    else:
        for req_comment_part in req_res.comments:
            assert req_comment_part in report.comment
    attachments = list(chain.from_iterable([comment.attachments for comment in report.comments if comment.attachments]))
    assert sorted(req_res.attachments_names) == sorted([getattr(attach, 'name', 'file') for attach in attachments])
    assert report.transition == req_res.transition
    assert report.assignee == req_res.assignee
