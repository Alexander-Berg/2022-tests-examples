# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import datetime as dt

import pytest

from balance.actions.invoice_turnon import InvoiceTurnOn
from balance import muzzle_util as ut
from autodasha.core.api.tracker import IssueTransitions

from tests.autodasha_tests.common import db_utils
from tests.autodasha_tests.functional_tests import case_utils
from tests.autodasha_tests.functional_tests.solvers.change_contract.common import (
    AbstractBaseDBTestCase, RequiredResult
)

__all__ = ['AbstractChangeWRulesDBTestCase']


class AbstractChangeWRulesDBTestCase(AbstractBaseDBTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    author = 'noob'

    def setup_config(self, session, config):
        config['CHANGECONTRACT_PROCESS_RULES'] = True

    def get_comments(self):
        return [
            ('boss', 'Подтверждено'),
            ('mscnad7', 'Подтверждено')
        ]


class SetParamsGeneralContractBadDBTestCase(AbstractChangeWRulesDBTestCase):
    _representation = 'w_rules_set_gen_c_bad'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person,
                                                         dt.datetime(2016, 1, 1),
                                                         external_id='12345/15')
        return self.contract, [
            ('dt', dt.datetime(2016, 2, 1)),
            ('finish_dt', dt.datetime(2016, 8, 1)),
            ('is_booked', dt.datetime(2016, 3, 1)),
            ('is_faxed', dt.datetime(2016, 4, 1)),
            ('is_signed', dt.datetime(2016, 5, 1)),
            ('sent_dt', dt.datetime(2016, 6, 1)),
            ('external_id', '666/777'),
            ('offer_confirmation_type', 'Не требуется'),
            ('payment_term', '15'),
            ('unilateral_acts', 'Нет'),
            ('memo', 'Woop-woop!\nThat`s the sound of da police!'),
            ('other_changes_checkbox', 'Да')
        ]

    def get_result(self):
        res = RequiredResult(self.contract, rule_no_manager=True, done=False, enqueued=False, delay=False, commit=False,
                              transition=IssueTransitions.wont_fix, state=[
            ('dt', dt.datetime(2016, 1, 1)),
            ('external_id', '12345/15'),
        ])
        return res


class SetParamsGeneralContractGoodDBTestCase(AbstractChangeWRulesDBTestCase):
    _representation = 'w_rules_set_gen_c_good'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person,
                                                         dt.datetime(2016, 1, 1),
                                                         external_id='12345/15', manager_code=-1)
        return self.contract, [
            ('dt', dt.datetime(2016, 2, 1)),
            ('finish_dt', dt.datetime(2016, 8, 1)),
            ('is_booked', dt.datetime(2016, 3, 1)),
            ('is_faxed', dt.datetime(2016, 4, 1)),
            ('is_signed', dt.datetime(2016, 5, 1)),
            ('sent_dt', dt.datetime(2016, 6, 1)),
            ('external_id', '666/777'),
            ('offer_confirmation_type', 'Не требуется'),
            ('payment_term', '15'),
            ('unilateral_acts', 'Нет'),
            ('memo', 'Woop-woop!\nThat`s the sound of da police!'),
            ('other_changes_checkbox', 'Да')
        ]

    def get_result(self):
        return RequiredResult(self.contract, done=True, enqueued=True, delay=False, commit=True,
                              transition=IssueTransitions.none, state=[
            ('dt', dt.datetime(2016, 2, 1)),
            ('is_booked_dt', dt.datetime(2016, 3, 1)),
            ('is_booked', 0),
            ('is_faxed', dt.datetime(2016, 4, 1)),
            ('is_signed', dt.datetime(2016, 5, 1)),
            ('sent_dt', dt.datetime(2016, 6, 1)),
            ('finish_dt', dt.datetime(2016, 8, 1)),
            ('external_id', '666/777'),
            ('offer_confirmation_type', 'no'),
            ('unilateral', 0),
            ('payment_term', 15),
            ('memo', '123333333\nWoop-woop!\nThat`s the sound of da police!')
        ])


