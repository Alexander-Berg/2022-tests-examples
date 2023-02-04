# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import datetime as dt

from balance.actions.invoice_turnon import InvoiceTurnOn
from autodasha.core.api.tracker import IssueTransitions

from tests.autodasha_tests.common import db_utils
from tests.autodasha_tests.functional_tests import case_utils
from tests.autodasha_tests.functional_tests.solvers.change_contract.common import (
    AbstractBaseDBTestCase, RequiredResult
)

__all__ = ['AbstractChangeDBTestCase']


class AbstractChangeDBTestCase(AbstractBaseDBTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    author = 'noob'

    def setup_config(self, session, config):
        config['CHANGE_CONTRACT']['payments_check'] = True

    def get_comments(self):
        return [
            ('boss', 'Подтверждено'),
            ('mscnad7', 'Подтверждено')
        ]


class SetParamsGeneralContractDBTestCase(AbstractChangeDBTestCase):
    _representation = 'set_gen_c'

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
        return RequiredResult(self.contract, done=True, enqueued=True, delay=False,
                              transition=IssueTransitions.none, state=[
            ('dt', dt.datetime(2016, 2, 1)),
            ('is_booked_dt', dt.datetime(2016, 3, 1)),
            ('is_booked', 1),
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


class SetParamsGeneralContractPAEmptyCNEmptyDBTestCase(AbstractChangeDBTestCase):
    _representation = 'set_gen_c_empty_pa_with_cn'

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
        res = RequiredResult(self.contract, self.collateral, done=True, enqueued=True, delay=False,
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


class SetParamsGeneralContractPANotEmptyDBTestCase(AbstractChangeDBTestCase):
    _representation = 'set_gen_c_not_empty_pa'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1),
                                                         finish_dt=dt.datetime(2016, 1, 1),
                                                         firm_id=13,
                                                         payment_type=3,
                                                         personal_account=1)

        pa = db_utils.create_personal_account(session, contract=self.contract)
        pa.dt = dt.datetime(2016, 1, 1)
        self.personal_account = pa
        self.charge_note = db_utils.create_charge_note(session, self.personal_account)
        self.charge_note.contract = self.contract
        InvoiceTurnOn(self.charge_note, sum=100).do()
        session.flush()

        return self.contract, [
            ('dt', dt.datetime(2016, 2, 1)),
            ('finish_dt', dt.datetime(2016, 8, 1)),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=False,
                             transition=IssueTransitions.wont_fix,
                             state=[
                                 ('dt', dt.datetime(2016, 1, 1)),
                             ],
                             pa_state=[
                                 (self.personal_account, 0)
                             ],
                             cn_state=[
                                 (self.charge_note, 0)
                             ]
                             )
        res.add_message('Выполнить нельзя, есть счета, выставленные до новой даты. Уточни, пожалуйста, дату и заполни форму еще раз.')
        return res


class SetParamsGeneralCollateralDBTestCase(AbstractChangeDBTestCase):
    _representation = 'set_gen_col'

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
        return RequiredResult(self.contract, self.collateral,
                              done=True, enqueued=True, delay=False,
                              transition=IssueTransitions.none,
                              state=[
                                  ('dt', dt.datetime(2016, 2, 1)),
                                  ('num', 'ТЕСТ'),
                                  ('is_booked_dt', dt.datetime(2016, 3, 1)),
                                  ('is_booked', 1),
                                  ('is_faxed', dt.datetime(2016, 4, 1)),
                                  ('is_signed', dt.datetime(2016, 5, 1)),
                                  ('sent_dt', dt.datetime(2016, 6, 1)),
                                  ('finish_dt', dt.datetime(2016, 8, 1)),
                                  ('print_form_type', 3),
                                  ('unilateral', 1),
                                  ('memo', 'test\nWoop-woop!\nThat`s the sound of da police!')
                              ])


class SetExternalIDWOSignedCollateralDBTestCase(AbstractChangeDBTestCase):
    _representation = 'set_external_id_wo_signed'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        params = {'is_signed': None}
        self.contract = db_utils.create_general_contract(
            session, client, person, dt.datetime(2016, 1, 1), **params
        )
        return self.contract, [
            ('external_id', 'lalka'),
        ]

    def get_result(self):
        return RequiredResult(self.contract, self.collateral,
                              done=True, enqueued=True, delay=False,
                              transition=IssueTransitions.none,
                              state=[
                                  ('external_id', 'lalka')
                              ])


class SetExternalIDWOPersonDBTestCase(AbstractChangeDBTestCase):
    _representation = 'set_external_id_wo_person'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_distr_contract(
            session, client, person, dt.datetime(2016, 1, 1)
        )
        self.contract.person_id = None
        session.flush()
        return self.contract, [
            ('external_id', 'lalka'),
        ]

    def get_result(self):
        return RequiredResult(self.contract, self.collateral,
                              done=True, enqueued=True, delay=False,
                              transition=IssueTransitions.none,
                              state=[
                                  ('external_id', 'lalka')
                              ])


class RemoveParamsGeneralContractDBTestCase(AbstractChangeDBTestCase):
    _representation = 'remove_gen_c'

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
        return RequiredResult(self.contract, done=True, enqueued=True, delay=False,
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


class RemoveParamsGeneralCollateralDBTestCase(AbstractChangeDBTestCase):
    _representation = 'remove_gen_col'

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
        return RequiredResult(self.contract, self.collateral,
                              done=True, enqueued=True, delay=False,
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


class CancelGeneralContractDBTestCase(AbstractChangeDBTestCase):
    _representation = 'cancel_gen_c'

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
        return RequiredResult(self.contract, done=True, enqueued=True, delay=False,
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


class CancelGeneralCollateralDBTestCase(AbstractChangeDBTestCase):
    _representation = 'cancel_gen_col'

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
        return RequiredResult(self.contract, self.collateral,
                              done=True, enqueued=True, delay=False,
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


class SetParamsPartnersContractDBTestCase(AbstractChangeDBTestCase):
    _representation = 'set_partners_c'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_partners_contract(session, client, person, dt.datetime(2016, 1, 1), **{'unilateral_acts': 1})
        return self.contract, [
            ('dt', dt.datetime(2016, 2, 1)),
            ('finish_dt', dt.datetime(2016, 8, 1)),
            ('is_booked', dt.datetime(2016, 3, 1)),
            ('is_faxed', dt.datetime(2016, 4, 1)),
            ('is_signed', dt.datetime(2016, 5, 1)),
            ('sent_dt', dt.datetime(2016, 6, 1)),
            ('nds', 'РФ, стандартный НДС'),
            ('unilateral_acts', 'Нет'),
            ('payment_type', 'Акт раз в квартал'),
            ('memo', 'Woop-woop!\nThat`s the sound of da police!'),
            ('other_changes_checkbox', 'Да')
        ]

    def get_result(self):
        return RequiredResult(self.contract, done=True, enqueued=True, delay=False,
                              transition=IssueTransitions.none, state=[
            ('dt', dt.datetime(2016, 2, 1)),
            ('is_booked_dt', dt.datetime(2016, 3, 1)),
            ('is_booked', 1),
            ('is_faxed', dt.datetime(2016, 4, 1)),
            ('is_signed', dt.datetime(2016, 5, 1)),
            ('sent_dt', dt.datetime(2016, 6, 1)),
            ('end_dt', dt.datetime(2016, 8, 1)),
            ('nds', 18),
            ('unilateral_acts', 0),
            ('payment_type', 2),
            ('memo', 'Woop-woop!\nThat`s the sound of da police!')
        ])


class SetParamsPartnersCollateralDBTestCase(AbstractChangeDBTestCase):
    _representation = 'set_partners_col'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_partners_contract(session, client, person, dt.datetime(2016, 1, 1),
                                                          firm=13, nds=18)
        self.collateral = db_utils.add_collateral(self.contract, 2010, is_signed=dt.datetime(2016, 1, 1))
        return self.contract, [
            ('col', self.collateral),
            ('dt', dt.datetime(2016, 2, 1)),
            ('col_num', 'ТЕСТ'),
            ('finish_dt', dt.datetime(2016, 8, 1)),
            ('is_booked', dt.datetime(2016, 3, 1)),
            ('is_faxed', dt.datetime(2016, 4, 1)),
            ('is_signed', dt.datetime(2016, 5, 1)),
            ('sent_dt', dt.datetime(2016, 6, 1)),
            ('nds', 'НДС 0'),
            ('unilateral_acts', 'Да'),
            ('print_form_type', 'Без печатной формы'),
            ('payment_type', 'Акт раз в квартал'),
            ('memo', 'Woop-woop!\nThat`s the sound of da police!'),
            ('other_changes_checkbox', 'Да')
        ]

    def get_result(self):
        return RequiredResult(self.contract, self.collateral,
                              done=True, enqueued=True, delay=False,
                              transition=IssueTransitions.none,
                              state=[
                                  ('dt', dt.datetime(2016, 2, 1)),
                                  ('num', 'ТЕСТ'),
                                  ('is_booked_dt', dt.datetime(2016, 3, 1)),
                                  ('is_booked', 1),
                                  ('is_faxed', dt.datetime(2016, 4, 1)),
                                  ('is_signed', dt.datetime(2016, 5, 1)),
                                  ('sent_dt', dt.datetime(2016, 6, 1)),
                                  ('end_dt', dt.datetime(2016, 8, 1)),
                                  ('nds', 0),
                                  ('unilateral_acts', 1),
                                  ('print_form_type', 3),
                                  ('payment_type', 2),
                                  ('memo', 'test\nWoop-woop!\nThat`s the sound of da police!')
                              ])


class SetParamsDistrContractDBTestCase(AbstractChangeDBTestCase):
    _representation = 'set_distr_c'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_distr_contract(session, client, person, dt.datetime(2016, 1, 1), nds=0)
        return self.contract, [
            ('dt', dt.datetime(2016, 2, 1)),
            ('finish_dt', dt.datetime(2016, 8, 1)),
            ('is_booked', dt.datetime(2016, 3, 1)),
            ('is_faxed', dt.datetime(2016, 4, 1)),
            ('is_signed', dt.datetime(2016, 5, 1)),
            ('sent_dt', dt.datetime(2016, 6, 1)),
            ('nds', 'РФ, стандартный НДС'),
            ('payment_type', 'Акт раз в квартал'),
            ('memo', 'Woop-woop!\nThat`s the sound of da police!'),
            ('other_changes_checkbox', 'Да')
        ]

    def get_result(self):
        return RequiredResult(self.contract, done=True, enqueued=True, delay=False,
                              transition=IssueTransitions.none, state=[
            ('dt', dt.datetime(2016, 2, 1)),
            ('is_booked_dt', dt.datetime(2016, 3, 1)),
            ('is_booked', 1),
            ('is_faxed', dt.datetime(2016, 4, 1)),
            ('is_signed', dt.datetime(2016, 5, 1)),
            ('sent_dt', dt.datetime(2016, 6, 1)),
            ('end_dt', dt.datetime(2016, 8, 1)),
            ('nds', 18),
            ('payment_type', 2),
            ('memo', 'Woop-woop!\nThat`s the sound of da police!')
        ])


class SetParamsDistrCollateralDBTestCase(AbstractChangeDBTestCase):
    _representation = 'set_distr_col'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_distr_contract(session, client, person, dt.datetime(2016, 1, 1), nds=18)
        self.collateral = db_utils.add_collateral(self.contract, 3010, is_signed=dt.datetime(2016, 1, 1))
        return self.contract, [
            ('col', self.collateral),
            ('dt', dt.datetime(2016, 2, 1)),
            ('col_num', 'ТЕСТ'),
            ('finish_dt', dt.datetime(2016, 8, 1)),
            ('is_booked', dt.datetime(2016, 3, 1)),
            ('is_faxed', dt.datetime(2016, 4, 1)),
            ('is_signed', dt.datetime(2016, 5, 1)),
            ('sent_dt', dt.datetime(2016, 6, 1)),
            ('nds', 'НДС 0'),
            ('print_form_type', 'Без печатной формы'),
            ('payment_type', 'Акт раз в квартал'),
            ('memo', 'Woop-woop!\nThat`s the sound of da police!'),
            ('other_changes_checkbox', 'Да')
        ]

    def get_result(self):
        return RequiredResult(self.contract, self.collateral,
                              done=True, enqueued=True, delay=False,
                              transition=IssueTransitions.none,
                              state=[
                                  ('dt', dt.datetime(2016, 2, 1)),
                                  ('num', 'ТЕСТ'),
                                  ('is_booked_dt', dt.datetime(2016, 3, 1)),
                                  ('is_booked', 1),
                                  ('is_faxed', dt.datetime(2016, 4, 1)),
                                  ('is_signed', dt.datetime(2016, 5, 1)),
                                  ('sent_dt', dt.datetime(2016, 6, 1)),
                                  ('end_dt', dt.datetime(2016, 8, 1)),
                                  ('nds', 0),
                                  ('print_form_type', 3),
                                  ('payment_type', 2),
                                  ('memo', 'test\nWoop-woop!\nThat`s the sound of da police!')
                              ])


class SetParamsSpendableContractDBTestCase(AbstractChangeDBTestCase):
    _representation = 'set_spendable_c'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_spendable_contract(session, client, person, 137, external_id='111/13',
                                                           on_dt=dt.datetime(2016, 1, 1), nds=0)
        return self.contract, [
            ('dt', dt.datetime(2016, 2, 1)),
            ('finish_dt', dt.datetime(2016, 8, 1)),
            ('is_booked', dt.datetime(2016, 3, 1)),
            ('is_faxed', dt.datetime(2016, 4, 1)),
            ('is_signed', dt.datetime(2016, 5, 1)),
            ('sent_dt', dt.datetime(2016, 6, 1)),
            ('nds', 'РФ, стандартный НДС'),
            ('payment_type', 'Акт раз в квартал'),
            ('memo', 'Woop-woop!\nThat`s the sound of da police!'),
            ('other_changes_checkbox', 'Да')
        ]

    def get_result(self):
        return RequiredResult(self.contract, done=True, enqueued=True, delay=False,
                              transition=IssueTransitions.none, state=[
            ('dt', dt.datetime(2016, 2, 1)),
            ('is_booked_dt', dt.datetime(2016, 3, 1)),
            ('is_booked', 1),
            ('is_faxed', dt.datetime(2016, 4, 1)),
            ('is_signed', dt.datetime(2016, 5, 1)),
            ('sent_dt', dt.datetime(2016, 6, 1)),
            ('end_dt', dt.datetime(2016, 8, 1)),
            ('nds', 18),
            ('payment_type', 2),
            ('memo', 'Woop-woop!\nThat`s the sound of da police!')
        ])


class SetParamsSpendableCollateralDBTestCase(AbstractChangeDBTestCase):
    _representation = 'set_spendable_col'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_spendable_contract(session, client, person, 137, external_id='111/13',
                                                           on_dt=dt.datetime(2016, 1, 1), nds=18)
        self.collateral = db_utils.add_collateral(self.contract, 7020, is_signed=dt.datetime(2016, 1, 1))
        return self.contract, [
            ('col', self.collateral),
            ('dt', dt.datetime(2016, 2, 1)),
            ('col_num', 'ТЕСТ'),
            ('finish_dt', dt.datetime(2016, 8, 1)),
            ('is_booked', dt.datetime(2016, 3, 1)),
            ('is_faxed', dt.datetime(2016, 4, 1)),
            ('is_signed', dt.datetime(2016, 5, 1)),
            ('sent_dt', dt.datetime(2016, 6, 1)),
            ('nds', 'НДС 0'),
            ('print_form_type', 'Без печатной формы'),
            ('payment_type', 'Акт раз в квартал'),
            ('memo', 'Woop-woop!\nThat`s the sound of da police!'),
            ('other_changes_checkbox', 'Да')
        ]

    def get_result(self):
        return RequiredResult(self.contract, self.collateral,
                              done=True, enqueued=True, delay=False,
                              transition=IssueTransitions.none,
                              state=[
                                  ('dt', dt.datetime(2016, 2, 1)),
                                  ('num', 'ТЕСТ'),
                                  ('is_booked_dt', dt.datetime(2016, 3, 1)),
                                  ('is_booked', 1),
                                  ('is_faxed', dt.datetime(2016, 4, 1)),
                                  ('is_signed', dt.datetime(2016, 5, 1)),
                                  ('sent_dt', dt.datetime(2016, 6, 1)),
                                  ('end_dt', dt.datetime(2016, 8, 1)),
                                  ('nds', 0),
                                  ('print_form_type', 3),
                                  ('payment_type', 2),
                                  ('memo', 'test\nWoop-woop!\nThat`s the sound of da police!')
                              ])
