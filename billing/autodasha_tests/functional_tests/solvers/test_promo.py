# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import datetime as dt
import itertools
import mock
import pytest

from autodasha.core.api.tracker import IssueTransitions
from autodasha.comments.promo import PromoCommentsManager
from autodasha.solver_cl.promo import BaseSolver, Promo
from autodasha.utils.solver_utils import D
from balance.mapper import PromoCode
from tests.autodasha_tests.functional_tests import case_utils


cmt = PromoCommentsManager()

robot = 'autodasha'
support_manager = 'electricsheep'
responsible_manager = 'electricsheep'

confirm_cmt = '''
кто:{approver}, проверь, пожалуйста, и подтверди:
%%
{{
    client_id = {client_id},
    start_dt = {start_dt},
    end_dt = {end_dt},
    calc_params = {calc_params},
    calc_class_name = {calc_class_name},
    checks = {{
              new_clients_only={new_clients_only},
              need_unique_urls={need_unique_url},
              service_ids={service_id},
              skip_reservation_check={skip_reservation_check},
              minimal_amounts={minimal_amounts}
              }},
    event_name = {event},
    firm_id = {firm_id},
    reservation_days = {reservation_days}
}}
%%
'''.strip()


def func_test_patches(f):
    promo_path = 'autodasha.solver_cl.promo.Promo.%s'

    patches = [
        (
            promo_path % '_read_xls',
            lambda s, att: att.read()[0]
        )
    ]

    for target, func in patches:
        f = mock.patch(target, func)(f)

    return f


@func_test_patches
def do_test(session, issue_data):
    queue_object, st_issue, case = issue_data

    solver = Promo(queue_object, st_issue)
    res = solver.solve()

    required_res = case.get_result()

    if required_res is None:
        assert res.commit is False
        assert res.delay is False
        assert res.issue_report is None
        return

    assert res.commit is required_res.commit
    assert res.delay == required_res.delay

    report = res.issue_report

    if not report.comments:
        assert not required_res.comments
    else:
        if 'Неизвестная ошибка:' in required_res.comments:
            assert 'Неизвестная ошибка:' in report.comments[-1].text
        else:
            assert set(required_res.comments) == set(cmt.text for cmt in report.comments)
    assert required_res.transition == report.transition
    assert required_res.assignee == report.assignee
    summonees = set(itertools.chain.from_iterable(cmt.summonees or [] for cmt in report.comments))
    req_summonees = getattr(required_res, 'summonees', None)
    assert (req_summonees and set(req_summonees)) == (summonees or None)

    for row in required_res.state:
        # pc = case.promocodes[row['code']]
        pc = session.query(PromoCode).getone(code=row['code'].replace('-', '').upper())
        pg = pc.group

        assert pc.client_id == row.get('client_id')
        assert pg.start_dt == dt.datetime.strptime(row.get('start_dt'), '%d.%m.%Y')
        assert pg.end_dt == dt.datetime.strptime(row.get('end_dt'), '%d.%m.%Y')
        assert pg.calc_class_name == row.get('calc_class_name')

        bonuses = pg.calc_params.get('currency_bonuses') or pg.calc_params.get('product_bonuses')
        if bonuses:
            for key, val in bonuses.iteritems():
                bonuses[key] = D(val)
        if pg.calc_params.get('discount_pct'):
            pg.calc_params['discount_pct'] = str(pg.calc_params['discount_pct'])

        assert pg.calc_params == row.get('calc_params')

        assert pg.calc_params.get('apply_on_create') == row.get('calc_params').get('apply_on_create')

        min_amounts = pg.minimal_amounts
        if min_amounts:
            for key, val in min_amounts.iteritems():
                min_amounts[key] = D(val)
            assert min_amounts == row.get('minimal_amounts')

        if row.get('_product_ids'):
            assert pg.product_ids == set(row.get('_product_ids'))

        assert pg.service_ids == row.get('service_ids')
        if row.get('event'):
            assert pg.event.event == row.get('event')
        assert pg.firm_id == row.get('firm_id')
        assert pg.new_clients_only == row.get('new_clients_only')
        assert pg.need_unique_urls == row.get('need_unique_url')
        assert pg.skip_reservation_check == row.get('skip_reservation_check')