class SetParamsGeneralContractPAEmptyCNEmptyBadDBTestCase(AbstractChangeWRulesDBTestCase):
    _representation = 'w_rules_set_gen_c_empty_pa_with_cn_bad'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1),
                                                         finish_dt=dt.datetime(2016, 1, 1),
                                                         firm_id=13,
                                                         payment_type=3,
                                                         personal_account=1)

        pa = db_utils.create_personal_account(session, contract=self.contract)
        # pa.dt = dt.datetime(2016, 1, 1)
        self.personal_account = pa
        self.charge_note = db_utils.create_charge_note(session, self.personal_account)
        self.charge_note.contract = self.contract
        # InvoiceTurnOn(self.charge_note, sum=100).do()
        session.flush()

        return self.contract, [
            ('dt', dt.datetime(2016, 2, 1)),
            ('finish_dt', dt.datetime(2016, 8, 1)),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, rule_no_manager=True, done=False, enqueued=False, delay=False,
                             commit=False,
                             transition=IssueTransitions.wont_fix,
                             state=[
                                 ('dt', dt.datetime(2016, 1, 1)),
                                 ('finish_dt', dt.datetime(2016, 1, 1)),
                             ],
                             pa_state=[
                                 (self.personal_account, 0)
                             ],
                             cn_state=[
                                 (self.charge_note, 0)
                             ]
                             )
        return res


class SetParamsGeneralContractPAEmptyCNEmptyGoodDBTestCase(AbstractChangeWRulesDBTestCase):
    _representation = 'w_rules_set_gen_c_empty_pa_with_cn_good'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1),
                                                         finish_dt=dt.datetime(2016, 1, 1),
                                                         firm_id=13,
                                                         payment_type=3,
                                                         personal_account=1, manager_code=-1)

        pa = db_utils.create_personal_account(session, contract=self.contract)
        # pa.dt = dt.datetime(2016, 1, 1)
        self.personal_account = pa
        self.charge_note = db_utils.create_charge_note(session, self.personal_account)
        self.charge_note.contract = self.contract
        # InvoiceTurnOn(self.charge_note, sum=100).do()
        session.flush()

        return self.contract, [
            ('dt', dt.datetime(2016, 2, 1)),
            ('finish_dt', dt.datetime(2016, 8, 1)),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, done=True, enqueued=True, delay=False, commit=True,
                             transition=IssueTransitions.none,
                             state=[
                                 ('dt', dt.datetime(2016, 2, 1)),
                                 ('finish_dt', dt.datetime(2016, 8, 1)),
                             ],
                             pa_state=[
                                 (self.personal_account, 2)
                             ],
                             cn_state=[
                                 (self.charge_note, 2)
                             ]
                             )
        return res


class SetParamsGeneralCollateralBadDBTestCase(AbstractChangeWRulesDBTestCase):
    _representation = 'w_rules_set_gen_col_bad'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1))
        self.collateral = db_utils.add_collateral(self.contract, is_signed=dt.datetime(2016, 1, 1))
        return self.contract, [
            ('col', self.collateral),
            ('col_num', 'ТЕСТ'),
            ('dt', dt.datetime(2016, 2, 1)),
            ('finish_dt', dt.datetime(2016, 8, 1)),
            ('is_booked', dt.datetime(2016, 3, 1)),
            ('is_faxed', dt.datetime(2016, 4, 1)),
            ('is_signed', dt.datetime(2016, 5, 1)),
            ('sent_dt', dt.datetime(2016, 6, 1)),
            ('print_form_type', 'Без печатной формы'),
            ('unilateral_acts', 'Да'),
            ('memo', 'Woop-woop!\nThat`s the sound of da police!'),
            ('other_changes_checkbox', 'Да')
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, rule_no_manager=True,
                             done=False, enqueued=False, delay=False, commit=False,
                             transition=IssueTransitions.wont_fix,
                             state=[
                                 ('dt', ut.trunc_date(dt.datetime.now())),
                                 ('is_signed', dt.datetime(2016, 1, 1)),
                             ])
        return res


class SetParamsGeneralCollateralGoodDBTestCase(AbstractChangeWRulesDBTestCase):
    _representation = 'w_rules_set_gen_col_good'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1), manager_code=-1)
        self.collateral = db_utils.add_collateral(self.contract, is_signed=dt.datetime(2016, 1, 1))
        return self.contract, [
            ('col', self.collateral),
            ('col_num', 'ТЕСТ'),
            ('dt', dt.datetime(2016, 2, 1)),
            ('finish_dt', dt.datetime(2016, 8, 1)),
            ('is_booked', dt.datetime(2016, 3, 1)),
            ('is_faxed', dt.datetime(2016, 4, 1)),
            ('is_signed', dt.datetime(2016, 5, 1)),
            ('sent_dt', dt.datetime(2016, 6, 1)),
            ('print_form_type', 'Без печатной формы'),
            ('unilateral_acts', 'Да'),
            ('memo', 'Woop-woop!\nThat`s the sound of da police!'),
            ('other_changes_checkbox', 'Да')
        ]

    def get_result(self):
        return RequiredResult(self.contract, self.collateral,
                              done=True, enqueued=True, delay=False, commit=True,
                              transition=IssueTransitions.none,
                              state=[
                                  ('dt', dt.datetime(2016, 2, 1)),
                                  ('num', 'ТЕСТ'),
                                  ('is_booked_dt', dt.datetime(2016, 3, 1)),
                                  ('is_booked', 0),
                                  ('is_faxed', dt.datetime(2016, 4, 1)),
                                  ('is_signed', dt.datetime(2016, 5, 1)),
                                  ('sent_dt', dt.datetime(2016, 6, 1)),
                                  ('finish_dt', dt.datetime(2016, 8, 1)),
                                  ('print_form_type', 3),
                                  ('unilateral', 1),
                                  ('memo', 'test\nWoop-woop!\nThat`s the sound of da police!')
                              ])


