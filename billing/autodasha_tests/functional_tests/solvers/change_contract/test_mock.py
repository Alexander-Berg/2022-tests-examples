# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import datetime as dt

import pytest
import mock

from balance import mapper
from balance import muzzle_util as ut

from autodasha.solver_cl import ParseException, ChangeContract
from autodasha.solver_cl.change_contract.state import State

from tests.autodasha_tests.functional_tests import case_utils
from tests.autodasha_tests.functional_tests import mock_utils

from tests.autodasha_tests.functional_tests.solvers.change_contract.common import func_test_patches


class AbstractMockTestCase(case_utils.AbstractMockTestCase):
    _default_lines = {
        'summary': 'Внесение изменений в договоры',
        'contract': '№ Договора: %s',
        'collateral': '№ ДС (если в нем изменения): %s',
        'col_num': '№ ДС: %s',
        'col_dt': 'Дата ДС (в настоящий момент): %s',
        'dt': 'Дата начала: %s',
        'finish_dt': 'Дата окончания: %s',
        'is_booked': 'Бронь подписи: %s',
        'remove_is_booked': 'Снять галку Бронь подписи: %s',
        'is_faxed': 'Подписан по факсу: %s',
        'remove_is_faxed': 'Снять галку Подписан по факсу: %s',
        'is_signed': 'Подписан: %s',
        'remove_is_signed': 'Снять галку Подписан оригинал: %s',
        'sent_dt': 'Отправлен оригинал: %s',
        'remove_sent_dt': 'Снять галку Отправлен оригинал: %s',
        'is_cancelled': 'Поставить галку Аннулирован: %s',
        'other_changes': 'Другие изменения: ну я даже не знаю, нарисуйте котёночка',
        'reason': 'Причина изменения в договоре / комментарий: Хатю!',
        'firm_name_for_comment': 'ООО Яндекс.Сексшоп',
        'services_for_comment': 'Без сервисов',
        'external_id': '№ Договора (новый номер): %s',
        'need_recalc_partner_reward': 'Пересчитать партнерское вознаграждение: %s',
        'partner_reward_recalc_period': 'Период для пересчета: %s',
        'nds': 'Ставка НДС: %s'
    }