class RequiredResult(case_utils.RequiredResult):
    def __init__(self, commit=True, delay=False, state=None):
        super(RequiredResult, self).__init__()
        self.commit = commit
        self.delay = delay
        self.state = state or {}


class FileGenMixin(object):
    pc_group_cols = dict(
        start_dt='23.09.2019',
        end_dt='01.10.2019',
        middle_dt='01.10.2019',
        payment=333,
        bonus1=666,
        bonus2=666,
        event='Kek Event',
        firm_id=27,
        service_id=7,
        reservation_days=30,
        new_clients_only=0,
        need_unique_url=0,
        skip_reservation_check=1
    )
    codes = [('kekekekek', 12345), ('fufufufu', 54321)]
    code_col = 'code'
    client_col = 'client_id'

    def _get_codes(self):
        if isinstance(self.codes[0], basestring):
            return [{self.code_col: i} for i in self.codes]
        return [{self.code_col: i, self.client_col: j} for i, j in self.codes]

    def get_contents(self):
        codes = self._get_codes()

        return [dict(code.items() + self.pc_group_cols.items()) for code in codes]


class AbstractPromoTestCase(case_utils.AbstractDBTestCase, FileGenMixin):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    _descr_vars = ['calc_class', 'is_discount', 'bonus_rub', 'bonus_byn', 'bonus_usd', 'bonus_eur', 'bonus_usz',
                   'bonus_kzt', 'min_rub', 'min_byn', 'min_usd', 'min_eur', 'min_usz', 'min_kzt']
    _description = '''
Тип промокода: {calc_class}
Скидка: {is_discount}
Минимальный платеж:
RUB: {min_rub}\r
BYN: {min_byn}\r
USD: {min_usd}\r
EUR: {min_eur}\r
USZ: {min_usz}\r
KZT: {min_kzt}\r
Бонус:
RUB: {bonus_rub}
BYN: {bonus_byn}
USD: {bonus_usd}
EUR: {bonus_eur}
USZ: {bonus_usz}
KZT: {bonus_kzt}
Файл: https://forms.yandex-team.ru/files?path=%2F42367%2Faf8c1856f28089aef5c8be5a7de53058_belarus_loyalnostavgust_100byn.xls
Комментарий:'''.strip()

    _filename = 'file_name.xls'

    def __init__(self, robot, support_manager, responsible_manager):
        super(AbstractPromoTestCase, self).__init__()

        self.robot = robot
        self.support_manager = support_manager
        self.responsible_manager = responsible_manager

    def get_attachments(self):
        contents = self.get_contents()
        return [(self._filename, contents)]

    def _get_data(self, session):
        desc = {var: '' for var in self._descr_vars}
        desc['calc_class'] = 'с бонусом в деньгах (FixedSumBonusPromoCodeGroup)'
        desc['is_discount'] = 'Нет'
        desc['min_byn'] = 333
        desc['bonus_byn'] = 666

        return desc

    def get_confirm_comment(self, **kwargs):
        cmt_dict = self.pc_group_cols.copy()
        cmt_dict['client_id'] = True
        cmt_dict['service_id'] = '[7]'
        cmt_dict['approver'] = self.support_manager
        cmt_dict['calc_class_name'] = 'FixedSumBonusPromoCodeGroup'
        cmt_dict['calc_params'] = '''{'currency_bonuses': {u'BYN': Decimal('666')}, u'apply_on_create': False}'''
        cmt_dict['minimal_amounts'] = '''{u'BYN': Decimal('333')}'''

        for key, val in kwargs.iteritems():
            cmt_dict[key] = val

        return confirm_cmt.format(**cmt_dict)