class SetExternalIDWOSignedCollateralBadDBTestCase(AbstractChangeWRulesDBTestCase):
    _representation = 'w_rules_set_external_id_wo_signed_bad'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        params = {'is_signed': None}
        self.contract = db_utils.create_general_contract(
            session, client, person, dt.datetime(2016, 1, 1), external_id='123/45', **params
        )
        return self.contract, [
            ('external_id', 'lalka'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, rule_no_manager=True,
                             done=False, enqueued=False, delay=False, commit=False,
                             transition=IssueTransitions.wont_fix,
                             state=[
                                 ('external_id', '123/45'),
                                 ('dt', dt.datetime(2016, 1, 1))
                             ])
        return res


class SetExternalIDWOSignedCollateralGoodDBTestCase(AbstractChangeWRulesDBTestCase):
    _representation = 'w_rules_set_external_id_wo_signed_good'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        params = {'is_signed': None}
        self.contract = db_utils.create_general_contract(
            session, client, person, dt.datetime(2016, 1, 1), manager_code=-1, **params
        )
        return self.contract, [
            ('external_id', 'lalka'),
        ]

    def get_result(self):
        return RequiredResult(self.contract, self.collateral,
                              done=True, enqueued=True, delay=False, commit=True,
                              transition=IssueTransitions.none,
                              state=[
                                  ('external_id', 'lalka')
                              ])


class RemoveParamsGeneralContractBadDBTestCase(AbstractChangeWRulesDBTestCase):
    _representation = 'w_rules_remove_gen_c_bad'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        params = {
            'is_booked_dt': dt.datetime(2016, 3, 1),
            'is_booked': 1,
            'is_faxed': dt.datetime(2016, 4, 1),
            'is_signed': dt.datetime(2016, 5, 1),
            'sent_dt': dt.datetime(2016, 6, 1),
            'finish_dt': dt.datetime(2016, 8, 1),
        }
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1), **params)
        return self.contract, [
            ('remove_is_booked', True),
            ('remove_is_faxed', True),
            ('remove_is_signed', True),
            ('remove_sent_dt', True),
        ]

    def get_result(self):
        return RequiredResult(self.contract, rule_no_manager=True, done=False, enqueued=False, delay=False, commit=False,
                              transition=IssueTransitions.wont_fix, state=[
            ('dt', dt.datetime(2016, 1, 1)),
            ('is_booked_dt', dt.datetime(2016, 3, 1)),
            ('is_booked', 1),
            ('is_faxed', dt.datetime(2016, 4, 1)),
            ('is_signed', dt.datetime(2016, 5, 1)),
            ('sent_dt', dt.datetime(2016, 6, 1)),
            ('finish_dt', dt.datetime(2016, 8, 1)),
            ('is_cancelled', None),
        ])


class RemoveParamsGeneralContractGoodDBTestCase(AbstractChangeWRulesDBTestCase):
    _representation = 'w_rules_remove_gen_c_good'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        params = {
            'is_booked_dt': dt.datetime(2016, 3, 1),
            'is_booked': 1,
            'is_faxed': dt.datetime(2016, 4, 1),
            'is_signed': dt.datetime(2016, 5, 1),
            'sent_dt': dt.datetime(2016, 6, 1),
            'finish_dt': dt.datetime(2016, 8, 1),
        }
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1), manager_code=-1,
                                                         **params)
        return self.contract, [
            ('remove_is_booked', True),
            ('remove_is_faxed', True),
            ('remove_is_signed', True),
            ('remove_sent_dt', True),
        ]

    def get_result(self):
        return RequiredResult(self.contract, done=True, enqueued=True, delay=False, commit=True,
                              transition=IssueTransitions.none, state=[
            ('dt', dt.datetime(2016, 1, 1)),
            ('is_booked_dt', dt.datetime(2016, 3, 1)),
            ('is_booked', None),
            ('is_faxed', None),
            ('is_signed', None),
            ('sent_dt', None),
            ('finish_dt', dt.datetime(2016, 8, 1)),
            ('is_cancelled', None),
        ])


