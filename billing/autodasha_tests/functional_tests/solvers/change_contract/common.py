# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import datetime as dt

import mock

from tests.autodasha_tests.common import staff_utils
from tests.autodasha_tests.functional_tests import case_utils


def get_approve_message(*args, **kwargs):
    return 'Нажми кнопку "Подтвердить", сука! https://jing.yandex-team.ru/files/autodasha/approve_button_something.png'


def _get_staff():
    noob = staff_utils.Person('noob')
    boss = staff_utils.Person('boss')
    alt_boss = staff_utils.Person('alt_boss')
    subdep = staff_utils.Department('subdep', [boss], [alt_boss], [noob])

    forced_noob = staff_utils.Person('forced_noob')
    shitty_boss = staff_utils.Person('shitty_boss')
    forced_boss = staff_utils.Person('forced_boss')
    forced_subdep = staff_utils.Department('forced_subdep', [shitty_boss], [], [forced_noob, forced_boss])

    bigboss1 = staff_utils.Person('bigboss1')
    bigboss2 = staff_utils.Person('bigboss2')
    dep = staff_utils.Department('dep', [bigboss1], [bigboss2], [], [subdep, forced_subdep])

    pchelovik = staff_utils.Person('pchelovik')
    ua = staff_utils.Department('ua', [pchelovik])

    medovik = staff_utils.Person('medovik')
    ua_taxi = staff_utils.Department('ua_taxi', [medovik])

    meganoob = staff_utils.Person('meganoob')
    megaboss = staff_utils.Person('megaboss')
    kommando = staff_utils.Person('kommando')

    absent_noob = staff_utils.Person('absent_noob')
    absent_boss1 = staff_utils.Person('absent_boss1')
    absent_dept1 = staff_utils.Department('absent_dept1', [absent_boss1], [], [absent_noob])

    absent_boss2 = staff_utils.Person('absent_boss2')
    absent_dept2 = staff_utils.Department('absent_dept2', [absent_boss2], [], [], [absent_dept1])

    absent_boss3 = staff_utils.Person('absent_boss3')
    absent_dept3 = staff_utils.Department('absent_dept3', [absent_boss3], [], [], [absent_dept2])

    god = staff_utils.Person('god')
    backoffice = staff_utils.Department('backoffice', [megaboss, kommando], [], [meganoob],
                                        [dep, ua, ua_taxi, absent_dept3])

    yandex = staff_utils.Department('yandex', [god], [], [], [backoffice])

    staff = staff_utils.StaffMock(yandex)

    return staff


def _get_gap():
    absent_boss1 = staff_utils.PersonGap('absent_boss1')
    absent_boss2 = staff_utils.PersonGap('absent_boss2')
    absent_boss3 = staff_utils.PersonGap('absent_boss3')
    gap = staff_utils.GapMock([absent_boss1, absent_boss2, absent_boss3])
    return gap


def checks_closed_dt():
    return dt.datetime(2016, 1, 1)


def _get_ro_session(self):
    return self.old_col.session


def func_test_patches(f):
    staff = _get_staff()
    gap = _get_gap()

    staff_path = 'autodasha.core.api.staff.Staff.%s'
    gap_path = 'autodasha.core.api.gap.Gap.%s'
    patches = [
        (
            staff_path % '__init__',
            lambda *args: None
        ),
        (
            staff_path % '_get_person_data',
            lambda s, *a, **k: staff._get_person_data(*a, **k)),
        (
            staff_path % '_get_department_data',
            lambda s, *a, **k: staff._get_department_data(*a, **k)),
        (
            staff_path % 'is_person_related_to_departments',
            lambda s, *a, **k: staff.is_person_related_to_departments(*a, **k)),
        (
            gap_path % '__init__',
            lambda *args: None),
        (
            gap_path % '_find_gaps',
            lambda s, *a, **k: gap._find_gaps(*a, **k)),
        (
            'autodasha.solver_cl.change_contract.checks._closed_dt',
            checks_closed_dt),
        (
            'autodasha.solver_cl.base_solver.BaseSolver.get_approve_message',
            get_approve_message
        ),
        (
            'balance.actions.contract.contract_payments_check.ContractPaymentsCheck._get_ro_session',
            _get_ro_session,
        ),
    ]

    for target, func in patches:
        f = mock.patch(target, func)(f)

    return f