class AbstractQtyBonusTestCase(AbstractPromoTestCase):
    def _get_data(self, session):
        desc = {var: '' for var in self._descr_vars}
        desc['calc_class'] = 'с бонусом в количестве (FixedQtyBonusPromoCodeGroup)'
        desc['is_discount'] = 'Нет'
        desc['min_rub'] = 2000
        desc['min_usd'] = 30
        desc['min_usz'] = 200000

        desc['bonus_rub'] = 2500
        desc['bonus_usd'] = 35
        desc['bonus_usz'] = 250000

        return desc


class QtyBonusTestCase(AbstractQtyBonusTestCase):
    _representation = 'qty_bonus'

    def get_result(self):
        rr = RequiredResult(delay=True)
        rr.transition = IssueTransitions.opened
        rr.add_message(
            self.get_confirm_comment(
                calc_class_name='FixedQtyBonusPromoCodeGroup',
                calc_params=(
                    '''{u'product_bonuses': '''
                    '''{u'503163': Decimal('35'), u'503162': Decimal('2500'), u'508588': Decimal('35'), u'1475': Decimal('35')}, '''
                    '''u'apply_on_create': False}'''
                ),
                minimal_amounts='''{u'RUB': Decimal('2000'), u'USD': Decimal('30'), u'USZ': Decimal('200000')}'''
            )
        )

        rr.summonees = [self.support_manager]

        return rr


# Min pay not matching the file
class MinPayNotMatchingFileTestCase(AbstractPromoTestCase):
    _representation = 'min_pay_not_matching_file'

    def _get_data(self, session):
        desc = super(MinPayNotMatchingFileTestCase, self)._get_data(session)
        desc['min_byn'] = 334

        return desc

    def get_result(self):
        rr = RequiredResult()
        rr.transition = IssueTransitions.wont_fix
        rr.add_message(cmt('ma_mismatch'))

        return rr


# Bonus not matching the file
class BonusNotMatchingFileTestCase(AbstractPromoTestCase):
    _representation = 'bonus_not_matching_file'

    def _get_data(self, session):
        desc = {var: '' for var in self._descr_vars}
        desc['calc_class'] = 'с бонусом в деньгах (FixedSumBonusPromoCodeGroup)'
        desc['is_discount'] = 'Нет'
        desc['min_byn'] = 333
        desc['bonus_byn'] = 667

        return desc

    def get_result(self):
        rr = RequiredResult()
        rr.transition = IssueTransitions.wont_fix
        rr.add_message(cmt('bonus_mismatch'))

        return rr


# Missing column
class MissingColumnTestCase(AbstractPromoTestCase):
    _representation = 'missing_column'

    pc_group_cols = AbstractPromoTestCase.pc_group_cols.copy()
    pc_group_cols.pop('payment')

    def get_result(self):
        rr = RequiredResult()
        rr.transition = IssueTransitions.wont_fix
        rr.add_message(cmt('missing_column', 'payment'))

        return rr


# Unknown column
class UnknownColumnTestCase(AbstractPromoTestCase):
    _representation = 'unknown_column'

    client_col = 'fuck_id'

    def get_result(self):
        rr = RequiredResult()
        rr.transition = IssueTransitions.wont_fix
        rr.add_message(cmt('unknown_column', 'fuck_id'))

        return rr


# Empty column
class EmptyColumnTestCase(AbstractPromoTestCase):
    _representation = 'empty_column'

    pc_group_cols = AbstractPromoTestCase.pc_group_cols.copy()
    pc_group_cols['event'] = None

    def get_result(self):
        rr = RequiredResult()
        rr.transition = IssueTransitions.wont_fix
        rr.add_message(cmt('empty_column', 'event'))

        return rr


# Multiple params
class MultipleParamsTestCase(AbstractPromoTestCase):
    _representation = 'multiple_params'

    pc_group_cols = AbstractPromoTestCase.pc_group_cols.copy()

    def get_attachments(self):
        old_codes = self.codes
        self.codes = [old_codes[0]]
        contents1 = self.get_contents()

        self.pc_group_cols['payment'] += 42
        self.codes = [old_codes[1]]
        contents2 = self.get_contents()

        return [('file_name.xls', contents1 + contents2)]

    def get_result(self):
        rr = RequiredResult()
        rr.transition = IssueTransitions.wont_fix
        rr.add_message(cmt('multiple_params'))

        return rr