class RemoveParamsGeneralCollateralBadDBTestCase(AbstractChangeWRulesDBTestCase):
    _representation = 'w_rules_remove_gen_col_bad'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        params = {
            'is_booked_dt': dt.datetime(2016, 3, 1),
            'is_booked': 1,
            'is_faxed': dt.datetime(2016, 4, 1),
            'is_signed': dt.datetime(2016, 5, 1),
            'sent_dt': dt.datetime(2016, 6, 1),
            'finish_dt': dt.datetime(2016, 8, 1),
        }
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1), **params)
        self.collateral = db_utils.add_collateral(self.contract, dt_=dt.datetime(2016, 1, 1), **params)
        return self.contract, [
            ('col', self.collateral),
            ('remove_is_booked', True),
            ('remove_is_faxed', True),
            ('remove_is_signed', True),
            ('remove_sent_dt', True),
        ]

    def get_result(self):
        return RequiredResult(self.contract, self.collateral, rule_no_manager=True,
                              done=False, enqueued=False, delay=False, commit=False,
                              transition=IssueTransitions.wont_fix,
                              state=[
                                  ('dt', dt.datetime(2016, 1, 1)),
                                  ('is_booked_dt', dt.datetime(2016, 3, 1)),
                                  ('is_booked', 1),
                                  ('is_faxed', dt.datetime(2016, 4, 1)),
                                  ('is_signed', dt.datetime(2016, 5, 1)),
                                  ('sent_dt', dt.datetime(2016, 6, 1)),
                                  ('finish_dt', dt.datetime(2016, 8, 1)),
                                  ('is_cancelled', None),
                              ])


class RemoveParamsGeneralCollateralGoodDBTestCase(AbstractChangeWRulesDBTestCase):
    _representation = 'w_rules_remove_gen_col_good'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        params = {
            'is_booked_dt': dt.datetime(2016, 3, 1),
            'is_booked': 1,
            'is_faxed': dt.datetime(2016, 4, 1),
            'is_signed': dt.datetime(2016, 5, 1),
            'sent_dt': dt.datetime(2016, 6, 1),
            'finish_dt': dt.datetime(2016, 8, 1),
        }
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1), manager_code=-1,
                                                         **params)
        self.collateral = db_utils.add_collateral(self.contract, dt_=dt.datetime(2016, 1, 1), **params)
        return self.contract, [
            ('col', self.collateral),
            ('remove_is_booked', True),
            ('remove_is_faxed', True),
            ('remove_is_signed', True),
            ('remove_sent_dt', True),
        ]

    def get_result(self):
        return RequiredResult(self.contract, self.collateral,
                              done=True, enqueued=True, delay=False, commit=True,
                              transition=IssueTransitions.none,
                              state=[
                                  ('dt', dt.datetime(2016, 1, 1)),
                                  ('is_booked_dt', dt.datetime(2016, 3, 1)),
                                  ('is_booked', None),
                                  ('is_faxed', None),
                                  ('is_signed', None),
                                  ('sent_dt', None),
                                  ('finish_dt', dt.datetime(2016, 8, 1)),
                                  ('is_cancelled', None),
                              ])


class CancelGeneralContractBadDBTestCase(AbstractChangeWRulesDBTestCase):
    _representation = 'w_rules_cancel_gen_c_bad'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        params = {
            'is_booked_dt': dt.datetime(2016, 3, 1),
            'is_booked': 1,
            'is_faxed': dt.datetime(2016, 4, 1),
            'is_signed': dt.datetime(2016, 5, 1),
            'sent_dt': dt.datetime(2016, 6, 1),
            'finish_dt': dt.datetime(2016, 8, 1),
        }
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1), **params)
        return self.contract, [
            ('remove_is_booked', True),
            ('remove_is_faxed', True),
            ('remove_is_signed', True),
            ('is_cancelled', True),
        ]

    def get_result(self):
        return RequiredResult(self.contract, rule_no_manager=True, done=False, enqueued=False, delay=False, commit=False,
                              transition=IssueTransitions.wont_fix, state=[
            ('dt', dt.datetime(2016, 1, 1)),
            ('is_booked_dt', dt.datetime(2016, 3, 1)),
            ('is_booked', 1),
            ('is_faxed', dt.datetime(2016, 4, 1)),
            ('is_signed', dt.datetime(2016, 5, 1)),
            ('sent_dt', dt.datetime(2016, 6, 1)),
            ('finish_dt', dt.datetime(2016, 8, 1)),
            ('is_cancelled', False),
        ])