class AbstractMockStateOkCase(AbstractMockTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class SetParamsMockStateOkCase(AbstractMockStateOkCase):
    _representation = 'set_params'

    def get_data(self, mock_manager):
        firm = mock_utils.create_firm(mock_manager)
        mock_utils.create_contract(mock_manager, external_id='666666/666', firm=firm, **{'current.services': None})
        lines = [
            self._get_default_line(contract='666666/666'),
            self._get_default_line(col_num='666'),
            self._get_default_line(dt='2016-01-01'),
            self._get_default_line(is_booked='2016-03-01'),
            self._get_default_line(is_faxed='2016-04-01'),
            self._get_default_line(is_signed='2016-05-01'),
            self._get_default_line(sent_dt='2016-06-01'),
            self._get_default_line(is_cancelled='True'),
            self._get_default_line('other_changes'),
            self._get_default_line('firm_name_for_comment'),
            self._get_default_line('services_for_comment'),
            self._get_default_line(external_id='666/66'),
            self._get_default_line(need_recalc_partner_reward='да'),
            self._get_default_line(
                partner_reward_recalc_period='2018-01-01 - 2019-01-01'
            ),
            self._get_default_line(nds='НДС 0')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return [
            ('col_num', '666'),
            ('dt', dt.datetime(2016, 1, 1)),
            ('is_booked_dt', dt.datetime(2016, 3, 1)),
            ('is_booked', True),
            ('is_faxed', dt.datetime(2016, 4, 1)),
            ('is_signed', dt.datetime(2016, 5, 1)),
            ('sent_dt', dt.datetime(2016, 6, 1)),
            ('is_cancelled', dt.datetime.now()),
            ('non_standard_changes', True),
            ('is_non_resident', True),
            ('firm_name_for_comment', 'ООО Яндекс.Сексшоп'),
            ('services_for_comment', 'Без сервисов'),
            ('external_id', '666/66'),
            ('need_recalc_partner_reward', True),
            (
                'partner_reward_recalc_period',
                (dt.datetime(2018, 1, 1), dt.datetime(2019, 1, 1)),
            ),
            ('nds', 'НДС 0')
        ]


class RemoveDatesMockStateOkCase(AbstractMockStateOkCase):
    _representation = 'remove_dates'

    def get_data(self, mock_manager):
        firm = mock_utils.create_firm(mock_manager)
        mock_utils.create_contract(mock_manager, external_id='666666/666', firm=firm, **{'current.services': None})
        lines = [
            self._get_default_line(contract='666666/666'),
            self._get_default_line(remove_is_booked='True'),
            self._get_default_line(remove_is_faxed='Да'),
            self._get_default_line(remove_is_signed='true'),
            self._get_default_line(remove_sent_dt='true'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return [
            ('is_booked', None),
            ('is_faxed', None),
            ('is_signed', None),
            ('sent_dt', None),
            ('non_standard_changes', False),
            ('is_non_resident', True),
            ('services_for_comment', 'Без сервисов'),
            ('firm_name_for_comment', 'ООО Яндекс.Сексшоп'),
        ]


class IncorrectBoolParameterMockStateOkCase(AbstractMockStateOkCase):
    _representation = 'incorrect_bool_parameters'

    def get_data(self, mock_manager):
        firm = mock_utils.create_firm(mock_manager)
        mock_utils.create_contract(mock_manager, external_id='666666/666', firm=firm, **{'current.services': None})
        lines = [
            self._get_default_line(contract='666666/666'),
            self._get_default_line(remove_is_booked='ну наверно'),
            self._get_default_line(remove_is_faxed='палюбас'),
            self._get_default_line(remove_is_signed='да ваще внатуре'),
            self._get_default_line(is_cancelled='а почему вы спрашиваете?'),
            self._get_default_line(need_recalc_partner_reward='false false false!!!')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return [
            ('non_standard_changes', False),
            ('is_non_resident', True),
            ('services_for_comment', 'Без сервисов'),
            ('firm_name_for_comment', 'ООО Яндекс.Сексшоп'),
        ]


class ContractIdTeamMockStateOkCase(AbstractMockStateOkCase):
    _representation = 'contract_id_w_team'

    def get_data(self, mock_manager):
        firm = mock_utils.create_firm(mock_manager)
        contract = mock_utils.create_contract(mock_manager, external_id='666666/666', firm=firm, **{'current.services': None})
        mock_manager.add_object_key(mapper.Contract, 666, contract)
        lines = [
            self._get_default_line(contract='666666/666'),
            self._get_default_line(dt='2016-01-01'),
        ]
        return self._get_default_line('summary'), lines

    def get_comments(self):
        dasha_cmt = 'Договоров с таким номером больше одного, дятел. Гони ссылку. https://yandex.ru/images/'
        lnk = 'https://admin.balance.yandex-team.ru/contract-edit.xml?contract_id=666'
        return [
            ('autodasha', dasha_cmt, dt.datetime(2016, 1, 15)),
            ('loh_ushastii', lnk, dt.datetime(2016, 1, 16)),
        ]

    def get_result(self):
        return [
            ('dt', dt.datetime(2016, 1, 1)),
            ('non_standard_changes', False),
            ('is_non_resident', True),
            ('services_for_comment', 'Без сервисов'),
            ('firm_name_for_comment', 'ООО Яндекс.Сексшоп'),
        ]


class ContractIdWOTeamMockStateOkCase(AbstractMockStateOkCase):
    _representation = 'contract_id_wo_team'

    def get_data(self, mock_manager):
        firm = mock_utils.create_firm(mock_manager)
        contract = mock_utils.create_contract(mock_manager, external_id='666666/666', firm=firm, **{'current.services': None})
        mock_manager.add_object_key(mapper.Contract, 666, contract)
        lines = [
            self._get_default_line(contract='666666/666'),
            self._get_default_line(dt='2016-01-01'),
        ]
        return self._get_default_line('summary'), lines

    def get_comments(self):
        dasha_cmt = 'Договоров с таким номером больше одного, дятел. Гони ссылку. https://yandex.ru/images/'
        lnk = 'https://admin.balance.yandex.ru/contract-edit.xml?contract_id=666'
        return [
            ('autodasha', dasha_cmt, dt.datetime(2016, 1, 15)),
            ('loh_ushastii', lnk, dt.datetime(2016, 1, 16)),
        ]

    def get_result(self):
        return [
            ('dt', dt.datetime(2016, 1, 1)),
            ('non_standard_changes', False),
            ('is_non_resident', True),
            ('services_for_comment', 'Без сервисов'),
            ('firm_name_for_comment', 'ООО Яндекс.Сексшоп'),
        ]


class ContractIdWOEditMockStateOkCase(AbstractMockStateOkCase):
    _representation = 'contract_id_wo_edit'

    def get_data(self, mock_manager):
        firm = mock_utils.create_firm(mock_manager)
        contract = mock_utils.create_contract(mock_manager, external_id='666666/666', firm=firm, **{'current.services': None})
        mock_manager.add_object_key(mapper.Contract, 666, contract)
        lines = [
            self._get_default_line(contract='666666/666'),
            self._get_default_line(dt='2016-01-01'),
        ]
        return self._get_default_line('summary'), lines

    def get_comments(self):
        dasha_cmt = 'Договоров с таким номером больше одного, дятел. Гони ссылку. https://yandex.ru/images/'
        lnk = 'https://admin.balance.yandex.ru/contract.xml?contract_id=666'
        return [
            ('autodasha', dasha_cmt, dt.datetime(2016, 1, 15)),
            ('loh_ushastii', lnk, dt.datetime(2016, 1, 16)),
        ]

    def get_result(self):
        return [
            ('dt', dt.datetime(2016, 1, 1)),
            ('non_standard_changes', False),
            ('is_non_resident', True),
            ('services_for_comment', 'Без сервисов'),
            ('firm_name_for_comment', 'ООО Яндекс.Сексшоп'),
        ]


def find_and_remove_epa_func_from_getters(getters):
    # слава богу я смог мокнуть эту *нехорошее слово*
    for pair in getters:
        if pair[1] in ('empty_personal_accounts', 'payments_check_result'):
            getters.remove(pair)
    return getters


@func_test_patches
@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in AbstractMockStateOkCase._cases],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_mock_state_ok(mock_manager, mock_issue_data, mock_config):
    session = mock_manager.construct_session()
    issue, case = mock_issue_data

    req_state = case.get_result()

    solver = ChangeContract(ut.Struct(session=session, issue=None), issue)

    with mock.patch('autodasha.solver_cl.change_contract.state.State._getters',
                    find_and_remove_epa_func_from_getters(State._getters)):
        state = solver._parse_issue()

    state._state.pop('c')
    state._state.pop('after_checker')
    state._state.pop('changed_state')
    if 'col' in state._state:
        state._state.pop('col')

    for k, v in req_state:
        val = getattr(state, k)
        if isinstance(v, dt.datetime):
            assert val.date() == v.date(), k
        else:
            assert val == v, k
        state._state.pop(k)

    assert not state._state


class AbstractMockStateFailCase(AbstractMockTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    _tests = []


class DoubledParameterMockStateFailCase(AbstractMockStateFailCase):

    @property
    def _representation(self):
        return 'doubled_parameter_%s' % self._name

    _tests = [
        ('col_num', 'Номер ДС', '666'),
        ('dt', 'Дата начала', '2016-01-01'),
        ('finish_dt', 'Дата окончания', '2016-01-01'),
        ('is_booked', 'Дата брони подписи', '2016-01-01'),
        ('remove_is_booked', 'Дата брони подписи', 'True'),
        ('is_faxed', 'Подписан по факсу', '2016-01-01'),
        ('remove_is_faxed', 'Подписан по факсу', 'True'),
        ('is_signed', 'Подписан', '2016-01-01'),
        ('remove_is_signed', 'Подписан', 'True'),
        ('sent_dt', 'Отправлен оригинал', '2016-01-01'),
        ('remove_sent_dt', 'Отправлен оригинал', 'Да'),
        ('is_cancelled', 'Аннулирован', 'True')
    ]

    def __init__(self, param_name, param_repr, value):
        self._name = param_name
        self._repr = param_repr
        self._value = value

    def get_data(self, mock_manager):
        firm = mock_utils.create_firm(mock_manager)
        mock_utils.create_contract(mock_manager, external_id='666666/666', firm=firm, **{'current.services': None})
        params = {self._name: self._value}
        lines = [
            self._get_default_line(contract='666666/666'),
            self._get_default_line(**params),
            self._get_default_line(**params),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Формат параметра "%s" некорректен. Заполни, пожалуйста, форму еще раз.' % self._repr


class InconsistentParameterMockStateFailCase(AbstractMockStateFailCase):
    @property
    def _representation(self):
        return 'inconsistent_parameter_%s' % self._set

    _tests = [
        ('is_booked', 'remove_is_booked', 'Дата брони подписи'),
        ('is_faxed', 'remove_is_faxed', 'Подписан по факсу'),
        ('is_signed', 'remove_is_signed', 'Подписан'),
        ('sent_dt', 'remove_sent_dt', 'Отправлен оригинал'),
    ]

    def __init__(self, set_param_name, remove_param_name, param_repr, value='2016-01-01'):
        self._set = set_param_name
        self._remove = remove_param_name
        self._repr = param_repr
        self._value = value

    def get_data(self, mock_manager):
        firm = mock_utils.create_firm(mock_manager)
        mock_utils.create_contract(mock_manager, external_id='666666/666', firm=firm, **{'current.services': None})
        lines = [
            self._get_default_line(contract='666666/666'),
            self._get_default_line(**{self._set: self._value}),
            self._get_default_line(**{self._remove: 'True'}),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'К изменению указаны противоречивые значения параметра "%s". ' \
               'Уточни, пожалуйста, и заполни при необходимости форму еще раз.' % self._repr


class IncorrectDateParameterMockStateFailCase(AbstractMockStateFailCase):
    @property
    def _representation(self):
        return 'incorrect_date_%s' % self._name

    _tests = [
        ('dt', 'Дата начала', 'ну там типа позавчера'),
        ('finish_dt', 'Дата окончания', '2066-66-66'),
        ('is_booked', 'Дата брони подписи', 'ты чё, сам не в курсе, внатуре?'),
        ('is_faxed', 'Подписан по факсу', '01 may 2016'),
        ('is_signed', 'Подписан', '16-01-01'),
        ('sent_dt', 'Отправлен оригинал', '01.01.2016'),
    ]

    def __init__(self, param_name, param_repr, value):
        self._name = param_name
        self._repr = param_repr
        self._value = value

    def get_data(self, mock_manager):
        firm = mock_utils.create_firm(mock_manager)
        mock_utils.create_contract(mock_manager, external_id='666666/666', firm=firm, **{'current.services': None})
        lines = [
            self._get_default_line(contract='666666/666'),
            self._get_default_line(**{self._name: self._value}),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Формат параметра "%s" некорректен. Заполни, пожалуйста, форму еще раз.' % self._repr


class IncorrectColDTMockStateFailCase(AbstractMockStateFailCase):
    @property
    def _representation(self):
        return 'incorrect_col_dt_%s' % self._value

    _tests = [
        ('2016-666-666', )
    ]

    def __init__(self, value):
        self._value = value

    def get_data(self, mock_manager):
        firm = mock_utils.create_firm(mock_manager)
        mock_utils.create_contract(mock_manager, external_id='666666/666', firm=firm, **{'current.services': None})
        lines = [
            self._get_default_line(contract='666666/666'),
            self._get_default_line(collateral=666),
            self._get_default_line(col_dt=self._value),
            self._get_default_line(dt='2016-02-01'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Некорректно заполнена дата ДС для изменения. Заполни, пожалуйста, форму еще раз.'


class ColDTWOColMockStateFailCase(AbstractMockStateFailCase):
    _representation = 'col_dt_wo_col'

    _tests = [
        ('2016-01-01', )
    ]

    def __init__(self, value):
        self._value = value

    def get_data(self, mock_manager):
        firm = mock_utils.create_firm(mock_manager)
        mock_utils.create_contract(mock_manager, external_id='666666/666', firm=firm, **{'current.services': None})
        lines = [
            self._get_default_line(contract='666666/666'),
            self._get_default_line(col_dt=self._value),
            self._get_default_line(dt='2016-02-01'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Указана дата ДС для изменения без указания номера ДС. Заполни, пожалуйста, форму еще раз.'


@func_test_patches
@pytest.mark.parametrize('mock_issue_data',
                         [case(*args) for case in AbstractMockStateFailCase._cases for args in case._tests],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_mock_state_fail(mock_manager, mock_issue_data, mock_config):
    session = mock_manager.construct_session()
    issue, case = mock_issue_data

    req_msg = case.get_result()

    solver = ChangeContract(ut.Struct(session=session, issue=None), issue)
    with pytest.raises(ParseException) as exc_info:
        state = solver._parse_issue()

    assert exc_info.value.message == req_msg