# Multiple currencies for fixed sum
class MultipleCurrenciesTestCase(AbstractPromoTestCase):
    _representation = 'multiple_currencies'

    def _get_data(self, session):
        desc = {var: '' for var in self._descr_vars}
        desc['calc_class'] = 'с бонусом в деньгах (FixedSumBonusPromoCodeGroup)'
        desc['is_discount'] = 'Нет'
        desc['min_byn'] = 333
        desc['bonus_byn'] = 666
        desc['min_rub'] = 42
        desc['bonus_rub'] = 1488

        return desc

    def get_result(self):
        rr = RequiredResult()
        rr.transition = IssueTransitions.wont_fix
        rr.add_message(cmt('multiple_currencies'))

        return rr


# Different currencies for min pay and bonus (fixed sum)
class DifferentCurrenciesFixedSumTestCase(AbstractPromoTestCase):
    _representation = 'different_cur_fixed_sum'

    def _get_data(self, session):
        desc = {var: '' for var in self._descr_vars}
        desc['calc_class'] = 'с бонусом в деньгах (FixedSumBonusPromoCodeGroup)'
        desc['is_discount'] = 'Нет'
        desc['min_byn'] = 333
        desc['bonus_rub'] = 666

        return desc

    def get_result(self):
        rr = RequiredResult()
        rr.transition = IssueTransitions.wont_fix
        rr.add_message(cmt('currency_mismatch'))

        return rr


# Different currencies for min pay and bonus (fixed qty)
class DifferentCurrenciesFixedQtyTestCase(AbstractPromoTestCase):
    _representation = 'different_cur_fixed_qty'

    def _get_data(self, session):
        desc = {var: '' for var in self._descr_vars}
        desc['calc_class'] = 'с бонусом в количестве (FixedQtyBonusPromoCodeGroup)'
        desc['is_discount'] = 'Нет'
        desc['min_byn'] = 333
        desc['min_rub'] = 42
        desc['bonus_rub'] = 666
        desc['bonus_usd'] = 1488

        return desc

    def get_result(self):
        rr = RequiredResult()
        rr.transition = IssueTransitions.wont_fix
        rr.add_message(cmt('currency_mismatch'))

        return rr


# Wrong currency for firm
class WrongCurrencyForFirmTestCase(AbstractPromoTestCase):
    _representation = 'wrong_cur_for_firm'

    def _get_data(self, session):
        desc = {var: '' for var in self._descr_vars}
        desc['calc_class'] = 'с бонусом в деньгах (FixedSumBonusPromoCodeGroup)'
        desc['is_discount'] = 'Нет'
        desc['min_rub'] = 333
        desc['bonus_rub'] = 666

        return desc

    def get_result(self):
        rr = RequiredResult()
        rr.transition = IssueTransitions.wont_fix
        rr.add_message(cmt('firm_cur'))

        return rr


# Fucked up dates (start_dt > end_dt, weird format, 31st of June, ...)
class BigStartDtTestCase(AbstractPromoTestCase):
    _representation = 'start_dt_gt_end_dt'

    pc_group_cols = AbstractPromoTestCase.pc_group_cols.copy()
    pc_group_cols['start_dt'] = '02.10.2019'

    def get_result(self):
        rr = RequiredResult()
        rr.transition = IssueTransitions.wont_fix
        rr.add_message(cmt('sdt_gt_edt'))

        return rr


class WrongDateFormatTestCase(AbstractPromoTestCase):
    _representation = 'wrong_date_format'

    pc_group_cols = AbstractPromoTestCase.pc_group_cols.copy()
    pc_group_cols['end_dt'] = '2019.10.02'

    def get_result(self):
        rr = RequiredResult()
        rr.transition = IssueTransitions.wont_fix
        rr.add_message(cmt('date_parse'))

        return rr