class CancelGeneralContractGoodDBTestCase(AbstractChangeWRulesDBTestCase):
    _representation = 'w_rules_cancel_gen_c_good'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        params = {
            'is_booked_dt': dt.datetime(2016, 3, 1),
            'is_booked': 1,
            'is_faxed': dt.datetime(2016, 4, 1),
            'is_signed': dt.datetime(2016, 5, 1),
            'sent_dt': dt.datetime(2016, 6, 1),
            'finish_dt': dt.datetime(2016, 8, 1),
        }
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1), manager_code=-1,
                                                         **params)
        return self.contract, [
            ('remove_is_booked', True),
            ('remove_is_faxed', True),
            ('remove_is_signed', True),
            ('is_cancelled', True),
        ]

    def get_result(self):
        return RequiredResult(self.contract, done=True, enqueued=True, delay=False, commit=True,
                              transition=IssueTransitions.none, state=[
            ('dt', dt.datetime(2016, 1, 1)),
            ('is_booked_dt', dt.datetime(2016, 3, 1)),
            ('is_booked', None),
            ('is_faxed', None),
            ('is_signed', None),
            ('sent_dt', dt.datetime(2016, 6, 1)),
            ('finish_dt', dt.datetime(2016, 8, 1)),
            ('is_cancelled', True),
        ])


class CancelGeneralCollateralBadDBTestCase(AbstractChangeWRulesDBTestCase):
    _representation = 'w_rules_cancel_gen_col_bad'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        params = {
            'is_booked_dt': dt.datetime(2016, 3, 1),
            'is_booked': 1,
            'is_faxed': dt.datetime(2016, 4, 1),
            'is_signed': dt.datetime(2016, 5, 1),
            'sent_dt': dt.datetime(2016, 6, 1),
            'finish_dt': dt.datetime(2016, 8, 1),
        }
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1), **params)
        self.collateral = db_utils.add_collateral(self.contract, dt_=dt.datetime(2016, 1, 1), **params)
        return self.contract, [
            ('col', self.collateral),
            ('remove_is_booked', True),
            ('remove_is_faxed', True),
            ('remove_is_signed', True),
            ('is_cancelled', True),
        ]

    def get_result(self):
        return RequiredResult(self.contract, self.collateral, rule_no_manager=True,
                              done=False, enqueued=False, delay=False, commit=False,
                              transition=IssueTransitions.wont_fix,
                              state=[
                                  ('dt', dt.datetime(2016, 1, 1)),
                                  ('is_booked_dt', dt.datetime(2016, 3, 1)),
                                  ('is_booked', 1),
                                  ('is_faxed', dt.datetime(2016, 4, 1)),
                                  ('is_signed', dt.datetime(2016, 5, 1)),
                                  ('sent_dt', dt.datetime(2016, 6, 1)),
                                  ('finish_dt', dt.datetime(2016, 8, 1)),
                                  ('is_cancelled', False),
                              ])


class CancelGeneralCollateralGoodDBTestCase(AbstractChangeWRulesDBTestCase):
    _representation = 'w_rules_cancel_gen_col_good'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        params = {
            'is_booked_dt': dt.datetime(2016, 3, 1),
            'is_booked': 1,
            'is_faxed': dt.datetime(2016, 4, 1),
            'is_signed': dt.datetime(2016, 5, 1),
            'sent_dt': dt.datetime(2016, 6, 1),
            'finish_dt': dt.datetime(2016, 8, 1),
        }
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1), manager_code=-1,
                                                         **params)
        self.collateral = db_utils.add_collateral(self.contract, dt_=dt.datetime(2016, 1, 1), **params)
        return self.contract, [
            ('col', self.collateral),
            ('remove_is_booked', True),
            ('remove_is_faxed', True),
            ('remove_is_signed', True),
            ('is_cancelled', True),
        ]

    def get_result(self):
        return RequiredResult(self.contract, self.collateral,
                              done=True, enqueued=True, delay=False, commit=True,
                              transition=IssueTransitions.none,
                              state=[
                                  ('dt', dt.datetime(2016, 1, 1)),
                                  ('is_booked_dt', dt.datetime(2016, 3, 1)),
                                  ('is_booked', None),
                                  ('is_faxed', None),
                                  ('is_signed', None),
                                  ('sent_dt', dt.datetime(2016, 6, 1)),
                                  ('finish_dt', dt.datetime(2016, 8, 1)),
                                  ('is_cancelled', True),
                              ])