class RequiredResult(case_utils.RequiredResult):

    _comments = {
        'done': 'Изменения внесены в договор.',
        'enqueued': 'Изменённые договор и допсоглашение добавлены в очередь на выгрузку в ОЕБС.',
        'approval_outdated': 'Нет ответа более 3 дней, поэтому мы закрываем тикет (по регламенту). '
                             'Если ваш запрос актуален, пожалуйста, создайте новый тикет, '
                             'не переоткрывая данный, и свяжите со старым.',
        'rule_no_manager': 'Произошла ошибка при попытке внести изменения в условия договора: !!Rule violation: \'Не выбран менеджер\'!!.',
    }

    def __init__(self, contract=None, collateral=None, state=None, summonees=None, delay=None, **kwargs):
        if collateral:
            self.col = collateral
        else:
            self.col = contract.col0
        self.contract = contract
        self.state = state or []
        self.delay = None
        self.c_exports = []
        self.col_exports = []
        self.summonees = summonees and set(summonees)
        self.delay = delay
        self.pa_state = kwargs.get('pa_state', None)
        self.cn_state = kwargs.get('cn_state', None)

        super(RequiredResult, self).__init__(**kwargs)

    def set_messages(self, done=None, enqueued=False, outdated=False, rule_no_manager=False, **kwargs):
        if done:
            self.add_message(self._comments['done'])

        if enqueued:
            self.add_message(self._comments['enqueued'])

        if outdated:
            self.add_message(self._comments['approval_outdated'])

        if rule_no_manager:
            self.add_message(self._comments['rule_no_manager'])

    def set_object_states(self, enqueued=None, **kwargs):
        if enqueued:
            self.c_exports.append(self.col.contract.id)
            self.col_exports.append(self.col.id)

            if self.col.collateral_type_id is not None:
                self.col_exports.append(self.col.contract.col0.id)


class AbstractBaseDBTestCase(case_utils.AbstractDBTestCase):
    _description = '''
№ Договора: {c.external_id}
{params}
Причина изменения в договоре / комментарий: а ещё я крестиком вышивать могу
'''.strip()

    _summary = 'Внесение изменений в договоры'

    _params_lines = {
        'col': '№ ДС (если в нем изменения): %s',
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
        'other_changes': 'Другие изменения: %s',
        'external_id': "№ Договора (новый номер): %s",
        'nds': "Ставка НДС: %s",
        'need_recalc_partner_reward': 'Пересчитать партнерское вознаграждение: %s',
        'partner_reward_recalc_period': 'Период для пересчета: %s',
        'unilateral_acts': 'Односторонние акты: %s',
        'other_changes_checkbox': 'МНЕ НУЖНЫ ДРУГИЕ ИЗМЕНЕНИЯ: %s',
        'print_form_type': 'Печатная форма: %s',
        'payment_type': 'Период актов: %s',
        'offer_confirmation_type': 'Способ подтверждения оферты: %s',
        'payment_term': 'Срок оплаты счетов: %s',
        'memo': 'Примечание: %s'
    }

    def get_description(self, session):
        contract, params = self._get_data(session)
        params_strs = []
        for k, v in params:
            param_line = self._params_lines[k]
            if isinstance(v, dt.datetime):
                v_str = v.strftime('%Y-%m-%d')
            elif k == 'col':
                v_str = v.num
            # elif k == 'nds':
            #     v_str = 'НДС 18%' if v == 18 else 'УСН' if v == 0 else v
            else:
                v_str = unicode(v)

            params_strs.append(param_line % v_str)

        return self._description.format(c=contract, params='\n'.join(params_strs))

    def __init__(self):
        self.contract = None
        self.collateral = None