class ImpossibleDateTestCase(AbstractPromoTestCase):
    _representation = 'impossible_date'

    pc_group_cols = AbstractPromoTestCase.pc_group_cols.copy()
    pc_group_cols['end_dt'] = '32.10.2019'

    def get_result(self):
        rr = RequiredResult()
        rr.transition = IssueTransitions.wont_fix
        rr.add_message(cmt('date_parse'))

        return rr


class DatetimeFmtTestCase(AbstractPromoTestCase):
    _representation = 'datetime_fmt'

    pc_group_cols = AbstractPromoTestCase.pc_group_cols.copy()
    pc_group_cols['start_dt'] = dt.datetime(2019, 9, 23)

    def get_result(self):
        rr = RequiredResult(delay=True)
        rr.transition = IssueTransitions.opened
        rr.add_message(
            self.get_confirm_comment(
                start_dt=dt.datetime.strftime(
                    self.pc_group_cols['start_dt'], '%d.%m.%Y'
                )
            )
        )
        rr.summonees = [self.support_manager]

        return rr


class BadFileTestCase(AbstractPromoTestCase):
    _representation = 'bad_file'

    _filename = 'i_am_a_file_made_by_a_retard.txt'

    def get_result(self):
        rr = RequiredResult()
        rr.transition = IssueTransitions.wont_fix
        rr.add_message(cmt('no_file'))

        return rr


class NoClientTestCase(AbstractPromoTestCase):
    _representation = 'no_client'

    codes = ['kekekek', 'fufufu']

    def get_result(self):
        rr = RequiredResult(delay=True)
        rr.transition = IssueTransitions.opened
        rr.add_message(self.get_confirm_comment(client_id=False))
        rr.summonees = [self.support_manager]

        return rr


class DiscountTestCase(AbstractPromoTestCase):
    _representation = 'discount'

    def _get_data(self, session):
        desc = super(DiscountTestCase, self)._get_data(session)
        desc['is_discount'] = 'да'

        return desc

    def get_result(self):
        rr = RequiredResult(delay=True)
        rr.transition = IssueTransitions.opened
        rr.add_message(
            self.get_confirm_comment(
                calc_params='''{'currency_bonuses': {u'BYN': Decimal('666')}, u'apply_on_create': True}'''
            )
        )
        rr.summonees = [self.support_manager]

        return rr


class UnapprovedTestCase(AbstractPromoTestCase):
    _representation = 'unapproved'

    def get_comments(self):
        return [
            {'author': self.robot, 'text': self.get_confirm_comment(), 'summonees': [self.support_manager]},
            (self.support_manager, 'Не подтверждено')
        ]

    def get_result(self):
        rr = RequiredResult(delay=False)
        rr.transition = IssueTransitions.wont_fix

        return rr


class WrongAuthorApprovedTestCase(AbstractPromoTestCase):
    _representation = 'wrong_author_approved'

    def get_result(self):
        res = RequiredResult(delay=True)
        res.transition = IssueTransitions.none
        return res

    def get_comments(self):
        return [
            {'author': self.robot, 'text': self.get_confirm_comment(), 'summonees': [self.support_manager]},
            ('naebshik', 'Подтверждено')
        ]


class WrongAuthorUnapprovedTestCase(AbstractPromoTestCase):
    _representation = 'wrong_author_unapproved'

    def get_result(self):
        res = RequiredResult(delay=True)
        res.transition = IssueTransitions.none
        return res

    def get_comments(self):
        return [
            {'author': self.robot, 'text': self.get_confirm_comment(), 'summonees': [self.support_manager]},
            ('naebshik', 'Не подтверждено')
        ]


class AbstractApprovedTestCase(AbstractPromoTestCase):

    def get_comments(self):
        return [
            {'author': self.robot, 'text': self.get_confirm_comment(), 'summonees': [self.support_manager]},
            (self.support_manager, 'Подтверждено')
        ]

    def _get_state(self):
        state = self.get_contents()

        for row in state:
            row['calc_params'] = {'currency_bonuses': {'BYN': D('666')}, 'apply_on_create': False}
            row['minimal_amounts'] = {'BYN': D('333')}
            row['calc_class_name'] = 'FixedSumBonusPromoCodeGroup'
            row['service_ids'] = [int(sid) for sid in str(row['service_id']).split(',')]

        return state


class ApprovedWClientTestCase(AbstractApprovedTestCase):
    _representation = 'approved_w_client'

    def get_result(self):
        state = self._get_state()

        rr = RequiredResult(state=state)
        rr.add_message(cmt('done'))

        return rr


class ApprovedNoClientTestCase(AbstractApprovedTestCase):
    _representation = 'approved_no_client'

    codes = ['kekekek', 'fufufu']

    def get_result(self):
        state = self._get_state()

        rr = RequiredResult(state=state)
        rr.add_message(cmt('done'))

        return rr


class ApprovedQtyBonusTestCase(AbstractQtyBonusTestCase, AbstractApprovedTestCase):
    _representation = 'approved_qty_bonus'

    def _get_state(self):
        state = self.get_contents()

        for row in state:
            row['calc_params'] = {
                'product_bonuses': {'503163': D('35'), '503162': D('2500'), '508588': D('35'), '1475': D('35')},
                'apply_on_create': False}
            row['minimal_amounts'] = {'RUB': D('2000'), 'USD': D('30'), 'USZ': D('200000')}
            row['calc_class_name'] = 'FixedQtyBonusPromoCodeGroup'
            row['service_ids'] = [int(sid) for sid in str(row['service_id']).split(',')]

        return state

    def get_result(self):
        state = self._get_state()

        rr = RequiredResult(state=state)
        rr.add_message(cmt('done'))

        return rr


class ApprovedDiscountTestCase(AbstractApprovedTestCase):
    _representation = 'approved_discount'

    def _get_data(self, session):
        desc = super(ApprovedDiscountTestCase, self)._get_data(session)
        desc['is_discount'] = 'да'

        return desc

    def _get_state(self):
        state = super(ApprovedDiscountTestCase, self)._get_state()

        for row in state:
            row['calc_params']['apply_on_create'] = True

        return state

    def get_result(self):
        state = self._get_state()

        rr = RequiredResult(state=state)
        rr.add_message(cmt('done'))

        return rr


class NotEmptyDiscountColumnTestCase(AbstractPromoTestCase):
    _representation = 'not_empty_discount_column'

    pc_group_cols = AbstractPromoTestCase.pc_group_cols.copy()
    pc_group_cols['discount_pct'] = 15

    def _get_data(self, session):
        desc = super(NotEmptyDiscountColumnTestCase, self)._get_data(session)
        desc['is_discount'] = 'да'

        return desc

    def get_result(self):
        rr = RequiredResult()
        rr.transition = IssueTransitions.wont_fix
        rr.add_message(cmt('not_empty_discount_pct'))

        return rr


class BonusNotFilledTestCase(AbstractPromoTestCase):
    _representation = 'empty_bonus'

    def _get_data(self, session):
        desc = {var: '' for var in self._descr_vars}
        desc['calc_class'] = 'с бонусом в количестве (FixedQtyBonusPromoCodeGroup)'
        desc['is_discount'] = 'Да'
        return desc

    def get_result(self):
        rr = RequiredResult()
        rr.transition = IssueTransitions.wont_fix
        rr.add_message(cmt('no_ma_bonus_data'))

        return rr


class NotEmptyBonusColumnTestCase(AbstractPromoTestCase):
    _representation = 'not_empty_bonus'

    pc_group_cols = AbstractPromoTestCase.pc_group_cols.copy()

    def _get_data(self, session):
        desc = {var: '' for var in self._descr_vars}
        desc['calc_class'] = 'с фиксированной скидкой (FixedDiscountPromoCodeGroup)'
        desc['is_discount'] = 'Да'
        desc['bonus_byn'] = 666
        return desc

    def get_result(self):
        rr = RequiredResult()
        rr.transition = IssueTransitions.wont_fix
        rr.add_message(cmt('not_empty_bonus_data'))

        return rr


class EmptyDiscountColumnTestCase(AbstractPromoTestCase):
    _representation = 'empty_discount_column'

    pc_group_cols = AbstractPromoTestCase.pc_group_cols.copy()
    pc_group_cols['discount_pct'] = None

    def _get_data(self, session):
        desc = {var: '' for var in self._descr_vars}
        desc['calc_class'] = 'с фиксированной скидкой (FixedDiscountPromoCodeGroup)'
        desc['is_discount'] = 'Да'
        return desc

    def get_result(self):
        rr = RequiredResult()
        rr.transition = IssueTransitions.wont_fix
        rr.add_message(cmt('empty_column', 'discount_pct'))

        return rr


class ApprovedFixedDiscountTestCase(AbstractApprovedTestCase):
    _representation = 'approved_fixed_discount'

    pc_group_cols = AbstractPromoTestCase.pc_group_cols.copy()
    pc_group_cols['discount_pct'] = 15
    pc_group_cols['event'] = None
    pc_group_cols['reservation_days'] = None
    pc_group_cols['product_id'] = '6, 66, 666'

    def _get_data(self, session):
        desc = {var: '' for var in self._descr_vars}
        desc['calc_class'] = 'с фиксированной скидкой (FixedDiscountPromoCodeGroup)'
        desc['is_discount'] = 'Да'
        desc['min_byn'] = 333
        return desc

    def _get_state(self):
        state = self.get_contents()

        for row in state:
            row['calc_params'] = {
                'adjust_quantity': False,
                'apply_on_create': True,
                'discount_pct': '15'
            }
            row['minimal_amounts'] = {'BYN': D('333')}
            row['calc_class_name'] = 'FixedDiscountPromoCodeGroup'
            row['service_ids'] = [int(sid) for sid in str(row['service_id']).split(',')]
            row['_product_ids'] = [int(sid) for sid in str(row['product_id']).split(',')]

        return state

    def get_result(self):
        state = self._get_state()

        rr = RequiredResult(state=state)
        rr.add_message(cmt('done'))

        return rr


class ApprovedFixedDiscountProductIdNoneTestCase(AbstractApprovedTestCase):
    _representation = 'approved_fixed_discount_product_id_none'

    pc_group_cols = AbstractPromoTestCase.pc_group_cols.copy()
    pc_group_cols['discount_pct'] = 20
    pc_group_cols['event'] = None
    pc_group_cols['reservation_days'] = None
    pc_group_cols['product_id'] = None

    def _get_data(self, session):
        desc = {var: '' for var in self._descr_vars}
        desc['calc_class'] = 'с фиксированной скидкой (FixedDiscountPromoCodeGroup)'
        desc['is_discount'] = 'Да'
        return desc

    def _get_state(self):
        state = self.get_contents()

        for row in state:
            row['calc_params'] = {
                'adjust_quantity': False,
                'apply_on_create': True,
                'discount_pct': '20'
            }
            row['calc_class_name'] = 'FixedDiscountPromoCodeGroup'
            row['service_ids'] = [int(sid) for sid in str(row['service_id']).split(',')]

        return state

    def get_result(self):
        state = self._get_state()

        rr = RequiredResult(state=state)
        rr.add_message(cmt('done'))

        return rr


class InternalErrorTestCase(AbstractApprovedTestCase):
    _representation = 'internal_error'

    # Fuck up the unique constraint to trigger internal error
    codes = [('kekekekek', 12345), ('kekekekek', 54321)]

    def get_result(self):
        rr = RequiredResult(commit=False)
        rr.add_message(cmt('unknown_error'))
        rr.transition = IssueTransitions.none
        rr.summonees = [self.responsible_manager]
        rr.assignee = self.responsible_manager

        return rr


@pytest.mark.parametrize('issue_data',
                         [case(robot, support_manager, responsible_manager) for case in AbstractPromoTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['issue_data'])
def test_promo(session, issue_data):
    do_test(session, issue_data)
